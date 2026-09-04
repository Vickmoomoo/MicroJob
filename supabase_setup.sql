-- ============================================================
-- MicroJob — Supabase 建表脚本（2026-08 定稿・完整版・可重复运行）
-- 在 Supabase Dashboard → SQL Editor 里整体运行一次即可。
-- 覆盖：6 张表 + RLS 策略 + Data API GRANT + Storage 桶 + Realtime。
-- 开头 DROP 旧表；每个 policy 前有 drop if exists，重复跑不会报错。
-- 注意：本文件仍会在最前面重建旧表，运行前请确认是否接受清除现有数据。
-- ============================================================

-- ---------- 0. 清理旧表（重建全套） ----------
drop table if exists course_certificates;
drop table if exists course_progress;
drop table if exists courses;
drop table if exists course_categories;
drop table if exists messages;
drop table if exists conversations;
drop table if exists reviews;
drop table if exists points_history;
drop table if exists profile_activities;
drop table if exists user_points;
drop table if exists donation_history;
drop table if exists vouchers;
drop table if exists users;
drop table if exists jobs;
drop table if exists categories;

-- ---------- 1. 分类 ----------
create table categories (
  id serial primary key,
  name text not null,
  emoji text not null
);

-- ---------- 2. 用户（must exist before course/user foreign keys） ----------
create table users (
  id bigserial primary key,
  name text not null,
  username text not null unique,
  password text not null,
  email text not null unique,
  security_question text not null default '',
  security_answer text not null default '',
  bio text not null default '',
  avatar_url text not null default '',
  region text not null default '',
  skills text[] not null default '{}',
  birthdate text not null default '',
  phone_number text not null default '',
  show_email boolean not null default false,
  show_birthdate boolean not null default false,
  show_phone_number boolean not null default false,
  show_avatar boolean not null default true,
  created_at timestamptz not null default now()
);

-- ---------- 1b. 课程分类 ----------
create table course_categories (
  id serial primary key,
  name text not null,
  emoji text not null
);

-- ---------- 1c. 课程 ----------
create table courses (
  id serial primary key,
  category_id int not null references course_categories(id) on delete cascade,
  title text not null,
  emoji text not null,
  lessons int not null default 1,
  duration text not null default '1h',
  description text not null default ''
);

-- ---------- 1d. 课程进度（用户维度） ----------
create table course_progress (
  id bigserial primary key,
  user_id bigint not null references users(id) on delete cascade,
  course_id int not null references courses(id) on delete cascade,
  enrolled boolean not null default false,
  progress int not null default 0,
  watched_episodes int[] not null default '{}',
  test_completed boolean not null default false,
  created_at timestamptz not null default now(),
  unique(user_id, course_id)
);

-- ---------- 1e. 课程证书 ----------
create table course_certificates (
  id bigserial primary key,
  user_id bigint not null references users(id) on delete cascade,
  course_id int not null references courses(id) on delete cascade,
  earned_date text not null,
  credential_id text not null unique,
  created_at timestamptz not null default now(),
  unique(user_id, course_id)
);

-- ---------- 3. 工作（对齐 Job.kt） ----------
create table jobs (
  id serial primary key,
  title text not null,
  price double precision not null,
  category text not null,
  location text not null,
  state text not null,
  area text not null,
  job_type text not null default 'onsite',
  description text not null,
  image_color bigint not null,
  images text[] not null default '{}',
  poster_id bigint not null default 0,
  worker_id bigint,
  status text not null default 'OPEN',             -- OPEN/IN_PROGRESS/COMPLETED/CANCELLED
  created_at timestamptz not null default now(),
  deadline timestamptz,
  scheduled_at timestamptz,
  require_gps boolean not null default false,
  tools_required text not null default '',
  payment_method text not null default 'Cash',
  bank text not null default '',
  payment_status text not null default 'ESCROWED', -- ESCROWED/RELEASED/REFUNDED
  donate boolean not null default false,
  donation_amount double precision not null default 0,
  boost_until timestamptz,
  currency text not null default 'RM',
  language text not null default ''
);

-- ---------- 4. 评价（对齐 Review.kt） ----------
create table reviews (
  id bigserial primary key,
  reviewed_user_id bigint not null,
  reviewer_user_id bigint not null,
  rating real not null default 5 check (rating between 0.5 and 5),
  comment text not null default '' check (char_length(comment) <= 500),
  job_id bigint,
  created_at timestamptz not null default now()
);
-- Prevent duplicate reviews for same job (allow multiple reviews when job_id is null)
create unique index if not exists reviews_unique_job on reviews (reviewer_user_id, reviewed_user_id, job_id);
create unique index if not exists reviews_unique_no_job on reviews (reviewer_user_id, reviewed_user_id) where job_id is null;

-- ---------- 5. 会话 + 消息（对齐 Conversation/Message） ----------
create table conversations (
  id text primary key default ('conv_' || replace(gen_random_uuid()::text, '-', '')), -- "conv_<小id>_<大id>"（App 生成），默认值兜底
  participant_ids bigint[] not null,
  last_message_preview text not null default '',
  last_message_at timestamptz not null default now(),
  last_sender_id bigint not null default 0,
  unread_counts jsonb not null default '{}'
);

create table messages (
  id text primary key default ('m_' || replace(gen_random_uuid()::text, '-', '')),   -- "m_<millis>"（App 生成），默认值兜底
  conversation_id text not null references conversations(id) on delete cascade,
  sender_id bigint not null,
  recipient_id bigint not null default 0,
  type text not null default 'TEXT',               -- TEXT/IMAGE/JOB_INVITE/PAYMENT_CARD/REVIEW
  text text not null default '',
  images text[] not null default '{}',
  job_id int not null default 0,
  created_at timestamptz not null default now(),
  review_rating real not null default 0,
  review_comment text not null default ''
);

-- ---------- 6. Social Impact tables ----------
create table donation_history (
  id bigserial primary key,
  user_id bigint not null,
  organization text not null,
  date text not null,
  amount text not null,
  created_at timestamptz not null default now()
);

create table vouchers (
  id serial primary key,
  brand text not null,
  title text not null,
  valid_stores text not null,
  points_required int not null,
  value text not null,
  brand_color bigint not null,
  description text not null default '',
  rules text[] not null default '{}',
  is_active boolean not null default true
);

create table user_points (
  id bigserial primary key,
  user_id bigint not null unique,
  points int not null default 0,
  updated_at timestamptz not null default now()
);

create table points_history (
  id bigserial primary key,
  user_id bigint not null,
  source text not null,
  points int not null,
  date text not null,
  is_earned boolean not null default true,
  created_at timestamptz not null default now()
);

-- ---------- 7. RLS 策略（public 读 + authenticated 写，贴近朋友做法／正规） ----------
-- 读：公开表谁都能看（anon + authenticated）；写：jobs/chat/users 等必须登录（authenticated）
alter table categories   enable row level security;
alter table course_categories enable row level security;
alter table courses      enable row level security;
alter table course_progress enable row level security;
alter table course_certificates enable row level security;
alter table jobs         enable row level security;
alter table users        enable row level security;
alter table reviews      enable row level security;
alter table conversations enable row level security;
alter table messages     enable row level security;
alter table donation_history enable row level security;
alter table vouchers     enable row level security;
alter table user_points  enable row level security;
alter table points_history enable row level security;

-- 清理旧 demo 策略（兼容旧名）
drop policy if exists "public read categories"   on categories;
drop policy if exists "public can read categories" on categories;
drop policy if exists "public read jobs"         on jobs;
drop policy if exists "public can read jobs" on jobs;
drop policy if exists "authenticated can read jobs" on jobs;
drop policy if exists "public read users"        on users;
drop policy if exists "public can read users" on users;
drop policy if exists "authenticated can read users" on users;
drop policy if exists "public read reviews"      on reviews;
drop policy if exists "public can read reviews" on reviews;
drop policy if exists "public read conversations" on conversations;
drop policy if exists "public can read conversations" on conversations;
drop policy if exists "authenticated can read conversations" on conversations;
drop policy if exists "public read messages"     on messages;
drop policy if exists "public can read messages" on messages;
drop policy if exists "authenticated can read messages" on messages;
drop policy if exists "public read donation_history" on donation_history;
drop policy if exists "public read vouchers" on vouchers;
drop policy if exists "public can read vouchers" on vouchers;
drop policy if exists "public read user_points" on user_points;
drop policy if exists "public read points_history" on points_history;
drop policy if exists "public insert jobs"          on jobs;
drop policy if exists "public can insert jobs" on jobs;
drop policy if exists "authenticated can insert jobs" on jobs;
drop policy if exists "public insert users"         on users;
drop policy if exists "public can insert users" on users;
drop policy if exists "authenticated can insert own profile" on users;
drop policy if exists "public insert reviews"       on reviews;
drop policy if exists "authenticated can insert reviews" on reviews;
drop policy if exists "public insert conversations" on conversations;
drop policy if exists "authenticated can insert conversations" on conversations;
drop policy if exists "public insert messages"      on messages;
drop policy if exists "authenticated can insert messages" on messages;
drop policy if exists "public insert donation_history" on donation_history;
drop policy if exists "public insert user_points" on user_points;
drop policy if exists "public insert points_history" on points_history;
drop policy if exists "public update jobs"          on jobs;
drop policy if exists "public can update jobs" on jobs;
drop policy if exists "authenticated can update jobs" on jobs;
drop policy if exists "public update users"         on users;
drop policy if exists "public can update users" on users;
drop policy if exists "authenticated can update own profile" on users;
drop policy if exists "authenticated can update users" on users;
drop policy if exists "public update reviews"       on reviews;
drop policy if exists "public update conversations" on conversations;
drop policy if exists "authenticated can update conversations" on conversations;
drop policy if exists "public update user_points" on user_points;
drop policy if exists "public delete jobs" on jobs;
drop policy if exists "authenticated can delete jobs" on jobs;

-- 读策略：公开表 public（anon + authenticated 谁都能读，首页不登录也能刷）
create policy "public can read categories" on categories for select to anon, authenticated using (true);
create policy "public can read course_categories" on course_categories for select to anon, authenticated using (true);
create policy "public can read courses" on courses for select to anon, authenticated using (true);
create policy "public can read vouchers"   on vouchers   for select to anon, authenticated using (true);
create policy "public can read jobs"       on jobs       for select to anon, authenticated using (true);
create policy "public can read users"      on users      for select to anon, authenticated using (true);
create policy "public can read reviews"    on reviews    for select to anon, authenticated using (true);
-- 私有表：只有登录能读（messages / conversations / points / donation_history / course_progress / course_certificates）
create policy "authenticated can read conversations" on conversations for select to authenticated using (true);
create policy "authenticated can read messages"      on messages      for select to authenticated using (true);
create policy "authenticated can read donation_history" on donation_history for select to authenticated using (true);
create policy "authenticated can read user_points"   on user_points   for select to authenticated using (true);
create policy "authenticated can read points_history" on points_history for select to authenticated using (true);

-- course_progress：用户只能操作自己的进度
drop policy if exists "authenticated can read course_progress" on course_progress;
drop policy if exists "authenticated can insert course_progress" on course_progress;
drop policy if exists "authenticated can update course_progress" on course_progress;
create policy "authenticated can read own course_progress" on course_progress
  for select to authenticated
  using (user_id = (select id from public.users where auth_user_id = (select auth.uid())));
create policy "authenticated can insert own course_progress" on course_progress
  for insert to authenticated
  with check (user_id = (select id from public.users where auth_user_id = (select auth.uid())));
create policy "authenticated can update own course_progress" on course_progress
  for update to authenticated
  using (user_id = (select id from public.users where auth_user_id = (select auth.uid())))
  with check (user_id = (select id from public.users where auth_user_id = (select auth.uid())));

-- course_certificates：用户只能读取和插入自己的证书
drop policy if exists "authenticated can read course_certificates" on course_certificates;
drop policy if exists "authenticated can insert course_certificates" on course_certificates;
create policy "authenticated can read own course_certificates" on course_certificates
  for select to authenticated
  using (user_id = (select id from public.users where auth_user_id = (select auth.uid())));
create policy "authenticated can insert own course_certificates" on course_certificates
  for insert to authenticated
  with check (user_id = (select id from public.users where auth_user_id = (select auth.uid())));

-- 写策略：jobs / users / reviews / chat 必须登录（authenticated）
create policy "authenticated can insert jobs" on jobs for insert to authenticated with check (true);
create policy "authenticated can update jobs" on jobs for update to authenticated using (true) with check (true);
create policy "authenticated can delete jobs" on jobs for delete to authenticated using (true);

-- users：注册时 anon 也需能 insert（兼容旧 App 明文注册），登录后 update 必须 authenticated
create policy "public can insert users" on users for insert to anon, authenticated with check (true);
create policy "authenticated can update users" on users for update to authenticated using (true) with check (true);

create policy "authenticated can insert reviews" on reviews for insert to authenticated with check (true);
create policy "authenticated can update reviews" on reviews for update to authenticated using (true) with check (true);
create policy "authenticated can delete reviews" on reviews for delete to authenticated using (true);

create policy "authenticated can insert conversations" on conversations for insert to authenticated with check (true);
create policy "authenticated can update conversations" on conversations for update to authenticated using (true) with check (true);
create policy "authenticated can insert messages" on messages for insert to authenticated with check (true);

create policy "authenticated can insert donation_history" on donation_history for insert to authenticated with check (true);
create policy "authenticated can insert user_points" on user_points for insert to authenticated with check (true);
create policy "authenticated can update user_points" on user_points for update to authenticated using (true) with check (true);
create policy "authenticated can insert points_history" on points_history for insert to authenticated with check (true);

-- 找回密码 RPC：security definer，允许 anon 调用（已校验安全问题才放行）
create or replace function reset_password_by_security_question(p_username text, p_question text, p_answer text, p_new_password text)
returns boolean
language plpgsql
security definer
set search_path = public, auth, extensions
as $$
declare
  v_id bigint;
  v_auth_user_id uuid;
begin
  select u.id, coalesce(u.auth_user_id, au.id)
    into v_id, v_auth_user_id
  from public.users u
  left join auth.users au on lower(au.email) = lower(u.email)
  where (u.username ilike p_username or u.email ilike p_username)
    and u.security_question = p_question
    and u.security_answer ilike p_answer
  limit 1;
  if v_id is null then return false; end if;
  update public.users set password = p_new_password where id = v_id;
  if v_auth_user_id is not null then
    update public.users set auth_user_id = v_auth_user_id where id = v_id and auth_user_id is null;
    update auth.users
      set encrypted_password = extensions.crypt(p_new_password, extensions.gen_salt('bf')),
          updated_at = now()
      where id = v_auth_user_id;
  end if;
  return true;
end; $$;
grant execute on function reset_password_by_security_question(text,text,text,text) to anon, authenticated;

-- ---------- 7b. Data API 权限（细粒度授权，贴近正规） ----------
-- 公开读：anon 只能 select 公开表
grant select on categories, course_categories, courses, vouchers, jobs, users, reviews to anon;
grant select, insert, update, delete on categories, vouchers, jobs, users, reviews to authenticated;
-- 课程进度和证书：仅 authenticated
grant select, insert, update on course_progress to authenticated;
grant select, insert on course_certificates to authenticated;
-- 私有表：仅 authenticated
grant select, insert, update, delete on conversations, messages, donation_history, user_points, points_history to authenticated;
grant all on all tables in schema public to service_role;
grant usage on schema public to anon, authenticated;
grant usage, select on all sequences in schema public to anon, authenticated;

-- ---------- 8. Storage：job-images 桶（public 读 + authenticated 上传） ----------
insert into storage.buckets (id, name, public)
  values ('job-images', 'job-images', true)
  on conflict (id) do nothing;

drop policy if exists "public upload job images" on storage.objects;
drop policy if exists "authenticated upload job images" on storage.objects;
create policy "authenticated upload job images"
  on storage.objects for insert to authenticated
  with check (bucket_id = 'job-images');

drop policy if exists "public read job images" on storage.objects;
create policy "public read job images"
  on storage.objects for select to anon, authenticated
  using (bucket_id = 'job-images');

-- ---------- 9. Realtime：让 chat 能订阅（messages + conversations + jobs） ----------
do $$ begin
  alter publication supabase_realtime add table public.messages;
exception when duplicate_object then null; end $$;

do $$ begin
  alter publication supabase_realtime add table public.conversations;
exception when duplicate_object then null; end $$;

do $$ begin
  alter publication supabase_realtime add table public.jobs;
exception when duplicate_object then null; end $$;

-- ============================================================
-- 种子数据（演示用）
-- ============================================================

insert into categories (name, emoji) values
  ('Cleaning Housework', '🧹'),
  ('Delivery Courier', '🛵'),
  ('Digital Marketing', '📱'),
  ('Graphic Design', '🎨'),
  ('Gardening & Outdoor', '🌿'),
  ('Home Repairs', '🔧'),
  ('Moving & Heavy Lifting', '📦'),
  ('Tutoring & Lessons', '📚'),
  ('Event Help', '🎉'),
  ('Cooking & Catering', '🍳'),
  ('Photography & Video', '📸'),
  ('Pet Care', '🐾'),
  ('IT & Programming', '💻'),
  ('Assembly & Furniture', '🛠️');

insert into users (name, username, password, email, security_question, security_answer, bio) values
  ('Ahmad bin Ali', 'ahmad', 'pass1234', 'ahmad@example.com', 'What is your favourite food?', 'nasi lemak', 'House owner in Batu Ferringhi, looking for helpers.'),
  ('Siti Aminah', 'siti', 'pass1234', 'siti@example.com', 'What is your favourite food?', 'rendang', 'Freelance cleaner, available on weekends.'),
  ('Wei Qi', 'weiqi', 'pass1234', 'weiqi@example.com', 'What is your favourite food?', 'dim sum', 'Marketing student, can design social media posts.'),
  ('Ravi Kumar', 'ravi', 'pass1234', 'ravi@example.com', 'What is your favourite food?', 'roti canai', 'Courier rider, delivery anywhere in Penang island.');

insert into jobs (title, price, category, location, state, area, job_type, description, image_color,
                  poster_id, status, require_gps, tools_required, payment_method, language) values
  ('Pet Bathing', 30.49, 'Cleaning Housework', '88, Jalan Batu Ferringhi, 11100 Batu Ferringhi, Pulau Pinang, Malaysia', 'Pulau Pinang', 'Batu Ferringhi', 'onsite', 'Looking for a gentle and friendly individual to help give our dog a complete bath and basic grooming.', 9281651,
   1, 'OPEN', true, 'None (supplies provided)', 'Cash', 'English'),
  ('Kitchen Deep Cleaning', 60.99, 'Cleaning Housework', '12, Lorong Melayu, 10200 George Town, Pulau Pinang, Malaysia', 'Pulau Pinang', 'George Town', 'onsite', 'Need help with a thorough kitchen deep cleaning.', 5533306,
   1, 'IN_PROGRESS', false, 'Cleaning gloves', 'TNG eWallet', 'Bahasa Malaysia'),
  ('Food Delivery (Lunch)', 12.00, 'Delivery Courier', 'Food Street Hawker Centre, 10400 George Town, Pulau Pinang, Malaysia', 'Pulau Pinang', 'George Town', 'onsite', 'Collect 3 lunch orders from the hawker centre and deliver to an office at Gurney Plaza.', 16367141,
   4, 'OPEN', true, '', 'Cash', 'English'),
  ('Social Media Post Design', 45.00, 'Digital Marketing', 'Remote / Online', 'Kuala Lumpur', 'Bukit Bintang', 'remote', 'Create 4 simple promotional posts for a local bakery.', 3756252,
   3, 'OPEN', false, '', 'Bank Transfer', 'Chinese / English'),
  ('Simple Logo Design', 80.00, 'Graphic Design', 'Remote / Online', 'Selangor', 'Petaling Jaya', 'remote', 'Design a simple logo for a small printing shop.', 897547,
   3, 'OPEN', false, 'Adobe Illustrator', 'Online Banking', 'English'),
  ('Garden Weeding', 35.00, 'Cleaning Housework', '45, Jalan Sultan Ahmad Shah, 10050 George Town, Pulau Pinang, Malaysia', 'Pulau Pinang', 'Tanjung Bungah', 'onsite', 'Clear weeds from the front garden and trim the hedge.', 8173378,
   2, 'COMPLETED', true, 'Garden gloves', 'Cash', 'Bahasa Malaysia');

insert into reviews (reviewed_user_id, reviewer_user_id, rating, comment, job_id) values
  (1, 2, 5, 'Very clear instructions, paid on time.', 2),
  (2, 1, 4, 'Good work, but arrived a bit late.', 6),
  (3, 1, 5, 'Quick and creative designs!', 4);

insert into vouchers (brand, title, valid_stores, points_required, value, brand_color, description, rules) values
  ('KFC', 'KFC RM15 Voucher', 'Valid at all KFC outlets nationwide', 675, 'RM 15', 14935083, 'Get RM15 off your next KFC meal.', '{"Valid at all KFC outlets","Minimum purchase RM15","One-time use only","Not valid with other promotions"}'),
  ('McDonald''s', 'McDonald''s RM10 Voucher', 'Valid at all McDonald''s restaurants', 450, 'RM 10', 16763180, 'Enjoy RM10 off your McDonald''s order.', '{"Valid at all McDonald''s outlets","Minimum purchase RM10","One-time use only","Not valid for McDelivery"}'),
  ('Domino''s', 'Domino''s RM20 Voucher', 'Valid at all Domino''s Pizza outlets', 900, 'RM 20', 275361, 'Save RM20 on your Domino''s Pizza order.', '{"Valid at all Domino''s outlets","Minimum purchase RM20","One-time use only","Valid for pickup and delivery"}'),
  ('Pizza Hut', 'Pizza Hut RM18 Voucher', 'Valid at all Pizza Hut restaurants', 810, 'RM 18', 14935083, 'Get RM18 off your Pizza Hut meal.', '{"Valid at all Pizza Hut outlets","Minimum purchase RM18","One-time use only","Not valid with other promos"}');

insert into donation_history (user_id, organization, date, amount) values
  (1, 'Penang Food Aid Foundation', '12 Aug 2026', 'RM 500'),
  (1, 'Children Education Fund', '03 Aug 2026', 'RM 300'),
  (1, 'Flood Relief Community', '15 Jul 2026', 'RM 1,000'),
  (1, 'Old Folks Home Support', '28 Jun 2026', 'RM 200');

-- ============================================================
-- Profile additions
-- ============================================================
-- These statements are also safe when applied to an existing database,
-- but the reset section at the top of this file is not non-destructive.

alter table public.users
  add column if not exists auth_user_id uuid;

create unique index if not exists users_auth_user_id_key
  on public.users (auth_user_id)
  where auth_user_id is not null;

update public.users app_user
set auth_user_id = auth_user.id
from auth.users auth_user
where app_user.auth_user_id is null
  and lower(app_user.email) = lower(auth_user.email);

create or replace view public.public_profiles as
select
  u.id,
  u.name,
  u.username,
  u.bio,
  u.avatar_url,
  u.region,
  u.skills,
  u.show_avatar,
  case when u.auth_user_id = (select auth.uid()) or u.show_email then u.email else '' end as email,
  case when u.auth_user_id = (select auth.uid()) or u.show_birthdate then u.birthdate else '' end as birthdate,
  case when u.auth_user_id = (select auth.uid()) or u.show_phone_number then u.phone_number else '' end as phone_number,
  u.show_email,
  u.show_birthdate,
  u.show_phone_number,
  u.created_at
from public.users u;

grant select on public.public_profiles to anon, authenticated;

drop policy if exists "public can read users" on public.users;
drop policy if exists "authenticated can read users" on public.users;
drop policy if exists "authenticated can read own profile row" on public.users;
create policy "authenticated can read own profile row"
  on public.users for select
  to authenticated
  using (auth_user_id = (select auth.uid()));

drop policy if exists "authenticated can update users" on public.users;
drop policy if exists "authenticated can update own profile" on public.users;
create policy "authenticated can update own profile"
  on public.users for update
  to authenticated
  using (auth_user_id = (select auth.uid()))
  with check (auth_user_id = (select auth.uid()));

create table if not exists public.profile_activities (
  id bigserial primary key,
  user_id bigint not null references public.users(id) on delete cascade,
  body text not null default '' check (char_length(body) <= 2000),
  photo_url text not null default '',
  created_at timestamptz not null default now(),
  constraint profile_activities_has_content check (char_length(trim(body)) > 0 or photo_url <> '')
);

create index if not exists profile_activities_user_created_idx
  on public.profile_activities (user_id, created_at desc);

alter table public.profile_activities enable row level security;
drop policy if exists "public can read profile activities" on public.profile_activities;
drop policy if exists "owners can insert profile activities" on public.profile_activities;
drop policy if exists "owners can delete profile activities" on public.profile_activities;
create policy "public can read profile activities"
  on public.profile_activities for select
  to anon, authenticated using (true);
create policy "owners can insert profile activities"
  on public.profile_activities for insert
  to authenticated
  with check (user_id = (select id from public.users where auth_user_id = (select auth.uid())));
create policy "owners can delete profile activities"
  on public.profile_activities for delete
  to authenticated
  using (user_id = (select id from public.users where auth_user_id = (select auth.uid())));

grant select on public.profile_activities to anon, authenticated;
grant insert, delete on public.profile_activities to authenticated;
grant usage, select on sequence public.profile_activities_id_seq to authenticated;

insert into storage.buckets (id, name, public)
values ('profile-activity-images', 'profile-activity-images', true)
on conflict (id) do nothing;

drop policy if exists "public can read profile activity images" on storage.objects;
create policy "public can read profile activity images"
  on storage.objects for select
  to anon, authenticated
  using (bucket_id = 'profile-activity-images');

drop policy if exists "owners can upload profile activity images" on storage.objects;
create policy "owners can upload profile activity images"
  on storage.objects for insert
  to authenticated
  with check (
    bucket_id = 'profile-activity-images'
    and (storage.foldername(name))[1] = 'activities'
    and (storage.foldername(name))[2] = (
      select id::text from public.users where auth_user_id = (select auth.uid())
    )
  );

notify pgrst, 'reload schema';

-- 课程分类
insert into course_categories (name, emoji) values
  ('Housekeeping', '🧹'),
  ('Caregiving', '👶'),
  ('Delivery & Transport', '🛵'),
  ('Gardening', '🌿'),
  ('Digital Literacy & Applied Technology', '💻'),
  ('Soft Skills & Professional Ethics', '💬');

-- 课程
insert into courses (category_id, title, emoji, lessons, duration, description) values
  -- Housekeeping
  (1, 'Basic Cleaning Techniques', '🧹', 8, '2h 30m', 'Learn professional home cleaning methods.'),
  (1, 'Kitchen Deep Cleaning', '🧹', 6, '2h', 'Master kitchen deep cleaning techniques.'),
  (1, 'Laundry & Ironing Basics', '🧹', 5, '1h 30m', 'Proper laundry and ironing skills.'),
  (1, 'Advanced Cleaning & Specialised Surface Care', '🧹', 10, '4h', 'Advanced cleaning techniques for marble, wood, glass and other special surfaces.'),
  (1, 'Professional Organising & Decluttering', '🧹', 8, '3h', 'Professional tidying and space organisation methods (KonMari, etc).'),
  -- Caregiving
  (2, 'Elderly Care Fundamentals', '👶', 10, '4h', 'Essential skills for elderly care.'),
  (2, 'Pet Grooming & Care', '🐾', 7, '2h 45m', 'How to groom and care for pets.'),
  (2, 'Professional Confinement Nanny & Infant Care', '👶', 12, '5h', 'Newborn care, breastfeeding support, and confinement practices.'),
  (2, 'Elderly & Dementia Care Certification', '👶', 14, '6h', 'Specialised care for elderly patients including dementia and Alzheimer''s.'),
  (2, 'First Aid & CPR Certification', '🏥', 8, '3h', 'Basic first aid, CPR, and emergency response certification.'),
  (2, 'Food Safety & Hygiene Certification', '🍽️', 6, '2h', 'Food handling, hygiene standards, and safety regulations.'),
  -- Delivery & Transport
  (3, 'Food Delivery Safety', '🛵', 5, '1h 15m', 'Safety guidelines for food delivery riders.'),
  (3, 'Navigation & Route Planning', '🗺️', 6, '2h', 'Optimize your delivery routes.'),
  -- Gardening
  (4, 'Garden Maintenance Basics', '🌿', 7, '2h 15m', 'Maintain and beautify gardens.'),
  (4, 'Indoor Plant Care', '🌱', 5, '1h 45m', 'Keep indoor plants healthy and thriving.'),
  -- Digital Literacy
  (5, 'Smart Home Operation & Troubleshooting', '💻', 8, '3h', 'Operate and troubleshoot smart home devices like speakers, cameras, and appliances.'),
  (5, 'Digital Bookkeeping & Management', '📖', 6, '2h 30m', 'Use digital tools for expense tracking, invoicing, and financial management.'),
  (5, 'Gig Platform Ordering & Operations', '📱', 7, '2h 45m', 'How to accept, manage and complete orders on gig economy platforms.'),
  -- Soft Skills
  (6, 'Customer Service Excellence', '💬', 6, '2h', 'Communicate professionally with clients.'),
  (6, 'Workplace Communication & Etiquette', '💬', 5, '1h 45m', 'Professional communication and workplace manners.'),
  (6, 'Time Management & Productivity', '⏰', 6, '2h', 'Manage your time effectively and boost productivity.'),
  (6, 'Basic Foreign Language & Dialect', '🌍', 10, '4h', 'Learn essential phrases in English, Mandarin, Malay or other languages for daily work.');

-- ============================================================
-- 完成后在 Dashboard 复制两样东西填进 App：
--   Project URL  →  app/src/main/java/com/example/microjob/data/SupabaseConfig.kt
--   anon public key  →  同上（用 anon key 而不是 secret key：
--                        密钥会进 GitHub，RLS 策略已开启，anon 最安全）
-- ============================================================
