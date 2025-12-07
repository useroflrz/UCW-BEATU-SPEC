package com.ucw.beatu.business.videofeed.data.mapper

import com.ucw.beatu.business.videofeed.data.api.dto.CommentDto
import com.ucw.beatu.business.videofeed.domain.model.Comment
import com.ucw.beatu.shared.database.entity.CommentEntity
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/**
 * 评论数据映射器
 */

/**
 * DTO -> Domain Model
 * 将 ISO 8601 格式的时间字符串转换为时间戳（毫秒）
 */
fun CommentDto.toDomain(): Comment {
    val timestamp = parseIso8601ToTimestamp(createdAt)
    return Comment(
        id = id,
        videoId = videoId,
        authorId = authorId,
        authorName = authorName,
        authorAvatar = authorAvatar,
        content = content,
        createdAt = timestamp,
        isAiReply = isAiReply,
        likeCount = likeCount
    )
}

/**
 * 将 ISO 8601 格式的时间字符串转换为时间戳（毫秒）
 * 支持格式：2024-01-01T12:00:00Z 或 2024-01-01T12:00:00+08:00
 * 兼容 Android API 24+
 */
private fun parseIso8601ToTimestamp(iso8601: String): Long {
    return try {
        // 标准化输入：移除毫秒部分和时区，统一处理
        var normalized = iso8601.trim()
        
        // 支持多种 ISO 8601 格式
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",  // 2024-01-01T12:00:00Z
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",  // 2024-01-01T12:00:00.000Z
            "yyyy-MM-dd'T'HH:mm:ssXXX",  // 2024-01-01T12:00:00+08:00
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",  // 2024-01-01T12:00:00.000+08:00
            "yyyy-MM-dd'T'HH:mm:ss",  // 2024-01-01T12:00:00
        )
        
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.getDefault())
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val date = sdf.parse(normalized)
                if (date != null) {
                    return date.time
                }
            } catch (e: ParseException) {
                // 尝试下一个格式
                continue
            }
        }
        
        // 如果所有格式都失败，返回当前时间戳
        System.currentTimeMillis()
    } catch (e: Exception) {
        // 如果所有解析都失败，返回当前时间戳
        System.currentTimeMillis()
    }
}

/**
 * Entity -> Domain Model
 */
fun CommentEntity.toDomain(): Comment {
    return Comment(
        id = id,
        videoId = videoId.toLongOrNull() ?: 0L,  // ✅ 修改：将 String 转换为 Long
        authorId = authorId,
        authorName = authorName,
        authorAvatar = null,
        content = content,
        createdAt = createdAt,
        isAiReply = isAiReply,
        likeCount = 0
    )
}

/**
 * Domain Model -> Entity
 */
fun Comment.toEntity(): CommentEntity {
    return CommentEntity(
        id = id,
        videoId = videoId.toString(),  // ✅ 修改：将 Long 转换为 String（因为 CommentEntity.videoId 是 String）
        authorId = authorId,
        authorName = authorName,
        content = content,
        createdAt = createdAt,
        isAiReply = isAiReply
    )
}

