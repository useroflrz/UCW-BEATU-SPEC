package com.ucw.beatu.shared.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 本地观看历史聚合表，对应后端 beatu_watch_history
 */
@Entity(
    tableName = "watch_history",
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
        )
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["videoId"]),
        Index(value = ["userId", "videoId"], unique = true)
    ]
)
data class WatchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val videoId: String,
    val lastWatchAt: Long = System.currentTimeMillis(),
    val lastSeekMs: Long = 0L,
    val watchCount: Int = 1,
    val totalDurationMs: Long = 0L,
    val completionRate: Double = 0.0
)


