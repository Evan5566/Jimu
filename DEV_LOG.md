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
