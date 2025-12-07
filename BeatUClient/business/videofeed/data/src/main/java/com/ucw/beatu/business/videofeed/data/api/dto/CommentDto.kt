package com.ucw.beatu.business.videofeed.data.api.dto

import com.squareup.moshi.JsonClass

/**
 * 评论数据传输对象
 */
@JsonClass(generateAdapter = true)
data class CommentDto(
    val id: String,  // 评论 ID 保持 String（因为后端是 Integer）
    val videoId: Long,  // ✅ 修改：从 String 改为 Long
    val authorId: String,
    val authorName: String,
    val authorAvatar: String? = null,
    val content: String,
    val createdAt: String, // ISO 8601 格式的时间字符串，如 "2024-01-01T12:00:00Z"
    val isAiReply: Boolean = false,
    val likeCount: Long = 0
)

