package com.example.deepseekbalance.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.deepseekbalance.R
import com.example.deepseekbalance.data.model.BalanceResponse
import com.example.deepseekbalance.data.model.KeyEntry
import com.example.deepseekbalance.data.network.BalanceFetchResult
import com.example.deepseekbalance.data.network.BalanceRepository
import com.example.deepseekbalance.data.repository.KeyRepository
import com.example.deepseekbalance.data.security.SecureKeyStorage
import com.example.deepseekbalance.data.security.SettingsStore
import com.example.deepseekbalance.notification.BalanceNotificationHelper
import com.example.deepseekbalance.widget.BalanceWidgetScheduler
import com.example.deepseekbalance.widget.BalanceWidgetUpdater
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 余额页面状态与业务逻辑。
 */
class BalanceViewModel(application: Application) : AndroidViewModel(application) {

    private val keyRepository = KeyRepository(SecureKeyStorage(application))
    private val balanceRepository = BalanceRepository()
    private val settings = SettingsStore(application)

    var keys by mutableStateOf(listOf<KeyEntry>())
        private set

    var selectedKeyId by mutableStateOf<Long?>(null)
        private set

    var balanceResponse by mutableStateOf<BalanceResponse?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var lastUpdated by mutableStateOf("")
        private set

    var notificationEnabled by mutableStateOf(false)
        private set

    var refreshIntervalMinutes by mutableStateOf(30)
        private set

    init {
        loadKeys()
        notificationEnabled = settings.notificationEnabled
        refreshIntervalMinutes = settings.refreshIntervalMinutes
        refreshBalance()
    }

    /**
     * 清除错误提示（已被 UI 消费）。
     */
    fun clearError() {
        errorMessage = null
    }

    /**
     * 重新加载 key 列表和当前选中项。
     */
    fun loadKeys() {
        keys = keyRepository.getAllKeys()
        selectedKeyId = keyRepository.getSelectedKeyId()
    }

    /**
     * 添加新 key。
     */
    fun addKey(name: String, key: String) {
        if (name.isBlank() || key.isBlank()) return
        keyRepository.addKey(name.trim(), key.trim())
        loadKeys()
        refreshBalance()
    }

    /**
     * 删除 key。
     */
    fun deleteKey(id: Long) {
        keyRepository.deleteKey(id)
        loadKeys()
        if (selectedKeyId == id) {
            balanceResponse = null
            refreshBalance()
        }
    }

    /**
     * 切换当前选中的 key。
     */
    fun selectKey(id: Long) {
        keyRepository.selectKey(id)
        loadKeys()
        refreshBalance()
    }

    /**
     * 手动刷新余额。
     */
    fun refreshBalance() {
        val apiKey = keyRepository.getSelectedKey()
        if (apiKey.isNullOrBlank()) {
            balanceResponse = null
            return
        }
        isLoading = true
        errorMessage = null
        viewModelScope.launch {
            when (val result = balanceRepository.fetchBalance(apiKey)) {
                is BalanceFetchResult.Success -> {
                    balanceResponse = result.response
                    lastUpdated = formatNow()
                    updateWidgetAndNotification()
                }
                is BalanceFetchResult.Error -> {
                    errorMessage = when {
                        result.isAuthError -> getApplication<Application>().getString(R.string.error_auth)
                        else -> result.message
                    }
                }
            }
            isLoading = false
        }
    }

    /**
     * 切换通知栏显示开关。
     */
    fun updateNotificationState(enabled: Boolean) {
        notificationEnabled = enabled
        settings.notificationEnabled = enabled
        val app = getApplication<Application>()
        if (enabled) {
            BalanceNotificationHelper.show(app, balanceResponse, selectedKeyName())
        } else {
            BalanceNotificationHelper.cancel(app)
        }
    }

    /**
     * 修改自动刷新间隔（分钟）。
     */
    fun setRefreshInterval(minutes: Int) {
        refreshIntervalMinutes = minutes
        settings.refreshIntervalMinutes = minutes
        BalanceWidgetScheduler.schedulePeriodic(getApplication(), minutes)
    }

    /**
     * 获取当前选中 key 的显示名称。
     */
    fun selectedKeyName(): String {
        return keys.find { it.id == selectedKeyId }?.name ?: ""
    }

    /**
     * 余额刷新成功后同步更新小部件与通知。
     */
    private fun updateWidgetAndNotification() {
        val app = getApplication<Application>()
        BalanceWidgetUpdater.update(app, balanceResponse, selectedKeyName())
        if (notificationEnabled) {
            BalanceNotificationHelper.show(app, balanceResponse, selectedKeyName())
        }
    }

    private fun formatNow(): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }
}
