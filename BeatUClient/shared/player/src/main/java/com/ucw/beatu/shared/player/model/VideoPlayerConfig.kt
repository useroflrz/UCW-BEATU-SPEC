package com.ucw.beatu.shared.player.model

data class VideoPlayerConfig(
    val useCache: Boolean = true,
    val defaultSpeed: Float = 1.0f,
    val offscreenPreloadCount: Int = 1,
    val connectTimeoutMs: Int = 1000,
    val readTimeoutMs: Int = 1000,
    val maxReusablePlayers: Int = 3
)

