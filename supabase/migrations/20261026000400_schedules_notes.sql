-- StudyBloom 4/4: Max-only tables (class schedule, notes per subject)
-- Students can always read their own rows (nothing disappears if they downgrade),
-- but only students on Max can add, change or delete them.

create table public.schedules (
  id          uuid primary key default gen_random_uuid(),
  user_id     uuid not null default auth.uid() references auth.users (id) on delete cascade,
  subject     text not null check (char_length(btrim(subject)) between 1 and 40),
  day_of_week smallint not null check (day_of_week between 1 and 7),   -- 1 = Monday
  start_time  time not null,
  end_time    time not null,
  room        text check (room is null or char_length(room) <= 40),
  created_at  timestamptz not null default now(),
  check (end_time > start_time)
);

create index schedules_user_day on public.schedules (user_id, day_of_week, start_time);

create table public.notes (
  id         uuid primary key default gen_random_uuid(),
  user_id    uuid not null default auth.uid() references auth.users (id) on delete cascade,
  subject    text not null check (char_length(btrim(subject)) between 1 and 40),
  body       text not null default '' check (char_length(body) <= 10000),
  updated_at timestamptz not null default now(),
  unique (user_id, subject)
);

alter table public.schedules enable row level security;
alter table public.notes enable row level security;

-- schedules
create policy "schedules: read own"
  on public.schedules for select to authenticated
  using (user_id = (select auth.uid()));

create policy "schedules: insert on max"
  on public.schedules for insert to authenticated
  with check (user_id = (select auth.uid()) and public.effective_plan((select auth.uid())) = 'max');

create policy "schedules: update on max"
  on public.schedules for update to authenticated
  using (user_id = (select auth.uid()) and public.effective_plan((select auth.uid())) = 'max')
  with check (user_id = (select auth.uid()));

create policy "schedules: delete on max"
  on public.schedules for delete to authenticated
  using (user_id = (select auth.uid()) and public.effective_plan((select auth.uid())) = 'max');

-- notes
create policy "notes: read own"
  on public.notes for select to authenticated
  using (user_id = (select auth.uid()));

create policy "notes: insert on max"
  on public.notes for insert to authenticated
  with check (user_id = (select auth.uid()) and public.effective_plan((select auth.uid())) = 'max');

create policy "notes: update on max"
  on public.notes for update to authenticated
  using (user_id = (select auth.uid()) and public.effective_plan((select auth.uid())) = 'max')
  with check (user_id = (select auth.uid()));

create policy "notes: delete on max"
  on public.notes for delete to authenticated
  using (user_id = (select auth.uid()) and public.effective_plan((select auth.uid())) = 'max');
