package com.ucw.beatu.shared.common.navigation

/**
 * Navigation Graph 中的路由 ID 常量
 * 用于跨模块导航，避免直接依赖 app 模块的 R 类
 */
object NavigationIds {
    // Destination IDs
    const val FEED = "feed"
    const val USER_PROFILE = "userProfile"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val LANDSCAPE = "landscape"
    
    // Action IDs
    const val ACTION_FEED_TO_USER_PROFILE = "action_feed_to_userProfile"
    const val ACTION_FEED_TO_SEARCH = "action_feed_to_search"
    const val ACTION_FEED_TO_SETTINGS = "action_feed_to_settings"
    const val ACTION_FEED_TO_LANDSCAPE = "action_feed_to_landscape"
    const val ACTION_USER_PROFILE_TO_FEED = "action_userProfile_to_feed"
    const val ACTION_SEARCH_TO_FEED = "action_search_to_feed"
    const val ACTION_SETTINGS_TO_FEED = "action_settings_to_feed"
    const val ACTION_LANDSCAPE_TO_FEED = "action_landscape_to_feed"
}
