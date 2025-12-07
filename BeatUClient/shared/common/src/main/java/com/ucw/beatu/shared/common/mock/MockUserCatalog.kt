package com.ucw.beatu.shared.common.mock


import com.ucw.beatu.shared.common.mock.MockVideoCatalog

/**
 * 统一的 Demo / Mock 视频资源目录
 */

data class User(
    val id: String,
    val avatarUrl: String?, // 头像（图片路径）
    val name: String, // 名称
    val bio: String?, // 名言/简介
    val likesCount: Long = 0, // 获赞
    val followingCount: Long = 0, // 关注
    val followersCount: Long = 0 // 粉丝
)

/**
 * 用户 Mock 数据目录
 * 基于 MockVideoCatalog 中出现的作者名称生成一批演示用户，
 * 并额外包含一个当前用户（例如 "current_user"）。
 */
object MockUserCatalog {

    /**
     * 生成一批演示用户：
     * - 包含一个当前用户（currentUserId）
     * - 加上从 MockVideoCatalog 中提取出的唯一作者列表
     */
    fun buildMockUsers(currentUserId: String = "current_user"): List<User> {
        val currentUser = User(
            id = currentUserId,
            avatarUrl = null,
            name = "BEATU",
            bio = "一句话介绍一下自己",
            likesCount = 56_000L,
            followingCount = 128L,
            followersCount = 12_000L
        )

        return listOf(currentUser)
    }
}


