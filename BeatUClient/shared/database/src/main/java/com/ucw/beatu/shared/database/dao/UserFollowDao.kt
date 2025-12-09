package com.ucw.beatu.shared.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ucw.beatu.shared.database.entity.UserFollowEntity
import kotlinx.coroutines.flow.Flow

/**
 * 用户关注关系 DAO，对应后端 beatu_user_follow
 */
@Dao
interface UserFollowDao {

    /**
     * 查询用户关注的所有作者（Flow）
     */
    @Query("SELECT * FROM beatu_user_follow WHERE userId = :userId AND isFollowed = 1")
    fun observeFollowees(userId: String): Flow<List<UserFollowEntity>>

    /**
     * 查询关注该用户的所有粉丝（Flow）
     */
    @Query("SELECT * FROM beatu_user_follow WHERE authorId = :userId AND isFollowed = 1")
    fun observeFollowers(userId: String): Flow<List<UserFollowEntity>>

    /**
     * 检查是否关注
     */
    @Query("SELECT isFollowed FROM beatu_user_follow WHERE userId = :userId AND authorId = :authorId LIMIT 1")
    suspend fun isFollowing(userId: String, authorId: String): Boolean?

    /**
     * 检查是否关注（Flow）
     */
    @Query("SELECT * FROM beatu_user_follow WHERE userId = :userId AND authorId = :authorId LIMIT 1")
    fun observeFollowStatus(userId: String, authorId: String): Flow<UserFollowEntity?>

    /**
     * 查询所有待同步的关注记录
     */
    @Query("SELECT * FROM beatu_user_follow WHERE isPending = 1")
    suspend fun getPendingFollows(): List<UserFollowEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(relation: UserFollowEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(relations: List<UserFollowEntity>)

    @Query("DELETE FROM beatu_user_follow WHERE userId = :userId AND authorId = :authorId")
    suspend fun delete(userId: String, authorId: String)

    @Query("DELETE FROM beatu_user_follow WHERE userId = :userId")
    suspend fun deleteByUser(userId: String)

    @Query("DELETE FROM beatu_user_follow WHERE authorId = :userId")
    suspend fun deleteByAuthor(userId: String)

    @Query("DELETE FROM beatu_user_follow")
    suspend fun clear()

    /**
     * 查询所有关注关系（用于调试）
     */
    @Query("SELECT * FROM beatu_user_follow")
    suspend fun getAll(): List<UserFollowEntity>

    /**
     * 查询指定用户的所有关注关系（包括 isFollowed=false 的）
     */
    @Query("SELECT * FROM beatu_user_follow WHERE userId = :userId")
    suspend fun getAllByUserId(userId: String): List<UserFollowEntity>
}


