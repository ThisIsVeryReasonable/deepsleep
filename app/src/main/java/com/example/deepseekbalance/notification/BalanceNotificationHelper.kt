package com.example.deepseekbalance.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.deepseekbalance.MainActivity
import com.example.deepseekbalance.R
import com.example.deepseekbalance.data.model.BalanceResponse

/**
 * 余额通知管理：负责构建/更新/取消通知栏余额显示。
 */
object BalanceNotificationHelper {

    const val CHANNEL_ID = "deepseek_balance_monitor"
    private const val NOTIFICATION_ID = 1001

    /**
     * 根据传入的余额数据刷新通知。如果未开启通知权限（Android 13+）会静默失败。
     */
    fun show(context: Context, response: BalanceResponse?, keyName: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val content = formatContent(context, response)
        val notification = buildNotification(context, content, keyName)
        manager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * 取消通知。
     */
    fun cancel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        manager.cancel(NOTIFICATION_ID)
    }

    private fun buildNotification(context: Context, content: String, keyName: String): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("${context.getString(R.string.notification_title)} · $keyName")
            .setContentText(content)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET) // 锁屏隐藏余额
            .build()
    }

    private fun formatContent(context: Context, response: BalanceResponse?): String {
        if (response == null) return context.getString(R.string.not_updated)
        val cny = response.balanceInfos.find { it.currency == "CNY" }
        val usd = response.balanceInfos.find { it.currency == "USD" }
        val parts = mutableListOf<String>()
        cny?.let { parts.add("CNY ${it.totalBalance}") }
        usd?.let { parts.add("USD ${it.totalBalance}") }
        return if (parts.isEmpty()) {
            context.getString(R.string.not_updated)
        } else {
            parts.joinToString("  |  ")
        }
    }
}
