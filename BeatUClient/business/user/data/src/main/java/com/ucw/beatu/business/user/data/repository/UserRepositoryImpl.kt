package com.ucw.beatu.business.user.data.repository

import com.ucw.beatu.business.user.data.local.UserLocalDataSource
import com.ucw.beatu.business.user.data.remote.UserRemoteDataSource
import com.ucw.beatu.business.user.domain.model.User
import com.ucw.beatu.business.user.domain.repository.UserRepository
import com.ucw.beatu.shared.common.exception.DataException
import com.ucw.beatu.shared.common.result.AppResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject

/**
 * 用户仓储实现
 * 协调本地和远程数据源，实现本地优先、远程补充的策略
 */
class UserRepositoryImpl @Inject constructor(
    private val localDataSource: UserLocalDataSource,
    private val remoteDataSource: UserRemoteDataSource
) : UserRepository {

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val TAG = "UserRepositoryImpl"
    
    private val _followSyncResult = MutableSharedFlow<UserRepository.FollowSyncResult>(replay = 0)
    override fun observeFollowSyncResult(): Flow<UserRepository.FollowSyncResult> = _followSyncResult.asSharedFlow()

    override fun observeUserById(userId: String): Flow<User?> {
        return localDataSource.observeUserById(userId)
    }

    override fun observeUserByName(userName: String): Flow<User?> {
        return localDataSource.observeUserByName(userName)
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

    override suspend fun getUserByName(userName: String): User? {
        // 优先从本地获取
        val localUser = localDataSource.getUserByName(userName)
        if (localUser != null) {
            return localUser
        }
        
        // 如果本地不存在，尝试从远程获取
        val remoteResult = remoteDataSource.getUserByName(userName)
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

    override suspend fun fetchUserFromRemoteByName(userName: String): AppResult<User> {
        val result = remoteDataSource.getUserByName(userName)
        // 如果成功，保存到本地
        if (result is AppResult.Success) {
            localDataSource.saveUser(result.data)
        }
        return result
    }

    override suspend fun saveUser(user: User) {
        localDataSource.saveUser(user)
    }

    override fun observeIsFollowing(currentUserId: String, targetUserId: String): Flow<Boolean> {
        return localDataSource.observeIsFollowing(currentUserId, targetUserId)
    }

    override suspend fun followUser(currentUserId: String, targetUserId: String) {
        // 1. 先更新本地数据库
        localDataSource.followUser(currentUserId, targetUserId)
        Log.d(TAG, "Follow user: currentUserId=$currentUserId, targetUserId=$targetUserId (local updated)")

        // 2. 异步同步到远程服务器
        syncScope.launch {
            try {
                val result = remoteDataSource.followUser(targetUserId)
                when (result) {
                    is AppResult.Success -> {
                        Log.d(TAG, "Follow user sync success: targetUserId=$targetUserId")
                        _followSyncResult.emit(UserRepository.FollowSyncResult.Success(isFollow = true, targetUserId))
                    }
                    is AppResult.Error -> {
                        val errorMessage = when (result.throwable) {
                            is DataException.NetworkException -> "网络异常，该服务不可用"
                            else -> result.message ?: result.throwable.message ?: "网络异常，该服务不可用"
                        }
                        Log.e(TAG, "Follow user sync failed: targetUserId=$targetUserId, error: $errorMessage")
                        // 同步失败，回滚本地状态
                        try {
                            localDataSource.unfollowUser(currentUserId, targetUserId)
                            Log.d(TAG, "Follow user local state rolled back: targetUserId=$targetUserId")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to rollback follow user local state: targetUserId=$targetUserId", e)
                        }
                        // 发送错误通知
                        _followSyncResult.emit(UserRepository.FollowSyncResult.Error(isFollow = true, targetUserId, errorMessage))
                    }
                    else -> {
                        Log.w(TAG, "Follow user sync unknown result: targetUserId=$targetUserId")
                        // 未知结果，也回滚本地状态
                        try {
                            localDataSource.unfollowUser(currentUserId, targetUserId)
                            Log.d(TAG, "Follow user local state rolled back (unknown result): targetUserId=$targetUserId")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to rollback follow user local state: targetUserId=$targetUserId", e)
                        }
                        // 发送错误通知
                        _followSyncResult.emit(UserRepository.FollowSyncResult.Error(isFollow = true, targetUserId, "网络异常，该服务不可用"))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Follow user sync exception: targetUserId=$targetUserId", e)
                // 异常情况，回滚本地状态
                try {
                    localDataSource.unfollowUser(currentUserId, targetUserId)
                    Log.d(TAG, "Follow user local state rolled back (exception): targetUserId=$targetUserId")
                } catch (rollbackException: Exception) {
                    Log.e(TAG, "Failed to rollback follow user local state: targetUserId=$targetUserId", rollbackException)
                }
                // 发送错误通知
                val errorMessage = if (e is DataException.NetworkException) {
                    "网络异常，该服务不可用"
                } else {
                    "网络异常，该服务不可用"
                }
                _followSyncResult.emit(UserRepository.FollowSyncResult.Error(isFollow = true, targetUserId, errorMessage))
            }
        }
    }

    override suspend fun unfollowUser(currentUserId: String, targetUserId: String) {
        // 1. 先更新本地数据库
        localDataSource.unfollowUser(currentUserId, targetUserId)
        Log.d(TAG, "Unfollow user: currentUserId=$currentUserId, targetUserId=$targetUserId (local updated)")

        // 2. 异步同步到远程服务器
        syncScope.launch {
            try {
                val result = remoteDataSource.unfollowUser(targetUserId)
                when (result) {
                    is AppResult.Success -> {
                        Log.d(TAG, "Unfollow user sync success: targetUserId=$targetUserId")
                        _followSyncResult.emit(UserRepository.FollowSyncResult.Success(isFollow = false, targetUserId))
                    }
                    is AppResult.Error -> {
                        val errorMessage = when (result.throwable) {
                            is DataException.NetworkException -> "网络异常，该服务不可用"
                            else -> result.message ?: result.throwable.message ?: "网络异常，该服务不可用"
                        }
                        Log.e(TAG, "Unfollow user sync failed: targetUserId=$targetUserId, error: $errorMessage")
                        // 同步失败，回滚本地状态
                        try {
                            localDataSource.followUser(currentUserId, targetUserId)
                            Log.d(TAG, "Unfollow user local state rolled back: targetUserId=$targetUserId")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to rollback unfollow user local state: targetUserId=$targetUserId", e)
                        }
                        // 发送错误通知
                        _followSyncResult.emit(UserRepository.FollowSyncResult.Error(isFollow = false, targetUserId, errorMessage))
                    }
                    else -> {
                        Log.w(TAG, "Unfollow user sync unknown result: targetUserId=$targetUserId")
                        // 未知结果，也回滚本地状态
                        try {
                            localDataSource.followUser(currentUserId, targetUserId)
                            Log.d(TAG, "Unfollow user local state rolled back (unknown result): targetUserId=$targetUserId")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to rollback unfollow user local state: targetUserId=$targetUserId", e)
                        }
                        // 发送错误通知
                        _followSyncResult.emit(UserRepository.FollowSyncResult.Error(isFollow = false, targetUserId, "网络异常，该服务不可用"))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unfollow user sync exception: targetUserId=$targetUserId", e)
                // 异常情况，回滚本地状态
                try {
                    localDataSource.followUser(currentUserId, targetUserId)
                    Log.d(TAG, "Unfollow user local state rolled back (exception): targetUserId=$targetUserId")
                } catch (rollbackException: Exception) {
                    Log.e(TAG, "Failed to rollback unfollow user local state: targetUserId=$targetUserId", rollbackException)
                }
                // 发送错误通知
                val errorMessage = if (e is DataException.NetworkException) {
                    "网络异常，该服务不可用"
                } else {
                    "网络异常，该服务不可用"
                }
                _followSyncResult.emit(UserRepository.FollowSyncResult.Error(isFollow = false, targetUserId, errorMessage))
            }
        }
    }
}

