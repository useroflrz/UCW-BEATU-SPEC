package com.ucw.beatu.business.videofeed.data.api

import com.ucw.beatu.business.videofeed.data.api.dto.CommentDto
import com.ucw.beatu.business.videofeed.data.api.dto.CommentRequest
import com.ucw.beatu.business.videofeed.data.api.dto.VideoDto
import com.ucw.beatu.business.videofeed.data.api.dto.WatchHistorySyncRequest
import com.ucw.beatu.shared.common.api.ApiResponse
import com.ucw.beatu.shared.common.api.PageResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 视频流API服务接口
 * 连接MySQL后端服务的API端点定义
 */
interface VideoFeedApiService {

    /**
     * 获取视频列表（分页）
     * GET /api/videos?page=1&limit=20
     */
    @GET("api/videos")
    suspend fun getVideoFeed(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("orientation") orientation: String? = null
    ): ApiResponse<PageResponse<VideoDto>>

    /**
     * 获取视频详情
     * GET /api/videos/{id}
     * 注意：后端期望的是 int (Long) 类型，但 Retrofit Path 参数必须是 String，在调用时会自动转换
     */
    @GET("api/videos/{id}")
    suspend fun getVideoDetail(@Path("id") videoId: String): ApiResponse<VideoDto>

    /**
     * 获取视频评论列表（分页）
     * GET /api/videos/{id}/comments?page=1&limit=20
     * 注意：后端期望的是 int (Long) 类型，但 Retrofit Path 参数必须是 String
     */
    @GET("api/videos/{id}/comments")
    suspend fun getComments(
        @Path("id") videoId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): ApiResponse<PageResponse<CommentDto>>

    /**
     * 点赞视频
     * POST /api/videos/{id}/like
     * 注意：后端期望的是 int (Long) 类型，但 Retrofit Path 参数必须是 String
     */
    @POST("api/videos/{id}/like")
    suspend fun likeVideo(@Path("id") videoId: String): ApiResponse<Any?>

    /**
     * 取消点赞
     * POST /api/videos/{id}/unlike
     * 注意：后端期望的是 int (Long) 类型，但 Retrofit Path 参数必须是 String
     */
    @POST("api/videos/{id}/unlike")
    suspend fun unlikeVideo(@Path("id") videoId: String): ApiResponse<Any?>

    /**
     * 收藏视频
     * POST /api/videos/{id}/favorite
     * 注意：后端期望的是 int (Long) 类型，但 Retrofit Path 参数必须是 String
     */
    @POST("api/videos/{id}/favorite")
    suspend fun favoriteVideo(@Path("id") videoId: String): ApiResponse<Any?>

    /**
     * 取消收藏
     * POST /api/videos/{id}/unfavorite
     * 注意：后端期望的是 int (Long) 类型，但 Retrofit Path 参数必须是 String
     */
    @POST("api/videos/{id}/unfavorite")
    suspend fun unfavoriteVideo(@Path("id") videoId: String): ApiResponse<Any?>

    /**
     * 分享视频（统计用）
     * POST /api/videos/{id}/share
     * 注意：后端期望的是 int (Long) 类型，但 Retrofit Path 参数必须是 String
     */
    @POST("api/videos/{id}/share")
    suspend fun shareVideo(@Path("id") videoId: String): ApiResponse<Any?>

    /**
     * 发布评论
     * POST /api/videos/{id}/comments
     * 注意：后端期望的是 int (Long) 类型，但 Retrofit Path 参数必须是 String
     */
    @POST("api/videos/{id}/comments")
    suspend fun postComment(
        @Path("id") videoId: String,
        @Body request: CommentRequest
    ): ApiResponse<CommentDto>

    /**
     * 搜索视频
     * GET /api/search/videos?query=xxx&page=1&limit=20
     */
    @GET("api/search/videos")
    suspend fun searchVideos(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): ApiResponse<PageResponse<VideoDto>>

    /**
     * 获取指定用户的所有视频交互（首次启动时全量加载）
     * GET /api/videos/interactions
     */
    @GET("api/videos/interactions")
    suspend fun getAllVideoInteractions(): ApiResponse<List<Map<String, Any>>>
    
    /**
     * 获取指定用户的所有观看历史（启动时全量加载）
     * GET /api/videos/watch-history
     */
    @GET("api/videos/watch-history")
    suspend fun getAllWatchHistories(): ApiResponse<List<Map<String, Any>>>
    
    /**
     * 同步观看历史（定时批量提交）
     * POST /api/videos/watch-history/sync
     */
    @POST("api/videos/watch-history/sync")
    suspend fun syncWatchHistories(@Body histories: List<WatchHistorySyncRequest>): ApiResponse<Map<String, Any>>
}

