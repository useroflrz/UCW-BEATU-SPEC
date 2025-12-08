package com.ucw.beatu.shared.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 观看历史表，对应后端 beatu_watch_history
 * 用于记录用户观看视频的历史，支持"从上次播放继续"功能
 */
@Entity(
    tableName = "beatu_watch_history",
    primaryKeys = ["videoId", "userId"],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = VideoEntity::class,
            parentColumns = ["videoId"],
            childColumns = ["videoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["videoId"]),
        Index(value = ["userId", "watchedAt"]),
        Index(value = ["isPending"])  // ✅ 新增：用于查询待同步的观看历史
    ]
)
data class WatchHistoryEntity(
    val videoId: Long,  // 视频 ID (PK)
    val userId: String,  // 用户 ID (PK)
    val lastPlayPositionMs: Long = 0L,  // 上次播放进度（用于"从上次播放继续"）
    val watchedAt: Long = System.currentTimeMillis(),  // 最后观看时间（排序用）
    val isPending: Boolean = false  // ✅ 新增：本地待同步状态（弱一致性数据，策略B：不回滚，自动重试）
)


