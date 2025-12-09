package com.ucw.beatu.business.user.data.remote

import android.util.Log
import com.ucw.beatu.business.user.data.api.UserApiService
import com.ucw.beatu.business.user.data.api.dto.UserDto
import com.ucw.beatu.business.user.data.mapper.toDomain
import com.ucw.beatu.business.user.domain.model.User
import com.ucw.beatu.shared.common.exception.DataException
import com.ucw.beatu.shared.common.result.AppResult
import com.ucw.beatu.shared.common.result.runAppResult
import com.ucw.beatu.shared.network.monitor.ConnectivityObserver
import javax.inject.Inject

/**
 * 用户远程数据源接口
 */
interface UserRemoteDataSource {
    suspend fun getUserById(userId: String): AppResult<User>
    suspend fun getUserByName(userName: String): AppResult<User>
    suspend fun followUser(userId: String): AppResult<Unit>
    suspend fun unfollowUser(userId: String): AppResult<Unit>
    suspend fun getAllUsers(): AppResult<List<User>>
    suspend fun getUserFollows(userId: String): AppResult<List<Map<String, Any>>>
}

/**
 * 用户远程数据源实现
 * 负责从后端服务获取用户数据
 */
class UserRemoteDataSourceImpl @Inject constructor(
    private val apiService: UserApiService,
    private val connectivityObserver: ConnectivityObserver
) : UserRemoteDataSource {

    override suspend fun getUserById(userId: String): AppResult<User> {
        return runAppResult {
            if (!connectivityObserver.isConnected()) {
                throw DataException.NetworkException("No internet connection")
            }

            val response = apiService.getUserById(userId)
            val data = response.data
            when {
                response.isSuccess && data != null -> {
                    data.toDomain()
                }
                response.isUnauthorized -> {
                    throw DataException.AuthException(
                        response.message,
                        response.code
                    )
                }
                response.isNotFound -> {
                    throw DataException.NotFoundException(response.message)
                }
                else -> {
                    throw DataException.ServerException(
                        response.message,
                        response.code
                    )
                }
            }
        }
    }

    override suspend fun getUserByName(userName: String): AppResult<User> {
        return runAppResult {
            if (!connectivityObserver.isConnected()) {
                throw DataException.NetworkException("No internet connection")
            }

            val response = apiService.getUserByName(userName)
            val data = response.data
            when {
                response.isSuccess && data != null -> {
                    data.toDomain()
                }
                response.isUnauthorized -> {
                    throw DataException.AuthException(
                        response.message,
                        response.code
                    )
                }
                response.isNotFound -> {
                    throw DataException.NotFoundException(response.message)
                }
                else -> {
                    throw DataException.ServerException(
                        response.message,
                        response.code
                    )
                }
            }
        }
    }

    override suspend fun followUser(userId: String): AppResult<Unit> {
        return runAppResult {
            if (!connectivityObserver.isConnected()) {
                throw DataException.NetworkException("No internet connection")
            }

            val response = apiService.followUser(userId)
            when {
                response.isSuccess -> Unit
                response.isUnauthorized -> {
                    throw DataException.AuthException(
                        response.message,
                        response.code
                    )
                }
                else -> {
                    throw DataException.ServerException(
                        response.message,
                        response.code
                    )
                }
            }
        }
    }

    override suspend fun unfollowUser(userId: String): AppResult<Unit> {
        return runAppResult {
            if (!connectivityObserver.isConnected()) {
                throw DataException.NetworkException("No internet connection")
            }

            val response = apiService.unfollowUser(userId)
            when {
                response.isSuccess -> Unit
                response.isUnauthorized -> {
                    throw DataException.AuthException(
                        response.message,
                        response.code
                    )
                }
                else -> {
                    throw DataException.ServerException(
                        response.message,
                        response.code
                    )
                }
            }
        }
    }

    override suspend fun getAllUsers(): AppResult<List<User>> {
        return runAppResult {
            if (!connectivityObserver.isConnected()) {
                throw DataException.NetworkException("No internet connection")
            }

            val response = apiService.getAllUsers()
            val data = response.data
            when {
                response.isSuccess && data != null -> {
                    data.map { it.toDomain() }
                }
                response.isUnauthorized -> {
                    throw DataException.AuthException(
                        response.message,
                        response.code
                    )
                }
                else -> {
                    throw DataException.ServerException(
                        response.message,
                        response.code
                    )
                }
            }
        }
    }

    override suspend fun getUserFollows(userId: String): AppResult<List<Map<String, Any>>> {
        return runAppResult {
            if (!connectivityObserver.isConnected()) {
                throw DataException.NetworkException("No internet connection")
            }

            Log.d("UserRemoteDataSource", "开始请求 getUserFollows: userId=$userId")
            val response = apiService.getUserFollows(userId)
            val dataSize = (response.data as? List<*>)?.size ?: 0
            Log.d("UserRemoteDataSource", "收到响应: code=${response.code}, message=${response.message}, data=${if (response.data != null) "有数据(${dataSize}条)" else "无数据"}")
            
            val data = response.data as? List<Map<String, Any>>
            when {
                response.isSuccess && data != null -> {
                    Log.d("UserRemoteDataSource", "解析成功，数据条数: ${data.size}")
                    // 打印第一条数据用于调试
                    if (data.isNotEmpty()) {
                        Log.d("UserRemoteDataSource", "第一条数据示例: ${data[0]}")
                    }
                    data
                }
                response.isUnauthorized -> {
                    Log.e("UserRemoteDataSource", "认证失败: code=${response.code}, message=${response.message}")
                    throw DataException.AuthException(
                        response.message,
                        response.code
                    )
                }
                else -> {
                    Log.e("UserRemoteDataSource", "服务器错误: code=${response.code}, message=${response.message}")
                    throw DataException.ServerException(
                        response.message,
                        response.code
                    )
                }
            }
        }
    }
}

