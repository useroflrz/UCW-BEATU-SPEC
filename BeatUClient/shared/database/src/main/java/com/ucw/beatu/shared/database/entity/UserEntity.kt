package com.ucw.beatu.shared.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 用户信息表，对应后端 beatu_user
 * 用于缓存用户基本信息，支持个人主页、作者信息弹窗等场景
 */
@Entity(
    tableName = "beatu_user",
    indices = [Index(value = ["userName"], unique = true)]
)
data class UserEntity(
    @PrimaryKey val userId: String,  // 用户 ID (PK)
    val userName: String,  // 用户昵称（唯一）
    val avatarUrl: String?,  // 头像 URL
    val followerCount: Long = 0,  // 粉丝数
    val followingCount: Long = 0,  // 关注数
    val bio: String? = null  // 简介
)

