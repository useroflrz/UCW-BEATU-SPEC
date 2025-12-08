package com.ucw.beatu.business.videofeed.data.repository

import com.ucw.beatu.business.videofeed.data.local.VideoInteractionLocalDataSource
import com.ucw.beatu.business.videofeed.data.local.VideoLocalDataSource
import com.ucw.beatu.business.videofeed.data.mapper.toEntity
import com.ucw.beatu.business.videofeed.data.remote.VideoRemoteDataSource
import com.ucw.beatu.business.videofeed.domain.model.Comment
import com.ucw.beatu.business.videofeed.domain.model.Video
import com.ucw.beatu.business.videofeed.domain.repository.VideoRepository
import com.ucw.beatu.shared.database.BeatUDatabase
import com.ucw.beatu.shared.database.datastore.PreferencesDataStore
import com.ucw.beatu.shared.database.entity.WatchHistoryEntity
import com.ucw.beatu.shared.common.exception.DataException
import com.ucw.beatu.shared.common.result.AppResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.internal.AbortFlowException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import android.util.Log
import javax.inject.Inject

/**
 * 视频仓储实现
 * 协调本地和远程数据源，实现缓存策略和乐观更新
 */
class VideoRepositoryImpl @Inject constructor(
    private val remoteDataSource: VideoRemoteDataSource,
    private val localDataSource: VideoLocalDataSource,
    private val interactionLocalDataSource: VideoInteractionLocalDataSource,
    private val preferencesDataStore: PreferencesDataStore,
    private val database: BeatUDatabase
) : VideoRepository {

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val TAG = "VideoRepositoryImpl"
    
    // ✅ 修改：当前用户ID，根据SQL，userId "BEATU" 对应的 userName 也是 "BEATU"
    // 使用 userId 而不是 userName，确保与数据库一致
    private val currentUserId: String = "BEATU"  // userId，不是 userName

    override fun getVideoFeed(
        page: Int,
        limit: Int,
        orientation: String?
    ): Flow<AppResult<List<Video>>> = flow {
        emit(AppResult.Loading) // 先通知 Loading

        coroutineScope {
            // ✅ 渲染逻辑：数据先取出，再渲染
            // 1. 先从本地数据库读取并显示（支持离线滑动）
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
                            // 如果缓存数据不足，返回空列表，等待远程数据
                            emptyList()
                        }
                    } ?: emptyList()
                } else {
                    emptyList()
                }
            }

            // 2. 异步从远程获取数据
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

            // 3. 先显示本地缓存数据（如果存在）
            val localVideos = localDeferred.await()
            if (localVideos.isNotEmpty()) {
                android.util.Log.d("VideoRepository", 
                    "返回本地缓存数据: page=$page, limit=$limit, orientation=$orientation, " +
                    "视频数量=${localVideos.size}"
                )
                emit(AppResult.Success(localVideos, metadata = mapOf("source" to "local"))) // 先显示本地缓存
            }

            // 4. 获取远程数据并保存到本地数据库
            val remoteResult = try {
                remoteDeferred.await()
            } catch (e: Exception) {
                AppResult.Error(e)
            }

            when (remoteResult) {
                is AppResult.Success -> {
                    // ✅ 渲染逻辑：后端把所有数据先塞到客户端本地数据库
                    android.util.Log.d("VideoRepository", 
                        "成功获取远程数据: page=$page, limit=$limit, orientation=$orientation, " +
                        "视频数量=${remoteResult.data.size}"
                    )
                    // 保存所有页面的数据到本地缓存，支持离线滑动
                    localDataSource.saveVideos(remoteResult.data)
                    // 异步为没有封面的视频生成缩略图
                    localDataSource.enqueueThumbnailGeneration(remoteResult.data)
                    // ✅ 然后界面再从本地数据库读出来显示
                    // 重新从本地数据库读取，确保数据一致性
                    val savedVideos = localDataSource.observeVideos(limit * page).firstOrNull()?.let { cachedVideos ->
                        val startIndex = (page - 1) * limit
                        val endIndex = startIndex + limit
                        if (startIndex < cachedVideos.size) {
                            cachedVideos.subList(startIndex, minOf(endIndex, cachedVideos.size))
                        } else {
                            remoteResult.data
                        }
                    } ?: remoteResult.data
                    // 标记数据来源为远程
                    emit(AppResult.Success(savedVideos, metadata = mapOf("source" to "remote"))) // 用远程最新数据刷新
                }
                is AppResult.Error -> {
                    // 记录远程请求失败的详细信息
                    val errorMessage = remoteResult.message ?: remoteResult.throwable.message ?: "未知错误"
                    val errorType = remoteResult.throwable::class.java.simpleName
                    android.util.Log.w("VideoRepository", 
                        "远程请求失败，使用fallback数据: errorType=$errorType, message=$errorMessage, " +
                        "page=$page, limit=$limit, orientation=$orientation"
                    )
                    
                    // 远程失败时，优先使用本地缓存
                    if (localVideos.isEmpty()) {
                        // 本地缓存为空，直接返回远程错误
                        android.util.Log.w("VideoRepository", "本地缓存为空，返回远程错误")
                            emit(remoteResult)
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
        // 如果本地和远程都没有数据，返回错误
            emit(AppResult.Error(it))
    }


    override suspend fun getVideoDetail(videoId: Long): AppResult<Video> {  // ✅ 修改：从 String 改为 Long
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

    override fun getComments(videoId: Long, page: Int, limit: Int): Flow<AppResult<List<Comment>>> = flow {  // ✅ 修改：从 String 改为 Long
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
                if (page != 1 || localDataSource.observeComments(videoId).firstOrNull()?.isEmpty() != false) {  // ✅ 修改：videoId 已经是 Long
                    emit(remoteResult)
                }
            }
            is AppResult.Loading -> emit(remoteResult)
        }
    }.onStart { emit(AppResult.Loading) }
        .catch { emit(AppResult.Error(it)) }

    override suspend fun likeVideo(videoId: Long): AppResult<Unit> {
        // 1. 先更新本地数据库（乐观更新）
        interactionLocalDataSource.updateLikeStatus(videoId, currentUserId, isLiked = true, isPending = true)
        Log.d(TAG, "Like video: videoId=$videoId, userId=$currentUserId (local updated)")

        // 2. 异步同步到远程服务器
        syncScope.launch {
            try {
                val result = remoteDataSource.likeVideo(videoId)
                when (result) {
                    is AppResult.Success -> {
                        Log.d(TAG, "Like video sync success: videoId=$videoId")
                        // 清除待同步标记
                        interactionLocalDataSource.clearPendingStatus(videoId, currentUserId)
                    }
                    is AppResult.Error -> {
                        val errorMessage = result.message ?: result.throwable.message ?: "网络异常"
                        Log.e(TAG, "Like video sync failed: videoId=$videoId, error: $errorMessage")
                        // 同步失败，回滚本地状态
                        try {
                            val existing = interactionLocalDataSource.getInteraction(videoId, currentUserId)
                            if (existing?.isFavorited == true) {
                                // 如果还有收藏状态，只取消点赞
                                interactionLocalDataSource.updateLikeStatus(videoId, currentUserId, isLiked = false, isPending = false)
                            } else {
                                // 如果没有其他状态，删除记录
                                interactionLocalDataSource.deleteInteraction(videoId, currentUserId)
                            }
                            Log.d(TAG, "Like video local state rolled back: videoId=$videoId")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to rollback like video local state: videoId=$videoId", e)
                        }
                    }
                    else -> {
                        Log.w(TAG, "Like video sync unknown result: videoId=$videoId")
                        // 未知结果，也回滚本地状态
                        try {
                            interactionLocalDataSource.deleteInteraction(videoId, currentUserId)
                            Log.d(TAG, "Like video local state rolled back (unknown result): videoId=$videoId")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to rollback like video local state: videoId=$videoId", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Like video sync exception: videoId=$videoId", e)
                // 异常情况，回滚本地状态
                try {
                    interactionLocalDataSource.deleteInteraction(videoId, currentUserId)
                    Log.d(TAG, "Like video local state rolled back (exception): videoId=$videoId")
                } catch (rollbackException: Exception) {
                    Log.e(TAG, "Failed to rollback like video local state: videoId=$videoId", rollbackException)
                }
            }
        }

        // 3. 立即返回成功，让 UI 立即更新
        return AppResult.Success(Unit)
    }

    override suspend fun unlikeVideo(videoId: Long): AppResult<Unit> {
        // 1. 先更新本地数据库（乐观更新）
        interactionLocalDataSource.updateLikeStatus(videoId, currentUserId, isLiked = false, isPending = true)
        Log.d(TAG, "Unlike video: videoId=$videoId, userId=$currentUserId (local updated)")

        // 2. 异步同步到远程服务器
        syncScope.launch {
            try {
                val result = remoteDataSource.unlikeVideo(videoId)
                when (result) {
                    is AppResult.Success -> {
                        Log.d(TAG, "Unlike video sync success: videoId=$videoId")
                        // 清除待同步标记
                        interactionLocalDataSource.clearPendingStatus(videoId, currentUserId)
                    }
                    is AppResult.Error -> {
                        val errorMessage = result.message ?: result.throwable.message ?: "网络异常"
                        Log.e(TAG, "Unlike video sync failed: videoId=$videoId, error: $errorMessage")
                        // 同步失败，回滚本地状态（恢复点赞）
                        try {
                            interactionLocalDataSource.updateLikeStatus(videoId, currentUserId, isLiked = true, isPending = false)
                            Log.d(TAG, "Unlike video local state rolled back: videoId=$videoId")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to rollback unlike video local state: videoId=$videoId", e)
                        }
                    }
                    else -> {
                        Log.w(TAG, "Unlike video sync unknown result: videoId=$videoId")
                        // 未知结果，也回滚本地状态
                        try {
                            interactionLocalDataSource.updateLikeStatus(videoId, currentUserId, isLiked = true, isPending = false)
                            Log.d(TAG, "Unlike video local state rolled back (unknown result): videoId=$videoId")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to rollback unlike video local state: videoId=$videoId", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unlike video sync exception: videoId=$videoId", e)
                // 异常情况，回滚本地状态
                try {
                    interactionLocalDataSource.updateLikeStatus(videoId, currentUserId, isLiked = true, isPending = false)
                    Log.d(TAG, "Unlike video local state rolled back (exception): videoId=$videoId")
                } catch (rollbackException: Exception) {
                    Log.e(TAG, "Failed to rollback unlike video local state: videoId=$videoId", rollbackException)
                }
            }
        }

        // 3. 立即返回成功，让 UI 立即更新
        return AppResult.Success(Unit)
    }

    override suspend fun favoriteVideo(videoId: Long): AppResult<Unit> {
        // 1. 先更新本地数据库（乐观更新）
        interactionLocalDataSource.updateFavoriteStatus(videoId, currentUserId, isFavorited = true, isPending = true)
        Log.d(TAG, "Favorite video: videoId=$videoId, userId=$currentUserId (local updated)")

        // 2. 异步同步到远程服务器
        syncScope.launch {
            try {
                val result = remoteDataSource.favoriteVideo(videoId)
                when (result) {
                    is AppResult.Success -> {
                        Log.d(TAG, "Favorite video sync success: videoId=$videoId")
                        // 清除待同步标记
                        interactionLocalDataSource.clearPendingStatus(videoId, currentUserId)
                    }
                    is AppResult.Error -> {
                        val errorMessage = result.message ?: result.throwable.message ?: "网络异常"
                        Log.e(TAG, "Favorite video sync failed: videoId=$videoId, error: $errorMessage")
                        // 同步失败，回滚本地状态
                        try {
                            val existing = interactionLocalDataSource.getInteraction(videoId, currentUserId)
                            if (existing?.isLiked == true) {
                                // 如果还有点赞状态，只取消收藏
                                interactionLocalDataSource.updateFavoriteStatus(videoId, currentUserId, isFavorited = false, isPending = false)
                            } else {
                                // 如果没有其他状态，删除记录
                                interactionLocalDataSource.deleteInteraction(videoId, currentUserId)
                            }
                            Log.d(TAG, "Favorite video local state rolled back: videoId=$videoId")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to rollback favorite video local state: videoId=$videoId", e)
                        }
                    }
                    else -> {
                        Log.w(TAG, "Favorite video sync unknown result: videoId=$videoId")
                        // 未知结果，也回滚本地状态
                        try {
                            interactionLocalDataSource.deleteInteraction(videoId, currentUserId)
                            Log.d(TAG, "Favorite video local state rolled back (unknown result): videoId=$videoId")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to rollback favorite video local state: videoId=$videoId", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Favorite video sync exception: videoId=$videoId", e)
                // 异常情况，回滚本地状态
                try {
                    interactionLocalDataSource.deleteInteraction(videoId, currentUserId)
                    Log.d(TAG, "Favorite video local state rolled back (exception): videoId=$videoId")
                } catch (rollbackException: Exception) {
                    Log.e(TAG, "Failed to rollback favorite video local state: videoId=$videoId", rollbackException)
                }
            }
        }

        // 3. 立即返回成功，让 UI 立即更新
        return AppResult.Success(Unit)
    }

    override suspend fun unfavoriteVideo(videoId: Long): AppResult<Unit> {
        // 1. 先更新本地数据库（乐观更新）
        interactionLocalDataSource.updateFavoriteStatus(videoId, currentUserId, isFavorited = false, isPending = true)
        Log.d(TAG, "Unfavorite video: videoId=$videoId, userId=$currentUserId (local updated)")

        // 2. 异步同步到远程服务器
        syncScope.launch {
            try {
                val result = remoteDataSource.unfavoriteVideo(videoId)
                when (result) {
                    is AppResult.Success -> {
                        Log.d(TAG, "Unfavorite video sync success: videoId=$videoId")
                        // 清除待同步标记
                        interactionLocalDataSource.clearPendingStatus(videoId, currentUserId)
                    }
                    is AppResult.Error -> {
                        val errorMessage = result.message ?: result.throwable.message ?: "网络异常"
                        Log.e(TAG, "Unfavorite video sync failed: videoId=$videoId, error: $errorMessage")
                        // 同步失败，回滚本地状态（恢复收藏）
                        try {
                            interactionLocalDataSource.updateFavoriteStatus(videoId, currentUserId, isFavorited = true, isPending = false)
                            Log.d(TAG, "Unfavorite video local state rolled back: videoId=$videoId")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to rollback unfavorite video local state: videoId=$videoId", e)
                        }
                    }
                    else -> {
                        Log.w(TAG, "Unfavorite video sync unknown result: videoId=$videoId")
                        // 未知结果，也回滚本地状态
                        try {
                            interactionLocalDataSource.updateFavoriteStatus(videoId, currentUserId, isFavorited = true, isPending = false)
                            Log.d(TAG, "Unfavorite video local state rolled back (unknown result): videoId=$videoId")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to rollback unfavorite video local state: videoId=$videoId", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unfavorite video sync exception: videoId=$videoId", e)
                // 异常情况，回滚本地状态
                try {
                    interactionLocalDataSource.updateFavoriteStatus(videoId, currentUserId, isFavorited = true, isPending = false)
                    Log.d(TAG, "Unfavorite video local state rolled back (exception): videoId=$videoId")
                } catch (rollbackException: Exception) {
                    Log.e(TAG, "Failed to rollback unfavorite video local state: videoId=$videoId", rollbackException)
                }
            }
        }

        // 3. 立即返回成功，让 UI 立即更新
        return AppResult.Success(Unit)
    }

    override suspend fun shareVideo(videoId: Long): AppResult<Unit> {  // ✅ 修改：从 String 改为 Long
        return remoteDataSource.shareVideo(videoId)
    }

    override suspend fun postComment(videoId: Long, content: String): AppResult<Comment> {
        val userId = preferencesDataStore.getStringSync("current_user_id") ?: "BEATU"
        val tempCommentId = "pending_${System.currentTimeMillis()}_${videoId}" // 临时评论ID
        
        // 1. 乐观更新：先保存评论到本地数据库（isPending = true）
        val pendingComment = Comment(
            id = tempCommentId,
            videoId = videoId,
            authorId = userId,
            authorName = "BEATU", // 临时用户名，同步成功后会更新
            authorAvatar = null,
            content = content,
            createdAt = System.currentTimeMillis(),
            isAiReply = false,
            likeCount = 0
        )
        // 保存为待同步状态（isPending = true）
        val pendingEntity = pendingComment.toEntity().copy(isPending = true)
        localDataSource.saveComment(pendingComment) // 保存临时评论
        
        Log.d(TAG, "Post comment: videoId=$videoId, tempCommentId=$tempCommentId (local saved, pending sync)")

        // 2. 异步同步到远程服务器
        syncScope.launch {
            try {
                val result = remoteDataSource.postComment(videoId, content)
                when (result) {
            is AppResult.Success -> {
                        Log.d(TAG, "Post comment sync success: videoId=$videoId, commentId=${result.data.id}")
                        // 同步成功，删除临时评论，保存真实评论（isPending = false）
                        try {
                            // 删除临时评论
                            localDataSource.deleteCommentById(tempCommentId)
                            // 保存真实评论（isPending = false）
                localDataSource.saveComment(result.data)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to update comment after sync success: videoId=$videoId", e)
                        }
                    }
                    is AppResult.Error -> {
                        val errorMessage = result.message ?: result.throwable.message ?: "网络异常，该服务不可用"
                        Log.e(TAG, "Post comment sync failed: videoId=$videoId, error: $errorMessage")
                        // 同步失败，删除本地待同步评论（策略A：回滚UI）
                        try {
                            localDataSource.deleteCommentById(tempCommentId)
                            Log.d(TAG, "Post comment local state rolled back: videoId=$videoId, tempCommentId=$tempCommentId")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to rollback post comment local state: videoId=$videoId", e)
                        }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Log.e(TAG, "Post comment sync exception: videoId=$videoId", e)
                // 异常情况，删除本地待同步评论（策略A：回滚UI）
                try {
                    localDataSource.deleteCommentById(tempCommentId)
                    Log.d(TAG, "Post comment local state rolled back (exception): videoId=$videoId, tempCommentId=$tempCommentId")
                } catch (rollbackException: Exception) {
                    Log.e(TAG, "Failed to rollback post comment local state: videoId=$videoId", rollbackException)
                }
            }
        }
        
        // 立即返回临时评论，实现乐观更新
        return AppResult.Success(pendingComment)
    }

    override fun getFollowFeed(page: Int, limit: Int): Flow<AppResult<List<Video>>> = flow {
        emit(AppResult.Loading)

        coroutineScope {
            // ✅ 渲染逻辑：数据先取出，再渲染
            // 1. 先从本地数据库读取用户关注的所有作者
            val followeesDeferred = async {
                database.userFollowDao().observeFollowees(currentUserId).firstOrNull() ?: emptyList()
            }

            val followees = followeesDeferred.await()
            val authorIds = followees.map { it.authorId }.toSet()

            if (authorIds.isEmpty()) {
                // 没有关注的作者，返回空列表
                emit(AppResult.Success(emptyList(), metadata = mapOf("source" to "local")))
                return@coroutineScope
            }

            // 2. 从本地数据库读取这些作者的所有视频
            val localVideosDeferred = async {
                val allVideos = localDataSource.observeVideos(limit * page).firstOrNull() ?: emptyList()
                // 筛选出关注的作者发布的视频
                allVideos.filter { it.authorId in authorIds }
                    .sortedByDescending { it.id }  // 按视频ID降序排序（最新的在前）
                    .let { videos ->
                        val startIndex = (page - 1) * limit
                        val endIndex = startIndex + limit
                        if (startIndex < videos.size) {
                            videos.subList(startIndex, minOf(endIndex, videos.size))
                        } else {
                            emptyList()
                        }
                    }
            }

            val localVideos = localVideosDeferred.await()
            if (localVideos.isNotEmpty()) {
                emit(AppResult.Success(localVideos, metadata = mapOf("source" to "local")))
            }

            // 3. 从远程获取关注作者的最新视频
            val remoteDeferred = async {
                try {
                    withTimeout(15000L) {
                        // 获取所有关注作者的最新视频
                        // 注意：这里需要后端支持按作者ID列表筛选视频的接口
                        // 暂时使用通用接口，后续可以优化
                        remoteDataSource.getVideoFeed(page, limit, null)
                    }
                } catch (e: TimeoutCancellationException) {
                    Log.e(TAG, "获取关注页视频列表超时: page=$page, limit=$limit", e)
                    AppResult.Error(e)
                } catch (e: Exception) {
                    Log.e(TAG, "获取关注页视频列表失败: page=$page, limit=$limit", e)
                    AppResult.Error(e)
                }
            }

            val remoteResult = try {
                remoteDeferred.await()
            } catch (e: Exception) {
                AppResult.Error(e)
            }

            when (remoteResult) {
                is AppResult.Success -> {
                    // ✅ 渲染逻辑：后端把所有数据先塞到客户端本地数据库
                    val followVideos = remoteResult.data.filter { it.authorId in authorIds }
                    if (followVideos.isNotEmpty()) {
                        localDataSource.saveVideos(followVideos)
                        // ✅ 然后界面再从本地数据库读出来显示
                        val savedVideos = localDataSource.observeVideos(limit * page).firstOrNull()?.let { allVideos ->
                            allVideos.filter { it.authorId in authorIds }
                                .sortedByDescending { it.id }
                                .let { videos ->
                                    val startIndex = (page - 1) * limit
                                    val endIndex = startIndex + limit
                                    if (startIndex < videos.size) {
                                        videos.subList(startIndex, minOf(endIndex, videos.size))
                                    } else {
                                        followVideos
                                    }
                                }
                        } ?: followVideos
                        emit(AppResult.Success(savedVideos, metadata = mapOf("source" to "remote")))
                    } else {
                        // 如果没有新的关注视频，使用本地数据
                        if (localVideos.isNotEmpty()) {
                            emit(AppResult.Success(localVideos, metadata = mapOf("source" to "local")))
                        } else {
                            emit(AppResult.Success(emptyList(), metadata = mapOf("source" to "remote")))
                        }
                    }
                }
                is AppResult.Error -> {
                    if (localVideos.isEmpty()) {
                        emit(remoteResult)
                    } else {
                        emit(AppResult.Success(localVideos, metadata = mapOf("source" to "local")))
                    }
                }
                is AppResult.Loading -> emit(remoteResult)
            }
        }
    }.catch {
        emit(AppResult.Error(it))
    }

    override suspend fun saveWatchHistory(videoId: Long, positionMs: Long) {
        // ✅ 观看历史异步写入：用户点击开始观看视频时，按照文档的异步写入数据库，上传远程
        // 策略B：不回滚（弱一致性数据，自动重试同步）
        syncScope.launch {
            try {
                val watchHistory = WatchHistoryEntity(
                    videoId = videoId,
                    userId = currentUserId,
                    lastPlayPositionMs = positionMs,
                    watchedAt = System.currentTimeMillis(),
                    isPending = true  // 标记为待同步
                )

                // 1. 先保存到本地数据库
                database.watchHistoryDao().upsert(watchHistory)
                Log.d(TAG, "保存观看历史到本地: videoId=$videoId, userId=$currentUserId, position=$positionMs")

                // 2. 异步上传到远程（在 DataSyncService 中处理）
                // 这里只保存到本地，DataSyncService 会定期同步待同步的观看历史
            } catch (e: Exception) {
                Log.e(TAG, "保存观看历史失败: videoId=$videoId, userId=$currentUserId", e)
                // 策略B：不回滚，保留待同步状态，下次启动时继续重试
            }
        }
    }

}

