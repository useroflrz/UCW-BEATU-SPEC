## BeatU 整体架构概览

> 最新需求参考 `BeatUClient/docs/需求.md` 与其原型/流程图。本文档描述跨项目架构、客户端模块化划分、核心数据流与 AI/性能策略。

### 1. 仓库层级

- `BeatUClient/`：Android 客户端（多 Module + MVVM + Clean Architecture）。
- `BeatUAIService/`：AI 能力服务。
- `BeatUContentService/`：内容与媒体服务。
- `BeatUGateway/`：API 网关与聚合层。
- `BeatUObservability/`：日志、埋点、监控与可观测性。

### 2. Android 客户端模块规划（BeatUClient）

**新架构（按业务边界划分）**：

- `app/`
  - 入口 Activity、Navigation、Hilt Application、Splash（Logo→Loading→Feed）。

- `shared/common/`
  - 扩展方法、Result 类型、Logger、性能指标采集器、协程调度器。
- `shared/network/`
  - Retrofit/OkHttp、API 拦截器、弱网降级策略、多码率源管理。
- `shared/database/`
  - Room 缓存（Feed、评论、用户信息）、离线策略。
- `shared/player/`
  - `VideoPlayer` 接口、ExoPlayer (Media3) 实现、PlayerPool（1~3 实例）、预加载与 Surface 复用工具。
- `shared/designsystem/`
  - 主题、半透明播控面板、动画/触觉反馈统一实现。

- `business/videofeed/`（视频流业务）
  - `presentation/`：`ViewPager2` 纵向 Feed（上下滑切换），Paging3 + DiffUtil，手势、播控面板、评论半屏弹层。
  - `domain/`：FeedRepository 接口、UseCase（GetFeedUseCase、LikeVideoUseCase、CommentUseCase 等）。
  - `data/`：FeedRepository 实现、RemoteDataSource、LocalDataSource、Mapper。
- `business/user/`（用户业务）
  - `presentation/`：
    - `UserProfileFragment`：个人主页/作者主页，显示用户头像、昵称、简介、作品列表（网格布局），支持头像/昵称/简介编辑，四个 Tab（作品/收藏/点赞/历史）切换。
    - `UserWorksViewerFragment`：用户作品视频列表观看页面（竖屏），使用 `ViewPager2` 垂直滑动，复用 `VideoItemFragment` 和播放器复用池，支持定位到初始视频，限制在用户视频列表范围内。
    - `UserProfileViewModel`：管理用户信息加载、作品列表订阅、用户信息更新（头像/昵称/简介）。
    - `UserWorksViewerViewModel`：管理用户作品视频列表加载，从 `UserWorksRepository` 获取数据并转换为 `VideoItem`。
  - `domain/`：
    - `UserRepository` 接口：`observeUserById(userId)`, `getUserById(userId)`, `saveUser(user)`
    - `UserWorksRepository` 接口：`observeUserWorks(userId, limit)`，复用视频流缓存数据
    - UseCase：用户信息查询、作品列表查询
  - `data/`：
    - `UserRepository` 实现：依赖 `shared/database` 的 `UserDao`，本地持久化用户信息
    - `UserWorksRepository` 实现：依赖 `shared/database` 的 `VideoDao`，复用 Feed 缓存，将 `VideoEntity` 转换为 `UserWork`
- `business/search/`（搜索业务）
  - `presentation/`：
    - `SearchFragment`：搜索入口页面，包含搜索框、搜索历史（FlowLayout）、热门搜索（FlowLayout）、搜索建议列表（RecyclerView），右下角 AI 搜索按钮。
    - `SearchResultFragment`：常规搜索结果页面，显示视频列表（网格布局），支持重新搜索。
    - `AiSearchFragment`：AI 搜索与对话合一的页面，顶部 Toolbar + `RecyclerView` 展示多轮对话（AI 左/用户右），底部输入框即时发送，支持接收搜索页传入的初始提问参数。
    - 所有搜索相关页面复用统一的 `view_search_header.xml` 布局（返回按钮、搜索框、清除按钮、搜索按钮），AI 页面使用标题栏。
  - `domain/`：SearchRepository 接口、UseCase（待实现，当前使用 Mock 数据）。
  - `data/`：SearchRepository 实现（待实现，当前使用 Mock 数据）。
- `business/ai/`（AI 业务）
  - `presentation/`：评论区 `@元宝` AI 问答 UI。
  - `domain/`：AiRepository 接口、UseCase（AiReplyUseCase、RecommendUseCase）。
  - `data/`：AiRepository 实现。
- `business/landscape/`（横屏业务）
  - `presentation/`：横屏模式 UI、亮度/音量/锁屏手势。
  - `domain/`：LandscapeRepository 接口、UseCase。
  - `data/`：LandscapeRepository 实现。
- `business/settings/`（设置业务）
  - `presentation/`：AI 开关、清晰度偏好、横屏锁定。
  - `domain/`：SettingsRepository 接口、UseCase。
  - `data/`：SettingsRepository 实现（DataStore）。

> 2025-11-24 更新
>
> - Settings 模块完成 DataStore 持久化与 `SettingsViewModel` 封装，所有交互通过 `SettingsUseCases` 下发，UI 行为以 iOS 风格的卡片 + MaterialSwitch 呈现，并由 `Stopwatch` 记录每次操作的端到端延迟（日志标签 `BeatU-SettingsViewModel`）。
> - Landscape 模块补齐 `LandscapeRepositoryImpl` → UseCase → ViewModel 链路，横屏 Feed 不再依赖 Fragment 内部 mock。`LandscapeVideoItemViewModel` 负责播放/手势/锁屏状态，并在 `AppLogger` 中输出首帧耗时（`startUpTimeMs`），供 KPI 评估。

### 3. Clean Architecture & 数据流

```
Presentation (原生 View + Jetpack View + ViewPager2)
      ↓ StateFlow/UIState (ViewModel)
Domain UseCase (FeedUseCase, LikeVideoUseCase, AiReplyUseCase)
      ↓ Repository Interfaces (FeedRepository, InteractionRepository, AiRepository)
Data Layer (RemoteDataSource + LocalCache + PlayerDataSource)
```

- ViewModel 负责播放器生命周期：`onPageSelected` → `PlayerPool.attach(surface)` → `play()`, `onPageRelease` → `pause()/release()`, 并暴露 StateFlow 给 UI。
- UseCase 层纯 Kotlin 协程，易于单测。
- Repository 协调网络（Retrofit）与本地缓存（Room/Media cache），并向 PlayerDataSource 提供 URL + 清晰度。

### 4. 播放器与性能策略

- `VideoPlayer` 抽象层屏蔽 ExoPlayer/ijkplayer 差异，接口包含 `prepare`, `play`, `pause`, `seek`, `setSpeed`, `setQuality`, `attachSurface`.
- PlayerPool：默认 2 个实例（当前+下一个），根据设备性能扩展到 3 个。利用 `ViewPager2` 的 `setOffscreenPageLimit(1)` 触发下一条预加载。
- 预加载：借助 `CacheDataSource` 在后台拉取 N+1 前 1–2 MB，结合封面图实现首帧 < 500 ms。
- 监控：`core/common/metrics` 记录 FPS、首帧时间、播放成功率、卡顿率、冷启动。数据上报到 `BeatUObservability`。
- 横竖屏复用：`shared/player/session/PlaybackSessionStore` 负责缓存播放进度/倍速/播放状态；竖屏 `VideoItemViewModel` 与横屏 `LandscapeVideoItemViewModel` 在切换时通过 `VideoPlayerPool` 共享同一个 ExoPlayer，并用 `PlayerView.switchTargetView` 热插拔 Surface，确保切换无需重新缓冲且保持 KPI（首帧 <500 ms、切换黑屏 = 0）。
- ViewPager2 子 Fragment 仅在 `RESUMED` 状态才会真正 `prepare+play` 播放器；离屏 Fragment 保持暂停并把 PlayerView 脱附，避免后台/错播。`onPause` 必定调用 `VideoPlayer.pause()`，`onDestroyView` 将 `PlayerView.player=null` 并把实例归还 `VideoPlayerPool`，保证同屏仅一条音画输出。
- 应用层顶部导航（MainActivity）在频道切换时，通过 `FeedFragment` 将推荐页 Fragment 的可见性事件下沉到 `RecommendFragment`/`VideoItemFragment`，保证“离开频道即暂停、回到频道再恢复”，杜绝关注页可见时仍播放推荐视频的跨频道串音。

### 5. 交互层设计

- `feature/feed`：负责竖屏主场景，包含手势检测（单击、双击、长按、上下/左右滑）、播控面板状态机、评论弹层。
- `feature/landscape`：横屏模式路由，可由 Feed 或系统旋转触发，包含亮度/音量/快进手势、防误触锁、倍速/清晰度菜单。
- `feature/profile`：Feed 作者头像跳转、关注状态同步、作品/收藏/点赞列表。
- `MainActivity` 顶部导航栏作为全局容器：通过 `TabIndicatorView` 与 FeedFragment 的 `MainActivityBridge` 保持同步，进入“我”或“搜索”等二级页面时会以 300ms iOS 风格滑动动画（右进左出）切换，并伴随顶部 Tab 平滑隐藏/返回推荐页时再显示，避免 Feed 内容高度突变。

### 6. AI 数据流

- **AI 内容理解/推荐**：
  1. Player 结束事件 → 埋点 → `AiRepository` 调用 `BeatUAIService /recommend`.
  2. 返回标签/推荐列表 → FeedRepository 更新 Paging 源 → ViewModel 刷新。
- **AI 评论助手 (@元宝)**：
  1. 评论输入框 `@元宝` 触发 `AiRepository.ask(videoId, question)`.
  2. 返回答案 + 置信度 → 评论列表插入机器人回复。
- **可选扩展**：AI 清晰度自适应（采集网络与设备状态→AIService→返回码率策略），AI 字幕。

### 7. 服务端协作与接口

- `BeatUContentService`：提供 `GET /feed`, `GET /video/{id}/comments`, `POST /interaction/like` 等。
- `BeatUAIService`：提供 `POST /ai/recommend`, `POST /ai/comment/qa`, `POST /ai/quality`.
- `BeatUGateway`：统一鉴权、聚合、版本管理，对客户端暴露 `GraphQL/REST` 接口。

### 8. 下一步行动

- 产出模块依赖图（core vs feature）。
- 绘制播放器生命周期/状态机图。
- 与设计稿对齐横屏/评论交互状态。
- 在 `docs/api_reference.md` 中补充上述接口的字段定义与契约。

### 9. 架构重构（2025-11-20）

**重构目标**：从技术层划分（MVVM + Clean Architecture）重构为业务边界划分，每个业务内部采用 Clean Architecture + Feature 分层。

**新架构特点**：
- 业务独立：每个业务模块（`business/*`）内部包含完整的 Presentation/Domain/Data 层
- 公共能力复用：播放器、网络、数据库等公共能力独立模块化（`shared/*`）
- 并行开发：不同业务可以独立开发、测试、发布
- 清晰边界：业务间通过明确的接口通信，避免隐式依赖

**公共模块（shared/*）**：
- `shared/common`：`AppResult`、`AppLogger`、`MetricsTracker`、`PlaybackMetrics`、`Stopwatch`、`DispatcherProvider`
- `shared/network`：`NetworkConfig`、`HeaderInterceptor`、`NetworkLoggingInterceptor`、`OkHttpProvider`、`RetrofitProvider`、`ConnectivityObserver`
- `shared/database`：`BeatUDatabase`、`VideoEntity`、`CommentEntity`、`InteractionStateEntity`、`Converters`、DAO 接口
- `shared/player`：`VideoPlayer` 接口、`VideoSource`/`VideoQuality`、`VideoPlayerConfig`、`ExoVideoPlayer`、`VideoPlayerPool`、`PlayerMetricsTracker`
- `shared/designsystem`：主题、颜色、字体、通用组件、动画资源

**业务模块（business/*）**：
- 每个业务模块遵循 Clean Architecture，包含 `presentation/`、`domain/`、`data/` 三层
- 业务间通过接口通信，通过 DI 注入依赖
- 详细架构说明见 `docs/重构方案.md`



