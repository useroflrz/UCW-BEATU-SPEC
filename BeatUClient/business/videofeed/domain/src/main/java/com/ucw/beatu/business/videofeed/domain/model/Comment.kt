package com.ucw.beatu.business.videofeed.domain.model

/**
 * 评论领域模型
 */
data class Comment(
    val id: String,  // 评论 ID 保持 String（因为后端是 Integer）
    val videoId: Long,  // ✅ 修改：从 String 改为 Long
    val authorId: String,
    val authorName: String,
    val authorAvatar: String? = null,
    val content: String,
    val createdAt: Long,
    val isAiReply: Boolean,
    val likeCount: Long = 0
)

