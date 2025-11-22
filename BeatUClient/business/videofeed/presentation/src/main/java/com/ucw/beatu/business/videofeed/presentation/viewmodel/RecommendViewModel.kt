package com.ucw.beatu.business.videofeed.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ucw.beatu.business.videofeed.presentation.model.VideoItem
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
    
    init {
        // 初始化时加载视频列表
        loadVideoList()
    }

    /**
     * 加载视频列表（硬编码测试数据）
     */
    private fun loadVideoList() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                delay(500) // 模拟网络请求延迟
                
                val videos = createMockVideoList()
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
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            
            try {
                delay(1000) // 模拟网络请求延迟
                
                val newVideos = createMockVideoList()
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
                
                val moreVideos = createMockVideoList()
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

    /**
     * 创建硬编码的测试视频列表
     */
    private fun createMockVideoList(): List<VideoItem> {
        return listOf(
            VideoItem(
                id = "video_001",
                videoUrl = "http://vjs.zencdn.net/v/oceans.mp4",
                title = "《切腹》1/2上集:浪人为何要用竹刀这般折磨自己?# 影视解说#动作冒险",
                authorName = "云哥讲电影",
                likeCount = 535,
                commentCount = 43,
                favoriteCount = 159,
                shareCount = 59
            ),
            VideoItem(
                id = "video_002",
                videoUrl = "http://www.w3school.com.cn/example/html5/mov_bbb.mp4",
                title = "Big Buck Bunny - 经典动画短片 #动画#搞笑",
                authorName = "动画世界",
                likeCount = 1234,
                commentCount = 89,
                favoriteCount = 567,
                shareCount = 234
            ),
            VideoItem(
                id = "video_003",
                videoUrl = "https://media.w3.org/2010/05/sintel/trailer.mp4",
                title = "Sintel 高清预告片 - 奇幻冒险 #奇幻#冒险",
                authorName = "电影推荐官",
                likeCount = 890,
                commentCount = 67,
                favoriteCount = 345,
                shareCount = 123
            ),
            VideoItem(
                id = "video_004",
                videoUrl = "http://vfx.mtime.cn/Video/2021/07/10/mp4/210710171112971120.mp4",
                title = "影视片段 - 精彩剪辑 #影视#剪辑",
                authorName = "剪辑大师",
                likeCount = 2345,
                commentCount = 156,
                favoriteCount = 789,
                shareCount = 456
            ),
            VideoItem(
                id = "video_005",
                videoUrl = "http://vjs.zencdn.net/v/oceans.mp4",
                title = "海洋世界 - 自然风光 #自然#海洋",
                authorName = "自然探索",
                likeCount = 678,
                commentCount = 45,
                favoriteCount = 234,
                shareCount = 89
            )
        )
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
}

