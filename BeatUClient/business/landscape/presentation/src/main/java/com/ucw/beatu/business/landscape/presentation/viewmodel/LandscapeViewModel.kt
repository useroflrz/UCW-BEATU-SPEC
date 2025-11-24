package com.ucw.beatu.business.landscape.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ucw.beatu.business.landscape.domain.repository.LandscapeRepository
import com.ucw.beatu.business.landscape.presentation.model.VideoItem

import com.ucw.beatu.shared.common.result.AppResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 横屏页 ViewModel
 * 管理横屏视频列表，使用 Repository 获取数据（目前使用 mock 数据）
 */
@HiltViewModel
class LandscapeViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    // 统一管理 Mock 视频链接（便于维护）
    private val MOCK_VIDEO_URL_1 =
        "https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/Justin%20Bieber%20-%20Beauty%20And%20A%20Beat.mp4"
    private val MOCK_VIDEO_URL_2 =
        "https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/%E6%B5%8B%E8%AF%95%E8%A7%86%E9%A2%91.mp4"

    private val MOCK_VIDEO_URL_3 =
        "https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/%E6%B5%8B%E8%AF%95%E8%A7%86%E9%A2%912.mp4"

    private val _uiState = MutableStateFlow(LandscapeUiState())
    val uiState: StateFlow<LandscapeUiState> = _uiState.asStateFlow()

    private var currentPage = 1
    private val pageSize = 20

    init {
        loadVideoList()
    }

    /**
     * 加载第一页 Mock 数据
     */
    fun loadVideoList() {
        viewModelScope.launch {
            currentPage = 1
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // 第一页 Mock 数据
            val mockVideos = listOf(
                VideoItem(
                    id = "mock_1",
                    videoUrl = MOCK_VIDEO_URL_1,
                    title = "Mock 横屏视频 1",
                    authorName = "Mock 作者1",
                    likeCount = 123,
                    commentCount = 45,
                    favoriteCount = 67,
                    shareCount = 89
                ),
                VideoItem(
                    id = "mock_2",
                    videoUrl = MOCK_VIDEO_URL_2,
                    title = "Mock 横屏视频 2",
                    authorName = "Mock 作者2",
                    likeCount = 987,
                    commentCount = 65,
                    favoriteCount = 43,
                    shareCount = 21
                ), VideoItem(
                    id = "mock_2",
                    videoUrl = MOCK_VIDEO_URL_3,
                    title = "Mock 横屏视频 2",
                    authorName = "Mock 作者2",
                    likeCount = 987,
                    commentCount = 65,
                    favoriteCount = 43,
                    shareCount = 21
                )
            )

            _uiState.value = _uiState.value.copy(
                videoList = mockVideos,
                isLoading = false,
                error = null
            )
        }
    }

    /**
     * 加载更多 Mock 数据（复用阿里云链接，保证有效性）
     */
    fun loadMoreVideos() {
        viewModelScope.launch {
            currentPage++
            // 加载更多用交替的 Mock 链接（避免全部重复）
            val moreVideoUrl = if (currentPage % 2 == 0) MOCK_VIDEO_URL_1 else MOCK_VIDEO_URL_2

            val moreMockVideos = listOf(
                VideoItem(
                    id = "mock_${currentPage}_1",
                    videoUrl = moreVideoUrl, // 用有效阿里云链接，而非 mock.com
                    title = "Mock 横屏视频 ${currentPage}",
                    authorName = "Mock 作者${currentPage}",
                    likeCount = 100 + currentPage * 10,
                    commentCount = 50 + currentPage * 5,
                    favoriteCount = 30 + currentPage * 3,
                    shareCount = 20 + currentPage * 2
                )
            )

            // 追加数据
            val currentList = _uiState.value.videoList.toMutableList()
            currentList.addAll(moreMockVideos)
            _uiState.value = _uiState.value.copy(
                videoList = currentList,
                error = null
            )
        }
    }
}

// 移到文件顶层！让外部（Activity/Adapter）能访问
data class LandscapeUiState(
    val videoList: List<VideoItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)