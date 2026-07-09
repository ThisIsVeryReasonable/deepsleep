package com.example.deepseekbalance.data.network

import com.example.deepseekbalance.data.model.BalanceResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

/**
 * DeepSeek 开放 API 接口定义。
 */
interface DeepSeekApi {

    /**
     * 查询账户余额。
     *
     * @param auth Authorization 头，格式：Bearer sk-xxx
     */
    @GET("user/balance")
    suspend fun getBalance(
        @Header("Authorization") auth: String,
        @Header("Accept") accept: String
    ): Response<BalanceResponse>
}
