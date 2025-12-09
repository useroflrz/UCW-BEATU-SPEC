package com.ucw.beatu.shared.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 用户-视频互动表，对应后端 beatu_video_interaction
 * 用于缓存用户与视频的互动状态（点赞/收藏），支持乐观更新和同步
 */
@Entity(
    tableName = "beatu_video_interaction",
    primaryKeys = ["videoId", "userId"],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["videoId"]),
        Index(value = ["isPending"])
    ]
)
data class VideoInteractionEntity(
    val videoId: Long,  // 视频 ID (PK)
    val userId: String,  // 用户 ID (PK)
    val isLiked: Boolean = false,  // 是否点赞 (0/1)
    val isFavorited: Boolean = false,  // 是否收藏 (0/1)
    val isPending: Boolean = false  // 本地待同步状态 (0/1)
)

