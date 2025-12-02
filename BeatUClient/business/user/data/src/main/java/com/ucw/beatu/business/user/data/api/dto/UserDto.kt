package com.ucw.beatu.business.user.data.api.dto

import com.squareup.moshi.JsonClass

/**
 * 用户数据传输对象
 * 对应后端API返回的用户数据格式
 */
@JsonClass(generateAdapter = true)
data class UserDto(
    val id: String,
    val avatarUrl: String? = null,
    val name: String,
    val bio: String? = null,
    val likesCount: Long = 0,
    val followingCount: Long = 0,
    val followersCount: Long = 0
)

