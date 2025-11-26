package com.ucw.beatu.business.videofeed.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.ucw.beatu.shared.player.VideoPlayer
import com.ucw.beatu.shared.player.model.VideoSource
import com.ucw.beatu.shared.player.pool.VideoPlayerPool
import com.ucw.beatu.shared.player.session.PlaybackSession
import com.ucw.beatu.shared.player.session.PlaybackSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 单个视频项 ViewModel
 * 管理单个视频的播放器生命周期和状态
 */
data class VideoItemUiState(
    val currentVideoId: String? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val showPlaceholder: Boolean = true,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val currentSpeed: Float = 1.0f,
    val error: String? = null
)

@HiltViewModel
class VideoItemViewModel @Inject constructor(
    application: Application,
    private val playerPool: VideoPlayerPool,
    private val playbackSessionStore: PlaybackSessionStore
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(VideoItemUiState())
    val uiState: StateFlow<VideoItemUiState> = _uiState.asStateFlow()

    private var currentPlayer: VideoPlayer? = null
    private var currentVideoId: String? = null
    private var currentVideoUrl: String? = null
    private var progressJob: Job? = null
    private var playerListener: VideoPlayer.Listener? = null
    private var handoffInProgress = false

    /**
     * 播放视频
     */
    fun playVideo(videoId: String, videoUrl: String) {
        viewModelScope.launch {
            // 如果已经在播放同一个视频，不重复播放
            if (currentVideoId == videoId && currentPlayer != null) {
                return@launch
            }

            // 释放之前的播放器
            releaseCurrentPlayer()

            // 更新状态
            _uiState.value = _uiState.value.copy(
                currentVideoId = videoId,
                isLoading = true,
                showPlaceholder = true,
                error = null
            )

            currentVideoId = videoId
            currentVideoUrl = videoUrl
        }
    }

    /**
     * 准备播放器（由 Fragment 调用，传入 PlayerView）
     */
    fun preparePlayer(videoId: String, videoUrl: String, playerView: PlayerView) {
        viewModelScope.launch {
            try {
                // 检查参数有效性
                if (videoId.isBlank() || videoUrl.isBlank()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "视频ID或URL为空",
                        isPlaying = false
                    )
                    return@launch
                }

                val source = VideoSource(
                    videoId = videoId,
                    url = videoUrl
                )

                val player = playerPool.acquire(videoId)
                currentPlayer = player
                currentVideoUrl = videoUrl

                // 添加监听器
                playerListener?.let { player.removeListener(it) }
                val listener = object : VideoPlayer.Listener {
                    override fun onReady(videoId: String) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            showPlaceholder = false,
                            isPlaying = true,
                            durationMs = player.player.duration.takeIf { it > 0 } ?: _uiState.value.durationMs
                        )
                    }

                    override fun onError(videoId: String, throwable: Throwable) {
                        android.util.Log.e("VideoItemViewModel", "Player error for $videoId", throwable)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = throwable.message ?: "播放失败",
                            isPlaying = false
                        )
                    }

                    override fun onPlaybackEnded(videoId: String) {
                        _uiState.value = _uiState.value.copy(
                            isPlaying = false
                        )
                    }
                }
                player.addListener(listener)
                playerListener = listener

                // 绑定播放器到 PlayerView
                player.attach(playerView)

                val pendingSession = playbackSessionStore.consume(videoId)
                handoffInProgress = pendingSession != null
                if (pendingSession != null) {
                    applyPlaybackSession(player, pendingSession)
                } else {
                    player.prepare(source)
                    player.play()
                }

                _uiState.value = _uiState.value.copy(
                    currentVideoId = videoId,
                    isPlaying = true
                )
                startProgressUpdates()

            } catch (e: Exception) {
                android.util.Log.e("VideoItemViewModel", "Error preparing player", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "播放器初始化失败",
                    isPlaying = false
                )
            }
        }
    }

    /**
     * 暂停/播放切换
     */
    fun togglePlayPause() {
        currentPlayer?.let { player ->
            if (_uiState.value.isPlaying) {
                player.pause()
                _uiState.value = _uiState.value.copy(isPlaying = false)
            } else {
                player.play()
                _uiState.value = _uiState.value.copy(isPlaying = true)
            }
        }
    }

    /**
     * 暂停播放
     */
    fun pause() {
        currentPlayer?.pause()
        _uiState.value = _uiState.value.copy(isPlaying = false)
    }

    /**
     * 恢复播放
     */
    fun resume() {
        currentPlayer?.play()
        _uiState.value = _uiState.value.copy(isPlaying = true)
    }

    /**
     * 将当前播放器重新绑定到新的 PlayerView（例如横竖屏切回）。
     */
    fun onHostResume(targetView: PlayerView?) {
        if (targetView == null) return
        val videoId = currentVideoId ?: return
        val player = currentPlayer ?: playerPool.acquire(videoId).also { currentPlayer = it }
        player.attach(targetView)

        playbackSessionStore.consume(videoId)?.let {
            applyPlaybackSession(player, it)
            handoffInProgress = false
        } ?: run {
            if (_uiState.value.isPlaying) {
                player.play()
            }
        }
        startProgressUpdates()
    }

    fun mediaPlayer(): Player? = currentPlayer?.player

    /**
     * 释放当前播放器
     */
    fun releaseCurrentPlayer() {
        stopProgressUpdates()
        playerListener?.let { listener ->
            currentPlayer?.removeListener(listener)
        }
        playerListener = null
        currentVideoId?.let { videoId ->
            playerPool.release(videoId)
        }
        currentPlayer = null
        currentVideoId = null
        currentVideoUrl = null
        _uiState.value = _uiState.value.copy(
            currentVideoId = null,
            isPlaying = false,
            showPlaceholder = true
        )
    }

    fun persistPlaybackSession(): PlaybackSession? {
        val session = snapshotPlayback() ?: return null
        playbackSessionStore.save(session)
        handoffInProgress = true
        return session
    }

    fun snapshotPlayback(): PlaybackSession? {
        val videoId = currentVideoId ?: return null
        val videoUrl = currentVideoUrl ?: return null
        val player = currentPlayer ?: return null
        val mediaPlayer = player.player
        return PlaybackSession(
            videoId = videoId,
            videoUrl = videoUrl,
            positionMs = mediaPlayer.currentPosition,
            speed = mediaPlayer.playbackParameters.speed,
            playWhenReady = mediaPlayer.playWhenReady
        )
    }

    override fun onCleared() {
        super.onCleared()
        releaseCurrentPlayer()
    }

    private fun applyPlaybackSession(player: VideoPlayer, session: PlaybackSession) {
        if (player.player.currentMediaItem == null) {
            player.prepare(VideoSource(session.videoId, session.videoUrl))
        }
        player.seekTo(session.positionMs)
        player.setSpeed(session.speed)
        if (session.playWhenReady) {
            player.play()
        } else {
            player.pause()
        }
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            showPlaceholder = false,
            isPlaying = session.playWhenReady,
            currentPositionMs = session.positionMs,
            currentSpeed = session.speed
        )
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (currentPlayer != null) {
                delay(500)
                currentPlayer?.player?.let { player ->
                    _uiState.value = _uiState.value.copy(
                        currentPositionMs = player.currentPosition,
                        durationMs = player.duration.takeIf { it > 0 } ?: _uiState.value.durationMs,
                        currentSpeed = player.playbackParameters.speed
                    )
                }
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }
}


