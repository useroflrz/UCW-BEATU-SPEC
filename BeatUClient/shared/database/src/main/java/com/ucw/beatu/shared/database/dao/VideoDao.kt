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
    
    /**
     * 根据作者ID查询该用户的作品（符合数据库表逻辑，使用authorId而非authorName）
     */
    @Query("""
    SELECT * FROM videos
    WHERE authorId = :authorId
    ORDER BY viewCount DESC
    LIMIT :limit
    """)
    fun observeVideosByAuthorId(authorId: String, limit: Int): Flow<List<VideoEntity>>
    
    /**
     * 根据作者名称查询该用户的作品（兼容旧接口，但建议使用authorId）
     */
    @Query("""
    SELECT * FROM videos
    WHERE authorName = :authorName
    ORDER BY viewCount DESC
    LIMIT :limit
    """)
    fun observeVideosByAuthorName(authorName: String, limit: Int): Flow<List<VideoEntity>>
    
    /**
     * 查询用户收藏的视频（JOIN user_interactions表，符合数据库表逻辑）
     */
    @Query("""
    SELECT v.* FROM videos v
    INNER JOIN user_interactions ui ON v.id = ui.videoId
    WHERE ui.userId = :userId AND ui.type = 'FAVORITE'
    ORDER BY ui.createdAt DESC
    LIMIT :limit
    """)
    fun observeFavoritedVideos(userId: String, limit: Int): Flow<List<VideoEntity>>
    
    /**
     * 查询用户点赞的视频（JOIN user_interactions表，符合数据库表逻辑）
     */
    @Query("""
    SELECT v.* FROM videos v
    INNER JOIN user_interactions ui ON v.id = ui.videoId
    WHERE ui.userId = :userId AND ui.type = 'LIKE'
    ORDER BY ui.createdAt DESC
    LIMIT :limit
    """)
    fun observeLikedVideos(userId: String, limit: Int): Flow<List<VideoEntity>>
    
    /**
     * 查询用户观看历史（JOIN watch_history表，符合数据库表逻辑）
     */
    @Query("""
    SELECT v.* FROM videos v
    INNER JOIN watch_history wh ON v.id = wh.videoId
    WHERE wh.userId = :userId
    ORDER BY wh.lastWatchAt DESC
    LIMIT :limit
    """)
    fun observeHistoryVideos(userId: String, limit: Int): Flow<List<VideoEntity>>

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

