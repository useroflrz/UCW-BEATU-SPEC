# BeatU 客户端技术方案白皮书

> 基于 `README.md`、`docs/architecture.md`、`docs/api_reference.md`、`docs/getting_started.md` 与 `BeatUClient/docs/需求.md` 汇总。面向评审展示架构深度、目录规划、性能与 AI 策略，为后续模块化开发提供统一蓝本。

## 1. 文档目标

- 统一客户端技术叙事：阐述 Clean Architecture + Feature Module 的层次结构与职责划分。
- 给出落地级目录规划，便于团队快速对齐代码组织、依赖关系与扩展点。
- 突出播放器、性能、AI 融合指标，确保每个功能点都有可量化验收标准。

## 2. 整体架构概览

- **架构模式**：MVVM + Clean Architecture，分层关系为 Presentation → Domain → Data → Infrastructure（Player/Network/Database）。
- **技术主干**：
  - UI：原生 View 系统（TextView、ImageView、RecyclerView 等）+ Jetpack View 组件（ViewPager2 纵向 Feed、Navigation、MotionLayout）+ 传统 XML 布局。
  - 播放器：ExoPlayer（Media3）+ `VideoPlayer` 抽象层 + PlayerPool。
  - 数据：Retrofit+OkHttp、Room、Paging3、DataStore。
  - 依赖注入：Hilt；异步：Kotlin Coroutines + Flow/StateFlow。
  - 注意：本项目**不使用 Jetpack Compose**，统一采用 Activity/Fragment + 原生 View + XML 布局的技术栈。
- **跨层策略**：
  - `core/common` 暴露 Result、Logger、Metrics。
  - `core/player` 提供播放器接口 & 生命周期管理工具。
  - Feature 模块仅依赖 Domain UseCase/Repository 接口，避免 Data 泄漏到 UI。

### 2.1 数据流

```
View (Activity/Fragment + 原生 View 系统) 
   ↓ StateFlow
Presentation ViewModel (FeedViewModel / LandscapeViewModel / AiAssistantViewModel)
   ↓ UseCase（Domain）
Repository Interface
   ↓ Remote (Gateway API) + Local (Room / CacheDataSource) + PlayerDataSource
Observability & Metrics Sink
```

## 3. 原型交互映射（参考 `BeatUClient/docs/原型图描述.md`）

- **推荐页（竖屏主场景）**：顶部 Tab（关注/推荐/我）与搜索图标对应 `feature/feed` 与 `feature/profile` 的 Navigation；中部粉色占位符即全屏视频区域，底部“全屏观看”按钮触发 `feature/landscape`；右侧点赞/收藏/评论/分享按钮映射互动 UseCase。
- **全屏沉浸模式**：右侧控制栏（评论、分享、亮度、锁屏、音量）与底部倍速/清晰度按钮，要求 `feature/landscape` 提供独立 UI 状态机，复用 `core/player` 的 `setSpeed/setQuality` 能力。
- **个人主页（关注/未关注两态）**：导航仍保持关注/推荐/我结构，个人信息区、关注按钮与作品/收藏/点赞/历史 Tab 映射 `feature/profile` 模块；关注状态变化需通过 `InteractionRepository.follow`.
- **评论区弹窗**：全屏模式下右侧弹出半屏评论列表（标题、滚动列表、底部输入框），由 `feature/feed` 评论面板与 `feature/aiassistant` (`@元宝`) 共同实现；UI 缩放策略需与播放器生命周期联动。

> 原型中的控件命名/手势需与本文目录规划一一映射，确保交互评审与实现保持一致。

## 4. 模块职责矩阵

| 分层 | 模块 | 职责摘要 | 关键依赖 |
| --- | --- | --- | --- |
| Presentation | `app`, `feature/*` | Navigation、手势、UI 状态机、横屏/评论交互 | ViewModel, Hilt, 原生 View + Jetpack View |
| Domain | `domain/usecase`, `domain/repository` | 纯 Kotlin 业务编排、可测试逻辑（Feed、Like、AI） | Coroutines |
| Data | `data/repository`, `data/source_remote`, `data/source_local` | Retrofit、Room、Cache、DTO ↔ Model 映射 | Retrofit, Room, DataStore |
| Infrastructure | `core/player`, `core/network`, `core/database`, `core/common`, `core/designsystem` | Player 抽象、网络栈、缓存、公共组件、主题动画 | ExoPlayer, OkHttp |

## 5. 目录规划（代码区）

```
BeatUClient/
├─ app/
│  ├─ src/main/java/com/beatu/app/
│  │  ├─ BeatUApp.kt                 // Hilt Application + 初始化
│  │  ├─ navigation/                 // 全局路由图（Feed/AI/横屏/个人主页）
│  │  └─ di/                         // App 级依赖注入入口
│  └─ src/main/res/                  // 主题、Lottie、MotionLayout
├─ core/
│  ├─ common/                        // Result、Logger、Metrics
│  ├─ network/                       // Retrofit、OkHttp、拦截器、弱网降级
│  ├─ database/                      // Room DB、Dao、迁移
│  ├─ player/                        // VideoPlayer 接口、PlayerPool、Cache 管理
│  └─ designsystem/                  // 主题、动画、触觉反馈
├─ domain/
│  ├─ model/                         // 领域模型（Video, Comment, PlaybackMetrics）
│  ├─ repository/                    // FeedRepository 等接口
│  └─ usecase/                       // FeedUseCase、LikeVideoUseCase、AiReplyUseCase
├─ data/
│  ├─ repository/impl/               // Repository 实现 & Mapper
│  ├─ source/remote/                 // Retrofit API 定义
│  ├─ source/local/                  // Room + DataStore
│  └─ source/player/                 // CacheDataSource 预加载、播放器指标上报
├─ feature/
│  ├─ feed/                          // ViewPager2 Feed、手势、评论半屏
│  ├─ landscape/                     // 横屏模式、亮度/音量/锁屏
│  ├─ profile/                       // 个人主页、关注状态
│  ├─ search/                        // 频道/话题、左右滑切换
│  ├─ aiassistant/                   // 评论区 @元宝、AI 推荐入口
│  └─ settings/                      // 默认倍速、清晰度、AI 开关
├─ build-logic/                      //（可选）集中 Gradle Convention
└─ docs/                             // 模块内补充文档（Player 设计、交互说明）
```

> 目录规划遵循“core 提供能力、data/domain 做胶水、feature 实现业务”的分层策略，确保 Feature 可独立开发/测试/发布。

## 6. 播放器与性能策略

- **PlayerPool**：默认 2 实例（当前/下一个），高端机扩展到 3；离屏立即 `pause`，`onViewRecycled` 中 `releaseSurface`。
- **预加载**：`CacheDataSource` + N+1 预取 1–2 MB，配合封面图，首帧控制在 `< 500 ms`。
- **生命周期**：ViewModel 统一调度 `prepare/attach/play/pause/release`，避免 Fragment 泄漏。
- **指标采集**：在 `core/common/metrics` 记录 `startUpMs`、`fps`、`rebufferCount`、`memoryPeakMb` 并通过 `PlayerRepository.reportMetrics` 上报 `/api/v1/metrics/playback`。
- **弱网降级**：`core/network` 暴露网络质量探测；`AiRepository` 可请求 `/api/v1/ai/quality` 调整码率。

## 7. AI 融合路径

| 能力 | 数据流 | 触达指标 |
| --- | --- | --- |
| AI 内容理解/推荐 | 播放完成 → `AiRepository.requestRecommendation` → Paging 追加 `nextVideos` | 推荐命中率 ≥ 60%，点击率 ≥ 25% |
| 评论区 @元宝 | 评论输入 `@元宝` → `/api/v1/comment/ai` → 插入机器人回复 | AI 回复成功率 ≥ 95%，响应 < 1.2s |
| AI 清晰度（扩展） | Player 采集网络/设备 → `/api/v1/ai/quality` → `PlayerPool.setQuality` | 首帧稳定 < 500ms，卡顿率 < 2% |

降级策略：AI 服务不可用时回退到默认推荐/清晰度，评论展示提示并允许手动重试。

## 8. 可量化指标与验收

- 播放成功率 ≥ 99%，首帧时间 < 500 ms，卡顿率 < 2%，冷启动 < 2.5 s，Feed FPS 55~60。
- 手势响应：双击点赞动画延迟 < 50 ms，评论半屏展开 < 180 ms。
- 内存峰值：播放 30 条视频峰值 < 512 MB，PlayerPool 不超过 3 个实例。
- AI 指标：AI 评论触达率 ≥ 30%，推荐 CTR ≥ 25%，清晰度 AI 覆盖率 ≥ 80%（启用后）。
- 所有指标需通过 Android Studio Profiler + Observability 埋点截图/数据佐证。

## 9. 下一步

1. 依据本目录规划创建 Feature/Core/Data/Domain 模块骨架（Gradle 子模块）。
2. 完善 `core/player` 的接口定义及默认实现说明文档。
3. 在 `docs/architecture.md` 增补 Player 状态机与模块依赖图，保持与本白皮书一致。
4. 将该文档纳入评审基线，后续迭代更新技术亮点与指标达成情况。

## 10. 第二阶段交付物快照（2025-11-20）

- **Core Common**：提供 `AppResult`、`AppLogger`、`DispatcherProvider`、`Stopwatch`、`MetricsTracker` 与 `PlaybackMetrics`，确保数据层/领域层有统一的异常包装与指标归集能力。
- **Core Network**：配置化 `NetworkConfig` + `OkHttpProvider` + `RetrofitProvider` + `ConnectivityObserver` + 拦截器组合，满足 Header 注入、磁盘缓存、可观测日志以及弱网感知。
- **Core Database**：Room 化 `BeatUDatabase`（含 `VideoEntity`、`CommentEntity`、`InteractionStateEntity`、DAO 与 TypeConverter），支持 Feed/评论/互动状态离线持久化。
- **Core Player**：`VideoPlayer` 抽象 + `VideoSource`/`VideoQuality` 模型 + `ExoVideoPlayer` + `VideoPlayerPool` + `PlayerMetricsTracker`，兑现播放器复用池与指标采集方案。
- 所有模块皆解耦于 UI/Feature，后续仅需在 Data 层通过 Hilt/DI 将这些能力组装即可进入第三阶段（Repository + UseCase 实现）。


