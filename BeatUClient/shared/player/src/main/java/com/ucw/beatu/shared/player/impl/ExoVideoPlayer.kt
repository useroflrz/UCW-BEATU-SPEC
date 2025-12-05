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
    // 使用配置的超时时间，从config.xml读取
    private val httpDataSourceFactory: HttpDataSource.Factory = DefaultHttpDataSource.Factory()
        .setUserAgent("BeatU-Android-Player/1.0")
        .setAllowCrossProtocolRedirects(true) // 允许跨协议重定向（HTTP -> HTTPS）
        .setConnectTimeoutMs(config.connectTimeoutMs)
        .setReadTimeoutMs(config.readTimeoutMs)
        // 设置默认请求属性，支持OSS等云存储服务
        .setDefaultRequestProperties(mapOf(
            "Accept" to "*/*",
            "Accept-Encoding" to "identity" // 禁用压缩，避免某些OSS的问题
        ))

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
                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> {
                        // 尝试从异常中提取HTTP状态码
                        val httpStatus = error.cause?.message?.let { msg ->
                            Regex("HTTP (\\d+)").find(msg)?.groupValues?.get(1)
                        } ?: "未知"
                        "HTTP 错误 ($httpStatus): ${error.message}"
                    }
                    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> "视频文件未找到"
                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED -> "视频格式错误"
                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED -> "不支持的视频格式"
                    PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED -> "视频清单格式错误"
                    PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED -> "不支持的视频清单格式"
                    PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED -> "不允许使用HTTP（需要HTTPS）"
                    else -> error.message ?: "播放错误 (${error.errorCode})"
                }
                
                // 记录详细的错误信息
                AppLogger.e(TAG, 
                    "onPlayerError: videoId=$currentVideoId, " +
                    "errorCode=${error.errorCode}, " +
                    "errorName=${error.errorCodeName}, " +
                    "message=$errorMessage, " +
                    "cause=${error.cause?.javaClass?.simpleName}: ${error.cause?.message}", 
                    error
                )
                
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
        
        // 清理和验证 URL
        val cleanedUrl = source.url.trim()
        if (cleanedUrl.isEmpty()) {
            AppLogger.e(TAG, "prepare: 视频 URL 为空（去除空格后），videoId=${source.videoId}")
            currentVideoId?.let { videoId ->
                listeners.forEach { 
                    it.onError(videoId, IllegalArgumentException("视频 URL 为空"))
                }
            }
            return
        }
        
        // 验证 URL 格式
        val uri = try {
            android.net.Uri.parse(cleanedUrl)
        } catch (e: Exception) {
            AppLogger.e(TAG, "prepare: URL 格式无效，videoId=${source.videoId}, url=$cleanedUrl", e)
            currentVideoId?.let { videoId ->
                listeners.forEach { 
                    it.onError(videoId, IllegalArgumentException("视频 URL 格式无效: $cleanedUrl", e))
                }
            }
            return
        }
        
        // 检查是否是有效的 HTTP/HTTPS URL
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") {
            AppLogger.e(TAG, "prepare: 不支持的 URL 协议，videoId=${source.videoId}, scheme=$scheme, url=$cleanedUrl")
            currentVideoId?.let { videoId ->
                listeners.forEach { 
                    it.onError(videoId, IllegalArgumentException("不支持的 URL 协议: $scheme"))
                }
            }
            return
        }
        
        try {
            currentVideoId = source.videoId
            AppLogger.d(TAG, "prepare: 准备播放视频，videoId=${source.videoId}, scheme=$scheme, host=${uri.host}, path=${uri.path}")
            
            val mediaItem = MediaItem.Builder()
                .setUri(uri)
                .setTag(source.videoId)
                .build()
            AppLogger.d(TAG, "prepare: MediaItem created, URI=${mediaItem.localConfiguration?.uri}, setting to player")
            player.setMediaItem(mediaItem)
            player.prepare()
            AppLogger.d(TAG, "prepare: player.prepare() called, playbackState=${player.playbackState}")
        } catch (e: Exception) {
            AppLogger.e(TAG, "prepare: 准备视频时出错，videoId=${source.videoId}, url=$cleanedUrl", e)
            currentVideoId?.let { videoId ->
                listeners.forEach { 
                    it.onError(videoId, Exception("准备视频失败: ${e.message}", e))
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

