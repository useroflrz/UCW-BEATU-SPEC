package com.ucw.beatu.business.user.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ucw.beatu.business.user.domain.model.User
import com.ucw.beatu.business.user.domain.model.UserWork
import com.ucw.beatu.business.user.domain.repository.UserRepository
import com.ucw.beatu.business.user.domain.repository.UserWorksRepository
import com.ucw.beatu.shared.common.mock.MockUserCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 用户主页 ViewModel
 */
@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val userWorksRepository: UserWorksRepository
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _userWorks = MutableStateFlow<List<UserWork>>(emptyList())
    val userWorks: StateFlow<List<UserWork>> = _userWorks.asStateFlow()

    private var observeWorksJob: Job? = null

    /**
     * 加载用户信息
     */
    fun loadUser(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 观察用户信息变化（使用 StateFlow 自动更新）
                userRepository.observeUserById(userId).collect { user ->
                    _user.value = user
                    if (user != null) {
                        _isLoading.value = false
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isLoading.value = false
            }
        }
    }

    /**
     * 订阅真实视频数据
     */
    fun observeUserWorks(userId: String, limit: Int = UserWorksRepository.DEFAULT_LIMIT) {
        observeWorksJob?.cancel()
        observeWorksJob = viewModelScope.launch {
            userWorksRepository.observeUserWorks(userId, limit).collect { works ->
                _userWorks.value = works
            }
        }
    }

    /**
     * 切换不同的标签页数据源
     */
    fun switchTab(tabType: TabType, userId: String, limit: Int = UserWorksRepository.DEFAULT_LIMIT) {
        observeWorksJob?.cancel()
        observeWorksJob = viewModelScope.launch {
            val flow = when (tabType) {
                TabType.WORKS -> userWorksRepository.observeUserWorks(userId, limit)
                TabType.COLLECTIONS -> userWorksRepository.observeFavoritedWorks(limit)
                TabType.LIKES -> userWorksRepository.observeLikedWorks(limit)
                TabType.HISTORY -> userWorksRepository.observeHistoryWorks(limit)
            }
            flow.collect { works ->
                _userWorks.value = works
            }
        }
    }

    enum class TabType {
        WORKS,      // 作品
        COLLECTIONS, // 收藏
        LIKES,      // 点赞
        HISTORY     // 历史
    }

    /**
     * 更新用户头像
     */
    fun updateAvatar(userId: String, avatarPath: String) {
        viewModelScope.launch {
            val currentUser = userRepository.getUserById(userId)
            if (currentUser != null) {
                val updatedUser = currentUser.copy(avatarUrl = avatarPath)
                userRepository.saveUser(updatedUser)
            }
        }
    }

    /**
     * 更新用户名字
     */
    fun updateName(userId: String, name: String) {
        viewModelScope.launch {
            val currentUser = userRepository.getUserById(userId)
            if (currentUser != null) {
                val updatedUser = currentUser.copy(name = name)
                userRepository.saveUser(updatedUser)
            }
        }
    }

    /**
     * 更新用户简介
     */
    fun updateBio(userId: String, bio: String?) {
        viewModelScope.launch {
            val currentUser = userRepository.getUserById(userId)
            if (currentUser != null) {
                val updatedUser = currentUser.copy(bio = bio)
                userRepository.saveUser(updatedUser)
            }
        }
    }

    /**
     * 初始化用户数据（用于测试，插入假数据）
     */
    fun initMockData(userId: String) {
        viewModelScope.launch {
            // 如果当前用户不存在，则插入一批基于 MockVideoCatalog 的演示用户（包含当前用户）
            val existingUser = userRepository.getUserById(userId)
            if (existingUser == null) {
                val mockUsers = MockUserCatalog.buildMockUsers(currentUserId = userId)
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
            }
        }
    }
}

