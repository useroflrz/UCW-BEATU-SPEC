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
import kotlinx.coroutines.launch
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
    private val pageSize = 20
    
    init {
        // 初始化时加载视频列表
        loadVideoList()
    }

    /**
     * 加载视频列表（使用GetFeedUseCase）
     */
    private fun loadVideoList() {
        viewModelScope.launch {
            currentPage = 1
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            getFeedUseCase(currentPage, pageSize)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "加载失败"
                    )
                }
                .collect { result ->
                    when (result) {
                        is AppResult.Loading -> {
                            _uiState.value = _uiState.value.copy(isLoading = true)
                        }
                        is AppResult.Success -> {
                            val videos = result.data.map { it.toVideoItem() }
                            // 如果第一页数量不足 pageSize，可以直接认为后端数据已经加载完
                            val hasLoadedAll = videos.size < pageSize
                            android.util.Log.d("RecommendViewModel", "loadVideoList: Success, loaded ${videos.size} videos")
                            videos.forEachIndexed { index, video ->
                                android.util.Log.d("RecommendViewModel", "Video[$index]: id=${video.id}, url=${video.videoUrl}, title=${video.title}")
                            }
                            _uiState.value = _uiState.value.copy(
                                videoList = videos,
                                isLoading = false,
                                error = null,
                                hasLoadedAllFromBackend = hasLoadedAll
                            )
                        }
                        is AppResult.Error -> {
                            android.util.Log.e("RecommendViewModel", "loadVideoList: Error - ${result.message ?: result.throwable?.message}", result.throwable)
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = result.message ?: result.throwable?.message ?: "加载失败"
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
     */
    fun loadMoreVideos() {
        viewModelScope.launch {
            // 避免重复加载；如果已经确认后端没有更多数据，则直接走前端“无限循环”逻辑，不再请求
            if (_uiState.value.isLoading || _uiState.value.hasLoadedAllFromBackend) return@launch
            
            currentPage++
            getFeedUseCase(currentPage, pageSize)
                .catch { e ->
                    currentPage-- // 回退页码
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "加载更多失败"
                    )
                }
                .collect { result ->
                    when (result) {
                        is AppResult.Loading -> {
                            // 加载更多时不显示全局loading
                        }
                        is AppResult.Success -> {
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
                            currentPage-- // 回退页码
                            _uiState.value = _uiState.value.copy(
                                error = result.message ?: result.throwable.message ?: "加载更多失败"
                            )
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

