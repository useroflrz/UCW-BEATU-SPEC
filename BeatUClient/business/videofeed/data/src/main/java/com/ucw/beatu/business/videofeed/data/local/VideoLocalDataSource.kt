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

/**
 * 本地数据源接口
 */
interface VideoLocalDataSource {
    fun observeVideos(limit: Int = 50): Flow<List<Video>>
    suspend fun getVideoById(id: String): Video?
    fun observeVideoById(id: String): Flow<Video?>
    suspend fun saveVideos(videos: List<Video>)
    suspend fun saveVideo(video: Video)
    suspend fun clearVideos()

    /**
     * 后台生成缩略图任务入口：前台落库后，异步为无封面的视频生成本地首帧并更新 DB。
     */
    fun enqueueThumbnailGeneration(videos: List<Video>)

    fun observeComments(videoId: String): Flow<List<Comment>>
    suspend fun getCommentById(commentId: String): Comment?
    suspend fun saveComments(comments: List<Comment>)
    suspend fun saveComment(comment: Comment)
    suspend fun clearComments(videoId: String)
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
    private val thumbnailScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun observeVideos(limit: Int): Flow<List<Video>> {
        return videoDao.observeTopVideos(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getVideoById(id: String): Video? {
        return videoDao.getVideoById(id)?.toDomain()
    }

    override fun observeVideoById(id: String): Flow<Video?> {
        return videoDao.observeVideoById(id).map { entity ->
            entity?.toDomain()
        }
    }

    override suspend fun saveVideos(videos: List<Video>) {
        // 前台仅做轻量落库，不阻塞在首帧生成上
        videoDao.insertAll(videos.map { it.toEntity() })
    }

    override suspend fun saveVideo(video: Video) {
        videoDao.insert(video.toEntity())
    }

    override suspend fun clearVideos() {
        videoDao.clear()
    }

    override fun observeComments(videoId: String): Flow<List<Comment>> {
        return commentDao.observeComments(videoId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getCommentById(commentId: String): Comment? {
        return commentDao.getCommentById(commentId)?.toDomain()
    }

    override suspend fun saveComments(comments: List<Comment>) {
        commentDao.insertAll(comments.map { it.toEntity() })
    }

    override suspend fun saveComment(comment: Comment) {
        commentDao.insert(comment.toEntity())
    }

    override suspend fun clearComments(videoId: String) {
        commentDao.clear(videoId)
    }
    override fun enqueueThumbnailGeneration(videos: List<Video>) {
        // 后台懒生成：仅针对当前批次中 coverUrl 为空的条目生成缩略图并更新 DB
        if (videos.isEmpty()) return
        thumbnailScope.launch {
            videos.forEach { video ->
                if (video.coverUrl.isNotBlank()) return@forEach
                val localPath = runCatching {
                    extractFirstFrameToFile(video.id, video.playUrl)
                }.getOrNull()
                if (!localPath.isNullOrBlank()) {
                    // 仅更新封面字段，避免重写整行
                    videoDao.updateCoverUrl(video.id, localPath)
                }
            }
        }
    }

    /**
     * 使用 MediaMetadataRetriever 抽取首帧并保存到本地文件，返回文件绝对路径。
     */
    private fun extractFirstFrameToFile(videoId: String, url: String): String? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(url, HashMap())
            val frame: Bitmap = retriever.getFrameAtTime(
                100_000L, // 取第 ~0.1s 的关键帧，避免黑帧
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            ) ?: return null

            val dir = File(appContext.filesDir, "video_thumbnails").apply {
                if (!exists()) mkdirs()
            }
            val file = File(dir, "${videoId}.jpg")
            FileOutputStream(file).use { out ->
                frame.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }
}

