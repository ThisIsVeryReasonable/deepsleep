package com.example.deepseekbalance.data.model

import kotlinx.serialization.Serializable

/**
 * 用户保存的 API key 条目。
 * 注意：key 字段只在内存中使用，最终会通过 EncryptedSharedPreferences 加密落盘。
 */
@Serializable
data class KeyEntry(
    val id: Long,
    val name: String,
    val key: String
)
