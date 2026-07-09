package com.example.deepseekbalance.data.repository

import com.example.deepseekbalance.data.model.KeyEntry
import com.example.deepseekbalance.data.security.SecureKeyStorage

/**
 * API key 仓库，向上层屏蔽加密存储细节。
 */
class KeyRepository(private val storage: SecureKeyStorage) {

    fun getAllKeys(): List<KeyEntry> = storage.getAllKeys()

    fun addKey(name: String, key: String): Long = storage.addKey(name, key)

    fun deleteKey(id: Long) = storage.deleteKey(id)

    fun getSelectedKeyId(): Long? = storage.getSelectedKeyId()

    fun selectKey(id: Long?) = storage.setSelectedKeyId(id)

    fun getSelectedKey(): String? = storage.getSelectedKey()
}
