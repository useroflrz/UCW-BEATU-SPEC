package com.ucw.beatu.business.user.domain.model

/**
 * 用户主页展示的视频概要信息
 */
data class UserWork(
    val id: String,
    val coverUrl: String,
    val playUrl: String,
    val title: String,
    val durationMs: Long,
    val viewCount: Long
)

