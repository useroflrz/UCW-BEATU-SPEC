package com.ucw.beatu.business.landscape.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ucw.beatu.shared.player.VideoPlayer
import com.ucw.beatu.shared.player.model.VideoSource
import com.ucw.beatu.shared.player.pool.VideoPlayerPool
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 横屏单个视频项 ViewModel
 * 复用竖屏的播放器生命周期管理逻辑
 */
@HiltViewModel
class LandscapeVideoItemViewModel @Inject constructor(
    application: Application,
    private val playerPool: VideoPlayerPool
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(LandscapeVideoItemUiState())
    val uiState: StateFlow<LandscapeVideoItemUiState> = _uiState.asStateFlow()

    private var currentPlayer: VideoPlayer? = null
    private var currentVideoId: String? = null
    
    // 播放进度更新
    private var progressUpdateJob: kotlinx.coroutines.Job? = null

    fun playVideo(videoId: String, videoUrl: String) {
        viewModelScope.launch {
            if (currentVideoId == videoId && currentPlayer != null) {
                return@launch
            }
            releaseCurrentPlayer()
            _uiState.value = _uiState.value.copy(
                currentVideoId = videoId,
                isLoading = true,
                showPlaceholder = true,
                error = null
            )
            currentVideoId = videoId
        }
    }

    fun preparePlayer(videoId: String, videoUrl: String, playerView: androidx.media3.ui.PlayerView) {
        viewModelScope.launch {
            try {
                if (videoId.isBlank() || videoUrl.isBlank()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "视频ID或URL为空",
                        isPlaying = false
                    )
                    return@launch
                }

                val source = VideoSource(videoId = videoId, url = videoUrl)
                val player = playerPool.acquire(videoId)
                currentPlayer = player

                player.addListener(object : VideoPlayer.Listener {
                    override fun onReady(videoId: String) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            showPlaceholder = false,
                            isPlaying = true,
                            durationMs = player.player.duration.takeIf { it > 0 } ?: 0L
                        )
                        startProgressUpdates()
                    }

                    override fun onError(videoId: String, throwable: Throwable) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = throwable.message ?: "播放失败",
                            isPlaying = false
                        )
                        stopProgressUpdates()
                    }

                    override fun onPlaybackEnded(videoId: String) {
                        _uiState.value = _uiState.value.copy(isPlaying = false)
                        stopProgressUpdates()
                    }
                })

                player.attach(playerView)
                player.prepare(source)
                player.play()

                _uiState.value = _uiState.value.copy(
                    currentVideoId = videoId,
                    isPlaying = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "播放器初始化失败",
                    isPlaying = false
                )
            }
        }
    }

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

    fun pause() {
        currentPlayer?.pause()
        _uiState.value = _uiState.value.copy(isPlaying = false)
    }

    fun resume() {
        currentPlayer?.play()
        _uiState.value = _uiState.value.copy(isPlaying = true)
    }

    fun setSpeed(speed: Float) {
        currentPlayer?.setSpeed(speed)
    }

    fun seekTo(positionMs: Long) {
        currentPlayer?.seekTo(positionMs)
        _uiState.value = _uiState.value.copy(currentPositionMs = positionMs)
    }
    
    fun getCurrentPosition(): Long {
        return currentPlayer?.player?.currentPosition ?: 0L
    }
    
    fun getDuration(): Long {
        return currentPlayer?.player?.duration?.takeIf { it > 0 } ?: 0L
    }
    
    private fun startProgressUpdates() {
        stopProgressUpdates()
        progressUpdateJob = viewModelScope.launch {
            while (currentPlayer != null) {
                kotlinx.coroutines.delay(500) // 每500ms更新一次
                currentPlayer?.let { player ->
                    val position = player.player.currentPosition
                    val duration = player.player.duration.takeIf { it > 0 } ?: 0L
                    _uiState.value = _uiState.value.copy(
                        currentPositionMs = position,
                        durationMs = duration
                    )
                }
            }
        }
    }
    
    private fun stopProgressUpdates() {
        progressUpdateJob?.cancel()
        progressUpdateJob = null
    }

    fun releaseCurrentPlayer() {
        stopProgressUpdates()
        currentVideoId?.let { videoId ->
            playerPool.release(videoId)
        }
        currentPlayer = null
        currentVideoId = null
        _uiState.value = _uiState.value.copy(
            currentVideoId = null,
            isPlaying = false,
            showPlaceholder = true,
            currentPositionMs = 0L,
            durationMs = 0L
        )
    }

    override fun onCleared() {
        super.onCleared()
        releaseCurrentPlayer()
    }
}

data class LandscapeVideoItemUiState(
    val currentVideoId: String? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val showPlaceholder: Boolean = true,
    val error: String? = null,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L
)

