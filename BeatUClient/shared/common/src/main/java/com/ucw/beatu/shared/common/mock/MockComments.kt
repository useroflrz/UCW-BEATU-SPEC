package com.ucw.beatu.shared.common.mock

/**
 * Demo / Mock 评论数据：
 * - 按视频 ID 提供一组“看起来真实”的评论
 * - 仅用于本地演示评论区 UI，不调用真实后端
 */
data class MockComment(
    val id: String,
    val videoId: String,
    val userName: String,
    val isAuthor: Boolean,
    val timeDesc: String,
    val location: String?,
    val content: String,
    val likeCount: Int
)

object MockComments {

    /**
     * 简单用固定几条样例评论，按 videoId 做一个“散列”选择，
     * 让不同视频看起来有不同的评论列表。
     */
    fun getCommentsForVideo(videoId: String, count: Int = 20): List<MockComment> {
        if (count <= 0) return emptyList()

        val base = sampleComments
        if (base.isEmpty()) return emptyList()

        val seed = (videoId.hashCode().toLong() and 0x7FFFFFFF).toInt()
        val shuffled = base.shuffled(kotlin.random.Random(seed))

        return (0 until count).map { index ->
            val template = shuffled[index % shuffled.size]
            val suffix = "_$index"
            template.copy(
                id = template.id + suffix,
                videoId = videoId,
                // 让每条评论的点赞数稍微有点区别
                likeCount = template.likeCount + (index * 3)
            )
        }
    }

    // 一组通用示例评论，文案参考你提供的截图风格
    private val sampleComments: List<MockComment> = listOf(
        MockComment(
            id = "c001",
            videoId = "",
            userName = "草莓",
            isAuthor = false,
            timeDesc = "3天前 · 河北",
            location = "河北",
            content = "今天开始我要自己学Java",
            likeCount = 536
        ),
        MockComment(
            id = "c002",
            videoId = "",
            userName = "我最喜欢旮旯给木",
            isAuthor = true,
            timeDesc = "3天前 · 湖南",
            location = "湖南",
            content = "我一出生就会说：你好世界！",
            likeCount = 262
        ),
        MockComment(
            id = "c003",
            videoId = "",
            userName = "甘乐",
            isAuthor = false,
            timeDesc = "2天前 · 上海",
            location = "上海",
            content = "爱评论的人，运气不会差。每次看完都感觉被治愈了。",
            likeCount = 189
        ),
        MockComment(
            id = "c004",
            videoId = "",
            userName = "今天也要好好吃饭",
            isAuthor = false,
            timeDesc = "昨天 · 北京",
            location = "北京",
            content = "这期真的笑死我了，镜头语言也好强，求出幕后花絮！！",
            likeCount = 97
        ),
        MockComment(
            id = "c005",
            videoId = "",
            userName = "今日养佐味a'a'a'a'z'z",
            isAuthor = false,
            timeDesc = "1小时前 · 广州",
            location = "广州",
            content = "弹幕+评论一起看，幸福值直接拉满，拜托你一定要一直拍下去！",
            likeCount = 54
        )
    )
}


