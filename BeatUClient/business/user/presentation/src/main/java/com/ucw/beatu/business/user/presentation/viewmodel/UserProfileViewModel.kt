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

    private val _isFollowing = MutableStateFlow<Boolean?>(null)
    val isFollowing: StateFlow<Boolean?> = _isFollowing.asStateFlow()

    private val _followOperationError = MutableStateFlow<String?>(null)
    val followOperationError: StateFlow<String?> = _followOperationError.asStateFlow()

    private var observeWorksJob: Job? = null
    private var observeFollowStatusJob: Job? = null

    /**
     * 加载用户信息（使用用户名或用户ID）
     * 如果 userName 是 "current_user"，则通过用户ID查询；否则通过用户名查询
     */
    fun loadUser(userName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 如果 userName 是 "current_user"，则通过用户ID查询
                val isUserId = userName == "current_user"
                
                if (isUserId) {
                    // 通过用户ID查询
                    android.util.Log.d("UserProfileViewModel", "Loading user by ID: $userName")
                    val localUser = userRepository.getUserById(userName)
                    if (localUser != null) {
                        _user.value = localUser
                        _isLoading.value = false
                    }
                    
                    // 观察用户信息变化（使用 StateFlow 自动更新）
                    userRepository.observeUserById(userName).collect { user ->
                        _user.value = user
                        if (user != null) {
                            _isLoading.value = false
                        } else {
                            // 如果本地和远程都没有，尝试从远程获取
                            val remoteResult = userRepository.fetchUserFromRemote(userName)
                            if (remoteResult is com.ucw.beatu.shared.common.result.AppResult.Success) {
                                _user.value = remoteResult.data
                                _isLoading.value = false
                            } else {
                                _isLoading.value = false
                            }
                        }
                    }
                } else {
                    // 通过用户名查询
                    android.util.Log.d("UserProfileViewModel", "Loading user by name: $userName")
                    val localUser = userRepository.getUserByName(userName)
                    if (localUser != null) {
                        _user.value = localUser
                        _isLoading.value = false
                    }
                    
                    // 然后观察用户信息变化（使用 StateFlow 自动更新）
                    // 如果本地没有，会尝试从远程获取
                    userRepository.observeUserByName(userName).collect { user ->
                        _user.value = user
                        if (user != null) {
                            _isLoading.value = false
                        } else {
                            // 如果本地和远程都没有，尝试从远程获取
                            val remoteResult = userRepository.fetchUserFromRemoteByName(userName)
                            if (remoteResult is com.ucw.beatu.shared.common.result.AppResult.Success) {
                                _user.value = remoteResult.data
                                _isLoading.value = false
                            } else {
                                _isLoading.value = false
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("UserProfileViewModel", "Failed to load user: $userName", e)
                e.printStackTrace()
                _isLoading.value = false
            }
        }
    }

    /**
     * 订阅真实视频数据（使用authorName查询）
     * @param authorName 作者名称（用于查询该用户的作品）
     */
    fun observeUserWorks(authorName: String, limit: Int = UserWorksRepository.DEFAULT_LIMIT) {
        observeWorksJob?.cancel()
        observeWorksJob = viewModelScope.launch {
            userWorksRepository.observeUserWorks(authorName, limit).collect { works ->
                _userWorks.value = works
            }
        }
    }

    /**
     * 切换不同的标签页数据源
     * @param tabType 标签类型
     * @param authorName 作者名称（用于作品查询）
     * @param currentUserId 当前用户ID（用于收藏、点赞、历史记录查询）
     * @param limit 查询限制
     */
    fun switchTab(
        tabType: TabType, 
        authorName: String, 
        currentUserId: String, // 当前用户ID（用于收藏、点赞、历史记录查询）
        limit: Int = UserWorksRepository.DEFAULT_LIMIT
    ) {
        observeWorksJob?.cancel()
        observeWorksJob = viewModelScope.launch {
            val flow = when (tabType) {
                TabType.WORKS -> userWorksRepository.observeUserWorks(authorName, limit)
                TabType.COLLECTIONS -> userWorksRepository.observeFavoritedWorks(currentUserId, limit)
                TabType.LIKES -> userWorksRepository.observeLikedWorks(currentUserId, limit)
                TabType.HISTORY -> userWorksRepository.observeHistoryWorks(currentUserId, limit)
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

    /**
     * 关注用户（直接更新本地数据库，异步同步到远程）
     */
    fun followUser(targetUserId: String, currentUserId: String) {
        viewModelScope.launch {
            android.util.Log.d("UserProfileViewModel", "Following user: targetUserId=$targetUserId, currentUserId=$currentUserId")
            // 直接更新本地数据库（会触发 observeIsFollowing 的 Flow 更新）
            userRepository.followUser(currentUserId, targetUserId)
            // 乐观更新粉丝数
            _user.value?.let { user ->
                _user.value = user.copy(followersCount = user.followersCount + 1)
            }
        }
    }

    /**
     * 取消关注用户（直接更新本地数据库，异步同步到远程）
     */
    fun unfollowUser(targetUserId: String, currentUserId: String) {
        viewModelScope.launch {
            android.util.Log.d("UserProfileViewModel", "Unfollowing user: targetUserId=$targetUserId, currentUserId=$currentUserId")
            // 直接更新本地数据库（会触发 observeIsFollowing 的 Flow 更新）
            userRepository.unfollowUser(currentUserId, targetUserId)
            // 乐观更新粉丝数
            _user.value?.let { user ->
                _user.value = user.copy(followersCount = (user.followersCount - 1).coerceAtLeast(0))
            }
        }
    }

    /**
     * 开始观察关注同步结果
     */
    fun startObservingFollowSyncResult() {
        viewModelScope.launch {
            userRepository.observeFollowSyncResult().collect { result ->
                when (result) {
                    is UserRepository.FollowSyncResult.Success -> {
                        // 同步成功，清除错误信息
                        _followOperationError.value = null
                    }
                    is UserRepository.FollowSyncResult.Error -> {
                        // 同步失败，显示错误信息
                        _followOperationError.value = result.error
                        android.util.Log.e("UserProfileViewModel", "Follow sync error: ${result.error}")
                    }
                }
            }
        }
    }
    
    /**
     * 清除关注操作错误信息
     */
    fun clearFollowOperationError() {
        _followOperationError.value = null
    }

    /**
     * 开始观察关注状态（从本地数据库）
     */
    fun startObservingFollowStatus(currentUserId: String, targetUserId: String) {
        observeFollowStatusJob?.cancel()
        observeFollowStatusJob = viewModelScope.launch {
            android.util.Log.d("UserProfileViewModel", "Starting to observe follow status: targetUserId=$targetUserId, currentUserId=$currentUserId")
            userRepository.observeIsFollowing(currentUserId, targetUserId).collect { isFollowing ->
                _isFollowing.value = isFollowing
                android.util.Log.d("UserProfileViewModel", "Follow status updated: $isFollowing")
            }
        }
    }

    /**
     * 停止观察关注状态
     */
    fun stopObservingFollowStatus() {
        observeFollowStatusJob?.cancel()
        observeFollowStatusJob = null
    }
}

