package com.ucw.beatu.shared.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 评论内容表，对应后端 beatu_comment
 * 用于缓存评论数据，支持评论弹窗显示
 */
@Entity(
    tableName = "beatu_comment",
    foreignKeys = [
        ForeignKey(
            entity = VideoEntity::class,
            parentColumns = ["videoId"],
            childColumns = ["videoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["videoId"]),
        Index(value = ["authorId"]),
        Index(value = ["createdAt"])
    ]
)
data class CommentEntity(
    @PrimaryKey val commentId: String,  // 评论 ID (PK)
    val videoId: Long,  // 所属视频 ID
    val authorId: String,  // 评论作者
    val content: String,  // 评论内容
    val createdAt: Long,  // 评论时间
    val likeCount: Long = 0,  // 点赞数
    val isLiked: Boolean = false,  // 是否点赞 (0/1)
    val isPending: Boolean = false,  // 本地待同步状态 (0/1)
    val authorAvatar: String? = null  // 作者头像
)

