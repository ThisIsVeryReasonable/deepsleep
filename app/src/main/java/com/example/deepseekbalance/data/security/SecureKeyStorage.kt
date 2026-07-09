package com.example.deepseekbalance.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.deepseekbalance.data.model.KeyEntry
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 使用 EncryptedSharedPreferences + Android Keystore 加密存储 API key。
 */
class SecureKeyStorage(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            PREFS_FILE_NAME,
            masterKeyAlias,
            context.applicationContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 添加一个新的 key，返回生成的唯一 id。
     */
    fun addKey(name: String, key: String): Long {
        val entry = KeyEntry(
            id = System.currentTimeMillis(),
            name = name,
            key = key
        )
        val list = getAllKeys().toMutableList().apply { add(entry) }
        saveKeys(list)
        if (getSelectedKeyId() == null && list.isNotEmpty()) {
            setSelectedKeyId(entry.id)
        }
        return entry.id
    }

    /**
     * 删除指定 key。
     */
    fun deleteKey(id: Long) {
        val list = getAllKeys().filterNot { it.id == id }
        saveKeys(list)
        if (getSelectedKeyId() == id) {
            setSelectedKeyId(list.firstOrNull()?.id)
        }
    }

    /**
     * 获取所有已保存的 key 条目。
     */
    fun getAllKeys(): List<KeyEntry> {
        val raw = prefs.getString(KEY_ENTRIES, null) ?: return emptyList()
        return try {
            json.decodeFromString(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 获取当前选中的 key 明文。
     */
    fun getSelectedKey(): String? {
        val id = getSelectedKeyId() ?: return null
        return getAllKeys().find { it.id == id }?.key
    }

    /**
     * 获取当前选中的 key id。
     */
    fun getSelectedKeyId(): Long? {
        val id = prefs.getLong(KEY_SELECTED_ID, -1L)
        return if (id == -1L) null else id
    }

    /**
     * 设置当前选中的 key id。
     */
    fun setSelectedKeyId(id: Long?) {
        if (id == null) {
            prefs.edit().remove(KEY_SELECTED_ID).apply()
        } else {
            prefs.edit().putLong(KEY_SELECTED_ID, id).apply()
        }
    }

    private fun saveKeys(list: List<KeyEntry>) {
        prefs.edit().putString(KEY_ENTRIES, json.encodeToString(list)).apply()
    }

    companion object {
        private const val PREFS_FILE_NAME = "deepseek_keys_secure"
        private const val KEY_ENTRIES = "key_entries"
        private const val KEY_SELECTED_ID = "selected_key_id"
    }
}
