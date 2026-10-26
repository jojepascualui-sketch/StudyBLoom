-- StudyBloom 2/4: subscriptions, plan helpers, sign-up trigger, demo mode
--
-- The browser may only READ its subscription row. Plans change in exactly two ways:
--   1. the stripe-webhook Edge Function (service role key, after a test-mode payment);
--   2. demo_set_plan(), which works only while app_settings.demo_mode = 'on'.

create table public.subscriptions (
  user_id                uuid primary key references auth.users (id) on delete cascade,
  plan                   text not null default 'free' check (plan in ('free', 'pro', 'max')),
  status                 text not null default 'active'
                         check (status in ('active', 'trialing', 'past_due', 'canceled', 'incomplete', 'unpaid')),
  current_period_end     timestamptz,
  stripe_customer_id     text,
  stripe_subscription_id text unique,
  is_demo                boolean not null default false,
  updated_at             timestamptz not null default now()
);

alter table public.subscriptions enable row level security;

create policy "subscriptions: read own"
  on public.subscriptions for select to authenticated
  using (user_id = (select auth.uid()));
-- No insert/update/delete policies on purpose: students cannot give themselves a plan.

-- The plan that is actually in force right now ('free' if missing, canceled or expired).
create or replace function public.effective_plan(uid uuid)
returns text
language sql
stable
security definer
set search_path = ''
as $$
  select coalesce(
    (select s.plan
       from public.subscriptions s
      where s.user_id = uid
        and s.status in ('active', 'trialing')
        and (s.current_period_end is null or s.current_period_end > now())),
    'free');
$$;

-- Same limits as the Java Subscription subclasses. null = unlimited.
create or replace function public.plan_max_active_tasks(plan text)
returns integer
language sql
immutable
set search_path = ''
as $$
  select case plan when 'free' then 15 else null end;
$$;

revoke all on function public.effective_plan(uuid) from public, anon;
grant execute on function public.effective_plan(uuid) to authenticated;

-- Create the profile and a Free subscription for every new sign-up.
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into public.profiles (id, email) values (new.id, new.email)
    on conflict (id) do nothing;
  insert into public.subscriptions (user_id) values (new.id)
    on conflict (user_id) do nothing;
  return new;
end;
$$;

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- ---------- Demo mode (for the class presentation) ----------
-- Turn on:  update public.app_settings set value = 'on'  where key = 'demo_mode';
-- Turn off: update public.app_settings set value = 'off' where key = 'demo_mode';

create table public.app_settings (
  key   text primary key,
  value text not null
);
alter table public.app_settings enable row level security;
-- No policies: only the SQL editor / service role can read or change settings.

insert into public.app_settings (key, value) values ('demo_mode', 'on');

create or replace function public.demo_set_plan(new_plan text)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
  if auth.uid() is null then
    raise exception 'Sign in first.';
  end if;
  if new_plan not in ('free', 'pro', 'max') then
    raise exception 'Unknown plan %', new_plan;
  end if;
  if not exists (select 1 from public.app_settings where key = 'demo_mode' and value = 'on') then
    raise exception 'Demo mode is turned off on the server. Use Stripe test checkout instead.';
  end if;

  insert into public.subscriptions (user_id, plan, status, current_period_end, is_demo, updated_at)
  values (auth.uid(), new_plan, 'active',
          case when new_plan = 'free' then null else now() + interval '30 days' end,
          new_plan <> 'free', now())
  on conflict (user_id) do update
    set plan = excluded.plan,
        status = excluded.status,
        current_period_end = excluded.current_period_end,
        is_demo = excluded.is_demo,
        updated_at = now();
end;
$$;

revoke all on function public.demo_set_plan(text) from public, anon;
grant execute on function public.demo_set_plan(text) to authenticated;
