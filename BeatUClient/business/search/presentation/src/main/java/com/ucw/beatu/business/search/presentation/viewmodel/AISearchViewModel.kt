package com.ucw.beatu.business.search.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ucw.beatu.business.search.domain.repository.AISearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * AI 搜索 UI 状态
 */
data class AISearchUiState(
    val aiAnswer: String = "",
    val keywords: List<String> = emptyList(),
    val videoIds: List<Long> = emptyList(),  // ✅ 修改：从 List<String> 改为 List<Long>
    val localVideoIds: List<Long> = emptyList(),  // ✅ 修改：从 List<String> 改为 List<Long>
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * AI 搜索 ViewModel
 */
@HiltViewModel
class AISearchViewModel @Inject constructor(
    private val repository: AISearchRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AISearchUiState())
    val uiState: StateFlow<AISearchUiState> = _uiState.asStateFlow()
    
    private var searchJob: Job? = null
    
    /**
     * 执行搜索
     */
    fun search(userQuery: String) {
        // 取消之前的搜索
        searchJob?.cancel()
        
        // 重置状态
        _uiState.value = AISearchUiState(
            isLoading = true,
            error = null
        )
        
        // 开始新的搜索
        searchJob = repository.searchStream(userQuery)
            .onEach { result ->
                // 累积更新状态
                _uiState.update { currentState ->
                    currentState.copy(
                        aiAnswer = if (result.aiAnswer.isNotEmpty()) {
                            // 流式累积 AI 回答
                            currentState.aiAnswer + result.aiAnswer
                        } else {
                            currentState.aiAnswer
                        },
                        keywords = result.keywords.ifEmpty { currentState.keywords },
                        videoIds = result.videoIds.ifEmpty { currentState.videoIds },
                        localVideoIds = result.localVideoIds.ifEmpty { currentState.localVideoIds },
                        isLoading = false,
                        error = result.error ?: currentState.error
                    )
                }
            }
            .catch { e ->
                // ✅ 统一错误消息：AI搜索不可用
                val errorMessage = "AI 搜索不可用"
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = errorMessage
                )
            }
            .launchIn(viewModelScope)
    }
    
    /**
     * 清除搜索结果
     */
    fun clear() {
        searchJob?.cancel()
        _uiState.value = AISearchUiState()
    }
    
    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
    }
}

// StateFlow 扩展函数，用于更新状态
private fun <T> MutableStateFlow<T>.update(update: (T) -> T) {
    value = update(value)
}

