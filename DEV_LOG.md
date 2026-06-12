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
