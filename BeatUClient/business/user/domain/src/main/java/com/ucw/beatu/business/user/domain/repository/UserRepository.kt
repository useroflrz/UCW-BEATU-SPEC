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
     * 根据用户名观察用户信息（Flow）
     */
    fun observeUserByName(userName: String): Flow<User?>

    /**
     * 获取用户信息（一次性）
     * 优先从本地获取，如果不存在则从远程获取
     */
    suspend fun getUserById(userId: String): User?

    /**
     * 根据用户名获取用户信息（一次性）
     * 优先从本地获取，如果不存在则从远程获取
     */
    suspend fun getUserByName(userName: String): User?

    /**
     * 从远程获取用户信息
     */
    suspend fun fetchUserFromRemote(userId: String): AppResult<User>

    /**
     * 根据用户名从远程获取用户信息
     */
    suspend fun fetchUserFromRemoteByName(userName: String): AppResult<User>

    /**
     * 保存用户信息
     */
    suspend fun saveUser(user: User)

    /**
     * 观察关注状态（Flow）
     */
    fun observeIsFollowing(currentUserId: String, targetUserId: String): kotlinx.coroutines.flow.Flow<Boolean>

    /**
     * 观察关注同步结果（Flow）
     * 用于通知关注/取消关注的同步结果
     */
    fun observeFollowSyncResult(): kotlinx.coroutines.flow.Flow<FollowSyncResult>

    /**
     * 关注用户（先更新本地数据库，然后异步同步到远程）
     * @param currentUserId 当前用户ID
     * @param targetUserId 目标用户ID
     */
    suspend fun followUser(currentUserId: String, targetUserId: String)

    /**
     * 取消关注用户（先更新本地数据库，然后异步同步到远程）
     * @param currentUserId 当前用户ID
     * @param targetUserId 目标用户ID
     */
    suspend fun unfollowUser(currentUserId: String, targetUserId: String)

    /**
     * 关注同步结果
     */
    sealed class FollowSyncResult {
        data class Success(val isFollow: Boolean, val targetUserId: String) : FollowSyncResult()
        data class Error(val isFollow: Boolean, val targetUserId: String, val error: String) : FollowSyncResult()
    }
}

