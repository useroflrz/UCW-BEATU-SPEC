package com.ucw.beatu.shared.common.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Feed 内容类型：
 * - VIDEO：常规短视频（有画面+声音）
 * - IMAGE_POST：图文+音乐（多张图片轮播 + 背景音乐，仅音频播放）
 */
enum class FeedContentType {
    VIDEO,
    IMAGE_POST
}

/**
 * 视频流中的单条内容模型
 *
 * 说明：
 * - 为了保持与现有代码兼容，这里仍命名为 VideoItem，但通过 [type] 字段区分是「视频」还是「图文+音乐」
 * - 当 [type] == [FeedContentType.IMAGE_POST] 时：
 *   - [imageUrls] 需提供至少 1 张图片地址
 *   - [bgmUrl] 可选，若不为空则使用播放器以"音频-only"方式播放背景音乐
 * - 此模型放在 shared:common 模块，供 videofeed 和 user 模块共享使用，避免循环依赖
 */
@Parcelize
data class VideoItem(
    val id: Long, //唯一，用视频id
    val videoUrl: String,
    val coverUrl: String? = null, // 视频封面URL
    val title: String,
    val authorName: String,
    val authorId: String = "", // 作者ID，用于获取用户详细信息
    val authorAvatar: String? = null,
    val likeCount: Int,
    val commentCount: Int,
    val favoriteCount: Int,
    val shareCount: Int,
    val isLiked: Boolean = false,
    val isFavorited: Boolean = false,
    val orientation: VideoOrientation = VideoOrientation.PORTRAIT,
    val type: FeedContentType = FeedContentType.VIDEO,
    val imageUrls: List<String> = emptyList(),
    val bgmUrl: String? = null
) : Parcelable

enum class VideoOrientation {
    PORTRAIT,
    LANDSCAPE
}

