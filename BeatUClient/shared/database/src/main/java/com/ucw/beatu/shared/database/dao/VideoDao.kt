package com.ucw.beatu.shared.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ucw.beatu.shared.database.entity.VideoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos ORDER BY viewCount DESC LIMIT :limit")
    fun observeTopVideos(limit: Int): Flow<List<VideoEntity>>
    //todo 后续结合表修改
    @Query("""
    SELECT * FROM videos
    WHERE authorName = :authorName
    ORDER BY viewCount DESC
    LIMIT :limit
    """)
    fun observeVideosByAuthorName(authorName: String, limit: Int): Flow<List<VideoEntity>>
    @Query("SELECT * FROM videos ORDER BY viewCount DESC LIMIT :limit")
    fun observeFavoritedVideos(limit: Int): Flow<List<VideoEntity>>
    @Query("SELECT * FROM videos ORDER BY viewCount DESC LIMIT :limit")
    fun observeLikedVideos(limit: Int): Flow<List<VideoEntity>>
    @Query("SELECT * FROM videos ORDER BY viewCount DESC LIMIT :limit")
    fun observeHistoryVideos(limit: Int): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE id = :id LIMIT 1")
    suspend fun getVideoById(id: String): VideoEntity?

    @Query("SELECT * FROM videos WHERE id = :id LIMIT 1")
    fun observeVideoById(id: String): Flow<VideoEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<VideoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: VideoEntity)

    @Query("UPDATE videos SET coverUrl = :coverUrl WHERE id = :id")
    suspend fun updateCoverUrl(id: String, coverUrl: String)

    @Query("DELETE FROM videos")
    suspend fun clear()

    @Query("DELETE FROM videos WHERE id = :id")
    suspend fun deleteById(id: String)
}

