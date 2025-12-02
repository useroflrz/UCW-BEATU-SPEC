package com.ucw.beatu.shared.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ucw.beatu.shared.database.entity.UserInteractionEntity
import kotlinx.coroutines.flow.Flow

/**
 * 用户互动 DAO，对应后端 beatu_interactions
 */
@Dao
interface UserInteractionDao {

    // 查询某用户对某视频的互动记录（点赞 / 收藏）
    @Query(
        """
        SELECT * FROM user_interactions
        WHERE userId = :userId AND videoId = :videoId
        """
    )
    fun observeInteractionsForVideo(
        userId: String,
        videoId: String
    ): Flow<List<UserInteractionEntity>>

    // 查询某用户对某作者的关注互动记录
    @Query(
        """
        SELECT * FROM user_interactions
        WHERE userId = :userId AND authorId = :authorId AND type = :type
        LIMIT 1
        """
    )
    fun observeAuthorInteraction(
        userId: String,
        authorId: String,
        type: String
    ): Flow<UserInteractionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(interaction: UserInteractionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(interactions: List<UserInteractionEntity>)

    @Query(
        """
        DELETE FROM user_interactions
        WHERE userId = :userId AND videoId = :videoId AND type = :type
        """
    )
    suspend fun deleteVideoInteraction(
        userId: String,
        videoId: String,
        type: String
    )

    @Query(
        """
        DELETE FROM user_interactions
        WHERE userId = :userId AND authorId = :authorId AND type = :type
        """
    )
    suspend fun deleteAuthorInteraction(
        userId: String,
        authorId: String,
        type: String
    )

    @Query("DELETE FROM user_interactions WHERE userId = :userId")
    suspend fun clearByUser(userId: String)

    @Query("DELETE FROM user_interactions")
    suspend fun clearAll()
}


