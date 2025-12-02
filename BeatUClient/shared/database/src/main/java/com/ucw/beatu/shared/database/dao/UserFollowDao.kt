package com.ucw.beatu.shared.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ucw.beatu.shared.database.entity.UserFollowEntity
import kotlinx.coroutines.flow.Flow

/**
 * 用户关注关系 DAO，对应后端 beatu_user_follows
 */
@Dao
interface UserFollowDao {

    @Query("SELECT * FROM user_follows WHERE followerId = :userId ORDER BY createdAt DESC")
    fun observeFollowees(userId: String): Flow<List<UserFollowEntity>>

    @Query("SELECT * FROM user_follows WHERE followeeId = :userId ORDER BY createdAt DESC")
    fun observeFollowers(userId: String): Flow<List<UserFollowEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(relation: UserFollowEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(relations: List<UserFollowEntity>)

    @Query("DELETE FROM user_follows WHERE followerId = :followerId AND followeeId = :followeeId")
    suspend fun delete(followerId: String, followeeId: String)

    @Query("DELETE FROM user_follows WHERE followerId = :userId")
    suspend fun deleteByFollower(userId: String)

    @Query("DELETE FROM user_follows WHERE followeeId = :userId")
    suspend fun deleteByFollowee(userId: String)

    @Query("DELETE FROM user_follows")
    suspend fun clear()
}


