package com.ucw.beatu.business.search.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ucw.beatu.business.search.domain.repository.AISearchRepository
import com.ucw.beatu.shared.common.threading.launchInNamed
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
    companion object {
        private const val MAX_KEYWORDS = 5
        private val MARKDOWN_BOLD_KEYWORD_REGEX = Regex("\\*\\*([^*]{2,20})\\*\\*")
        private val KEYWORD_EDGE_SYMBOLS_REGEX = Regex("^[\\p{Punct}\\p{S}\\s]+|[\\p{Punct}\\p{S}\\s]+$")
        private val KEYWORD_STOP_WORDS = setOf(
            "我们", "你们", "这是", "当前", "问题", "回答", "继续", "方向",
            "原理", "步骤", "思路", "方案", "可以", "展开", "搜索结果"
        )
    }
    
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
                    val nextAnswer = if (result.aiAnswer.isNotEmpty()) {
                        // 流式累积 AI 回答
                        currentState.aiAnswer + result.aiAnswer
                    } else {
                        currentState.aiAnswer
                    }
                    val parsedKeywords = extractKeywordsFromMarkdown(nextAnswer)
                    currentState.copy(
                        aiAnswer = nextAnswer,
                        keywords = parsedKeywords,
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
            .launchInNamed(viewModelScope, "search", "AISearchViewModel_launchIn")
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

    private fun extractKeywordsFromMarkdown(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        return MARKDOWN_BOLD_KEYWORD_REGEX.findAll(text)
            .mapNotNull { normalizeKeyword(it.groupValues[1]) }
            .distinct()
            .take(MAX_KEYWORDS)
            .toList()
    }

    private fun normalizeKeyword(raw: String): String? {
        val normalized = raw.trim()
            .replace(KEYWORD_EDGE_SYMBOLS_REGEX, "")
            .trim()

        if (normalized.length !in 2..20) return null
        if (normalized in KEYWORD_STOP_WORDS) return null
        return normalized
    }
}

// StateFlow 扩展函数，用于更新状态
private fun <T> MutableStateFlow<T>.update(update: (T) -> T) {
    value = update(value)
}

