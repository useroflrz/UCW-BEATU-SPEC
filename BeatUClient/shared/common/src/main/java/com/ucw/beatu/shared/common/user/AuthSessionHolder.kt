package com.ucw.beatu.shared.common.user

/**
 * 运行时登录会话容器。
 * 由登录流程写入，网络拦截器按请求读取。
 */
object AuthSessionHolder {
    @Volatile
    var userId: String? = null

    @Volatile
    var userName: String? = null

    @Volatile
    var accessToken: String? = null
}

