package com.ucw.beatu.shared.player.pool

import android.content.Context
import androidx.media3.ui.PlayerView
import com.ucw.beatu.shared.common.logger.AppLogger
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
    private val inUsePlayers = mutableMapOf<Long, VideoPlayer>()  // ✅ 修改：从 String 改为 Long

    fun acquire(videoId: Long): VideoPlayer {  // ✅ 修改：从 String 改为 Long
        val player = inUsePlayers[videoId] ?: availablePlayers.removeFirstOrNull()
        val target = player ?: ExoVideoPlayer(context, config)
        val isFromPool = player != null && player === target && player !in inUsePlayers.values
        
        // ✅ 添加：如果从 availablePlayers 中获取的播放器，检查其内容是否匹配
        if (isFromPool) {
            // 这是从 availablePlayers 中获取的播放器，需要检查内容
            val currentMediaItem = target.player.currentMediaItem
            val currentTag = currentMediaItem?.localConfiguration?.tag as? Long  // ✅ 修改：tag 现在是 Long
            if (currentTag != null && currentTag != videoId) {
                AppLogger.w(TAG, "acquire: 从池中获取的播放器内容不匹配 (当前=$currentTag, 目标=$videoId)，需要清理")
                // 播放器内容不匹配，需要清理
                target.pause()
                target.player.stop()
                target.player.clearMediaItems()
            } else {
                AppLogger.d(TAG, "acquire: 从池中获取播放器，内容匹配 (videoId=$videoId)")
            }
        } else if (player == null) {
            AppLogger.d(TAG, "acquire: 创建新播放器 (videoId=$videoId)")
        } else {
            AppLogger.d(TAG, "acquire: 使用已存在的播放器 (videoId=$videoId)")
        }
        
        inUsePlayers[videoId] = target
        return target
    }

    fun attach(videoId: Long, playerView: PlayerView, source: VideoSource) {  // ✅ 修改：从 String 改为 Long
        val player = acquire(videoId)
        player.attach(playerView)
        player.prepare(source)
        player.play()
    }

    fun release(videoId: Long) {  // ✅ 修改：从 String 改为 Long
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
        while (availablePlayers.size > config.maxReusablePlayers) {
            availablePlayers.removeLast().release()
        }
    }
    
    companion object {
        private const val TAG = "VideoPlayerPool"
    }
}

