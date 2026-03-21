package com.ucw.beatu.auth

import android.content.Context
import com.ucw.beatu.shared.common.user.AuthSessionHolder
import com.ucw.beatu.shared.common.user.CurrentUserDefaults
import com.ucw.beatu.shared.database.datastore.PreferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthSessionStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesDataStore: PreferencesDataStore
) {
    private val prefs by lazy {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Application.onCreate 中调用：恢复 SharedPreferences + DataStore 中的会话到内存。
     */
    fun loadToMemory() {
        runBlocking(Dispatchers.IO) {
            var uid = prefs.getString(KEY_USER_ID, null)
            if (uid.isNullOrBlank()) {
                uid = preferencesDataStore.getStringSync("current_user_id").takeIf { it.isNotBlank() }
            }
            var uname = prefs.getString(KEY_USER_NAME, null)
            if (uname.isNullOrBlank()) {
                uname = preferencesDataStore.getStringSync("current_user_name").takeIf { it.isNotBlank() }
            }
            val token = prefs.getString(KEY_ACCESS_TOKEN, null)
            if (!uid.isNullOrBlank()) {
                AuthSessionHolder.userId = uid
                AuthSessionHolder.userName = uname
                AuthSessionHolder.accessToken = token
            }
        }
    }

    fun save(userId: String, userName: String, accessToken: String) {
        prefs.edit()
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USER_NAME, userName)
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .apply()
        runBlocking(Dispatchers.IO) {
            preferencesDataStore.putString("current_user_id", userId)
            preferencesDataStore.putString("current_user_name", userName)
        }
        AuthSessionHolder.userId = userId
        AuthSessionHolder.userName = userName
        AuthSessionHolder.accessToken = accessToken
    }

    fun clear() {
        prefs.edit().clear().apply()
        runBlocking(Dispatchers.IO) {
            preferencesDataStore.removeString("current_user_id")
            preferencesDataStore.removeString("current_user_name")
        }
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
