package com.ucw.beatu.shared.common.mock

/**
 * Demo / Mock 评论数据：
 * - 按视频 ID 提供一组“看起来真实”的评论
 * - 仅用于本地演示评论区 UI，不调用真实后端
 */
data class MockComment(
    val id: String,
    val videoId: Long,  // ✅ 修改：从 String 改为 Long
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
    fun getCommentsForVideo(videoId: Long, count: Int = 20): List<MockComment> {  // ✅ 修改：从 String 改为 Long
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
    private val sampleComments: List<MockComment> = listOf()
}


