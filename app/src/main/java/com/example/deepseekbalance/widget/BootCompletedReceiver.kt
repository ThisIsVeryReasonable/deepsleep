package com.example.deepseekbalance.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.deepseekbalance.data.security.SettingsStore

/**
 * 开机完成后恢复后台刷新任务。
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val settings = SettingsStore(context)
        BalanceWidgetScheduler.schedulePeriodic(context, settings.refreshIntervalMinutes)
        BalanceWidgetUpdater.update(context, null, "")
    }
}
