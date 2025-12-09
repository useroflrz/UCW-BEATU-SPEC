package com.ucw.beatu.shared.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ucw.beatu.shared.database.entity.VideoInteractionEntity
import kotlinx.coroutines.flow.Flow

/**
 * 用户-视频互动数据访问对象
 * 用于管理点赞、收藏等互动状态
 */
@Dao
interface VideoInteractionDao {
    /**
     * 根据视频ID和用户ID查询互动状态
     */
    @Query("SELECT * FROM beatu_video_interaction WHERE videoId = :videoId AND userId = :userId LIMIT 1")
    suspend fun getInteraction(videoId: Long, userId: String): VideoInteractionEntity?

    /**
     * 根据视频ID和用户ID查询互动状态（Flow）
     */
    @Query("SELECT * FROM beatu_video_interaction WHERE videoId = :videoId AND userId = :userId LIMIT 1")
    fun observeInteraction(videoId: Long, userId: String): Flow<VideoInteractionEntity?>

    /**
     * 查询所有待同步的互动记录
     */
    @Query("SELECT * FROM beatu_video_interaction WHERE isPending = 1")
    suspend fun getPendingInteractions(): List<VideoInteractionEntity>

    /**
     * 插入或更新互动状态
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(interaction: VideoInteractionEntity)

    /**
     * 批量插入或更新互动状态
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(interactions: List<VideoInteractionEntity>)

    /**
     * 查询指定用户的所有互动记录
     */
    @Query("SELECT * FROM beatu_video_interaction WHERE userId = :userId")
    suspend fun getInteractionsByUser(userId: String): List<VideoInteractionEntity>

    /**
     * 删除互动记录
     */
    @Query("DELETE FROM beatu_video_interaction WHERE videoId = :videoId AND userId = :userId")
    suspend fun delete(videoId: Long, userId: String)

    /**
     * 清空所有互动数据
     */
    @Query("DELETE FROM beatu_video_interaction")
    suspend fun clear()

    /**
     * 查询所有视频交互（用于调试）
     */
    @Query("SELECT * FROM beatu_video_interaction")
    suspend fun getAll(): List<VideoInteractionEntity>
}

