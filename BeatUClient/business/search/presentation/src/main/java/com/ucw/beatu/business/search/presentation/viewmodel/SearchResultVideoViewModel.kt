package com.ucw.beatu.business.search.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ucw.beatu.business.search.domain.repository.SearchRepository
import com.ucw.beatu.business.videofeed.presentation.mapper.toVideoItem
import com.ucw.beatu.shared.common.model.VideoItem
import com.ucw.beatu.shared.common.result.AppResult
import com.ucw.beatu.shared.common.threading.launchNamed
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
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
    private val searchRepository: SearchRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SearchResultVideoUiState())
    val uiState: StateFlow<SearchResultVideoUiState> = _uiState.asStateFlow()

    private var currentPage = 1
    private val pageSize = 20
    private var isLoadingMore = false
    private var loadMoreJob: Job? = null
    private var activeSearchJob: Job? = null
    private var searchQuery: String = ""
    private var titleKeyword: String = ""

    /**
     * 初始化搜索，调用后端接口搜索视频
     */
    fun initSearch(query: String, titleKeyword: String) {
        // 新关键词触发时，必须先取消旧请求，避免被 isLoadingMore 拦截导致结果不更新。
        activeSearchJob?.cancel()
        loadMoreJob?.cancel()
        isLoadingMore = false
        searchQuery = query
        this.titleKeyword = titleKeyword
        currentPage = 1
        _uiState.value = _uiState.value.copy(
            videoList = emptyList(),
            isLoading = true,
            error = null,
            hasLoadedAllFromBackend = false
        )
        loadVideoList()
    }

    /**
     * 加载视频列表：调用后端接口，保存到数据库，然后从数据库读取
     */
    private fun loadVideoList() {
        if (isLoadingMore) return
        isLoadingMore = true

        activeSearchJob = viewModelScope.launchNamed("search", "SearchResultVideoViewModel_launch") {
            _uiState.value = _uiState.value.copy(error = null)

            // 确定搜索关键词：优先使用titleKeyword，如果没有则使用searchQuery
            val query = if (titleKeyword.isNotBlank()) titleKeyword else searchQuery
            
            if (query.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    videoList = emptyList(),
                    isLoading = false,
                    error = "搜索关键词不能为空"
                )
                isLoadingMore = false
                return@launchNamed
            }

            // 调用后端搜索接口（会自动保存到数据库）
            searchRepository.searchVideos(query, currentPage, pageSize)
                .catch { e ->
                    if (e is CancellationException) throw e
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "搜索失败"
                    )
                }
                .collect { result ->
                    when (result) {
                        is AppResult.Loading -> {
                            if (_uiState.value.videoList.isEmpty()) {
                                _uiState.value = _uiState.value.copy(isLoading = true)
                            }
                        }
                        is AppResult.Success -> {
                            // 后端接口已经将结果保存到数据库
                            // 直接使用后端返回的结果更新UI
                            val videoItems = result.data.map { it.toVideoItem() }
                            
                            // 关键词搜索直接使用后端返回结果，避免二次过滤把有效结果清空。
                            val filteredVideos = videoItems
                            
                            val currentList = if (currentPage == 1) {
                                filteredVideos
                            } else {
                                _uiState.value.videoList + filteredVideos
                            }
                            
                            val hasLoadedAll = result.data.size < pageSize
                            _uiState.value = _uiState.value.copy(
                                videoList = currentList,
                                isLoading = false,
                                hasLoadedAllFromBackend = hasLoadedAll
                            )
                        }
                        is AppResult.Error -> {
                            // 后端失败，尝试从本地数据库读取
                            loadFromLocalDatabase(query)
                            
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = result.message ?: "搜索失败"
                            )
                        }
                    }
                }
        }.also { job ->
            job.invokeOnCompletion {
                isLoadingMore = false
            }
        }
    }

    /**
     * 从本地数据库读取搜索结果并更新UI（一次性读取）
     */
    private suspend fun loadFromLocalDatabase(query: String) {
        val videos = searchRepository.observeSearchResults(query).firstOrNull() ?: emptyList()
        val videoItems = videos.map { it.toVideoItem() }
        
        val filteredVideos = videoItems
        
        val currentList = if (currentPage == 1) {
            filteredVideos
        } else {
            _uiState.value.videoList + filteredVideos
        }

        _uiState.value = _uiState.value.copy(
            videoList = currentList,
            isLoading = false
        )
    }

    /**
     * 加载更多视频
     */
    fun loadMoreVideos() {
        if (isLoadingMore || _uiState.value.hasLoadedAllFromBackend) {
            return
        }

        loadMoreJob?.cancel()
        loadMoreJob = viewModelScope.launchNamed("search", "SearchResultVideoViewModel_launch") {
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

