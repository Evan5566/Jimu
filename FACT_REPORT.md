# FACT_REPORT.md

本文件记录项目当前事实。产品路线写入 `AI_PLAN.md`，执行记录写入 `DEV_LOG.md`。

## 更新时间

2026-06-12

## 项目概况

- 项目名称：迹目。
- 类型：Android 单机个人管理 App。
- 包名：`com.jimu.app`。
- 当前主模块：`app`。
- 主要功能：待办、习惯、目标、已完成记录。
- 待补核心功能：复盘。

## 当前技术事实

- 语言：Kotlin。
- UI：Jetpack Compose。
- 数据库：Room / SQLite。
- 导航：Navigation Compose。
- 语音识别：Android 系统 `SpeechRecognizer`。
- 任务语音解析：本地规则解析 `MockTaskParseRepository`。
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
BUILD SUCCESSFUL in 37s
```

APK 输出路径：

```text
F:\jimuapp\app\build\outputs\apk\debug\app-debug.apk
```

## 当前代码事实

- `app/src/main/java/com/jimu/app/ui/home/HomeScreen.kt` 中 `VoiceInputSheet(` 当前只剩 1 处调用。
- T1 已完成：首页重复语音弹窗调用已清理。
- 仍未执行 T2 及之后任务。

## 当前已知风险

- Room 当前仍需做数据安全整改，后续应处理 schema 导出和 migration。
- 复盘功能尚未实现。
- 目标语音状态仍待后续 T2 独立处理。
- 习惯取消今日打卡仍待后续 T3 独立处理。
- 核心逻辑测试仍待后续 T9 补齐。
