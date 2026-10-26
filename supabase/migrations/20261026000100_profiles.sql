-- StudyBloom 1/4: profiles
-- One row per student, created automatically when they sign up.

create table public.profiles (
  id           uuid primary key references auth.users (id) on delete cascade,
  email        text,
  display_name text check (display_name is null or char_length(display_name) <= 60),
  created_at   timestamptz not null default now()
);

alter table public.profiles enable row level security;

-- Each student can read and edit only their own profile.
create policy "profiles: read own"
  on public.profiles for select to authenticated
  using (id = (select auth.uid()));

create policy "profiles: update own"
  on public.profiles for update to authenticated
  using (id = (select auth.uid()))
  with check (id = (select auth.uid()));
