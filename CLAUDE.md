# CLAUDE.md

本文件写给未来参与本项目的 AI 执行者。只记录长期规则和环境信息；具体执行过程写入 `DEV_LOG.md`，产品路线写入 `AI_PLAN.md`，项目事实快照写入 `FACT_REPORT.md`。

## 文档职责和阅读顺序

接手项目时建议按以下顺序阅读：

1. `CLAUDE.md`：长期规则、技术栈、构建环境和开发约定。
2. `FACT_REPORT.md`：当前事实快照，包括现有功能、数据库状态、权限和已知风险。
3. `AI_PLAN.md`：产品定位、阶段判断和下一步路线。
4. `RELEASE_PLAN.md`：发布候选版本的任务清单、状态和验收口径。
5. `DEV_LOG.md`：历史执行流水，仅在需要追溯某次任务细节时阅读。

文档维护原则：

- 不要把任务流水写进 `AI_PLAN.md`；路线图只保留阶段判断和优先级。
- 不要把过期历史堆进 `FACT_REPORT.md`；事实快照只描述当前状态。
- 不要把产品方向写散到 `RELEASE_PLAN.md`；发布文档只管理发布前任务和验收。
- `DEV_LOG.md` 可以保留详细过程，但顶部需要维持摘要和索引，方便快速定位历史。

## 项目简介

迹目是一个 Android 个人管理 App，用于管理待办、习惯、目标、完成记录和日常复盘，当前已经具备基础复盘闭环。

用户不是专业软件开发者，主要背景是 STM32、ESP32 和嵌入式 Linux。解释关键改动时，请使用通俗中文，必要时类比嵌入式分层：

- `AndroidManifest.xml` / `Application`：类似启动入口和系统初始化。
- Room / DAO：类似本地存储驱动接口。
- Repository / ViewModel：类似业务封装和状态机。
- Compose Screen：类似人机界面层。

## 长期技术栈

- 语言：Kotlin。
- UI：Jetpack Compose，当前不是 XML layout 页面。
- 数据库：Room，底层 SQLite。
- 导航：Navigation Compose。
- 语音：Android 系统 `SpeechRecognizer`，任务解析当前保留本地规则解析。
- 项目结构：单模块 Android App，包名 `com.jimu.app`。

## 构建环境

- 项目需要 Java 21 才能稳定运行 Gradle 构建。
- 本机可用 JDK 21 路径：

```text
C:\Users\Evan\.jdks\zulu21.50.19-ca-jdk21.0.11-win_x64\zulu21.50.19-ca-jdk21.0.11-win_x64
```

- 如果命令行默认 `java -version` 不是 21，可以在当前 PowerShell 会话里临时设置：

```powershell
$env:JAVA_HOME='C:\Users\Evan\.jdks\zulu21.50.19-ca-jdk21.0.11-win_x64\zulu21.50.19-ca-jdk21.0.11-win_x64'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

## 常用命令

常用 Debug 构建命令：

```powershell
.\gradlew.bat assembleDebug
```

本地 JVM 单元测试：

```powershell
.\gradlew.bat testDebugUnitTest
```

设备或模拟器测试：

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

## 开发约定

- 不要做无关重构。
- 不要随意删除文件。
- 不要随意修改 Gradle 配置、SDK 版本、Kotlin 版本、Room 版本、Compose 版本。
- 不要为了“架构更漂亮”引入 Hilt/Dagger/Koin 等新框架。
- 不要把 `build/`、`.gradle/`、`.idea/` 里的产物当作源码依据。
- 修改前先阅读相关代码，尽量沿用现有 Compose + ViewModel + Repository + Room 分层。
- 数据库改动必须谨慎，涉及 Room schema、migration、用户数据保留时，要先说明风险。
- 语音功能当前不要默认引入云服务或大模型 API。
- 每次改动后必须运行合适的验证命令；如果不能运行，要明确说明原因。

## 验证规则

- 只改 Markdown 文档：读取文件确认内容即可，可以不跑 Android 构建，但要说明未跑构建的原因。
- 改 Kotlin 业务逻辑：至少运行 `.\gradlew.bat assembleDebug`；涉及纯逻辑时也运行 `.\gradlew.bat testDebugUnitTest`。
- 改 Compose 页面或导航：运行 `.\gradlew.bat assembleDebug`，并给出模拟器手动验证步骤。
- 改 Room Entity/DAO/Database：运行 `.\gradlew.bat assembleDebug` 和相关测试，并明确说明迁移策略。
- 构建失败时，必须继续定位并修复，除非失败原因明确属于外部环境且无法在当前环境处理。
