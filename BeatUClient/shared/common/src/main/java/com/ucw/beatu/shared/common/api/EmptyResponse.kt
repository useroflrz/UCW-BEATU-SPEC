package com.ucw.beatu.shared.common.api

import com.squareup.moshi.JsonClass

/**
 * 空响应类型
 * 用于API调用不需要返回数据的情况（如关注/取消关注操作）
 * 使用可选的占位符字段，确保 Moshi 可以正确序列化
 */
@JsonClass(generateAdapter = true)
data class EmptyResponse(
    val success: Boolean? = null
)

