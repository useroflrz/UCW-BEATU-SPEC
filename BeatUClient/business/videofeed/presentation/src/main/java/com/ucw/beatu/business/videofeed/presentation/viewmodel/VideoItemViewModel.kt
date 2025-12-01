package com.ucw.beatu.business.videofeed.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.ucw.beatu.business.videofeed.domain.usecase.FavoriteVideoUseCase
import com.ucw.beatu.business.videofeed.domain.usecase.LikeVideoUseCase
import com.ucw.beatu.business.videofeed.domain.usecase.UnfavoriteVideoUseCase
import com.ucw.beatu.business.videofeed.domain.usecase.UnlikeVideoUseCase
import com.ucw.beatu.shared.common.result.AppResult
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
    val error: String? = null,
    // 互动状态
    val isLiked: Boolean = false,
    val isFavorited: Boolean = false,
    val likeCount: Long = 0L,
    val favoriteCount: Long = 0L,
    val isInteracting: Boolean = false // 是否正在执行互动操作
)

@HiltViewModel
class VideoItemViewModel @Inject constructor(
    application: Application,
    private val playerPool: VideoPlayerPool,
    private val playbackSessionStore: PlaybackSessionStore,
    private val likeVideoUseCase: LikeVideoUseCase,
    private val unlikeVideoUseCase: UnlikeVideoUseCase,
    private val favoriteVideoUseCase: FavoriteVideoUseCase,
    private val unfavoriteVideoUseCase: UnfavoriteVideoUseCase
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
                android.util.Log.d("VideoItemViewModel", "preparePlayer: videoId=$videoId, videoUrl=$videoUrl")
                // 检查参数有效性
                if (videoId.isBlank() || videoUrl.isBlank()) {
                    android.util.Log.e("VideoItemViewModel", "preparePlayer: 视频ID或URL为空 - videoId=$videoId, videoUrl=$videoUrl")
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

                android.util.Log.d("VideoItemViewModel", "preparePlayer: acquiring player for videoId=$videoId")
                val player = playerPool.acquire(videoId)
                currentPlayer = player
                currentVideoUrl = videoUrl
                android.util.Log.d("VideoItemViewModel", "preparePlayer: player acquired, attaching to PlayerView")

                // 添加监听器
                playerListener?.let { player.removeListener(it) }
                val listener = object : VideoPlayer.Listener {
                    override fun onReady(videoId: String) {
                        android.util.Log.d("VideoItemViewModel", "onReady: videoId=$videoId")
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
                        android.util.Log.d("VideoItemViewModel", "onPlaybackEnded: videoId=$videoId")
                        _uiState.value = _uiState.value.copy(
                            isPlaying = false
                        )
                    }
                }
                player.addListener(listener)
                playerListener = listener

                // 绑定播放器到 PlayerView
                android.util.Log.d("VideoItemViewModel", "preparePlayer: attaching player to PlayerView")
                player.attach(playerView)
                android.util.Log.d("VideoItemViewModel", "preparePlayer: attached player to PlayerView, playerView.player=${playerView.player}")

                val pendingSession = playbackSessionStore.consume(videoId)
                handoffInProgress = pendingSession != null
                if (pendingSession != null) {
                    android.util.Log.d("VideoItemViewModel", "preparePlayer: applying pending session for videoId=$videoId")
                    applyPlaybackSession(player, pendingSession)
                } else {
                    android.util.Log.d("VideoItemViewModel", "preparePlayer: preparing videoId=$videoId, url=$videoUrl (waiting for visibility)")
                    player.prepare(source)
                }

                _uiState.value = _uiState.value.copy(
                    currentVideoId = videoId,
                    isPlaying = handoffInProgress && pendingSession?.playWhenReady == true
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
     * 为图文内容准备仅音频播放的 BGM
     * 不绑定 PlayerView，只使用现有的 VideoPlayerPool 与 ExoPlayer 播放音频。
     */
    fun prepareAudioOnly(videoId: String, audioUrl: String) {
        viewModelScope.launch {
            try {
                if (videoId.isBlank() || audioUrl.isBlank()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "音频ID或URL为空",
                        isPlaying = false
                    )
                    return@launch
                }

                val source = VideoSource(
                    videoId = videoId,
                    url = audioUrl
                )

                val player = playerPool.acquire(videoId)
                currentPlayer = player
                currentVideoId = videoId
                currentVideoUrl = audioUrl

                playerListener?.let { player.removeListener(it) }
                val listener = object : VideoPlayer.Listener {
                    override fun onReady(videoId: String) {
                        // 仅音频场景下，依然使用 READY 状态更新 UI（此时没有画面，但有时长信息）
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            showPlaceholder = false,
                            isPlaying = true,
                            durationMs = player.player.duration.takeIf { it > 0 }
                                ?: _uiState.value.durationMs
                        )
                    }

                    override fun onError(videoId: String, throwable: Throwable) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = throwable.message ?: "BGM 播放失败",
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

                // 仅音频模式：不调用 attach(PlayerView)，直接 prepare + play，并设置单曲循环
                player.player.repeatMode = Player.REPEAT_MODE_ONE
                player.prepare(source)

                _uiState.value = _uiState.value.copy(
                    currentVideoId = videoId,
                    isPlaying = true,
                    isLoading = false,
                    showPlaceholder = false
                )
                startProgressUpdates()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "BGM 播放初始化失败",
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
        currentPlayer?.let {
            android.util.Log.d("VideoItemViewModel", "pause: videoId=${currentVideoId}")
            it.pause()
        }
        _uiState.value = _uiState.value.copy(isPlaying = false)
    }

    /**
     * 恢复播放
     */
    fun resume() {
        currentPlayer?.let {
            android.util.Log.d("VideoItemViewModel", "resume: videoId=${currentVideoId}, player=${it.player}")
            it.play()
        }
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
     * Seek 到指定位置（毫秒），供进度条拖动调用
     */
    fun seekTo(positionMs: Long) {
        val player = currentPlayer ?: return
        val safePosition = positionMs.coerceAtLeast(0L)
        player.seekTo(safePosition)
        _uiState.value = _uiState.value.copy(currentPositionMs = safePosition)
    }

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

    /**
     * 初始化互动状态（从VideoItem传入）
     */
    fun initInteractionState(
        isLiked: Boolean,
        isFavorited: Boolean,
        likeCount: Long,
        favoriteCount: Long
    ) {
        _uiState.value = _uiState.value.copy(
            isLiked = isLiked,
            isFavorited = isFavorited,
            likeCount = likeCount,
            favoriteCount = favoriteCount
        )
    }

    /**
     * 切换点赞状态
     */
    fun toggleLike() {
        val videoId = currentVideoId ?: return
        val currentState = _uiState.value
        
        // 乐观更新UI
        val newIsLiked = !currentState.isLiked
        val newLikeCount = if (newIsLiked) {
            currentState.likeCount + 1
        } else {
            (currentState.likeCount - 1).coerceAtLeast(0)
        }
        
        _uiState.value = _uiState.value.copy(
            isLiked = newIsLiked,
            likeCount = newLikeCount,
            isInteracting = true
        )
        
        viewModelScope.launch {
            val result = if (newIsLiked) {
                likeVideoUseCase(videoId)
            } else {
                unlikeVideoUseCase(videoId)
            }
            
            when (result) {
                is AppResult.Success<Unit> -> {
                    _uiState.value = _uiState.value.copy(isInteracting = false)
                }
                is AppResult.Error -> {
                    // 回滚乐观更新
                    _uiState.value = _uiState.value.copy(
                        isLiked = currentState.isLiked,
                        likeCount = currentState.likeCount,
                        isInteracting = false,
                        error = result.message ?: result.throwable.message ?: "操作失败"
                    )
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isInteracting = false)
                }
            }
        }
    }

    /**
     * 切换收藏状态
     */
    fun toggleFavorite() {
        val videoId = currentVideoId ?: return
        val currentState = _uiState.value
        
        // 乐观更新UI
        val newIsFavorited = !currentState.isFavorited
        val newFavoriteCount = if (newIsFavorited) {
            currentState.favoriteCount + 1
        } else {
            (currentState.favoriteCount - 1).coerceAtLeast(0)
        }
        
        _uiState.value = _uiState.value.copy(
            isFavorited = newIsFavorited,
            favoriteCount = newFavoriteCount,
            isInteracting = true
        )
        
        viewModelScope.launch {
            val result = if (newIsFavorited) {
                favoriteVideoUseCase(videoId)
            } else {
                unfavoriteVideoUseCase(videoId)
            }
            
            when (result) {
                is AppResult.Success<Unit> -> {
                    _uiState.value = _uiState.value.copy(isInteracting = false)
                }
                is AppResult.Error -> {
                    // 回滚乐观更新
                    _uiState.value = _uiState.value.copy(
                        isFavorited = currentState.isFavorited,
                        favoriteCount = currentState.favoriteCount,
                        isInteracting = false,
                        error = result.message ?: result.throwable.message ?: "操作失败"
                    )
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isInteracting = false)
                }
            }
        }
    }
}


