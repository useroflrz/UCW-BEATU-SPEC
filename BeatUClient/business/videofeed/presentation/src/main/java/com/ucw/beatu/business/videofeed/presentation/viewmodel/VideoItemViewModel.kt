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
    private val playbackSessionStore: PlaybackSessionStore
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(VideoItemUiState())
    val uiState: StateFlow<VideoItemUiState> = _uiState.asStateFlow()

    private var currentPlayer: VideoPlayer? = null
    private var currentVideoId: String? = null
    private var currentVideoUrl: String? = null
    private var progressJob: Job? = null
    private var playerListener: VideoPlayer.Listener? = null
    private var exoPlayerListener: Player.Listener? = null
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
                android.util.Log.d("VideoItemViewModel", "preparePlayer: 视频ID=$videoId，视频URL=$videoUrl")
                // 检查参数有效性
                if (videoId.isBlank() || videoUrl.isBlank()) {
                    android.util.Log.e("VideoItemViewModel", "preparePlayer: 视频ID或URL为空，视频ID=$videoId，视频URL=$videoUrl")
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

                android.util.Log.d("VideoItemViewModel", "preparePlayer: 正在获取播放器，视频ID=$videoId")
                val player = playerPool.acquire(videoId)
                currentPlayer = player
                currentVideoUrl = videoUrl
                android.util.Log.d("VideoItemViewModel", "preparePlayer: 播放器已获取，正在绑定到 PlayerView")

                // 添加监听器
                playerListener?.let { player.removeListener(it) }
                val listener = object : VideoPlayer.Listener {
                    override fun onReady(videoId: String) {
                        android.util.Log.d("VideoItemViewModel", "onReady: 视频ID=$videoId，播放状态=${player.player.playbackState}，是否准备播放=${player.player.playWhenReady}")
                        // 同步播放状态：只有当 playWhenReady=true 且 playbackState=READY 时才认为正在播放
                        val isActuallyPlaying = player.player.playWhenReady && player.player.playbackState == Player.STATE_READY
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            showPlaceholder = false,
                            isPlaying = isActuallyPlaying,
                            durationMs = player.player.duration.takeIf { it > 0 } ?: _uiState.value.durationMs
                        )
                        android.util.Log.d("VideoItemViewModel", "onReady: 已更新 isPlaying=$isActuallyPlaying")
                    }

                    override fun onError(videoId: String, throwable: Throwable) {
                        android.util.Log.e("VideoItemViewModel", "播放器错误，视频ID=$videoId", throwable)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = throwable.message ?: "播放失败",
                            isPlaying = false
                        )
                    }

                    override fun onPlaybackEnded(videoId: String) {
                        android.util.Log.d("VideoItemViewModel", "onPlaybackEnded: 视频ID=$videoId")
                        _uiState.value = _uiState.value.copy(
                            isPlaying = false
                        )
                    }
                }
                player.addListener(listener)
                playerListener = listener
                
                // 移除之前的 ExoPlayer 监听器
                exoPlayerListener?.let { player.player.removeListener(it) }
                
                // 添加 ExoPlayer 状态监听，确保 isPlaying 与 playWhenReady 同步
                val exoListener = object : Player.Listener {
                    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                        android.util.Log.d("VideoItemViewModel", "onPlayWhenReadyChanged: 是否准备播放=$playWhenReady，原因=$reason，播放状态=${player.player.playbackState}")
                        val isActuallyPlaying = playWhenReady && player.player.playbackState == Player.STATE_READY
                        if (_uiState.value.isPlaying != isActuallyPlaying) {
                            android.util.Log.d("VideoItemViewModel", "onPlayWhenReadyChanged: 正在同步 isPlaying，从 ${_uiState.value.isPlaying} 到 $isActuallyPlaying")
                            _uiState.value = _uiState.value.copy(isPlaying = isActuallyPlaying)
                        }
                    }
                    
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        android.util.Log.d("VideoItemViewModel", "onPlaybackStateChanged: 播放状态=$playbackState，是否准备播放=${player.player.playWhenReady}")
                        val isActuallyPlaying = player.player.playWhenReady && playbackState == Player.STATE_READY
                        if (_uiState.value.isPlaying != isActuallyPlaying && playbackState == Player.STATE_READY) {
                            android.util.Log.d("VideoItemViewModel", "onPlaybackStateChanged: 正在同步 isPlaying，从 ${_uiState.value.isPlaying} 到 $isActuallyPlaying")
                            _uiState.value = _uiState.value.copy(isPlaying = isActuallyPlaying)
                        }
                    }
                }
                player.player.addListener(exoListener)
                exoPlayerListener = exoListener

                // 绑定播放器到 PlayerView
                android.util.Log.d("VideoItemViewModel", "preparePlayer: 正在将播放器绑定到 PlayerView")
                player.attach(playerView)
                android.util.Log.d("VideoItemViewModel", "preparePlayer: 播放器已绑定到 PlayerView，playerView.player=${playerView.player}")

                val pendingSession = playbackSessionStore.consume(videoId)
                handoffInProgress = pendingSession != null
                if (pendingSession != null) {
                    android.util.Log.d("VideoItemViewModel", "preparePlayer: 正在应用待处理的会话，视频ID=$videoId")
                    applyPlaybackSession(player, pendingSession)
                } else {
                    android.util.Log.d("VideoItemViewModel", "preparePlayer: 正在准备视频，视频ID=$videoId，URL=$videoUrl（等待可见性）")
                    player.prepare(source)
                }

                _uiState.value = _uiState.value.copy(
                    currentVideoId = videoId,
                    isPlaying = handoffInProgress && pendingSession?.playWhenReady == true
                )
                startProgressUpdates()

            } catch (e: Exception) {
                android.util.Log.e("VideoItemViewModel", "准备播放器时出错", e)
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
            val currentIsPlaying = _uiState.value.isPlaying
            android.util.Log.d("VideoItemViewModel", "togglePlayPause: 当前是否正在播放=$currentIsPlaying，播放状态=${player.player.playbackState}，是否准备播放=${player.player.playWhenReady}")
            if (currentIsPlaying) {
                player.pause()
                _uiState.value = _uiState.value.copy(isPlaying = false)
            } else {
                player.play()
                // 同步状态：只有当播放器准备好时才设置 isPlaying=true
                val isActuallyPlaying = player.player.playWhenReady && player.player.playbackState == Player.STATE_READY
                _uiState.value = _uiState.value.copy(isPlaying = isActuallyPlaying)
                android.util.Log.d("VideoItemViewModel", "togglePlayPause: 已更新 isPlaying=$isActuallyPlaying")
            }
        }
    }

    /**
     * 暂停播放
     */
    fun pause() {
        currentPlayer?.let {
            android.util.Log.d("VideoItemViewModel", "pause: 视频ID=${currentVideoId}")
            it.pause()
        }
        _uiState.value = _uiState.value.copy(isPlaying = false)
    }

    /**
     * 恢复播放
     */
    fun resume() {
        currentPlayer?.let { player ->
            android.util.Log.d("VideoItemViewModel", "resume: 视频ID=${currentVideoId}，播放状态=${player.player.playbackState}，是否准备播放=${player.player.playWhenReady}")
            player.play()
            // 同步状态：只有当播放器真正准备好时才设置 isPlaying=true
            val isActuallyPlaying = player.player.playWhenReady && player.player.playbackState == Player.STATE_READY
            _uiState.value = _uiState.value.copy(isPlaying = isActuallyPlaying)
            android.util.Log.d("VideoItemViewModel", "resume: 已更新 isPlaying=$isActuallyPlaying")
        } ?: run {
            android.util.Log.w("VideoItemViewModel", "resume: 当前播放器为空")
            _uiState.value = _uiState.value.copy(isPlaying = false)
        }
    }

    /**
     * 将当前播放器重新绑定到新的 PlayerView（例如横竖屏切回）。
     */
    fun onHostResume(targetView: PlayerView?) {
        if (targetView == null) {
            android.util.Log.w("VideoItemViewModel", "onHostResume: 目标视图为空")
            return
        }
        val videoId = currentVideoId ?: run {
            android.util.Log.w("VideoItemViewModel", "onHostResume: 当前视频ID为空")
            return
        }
        
        android.util.Log.d("VideoItemViewModel", "onHostResume: 视频ID=$videoId，targetView.player=${targetView.player}")
        
        val player = currentPlayer ?: playerPool.acquire(videoId).also { 
            currentPlayer = it
            android.util.Log.d("VideoItemViewModel", "onHostResume: 从池中获取新播放器")
        }
        
        // 检查播放器是否已经 attach 到其他 PlayerView
        if (targetView.player != null && targetView.player !== player.player) {
            android.util.Log.d("VideoItemViewModel", "onHostResume: 正在从目标视图分离之前的播放器")
            targetView.player = null
        }
        
        // Attach 播放器到新的 PlayerView
        android.util.Log.d("VideoItemViewModel", "onHostResume: 正在将播放器绑定到目标视图，播放状态=${player.player.playbackState}，是否准备播放=${player.player.playWhenReady}")
        player.attach(targetView)
        android.util.Log.d("VideoItemViewModel", "onHostResume: 播放器已绑定，targetView.player=${targetView.player}")

        playbackSessionStore.consume(videoId)?.let { session ->
            android.util.Log.d("VideoItemViewModel", "onHostResume: 正在应用会话，位置=${session.positionMs}ms，是否准备播放=${session.playWhenReady}")
            applyPlaybackSession(player, session)
            handoffInProgress = false
        } ?: run {
            // 如果没有 session，根据当前状态决定是否播放
            val shouldPlay = _uiState.value.isPlaying
            android.util.Log.d("VideoItemViewModel", "onHostResume: 无会话，是否应该播放=$shouldPlay，播放状态=${player.player.playbackState}")
            if (shouldPlay && player.player.playbackState == Player.STATE_READY) {
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
        
        exoPlayerListener?.let { listener ->
            currentPlayer?.player?.removeListener(listener)
        }
        exoPlayerListener = null
        
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
        android.util.Log.d("VideoItemViewModel", "applyPlaybackSession: 视频ID=${session.videoId}，位置=${session.positionMs}ms，是否准备播放=${session.playWhenReady}，播放状态=${player.player.playbackState}")
        if (player.player.currentMediaItem == null) {
            android.util.Log.d("VideoItemViewModel", "applyPlaybackSession: 正在准备新的媒体项")
            player.prepare(VideoSource(session.videoId, session.videoUrl))
        }
        player.seekTo(session.positionMs)
        player.setSpeed(session.speed)
        if (session.playWhenReady) {
            player.play()
        } else {
            player.pause()
        }
        // 同步状态：只有当播放器准备好且 playWhenReady=true 时才认为正在播放
        val isActuallyPlaying = session.playWhenReady && player.player.playbackState == Player.STATE_READY
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            showPlaceholder = false,
            isPlaying = isActuallyPlaying,
            currentPositionMs = session.positionMs,
            currentSpeed = session.speed
        )
        android.util.Log.d("VideoItemViewModel", "applyPlaybackSession: 已更新 isPlaying=$isActuallyPlaying")
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
        val currentState = _uiState.value
        val newIsLiked = !currentState.isLiked
        val newLikeCount = if (newIsLiked) {
            currentState.likeCount + 1
        } else {
            (currentState.likeCount - 1).coerceAtLeast(0)
        }
        _uiState.value = currentState.copy(
            isLiked = newIsLiked,
            likeCount = newLikeCount,
            isInteracting = false,
            error = null
        )
    }

    /**
     * 切换收藏状态
     */
    fun toggleFavorite() {
        val currentState = _uiState.value
        val newIsFavorited = !currentState.isFavorited
        val newFavoriteCount = if (newIsFavorited) {
            currentState.favoriteCount + 1
        } else {
            (currentState.favoriteCount - 1).coerceAtLeast(0)
        }
        _uiState.value = currentState.copy(
            isFavorited = newIsFavorited,
            favoriteCount = newFavoriteCount,
            isInteracting = false,
            error = null
        )
    }
}


