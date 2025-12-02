package com.ucw.beatu

import android.app.Application
import android.util.Log
import com.ucw.beatu.business.user.domain.model.User
import com.ucw.beatu.business.user.domain.repository.UserRepository
import com.ucw.beatu.shared.common.mock.MockUserCatalog
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BeatU Application 类
 * Hilt 依赖注入入口
 */
@HiltAndroidApp
class BeatUApp : Application() {

    companion object {
        private const val TAG = "BeatUApp"
        private const val DEFAULT_USER_ID = "current_user"
    }

    @Inject
    lateinit var userRepository: UserRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: Application started")
        
        try {
            // 初始化逻辑可以在这里添加
            // 例如：初始化日志、性能监控、崩溃收集等
            // 在应用启动时初始化 Mock 用户数据（基于 MockVideoCatalog）
            initMockUsersOnce()
            Log.d(TAG, "onCreate: Application initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "onCreate: Error initializing application", e)
            throw e
        }
    }

    private fun initMockUsersOnce() {
        appScope.launch {
            try {
                // 检查第一个作者用户是否存在，如果不存在则初始化所有用户
                // 这样可以确保所有作者用户都被初始化，而不仅仅是当前用户
                val firstAuthorUser = userRepository.getUserById("mock_author_1")
                if (firstAuthorUser != null) {
                    Log.d(TAG, "initMockUsersOnce: mock users already initialized")
                    return@launch
                }
                
                // 生成所有 Mock 用户（包括当前用户和所有作者用户）
                Log.d(TAG, "initMockUsersOnce: calling MockUserCatalog.buildMockUsers()")
                val mockUsers = MockUserCatalog.buildMockUsers(currentUserId = DEFAULT_USER_ID)
                Log.d(TAG, "initMockUsersOnce: buildMockUsers returned ${mockUsers.size} users")
                if (mockUsers.isEmpty()) {
                    Log.w(TAG, "initMockUsersOnce: WARNING - buildMockUsers returned empty list!")
                } else {
                    Log.d(TAG, "initMockUsersOnce: first user = ${mockUsers.first().id}, last user = ${mockUsers.last().id}")
                    mockUsers.forEachIndexed { index, user ->
                        Log.d(TAG, "initMockUsersOnce: user[$index] = id=${user.id}, name=${user.name}")
                    }
                }
                
                // 保存所有用户
                mockUsers.forEach { mockUser ->
                    val domainUser = User(
                        id = mockUser.id,
                        avatarUrl = mockUser.avatarUrl,
                        name = mockUser.name,
                        bio = mockUser.bio,
                        likesCount = mockUser.likesCount,
                        followingCount = mockUser.followingCount,
                        followersCount = mockUser.followersCount
                    )
                    userRepository.saveUser(domainUser)
                }
                Log.d(TAG, "initMockUsersOnce: inserted ${mockUsers.size} mock users (current user + ${mockUsers.size - 1} author users)")
            } catch (e: Exception) {
                Log.e(TAG, "initMockUsersOnce: failed to init mock users", e)
            }
        }
    }
}

