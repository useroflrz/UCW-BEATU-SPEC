package com.ucw.beatu.business.search.presentation.mapper

import com.ucw.beatu.business.videofeed.domain.model.Video
import com.ucw.beatu.shared.common.model.FeedContentType
import com.ucw.beatu.shared.common.model.VideoItem
import com.ucw.beatu.shared.common.model.VideoOrientation

/**
 * Search 模块内聚的视频映射，避免依赖 videofeed:presentation 的 mapper。
 */
fun Video.toSearchVideoItem(): VideoItem {
    return VideoItem(
        id = id,
        videoUrl = playUrl,
        coverUrl = coverUrl,
        title = title,
        authorName = authorName,
        authorAvatar = authorAvatar,
        authorId = authorId,
        likeCount = likeCount.toInt(),
        commentCount = commentCount.toInt(),
        favoriteCount = favoriteCount.toInt(),
        shareCount = shareCount.toInt(),
        isLiked = isLiked,
        isFavorited = isFavorited,
        orientation = when (orientation.lowercase()) {
            "portrait", "vertical" -> VideoOrientation.PORTRAIT
            "landscape", "horizontal" -> VideoOrientation.LANDSCAPE
            else -> VideoOrientation.PORTRAIT
        },
        type = when (contentType?.uppercase()) {
            "IMAGE_POST" -> FeedContentType.IMAGE_POST
            else -> if (imageUrls.isNotEmpty()) FeedContentType.IMAGE_POST else FeedContentType.VIDEO
        },
        imageUrls = imageUrls,
        bgmUrl = bgmUrl
    )
}
