package com.ucw.beatu.business.landscape.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ucw.beatu.business.landscape.domain.usecase.LandscapeUseCases
import com.ucw.beatu.business.landscape.presentation.model.VideoItem
import com.ucw.beatu.business.videofeed.domain.model.Video
import com.ucw.beatu.shared.common.logger.AppLogger
import com.ucw.beatu.shared.common.result.AppResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 横屏页 ViewModel
 * Repository -> UseCase -> Presentation 状态流
 */
@HiltViewModel
class LandscapeViewModel @Inject constructor(
    private val useCases: LandscapeUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(LandscapeUiState())
    val uiState: StateFlow<LandscapeUiState> = _uiState.asStateFlow()

    private var currentPage = 1
    private val pageSize = 5
    private var isLoadingMore = false
    private var externalEntry: VideoItem? = null

    fun showExternalVideo(videoItem: VideoItem) {
        externalEntry = videoItem
        _uiState.value = LandscapeUiState(
            videoList = listOf(videoItem),
            isLoading = false,
            error = null,
            lastUpdated = System.currentTimeMillis()
        )
    }

    fun loadVideoList() {
        currentPage = 1
        fetchPage(page = currentPage, append = false)
    }

    fun loadMoreVideos() {
        if (isLoadingMore || _uiState.value.isLoading) return
        isLoadingMore = true
        val nextPage = currentPage + 1
        fetchPage(page = nextPage, append = true)
    }

    private fun fetchPage(page: Int, append: Boolean) {
        viewModelScope.launch {
            val flow = if (append) {
                useCases.loadMoreLandscapeVideos(page, pageSize)
            } else {
                useCases.getLandscapeVideos(page, pageSize)
            }

            flow.collect { result ->
                when (result) {
                    is AppResult.Loading -> {
                        if (!append) {
                            _uiState.update { it.copy(isLoading = true, error = null) }
                        }
                    }
                    is AppResult.Success -> {
                        val mapped = result.data.map { it.toLandscapeItem() }
                        _uiState.update { state ->
                            val baseList = if (append) state.videoList + mapped else mapped
                            val finalList = baseList.withExternalPinned(externalEntry)
                            state.copy(
                                videoList = finalList,
                                isLoading = false,
                                error = null,
                                lastUpdated = System.currentTimeMillis()
                            )
                        }
                        if (append) {
                            currentPage = page
                            isLoadingMore = false
                        }
                        AppLogger.d(TAG, "Loaded landscape page=$page size=${mapped.size}")
                    }
                    is AppResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.message ?: "加载失败"
                            )
                        }
                        isLoadingMore = false
                        AppLogger.e(TAG, "load landscape failed", result.throwable)
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "LandscapeViewModel"
    }
}

data class LandscapeUiState(
    val videoList: List<VideoItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastUpdated: Long? = null
)

private fun Video.toLandscapeItem(): VideoItem {
    return VideoItem(
        id = id,
        videoUrl = playUrl,
        title = title,
        authorName = authorName,
        likeCount = likeCount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
        commentCount = commentCount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
        favoriteCount = favoriteCount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
        shareCount = shareCount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
        isLiked = isLiked,
        isFavorited = isFavorited,
        defaultSpeed = 1.0f,
        defaultQuality = "自动"
    )
}

private fun List<VideoItem>.withExternalPinned(external: VideoItem? = null): List<VideoItem> {
    val entry = external ?: return this
    val filtered = this.filterNot { it.id == entry.id }
    return listOf(entry) + filtered
}