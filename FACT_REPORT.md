# FACT_REPORT.md

本文件记录项目当前事实。产品路线写入 `AI_PLAN.md`，执行记录写入 `DEV_LOG.md`。

## 更新时间

2026-06-15

## 项目概况

- 项目名称：迹目。
- 类型：Android 单机个人管理 App。
- 包名：`com.jimu.app`。
- 当前主模块：`app`。
- 主要功能：待办、习惯、目标、已完成记录。
- 复盘当前状态：最小闭环已完成；用户可从首页进入今日复盘页，保存后回到首页查看摘要，并再次进入修改今日复盘。

## 当前技术事实

- 语言：Kotlin。
- UI：Jetpack Compose。
- 数据库：Room / SQLite。
- 导航：Navigation Compose。
- 语音识别：Android 系统 `SpeechRecognizer`。
- 任务语音解析：本地规则解析 `MockTaskParseRepository`。
- 复盘数据层：`daily_reviews` / `ReviewEntity` / `ReviewDao` / `ReviewRepository`。
- 复盘界面层：首页“今日复盘”卡片 / `ReviewScreen` / `ReviewViewModel`。
- 当前未发现 Java 源文件。
- 当前未发现 XML layout 页面。

## 构建环境事实

- Gradle Wrapper：`9.3.1`。
- 项目 Gradle daemon 配置要求 Java 21。
- 命令行默认 Java 曾检测为 Java 8，不适合直接构建本项目。
- 本机已准备可用 JDK 21：

```text
C:\Users\Evan\.jdks\zulu21.50.19-ca-jdk21.0.11-win_x64\zulu21.50.19-ca-jdk21.0.11-win_x64
```

- 如果 Gradle 自动下载 JDK 21 失败，可临时设置 `JAVA_HOME` 指向上述路径。

## 当前构建状态

最近一次 Debug 构建命令：

```powershell
.\gradlew.bat assembleDebug
```

最近一次构建结果：

```text
BUILD SUCCESSFUL in 17s
```

APK 输出路径：

```text
F:\jimuapp\app\build\outputs\apk\debug\app-debug.apk
```

## 当前代码事实

- `app/src/main/java/com/jimu/app/ui/home/HomeScreen.kt` 中 `VoiceInputSheet(` 当前只剩 1 处调用。
- T1 已完成：首页重复语音弹窗调用已清理。
- T2 已完成：目标语音新增已改为独立 `GoalDraft` / `GoalReview`。
- 目标语音新增不再复用 `HabitDraft` / `HabitReview`。
- T2 commit：`36de302`。
- T3 已完成：习惯支持取消今日打卡。
- T3 commit：`ad741b6`。
- T3 验证：`assembleDebug` 已通过。
- T4 已完成：Room 数据安全最小整改。
- T4 commit：`0b85988`。
- T4 已建立 Room version 4 schema 基线：`app/schemas/com.jimu.app.data.local.AppDatabase/4.json`。
- 已移除 Room 破坏性迁移 fallback。
- T4 验证：`assembleDebug` 已通过。
- T5 已完成：复盘数据层与 Room 4 到 5 迁移。
- T5 commit：`3b26779`。
- `AppDatabase` 当前为 `version = 5`，并已设置 `exportSchema = true`。
- T5 已建立 Room version 5 schema：`app/schemas/com.jimu.app.data.local.AppDatabase/5.json`。
- T5 新增 `daily_reviews` 表。
- T5 新增 `MIGRATION_4_5`，只创建 `daily_reviews` 表，不修改旧 5 张表。
- `JimuApp` 当前已注册 `.addMigrations(MIGRATION_4_5)`，并暴露 `reviewRepository`。
- T5 验证：`testDebugUnitTest` 和 `assembleDebug` 已通过。
- T6 已完成：复盘最小闭环。
- T6 commit：`6c6237e`。
- T6 已新增 `ReviewViewModel` / `ReviewScreen` / 首页“今日复盘”卡片 / `Routes.Review` 非 tab 路由。
- T6 保存规则：至少“做得好的事”非空才允许保存；`mood = null`，`completedTaskSnapshot = 0`，`checkedHabitSnapshot = 0`。
- T6 未改 `ReviewEntity` / `ReviewDao` / `ReviewRepository`，未改 `AppDatabase` version，未写 migration，未新增第 6 个底部 tab。
- T6 验证：`testDebugUnitTest` 和 `assembleDebug` 已通过。
- T6 已在真实手机完成手测。
- T7 已完成：待办到期提醒技术验证，已真机手测通过。
- T7 新增 Android 13+ `POST_NOTIFICATIONS` 权限申请、高优先级通知 channel、`TaskReminderReceiver`、`TaskReminderRestoreReceiver`、`TaskReminderScheduler`、`TaskReminderNotifier`、`TaskReminderPlan`、`TaskReminderIds` 和 `TaskReminderAlarmPolicy`。
- T7 真机准点性验证发现非精确 `AlarmManager.setAndAllowWhileIdle` 对 19:12 待办延迟到 19:13:29 投递。
- T7 当前声明 `SCHEDULE_EXACT_ALARM`，优先使用 `setExactAndAllowWhileIdle`；若系统未授予精确闹钟特殊访问，则降级 `setAndAllowWhileIdle` 并在用户主动安排提醒时引导到系统授权页。未使用 `USE_EXACT_ALARM`。
- T7 通知 channel id 当前为 `task_due_reminders_v3`：旧 channel 创建后声音/重要性被系统锁定、代码改不动，升版强制重建带声音的高优先级 channel，并显式设置默认通知音、`AudioAttributes` 和震动模式。
- T7 当前让待办新增/语音新增/编辑/顺延时安排提醒，待办完成或删除时取消提醒。
- T7 当前支持应用启动时重排未来提醒，并监听 `BOOT_COMPLETED` / `MY_PACKAGE_REPLACED` / 精确闹钟授权变化后重排未来提醒。
- T7 reminder id 口径统一：`TaskReminderIds` 直接用 task id，超 Int 范围返回 null，schedule/cancel 一致。
- T7 未改 Room schema，未写 migration，未升级工具链，未扩展到习惯/目标。
- T7 验证：`testDebugUnitTest` 和 `assembleDebug` 已通过；真机手测通过——打开系统“闹钟和提醒”授权 + 关闭电池智能优化 + 通知音量非 0 后，后台/锁屏到点精准触发，声音正常，系统静音时同步静音。
- 当前下一步：交给 Opus 4.8 判断 T8 复盘增强切分（优先历史复盘列表）。

## 当前数据库安全状态

- 已有 Room version 4 和 version 5 schema 基线。
- 已有第一条正式 migration：`MIGRATION_4_5`。
- 未来数据库结构变更必须写 migration。
- 当前未做 `habit_records(habitId, recordDate)` 唯一约束。
- 当前未做子表外键。
- 当前未做索引。
- 当前未做删除习惯/目标的显式事务。
- 当前未处理 `insertHabitRecord` 的 `REPLACE + autoGenerate` 去重问题。
- 当前未做历史脏数据清理。

## 当前已知风险

- Room 已完成 schema 基线、破坏性迁移 fallback 移除和首条正式 migration；后续数据库结构变更必须写 migration。
- 数据层仍有唯一约束、外键、索引、显式事务和历史脏数据清理等后续安全债。
- v4 旧库覆盖安装到 v5 的设备/模拟器回归尚未执行；当前限制是命令行 `adb` 不在 PATH。该限制不影响代码提交，但后续需要用 Android Studio 或配置 adb 后补测。
- 复盘历史列表、mood 输入、真实快照和复盘删除 UI 尚未实现。
- 真实快照口径未定：`TaskEntity` 无 `completedAt`，`updatedAt` 会被完成/回退/编辑/改期污染，做待办完成数快照需先定口径（可能涉及 schema 变更）。
- T7 提醒已真机手测通过；但准点触发依赖三项设备侧条件——精确闹钟授权、关闭电池智能优化、通知音量非 0。
- T7 非精确降级路径（未授予精确闹钟特殊访问时）不承诺严格准点。
- 部分国产 ROM 的电池策略可能压制后台触发，需用户手动关闭电池优化或加白名单；当前未在代码中引导该项。
- 高优先级 channel 仍可能受厂商系统的横幅/悬浮通知开关影响。
- 核心逻辑测试仍待后续 T9 补齐。
