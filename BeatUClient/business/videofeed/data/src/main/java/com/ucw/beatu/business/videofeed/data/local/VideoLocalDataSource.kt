package com.ucw.beatu.business.videofeed.data.local

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import com.ucw.beatu.business.videofeed.data.mapper.toDomain
import com.ucw.beatu.business.videofeed.data.mapper.toEntity
import com.ucw.beatu.business.videofeed.domain.model.Comment
import com.ucw.beatu.business.videofeed.domain.model.Video
import com.ucw.beatu.shared.database.BeatUDatabase
import com.ucw.beatu.shared.database.dao.CommentDao
import com.ucw.beatu.shared.database.dao.UserDao
import com.ucw.beatu.shared.database.dao.VideoDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import kotlin.collections.List

/**
 * 本地数据源接口
 */
interface VideoLocalDataSource {
    fun observeVideos(limit: Int = 50): Flow<List<Video>>
    suspend fun getVideoById(id: Long): Video?  // ✅ 修改：从 String 改为 Long
    fun observeVideoById(id: Long): Flow<Video?>  // ✅ 修改：从 String 改为 Long
    suspend fun saveVideos(videos: List<Video>)
    suspend fun saveVideo(video: Video)
    suspend fun clearVideos()

    /**
     * 后台生成缩略图任务入口：前台落库后，异步为无封面的视频生成本地首帧并更新 DB。
     */
    fun enqueueThumbnailGeneration(videos: List<Video>)

    fun observeComments(videoId: Long): Flow<List<Comment>>  // ✅ 修改：从 String 改为 Long
    suspend fun getCommentById(commentId: String): Comment?
    suspend fun saveComments(comments: List<Comment>)
    suspend fun saveComment(comment: Comment)
    suspend fun deleteCommentById(commentId: String)  // ✅ 新增：删除指定评论（用于回滚）
    suspend fun clearComments(videoId: Long)  // ✅ 修改：从 String 改为 Long
}

/**
 * 本地数据源实现
 * 负责从Room数据库读写数据；
 * 为避免首屏卡顿，封面生成通过后台协程异步完成。
 */
class VideoLocalDataSourceImpl @Inject constructor(
    private val database: BeatUDatabase,
    @ApplicationContext private val appContext: Context
) : VideoLocalDataSource {

    private val videoDao: VideoDao = database.videoDao()
    private val commentDao: CommentDao = database.commentDao()
    private val userDao: UserDao = database.userDao()
    private val thumbnailScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun observeVideos(limit: Int): Flow<List<Video>> {
        return videoDao.observeTopVideos(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getVideoById(id: Long): Video? {  // ✅ 修改：从 String 改为 Long
        return videoDao.getVideoById(id)?.toDomain()
    }

    override fun observeVideoById(id: Long): Flow<Video?> {  // ✅ 修改：从 String 改为 Long
        return videoDao.observeVideoById(id).map { entity ->  // ✅ 修改：VideoDao 已更新为 Long
            entity?.toDomain()
        }
    }

    override suspend fun saveVideos(videos: List<Video>) {
        // 前台仅做轻量落库，不阻塞在首帧生成上
        videoDao.insertAll(videos.map { it.toEntity() })
        enqueueThumbnailGeneration(videos);
    }

    override suspend fun saveVideo(video: Video) {
        videoDao.insert(video.toEntity())
        enqueueThumbnailGeneration(listOf(video))
    }

    override suspend fun clearVideos() {
        videoDao.clear()
    }

    override fun observeComments(videoId: Long): Flow<List<Comment>> {
        return commentDao.observeComments(videoId).map { entities -> // ✅ 修改：CommentDao 现在使用 Long
            // 批量查询所有作者信息，避免N+1查询
            val authorIds = entities.map { it.authorId }.distinct()
            val authors = authorIds.mapNotNull { authorId ->
                userDao.getUserById(authorId)
            }.associateBy { it.userId }
            
            entities.map { entity ->
                val author = authors[entity.authorId]
                entity.toDomain(
                    authorName = author?.userName ?: entity.authorId,
                    authorAvatar = author?.avatarUrl ?: entity.authorAvatar
                )
            }
        }
    }

    override suspend fun getCommentById(commentId: String): Comment? {
        val entity = commentDao.getCommentById(commentId) ?: return null
        val author = userDao.getUserById(entity.authorId)
        return entity.toDomain(
            authorName = author?.userName ?: entity.authorId,
            authorAvatar = author?.avatarUrl ?: entity.authorAvatar
        )
    }

    override suspend fun saveComments(comments: List<Comment>) {
        commentDao.insertAll(comments.map { it.toEntity() })
    }

    override suspend fun saveComment(comment: Comment) {
        commentDao.insert(comment.toEntity())
    }

    override suspend fun deleteCommentById(commentId: String) {
        commentDao.deleteById(commentId)
    }

    override suspend fun clearComments(videoId: Long) {
        commentDao.clear(videoId) // ✅ 修改：CommentDao 现在使用 Long
    }
    override fun enqueueThumbnailGeneration(videos: List<Video>) {
        // 后台懒生成：仅针对当前批次中 coverUrl 为空的条目生成缩略图并更新 DB
        if (videos.isEmpty()) return
        android.util.Log.d("VideoLocalDataSource", "Enqueueing thumbnail generation for ${videos.size} videos")
        thumbnailScope.launch {
            videos.forEach { video ->
                // 检查 coverUrl 是否为空或空白（包括空字符串、null等）
                val needsThumbnail = video.coverUrl.isBlank() || video.coverUrl.isEmpty()
                if (!needsThumbnail) {
                    android.util.Log.d("VideoLocalDataSource", "Skipping video ${video.id}: coverUrl already exists: ${video.coverUrl}")
                    return@forEach
                }
                android.util.Log.d("VideoLocalDataSource", "Generating thumbnail for video ${video.id}, playUrl: ${video.playUrl}, current coverUrl: '${video.coverUrl}'")
                val localPath = runCatching {
                    extractFirstFrameToFile(video.id, video.playUrl)
                }.getOrNull()
                if (!localPath.isNullOrBlank()) {
                    // 仅更新封面字段，避免重写整行
                    try {
                        videoDao.updateCoverUrl(video.id, localPath)
                        android.util.Log.d("VideoLocalDataSource", "Thumbnail generated and saved for video ${video.id}: $localPath")
                    } catch (e: Exception) {
                        android.util.Log.e("VideoLocalDataSource", "Failed to update coverUrl in database for video ${video.id}", e)
                    }
                } else {
                    android.util.Log.w("VideoLocalDataSource", "Failed to generate thumbnail for video ${video.id}, playUrl: ${video.playUrl}")
                }
            }
        }
    }

    /**
     * 使用 MediaMetadataRetriever 抽取首帧并保存到本地文件，返回文件绝对路径。
     */
    private fun extractFirstFrameToFile(videoId: Long, url: String): String? {  // ✅ 修改：从 String 改为 Long
        val retriever = MediaMetadataRetriever()
        return try {
            android.util.Log.d("VideoLocalDataSource", "Extracting frame from: $url")
            
            // 尝试设置数据源，支持多种方式
            try {
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    // 对于网络URL，尝试使用 setDataSource(url, headers)
                    // 某些 Android 版本可能不支持直接从网络URL提取，需要先下载
                    retriever.setDataSource(url, HashMap())
                    android.util.Log.d("VideoLocalDataSource", "Set data source from network URL: $url")
                } else {
                    // 本地文件路径
                    retriever.setDataSource(url)
                    android.util.Log.d("VideoLocalDataSource", "Set data source from local file: $url")
                }
            } catch (e: IllegalArgumentException) {
                android.util.Log.e("VideoLocalDataSource", "Failed to set data source for video $videoId: ${e.message}", e)
                return null
            } catch (e: IllegalStateException) {
                android.util.Log.e("VideoLocalDataSource", "Illegal state when setting data source for video $videoId: ${e.message}", e)
                return null
            }
            
            // 尝试多个时间点提取首帧，避免黑帧
            val timePoints = listOf( 500_000L, 1_000_000L) //  0.5s, 1s
            var frame: Bitmap? = null
            
            for (timeUs in timePoints) {
                try {
                    frame = retriever.getFrameAtTime(
                        timeUs,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    )
                    if (frame != null) {
                        android.util.Log.d("VideoLocalDataSource", "Successfully extracted frame at ${timeUs / 1000}ms for video $videoId")
                        break
                    }
                } catch (e: Exception) {
                    android.util.Log.w("VideoLocalDataSource", "Failed to extract frame at ${timeUs / 1000}ms for video $videoId: ${e.message}")
                }
            }
            
            if (frame == null) {
                android.util.Log.w("VideoLocalDataSource", "Failed to extract frame at any time point for video $videoId")
                return null
            }

            val dir = File(appContext.filesDir, "video_thumbnails").apply {
                if (!exists()) {
                    val created = mkdirs()
                    android.util.Log.d("VideoLocalDataSource", "Created thumbnail directory: $created, path: ${absolutePath}")
                }
            }
            val file = File(dir, "${videoId}.jpg")
            
            // 如果文件已存在，先删除
            if (file.exists()) {
                val deleted = file.delete()
                android.util.Log.d("VideoLocalDataSource", "Deleted existing thumbnail file: $deleted")
            }
            
            FileOutputStream(file).use { out ->
                val compressed = frame.compress(Bitmap.CompressFormat.JPEG, 80, out)
                if (!compressed) {
                    android.util.Log.e("VideoLocalDataSource", "Failed to compress bitmap for video $videoId")
                    return null
                }
                out.flush()
                android.util.Log.d("VideoLocalDataSource", "Compressed bitmap: $compressed, file size: ${file.length()} bytes")
            }
            
            // 验证文件是否成功创建
            if (!file.exists() || file.length() == 0L) {
                android.util.Log.e("VideoLocalDataSource", "Thumbnail file not created or empty for video $videoId")
                return null
            }
            
            android.util.Log.d("VideoLocalDataSource", "Thumbnail saved successfully: ${file.absolutePath}, size: ${file.length()} bytes")
            file.absolutePath
        } catch (e: Exception) {
            android.util.Log.e("VideoLocalDataSource", "Error extracting frame for video $videoId from $url", e)
            e.printStackTrace()
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                android.util.Log.e("VideoLocalDataSource", "Error releasing MediaMetadataRetriever", e)
            }
        }
    }
}

