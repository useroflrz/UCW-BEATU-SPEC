package com.ucw.beatu.shared.common.user

/**
 * 当前用户信息提供者。
 * 后续接入登录态时，只需替换 DI 绑定到新的 Provider 实现。
 */
interface CurrentUserProvider {
    fun userId(): String
    fun userName(): String
    fun accessToken(): String? = null
}

class DefaultCurrentUserProvider : CurrentUserProvider {
    override fun userId(): String = AuthSessionHolder.userId ?: CurrentUserDefaults.DEFAULT_USER_ID

    override fun userName(): String = AuthSessionHolder.userName ?: CurrentUserDefaults.DEFAULT_USER_NAME

    override fun accessToken(): String? = AuthSessionHolder.accessToken
}
