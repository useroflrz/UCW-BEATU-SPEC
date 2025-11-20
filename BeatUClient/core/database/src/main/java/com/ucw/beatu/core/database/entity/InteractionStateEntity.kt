package com.ucw.beatu.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "interaction_state")
data class InteractionStateEntity(
    @PrimaryKey val videoId: String,
    val liked: Boolean,
    val favorited: Boolean,
    val followed: Boolean,
    val lastSeekMs: Long,
    val defaultSpeed: Float,
    val defaultQuality: String
)
