# 刷视频行为代码级流程文档

> 本文档描述用户从打开 App 到刷视频（上下滑动切换视频）的完整代码级流程，涵盖模块调用链、数据流转路径与关键代码触发点。基于 Clean Architecture + MVVM 架构，参考 `docs/architecture.md` 与 `docs/client_tech_whitepaper.md`。

## 目录

1. [App 启动到 Feed 展示](#1-app-启动到-feed-展示)
2. [首屏视频加载与播放](#2-首屏视频加载与播放)
3. [滑动切换视频（核心流程）](#3-滑动切换视频核心流程)
4. [预加载策略](#4-预加载策略)
5. [数据流转总结](#5-数据流转总结)

---

## 1. App 启动到 Feed 展示

### 1.1 入口流程

**代码路径：`app/` → `feature/feed/`**

```
用户点击 App 图标
  ↓
Android 系统启动进程
  ↓
app/src/main/AndroidManifest.xml
  → MainActivity (android:name="com.beatu.app.MainActivity")
  ↓
app/src/main/java/com/beatu/app/MainActivity.kt
  → onCreate()
    → setContentView(R.layout.activity_main)
    → initNavigation() // 初始化 Navigation Component
    → showSplashScreen() // Logo → Loading 动画
    ↓
app/src/main/java/com/beatu/app/navigation/NavGraph.kt
  → 定义路由：splash → feed
    ↓
feature/feed/src/main/java/com/beatu/feed/presentation/FeedFragment.kt
  → onCreateView() // Fragment 创建
    → inflate(R.layout.fragment_feed)
    → findViewById<ViewPager2>(R.id.viewpager_feed)
```

### 1.2 FeedFragment 初始化

**代码模块：`feature/feed/presentation/`**

```kotlin
// feature/feed/src/main/java/com/beatu/feed/presentation/FeedFragment.kt

class FeedFragment : Fragment() {
    private lateinit var viewModel: FeedViewModel // 由 Hilt 注入
    private lateinit var viewPager2: ViewPager2
    private lateinit var adapter: FeedAdapter

    override fun onCreateView(...): View {
        val binding = FragmentFeedBinding.inflate(...)
        viewPager2 = binding.viewpagerFeed
        // 设置纵向滑动
        viewPager2.orientation = ViewPager2.ORIENTATION_VERTICAL
        // 设置预加载页数（关键：触发 N+1 预加载）
        viewPager2.offscreenPageLimit = 1
        
        adapter = FeedAdapter(viewModel)
        viewPager2.adapter = adapter
        
        // 监听页面切换（核心：触发播放器 attach/detach）
        viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewModel.onPageSelected(position)
            }
            override fun onPageScrollStateChanged(state: Int) {
                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    viewModel.onPageScrollComplete()
                }
            }
        })
        
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 观察 ViewModel 的 UIState
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                // 更新 UI：播放状态、点赞数、评论数等
                updateUI(state)
            }
        }
    }
}
```

### 1.3 FeedViewModel 初始化与数据加载

**代码模块：`feature/feed/presentation/`**

```kotlin
// feature/feed/src/main/java/com/beatu/feed/presentation/FeedViewModel.kt

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val feedUseCase: FeedUseCase, // Domain 层
    private val playerPool: PlayerPool,   // Infrastructure 层
    private val metricsCollector: MetricsCollector // core/common
) : ViewModel() {

    private val _uiState = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private var pagingData: Flow<PagingData<Video>>? = null

    init {
        // 初始化时加载 Feed 数据
        loadFeed(channel = Channel.RECOMMEND, cursor = null)
    }

    private fun loadFeed(channel: Channel, cursor: String?) {
        viewModelScope.launch {
            try {
                // 调用 Domain 层 UseCase
                pagingData = feedUseCase.fetchFeed(channel, cursor)
                
                _uiState.value = FeedUiState.Success(pagingData)
                
                // 记录启动指标
                metricsCollector.recordColdStart(System.currentTimeMillis() - appStartTime)
            } catch (e: Exception) {
                _uiState.value = FeedUiState.Error(e.message)
            }
        }
    }
}
```

**数据流路径：**

```
FeedViewModel.loadFeed()
  ↓
feature/feed/src/main/java/com/beatu/feed/domain/FeedUseCase.kt
  → fetchFeed(channel, cursor)
    ↓
domain/src/main/java/com/beatu/domain/repository/FeedRepository.kt (接口)
  ↓
data/src/main/java/com/beatu/data/repository/impl/FeedRepositoryImpl.kt (实现)
  → fetchFeed(channel, cursor)
    ↓
data/src/main/java/com/beatu/data/source/remote/FeedRemoteDataSource.kt
  → retrofitService.getFeed(channel, cursor, pageSize)
    ↓
core/network/src/main/java/com/beatu/network/api/BeatUGatewayApi.kt
  → GET /api/v1/feed?channel=recommend&cursor=xxx&pageSize=20
    ↓
BeatUGateway (服务端)
  → BeatUContentService (获取视频列表)
    ↓
返回 FeedResponse { items: List<Video>, nextCursor }
    ↓
FeedRepositoryImpl 映射 DTO → Domain Model
    ↓
返回 Flow<PagingData<Video>>
    ↓
FeedUseCase 返回给 ViewModel
    ↓
FeedViewModel._uiState.value = FeedUiState.Success(pagingData)
    ↓
FeedFragment 观察到状态变化，通知 Adapter 更新
```

---

## 2. 首屏视频加载与播放

### 2.1 FeedAdapter 创建 ViewHolder

**代码模块：`feature/feed/presentation/`**

```kotlin
// feature/feed/src/main/java/com/beatu/feed/presentation/FeedAdapter.kt

class FeedAdapter(
    private val viewModel: FeedViewModel
) : RecyclerView.Adapter<FeedViewHolder>() {

    private val differ = AsyncPagingDataDiffer<Video>(
        diffCallback = VideoDiffCallback(),
        updateCallback = AdapterListUpdateCallback(this)
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        // 加载 item_feed_video.xml 布局
        val binding = ItemFeedVideoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FeedViewHolder(binding, viewModel)
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        // Paging3 会自动调用 getItem(position)
        val video = differ.getItem(position) ?: return
        
        holder.bind(video, position)
    }
}

class FeedViewHolder(
    private val binding: ItemFeedVideoBinding,
    private val viewModel: FeedViewModel
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(video: Video, position: Int) {
        // 1. 显示封面图（首帧占位）
        Glide.with(binding.coverImage)
            .load(video.coverUrl)
            .into(binding.coverImage)

        // 2. 显示视频信息
        binding.videoTitle.text = video.title
        binding.authorName.text = video.author.nickname
        Glide.with(binding.authorAvatar)
            .load(video.author.avatarUrl)
            .into(binding.authorAvatar)

        // 3. 显示互动数据
        binding.likeCount.text = formatCount(video.stats.likeCount)
        binding.commentCount.text = formatCount(video.stats.commentCount)

        // 4. 通知 ViewModel：准备播放此视频
        viewModel.prepareVideo(video, position, binding.playerSurfaceView)
        
        // 5. 设置手势监听（双击点赞、单击暂停等）
        setupGestureListeners(video)
    }

    private fun setupGestureListeners(video: Video) {
        binding.root.setOnClickListener { 
            viewModel.onVideoClick(video.id) // 单击暂停/播放
        }
        binding.root.setOnDoubleClickListener { 
            viewModel.onDoubleClickLike(video.id) // 双击点赞
        }
    }
}
```

### 2.2 ViewModel 准备播放器

**代码模块：`feature/feed/presentation/` → `core/player/`**

```kotlin
// feature/feed/src/main/java/com/beatu/feed/presentation/FeedViewModel.kt

fun prepareVideo(video: Video, position: Int, surfaceView: SurfaceView) {
    viewModelScope.launch {
        // 1. 从 PlayerPool 获取播放器实例
        val player = playerPool.acquire(position)
        
        // 2. 绑定 SurfaceView
        player.attachSurface(surfaceView)
        
        // 3. 准备播放源（多码率）
        val quality = determineQuality(video) // 根据网络/设备选择码率
        val mediaItem = MediaItem.fromUri(video.qualities.find { it.label == quality }?.url ?: video.playUrl)
        
        // 4. 准备播放器（ExoPlayer 内部会开始预加载）
        player.prepare(mediaItem)
        
        // 5. 如果这是第一个视频（position == 0），自动播放
        if (position == 0) {
            player.play()
            metricsCollector.recordFirstFrameStart()
        }
        
        // 6. 记录当前播放位置
        currentPlayingPosition = position
        currentVideo = video
    }
}

private fun determineQuality(video: Video): String {
    // 1. 读取用户偏好（DataStore）
    val userPreference = settingsRepository.getDefaultQuality()
    
    // 2. 检测网络质量（core/network）
    val networkQuality = networkMonitor.getCurrentQuality()
    
    // 3. AI 清晰度建议（可选）
    // val aiSuggestion = aiRepository.suggestQuality(video.id, networkQuality, deviceStats)
    
    return when {
        userPreference != "Auto" -> userPreference
        networkQuality == NetworkQuality.WEAK -> "SD"
        else -> "HD"
    }
}
```

**PlayerPool 实现（Infrastructure 层）：**

```kotlin
// core/player/src/main/java/com/beatu/player/PlayerPool.kt

class PlayerPool @Inject constructor(
    private val context: Context,
    private val cacheDataSourceFactory: CacheDataSource.Factory
) {
    private val players = mutableListOf<ExoPlayer>() // 默认 2 个实例，高端机 3 个
    private val playerStates = mutableMapOf<Int, PlayerState>() // position -> state

    fun acquire(position: Int): VideoPlayer {
        // 1. 查找是否有空闲播放器
        val availablePlayer = players.find { !it.isPlaying && it.playbackState == Player.STATE_IDLE }
        
        // 2. 如果没有，创建新实例（不超过最大数量）
        val player = availablePlayer ?: run {
            if (players.size < getMaxPoolSize()) {
                createPlayer().also { players.add(it) }
            } else {
                // 复用最早创建的播放器
                players.minByOrNull { it.currentPosition } ?: players[0]
            }
        }
        
        // 3. 如果播放器正在播放其他视频，先暂停
        if (player.isPlaying) {
            val oldPosition = playerStates.entries.find { it.value.player == player }?.key
            oldPosition?.let { release(it) }
        }
        
        // 4. 记录状态
        playerStates[position] = PlayerState(player, position)
        
        return ExoPlayerVideoPlayer(player) // 封装为 VideoPlayer 接口
    }

    fun release(position: Int) {
        playerStates[position]?.let { state ->
            state.player.pause()
            state.player.clearMediaItems()
            // 注意：不释放播放器实例，保留在池中复用
            playerStates.remove(position)
        }
    }

    private fun createPlayer(): ExoPlayer {
        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(cacheDataSourceFactory)
            )
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_READY) {
                            // 记录首帧时间
                            metricsCollector.recordFirstFrameReady()
                            // 隐藏封面图
                            // (通过回调通知 ViewHolder)
                        }
                    }
                })
            }
    }

    private fun getMaxPoolSize(): Int {
        return if (isHighEndDevice()) 3 else 2
    }
}
```

---

## 3. 滑动切换视频（核心流程）

### 3.1 用户上滑/下滑手势

**代码路径：`feature/feed/presentation/FeedFragment` → `ViewPager2`**

```
用户手指在屏幕上滑动
  ↓
ViewPager2.onInterceptTouchEvent() // ViewPager2 内部拦截
  ↓
ViewPager2.onTouchEvent() // 处理滑动事件
  ↓
RecyclerView (ViewPager2 内部使用) 处理滚动
  ↓
触发 onPageScrollStateChanged(SCROLL_STATE_DRAGGING)
  ↓
FeedFragment.registerOnPageChangeCallback()
  → onPageScrollStateChanged(state)
    ↓
FeedViewModel.onPageScrolling() // 可选：暂停当前视频
```

### 3.2 页面切换完成

**代码路径：`feature/feed/presentation/` → `core/player/`**

```kotlin
// FeedFragment.kt

viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
    override fun onPageSelected(position: Int) {
        // 1. 通知 ViewModel：页面切换完成
        viewModel.onPageSelected(position)
        
        // 2. 获取当前 ViewHolder
        val currentViewHolder = adapter.getViewHolderAt(position)
        val surfaceView = currentViewHolder?.getSurfaceView()
        
        // 3. 准备播放新视频
        if (surfaceView != null) {
            val video = adapter.getItem(position)
            viewModel.prepareVideo(video, position, surfaceView)
        }
    }

    override fun onPageScrollStateChanged(state: Int) {
        when (state) {
            ViewPager2.SCROLL_STATE_IDLE -> {
                // 滑动停止：清理上一页资源
                viewModel.onPageScrollComplete()
            }
            ViewPager2.SCROLL_STATE_DRAGGING -> {
                // 开始滑动：可选暂停当前视频
                viewModel.onPageScrolling()
            }
        }
    }
})
```

### 3.3 ViewModel 处理页面切换

**代码模块：`feature/feed/presentation/FeedViewModel.kt`**

```kotlin
fun onPageSelected(newPosition: Int) {
    viewModelScope.launch {
        // 1. 释放上一个视频的播放器资源
        if (currentPlayingPosition != -1 && currentPlayingPosition != newPosition) {
            releasePreviousVideo(currentPlayingPosition)
        }
        
        // 2. 获取新视频数据
        val video = getVideoAt(newPosition) ?: return@launch
        
        // 3. 从 PlayerPool 获取播放器
        val player = playerPool.acquire(newPosition)
        
        // 4. 获取当前 ViewHolder 的 SurfaceView（通过回调）
        // 注意：这里需要从 Adapter 获取，实际实现可能需要通过回调或 EventBus
        val surfaceView = getSurfaceViewForPosition(newPosition)
        
        // 5. 绑定 Surface 并准备播放
        player.attachSurface(surfaceView)
        val quality = determineQuality(video)
        val mediaItem = MediaItem.fromUri(video.qualities.find { it.label == quality }?.url ?: video.playUrl)
        player.prepare(mediaItem)
        
        // 6. 自动播放新视频
        player.play()
        
        // 7. 更新状态
        currentPlayingPosition = newPosition
        currentVideo = video
        
        // 8. 记录指标
        metricsCollector.recordVideoSwitch(newPosition)
        
        // 9. 触发预加载（N+1）
        triggerPreload(newPosition + 1)
    }
}

private fun releasePreviousVideo(oldPosition: Int) {
    viewModelScope.launch {
        // 1. 暂停播放
        playerPool.getPlayer(oldPosition)?.pause()
        
        // 2. 释放 Surface（但保留播放器实例在池中）
        playerPool.release(oldPosition)
        
        // 3. 记录观看时长（用于 AI 推荐）
        val watchedDuration = playerPool.getPlayer(oldPosition)?.currentPosition ?: 0L
        recordWatchTime(oldPosition, watchedDuration)
    }
}

private fun triggerPreload(nextPosition: Int) {
    viewModelScope.launch {
        // 1. 获取下一个视频数据
        val nextVideo = getVideoAt(nextPosition) ?: return@launch
        
        // 2. 调用预加载 UseCase
        playerUseCase.preloadVideo(nextVideo, quality = "HD")
    }
}
```

### 3.4 预加载实现（Domain → Data → Infrastructure）

**代码路径：`domain/usecase/PlayerUseCase.kt` → `core/player/CacheDataSource`**

```kotlin
// domain/src/main/java/com/beatu/domain/usecase/PlayerUseCase.kt

class PlayerUseCase @Inject constructor(
    private val playerRepository: PlayerRepository
) {
    suspend fun preloadVideo(video: Video, quality: String) {
        // 调用 Repository 预加载
        playerRepository.preload(video.id, video.qualities.find { it.label == quality }?.url ?: video.playUrl)
    }
}

// data/src/main/java/com/beatu/data/repository/impl/PlayerRepositoryImpl.kt

class PlayerRepositoryImpl @Inject constructor(
    private val cacheDataSourceFactory: CacheDataSource.Factory,
    private val cache: SimpleCache // ExoPlayer Cache
) : PlayerRepository {
    
    override suspend fun preload(videoId: String, url: String) {
        // 1. 创建 DataSource
        val dataSource = cacheDataSourceFactory.createDataSource()
        
        // 2. 使用 CacheDataSource 预加载前 1-2 MB
        val preloadSize = 2 * 1024 * 1024 // 2 MB
        
        withContext(Dispatchers.IO) {
            try {
                // 3. 打开连接
                val dataSpec = DataSpec(Uri.parse(url))
                val opened = dataSource.open(dataSpec)
                
                // 4. 读取数据到缓存
                val buffer = ByteArray(8192)
                var totalRead = 0L
                while (totalRead < preloadSize && opened != C.RESULT_END_OF_INPUT) {
                    val read = dataSource.read(buffer, 0, buffer.size)
                    if (read == C.RESULT_END_OF_INPUT) break
                    totalRead += read
                }
                
                // 5. 关闭连接（数据已缓存到 SimpleCache）
                dataSource.close()
                
                // 6. 记录预加载指标
                metricsCollector.recordPreloadSuccess(videoId, totalRead)
            } catch (e: Exception) {
                metricsCollector.recordPreloadFailure(videoId, e)
            }
        }
    }
}
```

---

## 4. 预加载策略

### 4.1 ViewPager2 触发预加载

**关键配置：**

```kotlin
// FeedFragment.kt

viewPager2.offscreenPageLimit = 1 // 关键：这会触发 RecyclerView 预创建下一个 ViewHolder
```

**执行流程：**

```
ViewPager2.offscreenPageLimit = 1
  ↓
RecyclerView (ViewPager2 内部) 提前创建 position+1 的 ViewHolder
  ↓
FeedAdapter.onBindViewHolder(holder, position+1)
  ↓
FeedViewHolder.bind(video, position+1)
  ↓
FeedViewModel.prepareVideo(video, position+1, surfaceView)
  ↓
PlayerPool.acquire(position+1)
  ↓
ExoPlayer.prepare(mediaItem) // ExoPlayer 内部使用 CacheDataSource 预加载
  ↓
CacheDataSource 检查 SimpleCache
  → 如果缓存中已有数据（来自 PlayerRepositoryImpl.preload），直接使用
  → 如果没有，开始下载并缓存
```

### 4.2 预加载时机总结

1. **播放第 N 条视频时**：
   - ViewPager2 自动创建 N+1 的 ViewHolder（offscreenPageLimit=1）
   - ViewModel 触发 `prepareVideo(N+1)`，ExoPlayer 开始预加载

2. **主动预加载（后台）**：
   - ViewModel.onPageSelected(N) 调用 `triggerPreload(N+1)`
   - PlayerUseCase → PlayerRepositoryImpl → CacheDataSource 预下载 1-2 MB

3. **播放器复用**：
   - PlayerPool 维护 2-3 个 ExoPlayer 实例
   - 切换视频时，复用一个已准备好的播放器实例，减少创建开销

---

## 5. 数据流转总结

### 5.1 完整数据流图

```
┌─────────────────────────────────────────────────────────────────┐
│ 用户操作层 (UI Events)                                           │
│ - 点击 App 图标                                                  │
│ - 上下滑动 ViewPager2                                            │
│ - 双击点赞                                                       │
└──────────────────────┬──────────────────────────────────────────┘
                       │
                       ↓
┌─────────────────────────────────────────────────────────────────┐
│ Presentation 层 (feature/feed/presentation/)                     │
│ - FeedFragment (Activity/Fragment + 原生 View)                   │
│ - FeedAdapter (RecyclerView.Adapter + ViewPager2)                │
│ - FeedViewHolder (单个视频 Item)                                 │
│                                                                  │
│ UI 事件 → FeedViewModel                                         │
│   - onPageSelected(position)                                    │
│   - onDoubleClickLike(videoId)                                  │
│   - onVideoClick(videoId)                                       │
└──────────────────────┬──────────────────────────────────────────┘
                       │
                       ↓ StateFlow / LiveData
┌─────────────────────────────────────────────────────────────────┐
│ Domain 层 (domain/usecase/, domain/repository/)                  │
│ - FeedUseCase.fetchFeed()                                       │
│ - LikeVideoUseCase.like()                                       │
│ - PlayerUseCase.preloadVideo()                                  │
│                                                                  │
│ Repository 接口：                                                │
│ - FeedRepository.fetchFeed() → Flow<PagingData<Video>>          │
│ - InteractionRepository.like() → Result<Boolean>                │
│ - PlayerRepository.preload() → Unit                             │
└──────────────────────┬──────────────────────────────────────────┘
                       │
                       ↓ 调用实现
┌─────────────────────────────────────────────────────────────────┐
│ Data 层 (data/repository/impl/, data/source/)                    │
│ - FeedRepositoryImpl                                             │
│   - 调用 FeedRemoteDataSource (Retrofit)                        │
│   - 调用 FeedLocalDataSource (Room)                             │
│   - DTO → Domain Model 映射                                     │
│                                                                  │
│ - PlayerRepositoryImpl                                           │
│   - 调用 CacheDataSource (ExoPlayer)                            │
│   - 预加载视频数据到 SimpleCache                                 │
└──────────────────────┬──────────────────────────────────────────┘
                       │
                       ↓
┌─────────────────────────────────────────────────────────────────┐
│ Infrastructure 层 (core/)                                        │
│ - core/network/                                                  │
│   - Retrofit + OkHttp                                            │
│   - API 拦截器、弱网降级                                         │
│   - GET /api/v1/feed → BeatUGateway                             │
│                                                                  │
│ - core/player/                                                   │
│   - PlayerPool (ExoPlayer 实例池)                                │
│   - VideoPlayer 接口实现                                         │
│   - CacheDataSource 预加载策略                                   │
│                                                                  │
│ - core/database/                                                 │
│   - Room (Feed、InteractionState 缓存)                          │
│   - DataStore (用户偏好)                                         │
└──────────────────────┬──────────────────────────────────────────┘
                       │
                       ↓ HTTP / Cache
┌─────────────────────────────────────────────────────────────────┐
│ 外部服务                                                         │
│ - BeatUGateway → BeatUContentService (视频列表)                  │
│ - BeatUGateway → BeatUAIService (AI 推荐，可选)                  │
│ - SimpleCache (ExoPlayer 本地视频缓存)                           │
└─────────────────────────────────────────────────────────────────┘
```

### 5.2 关键模块调用链（刷视频场景）

```
1. 用户上滑手势
   ↓
2. ViewPager2.onPageSelected(newPosition)
   ↓
3. FeedFragment.registerOnPageChangeCallback().onPageSelected()
   ↓
4. FeedViewModel.onPageSelected(newPosition)
   ↓
5. FeedViewModel.releasePreviousVideo(oldPosition)
   → PlayerPool.release(oldPosition)
   → ExoPlayer.pause() + clearMediaItems()
   ↓
6. FeedViewModel.prepareVideo(newVideo, newPosition, surfaceView)
   → PlayerPool.acquire(newPosition)
   → ExoPlayer.attachSurface(surfaceView)
   → ExoPlayer.prepare(mediaItem) // 从 CacheDataSource 读取（已预加载）
   → ExoPlayer.play()
   ↓
7. FeedViewModel.triggerPreload(newPosition + 1)
   → PlayerUseCase.preloadVideo(nextVideo)
   → PlayerRepositoryImpl.preload(nextVideoUrl)
   → CacheDataSource 下载 1-2 MB 到 SimpleCache
   ↓
8. ExoPlayer.onPlaybackStateChanged(READY)
   → MetricsCollector.recordFirstFrameReady()
   → FeedViewHolder.hideCoverImage() // 隐藏封面
   ↓
9. FeedViewModel._uiState.value = FeedUiState.Playing(newVideo)
   ↓
10. FeedFragment 观察到状态变化
    → updateUI(state) // 更新点赞数、评论数等
```

### 5.3 内存与性能关键点

1. **播放器复用**：
   - PlayerPool 最多 2-3 个 ExoPlayer 实例
   - 切换视频时复用实例，不销毁重建

2. **资源释放**：
   - `onPageSelected` 时立即 `pause()` 上一个视频
   - `onPageScrollComplete` 时 `releaseSurface()`，但保留播放器实例

3. **预加载策略**：
   - ViewPager2 `offscreenPageLimit=1` 触发 RecyclerView 预创建
   - 主动预加载下一视频的前 1-2 MB 到 SimpleCache
   - ExoPlayer 准备时直接从缓存读取，首帧 < 500ms

4. **数据缓存**：
   - Room 缓存视频列表元数据（Feed）
   - SimpleCache 缓存视频媒体数据（ExoPlayer）
   - DataStore 缓存用户偏好（清晰度、倍速等）

---

## 6. 关键代码位置索引

| 功能 | 模块路径 | 关键类/方法 |
|------|---------|------------|
| **App 入口** | `app/src/main/java/com/beatu/app/` | `MainActivity.kt` |
| **Feed UI** | `feature/feed/src/main/java/com/beatu/feed/presentation/` | `FeedFragment.kt`, `FeedAdapter.kt`, `FeedViewModel.kt` |
| **业务逻辑** | `domain/src/main/java/com/beatu/domain/usecase/` | `FeedUseCase.kt`, `PlayerUseCase.kt` |
| **数据仓库** | `data/src/main/java/com/beatu/data/repository/impl/` | `FeedRepositoryImpl.kt`, `PlayerRepositoryImpl.kt` |
| **网络请求** | `core/network/src/main/java/com/beatu/network/` | `BeatUGatewayApi.kt` |
| **播放器池** | `core/player/src/main/java/com/beatu/player/` | `PlayerPool.kt`, `VideoPlayer.kt` |
| **数据库** | `core/database/src/main/java/com/beatu/database/` | `FeedDao.kt`, `AppDatabase.kt` |

---

## 7. 性能指标记录点

在代码流程中，以下位置需要记录性能指标（通过 `MetricsCollector`）：

1. **冷启动时间**：`MainActivity.onCreate()` → `FeedFragment.onViewCreated()`
2. **首帧时间**：`ExoPlayer.onPlaybackStateChanged(READY)` 时记录
3. **视频切换时间**：`FeedViewModel.onPageSelected()` 开始 → `ExoPlayer.play()` 完成
4. **预加载成功率**：`PlayerRepositoryImpl.preload()` 成功/失败
5. **内存峰值**：`PlayerPool` 中播放器实例数量 × 单个实例内存占用
6. **FPS**：通过 `Choreographer` 或 `WindowCallback` 记录 UI 帧率

所有指标上报到 `BeatUObservability` 服务（`POST /api/v1/metrics/playback`）。

---

## 8. 后续扩展点

1. **AI 推荐触发**：视频播放完成时（`ExoPlayer.onPlaybackStateChanged(ENDED)`），调用 `AiRepository.requestRecommendation()`，更新 Paging 数据源
2. **横屏模式**：点击“全屏”按钮或旋转设备，导航到 `feature/landscape`，复用同一播放器实例
3. **手势识别**：双击点赞、长按倍速等，在 `FeedViewHolder` 中通过 `GestureDetector` 实现
4. **评论弹窗**：点击评论按钮，导航到 `feature/feed/presentation/CommentDialogFragment`，视频缩放至上半屏

---

> **本文档基于 Clean Architecture + MVVM 架构，遵循 `.cursorrules` 约定的模块划分与数据流设计。实际实现时，请确保代码结构与本文档描述一致。**

