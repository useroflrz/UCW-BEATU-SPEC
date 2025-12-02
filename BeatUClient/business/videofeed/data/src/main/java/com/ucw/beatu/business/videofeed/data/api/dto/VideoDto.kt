package com.ucw.beatu.business.videofeed.data.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 视频数据传输对象
 * 对应MySQL后端API返回的数据格式
 * 后端使用 alias_generator 将 snake_case 转换为 camelCase
 */
@JsonClass(generateAdapter = true)
data class VideoDto(
    val id: String,
    @Json(name = "playUrl") val playUrl: String,
    @Json(name = "coverUrl") val coverUrl: String,
    val title: String,
    val tags: List<String>? = null,
    @Json(name = "durationMs") val durationMs: Long,
    val orientation: String,
    @Json(name = "authorId") val authorId: String,
    @Json(name = "authorName") val authorName: String,
    @Json(name = "authorAvatar") val authorAvatar: String? = null,
    @Json(name = "likeCount") val likeCount: Long = 0,
    @Json(name = "commentCount") val commentCount: Long = 0,
    @Json(name = "favoriteCount") val favoriteCount: Long = 0,
    @Json(name = "shareCount") val shareCount: Long = 0,
    @Json(name = "viewCount") val viewCount: Long = 0,
    @Json(name = "isLiked") val isLiked: Boolean = false,
    @Json(name = "isFavorited") val isFavorited: Boolean = false,
    @Json(name = "isFollowedAuthor") val isFollowedAuthor: Boolean = false,
    @Json(name = "createdAt") val createdAt: Long? = null,
    @Json(name = "updatedAt") val updatedAt: Long? = null
    // 注意：qualities 字段在后端存在但客户端暂不使用，Moshi 会自动忽略未知字段
    ,
    // Feed 内容类型：VIDEO / IMAGE_POST
    @Json(name = "contentType") val contentType: String? = null,
    // 图文卡片专用字段
    @Json(name = "imageUrls") val imageUrls: List<String>? = null,
    @Json(name = "bgmUrl") val bgmUrl: String? = null
)

