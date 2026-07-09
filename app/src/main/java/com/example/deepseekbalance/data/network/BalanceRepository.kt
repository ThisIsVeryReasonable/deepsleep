package com.example.deepseekbalance.data.network

import android.util.Log
import com.example.deepseekbalance.BuildConfig
import com.example.deepseekbalance.data.model.BalanceResponse
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * DeepSeek API 客户端工厂。
 */
object DeepSeekApiClient {

    private const val BASE_URL = "https://api.deepseek.com/"
    private const val TAG = "DeepSeekApi"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    val api: DeepSeekApi by lazy {
        val logging = HttpLoggingInterceptor { message ->
            // 避免在日志中泄露 API key：如果命中 Bearer 则脱敏
            val safe = if (message.contains("Bearer ")) {
                message.replace(Regex("Bearer sk-[a-zA-Z0-9]+"), "Bearer sk-***")
            } else {
                message
            }
            Log.d(TAG, safe)
        }.apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(DeepSeekApi::class.java)
    }
}

sealed class BalanceFetchResult {
    data class Success(val response: BalanceResponse) : BalanceFetchResult()
    data class Error(val message: String, val isAuthError: Boolean = false) : BalanceFetchResult()
}

/**
 * 余额仓库：负责调用 DeepSeek API 并统一处理错误。
 */
class BalanceRepository(private val api: DeepSeekApi = DeepSeekApiClient.api) {

    suspend fun fetchBalance(apiKey: String): BalanceFetchResult {
        return try {
            val response = api.getBalance(auth = "Bearer $apiKey", accept = "application/json")
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    BalanceFetchResult.Success(body)
                } else {
                    BalanceFetchResult.Error("响应体为空")
                }
            } else {
                val isAuth = response.code() == 401
                val msg = response.errorBody()?.string()?.take(200) ?: "HTTP ${response.code()}"
                BalanceFetchResult.Error(msg, isAuthError = isAuth)
            }
        } catch (e: Exception) {
            BalanceFetchResult.Error(e.message ?: "网络异常")
        }
    }
}
