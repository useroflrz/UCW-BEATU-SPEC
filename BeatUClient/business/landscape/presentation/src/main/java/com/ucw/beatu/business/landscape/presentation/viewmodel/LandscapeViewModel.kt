package com.ucw.beatu.business.landscape.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ucw.beatu.business.landscape.presentation.model.VideoItem
import com.ucw.beatu.business.landscape.presentation.model.VideoOrientation
import com.ucw.beatu.shared.common.mock.MockVideoCatalog
import com.ucw.beatu.shared.common.mock.MockVideoCatalog.Orientation.LANDSCAPE
import com.ucw.beatu.shared.common.mock.Video
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

    private val _uiState = MutableStateFlow(LandscapeUiState())
    val uiState: StateFlow<LandscapeUiState> = _uiState.asStateFlow()

    private var currentPage = 1
    private val pageSize = 5
    private val maxCachedItems = 40

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
            val mockVideos = MockVideoCatalog.getPage(LANDSCAPE, currentPage, pageSize)
                .map { it.toLandscapeVideoItem() }
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
            val moreMockVideos = MockVideoCatalog.getPage(LANDSCAPE, currentPage, pageSize)
                .map { it.toLandscapeVideoItem() }
            val mergedList = (_uiState.value.videoList + moreMockVideos)
                .takeLast(maxCachedItems)
            _uiState.value = _uiState.value.copy(
                videoList = mergedList,
                error = null
            )
        }
    }

    /**
     * 插入外部（竖屏传入）的当前视频，确保横屏页首条就是当前播放视频。
     */
    fun showExternalVideo(videoItem: VideoItem) {
        viewModelScope.launch {
            val sanitized = videoItem.copy(orientation = VideoOrientation.LANDSCAPE)
            val mutableList = _uiState.value.videoList.toMutableList().apply {
                val existingIndex = indexOfFirst { it.id == sanitized.id }
                if (existingIndex >= 0) {
                    removeAt(existingIndex)
                }
                add(0, sanitized)
                while (size > maxCachedItems) {
                    removeAt(lastIndex)
                }
            }
            _uiState.value = _uiState.value.copy(
                videoList = mutableList,
                isLoading = false,
                error = null
            )
        }
    }

    private fun Video.toLandscapeVideoItem(): VideoItem =
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
                MockVideoCatalog.Orientation.PORTRAIT -> com.ucw.beatu.business.landscape.presentation.model.VideoOrientation.PORTRAIT
                MockVideoCatalog.Orientation.LANDSCAPE -> com.ucw.beatu.business.landscape.presentation.model.VideoOrientation.LANDSCAPE
            }
        )
}

// 移到文件顶层！让外部（Activity/Adapter）能访问
data class LandscapeUiState(
    val videoList: List<VideoItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)