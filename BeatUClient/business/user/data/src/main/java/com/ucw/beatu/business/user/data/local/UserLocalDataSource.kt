package com.ucw.beatu.business.user.data.local

import com.ucw.beatu.business.user.data.mapper.toDomain
import com.ucw.beatu.business.user.data.mapper.toEntity
import com.ucw.beatu.business.user.domain.model.User
import com.ucw.beatu.shared.database.BeatUDatabase
import com.ucw.beatu.shared.database.dao.UserDao
import com.ucw.beatu.shared.database.dao.UserFollowDao
import com.ucw.beatu.shared.database.entity.UserFollowEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 用户本地数据源接口
 */
interface UserLocalDataSource {
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
     */
    suspend fun getUserById(userId: String): User?

    /**
     * 根据用户名获取用户信息（一次性）
     */
    suspend fun getUserByName(userName: String): User?

    /**
     * 保存用户信息
     */
    suspend fun saveUser(user: User)

    /**
     * 批量保存用户信息
     */
    suspend fun saveUsers(users: List<User>)

    /**
     * 检查是否关注了某个用户
     */
    suspend fun isFollowing(currentUserId: String, targetUserId: String): Boolean

    /**
     * 观察关注状态（Flow）
     */
    fun observeIsFollowing(currentUserId: String, targetUserId: String): Flow<Boolean>

    /**
     * 添加关注关系（本地）
     */
    suspend fun followUser(currentUserId: String, targetUserId: String)

    /**
     * 删除关注关系（本地）
     */
    suspend fun unfollowUser(currentUserId: String, targetUserId: String)

    /**
     * 清除待同步标记（用于同步成功后）
     */
    suspend fun clearFollowPendingStatus(currentUserId: String, targetUserId: String)

    /**
     * 回滚关注状态（用于同步失败时）
     */
    suspend fun rollbackFollowStatus(currentUserId: String, targetUserId: String, shouldFollow: Boolean)

    /**
     * 批量保存用户关注关系（首次启动时全量加载）
     */
    suspend fun saveUserFollows(follows: List<UserFollowEntity>)
}

/**
 * 用户本地数据源实现
 * 负责从Room数据库读写用户数据
 */
class UserLocalDataSourceImpl @Inject constructor(
    private val database: BeatUDatabase
) : UserLocalDataSource {

    private val userDao: UserDao = database.userDao()
    private val userFollowDao: UserFollowDao = database.userFollowDao()

    override fun observeUserById(userId: String): Flow<User?> {
        return userDao.observeUserById(userId).map { entity ->
            entity?.toDomain()
        }
    }

    override suspend fun getUserById(userId: String): User? {
        return userDao.getUserById(userId)?.toDomain()
    }

    override fun observeUserByName(userName: String): Flow<User?> {
        return userDao.observeUserByName(userName).map { entity ->
            entity?.toDomain()
        }
    }

    override suspend fun getUserByName(userName: String): User? {
        return userDao.getUserByName(userName)?.toDomain()
    }

    override suspend fun saveUser(user: User) {
        userDao.insertOrUpdate(user.toEntity())
    }

    override suspend fun saveUsers(users: List<User>) {
        userDao.insertOrUpdateAll(users.map { it.toEntity() })
    }

    override suspend fun isFollowing(currentUserId: String, targetUserId: String): Boolean {
        return userFollowDao.isFollowing(currentUserId, targetUserId) ?: false
    }

    override fun observeIsFollowing(currentUserId: String, targetUserId: String): Flow<Boolean> {
        return userFollowDao.observeFollowStatus(currentUserId, targetUserId).map { entity ->
            entity?.isFollowed ?: false
        }
    }

    override suspend fun followUser(currentUserId: String, targetUserId: String) {
        val relation = UserFollowEntity(
            userId = currentUserId, // ✅ 修改：使用 userId 字段
            authorId = targetUserId, // ✅ 修改：使用 authorId 字段
            isFollowed = true, // ✅ 修改：设置 isFollowed = true
            isPending = true // ✅ 修改：设置 isPending = true（乐观更新）
        )
        userFollowDao.insert(relation)
    }

    override suspend fun unfollowUser(currentUserId: String, targetUserId: String) {
        // ✅ 修改：不直接删除，而是更新 isFollowed = false, isPending = true
        val existing = userFollowDao.observeFollowStatus(currentUserId, targetUserId).firstOrNull()
        if (existing != null) {
            userFollowDao.insert(existing.copy(isFollowed = false, isPending = true))
        } else {
            // 如果不存在，创建一个标记为未关注且待同步的记录
            userFollowDao.insert(UserFollowEntity(
                userId = currentUserId,
                authorId = targetUserId,
                isFollowed = false,
                isPending = true
            ))
        }
    }

    override suspend fun clearFollowPendingStatus(currentUserId: String, targetUserId: String) {
        val existing = userFollowDao.observeFollowStatus(currentUserId, targetUserId).firstOrNull()
        if (existing != null) {
            userFollowDao.insert(existing.copy(isPending = false))
        }
    }

    override suspend fun rollbackFollowStatus(currentUserId: String, targetUserId: String, shouldFollow: Boolean) {
        val existing = userFollowDao.observeFollowStatus(currentUserId, targetUserId).firstOrNull()
        if (existing != null) {
            // 回滚时清除 isPending 标记，恢复到同步完成状态
            userFollowDao.insert(existing.copy(isFollowed = shouldFollow, isPending = false))
        } else if (shouldFollow) {
            // 如果不存在且应该关注，创建一个已关注且已同步的记录
            userFollowDao.insert(UserFollowEntity(
                userId = currentUserId,
                authorId = targetUserId,
                isFollowed = true,
                isPending = false
            ))
        } else {
            // 如果不存在且不应该关注，删除记录（如果存在）
            userFollowDao.delete(currentUserId, targetUserId)
        }
    }

    override suspend fun saveUserFollows(follows: List<UserFollowEntity>) {
        userFollowDao.insertAll(follows)
    }
}

