package com.ucw.beatu.shared.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ucw.beatu.shared.database.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * 搜索历史数据访问对象
 * 用于管理搜索历史，支持 LRU 策略（最多保存 5 条）
 */
@Dao
interface SearchHistoryDao {
    /**
     * 查询用户的搜索历史（按时间倒序，最多 5 条）
     */
    @Query("""
        SELECT * FROM beatu_search_history 
        WHERE userId = :userId 
        ORDER BY createdAt DESC 
        LIMIT 5
    """)
    fun observeSearchHistory(userId: String): Flow<List<SearchHistoryEntity>>

    /**
     * 查询用户的搜索历史（一次性查询）
     */
    @Query("""
        SELECT * FROM beatu_search_history 
        WHERE userId = :userId 
        ORDER BY createdAt DESC 
        LIMIT 5
    """)
    suspend fun getSearchHistory(userId: String): List<SearchHistoryEntity>

    /**
     * 插入搜索历史
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: SearchHistoryEntity)

    /**
     * 删除用户的搜索历史
     */
    @Query("DELETE FROM beatu_search_history WHERE userId = :userId")
    suspend fun deleteByUserId(userId: String)

    /**
     * 删除单条搜索历史
     */
    @Query("DELETE FROM beatu_search_history WHERE query = :query AND userId = :userId")
    suspend fun deleteByQuery(query: String, userId: String)

    /**
     * 清空所有搜索历史
     */
    @Query("DELETE FROM beatu_search_history")
    suspend fun clear()

    /**
     * 获取用户的搜索历史数量
     */
    @Query("SELECT COUNT(*) FROM beatu_search_history WHERE userId = :userId")
    suspend fun getHistoryCount(userId: String): Int
    
    /**
     * 删除超出限制的旧记录（保留最新的 5 条，LRU 策略）
     * 注意：由于 Room 对复杂子查询的支持有限，这里只删除最旧的记录
     * 实际 LRU 逻辑在应用层实现：先调用此方法，然后检查数量，如果仍超过 5 条则继续删除
     */
    @Query("""
        DELETE FROM beatu_search_history 
        WHERE userId = :userId 
        AND createdAt = (
            SELECT MIN(createdAt) FROM beatu_search_history 
            WHERE userId = :userId
        )
    """)
    suspend fun deleteOldestRecord(userId: String)
}

