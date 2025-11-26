## 2025-11-26 VideoItemFragment 合并修正

- 统一保留 `navigatingToLandscape` + `hasPreparedPlayer`，横竖屏切换不重复初始化也不误暂停。
- 在 `onStart()` 根据 `currentVideoId` 判定：首次加载调用 `preparePlayerForFirstTime()`，横屏返回执行 `reattachPlayer()`。
- `onResume()` 再次调用 `startPlaybackIfNeeded()`，避免宿主恢复后不播。
- 通过 `@file:OptIn(UnstableApi::class)` 解决 `PlayerView.switchTargetView()` 的 `@UnstableApi` 告警，`openLandscapeMode()` 直接保留导航版本。
- `onParentVisibilityChanged()` / `onParentTabVisibilityChanged()` 统一驱动播放暂停，方便父容器控制。

## 2025-11-26 Landscape 模块数据链路同步

- Domain 层补充 `VideoItem`/`VideoOrientation` 数据类，UseCase/Repository 改为 `Flow<AppResult<List<VideoItem>>>`，避免依赖 `videofeed.domain.Video`。
- Data 层 mock 仓库直接生成 `VideoItem`，同时保留原有分页逻辑；presentation 层通过 typealias 复用 Domain 模型，以免重复定义。
- `LandscapeViewModel` 不再执行额外映射，直接消费 `VideoItem` 列表，并继续保持 `pendingExternalVideo` 首条插入逻辑。

## 2025-11-26 LandscapeVideoItemFragment 生命周期修复

- Fragment 顶部新增 `@file:OptIn(UnstableApi::class)` 并明确 `BundleCompat.getParcelable<VideoItem>`，解决 Media3 opt-in 与类型推断错误。
- `LandscapeVideoItemViewModel` 清理合并冲突：保留 `Player`/`PlayerView` 依赖、`currentVideoUrl` 字段、`PlaybackSession` 接口，实现与 ZX-good 一致的播放器切换与 `persistPlaybackSession()`。
- Fragment `prepareForExit()` / `onDestroyView()` 均确保 `PlayerView.switchTargetView(player, playerView, null)` 执行，横竖切换时保留播放器实例。

## 2025-11-26 LandscapeActivity 与布局重构

- `activity_landscape.xml` 改为纯容器（FragmentContainerView），横屏 UI 交由 `LandscapeFragment` 控制，背景保持纯黑。
- `LandscapeActivity` 去除旧的 ViewPager/Adapter 逻辑，仅在启动时托管 `LandscapeFragment` 并将 `Intent` Extras 传入 `arguments`；通过 `WindowCompat + WindowInsetsControllerCompat` 统一沉浸式设置与 onWindowFocus change。
- `LandscapeLaunchContract` 清除冲突并补上 `ACTIVITY_CLASS_NAME` 常量，供竖屏端直接定位横屏 Activity。

## 2025-11-27 RecommendFragment 手势/生命周期合并

- 将 ZX-good 的 `pendingRestoreIndex`、状态保存/恢复逻辑与 main 的左滑手势 `GestureDetectorCompat` 整合，Fragment 顶部重新导入相关字段。
- 新增 `onParentTabVisibilityChanged()`：Tab 可见时调用当前 `VideoItemFragment.onParentVisibilityChanged(true)`，不可见时批量暂停，配合 `pendingResumeRequest` 在 `onResume()` 恢复，契合生命周期优化。
- `observeViewModel()` 支持刷新归位与保存的下标恢复，`setupSwipeLeftGesture()` 中左滑导航到用户主页（`NavigationHelper`），同时保留 ZX-good 的 `restoreState()`/`onSaveInstanceState()` 以支持竖屏返回续播。

