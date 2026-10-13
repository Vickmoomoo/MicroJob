# MicroJob

马来西亚本地零工撮合 Android App（大学 assignment 项目），Jetpack Compose + Supabase 后端。

之前真实代码在 `Microjob` 分支，现已合并到 `main`，`main` 为唯一主线。

## Creators

- Ch'ng Wei Qi
- Louis Seah Yik
- Swee Hian A/L Sun Lai
- Jason Choong Jie Sheng

## 功能

- **找活 / 发活**：Home 刷任务流（`jobs` 表），详情页接单，中心 FAB 发任务，支持 onsite / remote、分类、地区、价格（RM）、图片
- **聊天**：Realtime 聊天（`conversations` / `messages`），支持 TEXT / IMAGE / JOB_INVITE / PAYMENT_CARD / REVIEW 类型
- **评价**：完工后互评（`reviews`，0.5~5 星，防重复索引）
- **技能课**：`CourseScreen → CourseDetail → VideoPlayer → TestQuiz`，通过拿证书（`courses` / `course_progress` / `course_certificates`，22 门课 + 6 分类种子数据）
- **Social Impact**：积分（`user_points` / `points_history`）→ Voucher 兑换（KFC / McD 等 4 条种子）→ 捐赠公示（`donation_history`）+ 3 个小游戏赚分（TicTacToe / NumberGuess / MemoryFlip）
- **语音翻译**：独立 `VoiceTranslationScreen`，需 `RECORD_AUDIO` 权限
- **登录**：Supabase Auth + `SessionManager` / SharedPreferences 本地会话，支持注册 + 密保找回（`reset_password_by_security_question` function）

## 技术栈

- 单 Activity + Compose（Material3，Compose BOM 2026.08.00，navigation-compose 2.10.0，coil 2.7.0）
- 后端：Supabase-KT BOM 3.8.0（postgrest + realtime + storage + auth-kt）+ Ktor-OkHttp 3.5.2，无 Retrofit / Room，离线用 `SampleData` + SharedPreferences 兜底
- `compileSdk 37 / minSdk 26 / targetSdk 36`，`applicationId com.example.microjob`，`versionName 1.0`

## 数据库（Supabase）

建表脚本：`supabase_setup.sql`（631 行，直接在 Supabase SQL Editor 跑）。

表（13 + 1 视图）：

- 交易：`categories`（14 分类）、`users`、`jobs`、`reviews`、`conversations`、`messages`（唯一有 FK 的是 `messages.conversation_id → conversations.id`，其余靠逻辑关联）
- 课程：`course_categories`（6 条）、`courses`（22 条）、`course_progress`、`course_certificates`、`profile_activities`，视图 `public_profiles`（脱敏）
- 积分：`donation_history`、`vouchers`（4 条）、`user_points`、`points_history`

其它：全表 RLS（公开读分类/课程/voucher/jobs，私密表本人限定）；Function `reset_password_by_security_question`；Storage public bucket `job-images`、`profile-activity-images`；Realtime 订阅 `messages, conversations, jobs`。

注意：`SupabaseConfig.kt` 内含当前项目的 URL + publishable key，fork 后请换成自己的。

## 目录结构

```
app/src/main/java/com/example/microjob/
  MainActivity.kt
  model/      Job/User/Chat/Review/Course/SocialImpact/...
  data/       Supabase*Repository + Local*Repository + RepositoryProvider/SessionManager/SupabaseConfig
  viewmodel/  Auth/Home/PostJob/JobDetail/Chat/Profile/Review/Course/SocialImpact/Translation
  ui/
    navigation/MicroJobApp.kt（Scaffold + 30+ 路由）/ MicroJobRoutes.kt
    screens/home/detail/post/login/messages/profile/reviews/translation/
    theme/
supabase_setup.sql
```

## 运行

1. Android Studio（AGP 9.2.1，Kotlin 2.4.10）打开本目录
2. Supabase 建项目 → 跑 `supabase_setup.sql` → 把 URL / key 填进 `data/SupabaseConfig.kt`
3. `./gradlew assembleDebug` 或点 Run，minSdk 26 真机/模拟器均可
4. 无后端时 App 会自动切 `Local*Repository` + `SampleData`，不白屏

## 分支 / 发版

- `main`：主线（已合并原 `Microjob` 分支全部历史，`--allow-unrelated-histories`）
- `Microjob`：旧开发分支，保留只读，不再更新
- Release 见 GitHub Releases（v1.0 起）
