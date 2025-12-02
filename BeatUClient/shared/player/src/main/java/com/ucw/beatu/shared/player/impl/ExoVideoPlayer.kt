package com.ucw.beatu.shared.player.impl

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import com.ucw.beatu.shared.common.logger.AppLogger
import com.ucw.beatu.shared.player.VideoPlayer
import com.ucw.beatu.shared.player.model.VideoPlayerConfig
import com.ucw.beatu.shared.player.model.VideoSource

@UnstableApi
class ExoVideoPlayer(
    context: Context,
    private val config: VideoPlayerConfig = VideoPlayerConfig()
) : VideoPlayer {

    private val trackSelector = DefaultTrackSelector(context).apply {
        setParameters(buildUponParameters().setMaxVideoSizeSd())
    }

    // 配置 HttpDataSource 以支持 OSS 链接
    private val httpDataSourceFactory: HttpDataSource.Factory = DefaultHttpDataSource.Factory()
        .setUserAgent("BeatU-Android-Player/1.0")
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(15000)
        .setReadTimeoutMs(15000)

    private val mediaSourceFactory = DefaultMediaSourceFactory(context)
        .setDataSourceFactory(httpDataSourceFactory)

    override val player: Player = ExoPlayer.Builder(context)
        .setTrackSelector(trackSelector)
        .setMediaSourceFactory(mediaSourceFactory)
        .build()

    private var currentVideoId: String? = null
    private val listeners = mutableSetOf<VideoPlayer.Listener>()

    init {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val stateName = when (playbackState) {
                    Player.STATE_IDLE -> "IDLE"
                    Player.STATE_BUFFERING -> "BUFFERING"
                    Player.STATE_READY -> "READY"
                    Player.STATE_ENDED -> "ENDED"
                    else -> "UNKNOWN($playbackState)"
                }
                AppLogger.d(TAG, "onPlaybackStateChanged: $stateName for videoId=$currentVideoId")
                if (playbackState == Player.STATE_ENDED) {
                    currentVideoId?.let { videoId ->
                        listeners.forEach { it.onPlaybackEnded(videoId) }
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val errorMessage = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "网络连接失败"
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "网络连接超时"
                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "HTTP 错误: ${error.message}"
                    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> "视频文件未找到"
                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED -> "视频格式错误"
                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED -> "不支持的视频格式"
                    else -> error.message ?: "播放错误 (${error.errorCode})"
                }
                AppLogger.e(TAG, "onPlayerError: videoId=$currentVideoId, errorCode=${error.errorCode}, message=$errorMessage", error)
                currentVideoId?.let { videoId ->
                    // 创建一个新的异常，包含更友好的错误信息
                    val friendlyError = Exception(errorMessage, error)
                    listeners.forEach { it.onError(videoId, friendlyError) }
                }
            }

            override fun onRenderedFirstFrame() {
                AppLogger.d(TAG, "onRenderedFirstFrame: videoId=$currentVideoId")
                currentVideoId?.let { videoId ->
                    listeners.forEach { it.onReady(videoId) }
                }
            }
        })
    }

    override fun attach(playerView: PlayerView) {
        if (playerView.player === player) {
            AppLogger.d(TAG, "attach: 播放器已绑定到此 PlayerView")
            return
        }
        // 如果播放器已经 attach 到其他 PlayerView，先 detach
        val previousView = playerView.player?.let { 
            // 查找当前播放器 attach 的 PlayerView（通过反射或状态检查）
            null // ExoPlayer 会自动处理，但我们需要记录日志
        }
        AppLogger.d(TAG, "attach: 正在将播放器绑定到 PlayerView，视频ID=$currentVideoId，播放状态=${player.playbackState}，是否准备播放=${player.playWhenReady}")
        playerView.player = player
        AppLogger.d(TAG, "attach: 播放器已绑定，PlayerView.player=${playerView.player}，是否已绑定=${playerView.player === player}")
    }

    override fun prepare(source: VideoSource) {
        AppLogger.d(TAG, "prepare: videoId=${source.videoId}, url=${source.url}")
        
        // 验证 URL 格式
        if (source.url.isBlank()) {
            AppLogger.e(TAG, "prepare: 视频 URL 为空，videoId=${source.videoId}")
            currentVideoId?.let { videoId ->
                listeners.forEach { 
                    it.onError(videoId, IllegalArgumentException("视频 URL 为空"))
                }
            }
            return
        }
        
        try {
            currentVideoId = source.videoId
            val mediaItem = MediaItem.Builder()
                .setUri(source.url)
                .setTag(source.videoId)
                .build()
            AppLogger.d(TAG, "prepare: MediaItem created, URI=${mediaItem.localConfiguration?.uri}, setting to player")
            player.setMediaItem(mediaItem)
            player.prepare()
            AppLogger.d(TAG, "prepare: player.prepare() called, playbackState=${player.playbackState}")
        } catch (e: Exception) {
            AppLogger.e(TAG, "prepare: 准备视频时出错，videoId=${source.videoId}, url=${source.url}", e)
            currentVideoId?.let { videoId ->
                listeners.forEach { 
                    it.onError(videoId, e)
                }
            }
        }
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
        AppLogger.d(TAG, "release player $currentVideoId")
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

