# 迹目当前事实快照

本文件只记录项目当前事实，方便后续 AI 或开发者快速接手。产品路线写入 `AI_PLAN.md`，执行流水写入 `DEV_LOG.md`，发布准备清单写入 `RELEASE_PLAN.md`。

更新时间：2026-06-18

## 1. 项目概况

- 项目名称：迹目。
- 类型：Android 单机个人管理 App。
- 包名：`com.jimu.app`。
- 当前主模块：`app`。
- 当前工作分支：`codex/data-backup-restore-mvp`。
- 本轮功能基线提交：`20ff4c3`。

## 2. 当前功能状态

### 首页

- 展示今日待处理、完成节奏、当前目标推进和今日复盘卡片。
- 首页右下角保留语音录入口。
- 首页“今日复盘”卡与底栏“复盘”入口共用同一个一级 tab 导航。

### 待办

- 支持新增、编辑、删除、完成、顺延、日期和时间。
- 支持“今日 / 全部 / 已完成”三段切换。
- 已完成记录迁回待办页“已完成”分支，按日期归档展示，可回退为未完成。
- 删除前已经补齐二次确认。

### 习惯

- 支持新增、编辑、删除、今日打卡、取消今日打卡。
- 支持连续天数、累计次数和今日/全部切换。
- 删除前已经补齐二次确认。

### 目标

- 支持新增、编辑、删除、步骤管理和进度统计。
- 支持按本周、本月、本年等周期分组展示。
- 目标删除确认原本已具备，当前保持。

### 复盘

- 支持今日复盘编辑，字段为“做得好的事 / 遇到的问题 / 明日重点”。
- 支持保存后留在当前页并显示“已保存”。
- 支持历史复盘列表和指定日期编辑。
- 今日复盘页顶部展示本地“今日成果草稿”，聚合当前待办、习惯和目标数据。
- 历史日期编辑页不展示今天的成果草稿。

### 语音

- 使用 Android 系统 `SpeechRecognizer`。
- 当前用于语音新增待办、习惯和目标。
- 任务语音解析仍为本地规则解析 `MockTaskParseRepository`。
- 部分设备的系统语音服务依赖联网；无网时会先提示“当前语音识别需要联网”。

### 提醒

- 当前只支持待办到期提醒。
- 已接入 Android 13+ 通知权限、高优先级通知 channel、精确闹钟优先调度和降级路径。
- 支持应用启动、设备重启、应用升级、精确闹钟授权变化后的未来提醒重排。
- 数据恢复成功后会先取消恢复前的旧待办提醒，再为恢复后的未来未完成待办重建提醒。

### 设置与本地备份

- 首页右上角设置按钮已接入二级设置页，设置页隐藏底部导航并提供返回入口。
- 设置页显示版本信息、本地 JSON 导出入口和 JSON 恢复入口。
- 备份覆盖待办、习惯、习惯记录、目标、目标步骤和复盘，格式使用独立 V1 DTO，不直接暴露 Room Entity。
- 导入采用“受限读取 -> JSON 解码 -> 完整校验 -> 预览确认 -> 强制保存当前数据保险备份 -> 事务全量替换 -> 提醒重建”流程。
- 导入预览显示备份时间和各类数据数量。
- 备份文件为未加密 UTF-8 JSON，导入和导出统一限制为 10 MiB。
- 第一版恢复策略是完整备份全量替换，不做智能合并。
- 2026-06-18 用户已在真机完成基本路径测试，设置入口、导出、恢复预览、恢复前保险备份和数据恢复整体成功。

## 3. 当前技术事实

- 语言：Kotlin。
- UI：Jetpack Compose。
- 数据库：Room / SQLite。
- 导航：Navigation Compose。
- 当前未发现 Java 源文件。
- 当前未发现 XML layout 页面。
- 主题：`JimuTheme` 默认跟随系统深色模式。
- 浅色方案：`JimuLightColorScheme`。
- 深色方案：`JimuDarkColorScheme`。
- 待办和习惯卡片容器色通过 `panelColor(darkTheme)` 随主题切换。

## 4. 数据层事实

- 当前 Room database version：5。
- 已启用 `exportSchema = true`。
- 已有 schema：
  - `app/schemas/com.jimu.app.data.local.AppDatabase/4.json`
  - `app/schemas/com.jimu.app.data.local.AppDatabase/5.json`
- 已有正式 migration：`MIGRATION_4_5`。
- `MIGRATION_4_5` 只创建 `daily_reviews` 表，不修改旧表。
- `JimuApp` 已注册 `.addMigrations(MIGRATION_4_5)`。
- 破坏性迁移 fallback 已移除。

当前主要表：

- `tasks`
- `habits`
- `habit_records`
- `goals`
- `goal_steps`
- `daily_reviews`

当前主要 Repository：

- `TaskRepository`
- `HabitRepository`
- `GoalRepository`
- `ReviewRepository`
- `DailyDigestRepository`
- `BackupRepository`

备份恢复边界：

- `BackupTransactionRunner` 隔离 Room 事务，生产实现使用 `AppDatabase.withTransaction`。
- 恢复专用批量插入使用 `OnConflictStrategy.ABORT`。
- `TaskReminderController` 隔离 Android 提醒调度，便于 JVM 测试逐项失败汇总。
- Room database version 仍为 5，本轮没有 schema 变更或 migration。

## 5. 权限事实

当前保留的关键权限：

- `RECORD_AUDIO`：用于系统语音识别。
- `ACCESS_NETWORK_STATE`：用于语音识别前联网预检。
- `POST_NOTIFICATIONS`：用于 Android 13+ 待办提醒通知。
- `SCHEDULE_EXACT_ALARM`：用于优先安排精确待办提醒。

当前已移除：

- `INTERNET`：App 没有自有联网能力；系统语音服务是否联网由系统侧处理。

## 6. 构建环境事实

- Gradle Wrapper：`9.3.1`。
- 项目需要 Java 21 才能稳定运行 Gradle 构建。
- 本机可用 JDK 21：

```text
C:\Users\Evan\.jdks\zulu21.50.19-ca-jdk21.0.11-win_x64\zulu21.50.19-ca-jdk21.0.11-win_x64
```

如果命令行默认 Java 不是 21，可在当前 PowerShell 会话中临时设置：

```powershell
$env:JAVA_HOME='C:\Users\Evan\.jdks\zulu21.50.19-ca-jdk21.0.11-win_x64\zulu21.50.19-ca-jdk21.0.11-win_x64'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

常用验证命令：

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

最近文档记录中的构建结果：

```text
testDebugUnitTest --rerun-tasks: BUILD SUCCESSFUL（91 tests，0 failures）
assembleDebug --rerun-tasks: BUILD SUCCESSFUL
assembleDebugAndroidTest --rerun-tasks: BUILD SUCCESSFUL
connectedDebugAndroidTest: 未执行成功；真机已连接，但系统以 INSTALL_FAILED_USER_RESTRICTED 拒绝安装测试 APK，执行 0 tests
```

Debug APK 输出路径：

```text
F:\jimuapp\app\build\outputs\apk\debug\app-debug.apk
```

## 7. 当前已知风险

- 数据导出/导入基本路径已通过用户真机测试，但真实 Room 回滚和 SQLite 自增仪器测试仍未自动执行；当前小米真机拒绝通过 USB 安装测试 APK。
- 非法 JSON、超 10 MiB、保险备份写入失败和提醒到点行为未收到逐项验收反馈，仍保留为待覆盖边界。
- 备份文件为未加密明文，可能包含待办、目标和复盘内容，需要用户妥善保管。
- `habit_records(habitId, recordDate)` 尚无唯一约束。
- 部分表尚无外键、索引和显式事务保护。
- `insertHabitRecord` 的 `REPLACE + autoGenerate` 去重问题尚未彻底处理。
- v4 旧库覆盖安装到 v5 的设备/模拟器回归尚未补测。
- `TaskEntity` 没有 `completedAt`，当前不能严谨统计“今日完成”或“本周完成”。
- 提醒准点性依赖设备授权和厂商后台策略；非精确降级路径不承诺严格准点。
- 部分国产 ROM 可能需要用户手动关闭电池优化或加白名单。
- mood 输入、真实快照、复盘删除 UI、统计图表和 AI 总结尚未实现。
- 核心 ViewModel / Repository 测试覆盖仍待后续质量加固阶段补齐。

## 8. 文档查阅建议

- 看长期规则和开发约束：`CLAUDE.md`。
- 看当前产品路线：`AI_PLAN.md`。
- 看当前事实快照：本文件。
- 看发布前任务：`RELEASE_PLAN.md`。
- 查某次任务具体做了什么：`DEV_LOG.md`。
