package com.ucw.beatu.business.videofeed.presentation.mapper

import com.ucw.beatu.business.videofeed.domain.model.Video
import com.ucw.beatu.business.videofeed.presentation.model.FeedContentType
import com.ucw.beatu.business.videofeed.presentation.model.VideoItem
import com.ucw.beatu.business.videofeed.presentation.model.VideoOrientation

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
        title = title,
        authorName = authorName,
        likeCount = likeCount.toInt(),
        commentCount = commentCount.toInt(),
        favoriteCount = favoriteCount.toInt(),
        shareCount = shareCount.toInt(),
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

