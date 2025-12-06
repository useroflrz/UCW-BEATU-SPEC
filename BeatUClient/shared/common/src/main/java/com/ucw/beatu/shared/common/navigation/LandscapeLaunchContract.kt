package com.ucw.beatu.shared.common.navigation

/**
 * 横屏页面启动所需的 Intent Key。
 */
object LandscapeLaunchContract {
    const val ACTIVITY_CLASS_NAME =
        "com.ucw.beatu.business.landscape.presentation.ui.LandscapeActivity"

    const val EXTRA_VIDEO_ID = "extra_landscape_video_id"
    const val EXTRA_VIDEO_URL = "extra_landscape_video_url"
    const val EXTRA_VIDEO_TITLE = "extra_landscape_video_title"
    const val EXTRA_VIDEO_AUTHOR = "extra_landscape_video_author"
    const val EXTRA_VIDEO_LIKE = "extra_landscape_video_like"
    const val EXTRA_VIDEO_COMMENT = "extra_landscape_video_comment"
    const val EXTRA_VIDEO_FAVORITE = "extra_landscape_video_favorite"
    const val EXTRA_VIDEO_SHARE = "extra_landscape_video_share"
    
    // 视频列表限制（用于用户作品观看页面切换到横屏时）
    const val EXTRA_VIDEO_LIST = "extra_landscape_video_list"
    const val EXTRA_CURRENT_INDEX = "extra_landscape_current_index"
}


