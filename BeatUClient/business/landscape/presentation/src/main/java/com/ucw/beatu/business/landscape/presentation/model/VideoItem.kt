package com.ucw.beatu.business.landscape.presentation.model

import android.os.Parcelable
import com.ucw.beatu.business.landscape.domain.model.VideoItem as DomainVideoItem
import com.ucw.beatu.business.landscape.domain.model.VideoOrientation
import kotlinx.parcelize.Parcelize

/**
 * Presentation 层使用的 Parcelable VideoItem。
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
    val isLiked: Boolean = false,
    val isFavorited: Boolean = false,
    val defaultSpeed: Float = 1.0f,
    val defaultQuality: String = "自动",
    val orientation: VideoOrientation = VideoOrientation.LANDSCAPE
) : Parcelable

fun DomainVideoItem.toPresentationModel(): VideoItem = VideoItem(
    id = id,
    videoUrl = videoUrl,
    title = title,
    authorName = authorName,
    likeCount = likeCount,
    commentCount = commentCount,
    favoriteCount = favoriteCount,
    shareCount = shareCount,
    isLiked = isLiked,
    isFavorited = isFavorited,
    defaultSpeed = defaultSpeed,
    defaultQuality = defaultQuality,
    orientation = orientation
)

fun VideoItem.toDomainModel(): DomainVideoItem = DomainVideoItem(
    id = id,
    videoUrl = videoUrl,
    title = title,
    authorName = authorName,
    likeCount = likeCount,
    commentCount = commentCount,
    favoriteCount = favoriteCount,
    shareCount = shareCount,
    isLiked = isLiked,
    isFavorited = isFavorited,
    defaultSpeed = defaultSpeed,
    defaultQuality = defaultQuality,
    orientation = orientation
)
