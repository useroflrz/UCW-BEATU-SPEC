package com.ucw.beatu.business.landscape.data.mapper

import com.ucw.beatu.business.landscape.domain.model.VideoItem
import com.ucw.beatu.business.landscape.domain.model.VideoOrientation
import com.ucw.beatu.business.videofeed.domain.model.Video

/**
 * 视频模型映射器
 * 负责将VideoFeed的Video模型转换为Landscape的VideoItem模型
 */

/**
 * VideoFeed Domain Model -> Landscape Domain Model
 */
fun Video.toLandscapeVideoItem(): VideoItem {
    return VideoItem(
        id = id,
        videoUrl = playUrl,
        title = title,
        authorName = authorName,
        likeCount = likeCount.toInt(),
        commentCount = commentCount.toInt(),
        favoriteCount = favoriteCount.toInt(),
        shareCount = shareCount.toInt(),
        isLiked = isLiked,
        isFavorited = isFavorited,
        defaultSpeed = 1.0f, // 默认倍速，可以从Settings读取
        defaultQuality = "自动", // 默认清晰度，可以从Settings读取
        orientation = when (orientation.lowercase()) {
            "portrait", "vertical" -> VideoOrientation.PORTRAIT
            "landscape", "horizontal" -> VideoOrientation.LANDSCAPE
            else -> VideoOrientation.LANDSCAPE // 默认横屏
        }
    )
}

