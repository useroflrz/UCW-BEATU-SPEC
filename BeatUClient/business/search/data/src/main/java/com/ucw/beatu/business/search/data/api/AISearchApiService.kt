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
        try {
            android.util.Log.e("AISearchApi", "🔍 开始 AI 搜索请求: query=$userQuery")
            val requestBody = AISearchRequest(userQuery = userQuery)
            val jsonBody = requestAdapter.toJson(requestBody)
            
            val url = if (networkConfig.baseUrl.endsWith("/")) {
                "${networkConfig.baseUrl}api/ai/search/stream"
            } else {
                "${networkConfig.baseUrl}/api/ai/search/stream"
            }
            
            android.util.Log.e("AISearchApi", "🔍 请求 URL: $url, baseUrl=${networkConfig.baseUrl}")
            
            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .addHeader("Accept", "text/event-stream")
                .addHeader("Cache-Control", "no-cache")
                .build()
            
            android.util.Log.e("AISearchApi", "🔍 开始执行 HTTP 请求...")
            okHttpClient.newCall(request).execute().use { response ->
                // ✅ 添加详细日志：记录响应状态（使用 ERROR 级别确保输出）
                android.util.Log.e("AISearchApi", "🔍 响应状态码: ${response.code}, 是否成功: ${response.isSuccessful}, URL: $url")
                
                if (!response.isSuccessful) {
                    // ✅ 读取错误响应体，提供更详细的错误信息
                    val errorBody = try {
                        response.body?.string() ?: "未知错误"
                    } catch (e: Exception) {
                        "无法读取错误响应体: ${e.message}"
                    }
                    android.util.Log.e("AISearchApi", "❌ 请求失败: code=${response.code}, message=${response.message}, body=$errorBody")
                    
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
                
                if (response.body != null) {
                    val body = response.body!!
                    BufferedReader(InputStreamReader(body.byteStream(), "UTF-8")).use { reader ->
                        var line: String?
                        var lineCount = 0
                        
                        android.util.Log.e("AISearchApi", "🔍 开始读取 SSE 流")
                        
                        while (true) {
                            line = reader.readLine()
                            if (line == null) break
                            lineCount++
                            line?.let { currentLine ->
                                android.util.Log.e("AISearchApi", "🔍 读取第 $lineCount 行: ${currentLine.take(200)}")
                                
                                when {
                                    currentLine.startsWith("data: ") -> {
                                        // 解析 SSE 数据
                                        val data = currentLine.substring(6) // 移除 "data: " 前缀
                                        android.util.Log.e("AISearchApi", "🔍 解析 SSE 数据: ${data.take(200)}")
                                        try {
                                            val chunk = chunkAdapter.fromJson(data)
                                            chunk?.let { 
                                                // ✅ 添加日志：记录接收到的 chunk 类型（使用 ERROR 级别确保输出）
                                                android.util.Log.e("AISearchApi", "✅ 成功解析 chunk: type=${chunk.chunkType}, content length=${chunk.content.length}, isFinal=${chunk.isFinal}, content preview=${chunk.content.take(50)}")
                                                emit(it) 
                                            } ?: run {
                                                android.util.Log.e("AISearchApi", "⚠️ chunk 为 null")
                                            }
                                        } catch (e: Exception) {
                                            // ✅ 解析失败，记录详细错误并返回错误消息
                                            android.util.Log.e("AISearchApi", "❌ 解析 SSE 数据失败: data=${data.take(200)}", e)
                                            emit(
                                                AISearchStreamChunk(
                                                    chunkType = "error",
                                                    content = "AI 搜索不可用：数据解析失败 - ${e.message}",
                                                    isFinal = true
                                                )
                                            )
                                        }
                                    }
                                    currentLine.isEmpty() -> {
                                        // 空行表示一个事件结束，继续处理下一行
                                        android.util.Log.d("AISearchApi", "空行（事件结束）")
                                    }
                                    currentLine.startsWith("event: ") -> {
                                        // SSE 事件类型（可选）
                                        android.util.Log.d("AISearchApi", "SSE 事件类型: $currentLine")
                                    }
                                    currentLine.startsWith("id: ") -> {
                                        // SSE 事件 ID（可选）
                                        android.util.Log.d("AISearchApi", "SSE 事件 ID: $currentLine")
                                    }
                                    else -> {
                                        // 其他行（可能是注释或格式错误）
                                        android.util.Log.w("AISearchApi", "⚠️ 未知的 SSE 行格式: $currentLine")
                                    }
                                }
                            }
                        }
                        android.util.Log.e("AISearchApi", "🔍 SSE 流读取完成，共读取 $lineCount 行")
                    }
                } else {
                    android.util.Log.e("AISearchApi", "❌ 响应体为空")
                    emit(
                        AISearchStreamChunk(
                            chunkType = "error",
                            content = "AI 搜索不可用：响应体为空",
                            isFinal = true
                        )
                    )
                }
            }
        } catch (e: IOException) {
            android.util.Log.e("AISearchApi", "❌ 网络请求异常: ${e.javaClass.simpleName}, message=${e.message}", e)
            emit(
                AISearchStreamChunk(
                    chunkType = "error",
                    content = "AI 搜索不可用：网络异常 - ${e.message}",
                    isFinal = true
                )
            )
        } catch (e: Exception) {
            android.util.Log.e("AISearchApi", "❌ 请求处理异常: ${e.javaClass.simpleName}, message=${e.message}", e)
            emit(
                AISearchStreamChunk(
                    chunkType = "error",
                    content = "AI 搜索不可用：处理异常 - ${e.message}",
                    isFinal = true
                )
            )
        }
    }.catch { e ->
        // ✅ 统一错误消息：AI搜索不可用（Flow 级别的 catch）
        android.util.Log.e("AISearchApi", "❌ Flow catch 异常: ${e.javaClass.simpleName}, message=${e.message}", e)
        val errorMessage = "AI 搜索不可用：${e.message}"
        emit(
            AISearchStreamChunk(
                chunkType = "error",
                content = errorMessage,
                isFinal = true
            )
        )
    }.flowOn(Dispatchers.IO)
}

