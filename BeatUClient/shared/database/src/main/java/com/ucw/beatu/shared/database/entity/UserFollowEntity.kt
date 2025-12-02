package com.ucw.beatu.shared.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 用户关注关系表（本地缓存后端 beatu_user_follows）
 */
@Entity(
    tableName = "user_follows",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["followerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["followeeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["followerId"]),
        Index(value = ["followeeId"]),
        Index(value = ["followerId", "followeeId"], unique = true)
    ]
)
data class UserFollowEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val followerId: String,
    val followeeId: String,
    val createdAt: Long = System.currentTimeMillis()
)


