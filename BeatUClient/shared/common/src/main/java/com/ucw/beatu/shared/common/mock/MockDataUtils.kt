package com.ucw.beatu.shared.common.mock

/**
 * 统一维护 Mock 数据中作者名称到用户 ID 的映射规则，保证
 * - 视频与用户目录对同一作者生成一致的 userId
 * - 特殊作者（如当前用户）可以手动指定固定 ID
 */
private val specialAuthorIds = mapOf(
    "BEATU" to "current_user"
)

fun mockAuthorIdFor(authorName: String): String {
    specialAuthorIds[authorName]?.let { return it }

    val sanitized = authorName
        .lowercase()
        .filter { it.isLetterOrDigit() }
        .ifEmpty { "author" }

    return "mock_author_$sanitized"
}

