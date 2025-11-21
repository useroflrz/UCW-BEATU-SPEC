package com.ucw.beatu.shared.player.pool

import android.content.Context
import androidx.media3.ui.PlayerView
import com.ucw.beatu.shared.player.VideoPlayer
import com.ucw.beatu.shared.player.impl.ExoVideoPlayer
import com.ucw.beatu.shared.player.model.VideoPlayerConfig
import com.ucw.beatu.shared.player.model.VideoSource
import kotlin.collections.ArrayDeque

class VideoPlayerPool(
    private val context: Context,
    private val config: VideoPlayerConfig = VideoPlayerConfig()
) {

    private val availablePlayers = ArrayDeque<VideoPlayer>()
    private val inUsePlayers = mutableMapOf<String, VideoPlayer>()

    fun acquire(videoId: String): VideoPlayer {
        val player = inUsePlayers[videoId] ?: availablePlayers.removeFirstOrNull()
        val target = player ?: ExoVideoPlayer(context, config)
        inUsePlayers[videoId] = target
        return target
    }

    fun attach(videoId: String, playerView: PlayerView, source: VideoSource) {
        val player = acquire(videoId)
        player.attach(playerView)
        player.prepare(source)
        player.play()
    }

    fun release(videoId: String) {
        val player = inUsePlayers.remove(videoId) ?: return
        player.pause()
        availablePlayers.addLast(player)
        trimPool()
    }

    fun releaseAll() {
        (availablePlayers + inUsePlayers.values).forEach { it.release() }
        availablePlayers.clear()
        inUsePlayers.clear()
    }

    private fun trimPool() {
        while (availablePlayers.size > MAX_REUSABLE_PLAYERS) {
            availablePlayers.removeLast().release()
        }
    }

    companion object {
        private const val MAX_REUSABLE_PLAYERS = 3
    }
}

