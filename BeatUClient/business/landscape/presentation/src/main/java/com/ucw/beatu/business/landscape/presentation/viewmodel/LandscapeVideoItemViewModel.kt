package com.ucw.beatu.business.landscape.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.ucw.beatu.business.landscape.presentation.model.VideoItem
import com.ucw.beatu.business.videofeed.domain.usecase.FavoriteVideoUseCase
import com.ucw.beatu.business.videofeed.domain.usecase.LikeVideoUseCase
import com.ucw.beatu.business.videofeed.domain.usecase.ShareVideoUseCase
import com.ucw.beatu.business.videofeed.domain.usecase.UnfavoriteVideoUseCase
import com.ucw.beatu.business.videofeed.domain.usecase.UnlikeVideoUseCase
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
    private val playbackSessionStore: PlaybackSessionStore,
    private val likeVideoUseCase: LikeVideoUseCase,
    private val unlikeVideoUseCase: UnlikeVideoUseCase,
    private val favoriteVideoUseCase: FavoriteVideoUseCase,
    private val unfavoriteVideoUseCase: UnfavoriteVideoUseCase,
    private val shareVideoUseCase: ShareVideoUseCase
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(LandscapeVideoItemUiState())
    val uiState: StateFlow<LandscapeVideoItemUiState> = _uiState.asStateFlow()

    private val _controlsState = MutableStateFlow(LandscapeControlsState())
    val controlsState: StateFlow<LandscapeControlsState> = _controlsState.asStateFlow()

    private var currentPlayer: VideoPlayer? = null
    private var currentVideoId: Long? = null  // ✅ 修改：从 String? 改为 Long?
    private var currentVideoUrl: String? = null

    private val startUpStopwatch = Stopwatch()

    private var progressUpdateJob: Job? = null
    private var playerListener: VideoPlayer.Listener? = null
    private var handoffFromPortrait = false
    private var pendingSession: PlaybackSession? = null // 保存会话信息，在 onReady 中使用

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

    fun playVideo(videoId: Long, videoUrl: String) {  // ✅ 修改：从 String 改为 Long
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

    fun preparePlayer(videoId: Long, videoUrl: String, playerView: PlayerView) {  // ✅ 修改：从 String 改为 Long
        viewModelScope.launch {
            try {
                if (videoId <= 0 || videoUrl.isBlank()) {  // ✅ 修改：videoId 现在是 Long，检查 <= 0 而不是 isBlank()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "视频ID或URL为空",
                        isPlaying = false
                    )
                    return@launch
                }

                val source = VideoSource(videoId = videoId, url = videoUrl)
                AppLogger.d(TAG, "preparePlayer: 正在获取播放器，视频ID=$videoId")
                val player = playerPool.acquire(videoId)
                currentPlayer = player
                currentVideoUrl = videoUrl
                AppLogger.d(TAG, "preparePlayer: 播放器已获取，正在检查内容匹配")
                
                // ✅ 添加：检查播放器当前的内容是否匹配新的 videoId
                val currentMediaItem = player.player.currentMediaItem
                val currentTag = currentMediaItem?.localConfiguration?.tag as? Long  // ✅ 修改：tag 现在是 Long
                if (currentTag != null && currentTag != videoId) {
                    AppLogger.w(TAG, "preparePlayer: 播放器当前播放的视频ID=$currentTag 与目标视频ID=$videoId 不匹配，需要重新准备")
                    // 停止当前播放并清除内容
                    player.pause()
                    player.player.stop()
                    player.player.clearMediaItems()
                } else {
                    AppLogger.d(TAG, "preparePlayer: 播放器内容检查通过，当前视频ID=${currentTag ?: "null"}，目标视频ID=$videoId")
                }
                
                startUpStopwatch.start()

                playerListener?.let { player.removeListener(it) }
                val listener = object : VideoPlayer.Listener {
                    override fun onReady(videoId: Long) {  // ✅ 修改：从 String 改为 Long
                        val startUp = startUpStopwatch.elapsedMillis()
                        
                        // ✅ 修复：如果是从竖屏切换过来的，需要确保位置正确，并根据会话状态决定是否播放
                        val session = pendingSession
                        var shouldAutoPlay = false
                        
                        if (handoffFromPortrait && session != null) {
                            // 从竖屏切换过来，确保位置正确
                            AppLogger.d(TAG, "onReady: 从竖屏切换过来，再次确认位置=${session.positionMs}ms，会话中 playWhenReady=${session.playWhenReady}")
                            // 再次确认位置（因为可能在 prepare 过程中位置发生了变化）
                            player.seekTo(session.positionMs)
                            
                            // ✅ 修复：从竖屏切换到横屏时，如果会话中 playWhenReady=true，应该自动播放
                            // 注意：这里不能直接播放，因为 Fragment 的可见性可能还没设置
                            // 但是，如果 Fragment 已经可见，应该自动播放
                            shouldAutoPlay = session.playWhenReady
                            AppLogger.d(TAG, "onReady: 从竖屏切换过来，播放器已准备好，会话中 playWhenReady=${session.playWhenReady}，shouldAutoPlay=$shouldAutoPlay")
                        }
                        
                        // 清除会话信息（已经使用完毕）
                        pendingSession = null
                        
                        // ✅ 修复：如果应该自动播放，等待一小段时间让 Fragment 设置可见性，然后播放
                        // 否则保持暂停状态，等待 Fragment 调用 resume()
                        if (shouldAutoPlay) {
                            // 延迟一小段时间，让 Fragment 的 onParentVisibilityChanged 先执行
                            viewModelScope.launch {
                                kotlinx.coroutines.delay(100)
                                // 检查视频尺寸，如果有尺寸说明 Surface 已准备好
                                if (player.player.videoSize.width > 0 && player.player.videoSize.height > 0) {
                                    AppLogger.d(TAG, "onReady: Surface 已准备好，自动播放")
                                    player.play()
                                } else {
                                    // 等待首帧渲染
                                    var surfaceReady = false
                                    val surfaceListener = object : Player.Listener {
                                        override fun onRenderedFirstFrame() {
                                            if (!surfaceReady) {
                                                surfaceReady = true
                                                AppLogger.d(TAG, "onReady: Surface 准备好，首帧已渲染，自动播放")
                                                player.player.removeListener(this)
                                                player.play()
                                            }
                                        }
                                    }
                                    player.player.addListener(surfaceListener)
                                    
                                    // 延迟检查，如果 300ms 后还没有首帧，也尝试播放
                                    kotlinx.coroutines.delay(300)
                                    if (!surfaceReady && player.player.videoSize.width > 0) {
                                        surfaceReady = true
                                        AppLogger.d(TAG, "onReady: 300ms后检测到视频尺寸，自动播放")
                                        player.player.removeListener(surfaceListener)
                                        player.play()
                                    }
                                }
                            }
                        } else {
                            // 保持暂停状态，等待 Fragment 调用 resume()
                            player.pause()
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            showPlaceholder = false,
                            isPlaying = shouldAutoPlay, // ✅ 修复：根据会话状态设置初始播放状态
                            durationMs = player.player.duration.takeIf { it > 0 } ?: 0L,
                            startUpTimeMs = startUp
                        )
                        AppLogger.d(TAG, "Landscape video ready startUp=${startUp}ms, shouldAutoPlay=$shouldAutoPlay，currentPosition=${player.player.currentPosition}ms")
                        
                        startProgressUpdates()
                    }

                    override fun onError(videoId: Long, throwable: Throwable) {  // ✅ 修改：从 String 改为 Long
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = throwable.message ?: "播放失败",
                            isPlaying = false
                        )
                        stopProgressUpdates()
                    }

                    override fun onPlaybackEnded(videoId: Long) {  // ✅ 修改：从 String 改为 Long
                        _uiState.value = _uiState.value.copy(isPlaying = false)
                        stopProgressUpdates()
                    }
                }
                player.addListener(listener)
                playerListener = listener

                player.attach(playerView)
                val session = playbackSessionStore.consume(videoId)
                handoffFromPortrait = session != null
                pendingSession = session // 保存会话信息，在 onReady 中使用
                if (session != null) {
                    AppLogger.d(TAG, "preparePlayer: 检测到播放会话，视频ID=${session.videoId}，位置=${session.positionMs}ms，倍速=${session.speed}，是否准备播放=${session.playWhenReady}")
                    applyPlaybackSession(player, session)
                } else {
                    AppLogger.d(TAG, "preparePlayer: 未检测到播放会话，准备播放器但不自动播放，视频ID=$videoId")
                    player.prepare(source)
                    // ✅ 修复：不要在准备时直接播放，让 Fragment 根据可见性决定是否播放
                    player.pause()
                }

                _uiState.value = _uiState.value.copy(
                    currentVideoId = videoId,
                    isPlaying = false // ✅ 修复：初始状态设为暂停，等待可见时再播放
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
        val videoId = _uiState.value.currentVideoId ?: return
        val prev = _controlsState.value
        val targetLiked = !prev.isLiked
        val optimisticCount = (prev.likeCount + if (targetLiked) 1 else -1).coerceAtLeast(0)
        _controlsState.value = prev.copy(isLiked = targetLiked, likeCount = optimisticCount)

        viewModelScope.launch {
            val result = if (targetLiked) {
                likeVideoUseCase(videoId)
            } else {
                unlikeVideoUseCase(videoId)
            }
            if (result is com.ucw.beatu.shared.common.result.AppResult.Error) {
                // 失败回滚
                _controlsState.value = prev
            }
        }
    }

    fun toggleFavorite() {
        val videoId = _uiState.value.currentVideoId ?: return
        val prev = _controlsState.value
        val targetFavorited = !prev.isFavorited
        val optimisticCount = (prev.favoriteCount + if (targetFavorited) 1 else -1).coerceAtLeast(0)
        _controlsState.value = prev.copy(isFavorited = targetFavorited, favoriteCount = optimisticCount)

        viewModelScope.launch {
            val result = if (targetFavorited) {
                favoriteVideoUseCase(videoId)
            } else {
                unfavoriteVideoUseCase(videoId)
            }
            if (result is com.ucw.beatu.shared.common.result.AppResult.Error) {
                _controlsState.value = prev
            }
        }
    }

    /**
     * 上报一次分享（横屏使用）
     */
    fun reportShare() {
        val videoId = _uiState.value.currentVideoId ?: return
        viewModelScope.launch {
            // 忽略结果，仅用于统计；失败时不影响前端 UI
            runCatching { shareVideoUseCase(videoId) }
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
        
        // ✅ 修复：确保获取最新的播放进度
        val currentPosition = mediaPlayer.currentPosition
        val speed = mediaPlayer.playbackParameters.speed
        val playWhenReady = mediaPlayer.playWhenReady
        
        AppLogger.d(TAG, "snapshotPlayback: 保存播放会话，视频ID=$videoId，位置=${currentPosition}ms，倍速=$speed，是否准备播放=$playWhenReady")
        
        return PlaybackSession(
            videoId = videoId,
            videoUrl = videoUrl,
            positionMs = currentPosition,
            speed = speed,
            playWhenReady = playWhenReady
        )
    }

    fun mediaPlayer(): Player? = currentPlayer?.player

    private fun applyPlaybackSession(player: VideoPlayer, session: PlaybackSession) {
        AppLogger.d(TAG, "applyPlaybackSession: 开始应用播放会话，视频ID=${session.videoId}，位置=${session.positionMs}ms，倍速=${session.speed}，是否准备播放=${session.playWhenReady}")
        
        // ✅ 添加：检查播放器当前的内容是否匹配会话的视频ID
        val currentMediaItem = player.player.currentMediaItem
        val currentTag = currentMediaItem?.localConfiguration?.tag as? Long  // ✅ 修改：tag 现在是 Long
        val needsPrepare = if (currentTag != null && currentTag != session.videoId) {
            AppLogger.w(TAG, "applyPlaybackSession: 播放器当前播放的视频ID=$currentTag 与会话视频ID=${session.videoId} 不匹配，需要重新准备")
            // 停止当前播放并清除内容
            player.pause()
            player.player.stop()
            player.player.clearMediaItems()
            true
        } else {
            currentMediaItem == null
        }
        
        // ✅ 修复：如果播放器已经有正确的媒体项，先暂停确保状态正确
        if (!needsPrepare && currentMediaItem != null) {
            AppLogger.d(TAG, "applyPlaybackSession: 播放器已有正确的媒体项，先暂停确保状态正确")
            player.pause()
        }
        
        if (needsPrepare) {
            AppLogger.d(TAG, "applyPlaybackSession: 播放器没有媒体项，准备新的媒体项")
            player.prepare(VideoSource(session.videoId, session.videoUrl))
        }
        
        // ✅ 修复：设置倍速（在 seekTo 之前设置，确保跳转时使用正确的倍速）
        player.setSpeed(session.speed)
        
        // ✅ 修复：确保在应用会话时，先 seek 到正确的位置
        // ExoPlayer 的 seekTo 可以在 prepare 之前调用，会在准备好后自动跳转
        AppLogger.d(TAG, "applyPlaybackSession: 跳转到位置=${session.positionMs}ms")
        player.seekTo(session.positionMs)
        
        // ✅ 修复：根据会话的 playWhenReady 状态和播放器状态决定是否播放
        // 如果播放器已经准备好，根据会话状态决定是否播放
        // 如果播放器还没准备好，会在 onReady 中处理
        val shouldPlay = if (player.player.playbackState == Player.STATE_READY) {
            // 播放器已准备好，检查 Surface 是否准备好
            val hasVideoSize = player.player.videoSize.width > 0 && player.player.videoSize.height > 0
            if (hasVideoSize && session.playWhenReady) {
                AppLogger.d(TAG, "applyPlaybackSession: 播放器已准备好，Surface 已准备好，会话中 playWhenReady=true，开始播放")
                player.play()
                true
            } else {
                AppLogger.d(TAG, "applyPlaybackSession: 播放器已准备好，但 Surface 未准备好或会话中 playWhenReady=false，保持暂停")
                player.pause()
                false
            }
        } else {
            // 播放器还没准备好，先暂停，等待 onReady 后由 Fragment 根据可见性决定
            AppLogger.d(TAG, "applyPlaybackSession: 播放器未准备好，保持暂停状态，等待 onReady 后由 Fragment 决定是否播放")
            player.pause()
            false
        }
        
        // 更新 UI 状态
        _uiState.value = _uiState.value.copy(
            isLoading = needsPrepare,
            showPlaceholder = false,
            isPlaying = shouldPlay,
            currentPositionMs = session.positionMs,
            durationMs = player.player.duration.takeIf { it > 0 } ?: _uiState.value.durationMs
        )
        
        AppLogger.d(TAG, "applyPlaybackSession: 播放会话已应用，当前播放位置=${player.player.currentPosition}ms，播放状态=${player.player.playbackState}，是否播放=$shouldPlay")
    }

    fun isHandoffFromPortrait(): Boolean = handoffFromPortrait

    companion object {
        private const val TAG = "LandscapeItemVM"
    }
}

data class LandscapeVideoItemUiState(
    val currentVideoId: Long? = null,  // ✅ 修改：从 String? 改为 Long?
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

