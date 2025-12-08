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
        id = videoId, // ✅ 修改：使用 videoId 字段
        playUrl = playUrl,
        coverUrl = coverUrl,
        title = title,
        tags = emptyList(), // ✅ 修改：新表结构中没有 tags 字段
        durationMs = durationMs,
        orientation = orientation,
        authorId = authorId,
        authorName = "", // ✅ 修改：新表结构中没有 authorName 字段，需要通过 authorId 查询 UserEntity
        authorAvatar = authorAvatar, // ✅ 修改：使用 authorAvatar 字段
        likeCount = likeCount,
        commentCount = commentCount,
        favoriteCount = favoriteCount,
        shareCount = 0, // ✅ 修改：新表结构中没有 shareCount 字段
        viewCount = viewCount,
        isLiked = false, // ✅ 修改：需要通过 VideoInteractionEntity 查询
        isFavorited = false, // ✅ 修改：需要通过 VideoInteractionEntity 查询
        isFollowedAuthor = false, // ✅ 修改：需要通过 UserFollowEntity 查询
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
        videoId = id, // ✅ 修改：使用 videoId 字段
        playUrl = playUrl,
        coverUrl = coverUrl,
        title = title,
        authorId = authorId,
        orientation = orientation,
        durationMs = durationMs,
        likeCount = likeCount,
        commentCount = commentCount,
        favoriteCount = favoriteCount,
        viewCount = viewCount,
        authorAvatar = authorAvatar, // ✅ 修改：使用 authorAvatar 字段
        shareUrl = null // ✅ 修改：新表结构中有 shareUrl 字段，但 Domain Model 中没有
    )
}

/**
 * DTO -> Entity (直接映射，用于快速保存到数据库)
 */
fun VideoDto.toEntity(): VideoEntity {
    return VideoEntity(
        videoId = id,
        playUrl = playUrl,
        coverUrl = coverUrl,
        title = title,
        authorId = authorId,
        orientation = orientation,
        durationMs = durationMs,
        likeCount = likeCount,
        commentCount = commentCount,
        favoriteCount = favoriteCount,
        viewCount = viewCount,
        authorAvatar = authorAvatar,
        shareUrl = null // VideoDto 中没有 shareUrl 字段
    )
}

