package com.ucw.beatu.business.videofeed.domain.repository

import com.ucw.beatu.business.videofeed.domain.model.Comment
import com.ucw.beatu.business.videofeed.domain.model.Video
import com.ucw.beatu.shared.common.result.AppResult
import kotlinx.coroutines.flow.Flow

/**
 * 视频仓储接口
 * 定义业务层需要的数据访问接口
 */
interface VideoRepository {
    /**
     * 获取视频列表（分页）
     * @param page 页码，从1开始
     * @param limit 每页数量
     * @param orientation 视频方向（可选）："portrait"、"landscape"，null表示不筛选
     * @return Flow<AppResult<List<Video>>> 响应式数据流
     */
    fun getVideoFeed(page: Int = 1, limit: Int = 20, orientation: String? = null): Flow<AppResult<List<Video>>>

    /**
     * 获取视频详情
     * @param videoId 视频ID
     * @return AppResult<Video>
     */
    suspend fun getVideoDetail(videoId: Long): AppResult<Video>  // ✅ 修改：从 String 改为 Long

    /**
     * 获取评论列表（分页）
     * @param videoId 视频ID
     * @param page 页码，从1开始
     * @param limit 每页数量
     * @return Flow<AppResult<List<Comment>>> 响应式数据流
     */
    fun getComments(videoId: Long, page: Int = 1, limit: Int = 20): Flow<AppResult<List<Comment>>>  // ✅ 修改：从 String 改为 Long

    /**
     * 点赞视频
     */
    suspend fun likeVideo(videoId: Long): AppResult<Unit>  // ✅ 修改：从 String 改为 Long

    /**
     * 取消点赞
     */
    suspend fun unlikeVideo(videoId: Long): AppResult<Unit>  // ✅ 修改：从 String 改为 Long

    /**
     * 收藏视频
     */
    suspend fun favoriteVideo(videoId: Long): AppResult<Unit>  // ✅ 修改：从 String 改为 Long

    /**
     * 取消收藏
     */
    suspend fun unfavoriteVideo(videoId: Long): AppResult<Unit>  // ✅ 修改：从 String 改为 Long

    /**
     * 分享视频（统计）
     */
    suspend fun shareVideo(videoId: Long): AppResult<Unit>  // ✅ 修改：从 String 改为 Long

    /**
     * 发布评论
     */
    suspend fun postComment(videoId: Long, content: String): AppResult<Comment>  // ✅ 修改：从 String 改为 Long

    /**
     * 获取关注页视频列表（显示所有用户关注的创作者的视频）
     * @param page 页码，从1开始
     * @param limit 每页数量
     * @return Flow<AppResult<List<Video>>> 响应式数据流
     */
    fun getFollowFeed(page: Int = 1, limit: Int = 20): Flow<AppResult<List<Video>>>

    /**
     * 保存观看历史（异步写入数据库，上传远程）
     * @param videoId 视频ID
     * @param positionMs 播放位置（毫秒）
     */
    suspend fun saveWatchHistory(videoId: Long, positionMs: Long = 0L)
}

