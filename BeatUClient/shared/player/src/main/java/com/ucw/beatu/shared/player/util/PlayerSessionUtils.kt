package com.ucw.beatu.shared.player.util

import androidx.media3.common.Player
import com.ucw.beatu.shared.common.threading.ThreadTaskPatterns
import com.ucw.beatu.shared.player.VideoPlayer
import com.ucw.beatu.shared.player.model.VideoSource
import com.ucw.beatu.shared.player.session.PlaybackSession
import kotlin.math.abs

object PlayerSessionUtils {

    fun isActuallyPlaying(player: Player): Boolean {
        return player.playWhenReady && player.playbackState == Player.STATE_READY
    }

    fun snapshotSession(
        videoId: Long,
        videoUrl: String,
        player: VideoPlayer,
        logger: (String) -> Unit
    ): PlaybackSession {
        val mediaPlayer = player.player
        val currentPosition = mediaPlayer.currentPosition
        val speed = mediaPlayer.playbackParameters.speed
        val playWhenReady = mediaPlayer.playWhenReady
        logger("snapshotSession: videoId=$videoId, position=${currentPosition}ms, speed=$speed, playWhenReady=$playWhenReady")
        return PlaybackSession(
            videoId = videoId,
            videoUrl = videoUrl,
            positionMs = currentPosition,
            speed = speed,
            playWhenReady = playWhenReady
        )
    }

    /**
     * 确保播放器承载正确的视频媒体项。
     * @return true 表示执行了 prepare（通常需要显示 loading）
     */
    fun ensurePreparedForSession(
        player: VideoPlayer,
        session: PlaybackSession,
        logger: (String) -> Unit
    ): Boolean {
        val currentMediaItem = player.player.currentMediaItem
        val currentTag = currentMediaItem?.localConfiguration?.tag as? Long
        val needsPrepare = if (currentTag != null && currentTag != session.videoId) {
            logger("ensurePreparedForSession: currentTag=$currentTag mismatch sessionVideoId=${session.videoId}, clear & prepare")
            player.pause()
            player.player.stop()
            player.player.clearMediaItems()
            true
        } else {
            currentMediaItem == null
        }

        if (!needsPrepare && currentMediaItem != null) {
            player.pause()
        } else if (needsPrepare) {
            player.prepare(VideoSource(session.videoId, session.videoUrl))
        }
        return needsPrepare
    }

    fun applySessionBase(
        player: VideoPlayer,
        session: PlaybackSession,
        logger: (String) -> Unit
    ) {
        player.setSpeed(session.speed)
        val diff = abs(player.player.currentPosition - session.positionMs)
        logger("applySessionBase: seek to ${session.positionMs}ms, diff=${diff}ms")
        player.seekTo(session.positionMs)
    }

    fun restorePlayStateWhenReady(
        player: VideoPlayer,
        shouldPlay: Boolean,
        logger: (String) -> Unit,
        maxWaitMs: Long = 300L
    ) {
        if (!shouldPlay) {
            player.pause()
            return
        }

        if (player.player.playbackState != Player.STATE_READY) {
            logger("restorePlayStateWhenReady: player not ready, skip immediate play")
            return
        }

        var waited = 0L
        while (waited <= maxWaitMs) {
            val hasVideoSize = player.player.videoSize.width > 0 && player.player.videoSize.height > 0
            if (hasVideoSize) {
                player.play()
                logger("restorePlayStateWhenReady: surface ready after ${waited}ms, play now")
                return
            }
            if (!ThreadTaskPatterns.sleepOrStop(50L)) return
            waited += 50L
        }
        logger("restorePlayStateWhenReady: force play after ${maxWaitMs}ms wait")
        player.play()
    }

}
