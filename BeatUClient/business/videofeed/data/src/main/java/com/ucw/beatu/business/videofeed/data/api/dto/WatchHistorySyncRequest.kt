package com.ucw.beatu.business.videofeed.data.api.dto

import com.squareup.moshi.JsonClass

/**
 * 观看历史同步请求 DTO
 * 用于批量同步观看历史到后端
 */
@JsonClass(generateAdapter = true)
data class WatchHistorySyncRequest(
    val videoId: Long,
    val userId: String,
    val lastPlayPositionMs: Long,
    val watchedAt: Long
)

