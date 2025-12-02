package com.ucw.beatu.business.videofeed.data.repository

import com.ucw.beatu.business.videofeed.data.local.VideoLocalDataSource
import com.ucw.beatu.business.videofeed.data.remote.VideoRemoteDataSource
import com.ucw.beatu.business.videofeed.domain.model.Comment
import com.ucw.beatu.business.videofeed.domain.model.Video
import com.ucw.beatu.business.videofeed.domain.repository.VideoRepository
import com.ucw.beatu.shared.common.mock.MockVideoCatalog
import com.ucw.beatu.shared.common.result.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * 视频仓储实现
 * 协调本地和远程数据源，实现缓存策略
 */
class VideoRepositoryImpl @Inject constructor(
    private val remoteDataSource: VideoRemoteDataSource,
    private val localDataSource: VideoLocalDataSource
) : VideoRepository {

    override fun getVideoFeed(page: Int, limit: Int, orientation: String?): Flow<AppResult<List<Video>>> = flow {
        // 优先从远程获取最新数据
        val remoteResult = remoteDataSource.getVideoFeed(page, limit, orientation)
        when (remoteResult) {
            is AppResult.Success -> {
                // 保存到本地缓存（仅第一页且没有orientation筛选）
                if (page == 1 && orientation == null) {
                    localDataSource.saveVideos(remoteResult.data)
                }
                emit(remoteResult)
            }
            is AppResult.Error -> {
                // 远程失败时，尝试使用本地缓存
                if (page == 1 && orientation == null) {
                    val localVideos = localDataSource.observeVideos(limit).firstOrNull() ?: emptyList()
                    if (localVideos.isNotEmpty()) {
                        // 有本地缓存，使用缓存数据
                        emit(AppResult.Success(localVideos))
                        return@flow
                    }
                }

                // 本地也没有数据，使用 Mock 数据兜底
                val fallbackVideos = buildMockVideos(page, limit, orientation)
                if (fallbackVideos.isNotEmpty()) {
                    // 将 Mock 数据写入本地缓存（仅第一页且无 orientation 筛选），便于后续离线复用
                    if (page == 1 && orientation == null) {
                        localDataSource.saveVideos(fallbackVideos)
                    }
                    emit(AppResult.Success(fallbackVideos))
                    return@flow
                }

                // 所有降级方案都失败，返回错误
                emit(remoteResult)
            }
            is AppResult.Loading -> emit(remoteResult)
        }
    }.onStart { emit(AppResult.Loading) }
        .catch { emit(AppResult.Error(it)) }

    override suspend fun getVideoDetail(videoId: String): AppResult<Video> {
        // 先检查本地缓存
        localDataSource.getVideoById(videoId)?.let {
            // 异步获取最新数据并更新缓存
            val remoteResult = remoteDataSource.getVideoDetail(videoId)
            if (remoteResult is AppResult.Success) {
                localDataSource.saveVideo(remoteResult.data)
                return remoteResult
            }
            // 如果远程获取失败，返回本地缓存
            return AppResult.Success(it)
        }

        // 本地没有缓存，从远程获取
        return when (val result = remoteDataSource.getVideoDetail(videoId)) {
            is AppResult.Success -> {
                localDataSource.saveVideo(result.data)
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
                isLiked = false,
                isFavorited = false,
                isFollowedAuthor = false,
                createdAt = null,
                updatedAt = null
            )
        }
    }
}

