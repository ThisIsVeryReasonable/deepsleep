package com.example.deepseekbalance.data.security

import android.content.Context
import android.content.SharedPreferences

/**
 * 普通设置存储（非敏感信息）。
 */
class SettingsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var notificationEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATION_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATION_ENABLED, value).apply()

    var refreshIntervalMinutes: Int
        get() = prefs.getInt(KEY_REFRESH_INTERVAL, 30)
        set(value) = prefs.edit().putInt(KEY_REFRESH_INTERVAL, value).apply()

    companion object {
        private const val PREFS_NAME = "deepseek_settings"
        private const val KEY_NOTIFICATION_ENABLED = "notification_enabled"
        private const val KEY_REFRESH_INTERVAL = "refresh_interval_minutes"
    }
}
