package com.ucw.beatu.business.videofeed.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ucw.beatu.business.videofeed.presentation.model.VideoItem
import com.ucw.beatu.shared.common.mock.MockVideoCatalog
import com.ucw.beatu.shared.common.mock.MockVideoCatalog.Orientation.PORTRAIT
import com.ucw.beatu.shared.common.mock.Video
import com.ucw.beatu.shared.player.VideoPlayer
import com.ucw.beatu.shared.player.model.VideoSource
import com.ucw.beatu.shared.player.pool.VideoPlayerPool
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 推荐页 ViewModel
 * 管理视频列表、播放器生命周期和状态
 */
data class RecommendUiState(
    val videoList: List<VideoItem> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class RecommendViewModel @Inject constructor(
    application: Application,
    private val playerPool: VideoPlayerPool
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(RecommendUiState())
    val uiState: StateFlow<RecommendUiState> = _uiState.asStateFlow()

    private var currentPlayer: VideoPlayer? = null
    private var currentVideoId: String? = null
    private var currentPage = 1
    private val pageSize = 5
    
    init {
        // 初始化时加载视频列表
        loadVideoList()
    }

    /**
     * 加载视频列表（硬编码测试数据）
     */
    private fun loadVideoList() {
        viewModelScope.launch {
            currentPage = 1
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                delay(500) // 模拟网络请求延迟
                
                val videos = MockVideoCatalog.getPage(PORTRAIT, currentPage, pageSize)
                    .map { it.toVideoItem() }
                _uiState.value = _uiState.value.copy(
                    videoList = videos,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "加载失败"
                )
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
            
            try {
                delay(1000) // 模拟网络请求延迟
                
                val newVideos = MockVideoCatalog.getPage(PORTRAIT, currentPage, pageSize)
                    .map { it.toVideoItem() }
                _uiState.value = _uiState.value.copy(
                    videoList = newVideos,
                    isRefreshing = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    error = e.message ?: "刷新失败"
                )
            }
        }
    }

    /**
     * 加载更多视频（上拉加载）
     */
    fun loadMoreVideos() {
        viewModelScope.launch {
            try {
                delay(500) // 模拟网络请求延迟
                
                currentPage++
                val moreVideos = MockVideoCatalog.getPage(PORTRAIT, currentPage, pageSize)
                    .map { it.toVideoItem() }
                val currentList = _uiState.value.videoList.toMutableList()
                currentList.addAll(moreVideos)
                
                _uiState.value = _uiState.value.copy(
                    videoList = currentList
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "加载更多失败"
                )
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

    private fun Video.toVideoItem(): VideoItem =
        VideoItem(
            id = id,
            videoUrl = url,
            title = title,
            authorName = author,
            likeCount = likeCount,
            commentCount = commentCount,
            favoriteCount = favoriteCount,
            shareCount = shareCount,
            orientation = when (orientation) {
                MockVideoCatalog.Orientation.PORTRAIT -> com.ucw.beatu.business.videofeed.presentation.model.VideoOrientation.PORTRAIT
                MockVideoCatalog.Orientation.LANDSCAPE -> com.ucw.beatu.business.videofeed.presentation.model.VideoOrientation.LANDSCAPE
            }
        )

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
}

