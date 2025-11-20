package com.ucw.beatu.core.network.interceptor

import com.ucw.beatu.core.common.logger.AppLogger
import okhttp3.Interceptor
import okhttp3.Response

class NetworkLoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        AppLogger.d(TAG, "➡️ ${'$'}{request.method} ${'$'}{request.url}")
        val response = chain.proceed(request)
        AppLogger.d(TAG, "⬅️ ${'$'}{response.code} ${'$'}{response.request.url}")
        return response
    }

    companion object {
        private const val TAG = "Network"
    }
}
