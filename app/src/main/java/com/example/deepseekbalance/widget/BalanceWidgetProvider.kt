package com.example.deepseekbalance.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.deepseekbalance.MainActivity
import com.example.deepseekbalance.R
import com.example.deepseekbalance.data.model.BalanceResponse
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 桌面小部件提供者，处理系统更新与刷新点击事件。
 */
class BalanceWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // 系统触发时展示占位，真正的余额数据由 WorkManager 或用户刷新后更新
        BalanceWidgetUpdater.update(context, null, "")
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_REFRESH_WIDGET) {
            BalanceWidgetUpdater.showUpdating(context)
            BalanceWidgetScheduler.enqueueOneTime(context)
            return
        }
        super.onReceive(context, intent)
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.example.deepseekbalance.ACTION_REFRESH_WIDGET"
    }
}

/**
 * 小部件视图与调度工具。
 */
object BalanceWidgetUpdater {

    /**
     * 刷新所有小部件的显示内容。
     */
    fun update(context: Context, response: BalanceResponse?, keyName: String) {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, BalanceWidgetProvider::class.java)
        val ids = manager.getAppWidgetIds(component)
        if (ids.isEmpty()) return

        val views = buildRemoteViews(context, response, keyName)
        manager.updateAppWidget(ids, views)
    }

    /**
     * 显示“更新中”状态，通常在用户点击刷新按钮后调用。
     */
    fun showUpdating(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, BalanceWidgetProvider::class.java)
        val ids = manager.getAppWidgetIds(component)
        if (ids.isEmpty()) return

        val views = RemoteViews(context.packageName, R.layout.balance_widget).apply {
            setTextViewText(R.id.widget_key_name, context.getString(R.string.app_name))
            setTextViewText(R.id.widget_balance, "--")
            setTextViewText(R.id.widget_time, "更新中...")
            setupClickIntents(context)
        }
        manager.updateAppWidget(ids, views)
    }

    private fun buildRemoteViews(
        context: Context,
        response: BalanceResponse?,
        keyName: String
    ): RemoteViews {
        return RemoteViews(context.packageName, R.layout.balance_widget).apply {
            setTextViewText(
                R.id.widget_key_name,
                if (keyName.isNotBlank()) keyName else context.getString(R.string.app_name)
            )
            setTextViewText(R.id.widget_balance, formatBalance(response))
            setTextViewText(R.id.widget_time, formatTime(context))
            setupClickIntents(context)
        }
    }

    private fun RemoteViews.setupClickIntents(context: Context) {
        // 点击余额区域打开 App
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPending = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setOnClickPendingIntent(R.id.widget_balance, openAppPending)
        setOnClickPendingIntent(R.id.widget_key_name, openAppPending)
        setOnClickPendingIntent(R.id.widget_time, openAppPending)

        // 点击刷新按钮触发刷新
        val refreshIntent = Intent(context, BalanceWidgetProvider::class.java).apply {
            action = BalanceWidgetProvider.ACTION_REFRESH_WIDGET
        }
        val refreshPending = PendingIntent.getBroadcast(
            context,
            1,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setOnClickPendingIntent(R.id.widget_refresh, refreshPending)
    }

    private fun formatBalance(response: BalanceResponse?): String {
        if (response == null) return "--"
        val cny = response.balanceInfos.find { it.currency == "CNY" }
        val usd = response.balanceInfos.find { it.currency == "USD" }
        return when {
            cny != null && usd != null -> "${cny.totalBalance} / ${usd.totalBalance}"
            cny != null -> "${cny.totalBalance} CNY"
            usd != null -> "${usd.totalBalance} USD"
            else -> "--"
        }
    }

    private fun formatTime(context: Context): String {
        return context.getString(
            R.string.last_updated,
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        )
    }
}
