package com.ucw.beatu.business.user.data.mapper

import com.ucw.beatu.business.user.domain.model.UserWork
import com.ucw.beatu.shared.database.entity.VideoEntity

fun VideoEntity.toUserWork(): UserWork = UserWork(
    id = id, // ✅ 修改：直接使用 Long，无需转换
    coverUrl = coverUrl,
    playUrl = playUrl,
    title = title,
    durationMs = durationMs,
    viewCount = viewCount,
    likeCount = likeCount,
    commentCount = commentCount,
    favoriteCount = favoriteCount,
    shareCount = shareCount,
    orientation = orientation  // ✅ 新增：从 VideoEntity 读取 orientation
)

