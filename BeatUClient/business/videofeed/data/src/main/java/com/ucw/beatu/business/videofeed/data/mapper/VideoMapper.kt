package com.ucw.beatu.business.videofeed.data.mapper

import com.ucw.beatu.business.videofeed.data.api.dto.VideoDto
import com.ucw.beatu.business.videofeed.domain.model.Video
import com.ucw.beatu.shared.database.entity.VideoEntity

/**
 * 视频数据映射器
 * 负责在不同数据层之间转换视频模型
 */

/**
 * DTO -> Domain Model
 */
fun VideoDto.toDomain(): Video {
    return Video(
        id = id,
        playUrl = playUrl,
        coverUrl = coverUrl,
        title = title,
        tags = tags ?: emptyList(),
        durationMs = durationMs,
        orientation = orientation,
        authorId = authorId,
        authorName = authorName,
        authorAvatar = authorAvatar,
        likeCount = likeCount,
        commentCount = commentCount,
        favoriteCount = favoriteCount,
        shareCount = shareCount,
        viewCount = viewCount,
        isLiked = isLiked,
        isFavorited = isFavorited,
        isFollowedAuthor = isFollowedAuthor,
        createdAt = createdAt,
        updatedAt = updatedAt,
        contentType = contentType,
        imageUrls = imageUrls ?: emptyList(),
        bgmUrl = bgmUrl
    )
}

/**
 * Entity -> Domain Model
 */
fun VideoEntity.toDomain(): Video {
    return Video(
        id = id,
        playUrl = playUrl,
        coverUrl = coverUrl,
        title = title,
        tags = tags,
        durationMs = durationMs,
        orientation = orientation,
        authorId = authorId,
        authorName = authorName,
        authorAvatar = null, // Entity中没有avatar字段
        likeCount = likeCount,
        commentCount = commentCount,
        favoriteCount = favoriteCount,
        shareCount = shareCount,
        viewCount = viewCount,
        isLiked = false,
        isFavorited = false,
        isFollowedAuthor = false,
        createdAt = null,
        updatedAt = null,
        // 本地缓存当前不存储图文扩展字段，保持默认值
        contentType = null,
        imageUrls = emptyList(),
        bgmUrl = null
    )
}

/**
 * Domain Model -> Entity
 */
fun Video.toEntity(): VideoEntity {
    return VideoEntity(
        id = id,
        playUrl = playUrl,
        coverUrl = coverUrl,
        title = title,
        tags = tags,
        durationMs = durationMs,
        orientation = orientation,
        authorId = authorId,
        authorName = authorName,
        likeCount = likeCount,
        commentCount = commentCount,
        favoriteCount = favoriteCount,
        shareCount = shareCount,
        viewCount = viewCount
    )
}

