package com.ucw.beatu.shared.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ucw.beatu.shared.database.entity.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * 观看历史 DAO，本地缓存后端 beatu_watch_history
 */
@Dao
interface WatchHistoryDao {

    @Query("SELECT * FROM watch_history WHERE userId = :userId ORDER BY lastWatchAt DESC LIMIT :limit")
    fun observeHistory(userId: String, limit: Int): Flow<List<WatchHistoryEntity>>

    @Query("SELECT videoId FROM watch_history WHERE userId = :userId ORDER BY lastWatchAt DESC LIMIT :limit")
    fun observeHistoryVideoIds(userId: String, limit: Int): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(history: WatchHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<WatchHistoryEntity>)

    @Query("DELETE FROM watch_history WHERE userId = :userId AND videoId = :videoId")
    suspend fun delete(userId: String, videoId: String)

    @Query("DELETE FROM watch_history WHERE userId = :userId")
    suspend fun clearByUser(userId: String)

    @Query("DELETE FROM watch_history")
    suspend fun clearAll()
}


