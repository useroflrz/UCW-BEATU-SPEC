package com.ucw.beatu.shared.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/** 每请求附加 `X-User-Id`（由 [userIdProvider] 提供当前用户 id）。 */
class UserIdHeaderInterceptor(
    private val userIdProvider: () -> String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("X-User-Id", userIdProvider())
            .build()
        return chain.proceed(request)
    }
}
