## 开发计划（Development Plan 2026）

> 说明：仅包含 2026 年任务。
> 排序规则：按时间倒序维护（最近在前，越早的任务越靠后）；新增任务必须插入到对应分组顶部。

### 1. 当前迭代任务

梳理exoplayer复用的具体优化细节；梳理播放器的生命周期；
手写的抽象语法树解析，解析不出来的部分用默认文本渲染；


补充sse与websocket的区别，完善sse；添加ai流式搜索回答中途网络波动或者断网的处理方法；梳理ai流式搜索回答如果未渲染完，就点击可点击文本，会发生什么？ai流式搜索回答部分渲染方案；

客户端的登录（输入账号密码点击登录如果无对应账号自动注册）功能：不需要token，只需要一次登录，以后都用这个，除非自己注销

- [x] Android：单一接口登录/注册 + LoginFragment 为导航起点 + 动态 `X-User-Id` + 设置页退出  
  - 2026-03-21 - done by AI  
  - 后端：`POST /api/auth/login-or-register`，`beatu_user.passwordHash`（bcrypt）；脚本：`测试数据及脚本/beatu/add_beatu_user_password_hash.sql`  
  - 客户端：`LoginFragment` → `Feed`；`AuthSessionStore` 同步 DataStore `current_user_id`；`UserIdHeaderInterceptor`；设置「退出登录」  

- [x] 客户端：移除硬编码默认用户，统一会话用户（`CurrentUserProvider` / `AuthSessionHolder`）  
  - 2026-03-21 - done by AI  
  - 范围：`UserProfileFragment`（`newInstance` 未传参时不再写死用户名；关注/Tab/只读观察用 `currentUserProvider.userId()`）、`UserProfileNavigationHelper`（`currentViewerUserId` + `onUserWorkClicked`）、`NetworkLoggingInterceptor`（`/api/users` 调试日志按当前会话 `id` 切片）；此前已接：`VideoRepositoryImpl`、`UserWorksLocalDataSource`、`SearchFragment`、`AppStartupDataLoader`、`DataSyncService`、`UserProfileViewModel`  
  - 保留：`CurrentUserDefaults`（无会话兜底）、种子数据 `init_database.sql` 中的 `BEATU`、后端 `default_user_id` 配置  

- [x] AI 搜索：`sessionId` 单流隔离 + 块流组装 + 块级局部渲染  
  - 2026-03-23 - done by AI  
  - 范围：`AISearchViewModel` 引入 `renderSessionId` 与 `restartSearch(停旧流/清空/重置/新流)`，将 SSE 文本组装为 `blocks`（增量切块：`completedBlocks` + `tailBuffer` 按 `\n\n` 扫描，避免每次对全文 `split`）；`SearchResultFragment` 改为 `RecyclerView` 块列表局部更新，并将 AI 块适配器独立为 `AiAnswerBlockAdapter(ListAdapter + DiffUtil)`；`AiAnswerAstRenderer` 改为单块 markdown 渲染；关键词点击仅作用当前块并触发重搜四步流程；`AISearchRepository`/`AISearchApiService` 增加 `stopCurrentStream`。  

3.25，3.26实现：

前端实现管理员/用户的登录注册页面与功能

前端：用户的视频上传功能，用户在前端的个人页面功能；



- [x] 搜索链接直达改为“内部路由到 Feed + videoId”  
  - 2026-03-08 - done by AI  
  - 需求：修复搜索框输入链接后点击搜索出现应用异常切换问题，移除 `ACTION_VIEW` 二次启动路径，改为应用内导航。  
  - 改动范围：  
    1. 更新：`BeatUClient/business/search/presentation/src/main/java/com/ucw/beatu/business/search/presentation/ui/SearchFragment.kt`（命中链接后解析 `videoId` 写入导航 `savedStateHandle`，直接走 `action_search_to_feed`）  
    2. 更新：`BeatUClient/business/videofeed/presentation/src/main/java/com/ucw/beatu/business/videofeed/presentation/ui/FeedFragment.kt`（监听 `savedStateHandle` 的目标 `videoId`，收到后调用 `openVideoById`）  
    3. 更新：`BeatUClient/shared/common/src/main/java/com/ucw/beatu/shared/common/navigation/NavigationIds.kt`（新增跨模块导航参数 key 常量）  
    4. 更新：`BeatUClient/business/search/presentation/src/main/java/com/ucw/beatu/business/search/presentation/ui/SearchFragment.kt`（增强回流可靠性：优先写 `previousBackStackEntry(feed)`，并优先 `popBackStack(feed,false)` 回退）  
  - 结果：搜索页链接命中后不再发 `Intent`，改为内部回到 Feed 并定位目标视频播放。  

- [x] 互动失败回滚补齐用户可见提示（竖屏）  
  - 2026-03-08 - done by AI  
  - 需求：点赞/收藏异步同步失败后，除状态回滚外，需明确给用户提示，避免“已回滚但无感知”。  
  - 改动范围：  
    1. 更新：`BeatUClient/business/videofeed/presentation/src/main/java/com/ucw/beatu/business/videofeed/presentation/ui/VideoItemFragment.kt`（监听 `interactionError`，弹 `Toast` 后调用 `consumeInteractionError()` 清空）  
  - 结果：竖屏视频页在点赞/收藏失败并回滚时可即时提示用户；评论失败提示链路保持不变。  

- [x] DeepLink 在 App 内统一回流主页并定位目标视频播放  
  - 2026-03-08 - done by AI  
  - 需求：支持 `beatu://video/{id}` 与 `https://beatu.app/video/{id}` 在应用内解析后，统一回到主页 Feed 并打开对应视频播放，避免停留在搜索/个人页。  
  - 改动范围：  
    1. 更新：`BeatUClient/app/src/main/java/com/ucw/beatu/MainActivity.kt`（DeepLink 待处理视频在 `feed` 可见后再分发；非 `feed` 目标时先导航回 `feed` 再执行 `openVideoById`）  
    2. 更新：`BeatUClient/business/search/presentation/src/main/java/com/ucw/beatu/business/search/presentation/ui/SearchFragment.kt`（`ACTION_VIEW` intent 限制到当前包名，确保链接在本 App 内处理）  
  - 结果：应用内触发 DeepLink 时会先回到主页，再切到推荐流并定位到目标视频播放；`beatu://` 与 `https://beatu.app/video/...` 行为保持一致。  

- [x] 搜索框支持“链接识别并直达视频”  
  - 2026-03-08 - done by AI  
  - 需求：用户在搜索框输入或粘贴分享链接后，按搜索键可直接跳转到目标视频；普通关键词搜索流程保持不变。  
  - 改动范围：  
    1. 更新：`BeatUClient/business/search/presentation/src/main/java/com/ucw/beatu/business/search/presentation/ui/SearchFragment.kt`（`performSearch` 增加链接优先分支：整串解析 + 文本中提取 `beatu://...` / `https://beatu.app/video...`；命中后 `ACTION_VIEW` 跳转，失败回退普通搜索）  
  - 结果：纯链接与混合文本（含链接）可直达对应视频；无效链接或普通关键词继续走原搜索结果列表。  

- [x] AI 搜索结果页优化关键词展示与交互输入联动  
  - 2026-03-08 - done by AI  
  - 需求：解决“AI 回答后重复列举关键词影响阅读”与“点击关键词后二次搜索未回填搜索框”的体验问题。  
  - 改动范围：  
    1. 更新：`BeatUClient/business/search/presentation/src/main/java/com/ucw/beatu/business/search/presentation/ui/SearchResultFragment.kt`（渲染前过滤“关键词”段落；关键词点击时先回填搜索框再触发搜索）  
  - 结果：AI 回答区域不再显示尾部关键词列举，保留关键词点击搜索能力且交互与搜索框状态一致。  

- [x] 分享链接可点击并可回流 App 指定视频  
  - 2026-03-08 - done by AI  
  - 需求：修复“分享 DeepLink 文本在聊天场景不可点击”的问题，同时保持点击后可直达 App 对应视频。  
  - 改动范围：  
    1. 更新：`BeatUClient/shared/common/src/main/java/com/ucw/beatu/shared/common/navigation/VideoShareDeepLink.kt`（默认构造可点击 `https://beatu.app/video/{id}`；兼容解析旧 `beatu://video/{id}`）  
    2. 更新：`BeatUClient/app/src/main/AndroidManifest.xml`（为 `MainActivity` 增加 `https://beatu.app/video...` 的 `intent-filter`）  
    3. 更新：`BeatUClient/business/videofeed/presentation/src/main/java/com/ucw/beatu/business/videofeed/presentation/ui/VideoItemFragment.kt`（分享文本改为可点击 URL）  
  - 结果：分享文案在常见 IM 中可被识别为可点击链接，点击后仍可由 `MainActivity` 解析并跳转到目标视频。  

- [x] 用户主页与作品页补齐“本地优先 + 远端刷新 + 互动态透传”  
  - 2026-03-08 - done by AI  
  - 需求：修复“从视频头像进入个人页数据不更新、从个人页点进视频互动态丢失”的问题，避免页面只消费本地快照。  
  - 改动范围：  
    1. 更新：`BeatUClient/business/user/presentation/src/main/java/com/ucw/beatu/business/user/presentation/viewmodel/UserProfileViewModel.kt`（设置用户快照后立即订阅本地流并触发远端刷新回写；`loadUser` 本地命中后也会主动刷新远端）  
    2. 更新：`BeatUClient/shared/database/src/main/java/com/ucw/beatu/shared/database/dao/VideoInteractionDao.kt`（新增按用户观察互动记录流）  
    3. 更新：`BeatUClient/business/user/data/src/main/java/com/ucw/beatu/business/user/data/local/UserWorksLocalDataSource.kt`（将作品列表与互动表实时合并，给作品补齐 `isLiked/isFavorited`）  
    4. 更新：`BeatUClient/business/user/domain/src/main/java/com/ucw/beatu/business/user/domain/model/UserWork.kt`、`.../data/mapper/UserWorkMapper.kt`、`.../presentation/ui/helper/UserProfileNavigationHelper.kt`（补齐作者信息与互动态，透传到 `VideoItem`）  
  - 结果：头像进入个人主页后会自动远端刷新并回写本地；从个人主页进入视频时点赞/收藏状态与本地互动记录一致。  

- [x] AI 搜索关键词收敛为 markdown 单通道并限制上限  
  - 2026-03-08 - done by AI  
  - 需求：解决 AI 搜索关键词过多、噪声词累积问题，关键词仅从云 LLM markdown 中 `**关键词**` 解析，统一最多 5 个。  
  - 改动范围：  
    1. 更新：`BeatUClient/business/search/data/src/main/java/com/ucw/beatu/business/search/data/repository/AISearchRepositoryImpl.kt`（`answer` 分片不再本地 NLP 抽词，仅透传回答）  
    2. 更新：`BeatUClient/business/search/presentation/src/main/java/com/ucw/beatu/business/search/presentation/viewmodel/AISearchViewModel.kt`（仅从 `**...**` 解析关键词，`normalize + distinct + take(5)`）  
  - 结果：关键词来源收敛为单通道，数量稳定在 5 个以内，首关键词驱动搜索逻辑保持不变且更稳定。  

- [x] AI 搜索接口切换为云 LLM SSE 流式 markdown 输出  
  - 2026-03-06 - done by AI  
  - 需求：将 `/api/ai/search/stream` 从本地规则回答切换为云 LLM 生成，输出可直接被客户端 AST 渲染的 markdown（标题 + 关键词高亮），保持 SSE 协议不变。  
  - 改动范围：  
    1. 更新：`BeatUBackend/services/ai_search_service.py`（接入云 LLM `astream`，输出 `chunkType=answer` SSE 数据块）  
    2. 更新：`BeatUBackend/routes/ai.py`（接口注释改为云 LLM 流式 markdown）  
    3. 更新：`docs/api_reference.md`、`docs/architecture.md`（补齐接口与链路说明）  
  - 结果：客户端无需改 SSE 解析协议，即可接收云 LLM 流式 markdown 文本并实时渲染。  

- [x] 点赞/收藏/评论乐观更新补齐“异步失败回滚提示”闭环  
  - 2026-03-06 - done by AI  
  - 需求：将“本地先更新、远程异步同步、失败后可见回滚提示”在点赞/收藏/评论链路中真实落地，避免仅日志回滚导致用户无感知。  
  - 改动范围：  
    1. 新增：`BeatUClient/business/videofeed/domain/model/InteractionSyncEvent.kt`（定义互动同步失败事件）  
    2. 新增：`BeatUClient/business/videofeed/domain/usecase/ObserveInteractionSyncEventsUseCase.kt`（暴露事件流）  
    3. 更新：`BeatUClient/business/videofeed/domain/repository/VideoRepository.kt`、`.../data/repository/VideoRepositoryImpl.kt`（在异步同步失败后发出回滚事件）  
    4. 更新：`BeatUClient/.../videofeed/presentation/viewmodel/VideoItemViewModel.kt`、`.../videofeed/presentation/ui/VideoItemFragment.kt`（竖屏点赞/收藏失败时 UI 回滚并提示）  
    5. 更新：`BeatUClient/.../videofeed/presentation/ui/VideoCommentsDialogFragment.kt`（评论异步失败时移除临时评论并 Toast 提示）  
    6. 更新：`BeatUClient/.../landscape/presentation/viewmodel/LandscapeVideoItemViewModel.kt`、`.../landscape/presentation/ui/LandscapeVideoItemFragment.kt`（横屏点赞/收藏失败时 UI 回滚并提示）  
  - 结果：互动链路满足“乐观更新 + 异步同步 + 失败回滚提示”闭环，用户可感知失败并看到状态回滚。  

- [x] 分享链接支持 Deep Link 回流到指定视频播放  
  - 2026-03-06 - done by AI  
  - 需求：分享内容改为“标题 + 可点击链接”，用户点击链接后可拉起 App，并自动进入对应视频播放。  
  - 改动范围：  
    1. 新增：`BeatUClient/shared/common/src/main/java/com/ucw/beatu/shared/common/navigation/VideoShareDeepLink.kt`（统一构建/解析 `beatu://video/{id}`）  
    2. 更新：`BeatUClient/business/videofeed/presentation/src/main/java/com/ucw/beatu/business/videofeed/presentation/ui/VideoItemFragment.kt`（分享文案与海报二维码链接改为 Deep Link）  
    3. 更新：`BeatUClient/business/landscape/presentation/src/main/java/com/ucw/beatu/business/landscape/presentation/ui/LandscapeVideoItemFragment.kt`（横屏分享链路对齐）  
    4. 更新：`BeatUClient/app/src/main/AndroidManifest.xml`、`app/src/main/java/com/ucw/beatu/MainActivity.kt`、`FeedFragment.kt`、`RecommendFragment.kt`、`FeedFragmentCallback.kt`、`VideoFeedAdapter.kt`、`RecommendViewModel.kt`（注册 Deep Link、解析 Intent、按 `videoId` 定位并自动播放）  
  - 结果：分享链接可直接唤起 App 并定位到目标视频；若列表未命中，会拉取视频详情后置顶播放，提升分享回流命中率。  

- [x] AI 流式搜索链路完整接入页面（OkHttp + SSE + StateFlow + AST 渲染）
  - 2026-03-02 - done by AI  
  - 需求：将 AI 流式搜索从网络到页面展示全链路打通，接入 AST 富文本渲染（标题、关键词高亮），并修复前后端 SSE 数据块字段不一致导致的视频结果丢失问题。
  - 改动范围：  
    1. 更新：`BeatUClient/business/search/presentation/src/main/java/com/ucw/beatu/business/search/presentation/ui/SearchResultFragment.kt`（接入 `AiAnswerAstRenderer`、关键词高亮点击、关键词驱动结果收敛）  
    2. 更新：`BeatUClient/business/search/data/src/main/java/com/ucw/beatu/business/search/data/repository/AISearchRepositoryImpl.kt`（兼容 `videos` 与 `videoIds` 数据块）  
    3. 更新：`BeatUBackend/services/ai_search_service.py`（统一下发 `videoIds` 数据块）  
    4. 更新：`BeatUBackend/routes/ai.py`（接口注释与数据块语义同步）  
  - 结果：搜索结果页支持 AI 分段流式富文本实时展示，关键词高亮可直接触发二次搜索；前后端 SSE 数据块协议对齐，视频 ID 结果可稳定进入内容消费链路。  

- [x] 客户端视频渲染控件统一为 `SurfaceView`  
  - 2026-03-02 - done by AI  
  - 需求：仅调整视频渲染相关控件，将客户端播放器渲染类型从 `TextureView` 切换为 `SurfaceView`，避免误改文本控件。  
  - 改动范围：  
    1. 更新：`BeatUClient/business/videofeed/presentation/src/main/res/layout/item_video.xml`（`app:surface_type` 改为 `surface_view`）  
    2. 更新：`BeatUClient/business/landscape/presentation/src/main/res/layout/fragment_landscape_video_item.xml`（`app:surface_type` 改为 `surface_view`）  
  - 结果：竖屏与横屏视频播放页渲染类型统一为 `SurfaceView`，视频渲染链路配置收敛。  

- [x] 第三轮 Backend 方法级 API 收敛（Service 内部定义-调用矩阵）  
  - 2026-02-28 - done by AI  
  - 需求：将 `BeatUBackend/services` 从“文件级收敛”推进到“方法级收敛”，按定义-调用矩阵识别可删/可合并/可重命名项，并直接落地。  
  - 清单与处理：  
    1. **可删**：删除无调用 `AISearchService.close()`；删除已无引用的 `services/helpers.py`；删除 `routes/mcp.py` 中无使用 `MCPResponse`。  
    2. **可合并**：移除仅透传且无业务作用的参数（`CommentService.create_comment(..., user_name)` 与路由对应依赖）；移除未使用 import。  
    3. **可重命名**：  
       - `UserService`: `get_user/get_user_by_name/get_all_users/get_all_user_follows` -> `fetch_user/fetch_user_by_name/list_users/list_user_follows`  
       - `VideoService`: `get_all_video_interactions/get_all_watch_histories/sync_watch_histories` -> `list_video_interactions/list_watch_histories/sync_watch_history`  
       - `AIService`: `comment_qa` -> `answer_comment`  
  - 改动范围：  
    1. 更新：`BeatUBackend/services/user_service.py`、`video_service.py`、`comment_service.py`、`ai_service.py`、`ai_search_service.py`  
    2. 更新：`BeatUBackend/routes/users.py`、`videos.py`、`ai.py`、`mcp.py`  
    3. 删除：`BeatUBackend/services/helpers.py`  
  - 结果：后端 Service API 语义更短、更聚焦业务动作；方法级调用关系清晰化；`python -m compileall services routes` 通过。

- [x] 第二轮全项目 API 收敛（Service/Helper/Utils 定义-调用矩阵）  
  - 2026-02-28 - done by AI  
  - 需求：按 `*Service/*Helper/*Utils` 再次全仓扫描定义-调用关系，继续删除未使用 API，并把保留 API 命名收敛为更短业务语义。  
  - 方案：  
    1. `BeatUClient`：基于定义清单逐个检索调用方，删除“仅定义无调用”的重复/僵尸 API；  
    2. `BeatUClient`：将通用导航与存储提醒 API 改为短命名（`navigateByStringId` -> `navigate`，`maybeNotify` -> `notifyIfLow`）并全量替换调用；  
    3. `BeatUBackend/BeatUWeb`：复核 service/helper/utils 调用矩阵，仅删除高置信无引用项。  
  - 改动范围：  
    1. 删除：`shared/common/share/ShareImageUtils.kt`（重复且无调用）  
    2. 删除：`shared/common/mock/MockDataUtils.kt`（无调用）  
    3. 更新：`NavigationHelper.kt`、`MainActivity.kt`、`SearchFragment.kt`、`RecommendFragment.kt`、`StorageLowNotifyHelper.kt`、`StorageLowReceiver.kt`、`PlayerSessionUtils.kt`  
  - 结果：`Service/Helper/Utils` API 面进一步收敛，命名更短且语义更聚焦；受影响模块编译通过，`app` 编译失败点仍为既有问题（AI/Auth/Publish 相关）。

- [x] 全项目 API 收敛与短命名统一（Client/Backend/Web）  
  - 2026-02-28 - done by AI  
  - 需求：对整个项目继续排查“只保留实际使用 API”，并将核心调用命名统一为更短、更业务化，不局限于线程清理。  
  - 方案：  
    1. `BeatUClient`：将线程任务入口从 `ThreadTaskManager` + `ThreadObservableTaskNames` 统一收敛为 `TaskRunner` + `TaskNames`，并删除无引用实现；  
    2. `BeatUWeb`：删除未被页面调用的 API 导出，保留并统一 `listUsers`、`listVideos` 等业务语义命名；  
    3. `BeatUBackend`：删除未被任何路由/服务引用的 `storage_service.py`。  
  - 改动范围：  
    1. 新增：`BeatUClient/shared/common/src/main/java/com/ucw/beatu/shared/common/threading/TaskRunner.kt`  
    2. 新增：`BeatUClient/shared/common/src/main/java/com/ucw/beatu/shared/common/threading/TaskNames.kt`  
    3. 删除：`ThreadTaskManager.kt`、`ThreadObservableTaskNames.kt`、`services/storage_service.py`  
    4. 更新：`AuthActivity`、`PublishVideoActivity`、`FollowViewModel`、`VideoLandscapeCoordinator`、`VideoUserInfoOverlayCoordinator`、`PlayerSessionUtils` 及 `BeatUWeb/src/api/*.js`、对应管理页引用。  
  - 结果：跨仓 API 面收敛，命名更短更聚焦业务动作；受影响客户端模块编译通过，后端 Python 语法编译通过。

- [x] 协程命名规则全客户端代码落地与 `launchNamed` 简化  
  - 2026-02-28 - done by AI  
  - 需求：将“协程优先 + 命名可追踪”规则落地到 `BeatUClient` 全模块，并解决 `com.ucw.beatu.shared.common.threading.launchNamed(...)` 调用过长影响可读性的问题。  
  - 方案：对客户端所有使用 `launchNamed` 的文件统一改为 import 后短写调用；并在规范文档中补充“action 必须为业务动作语义、禁止 `Class_launch` 弱语义命名”的规则。  
  - 改动范围：
    1. 更新：`BeatUClient/business/*/{ui,viewmodel}/*.kt`（统一 `launchNamed` 调用写法）  
    2. 更新：`docs/coroutine_naming_guidelines.md`（补充短写调用与 action 命名约束）  
    3. 更新：`docs/architecture.md`（补充协程命名约束入口）  
  - 结果：协程调用可读性提升，命名规范从“文档约束”推进到“代码层统一风格”，便于 code review 与并发链路排障。

- [x] 协程命名规范全客户端落地  
  - 2026-02-28 - done by AI  
  - 需求：统一 `BeatUClient` 全量模块协程相关命名，解决并发链路中命名风格不一致、排障时难以快速定位协程任务语义的问题，形成可执行、可评审、可迁移的规范文档。  
  - 方案：新增独立文档 `docs/coroutine_naming_guidelines.md`，明确协程优先的命名目标与原则，细化 `suspend`/`Flow`/`StateFlow`/`SharedFlow`/`Job`/`Dispatcher` 命名规则，统一 `CoroutineName` 格式为 `feature_action`，并给出 `launch`/`launchIn`/`async`/`withContext` 推荐写法、反例与分阶段迁移策略。  
  - 改动范围：
    1. 新增：`docs/coroutine_naming_guidelines.md`（规范正文与示例）
    2. 更新：`docs/development_plan.md`（登记任务闭环）
    3. 更新：`docs/architecture.md`（补充协程约定入口说明）
  - 结果：客户端后续协程代码具备统一命名基线，关键并发任务可通过 `CoroutineName(feature_action)` 快速定位；评审环节可按命名清单执行一致性检查，降低维护与排障成本。
