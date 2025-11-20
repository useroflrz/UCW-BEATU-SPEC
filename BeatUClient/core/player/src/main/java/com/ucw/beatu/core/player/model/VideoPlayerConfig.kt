package com.ucw.beatu.core.player.model

data class VideoPlayerConfig(
    val useCache: Boolean = true,
    val defaultSpeed: Float = 1.0f,
    val offscreenPreloadCount: Int = 1
)
