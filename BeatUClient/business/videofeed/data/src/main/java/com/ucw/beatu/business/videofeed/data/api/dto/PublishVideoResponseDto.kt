package com.ucw.beatu.business.videofeed.data.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PublishVideoResponseDto(
    @Json(name = "videoId") val videoId: Long,
    @Json(name = "playUrl") val playUrl: String,
    @Json(name = "coverUrl") val coverUrl: String,
    val title: String,
    @Json(name = "authorId") val authorId: String
)

