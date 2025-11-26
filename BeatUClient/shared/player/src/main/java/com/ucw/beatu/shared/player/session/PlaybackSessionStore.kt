package com.ucw.beatu.shared.player.session

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 保存播放器在不同宿主之间切换时的临时状态。
 * 目前用于竖屏 <-> 横屏切换时的播放进度、倍速、播放状态同步。
 */
data class PlaybackSession(
    val videoId: String,
    val videoUrl: String,
    val positionMs: Long,
    val speed: Float,
    val playWhenReady: Boolean
)

@Singleton
class PlaybackSessionStore @Inject constructor() {

    private val sessions = ConcurrentHashMap<String, PlaybackSession>()

    fun save(session: PlaybackSession) {
        sessions[session.videoId] = session
    }

    fun consume(videoId: String): PlaybackSession? = sessions.remove(videoId)

    fun peek(videoId: String): PlaybackSession? = sessions[videoId]
}

