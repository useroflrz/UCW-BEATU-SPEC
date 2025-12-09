package com.ucw.beatu.business.videofeed.data.remote

import android.util.Log
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
    suspend fun getVideoDetail(videoId: Long): AppResult<Video>  // ✅ 修改：从 String 改为 Long
    suspend fun getComments(videoId: Long, page: Int, limit: Int): AppResult<List<Comment>>  // ✅ 修改：从 String 改为 Long
    suspend fun likeVideo(videoId: Long): AppResult<Unit>  // ✅ 修改：从 String 改为 Long
    suspend fun unlikeVideo(videoId: Long): AppResult<Unit>  // ✅ 修改：从 String 改为 Long
    suspend fun favoriteVideo(videoId: Long): AppResult<Unit>  // ✅ 修改：从 String 改为 Long
    suspend fun unfavoriteVideo(videoId: Long): AppResult<Unit>  // ✅ 修改：从 String 改为 Long
    suspend fun shareVideo(videoId: Long): AppResult<Unit>  // ✅ 修改：从 String 改为 Long
    suspend fun postComment(videoId: Long, content: String): AppResult<Comment>  // ✅ 修改：从 String 改为 Long
    suspend fun getAllVideoInteractions(): AppResult<List<Map<String, Any>>>
    suspend fun getAllWatchHistories(): AppResult<List<Map<String, Any>>>
    suspend fun syncWatchHistories(histories: List<Map<String, Any>>): AppResult<Unit>
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
                        android.util.Log.d("VideoRemoteDataSource", 
                            "解析成功，视频数量: ${data.items.size}, " +
                            "page=${data.page}, pageSize=${data.pageSize}, total=${data.total}, " +
                            "totalPages=${data.totalPages}, hasNext=${data.hasNext}"
                        )
                        if (data.items.isEmpty()) {
                            android.util.Log.w("VideoRemoteDataSource", "警告：items 列表为空！")
                        }
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

    override suspend fun getVideoDetail(videoId: Long): AppResult<Video> {  // ✅ 修改：从 String 改为 Long
        return runAppResult {
            if (!connectivityObserver.isConnected()) {
                throw DataException.NetworkException("No internet connection")
            }

            val response = apiService.getVideoDetail(videoId.toString())  // ✅ 修改：转换为 String 传递给 Retrofit
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

    override suspend fun getComments(videoId: Long, page: Int, limit: Int): AppResult<List<Comment>> {  // ✅ 修改：从 String 改为 Long
        return runAppResult {
            if (!connectivityObserver.isConnected()) {
                throw DataException.NetworkException("No internet connection")
            }

            val response = apiService.getComments(videoId.toString(), page, limit)  // ✅ 修改：转换为 String 传递给 Retrofit
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

    override suspend fun likeVideo(videoId: Long): AppResult<Unit> {  // ✅ 修改：从 String 改为 Long
        return runAppResult {
            if (!connectivityObserver.isConnected()) {
                throw DataException.NetworkException("No internet connection")
            }

            val response = apiService.likeVideo(videoId.toString())  // ✅ 修改：转换为 String 传递给 Retrofit
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

    override suspend fun unlikeVideo(videoId: Long): AppResult<Unit> {  // ✅ 修改：从 String 改为 Long
        return runAppResult {
            if (!connectivityObserver.isConnected()) {
                throw DataException.NetworkException("No internet connection")
            }

            val response = apiService.unlikeVideo(videoId.toString())  // ✅ 修改：转换为 String 传递给 Retrofit
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

    override suspend fun favoriteVideo(videoId: Long): AppResult<Unit> {  // ✅ 修改：从 String 改为 Long
        return runAppResult {
            if (!connectivityObserver.isConnected()) {
                throw DataException.NetworkException("No internet connection")
            }

            val response = apiService.favoriteVideo(videoId.toString())  // ✅ 修改：转换为 String 传递给 Retrofit
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

    override suspend fun unfavoriteVideo(videoId: Long): AppResult<Unit> {  // ✅ 修改：从 String 改为 Long
        return runAppResult {
            if (!connectivityObserver.isConnected()) {
                throw DataException.NetworkException("No internet connection")
            }

            val response = apiService.unfavoriteVideo(videoId.toString())  // ✅ 修改：转换为 String 传递给 Retrofit
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

    override suspend fun shareVideo(videoId: Long): AppResult<Unit> {  // ✅ 修改：从 String 改为 Long
        return runAppResult {
            if (!connectivityObserver.isConnected()) {
                throw DataException.NetworkException("No internet connection")
            }

            val response = apiService.shareVideo(videoId.toString())  // ✅ 修改：转换为 String 传递给 Retrofit
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

    override suspend fun postComment(videoId: Long, content: String): AppResult<Comment> {  // ✅ 修改：从 String 改为 Long
        return runAppResult {
            if (!connectivityObserver.isConnected()) {
                throw DataException.NetworkException("No internet connection")
            }

            val response = apiService.postComment(videoId.toString(), CommentRequest(content))  // ✅ 修改：转换为 String 传递给 Retrofit
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

    override suspend fun getAllVideoInteractions(): AppResult<List<Map<String, Any>>> {
        return runAppResult {
            if (!connectivityObserver.isConnected()) {
                throw DataException.NetworkException("No internet connection")
            }

            Log.d("VideoRemoteDataSource", "开始请求 getAllVideoInteractions")
            val response = apiService.getAllVideoInteractions()
            val dataSize = (response.data as? List<*>)?.size ?: 0
            Log.d("VideoRemoteDataSource", "收到响应: code=${response.code}, message=${response.message}, data=${if (response.data != null) "有数据(${dataSize}条)" else "无数据"}")
            
            val data = response.data as? List<Map<String, Any>>
            when {
                response.isSuccess && data != null -> {
                    Log.d("VideoRemoteDataSource", "解析成功，数据条数: ${data.size}")
                    // 打印第一条数据用于调试
                    if (data.isNotEmpty()) {
                        Log.d("VideoRemoteDataSource", "第一条数据示例: ${data[0]}")
                    }
                    data
                }
                response.isUnauthorized -> {
                    android.util.Log.e("VideoRemoteDataSource", "认证失败: code=${response.code}, message=${response.message}")
                    throw DataException.AuthException(
                        response.message,
                        response.code
                    )
                }
                else -> {
                    android.util.Log.e("VideoRemoteDataSource", "服务器错误: code=${response.code}, message=${response.message}")
                    throw DataException.ServerException(
                        response.message,
                        response.code
                    )
                }
            }
        }
    }

    override suspend fun getAllWatchHistories(): AppResult<List<Map<String, Any>>> {
        return runAppResult {
            if (!connectivityObserver.isConnected()) {
                throw DataException.NetworkException("No internet connection")
            }

            Log.d("VideoRemoteDataSource", "开始请求 getAllWatchHistories")
            val response = apiService.getAllWatchHistories()
            val dataSize = (response.data as? List<*>)?.size ?: 0
            Log.d("VideoRemoteDataSource", "收到响应: code=${response.code}, message=${response.message}, data=${if (response.data != null) "有数据(${dataSize}条)" else "无数据"}")
            
            val data = response.data as? List<Map<String, Any>>
            when {
                response.isSuccess && data != null -> {
                    Log.d("VideoRemoteDataSource", "解析成功，数据条数: ${data.size}")
                    // 打印第一条数据用于调试
                    if (data.isNotEmpty()) {
                        Log.d("VideoRemoteDataSource", "第一条数据示例: ${data[0]}")
                    }
                    data
                }
                response.isUnauthorized -> {
                    Log.e("VideoRemoteDataSource", "认证失败: code=${response.code}, message=${response.message}")
                    throw DataException.AuthException(
                        response.message,
                        response.code
                    )
                }
                else -> {
                    Log.e("VideoRemoteDataSource", "服务器错误: code=${response.code}, message=${response.message}")
                    throw DataException.ServerException(
                        response.message,
                        response.code
                    )
                }
            }
        }
    }

    override suspend fun syncWatchHistories(histories: List<Map<String, Any>>): AppResult<Unit> {
        return runAppResult {
            if (!connectivityObserver.isConnected()) {
                throw DataException.NetworkException("No internet connection")
            }

            Log.d("VideoRemoteDataSource", "开始同步观看历史：${histories.size} 条")
            if (histories.isNotEmpty()) {
                Log.d("VideoRemoteDataSource", "第一条历史记录示例：${histories[0]}")
            }

            // 转换为 DTO 对象列表
            val historyDtos = histories.map { map ->
                com.ucw.beatu.business.videofeed.data.api.dto.WatchHistorySyncRequest(
                    videoId = (map["videoId"] as? Number)?.toLong() ?: 0L,
                    userId = map["userId"] as? String ?: "",
                    lastPlayPositionMs = (map["lastPlayPositionMs"] as? Number)?.toLong() ?: 0L,
                    watchedAt = (map["watchedAt"] as? Number)?.toLong() ?: 0L
                )
            }

            val response = apiService.syncWatchHistories(historyDtos)
            Log.d("VideoRemoteDataSource", "观看历史同步响应：code=${response.code}, message=${response.message}, isSuccess=${response.isSuccess}")
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

