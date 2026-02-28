package com.ucw.beatu.shared.network.interceptor

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.ucw.beatu.shared.common.user.AuthSessionHolder
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 统一处理 401：
 * - 清理本地会话
 * - 自动跳转登录页
 */
class UnauthorizedRedirectInterceptor(
    private val appContext: Context
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        if (response.code != 401) return response

        val path = request.url.encodedPath
        // 登录/注册接口本身失败，不触发跳转，避免循环打断输入流程
        if (path.contains("/api/auth/login") || path.contains("/api/auth/register")) {
            return response
        }

        val now = SystemClock.elapsedRealtime()
        if (now - lastRedirectAt < REDIRECT_COOLDOWN_MS) {
            return response
        }
        lastRedirectAt = now

        AuthSessionHolder.accessToken = null
        AuthSessionHolder.userId = null
        AuthSessionHolder.userName = null
        appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_NAME)
            .apply()

        val intent = Intent().apply {
            setClassName(appContext.packageName, "com.ucw.beatu.auth.AuthActivity")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        runCatching { appContext.startActivity(intent) }
        return response
    }

    companion object {
        @Volatile
        private var lastRedirectAt: Long = 0L
        private const val REDIRECT_COOLDOWN_MS = 2000L
        private const val PREF_NAME = "auth_session_store"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_ACCESS_TOKEN = "access_token"
    }
}

