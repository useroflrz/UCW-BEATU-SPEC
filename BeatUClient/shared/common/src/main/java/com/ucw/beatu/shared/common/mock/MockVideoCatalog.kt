package com.ucw.beatu.shared.common.mock

import com.ucw.beatu.shared.common.mock.MockVideoCatalog.Orientation.LANDSCAPE
import com.ucw.beatu.shared.common.mock.MockVideoCatalog.Orientation.PORTRAIT

/**
 * 统一的 Demo / Mock 视频资源目录
 */

data class Video(
    val id: String,
    val url: String,
    val title: String,
    val author: String,
    val likeCount: Int,
    val commentCount: Int,
    val favoriteCount: Int,
    val shareCount: Int,
    val orientation: MockVideoCatalog.Orientation
)

object MockVideoCatalog {

    enum class Orientation {
        PORTRAIT,
        LANDSCAPE
    }

    /** 明确类型：List<Video> */
    private val portraitVideos: List<Video> = listOf()

    /**
     * 获取原始视频列表（用于生成用户数据等场景）
     */
    fun getPortraitVideos(): List<Video> = portraitVideos

    private val landscapeVideos: List<Video> = portraitVideos.map { template ->
        template.copy(
            orientation = LANDSCAPE,
            title = "${template.title} · 横屏版"
        )
    }

    private val allVideos: List<Video> = (portraitVideos + landscapeVideos).mapIndexed { index, video ->
        video.copy(id = "${video.id}_base_$index")
    }

    fun getPage(
        preferredOrientation: Orientation?,
        page: Int,
        pageSize: Int
    ): List<Video> {

        val source: List<Video> = preferredOrientation?.let { orientation ->
            allVideos.filter { it.orientation == orientation }
        } ?: allVideos

        if (source.isEmpty() || pageSize <= 0) return emptyList()

        val seed = page.coerceAtLeast(1)
        val shuffled = source.shuffled(kotlin.random.Random(seed))

        return (0 until pageSize).map { index ->
            val template = shuffled[index % shuffled.size]
            val suffix = "_p${page}_$index"

            template.copy(
                id = template.id + suffix,
                likeCount = template.likeCount + page * (10 + index),
                commentCount = template.commentCount + page * 5,
                favoriteCount = template.favoriteCount + page * 3,
                shareCount = template.shareCount + page * 2,
                title = "${template.title} · 第${page}辑"
            )
        }
    }
}
