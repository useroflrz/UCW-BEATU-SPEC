package com.ucw.beatu.shared.network.interceptor

import com.ucw.beatu.shared.common.logger.AppLogger
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer

class NetworkLoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        AppLogger.d(TAG, "➡️ ${request.method} ${request.url}")
        val response = chain.proceed(request)
        
        // ✅ 如果是用户列表接口，打印响应体（用于调试字段映射问题）
        if (request.url.toString().contains("/api/users") && !request.url.toString().contains("/follow")) {
            val responseBody = response.body
            val source = responseBody?.source()
            source?.request(Long.MAX_VALUE) // Buffer the entire body
            val buffer = source?.buffer
            val responseBodyString = buffer?.clone()?.readUtf8()
            
            // 只打印 BEATU 用户的部分（避免日志过长）
            responseBodyString?.let { body ->
                if (body.contains("BEATU")) {
                    val beatuStart = body.indexOf("\"id\":\"BEATU\"")
                    if (beatuStart >= 0) {
                        val beatuEnd = body.indexOf("}", beatuStart) + 1
                        val beatuJson = body.substring(beatuStart.coerceAtLeast(0), beatuEnd.coerceAtMost(body.length))
                        AppLogger.d(TAG, "⬅️ BEATU 用户原始 JSON: $beatuJson")
                    }
                }
            }
            
            // 重新创建响应（因为 body 已经被读取）
            val newResponseBody = responseBodyString?.toResponseBody(responseBody.contentType())
            return response.newBuilder().body(newResponseBody).build()
        }
        
        AppLogger.d(TAG, "⬅️ ${response.code} ${response.request.url}")
        return response
    }

    companion object {
        private const val TAG = "Network"
    }
}

