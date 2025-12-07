package com.ucw.beatu.shared.player

import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.ucw.beatu.shared.player.model.VideoSource

interface VideoPlayer {
    val player: Player

    fun attach(playerView: PlayerView)
    fun prepare(source: VideoSource)
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun setSpeed(speed: Float)
    fun release()

    fun addListener(listener: Listener)
    fun removeListener(listener: Listener)

    interface Listener {
        fun onReady(videoId: Long) {}  // ✅ 修改：从 String 改为 Long
        fun onError(videoId: Long, throwable: Throwable) {}  // ✅ 修改：从 String 改为 Long
        fun onPlaybackEnded(videoId: Long) {}  // ✅ 修改：从 String 改为 Long
    }
}

