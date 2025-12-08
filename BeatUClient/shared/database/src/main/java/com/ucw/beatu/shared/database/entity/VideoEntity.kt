package com.ucw.beatu.shared.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 视频内容表，对应后端 beatu_video
 * 用于缓存视频数据，支持主页Feed、关注Feed、个人主页、搜索等场景
 */
@Entity(tableName = "beatu_video")
data class VideoEntity(
    @PrimaryKey val videoId: Long,  // 视频 ID (PK)
    val playUrl: String,  // 播放地址
    val coverUrl: String,  // 封面地址
    val title: String,  // 视频标题
    val authorId: String,  // 作者 ID
    val orientation: String,  // 横屏/竖屏
    val durationMs: Long,  // 视频时长（毫秒）
    val likeCount: Long,  // 点赞数
    val commentCount: Long,  // 评论数
    val favoriteCount: Long,  // 收藏数
    val viewCount: Long,  // 观看次数
    val authorAvatar: String? = null,  // 作者头像
    val shareUrl: String? = null  // 分享链接
)

