package com.ucw.beatu.business.user.data.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginRequestDto(
    @Json(name = "userName") val userName: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class RegisterRequestDto(
    @Json(name = "userName") val userName: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class AuthTokenDto(
    @Json(name = "accessToken") val accessToken: String,
    @Json(name = "tokenType") val tokenType: String,
    @Json(name = "userId") val userId: String,
    @Json(name = "userName") val userName: String
)

