package com.ucw.beatu.core.player

import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.ucw.beatu.core.player.model.VideoSource

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
        fun onReady(videoId: String) {}
        fun onError(videoId: String, throwable: Throwable) {}
        fun onPlaybackEnded(videoId: String) {}
    }
}
