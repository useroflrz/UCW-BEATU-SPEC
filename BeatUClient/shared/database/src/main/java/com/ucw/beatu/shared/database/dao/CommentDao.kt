package com.ucw.beatu.shared.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ucw.beatu.shared.database.entity.CommentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommentDao {
    @Query("SELECT * FROM beatu_comment WHERE videoId = :videoId ORDER BY createdAt DESC")
    fun observeComments(videoId: Long): Flow<List<CommentEntity>>

    @Query("SELECT * FROM beatu_comment WHERE commentId = :commentId LIMIT 1")
    suspend fun getCommentById(commentId: String): CommentEntity?

    /**
     * 查询所有待同步的评论
     */
    @Query("SELECT * FROM beatu_comment WHERE isPending = 1")
    suspend fun getPendingComments(): List<CommentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CommentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CommentEntity)

    @Query("DELETE FROM beatu_comment WHERE videoId = :videoId")
    suspend fun clear(videoId: Long)

    @Query("DELETE FROM beatu_comment WHERE commentId = :commentId")
    suspend fun deleteById(commentId: String)
}

