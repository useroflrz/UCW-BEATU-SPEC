## BeatU 整体架构概览

> 最新需求参考 `BeatUClient/docs/需求.md` 与其原型/流程图。本文档描述跨项目架构、客户端模块化划分、核心数据流与 AI/性能策略。

### 1. 仓库层级

- `BeatUClient/`：Android 客户端（多 Module + MVVM + Clean Architecture）。
- `BeatUAIService/`：AI 能力服务。
- `BeatUContentService/`：内容与媒体服务。
- `BeatUGateway/`：API 网关与聚合层。
- `BeatUObservability/`：日志、埋点、监控与可观测性。

### 2. Android 客户端模块规划（BeatUClient）

- `app/`
  - 入口 Activity、Navigation、Hilt Application、Splash（Logo→Loading→Feed）。
- `core/common/`
  - 扩展方法、Result 类型、Logger、性能指标采集器。
- `core/network/`
  - Retrofit/OkHttp、API 拦截器、弱网降级策略、多码率源管理。
- `core/database/`
  - Room 缓存（Feed、评论、用户信息）、离线策略。
- `core/player/`
  - `VideoPlayer` 接口、ExoPlayer (Media3) 实现、PlayerPool（1~3 实例）、预加载与 Surface 复用工具。
- `core/designsystem/`
  - 主题、半透明播控面板、动画/触觉反馈统一实现。
- `feature/feed/`
  - `ViewPager2` 纵向 Feed（上下滑切换），Paging3 + DiffUtil，手势、播控面板、评论半屏弹层。
- `feature/profile/`
  - 个人主页/作者主页、关注状态同步。
- `feature/search/`
  - 频道切换、话题发现。
- `feature/settings/`
  - AI 开关、清晰度偏好、横屏锁定。
- `feature/landscape/`
  - 横屏模式 UI、亮度/音量/锁屏手势。
- `feature/aiassistant/`
  - 评论区 `@元宝` AI 问答、推荐策略接口。

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

### 5. 交互层设计

- `feature/feed`：负责竖屏主场景，包含手势检测（单击、双击、长按、上下/左右滑）、播控面板状态机、评论弹层。
- `feature/landscape`：横屏模式路由，可由 Feed 或系统旋转触发，包含亮度/音量/快进手势、防误触锁、倍速/清晰度菜单。
- `feature/profile`：Feed 作者头像跳转、关注状态同步、作品/收藏/点赞列表。

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

### 9. 第二阶段：核心基础设施落地（2025-11-20）

- `core/common`：落地 `AppResult`、`AppLogger`、`MetricsTracker`、`PlaybackMetrics`、`Stopwatch` 与 `DispatcherProvider`，作为跨层标准工具集，为 Repository 与 UseCase 提供统一的结果包装与性能埋点。
- `core/network`：产出 `NetworkConfig`、`HeaderInterceptor`、`NetworkLoggingInterceptor`、`OkHttpProvider`、`RetrofitProvider` 与 `ConnectivityObserver`。支持自定义 Header、磁盘缓存、动态日志开关以及网络可达性 Flow，供 Data 层按需注入。
- `core/database`：基于 Room 构建 `BeatUDatabase`，定义 `VideoEntity`、`CommentEntity`、`InteractionStateEntity`、`Converters` 以及对应 DAO（`VideoDao`、`CommentDao`、`InteractionStateDao`）。数据库构建器默认开启 `fallbackToDestructiveMigration` 以便快速迭代。
- `core/player`：实现 `VideoPlayer` 接口、`VideoSource`/`VideoQuality` 模型、`VideoPlayerConfig`、`ExoVideoPlayer` 与 `VideoPlayerPool`，并提供 `PlayerMetricsTracker` 负责与 `core/common/metrics` 对接，形成播放器复用 + 指标采集闭环。
- 以上模块均保持与 Hilt 解耦（纯 Kotlin/Android 类），保证 Feature 与 Data 层在需要时通过 Module/DI 注入，满足 Clean Architecture 的依赖倒置原则。



