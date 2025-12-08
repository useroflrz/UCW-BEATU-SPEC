package com.ucw.beatu.shared.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ucw.beatu.shared.database.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * 用户数据访问对象
 */
@Dao
interface UserDao {
    /**
     * 根据用户ID查询用户信息（Flow）
     */
    @Query("SELECT * FROM beatu_user WHERE userId = :userId LIMIT 1")
    fun observeUserById(userId: String): Flow<UserEntity?>

    /**
     * 根据用户ID查询用户信息（一次性）
     */
    @Query("SELECT * FROM beatu_user WHERE userId = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    /**
     * 根据用户名查询用户信息（Flow）
     */
    @Query("SELECT * FROM beatu_user WHERE userName = :userName LIMIT 1")
    fun observeUserByName(userName: String): Flow<UserEntity?>

    /**
     * 根据用户名查询用户信息（一次性）
     */
    @Query("SELECT * FROM beatu_user WHERE userName = :userName LIMIT 1")
    suspend fun getUserByName(userName: String): UserEntity?

    /**
     * 插入或更新用户信息
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(user: UserEntity)

    /**
     * 批量插入或更新用户信息
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(users: List<UserEntity>)

    /**
     * 删除用户
     */
    @Query("DELETE FROM beatu_user WHERE userId = :userId")
    suspend fun deleteById(userId: String)

    /**
     * 清空所有用户数据
     */
    @Query("DELETE FROM beatu_user")
    suspend fun clear()
}

