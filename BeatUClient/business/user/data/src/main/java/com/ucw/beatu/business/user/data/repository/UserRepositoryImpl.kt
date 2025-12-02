package com.ucw.beatu.business.user.data.repository

import com.ucw.beatu.business.user.data.local.UserLocalDataSource
import com.ucw.beatu.business.user.data.remote.UserRemoteDataSource
import com.ucw.beatu.business.user.domain.model.User
import com.ucw.beatu.business.user.domain.repository.UserRepository
import com.ucw.beatu.shared.common.result.AppResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 用户仓储实现
 * 协调本地和远程数据源，实现本地优先、远程补充的策略
 */
class UserRepositoryImpl @Inject constructor(
    private val localDataSource: UserLocalDataSource,
    private val remoteDataSource: UserRemoteDataSource
) : UserRepository {

    override fun observeUserById(userId: String): Flow<User?> {
        return localDataSource.observeUserById(userId)
    }

    override suspend fun getUserById(userId: String): User? {
        // 优先从本地获取
        val localUser = localDataSource.getUserById(userId)
        if (localUser != null) {
            return localUser
        }
        
        // 如果本地不存在，尝试从远程获取
        val remoteResult = remoteDataSource.getUserById(userId)
        return when (remoteResult) {
            is AppResult.Success -> {
                // 保存到本地
                localDataSource.saveUser(remoteResult.data)
                remoteResult.data
            }
            else -> null
        }
    }

    override suspend fun fetchUserFromRemote(userId: String): AppResult<User> {
        val result = remoteDataSource.getUserById(userId)
        // 如果成功，保存到本地
        if (result is AppResult.Success) {
            localDataSource.saveUser(result.data)
        }
        return result
    }

    override suspend fun saveUser(user: User) {
        localDataSource.saveUser(user)
    }

    override suspend fun followUser(userId: String): AppResult<Unit> {
        return remoteDataSource.followUser(userId)
    }

    override suspend fun unfollowUser(userId: String): AppResult<Unit> {
        return remoteDataSource.unfollowUser(userId)
    }
}

