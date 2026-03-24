package com.ucw.beatu.business.search.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ucw.beatu.business.search.domain.repository.AISearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiAnswerBlock(
    val index: Int,
    val markdown: String
)

/**
 * AI 搜索 UI 状态
 */
data class AISearchUiState(
    val aiAnswer: String = "",
    val blocks: List<AiAnswerBlock> = emptyList(),
    val keywords: List<String> = emptyList(),
    val videoIds: List<Long> = emptyList(),  // ✅ 修改：从 List<String> 改为 List<Long>
    val localVideoIds: List<Long> = emptyList(),  // ✅ 修改：从 List<String> 改为 List<Long>
    val isLoading: Boolean = false,
    val error: String? = null,
    val renderSessionId: Long = 0L
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
    private var sessionId: Long = 0L
    private var lastQuery: String = ""
    
    /**
     * 执行搜索
     */
    fun search(userQuery: String) {
        restartSearch(userQuery)
    }

    /**
     * 按四步重启搜索：停旧流 -> 清空 -> 重置会话 -> 启动新流
     */
    fun restartSearch(userQuery: String) {
        val query = userQuery.trim()
        if (query.isBlank()) return
        viewModelScope.launch {
            stopSearchInternal()
            sessionId += 1L
            lastQuery = query
            _uiState.value = AISearchUiState(
                isLoading = true,
                error = null,
                renderSessionId = sessionId
            )
            startSearch(query, sessionId)
        }
    }

    fun retryLastSearch() {
        if (lastQuery.isNotBlank()) {
            restartSearch(lastQuery)
        }
    }
    
    /**
     * 清除搜索结果
     */
    fun clear() {
        viewModelScope.launch {
            stopSearchInternal()
            sessionId += 1L
            _uiState.value = AISearchUiState(renderSessionId = sessionId)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        repository.stopCurrentStream()
        searchJob?.cancel()
    }

    private fun startSearch(query: String, currentSessionId: Long) {
        var answerBuffer = ""
        // 增量切块：只维护已完成的块 + 当前尾段，避免每次 SSE 对全文 split
        val completedBlocks = mutableListOf<String>()
        val tailBuffer = StringBuilder()
        searchJob = repository.searchStream(query)
            .onEach { result ->
                if (currentSessionId != sessionId) return@onEach
                answerBuffer += result.aiAnswer
                appendAndFlushBlocks(
                    delta = result.aiAnswer.replace("\r\n", "\n"),
                    completedBlocks = completedBlocks,
                    tailBuffer = tailBuffer
                )
                val parsedKeywords = extractKeywordsFromMarkdown(answerBuffer)
                _uiState.update { currentState ->
                    currentState.copy(
                        aiAnswer = answerBuffer,
                        blocks = buildBlocksFromIncremental(completedBlocks, tailBuffer),
                        keywords = if (result.keywords.isNotEmpty()) result.keywords else parsedKeywords,
                        videoIds = result.videoIds.ifEmpty { currentState.videoIds },
                        localVideoIds = result.localVideoIds.ifEmpty { currentState.localVideoIds },
                        isLoading = false,
                        error = result.error ?: currentState.error,
                        renderSessionId = currentSessionId
                    )
                }
            }
            .catch {
                if (currentSessionId != sessionId) return@catch
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "AI 搜索不可用",
                        renderSessionId = currentSessionId
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * 将新片段追加到尾缓冲，并按 `\n\n` 切出已完成段落（增量，不全量重切全文）。
     * 在 [StringBuilder] 上原地 [indexOf]/[delete]，避免循环内对整段尾串反复 toString。
     */
    private fun appendAndFlushBlocks(
        delta: String,
        completedBlocks: MutableList<String>,
        tailBuffer: StringBuilder
    ) {
        if (delta.isEmpty()) return
        tailBuffer.append(delta)
        while (true) {
            val idx = tailBuffer.indexOf("\n\n")
            if (idx < 0) break
            val segment = tailBuffer.substring(0, idx)
            if (segment.isNotBlank()) {
                completedBlocks.add(segment)
            }
            tailBuffer.delete(0, idx + 2)
        }
    }

    private fun buildBlocksFromIncremental(
        completedBlocks: List<String>,
        tailBuffer: StringBuilder
    ): List<AiAnswerBlock> {
        val tail = tailBuffer.toString()
        return buildList {
            completedBlocks.forEachIndexed { index, markdown ->
                add(AiAnswerBlock(index = index, markdown = markdown))
            }
            if (tail.isNotBlank()) {
                add(AiAnswerBlock(index = completedBlocks.size, markdown = tail))
            }
        }
    }

    private suspend fun stopSearchInternal() {
        repository.stopCurrentStream()
        searchJob?.cancelAndJoin()
        searchJob = null
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

