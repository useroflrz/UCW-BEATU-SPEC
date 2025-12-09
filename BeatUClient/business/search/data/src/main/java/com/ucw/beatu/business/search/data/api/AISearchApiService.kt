package com.ucw.beatu.business.search.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.ucw.beatu.business.search.data.api.dto.AISearchRequest
import com.ucw.beatu.business.search.data.api.dto.AISearchStreamChunk
import com.ucw.beatu.shared.network.config.NetworkConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.UnknownHostException
import javax.inject.Inject

/**
 * AI 搜索 API 服务
 * 使用 OkHttp 处理 SSE 流式传输
 */
class AISearchApiService @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val networkConfig: NetworkConfig
) {
    
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    
    private val requestAdapter = moshi.adapter(AISearchRequest::class.java)
    private val chunkAdapter = moshi.adapter(AISearchStreamChunk::class.java)
    
    /**
     * 执行 AI 搜索（流式）
     * 
     * @param userQuery 用户查询文本
     * @return Flow<AISearchStreamChunk> 流式数据块
     */
    fun searchStream(userQuery: String): Flow<AISearchStreamChunk> = flow {
        val requestBody = AISearchRequest(userQuery = userQuery)
        val jsonBody = requestAdapter.toJson(requestBody)
        
        val url = if (networkConfig.baseUrl.endsWith("/")) {
            "${networkConfig.baseUrl}api/ai/search/stream"
        } else {
            "${networkConfig.baseUrl}/api/ai/search/stream"
        }
        
        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .addHeader("Accept", "text/event-stream")
            .addHeader("Cache-Control", "no-cache")
            .build()
        
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                // ✅ 统一错误消息：AI搜索不可用
                val errorMessage = "AI 搜索不可用"
                emit(
                    AISearchStreamChunk(
                        chunkType = "error",
                        content = errorMessage,
                        isFinal = true
                    )
                )
                return@flow
            }
            
            response.body?.let { body ->
                BufferedReader(InputStreamReader(body.byteStream(), "UTF-8")).use { reader ->
                    var line: String?
                    
                    while (reader.readLine().also { line = it } != null) {
                        line?.let { currentLine ->
                            when {
                                currentLine.startsWith("data: ") -> {
                                    // 解析 SSE 数据
                                    val data = currentLine.substring(6) // 移除 "data: " 前缀
                                    try {
                                        val chunk = chunkAdapter.fromJson(data)
                                        chunk?.let { emit(it) }
                                    } catch (e: Exception) {
                                        // ✅ 解析失败，统一错误消息：AI搜索不可用
                                        emit(
                                            AISearchStreamChunk(
                                                chunkType = "error",
                                                content = "AI 搜索不可用",
                                                isFinal = true
                                            )
                                        )
                                    }
                                }
                                currentLine.isEmpty() -> {
                                    // 空行表示一个事件结束，继续处理下一行
                                }
                            }
                        }
                    }
                }
            }
        }
    }.catch { e ->
        // ✅ 统一错误消息：AI搜索不可用
        val errorMessage = "AI 搜索不可用"
        emit(
            AISearchStreamChunk(
                chunkType = "error",
                content = errorMessage,
                isFinal = true
            )
        )
    }.flowOn(Dispatchers.IO)
}

