package com.ucw.beatu.auth

import android.content.Context
import android.content.Intent
import com.ucw.beatu.MainActivity
import com.ucw.beatu.shared.common.auth.AuthLogout
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthLogoutImpl @Inject constructor(
    private val authSessionStore: AuthSessionStore,
    @ApplicationContext private val context: Context
) : AuthLogout {
    override fun logout() {
        authSessionStore.clear()
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }
}
