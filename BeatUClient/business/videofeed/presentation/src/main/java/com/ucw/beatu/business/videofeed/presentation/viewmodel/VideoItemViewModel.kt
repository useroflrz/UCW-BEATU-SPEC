package com.ucw.beatu.business.videofeed.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.ucw.beatu.business.videofeed.domain.usecase.FavoriteVideoUseCase
import com.ucw.beatu.business.videofeed.domain.usecase.LikeVideoUseCase
import com.ucw.beatu.business.videofeed.domain.usecase.SaveWatchHistoryUseCase
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
    val currentVideoId: Long? = null,  // ✅ 修改：从 String? 改为 Long?
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
    private val shareVideoUseCase: ShareVideoUseCase,
    private val saveWatchHistoryUseCase: SaveWatchHistoryUseCase
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(VideoItemUiState())
    val uiState: StateFlow<VideoItemUiState> = _uiState.asStateFlow()

    private var currentPlayer: VideoPlayer? = null
    private var currentVideoId: Long? = null  // ✅ 修改：从 String? 改为 Long?
    private var currentVideoUrl: String? = null
    private var progressJob: Job? = null
    private var playerListener: VideoPlayer.Listener? = null
    private var exoPlayerListener: Player.Listener? = null
    private var handoffInProgress = false
    private var pendingSession: PlaybackSession? = null // 保存会话信息，在 onReady 中使用

    /**
     * 播放视频
     */
    fun playVideo(videoId: Long, videoUrl: String) {  // ✅ 修改：从 String 改为 Long
        viewModelScope.launch {
            // 如果已经在播放同一个视频，不重复播放
            if (currentVideoId == videoId && currentPlayer != null) {
                return@launch
            }

            // ✅ 修复：如果是从横屏返回（handoffInProgress），且播放器已经存在且是同一个视频，不释放播放器
            // 因为播放器需要被复用，不应该被释放
            val shouldRelease = !(handoffInProgress && currentPlayer != null && currentVideoId == videoId)
            if (shouldRelease) {
                // 释放之前的播放器
                releaseCurrentPlayer()
            } else {
                android.util.Log.d("VideoItemViewModel", "playVideo: 从横屏返回，跳过释放播放器，videoId=$videoId")
            }

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
    fun preparePlayer(videoId: Long, videoUrl: String, playerView: PlayerView) {  // ✅ 修改：从 String 改为 Long
        viewModelScope.launch {
            try {
                android.util.Log.d("VideoItemViewModel", "preparePlayer: 视频ID=$videoId，视频URL=$videoUrl")
                // 检查参数有效性
                if (videoId <= 0 || videoUrl.isBlank()) {  // ✅ 修改：videoId 现在是 Long，检查 <= 0 而不是 isBlank()
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
                val currentTag = currentMediaItem?.localConfiguration?.tag as? Long  // ✅ 修改：tag 现在是 Long
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
                    override fun onReady(videoId: Long) {  // ✅ 修改：从 String 改为 Long
                        android.util.Log.d("VideoItemViewModel", "onReady: 视频ID=$videoId，播放状态=${player.player.playbackState}，是否准备播放=${player.player.playWhenReady}，当前位置=${player.player.currentPosition}ms")
                        
                        // ✅ 修复：如果是从横屏返回的，再次确认位置（因为可能在 prepare 过程中位置发生了变化）
                        if (handoffInProgress && pendingSession != null) {
                            val session = pendingSession!!
                            val currentPosition = player.player.currentPosition
                            val targetPosition = session.positionMs
                            // ✅ 修复：降低位置差异阈值，从500ms改为50ms，确保更精确的时间同步
                            val positionDiff = kotlin.math.abs(currentPosition - targetPosition)
                            if (positionDiff > 50) {
                                android.util.Log.d("VideoItemViewModel", "onReady: 从横屏返回，位置不匹配（当前=${currentPosition}ms，目标=${targetPosition}ms，差异=${positionDiff}ms），重新跳转")
                                player.seekTo(targetPosition)
                                // ✅ 修复：确保跳转后立即更新UI状态
                                _uiState.value = _uiState.value.copy(currentPositionMs = targetPosition)
                                
                                // ✅ 修复：等待一小段时间后再次检查位置，确保跳转成功
                                viewModelScope.launch {
                                    kotlinx.coroutines.delay(50)
                                    val newPosition = player.player.currentPosition
                                    val newDiff = kotlin.math.abs(newPosition - targetPosition)
                                    if (newDiff > 50) {
                                        android.util.Log.w("VideoItemViewModel", "onReady: 跳转后位置仍不匹配（当前=${newPosition}ms，目标=${targetPosition}ms，差异=${newDiff}ms），再次跳转")
                                        player.seekTo(targetPosition)
                                        _uiState.value = _uiState.value.copy(currentPositionMs = targetPosition)
                                    } else {
                                        android.util.Log.d("VideoItemViewModel", "onReady: 跳转成功，位置已匹配（当前=${newPosition}ms，目标=${targetPosition}ms）")
                                    }
                                }
                            } else {
                                android.util.Log.d("VideoItemViewModel", "onReady: 从横屏返回，位置匹配（当前=${currentPosition}ms，目标=${targetPosition}ms，差异=${positionDiff}ms）")
                            }
                            
                            // ✅ 修复：根据会话的 playWhenReady 状态决定是否自动播放
                            // 从横屏返回竖屏时，如果会话中 playWhenReady=true，应该自动播放
                            if (session.playWhenReady) {
                                android.util.Log.d("VideoItemViewModel", "onReady: 从横屏返回，会话中 playWhenReady=true，自动恢复播放")
                                // 等待 Surface 准备好后再播放
                                viewModelScope.launch {
                                    // 检查视频尺寸，如果有尺寸说明 Surface 已准备好
                                    if (player.player.videoSize.width > 0 && player.player.videoSize.height > 0) {
                                        android.util.Log.d("VideoItemViewModel", "onReady: Surface 已准备好，立即播放")
                                        player.play()
                                    } else {
                                        // 等待首帧渲染
                                        var surfaceReady = false
                                        val surfaceListener = object : Player.Listener {
                                            override fun onRenderedFirstFrame() {
                                                if (!surfaceReady) {
                                                    surfaceReady = true
                                                    android.util.Log.d("VideoItemViewModel", "onReady: Surface 准备好，首帧已渲染，开始播放")
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
                                            android.util.Log.d("VideoItemViewModel", "onReady: 300ms后检测到视频尺寸，开始播放")
                                            player.player.removeListener(surfaceListener)
                                            player.play()
                                        }
                                    }
                                }
                            } else {
                                android.util.Log.d("VideoItemViewModel", "onReady: 从横屏返回，会话中 playWhenReady=false，保持暂停状态")
                                player.pause()
                            }
                            
                            // 清除会话信息（已经使用完毕）
                            pendingSession = null
                            handoffInProgress = false
                        }
                        
                        // 同步播放状态：只有当 playWhenReady=true 且 playbackState=READY 时才认为正在播放
                        val isActuallyPlaying = player.player.playWhenReady && player.player.playbackState == Player.STATE_READY
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            showPlaceholder = false,
                            isPlaying = isActuallyPlaying,
                            durationMs = player.player.duration.takeIf { it > 0 } ?: _uiState.value.durationMs
                        )
                        android.util.Log.d("VideoItemViewModel", "onReady: 已更新 isPlaying=$isActuallyPlaying，当前位置=${player.player.currentPosition}ms")
                        
                        // ✅ 观看历史异步写入：用户点击开始观看视频时，按照文档的异步写入数据库，上传远程
                        // 策略B：不回滚（弱一致性数据，自动重试同步）
                        val currentPosition = player.player.currentPosition
                        viewModelScope.launch {
                            try {
                                saveWatchHistoryUseCase(videoId, currentPosition)
                                android.util.Log.d("VideoItemViewModel", "保存观看历史: videoId=$videoId, position=$currentPosition")
                            } catch (e: Exception) {
                                android.util.Log.e("VideoItemViewModel", "保存观看历史失败: videoId=$videoId", e)
                                // 策略B：不回滚，保留待同步状态，下次启动时继续重试
                            }
                        }
                    }

                    override fun onError(videoId: Long, throwable: Throwable) {  // ✅ 修改：从 String 改为 Long
                        android.util.Log.e("VideoItemViewModel", "播放器错误，视频ID=$videoId", throwable)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = throwable.message ?: "播放失败",
                            isPlaying = false
                        )
                    }

                    override fun onPlaybackEnded(videoId: Long) {  // ✅ 修改：从 String 改为 Long
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

                // ✅ 修复：检查播放会话，如果存在则恢复，否则正常准备
                // ✅ 修复：先 peek 检查会话是否存在，添加详细日志
                val sessionBeforeConsume = playbackSessionStore.peek(videoId)
                android.util.Log.d("VideoItemViewModel", "preparePlayer: 检查播放会话，videoId=$videoId，会话存在=${sessionBeforeConsume != null}，会话位置=${sessionBeforeConsume?.positionMs ?: 0}ms")
                
                val session = playbackSessionStore.consume(videoId)
                handoffInProgress = session != null
                if (session != null) {
                    android.util.Log.d("VideoItemViewModel", "preparePlayer: ✅ 找到播放会话，正在应用，视频ID=$videoId，位置=${session.positionMs}ms，是否准备播放=${session.playWhenReady}，倍速=${session.speed}")
                    // ✅ 修复：保存会话信息，以便在 onReady 中使用（因为会话已经被 consume 了）
                    pendingSession = session
                    applyPlaybackSession(player, session)
                } else {
                    android.util.Log.w("VideoItemViewModel", "preparePlayer: ❌ 未找到播放会话，videoId=$videoId，可能原因：1)会话未保存 2)videoId不匹配 3)会话已被消费")
                    android.util.Log.d("VideoItemViewModel", "preparePlayer: 无播放会话，正常准备视频，视频ID=$videoId，URL=$videoUrl（等待可见性）")
                    pendingSession = null
                    player.prepare(source)
                }

                _uiState.value = _uiState.value.copy(
                    currentVideoId = videoId,
                    isPlaying = handoffInProgress && session?.playWhenReady == true
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
    fun prepareAudioOnly(videoId: Long, audioUrl: String) {  // ✅ 修改：从 String 改为 Long
        viewModelScope.launch {
            try {
                if (videoId <= 0 || audioUrl.isBlank()) {  // ✅ 修改：videoId 现在是 Long，检查 <= 0 而不是 isBlank()
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
                    override fun onReady(videoId: Long) {  // ✅ 修改：从 String 改为 Long
                        // 仅音频场景下，依然使用 READY 状态更新 UI（此时没有画面，但有时长信息）
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            showPlaceholder = false,
                            isPlaying = true,
                            durationMs = player.player.duration.takeIf { it > 0 }
                                ?: _uiState.value.durationMs
                        )
                    }

                    override fun onError(videoId: Long, throwable: Throwable) {  // ✅ 修改：从 String 改为 Long
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = throwable.message ?: "BGM 播放失败",
                            isPlaying = false
                        )
                    }

                    override fun onPlaybackEnded(videoId: Long) {  // ✅ 修改：从 String 改为 Long
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
        // ✅ 修复：优先使用 currentVideoId，如果为空则使用 _uiState.value.currentVideoId
        // 因为 playVideo() 是异步的，可能 currentVideoId 变量还没设置，但 _uiState 已经更新了
        val videoId = currentVideoId ?: _uiState.value.currentVideoId ?: run {
            android.util.Log.w("VideoItemViewModel", "onHostResume: 当前视频ID为空，currentVideoId=$currentVideoId, uiState.currentVideoId=${_uiState.value.currentVideoId}")
            return
        }
        
        // ✅ 修复：如果 currentVideoId 变量为空但 _uiState 中有值，需要等待 playVideo 完成
        // 或者从播放会话中获取 videoUrl（如果有会话的话）
        if (currentVideoId == null && _uiState.value.currentVideoId != null) {
            // 先尝试从播放会话中获取 videoUrl
            val session = playbackSessionStore.peek(videoId)
            if (session != null) {
                currentVideoId = videoId
                currentVideoUrl = session.videoUrl
                android.util.Log.d("VideoItemViewModel", "onHostResume: 从播放会话同步 currentVideoId=$currentVideoId, currentVideoUrl=$currentVideoUrl")
            } else {
                // 如果没有会话，等待 playVideo 完成（延迟一小段时间）
                android.util.Log.w("VideoItemViewModel", "onHostResume: currentVideoId 为空且无播放会话，等待 playVideo 完成")
                // 这里不直接返回，而是继续执行，因为后续逻辑会处理
            }
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

        // ✅ 修复：添加详细日志，检查会话是否存在
        val sessionBeforeConsume = playbackSessionStore.peek(videoId)
        android.util.Log.d("VideoItemViewModel", "onHostResume: 检查播放会话，videoId=$videoId，会话存在=${sessionBeforeConsume != null}，会话位置=${sessionBeforeConsume?.positionMs ?: 0}ms")
        
        playbackSessionStore.consume(videoId)?.let { session ->
            android.util.Log.d("VideoItemViewModel", "onHostResume: ✅ 找到播放会话，视频ID=${session.videoId}，位置=${session.positionMs}ms，是否准备播放=${session.playWhenReady}，倍速=${session.speed}")
            // ✅ 修复：保存会话信息，以便在 onReady 中使用（因为会话已经被 consume 了）
            pendingSession = session
            handoffInProgress = true
            applyPlaybackSession(player, session)
        } ?: run {
            // 没有会话，清除标志
            android.util.Log.w("VideoItemViewModel", "onHostResume: ❌ 未找到播放会话，videoId=$videoId，可能原因：1)会话未保存 2)videoId不匹配 3)会话已被消费")
            pendingSession = null
            handoffInProgress = false
            // 如果没有 session，根据当前状态决定是否播放
            // ✅ 修复：即使没有会话，也要使用 Surface 检测机制来恢复播放
            val shouldPlay = _uiState.value.isPlaying
            android.util.Log.d("VideoItemViewModel", "onHostResume: 无会话，是否应该播放=$shouldPlay，播放状态=${player.player.playbackState}")
            if (shouldPlay && player.player.playbackState == Player.STATE_READY) {
                // 检查 Surface 是否准备好
                val hasVideoSize = player.player.videoSize.width > 0 && player.player.videoSize.height > 0
                if (!hasVideoSize) {
                    // 没有视频尺寸，Surface 可能还没准备好，等待首帧渲染
                    android.util.Log.d("VideoItemViewModel", "onHostResume: 播放器已准备好但未检测到视频尺寸，等待 Surface 初始化")
                    
                    var surfaceReadyHandled = false
                    val surfaceReadyListener = object : Player.Listener {
                        override fun onRenderedFirstFrame() {
                            if (!surfaceReadyHandled) {
                                surfaceReadyHandled = true
                                android.util.Log.d("VideoItemViewModel", "onHostResume: Surface 准备好，首帧已渲染，开始播放")
                                player.player.removeListener(this)
                                player.play()
                            }
                        }
                    }
                    player.player.addListener(surfaceReadyListener)
                    
                    // 延迟检查
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(300)
                        if (!surfaceReadyHandled) {
                            if (player.player.videoSize.width > 0 && player.player.videoSize.height > 0) {
                                surfaceReadyHandled = true
                                android.util.Log.d("VideoItemViewModel", "onHostResume: 检测到视频尺寸，Surface 可能已准备好，开始播放")
                                player.player.removeListener(surfaceReadyListener)
                                player.play()
                            } else {
                                // 强制播放（避免一直等待）
                                android.util.Log.w("VideoItemViewModel", "onHostResume: 300ms后仍未检测到视频尺寸，强制播放")
                                surfaceReadyHandled = true
                                player.player.removeListener(surfaceReadyListener)
                                player.play()
                            }
                        }
                    }
                } else {
                    // 已经有视频尺寸，Surface 可能已准备好，直接播放
                    player.play()
                }
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
    
    /**
     * 查看播放会话（不消费），用于调试和检查
     */
    fun peekPlaybackSession(videoId: Long): PlaybackSession? {
        return playbackSessionStore.peek(videoId)
    }

    fun snapshotPlayback(): PlaybackSession? {
        val videoId = currentVideoId ?: return null
        val videoUrl = currentVideoUrl ?: return null
        val player = currentPlayer ?: return null
        val mediaPlayer = player.player
        
        // ✅ 修复：确保获取最新的播放进度
        // currentPosition 是实时的，但为了确保准确性，我们使用它
        val currentPosition = mediaPlayer.currentPosition
        val speed = mediaPlayer.playbackParameters.speed
        val playWhenReady = mediaPlayer.playWhenReady
        
        android.util.Log.d("VideoItemViewModel", "snapshotPlayback: 保存播放会话，视频ID=$videoId，位置=${currentPosition}ms，倍速=$speed，是否准备播放=$playWhenReady")
        
        return PlaybackSession(
            videoId = videoId,
            videoUrl = videoUrl,
            positionMs = currentPosition,
            speed = speed,
            playWhenReady = playWhenReady
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
        val currentTag = currentMediaItem?.localConfiguration?.tag as? Long  // ✅ 修改：tag 现在是 Long
        val needsPrepare = if (currentTag != null && currentTag != session.videoId) {
            android.util.Log.w("VideoItemViewModel", "applyPlaybackSession: 播放器当前播放的视频ID=$currentTag 与会话视频ID=${session.videoId} 不匹配，需要重新准备")
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
            android.util.Log.d("VideoItemViewModel", "applyPlaybackSession: 播放器已有正确的媒体项，先暂停确保状态正确")
            player.pause()
        }
        
        // ✅ 修复：设置倍速（在 prepare 和 seekTo 之前设置，确保跳转时使用正确的倍速）
        player.setSpeed(session.speed)
        
        if (needsPrepare) {
            android.util.Log.d("VideoItemViewModel", "applyPlaybackSession: 正在准备新的媒体项")
            player.prepare(VideoSource(session.videoId, session.videoUrl))
        }
        
        // ✅ 修复：确保在应用会话时，先 seek 到正确的位置
        // ExoPlayer 的 seekTo 可以在 prepare 之前调用，会在准备好后自动跳转
        // 如果播放器已经准备好，立即跳转；否则等待 onReady 时再跳转
        val currentPosition = player.player.currentPosition
        val targetPosition = session.positionMs
        val positionDiff = kotlin.math.abs(currentPosition - targetPosition)
        
        android.util.Log.d("VideoItemViewModel", "applyPlaybackSession: 当前位置=${currentPosition}ms，目标位置=${targetPosition}ms，差异=${positionDiff}ms，播放状态=${player.player.playbackState}")
        
        // ✅ 修复：无论位置差异大小，都执行 seekTo，确保位置正确恢复
        // 因为从横屏返回时，播放器可能还在播放其他视频，位置可能不准确
        if (positionDiff > 50) {  // 降低阈值，从100ms改为50ms，确保更精确的时间同步
            android.util.Log.d("VideoItemViewModel", "applyPlaybackSession: 位置差异较大，跳转到位置=${targetPosition}ms")
            player.seekTo(targetPosition)
            // ✅ 修复：立即更新UI状态，确保进度条显示正确
            _uiState.value = _uiState.value.copy(currentPositionMs = targetPosition)
        } else {
            android.util.Log.d("VideoItemViewModel", "applyPlaybackSession: 位置差异较小（${positionDiff}ms），但仍执行 seekTo 确保位置准确")
            // ✅ 修复：即使位置差异较小，也执行 seekTo，确保位置准确
            player.seekTo(targetPosition)
            _uiState.value = _uiState.value.copy(currentPositionMs = targetPosition)
        }
        
        // ✅ 修复：优化 Surface 准备检测，减少延迟
        // 如果播放器已经准备好了，但 Surface 可能还没准备好（从横屏返回竖屏时）
        // 需要等待 Surface 准备好后再开始播放
        if (player.player.playbackState == Player.STATE_READY) {
            android.util.Log.d("VideoItemViewModel", "applyPlaybackSession: 播放器已准备好，检查 Surface 状态")
            
            // 先检查是否已经有首帧了（从横屏切换回来时可能已经渲染过）
            if (player.player.videoSize.width > 0 && player.player.videoSize.height > 0) {
                android.util.Log.d("VideoItemViewModel", "applyPlaybackSession: 检测到视频尺寸，Surface 已准备好，立即恢复播放")
                // Surface 已经准备好，直接恢复播放
                if (session.playWhenReady) {
                    player.play()
                } else {
                    player.pause()
                }
            } else {
                // Surface 还没准备好，等待首帧渲染
                android.util.Log.d("VideoItemViewModel", "applyPlaybackSession: Surface 未准备好，等待首帧渲染")
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
                
                // ✅ 修复：减少延迟检查时间，从300ms改为100ms，加快恢复速度
                viewModelScope.launch {
                    kotlinx.coroutines.delay(100) // 减少延迟，加快恢复
                    // 检查是否已经有首帧了（通过检查视频尺寸）
                    if (!surfaceReadyHandled) {
                        if (player.player.videoSize.width > 0 && player.player.videoSize.height > 0) {
                            surfaceReadyHandled = true
                            android.util.Log.d("VideoItemViewModel", "applyPlaybackSession: 100ms后检测到视频尺寸，Surface 已准备好，恢复播放")
                            player.player.removeListener(surfaceReadyListener)
                            if (session.playWhenReady) {
                                player.play()
                            }
                        } else {
                            // 如果100ms后还没有首帧，再等待一段时间（最多再等200ms）
                            android.util.Log.d("VideoItemViewModel", "applyPlaybackSession: 100ms后仍未检测到视频尺寸，继续等待首帧渲染")
                            kotlinx.coroutines.delay(200)
                            if (!surfaceReadyHandled && player.player.videoSize.width > 0 && player.player.videoSize.height > 0) {
                                surfaceReadyHandled = true
                                android.util.Log.d("VideoItemViewModel", "applyPlaybackSession: 300ms后检测到视频尺寸，恢复播放")
                                player.player.removeListener(surfaceReadyListener)
                                if (session.playWhenReady) {
                                    player.play()
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // 播放器还没准备好，会在 onReady 中处理
            android.util.Log.d("VideoItemViewModel", "applyPlaybackSession: 播放器未准备好，等待 onReady 后再恢复播放")
            // 如果播放器还没准备好，先设置播放状态（会在 onReady 中根据会话状态恢复）
            if (session.playWhenReady) {
                // 播放器准备好后会自动播放（因为 seekTo 和 prepare 已经调用）
            } else {
                // 如果会话中 playWhenReady = false，确保播放器准备好后不会自动播放
                player.pause()
            }
        }
        
        // 同步状态：只有当播放器准备好且 playWhenReady=true 时才认为正在播放
        val isActuallyPlaying = session.playWhenReady && player.player.playbackState == Player.STATE_READY
        _uiState.value = _uiState.value.copy(
            isLoading = needsPrepare, // 如果需要准备，显示加载状态
            showPlaceholder = false,
            isPlaying = isActuallyPlaying,
            currentPositionMs = session.positionMs,
            currentSpeed = session.speed
        )
        android.util.Log.d("VideoItemViewModel", "applyPlaybackSession: 已更新 isPlaying=$isActuallyPlaying, isLoading=$needsPrepare, currentPositionMs=${session.positionMs}ms")
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
        videoId: Long,  // ✅ 修改：从 String 改为 Long
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
     * 设置播放倍速
     */
    fun setSpeed(speed: Float) {
        currentPlayer?.setSpeed(speed)
        _uiState.value = _uiState.value.copy(currentSpeed = speed)
        android.util.Log.d("VideoItemViewModel", "setSpeed: 设置倍速=$speed")
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


