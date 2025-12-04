package com.ucw.beatu.business.user.data.remote

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
}

