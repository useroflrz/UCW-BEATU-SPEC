package com.ucw.beatu.business.videofeed.presentation.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ucw.beatu.business.videofeed.domain.usecase.GetFeedUseCase
import com.ucw.beatu.business.videofeed.presentation.mapper.toVideoItem
import com.ucw.beatu.shared.common.model.FeedContentType
import com.ucw.beatu.shared.common.model.VideoItem
import com.ucw.beatu.shared.common.model.VideoOrientation
import com.ucw.beatu.shared.common.result.AppResult
import com.ucw.beatu.shared.player.VideoPlayer
import com.ucw.beatu.shared.player.model.VideoSource
import com.ucw.beatu.shared.player.pool.VideoPlayerPool
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlin.random.Random
import javax.inject.Inject

data class RecommendUiState(
    val videoList: List<VideoItem> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    /**
     * 是否已经把后端的视频页全部加载完：
     * - true：不再向后端请求更多数据，前端在已有列表中做“无限循环”
     * - false：滑到尾部仍会继续向后端要下一页
     */
    val hasLoadedAllFromBackend: Boolean = false
)

@HiltViewModel
class RecommendViewModel @Inject constructor(
    application: Application,
    private val playerPool: VideoPlayerPool,
    private val getFeedUseCase: GetFeedUseCase
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(RecommendUiState())
    val uiState: StateFlow<RecommendUiState> = _uiState.asStateFlow()

    private var currentPlayer: VideoPlayer? = null
    private var currentVideoId: Long? = null  // ✅ 修改：从 String? 改为 Long?（视频 ID 现在是 Long 类型）
    private var currentPage = 1
    // 记录每个视频的数据来源（"local"、"remote" 或 "mock"）
    private val videoSourceMap = mutableMapOf<Long, String>()  // ✅ 修改：key 从 String 改为 Long
    // 从资源或默认值读取页面大小配置（避免直接依赖 app 的 R）
    private val pageSize = getIntegerResource(
        application,
        "video_page_size",
        DEFAULT_PAGE_SIZE
    )
    private var isLoadingMore = false // 防止重复加载更多
    private var loadMoreJob: Job? = null // 加载更多的Job，用于取消
    
    init {
        // 初始化时立即加载视频列表（会先显示本地缓存，然后后台更新）
        loadVideoList()
    }

    /**
     * 加载视频列表（使用GetFeedUseCase）
     * 优化：立即显示本地缓存，不等待网络请求，确保UI流畅
     */
    private fun loadVideoList() {
        viewModelScope.launch {
            currentPage = 1
            // 不立即设置isLoading，让本地缓存先显示
            _uiState.value = _uiState.value.copy(error = null)
            
            getFeedUseCase(currentPage, pageSize)
                .catch { e ->
                    // 即使出错，也保持当前显示的数据（可能是本地缓存或mock数据）
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "加载失败"
                    )
                }
                .collect { result ->
                    when (result) {
                        is AppResult.Loading -> {
                            // 只有在没有数据时才显示loading，避免闪烁
                            if (_uiState.value.videoList.isEmpty()) {
                                _uiState.value = _uiState.value.copy(isLoading = true)
                            }
                        }
                        is AppResult.Success -> {
                            val videos = result.data.map { it.toVideoItem() }
                            // 记录数据来源
                            val source = result.metadata["source"] as? String ?: "unknown"
                            
                            // 对视频列表进行随机打乱，确保每次打开app顺序不同
                            // 无论是本地缓存还是远程数据，都应该打乱，确保每次打开app顺序不同
                            val currentList = _uiState.value.videoList
                            val isFirstLoad = currentList.isEmpty()
                            val isReplacingLocalCache = !isFirstLoad && 
                                                       currentList.any { videoSourceMap[it.id] == "local" } && 
                                                       source == "remote"
                            
                            // 打乱条件：
                            // 1. 首次加载时，无论是本地缓存还是远程数据都打乱
                            // 2. 或者当前列表是本地缓存数据，现在收到远程数据（需要替换并打乱）
                            val shouldShuffle = isFirstLoad || isReplacingLocalCache
                            
                            val finalVideos = if (shouldShuffle) {
                                // 使用当前时间戳作为随机种子，确保每次打开app顺序不同
                                val shuffled = videos.shuffled(Random(System.currentTimeMillis()))
                                android.util.Log.d("RecommendViewModel", "loadVideoList: Shuffled ${videos.size} videos using timestamp seed, source=$source, isFirstLoad=$isFirstLoad, isReplacingLocalCache=$isReplacingLocalCache")
                                shuffled
                            } else {
                                videos
                            }
                            
                            // 记录数据来源（在打乱后，这样日志顺序和实际播放顺序一致）
                            finalVideos.forEach { video ->
                                videoSourceMap[video.id] = source
                            }
                            
                            // 打印打乱后的顺序（用于调试）
                            if (shouldShuffle) {
                                android.util.Log.d("RecommendViewModel", "loadVideoList: Final shuffled order:")
                                finalVideos.forEachIndexed { index, video ->
                                    android.util.Log.d("RecommendViewModel", "  [$index] ${video.id} (source=$source)")
                            }
                            }
                            
                            // 如果第一页数量不足 pageSize，可以直接认为后端数据已经加载完
                            val hasLoadedAll = finalVideos.size < pageSize
                            android.util.Log.d("RecommendViewModel", "loadVideoList: Success, loaded ${finalVideos.size} videos, source=$source, shuffled=$shouldShuffle")
                            _uiState.value = _uiState.value.copy(
                                videoList = finalVideos,
                                isLoading = false,
                                error = null,
                                hasLoadedAllFromBackend = hasLoadedAll
                            )
                        }
                        is AppResult.Error -> {
                            android.util.Log.e("RecommendViewModel", "loadVideoList: Error - ${result.message ?: result.throwable?.message}", result.throwable)
                            // 即使出错，也保持当前显示的数据，不阻塞UI
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                // 静默失败，不显示错误，因为已经有本地缓存或mock数据
                                error = if (_uiState.value.videoList.isEmpty()) {
                                    result.message ?: result.throwable?.message ?: "加载失败"
                                } else null
                            )
                        }
                    }
                }
        }
    }

    /**
     * 刷新视频列表（下拉刷新）
     * 获取最新 5-10 条视频，插入到列表顶部
     */
    fun refreshVideoList() {
        android.util.Log.d("RecommendViewModel", "refreshVideoList: called")
        viewModelScope.launch {
            // 刷新时获取 5-10 条视频（随机数量，增加变化性）
            val refreshCount = (5..10).random()
            android.util.Log.d("RecommendViewModel", "refreshVideoList: setting isRefreshing=true, refreshCount=$refreshCount")
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            
            getFeedUseCase(1, refreshCount)
                .catch { e ->
                    android.util.Log.e("RecommendViewModel", "refreshVideoList: error caught", e)
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        error = e.message ?: "刷新失败"
                    )
                }
                .collect { result ->
                    android.util.Log.d("RecommendViewModel", "refreshVideoList: received result type=${result.javaClass.simpleName}")
                    when (result) {
                        is AppResult.Loading -> {
                            android.util.Log.d("RecommendViewModel", "refreshVideoList: Loading state")
                            // Loading状态已在开始时设置
                        }
                        is AppResult.Success -> {
                            val newVideos = result.data.map { it.toVideoItem() }
                            // 记录数据来源
                            val source = result.metadata["source"] as? String ?: "unknown"
                            newVideos.forEach { video ->
                                videoSourceMap[video.id] = source
                                android.util.Log.d("RecommendViewModel", 
                                    "refreshVideoList: 视频 ${video.id} 数据来源=$source, authorAvatar=${video.authorAvatar ?: "null"}"
                                )
                            }
                            
                            // 刷新时，只在收到远程数据时才完成刷新
                            // 本地缓存数据不应该结束刷新状态
                            val isRemoteData = source == "remote"
                            android.util.Log.d("RecommendViewModel", "refreshVideoList: Success, loaded ${newVideos.size} videos, source=$source, isRemoteData=$isRemoteData")
                            
                            if (isRemoteData) {
                                // 远程数据到达，将新视频插入到列表顶部
                                val currentList = _uiState.value.videoList.toMutableList()
                                
                                // 去重：移除已存在的视频（根据 ID）
                                val existingIds = currentList.map { it.id }.toSet()
                                val uniqueNewVideos = newVideos.filter { it.id !in existingIds }
                                
                                if (uniqueNewVideos.isNotEmpty()) {
                                    // 将新视频插入到列表顶部
                                    currentList.addAll(0, uniqueNewVideos)
                                    android.util.Log.d("RecommendViewModel", "refreshVideoList: Inserted ${uniqueNewVideos.size} new videos at top, total=${currentList.size}")
                                    
                                    // 刷新后重新打乱整个列表，确保顺序变化
                                    val shuffledList = currentList.shuffled(Random(System.currentTimeMillis()))
                                    android.util.Log.d("RecommendViewModel", "refreshVideoList: Shuffled entire list after refresh")
                                    
                                    // 记录数据来源（在打乱后）
                                    shuffledList.forEach { video ->
                                        videoSourceMap[video.id] = source
                                    }
                                    
                                    // 打印打乱后的顺序（用于调试）
                                    android.util.Log.d("RecommendViewModel", "refreshVideoList: Final shuffled order:")
                                    shuffledList.forEachIndexed { index, video ->
                                        android.util.Log.d("RecommendViewModel", "  [$index] ${video.id}")
                                    }
                                    
                                    // 刷新完成
                                    android.util.Log.d("RecommendViewModel", "refreshVideoList: Remote data received, setting isRefreshing=false")
                                    _uiState.value = _uiState.value.copy(
                                        videoList = shuffledList,
                                        isRefreshing = false,
                                        error = null,
                                        // 刷新不影响 hasLoadedAllFromBackend 状态
                                        hasLoadedAllFromBackend = _uiState.value.hasLoadedAllFromBackend
                                    )
                                } else {
                                    android.util.Log.d("RecommendViewModel", "refreshVideoList: No new videos to insert (all duplicates)")
                                    // 即使没有新视频，也重新打乱列表
                                    val shuffledList = currentList.shuffled(Random(System.currentTimeMillis()))
                                    android.util.Log.d("RecommendViewModel", "refreshVideoList: Shuffled list even though no new videos")
                                    
                                    // 打印打乱后的顺序（用于调试）
                                    android.util.Log.d("RecommendViewModel", "refreshVideoList: Final shuffled order:")
                                    shuffledList.forEachIndexed { index, video ->
                                        android.util.Log.d("RecommendViewModel", "  [$index] ${video.id}")
                                    }
                                    
                            _uiState.value = _uiState.value.copy(
                                        videoList = shuffledList,
                                isRefreshing = false,
                                error = null,
                                        hasLoadedAllFromBackend = _uiState.value.hasLoadedAllFromBackend
                            )
                                }
                            } else {
                                // 本地缓存数据，暂时不更新列表，等待远程数据
                                android.util.Log.d("RecommendViewModel", "refreshVideoList: Local cache data, waiting for remote data")
                            }
                        }
                        is AppResult.Error -> {
                            android.util.Log.e("RecommendViewModel", "refreshVideoList: Error, setting isRefreshing=false")
                            _uiState.value = _uiState.value.copy(
                                isRefreshing = false,
                                error = result.message ?: result.throwable.message ?: "刷新失败"
                            )
                        }
                    }
                }
        }
    }

    /**
     * 加载更多视频（上拉加载）
     * 优化：异步加载，不阻塞UI滑动，即使失败也不影响用户体验
     */
    fun loadMoreVideos() {
        // 避免重复加载；如果已经确认后端没有更多数据，则直接走前端"无限循环"逻辑，不再请求
        if (isLoadingMore || _uiState.value.hasLoadedAllFromBackend) {
            return
        }
        
        // 取消之前的加载任务，避免重复请求
        loadMoreJob?.cancel()
        
        isLoadingMore = true
        loadMoreJob = viewModelScope.launch {
            val nextPage = currentPage + 1
            
            getFeedUseCase(nextPage, pageSize)
                .catch { e ->
                    // 加载失败时，静默失败，不影响用户滑动
                    isLoadingMore = false
                    android.util.Log.w("RecommendViewModel", "loadMoreVideos: Error - ${e.message}", e)
                }
                .collect { result ->
                    when (result) {
                        is AppResult.Loading -> {
                            // 加载更多时不显示全局loading，保持UI流畅
                        }
                        is AppResult.Success -> {
                            isLoadingMore = false
                            val moreVideos = result.data.map { it.toVideoItem() }
                            val currentList = _uiState.value.videoList.toMutableList()

                            // 记录数据来源
                            val source = result.metadata["source"] as? String ?: "unknown"
                            moreVideos.forEach { video ->
                                videoSourceMap[video.id] = source
                                android.util.Log.d("RecommendViewModel", 
                                    "loadMoreVideos: 视频 ${video.id} 数据来源=$source, authorAvatar=${video.authorAvatar ?: "null"}"
                                )
                            }

                            // 如果此次没有再返回新数据，说明后端所有页已经加载完
                            if (moreVideos.isEmpty()) {
                                _uiState.value = _uiState.value.copy(
                                    videoList = currentList,
                                    error = null,
                                    hasLoadedAllFromBackend = true
                                )
                                return@collect
                            } else {
                                currentPage = nextPage // 更新页码
                                
                                // 基于 videoId 去重，避免重复加载
                                val existingIds = currentList.map { it.id }.toSet()
                                val uniqueNewVideos = moreVideos.filter { it.id !in existingIds }
                                
                                if (uniqueNewVideos.isEmpty()) {
                                    // 没有新视频，标记为已加载完
                                    _uiState.value = _uiState.value.copy(
                                        hasLoadedAllFromBackend = true
                                    )
                                    return@collect
                                }
                                
                                currentList.addAll(uniqueNewVideos)
                                
                                // 加载更多后，重新打乱整个列表，确保顺序变化
                                val shuffledList = currentList.shuffled(Random(System.currentTimeMillis()))
                                android.util.Log.d("RecommendViewModel", "loadMoreVideos: Shuffled entire list after loading more")
                                
                                val hasLoadedAll = moreVideos.size < pageSize
                                android.util.Log.d("RecommendViewModel", "loadMoreVideos: Success, loaded ${uniqueNewVideos.size} new videos, source=$source")
                                _uiState.value = _uiState.value.copy(
                                    videoList = shuffledList,
                                    error = null,
                                    hasLoadedAllFromBackend = hasLoadedAll
                                )
                            }
                        }
                        is AppResult.Error -> {
                            isLoadingMore = false
                            // 静默失败，不显示错误，不影响用户滑动
                            // 如果后端数据已加载完，标记为已加载完，启用无限循环
                            android.util.Log.w("RecommendViewModel", "loadMoreVideos: Error - ${result.message ?: result.throwable?.message}", result.throwable)
                            // 如果连续失败多次，可以考虑标记为已加载完，启用无限循环
                            // 这里暂时不处理，让用户继续尝试
                        }
                    }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        releaseCurrentPlayer()
    }
    
    private fun releaseCurrentPlayer() {
        currentVideoId?.let { videoId ->
            playerPool.release(videoId)
        }
        currentPlayer = null
        currentVideoId = null
    }


    fun restoreState(videos: List<VideoItem>, restoredPage: Int) {
        currentPage = restoredPage.coerceAtLeast(1)
        _uiState.value = _uiState.value.copy(
            videoList = videos,
            isLoading = false,
            isRefreshing = false,
            error = null
        )
    }
    
    /**
     * 获取视频的数据来源（用于日志）
     * ✅ 修改：参数类型从 String 改为 Long
     */
    fun getVideoSource(videoId: Long): String {
        return videoSourceMap[videoId] ?: "unknown"
    }

    fun getCurrentPage(): Int = currentPage

    /**
     * 推荐页中的静态图文+BGM，用于体验"图文+音乐"效果
     * index 用于区分出现在不同页的示例卡片，方便后续接入真实后端数据时替换
     * ✅ 修改：id 从 String 改为 Long（使用字符串哈希值转换为 Long）
     */
    private fun createMockImagePost(index: Int): VideoItem {
        val mockIdString = "image_post_mock_$index"
        // 使用字符串的哈希值转换为 Long，确保唯一性
        val mockId = mockIdString.hashCode().toLong()
        return VideoItem(
            id = mockId,
            videoUrl = "",
            title = "这是一个图文+BGM 示例（第 $index 段）",
            authorName = "BeatU 官方",
            likeCount = 1314,
            commentCount = 99,
            favoriteCount = 520,
            shareCount = 66,
            orientation = VideoOrientation.PORTRAIT,
            type = FeedContentType.IMAGE_POST,
            imageUrls = listOf(
                "https://images.pexels.com/photos/572897/pexels-photo-572897.jpeg",
                "https://images.pexels.com/photos/210186/pexels-photo-210186.jpeg",
                "https://images.pexels.com/photos/1103970/pexels-photo-1103970.jpeg"
            ),
            bgmUrl = "https://samplelib.com/lib/preview/mp3/sample-6s.mp3"
        )
    }

    companion object {
        private const val DEFAULT_PAGE_SIZE = 10

        private fun getIntegerResource(
            context: Context,
            name: String,
            defaultValue: Int
        ): Int {
            val resId = context.resources.getIdentifier(name, "integer", context.packageName)
            return if (resId != 0) context.resources.getInteger(resId) else defaultValue
        }
    }
}

