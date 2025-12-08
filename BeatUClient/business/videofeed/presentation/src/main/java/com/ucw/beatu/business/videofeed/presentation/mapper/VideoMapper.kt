package com.ucw.beatu.business.videofeed.presentation.mapper

import com.ucw.beatu.business.videofeed.domain.model.Video
import com.ucw.beatu.shared.common.model.FeedContentType
import com.ucw.beatu.shared.common.model.VideoItem
import com.ucw.beatu.shared.common.model.VideoOrientation

/**
 * 视频模型映射器
 * 负责将Domain层的Video模型转换为Presentation层的VideoItem模型
 */

/**
 * Domain Model -> Presentation Model
 */
fun Video.toVideoItem(): VideoItem {
    return VideoItem(
        id = id,
        videoUrl = playUrl,
        coverUrl = coverUrl, // 传递视频封面URL
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
        // 根据后端的 contentType 和 imageUrls/bgmUrl 决定是视频还是图文卡片
        type = when (contentType?.uppercase()) {
            "IMAGE_POST" -> FeedContentType.IMAGE_POST
            else -> if (imageUrls.isNotEmpty()) FeedContentType.IMAGE_POST else FeedContentType.VIDEO
        },
        imageUrls = imageUrls,
        bgmUrl = bgmUrl
    )
}

