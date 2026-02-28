## 开发计划（Development Plan 2026）

> 说明：仅包含 2026 年任务。
> 排序规则：按时间倒序维护（最近在前，越早的任务越靠后）；新增任务必须插入到对应分组顶部。

### 1. 当前迭代任务

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
