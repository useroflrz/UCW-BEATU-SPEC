package com.ucw.beatu.core.common.metrics

data class PlaybackMetrics(
    val videoId: String,
    val channel: String,
    val startUpTimeMs: Long,
    val fps: Float,
    val rebufferCount: Int,
    val memoryPeakMb: Int,
    val renderedDurationMs: Long,
    val timestamp: Long = System.currentTimeMillis()
)
