-- ============================================================
-- MicroJob — Supabase 建表脚本（2026-08 定稿）
-- 在 Supabase Dashboard → SQL Editor 里整体运行。
-- 如果之前建过旧版表，先运行最下面的 DROP 语句再跑本脚本。
-- ============================================================

-- 分类表
create table categories (
  id serial primary key,
  name text not null,
  emoji text not null
);

-- 工作表（含平台流程 / GPS / 工具 / 支付托管 / 州地区 / 语言字段）
create table jobs (
  id serial primary key,
  title text not null,
  price double precision not null,
  category text not null,
  state text not null,                          -- 州（poster 选择）
  area text not null,                           -- 地区（poster 选择）
  description text not null,
  image_color bigint not null,

  -- 平台流程字段
  poster_id bigint not null default 0,          -- 发布者 id（关联 users.id）
  worker_id bigint,                             -- 接单者 id（null = 还没人接）
  status text not null default 'OPEN',          -- OPEN → IN_PROGRESS → COMPLETED / CANCELLED
  created_at timestamptz not null default now(),
  deadline timestamptz,                         -- null = 无截止

  -- 信任与安全
  require_gps boolean not null default false,   -- poster 可选要求 worker 开定位
  tools_required text not null default '',      -- 所需工具描述（空 = 无要求）

  -- 支付托管（escrow）
  payment_method text not null default 'Cash',  -- Cash / TNG eWallet / Bank Transfer / Online Banking
  payment_status text not null default 'ESCROWED', -- ESCROWED → RELEASED / REFUNDED

  -- 曝光与展示
  boost_until timestamptz,                      -- 曝光券到期（null = 无 boost）
  currency text not null default 'RM',
  language text not null default ''             -- 推荐沟通语言
);

-- 用户表（一个用户同时是 poster 和 worker）
create table users (
  id bigserial primary key,
  name text not null,
  bio text not null default '',
  avatar_url text not null default '',
  created_at timestamptz not null default now()
);

-- 评价表（挂在"人"身上，不挂在 job 上）
create table reviews (
  id bigserial primary key,
  reviewed_user_id bigint not null,             -- 被评价的人
  reviewer_user_id bigint not null,             -- 写评价的人
  rating int not null default 5 check (rating between 1 and 5),
  comment text not null default '',
  job_id bigint,                                -- 可选：关联哪笔 job 之后留的
  created_at timestamptz not null default now()
);

-- ============================================================
-- 权限：允许匿名用户读取（anon key 只读）
-- ============================================================
alter table categories enable row level security;
alter table jobs enable row level security;
alter table users enable row level security;
alter table reviews enable row level security;

create policy "public read categories" on categories for select using (true);
create policy "public read jobs" on jobs for select using (true);
create policy "public read users" on users for select using (true);
create policy "public read reviews" on reviews for select using (true);

-- ============================================================
-- 测试数据（可选）
-- ============================================================
insert into categories (name, emoji) values
  ('Cleaning Housework', '🧹'),
  ('Delivery Courier', '🛵'),
  ('Digital Marketing', '📱'),
  ('Graphic Design', '🎨');

insert into users (name, bio) values
  ('Ahmad bin Ali', 'House owner in Batu Ferringhi, looking for helpers.'),
  ('Siti Aminah', 'Freelance cleaner, available on weekends.'),
  ('Wei Qi', 'Marketing student, can design social media posts.'),
  ('Ravi Kumar', 'Courier rider, delivery anywhere in Penang island.');

insert into jobs (title, price, category, state, area, description, image_color,
                  poster_id, status, require_gps, tools_required, payment_method, language) values
  ('Pet Bathing', 30.49, 'Cleaning Housework', 'Pulau Pinang', 'Batu Ferringhi', 'Looking for a gentle and friendly individual to help give our dog a complete bath and basic grooming.', 9281651,
   1, 'OPEN', true, 'None (supplies provided)', 'Cash', 'English'),
  ('Kitchen Deep Cleaning', 60.99, 'Cleaning Housework', 'Pulau Pinang', 'George Town', 'Need help with a thorough kitchen deep cleaning.', 5533306,
   1, 'IN_PROGRESS', false, 'Cleaning gloves', 'TNG eWallet', 'Bahasa Malaysia'),
  ('Food Delivery (Lunch)', 12.00, 'Delivery Courier', 'Pulau Pinang', 'George Town', 'Collect 3 lunch orders from the hawker centre and deliver to an office at Gurney Plaza.', 16367141,
   4, 'OPEN', true, '', 'Cash', 'English'),
  ('Social Media Post Design', 45.00, 'Digital Marketing', 'Kuala Lumpur', 'Bukit Bintang', 'Create 4 simple promotional posts for a local bakery.', 3756252,
   3, 'OPEN', false, '', 'Bank Transfer', 'Chinese / English'),
  ('Simple Logo Design', 80.00, 'Graphic Design', 'Selangor', 'Petaling Jaya', 'Design a simple logo for a small printing shop.', 897547,
   3, 'OPEN', false, 'Adobe Illustrator', 'Online Banking', 'English'),
  ('Garden Weeding', 35.00, 'Cleaning Housework', 'Pulau Pinang', 'Tanjung Bungah', 'Clear weeds from the front garden and trim the hedge.', 8173378,
   2, 'COMPLETED', true, 'Garden gloves', 'Cash', 'Bahasa Malaysia');

insert into reviews (reviewed_user_id, reviewer_user_id, rating, comment, job_id) values
  (1, 2, 5, 'Very clear instructions, paid on time.', 2),
  (2, 1, 4, 'Good work, but arrived a bit late.', 6),
  (3, 1, 5, 'Quick and creative designs!', 4);

-- ============================================================
-- 升级旧表用（仅当之前建过旧版表时才需要）：
-- drop table if exists reviews;
-- drop table if exists users;
-- drop table if exists jobs;
-- drop table if exists categories;
-- ============================================================
