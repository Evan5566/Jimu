# 本地数据导出导入 MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为迹目加入本地 JSON 数据导出/导入能力，支持“先校验、先保存当前备份、再全量替换恢复、最后重排待办提醒”，确保长期自用时数据可回退、提醒不残留、不漏排。

**Architecture:** 采用“备份 DTO + 有上限的文件流读写 + JSON 编解码/校验 + 可注入事务边界 + 数据恢复服务 + 可注入提醒控制器 + Compose 设置页入口”结构。备份格式不直接暴露 Room Entity，而是使用 `TaskBackupV1`、`HabitBackupV1` 等独立 DTO；恢复流程拆成 `readUtf8Limited()`、`decodeAndValidate()`、`createCurrentBackup()`、`restoreValidatedPayload()`、`rebuildAfterSuccessfulRestore()`，避免一个 `importBackupJson()` 同时承担文件读取、校验、保险备份、数据库替换和提醒重排。

**Tech Stack:** Kotlin, Jetpack Compose, Room, Navigation Compose, Android Storage Access Framework, coroutines, existing Repository / ViewModel 分层。生产代码使用 Android `org.json`；JVM 测试固定增加 `testImplementation("org.json:json:20240303")`，并保留设备侧 codec 往返测试验证 Android 实现一致性。

## Global Constraints

- 继续使用 Kotlin、Jetpack Compose、Room、Navigation Compose。
- 不引入 Hilt/Dagger/Koin 等新框架。
- 不修改 AGP、Kotlin、Room、Compose 版本。
- 数据导出和导入必须是本地离线流程，不接入云服务或大模型 API。
- 当前 Room database version 默认保持 5；本计划不处理 R10 的唯一约束和 migration。
- 导入第一版采用“完整备份、全量替换恢复”，不做智能合并。
- 备份文件为未加密明文，UI 必须提示可能包含私人内容，需要妥善保管。
- 第一版备份文件 UTF-8 内容上限固定为 `10 * 1024 * 1024` bytes；导出和导入使用同一上限，确保本版本生成的备份一定能被本版本重新导入。
- SAF 元数据中的文件大小只用于快速拒绝；实际读取必须最多读取“上限 + 1 byte”，不得使用无限制 `readText()` 或 `readBytes()`。
- 文件读取、JSON 编解码、数据库读写、提醒重排必须在 `Dispatchers.IO`。
- 设置页是二级页面，显示顶部返回按钮，并隐藏底部导航栏。
- 验证时至少运行 `./gradlew.bat testDebugUnitTest`、`./gradlew.bat assembleDebug`；事务回滚和 SAF/提醒相关路径需要 `connectedDebugAndroidTest` 或等价模拟器/真机验证。
- 运行构建前如命令行默认 Java 不是 21，需要临时切到本机 JDK 21。

---

## 已核查的当前代码事实

- `TaskReminderScheduler.schedule()` 会用任务 ID 生成独立 `PendingIntent`，并把标题和 notificationId 写入 extras；恢复数据库后若不处理旧 alarm，会出现旧提醒残留或新提醒漏排。
- `TaskReminderScheduler.cancel(taskId)` 已能按任务 ID 取消已有提醒，可用于恢复成功后的旧任务提醒清理。
- `JimuApp.restoreFutureTaskReminders()` 当前只会为未来未完成任务重新安排提醒，不会自动取消数据库中已经不存在的旧任务提醒。
- 当前 `app/build.gradle.kts` 的 JVM 测试依赖只有 JUnit 和 coroutines test，没有 Robolectric、`room-testing`、AndroidX Test Core，也不能假定 Android `org.json` 可直接跑 JVM 单测。
- 当前 DAO 多处 `@Insert(onConflict = OnConflictStrategy.REPLACE)`，恢复专用插入不能沿用会静默覆盖的 `REPLACE` 策略，必须单独定义 `ABORT` 插入方法。
- `AppDatabase.withTransaction` 和 `TaskReminderScheduler` 都是具体 Android/Room 依赖，不能直接在现有 JVM 单测里 fake；计划必须通过 `BackupTransactionRunner` 和 `TaskReminderController` 提供可注入边界。

---

## 文件边界

### 新增文件

- `app/src/main/java/com/jimu/app/data/backup/BackupModels.kt`：定义 `AppBackupPayloadV1`、`TaskBackupV1`、`HabitBackupV1`、`HabitRecordBackupV1`、`GoalBackupV1`、`GoalStepBackupV1`、`ReviewBackupV1`、校验结果和恢复结果。
- `app/src/main/java/com/jimu/app/data/backup/BackupStreamIo.kt`：负责有上限的 UTF-8 读取和完整写入，统一执行 10 MiB 上限。
- `app/src/main/java/com/jimu/app/data/backup/BackupJsonCodec.kt`：负责备份 DTO 与 JSON 字符串的双向转换；不得直接把 Entity 当作长期备份格式。
- `app/src/main/java/com/jimu/app/data/backup/BackupValidator.kt`：负责版本、包名、主键唯一性、关联完整性、必填字段和日期格式校验；文件大小由 `BackupStreamIo` 负责。
- `app/src/main/java/com/jimu/app/data/backup/BackupTransactionRunner.kt`：定义 `BackupTransactionRunner`，生产实现使用 `AppDatabase.withTransaction`，JVM 测试使用可回滚 fake。
- `app/src/main/java/com/jimu/app/data/backup/BackupRepository.kt`：负责只读事务导出、当前数据备份、事务恢复。
- `app/src/main/java/com/jimu/app/data/backup/BackupReminderRebuilder.kt`：负责恢复成功后取消旧任务提醒，并为恢复后的未来未完成任务重新安排提醒。
- `app/src/main/java/com/jimu/app/reminder/TaskReminderController.kt`：定义可测试的 `cancel(taskId)` / `schedule(task, mayRequestExactAlarmPermission)` 接口。
- `app/src/main/java/com/jimu/app/ui/settings/SettingsScreen.kt`：设置页 UI，承载版本信息、导出入口、导入入口、明文风险提示、恢复确认和结果展示。
- `app/src/main/java/com/jimu/app/viewmodel/SettingsViewModel.kt`：设置页状态机，使用 sealed state 表达导出、读取、校验、等待保存保险备份、恢复、提醒重排、成功和失败。
- `app/src/main/java/com/jimu/app/viewmodel/SettingsViewModelFactory.kt`：创建 `SettingsViewModel`，接入 `JimuApp` 中的依赖。
- `app/src/test/java/com/jimu/app/data/backup/BackupValidatorTest.kt`：JVM 单测校验备份结构和错误数据。
- `app/src/test/java/com/jimu/app/data/backup/BackupStreamIoTest.kt`：JVM 单测验证大小预检、上限边界、超限中止、UTF-8 读写。
- `app/src/test/java/com/jimu/app/data/backup/BackupJsonCodecTest.kt`：使用测试专用 `org.json` 依赖验证 JVM 侧编解码、非法 JSON 和版本字段。
- `app/src/test/java/com/jimu/app/data/backup/BackupRepositoryTest.kt`：用 fake DAO 测业务分支、失败分支和中途抛异常保护。
- `app/src/test/java/com/jimu/app/data/backup/BackupReminderRebuilderTest.kt`：使用 fake `TaskReminderController` 验证提醒取消、重排和逐项失败汇总。
- `app/src/test/java/com/jimu/app/viewmodel/SettingsViewModelTest.kt`：验证设置页 sealed state。
- `app/src/androidTest/java/com/jimu/app/data/backup/BackupJsonCodecInstrumentedTest.kt`：在设备/模拟器上验证 Android `org.json` 与 JVM 测试覆盖的备份格式一致。
- `app/src/androidTest/java/com/jimu/app/data/backup/BackupRestoreTransactionInstrumentedTest.kt`：用真实 Room 数据库验证事务回滚。

### 修改文件

- `app/src/main/java/com/jimu/app/data/local/dao/TaskDao.kt`：补充 `getAllTasksForBackup()`、`deleteAllTasksForRestore()`、`insertTasksForRestoreAbort(tasks)`。
- `app/src/main/java/com/jimu/app/data/local/dao/HabitDao.kt`：补充习惯和习惯记录的全量读取、清空、`ABORT` 插入方法。
- `app/src/main/java/com/jimu/app/data/local/dao/GoalDao.kt`：补充目标和目标步骤的全量读取、清空、`ABORT` 插入方法。
- `app/src/main/java/com/jimu/app/data/local/dao/ReviewDao.kt`：补充复盘的全量读取、清空、`ABORT` 插入方法。
- `app/src/main/java/com/jimu/app/JimuApp.kt`：初始化并暴露 `BackupRepository`、`BackupReminderRebuilder`。
- `app/src/main/java/com/jimu/app/reminder/TaskReminderScheduler.kt`：实现 `TaskReminderController`，原有业务调用方式不变。
- `app/src/main/java/com/jimu/app/navigation/Routes.kt`：新增 `Settings` route。
- `app/src/main/java/com/jimu/app/navigation/AppNavHost.kt`：挂接设置页导航；当前目的地为 `Settings` 时不渲染底部栏。
- `app/src/main/java/com/jimu/app/ui/home/HomeScreen.kt`：把右上角设置按钮接到设置页回调。
- `app/build.gradle.kts`：固定增加 `testImplementation("org.json:json:20240303")`；不改任何 app runtime 依赖或核心库版本。
- `FACT_REPORT.md`、`RELEASE_PLAN.md`、`AI_PLAN.md`、`DEV_LOG.md`：按实际完成状态更新，不提前宣称 R8/R9 完成。

---

## 总体风险点

1. **提醒残留风险**：恢复删除了旧任务但旧 `PendingIntent` 仍存在，用户会收到已不存在任务的提醒。
2. **提醒漏排风险**：导入的新未来待办不会自动安排提醒，必须恢复成功后重排。
3. **提醒内容陈旧风险**：相同任务 ID 的旧 `PendingIntent` 可能保留旧标题或旧时间，必须先取消旧任务 ID，再按恢复后数据重新 schedule。
4. **数据丢失风险**：导入如果先删后失败，原数据会丢失；必须先完整校验，再强制保存当前备份，再事务内替换。
5. **关系断裂风险**：当前数据库没有外键保证，必须在恢复前校验 `habitRecord.habitId` 和 `goalStep.goalId`。
6. **静默覆盖风险**：恢复专用插入不能使用 `REPLACE`，重复 ID 应失败，不能悄悄覆盖。
7. **测试误判风险**：fake DAO 不能证明真实 Room 事务回滚；事务回滚必须用仪器测试验证。
8. **明文泄露风险**：JSON 备份未加密，可能包含私人事项、复盘内容和目标，UI 必须提示。
9. **文件过大风险**：SAF 文件大小可能未知或不可信；必须通过受限流读取强制执行 10 MiB 上限。
10. **部分提醒失败风险**：单条提醒取消或安排失败不能阻断其余任务处理；必须继续处理并汇总失败任务 ID。

---

### Task 1: 固定备份 DTO、JSON 和校验规则

**Files:**
- Create: `app/src/main/java/com/jimu/app/data/backup/BackupModels.kt`
- Create: `app/src/main/java/com/jimu/app/data/backup/BackupStreamIo.kt`
- Create: `app/src/main/java/com/jimu/app/data/backup/BackupJsonCodec.kt`
- Create: `app/src/main/java/com/jimu/app/data/backup/BackupValidator.kt`
- Modify: `app/build.gradle.kts`
- Test: `app/src/test/java/com/jimu/app/data/backup/BackupStreamIoTest.kt`
- Test: `app/src/test/java/com/jimu/app/data/backup/BackupJsonCodecTest.kt`
- Test: `app/src/test/java/com/jimu/app/data/backup/BackupValidatorTest.kt`
- Test: `app/src/androidTest/java/com/jimu/app/data/backup/BackupJsonCodecInstrumentedTest.kt`

**Interfaces:**
- Produces: `BackupMetaV1`, `AppBackupPayloadV1`, `TaskBackupV1`, `HabitBackupV1`, `HabitRecordBackupV1`, `GoalBackupV1`, `GoalStepBackupV1`, `ReviewBackupV1`。
- Produces: `BackupJsonCodec.encode(payload: AppBackupPayloadV1): String`。
- Produces: `BackupJsonCodec.decode(json: String): BackupDecodeResult`。
- Produces: `BackupValidator.validate(payload: AppBackupPayloadV1): BackupValidationResult`。
- Produces: `BackupStreamIo.readUtf8Limited(input: InputStream, declaredSizeBytes: Long?): BackupFileReadResult`。
- Produces: `BackupStreamIo.writeUtf8(output: OutputStream, content: String): BackupFileWriteResult`。
- Produces: `ValidatedBackupPayload(payload: AppBackupPayloadV1)`；构造函数保持 `internal`，业务调用方只能从成功校验结果获得。

- [ ] **Step 1: 定义 DTO，不直接使用 Entity**
  - `AppBackupPayloadV1` 根字段固定为：`backupVersion`, `meta`, `tasks`, `habits`, `habitRecords`, `goals`, `goalSteps`, `reviews`。
  - `BackupMetaV1` 字段固定为：`exportedAt`, `appPackage`, `appVersionName`, `appVersionCode`。
  - `TaskBackupV1` 字段固定为：`id`, `title`, `description`, `dueDate`, `isCompleted`, `createdAt`, `updatedAt`。
  - `HabitBackupV1` 字段固定为：`id`, `name`, `description`, `createdAt`。
  - `HabitRecordBackupV1` 字段固定为：`id`, `habitId`, `recordDate`, `createdAt`。
  - `GoalBackupV1` 字段固定为：`id`, `title`, `description`, `createdAt`, `updatedAt`。
  - `GoalStepBackupV1` 字段固定为：`id`, `goalId`, `title`, `isCompleted`, `createdAt`, `updatedAt`。
  - `ReviewBackupV1` 字段固定为：`id`, `reviewDate`, `type`, `summary`, `problems`, `tomorrowFocus`, `mood`, `completedTaskSnapshot`, `checkedHabitSnapshot`, `createdAt`, `updatedAt`。

- [ ] **Step 2: 定义校验规则**
  - `backupVersion` 必须等于 1。
  - `appPackage` 必须等于 `com.jimu.app`。
  - 所有 `id` 必须大于 0，且各表内唯一。
  - `habitRecords.habitId` 必须存在于 `habits.id`。
  - `goalSteps.goalId` 必须存在于 `goals.id`。
  - `TaskBackupV1.title`、`HabitBackupV1.name`、`GoalBackupV1.title`、`GoalStepBackupV1.title` 不能为空白。
  - `HabitRecordBackupV1.recordDate` 和 `ReviewBackupV1.reviewDate` 必须符合 `yyyy-MM-dd`。
  - `ReviewBackupV1.type` 第一版只接受 `daily`。
  - `mood` 第一版按数据库字段做无损 nullable `Int` 保存，不设置额外数值范围；原因是当前 UI 未启用 mood，现有 schema 也没有范围约束。

- [ ] **Step 3: 写校验单测**
  - 合法 payload 返回 `Valid`。
  - 重复 task id 返回 `Invalid`。
  - `habitId` 指向不存在习惯返回 `Invalid`。
  - `goalId` 指向不存在目标返回 `Invalid`。
  - 空标题返回 `Invalid`。
  - 错误日期返回 `Invalid`。
  - 非 `com.jimu.app` 包名返回 `Invalid`。
  - 校验成功返回包含 `ValidatedBackupPayload` 的 `Valid`，恢复接口不直接接受裸 `AppBackupPayloadV1`。

- [ ] **Step 4: 固定 JSON 测试依赖和 codec 测试**
  - 在 `app/build.gradle.kts` 增加 `testImplementation("org.json:json:20240303")`。
  - 该依赖只进入 JVM 测试 classpath，不进入 APK runtime。
  - `BackupJsonCodecTest` 覆盖完整 payload 往返、JSON null、非法 JSON、缺失版本、未知版本。
  - `BackupJsonCodecInstrumentedTest` 在 Android 设备侧复用同一份样本，验证 Android `org.json` 解码结果与 JVM 期望一致。

- [ ] **Step 5: 实现并测试受限 UTF-8 文件读写**
  - 定义 `MAX_BACKUP_BYTES = 10 * 1024 * 1024`。
  - `declaredSizeBytes` 已知且大于上限时，在打开完整内容前返回 `FileTooLarge`。
  - 无论元数据是否存在，都最多读取 `MAX_BACKUP_BYTES + 1` bytes；发现第 `MAX_BACKUP_BYTES + 1` byte 时立即返回 `FileTooLarge`。
  - 使用严格 UTF-8 decoder，非法 UTF-8 返回 `InvalidEncoding`。
  - `writeUtf8()` 写入前检查内容 UTF-8 byte 数不超过同一上限；写入异常返回 `WriteFailed`。
  - 测试精确覆盖：刚好等于上限成功、超过 1 byte 失败、元数据未知仍受限、非法 UTF-8 失败、写入异常失败。

- [ ] **Step 6: 跑 Task 1 测试**
  - 运行：`./gradlew.bat testDebugUnitTest --tests com.jimu.app.data.backup.BackupStreamIoTest --tests com.jimu.app.data.backup.BackupJsonCodecTest --tests com.jimu.app.data.backup.BackupValidatorTest`
  - 运行：`./gradlew.bat connectedDebugAndroidTest`
  - 期望：JVM codec/校验/流读取测试通过，设备侧 codec 往返测试通过。

**风险点：**
- Entity 会随 Room schema 演进，不适合作为永久备份格式；DTO 是兼容层。
- 只校验 JSON 能解析不够，必须校验关系和业务字段。
- 仅依赖 SAF 元数据大小不能阻止超大文件进入内存，必须由 `BackupStreamIo` 执行硬上限。

---

### Task 2: 补齐恢复专用 DAO 能力

**Files:**
- Modify: `app/src/main/java/com/jimu/app/data/local/dao/TaskDao.kt`
- Modify: `app/src/main/java/com/jimu/app/data/local/dao/HabitDao.kt`
- Modify: `app/src/main/java/com/jimu/app/data/local/dao/GoalDao.kt`
- Modify: `app/src/main/java/com/jimu/app/data/local/dao/ReviewDao.kt`

**Interfaces:**
- Produces: `TaskDao.getAllTasksForBackup(): List<TaskEntity>`。
- Produces: `TaskDao.deleteAllTasksForRestore()`。
- Produces: `TaskDao.insertTasksForRestoreAbort(tasks: List<TaskEntity>)`，使用 `OnConflictStrategy.ABORT`。
- 同类方法在 `HabitDao`、`GoalDao`、`ReviewDao` 中使用确定命名，不使用“实际命名按语义”这种开放表述。

- [ ] **Step 1: 为 tasks 增加确定方法**
  - `@Query("SELECT * FROM tasks ORDER BY id ASC") suspend fun getAllTasksForBackup(): List<TaskEntity>`。
  - `@Query("DELETE FROM tasks") suspend fun deleteAllTasksForRestore()`。
  - `@Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertTasksForRestoreAbort(tasks: List<TaskEntity>)`。

- [ ] **Step 2: 为 habits 和 habit_records 增加确定方法**
  - `getAllHabitsForBackup()` 使用 `ORDER BY id ASC`。
  - `getAllHabitRecordsForBackup()` 使用 `ORDER BY id ASC`。
  - `deleteAllHabitRecordsForRestore()`。
  - `deleteAllHabitsForRestore()`。
  - `insertHabitsForRestoreAbort(habits: List<HabitEntity>)`。
  - `insertHabitRecordsForRestoreAbort(records: List<HabitRecordEntity>)`。

- [ ] **Step 3: 为 goals 和 goal_steps 增加确定方法**
  - `getAllGoalsForBackup()` 使用 `ORDER BY id ASC`。
  - `getAllGoalStepsForBackup()` 使用 `ORDER BY id ASC`。
  - `deleteAllGoalStepsForRestore()`。
  - `deleteAllGoalsForRestore()`。
  - `insertGoalsForRestoreAbort(goals: List<GoalEntity>)`。
  - `insertGoalStepsForRestoreAbort(steps: List<GoalStepEntity>)`。

- [ ] **Step 4: 为 daily_reviews 增加确定方法**
  - `getAllReviewsForBackup()` 使用 `ORDER BY id ASC`。
  - `deleteAllReviewsForRestore()`。
  - `insertReviewsForRestoreAbort(reviews: List<ReviewEntity>)`。

- [ ] **Step 5: 编译验证 DAO SQL**
  - 运行：`./gradlew.bat assembleDebug`
  - 期望：Room/KSP 编译通过。

**风险点：**
- 恢复专用插入必须是 `ABORT`，不是当前业务使用的 `REPLACE`。
- 清空表方法只允许 `BackupRepository` 调用，普通业务层不要暴露入口。

---

### Task 3: 实现只读事务导出和当前备份创建

**Files:**
- Create: `app/src/main/java/com/jimu/app/data/backup/BackupTransactionRunner.kt`
- Create: `app/src/main/java/com/jimu/app/data/backup/BackupRepository.kt`
- Modify: `app/src/main/java/com/jimu/app/JimuApp.kt`
- Test: `app/src/test/java/com/jimu/app/data/backup/BackupRepositoryTest.kt`

**Interfaces:**
- Produces: `suspend fun createCurrentBackup(meta: BackupMetaV1): AppBackupPayloadV1`。
- Produces: `suspend fun exportCurrentBackupJson(meta: BackupMetaV1): String`。
- Produces: `interface BackupTransactionRunner { suspend fun <T> runInTransaction(block: suspend () -> T): T }`；生产实现 `RoomBackupTransactionRunner` 包装 `database.withTransaction`。

- [ ] **Step 1: 定义可注入事务边界**
  - `BackupRepository` 构造函数接收 `BackupTransactionRunner` 和四组 DAO，不直接持有 `AppDatabase`。
  - `RoomBackupTransactionRunner` 是唯一直接调用 `database.withTransaction` 的生产类。
  - JVM 测试实现 `SnapshottingFakeTransactionRunner`：进入事务前复制 fake store；block 抛异常时恢复副本并重新抛出。

- [ ] **Step 2: 写 fake DAO 导出测试**
  - 空数据库导出：六个数组为空，但 meta 完整。
  - 非空数据库导出：每张表至少 1 条，DTO 字段完整。
  - 断言六组 DAO 读取都发生在同一次 `runInTransaction` 调用中。

- [ ] **Step 3: 实现只读事务导出**
  - 在 `transactionRunner.runInTransaction { ... }` 中读取六张表，得到同一时刻快照。
  - 读取顺序固定为：tasks, habits, habitRecords, goals, goalSteps, reviews。
  - Entity 转 DTO 显式写转换函数，不用反射和自动序列化。
  - 编码后的 UTF-8 byte 数超过 `MAX_BACKUP_BYTES` 时返回明确的 `BackupTooLarge`，不得生成一个本版本无法重新导入的文件。

- [ ] **Step 4: 接入 `JimuApp`**
  - 初始化 `backupRepository`。
  - 创建 `RoomBackupTransactionRunner(database)`，并连同四组 DAO 传入 `BackupRepository`。

- [ ] **Step 5: 跑导出测试**
  - 运行：`./gradlew.bat testDebugUnitTest --tests com.jimu.app.data.backup.BackupRepositoryTest`
  - 期望：fake DAO 导出测试通过。

**风险点：**
- 依次读取但不在事务里，可能导出到半新半旧的数据快照。
- 导出不应改变任何 `updatedAt`、提醒或业务状态。

---

### Task 4: 拆分恢复流程并实现事务恢复

**Files:**
- Modify: `app/src/main/java/com/jimu/app/data/backup/BackupRepository.kt`
- Test: `app/src/test/java/com/jimu/app/data/backup/BackupRepositoryTest.kt`
- Test: `app/src/androidTest/java/com/jimu/app/data/backup/BackupRestoreTransactionInstrumentedTest.kt`

**Interfaces:**
- Produces: `fun decodeAndValidate(json: String): BackupImportPreviewResult`。
- Produces: `suspend fun restoreValidatedPayload(validated: ValidatedBackupPayload): RestoreDatabaseResult`。
- Does not produce: 一步式 `importBackupJson()`；该接口不得出现。

- [ ] **Step 1: 写 decode/preview JVM 测试**
  - `BackupStreamIo` 已负责文件大小和编码检查；`decodeAndValidate()` 只接收已安全读入的字符串。
  - 合法 JSON 返回 `ReadyToConfirm(validatedPayload, preview)`。
  - 非法 JSON、未知版本和业务校验失败分别返回明确结果。
  - 断言失败路径没有调用事务 runner 或任何 DAO 写方法。

- [ ] **Step 2: 固定 preview 内容**
  - preview 包含 `exportedAt`、tasks 数量、habits 数量、habitRecords 数量、goals 数量、goalSteps 数量、reviews 数量。
  - preview 同时保留 `ValidatedBackupPayload`，用户确认后原样传入恢复接口，不重复解析文件。

- [ ] **Step 3: 写 fake DAO 恢复测试**
  - 当前数据库有旧数据，恢复合法 payload 后旧数据消失、新数据存在。
  - 子表关系保持正确。
  - 中途 fake DAO 抛异常时，`SnapshottingFakeTransactionRunner` 恢复事务前快照，repository 返回 `RestoreFailed`。
  - 测试只能通过成功校验结果取得 `ValidatedBackupPayload`，不能直接把裸 DTO 传给恢复接口。

- [ ] **Step 4: 写真实 Room 仪器测试**
  - 使用真实 Room 数据库创建旧数据。
  - 不使用“重复主键 payload”制造失败，因为它会先被 validator 拒绝，无法进入恢复事务。
  - 在测试数据库上创建仅测试使用的 SQLite trigger：当插入标题为 `__force_restore_failure__` 的 task 时执行 `RAISE(ABORT, 'forced restore failure')`。
  - 构造结构合法、关系合法且包含该标题的 payload，使其通过 validator 后在真实插入阶段失败。
  - 调用 `restoreValidatedPayload()`。
  - 断言事务回滚，旧数据仍存在，未留下半套新数据。

- [ ] **Step 5: 实现恢复顺序**
  - 进入事务前只接受已经校验通过的 `ValidatedBackupPayload`。
  - 事务内先清空子表：`habit_records`, `goal_steps`。
  - 再清空普通表：`daily_reviews`, `tasks`, `habits`, `goals`。
  - 再插入主表：`tasks`, `habits`, `goals`, `reviews`。
  - 再插入子表：`habit_records`, `goal_steps`。
  - 所有恢复插入使用 `ABORT`。

- [ ] **Step 6: 在真实 Room 仪器测试中验证 id 自增**
  - 恢复含 `id=100` 的 task。
  - 恢复后通过现有 `TaskRepository.addTask()` 新增任务。
  - 断言新 id 大于 100，且不覆盖恢复数据。
  - 不用 fake DAO 证明 SQLite 自增序列行为。

- [ ] **Step 7: 跑恢复测试**
  - 运行：`./gradlew.bat testDebugUnitTest --tests com.jimu.app.data.backup.BackupRepositoryTest`
  - 运行：`./gradlew.bat connectedDebugAndroidTest`
  - 期望：fake 分支测试和真实 Room 事务测试通过。

**风险点：**
- 恢复前保险备份不属于 `restoreValidatedPayload()`，必须由 UI 流程在调用恢复前强制完成。
- 没有真实 Room 测试时，不能宣称事务回滚已验证。

---

### Task 5: 恢复后重建待办提醒

**Files:**
- Create: `app/src/main/java/com/jimu/app/data/backup/BackupReminderRebuilder.kt`
- Create: `app/src/main/java/com/jimu/app/reminder/TaskReminderController.kt`
- Modify: `app/src/main/java/com/jimu/app/data/backup/BackupRepository.kt`
- Modify: `app/src/main/java/com/jimu/app/JimuApp.kt`
- Modify: `app/src/main/java/com/jimu/app/reminder/TaskReminderScheduler.kt`
- Test: `app/src/test/java/com/jimu/app/data/backup/BackupReminderRebuilderTest.kt`

**Interfaces:**
- Produces: `suspend fun rebuildAfterSuccessfulRestore(oldTaskIds: List<Long>): ReminderRebuildResult`。
- Produces: `interface TaskReminderController { fun cancel(taskId: Long); fun schedule(task: TaskEntity, mayRequestExactAlarmPermission: Boolean = false) }`。
- Consumes: `TaskReminderController.cancel(taskId)`。
- Consumes: `TaskReminderController.schedule(task, mayRequestExactAlarmPermission)`。
- Consumes: `TaskRepository.getFutureReminderTasks(nowMillis)`。

- [ ] **Step 1: 定义可注入提醒边界**
  - `TaskReminderController` 只定义现有调度所需的 `cancel()` 和 `schedule()`。
  - `TaskReminderScheduler` 实现该接口，原有 `TasksViewModel` 和 `JimuApp` 调用方式保持不变。
  - `BackupReminderRebuilder` 只依赖接口，JVM 测试使用记录调用和按指定 task id 抛异常的 fake controller。

- [ ] **Step 2: 写提醒重排测试**
  - oldTaskIds 包含 1、2、3，恢复后未来未完成任务是 2、4。
  - 断言先 cancel 1、2、3。
  - 断言再 schedule 2、4。

- [ ] **Step 3: 写逐项失败汇总测试**
  - fake controller 在 cancel 2 和 schedule 4 时抛异常。
  - 断言 cancel 1、2、3 和 schedule 2、4 全部都被尝试，单项失败不阻断后续项目。
  - 结果返回 `PartialFailure(failedCancelIds = [2], failedScheduleIds = [4])`，不回滚数据库。
  - UI 文案必须能显示“数据恢复成功，但提醒重建失败”。

- [ ] **Step 4: 在恢复事务内捕获 old task id**
  - `restoreValidatedPayload()` 在清空旧 tasks 前读取全部旧 task id。
  - 只有事务成功时，`RestoreDatabaseResult.Success` 才带回 `oldTaskIds` 和导入数量。
  - ViewModel 仅在收到 `Success` 后调用提醒重排。
  - 数据库事务失败时不取消任何旧提醒。

- [ ] **Step 5: 实现重排顺序**
  - 先取消所有旧 task id 的提醒。
  - 再读取恢复后的未来未完成任务。
  - 对每个未来未完成任务调用 `TaskReminderController.schedule(task, mayRequestExactAlarmPermission = false)`。
  - 每个 cancel/schedule 独立 `runCatching`，最终统一汇总失败 ID。

- [ ] **Step 6: 接入恢复结果**
  - 恢复接口返回数据库恢复结果。
  - ViewModel 在数据库恢复成功后调用 `BackupReminderRebuilder`。
  - 若提醒重排失败，最终状态为“数据恢复成功，提醒重建失败”，不能显示完全成功。

**风险点：**
- 恢复前取消旧提醒会导致数据库失败后提醒丢失；必须事务成功后再取消。
- 只 schedule 新任务不 cancel 旧任务，会留下已删除任务提醒。

---

### Task 6: 设置页流程和状态机

**Files:**
- Create: `app/src/main/java/com/jimu/app/ui/settings/SettingsScreen.kt`
- Create: `app/src/main/java/com/jimu/app/viewmodel/SettingsViewModel.kt`
- Create: `app/src/main/java/com/jimu/app/viewmodel/SettingsViewModelFactory.kt`
- Modify: `app/src/main/java/com/jimu/app/navigation/Routes.kt`
- Modify: `app/src/main/java/com/jimu/app/navigation/AppNavHost.kt`
- Modify: `app/src/main/java/com/jimu/app/ui/home/HomeScreen.kt`
- Test: `app/src/test/java/com/jimu/app/viewmodel/SettingsViewModelTest.kt`

**Interfaces:**
- Produces: `sealed interface SettingsUiState`，至少包含 `Idle`, `Exporting`, `ReadingImportFile`, `PreviewReady`, `WaitingForPreImportBackupLocation`, `SavingPreImportBackup`, `RestoringDatabase`, `RebuildingReminders`, `Success`, `PartialSuccess`, `Error`。
- UI contract: 普通导出和保险备份使用 `ActivityResultContracts.CreateDocument("application/json")`。
- UI contract: 导入使用 `ActivityResultContracts.OpenDocument()`，类型固定为 `arrayOf("application/json", "text/json", "text/plain")`。

- [ ] **Step 1: 写状态机测试**
  - 导出成功：`Idle -> Exporting -> Success`。
  - 导出创建文件被用户取消：回到 `Idle`，不显示错误或成功。
  - 导出写入失败：进入 `Error`。
  - 导入文件读取成功并校验：`Idle -> ReadingImportFile -> PreviewReady`。
  - 导入选择文件被用户取消：保持 `Idle`，不显示错误。
  - 导入文件超过上限或 UTF-8 非法：进入 `Error`，不调用 decode 或数据库。
  - 用户取消保险备份保存：`PreviewReady -> WaitingForPreImportBackupLocation -> Idle`，不恢复数据库。
  - 保险备份写入失败：进入 `Error`，不恢复数据库。
  - 数据恢复成功但提醒重排失败：进入 `PartialSuccess`。

- [ ] **Step 2: 实现普通导出 SAF 流程**
  - 用户点击“导出数据备份”后，ViewModel 先生成 JSON；生成失败或超过上限时不启动文件选择器。
  - 生成成功后启动 `CreateDocument("application/json")`。
  - 默认文件名固定为 `jimu-backup-YYYYMMDD-HHmmss.json`。
  - 用户取消时回到 `Idle`，不显示错误。
  - 获取 Uri 后，在 `Dispatchers.IO` 打开 `OutputStream` 并调用 `BackupStreamIo.writeUtf8()`。
  - 只有 stream 写入和关闭均成功后才能显示“备份已导出”。

- [ ] **Step 3: 实现导入文件选择和安全读取**
  - 用户点击“从备份恢复”后启动 `OpenDocument()`，MIME 类型固定为 `application/json`、`text/json`、`text/plain`。
  - 用户取消时回到 `Idle`，不显示错误。
  - 获取 Uri 后先通过 `OpenableColumns.SIZE` 读取声明大小；大小未知时传 null。
  - 在 `Dispatchers.IO` 打开 `InputStream` 并调用 `BackupStreamIo.readUtf8Limited()`；不得使用无限制 `readText()` 或 `readBytes()`。
  - 只有受限读取成功后才把字符串交给 ViewModel 的 `decodeAndValidate()` 流程。

- [ ] **Step 4: 实现强制保险备份流程**
  - 用户先选择导入文件。
  - UI 使用受限流读取文件并调用 `decodeAndValidate()`。
  - 显示备份日期和各类数据数量。
  - 用户确认恢复。
  - ViewModel 调用 `exportCurrentBackupJson(meta)` 生成当前数据保险备份；生成失败或超过上限时终止恢复。
  - 强制弹出 `CreateDocument("application/json")` 保存当前数据。
  - 默认文件名固定为 `jimu-pre-restore-YYYYMMDD-HHmmss.json`。
  - 保存取消或失败，终止恢复。
  - 保存成功后，使用 `PreviewReady` 中保留的同一个 `ValidatedBackupPayload` 调用 `restoreValidatedPayload()`。
  - 数据库恢复成功后调用提醒重排。

- [ ] **Step 5: 实现文件读写边界**
  - UI 层只处理 Uri 和 stream。
  - ViewModel 不保存 Uri，不直接依赖 Android 文件 API。
  - UI 将文件读取结果字符串、写入成功或失败结果传回 ViewModel。
  - 文件读取、写入、JSON 处理都在 `Dispatchers.IO`。

- [ ] **Step 6: 实现明文风险提示**
  - 设置页导出区域显示：“备份文件为未加密明文，可能包含待办、目标和复盘内容，请妥善保管。”
  - 导入确认框显示：“恢复会替换当前数据；恢复前会先要求保存当前数据备份。”

- [ ] **Step 7: 接通导航和返回**
  - `Routes.kt` 新增 `data object Settings : Routes("settings", "设置")`。
  - `HomeScreen` 增加 `onOpenSettings: () -> Unit` 参数。
  - `AppNavHost` 中首页设置按钮跳转设置页。
  - `AppNavHost` 在 `currentDestination?.route == Routes.Settings.route` 时不渲染 `JimuBottomBar`。
  - 设置页提供返回按钮，返回后底部导航仍停留在首页状态。

- [ ] **Step 8: 跑 ViewModel 测试和构建**
  - 运行：`./gradlew.bat testDebugUnitTest --tests com.jimu.app.viewmodel.SettingsViewModelTest`
  - 运行：`./gradlew.bat assembleDebug`
  - 期望：通过。

**风险点：**
- 若状态只用 `isBusy + message`，恢复流程会变得不可控，难以处理取消、失败和部分成功。
- 保险备份必须在数据库恢复前成功落盘。

---

### Task 7: 仪器测试、手测和文档更新

**Files:**
- Modify: `FACT_REPORT.md`
- Modify: `RELEASE_PLAN.md`
- Modify: `AI_PLAN.md`
- Modify: `DEV_LOG.md`

**Interfaces:**
- Consumes: 前面任务的实际实现和测试结果。
- Produces: 准确的项目状态文档，不夸大数据安全完成度。

- [ ] **Step 1: 跑 JVM 单测**
  - 运行：`./gradlew.bat testDebugUnitTest`
  - 期望：全部通过。

- [ ] **Step 2: 跑构建**
  - 运行：`./gradlew.bat assembleDebug`
  - 期望：构建通过。

- [ ] **Step 3: 跑仪器测试**
  - 运行：`./gradlew.bat connectedDebugAndroidTest`
  - 期望：Android JSON codec 往返和真实 Room 事务回滚测试通过。
  - 如果当前环境没有设备或模拟器，必须在 `DEV_LOG.md` 明确记录未运行原因，不得宣称已完成设备验证。

- [ ] **Step 4: 手测正常路径**
  - 首页点击设置。
  - 导出备份文件。
  - 用文本查看器确认导出文件是 JSON，文件名和明文风险提示符合预期。
  - 新增一条未来待办并设置提醒。
  - 从旧备份恢复。
  - 在恢复确认前成功保存 `jimu-pre-restore-*.json` 保险备份。
  - 确认恢复前新增待办消失。
  - 确认旧任务提醒不会再触发，新恢复的未来未完成任务能重新安排提醒。
  - 恢复后再新增一条待办，确认生成的新 id 不冲突且原恢复数据未被覆盖。

- [ ] **Step 5: 手测异常路径**
  - 选择非法 JSON 文件。
  - 选择超过 10 MiB 的文件，确认受限读取立即拒绝且 App 不崩溃。
  - 取消普通导出文件创建，确认不显示成功。
  - 取消导入文件选择，确认不显示错误。
  - 在保险备份保存位置选择时取消。
  - 保险备份写入失败、恢复中途失败和提醒重排部分失败由 JVM/仪器测试覆盖；如果设备环境可以稳定构造相同故障，再补充手测，不把它们作为必须人工伪造的步骤。

- [ ] **Step 6: 更新文档**
  - `FACT_REPORT.md`：写入已支持本地 JSON 导出/导入、恢复前强制保存当前备份、恢复后重排提醒；保留 R10 数据层安全债。
  - `RELEASE_PLAN.md`：只有在导出、导入、设置入口、验证都完成后，才把 R8/R9/R12/R13 标为已完成；否则标“部分完成”。
  - `AI_PLAN.md`：只更新阶段判断，不写执行流水。
  - `DEV_LOG.md`：记录修改文件、测试命令、测试结果、手测结果和未覆盖项。

**风险点：**
- 没有 `connectedDebugAndroidTest` 或等价手测时，不能把事务回滚和文件选择器路径写成已验证。
- R10 唯一约束、索引、去重 migration 仍不在本计划范围内，文档必须保留风险。

---

## 推荐执行顺序

1. Task 1：先固定 DTO、JSON 和校验规则。
2. Task 2：补恢复专用 DAO 能力，明确 `ABORT` 插入。
3. Task 3：实现只读事务导出和当前备份创建。
4. Task 4：实现拆分后的恢复流程和真实 Room 事务测试。
5. Task 5：恢复成功后重建待办提醒。
6. Task 6：实现设置页、强制保险备份和状态机。
7. Task 7：完成仪器测试、手测和文档更新。

## 不在本计划范围内

- 不做智能合并导入。
- 不做云同步。
- 不做账号系统。
- 不修改 Room schema version。
- 不处理 `habit_records(habitId, recordDate)` 唯一约束；该项保留给 R10。
- 不做 release 签名、App 图标、启动画面。
- 不做大规模 UI 重做。

## 自检结果

- 已修正恢复后提醒重排缺口：新增 Task 5，恢复成功后取消旧任务提醒并重新安排恢复后的未来未完成任务提醒。
- 已修正保险备份时序矛盾：导入流程改为先选择文件、解析校验、显示 preview、用户确认、强制保存当前备份、再恢复数据库、再重排提醒。
- 已固定 JSON 测试策略：JVM 测试使用 `org.json:json:20240303`，Android 设备测试复核平台实现；真实 Room 事务仍由仪器测试验证。
- 已增加可注入测试边界：`BackupTransactionRunner` 隔离 Room 事务，`TaskReminderController` 隔离 Android 提醒调度。
- 已补充完整性校验：主键唯一、外键式关系、必填字段、日期格式、包名、文件大小和 `ABORT` 插入。
- 已固定文件安全策略：导入采用 10 MiB 有上限流读取，导出使用同一上限，禁止无限制 `readText()` / `readBytes()`。
- 已补齐普通导出、导入选择和保险备份的 SAF 契约、MIME 类型、默认文件名及取消/失败语义。
- 已去掉模糊表述：DAO 方法名、排序、恢复流程、保险备份流程、底部导航行为、mood 口径和完成状态均固定。
