package com.ucw.beatu.shared.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ucw.beatu.shared.database.entity.VideoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM beatu_video ORDER BY viewCount DESC LIMIT :limit")
    fun observeTopVideos(limit: Int): Flow<List<VideoEntity>>
    
    /**
     * 根据作者ID查询该用户的作品
     */
    @Query("""
    SELECT * FROM beatu_video
    WHERE authorId = :authorId
    ORDER BY viewCount DESC
    LIMIT :limit
    """)
    fun observeVideosByAuthorId(authorId: String, limit: Int): Flow<List<VideoEntity>>
    
    /**
     * 根据作者名称查询该用户的作品（JOIN beatu_user 表）
     * Room 要求明确指定所有字段，不能使用 v.*
     */
    @Query("""
    SELECT v.videoId, v.playUrl, v.coverUrl, v.title, v.authorId, v.orientation, 
           v.durationMs, v.likeCount, v.commentCount, v.favoriteCount, v.viewCount, 
           v.authorAvatar, v.shareUrl
    FROM beatu_video v
    INNER JOIN beatu_user u ON v.authorId = u.userId
    WHERE u.userName = :authorName
    ORDER BY v.viewCount DESC
    LIMIT :limit
    """)
    fun observeVideosByAuthorName(authorName: String, limit: Int): Flow<List<VideoEntity>>
    
    /**
     * 查询用户收藏的视频（JOIN beatu_video_interaction表）
     * Room 要求明确指定所有字段，不能使用 v.*
     */
    @Query("""
    SELECT v.videoId, v.playUrl, v.coverUrl, v.title, v.authorId, v.orientation, 
           v.durationMs, v.likeCount, v.commentCount, v.favoriteCount, v.viewCount, 
           v.authorAvatar, v.shareUrl
    FROM beatu_video v
    INNER JOIN beatu_video_interaction vi ON v.videoId = vi.videoId
    WHERE vi.userId = :userId AND vi.isFavorited = 1
    ORDER BY v.viewCount DESC
    LIMIT :limit
    """)
    fun observeFavoritedVideos(userId: String, limit: Int): Flow<List<VideoEntity>>
    
    /**
     * 查询用户点赞的视频（JOIN beatu_video_interaction表）
     * Room 要求明确指定所有字段，不能使用 v.*
     */
    @Query("""
    SELECT v.videoId, v.playUrl, v.coverUrl, v.title, v.authorId, v.orientation, 
           v.durationMs, v.likeCount, v.commentCount, v.favoriteCount, v.viewCount, 
           v.authorAvatar, v.shareUrl
    FROM beatu_video v
    INNER JOIN beatu_video_interaction vi ON v.videoId = vi.videoId
    WHERE vi.userId = :userId AND vi.isLiked = 1
    ORDER BY v.viewCount DESC
    LIMIT :limit
    """)
    fun observeLikedVideos(userId: String, limit: Int): Flow<List<VideoEntity>>
    
    /**
     * 查询用户观看历史（JOIN beatu_watch_history表）
     * Room 要求明确指定所有字段，不能使用 v.*
     * 按观看时间升序排列，最先观看的显示在前面
     * 
     * 注意：使用 INNER JOIN 意味着只有 watch_history 中的 videoId 在 beatu_video 表中存在时才会返回结果
     * 如果 watch_history 中有 videoId 但 beatu_video 中没有对应的视频，这些记录会被过滤掉
     */
    @Query("""
    SELECT DISTINCT v.videoId, v.playUrl, v.coverUrl, v.title, v.authorId, v.orientation, 
           v.durationMs, v.likeCount, v.commentCount, v.favoriteCount, v.viewCount, 
           v.authorAvatar, v.shareUrl
    FROM beatu_video v
    INNER JOIN beatu_watch_history wh ON v.videoId = wh.videoId
    WHERE wh.userId = :userId
    ORDER BY wh.watchedAt ASC
    LIMIT :limit
    """)
    fun observeHistoryVideos(userId: String, limit: Int): Flow<List<VideoEntity>>
    
    /**
     * 查询用户观看历史的 videoId 列表（用于调试，检查有多少条历史记录）
     * 这个查询不依赖 beatu_video 表，直接查询 watch_history 表
     */
    @Query("""
    SELECT wh.videoId
    FROM beatu_watch_history wh
    WHERE wh.userId = :userId
    ORDER BY wh.watchedAt ASC
    """)
    suspend fun getHistoryVideoIds(userId: String): List<Long>
    
    /**
     * 统计用户观看历史数量（用于调试）
     */
    @Query("""
    SELECT COUNT(*)
    FROM beatu_watch_history
    WHERE userId = :userId
    """)
    suspend fun countHistoryVideos(userId: String): Int

    @Query("SELECT * FROM beatu_video WHERE videoId = :videoId LIMIT 1")
    suspend fun getVideoById(videoId: Long): VideoEntity?

    @Query("SELECT * FROM beatu_video WHERE videoId = :videoId LIMIT 1")
    fun observeVideoById(videoId: Long): Flow<VideoEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<VideoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: VideoEntity)

    @Query("UPDATE beatu_video SET coverUrl = :coverUrl WHERE videoId = :videoId")
    suspend fun updateCoverUrl(videoId: Long, coverUrl: String)

    @Query("DELETE FROM beatu_video")
    suspend fun clear()

    @Query("DELETE FROM beatu_video WHERE videoId = :videoId")
    suspend fun deleteById(videoId: Long)

    /**
     * 根据关键词搜索视频（标题匹配）
     * 使用LIKE进行模糊匹配
     */
    @Query("""
    SELECT * FROM beatu_video
    WHERE title LIKE '%' || :query || '%'
    ORDER BY viewCount DESC
    """)
    fun observeSearchResults(query: String): Flow<List<VideoEntity>>
    
    /**
     * 计算某个用户所有视频的点赞数总和
     * 用于个人主页显示获赞数
     */
    @Query("""
    SELECT COALESCE(SUM(likeCount), 0) FROM beatu_video
    WHERE authorId = :authorId
    """)
    fun observeTotalLikesCountByAuthorId(authorId: String): Flow<Long>
}

