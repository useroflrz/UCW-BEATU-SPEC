package com.ucw.beatu.business.videofeed.presentation.ui

import android.util.Log
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.ucw.beatu.shared.common.model.FeedContentType
import com.ucw.beatu.shared.common.model.VideoItem

/**
 * 承接播放器准备/重绑链路，降低 VideoItemFragment 体积与分支复杂度。
 */
class VideoPlaybackAttachCoordinator(
    private val tag: String,
    private val isAddedProvider: () -> Boolean,
    private val videoItemProvider: () -> VideoItem?,
    private val playerViewProvider: () -> PlayerView?,
    private val hasPreparedPlayerProvider: () -> Boolean,
    private val setHasPreparedPlayer: (Boolean) -> Unit,
    private val currentVideoIdProvider: () -> Long?,
    private val mediaPlayerProvider: () -> Player?,
    private val videoSourceProvider: (Long) -> String,
    private val prepareAudioOnly: (Long, String) -> Unit,
    private val playVideo: (Long, String) -> Unit,
    private val preparePlayer: (Long, String, PlayerView) -> Unit,
    private val resumePlayback: () -> Unit
) {
    fun handleOnStart(itemId: Long) {
        val item = videoItemProvider() ?: return
        val player = playerViewProvider()?.player
        val currentMediaItem = (player as? Player)?.currentMediaItem
        val currentTag = currentMediaItem?.localConfiguration?.tag as? Long
        val contentMismatch = currentTag != null && currentTag != itemId

        Log.d(
            tag,
            "onStart: 准备播放器，videoId=$itemId, currentVideoId=${currentVideoIdProvider()}, " +
                "hasPrepared=${hasPreparedPlayerProvider()}, contentMismatch=$contentMismatch"
        )

        if (currentVideoIdProvider() != null || hasPreparedPlayerProvider() || contentMismatch) {
            if (contentMismatch) {
                Log.w(tag, "onStart: 播放器内容不匹配 (当前=$currentTag, 目标=$itemId)，强制重新绑定")
            } else {
                Log.d(tag, "onStart: 检测到已有播放器或已准备，重新绑定")
            }
            reattachPlayer()
        } else {
            Log.d(tag, "onStart: 首次加载，准备播放器")
            preparePlayerForFirstTime()
        }
    }

    fun startPlaybackIfNeeded(forcePrepare: Boolean = false) {
        if (!isAddedProvider() || videoItemProvider() == null) {
            Log.w(tag, "Fragment not ready, skip startPlayback")
            return
        }

        val item = videoItemProvider() ?: return
        if (item.type == FeedContentType.IMAGE_POST) {
            val bgmUrl = item.bgmUrl
            if (!hasPreparedPlayerProvider() && !bgmUrl.isNullOrBlank()) {
                Log.d(tag, "startPlaybackIfNeeded: preparing audio-only BGM for image post ${item.id}")
                prepareAudioOnly(item.id, bgmUrl)
                setHasPreparedPlayer(true)
            }
            resumePlayback()
            return
        }

        val playerView = playerViewProvider()
        if (playerView == null) {
            Log.w(tag, "startPlaybackIfNeeded: playerView is null for video item, skip")
            return
        }

        val currentMediaItem = (playerView.player as? Player)?.currentMediaItem
        val currentTag = currentMediaItem?.localConfiguration?.tag as? Long
        val contentMismatch = currentTag != null && currentTag != item.id
        val needsReprepare = forcePrepare || !hasPreparedPlayerProvider() || playerView.player == null || contentMismatch

        if (needsReprepare) {
            if (contentMismatch) {
                Log.w(tag, "播放器内容不匹配，重新准备: ${item.id}")
            }
            Log.d(
                tag,
                "startPlaybackIfNeeded: (re)preparing player (force=$forcePrepare, hasPrepared=${hasPreparedPlayerProvider()}, playerView.player=${playerView.player}, contentMismatch=$contentMismatch)"
            )
            preparePlayerForFirstTime()
        } else {
            Log.d(tag, "startPlaybackIfNeeded: player already prepared and attached, resuming")
            Log.d(tag, "恢复播放: ${item.id}")
            resumePlayback()
        }
    }

    fun preparePlayerForFirstTime() {
        val item = videoItemProvider() ?: return
        val pv = playerViewProvider() ?: return
        if (item.type == FeedContentType.IMAGE_POST) {
            Log.d(tag, "preparePlayerForFirstTime: skip for image post ${item.id}")
            return
        }

        val videoSource = videoSourceProvider(item.id)
        Log.d(tag, "preparePlayerForFirstTime: 准备播放视频，视频ID=${item.id}, 视频URL=${item.videoUrl}, 数据来源=$videoSource, authorAvatar=${item.authorAvatar ?: "null"}")
        playVideo(item.id, item.videoUrl)
        preparePlayer(item.id, item.videoUrl, pv)
        setHasPreparedPlayer(true)
        Log.d(tag, "preparePlayerForFirstTime: hasPrepared=${hasPreparedPlayerProvider()}, playerView.player=${pv.player}")
    }

    fun reattachPlayer() {
        if (!isAddedProvider()) {
            Log.w(tag, "reattachPlayer: Fragment not added, skip")
            return
        }
        val item = videoItemProvider() ?: run {
            Log.w(tag, "reattachPlayer: videoItem is null, skip")
            return
        }
        val pv = playerViewProvider() ?: run {
            Log.w(tag, "reattachPlayer: playerView is null, skip")
            return
        }
        if (item.type == FeedContentType.IMAGE_POST) {
            Log.d(tag, "reattachPlayer: skip for image post ${item.id}")
            return
        }

        Log.d(tag, "重新绑定播放器: ${item.id}")
        pv.post {
            if (!isAddedProvider()) {
                Log.w(tag, "reattachPlayer: Fragment not added after post")
                return@post
            }
            if (pv.player != null && pv.player !== mediaPlayerProvider()) {
                Log.d(tag, "reattachPlayer: 清理 PlayerView 上之前的播放器")
                pv.player = null
            }
            preparePlayer(item.id, item.videoUrl, pv)
            setHasPreparedPlayer(true)
            Log.d(tag, "reattachPlayer: hasPrepared=${hasPreparedPlayerProvider()}, playerView.player=${pv.player}")
        }
    }
}
