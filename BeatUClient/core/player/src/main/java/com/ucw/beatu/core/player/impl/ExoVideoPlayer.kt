package com.ucw.beatu.core.player.impl

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import com.ucw.beatu.core.common.logger.AppLogger
import com.ucw.beatu.core.player.VideoPlayer
import com.ucw.beatu.core.player.model.VideoPlayerConfig
import com.ucw.beatu.core.player.model.VideoSource

@OptIn(UnstableApi::class)
class ExoVideoPlayer(
    context: Context,
    private val config: VideoPlayerConfig = VideoPlayerConfig()
) : VideoPlayer {

    private val trackSelector = DefaultTrackSelector(context).apply {
        setParameters(buildUponParameters().setMaxVideoSizeSd())
    }

    override val player: Player = ExoPlayer.Builder(context)
        .setTrackSelector(trackSelector)
        .build()

    private var currentVideoId: String? = null
    private val listeners = mutableSetOf<VideoPlayer.Listener>()

    init {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    currentVideoId?.let { videoId ->
                        listeners.forEach { it.onPlaybackEnded(videoId) }
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                currentVideoId?.let { videoId ->
                    listeners.forEach { it.onError(videoId, error) }
                }
            }

            override fun onRenderedFirstFrame() {
                currentVideoId?.let { videoId ->
                    listeners.forEach { it.onReady(videoId) }
                }
            }
        })
    }

    override fun attach(playerView: PlayerView) {
        if (playerView.player === player) return
        playerView.player = player
    }

    override fun prepare(source: VideoSource) {
        currentVideoId = source.videoId
        val mediaItem = MediaItem.Builder()
            .setUri(source.url)
            .setTag(source.videoId)
            .build()
        player.setMediaItem(mediaItem)
        player.prepare()
    }

    override fun play() {
        player.playWhenReady = true
    }

    override fun pause() {
        player.playWhenReady = false
    }

    override fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }

    override fun setSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
    }

    override fun release() {
        AppLogger.d(TAG, "release player ${'$'}currentVideoId")
        player.release()
        listeners.clear()
    }

    override fun addListener(listener: VideoPlayer.Listener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: VideoPlayer.Listener) {
        listeners.remove(listener)
    }

    companion object {
        private const val TAG = "ExoVideoPlayer"
    }
}
