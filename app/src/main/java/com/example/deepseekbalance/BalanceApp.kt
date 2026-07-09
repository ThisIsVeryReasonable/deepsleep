package com.example.deepseekbalance

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.deepseekbalance.data.security.SettingsStore
import com.example.deepseekbalance.notification.BalanceNotificationHelper
import com.example.deepseekbalance.widget.BalanceWidgetScheduler
import com.example.deepseekbalance.widget.BalanceWidgetUpdater

/**
 * Application 入口，负责初始化通知渠道、恢复后台任务等全局配置。
 */
class BalanceApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // 恢复开机前设置的刷新任务
        val settings = SettingsStore(this)
        BalanceWidgetScheduler.schedulePeriodic(this, settings.refreshIntervalMinutes)
        // 小部件先展示占位
        BalanceWidgetUpdater.update(this, null, "")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                BalanceNotificationHelper.CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_description)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
