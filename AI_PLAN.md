# AI_PLAN.md

本文件用于记录 Opus 4.8 对“迹目”的产品决策和开发路线。当前是初版，只做轻量规划，不做过度设计。

## 产品定位

迹目是一个本地优先的个人推进工具，核心目标不是做复杂团队协作，而是帮助一个人把日程、习惯、目标和复盘串起来。

它应该像一个随身的“推进面板”：今天做什么、习惯有没有坚持、目标推进到哪一步、完成后能不能回看。

## 已确定的产品决策

- 先做单人本地 App，不做账号、云同步、团队协作。
- 继续使用当前 Android 原生技术栈：Kotlin、Compose、Room、Navigation Compose。
- 首页保留为总览入口，重点展示今日待处理、已完成数量、当前目标进度。
- 待办保持“逾期仍保留”的逻辑，适合真实生活里任务会延期的情况。
- 习惯先按“每天一次打卡”处理，不急着支持复杂频率。
- 目标采用“目标 + 步骤 + 进度”的模型，比单纯写一个目标更可执行。
- 语音作为快速录入入口，先服务待办、习惯、目标，不急着做复杂 AI 助手。
- 复盘是下一阶段关键能力，但先做最小可用版本。

## 当前已经具备的基础

- 底部 5 个页面：首页、待办、习惯、目标、已完成。
- Room 已有 5 张表：tasks、habits、habit_records、goals、goal_steps。
- 待办支持新增、编辑、删除、完成、延期、日期和时间。
- 习惯支持新增、编辑、删除、今日打卡、连续天数和累计次数。
- 目标支持新增、编辑、删除、步骤管理和进度统计。
- 已完成页支持查看完成记录和回退待办状态。
- 首页有语音新增入口。
- Room 已从 version 4 迁移到 version 5，已建立 schema 基线并写入第一条正式 migration。
- 复盘数据层已具备：`daily_reviews` 表、`ReviewEntity`、`ReviewDao`、`ReviewRepository`。
- 复盘最小闭环已具备：首页“今日复盘”卡片、今日复盘编辑页、保存后首页显示摘要、再次进入预填修改。

## 近期路线

### 1. 先整理基础可靠性

目标：让项目更适合继续开发，不先堆新功能。

- T1：已完成 - 修复首页重复 `VoiceInputSheet`，`assembleDebug` 已通过。
- T2：已完成 - 目标语音用独立状态，不再复用 `HabitReview`。
- T2.5：暂时跳过 - 不在 T3 前升级 Android Gradle Plugin。
  - 当前 `assembleDebug` 已通过。
  - AGP 8.5.1 + Gradle 9.3.1 虽然存在版本矩阵错配，但当前可用。
  - AGP 升级会牵涉 Kotlin / KSP / Compose compiler / plugin 迁移。
  - 不应在 T3 / T4 之前引入构建工具链风险。
- T3：已完成 - 习惯支持取消今日打卡，commit `ad741b6`，`assembleDebug` 已通过。
- T4：已完成 - Room 数据安全最小整改，commit `0b85988`，已建立 Room version 4 schema 基线，`exportSchema = true`，已移除破坏性迁移 fallback，`assembleDebug` 已通过。
- T5：已完成 - 复盘数据层与 Room 4 到 5 迁移，commit `3b26779`，新增 `daily_reviews` 表、`MIGRATION_4_5`、`ReviewRepository`，`testDebugUnitTest` 和 `assembleDebug` 已通过。

T5 后当前数据库安全状态：

- 已有 Room version 4 和 version 5 schema。
- 已有第一条正式 migration：`MIGRATION_4_5`，只创建 `daily_reviews` 表，不修改旧表。
- 未来数据库结构变更必须写 migration，不再依赖破坏性删库。
- 暂未处理 `habit_records(habitId, recordDate)` 唯一约束、子表外键、索引、删除习惯/目标的显式事务、`insertHabitRecord` 去重问题和历史脏数据清理。
- v4 旧库覆盖安装到 v5 的设备/模拟器回归尚未补测；当前记录的限制是命令行 `adb` 不在 PATH。

### 2. 做复盘最小版本

目标：让“迹目”真正覆盖计划到回看的闭环。

当前状态：

- T5：已完成复盘数据层，commit `3b26779`。
- T6：已完成复盘最小闭环，commit `6c6237e`，新增首页入口、今日复盘编辑页、今日复盘 ViewModel 与表单状态测试，`testDebugUnitTest` 和 `assembleDebug` 已通过。
- T6 已在真实手机完成手测。

最小版本只需要：

- 新增复盘数据表。已在 T5 完成数据层。
- 支持写一条今日复盘。已在 T6 完成。
- 字段先保持简单：日期、做得好的事、遇到的问题、明日重点。已在 T6 完成。
- 在 UI 中提供入口。已在 T6 通过首页“今日复盘”卡片进入，不加入底部 tab。

暂时不做历史复盘列表、mood 输入、真实快照、复盘删除、复杂模板、AI 总结、图表分析。

### 3. 做提醒能力的技术验证

目标：验证待办到期提醒是否可行。

- 先选一个最简单的提醒路径。
- 明确 Android 版本权限要求。
- 先支持待办提醒，不扩展到习惯和目标。
- 不急着做重复提醒、智能提醒、复杂通知分类。
- T7：已完成 - 待办到期提醒技术验证，已真机手测通过。
  - 已接入 Android 13+ `POST_NOTIFICATIONS` 运行时权限、高优先级通知 channel、`BroadcastReceiver` 和待办新增/编辑/完成/删除时的提醒安排/取消。
  - 真机准点性验证发现非精确 `AlarmManager.setAndAllowWhileIdle` 对 19:12 待办延迟到 19:13:29 投递，无法承诺严格准点；因此改为优先 `setExactAndAllowWhileIdle`，无 `SCHEDULE_EXACT_ALARM` 特殊访问时降级到 `setAndAllowWhileIdle`，并在用户主动安排提醒时引导到系统授权页。
  - 已新增启动时未来提醒重排、`BOOT_COMPLETED` / `MY_PACKAGE_REPLACED` / 精确闹钟授权变化后的未来提醒重排。
  - 已统一 reminder id 口径（`TaskReminderIds` 直接用 task id，超 Int 范围返回 null），避免 schedule/cancel 使用分散的 `hashCode()` 规则。
  - 声音修复：旧通知 channel 一旦创建，其重要性/声音/震动会被系统锁定、代码改不动；为强制重建带声音的高优先级 channel，channel id 升级到 `task_due_reminders_v3`，并显式设置默认通知音、`AudioAttributes` 和震动模式。
  - 未改 Room schema，未写 migration，未升级工具链，未扩展到习惯/目标。
  - `testDebugUnitTest` 和 `assembleDebug` 已通过。
  - 真机手测结论：打开系统“闹钟和提醒”授权 + 关闭电池智能优化 + 通知音量非 0 后，后台/锁屏到点精准触发，声音正常，系统静音时同步静音（走标准通知音量通道，行为正确）。
  - 已知限制：非精确降级路径不承诺严格准点；部分国产 ROM 的电池策略可能压制后台触发，需用户手动关闭电池优化或加白名单，当前未在代码中引导该项。

### 4. 后续技术债：构建工具链升级

目标：在 MVP 稳定后，单独处理 AGP / Kotlin / KSP / Compose 构建工具链升级。

- 当前不在 T3 / T4 之前升级 Android Gradle Plugin。
- 后续升级应作为独立技术债任务处理，避免把构建工具链风险混入功能开发。
- 升级时需要统一评估 AGP、Kotlin、KSP、Compose compiler / plugin 的版本兼容关系。

## 暂不做的事情

- 不做账号系统。
- 不做云同步。
- 不做社交、打卡排行、团队协作。
- 不做复杂数据分析大屏。
- 不引入大模型 API 作为核心依赖。
- 不大改 UI 风格。
- 不为了“架构漂亮”做大规模重构。

## 推荐下一步

优先顺序：

1. T7 已收口（待办提醒技术验证，真机手测通过）。下一步交给 Opus 4.8 判断复盘增强（T8）切分：优先做历史复盘列表（纯 UI + 已有 `observeAllReviews()`，不碰 schema）。
2. 真实快照单独立项、先定口径：当前 `TaskEntity` 无 `completedAt`，`updatedAt` 会被完成/回退/编辑/改期污染，无法可靠表达“今天完成几件待办”；若要做需先决定是否新增 `completedAt`（属 schema 变更 + migration）。习惯快照口径较清晰（`habit_records.recordDate`）。
3. 复盘其余增强（mood 输入、复盘删除 UI）继续排在历史列表之后。
4. 提醒后续增强（电池白名单引导、习惯/目标提醒、重复提醒）待 MVP 稳定后再评估。
5. 数据层安全债（唯一约束、外键、索引、显式事务、`insertHabitRecord` 去重、历史脏数据清理）继续作为独立任务保留。
6. MVP 稳定后：单独处理 AGP / Kotlin / KSP / Compose 构建工具链升级。

这样推进的好处是：T1–T7 已经收口，Room migration 通道已经首次跑通，复盘 MVP 闭环已具备并真机手测通过，待办提醒技术验证已真机确认可行；下一轮只做复盘增强，不把真实快照口径、数据层安全债和构建工具链升级混入同一轮。
