package com.example.deepseekbalance.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.deepseekbalance.data.network.BalanceFetchResult
import com.example.deepseekbalance.data.network.BalanceRepository
import com.example.deepseekbalance.data.repository.KeyRepository
import com.example.deepseekbalance.data.security.SecureKeyStorage
import com.example.deepseekbalance.data.security.SettingsStore
import com.example.deepseekbalance.notification.BalanceNotificationHelper
import com.example.deepseekbalance.widget.BalanceWidgetUpdater

/**
 * 后台刷新任务：定时获取余额并同步更新小部件与通知栏。
 */
class BalanceUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val keyRepo = KeyRepository(SecureKeyStorage(applicationContext))
        val apiKey = keyRepo.getSelectedKey()
        val keyName = keyRepo.getAllKeys().find { it.id == keyRepo.getSelectedKeyId() }?.name ?: ""

        if (apiKey.isNullOrBlank()) {
            // 没有可用 key，只更新小部件为无数据状态
            BalanceWidgetUpdater.update(applicationContext, null, keyName)
            return Result.failure()
        }

        return when (val result = BalanceRepository().fetchBalance(apiKey)) {
            is BalanceFetchResult.Success -> {
                val response = result.response
                BalanceWidgetUpdater.update(applicationContext, response, keyName)
                val settings = SettingsStore(applicationContext)
                if (settings.notificationEnabled) {
                    BalanceNotificationHelper.show(applicationContext, response, keyName)
                }
                Result.success()
            }
            is BalanceFetchResult.Error -> {
                // 失败时也更新小部件，避免用户看到旧数据还以为没刷新
                BalanceWidgetUpdater.update(applicationContext, null, keyName)
                Result.retry()
            }
        }
    }

    companion object {
        const val WORK_TAG = "deepseek_balance_update"
    }
}
