package com.ucw.beatu.shared.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 用户-用户关注表，对应后端 beatu_user_follow
 * 用于缓存用户与用户的关注状态，支持乐观更新和同步
 */
@Entity(
    tableName = "beatu_user_follow",
    primaryKeys = ["userId", "authorId"],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["authorId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["authorId"]),
        Index(value = ["isPending"])
    ]
)
data class UserFollowEntity(
    val userId: String,  // 当前用户 ID (PK)
    val authorId: String,  // 被关注的作者 ID (PK)
    val isFollowed: Boolean = false,  // 是否关注 (0/1)
    val isPending: Boolean = false  // 本地待同步状态 (0/1)
)


