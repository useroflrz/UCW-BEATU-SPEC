package com.ucw.beatu.shared.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ucw.beatu.shared.database.entity.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * 观看历史 DAO，对应后端 beatu_watch_history
 */
@Dao
interface WatchHistoryDao {

    @Query("SELECT * FROM beatu_watch_history WHERE userId = :userId ORDER BY watchedAt DESC LIMIT :limit")
    fun observeHistory(userId: String, limit: Int): Flow<List<WatchHistoryEntity>>

    @Query("SELECT videoId FROM beatu_watch_history WHERE userId = :userId ORDER BY watchedAt DESC LIMIT :limit")
    fun observeHistoryVideoIds(userId: String, limit: Int): Flow<List<Long>>

    /**
     * 根据视频ID和用户ID查询观看历史
     */
    @Query("SELECT * FROM beatu_watch_history WHERE userId = :userId AND videoId = :videoId LIMIT 1")
    suspend fun getHistory(userId: String, videoId: Long): WatchHistoryEntity?

    /**
     * 根据视频ID和用户ID查询观看历史（Flow）
     */
    @Query("SELECT * FROM beatu_watch_history WHERE userId = :userId AND videoId = :videoId LIMIT 1")
    fun observeHistory(userId: String, videoId: Long): Flow<WatchHistoryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(history: WatchHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<WatchHistoryEntity>)

    @Query("DELETE FROM beatu_watch_history WHERE userId = :userId AND videoId = :videoId")
    suspend fun delete(userId: String, videoId: Long)

    @Query("DELETE FROM beatu_watch_history WHERE userId = :userId")
    suspend fun clearByUser(userId: String)

    @Query("DELETE FROM beatu_watch_history")
    suspend fun clearAll()

    /**
     * 查询所有待同步的观看历史（用于数据同步服务）
     */
    @Query("SELECT * FROM beatu_watch_history WHERE isPending = 1")
    suspend fun getPendingHistories(): List<WatchHistoryEntity>

    /**
     * 清除待同步标记（用于同步成功后）
     */
    @Query("UPDATE beatu_watch_history SET isPending = 0 WHERE videoId = :videoId AND userId = :userId")
    suspend fun clearPendingStatus(videoId: Long, userId: String)
}


