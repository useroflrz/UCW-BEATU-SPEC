package com.ucw.beatu.business.search.domain.model

/**
 * 搜索历史领域模型
 */
data class SearchHistory(
    val query: String,
    val userId: String,
    val createdAt: Long
)
