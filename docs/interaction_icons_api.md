## 竖屏 / 横屏交互图标接口对接规范

> 目的：为后续接入真实后端或 UseCase 提供统一的交互状态流与接口契约，确保竖屏 `VideoControlsView` 与横屏 `LandscapeVideoItemFragment` 在点赞/收藏等操作上共享同一条业务链路，可快速从当前本地逻辑切换到线上实现。

### 1. 总体架构

```
用户点击图标
      ↓
UI 控件（VideoControlsView / Landscape 控制面板）
      ↓ 监听接口
Fragment（VideoItemFragment / LandscapeVideoItemFragment）
      ↓ 调用
ViewModel（VideoItemViewModel / LandscapeVideoItemViewModel）
      ↓ （未来：UseCase -> Repository -> API）
```

- **竖屏 UI**：`shared/designsystem/widget/VideoControlsView` 暴露 `VideoControlsListener`，所有交互事件（播放、点赞、收藏、评论、分享、Seek）都通过回调交给 `VideoItemFragment`。
- **横屏 UI**：直接在 `LandscapeVideoItemFragment` 中绑定控件事件（`btn_like`、`btn_favorite` 等）并调用对应 ViewModel 方法。
- **状态**：两套 ViewModel 都持有 `StateFlow`，字段均包含 `isLiked` / `isFavorited` / 计数；UI 只关心状态，不直接访问数据层。

### 2. 竖屏交互事件映射

| UI 组件 | 触发回调 | Fragment 调用 | ViewModel 方法 | 未来 UseCase | API 契约 |
| --- | --- | --- | --- | --- | --- |
| `VideoControlsView.iv_like` | `onLikeClicked()` | `viewModel.toggleLike()` | `VideoItemViewModel.toggleLike()` | `LikeVideoUseCase` / `UnlikeVideoUseCase` | `POST /api/v1/interaction/like { videoId, action }` |
| `VideoControlsView.iv_favorite` | `onFavoriteClicked()` | `viewModel.toggleFavorite()` | `VideoItemViewModel.toggleFavorite()` | `FavoriteVideoUseCase` / `UnfavoriteVideoUseCase` | `POST /api/v1/interaction/favorite` |
| `VideoControlsView.iv_comment` | `onCommentClicked()` | TODO：评论弹层 | `CommentViewModel`（待接入） | `GetCommentsUseCase` / `PostCommentUseCase` | `/api/v1/comment/list` / `/api/v1/comment/create` |
| `VideoControlsView.iv_share` | `onShareClicked()` | TODO：分享面板 | `ShareController`（待定） | - | - |

#### 状态字段

```146:155:BeatUClient/business/videofeed/presentation/src/main/java/com/ucw/beatu/business/videofeed/presentation/ui/VideoItemFragment.kt
controlsView?.state = VideoControlsView.VideoControlsState(
    isPlaying = state.isPlaying,
    ...,
    isLiked = state.isLiked,
    isFavorited = state.isFavorited,
    likeCount = state.likeCount,
    favoriteCount = state.favoriteCount
)
```

- **本地占位实现**：`VideoItemViewModel.toggleLike()` / `toggleFavorite()` 当前只在内存里翻转状态，无网络依赖。切换到真实业务时，只需恢复 UseCase 注入并在乐观更新后调用接口即可。
- **推荐做法**：保留乐观更新 + 失败回滚，错误通过 `_uiState.error` 通知 UI；对 `ApiResponse` 的解析可在 `shared/network` 中实现自定义 `Converter.Factory`。

### 3. 横屏交互事件映射

| UI 组件 | 绑定控件 | Fragment 调用 | ViewModel 方法 | 未来 UseCase | API 契约 |
| --- | --- | --- | --- | --- | --- |
| 左下角点赞 `btn_like` | `ImageButton` | `toggleLike()` | `LandscapeVideoItemViewModel.toggleLike()` | 同 `LikeVideoUseCase` | `POST /api/v1/interaction/like` |
| 收藏 `btn_favorite` | `ImageButton` | `toggleFavorite()` | `LandscapeVideoItemViewModel.toggleFavorite()` | `FavoriteVideoUseCase` | `POST /api/v1/interaction/favorite` |
| 评论 `btn_comment` | `ImageButton` | 预留日志 | 评论弹层（待实现） | `GetCommentsUseCase`/`PostCommentUseCase` | `/api/v1/comment/*` |
| 分享 `btn_share` | `ImageButton` | 预留日志 | 分享面板 | - | - |
| 右侧亮度/音量/锁屏 | `btn_brightness` 等 | Fragment 直接控制 | `LandscapeVideoItemViewModel` | - | - |

#### 亮度长按手势（2025-12-02）

- **交互说明**：长按右侧亮度按钮 (`btn_brightness`) 进入调节模式，保持按压状态时上下滑动即可增减 `WindowManager.LayoutParams.screenBrightness`，范围 `0.08f~1.0f`。放开按钮或滑出其区域即退出。
- **可视化**：`fragment_landscape_video_item.xml` 中新增椭圆形 `brightness_indicator` 与百分比文本，实时展示亮度百分比，并以渐变填充表示相对值。
- **实现要点**：按钮通过 `setOnTouchListener` 拦截事件，触发长按后调用 `LandscapeFragment.setPagingEnabled(false)` 与 `requestDisallowInterceptTouchEvent(true)` 暂停 ViewPager2 滑动。调节结束再恢复。
- **已知问题**：在部分机型上，上下拖动仍可能触发 ViewPager2 纵向滑动（导致视频换页）。当前版本已通过 `isUserInputEnabled=false` 与拦截请求尽量规避，但该 bug 仍待彻底修复，需在后续版本继续排查输入冲突。

#### 横屏 UI 状态渲染

```642:660:BeatUClient/business/landscape/presentation/src/main/java/com/ucw/beatu/business/landscape/presentation/ui/LandscapeVideoItemFragment.kt
likeButton?.apply {
    setImageResource(DesignSystemR.drawable.ic_like)
    imageTintList = ColorStateList.valueOf(
        if (state.isLiked) LIKE_ACTIVE_COLOR else ICON_INACTIVE_COLOR
    )
}
favoriteButton?.apply {
    setImageResource(DesignSystemR.drawable.ic_favorite)
    imageTintList = ColorStateList.valueOf(
        if (state.isFavorited) FAVORITE_ACTIVE_COLOR else ICON_INACTIVE_COLOR
    )
}
```

- `LandscapeVideoItemViewModel.toggleLike()` 与 `toggleFavorite()` 同样仅更新 `_controlsState`。接入业务层时流程与竖屏保持一致：Fragment 调 ViewModel → ViewModel 调 UseCase → Repository → API。

### 4. 推荐的 UseCase / Repository 契约

| UseCase | 参数 | 说明 |
| --- | --- | --- |
| `LikeVideoUseCase(videoId: String)` | `videoId` | 点赞视频；`action=LIKE` |
| `UnlikeVideoUseCase(videoId: String)` | `videoId` | 取消点赞；`action=UNLIKE` |
| `FavoriteVideoUseCase(videoId: String)` | `videoId` | 收藏视频 |
| `UnfavoriteVideoUseCase(videoId: String)` | `videoId` | 取消收藏 |
| `GetCommentsUseCase(videoId: String)` | `cursor`, `pageSize` | 评论列表（竖横共用） |
| `PostCommentUseCase(videoId: String, content: String)` | - | 评论提交 |

Repository 层接口示例：

```kotlin
interface InteractionRepository {
    suspend fun likeVideo(videoId: String): AppResult<Unit>
    suspend fun unlikeVideo(videoId: String): AppResult<Unit>
    suspend fun favoriteVideo(videoId: String): AppResult<Unit>
    suspend fun unfavoriteVideo(videoId: String): AppResult<Unit>
}
```

### 5. API 契约（与 `docs/api_reference.md` 保持一致）

| Method | Path | Body | 成功返回 | 失败策略 |
| --- | --- | --- | --- | --- |
| POST | `/api/v1/interaction/like` | `{ "videoId": "xxx", "action": "LIKE" }` | `{ "code":0,"message":"OK" }` | 失败时回滚乐观状态，提示“点赞失败，可稍后重试” |
| POST | `/api/v1/interaction/favorite` | `{ "videoId": "xxx", "action": "SAVE" }` | 同上 | 失败回滚收藏状态 |

- 若服务端返回 `code=3001`（AI/互动服务不可用），客户端可保留本地状态并待网络恢复后批量同步。
- `ApiResponse<T>` 推荐结构：`{ "code": Int, "message": String, "data": T }`。为避免再度出现 “Unable to create converter for ApiResponse<Unit)” 的问题，应实现适配 `Unit` 的 `Converter`，或让接口返回 `ApiResponse<ApiSuccess>`。

### 6. 本地降级策略

1. **竖屏**：当前实现已经是纯本地翻转，可在 UseCase 报错时回退到旧状态；离线模式可继续沿用本地状态。
2. **横屏**：同竖屏；若需要离线缓存，可借助 `LandscapeControlsState` 与 `PlaybackSessionStore` 在 `landscape` 模块保存最近的互动状态。
3. **跨屏同步**：当用户在竖屏/横屏任一场景点赞后，可通过事件总线或共享 Repository 将状态写入本地数据库 (`shared/database.InteractionStateDao`)，另一端订阅同一数据源即可做到 UI 同步。

### 7. 接入步骤清单

1. 在 `business/videofeed` 与 `business/landscape` 的 Domain 层恢复/创建 Interaction UseCase，并通过 Hilt 模块注入。
2. Repository 调用 `shared/network` 提供的 `VideoFeedApiService`，确保 Retrofit 注册自定义 `ApiResponseConverterFactory`。
3. ViewModel 乐观更新 UI → 调用 UseCase → 失败回滚并输出日志。
4. 在 `docs/api_reference.md` 中追加新的字段或错误码时，同步更新本文件。
5. 编写单元测试（ViewModel + UseCase）验证状态流转。

### 8. 指标与埋点

- 记录点赞/收藏延迟、成功率、失败原因，统一上报到 `BeatUObservability` (`/api/v1/metrics/playback` 可扩展 `interaction` 字段)。
- 横竖屏共用埋点字段：`videoId`、`channel`（recommend/follow/landscape）、`action`、`result`、`latencyMs`。

> 当后端上线后，只需按本规范补齐 UseCase & API，实现即可无缝切换至真实链路。


