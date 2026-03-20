package com.ucw.beatu.shared.common.navigation

import android.net.Uri

/**
 * 视频分享 Deep Link 约定：
 * - App Deep Link：beatu://video/{videoId}
 * - 可点击分享链接：https://beatu.app/video/{videoId}
 * - 兼容查询参数：beatu://video?id={videoId}
 */
object VideoShareDeepLink {
    private const val APP_SCHEME = "beatu"
    private const val APP_HOST_VIDEO = "video"
    private const val WEB_SCHEME = "https"
    private const val WEB_HOST = "beatu.app"
    private const val WEB_PATH_VIDEO = "video"
    private const val QUERY_VIDEO_ID = "id"

    fun buildApp(videoId: Long): String {
        return Uri.Builder()
            .scheme(APP_SCHEME)
            .authority(APP_HOST_VIDEO)
            .appendPath(videoId.toString())
            .build()
            .toString()
    }

    /**
     * 默认返回可点击的 https 链接，兼容大多数聊天应用的自动识别。
     */
    fun build(videoId: Long): String {
        return Uri.Builder()
            .scheme(WEB_SCHEME)
            .authority(WEB_HOST)
            .appendPath(WEB_PATH_VIDEO)
            .appendPath(videoId.toString())
            .build()
            .toString()
    }

    fun parseVideoId(uri: Uri?): Long? {
        if (uri == null) return null

        val isAppLink = uri.scheme.equals(APP_SCHEME, ignoreCase = true) &&
            uri.host.equals(APP_HOST_VIDEO, ignoreCase = true)

        val isWebLink = uri.scheme.equals(WEB_SCHEME, ignoreCase = true) &&
            uri.host.equals(WEB_HOST, ignoreCase = true)

        if (!isAppLink && !isWebLink) return null

        val pathId = if (isWebLink) {
            if (uri.pathSegments.firstOrNull()?.equals(WEB_PATH_VIDEO, ignoreCase = true) == true) {
                uri.pathSegments.getOrNull(1)?.toLongOrNull()
            } else {
                null
            }
        } else {
            uri.pathSegments.firstOrNull()?.toLongOrNull()
        }

        if (pathId != null && pathId > 0) return pathId

        val queryId = uri.getQueryParameter(QUERY_VIDEO_ID)?.toLongOrNull()
        return queryId?.takeIf { it > 0 }
    }
}
