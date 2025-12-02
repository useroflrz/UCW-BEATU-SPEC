package com.ucw.beatu.shared.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey val id: String,
    val playUrl: String,
    val coverUrl: String,
    val title: String,
    val tags: List<String>,
    val durationMs: Long,
    val orientation: String,
    val authorId: String,
    val authorName: String,
    val likeCount: Long,
    val commentCount: Long,
    val favoriteCount: Long,
    val shareCount: Long,
    val viewCount: Long
)

