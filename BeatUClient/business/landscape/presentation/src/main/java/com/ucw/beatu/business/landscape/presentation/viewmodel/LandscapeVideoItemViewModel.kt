package com.ucw.beatu.business.landscape.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.ucw.beatu.business.landscape.presentation.model.VideoItem
import com.ucw.beatu.shared.common.logger.AppLogger
import com.ucw.beatu.shared.common.time.Stopwatch
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 横屏单个视频项 ViewModel
 * 复用竖屏的播放器生命周期管理逻辑
 */
@HiltViewModel
class LandscapeVideoItemViewModel @Inject constructor(
    application: Application,
    private val playerPool: VideoPlayerPool,
    private val playbackSessionStore: PlaybackSessionStore
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(LandscapeVideoItemUiState())
    val uiState: StateFlow<LandscapeVideoItemUiState> = _uiState.asStateFlow()

    private val _controlsState = MutableStateFlow(LandscapeControlsState())
    val controlsState: StateFlow<LandscapeControlsState> = _controlsState.asStateFlow()

    private var currentPlayer: VideoPlayer? = null
    private var currentVideoId: String? = null
    private var currentVideoUrl: String? = null

    private val startUpStopwatch = Stopwatch()

    private var progressUpdateJob: Job? = null
    private var playerListener: VideoPlayer.Listener? = null
    private var handoffFromPortrait = false

    fun bindVideoMeta(videoItem: VideoItem) {
        _controlsState.value = LandscapeControlsState(
            isLocked = false,
            currentSpeed = videoItem.defaultSpeed,
            currentQualityLabel = videoItem.defaultQuality,
            isLiked = videoItem.isLiked,
            isFavorited = videoItem.isFavorited,
            likeCount = videoItem.likeCount,
            favoriteCount = videoItem.favoriteCount
        )
        if (videoItem.defaultSpeed != 1.0f) {
            setSpeed(videoItem.defaultSpeed)
        }
    }

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
            currentVideoUrl = videoUrl
        }
    }

    fun preparePlayer(videoId: String, videoUrl: String, playerView: PlayerView) {
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
                currentVideoUrl = videoUrl
                startUpStopwatch.start()

                playerListener?.let { player.removeListener(it) }
                val listener = object : VideoPlayer.Listener {
                    override fun onReady(videoId: String) {
                        val startUp = startUpStopwatch.elapsedMillis()
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            showPlaceholder = false,
                            isPlaying = true,
                            durationMs = player.player.duration.takeIf { it > 0 } ?: 0L,
                            startUpTimeMs = startUp
                        )
                        AppLogger.d(TAG, "Landscape video ready startUp=${startUp}ms")
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
                }
                player.addListener(listener)
                playerListener = listener

                player.attach(playerView)
                val pendingSession = playbackSessionStore.consume(videoId)
                handoffFromPortrait = pendingSession != null
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

    fun toggleLike() {
        _controlsState.update { state ->
            val newLiked = !state.isLiked
            val newCount = (state.likeCount + if (newLiked) 1 else -1).coerceAtLeast(0)
            state.copy(isLiked = newLiked, likeCount = newCount)
        }
    }

    fun toggleFavorite() {
        _controlsState.update { state ->
            val newFavorited = !state.isFavorited
            val newCount = (state.favoriteCount + if (newFavorited) 1 else -1).coerceAtLeast(0)
            state.copy(isFavorited = newFavorited, favoriteCount = newCount)
        }
    }

    fun cycleSpeed() {
        val options = listOf(1.0f, 1.25f, 1.5f, 2.0f)
        val currentIndex = options.indexOf(_controlsState.value.currentSpeed).takeIf { it >= 0 } ?: 0
        val nextSpeed = options[(currentIndex + 1) % options.size]
        setSpeed(nextSpeed)
    }

    fun cycleQuality() {
        val options = listOf("自动", "高清", "标清")
        val current = _controlsState.value.currentQualityLabel
        val index = options.indexOf(current).takeIf { it >= 0 } ?: 0
        val nextLabel = options[(index + 1) % options.size]
        _controlsState.update { it.copy(currentQualityLabel = nextLabel) }
    }

    fun lockControls() {
        _controlsState.update { it.copy(isLocked = true) }
    }

    fun unlockControls() {
        _controlsState.update { it.copy(isLocked = false) }
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
        _controlsState.update { it.copy(currentSpeed = speed) }
    }

    fun seekTo(positionMs: Long) {
        currentPlayer?.seekTo(positionMs)
        _uiState.value = _uiState.value.copy(currentPositionMs = positionMs)
    }

    fun getCurrentPosition(): Long = currentPlayer?.player?.currentPosition ?: 0L

    fun getDuration(): Long = currentPlayer?.player?.duration?.takeIf { it > 0 } ?: 0L

    private fun startProgressUpdates() {
        stopProgressUpdates()
        progressUpdateJob = viewModelScope.launch {
            while (currentPlayer != null) {
                delay(500)
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

    fun releaseCurrentPlayer(force: Boolean = true) {
        stopProgressUpdates()
        playerListener?.let { listener ->
            currentPlayer?.removeListener(listener)
        }
        playerListener = null

        if (!force && handoffFromPortrait) {
            currentPlayer = null
            currentVideoId = null
            currentVideoUrl = null
            handoffFromPortrait = false
            return
        }

        currentVideoId?.let { videoId ->
            playerPool.release(videoId)
        }
        currentPlayer = null
        currentVideoId = null
        currentVideoUrl = null
        handoffFromPortrait = false
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

    fun persistPlaybackSession(): PlaybackSession? {
        val session = snapshotPlayback() ?: return null
        playbackSessionStore.save(session)
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

    fun mediaPlayer(): Player? = currentPlayer?.player

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
            durationMs = player.player.duration.takeIf { it > 0 } ?: _uiState.value.durationMs
        )
    }

    fun isHandoffFromPortrait(): Boolean = handoffFromPortrait

    companion object {
        private const val TAG = "LandscapeItemVM"
    }
}

data class LandscapeVideoItemUiState(
    val currentVideoId: String? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val showPlaceholder: Boolean = true,
    val error: String? = null,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val startUpTimeMs: Long = 0L
)

data class LandscapeControlsState(
    val isLocked: Boolean = false,
    val currentSpeed: Float = 1.0f,
    val currentQualityLabel: String = "自动",
    val isLiked: Boolean = false,
    val isFavorited: Boolean = false,
    val likeCount: Int = 0,
    val favoriteCount: Int = 0
)

