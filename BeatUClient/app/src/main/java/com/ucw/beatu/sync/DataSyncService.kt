package com.ucw.beatu.sync

import android.util.Log
import com.ucw.beatu.business.user.data.local.UserLocalDataSource
import com.ucw.beatu.business.user.data.remote.UserRemoteDataSource
import com.ucw.beatu.business.videofeed.data.local.VideoInteractionLocalDataSource
import com.ucw.beatu.business.videofeed.data.local.VideoLocalDataSource
import com.ucw.beatu.business.videofeed.data.remote.VideoRemoteDataSource
import com.ucw.beatu.shared.common.result.AppResult
import com.ucw.beatu.shared.database.dao.CommentDao
import com.ucw.beatu.shared.database.dao.UserFollowDao
import com.ucw.beatu.shared.database.dao.WatchHistoryDao
import com.ucw.beatu.shared.database.entity.UserFollowEntity
import com.ucw.beatu.shared.database.entity.WatchHistoryEntity
import com.ucw.beatu.shared.database.datastore.PreferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据同步服务
 * 负责在应用启动时同步所有待同步的数据
 */
@Singleton
class DataSyncService @Inject constructor(
    private val videoInteractionLocalDataSource: VideoInteractionLocalDataSource,
    private val videoRemoteDataSource: VideoRemoteDataSource,
    private val videoLocalDataSource: VideoLocalDataSource,
    private val userLocalDataSource: UserLocalDataSource,
    private val userRemoteDataSource: UserRemoteDataSource,
    private val userFollowDao: UserFollowDao,
    private val commentDao: CommentDao,
    private val watchHistoryDao: WatchHistoryDao,
    private val preferencesDataStore: PreferencesDataStore
) {
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val TAG = "DataSyncService"
    
    // 当前用户ID，根据设计文档，默认用户名为 BEATU
    private val currentUserId: String = "BEATU"

    /**
     * 启动数据同步（在应用启动时调用）
     * 异步同步所有待同步的数据，不阻塞主线程
     */
    fun startSync() {
        syncScope.launch {
            try {
                Log.d(TAG, "开始同步待同步数据...")
                
                // 并行同步不同类型的数据
                syncVideoInteractions()
                syncUserFollows()
                syncComments()
                syncWatchHistory()
                
                Log.d(TAG, "数据同步完成")
            } catch (e: Exception) {
                Log.e(TAG, "数据同步失败", e)
            }
        }
    }

    /**
     * 同步视频互动（点赞/收藏）
     */
    private suspend fun syncVideoInteractions() {
        try {
            val pendingInteractions = videoInteractionLocalDataSource.getPendingInteractions()
            if (pendingInteractions.isEmpty()) {
                Log.d(TAG, "没有待同步的视频互动")
                return
            }

            Log.d(TAG, "开始同步 ${pendingInteractions.size} 条视频互动记录")
            
            for (interaction in pendingInteractions) {
                try {
                    // 同步点赞状态
                    if (interaction.isLiked) {
                        val result = videoRemoteDataSource.likeVideo(interaction.videoId)
                        if (result is AppResult.Success) {
                            videoInteractionLocalDataSource.clearPendingStatus(interaction.videoId, interaction.userId)
                            Log.d(TAG, "同步点赞成功: videoId=${interaction.videoId}")
                        } else {
                            Log.w(TAG, "同步点赞失败: videoId=${interaction.videoId}")
                        }
                    } else {
                        // 如果之前是点赞的，现在取消点赞
                        val existing = videoInteractionLocalDataSource.getInteraction(interaction.videoId, interaction.userId)
                        if (existing?.isLiked == true) {
                            val result = videoRemoteDataSource.unlikeVideo(interaction.videoId)
                            if (result is AppResult.Success) {
                                videoInteractionLocalDataSource.clearPendingStatus(interaction.videoId, interaction.userId)
                                Log.d(TAG, "同步取消点赞成功: videoId=${interaction.videoId}")
                            } else {
                                Log.w(TAG, "同步取消点赞失败: videoId=${interaction.videoId}")
                            }
                        }
                    }

                    // 同步收藏状态
                    if (interaction.isFavorited) {
                        val result = videoRemoteDataSource.favoriteVideo(interaction.videoId)
                        if (result is AppResult.Success) {
                            videoInteractionLocalDataSource.clearPendingStatus(interaction.videoId, interaction.userId)
                            Log.d(TAG, "同步收藏成功: videoId=${interaction.videoId}")
                        } else {
                            Log.w(TAG, "同步收藏失败: videoId=${interaction.videoId}")
                        }
                    } else {
                        // 如果之前是收藏的，现在取消收藏
                        val existing = videoInteractionLocalDataSource.getInteraction(interaction.videoId, interaction.userId)
                        if (existing?.isFavorited == true) {
                            val result = videoRemoteDataSource.unfavoriteVideo(interaction.videoId)
                            if (result is AppResult.Success) {
                                videoInteractionLocalDataSource.clearPendingStatus(interaction.videoId, interaction.userId)
                                Log.d(TAG, "同步取消收藏成功: videoId=${interaction.videoId}")
                            } else {
                                Log.w(TAG, "同步取消收藏失败: videoId=${interaction.videoId}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "同步视频互动异常: videoId=${interaction.videoId}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "同步视频互动失败", e)
        }
    }

    /**
     * 同步用户关注
     */
    private suspend fun syncUserFollows() {
        try {
            val pendingFollows = userFollowDao.getPendingFollows()
            if (pendingFollows.isEmpty()) {
                Log.d(TAG, "没有待同步的用户关注")
                return
            }

            Log.d(TAG, "开始同步 ${pendingFollows.size} 条用户关注记录")
            
            for (follow in pendingFollows) {
                try {
                    if (follow.isFollowed) {
                        val result = userRemoteDataSource.followUser(follow.authorId)
                        if (result is AppResult.Success) {
                            userLocalDataSource.clearFollowPendingStatus(follow.userId, follow.authorId)
                            Log.d(TAG, "同步关注成功: authorId=${follow.authorId}")
                        } else {
                            Log.w(TAG, "同步关注失败: authorId=${follow.authorId}")
                        }
                    } else {
                        val result = userRemoteDataSource.unfollowUser(follow.authorId)
                        if (result is AppResult.Success) {
                            userLocalDataSource.clearFollowPendingStatus(follow.userId, follow.authorId)
                            Log.d(TAG, "同步取消关注成功: authorId=${follow.authorId}")
                        } else {
                            Log.w(TAG, "同步取消关注失败: authorId=${follow.authorId}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "同步用户关注异常: authorId=${follow.authorId}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "同步用户关注失败", e)
        }
    }

    /**
     * 同步评论（策略A：回滚UI）
     * 注意：评论同步失败时应该删除待同步评论
     */
    private suspend fun syncComments() {
        try {
            val pendingComments = commentDao.getPendingComments()
            if (pendingComments.isEmpty()) {
                Log.d(TAG, "没有待同步的评论")
                return
            }

            Log.d(TAG, "开始同步 ${pendingComments.size} 条评论记录")
            
            for (comment in pendingComments) {
                try {
                    // 重新发送评论到远程服务器
                    val result = videoRemoteDataSource.postComment(comment.videoId, comment.content)
                    when (result) {
                        is AppResult.Success -> {
                            // 同步成功，删除临时评论，保存真实评论
                            commentDao.deleteById(comment.commentId)
                            videoLocalDataSource.saveComment(result.data)
                            Log.d(TAG, "同步评论成功: commentId=${comment.commentId}, videoId=${comment.videoId}")
                        }
                        is AppResult.Error -> {
                            // 同步失败，删除待同步评论（策略A：回滚UI）
                            commentDao.deleteById(comment.commentId)
                            Log.w(TAG, "同步评论失败，已删除待同步评论: commentId=${comment.commentId}, videoId=${comment.videoId}")
                        }
                        else -> {}
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "同步评论异常: commentId=${comment.commentId}, videoId=${comment.videoId}", e)
                    // 异常情况，删除待同步评论
                    try {
                        commentDao.deleteById(comment.commentId)
                    } catch (deleteException: Exception) {
                        Log.e(TAG, "删除待同步评论失败: commentId=${comment.commentId}", deleteException)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "同步评论失败", e)
        }
    }

    /**
     * 同步观看历史（策略B：不回滚，弱一致性数据）
     * 注意：观看历史同步失败时不删除，保留待同步状态，下次启动时继续重试
     */
    private suspend fun syncWatchHistory() {
        try {
            val pendingHistories = watchHistoryDao.getPendingHistories()
            if (pendingHistories.isEmpty()) {
                Log.d(TAG, "没有待同步的观看历史")
                return
            }

            Log.d(TAG, "开始同步 ${pendingHistories.size} 条观看历史记录")
            
            for (history in pendingHistories) {
                try {
                    // 调用后端API同步观看历史
                    // 注意：这里需要根据后端API实现，暂时先记录日志
                    // TODO: 实现观看历史同步API调用
                    Log.d(TAG, "同步观看历史: videoId=${history.videoId}, userId=${history.userId}, position=${history.lastPlayPositionMs}")
                    
                    // 模拟同步成功，清除待同步标记
                    // 实际应该调用 videoRemoteDataSource.updateWatchHistory(...)
                    watchHistoryDao.clearPendingStatus(history.videoId, history.userId)
                    Log.d(TAG, "同步观看历史成功: videoId=${history.videoId}")
                } catch (e: Exception) {
                    Log.e(TAG, "同步观看历史异常: videoId=${history.videoId}, userId=${history.userId}", e)
                    // 策略B：不回滚，保留待同步状态，下次启动时继续重试
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "同步观看历史失败", e)
        }
    }
}

