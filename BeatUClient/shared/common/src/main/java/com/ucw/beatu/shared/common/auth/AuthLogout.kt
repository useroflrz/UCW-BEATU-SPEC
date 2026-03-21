package com.ucw.beatu.shared.common.auth

/**
 * 注销当前会话（由 app 模块实现：清本地持久化 + [com.ucw.beatu.shared.common.user.AuthSessionHolder]）。
 */
fun interface AuthLogout {
    fun logout()
}
