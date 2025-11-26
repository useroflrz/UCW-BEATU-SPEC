package com.ucw.beatu.business.landscape.domain.model

data class VideoItem(
    val id: String,
    val videoUrl: String,
    val title: String,
    val authorName: String,
    val likeCount: Int,
    val commentCount: Int,
    val favoriteCount: Int,
    val shareCount: Int,
    val isLiked: Boolean = false,
    val isFavorited: Boolean = false,
    val defaultSpeed: Float = 1.0f,
    val defaultQuality: String = "自动",
    val orientation: VideoOrientation = VideoOrientation.LANDSCAPE
)

enum class VideoOrientation {
    PORTRAIT,
    LANDSCAPE
}