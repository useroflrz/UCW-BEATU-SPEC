package com.ucw.beatu.business.videofeed.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.ucw.beatu.business.videofeed.domain.usecase.FavoriteVideoUseCase
import com.ucw.beatu.business.videofeed.domain.usecase.LikeVideoUseCase
import com.ucw.beatu.business.videofeed.domain.usecase.UnfavoriteVideoUseCase
import com.ucw.beatu.business.videofeed.domain.usecase.ShareVideoUseCase
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
    private val unfavoriteVideoUseCase: UnfavoriteVideoUseCase,
    private val shareVideoUseCase: ShareVideoUseCase
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

                // 先设置 currentVideoId，避免其他逻辑检查失败
                // 从横屏返回时，currentVideoId 可能为 null
                currentVideoId = videoId
                currentVideoUrl = videoUrl

                val source = VideoSource(
                    videoId = videoId,
                    url = videoUrl
                )

                android.util.Log.d("VideoItemViewModel", "preparePlayer: 正在获取播放器，视频ID=$videoId")
                val player = playerPool.acquire(videoId)
                currentPlayer = player
                
                // 检查播放器当前的内容是否匹配新的 videoId
                // 如果从 availablePlayers 中获取的播放器可能还在播放其他视频
                val currentMediaItem = player.player.currentMediaItem
                val currentTag = currentMediaItem?.localConfiguration?.tag as? String
                if (currentTag != null && currentTag != videoId) {
                    android.util.Log.w("VideoItemViewModel", "preparePlayer: 播放器当前播放的视频ID=$currentTag 与目标视频ID=$videoId 不匹配，需要重新准备")
                    // 停止当前播放并清除内容
                    player.pause()
                    player.player.stop()
                    player.player.clearMediaItems()
                }
                
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
                // 如果 PlayerView 已经绑定了其他播放器，先解绑
                if (playerView.player != null && playerView.player !== player.player) {
                    android.util.Log.d("VideoItemViewModel", "preparePlayer: PlayerView 已绑定其他播放器，先解绑")
                    playerView.player = null
                }
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
        
        // 检查播放器当前的内容是否匹配会话的视频ID
        val currentMediaItem = player.player.currentMediaItem
        val currentTag = currentMediaItem?.localConfiguration?.tag as? String
        if (currentTag != null && currentTag != session.videoId) {
            android.util.Log.w("VideoItemViewModel", "applyPlaybackSession: 播放器当前播放的视频ID=$currentTag 与会话视频ID=${session.videoId} 不匹配，需要重新准备")
            // 停止当前播放并清除内容
            player.pause()
            player.player.stop()
            player.player.clearMediaItems()
        }
        
        if (player.player.currentMediaItem == null) {
            android.util.Log.d("VideoItemViewModel", "applyPlaybackSession: 正在准备新的媒体项")
            player.prepare(VideoSource(session.videoId, session.videoUrl))
        }
        player.seekTo(session.positionMs)
        player.setSpeed(session.speed)
        
        // 如果播放器已经准备好了，但 Surface 可能还没准备好（从横屏返回竖屏时）
        // 需要等待 Surface 准备好后再开始播放
        if (player.player.playbackState == Player.STATE_READY) {
            android.util.Log.d("VideoItemViewModel", "applyPlaybackSession: 播放器已准备好，等待 Surface 初始化后再播放")
            // 先暂停，等待 Surface 准备好
            player.pause()
            
            // 使用标记变量来跟踪是否已经处理过，避免在闭包中修改 listener 变量
            var surfaceReadyHandled = false
            
            // 添加临时监听器，等待首帧渲染后再播放（确保 Surface 已准备好）
            val surfaceReadyListener = object : Player.Listener {
                override fun onRenderedFirstFrame() {
                    if (!surfaceReadyHandled) {
                        surfaceReadyHandled = true
                        android.util.Log.d("VideoItemViewModel", "applyPlaybackSession: Surface 准备好，首帧已渲染，恢复播放")
                        player.player.removeListener(this)
                        if (session.playWhenReady) {
                            player.play()
                        }
                    }
                }
            }
            player.player.addListener(surfaceReadyListener)
            
            // 如果已经有首帧了（从横屏切换回来时可能已经渲染过），延迟检查
            viewModelScope.launch {
                kotlinx.coroutines.delay(300) // 给 Surface 更多时间初始化
                // 检查是否已经有首帧了（通过检查视频尺寸）
                if (!surfaceReadyHandled) {
                    if (player.player.videoSize.width > 0 && player.player.videoSize.height > 0) {
                        surfaceReadyHandled = true
                        android.util.Log.d("VideoItemViewModel", "applyPlaybackSession: 检测到视频尺寸，Surface 可能已准备好，恢复播放")
                        player.player.removeListener(surfaceReadyListener)
                        if (session.playWhenReady) {
                            player.play()
                        }
                    } else {
                        // 如果300ms后还没有首帧，再等待一段时间
                        android.util.Log.d("VideoItemViewModel", "applyPlaybackSession: 300ms后仍未检测到视频尺寸，继续等待首帧渲染")
                    }
                }
            }
        } else {
            // 播放器还没准备好，直接设置播放状态
            if (session.playWhenReady) {
                player.play()
            } else {
                player.pause()
            }
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
     * 初始化互动状态（从 VideoItem 传入）
     * 同时记录当前 videoId，保证在播放器尚未准备时也能执行点赞/收藏操作。
     */
    fun initInteractionState(
        videoId: String,
        isLiked: Boolean,
        isFavorited: Boolean,
        likeCount: Long,
        favoriteCount: Long
    ) {
        _uiState.value = _uiState.value.copy(
            currentVideoId = videoId,
            isLiked = isLiked,
            isFavorited = isFavorited,
            likeCount = likeCount,
            favoriteCount = favoriteCount
        )
    }

    /**
     * 上报一次分享行为（不负责拉起系统分享）
     */
    fun reportShare() {
        val videoId = _uiState.value.currentVideoId ?: return
        viewModelScope.launch {
            when (val result = shareVideoUseCase(videoId)) {
                is AppResult.Success -> {
                    // 暂时不处理 UI 计数，由后端返回的列表刷新 shareCount
                }
                is AppResult.Error -> {
                    android.util.Log.e("VideoItemViewModel", "shareVideo failed, videoId=$videoId", result.throwable)
                }
                else -> Unit
            }
        }
    }

    /**
     * 切换点赞状态
     */
    fun toggleLike() {
        val videoId = _uiState.value.currentVideoId ?: return
        val prevState = _uiState.value
        val targetLiked = !prevState.isLiked
        val optimisticCount = if (targetLiked) {
            prevState.likeCount + 1
        } else {
            (prevState.likeCount - 1).coerceAtLeast(0)
        }

        // 本地乐观更新
        _uiState.value = prevState.copy(
            isLiked = targetLiked,
            likeCount = optimisticCount,
            isInteracting = true,
            error = null
        )

        viewModelScope.launch {
            val result = if (targetLiked) {
                android.util.Log.d("VideoItemViewModel", "toggleLike: 调用 likeVideoUseCase, videoId=$videoId")
                likeVideoUseCase(videoId)
            } else {
                android.util.Log.d("VideoItemViewModel", "toggleLike: 调用 unlikeVideoUseCase, videoId=$videoId")
                unlikeVideoUseCase(videoId)
            }

            _uiState.value = when (result) {
                is AppResult.Success -> {
                    android.util.Log.d("VideoItemViewModel", "toggleLike: 成功, videoId=$videoId")
                    // 后端成功：只结束交互状态，保留乐观状态
                    _uiState.value.copy(isInteracting = false)
                }
                is AppResult.Error -> {
                    android.util.Log.e("VideoItemViewModel", "toggleLike: 失败, videoId=$videoId", result.throwable)
                    // 失败：保留当前乐观状态，只记录错误，交由 UI 以 Toast 提示
                    _uiState.value.copy(
                        isInteracting = false,
                        error = result.throwable.message ?: "点赞失败，请稍后重试"
                    )
                }
                is AppResult.Loading -> _uiState.value
            }
        }
    }

    /**
     * 切换收藏状态
     */
    fun toggleFavorite() {
        val videoId = _uiState.value.currentVideoId ?: return
        val prevState = _uiState.value
        val targetFavorited = !prevState.isFavorited
        val optimisticCount = if (targetFavorited) {
            prevState.favoriteCount + 1
        } else {
            (prevState.favoriteCount - 1).coerceAtLeast(0)
        }

        // 本地乐观更新
        _uiState.value = prevState.copy(
            isFavorited = targetFavorited,
            favoriteCount = optimisticCount,
            isInteracting = true,
            error = null
        )

        viewModelScope.launch {
            val result = if (targetFavorited) {
                android.util.Log.d("VideoItemViewModel", "toggleFavorite: 调用 favoriteVideoUseCase, videoId=$videoId")
                favoriteVideoUseCase(videoId)
            } else {
                android.util.Log.d("VideoItemViewModel", "toggleFavorite: 调用 unfavoriteVideoUseCase, videoId=$videoId")
                unfavoriteVideoUseCase(videoId)
            }

            _uiState.value = when (result) {
                is AppResult.Success -> {
                    android.util.Log.d("VideoItemViewModel", "toggleFavorite: 成功, videoId=$videoId")
                    _uiState.value.copy(isInteracting = false)
                }
                is AppResult.Error -> {
                    android.util.Log.e("VideoItemViewModel", "toggleFavorite: 失败, videoId=$videoId", result.throwable)
                    _uiState.value.copy(
                        isInteracting = false,
                        error = result.throwable.message ?: "收藏失败，请稍后重试"
                    )
                }
                is AppResult.Loading -> _uiState.value
            }
        }
    }
}


