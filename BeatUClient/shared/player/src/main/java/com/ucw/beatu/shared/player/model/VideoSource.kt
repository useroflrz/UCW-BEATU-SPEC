package com.ucw.beatu.shared.player.model

data class VideoSource(
    val videoId: Long,  // ✅ 修改：从 String 改为 Long
    val url: String,
    val qualities: List<VideoQuality> = emptyList(),
    val headers: Map<String, String> = emptyMap()
)

