package com.ucw.beatu.business.videofeed.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ucw.beatu.business.videofeed.domain.usecase.GetFeedUseCase
import com.ucw.beatu.business.videofeed.presentation.mapper.toVideoItem
import com.ucw.beatu.business.videofeed.presentation.model.FeedContentType
import com.ucw.beatu.business.videofeed.presentation.model.VideoItem
import com.ucw.beatu.business.videofeed.presentation.model.VideoOrientation
import com.ucw.beatu.shared.common.result.AppResult
import com.ucw.beatu.shared.player.VideoPlayer
import com.ucw.beatu.shared.player.model.VideoSource
import com.ucw.beatu.shared.player.pool.VideoPlayerPool
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import javax.inject.Inject

data class RecommendUiState(
    val videoList: List<VideoItem> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    /**
     * 是否已经把后端的视频页全部加载完：
     * - true：不再向后端请求更多数据，前端在已有列表中做“无限循环”
     * - false：滑到尾部仍会继续向后端要下一页
     */
    val hasLoadedAllFromBackend: Boolean = false
)

@HiltViewModel
class RecommendViewModel @Inject constructor(
    application: Application,
    private val playerPool: VideoPlayerPool,
    private val getFeedUseCase: GetFeedUseCase
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(RecommendUiState())
    val uiState: StateFlow<RecommendUiState> = _uiState.asStateFlow()

    private var currentPlayer: VideoPlayer? = null
    private var currentVideoId: String? = null
    private var currentPage = 1
    // 从config.xml读取页面大小配置
    private val pageSize = application.resources.getInteger(com.ucw.beatu.R.integer.video_page_size)
    private var isLoadingMore = false // 防止重复加载更多
    private var loadMoreJob: Job? = null // 加载更多的Job，用于取消
    
    init {
        // 初始化时立即加载视频列表（会先显示本地缓存，然后后台更新）
        loadVideoList()
    }

    /**
     * 加载视频列表（使用GetFeedUseCase）
     * 优化：立即显示本地缓存，不等待网络请求，确保UI流畅
     */
    private fun loadVideoList() {
        viewModelScope.launch {
            currentPage = 1
            // 不立即设置isLoading，让本地缓存先显示
            _uiState.value = _uiState.value.copy(error = null)
            
            getFeedUseCase(currentPage, pageSize)
                .catch { e ->
                    // 即使出错，也保持当前显示的数据（可能是本地缓存或mock数据）
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "加载失败"
                    )
                }
                .collect { result ->
                    when (result) {
                        is AppResult.Loading -> {
                            // 只有在没有数据时才显示loading，避免闪烁
                            if (_uiState.value.videoList.isEmpty()) {
                                _uiState.value = _uiState.value.copy(isLoading = true)
                            }
                        }
                        is AppResult.Success -> {
                            val videos = result.data.map { it.toVideoItem() }
                            // 如果第一页数量不足 pageSize，可以直接认为后端数据已经加载完
                            val hasLoadedAll = videos.size < pageSize
                            android.util.Log.d("RecommendViewModel", "loadVideoList: Success, loaded ${videos.size} videos")
                            _uiState.value = _uiState.value.copy(
                                videoList = videos,
                                isLoading = false,
                                error = null,
                                hasLoadedAllFromBackend = hasLoadedAll
                            )
                        }
                        is AppResult.Error -> {
                            android.util.Log.e("RecommendViewModel", "loadVideoList: Error - ${result.message ?: result.throwable?.message}", result.throwable)
                            // 即使出错，也保持当前显示的数据，不阻塞UI
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                // 静默失败，不显示错误，因为已经有本地缓存或mock数据
                                error = if (_uiState.value.videoList.isEmpty()) {
                                    result.message ?: result.throwable?.message ?: "加载失败"
                                } else null
                            )
                        }
                    }
                }
        }
    }

    /**
     * 刷新视频列表（下拉刷新）
     */
    fun refreshVideoList() {
        viewModelScope.launch {
            currentPage = 1
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            
            getFeedUseCase(currentPage, pageSize)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        error = e.message ?: "刷新失败"
                    )
                }
                .collect { result ->
                    when (result) {
                        is AppResult.Loading -> {
                            // Loading状态已在开始时设置
                        }
                        is AppResult.Success -> {
                            val videos = result.data.map { it.toVideoItem() }
                            val hasLoadedAll = videos.size < pageSize
                            _uiState.value = _uiState.value.copy(
                                videoList = videos,
                                isRefreshing = false,
                                error = null,
                                hasLoadedAllFromBackend = hasLoadedAll
                            )
                        }
                        is AppResult.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isRefreshing = false,
                                error = result.message ?: result.throwable.message ?: "刷新失败"
                            )
                        }
                    }
                }
        }
    }

    /**
     * 加载更多视频（上拉加载）
     * 优化：异步加载，不阻塞UI滑动，即使失败也不影响用户体验
     */
    fun loadMoreVideos() {
        // 避免重复加载；如果已经确认后端没有更多数据，则直接走前端"无限循环"逻辑，不再请求
        if (isLoadingMore || _uiState.value.hasLoadedAllFromBackend) {
            return
        }
        
        // 取消之前的加载任务，避免重复请求
        loadMoreJob?.cancel()
        
        isLoadingMore = true
        loadMoreJob = viewModelScope.launch {
            val nextPage = currentPage + 1
            
            getFeedUseCase(nextPage, pageSize)
                .catch { e ->
                    // 加载失败时，静默失败，不影响用户滑动
                    isLoadingMore = false
                    android.util.Log.w("RecommendViewModel", "loadMoreVideos: Error - ${e.message}", e)
                }
                .collect { result ->
                    when (result) {
                        is AppResult.Loading -> {
                            // 加载更多时不显示全局loading，保持UI流畅
                        }
                        is AppResult.Success -> {
                            isLoadingMore = false
                            val moreVideos = result.data.map { it.toVideoItem() }
                            val currentList = _uiState.value.videoList.toMutableList()

                            // 如果此次没有再返回新数据，说明后端所有页已经加载完
                            if (moreVideos.isEmpty()) {
                                _uiState.value = _uiState.value.copy(
                                    videoList = currentList,
                                    error = null,
                                    hasLoadedAllFromBackend = true
                                )
                                return@collect
                            } else {
                                currentPage = nextPage // 更新页码
                                currentList.addAll(moreVideos)
                                val hasLoadedAll = moreVideos.size < pageSize
                                _uiState.value = _uiState.value.copy(
                                    videoList = currentList,
                                    error = null,
                                    hasLoadedAllFromBackend = hasLoadedAll
                                )
                            }
                        }
                        is AppResult.Error -> {
                            isLoadingMore = false
                            // 静默失败，不显示错误，不影响用户滑动
                            // 如果后端数据已加载完，标记为已加载完，启用无限循环
                            android.util.Log.w("RecommendViewModel", "loadMoreVideos: Error - ${result.message ?: result.throwable?.message}", result.throwable)
                            // 如果连续失败多次，可以考虑标记为已加载完，启用无限循环
                            // 这里暂时不处理，让用户继续尝试
                        }
                    }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        releaseCurrentPlayer()
    }
    
    private fun releaseCurrentPlayer() {
        currentVideoId?.let { videoId ->
            playerPool.release(videoId)
        }
        currentPlayer = null
        currentVideoId = null
    }


    fun restoreState(videos: List<VideoItem>, restoredPage: Int) {
        currentPage = restoredPage.coerceAtLeast(1)
        _uiState.value = _uiState.value.copy(
            videoList = videos,
            isLoading = false,
            isRefreshing = false,
            error = null
        )
    }

    fun getCurrentPage(): Int = currentPage

    /**
     * 推荐页中的静态图文+BGM，用于体验“图文+音乐”效果
     * index 用于区分出现在不同页的示例卡片，方便后续接入真实后端数据时替换
     */
    private fun createMockImagePost(index: Int): VideoItem {
        return VideoItem(
            id = "image_post_mock_$index",
            videoUrl = "",
            title = "这是一个图文+BGM 示例（第 $index 段）",
            authorName = "BeatU 官方",
            likeCount = 1314,
            commentCount = 99,
            favoriteCount = 520,
            shareCount = 66,
            orientation = VideoOrientation.PORTRAIT,
            type = FeedContentType.IMAGE_POST,
            imageUrls = listOf(
                "https://images.pexels.com/photos/572897/pexels-photo-572897.jpeg",
                "https://images.pexels.com/photos/210186/pexels-photo-210186.jpeg",
                "https://images.pexels.com/photos/1103970/pexels-photo-1103970.jpeg"
            ),
            bgmUrl = "https://samplelib.com/lib/preview/mp3/sample-6s.mp3"
        )
    }
}

