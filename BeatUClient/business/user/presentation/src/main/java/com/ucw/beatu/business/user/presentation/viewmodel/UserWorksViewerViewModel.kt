package com.ucw.beatu.business.user.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.ucw.beatu.business.videofeed.presentation.model.VideoItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserWorksViewerUiState(
    val userId: String = "",
    val videoList: List<VideoItem> = emptyList(),
    val currentIndex: Int = 0
)

@HiltViewModel
class UserWorksViewerViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(UserWorksViewerUiState())
    val uiState: StateFlow<UserWorksViewerUiState> = _uiState.asStateFlow()

    private var initialized = false

    fun setInitialData(userId: String, videos: List<VideoItem>, initialIndex: Int) {
        if (initialized) return
        initialized = true
        val boundedIndex = if (videos.isEmpty()) 0 else initialIndex.coerceIn(0, videos.lastIndex)
        _uiState.value = UserWorksViewerUiState(
            userId = userId,
            videoList = videos,
            currentIndex = boundedIndex
        )
    }

    fun updateCurrentIndex(index: Int) {
        if (_uiState.value.currentIndex == index) return
        _uiState.value = _uiState.value.copy(currentIndex = index)
    }

    fun getVideoAt(position: Int): VideoItem? = _uiState.value.videoList.getOrNull(position)
}


