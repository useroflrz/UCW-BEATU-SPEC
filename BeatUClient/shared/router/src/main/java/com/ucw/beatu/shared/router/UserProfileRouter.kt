package com.ucw.beatu.shared.router

import androidx.fragment.app.Fragment

/**
 * 用户信息展示路由接口
 * 用于解耦 videofeed 和 user 模块之间的循环依赖
 * 
 * videofeed 模块依赖此接口，user 模块实现此接口
 */
interface UserProfileRouter {
    /**
     * 创建用户信息展示 Fragment（只读模式）
     * @param userId 用户ID（保留用于兼容，但优先使用authorName）
     * @param authorName 作者名称（作为用户名使用）
     * @param readOnly 是否只读模式
     * @return 用户信息展示 Fragment
     */
    fun createUserProfileFragment(userId: String, authorName: String, readOnly: Boolean = true): Fragment
}

