package com.ucw.beatu.business.user.domain.repository

import com.ucw.beatu.business.user.domain.model.User
import com.ucw.beatu.shared.common.result.AppResult
import kotlinx.coroutines.flow.Flow

/**
 * 用户仓储接口
 * 定义业务层需要的数据访问接口
 */
interface UserRepository {
    /**
     * 观察用户信息（Flow）
     */
    fun observeUserById(userId: String): Flow<User?>

    /**
     * 获取用户信息（一次性）
     * 优先从本地获取，如果不存在则从远程获取
     */
    suspend fun getUserById(userId: String): User?

    /**
     * 从远程获取用户信息
     */
    suspend fun fetchUserFromRemote(userId: String): AppResult<User>

    /**
     * 保存用户信息
     */
    suspend fun saveUser(user: User)

    /**
     * 关注用户
     */
    suspend fun followUser(userId: String): AppResult<Unit>

    /**
     * 取消关注用户
     */
    suspend fun unfollowUser(userId: String): AppResult<Unit>
}

