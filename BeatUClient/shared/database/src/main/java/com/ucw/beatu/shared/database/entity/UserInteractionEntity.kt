package com.ucw.beatu.shared.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 本地用户互动表，对应后端 beatu_interactions
 *
 * 用于缓存：点赞 / 收藏 / 关注作者 等操作的本地状态，便于按钮状态回显、
 * “我的点赞 / 我的收藏” 等页面，以及与后端增量同步。
 */
@Entity(
    tableName = "user_interactions",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = VideoEntity::class,
            parentColumns = ["id"],
            childColumns = ["videoId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["authorId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["videoId"]),
        Index(value = ["authorId"]),
        Index(value = ["userId", "videoId", "type"], unique = true),
        Index(value = ["userId", "authorId", "type"], unique = true)
    ]
)
data class UserInteractionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val videoId: String? = null,
    val authorId: String? = null,
    val type: String, // "LIKE", "FAVORITE", "FOLLOW_AUTHOR" 等
    val createdAt: Long = System.currentTimeMillis()
)


