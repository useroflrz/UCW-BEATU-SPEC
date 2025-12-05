package com.ucw.beatu.business.videofeed.data.remote

import com.ucw.beatu.business.videofeed.data.api.VideoFeedApiService
import com.ucw.beatu.business.videofeed.data.api.dto.CommentRequest
import com.ucw.beatu.business.videofeed.data.mapper.toDomain
import com.ucw.beatu.business.videofeed.domain.model.Comment
import com.ucw.beatu.business.videofeed.domain.model.Video
import com.ucw.beatu.shared.common.exception.DataException
import com.ucw.beatu.shared.common.result.AppResult
import com.ucw.beatu.shared.common.result.runAppResult
import com.ucw.beatu.shared.network.monitor.ConnectivityObserver
import javax.inject.Inject

/**
 * 远程数据源接口
 */
interface VideoRemoteDataSource {
    suspend fun getVideoFeed(page: Int, limit: Int, orientation: String? = null): AppResult<List<Video>>
    suspend fun getVideoDetail(videoId: String): AppResult<Video>
    suspend fun getComments(videoId: String, page: Int, limit: Int): AppResult<List<Comment>>
    suspend fun likeVideo(videoId: String): AppResult<Unit>
    suspend fun unlikeVideo(videoId: String): AppResult<Unit>
    suspend fun favoriteVideo(videoId: String): AppResult<Unit>
    suspend fun unfavoriteVideo(videoId: String): AppResult<Unit>
    suspend fun shareVideo(videoId: String): AppResult<Unit>
    suspend fun postComment(videoId: String, content: String): AppResult<Comment>
}

/**
 * 远程数据源实现
 * 负责从MySQL后端服务获取数据
 */
class VideoRemoteDataSourceImpl @Inject constructor(
    private val apiService: VideoFeedApiService,
    private val connectivityObserver: ConnectivityObserver
) : VideoRemoteDataSource {

    override suspend fun getVideoFeed(page: Int, limit: Int, orientation: String?): AppResult<List<Video>> {
        return runAppResult {
            // 检查网络连接
            if (!connectivityObserver.isConnected()) {
                android.util.Log.e("VideoRemoteDataSource", "网络未连接")
                throw DataException.NetworkException("No internet connection")
            }

            android.util.Log.d("VideoRemoteDataSource", "开始请求视频列表: page=$page, limit=$limit, orientation=$orientation")
            
            try {
                val response = apiService.getVideoFeed(page, limit, orientation)
                android.util.Log.d("VideoRemoteDataSource", 
                    "收到响应: code=${response.code}, message=${response.message}, " +
                    "data=${if (response.data != null) "有数据" else "无数据"}"
                )
                
                val data = response.data
                when {
                    response.isSuccess && data != null -> {
                        android.util.Log.d("VideoRemoteDataSource", "解析成功，视频数量: ${data.items.size}")
                        data.items.map { it.toDomain() }
                    }
                    response.isUnauthorized -> {
                        android.util.Log.e("VideoRemoteDataSource", "认证失败: code=${response.code}, message=${response.message}")
                        throw DataException.AuthException(
                            response.message,
                            response.code
                        )
                    }
                    response.isNotFound -> {
                        android.util.Log.e("VideoRemoteDataSource", "资源未找到: code=${response.code}, message=${response.message}")
                        throw DataException.NotFoundException(response.message)
                    }
                    else -> {
                        android.util.Log.e("VideoRemoteDataSource", 
                            "服务器错误: code=${response.code}, message=${response.message}, " +
                            "data=${response.data}"
                        )
                        throw DataException.ServerException(
                            response.message ?: "Unknown server error",
                            response.code
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("VideoRemoteDataSource", 
                    "请求异常: ${e.javaClass.simpleName}, message=${e.message}", 
                    e
                )
                throw e
            }
        }
    }

    override suspend fun getVideoDetail(videoId: String): AppResult<Video> {
        return runAppResult {
            if (!connectivityObserver.isConnected()) {
                throw DataException.NetworkException("No internet connection")
            }

            val response = apiService.getVideoDetail(videoId)
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

    override suspend fun getComments(videoId: String, page: Int, limit: Int): AppResult<List<Comment>> {
        return runAppResult {
            if (!connectivityObserver.isConnected()) {
                throw DataException.NetworkException("No internet connection")
            }

            val response = apiService.getComments(videoId, page, limit)
            val data = response.data
            when {
                response.isSuccess && data != null -> {
                    data.items.map { it.toDomain() }
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

    override suspend fun likeVideo(videoId: String): AppResult<Unit> {
        return runAppResult {
            if (!connectivityObserver.isConnected()) {
                throw DataException.NetworkException("No internet connection")
            }

            val response = apiService.likeVideo(videoId)
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

    override suspend fun unlikeVideo(videoId: String): AppResult<Unit> {
        return runAppResult {
            if (!connectivityObserver.isConnected()) {
                throw DataException.NetworkException("No internet connection")
            }

            val response = apiService.unlikeVideo(videoId)
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

    override suspend fun favoriteVideo(videoId: String): AppResult<Unit> {
        return runAppResult {
            if (!connectivityObserver.isConnected()) {
                throw DataException.NetworkException("No internet connection")
            }

            val response = apiService.favoriteVideo(videoId)
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

    override suspend fun unfavoriteVideo(videoId: String): AppResult<Unit> {
        return runAppResult {
            if (!connectivityObserver.isConnected()) {
                throw DataException.NetworkException("No internet connection")
            }

            val response = apiService.unfavoriteVideo(videoId)
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

    override suspend fun shareVideo(videoId: String): AppResult<Unit> {
        return runAppResult {
            if (!connectivityObserver.isConnected()) {
                throw DataException.NetworkException("No internet connection")
            }

            val response = apiService.shareVideo(videoId)
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

    override suspend fun postComment(videoId: String, content: String): AppResult<Comment> {
        return runAppResult {
            if (!connectivityObserver.isConnected()) {
                throw DataException.NetworkException("No internet connection")
            }

            val response = apiService.postComment(videoId, CommentRequest(content))
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

