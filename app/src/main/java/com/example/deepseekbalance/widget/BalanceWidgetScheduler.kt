package com.example.deepseekbalance.widget

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.deepseekbalance.data.worker.BalanceUpdateWorker
import java.util.concurrent.TimeUnit

/**
 * 小部件后台刷新调度器。
 */
object BalanceWidgetScheduler {

    private const val PERIODIC_WORK_NAME = "balance_periodic_update"
    private const val ONE_TIME_WORK_NAME = "balance_one_time_update"

    /**
     * 按照指定分钟间隔调度周期性刷新任务。
     */
    fun schedulePeriodic(context: Context, intervalMinutes: Int) {
        val interval = intervalMinutes.toLong().coerceAtLeast(15)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<BalanceUpdateWorker>(interval, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .addTag(BalanceUpdateWorker.WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    /**
     * 立即执行一次刷新。
     */
    fun enqueueOneTime(context: Context) {
        val request = OneTimeWorkRequestBuilder<BalanceUpdateWorker>()
            .addTag(BalanceUpdateWorker.WORK_TAG)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            ONE_TIME_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    /**
     * 取消所有余额刷新任务。
     */
    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(BalanceUpdateWorker.WORK_TAG)
    }
}
