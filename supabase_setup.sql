-- ============================================================
-- MicroJob — Supabase 建表脚本（2026-08 定稿・完整版・可重复运行）
-- 在 Supabase Dashboard → SQL Editor 里整体运行一次即可。
-- 覆盖：6 张表 + RLS 策略 + Data API GRANT + Storage 桶 + Realtime。
-- 开头 DROP 旧表；每个 policy 前有 drop if exists，重复跑不会报错。
-- ============================================================

-- ---------- 0. 清理旧表（重建全套） ----------
drop table if exists messages;
drop table if exists conversations;
drop table if exists reviews;
drop table if exists users;
drop table if exists jobs;
drop table if exists categories;

-- ---------- 1. 分类 ----------
create table categories (
  id serial primary key,
  name text not null,
  emoji text not null
);

-- ---------- 2. 用户（对齐 User.kt） ----------
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
  comment text not null default '',
  job_id bigint,
  created_at timestamptz not null default now()
);

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

-- ---------- 6. RLS 策略（demo：anon token 全表读写） ----------
alter table categories   enable row level security;
alter table jobs         enable row level security;
alter table users        enable row level security;
alter table reviews      enable row level security;
alter table conversations enable row level security;
alter table messages     enable row level security;

drop policy if exists "public read categories"   on categories;
create policy "public read categories"   on categories   for select using (true);

drop policy if exists "public read jobs"   on jobs;
create policy "public read jobs"         on jobs         for select using (true);

drop policy if exists "public read users"   on users;
create policy "public read users"        on users        for select using (true);

drop policy if exists "public read reviews"   on reviews;
create policy "public read reviews"      on reviews      for select using (true);

drop policy if exists "public read conversations"   on conversations;
create policy "public read conversations" on conversations for select using (true);

drop policy if exists "public read messages"   on messages;
create policy "public read messages"     on messages     for select using (true);

drop policy if exists "public insert jobs"   on jobs;
create policy "public insert jobs"          on jobs          for insert with check (true);

drop policy if exists "public insert users"   on users;
create policy "public insert users"         on users         for insert with check (true);

drop policy if exists "public insert reviews"   on reviews;
create policy "public insert reviews"       on reviews       for insert with check (true);

drop policy if exists "public insert conversations"   on conversations;
create policy "public insert conversations" on conversations for insert with check (true);

drop policy if exists "public insert messages"   on messages;
create policy "public insert messages"      on messages      for insert with check (true);

drop policy if exists "public update jobs"   on jobs;
create policy "public update jobs"          on jobs          for update using (true);

drop policy if exists "public update users"   on users;
create policy "public update users"         on users         for update using (true);

drop policy if exists "public update reviews"   on reviews;
create policy "public update reviews"       on reviews       for update using (true);

drop policy if exists "public update conversations"   on conversations;
create policy "public update conversations" on conversations for update using (true);

drop policy if exists "public delete jobs"   on jobs;
create policy "public delete jobs" on jobs for delete using (true);

-- ---------- 7. Data API 权限（官方要求：给 anon/authenticated 角色授权） ----------
grant select, insert, update, delete on all tables in schema public to anon;
grant select, insert, update, delete on all tables in schema public to authenticated;
grant all on all tables in schema public to service_role;
grant usage on schema public to anon, authenticated;
grant usage, select on all sequences in schema public to anon, authenticated;

-- ---------- 8. Storage：job-images 桶（public）+ 上传/读取策略 ----------
insert into storage.buckets (id, name, public)
  values ('job-images', 'job-images', true)
  on conflict (id) do nothing;

drop policy if exists "public upload job images" on storage.objects;
create policy "public upload job images"
  on storage.objects for insert
  with check (bucket_id = 'job-images');

drop policy if exists "public read job images" on storage.objects;
create policy "public read job images"
  on storage.objects for select
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

-- ============================================================
-- 完成后在 Dashboard 复制两样东西填进 App：
--   Project URL  →  app/src/main/java/com/example/microjob/data/SupabaseConfig.kt
--   anon public key  →  同上（用 anon key 而不是 secret key：
--                        密钥会进 GitHub，RLS 策略已开启，anon 最安全）
-- ============================================================
