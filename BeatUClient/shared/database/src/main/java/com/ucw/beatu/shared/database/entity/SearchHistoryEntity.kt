package com.ucw.beatu.shared.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 搜索历史表，对应 beatu_search_history
 * 用于缓存搜索历史，支持搜索界面提醒（持久化 0~5，LRU 策略）
 */
@Entity(
    tableName = "beatu_search_history",
    primaryKeys = ["query", "userId"], // ✅ 修改：根据需求文档，query 是主键（复合主键）
    indices = [
        Index(value = ["userId", "createdAt"]),
        Index(value = ["userId", "query"])
    ]
)
data class SearchHistoryEntity(
    val query: String,  // 搜索词 (PK)
    val userId: String,  // 用户 ID (PK)
    val createdAt: Long = System.currentTimeMillis()  // 搜索时间
)

