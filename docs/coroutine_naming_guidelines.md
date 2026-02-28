# 客户端协程命名规范

> 适用范围：`BeatUClient` 全量 Kotlin 模块（`app/`、`business/*`、`shared/*`）。
>
> 目标：统一协程命名语义，降低并发代码的理解与排障成本，优先体现“这个协程在做什么、在哪个业务域执行、生命周期如何管理”。

## 1. 命名目标与原则（协程优先）

- **协程优先**：命名首先表达协程任务语义，而不是线程细节（如 Thread/Handler）。
- **业务优先**：变量名体现业务 Feature + Action（例如 feed、search、profile）。
- **生命周期可读**：名字能看出是一次性任务、持续流、还是可取消任务。
- **一致性优先**：同类对象统一后缀（`Flow`、`State`、`Events`、`Job`、`Dispatcher`）。
- **避免歧义**：不使用 `task1`、`jobA`、`tempFlow`、`dataFlow2` 这类弱语义命名。

## 2. suspend / Flow / StateFlow / SharedFlow / Job / Dispatcher 命名规则

### 2.1 suspend 函数

- 使用动词开头，明确行为：`load`、`fetch`、`save`、`update`、`sync`、`observe`。
- 与业务域组合：`fetchFeedPage()`、`syncWatchHistory()`、`updateUserProfile()`。
- 有副作用时使用动作动词：`persistPlaybackSession()`、`reportPlaybackMetric()`。
- 查询类尽量用 `get/query/load/fetch`，变更类尽量用 `create/update/delete/toggle`。

### 2.2 Flow

- 持续数据流统一后缀 `Flow`：`feedItemsFlow`、`networkStateFlow`。
- 来源清晰：`localWatchHistoryFlow`、`remoteFeedFlow`、`mergedFeedFlow`。
- 转换结果流体现语义：`filteredLandscapeVideosFlow`、`debouncedQueryFlow`。

### 2.3 StateFlow

- 私有可变状态统一 `_xxxState`，公开只读状态统一 `xxxState`。
- UI 状态建议命名为 `uiState` / `screenState`，避免 `stateFlow` 冗余。
- 示例：
  - `private val _uiState = MutableStateFlow(SearchUiState())`
  - `val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()`

### 2.4 SharedFlow

- 事件流统一后缀 `Events` 或 `EventFlow`，推荐优先 `Events`。
- 私有可变事件流统一 `_xxxEvents`，公开只读 `xxxEvents`。
- 示例：`_navigationEvents`、`navigationEvents`、`playerErrorEvents`。

### 2.5 Job

- 可取消任务统一后缀 `Job`。
- 命名体现“任务 + 场景”：`loadMoreJob`、`refreshFeedJob`、`syncHistoryJob`。
- 同类任务只保留一个活跃实例时，必须明确其唯一性语义（如 `refreshJob` + cancel before launch）。

### 2.6 Dispatcher

- `CoroutineDispatcher` 变量统一后缀 `Dispatcher`。
- 命名与用途对应：`ioDispatcher`、`mainDispatcher`、`defaultDispatcher`。
- 若通过 Provider 注入，优先统一入口：`dispatcherProvider.io`、`dispatcherProvider.main`。

## 3. CoroutineName 命名格式

统一格式：

- `feature_action`
- 可选扩展：`feature_action_stage`（仅在需要区分阶段时使用）

示例：

- `videofeed_load_page`
- `videofeed_refresh_first_page`
- `search_sync_history`
- `user_update_profile`
- `landscape_restore_session`
- `ai_stream_answer_chunk`

推荐写法：

```kotlin
viewModelScope.launch(
    context = CoroutineName("videofeed_refresh_first_page")
) {
    refreshFeed()
}
```

## 4. launch / launchIn / async / withContext 推荐写法

### 4.1 launch

- 用于“只关心完成副作用，不直接返回值”的任务。
- 建议显式补充 `CoroutineName`，复杂任务建议带上 dispatcher。
- 客户端统一使用 `launchNamed`（`shared/common/threading/CoroutineNaming.kt`）包装命名，避免手写全限定名。
- 调用优先使用短写法（通过 import）：`launchNamed(viewModelScope, "feature", "action") { ... }` 或 `viewModelScope.launchNamed("feature", "action") { ... }`。
- `action` 禁止使用泛化后缀（如 `Class_launch`），应直接描述业务动作（如 `load_page`、`observe_ui_state`、`sync_history`）。

```kotlin
viewModelScope.launch(ioDispatcher + CoroutineName("search_sync_history")) {
    syncSearchHistory()
}
```

### 4.2 launchIn

- 用于 `Flow` 链路终端收集，命名在 `onEach` 前后保持业务语义。
- 推荐保存返回的 `Job` 以便按生命周期取消。

```kotlin
observePlaybackState()
    .onEach { state -> renderPlaybackState(state) }
    .launchIn(viewModelScope + CoroutineName("videofeed_observe_playback_state"))
```

### 4.3 async

- 仅用于“并行且需要返回值”的场景；禁止为省事替代 `launch`。
- `Deferred` 命名使用 `xxxDeferred`，并在同一作用域 `await()`。

```kotlin
coroutineScope {
    val feedDeferred = async(ioDispatcher + CoroutineName("videofeed_fetch_page")) {
        fetchFeedPage()
    }
    val profileDeferred = async(ioDispatcher + CoroutineName("user_fetch_profile")) {
        fetchUserProfile()
    }
    combine(feedDeferred.await(), profileDeferred.await())
}
```

### 4.4 withContext

- 用于切换上下文执行明确的一段阻塞或 IO 逻辑。
- 不要把整段业务函数都包在 `withContext`，只包必要片段。

```kotlin
suspend fun saveDraft(content: String) {
    withContext(ioDispatcher + CoroutineName("publish_save_draft")) {
        draftRepository.save(content)
    }
}
```

## 5. 反例与迁移建议

### 5.1 常见反例

- `val flow = MutableStateFlow(...)`（语义过弱，无法表达业务）
- `launch { ... }`（缺少 `CoroutineName`，排障日志不可读）
- `val job1: Job? = null`、`val tempJob`（弱语义）
- `async` 执行副作用但不 `await`（语义和结构不一致）
- `withContext(Dispatchers.IO)` 包裹整个 ViewModel 主流程（可读性差且易误用）

### 5.2 迁移建议（全客户端分阶段）

1. **增量优先**：新增代码必须按本规范命名，不再引入旧风格。
2. **高频路径先改**：优先改 `videofeed`、`landscape`、`search` 的播放与交互链路。
3. **统一替换模式**：
   - `_stateFlow` -> `_uiState`
   - `eventsFlow` -> `events`
   - `job` -> `<action>Job`
4. **CoroutineName 补齐**：对关键 `launch/async/launchIn/withContext` 增加 `CoroutineName("feature_action")`。
5. **代码评审卡点**：PR 检查命名后缀与协程语义是否一致；不符合项在评审阶段直接拦截。

## 6. 快速检查清单（Review Checklist）

- 是否能从命名直接看出业务域与动作？
- Flow/StateFlow/SharedFlow/Job/Dispatcher 后缀是否统一？
- 关键协程入口是否带 `CoroutineName("feature_action")`？
- 是否存在 `async` 不 `await`、`withContext` 过度包裹等误用？
- 新增命名是否与现有模块命名风格一致？

