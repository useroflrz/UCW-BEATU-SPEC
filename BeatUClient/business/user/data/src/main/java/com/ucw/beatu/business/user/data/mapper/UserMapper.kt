package com.ucw.beatu.business.user.data.mapper

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
        id = id,
        avatarUrl = avatarUrl,
        name = name,
        bio = bio,
        likesCount = likesCount,
        followingCount = followingCount,
        followersCount = followersCount
    )
}

/**
 * User -> UserEntity
 */
fun User.toEntity(): UserEntity {
    return UserEntity(
        id = id,
        avatarUrl = avatarUrl,
        name = name,
        bio = bio,
        likesCount = likesCount,
        followingCount = followingCount,
        followersCount = followersCount
    )
}

/**
 * UserDto -> User
 */
fun UserDto.toDomain(): User {
    return User(
        id = id,
        avatarUrl = avatarUrl,
        name = name,
        bio = bio,
        likesCount = likesCount,
        followingCount = followingCount,
        followersCount = followersCount
    )
}

