package com.ucw.beatu.business.landscape.presentation.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 视频项数据模型（硬编码测试数据）
 */
@Parcelize
data class VideoItem(
    val id: String,
    val videoUrl: String,
    val title: String,
    val authorName: String,
    val likeCount: Int,
    val commentCount: Int,
    val favoriteCount: Int,
    val shareCount: Int,
    val orientation: VideoOrientation = VideoOrientation.LANDSCAPE,
    val defaultSpeed: Float = 1.0f,
    val defaultQuality: String = "自动",
    val isLiked: Boolean = false,
    val isFavorited: Boolean = false
) : Parcelable

enum class VideoOrientation {
    PORTRAIT,
    LANDSCAPE
}

