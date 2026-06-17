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
BUILD SUCCESSFUL in 40s
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

## 2026-06-16 - T8 复盘历史列表与指定日期编辑

### 任务范围

执行 T8：作为发布准备前的小补齐任务，只做“复盘历史列表 + 点击编辑指定日期”。本次同时按实机反馈修正首页复盘摘要展示和从复盘页点击底部“首页”不回到首页顶部的问题。

本次不新增第 6 个底部 tab，不改 Room schema，不写 migration，不做 mood 输入，不接真实快照，不做复盘删除 UI，不做统计图表、复杂模板或 AI 总结，不升级工具链。

### 评审约束

本次按 Opus 4.8 评审结果收口：

- T8 不做“复盘增强包”，只做历史列表和指定日期编辑。
- 数据层复用既有 `ReviewDao.observeAllReviews()` 和 `ReviewRepository.observeAllReviews()`。
- 入口从今日复盘页进入，不加入底部 tab。
- `ReviewViewModel` 必须支持指定日期，保存时不能固定写今天。
- 重复日期脏数据不在 T8 清理；列表按 DAO 返回展示，点击同一日期时仍由 `getReviewByDate()` 取最新一条。
- 真实快照继续单独立项；当前 `TaskEntity` 无可靠 `completedAt`，不能顺手做统计。

### 修改文件

- `app/build.gradle.kts`
- `app/src/main/java/com/jimu/app/navigation/Routes.kt`
- `app/src/main/java/com/jimu/app/navigation/AppNavHost.kt`
- `app/src/main/java/com/jimu/app/navigation/TabNavigationPolicy.kt`
- `app/src/main/java/com/jimu/app/ui/components/JimuBottomBar.kt`
- `app/src/main/java/com/jimu/app/ui/home/HomeScreen.kt`
- `app/src/main/java/com/jimu/app/ui/review/ReviewScreen.kt`
- `app/src/main/java/com/jimu/app/ui/review/ReviewHistoryScreen.kt`
- `app/src/main/java/com/jimu/app/viewmodel/HomeViewModel.kt`
- `app/src/main/java/com/jimu/app/viewmodel/ReviewViewModel.kt`
- `app/src/main/java/com/jimu/app/viewmodel/ReviewHistoryViewModel.kt`
- `app/src/test/java/com/jimu/app/navigation/TabNavigationPolicyTest.kt`
- `app/src/test/java/com/jimu/app/ui/home/TodayReviewCardStyleTest.kt`
- `app/src/test/java/com/jimu/app/viewmodel/HomeTodayReviewUiModelTest.kt`
- `app/src/test/java/com/jimu/app/viewmodel/ReviewViewModelTest.kt`
- `app/src/test/java/com/jimu/app/viewmodel/ReviewHistoryViewModelTest.kt`

### 修改内容

- `Routes` 新增 `ReviewHistory` 和 `ReviewByDate` 两个非 tab 路由。
- `AppNavHost` 注册复盘历史列表页和指定日期复盘页；底部 tab 仍只保留首页、待办、习惯、目标、已完成 5 个入口。
- 新增 `ReviewHistoryViewModel`，复用 `reviewRepository.observeAllReviews()`，只映射每日复盘列表，不处理 weekly 预留类型。
- 新增 `ReviewHistoryScreen`，展示日期、摘要首行和明日重点首行；空状态只显示轻量提示。
- `ReviewViewModel` 增加 `reviewDate` 参数，默认今天；加载和保存均按指定日期执行，保存改为调用 `saveDailyReview(reviewDate = ...)`，避免编辑旧日期时误写到今天。
- `ReviewScreen` 增加“历史”入口，标题根据是否为今天显示“今日复盘”或“复盘记录”，保存按钮统一为“保存并返回”。
- 首页“今日复盘”卡片改为展示“做得好的事 / 遇到的问题 / 明日重点”三段摘要预览。
- 修正首页复盘卡片已记录状态下淡蓝背景过重的问题，外层卡片统一使用中性 `surface` 背景。
- 新增 `TabNavigationPolicy`：从复盘等非 tab 页面点击底部“首页”时不恢复旧滚动状态，并触发首页滚动到顶部；普通 tab 之间切换仍保留原有状态恢复。
- 测试侧新增 `kotlinx-coroutines-test` 依赖，用于 ViewModel `StateFlow` 行为测试。

### TDD 验证记录

先写复盘指定日期保存、历史列表映射、每日复盘过滤、首页摘要三段内容、首页卡片样式和 tab 导航策略测试后运行，对应预期失败包括：

```text
Unresolved reference: ReviewHistoryItemUiModel
Unresolved reference: ReviewHistoryViewModel
Cannot find a parameter with this name: reviewDate
Unresolved reference: fromReview
Unresolved reference: shouldResetHomeScrollOnTabClick
Unresolved reference: shouldRestoreTabState
```

实现后先分组运行新增/变更测试，再运行完整本地验证。

### 最终验证

提交前重新运行：

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

结果：

```text
BUILD SUCCESSFUL in 7s
```

### 真机手测结论

用户已在真实手机完成手测：

- 复盘历史列表可进入并点击日期编辑。
- 指定日期复盘保存不会误写到今天。
- 首页“今日复盘”卡片已记录状态下灰色外框问题已收敛。
- 首页“今日复盘”卡片能显示更多复盘摘要信息。
- 从复盘页点击底部“首页”可回到首页顶部。

### 提交记录

```text
66d8da8 feat: add review history flow
```

推送状态：

```text
已推送到 origin/main
```

### 后续边界

- R11 复盘历史列表已通过 T8 前置完成。
- mood 输入、真实快照、复盘删除、统计图表、AI 总结仍不在当前版本范围内。
- 真实快照仍需先定义口径，尤其是待办完成数不能直接依赖 `updatedAt`。
- 下一阶段继续转向发布准备：App 图标、release 签名、深色模式、Typography、启动画面、删除确认、权限/隐私说明和数据导出导入。

## 2026-06-16 - T9 本地复盘草稿 MVP

### 任务范围

执行 T9：在不修改数据库、不新增底部 tab、不引入 AI 或云服务的前提下，用现有待办、习惯和目标数据生成本地“今日成果草稿”，并展示在今日复盘页顶部，辅助用户填写三段手填复盘。

本次不新增 `completedAt`，不升级 Room version，不写 migration，不改 `ReviewEntity` / `ReviewDao` / `ReviewRepository`，不改底部导航，不删除“已完成”页，不做周报、统计图、AI 总结、复盘删除或复杂模板。

### 修改文件

- `app/src/main/java/com/jimu/app/data/repository/DailyDigestRepository.kt`
- `app/src/main/java/com/jimu/app/JimuApp.kt`
- `app/src/main/java/com/jimu/app/viewmodel/ReviewViewModel.kt`
- `app/src/main/java/com/jimu/app/ui/review/ReviewScreen.kt`
- `app/src/test/java/com/jimu/app/data/repository/DailyDigestBuilderTest.kt`
- `app/src/test/java/com/jimu/app/viewmodel/ReviewViewModelTest.kt`
- `AI_PLAN.md`
- `FACT_REPORT.md`
- `DEV_LOG.md`

### 修改内容

- 新增 `DailyDigestUiModel`，作为复盘草稿 UI 模型，不暴露 Room Entity。
- 新增 `DailyDigestBuilder`，将核心聚合和文案生成放在纯函数里，便于单元测试。
- 新增 `DailyDigestRepository`，通过现有 `TaskRepository.observeAllTasks()`、`HabitRepository.observeHabitUiModels()`、`GoalRepository.observeGoalUiModels()` 合并生成 `Flow<DailyDigestUiModel>`。
- 待办草稿使用“当前已完成 X 项待办”口径，不声称“今天完成”，避免 `TaskEntity` 缺少 `completedAt` 导致误导。
- 习惯草稿使用 `checkedToday`，其底层仍来自 `recordDate == LocalDate.now().toString()`。
- 目标草稿使用“当前目标推进 X/Y 个步骤”，不声称“今日推进”，避免 `GoalStepEntity` 缺少独立完成时间导致误导。
- 未完成提醒聚合今日待处理和逾期未完成待办。
- `JimuApp` 初始化并暴露 `dailyDigestRepository`。
- `ReviewViewModel` 新增 `dailyDigest` 状态；保存复盘逻辑保持原样，`completedTaskSnapshot` 和 `checkedHabitSnapshot` 继续写 0。
- `ReviewScreen` 在今日复盘页标题下方、输入表单上方展示“今日成果草稿”卡片；编辑历史日期复盘时不展示今天的草稿，避免上下文误导。
- 草稿卡片文案包含“根据当前数据整理，供参考，不代表精准统计。”，避免把近似数据包装成严肃统计。
- 无数据时展示空状态：“今天还没有可整理的成果，先完成一个待办或打卡一个习惯。”

### TDD 验证记录

先写 `DailyDigestBuilderTest` 和 `ReviewViewModelTest` 后运行：

```powershell
.\gradlew.bat testDebugUnitTest --tests com.jimu.app.data.repository.DailyDigestBuilderTest --tests com.jimu.app.viewmodel.ReviewViewModelTest
```

预期红灯包括：

```text
Unresolved reference: DailyDigestBuilder
Unresolved reference: DailyDigestRepository
Cannot find a parameter with this name: dailyDigestRepository
Unresolved reference: dailyDigest
```

实现后再次运行同一组测试，结果：

```text
BUILD SUCCESSFUL in 10s
```

### 最终验证

运行完整本地单元测试和 Debug 构建：

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

结果：

```text
BUILD SUCCESSFUL in 28s
```

### 实机验证结论

用户已在真实手机完成 T9 验证，结论为功能全部 OK。已确认：

- 今日复盘页顶部是否出现“今日成果草稿”。
- 有待办、习惯、目标数据时，草稿是否能展示四类信息。
- 没有数据时是否显示友好空状态。
- 草稿是否没有直接覆盖三段手填输入。
- 保存复盘后是否仍能回到首页并显示手填摘要。
- 历史复盘编辑页是否不展示今天的草稿。

## 2026-06-16 - 修复底部首页 tab 恢复到错误页面

### 问题现象

T9 实机验证通过后，用户发现一个底部导航问题：在待办 tab 添加待办后点击底部“首页”，页面会跳回待办页；重启 App 后如果先到目标页添加目标和步骤，点击“首页”又会回到目标页，表现为“首页像是被当前 tab 替换了”。

### 根因

T8 新增底部 tab 状态恢复策略时，将 `popUpTo.saveState` 和 `navigate.restoreState` 共用同一个 `shouldRestoreState` 判断。普通 tab 切换需要保存/恢复状态，但“首页”是 `NavHost` 的 start destination，点击首页时不应启用 `restoreState`；否则系统可能恢复出之前保存的待办/目标 tab 状态，让首页按钮表现成刚才操作过的 tab。

### 修改内容

- 在 `TabNavigationPolicy` 中拆分 `shouldSaveTabState` 和 `shouldRestoreTabState`：
  - 从待办/目标等 tab 回首页时，可以保存当前 tab 状态。
  - 但目标是首页时，不恢复目标状态，确保真正回到 `Routes.Home`。
  - 从复盘等非 tab 页面回首页时，仍不保存也不恢复，并继续触发首页滚动到顶部。
- `AppNavHost` 分别使用 `shouldSaveTabState` 控制 `popUpTo.saveState`，使用 `shouldRestoreTabState` 控制 `navigate.restoreState`。
- 更新 `TabNavigationPolicyTest`，覆盖“从待办到首页不恢复首页状态”和“普通非首页 tab 切换仍保存/恢复状态”。

### TDD 验证记录

先改测试后运行：

```powershell
.\gradlew.bat testDebugUnitTest --tests com.jimu.app.navigation.TabNavigationPolicyTest
```

预期红灯：

```text
Unresolved reference: shouldSaveTabState
```

实现策略拆分后再次运行同一测试，结果：

```text
BUILD SUCCESSFUL in 13s
```

### 最终验证与实机复测

运行完整本地单元测试和 Debug 构建：

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

结果：

```text
BUILD SUCCESSFUL in 10s
```

用户已在真实手机复测，底部“首页”tab 能从待办、目标等页面正常回到真正首页，T9 相关功能全部 OK。T9 可以收口。

## 2026-06-16 - T10 底部复盘入口与已完成迁移

### 任务范围

执行 T10 最终落地版：采用“方案 A + 保守统计口径”。本次只做信息架构重排，不提交、不推送，等待用户实机验证后再决定后续操作。

本次明确不做：不引入基于 `updatedAt` 的“今日完成 / 本周完成”时间统计，不删除 `CompletedScreen.kt`，不改 Room schema，不写 migration，不引入 AI、图表、账号或云服务，不升级 Gradle / AGP / Kotlin / Room / Compose 工具链。

### 修改文件

- `app/src/main/java/com/jimu/app/navigation/Routes.kt`
- `app/src/main/java/com/jimu/app/navigation/AppNavHost.kt`
- `app/src/main/java/com/jimu/app/ui/components/JimuBottomBar.kt`
- `app/src/main/java/com/jimu/app/ui/tasks/TasksScreen.kt`
- `app/src/main/java/com/jimu/app/ui/completed/CompletedTaskGroups.kt`
- `app/src/main/java/com/jimu/app/ui/completed/CompletedScreen.kt`
- `app/src/main/java/com/jimu/app/ui/home/HomeScreen.kt`
- `app/src/main/java/com/jimu/app/viewmodel/HomeViewModel.kt`
- `app/src/test/java/com/jimu/app/navigation/TabNavigationPolicyTest.kt`
- `app/src/test/java/com/jimu/app/ui/completed/CompletedTaskGroupsTest.kt`
- `app/src/test/java/com/jimu/app/viewmodel/HomeCompletionPaceUiModelTest.kt`
- `app/src/test/java/com/jimu/app/data/repository/DailyDigestBuilderTest.kt`
- `AI_PLAN.md`
- `DEV_LOG.md`

### 修改内容

- `Routes` 新增 `tabTitle`，让 `Routes.Review.title` 继续作为页面语义“今日复盘”，底栏显示则使用“复盘”。
- `JimuBottomBar` 改为显示 `tabTitle`，并将 `Routes.Review` 图标改为 `Icons.Outlined.RateReview`。
- `AppNavHost` 的底部 tabs 从 `首页 / 待办 / 习惯 / 目标 / 已完成` 改为 `首页 / 待办 / 习惯 / 目标 / 复盘`。
- 从 `AppNavHost` 中移除 `Routes.Completed` 的页面注册；`CompletedScreen.kt` 文件保留。
- `TasksScreen` 的 `TaskViewMode` 新增 `COMPLETED`，`TaskViewSwitcher` 从两段改为三段：`今日 / 全部 / 已完成`，指示器宽度和 offset 按 3 段动态计算。
- 待办页“已完成”分支展示已完成待办，按天归档；点击“已完成”状态复用 `TasksViewModel.toggleTaskCompleted(task)` 回退到待办。
- 将已完成分组逻辑抽到 `CompletedTaskGroups.kt` 的 `buildCompletedGroups(...)`，旧 `CompletedScreen` 和新待办页都复用同一套分组逻辑。
- 首页原“已完成”小卡替换为“完成节奏”卡，主数字仍为当前已完成总数，辅助信息展示当前待处理、目标推进和今日复盘状态。
- 首页完成卡不出现“今天完成 / 今日完成 / 本周完成”文案，继续使用 T9 的“当前已完成”保守口径。

### TDD 验证记录

先写测试后运行目标测试：

```powershell
.\gradlew.bat testDebugUnitTest --tests com.jimu.app.navigation.TabNavigationPolicyTest --tests com.jimu.app.ui.completed.CompletedTaskGroupsTest --tests com.jimu.app.viewmodel.HomeCompletionPaceUiModelTest --tests com.jimu.app.data.repository.DailyDigestBuilderTest
```

预期红灯包括：

```text
Unresolved reference: tabTitle
Cannot access 'buildCompletedGroups': it is private in file
Unresolved reference: HomeCompletionPaceUiModel
```

实现后重跑同一组目标测试，结果：

```text
BUILD SUCCESSFUL in 3s
```

### 最终本地验证

完整本地单元测试：

```powershell
.\gradlew.bat testDebugUnitTest
```

结果：

```text
BUILD SUCCESSFUL in 17s
```

Debug 构建：

```powershell
.\gradlew.bat assembleDebug
```

结果：

```text
BUILD SUCCESSFUL in 41s
```

### 待实机验证

本轮尚未提交、尚未推送，等待用户实机验证。建议重点验证：

1. 底栏第 5 栏显示“复盘”，点击后进入今日复盘页。
2. 从复盘历史页或指定日期复盘页点击底部“首页”，能回到首页顶部。
3. 从待办、目标等 tab 点击底部“首页”，不会恢复到刚才的 tab。
4. 待办页三段切换 `今日 / 全部 / 已完成` 的指示器宽度和动画对齐。
5. 已完成分支能按日期归档展示完成记录。
6. 点击完成记录的“已完成”状态后能回退到待办，并从已完成列表消失。
7. 首页“完成节奏”卡不出现“今日完成 / 本周完成”等时间统计口径。

## 2026-06-16 - T10 收尾：复盘入口统一与旧统计清理

### 任务范围

承接 T10，执行收尾整改：清理不可达 `CompletedScreen` 内部的旧时间统计病灶，精简首页卡片，统一首页复盘卡和底栏复盘 tab 的导航身份，并让 `ReviewScreen` 区分一级 tab 与历史日期二级页两种模式。

本次不提交、不推送；不删除 `CompletedScreen.kt` / `CompletedViewModel.kt` / `HomeHintCard`；不改 Room schema，不写 migration，不升级 Gradle / AGP / Kotlin / Room / Compose，不引入 DI、AI、图表或云服务。

### 修改文件

- `app/src/main/java/com/jimu/app/navigation/AppNavHost.kt`
- `app/src/main/java/com/jimu/app/ui/home/HomeScreen.kt`
- `app/src/main/java/com/jimu/app/ui/review/ReviewScreen.kt`
- `app/src/main/java/com/jimu/app/ui/completed/CompletedScreen.kt`
- `app/src/main/java/com/jimu/app/ui/completed/CompletedStats.kt`
- `app/src/test/java/com/jimu/app/navigation/TabNavigationPolicyTest.kt`
- `app/src/test/java/com/jimu/app/ui/completed/CompletedStatsTest.kt`
- `app/src/test/java/com/jimu/app/ui/review/ReviewScreenModeTest.kt`
- `AI_PLAN.md`
- `FACT_REPORT.md`
- `DEV_LOG.md`

### 修改内容

- `CompletedScreen` 的 `CompletedSummaryCard` 从“累计 / 今天 / 本周”三格统计改为只展示“累计完成”和最近一条记录。
- 新增 `CompletedStats.kt`，`buildCompletedStats(...)` 只保留 `totalCount` 和 `latestText`，不再基于 `updatedAt` 计算今日/本周完成。
- 删除 `CompletedScreen.kt` 内部的 `todayCount`、`weekCount`、`startOfDay`、`startOfWeek` 旧病灶；`CompletedScreen.kt` 文件和 `CompletedViewModel.kt` 均保留。
- 首页取消“今日概览”文字卡调用；`HomeHintCard` 函数本体保留。
- `AppNavHost` 抽出本地 `navigateToTab(route)`，底栏点击和首页“今日复盘”卡共用同一套 `saveState` / `restoreState` / `popUpTo(startDestination)` tab 导航逻辑。
- `Routes.Review` 场景的 `ReviewScreen` 传入 `isTopLevelTab = true`；保存后留在复盘页，不再 pop 回首页。
- `ReviewScreen` 新增独立的 `isTopLevelTab` 参数，不复用 `isTodayReview` 判断，避免“历史里点今天”被误判为一级 tab。
- `ReviewScreen` tab 模式隐藏返回按钮，保存按钮文案为“保存”，保存成功后显示轻量“已保存”反馈。
- `ReviewByDate` 二级页后续按实机反馈调整为：返回按钮仍显示，隐藏“历史”入口，保存按钮文案统一为“保存”，保存后留在当前页并显示“已保存”；离开页面只由顶部“返回”负责。

### TDD 验证记录

先写测试后运行目标测试：

```powershell
.\gradlew.bat testDebugUnitTest --tests com.jimu.app.ui.completed.CompletedStatsTest --tests com.jimu.app.ui.review.ReviewScreenModeTest --tests com.jimu.app.navigation.TabNavigationPolicyTest
```

预期红灯包括：

```text
Cannot access 'buildCompletedStats': it is private in file
Unresolved reference: reviewShowBackButton
Unresolved reference: reviewSaveButtonText
```

实现后重跑同一组目标测试，结果：

```text
BUILD SUCCESSFUL in 9s
```

### 分步验证记录

清理 `CompletedScreen` 旧统计病灶后运行：

```powershell
.\gradlew.bat assembleDebug
```

结果：

```text
BUILD SUCCESSFUL in 28s
```

移除首页“今日概览”卡调用后运行：

```powershell
.\gradlew.bat assembleDebug
```

结果：

```text
BUILD SUCCESSFUL in 12s
```

抽出 `navigateToTab(route)` 并统一首页复盘卡 / 底栏复盘 tab 导航后运行：

```powershell
.\gradlew.bat assembleDebug
```

结果：

```text
BUILD SUCCESSFUL in 7s
```

完成 `ReviewScreen` 双模式后运行：

```powershell
.\gradlew.bat assembleDebug
```

结果：

```text
BUILD SUCCESSFUL in 4s
```

### 最终本地验证

强制重跑完整本地单元测试：

```powershell
.\gradlew.bat testDebugUnitTest --rerun-tasks
```

结果：

```text
BUILD SUCCESSFUL in 49s
```

强制重跑 Debug 构建：

```powershell
.\gradlew.bat assembleDebug --rerun-tasks
```

结果：

```text
BUILD SUCCESSFUL in 43s
```

### 实机验证结论

用户已在真实手机完成 T10 手测。首页复盘卡与底栏复盘入口一致性、复盘 tab 保存反馈、复盘历史列表、待办页三段切换、已完成回退、首页精简和完成节奏口径均通过。

实机验证时发现一处体验争议：从复盘历史列表点进某一天后，历史日期编辑页顶部同时出现“历史”和“返回”，底部按钮为“保存并返回”。用户判断“保存”和“返回”应拆开，保存后不应强制回历史列表。

## 2026-06-16 - T10 历史复盘编辑交互收口

### 背景

承接 T10 实机验证反馈，收口历史日期复盘编辑页的按钮语义。本次只改复盘页交互与对应测试、文档，不改 Room schema，不改复盘数据层，不调整底部导航结构，不进入 R3 深色模式任务。

目标语义：

1. 复盘一级 tab：顶部显示“历史”，隐藏“返回”，底部按钮为“保存”，保存后留在当前页并显示“已保存”。
2. 复盘历史列表页：顶部只有“返回”，没有保存按钮。
3. 历史日期编辑页：顶部只有“返回”，不再显示“历史”；底部按钮为“保存”，保存后留在当前页并显示“已保存”；离开页面由顶部“返回”负责。

### 修改文件

- `app/src/main/java/com/jimu/app/navigation/AppNavHost.kt`
- `app/src/main/java/com/jimu/app/ui/review/ReviewScreen.kt`
- `app/src/test/java/com/jimu/app/ui/review/ReviewScreenModeTest.kt`
- `AI_PLAN.md`
- `FACT_REPORT.md`
- `DEV_LOG.md`
- `RELEASE_PLAN.md`

### TDD 验证记录

先修改 `ReviewScreenModeTest`，将期望改为：

- 一级 tab 显示“历史”、隐藏“返回”、保存按钮为“保存”。
- 历史日期编辑页隐藏“历史”、显示“返回”、保存按钮也为“保存”。
- 保存中统一显示“保存中...”。

随后运行：

```powershell
.\gradlew.bat testDebugUnitTest --tests com.jimu.app.ui.review.ReviewScreenModeTest
```

预期红灯：

```text
Unresolved reference: reviewShowHistoryButton
No value passed for parameter 'isTopLevelTab'
```

实现 `reviewShowHistoryButton(...)`、简化 `reviewSaveButtonText(...)`，并移除 `ReviewByDate` 保存后的 `popBackStack()` 后，重跑同一测试，结果：

```text
BUILD SUCCESSFUL in 11s
```

### 最终本地验证

完整单元测试：

```powershell
.\gradlew.bat testDebugUnitTest
```

结果：

```text
BUILD SUCCESSFUL in 4s
```

Debug 构建：

```powershell
.\gradlew.bat assembleDebug
```

结果：

```text
BUILD SUCCESSFUL in 6s
```

### 待快速实机复测

本轮代码调整发生在用户完成 T10 主体真机手测之后。建议只复测历史日期编辑页这条路径：复盘 tab -> 历史 -> 点某一天 -> 顶部只有“返回”、底部按钮为“保存”、保存后停留当前页并显示“已保存”，点“返回”后回到历史列表。

## 2026-06-17 - R3 深色模式修复

### 任务范围

只执行 R3：修复深色模式没有真正启用的问题。按用户要求，本次只改主题代码、测试和上下文文档，不改 Room schema，不改导航，不改 Gradle 配置，不进入 R1 图标、R2 签名、R4 Typography 或其他发布任务。

### 问题原因

`Theme.kt` 中已经定义了 `JimuDarkColorScheme`，但 `JimuTheme` 存在两处问题：

- `darkTheme` 默认值写死为 `false`，没有跟随系统深色模式。
- `colorScheme` 分支在 `darkTheme == true` 时仍返回 `JimuLightColorScheme`。

### 修改文件

- `app/src/main/java/com/jimu/app/ui/theme/Theme.kt`
- `app/src/test/java/com/jimu/app/ui/theme/JimuThemeTest.kt`
- `AI_PLAN.md`
- `FACT_REPORT.md`
- `DEV_LOG.md`

### 修改内容

- `JimuTheme` 默认使用 `isSystemInDarkTheme()` 判断系统深色模式。
- 新增 `selectJimuColorScheme(darkTheme: Boolean)`，浅色分支返回 `JimuLightColorScheme`，深色分支返回 `JimuDarkColorScheme`。
- 新增 `JimuThemeTest`，验证 `darkTheme = true` 时使用深色方案，背景为 `DeepNavy`，surface 为 `NightBlue`。

### TDD 验证记录

先新增 `JimuThemeTest`，运行目标测试时按预期失败：

```text
Unresolved reference: selectJimuColorScheme
```

随后实现 `selectJimuColorScheme(...)` 并修复 `JimuTheme`，重跑目标测试通过。

### 最终验证

目标测试：

```powershell
.\gradlew.bat testDebugUnitTest --tests com.jimu.app.ui.theme.JimuThemeTest
```

结果：

```text
BUILD SUCCESSFUL in 13s
```

完整单元测试：

```powershell
.\gradlew.bat testDebugUnitTest
```

结果：

```text
BUILD SUCCESSFUL in 3s
```

Debug 构建：

```powershell
.\gradlew.bat assembleDebug
```

结果：

```text
BUILD SUCCESSFUL in 5s
```

### 后续手测建议

在真实手机或模拟器中切换系统深色模式，快速查看首页、待办、习惯、目标和复盘页的整体对比度。当前代码层面已接入深色方案，但个别自定义蓝色卡片在深色背景下的视觉舒适度仍需要肉眼确认。

## 2026-06-17 - R3 深色模式实机复测与修复

### 背景

R3 接入深色方案后做实机复测，发现两处深色模式下的视觉问题，本次按问题逐项修复。仍只改主题/页面代码、测试和上下文文档，不改 Room schema，不改导航，不改 Gradle 配置。

### 问题一：待办/习惯卡片在深色下文字看不清

根因：待办页和习惯页的卡片把容器色写死为浅色常量 `PanelBlue`，但卡片内文字使用主题色。深色模式下文字变浅，浅底配浅字导致看不清。目标/复盘卡片正常，是因为它们用 `MaterialTheme.colorScheme.surface` 等主题色容器。

修复：

- 在 `Color.kt` 新增 `panelColor(darkTheme)`：浅色返回 `PanelBlue`，深色返回 `NightBlue`，保持浅色观感不变的同时让深色卡片变深。
- 将以下三处卡片容器色改为 `panelColor(isSystemInDarkTheme())`：
  - `app/src/main/java/com/jimu/app/ui/tasks/TasksScreen.kt`
  - `app/src/main/java/com/jimu/app/ui/habits/HabitsScreen.kt`（两处）
- 新增 `PanelColorTest`，先因 `panelColor` 未定义失败，实现后通过。

说明：`CompletedScreen.kt` 内也使用 `PanelBlue`，但该文件已是不可达死代码（不在底部导航注册），本次不改，留待后续死代码清理。

### 问题二：四个页面右下角悬浮按钮配色不一致

根因：待办/习惯的 FAB 显式使用 `primary` / `onPrimary`，深浅色都醒目；首页/目标的 FAB 未指定颜色，使用 Material3 默认 `primaryContainer`，深色方案下 `primaryContainer = NightBlue`，配深背景不明显。

修复：

- 首页语音 FAB 与目标新增 FAB 显式设置 `containerColor = MaterialTheme.colorScheme.primary`、`contentColor = MaterialTheme.colorScheme.onPrimary`，与待办/习惯统一。
- FAB 取色属于 Compose 渲染参数，纯函数无法有效断言，按现有 FAB 模式以编译和实机验证，不新增单元测试。

### 修改文件

- `app/src/main/java/com/jimu/app/ui/theme/Color.kt`
- `app/src/main/java/com/jimu/app/ui/tasks/TasksScreen.kt`
- `app/src/main/java/com/jimu/app/ui/habits/HabitsScreen.kt`
- `app/src/main/java/com/jimu/app/ui/home/HomeScreen.kt`
- `app/src/main/java/com/jimu/app/ui/goals/GoalsScreen.kt`
- `app/src/test/java/com/jimu/app/ui/theme/PanelColorTest.kt`
- `AI_PLAN.md`
- `FACT_REPORT.md`
- `DEV_LOG.md`

### 最终验证

完整单元测试：

```powershell
.\gradlew.bat testDebugUnitTest
```

结果：

```text
BUILD SUCCESSFUL
```

Debug 构建：

```powershell
.\gradlew.bat assembleDebug
```

结果：

```text
BUILD SUCCESSFUL
```

### 实机复测结论

用户已在真实手机复测通过：深色模式下待办/习惯卡片文字清晰可见；首页、待办、习惯、目标四个页面右下角悬浮按钮配色统一且醒目；浅色模式观感未变。
