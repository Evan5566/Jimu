# DEV_LOG.md

本文件记录每次实际做了什么。长期规则不要写在这里，长期规则写入 `CLAUDE.md`。

## 2026-06-12 - T1 清理首页重复的 VoiceInputSheet

### 任务范围

只执行 T1：清理首页重复的 `VoiceInputSheet`。不做 T2，不改目标语音状态，不改导航，不改 Gradle 配置。

### 修改文件

- `app/src/main/java/com/jimu/app/ui/home/HomeScreen.kt`

### 修改内容

- 阅读 `HomeScreen.kt` 后确认：同一个 `if (showVoiceSheet) { VoiceInputSheet(...) }` 在文件中出现了两次。
- 删除第二段重复的 `VoiceInputSheet` 调用。
- 保留第一段原有语音弹窗逻辑，原有回调不变。
- 删除重复块后补回 `HomeScreen` 函数结尾的 `}`。

### 构建过程

第一次运行 `.\gradlew.bat assembleDebug` 时，Gradle 尝试自动下载 Java 21，下载源跳转到 GitHub 后超时。

处理方式：

- 没有修改项目 Gradle 配置。
- 下载并使用本地 Azul JDK 21：

```text
C:\Users\Evan\.jdks\zulu21.50.19-ca-jdk21.0.11-win_x64\zulu21.50.19-ca-jdk21.0.11-win_x64
```

- 临时设置 `JAVA_HOME` 后重新运行构建。

### 验证结果

源码检查：

- `HomeScreen.kt` 中 `VoiceInputSheet(` 只剩 1 处。

构建命令：

```powershell
.\gradlew.bat assembleDebug
```

构建结果：

```text
BUILD SUCCESSFUL in 37s
```

APK 输出路径：

```text
F:\jimuapp\app\build\outputs\apk\debug\app-debug.apk
```

### Android Studio 模拟器验证步骤

1. 用 Android Studio 打开 `F:\jimuapp`。
2. 如果 Android Studio 提示 Gradle JDK 问题，把 Gradle JDK 指到：

```text
C:\Users\Evan\.jdks\zulu21.50.19-ca-jdk21.0.11-win_x64\zulu21.50.19-ca-jdk21.0.11-win_x64
```

3. 启动一个 Android 模拟器。
4. 运行 App。
5. 进入首页。
6. 点击右下角语音按钮。
7. 选择“待办 / 习惯 / 目标”任意一个。
8. 确认只出现一个底部语音弹窗，不会叠出两个弹窗。
9. 关闭弹窗后重复打开一次，确认行为仍正常。

### 备注

这次只完成 T1。没有处理目标语音状态、习惯取消打卡、复盘、Room migration、已完成并入待办等后续任务。

## 2026-06-12 - T2 目标语音使用独立状态

### 任务范围

只执行 T2：让“语音新增目标”使用独立的 `GoalReview` / `GoalDraft`，不再复用 `HabitReview` / `HabitDraft`。不做 T3，不做 T4，不改数据库，不改 Gradle 配置，不引入新框架。

### 修改文件

- `app/src/main/java/com/jimu/app/voice/VoiceInputState.kt`
- `app/src/main/java/com/jimu/app/viewmodel/VoiceInputViewModel.kt`
- `app/src/main/java/com/jimu/app/voice/VoiceInputSheet.kt`
- `DEV_LOG.md`

### 修改内容

- 在 `VoiceInputState.kt` 新增 `GoalDraft` 和 `VoiceInputState.GoalReview`。
- 在 `VoiceInputViewModel.kt` 中把 `VoiceInputTarget.GOAL` 分支改为产出 `GoalReview(goalDraft = GoalDraft(...))`。
- 保留 `extractGoalTitle` 和 `normalizeGoalPeriodWords` 原有逻辑，没有修改目标标题清洗规则。
- 在 `VoiceInputSheet.kt` 中新增 `GoalReview` 状态分支和独立的 `GoalReviewContent`。
- 移除 `HabitReviewContent` 里的 `isGoal` 分叉，让它只服务习惯确认页。
- 目标确认页仍保持原有视觉和交互：显示“确认目标”、只编辑目标名称、按钮为“添加目标”。
- 习惯确认页仍显示“确认习惯”、保留备注框、按钮为“添加习惯”。

### 验证结果

源码检查：

- `VoiceInputTarget.GOAL` 分支现在产出 `VoiceInputState.GoalReview`。
- `HabitReview` / `HabitDraft` 当前只保留在习惯流程中。
- 全局搜索未发现 `isGoal` 分叉残留。

构建命令：

```powershell
.\gradlew.bat assembleDebug
```

构建结果：

```text
命令退出码：0
```

APK 输出路径：

```text
F:\jimuapp\app\build\outputs\apk\debug\app-debug.apk
```

### 备注

本次没有执行模拟器手测；建议后续在 Android Studio 中分别验证待办、习惯、目标三条语音新增流程，以及目标语音下的重新识别、取消和错误提示。

## 2026-06-12 - T3 习惯支持取消今日打卡

### 任务范围

只执行 T3：让已打卡习惯可以取消今日打卡。不做 T4，不改 Room schema，不改 database version，不写 migration，不改 Gradle 配置，不引入新框架，不改变 `calculateStreak` 算法和连续打卡语义。

### 修改文件

- `app/src/main/java/com/jimu/app/data/local/dao/HabitDao.kt`
- `app/src/main/java/com/jimu/app/data/repository/HabitRepository.kt`
- `app/src/main/java/com/jimu/app/viewmodel/HabitsViewModel.kt`
- `app/src/main/java/com/jimu/app/ui/habits/HabitsScreen.kt`
- `DEV_LOG.md`

### 修改内容

- 在 `HabitDao` 新增按 `habitId + recordDate` 删除打卡记录的方法。
- 删除逻辑会删除同一习惯同一天的所有匹配记录，用于确保历史重复记录脏数据也能被一次取消干净。
- 在 `HabitRepository` 新增 `uncheckInToday(habit)`，使用与 `checkInToday` 相同的 `LocalDate.now().toString()` 日期口径。
- 保留 `checkInToday` 原有手动 count 防重逻辑，没有修改打卡插入路径。
- 在 `HabitsViewModel` 新增 `uncheckInToday(habit)`，与现有 `checkInToday(habit)` 对称。
- 在 `HabitsScreen` 中解除已打卡按钮的 disabled 状态；未打卡时点击仍打卡，已打卡时点击取消今日打卡。
- 已打卡按钮的 `contentDescription` 改为“取消今日打卡”，图标仍保持已打卡和未打卡两种状态区分。

### 验证结果

构建命令：

```powershell
.\gradlew.bat assembleDebug
```

本次在当前 PowerShell 命令中临时设置了 `JAVA_HOME` 指向本机 JDK 21，没有修改项目配置。

构建结果：

```text
BUILD SUCCESSFUL in 30s
```

提交记录：

```text
ad741b6 feat: support undoing habit check-ins
```

### 备注

- 本次没有执行模拟器手测；建议手动验证“打卡 → 取消 → 再打卡”的可逆流程，以及杀掉 App 重开后的持久化状态。
- 当前 `insertHabitRecord` 使用 `OnConflictStrategy.REPLACE`，但主键是 autoGenerate 的 `id`，不会按 `habitId + recordDate` 业务键天然去重；本次只通过取消时删除所有匹配记录清理同日重复脏数据，不顺手修改插入防重策略。

## 2026-06-13 - T4 Room 数据安全最小整改

### 任务范围

只执行 T4：建立 Room schema 基线并停用破坏性删库。保持数据库版本为 4，不改 Entity / DAO / Repository 业务逻辑，不改 Room 表结构，不写 migration，不升级 Gradle / AGP / Kotlin / KSP / Compose / Room 版本，不处理历史脏数据，不做 T5。

### 修改文件

- `app/src/main/java/com/jimu/app/data/local/AppDatabase.kt`
- `app/src/main/java/com/jimu/app/JimuApp.kt`
- `app/build.gradle.kts`
- `DEV_LOG.md`
- `app/schemas/com.jimu.app.data.local.AppDatabase/4.json`

### 修改内容

- 将 `AppDatabase` 的 `exportSchema` 从 `false` 改为 `true`。
- 保持 `AppDatabase` 的 `version = 4`，实体列表不变。
- 移除 `Room.databaseBuilder` 中的破坏性迁移 fallback 调用，不替换为任何 fallback。
- 在 `app/build.gradle.kts` 中增加 Room KSP schema 输出路径：`room.schemaLocation = $projectDir/schemas`。
- 生成并纳入当前 version 4 的 Room schema JSON。

### 当前数据库安全状态

- 已有 Room version 4 schema 基线。
- 未来数据库结构变更必须写 migration。
- 破坏性迁移 fallback 已移除。
- 本次未做唯一约束、外键、索引、删除习惯/目标的显式事务、`insertHabitRecord` 去重修正和历史脏数据清理。

### 本次明确不处理的后续待办

- `habit_records(habitId, recordDate)` 唯一约束。
- 子表外键。
- 索引。
- 删除习惯/目标的显式事务。
- `insertHabitRecord` 的 `REPLACE + autoGenerate` 去重问题。
- 历史脏数据清理。

### 验证结果

本次在当前 PowerShell 命令中临时设置了 `JAVA_HOME` 指向本机 JDK 21，没有修改项目配置。

构建命令：

```powershell
.\gradlew.bat assembleDebug
```

构建结果：

```text
BUILD SUCCESSFUL in 24s
```

schema 输出路径：

```text
F:\jimuapp\app\schemas\com.jimu.app.data.local.AppDatabase\4.json
```

核查结果：

- `app/schemas/com.jimu.app.data.local.AppDatabase/4.json` 已生成，且未被 `.gitignore` 忽略。
- 源码中未发现破坏性迁移 fallback 调用。
- `AppDatabase` 仍保持 `version = 4`。

提交记录：

```text
0b85988 chore: add Room schema baseline
```

### 下一步

T5：由 Opus 4.8 决策复盘 MVP / 数据层下一步。本次不执行 T5。

## 2026-06-13 - T5 复盘数据层与 Room 4 到 5 迁移

### 任务范围

只执行 T5：新增复盘数据层 `ReviewEntity` / `ReviewDao` / `ReviewRepository`，并完成 Room 数据库从 version 4 到 version 5 的第一次正式迁移。

本次不写 ViewModel，不写 Screen，不加路由，不加底部 tab，不加首页入口。复盘界面与入口留给 T6 / T7。

本次 migration 只创建 `daily_reviews` 表，不修改现有 `tasks`、`habits`、`habit_records`、`goals`、`goal_steps` 五张表，不补唯一约束、外键、索引或历史脏数据清理。

### 修改文件

- `app/src/main/java/com/jimu/app/data/local/entity/ReviewEntity.kt`
- `app/src/main/java/com/jimu/app/data/local/dao/ReviewDao.kt`
- `app/src/main/java/com/jimu/app/data/repository/ReviewRepository.kt`
- `app/src/main/java/com/jimu/app/data/local/AppDatabase.kt`
- `app/src/main/java/com/jimu/app/JimuApp.kt`
- `app/src/test/java/com/jimu/app/data/repository/ReviewRepositoryTest.kt`
- `app/schemas/com.jimu.app.data.local.AppDatabase/5.json`
- `DEV_LOG.md`

### 修改内容

- 新增 `daily_reviews` 表对应的 `ReviewEntity`，字段包括 `reviewDate`、`type`、`summary`、`problems`、`tomorrowFocus`、`mood`、`completedTaskSnapshot`、`checkedHabitSnapshot`、`createdAt`、`updatedAt`。
- `reviewDate` 使用 `String`，口径对齐 `habit_records.recordDate` 的 `LocalDate.now().toString()`。
- `type` 默认值为 `"daily"`，只为后续 weekly 预留字段，本次不实现 weekly 逻辑。
- 新增 `ReviewDao`，支持插入、更新、删除、按 `reviewDate` 查询单条 daily 复盘，以及按 `reviewDate DESC, updatedAt DESC` 观察全部复盘。
- 新增 `ReviewRepository`，封装复盘增删改查，并提供 `saveTodayReview` / `saveDailyReview`。
- `saveDailyReview` 会先按 `reviewDate` 查询已有 daily 复盘；存在则更新，不存在则插入，用 Repository 层逻辑避免一天多条复盘。
- `AppDatabase` 从 `version = 4` 升为 `version = 5`，加入 `ReviewEntity` 和 `reviewDao()`，`exportSchema` 保持 `true`。
- 新增 `MIGRATION_4_5`，只执行 `CREATE TABLE IF NOT EXISTS daily_reviews (...)`。
- `JimuApp` 在 `Room.databaseBuilder` 上注册 `.addMigrations(MIGRATION_4_5)`，并暴露 `reviewRepository`。
- 生成 Room version 5 schema：`app/schemas/com.jimu.app.data.local.AppDatabase/5.json`。

### 测试与验证

先写 `ReviewRepositoryTest` 后运行：

```powershell
.\gradlew.bat testDebugUnitTest --tests com.jimu.app.data.repository.ReviewRepositoryTest
```

预期失败结果：

```text
Unresolved reference: ReviewDao
Unresolved reference: ReviewEntity
Unresolved reference: ReviewRepository
```

实现复盘数据层后运行完整本地单元测试：

```powershell
.\gradlew.bat testDebugUnitTest
```

结果：

```text
BUILD SUCCESSFUL in 10s
```

运行 Debug 构建：

```powershell
.\gradlew.bat assembleDebug
```

结果：

```text
BUILD SUCCESSFUL in 13s
```

核查结果：

- `app/schemas/com.jimu.app.data.local.AppDatabase/5.json` 已生成。
- `5.json` 中包含 `daily_reviews` 表。
- `daily_reviews` 的 `indices` 和 `foreignKeys` 均为空，符合本次不处理旧数据层债务的边界。
- `daily_reviews` 的字段类型与 `MIGRATION_4_5` 中的 `CREATE TABLE` 保持一致。

提交记录：

```text
3b26779 feat: add daily review data layer
```

推送状态：

```text
已推送到 origin/main
```

### 未执行的验证

本轮未执行设备或模拟器上的 v4 旧库覆盖安装回归；当前 PowerShell 中 `adb` 不在 PATH。后续应在 Android Studio 或真实设备上执行：先安装 T4 状态 APK 并写入待办、习惯、目标数据，再覆盖安装 T5 新 APK，确认 App 启动不崩且旧数据保留。

### 下一步

T5 功能 commit 已完成并推送。下一步交给 Opus 4.8 判断复盘界面、入口与后续任务切分；不要把数据层安全债混入复盘界面任务。

## 2026-06-15 - T6 复盘最小闭环

### 任务范围

执行 T6：让用户能从首页进入今日复盘页，填写“做得好的事 / 遇到的问题 / 明日重点”并保存；再次进入能预填并修改今天已写内容。

本次不加第 6 个底部 tab，不做历史复盘列表，不做 mood 输入，不接真实快照，不做复盘删除 UI，不改 `ReviewEntity` / `ReviewDao` / `ReviewRepository`，不改数据库版本，不写 migration，不处理数据库安全债，不做工具链升级。

### 修改文件

- `app/src/main/java/com/jimu/app/viewmodel/ReviewViewModel.kt`
- `app/src/main/java/com/jimu/app/ui/review/ReviewScreen.kt`
- `app/src/main/java/com/jimu/app/viewmodel/HomeViewModel.kt`
- `app/src/main/java/com/jimu/app/ui/home/HomeScreen.kt`
- `app/src/main/java/com/jimu/app/navigation/Routes.kt`
- `app/src/main/java/com/jimu/app/navigation/AppNavHost.kt`
- `app/src/main/java/com/jimu/app/ui/components/JimuBottomBar.kt`
- `app/src/test/java/com/jimu/app/viewmodel/ReviewViewModelTest.kt`
- `DEV_LOG.md`

### 修改内容

- 新增 `ReviewViewModel` 和 `ReviewFormUiState`。
- 进入复盘页时使用 `LocalDate.now().toString()` 查询今日复盘并预填。
- 暴露 `summary`、`problems`、`tomorrowFocus` 输入状态，并提供对应更新方法。
- 保存时调用 `saveTodayReview(summary, problems, tomorrowFocus, mood = null, completedTaskSnapshot = 0, checkedHabitSnapshot = 0)`。
- 保存规则采用“做得好的事”非空才允许保存；三个字段全空不会落库。
- 新增 `ReviewScreen`，包含只读今日日期、三个多行输入框和保存按钮。
- 首页新增“今日复盘”卡片，未写时显示引导文案，已写时显示已记录状态和摘要。
- `HomeViewModel` 注入 `ReviewRepository`，通过 `observeAllReviews()` 派生今日复盘卡片状态。
- `Routes` 新增 `Review` 非 tab 路由，`AppNavHost` 新增复盘页 composable。
- 底部 tab 列表仍保持 5 个；`JimuBottomBar` 只为 sealed route 穷尽性补齐 `Routes.Review` 分支，不把 Review 加入 tabs。
- 新增 `ReviewViewModelTest`，覆盖“summary 为空不能保存”“summary 非空可保存”“已有复盘可预填”的表单状态规则。

### 验证结果

先写测试后运行单测，预期失败：

```powershell
.\gradlew.bat testDebugUnitTest --tests com.jimu.app.viewmodel.ReviewViewModelTest
```

失败原因：

```text
Unresolved reference: ReviewFormUiState
```

实现 `ReviewFormUiState` 后再次运行：

```powershell
.\gradlew.bat testDebugUnitTest --tests com.jimu.app.viewmodel.ReviewViewModelTest
```

结果：

```text
BUILD SUCCESSFUL in 9s
```

第一次运行 Debug 构建时，发现 `JimuBottomBar` 的 sealed `when` 未覆盖新增 `Routes.Review` 分支。补齐分支后再次运行：

```powershell
.\gradlew.bat assembleDebug
```

结果：

```text
BUILD SUCCESSFUL in 20s
```

代码提交前再次运行完整本地单元测试：

```powershell
.\gradlew.bat testDebugUnitTest
```

结果：

```text
BUILD SUCCESSFUL in 4s
```

代码提交前再次运行 Debug 构建：

```powershell
.\gradlew.bat assembleDebug
```

结果：

```text
BUILD SUCCESSFUL in 17s
```

提交记录：

```text
6c6237e feat: add daily review MVP flow
```

推送状态：

```text
已推送到 origin/main
```

### 真实手机手测

T6 已由用户在真实手机完成手测，复盘最小闭环进入可用状态。

### 后续增强

- 历史复盘列表。
- mood 心情输入。
- `completedTaskSnapshot` / `checkedHabitSnapshot` 接真实数据。
- 复盘删除 UI。

## 2026-06-15 - T7 待办到期提醒技术验证代码接入

### 任务范围

执行 T7 的代码层接入：验证待办到期提醒的最小技术链路。

本次只支持待办提醒，不扩展到习惯和目标；不做重复提醒、提前 N 分钟、智能提醒、复杂通知分类；不做完整提醒设置 UI；不改 Room schema，不写 migration，不升级 Gradle / AGP / Kotlin / Compose / Room；不处理复盘真实快照和历史列表。

本轮采用非精确 `AlarmManager.setAndAllowWhileIdle` 路径做最小验证，不默认申请 `SCHEDULE_EXACT_ALARM` 或 `USE_EXACT_ALARM`。T7 的产品语义是“到期附近提醒”，不是承诺严格准点。

本轮不处理设备重启后的提醒重排；如果手机重启，已安排但尚未触发的 alarm 可能丢失，后续正式提醒系统需要单独处理 `BOOT_COMPLETED` 和待办重排策略。

真机初测后通过 `adb shell dumpsys alarm` 和 `adb shell dumpsys notification --noredact` 定位：系统已有 `com.jimu.app.action.TASK_DUE` 的 alarm 触发历史，且 `com.jimu.app` 已经发布过 `task_due_reminders` 通知；问题不在调度链路，而在原通知 channel 为 `IMPORTANCE_DEFAULT`，通常只进入通知栏，不稳定弹出横幅/悬浮提醒。由于 Android 已创建的 channel 不能通过代码直接升高重要性，本轮切换到新的高优先级 channel：`task_due_reminders_v2`。

### 修改文件

- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/jimu/app/MainActivity.kt`
- `app/src/main/java/com/jimu/app/JimuApp.kt`
- `app/src/main/java/com/jimu/app/data/local/dao/TaskDao.kt`
- `app/src/main/java/com/jimu/app/data/repository/TaskRepository.kt`
- `app/src/main/java/com/jimu/app/reminder/TaskReminderPlan.kt`
- `app/src/main/java/com/jimu/app/reminder/TaskReminderNotifier.kt`
- `app/src/main/java/com/jimu/app/reminder/TaskReminderReceiver.kt`
- `app/src/main/java/com/jimu/app/reminder/TaskReminderScheduler.kt`
- `app/src/main/java/com/jimu/app/viewmodel/TasksViewModel.kt`
- `app/src/main/java/com/jimu/app/ui/tasks/TasksScreen.kt`
- `app/src/test/java/com/jimu/app/data/repository/TaskRepositoryTest.kt`
- `app/src/test/java/com/jimu/app/reminder/TaskReminderPlanTest.kt`
- `AI_PLAN.md`
- `FACT_REPORT.md`
- `DEV_LOG.md`

### 修改内容

- 在 `AndroidManifest.xml` 新增 `POST_NOTIFICATIONS` 权限，并声明非导出的 `TaskReminderReceiver`。
- 在 `MainActivity` 中为 Android 13+ 申请通知运行时权限。
- 在 `JimuApp` 初始化待办提醒通知 channel，并暴露 `taskReminderScheduler`。
- 新增 `TaskReminderPlan`，用纯 Kotlin 逻辑判断待办是否需要安排提醒：必须未完成、有未来的 `dueDate`。
- 新增 `TaskReminderNotifier`，负责创建高优先级通知 channel 和展示本地通知。
- 新增 `TaskReminderReceiver`，到期广播触发后展示通知。
- 新增 `TaskReminderScheduler`，用 `AlarmManager.setAndAllowWhileIdle` 安排待办到期提醒，并支持按任务 id 取消提醒。
- 将 `TaskDao.insertTask` 改为返回插入 id。
- 将 `TaskRepository.addTask` 改为返回 `TaskEntity?`，空标题返回 `null`，新增成功返回带 id 的任务。
- 在 `TasksViewModel` 中接入提醒安排/取消：
  - 新增待办和语音新增待办成功后安排提醒。
  - 编辑或顺延待办后重新安排提醒。
  - 完成待办或删除待办时取消提醒。
- 在 `TasksScreen` 中把 `app.taskReminderScheduler` 传入 `TasksViewModelFactory`。

### 测试与验证

先写 `TaskRepositoryTest` 后运行：

```powershell
.\gradlew.bat testDebugUnitTest --tests com.jimu.app.data.repository.TaskRepositoryTest
```

预期失败结果：

```text
Unresolved reference: id
Unresolved reference: title
Unresolved reference: dueDate
Unresolved reference: isCompleted
```

原因：当时 `TaskRepository.addTask` 仍返回 `Unit`，测试证明提醒调度拿不到稳定任务 id。

实现 `TaskDao.insertTask` 返回 id、`TaskRepository.addTask` 返回 `TaskEntity?` 后再次运行：

```powershell
.\gradlew.bat testDebugUnitTest --tests com.jimu.app.data.repository.TaskRepositoryTest
```

结果：

```text
BUILD SUCCESSFUL in 33s
```

再写 `TaskReminderPlanTest` 后运行：

```powershell
.\gradlew.bat testDebugUnitTest --tests com.jimu.app.reminder.TaskReminderPlanTest
```

预期失败结果：

```text
Unresolved reference: TaskReminderPlan
```

实现 `TaskReminderPlan` 后再次运行：

```powershell
.\gradlew.bat testDebugUnitTest --tests com.jimu.app.reminder.TaskReminderPlanTest
```

结果：

```text
BUILD SUCCESSFUL in 10s
```

运行完整本地单元测试：

```powershell
.\gradlew.bat testDebugUnitTest
```

结果：

```text
BUILD SUCCESSFUL in 10s
```

运行 Debug 构建：

```powershell
.\gradlew.bat assembleDebug
```

结果：

```text
BUILD SUCCESSFUL in 7s
```

### 待执行的真机/模拟器手测

T7 涉及 Android 运行时权限、系统调度、锁屏/息屏通知展示，本地 JVM 单测无法覆盖。后续需要在 Android Studio 或真实设备上执行：

1. 安装当前 Debug APK。
2. 首次启动 App 时允许通知权限。
3. 新增一个 1-2 分钟后到期的待办。
4. 回到桌面或锁屏等待。
5. 确认到期附近出现“待办到期”通知。
6. 点击通知，确认能回到 App。
7. 再新增一个未来时间待办后立即标记完成，确认到期时不再弹通知。
8. 再新增一个未来时间待办后删除，确认到期时不再弹通知。
9. 拒绝通知权限后重复新增待办，确认不会崩溃，且不会展示通知。

### 当前结论

- 代码层已经打通待办提醒最小链路。
- 当前路线不依赖精确闹钟权限，不承诺严格准点。
- 当前提醒使用新的 `task_due_reminders_v2` 高优先级 channel；如果设备系统仍关闭该 channel 的横幅/悬浮通知，需要在系统通知设置中手动打开。
- 当前 spike 不处理设备重启后的提醒恢复。
- 运行时权限、锁屏/息屏触发、不同 Android 版本上的实际延迟情况仍需设备手测后再决定正式提醒系统路线。

### 挂账

- 如果产品后续要求严格准点提醒，需要单独评估 `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` 的申请体验和限制。
- 真实快照需要先定义“今天完成几件待办”的口径；当前 `TaskEntity` 没有 `completedAt`，不能用 `updatedAt` 直接统计。
- 复盘历史列表仍单独排期，不混入 T7。

## 2026-06-15 - T7 精确闹钟与恢复收口

### 背景

真机手测发现非精确 `AlarmManager.setAndAllowWhileIdle` 会出现明显延迟：设定 19:12 到期的待办，通知实际在 19:13:29 进入通知栏。结论是提醒链路已打通，但非精确闹钟不能支撑“准点提醒”的产品语义。

本次按 Opus 4.8 评估收口三项问题：

- P0：精确闹钟 + 权限降级。
- P0：应用启动、设备重启、安装替换和精确闹钟授权变化后的未来提醒重排。
- P1：统一 requestCode / notificationId 口径，不再使用分散的 `hashCode()` 规则。

### 修改内容

- 新增 `SCHEDULE_EXACT_ALARM` 和 `RECEIVE_BOOT_COMPLETED` 权限声明。
- `TaskReminderScheduler` 优先使用 `setExactAndAllowWhileIdle`；当系统未授予精确闹钟特殊访问时，降级到 `setAndAllowWhileIdle`。
- 用户主动新增、编辑、顺延或恢复待办提醒时，如果缺少精确闹钟特殊访问，会跳转系统“闹钟和提醒”授权页；后台重排时不弹授权页。
- `TaskReminderScheduler` 在精确闹钟调用处增加 `SecurityException` 兜底，避免 Android 12+ / 14 权限状态异常导致崩溃。
- 新增 `TaskReminderIds`，把支持范围内的 `task.id` 直接映射为 requestCode / notificationId；超出 `Int` 范围时不安排提醒，避免 Long hash 冲突。
- `TaskReminderPlan`、`TaskReminderScheduler.cancel()` 统一使用 `TaskReminderIds`。
- `TaskDao` / `TaskRepository` 新增未来未完成待办查询，用于提醒恢复。
- `JimuApp` 启动后重排未来待办提醒。
- 新增 `TaskReminderRestoreReceiver`，监听 `BOOT_COMPLETED`、`MY_PACKAGE_REPLACED` 和 `SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` 后重排未来待办提醒。
- 新增 `TaskReminderAlarmPolicyTest`、`TaskReminderIdsTest`，并扩展 `TaskRepositoryTest` 覆盖未来提醒查询。

### TDD 验证记录

先写精确闹钟策略、ID 口径和未来提醒查询测试后运行：

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.jimu.app.reminder.TaskReminderAlarmPolicyTest" --tests "com.jimu.app.reminder.TaskReminderIdsTest" --tests "com.jimu.app.data.repository.TaskRepositoryTest"
```

预期失败结果：

```text
Unresolved reference: TaskReminderAlarmPolicy
Unresolved reference: TaskReminderAlarmMode
Unresolved reference: TaskReminderIds
Unresolved reference: getFutureReminderTasks
```

实现后再次运行同一组测试，结果：

```text
BUILD SUCCESSFUL in 22s
```

### 最终本地验证

运行完整本地单元测试：

```powershell
.\gradlew.bat testDebugUnitTest
```

结果：

```text
BUILD SUCCESSFUL in 2s
```

运行 Debug 构建：

```powershell
.\gradlew.bat assembleDebug
```

结果：

```text
BUILD SUCCESSFUL in 16s
```

### 当前结论

- T7 代码层已完成：待办到期提醒、通知权限、精确闹钟优先、非精确降级、取消提醒、启动/重启恢复、ID 口径统一均已接入。
- T7 不使用 `USE_EXACT_ALARM`，避免走不适合待办 App 的 Play 审核捷径。
- T7 不改 Room schema，不写 migration，不升级工具链，不扩展到习惯/目标，不做重复提醒或完整提醒设置 UI。
- Android 13+ 通知权限、Android 12+ / 14 精确闹钟特殊访问、锁屏/息屏准点性和设备重启恢复仍需真机手测收口。

### 真机手测方案

1. 安装当前 Debug APK。
2. 首次启动 App 时允许通知权限。
3. 新增一个 1-2 分钟后到期的待办；如果系统打开“闹钟和提醒”授权页，授予 `迹目` 精确闹钟特殊访问。
4. 回到 App，再新增一个 1-2 分钟后到期的待办。
5. 回到桌面或锁屏等待，确认通知是否在设定时间附近进入通知栏。
6. 点击通知，确认能回到 App。
7. 新增一个未来到期待办后立即标记完成，确认到期时不再弹通知。
8. 新增一个未来到期待办后删除，确认到期时不再弹通知。
9. 关闭精确闹钟特殊访问后重复新增未来到期待办，确认不会崩溃，通知可能延迟但仍走降级链路。
10. 新增一个几分钟后到期的待办后重启手机，重启并解锁后打开 App 或等待系统开机广播，确认未来提醒会被重排并触发。

## 2026-06-15 - T7 通知声音修复与真机验证收口

### 任务范围

承接 T7：定位并修复“到点有通知但无声音”问题，完成真机手测收口。本次只改通知 channel，不改调度逻辑，不改数据库，不写 migration，不升级工具链，不扩展到习惯/目标。

### 问题定位过程

真机分层测试结论：

- 打开系统“闹钟和提醒”授权后，前台/后台均可准点收到通知（`19:12 → 19:13:29` 的延迟是精确闹钟权限未授予时走非精确降级路径所致，授权后消失）。
- 但通知始终无声。根因：旧通知 channel `task_due_reminders_v2` 在更早测试中已被系统创建，channel 的重要性/声音/震动一旦创建即被系统锁定，代码再设 `IMPORTANCE_HIGH` / `enableVibration` 也不生效。

### 修改文件

- `app/src/main/java/com/jimu/app/reminder/TaskReminderNotifier.kt`

### 修改内容

- channel id 从 `task_due_reminders_v2` 升级到 `task_due_reminders_v3`，强制系统重建一个带声音的高优先级 channel。
- 在 `createNotificationChannel` 中显式设置默认通知音 `RingtoneManager.getDefaultUri(TYPE_NOTIFICATION)` + `AudioAttributes`（`USAGE_NOTIFICATION`），并设置 `vibrationPattern`。
- 未改 `showTaskDueNotification` 的通知构建逻辑，未改调度、权限、恢复链路。

### 验证结果

构建与单测命令（临时设置 `JAVA_HOME` 指向本机 JDK 21，未改项目配置）：

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

结果：

```text
BUILD SUCCESSFUL
```

真机手测结论（用户执行）：

- 打开“待办提醒”声音 + 取消电池智能优化后，后台/锁屏到点精准触发，时间准确。
- 通知带声音；系统设为静音时声音同步消失，说明走标准通知音量通道，行为正确。

### 已知限制

- 准点触发依赖三项设备侧条件：精确闹钟授权、关闭电池智能优化、通知音量非 0。
- 非精确降级路径不承诺严格准点。
- 部分国产 ROM 电池策略可能压制后台触发，需用户手动关闭电池优化或加白名单；本次未在代码中引导该项。

### 下一步

T7 收口完成。下一步交给 Opus 4.8 判断 T8 复盘增强切分，优先历史复盘列表；真实快照需先定口径，单独立项。
