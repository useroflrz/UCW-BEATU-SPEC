package com.ucw.beatu.business.search.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ucw.beatu.business.videofeed.domain.usecase.GetFeedUseCase
import com.ucw.beatu.business.videofeed.presentation.mapper.toVideoItem
import com.ucw.beatu.shared.common.model.VideoItem
import com.ucw.beatu.shared.common.result.AppResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import javax.inject.Inject

data class SearchResultVideoUiState(
    val videoList: List<VideoItem> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val hasLoadedAllFromBackend: Boolean = false
)

@HiltViewModel
class SearchResultVideoViewModel @Inject constructor(
    application: Application,
    private val getFeedUseCase: GetFeedUseCase
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SearchResultVideoUiState())
    val uiState: StateFlow<SearchResultVideoUiState> = _uiState.asStateFlow()

    private var currentPage = 1
    private val pageSize = 20
    private var isLoadingMore = false
    private var loadMoreJob: Job? = null
    private var searchQuery: String = ""
    private var titleKeyword: String = ""

    /**
     * 初始化搜索，根据搜索词和title关键词匹配视频
     */
    fun initSearch(query: String, titleKeyword: String) {
        searchQuery = query
        this.titleKeyword = titleKeyword
        currentPage = 1
        loadVideoList()
    }

    /**
     * 加载视频列表，并根据搜索词和title进行匹配
     */
    private fun loadVideoList() {
        if (isLoadingMore) return
        isLoadingMore = true

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(error = null)

            getFeedUseCase(currentPage, pageSize, null)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "加载失败"
                    )
                    isLoadingMore = false
                }
                .collect { result ->
                    when (result) {
                        is AppResult.Loading -> {
                            if (_uiState.value.videoList.isEmpty()) {
                                _uiState.value = _uiState.value.copy(isLoading = true)
                            }
                        }
                        is AppResult.Success -> {
                            val allVideos = result.data.map { it.toVideoItem() }
                            // 根据搜索词和title关键词过滤视频
                            val filteredVideos = filterVideosByKeyword(allVideos, searchQuery, titleKeyword)
                            
                            val hasLoadedAll = filteredVideos.size < pageSize
                            
                            val currentList = if (currentPage == 1) {
                                filteredVideos
                            } else {
                                _uiState.value.videoList + filteredVideos
                            }

                            _uiState.value = _uiState.value.copy(
                                videoList = currentList,
                                isLoading = false,
                                hasLoadedAllFromBackend = hasLoadedAll
                            )
                            isLoadingMore = false
                        }
                        is AppResult.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = result.message ?: "加载失败"
                            )
                            isLoadingMore = false
                        }
                    }
                }
        }
    }

    /**
     * 根据搜索词和title关键词过滤视频
     * 匹配逻辑：视频title需要同时包含搜索词和resultTitle关键词
     */
    private fun filterVideosByKeyword(
        videos: List<VideoItem>,
        searchQuery: String,
        titleKeyword: String
    ): List<VideoItem> {
        if (searchQuery.isBlank() && titleKeyword.isBlank()) {
            return videos
        }

        return videos.filter { video ->
            val titleContainsKeyword = titleKeyword.isNotBlank() && 
                video.title.contains(titleKeyword, ignoreCase = true)
            val titleContainsQuery = searchQuery.isNotBlank() && 
                video.title.contains(searchQuery, ignoreCase = true)
            
            // 如果同时提供了searchQuery和titleKeyword，title需要同时包含两者
            // 如果只提供了其中一个，则只需要包含该关键词即可
            when {
                searchQuery.isNotBlank() && titleKeyword.isNotBlank() -> {
                    // 两者都提供：title需要同时包含搜索词和title关键词
                    titleContainsQuery && titleContainsKeyword
                }
                titleKeyword.isNotBlank() -> {
                    // 只提供titleKeyword：title需要包含title关键词
                    titleContainsKeyword
                }
                searchQuery.isNotBlank() -> {
                    // 只提供searchQuery：title需要包含搜索词
                    titleContainsQuery
                }
                else -> true
            }
        }
    }

    /**
     * 加载更多视频
     */
    fun loadMoreVideos() {
        if (isLoadingMore || _uiState.value.hasLoadedAllFromBackend) {
            return
        }

        loadMoreJob?.cancel()
        loadMoreJob = viewModelScope.launch {
            currentPage++
            loadVideoList()
        }
    }

    /**
     * 刷新视频列表
     */
    fun refreshVideoList() {
        currentPage = 1
        _uiState.value = _uiState.value.copy(
            isRefreshing = true,
            hasLoadedAllFromBackend = false
        )
        loadVideoList()
    }

    /**
     * 获取当前页码
     */
    fun getCurrentPage(): Int = currentPage

    /**
     * 恢复状态（用于页面恢复）
     */
    fun restoreState(videos: List<VideoItem>, page: Int) {
        currentPage = page
        _uiState.value = _uiState.value.copy(
            videoList = videos,
            isLoading = false
        )
    }
}

