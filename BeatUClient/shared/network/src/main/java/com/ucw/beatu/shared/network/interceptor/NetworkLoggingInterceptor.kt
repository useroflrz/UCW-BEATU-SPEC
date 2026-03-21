package com.ucw.beatu.shared.network.interceptor

import com.ucw.beatu.shared.common.logger.AppLogger
import com.ucw.beatu.shared.common.user.AuthSessionHolder
import com.ucw.beatu.shared.common.user.CurrentUserDefaults
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
            
            // 只打印当前会话用户对应条目的片段（避免日志过长）
            responseBodyString?.let { body ->
                val uid = AuthSessionHolder.userId ?: CurrentUserDefaults.DEFAULT_USER_ID
                val idMarker = "\"id\":\"$uid\""
                if (body.contains(idMarker)) {
                    val start = body.indexOf(idMarker)
                    if (start >= 0) {
                        val end = body.indexOf("}", start) + 1
                        val slice = body.substring(start.coerceAtLeast(0), end.coerceAtMost(body.length))
                        AppLogger.d(TAG, "⬅️ 当前用户条目 JSON 片段: $slice")
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

