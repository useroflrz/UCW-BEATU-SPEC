package com.ucw.beatu.business.search.domain.model

/**
 * AI 搜索结果
 */
data class AISearchResult(
    val aiAnswer: String = "",
    val keywords: List<String> = emptyList(),
    val videoIds: List<Long> = emptyList(),  // ✅ 修改：从 List<String> 改为 List<Long>
    val localVideoIds: List<Long> = emptyList(),  // ✅ 修改：从 List<String> 改为 List<Long>
    val error: String? = null
)

