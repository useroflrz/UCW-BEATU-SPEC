package com.ucw.beatu.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response

class HeaderInterceptor(private val headers: Map<String, String>) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
        headers.forEach { (key, value) ->
            requestBuilder.header(key, value)
        }
        return chain.proceed(requestBuilder.build())
    }
}
