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
    private val portraitVideos: List<Video> = listOf(
        Video(
            id = "video_0011",
            url = "http://vjs.zencdn.net/v/oceans.mp4",
            title = "测试视频1",
            author = "云哥讲电影 视频1",
            likeCount = 535,
            commentCount = 43,
            favoriteCount = 159,
            shareCount = 59,
            orientation = LANDSCAPE
        ),
        Video(
            id = "video_0012",
            url = "https://media.w3.org/2010/05/sintel/trailer.mp4",
            title = "Sintel 高清预告片 - 奇幻冒险",
            author = "视频3",
            likeCount = 890,
            commentCount = 67,
            favoriteCount = 345,
            shareCount = 123,
            orientation = LANDSCAPE
        ),
        Video(
            id = "video_002",
            url = "http://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/%E6%A8%AA%E5%B1%8F%E8%A7%86%E9%A2%911.mp4",
            title = "横屏视频1",
            author = "视频2",
            likeCount = 1234,
            commentCount = 89,
            favoriteCount = 567,
            shareCount = 234,
            orientation = LANDSCAPE
        ),
        Video(
            id = "video_003",
            url = "http://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/%E6%A8%AA%E5%B1%8F%E8%A7%86%E9%A2%912.mp4",
            title = "横屏视频2",
            author = "视频3",
            likeCount = 1234,
            commentCount = 89,
            favoriteCount = 567,
            shareCount = 234,
            orientation = LANDSCAPE
        ),
        Video(
            id = "video_004",
            url = "http://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/%E6%A8%AA%E5%B1%8F%E8%A7%86%E9%A2%913.mp4",
            title = "横屏视频3",
            author = "视频4",
            likeCount = 1234,
            commentCount = 89,
            favoriteCount = 567,
            shareCount = 234,
            orientation = LANDSCAPE
        ),
        Video(
            id = "video_005",
            url = "http://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/%E6%A8%AA%E5%B1%8F%E8%A7%86%E9%A2%914.mp4",
            title = "横屏视频4",
            author = "视频5",
            likeCount = 1234,
            commentCount = 89,
            favoriteCount = 567,
            shareCount = 234,
            orientation = LANDSCAPE
        ),
        Video(
            id = "video_006",
            url = "http://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/%E6%A8%AA%E5%B1%8F%E8%A7%86%E9%A2%915.mp4",
            title = "横屏视频5",
            author = "视频6",
            likeCount = 1234,
            commentCount = 89,
            favoriteCount = 567,
            shareCount = 234,
            orientation = LANDSCAPE
        ),
        Video(
            id = "video_007",
            url = "http://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/%E7%AB%96%E5%B1%8F%E8%A7%86%E9%A2%911.mp4",
            title = "竖屏视频1",
            author = "竖屏视频1",
            likeCount = 2345,
            commentCount = 156,
            favoriteCount = 789,
            shareCount = 456,
            orientation = PORTRAIT
        ),
        Video(
            id = "video_008",
            url = "http://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/%E7%AB%96%E5%B1%8F%E7%AB%96%E5%B1%8F2.mp4",
            title = "竖屏视频2",
            author = "竖屏视频2",
            likeCount = 2345,
            commentCount = 156,
            favoriteCount = 789,
            shareCount = 456,
            orientation = PORTRAIT
        ),
        Video(
            id = "video_009",
            url = "http://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/%E7%AB%96%E5%B1%8F%E8%A7%86%E9%A2%913.mp4",
            title = "竖屏视频3",
            author = "竖屏视频3",
            likeCount = 2345,
            commentCount = 156,
            favoriteCount = 789,
            shareCount = 456,
            orientation = PORTRAIT
        ),
        Video(
            id = "video_010",
            url = "http://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/%E7%AB%96%E5%B1%8F%E8%A7%86%E9%A2%914.mp4",
            title = "竖屏视频4",
            author = "竖屏视频4",
            likeCount = 2345,
            commentCount = 156,
            favoriteCount = 789,
            shareCount = 456,
            orientation = PORTRAIT
        ),
        Video(
            id = "video_011",
            url = "http://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/%E7%AB%96%E5%B1%8F%E7%AB%96%E5%B1%8F5.mp4",
            title = "竖屏视频5",
            author = "竖屏视频5",
            likeCount = 2345,
            commentCount = 156,
            favoriteCount = 789,
            shareCount = 456,
            orientation = PORTRAIT
        ),
        Video(
            id = "video_005",
            url = "https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/Justin%20Bieber%20-%20Beauty%20And%20A%20Beat.mp4",
            title = "Justin Bieber - Beauty And A Beat",
            author = "视频5",
            likeCount = 678,
            commentCount = 45,
            favoriteCount = 234,
            shareCount = 89,
            orientation = LANDSCAPE
        )
    )

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
