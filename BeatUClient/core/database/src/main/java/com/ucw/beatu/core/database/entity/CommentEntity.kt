package com.ucw.beatu.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey val id: String,
    val videoId: String,
    val authorId: String,
    val authorName: String,
    val content: String,
    val createdAt: Long,
    val isAiReply: Boolean
)
