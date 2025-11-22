# 功能开发前准备清单与团队分工建议（业务优先模式）

> **开发策略**：采用"业务优先、从具象到抽象"的开发方式，优先完成UI层和页面跳转，让项目先跑起来，再逐步对接底层数据。  
> 本文档基于当前项目实现程度，梳理在功能开发（如App主页、业务页面跳转和联动）之前需要完成的基础工作，并提供3人团队的分工建议。

## 一、当前项目实现程度评估

### ✅ 已完成的工作

1. **架构搭建**
   - ✅ 新架构目录结构（`business/*` + `shared/*`）已建立
   - ✅ 所有业务模块骨架已创建（videofeed、user、search、ai、landscape、settings）
   - ✅ 公共模块基础设施已搭建（common、network、database、player、designsystem）

2. **数据库层（Room）**
   - ✅ `BeatUDatabase` 已创建
   - ✅ Entity 已定义（`VideoEntity`、`CommentEntity`、`InteractionStateEntity`）
   - ✅ DAO 接口已定义（`VideoDao`、`CommentDao`、`InteractionStateDao`）
   - ⚠️ **未完成**：数据库初始化与数据源实现

3. **网络层**
   - ✅ `NetworkConfig`、`OkHttpProvider`、`RetrofitProvider` 已实现
   - ✅ 拦截器已实现（`HeaderInterceptor`、`NetworkLoggingInterceptor`）
   - ✅ `ConnectivityObserver` 已实现
   - ⚠️ **未完成**：API 接口定义（Retrofit Service）、DTO 模型、数据映射

4. **播放器层**
   - ✅ `VideoPlayer` 接口已定义
   - ✅ `ExoVideoPlayer`、`VideoPlayerPool` 已实现
   - ⚠️ **未完成**：播放器与 UI 层的集成、生命周期管理

5. **UI 层**
   - ✅ `FeedFragment` 布局已创建
   - ✅ `MainActivity` 已创建
   - ⚠️ **未完成**：ViewModel、数据绑定、Navigation 配置、页面跳转

### ❌ 缺失的关键工作

1. **数据层对接**
   - ❌ Repository 实现（`FeedRepositoryImpl`、`UserRepositoryImpl` 等）
   - ❌ RemoteDataSource 实现（API 调用）
   - ❌ LocalDataSource 实现（Room 数据操作）
   - ❌ DTO 到 Model 的 Mapper
   - ❌ 本地视频数据库初始化（Mock 数据或真实数据）

2. **Domain 层**
   - ❌ Repository 接口定义（部分业务模块可能缺失）
   - ❌ UseCase 实现
   - ❌ Domain Model 定义

3. **Presentation 层**
   - ❌ ViewModel 实现
   - ❌ UIState/UIEvent 定义
   - ❌ 数据绑定（StateFlow/LiveData → UI）
   - ❌ Navigation 配置（页面路由、参数传递）

4. **依赖注入**
   - ❌ Hilt Module 配置（各业务模块的 DI 模块）
   - ❌ Repository、DataSource 的注入配置

5. **本地数据准备**
   - ❌ 视频数据库初始化脚本或工具
   - ❌ Mock 数据生成（用于开发测试）

---

## 二、功能开发前必须完成的工作清单（业务优先模式）

> **开发顺序说明**：采用从具象到抽象的方式，先做UI层让项目跑起来，再逐步对接底层数据。这样可以让团队快速看到成果，同时并行开发不阻塞。

### 阶段 1：UI层 + Navigation（优先级：🔥🔥🔥 最高）

> **目标**：让项目先跑起来，所有页面可见，页面跳转正常工作。不涉及数据层，使用静态UI和占位内容。

#### 1.1 所有业务模块的 Fragment + 布局
**目标**：创建所有页面的Fragment和布局文件，让每个页面都能显示。

**任务清单**：
- [ ] **VideoFeed 业务**（成员A）
  - [ ] 完善 `FeedFragment.kt`
    - 路径：`BeatUClient/business/videofeed/presentation/src/main/java/com/ucw/beatu/business/videofeed/presentation/ui/FeedFragment.kt`
  - [ ] 完善 `fragment_feed.xml` 布局
    - 路径：`BeatUClient/business/videofeed/presentation/src/main/res/layout/fragment_feed.xml`
  - [ ] 添加占位内容（视频占位图、作者信息占位等）
- [ ] **User 业务**（成员B）
  - [ ] 创建 `UserProfileFragment.kt`
    - 路径：`BeatUClient/business/user/presentation/src/main/java/com/ucw/beatu/business/user/presentation/ui/UserProfileFragment.kt`
  - [ ] 创建 `fragment_user_profile.xml` 布局
    - 路径：`BeatUClient/business/user/presentation/src/main/res/layout/fragment_user_profile.xml`
  - [ ] 添加占位内容（头像、昵称、作品列表等）
- [ ] **Search 业务**（成员B）
  - [ ] 创建 `SearchFragment.kt`
    - 路径：`BeatUClient/business/search/presentation/src/main/java/com/ucw/beatu/business/search/presentation/ui/SearchFragment.kt`
  - [ ] 创建 `fragment_search.xml` 布局
    - 路径：`BeatUClient/business/search/presentation/src/main/res/layout/fragment_search.xml`
  - [ ] 添加占位内容（搜索框、搜索结果列表等）
- [ ] **Settings 业务**（成员C）
  - [ ] 创建 `SettingsFragment.kt`
    - 路径：`BeatUClient/business/settings/presentation/src/main/java/com/ucw/beatu/business/settings/presentation/ui/SettingsFragment.kt`
  - [ ] 创建 `fragment_settings.xml` 布局
    - 路径：`BeatUClient/business/settings/presentation/src/main/res/layout/fragment_settings.xml`
  - [ ] 添加占位内容（设置项列表等）
- [ ] **Landscape 业务**（成员C）
  - [ ] 创建 `LandscapeActivity.kt` 或 `LandscapeFragment.kt`
    - Activity路径：`BeatUClient/business/landscape/presentation/src/main/java/com/ucw/beatu/business/landscape/presentation/ui/LandscapeActivity.kt`
    - Fragment路径：`BeatUClient/business/landscape/presentation/src/main/java/com/ucw/beatu/business/landscape/presentation/ui/LandscapeFragment.kt`
  - [ ] 创建对应布局文件
    - Activity布局：`BeatUClient/business/landscape/presentation/src/main/res/layout/activity_landscape.xml`
    - Fragment布局：`BeatUClient/business/landscape/presentation/src/main/res/layout/fragment_landscape.xml`
  - [ ] 添加占位内容（横屏播放器界面）
- [ ] **AI 业务**（成员C，如果有独立页面）
  - [ ] 创建 AI 相关 Fragment（如评论弹层）
    - 路径：`BeatUClient/business/ai/presentation/src/main/java/com/ucw/beatu/business/ai/presentation/ui/AiCommentDialogFragment.kt`（示例）
  - [ ] 创建对应布局文件
    - 路径：`BeatUClient/business/ai/presentation/src/main/res/layout/fragment_ai_comment_dialog.xml`（示例）

**预计工作量**：2-3 天（3人并行）

#### 1.2 Navigation 配置
**目标**：配置页面路由，实现页面跳转。

**任务清单**：
- [x] 创建 Navigation Graph（成员A） - 2024-12-19
  - 路径：`BeatUClient/app/src/main/res/navigation/main_nav_graph.xml`
  - 包含所有页面路由：Feed、UserProfile、Search、Settings、Landscape
  - 已完成：创建了完整的 Navigation Graph，包含所有页面路由和跳转 action
- [ ] 在 `MainActivity` 中配置 Navigation（成员A）
  - 文件路径：`BeatUClient/app/src/main/java/com/ucw/beatu/MainActivity.kt`
  - 设置 `NavController`
  - 配置 `NavHostFragment`
  - 设置默认启动页面（FeedFragment）
- [ ] 实现页面跳转逻辑（各成员负责自己模块）
  - FeedFragment → UserProfileFragment（点击"我的"按钮）
    - 在 `FeedFragment.kt` 中添加跳转代码
  - FeedFragment → SearchFragment（点击搜索图标）
    - 在 `FeedFragment.kt` 中添加跳转代码
  - FeedFragment → SettingsFragment（如果有设置入口）
    - 在 `FeedFragment.kt` 中添加跳转代码
  - FeedFragment → LandscapeActivity（横屏模式，后续实现）
    - 在 `FeedFragment.kt` 中添加跳转代码
  - UserProfileFragment → FeedFragment（返回）
    - 在 `UserProfileFragment.kt` 中添加返回逻辑
  - SearchFragment → FeedFragment（返回）
    - 在 `SearchFragment.kt` 中添加返回逻辑

**预计工作量**：1 天

#### 1.3 基础交互（点击事件）
**目标**：为所有按钮和可点击元素添加点击事件，暂时只做页面跳转，不做业务逻辑。

**任务清单**：
- [ ] FeedFragment 中的按钮点击事件（成员A）
  - 顶部导航栏按钮（关注/推荐/我的）
  - 搜索图标
  - 底部交互按钮（点赞/收藏/评论/分享）- 暂时只显示Toast或占位
- [ ] UserProfileFragment 中的按钮点击事件（成员B）
  - 返回按钮
  - 关注/取消关注按钮（占位）
- [ ] SearchFragment 中的交互（成员B）
  - 搜索框点击
  - 返回按钮
- [ ] SettingsFragment 中的交互（成员C）
  - 设置项点击（占位）
  - 返回按钮

**预计工作量**：0.5 天

**阶段1完成标准**：
- ✅ 所有页面可以正常显示
- ✅ 所有页面跳转正常工作
- ✅ 项目可以运行，无崩溃
- ✅ 可以看到完整的UI结构

---

### 阶段 2：ViewModel + UIState（优先级：🔥🔥 高）

> **目标**：为UI层添加ViewModel和状态管理，使用Mock数据让UI"动起来"，不依赖真实数据层。

#### 2.1 UIState 和 UIEvent 定义
**目标**：定义UI状态和事件，为数据驱动UI做准备。

**任务清单**：
- [ ] `FeedUIState`、`FeedUIEvent`（成员A）
  - 路径：`BeatUClient/business/videofeed/presentation/src/main/java/com/ucw/beatu/business/videofeed/presentation/ui/state/FeedUIState.kt`
  - 路径：`BeatUClient/business/videofeed/presentation/src/main/java/com/ucw/beatu/business/videofeed/presentation/ui/state/FeedUIEvent.kt`
  - 定义视频列表状态、加载状态、错误状态
  - 定义UI事件（点赞、收藏、评论等）
- [ ] `UserProfileUIState`、`UserProfileUIEvent`（成员B）
  - 路径：`BeatUClient/business/user/presentation/src/main/java/com/ucw/beatu/business/user/presentation/ui/state/UserProfileUIState.kt`
  - 路径：`BeatUClient/business/user/presentation/src/main/java/com/ucw/beatu/business/user/presentation/ui/state/UserProfileUIEvent.kt`
- [ ] `SearchUIState`、`SearchUIEvent`（成员B）
  - 路径：`BeatUClient/business/search/presentation/src/main/java/com/ucw/beatu/business/search/presentation/ui/state/SearchUIState.kt`
  - 路径：`BeatUClient/business/search/presentation/src/main/java/com/ucw/beatu/business/search/presentation/ui/state/SearchUIEvent.kt`
- [ ] `SettingsUIState`、`SettingsUIEvent`（成员C）
  - 路径：`BeatUClient/business/settings/presentation/src/main/java/com/ucw/beatu/business/settings/presentation/ui/state/SettingsUIState.kt`
  - 路径：`BeatUClient/business/settings/presentation/src/main/java/com/ucw/beatu/business/settings/presentation/ui/state/SettingsUIEvent.kt`
- [ ] 其他业务模块的 UIState/UIEvent

**预计工作量**：0.5 天

#### 2.2 ViewModel 基础实现（使用Mock数据）
**目标**：实现ViewModel，使用硬编码的Mock数据，不依赖Repository。

**任务清单**：
- [ ] `FeedViewModel`（成员A）
  - 路径：`BeatUClient/business/videofeed/presentation/src/main/java/com/ucw/beatu/business/videofeed/presentation/viewmodel/FeedViewModel.kt`
  - 创建Mock数据（硬编码几个视频对象）
  - 暴露 `StateFlow<FeedUIState>`
  - 实现UI事件处理（点赞、收藏等）- 暂时只更新UI状态，不调用Repository
- [ ] `UserProfileViewModel`（成员B）
  - 路径：`BeatUClient/business/user/presentation/src/main/java/com/ucw/beatu/business/user/presentation/viewmodel/UserProfileViewModel.kt`
  - 创建Mock用户数据
  - 暴露 `StateFlow<UserProfileUIState>`
- [ ] `SearchViewModel`（成员B）
  - 路径：`BeatUClient/business/search/presentation/src/main/java/com/ucw/beatu/business/search/presentation/viewmodel/SearchViewModel.kt`
  - 创建Mock搜索结果数据
  - 暴露 `StateFlow<SearchUIState>`
- [ ] `SettingsViewModel`（成员C）
  - 路径：`BeatUClient/business/settings/presentation/src/main/java/com/ucw/beatu/business/settings/presentation/viewmodel/SettingsViewModel.kt`
  - 暴露 `StateFlow<SettingsUIState>`
- [ ] 其他业务模块的 ViewModel

**预计工作量**：1-2 天

#### 2.3 UI 数据绑定
**目标**：将ViewModel的状态绑定到UI，实现数据驱动UI。

**任务清单**：
- [ ] FeedFragment 数据绑定（成员A）
  - 文件路径：`BeatUClient/business/videofeed/presentation/src/main/java/com/ucw/beatu/business/videofeed/presentation/ui/FeedFragment.kt`
  - 观察 `FeedViewModel.uiState`
  - 更新视频列表、作者信息、统计数据
  - 处理加载状态、错误状态
- [ ] UserProfileFragment 数据绑定（成员B）
  - 文件路径：`BeatUClient/business/user/presentation/src/main/java/com/ucw/beatu/business/user/presentation/ui/UserProfileFragment.kt`
- [ ] SearchFragment 数据绑定（成员B）
  - 文件路径：`BeatUClient/business/search/presentation/src/main/java/com/ucw/beatu/business/search/presentation/ui/SearchFragment.kt`
- [ ] SettingsFragment 数据绑定（成员C）
  - 文件路径：`BeatUClient/business/settings/presentation/src/main/java/com/ucw/beatu/business/settings/presentation/ui/SettingsFragment.kt`
- [ ] 其他Fragment的数据绑定

**预计工作量**：1 天

**阶段2完成标准**：
- ✅ 所有ViewModel已实现
- ✅ UI可以显示Mock数据
- ✅ UI交互可以更新状态（如点赞按钮状态变化）
- ✅ 数据流：UI → ViewModel → UIState → UI 正常工作

---

### 阶段 3：数据层对接（优先级：🔥 中）

> **目标**：对接真实数据层，替换Mock数据，实现数据持久化。

#### 3.1 本地视频数据库初始化
**目标**：搭建本地视频数据库，支持从本地读取视频数据。

**任务清单**：
- [ ] 创建数据库初始化工具类（`DatabaseInitializer`）（成员A）
  - 路径：`BeatUClient/shared/database/src/main/java/com/ucw/beatu/shared/database/initializer/DatabaseInitializer.kt`
  - 功能：在首次启动时插入 Mock 视频数据
- [ ] 准备 Mock 数据（成员A）
  - 路径：`BeatUClient/shared/database/src/main/java/com/ucw/beatu/shared/database/initializer/MockVideoData.kt`（或JSON文件）
  - 至少 20-30 条视频数据
  - 包含视频 URL（可使用网络视频 URL 或本地测试视频）
  - 包含封面图 URL
  - 包含作者信息、标签、统计数据
  - 格式：JSON 文件或 Kotlin 数据类
- [ ] 在 `BeatUApp.onCreate()` 中调用初始化工具（成员A）
  - 文件路径：`BeatUClient/app/src/main/java/com/ucw/beatu/BeatUApp.kt`
- [ ] 验证：通过 `VideoDao.observeTopVideos()` 能读取到数据（成员A）

**预计工作量**：1-2 天

#### 3.2 Domain Model 定义
**目标**：定义业务模型，与 Entity、DTO 区分。

**任务清单**：
- [ ] `business/videofeed/domain/model/Video.kt`（成员A）
  - 路径：`BeatUClient/business/videofeed/domain/src/main/java/com/ucw/beatu/business/videofeed/domain/model/Video.kt`
- [ ] `business/videofeed/domain/model/Comment.kt`（成员A）
  - 路径：`BeatUClient/business/videofeed/domain/src/main/java/com/ucw/beatu/business/videofeed/domain/model/Comment.kt`
- [ ] `business/user/domain/model/User.kt`（成员B）
  - 路径：`BeatUClient/business/user/domain/src/main/java/com/ucw/beatu/business/user/domain/model/User.kt`
- [ ] 其他业务模型（各成员负责自己模块）

**预计工作量**：0.5 天

#### 3.3 Repository 接口定义
**目标**：定义各业务模块的 Repository 接口。

**任务清单**：
- [ ] `business/videofeed/domain/repository/FeedRepository.kt`（成员A）
  - 路径：`BeatUClient/business/videofeed/domain/src/main/java/com/ucw/beatu/business/videofeed/domain/repository/FeedRepository.kt`
  - `fun fetchFeed(channel: String, cursor: String?): Flow<PagingData<Video>>`
  - `suspend fun likeVideo(videoId: String, action: LikeAction): Result<Unit>`
  - `suspend fun favoriteVideo(videoId: String, action: FavoriteAction): Result<Unit>`
  - `fun observeComments(videoId: String): Flow<List<Comment>>`
- [ ] `business/user/domain/repository/UserRepository.kt`（成员B）
  - 路径：`BeatUClient/business/user/domain/src/main/java/com/ucw/beatu/business/user/domain/repository/UserRepository.kt`
  - `fun getUserProfile(userId: String): Flow<User>`
  - `suspend fun followUser(userId: String, action: FollowAction): Result<Unit>`
- [ ] 其他业务模块的 Repository 接口（各成员负责）

**预计工作量**：1 天

#### 3.4 数据源实现（RemoteDataSource + LocalDataSource）
**目标**：实现数据获取逻辑，支持从网络和本地数据库读取数据。

**任务清单**：
- [ ] **VideoFeed 业务**（成员A）
  - [ ] `FeedLocalDataSource`：调用 `VideoDao` 获取本地数据
    - 路径：`BeatUClient/business/videofeed/data/src/main/java/com/ucw/beatu/business/videofeed/data/source/local/FeedLocalDataSource.kt`
  - [ ] `FeedRepositoryImpl`：协调 Local，实现缓存策略
    - 路径：`BeatUClient/business/videofeed/data/src/main/java/com/ucw/beatu/business/videofeed/data/repository/FeedRepositoryImpl.kt`
  - [ ] Entity → Model Mapper（`VideoEntityMapper`、`CommentEntityMapper`）
    - 路径：`BeatUClient/business/videofeed/data/src/main/java/com/ucw/beatu/business/videofeed/data/mapper/VideoEntityMapper.kt`
    - 路径：`BeatUClient/business/videofeed/data/src/main/java/com/ucw/beatu/business/videofeed/data/mapper/CommentEntityMapper.kt`
- [ ] **User 业务**（成员B）
  - [ ] `UserLocalDataSource`
    - 路径：`BeatUClient/business/user/data/src/main/java/com/ucw/beatu/business/user/data/source/local/UserLocalDataSource.kt`
  - [ ] `UserRepositoryImpl`
    - 路径：`BeatUClient/business/user/data/src/main/java/com/ucw/beatu/business/user/data/repository/UserRepositoryImpl.kt`
- [ ] **其他业务**（Search、AI、Landscape、Settings）
  - [ ] 按需实现对应的 DataSource 和 Repository（各成员负责）

**预计工作量**：2-3 天

#### 3.5 UseCase 实现
**目标**：实现业务逻辑用例，封装 Repository 调用。

**任务清单**：
- [ ] `business/videofeed/domain/usecase/`（成员A）
  - [ ] `GetFeedUseCase`
    - 路径：`BeatUClient/business/videofeed/domain/src/main/java/com/ucw/beatu/business/videofeed/domain/usecase/GetFeedUseCase.kt`
  - [ ] `LikeVideoUseCase`
    - 路径：`BeatUClient/business/videofeed/domain/src/main/java/com/ucw/beatu/business/videofeed/domain/usecase/LikeVideoUseCase.kt`
  - [ ] `FavoriteVideoUseCase`
    - 路径：`BeatUClient/business/videofeed/domain/src/main/java/com/ucw/beatu/business/videofeed/domain/usecase/FavoriteVideoUseCase.kt`
  - [ ] `GetCommentsUseCase`
    - 路径：`BeatUClient/business/videofeed/domain/src/main/java/com/ucw/beatu/business/videofeed/domain/usecase/GetCommentsUseCase.kt`
  - [ ] `CreateCommentUseCase`
    - 路径：`BeatUClient/business/videofeed/domain/src/main/java/com/ucw/beatu/business/videofeed/domain/usecase/CreateCommentUseCase.kt`
- [ ] `business/user/domain/usecase/`（成员B）
  - [ ] `GetUserProfileUseCase`
    - 路径：`BeatUClient/business/user/domain/src/main/java/com/ucw/beatu/business/user/domain/usecase/GetUserProfileUseCase.kt`
  - [ ] `FollowUserUseCase`
    - 路径：`BeatUClient/business/user/domain/src/main/java/com/ucw/beatu/business/user/domain/usecase/FollowUserUseCase.kt`
- [ ] 其他业务模块的 UseCase（各成员负责）

**预计工作量**：1-2 天

#### 3.6 ViewModel 对接真实数据
**目标**：将ViewModel从Mock数据切换到真实Repository。

**任务清单**：
- [ ] `FeedViewModel` 对接 `GetFeedUseCase`（成员A）
- [ ] `UserProfileViewModel` 对接 `GetUserProfileUseCase`（成员B）
- [ ] 其他ViewModel对接对应的UseCase（各成员负责）

**预计工作量**：1 天

**阶段3完成标准**：
- ✅ 本地数据库可以正常读取数据
- ✅ ViewModel使用真实Repository获取数据
- ✅ UI可以显示从数据库读取的数据
- ✅ 数据流：UI → ViewModel → UseCase → Repository → LocalDataSource → Database 正常工作

---

### 阶段 4：网络层对接（优先级：🔥 中）

> **目标**：对接网络层，支持从服务器获取数据。

#### 4.1 网络层 API 接口定义
**目标**：定义 Retrofit Service 接口。

**任务清单**：
- [ ] 创建 API Service 接口（成员A）
  - `FeedApiService`：`GET /api/v1/feed`
    - 路径：`BeatUClient/shared/network/src/main/java/com/ucw/beatu/shared/network/api/FeedApiService.kt`
  - `InteractionApiService`：`POST /api/v1/interaction/like`、`favorite`、`follow`
    - 路径：`BeatUClient/shared/network/src/main/java/com/ucw/beatu/shared/network/api/InteractionApiService.kt`
  - `CommentApiService`：`GET /api/v1/comment/list`、`POST /api/v1/comment/create`
    - 路径：`BeatUClient/shared/network/src/main/java/com/ucw/beatu/shared/network/api/CommentApiService.kt`
  - `AiApiService`：`POST /api/v1/comment/ai`、`/api/v1/ai/recommend`
    - 路径：`BeatUClient/shared/network/src/main/java/com/ucw/beatu/shared/network/api/AiApiService.kt`
- [ ] 定义 DTO 模型（成员A）
  - `VideoDto`、`CommentDto`、`UserDto`、`FeedResponseDto` 等
    - 路径：`BeatUClient/shared/network/src/main/java/com/ucw/beatu/shared/network/dto/VideoDto.kt`
    - 路径：`BeatUClient/shared/network/src/main/java/com/ucw/beatu/shared/network/dto/CommentDto.kt`
    - 路径：`BeatUClient/shared/network/src/main/java/com/ucw/beatu/shared/network/dto/UserDto.kt`
    - 路径：`BeatUClient/shared/network/src/main/java/com/ucw/beatu/shared/network/dto/FeedResponseDto.kt`
- [ ] 配置 Retrofit Service 的 Hilt Module（成员C）
  - 路径：`BeatUClient/shared/network/src/main/java/com/ucw/beatu/shared/network/di/NetworkModule.kt`

**预计工作量**：1 天

#### 4.2 RemoteDataSource 实现
**目标**：实现网络数据源，调用API获取数据。

**任务清单**：
- [ ] `FeedRemoteDataSource`：调用 `FeedApiService` 获取网络数据（成员A）
  - 路径：`BeatUClient/business/videofeed/data/src/main/java/com/ucw/beatu/business/videofeed/data/source/remote/FeedRemoteDataSource.kt`
- [ ] `UserRemoteDataSource`（成员B）
  - 路径：`BeatUClient/business/user/data/src/main/java/com/ucw/beatu/business/user/data/source/remote/UserRemoteDataSource.kt`
- [ ] 其他业务的 RemoteDataSource（各成员负责）
  - Search：`BeatUClient/business/search/data/src/main/java/com/ucw/beatu/business/search/data/source/remote/SearchRemoteDataSource.kt`
  - Settings、Landscape、AI：类似路径结构
- [ ] DTO → Model Mapper（`VideoDtoMapper`、`CommentDtoMapper`等）（各成员负责）
  - VideoDtoMapper：`BeatUClient/business/videofeed/data/src/main/java/com/ucw/beatu/business/videofeed/data/mapper/VideoDtoMapper.kt`
  - CommentDtoMapper：`BeatUClient/business/videofeed/data/src/main/java/com/ucw/beatu/business/videofeed/data/mapper/CommentDtoMapper.kt`

**预计工作量**：1-2 天

#### 4.3 Repository 实现网络+本地策略
**目标**：Repository协调网络和本地数据源，实现缓存策略。

**任务清单**：
- [ ] `FeedRepositoryImpl` 实现网络+本地策略（成员A）
  - 路径：`BeatUClient/business/videofeed/data/src/main/java/com/ucw/beatu/business/videofeed/data/repository/FeedRepositoryImpl.kt`
  - 优先从本地读取，本地无数据时从网络获取
  - 网络获取后更新本地缓存
- [ ] `UserRepositoryImpl` 实现网络+本地策略（成员B）
  - 路径：`BeatUClient/business/user/data/src/main/java/com/ucw/beatu/business/user/data/repository/UserRepositoryImpl.kt`
- [ ] 其他Repository实现网络+本地策略（各成员负责）
  - Search：`BeatUClient/business/search/data/src/main/java/com/ucw/beatu/business/search/data/repository/SearchRepositoryImpl.kt`
  - Settings、Landscape、AI：类似路径结构

**预计工作量**：1-2 天

**阶段4完成标准**：
- ✅ API接口已定义
- ✅ RemoteDataSource可以正常调用API
- ✅ Repository可以协调网络和本地数据
- ✅ 数据流：UI → ViewModel → UseCase → Repository → (RemoteDataSource/LocalDataSource) 正常工作

---

### 阶段 5：依赖注入配置（优先级：🔥 中）

> **目标**：配置所有模块的依赖注入，确保依赖关系正确。

#### 5.1 Hilt Module 配置
**目标**：配置所有模块的依赖注入。

**任务清单**：
- [ ] `shared/database/di/DatabaseModule.kt`（成员C）
  - 路径：`BeatUClient/shared/database/src/main/java/com/ucw/beatu/shared/database/di/DatabaseModule.kt`
  - 提供 `BeatUDatabase`、`VideoDao`、`CommentDao` 等
- [ ] `shared/network/di/NetworkModule.kt`（成员C）
  - 路径：`BeatUClient/shared/network/src/main/java/com/ucw/beatu/shared/network/di/NetworkModule.kt`
  - 提供 `Retrofit`、`OkHttpClient`、API Service
- [ ] `business/videofeed/data/di/VideoFeedDataModule.kt`（成员C）
  - 路径：`BeatUClient/business/videofeed/data/src/main/java/com/ucw/beatu/business/videofeed/data/di/VideoFeedDataModule.kt`
  - 提供 `FeedRepository`、`FeedRemoteDataSource`、`FeedLocalDataSource`
- [ ] `business/videofeed/domain/di/VideoFeedDomainModule.kt`（成员C）
  - 路径：`BeatUClient/business/videofeed/domain/src/main/java/com/ucw/beatu/business/videofeed/domain/di/VideoFeedDomainModule.kt`
  - 提供 UseCase
- [ ] 其他业务模块的 DI 模块（成员C）
  - User业务：`BeatUClient/business/user/data/src/main/java/com/ucw/beatu/business/user/data/di/UserDataModule.kt`
  - User业务：`BeatUClient/business/user/domain/src/main/java/com/ucw/beatu/business/user/domain/di/UserDomainModule.kt`
  - Search业务：类似路径结构
  - Settings业务：类似路径结构
  - 其他业务：类似路径结构

**预计工作量**：1-2 天

**阶段5完成标准**：
- ✅ 所有依赖注入配置完成
- ✅ 项目可以正常运行，无DI相关错误
- ✅ ViewModel、Repository、DataSource都可以正常注入

---

### 阶段 6：播放器集成（优先级：🔥 中）

> **目标**：将播放器集成到UI层，实现视频播放功能。

#### 6.1 播放器与 UI 层集成
**目标**：将播放器集成到 FeedFragment，实现视频播放。

**任务清单**：
- [ ] 在 `FeedViewModel` 中管理播放器生命周期（成员A + 成员C协作）
  - 文件路径：`BeatUClient/business/videofeed/presentation/src/main/java/com/ucw/beatu/business/videofeed/presentation/viewmodel/FeedViewModel.kt`
  - `onPageSelected` → `PlayerPool.attach(surface)` → `play()`
  - `onPageRelease` → `pause()/release()`
- [ ] 在 `FeedFragment` 中绑定 `SurfaceView` 和播放器（成员A）
  - 文件路径：`BeatUClient/business/videofeed/presentation/src/main/java/com/ucw/beatu/business/videofeed/presentation/ui/FeedFragment.kt`
  - 布局文件：`BeatUClient/business/videofeed/presentation/src/main/res/layout/fragment_feed.xml`（添加SurfaceView）
- [ ] 实现预加载逻辑（N+1 视频）（成员C）
  - 播放器池路径：`BeatUClient/shared/player/src/main/java/com/ucw/beatu/shared/player/VideoPlayerPool.kt`
  - 预加载逻辑可在FeedViewModel或独立的PreloadManager中实现
- [ ] 实现播放器状态监听（播放/暂停/错误）（成员A）
  - 在FeedViewModel中监听播放器状态
  - 更新UIState中的播放状态

**预计工作量**：2-3 天

**阶段6完成标准**：
- ✅ FeedFragment可以播放视频
- ✅ 上下滑动可以切换视频
- ✅ 播放器生命周期管理正确（无内存泄漏）
- ✅ 预加载功能正常工作

---

## 三、3人团队分工建议（业务优先模式）

### 分工原则

1. **按业务模块分工**：每人负责 1-2 个完整业务模块（包含 Presentation/Domain/Data 三层）
2. **UI优先**：先完成UI层和页面跳转，再逐步对接底层
3. **并行开发**：UI层可以完全并行开发，互不阻塞

### 推荐分工方案

#### 👤 成员 A：视频流业务 + Navigation配置

**负责模块**：
- `business/videofeed/`（核心业务）
- Navigation 配置（页面路由）

**阶段1任务（UI层）**：
1. **FeedFragment UI完善**
   - 完善 `FeedFragment.kt`
     - 路径：`BeatUClient/business/videofeed/presentation/src/main/java/com/ucw/beatu/business/videofeed/presentation/ui/FeedFragment.kt`
   - 完善 `fragment_feed.xml` 布局
     - 路径：`BeatUClient/business/videofeed/presentation/src/main/res/layout/fragment_feed.xml`
   - 添加占位内容和交互
2. **Navigation 配置**
   - 创建 Navigation Graph
     - 路径：`BeatUClient/app/src/main/res/navigation/main_nav_graph.xml`
   - 在 MainActivity 中配置 Navigation
     - 路径：`BeatUClient/app/src/main/java/com/ucw/beatu/MainActivity.kt`
   - 实现所有页面跳转逻辑

**阶段2任务（ViewModel）**：
1. **FeedViewModel实现**
   - 路径：`BeatUClient/business/videofeed/presentation/src/main/java/com/ucw/beatu/business/videofeed/presentation/viewmodel/FeedViewModel.kt`
   - UIState路径：`BeatUClient/business/videofeed/presentation/src/main/java/com/ucw/beatu/business/videofeed/presentation/ui/state/FeedUIState.kt`
   - UIEvent路径：`BeatUClient/business/videofeed/presentation/src/main/java/com/ucw/beatu/business/videofeed/presentation/ui/state/FeedUIEvent.kt`
   - 使用Mock数据
   - 实现UIState/UIEvent
   - 数据绑定（在FeedFragment中）

**阶段3-4任务（数据层）**：
1. **数据库初始化**
   - 创建 `DatabaseInitializer`
     - 路径：`BeatUClient/shared/database/src/main/java/com/ucw/beatu/shared/database/initializer/DatabaseInitializer.kt`
   - 准备Mock数据
     - 路径：`BeatUClient/shared/database/src/main/java/com/ucw/beatu/shared/database/initializer/MockVideoData.kt`
2. **VideoFeed数据层实现**
   - Domain Model
     - 路径：`BeatUClient/business/videofeed/domain/src/main/java/com/ucw/beatu/business/videofeed/domain/model/Video.kt`
     - 路径：`BeatUClient/business/videofeed/domain/src/main/java/com/ucw/beatu/business/videofeed/domain/model/Comment.kt`
   - Repository接口
     - 路径：`BeatUClient/business/videofeed/domain/src/main/java/com/ucw/beatu/business/videofeed/domain/repository/FeedRepository.kt`
   - LocalDataSource
     - 路径：`BeatUClient/business/videofeed/data/src/main/java/com/ucw/beatu/business/videofeed/data/source/local/FeedLocalDataSource.kt`
   - RepositoryImpl
     - 路径：`BeatUClient/business/videofeed/data/src/main/java/com/ucw/beatu/business/videofeed/data/repository/FeedRepositoryImpl.kt`
   - UseCase实现
     - 路径：`BeatUClient/business/videofeed/domain/src/main/java/com/ucw/beatu/business/videofeed/domain/usecase/`
3. **网络层API定义**
   - API Service接口
     - 路径：`BeatUClient/shared/network/src/main/java/com/ucw/beatu/shared/network/api/FeedApiService.kt`
   - DTO模型
     - 路径：`BeatUClient/shared/network/src/main/java/com/ucw/beatu/shared/network/dto/`
4. **RemoteDataSource实现**
   - FeedRemoteDataSource
     - 路径：`BeatUClient/business/videofeed/data/src/main/java/com/ucw/beatu/business/videofeed/data/source/remote/FeedRemoteDataSource.kt`
   - DTO → Model Mapper
     - 路径：`BeatUClient/business/videofeed/data/src/main/java/com/ucw/beatu/business/videofeed/data/mapper/VideoDtoMapper.kt`

**阶段6任务（播放器）**：
1. **播放器集成**（与团队协作）
   - FeedFragment中集成播放器
   - 播放器生命周期管理

**预计总工作量**：20天（与团队协作完成）

#### 👤 成员 B：用户业务 + 搜索业务

**负责模块**：
- `business/user/`（用户主页、关注功能）
- `business/search/`（搜索功能）

**阶段1任务（UI层）**：
1. **UserProfileFragment**
   - 创建Fragment
     - 路径：`BeatUClient/business/user/presentation/src/main/java/com/ucw/beatu/business/user/presentation/ui/UserProfileFragment.kt`
   - 创建布局
     - 路径：`BeatUClient/business/user/presentation/src/main/res/layout/fragment_user_profile.xml`
   - 添加占位内容
   - 实现点击事件
2. **SearchFragment**
   - 创建Fragment
     - 路径：`BeatUClient/business/search/presentation/src/main/java/com/ucw/beatu/business/search/presentation/ui/SearchFragment.kt`
   - 创建布局
     - 路径：`BeatUClient/business/search/presentation/src/main/res/layout/fragment_search.xml`
   - 添加占位内容
   - 实现点击事件

**阶段2任务（ViewModel）**：
1. **UserProfileViewModel**
   - ViewModel路径：`BeatUClient/business/user/presentation/src/main/java/com/ucw/beatu/business/user/presentation/viewmodel/UserProfileViewModel.kt`
   - UIState路径：`BeatUClient/business/user/presentation/src/main/java/com/ucw/beatu/business/user/presentation/ui/state/UserProfileUIState.kt`
   - UIEvent路径：`BeatUClient/business/user/presentation/src/main/java/com/ucw/beatu/business/user/presentation/ui/state/UserProfileUIEvent.kt`
   - 使用Mock数据
   - 实现UIState/UIEvent
   - 数据绑定（在UserProfileFragment中）
2. **SearchViewModel**
   - ViewModel路径：`BeatUClient/business/search/presentation/src/main/java/com/ucw/beatu/business/search/presentation/viewmodel/SearchViewModel.kt`
   - UIState路径：`BeatUClient/business/search/presentation/src/main/java/com/ucw/beatu/business/search/presentation/ui/state/SearchUIState.kt`
   - UIEvent路径：`BeatUClient/business/search/presentation/src/main/java/com/ucw/beatu/business/search/presentation/ui/state/SearchUIEvent.kt`
   - 使用Mock数据
   - 实现UIState/UIEvent
   - 数据绑定（在SearchFragment中）

**阶段3-4任务（数据层）**：
1. **User业务数据层实现**
   - Domain Model
     - 路径：`BeatUClient/business/user/domain/src/main/java/com/ucw/beatu/business/user/domain/model/User.kt`
   - Repository接口
     - 路径：`BeatUClient/business/user/domain/src/main/java/com/ucw/beatu/business/user/domain/repository/UserRepository.kt`
   - LocalDataSource
     - 路径：`BeatUClient/business/user/data/src/main/java/com/ucw/beatu/business/user/data/source/local/UserLocalDataSource.kt`
   - RemoteDataSource
     - 路径：`BeatUClient/business/user/data/src/main/java/com/ucw/beatu/business/user/data/source/remote/UserRemoteDataSource.kt`
   - RepositoryImpl
     - 路径：`BeatUClient/business/user/data/src/main/java/com/ucw/beatu/business/user/data/repository/UserRepositoryImpl.kt`
   - UseCase实现
     - 路径：`BeatUClient/business/user/domain/src/main/java/com/ucw/beatu/business/user/domain/usecase/`
2. **Search业务数据层实现**
   - Domain Model
     - 路径：`BeatUClient/business/search/domain/src/main/java/com/ucw/beatu/business/search/domain/model/`
   - Repository接口
     - 路径：`BeatUClient/business/search/domain/src/main/java/com/ucw/beatu/business/search/domain/repository/SearchRepository.kt`
   - LocalDataSource
     - 路径：`BeatUClient/business/search/data/src/main/java/com/ucw/beatu/business/search/data/source/local/SearchLocalDataSource.kt`
   - RemoteDataSource
     - 路径：`BeatUClient/business/search/data/src/main/java/com/ucw/beatu/business/search/data/source/remote/SearchRemoteDataSource.kt`
   - RepositoryImpl
     - 路径：`BeatUClient/business/search/data/src/main/java/com/ucw/beatu/business/search/data/repository/SearchRepositoryImpl.kt`
   - UseCase实现
     - 路径：`BeatUClient/business/search/domain/src/main/java/com/ucw/beatu/business/search/domain/usecase/`

**预计总工作量**：20天（与团队协作完成）

#### 👤 成员 C：横屏业务 + AI业务 + 设置业务 + 公共模块

**负责模块**：
- `business/landscape/`（横屏模式）
- `business/ai/`（AI 评论助手）
- `business/settings/`（设置页面）
- `shared/player/`（播放器完善）
- 依赖注入配置

**阶段1任务（UI层）**：
1. **SettingsFragment**
   - 创建Fragment
     - 路径：`BeatUClient/business/settings/presentation/src/main/java/com/ucw/beatu/business/settings/presentation/ui/SettingsFragment.kt`
   - 创建布局
     - 路径：`BeatUClient/business/settings/presentation/src/main/res/layout/fragment_settings.xml`
   - 添加占位内容
2. **LandscapeActivity/Fragment**
   - 创建Activity（推荐）或Fragment
     - Activity路径：`BeatUClient/business/landscape/presentation/src/main/java/com/ucw/beatu/business/landscape/presentation/ui/LandscapeActivity.kt`
     - Fragment路径：`BeatUClient/business/landscape/presentation/src/main/java/com/ucw/beatu/business/landscape/presentation/ui/LandscapeFragment.kt`
   - 创建布局
     - Activity布局：`BeatUClient/business/landscape/presentation/src/main/res/layout/activity_landscape.xml`
     - Fragment布局：`BeatUClient/business/landscape/presentation/src/main/res/layout/fragment_landscape.xml`
   - 添加占位内容
3. **AI相关页面**（如果有独立页面）
   - 创建Fragment（如评论弹层）
     - 路径：`BeatUClient/business/ai/presentation/src/main/java/com/ucw/beatu/business/ai/presentation/ui/AiCommentDialogFragment.kt`（示例）
   - 创建布局
     - 路径：`BeatUClient/business/ai/presentation/src/main/res/layout/fragment_ai_comment_dialog.xml`（示例）

**阶段2任务（ViewModel）**：
1. **SettingsViewModel**
   - ViewModel路径：`BeatUClient/business/settings/presentation/src/main/java/com/ucw/beatu/business/settings/presentation/viewmodel/SettingsViewModel.kt`
   - UIState路径：`BeatUClient/business/settings/presentation/src/main/java/com/ucw/beatu/business/settings/presentation/ui/state/SettingsUIState.kt`
   - UIEvent路径：`BeatUClient/business/settings/presentation/src/main/java/com/ucw/beatu/business/settings/presentation/ui/state/SettingsUIEvent.kt`
   - 使用Mock数据
   - 实现UIState/UIEvent
2. **LandscapeViewModel**（如果需要）
   - 路径：`BeatUClient/business/landscape/presentation/src/main/java/com/ucw/beatu/business/landscape/presentation/viewmodel/LandscapeViewModel.kt`
3. **AI相关ViewModel**（如果需要）
   - 路径：`BeatUClient/business/ai/presentation/src/main/java/com/ucw/beatu/business/ai/presentation/viewmodel/`（按需创建）

**阶段3-4任务（数据层）**：
1. **Settings业务数据层实现**
   - Domain Model、Repository接口
   - DataStore实现
   - UseCase实现
2. **Landscape业务数据层实现**
3. **AI业务数据层实现**

**阶段5任务（依赖注入）**：
1. **所有Hilt Module配置**
   - shared模块的DI
     - DatabaseModule：`BeatUClient/shared/database/src/main/java/com/ucw/beatu/shared/database/di/DatabaseModule.kt`
     - NetworkModule：`BeatUClient/shared/network/src/main/java/com/ucw/beatu/shared/network/di/NetworkModule.kt`
   - 所有业务模块的DI
     - VideoFeed：`BeatUClient/business/videofeed/data/src/main/java/com/ucw/beatu/business/videofeed/data/di/VideoFeedDataModule.kt`
     - VideoFeed：`BeatUClient/business/videofeed/domain/src/main/java/com/ucw/beatu/business/videofeed/domain/di/VideoFeedDomainModule.kt`
     - User：`BeatUClient/business/user/data/src/main/java/com/ucw/beatu/business/user/data/di/UserDataModule.kt`
     - User：`BeatUClient/business/user/domain/src/main/java/com/ucw/beatu/business/user/domain/di/UserDomainModule.kt`
     - Search、Settings、Landscape、AI：类似路径结构

**阶段6任务（播放器）**：
1. **播放器集成**（与成员A协作）
   - 播放器生命周期管理
   - 预加载逻辑实现

**预计总工作量**：20天（与团队协作完成）

### 协作点

1. **Navigation配置**（阶段1）
   - 成员A 负责Navigation Graph配置
   - 成员B 和成员C 提供页面跳转需求，测试跳转功能

2. **播放器集成**（阶段6）
   - 成员A 负责FeedFragment中的播放器调用
   - 成员C 负责播放器生命周期管理和预加载

3. **依赖注入**（阶段5）
   - 成员C 负责所有DI模块配置
   - 成员A 和成员B 提供依赖需求，测试DI配置

---

## 四、开发时间线建议（20天业务优先模式）

> **重要说明**：项目周期为20个工作日（4周），需要压缩任务，优先完成核心功能。非核心业务（Search、Settings、AI）可以简化实现。

### 第1周（Day 1-5）：UI层 + Navigation

**目标**：所有核心页面可见，页面跳转正常工作。

- **Day 1-2**：核心Fragment + 布局（并行开发）
  - 成员A：完善FeedFragment布局
  - 成员B：创建UserProfileFragment + 布局（SearchFragment简化，只做基础UI）
  - 成员C：创建SettingsFragment + LandscapeActivity + 布局（AI相关页面可延后）
- **Day 3**：Navigation配置
  - 成员A：创建Navigation Graph，配置MainActivity
  - 所有成员：实现各自模块的页面跳转逻辑
- **Day 4**：基础交互
  - 所有成员：为按钮添加点击事件（页面跳转）
- **Day 5**：联调和测试
  - 所有成员：测试所有页面显示和跳转，修复问题

**第1周完成标准**：
- ✅ 核心页面可以正常显示（Feed、UserProfile、Settings、Landscape）
- ✅ 所有页面跳转正常工作
- ✅ 项目可以运行，无崩溃

---

### 第2周（Day 6-10）：ViewModel + Mock数据

**目标**：UI可以显示数据，交互可以更新状态。

- **Day 6**：UIState/UIEvent定义（并行）
  - 所有成员：定义各自模块的UIState和UIEvent（核心业务优先）
- **Day 7-8**：ViewModel实现（使用Mock数据，并行）
  - 成员A：FeedViewModel（核心，优先完成）
  - 成员B：UserProfileViewModel（SearchViewModel可简化）
  - 成员C：SettingsViewModel（LandscapeViewModel可延后）
- **Day 9**：UI数据绑定
  - 所有成员：将ViewModel状态绑定到UI（核心业务优先）
- **Day 10**：联调和测试
  - 所有成员：测试数据绑定和状态更新

**第2周完成标准**：
- ✅ 核心ViewModel已实现（FeedViewModel、UserProfileViewModel）
- ✅ UI可以显示Mock数据
- ✅ UI交互可以更新状态

---

### 第3周（Day 11-15）：数据层对接 + UseCase + 依赖注入基础

**目标**：对接本地数据库，实现核心业务数据流。

- **Day 11**：数据库初始化（成员A）
  - 创建DatabaseInitializer，准备Mock数据
  - 在BeatUApp中调用初始化
- **Day 12**：Domain Model + Repository接口（并行）
  - 成员A：VideoFeed的Domain Model和Repository接口
  - 成员B：User的Domain Model和Repository接口
  - 成员C：Settings的Domain Model和Repository接口（简化）
- **Day 13**：UseCase实现（并行）
  - 成员A：VideoFeed的UseCase（核心）
  - 成员B：User的UseCase
  - 成员C：Settings的UseCase（简化）
- **Day 14**：数据源实现（并行）
  - 成员A：FeedLocalDataSource + FeedRepositoryImpl（核心）
  - 成员B：UserLocalDataSource + UserRepositoryImpl
  - 成员C：Settings的LocalDataSource + RepositoryImpl（简化）
- **Day 15**：ViewModel对接 + 依赖注入基础配置
  - 所有成员：将ViewModel从Mock数据切换到真实Repository（核心业务优先）
  - 成员C：配置核心业务的Hilt Module（VideoFeed、User）

**第3周完成标准**：
- ✅ 本地数据库可以正常读取数据
- ✅ 核心业务ViewModel使用真实Repository获取数据
- ✅ 核心业务UI可以显示从数据库读取的数据
- ✅ 核心业务依赖注入配置完成

---

### 第4周（Day 16-20）：网络层对接 + 播放器集成 + 最终联调

**目标**：完成网络层对接和播放器集成，项目可演示。

- **Day 16**：网络层API定义（成员A）
  - 创建核心API Service接口（FeedApiService、InteractionApiService）
  - 定义核心DTO模型（VideoDto、CommentDto）
  - 成员C：配置NetworkModule
- **Day 17**：RemoteDataSource实现（并行）
  - 成员A：FeedRemoteDataSource（核心）
  - 成员B：UserRemoteDataSource（简化）
  - 成员C：其他业务的RemoteDataSource（可选）
- **Day 18**：Repository网络+本地策略 + 播放器集成准备
  - 成员A：FeedRepositoryImpl实现网络+本地策略（核心）
  - 成员B：UserRepositoryImpl实现网络+本地策略（简化）
  - 成员C：播放器生命周期管理完善
- **Day 19**：播放器集成 + 依赖注入完善
  - 成员A：在FeedViewModel中集成播放器，在FeedFragment中绑定SurfaceView
  - 成员C：实现N+1视频预加载逻辑
  - 成员C：完善所有Hilt Module配置
- **Day 20**：最终联调和测试
  - 所有成员：测试所有功能，修复问题
  - 所有成员：性能测试，内存泄漏检查
  - 所有成员：准备演示

**第4周完成标准**：
- ✅ 核心业务RemoteDataSource可以正常调用API
- ✅ 核心业务Repository可以协调网络和本地数据
- ✅ FeedFragment可以播放视频
- ✅ 上下滑动可以切换视频
- ✅ 播放器生命周期管理正确
- ✅ 所有依赖注入配置完成
- ✅ 项目可以正常运行，核心功能完整

---

### 时间线优化说明

**压缩策略**：
1. **核心业务优先**：VideoFeed、User、播放器是核心，优先完成
2. **非核心业务简化**：Search、Settings、AI可以只做基础实现，后续迭代完善
3. **并行开发最大化**：同一阶段的任务尽量并行进行
4. **依赖注入提前**：在第3周就开始配置核心业务的DI，不等到最后
5. **网络层简化**：只实现核心业务的网络层，非核心业务可以延后

**风险控制**：
- 如果第3周进度延迟，第4周优先保证播放器集成，网络层可以简化
- 如果播放器集成遇到问题，可以先完成数据层和网络层，播放器作为独立功能后续添加

---

## 五、关键检查点（20天业务优先模式）

### 检查点 1：UI层完成（Day 5）

**验收标准**：
- [ ] 核心页面可以正常显示（Feed、UserProfile、Settings、Landscape）
- [ ] 所有页面跳转可以正常工作
- [ ] 项目可以运行，无崩溃
- [ ] 可以看到完整的UI结构

### 检查点 2：ViewModel + Mock数据完成（Day 10）

**验收标准**：
- [ ] 核心ViewModel已实现（FeedViewModel、UserProfileViewModel）
- [ ] UI可以显示Mock数据
- [ ] UI交互可以更新状态（如点赞按钮状态变化）
- [ ] 数据流：UI → ViewModel → UIState → UI 正常工作

### 检查点 3：数据层对接完成（Day 15）

**验收标准**：
- [ ] 本地视频数据库可以正常读取数据
- [ ] 核心业务ViewModel使用真实Repository获取数据
- [ ] 核心业务UI可以显示从数据库读取的数据
- [ ] 核心业务数据流：UI → ViewModel → UseCase → Repository → LocalDataSource → Database 正常工作
- [ ] 核心业务依赖注入配置完成

### 检查点 4：网络层对接完成（Day 17）

**验收标准**：
- [ ] 核心业务RemoteDataSource可以正常调用API
- [ ] 核心业务Repository可以协调网络和本地数据
- [ ] 核心业务数据流：UI → ViewModel → UseCase → Repository → (RemoteDataSource/LocalDataSource) 正常工作

### 检查点 5：播放器集成完成（Day 19）

**验收标准**：
- [ ] FeedFragment可以播放视频
- [ ] 上下滑动可以切换视频
- [ ] 播放器生命周期管理正确（无内存泄漏）
- [ ] 预加载功能正常工作（基础版本即可）

### 检查点 6：项目完成（Day 20）

**验收标准**：
- [ ] 核心业务模块的 Repository、UseCase、ViewModel 已实现
- [ ] 所有页面的 Navigation 路由已配置
- [ ] 核心业务依赖注入配置完整
- [ ] 可以正常运行，无崩溃
- [ ] 核心业务数据流可以正常工作（UI → ViewModel → UseCase → Repository → DataSource）
- [ ] 播放器可以正常播放视频
- [ ] 核心功能可以演示

---

## 六、风险与注意事项（业务优先模式）

### 风险 1：UI层完成后，ViewModel对接困难

**风险**：如果UI层设计不合理，后续对接ViewModel时可能需要大量修改。

**应对**：
- UI层设计时考虑数据驱动的结构
- 使用占位数据验证UI布局的合理性
- ViewModel对接时，如果UI需要调整，及时沟通

### 风险 2：Mock数据与真实数据结构不一致

**风险**：Mock数据结构和真实数据结构不一致，导致后续对接时需要大量修改。

**应对**：
- Mock数据尽量参考真实数据结构
- 定义Domain Model时，考虑Mock数据的结构
- 如果结构不一致，及时调整Mock数据

### 风险 3：依赖注入配置错误

**风险**：DI 配置错误会导致运行时崩溃。

**应对**：
- 统一由成员C负责 DI 配置
- 使用 Hilt 的编译时检查
- 编写简单的集成测试验证 DI
- 在阶段5集中配置DI，避免过早配置导致问题

### 风险 4：播放器集成复杂度高

**风险**：播放器生命周期管理复杂，容易出现内存泄漏。

**应对**：
- 参考 ExoPlayer 官方文档和最佳实践
- 使用 Profiler 监控内存使用
- 及时释放播放器资源
- 播放器集成放在最后阶段，确保其他功能稳定后再集成

### 注意事项

1. **代码规范**：遵循 `.cursorrules` 中的代码规范
2. **文档更新**：完成每个阶段后，更新 `docs/development_plan.md`
3. **测试**：每个模块完成后，编写单元测试和集成测试
4. **性能监控**：使用 Profiler 监控内存、CPU、网络使用情况
5. **UI优先原则**：在UI层完成前，不要过早考虑数据层实现
6. **Mock数据管理**：Mock数据要统一管理，便于后续替换
7. **并行开发**：UI层可以完全并行开发，互不阻塞

---

## 七、下一步行动（20天业务优先模式）

1. **立即开始**：所有成员开始UI层开发（Day 1）
   - 成员A：完善FeedFragment + Navigation配置
   - 成员B：创建UserProfileFragment（SearchFragment简化）
   - 成员C：创建SettingsFragment + LandscapeActivity（AI相关延后）
2. **第1周目标（Day 1-5）**：所有核心页面可见，页面跳转正常工作
3. **每日同步**：每天结束时同步进度，解决阻塞问题（必须！）
4. **每周 Review**：每周结束时 Review 代码，确保质量
5. **阶段切换**：完成一个阶段后，再进入下一个阶段，不要跳跃开发
6. **核心优先**：始终优先完成核心功能（VideoFeed、User、播放器），非核心功能可以简化或延后
7. **风险预警**：如果Day 15前进度延迟超过1天，需要调整计划，优先保证核心功能

---

## 附录：相关文档

- 架构文档：`docs/architecture.md`
- 开发计划：`docs/development_plan.md`
- API 参考：`docs/api_reference.md`
- 重构方案：`docs/重构方案.md`
- 需求文档：`BeatUClient/docs/requirements.md`

---

**文档创建时间**：2025-01-27  
**最后更新时间**：2025-01-27  
**项目周期**：20个工作日（4周）  
**维护人**：团队全体成员

---

## 附录：20天开发计划快速参考

### 核心任务清单（必须完成）

**第1周（Day 1-5）**：
- ✅ FeedFragment UI完善
- ✅ UserProfileFragment创建
- ✅ SettingsFragment创建
- ✅ LandscapeActivity创建
- ✅ Navigation配置
- ✅ 页面跳转实现

**第2周（Day 6-10）**：
- ✅ FeedViewModel + UIState/UIEvent
- ✅ UserProfileViewModel + UIState/UIEvent
- ✅ UI数据绑定

**第3周（Day 11-15）**：
- ✅ 数据库初始化
- ✅ VideoFeed数据层（Domain Model、Repository、LocalDataSource、UseCase）
- ✅ User数据层（Domain Model、Repository、LocalDataSource、UseCase）
- ✅ ViewModel对接真实数据
- ✅ 核心业务依赖注入配置

**第4周（Day 16-20）**：
- ✅ 网络层API定义（核心业务）
- ✅ FeedRemoteDataSource
- ✅ Repository网络+本地策略
- ✅ 播放器集成
- ✅ 预加载逻辑
- ✅ 最终联调和测试

### 可选任务（时间允许时完成）

- Search业务完整实现
- Settings业务完整实现
- AI业务实现
- 非核心业务的网络层对接
- 高级播放器功能（倍速、清晰度切换等）

### 风险应对预案

**如果Day 15进度延迟**：
- 优先完成VideoFeed数据层
- User数据层可以简化
- Settings可以延后

**如果Day 18进度延迟**：
- 优先完成播放器集成
- 网络层可以只做接口定义，实际对接延后
- 非核心业务可以延后

**如果Day 20前仍有问题**：
- 优先保证核心功能可演示（Feed、播放器）
- 其他功能可以作为后续迭代

---

## 附录：开发模式对比

### 传统模式（抽象到具象）
1. 数据层基础设施
2. Domain层
3. Presentation层
4. UI层

**优点**：架构清晰，依赖关系明确  
**缺点**：前期看不到成果，容易阻塞

### 业务优先模式（具象到抽象）✅ 当前采用
1. UI层 + Navigation
2. ViewModel + Mock数据
3. 数据层对接
4. 网络层对接
5. 依赖注入
6. 播放器集成

**优点**：
- 快速看到成果，提升团队信心
- UI层可以完全并行开发，不阻塞
- 从用户可见的功能开始，符合人类思维习惯
- 每个阶段都有可运行的成果

**缺点**：
- 需要管理Mock数据
- 后续对接时需要确保结构一致性

**适用场景**：适合快速迭代、需要快速看到成果的项目

