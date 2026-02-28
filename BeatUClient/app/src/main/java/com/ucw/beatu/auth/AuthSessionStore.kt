package com.ucw.beatu.auth

import android.content.Context
import com.ucw.beatu.shared.common.user.AuthSessionHolder
import com.ucw.beatu.shared.common.user.CurrentUserDefaults
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthSessionStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs by lazy {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun loadToMemory() {
        AuthSessionHolder.userId = prefs.getString(KEY_USER_ID, null)
        AuthSessionHolder.userName = prefs.getString(KEY_USER_NAME, null)
        AuthSessionHolder.accessToken = prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    fun save(userId: String, userName: String, accessToken: String) {
        prefs.edit()
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USER_NAME, userName)
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .apply()
        AuthSessionHolder.userId = userId
        AuthSessionHolder.userName = userName
        AuthSessionHolder.accessToken = accessToken
    }

    fun clear() {
        prefs.edit().clear().apply()
        AuthSessionHolder.userId = null
        AuthSessionHolder.userName = null
        AuthSessionHolder.accessToken = null
    }

    fun userIdOrDefault(): String = AuthSessionHolder.userId ?: CurrentUserDefaults.DEFAULT_USER_ID
    fun userNameOrDefault(): String = AuthSessionHolder.userName ?: CurrentUserDefaults.DEFAULT_USER_NAME
    fun tokenOrNull(): String? = AuthSessionHolder.accessToken

    companion object {
        private const val PREF_NAME = "auth_session_store"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_ACCESS_TOKEN = "access_token"
    }
}

