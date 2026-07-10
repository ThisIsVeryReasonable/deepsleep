# DeepSeek 余额助手

一个原生 Android 小工具应用，用于实时查看 [DeepSeek API](https://api.deepseek.com) 账户余额。采用简洁现代的 Material3 设计风格，支持 App 内展示、桌面小部件、通知栏实时显示以及多 API key 切换。

> 本应用仅读取账户余额信息，不会上传或分享你的 API key。

---

## 功能特性

- **余额查看**：主界面实时展示 DeepSeek 账户可用余额与货币单位。
- **多 Key 管理**：支持添加、切换、删除多个 API key，方便区分个人/团队账户。
- **桌面小部件**：一键添加 1×1 / 2×1 小部件，无需打开 App 即可查看余额。
- **通知栏显示**：开启后可在通知栏常驻显示当前余额，支持锁屏隐藏（`VISIBILITY_SECRET`）。
- **后台自动刷新**：基于 WorkManager 按设定间隔自动刷新余额。
- **简洁现代的界面**：基于 Material3 设计系统，统一图标、主题、小部件背景与配色。

---

## 截图

（此处可补充 App 主界面、小部件与通知栏截图）

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material3 |
| 架构 | 轻量级 MVVM |
| 网络 | Retrofit + OkHttp + Kotlinx Serialization |
| 后台任务 | WorkManager |
| 安全存储 | EncryptedSharedPreferences + Android Keystore |
| 构建工具 | Gradle 8.6 + Android Gradle Plugin 8.3 |

---

## 开发环境

- JDK 17（AGP 8.3 必需）
- Android SDK（`platforms;android-34`、`build-tools;34.0.0`）
- Android Studio Hedgehog 或更高版本（推荐）

项目当前未包含 Gradle Wrapper。首次使用建议用 Android Studio 打开，由 IDE 自动下载 Gradle 并生成 wrapper；或在本地配置好 Gradle 8.6 与 Android SDK 后使用命令行构建。

---

## 快速开始

```bash
# 编译 Debug APK
gradle assembleDebug

# 清理构建产物
gradle clean

# 重新安装到已连接设备（需 platform-tools）
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

> 若系统未全局安装 `gradle`，请将 Gradle 8.6 的 `bin` 目录加入 PATH，或让 Android Studio 生成 `gradlew` 后改用 `./gradlew`。

---

## 项目结构

```
app/src/main/java/com/example/deepseekbalance/
├── MainActivity.kt                  # 应用入口
├── BalanceApp.kt                    # Application 类
├── data/
│   ├── model/                       # 数据模型（BalanceResponse、KeyEntry）
│   ├── network/                     # DeepSeekApi、Retrofit 工厂、Repository
│   ├── repository/                  # KeyRepository
│   ├── security/                    # SecureKeyStorage、SettingsStore
│   └── worker/                      # BalanceUpdateWorker
├── notification/                    # 通知栏余额显示
├── ui/
│   ├── screen/                      # BalanceScreen、KeyManagementScreen
│   ├── theme/                       # Color、Theme、Type（主题配色与字体）
│   └── viewmodel/                   # BalanceViewModel
└── widget/                          # 桌面小部件相关
```

---

## 安全与隐私

- API key 仅保存在 `EncryptedSharedPreferences` 中，由 Android Keystore 保护。
- 日志中对 `Authorization` 请求头做脱敏处理。
- `AndroidManifest.xml` 设置 `android:allowBackup="false"`，避免本地数据被备份导出。
- 通知使用 `VISIBILITY_SECRET`，锁屏时不显示余额。

---

## 注意事项

- HyperOS 等系统可能限制后台任务，如需小部件与通知稳定刷新，请在「应用信息 → 省电策略」中将本应用设为「无限制」。
- `BalanceViewModel` 中 `notificationEnabled` 为 Compose 状态属性，如需切换通知状态请调用 `updateNotificationState(enabled)`，不要直接调用同名 setter。
- 当前未配置单元测试与 UI 测试；新增测试请放在 `app/src/test/` 与 `app/src/androidTest/`。

---

## APK 版本命名

Debug APK 默认输出路径：`app/build/outputs/apk/debug/app-debug.apk`

每次打包完成后，建议将 APK 重命名为 `app-debug-vX.Y.apk`：

- 小版本号（`.` 后）范围为 `0-9`，每打包一次 `+1`。
- 小版本号达到 `9` 后，下一次大版本号（`.` 前）`+1`，小版本号归零。

示例序列：`v1.0` → `v1.1` → … → `v1.9` → `v2.0` → `v2.1`。

---

## 许可证

（此处可补充项目所采用的许可证，如 MIT、Apache-2.0 等）
