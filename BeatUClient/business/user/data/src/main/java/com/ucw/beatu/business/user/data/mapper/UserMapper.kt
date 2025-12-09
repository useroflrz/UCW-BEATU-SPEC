package com.ucw.beatu.business.user.data.mapper

import android.util.Log
import com.ucw.beatu.business.user.data.api.dto.UserDto
import com.ucw.beatu.business.user.domain.model.User
import com.ucw.beatu.shared.database.entity.UserEntity

/**
 * 用户实体到领域模型的映射器
 */

/**
 * UserEntity -> User
 */
fun UserEntity.toDomain(): User {
    return User(
        id = userId, // ✅ 修改：使用 userId 字段
        avatarUrl = avatarUrl,
        name = userName, // ✅ 修改：使用 userName 字段
        bio = bio,
        likesCount = 0, // ✅ 修改：新表结构中没有 likesCount 字段
        followingCount = followingCount,
        followersCount = followerCount // ✅ 修改：使用 followerCount 字段
    )
}

/**
 * User -> UserEntity
 */
fun User.toEntity(): UserEntity {
    return UserEntity(
        userId = id, // ✅ 修改：使用 userId 字段
        userName = name, // ✅ 修改：使用 userName 字段
        avatarUrl = avatarUrl,
        followerCount = followersCount, // ✅ 修改：使用 followerCount 字段
        followingCount = followingCount,
        bio = bio
    )
}

/**
 * UserDto -> User
 */
fun UserDto.toDomain(): User {
    Log.d("UserDto", "id=${id}")
    Log.d("UserDto", "name=${name}")
    Log.d("UserDto", "followingCount=${followingCount} (类型: ${followingCount::class.java.simpleName})")
    Log.d("UserDto", "followersCount=${followersCount} (类型: ${followersCount::class.java.simpleName})")
    Log.d("UserDto", "bio=${bio}")
    return User(
        id = id,
        avatarUrl = avatarUrl,
        name = name,
        bio = bio,
        followingCount = followingCount,
        followersCount = followersCount,
        likesCount = likesCount
    )
}

