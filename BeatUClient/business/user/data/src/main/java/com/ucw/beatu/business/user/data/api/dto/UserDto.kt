package com.ucw.beatu.business.user.data.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 用户数据传输对象
 * 对应后端API返回的用户数据格式
 * 后端使用 by_alias=True，所以返回 camelCase 格式
 */
@JsonClass(generateAdapter = true)
data class UserDto(
    val id: String,
    @Json(name = "userName") val userName: String? = null, // JSON 中有此字段，但客户端主要使用 name
    val avatarUrl: String? = null,
    val name: String,
    val bio: String? = null,
    @Json(name = "followingCount") val followingCount: Long,
    @Json(name = "followersCount") val followersCount: Long,
    @Json(name = "likesCount") val likesCount: Long = 0
)

