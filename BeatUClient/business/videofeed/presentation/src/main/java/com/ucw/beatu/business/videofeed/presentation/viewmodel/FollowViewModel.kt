package com.ucw.beatu.business.videofeed.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.ucw.beatu.business.videofeed.domain.usecase.GetFollowFeedUseCase
import com.ucw.beatu.business.videofeed.presentation.mapper.toVideoItem
import com.ucw.beatu.shared.common.model.VideoItem
import com.ucw.beatu.shared.common.result.AppResult
import com.ucw.beatu.shared.common.threading.SuspendBridge
import com.ucw.beatu.shared.common.threading.TaskNames
import com.ucw.beatu.shared.common.threading.TaskRunner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

data class FollowUiState(
    val videoList: List<VideoItem> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val hasLoadedAllFromBackend: Boolean = false
)

@HiltViewModel
class FollowViewModel @Inject constructor(
    application: Application,
    private val getFollowFeedUseCase: GetFollowFeedUseCase
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(FollowUiState())
    val uiState: StateFlow<FollowUiState> = _uiState.asStateFlow()

    private var currentPage = 1
    private val pageSize = 10
    private var isLoadingMore = false
    private var loadMoreJob: TaskRunner.Handle? = null

    private fun launch(taskName: String, timeoutMs: Long? = null, block: suspend () -> Unit): TaskRunner.Handle {
        return TaskRunner.run(
            name = TaskNames.of(
                feature = "vm_follow",
                action = taskName
            ),
            timeoutMs = timeoutMs,
            onError = { throwable -> android.util.Log.e(TAG, "Task failed: $taskName", throwable) }
        ) {
            SuspendBridge.runBlocking { block() }
        }
    }

    init {
        loadVideoList()
    }

    fun loadVideoList() {
        launch("loadFollowList", timeoutMs = 15_000L) {
            currentPage = 1
            _uiState.value = _uiState.value.copy(error = null)

            getFollowFeedUseCase(currentPage, pageSize)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "加载失败"
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
                            val videos = result.data.map { it.toVideoItem() }
                            val hasLoadedAll = videos.size < pageSize
                            _uiState.value = _uiState.value.copy(
                                videoList = videos,
                                isLoading = false,
                                error = null,
                                hasLoadedAllFromBackend = hasLoadedAll
                            )
                        }
                        is AppResult.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = if (_uiState.value.videoList.isEmpty()) {
                                    result.message ?: result.throwable?.message ?: "加载失败"
                                } else null
                            )
                        }
                    }
                }
        }
    }

    fun refreshVideoList() {
        loadMoreJob?.cancel()
        launch("refreshFollowList", timeoutMs = 15_000L) {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            currentPage = 1

            getFollowFeedUseCase(1, pageSize.coerceAtMost(10))
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        error = e.message ?: "刷新失败"
                    )
                }
                .collect { result ->
                    when (result) {
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
                                error = result.message ?: "刷新失败"
                            )
                        }
                        else -> {}
                    }
                }
        }
    }

    fun loadMoreVideos() {
        if (isLoadingMore || _uiState.value.hasLoadedAllFromBackend) return
        loadMoreJob?.cancel()
        isLoadingMore = true
        loadMoreJob = launch("loadMoreFollow", timeoutMs = 15_000L) {
            val nextPage = currentPage + 1
            getFollowFeedUseCase(nextPage, pageSize)
                .catch { isLoadingMore = false }
                .collect { result ->
                    when (result) {
                        is AppResult.Success -> {
                            isLoadingMore = false
                            val moreVideos = result.data.map { it.toVideoItem() }
                            if (moreVideos.isEmpty()) {
                                _uiState.value = _uiState.value.copy(hasLoadedAllFromBackend = true)
                                return@collect
                            }
                            currentPage = nextPage
                            val existingIds = _uiState.value.videoList.map { it.id }.toSet()
                            val uniqueNew = moreVideos.filter { it.id !in existingIds }
                            val hasLoadedAll = moreVideos.size < pageSize
                            _uiState.value = _uiState.value.copy(
                                videoList = _uiState.value.videoList + uniqueNew,
                                hasLoadedAllFromBackend = hasLoadedAll
                            )
                        }
                        is AppResult.Error -> isLoadingMore = false
                        else -> {}
                    }
                }
        }
    }

    fun getCurrentPage(): Int = currentPage

    companion object {
        private const val TAG = "FollowViewModel"
    }
}
