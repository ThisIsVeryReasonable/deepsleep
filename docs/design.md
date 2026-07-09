# DeepSeek 余额查看工具 — 设计文档

## 1. 技术选型

| 层级 | 技术 | 说明 |
|------|------|------|
| 开发语言 | Kotlin | Android 官方首选，表达力强 |
| UI 框架 | Jetpack Compose + Material3 | 声明式 UI，现代且易于维护 |
| 网络库 | Retrofit + OkHttp + Kotlinx Serialization | 轻量、类型安全 |
| 后台任务 | WorkManager | 系统级省电调度，支持开机恢复 |
| 安全存储 | EncryptedSharedPreferences + Android Keystore | API key 加密落盘 |
| 构建工具 | Gradle 8.6 + Android Gradle Plugin 8.3 | 官方推荐版本 |

未选择 Flutter / React Native / Web 套壳的原因是：项目只需 Android、重度依赖桌面小部件与通知栏原生能力，原生方案体积最小、稳定性最高。

## 2. 架构概览

采用轻量级 MVVM 结构：

```
UI 层 (Compose Screen + ViewModel)
    ↓
Repository 层 (BalanceRepository / KeyRepository)
    ↓
数据源层 (DeepSeekApi / SecureKeyStorage / SettingsStore)
    ↓
系统能力 (AppWidgetManager / NotificationManager / WorkManager)
```

## 3. 模块划分

| 模块 | 关键文件 | 职责 |
|------|----------|------|
| 应用入口 | `BalanceApp.kt` | Application 初始化：通知渠道、恢复 WorkManager |
| 主界面 | `MainActivity.kt`, `BalanceScreen.kt`, `KeyManagementScreen.kt` | 余额展示、key 管理、设置 |
| 状态管理 | `BalanceViewModel.kt` | 承载 UI 状态，协调刷新/切换/设置 |
| 网络 | `DeepSeekApi.kt`, `BalanceRepository.kt`, `DeepSeekApiClient.kt` | 调用 DeepSeek `/user/balance` |
| 数据模型 | `BalanceResponse.kt`, `KeyEntry.kt` | 接口与本地数据结构 |
| 安全存储 | `SecureKeyStorage.kt`, `SettingsStore.kt` | key 加密、普通设置持久化 |
| 小部件 | `BalanceWidgetProvider.kt`, `BalanceWidgetUpdater.kt`, `BalanceWidgetScheduler.kt` | 桌面小部件 UI 与刷新调度 |
| 通知 | `BalanceNotificationHelper.kt` | 通知栏余额展示与取消 |
| 后台刷新 | `BalanceUpdateWorker.kt` | WorkManager 周期性刷新任务 |
| 开机恢复 | `BootCompletedReceiver.kt` | 开机后恢复 WorkManager 任务 |

## 4. 数据流

1. 用户在 App 内添加 API key，`SecureKeyStorage` 将其加密写入本地。
2. `BalanceViewModel` 通过 `BalanceRepository` 发起 `GET /user/balance` 请求。
3. 请求携带 `Authorization: Bearer <key>` 头，返回 `balance_infos` 数组。
4. 解析后更新主界面，并同步调用：
   - `BalanceWidgetUpdater.update(...)` 刷新桌面小部件；
   - `BalanceNotificationHelper.show(...)` 刷新通知栏（若开启）。
5. `WorkManager` 按设定间隔触发 `BalanceUpdateWorker`，在后台重复步骤 2–4。

## 5. DeepSeek 余额接口

```http
GET https://api.deepseek.com/user/balance
Accept: application/json
Authorization: Bearer <DEEPSEEK_API_KEY>
```

响应示例：

```json
{
  "is_available": true,
  "balance_infos": [
    {
      "currency": "CNY",
      "total_balance": "110.00",
      "granted_balance": "10.00",
      "topped_up_balance": "100.00"
    }
  ]
}
```

文档来源：[DeepSeek API Docs - Get User Balance](https://api-docs.deepseek.com/api/get-user-balance/)

## 6. 安全设计

- API key 通过 `EncryptedSharedPreferences` 保存，由 `MasterKey`（Android Keystore）保护。
- 禁止将 key 打印到日志或崩溃报告；OkHttp 日志对 `Authorization` 头做脱敏处理。
- 网络请求强制 HTTPS，不忽略证书校验。
- 设置 `android:allowBackup="false"`，并在 `backup_rules.xml` / `data_extraction_rules.xml` 中排除本地数据。
- 通知使用 `VISIBILITY_SECRET`，锁屏时不显示余额内容。

## 7. 权限清单

| 权限 | 用途 |
|------|------|
| `INTERNET` | 请求 DeepSeek API |
| `POST_NOTIFICATIONS` | Android 13+ 发送通知 |
| `RECEIVE_BOOT_COMPLETED` | 开机后恢复 WorkManager 刷新任务 |

> 说明：未使用前台服务，通知通过 `NotificationManager` 直接发布，因此无需 `FOREGROUND_SERVICE` 权限。

## 8. 界面设计

### 8.1 主界面（BalanceScreen）

- 顶部：当前选中 key 下拉选择器、右上角 key 管理入口。
- 中部：余额卡片，按币种分组展示 `total_balance`、`granted_balance`、`topped_up_balance`。
- 下部：刷新按钮、上次更新时间、设置卡片。
- 设置卡片：通知栏开关、自动刷新间隔（15/30/60 分钟）。

### 8.2 Key 管理界面（KeyManagementScreen）

- 列表展示所有已保存 key，点击切换当前选中 key。
- 右上角添加按钮，弹出对话框输入 key 名称与值。
- 每条目右侧删除按钮。

## 9. 桌面小部件设计

- 尺寸：默认 2×1，支持横向/纵向拉伸。
- 内容：key 名称、余额（优先显示 CNY，双币种时显示 CNY/USD）、上次更新时间。
- 交互：
  - 点击余额区域打开 App；
  - 点击刷新按钮立即触发一次 WorkManager 单次任务。
- 更新机制：`AppWidgetProvider.onUpdate` + `WorkManager` 周期性任务。

## 10. 通知栏设计

- 默认关闭，用户手动开启。
- 以低优先级、常驻通知形式展示当前余额。
- 点击通知进入 App。
- Android 13+ 开启时自动申请 `POST_NOTIFICATIONS` 权限。

## 11. 错误处理

| 场景 | 处理方式 |
|------|----------|
| 网络异常 | Snackbar 提示“网络异常，请检查连接” |
| 鉴权失败（HTTP 401） | 提示“API key 无效，请检查或切换” |
| 解析异常 | 显示“数据异常”，记录日志 |
| 后台刷新失败 | 小部件显示占位/失败状态，不弹窗打扰 |

## 12. 构建与运行

1. 使用 Android Studio Hedgehog 或更高版本打开项目。
2. 确保 JDK 为 17（AGP 8.3 要求）。
3. 首次打开会自动下载 Gradle 8.6 与依赖。
4. 连接红米 K70 并开启 USB 调试，点击 Run 或执行：

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 13. 验证清单

1. 添加有效 key 后，主界面余额正确。
2. 多个 key 切换后余额对应变化。
3. 桌面小部件显示与 App 一致，点击刷新可更新。
4. 通知栏开启后显示余额，关闭后消失。
5. 断网时给出错误提示，恢复网络后可刷新。
6. 重启手机后后台刷新任务与小部件保持工作。

## 14. 本期不做（Out of Scope）

- 多币种换算。
- 余额变动历史。
- 自动充值 / 支付。
- iOS 版本。
- 云端同步 key。
