package com.ucw.beatu.business.user.data.mapper

import com.ucw.beatu.business.user.domain.model.UserWork
import com.ucw.beatu.shared.database.entity.VideoEntity

fun VideoEntity.toUserWork(): UserWork = UserWork(
    id = videoId, // ✅ 修改：使用 videoId 字段
    coverUrl = coverUrl,
    playUrl = playUrl,
    title = title,
    durationMs = durationMs,
    viewCount = viewCount,
    likeCount = likeCount,
    commentCount = commentCount,
    favoriteCount = favoriteCount,
    shareCount = 0, // ✅ 修改：新表结构中没有 shareCount 字段
    orientation = orientation
)

