package com.ucw.beatu.business.user.domain.model

/**
 * 用户主页展示的视频概要信息
 */
data class UserWork(
    val id: Long,  // ✅ 修改：从 String 改为 Long
    val coverUrl: String,
    val playUrl: String,
    val title: String,
    val durationMs: Long,
    val viewCount: Long,
    val likeCount: Long,
    val commentCount: Long,
    val favoriteCount: Long,
    val shareCount: Long,
    val orientation: String = "PORTRAIT"  // ✅ 新增：视频方向，默认为 PORTRAIT
)

