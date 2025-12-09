package com.ucw.beatu.business.user.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ucw.beatu.business.user.domain.model.User
import com.ucw.beatu.business.user.domain.model.UserWork
import com.ucw.beatu.business.user.domain.repository.UserRepository
import com.ucw.beatu.business.user.domain.repository.UserWorksRepository
import com.ucw.beatu.shared.database.dao.VideoDao
import com.ucw.beatu.shared.database.dao.WatchHistoryDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 用户主页 ViewModel
 */
@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val userWorksRepository: UserWorksRepository,
    private val videoDao: VideoDao,
    private val watchHistoryDao: WatchHistoryDao
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

    private val _isInteracting = MutableStateFlow(false)
    val isInteracting: StateFlow<Boolean> = _isInteracting.asStateFlow()

    private var observeWorksJob: Job? = null
    private var observeFollowStatusJob: Job? = null
    private var observeLikesCountJob: Job? = null

    /**
     * 直接设置用户数据（用于从弹窗传递完整用户数据）
     * 注意：likesCount 会被自动计算（通过查询该用户所有视频的点赞数总和）
     */
    fun setUserData(
        id: String,
        name: String,
        avatarUrl: String?,
        bio: String?,
        likesCount: Long,
        followingCount: Long,
        followersCount: Long
    ) {
        _user.value = User(
            id = id,
            name = name,
            avatarUrl = avatarUrl,
            bio = bio,
            likesCount = likesCount, // 初始值，会被自动计算覆盖
            followingCount = followingCount,
            followersCount = followersCount
        )
        _isLoading.value = false
        
        // 开始观察获赞数（通过查询该用户所有视频的点赞数总和）
        startObservingLikesCount(id)
        
        // 使用用户的ID加载作品
        viewModelScope.launch {
            switchTab(TabType.WORKS, authorId = id, currentUserId = "BEATU")
        }
    }

    /**
     * 加载用户信息（使用用户名或用户ID）
     * 如果 userName 是 "BEATU"，则通过用户ID查询；否则通过用户名查询
     */
    fun loadUser(userName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 如果 userName 是 "BEATU"，则通过用户ID查询
                val isUserId = userName == "BEATU"
                
                if (isUserId) {
                    // 通过用户ID查询
                    android.util.Log.d("UserProfileViewModel", "Loading user by ID: $userName")
                    val localUser = userRepository.getUserById(userName)
                    if (localUser != null) {
                        _user.value = localUser
                        _isLoading.value = false
                    }
                    
                    // 观察用户信息变化（使用 StateFlow 自动更新）
                    try {
                    userRepository.observeUserById(userName).collect { user ->
                        _user.value = user
                        if (user != null) {
                            _isLoading.value = false
                                // 开始观察获赞数（通过查询该用户所有视频的点赞数总和）
                                startObservingLikesCount(user.id)
                        } else {
                            // 如果本地和远程都没有，尝试从远程获取
                                try {
                            val remoteResult = userRepository.fetchUserFromRemote(userName)
                            if (remoteResult is com.ucw.beatu.shared.common.result.AppResult.Success) {
                                _user.value = remoteResult.data
                                _isLoading.value = false
                            } else {
                                        _isLoading.value = false
                                    }
                                } catch (e: kotlinx.coroutines.CancellationException) {
                                    throw e // 重新抛出取消异常
                                } catch (e: Exception) {
                                    android.util.Log.e("UserProfileViewModel", "Failed to fetch user from remote: $userName", e)
                                _isLoading.value = false
                            }
                        }
                        }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        // 协程被取消是正常的，不需要记录错误
                        android.util.Log.d("UserProfileViewModel", "User loading cancelled: $userName")
                        throw e
                    }
                } else {
                    // 通过用户名查询
                    android.util.Log.d("UserProfileViewModel", "Loading user by name: $userName")
                    var localUser = userRepository.getUserByName(userName)
                    
                    // 如果通过用户名没找到，且userName看起来像用户ID（纯数字），尝试作为用户ID查询
                    if (localUser == null && userName.all { it.isDigit() }) {
                        android.util.Log.d("UserProfileViewModel", "UserName looks like user ID, trying to get by ID: $userName")
                        localUser = userRepository.getUserById(userName)
                    }
                    
                    if (localUser != null) {
                        _user.value = localUser
                        _isLoading.value = false
                    }
                    
                    // 然后观察用户信息变化（使用 StateFlow 自动更新）
                    // 如果本地没有，会尝试从远程获取
                    try {
                        // 如果userName看起来像用户ID，使用observeUserById；否则使用observeUserByName
                        val flow = if (userName.all { it.isDigit() }) {
                            userRepository.observeUserById(userName)
                        } else {
                            userRepository.observeUserByName(userName)
                        }
                        
                        flow.collect { user ->
                        _user.value = user
                        if (user != null) {
                            _isLoading.value = false
                                // 开始观察获赞数（通过查询该用户所有视频的点赞数总和）
                                startObservingLikesCount(user.id)
                        } else {
                            // 如果本地和远程都没有，尝试从远程获取
                                try {
                                    val remoteResult = if (userName.all { it.isDigit() }) {
                                        userRepository.fetchUserFromRemote(userName)
                                    } else {
                                        userRepository.fetchUserFromRemoteByName(userName)
                                    }
                            if (remoteResult is com.ucw.beatu.shared.common.result.AppResult.Success) {
                                _user.value = remoteResult.data
                                _isLoading.value = false
                            } else {
                                _isLoading.value = false
                            }
                                } catch (e: kotlinx.coroutines.CancellationException) {
                                    throw e // 重新抛出取消异常
                                } catch (e: Exception) {
                                    android.util.Log.e("UserProfileViewModel", "Failed to fetch user from remote: $userName", e)
                                    _isLoading.value = false
                                }
                            }
                        }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        // 协程被取消是正常的，不需要记录错误
                        android.util.Log.d("UserProfileViewModel", "User loading cancelled: $userName")
                        throw e
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // 协程被取消是正常的，不需要记录错误
                android.util.Log.d("UserProfileViewModel", "User loading cancelled: $userName")
            } catch (e: Exception) {
                android.util.Log.e("UserProfileViewModel", "Failed to load user: $userName", e)
                e.printStackTrace()
                _isLoading.value = false
            }
        }
    }

    /**
     * 订阅真实视频数据（使用authorId查询）
     * @param authorId 作者ID（用于查询该用户的作品）
     */
    fun observeUserWorks(authorId: String, limit: Int = UserWorksRepository.DEFAULT_LIMIT) {
        observeWorksJob?.cancel()
        observeWorksJob = viewModelScope.launch {
            userWorksRepository.observeUserWorks(authorId, limit).collect { works ->
                _userWorks.value = works
            }
        }
    }

    /**
     * 切换不同的标签页数据源
     * @param tabType 标签类型
     * @param authorId 作者ID（用于作品查询）
     * @param currentUserId 当前用户ID（用于收藏、点赞、历史记录查询）
     * @param limit 查询限制
     */
    fun switchTab(
        tabType: TabType, 
        authorId: String, 
        currentUserId: String, // 当前用户ID（用于收藏、点赞、历史记录查询）
        limit: Int = UserWorksRepository.DEFAULT_LIMIT
    ) {
        observeWorksJob?.cancel()
        observeWorksJob = viewModelScope.launch {
            android.util.Log.d("UserProfileViewModel", "切换标签：tabType=$tabType, authorId=$authorId, currentUserId=$currentUserId")
            val flow = when (tabType) {
                TabType.WORKS -> {
                    // 从数据库查询该用户的作品（通过 authorId）
                    android.util.Log.d("UserProfileViewModel", "从数据库查询用户作品：authorId=$authorId")
                    userWorksRepository.observeUserWorks(authorId, limit)
                }
                TabType.COLLECTIONS -> {
                    // 从数据库查询用户收藏的视频（JOIN beatu_video_interaction表）
                    android.util.Log.d("UserProfileViewModel", "从数据库查询收藏视频：userId=$currentUserId")
                    userWorksRepository.observeFavoritedWorks(currentUserId, limit)
                }
                TabType.LIKES -> {
                    // 从数据库查询用户点赞的视频（JOIN beatu_video_interaction表）
                    android.util.Log.d("UserProfileViewModel", "从数据库查询点赞视频：userId=$currentUserId")
                    userWorksRepository.observeLikedWorks(currentUserId, limit)
                }
                TabType.HISTORY -> {
                    // 从数据库查询用户观看历史（JOIN beatu_watch_history表）
                    android.util.Log.d("UserProfileViewModel", "从数据库查询观看历史：userId=$currentUserId, limit=$limit")
                    // 先验证数据库中的历史记录数量
                    viewModelScope.launch {
                        try {
                            val historyCount = watchHistoryDao.getHistoryCountByUser(currentUserId)
                            android.util.Log.i("UserProfileViewModel", "数据库中的历史记录数量：$historyCount 条（userId=$currentUserId）")
                            val allHistories = watchHistoryDao.getAllHistoriesByUser(currentUserId)
                            android.util.Log.d("UserProfileViewModel", "数据库中的历史记录详情：")
                            allHistories.forEachIndexed { index, history ->
                                android.util.Log.d("UserProfileViewModel", "  历史[$index]: videoId=${history.videoId}, watchedAt=${history.watchedAt}")
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("UserProfileViewModel", "查询历史记录数量失败", e)
                        }
                    }
                    userWorksRepository.observeHistoryWorks(currentUserId, limit)
                }
            }
            flow.collect { works ->
                android.util.Log.d("UserProfileViewModel", "用户作品数据更新：${works.size} 条")
                if (tabType == TabType.HISTORY && works.isNotEmpty()) {
                    android.util.Log.d("UserProfileViewModel", "历史记录详情：")
                    works.forEachIndexed { index, work ->
                        android.util.Log.d("UserProfileViewModel", "  历史[$index]: videoId=${work.id}, title=${work.title}")
                    }
                }
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
     * 关注用户（乐观更新：先更新本地，然后异步同步到远程，失败则回滚）
     * 使用 isInteracting 防止重复点击
     */
    fun followUser(targetUserId: String, currentUserId: String) {
        // 防止重复点击
        if (_isInteracting.value) {
            android.util.Log.d("UserProfileViewModel", "Follow user already in progress, ignoring duplicate click")
            return
        }

        viewModelScope.launch {
            try {
            android.util.Log.d("UserProfileViewModel", "Following user: targetUserId=$targetUserId, currentUserId=$currentUserId")
                // 设置交互状态
                _isInteracting.value = true
                _followOperationError.value = null

                // 保存当前用户状态，用于回滚
                val currentUser = _user.value
                // 1. 乐观更新：先更新本地数据库（会触发 observeIsFollowing 的 Flow 更新）
                userRepository.followUser(currentUserId, targetUserId)
                // 2. 乐观更新粉丝数
                currentUser?.let { user ->
                _user.value = user.copy(followersCount = user.followersCount + 1)
                }
                // 注意：远程同步和回滚逻辑在 Repository 中处理，通过 observeFollowSyncResult 监听结果
            } catch (e: kotlinx.coroutines.CancellationException) {
                // 协程被取消是正常的，不需要记录错误
                android.util.Log.d("UserProfileViewModel", "Follow user cancelled: targetUserId=$targetUserId")
                _isInteracting.value = false
            } catch (e: Exception) {
                android.util.Log.e("UserProfileViewModel", "Failed to follow user: targetUserId=$targetUserId", e)
                // 本地更新失败，显示错误提示
                _followOperationError.value = "操作失败，请重试"
                _isInteracting.value = false
            }
        }
    }

    /**
     * 取消关注用户（乐观更新：先更新本地，然后异步同步到远程，失败则回滚）
     * 使用 isInteracting 防止重复点击
     */
    fun unfollowUser(targetUserId: String, currentUserId: String) {
        // 防止重复点击
        if (_isInteracting.value) {
            android.util.Log.d("UserProfileViewModel", "Unfollow user already in progress, ignoring duplicate click")
            return
        }

        viewModelScope.launch {
            try {
            android.util.Log.d("UserProfileViewModel", "Unfollowing user: targetUserId=$targetUserId, currentUserId=$currentUserId")
                // 设置交互状态
                _isInteracting.value = true
                _followOperationError.value = null

                // 保存当前用户状态，用于回滚
                val currentUser = _user.value
                // 1. 乐观更新：先更新本地数据库（会触发 observeIsFollowing 的 Flow 更新）
            userRepository.unfollowUser(currentUserId, targetUserId)
                // 2. 乐观更新粉丝数
                currentUser?.let { user ->
                _user.value = user.copy(followersCount = (user.followersCount - 1).coerceAtLeast(0))
                }
                // 注意：远程同步和回滚逻辑在 Repository 中处理，通过 observeFollowSyncResult 监听结果
            } catch (e: kotlinx.coroutines.CancellationException) {
                // 协程被取消是正常的，不需要记录错误
                android.util.Log.d("UserProfileViewModel", "Unfollow user cancelled: targetUserId=$targetUserId")
                _isInteracting.value = false
            } catch (e: Exception) {
                android.util.Log.e("UserProfileViewModel", "Failed to unfollow user: targetUserId=$targetUserId", e)
                // 本地更新失败，显示错误提示
                _followOperationError.value = "操作失败，请重试"
                _isInteracting.value = false
            }
        }
    }

    /**
     * 开始观察关注同步结果（处理远程同步成功/失败，失败时回滚乐观更新）
     */
    fun startObservingFollowSyncResult() {
        viewModelScope.launch {
            userRepository.observeFollowSyncResult().collect { result ->
                when (result) {
                    is UserRepository.FollowSyncResult.Success -> {
                        // 同步成功，清除错误信息和交互状态
                        _followOperationError.value = null
                        _isInteracting.value = false
                        android.util.Log.d("UserProfileViewModel", "Follow sync success: isFollow=${result.isFollow}, targetUserId=${result.targetUserId}")
                    }
                    is UserRepository.FollowSyncResult.Error -> {
                        // 同步失败，回滚乐观更新的粉丝数
                        android.util.Log.e("UserProfileViewModel", "Follow sync error: ${result.error}, isFollow=${result.isFollow}, targetUserId=${result.targetUserId}")
                        _user.value?.let { user ->
                            // 根据操作类型回滚粉丝数
                            if (result.isFollow) {
                                // 关注失败，回滚：粉丝数 -1
                                _user.value = user.copy(followersCount = (user.followersCount - 1).coerceAtLeast(0))
                            } else {
                                // 取消关注失败，回滚：粉丝数 +1
                                _user.value = user.copy(followersCount = user.followersCount + 1)
                            }
                        }
                        // 显示错误信息（关注状态已由 Repository 回滚，通过 observeIsFollowing 自动更新）
                        _followOperationError.value = result.error
                        _isInteracting.value = false
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
    
    /**
     * 开始观察用户的获赞数（通过查询该用户所有视频的点赞数总和）
     * 当视频点赞数变化时，自动更新用户的 likesCount
     */
    private fun startObservingLikesCount(authorId: String) {
        observeLikesCountJob?.cancel()
        observeLikesCountJob = viewModelScope.launch {
            android.util.Log.d("UserProfileViewModel", "开始观察获赞数：authorId=$authorId")
            videoDao.observeTotalLikesCountByAuthorId(authorId).collect { totalLikesCount: Long ->
                android.util.Log.d("UserProfileViewModel", "获赞数更新：authorId=$authorId, totalLikesCount=$totalLikesCount")
                // 更新当前用户的 likesCount
                _user.value?.let { currentUser: User ->
                    _user.value = currentUser.copy(likesCount = totalLikesCount)
                }
            }
        }
    }
    
    /**
     * 停止观察获赞数
     */
    fun stopObservingLikesCount() {
        observeLikesCountJob?.cancel()
        observeLikesCountJob = null
    }
}

