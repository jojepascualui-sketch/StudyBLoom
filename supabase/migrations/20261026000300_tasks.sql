-- StudyBloom 3/4: tasks, with plan limits enforced in the database
-- (the same rules SubscriptionService checks in Java, so they can't be bypassed
-- by editing requests in the browser).

create table public.tasks (
  id           uuid primary key default gen_random_uuid(),
  user_id      uuid not null default auth.uid() references auth.users (id) on delete cascade,
  title        text not null check (char_length(btrim(title)) between 1 and 200),
  subject      text check (subject is null or char_length(subject) <= 40),
  description  text check (description is null or char_length(description) <= 2000),
  due_date     timestamptz not null,
  is_completed boolean not null default false,
  completed_at timestamptz,
  created_at   timestamptz not null default now()
);

create index tasks_user_open_due on public.tasks (user_id, is_completed, due_date);

alter table public.tasks enable row level security;

-- Each student sees and changes only their own tasks.
create policy "tasks: read own"
  on public.tasks for select to authenticated
  using (user_id = (select auth.uid()));

create policy "tasks: insert own"
  on public.tasks for insert to authenticated
  with check (user_id = (select auth.uid()));

create policy "tasks: update own"
  on public.tasks for update to authenticated
  using (user_id = (select auth.uid()))
  with check (user_id = (select auth.uid()));

create policy "tasks: delete own"
  on public.tasks for delete to authenticated
  using (user_id = (select auth.uid()));

-- Plan rules:
--  * Free: at most 15 unfinished tasks (checked when a task is added or un-completed).
--  * Free: no new subject tags (the subject is dropped).
--  * completed_at is kept consistent with is_completed.
create or replace function public.enforce_task_rules()
returns trigger
language plpgsql
set search_path = ''
as $$
declare
  user_plan  text := public.effective_plan(new.user_id);
  max_open   integer := public.plan_max_active_tasks(user_plan);
  open_count integer;
begin
  if tg_op = 'UPDATE' then
    new.user_id := old.user_id;          -- tasks can't be moved to another student
    new.created_at := old.created_at;
  end if;

  -- Only new or changed subjects are dropped, so tags from a past Pro period stay.
  if user_plan = 'free' and (tg_op = 'INSERT' or new.subject is distinct from old.subject) then
    new.subject := null;
  end if;

  if new.is_completed then
    new.completed_at := coalesce(new.completed_at, now());
  else
    new.completed_at := null;
  end if;

  if max_open is not null and not new.is_completed
     and (tg_op = 'INSERT' or old.is_completed) then
    -- Serialize per student so two quick inserts can't both slip under the limit.
    perform pg_advisory_xact_lock(hashtext(new.user_id::text));
    select count(*) into open_count
      from public.tasks t
     where t.user_id = new.user_id and not t.is_completed and t.id <> new.id;
    if open_count >= max_open then
      raise exception 'Free plan limit reached: up to % unfinished tasks. Upgrade to Pro for unlimited tasks.', max_open
        using errcode = 'P0001';
    end if;
  end if;

  return new;
end;
$$;

create trigger tasks_enforce_rules
  before insert or update on public.tasks
  for each row execute function public.enforce_task_rules();
