package com.ucw.beatu.business.landscape.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ucw.beatu.business.landscape.domain.usecase.LandscapeUseCases
import com.ucw.beatu.business.landscape.presentation.model.VideoItem
import com.ucw.beatu.business.landscape.presentation.model.toPresentationModel
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
 * 横屏列表 ViewModel：负责调用 UseCase、分页加载并保证首条为外部传入视频。
 */
@HiltViewModel
class LandscapeViewModel @Inject constructor(
    private val useCases: LandscapeUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(LandscapeUiState())
    val uiState: StateFlow<LandscapeUiState> = _uiState.asStateFlow()

    private var currentPage = 1
    private val pageSize = 5
    private val maxCachedItems = 40

    private var pendingExternalVideo: VideoItem? = null
    private var shouldReapplyExternalVideo = false
    private var isDefaultListLoading = false
    private var isLoadingMore = false

    fun loadVideoList() {
        currentPage = 1
        isDefaultListLoading = true
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
                        val mapped = result.data.map { it.toPresentationModel() }
                        _uiState.update { state ->
                            val baseList = if (append) {
                                state.videoList + mapped
                            } else {
                                mapped
                            }
                            state.copy(
                                videoList = baseList.takeLast(maxCachedItems),
                                isLoading = false,
                                error = null,
                                lastUpdated = System.currentTimeMillis()
                            )
                        }
                        if (append) {
                            currentPage = page
                            isLoadingMore = false
                        } else {
                            isDefaultListLoading = false
                        }
                        applyPendingExternalVideo(forceInsert = false)
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
                        isDefaultListLoading = false
                        AppLogger.e(TAG, "load landscape failed", result.throwable)
                    }
                }
            }
        }
    }

    fun showExternalVideo(videoItem: VideoItem) {
        viewModelScope.launch {
            pendingExternalVideo = videoItem
            shouldReapplyExternalVideo =
                isDefaultListLoading || _uiState.value.videoList.isEmpty()
            applyPendingExternalVideo(forceInsert = true)
            if (!shouldReapplyExternalVideo) {
                pendingExternalVideo = null
            }
        }
    }

    private fun applyPendingExternalVideo(forceInsert: Boolean) {
        val external = pendingExternalVideo ?: return
        val currentList = _uiState.value.videoList
        if (currentList.isEmpty() && !forceInsert) return

        val merged = buildList {
            add(external)
            currentList.forEach { item ->
                if (item.id != external.id) add(item)
            }
        }.take(maxCachedItems)

        _uiState.value = _uiState.value.copy(
            videoList = merged,
            isLoading = false,
            error = null
        )

        if (!forceInsert || !shouldReapplyExternalVideo) {
            pendingExternalVideo = null
            shouldReapplyExternalVideo = false
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

