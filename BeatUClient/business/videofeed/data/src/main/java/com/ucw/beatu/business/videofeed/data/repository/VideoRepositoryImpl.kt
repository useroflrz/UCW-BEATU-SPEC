package com.ucw.beatu.business.videofeed.data.repository

import com.ucw.beatu.business.videofeed.data.local.VideoLocalDataSource
import com.ucw.beatu.business.videofeed.data.remote.VideoRemoteDataSource
import com.ucw.beatu.business.videofeed.domain.model.Comment
import com.ucw.beatu.business.videofeed.domain.model.Video
import com.ucw.beatu.business.videofeed.domain.repository.VideoRepository
import com.ucw.beatu.shared.common.mock.MockVideoCatalog
import com.ucw.beatu.shared.common.result.AppResult
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

/**
 * 视频仓储实现
 * 协调本地和远程数据源，实现缓存策略
 */
class VideoRepositoryImpl @Inject constructor(
    private val remoteDataSource: VideoRemoteDataSource,
    private val localDataSource: VideoLocalDataSource
) : VideoRepository {

    override fun getVideoFeed(
        page: Int,
        limit: Int,
        orientation: String?
    ): Flow<AppResult<List<Video>>> = flow {
        emit(AppResult.Loading) // 先通知 Loading

        coroutineScope {
            // 并行：本地 & 远端
            // 优化：优先从本地缓存读取，支持离线滑动
            val localDeferred = async {
                if (orientation == null) {
                    // 读取足够多的缓存数据，支持多页滑动
                    val cacheLimit = limit * maxOf(page, 3) // 至少缓存3页数据
                    localDataSource.observeVideos(cacheLimit).firstOrNull()?.let { cachedVideos ->
                        // 从缓存中提取对应页的数据
                        val startIndex = (page - 1) * limit
                        val endIndex = startIndex + limit
                        if (startIndex < cachedVideos.size) {
                            cachedVideos.subList(startIndex, minOf(endIndex, cachedVideos.size))
                        } else {
                            // 如果缓存数据不足，返回空列表，等待远程数据或mock数据
                            emptyList()
                        }
                    } ?: emptyList()
                } else {
                    emptyList()
                }
            }

            // 远程请求添加超时控制（15秒），与网络配置保持一致
            val remoteDeferred = async {
                try {
                    // 使用15秒超时时间，与网络配置的connectTimeout保持一致
                    withTimeout(15000L) {
                        remoteDataSource.getVideoFeed(page, limit, orientation)
                    }
                } catch (e: TimeoutCancellationException) {
                    // 记录超时错误
                    android.util.Log.e("VideoRepository", "获取视频列表超时: page=$page, limit=$limit, orientation=$orientation", e)
                    AppResult.Error(e)
                } catch (e: Exception) {
                    // 记录其他错误
                    android.util.Log.e("VideoRepository", "获取视频列表失败: page=$page, limit=$limit, orientation=$orientation", e)
                    AppResult.Error(e)
                }
            }

            val localVideos = localDeferred.await()
            if (localVideos.isNotEmpty()) {
                android.util.Log.d("VideoRepository", 
                    "返回本地缓存数据: page=$page, limit=$limit, orientation=$orientation, " +
                    "视频数量=${localVideos.size}"
                )
                emit(AppResult.Success(localVideos, metadata = mapOf("source" to "local"))) // 先显示本地缓存
            }

            // 尝试获取远程数据，如果失败则快速fallback到mock数据
            val remoteResult = try {
                remoteDeferred.await()
            } catch (e: Exception) {
                AppResult.Error(e)
            }

            when (remoteResult) {
                is AppResult.Success -> {
                    // 记录成功获取远程数据
                    android.util.Log.d("VideoRepository", 
                        "成功获取远程数据: page=$page, limit=$limit, orientation=$orientation, " +
                        "视频数量=${remoteResult.data.size}"
                    )
                    // 保存所有页面的数据到本地缓存，支持离线滑动
                    localDataSource.saveVideos(remoteResult.data)
                    // 异步为没有封面的视频生成缩略图
                    localDataSource.enqueueThumbnailGeneration(remoteResult.data)
                    // 标记数据来源为远程
                    emit(AppResult.Success(remoteResult.data, metadata = mapOf("source" to "remote"))) // 用远程最新数据刷新
                }
                is AppResult.Error -> {
                    // 记录远程请求失败的详细信息
                    val errorMessage = remoteResult.message ?: remoteResult.throwable.message ?: "未知错误"
                    val errorType = remoteResult.throwable::class.java.simpleName
                    android.util.Log.w("VideoRepository", 
                        "远程请求失败，使用fallback数据: errorType=$errorType, message=$errorMessage, " +
                        "page=$page, limit=$limit, orientation=$orientation"
                    )
                    
                    // 远程失败时，优先使用本地缓存，如果没有则使用mock数据
                    if (localVideos.isEmpty()) {
                        android.util.Log.w("VideoRepository", "本地缓存为空，使用mock数据作为fallback")
                        val fallbackVideos = buildMockVideos(page, limit, orientation)
                        if (fallbackVideos.isNotEmpty()) {
                            // 保存mock数据到本地缓存，支持后续离线使用
                            localDataSource.saveVideos(fallbackVideos)
                            // 异步为没有封面的视频生成缩略图
                            localDataSource.enqueueThumbnailGeneration(fallbackVideos)
                            emit(AppResult.Success(fallbackVideos, metadata = mapOf("source" to "mock")))
                        } else {
                            // 如果mock数据也没有，才发送错误
                            android.util.Log.e("VideoRepository", "mock数据也为空，发送错误响应")
                            emit(remoteResult)
                        }
                    } else {
                        // 本地已有数据时，静默失败，不发送错误，保持UI流畅
                        // 使用本地缓存数据，确保用户可以继续滑动
                        android.util.Log.d("VideoRepository", "使用本地缓存数据，忽略远程错误")
                        emit(AppResult.Success(localVideos, metadata = mapOf("source" to "local")))
                    }
                }
                is AppResult.Loading -> emit(remoteResult)
            }
        }
    }.catch { 
        // 捕获所有异常，确保不会导致Flow崩溃
        // 如果本地和mock都没有数据，才发送错误
        val fallbackVideos = buildMockVideos(1, 20, null)
        if (fallbackVideos.isNotEmpty()) {
            // 保存mock数据到本地缓存
            localDataSource.saveVideos(fallbackVideos)
            // 异步为没有封面的视频生成缩略图
            localDataSource.enqueueThumbnailGeneration(fallbackVideos)
            emit(AppResult.Success(fallbackVideos, metadata = mapOf("source" to "mock")))
        } else {
            emit(AppResult.Error(it))
        }
    }


    override suspend fun getVideoDetail(videoId: String): AppResult<Video> {
        // 先检查本地缓存
        localDataSource.getVideoById(videoId)?.let {
            // 异步获取最新数据并更新缓存
            val remoteResult = remoteDataSource.getVideoDetail(videoId)
            if (remoteResult is AppResult.Success) {
                localDataSource.saveVideo(remoteResult.data)
                // 异步为没有封面的视频生成缩略图
                localDataSource.enqueueThumbnailGeneration(listOf(remoteResult.data))
                return remoteResult
            }
            // 如果远程获取失败，返回本地缓存
            return AppResult.Success(it)
        }

        // 本地没有缓存，从远程获取
        return when (val result = remoteDataSource.getVideoDetail(videoId)) {
            is AppResult.Success -> {
                localDataSource.saveVideo(result.data)
                // 异步为没有封面的视频生成缩略图
                localDataSource.enqueueThumbnailGeneration(listOf(result.data))
                result
            }
            else -> result
        }
    }

    override fun getComments(videoId: String, page: Int, limit: Int): Flow<AppResult<List<Comment>>> = flow {
        // 如果是第一页，先尝试从本地获取并发送
        if (page == 1) {
            val localComments = localDataSource.observeComments(videoId).firstOrNull() ?: emptyList()
            if (localComments.isNotEmpty()) {
                emit(AppResult.Success(localComments))
            }
        }

        // 从远程获取
        val remoteResult = remoteDataSource.getComments(videoId, page, limit)
        when (remoteResult) {
            is AppResult.Success -> {
                if (page == 1) {
                    localDataSource.saveComments(remoteResult.data)
                }
                emit(remoteResult)
            }
            is AppResult.Error -> {
                // 如果远程失败且是第一页，且已发送本地数据，不再发送错误
                if (page != 1 || localDataSource.observeComments(videoId).firstOrNull()?.isEmpty() != false) {
                    emit(remoteResult)
                }
            }
            is AppResult.Loading -> emit(remoteResult)
        }
    }.onStart { emit(AppResult.Loading) }
        .catch { emit(AppResult.Error(it)) }

    override suspend fun likeVideo(videoId: String): AppResult<Unit> {
        return remoteDataSource.likeVideo(videoId)
    }

    override suspend fun unlikeVideo(videoId: String): AppResult<Unit> {
        return remoteDataSource.unlikeVideo(videoId)
    }

    override suspend fun favoriteVideo(videoId: String): AppResult<Unit> {
        return remoteDataSource.favoriteVideo(videoId)
    }

    override suspend fun unfavoriteVideo(videoId: String): AppResult<Unit> {
        return remoteDataSource.unfavoriteVideo(videoId)
    }

    override suspend fun shareVideo(videoId: String): AppResult<Unit> {
        return remoteDataSource.shareVideo(videoId)
    }

    override suspend fun postComment(videoId: String, content: String): AppResult<Comment> {
        return when (val result = remoteDataSource.postComment(videoId, content)) {
            is AppResult.Success -> {
                // 保存新评论到本地
                localDataSource.saveComment(result.data)
                result
            }
            else -> result
        }
    }

    /**
     * 将 MockVideoCatalog 中的数据映射为领域层 Video，用于网络失败时的服务降级兜底。
     */
    private fun buildMockVideos(
        page: Int,
        limit: Int,
        orientation: String?
    ): List<Video> {
        val requestOrientation = when (orientation?.lowercase()) {
            "landscape", "horizontal" -> MockVideoCatalog.Orientation.LANDSCAPE
            "portrait", "vertical" -> MockVideoCatalog.Orientation.PORTRAIT
            else -> null
        }

        val mockList = MockVideoCatalog.getPage(
            preferredOrientation = requestOrientation,
            page = page,
            pageSize = limit
        )

        if (mockList.isEmpty()) return emptyList()

        return mockList.map { mock ->
            val orientationString = when (mock.orientation) {
                MockVideoCatalog.Orientation.LANDSCAPE -> "landscape"
                MockVideoCatalog.Orientation.PORTRAIT -> "portrait"
            }
            Video(
                id = mock.id,
                playUrl = mock.url,
                coverUrl = "", // Mock 数据暂不提供封面，可后续扩展
                title = mock.title,
                tags = emptyList(),
                durationMs = 0L, // 未知时长，播放器会在 Ready 后更新 UI
                orientation = orientationString,
                authorId = "", // Mock 数据暂无作者 ID，仅展示名称
                authorName = mock.author,
                authorAvatar = null,
                likeCount = mock.likeCount.toLong(),
                commentCount = mock.commentCount.toLong(),
                favoriteCount = mock.favoriteCount.toLong(),
                shareCount = mock.shareCount.toLong(),
                viewCount = 0L,
                createdAt = null,
                updatedAt = null
            )
        }
    }
}

