package com.ucw.beatu.business.landscape.presentation.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 视频项数据模型（硬编码测试数据）
 */
@Parcelize
data class VideoItem(
    val id: String,
    val videoUrl: String,
    val title: String,
    val authorName: String,
    val likeCount: Int,
    val commentCount: Int,
    val favoriteCount: Int,
    val shareCount: Int
) : Parcelable

