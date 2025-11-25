package com.ucw.beatu.business.user.data.mapper

import com.ucw.beatu.business.user.domain.model.UserWork
import com.ucw.beatu.shared.database.entity.VideoEntity

fun VideoEntity.toUserWork(): UserWork = UserWork(
    id = id,
    coverUrl = coverUrl,
    playUrl = playUrl,
    title = title,
    durationMs = durationMs,
    viewCount = viewCount
)

