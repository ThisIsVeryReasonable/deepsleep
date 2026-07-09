# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

这是一个原生 Android 小工具应用，用于查看 DeepSeek API 账户余额。支持 App 内展示、桌面小部件、通知栏实时显示以及多 API key 切换。

## 技术栈

- **语言**：Kotlin
- **UI**：Jetpack Compose + Material3
- **架构**：轻量级 MVVM
- **网络**：Retrofit + OkHttp + Kotlinx Serialization
- **后台任务**：WorkManager
- **安全存储**：EncryptedSharedPreferences + Android Keystore
- **构建工具**：Gradle 8.6 + Android Gradle Plugin 8.3

## 开发环境要求

- JDK 17（AGP 8.3 必需）
- Android SDK（platforms;android-34、build-tools;34.0.0）
- Android Studio Hedgehog 或更高版本（推荐）

项目当前未包含 Gradle Wrapper。首次使用建议用 Android Studio 打开，由 IDE 自动下载 Gradle 并生成 wrapper；或在本地配置好 Gradle 8.6 与 Android SDK 后使用命令行构建。

## 常用命令

```bash
# 编译 Debug APK
gradle assembleDebug

# 清理构建产物
gradle clean

# 重新安装到已连接设备（需 platform-tools）
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 运行单元测试（当前项目未配置测试源集，添加后使用）
gradle test

# 运行单个测试类/方法（添加测试后）
gradle testDebugUnitTest --tests "com.example.deepseekbalance.ExampleUnitTest"
```

> 注：若系统未全局安装 `gradle`，请将 Gradle 8.6 的 `bin` 目录加入 PATH，或让 Android Studio 生成 `gradlew` 后改用 `./gradlew`。

## 架构概览

应用采用分层结构，各层职责如下：

```
UI 层 (Compose Screen + ViewModel)
    ↓
Repository 层 (BalanceRepository / KeyRepository)
    ↓
数据源层 (DeepSeekApi / SecureKeyStorage / SettingsStore)
    ↓
系统能力 (AppWidgetManager / NotificationManager / WorkManager)
```

### 数据流

1. 用户在 Compose 界面操作（添加/切换 key、刷新、修改设置）。
2. `BalanceViewModel` 调用 `KeyRepository` 或 `BalanceRepository`。
3. `BalanceRepository` 通过 `DeepSeekApi` 请求 `GET https://api.deepseek.com/user/balance`。
4. 结果更新 `ViewModel` 状态，驱动 Compose UI 重新渲染。
5. 同时同步更新桌面小部件（`BalanceWidgetUpdater`）和通知栏（`BalanceNotificationHelper`）。
6. `BalanceUpdateWorker` 通过 `WorkManager` 按设定间隔在后台重复刷新。

### 关键模块

- **`ui/screen/`**：`BalanceScreen`（主界面）、`KeyManagementScreen`（key 管理）。
- **`ui/viewmodel/BalanceViewModel.kt`**：唯一 ViewModel，承载页面状态并协调刷新/切换/设置。
- **`data/network/`**：`DeepSeekApi` 接口定义、`BalanceRepository` 错误处理、`DeepSeekApiClient` Retrofit 工厂。
- **`data/security/`**：`SecureKeyStorage` 加密存储 API key；`SettingsStore` 普通设置持久化。
- **`widget/`**：`BalanceWidgetProvider` 桌面小部件、`BalanceWidgetUpdater` 视图更新、`BalanceWidgetScheduler` WorkManager 调度。
- **`notification/BalanceNotificationHelper.kt`**：通知栏余额展示与取消。
- **`data/worker/BalanceUpdateWorker.kt`**：后台刷新任务。

## 安全与隐私

- API key 仅保存在 `EncryptedSharedPreferences` 中，由 Android Keystore 保护。
- 日志中对 `Authorization` 头做脱敏处理。
- `AndroidManifest.xml` 设置 `android:allowBackup="false"`，并排除本地数据备份。
- 通知使用 `VISIBILITY_SECRET`，锁屏时不显示余额。

## 设计文档

项目文档位于 `docs/`：

- `docs/requirements.md`：需求与验收标准
- `docs/design.md`：技术选型、架构、数据流、UI/小部件/通知设计

进行 UI 改动前建议先阅读 `docs/design.md`，保持设计系统一致。

## 构建产物

Debug APK 默认输出路径：

```
app/build/outputs/apk/debug/app-debug.apk
```

### APK 版本命名规则

每次打包完成后，将 APK 文件重命名为 `app-debug-vX.Y.apk`，版本从 `v1.0` 开始：

- 小版本号（`.` 后）范围为 `0-9`，每打包一次 `+1`。
- 小版本号达到 `9` 后，下一次大版本号（`.` 前）`+1`，小版本号归零。

示例序列：`v1.0` → `v1.1` → … → `v1.9` → `v2.0` → `v2.1`。

## 注意事项

- 当前未配置单元测试与 UI 测试；新增测试时请放在 `app/src/test/` 与 `app/src/androidTest/`。
- 小部件和通知依赖 WorkManager，HyperOS 等系统可能限制后台任务，需在「应用信息 → 省电策略」中设为「无限制」以保证刷新稳定。
- `BalanceViewModel` 中 `notificationEnabled` 为 Compose 状态属性，如需切换通知状态请调用 `updateNotificationState(enabled)`，不要直接命名冲突的 setter。
