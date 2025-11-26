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
            id = "video_001",
            url = "http://vjs.zencdn.net/v/oceans.mp4",
            title = "《切腹》1/2上集:浪人为何要用竹刀这般折磨自己?",
            author = "云哥讲电影 视频1",
            likeCount = 535,
            commentCount = 43,
            favoriteCount = 159,
            shareCount = 59,
            orientation = PORTRAIT
        ),
        Video(
            id = "video_002",
            url = "https://upos-sz-estgoss.bilivideo.com/upgcxcode/61/72/395437261/395437261-1-16.mp4?e=ig8euxZM2rNcNbRVhwdVhwdlhWdVhwdVhoNvNC8BqJIzNbfq9rVEuxTEnE8L5F6VnEsSTx0vkX8fqJeYTj_lta53NCM=&platform=html5&deadline=1764083454&nbs=1&os=estgoss&og=ali&trid=81655f24cd334ae7b1ddc9edae84c8ah&mid=0&oi=2672555743&gen=playurlv3&uipk=5&upsig=4b57cef5f552aa366abfb7418f7172aa&uparams=e,platform,deadline,nbs,os,og,trid,mid,oi,gen,uipk&bvc=vod&nettype=0&bw=426663&buvid=&build=0&dl=0&f=h_0_0&agrr=0&orderid=0,1",
            title = "千本樱",
            author = "视频2",
            likeCount = 1234,
            commentCount = 89,
            favoriteCount = 567,
            shareCount = 234,
            orientation = PORTRAIT
        ),
        Video(
            id = "video_003",
            url = "https://media.w3.org/2010/05/sintel/trailer.mp4",
            title = "Sintel 高清预告片 - 奇幻冒险",
            author = "视频3",
            likeCount = 890,
            commentCount = 67,
            favoriteCount = 345,
            shareCount = 123,
            orientation = PORTRAIT
        ),
        Video(
            id = "video_004",
            url = "https://upos-sz-estgoss.bilivideo.com/upgcxcode/02/94/364849402/364849402_da2-1-192.mp4?e=ig8euxZM2rNcNbRjhzdVhwdlhWTzhwdVhoNvNC8BqJIzNbfq9rVEuxTEnE8L5F6VnEsSTx0vkX8fqJeYTj_lta53NCM=&uipk=5&oi=1782024106&platform=html5&gen=playurlv3&os=estgoss&og=cos&nbs=1&trid=5b767b61c3d34b2fb925c85bda44013h&mid=0&deadline=1764084038&upsig=330adcf93bb29188dc8c2530009e51af&uparams=e,uipk,oi,platform,gen,os,og,nbs,trid,mid,deadline&bvc=vod&nettype=0&bw=1309699&buvid=&build=0&dl=0&f=h_0_0&agrr=0&orderid=0,1",
            title = "【猛男版】新宝岛 ",
            author = "视频4",
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

    /** 显式指定类型避免递归推断失败 */
    private val landscapeVideos: List<Video> = portraitVideos.map { template ->
        template.copy(
            orientation = LANDSCAPE,
            title = "${template.title} · 横屏版"
        )
    }

    fun getPage(
        orientation: Orientation,
        page: Int,
        pageSize: Int
    ): List<Video> {

        val source: List<Video> = when (orientation) {
            PORTRAIT -> portraitVideos
            LANDSCAPE -> landscapeVideos
        }

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
