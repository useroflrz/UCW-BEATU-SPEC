package com.ucw.beatu.business.videofeed.data.local

import com.ucw.beatu.shared.database.BeatUDatabase
import com.ucw.beatu.shared.database.dao.VideoInteractionDao
import com.ucw.beatu.shared.database.entity.VideoInteractionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 视频互动本地数据源
 * 负责管理用户与视频的互动状态（点赞/收藏）
 */
interface VideoInteractionLocalDataSource {
    /**
     * 观察互动状态
     */
    fun observeInteraction(videoId: Long, userId: String): Flow<VideoInteractionEntity?>

    /**
     * 获取互动状态
     */
    suspend fun getInteraction(videoId: Long, userId: String): VideoInteractionEntity?

    /**
     * 更新点赞状态（乐观更新）
     */
    suspend fun updateLikeStatus(videoId: Long, userId: String, isLiked: Boolean, isPending: Boolean = true)

    /**
     * 更新收藏状态（乐观更新）
     */
    suspend fun updateFavoriteStatus(videoId: Long, userId: String, isFavorited: Boolean, isPending: Boolean = true)

    /**
     * 清除待同步标记
     */
    suspend fun clearPendingStatus(videoId: Long, userId: String)

    /**
     * 删除互动记录（用于回滚）
     */
    suspend fun deleteInteraction(videoId: Long, userId: String)

    /**
     * 获取所有待同步的互动记录
     */
    suspend fun getPendingInteractions(): List<VideoInteractionEntity>

    /**
     * 批量保存视频交互数据（首次启动时全量加载）
     */
    suspend fun saveInteractions(interactions: List<VideoInteractionEntity>)
}

/**
 * 视频互动本地数据源实现
 */
class VideoInteractionLocalDataSourceImpl @Inject constructor(
    private val database: BeatUDatabase
) : VideoInteractionLocalDataSource {

    private val interactionDao: VideoInteractionDao = database.videoInteractionDao()

    override fun observeInteraction(videoId: Long, userId: String): Flow<VideoInteractionEntity?> {
        return interactionDao.observeInteraction(videoId, userId)
    }

    override suspend fun getInteraction(videoId: Long, userId: String): VideoInteractionEntity? {
        return interactionDao.getInteraction(videoId, userId)
    }

    override suspend fun updateLikeStatus(videoId: Long, userId: String, isLiked: Boolean, isPending: Boolean) {
        val existing = interactionDao.getInteraction(videoId, userId)
        val entity = existing?.copy(
            isLiked = isLiked,
            isPending = isPending
        ) ?: VideoInteractionEntity(
            videoId = videoId,
            userId = userId,
            isLiked = isLiked,
            isFavorited = false,
            isPending = isPending
        )
        interactionDao.insertOrUpdate(entity)
    }

    override suspend fun updateFavoriteStatus(videoId: Long, userId: String, isFavorited: Boolean, isPending: Boolean) {
        val existing = interactionDao.getInteraction(videoId, userId)
        val entity = existing?.copy(
            isFavorited = isFavorited,
            isPending = isPending
        ) ?: VideoInteractionEntity(
            videoId = videoId,
            userId = userId,
            isLiked = false,
            isFavorited = isFavorited,
            isPending = isPending
        )
        interactionDao.insertOrUpdate(entity)
    }

    override suspend fun clearPendingStatus(videoId: Long, userId: String) {
        val existing = interactionDao.getInteraction(videoId, userId)
        if (existing != null) {
            interactionDao.insertOrUpdate(existing.copy(isPending = false))
        }
    }

    override suspend fun deleteInteraction(videoId: Long, userId: String) {
        interactionDao.delete(videoId, userId)
    }

    override suspend fun getPendingInteractions(): List<VideoInteractionEntity> {
        return interactionDao.getPendingInteractions()
    }

    override suspend fun saveInteractions(interactions: List<VideoInteractionEntity>) {
        interactionDao.insertOrUpdateAll(interactions)
    }
}

