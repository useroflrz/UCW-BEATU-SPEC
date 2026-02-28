# 模块循环依赖解决方案：Router 接口模式（推荐💯）

## 问题描述

在实现"点击头像显示用户信息"功能时，出现了模块间的循环依赖，导致 Gradle DataBinding 任务无法执行：

```
Circular dependency between the following tasks:
:business:user:presentation:dataBindingGenBaseClassesDebug
:business:videofeed:presentation:dataBindingGenBaseClassesDebug
```

### 依赖关系

- `videofeed:presentation` 需要依赖 `user:presentation`（使用 `UserProfileFragment`）
- `user:presentation` 需要依赖 `videofeed:presentation`（使用 `VideoItem` 和 `VideoItemFragment`）

## 解决方案：Router 接口模式（推荐💯）

### 核心思想

通过**公共的 Router 接口模块**来解耦两个 presentation 模块，而不是使用反射或 `compileOnly` 依赖。

### 方案 1：共享模型提取（解决模型循环依赖）

**将 `VideoItem`、`FeedContentType`、`VideoOrientation` 移到 `shared:common` 模块**

**实现步骤**：
1. 创建 `shared/common/src/main/java/com/ucw/beatu/shared/common/model/VideoItem.kt`
2. 在 `shared/common/build.gradle.kts` 中添加 `kotlin-parcelize` 插件支持
3. 更新所有引用 `VideoItem` 的文件，将导入路径改为 `com.ucw.beatu.shared.common.model`
4. 删除 `videofeed:presentation` 中的 `VideoItem.kt`

**效果**：
- 两个模块都依赖 `shared:common`，而不是互相依赖
- 消除了因为共享模型导致的循环依赖

### 方案 2：Router 接口模式（解决 Fragment 循环依赖）⭐

**创建 `shared:router` 模块，定义 Router 接口，各模块实现接口并通过 RouterRegistry 注册**

**架构设计**：
```
shared:router (定义接口)
    ├── UserProfileRouter (接口)
    ├── VideoItemRouter (接口)
    └── RouterRegistry (注册表)

videofeed:presentation
    ├── 依赖 shared:router
    └── VideoItemRouterImpl (实现)

user:presentation
    ├── 依赖 shared:router
    └── UserProfileRouterImpl (实现)

app (应用层)
    ├── 依赖 videofeed:presentation
    ├── 依赖 user:presentation
    └── 在启动时注册 Router 实现
```

**实现步骤**：

1. **创建 `shared:router` 模块**：
   - 定义 `UserProfileRouter` 接口
   - 定义 `VideoItemRouter` 接口
   - 创建 `RouterRegistry` 单例用于注册和获取 Router 实例

2. **各模块实现 Router 接口**：
   - `videofeed:presentation` 实现 `VideoItemRouterImpl`
   - `user:presentation` 实现 `UserProfileRouterImpl`

3. **在 app 模块注册 Router**：
   - 在 `BeatUApp.onCreate()` 中注册所有 Router 实现

4. **使用 Router 接口**：
   - `VideoItemFragment` 通过 `RouterRegistry.getUserProfileRouter()` 获取 Router 并创建 Fragment
   - `UserWorksViewerAdapter` 通过 `RouterRegistry.getVideoItemRouter()` 获取 Router 并创建 Fragment
   - `UserWorksViewerFragment` 通过 `RouterRegistry.getVideoItemRouter()` 获取 Router 并调用方法

**代码示例**：

1. **Router 接口定义**（`shared:router`）：
```kotlin
interface UserProfileRouter {
    fun createUserProfileFragment(userId: String, authorName: String, readOnly: Boolean = true): Fragment
}

interface VideoItemRouter {
    fun createVideoItemFragment(videoItem: VideoItem): Fragment
    fun checkVisibilityAndPlay(fragment: Fragment)
    fun onParentVisibilityChanged(fragment: Fragment, isVisible: Boolean)
}
```

2. **Router 实现**（`videofeed:presentation`）：
```kotlin
class VideoItemRouterImpl : VideoItemRouter {
    override fun createVideoItemFragment(videoItem: VideoItem): Fragment {
        return VideoItemFragment.newInstance(videoItem)
    }
    // ...
}
```

3. **Router 注册**（`app` 模块）：
```kotlin
private fun registerRouters() {
    RouterRegistry.registerUserProfileRouter(UserProfileRouterImpl())
    RouterRegistry.registerVideoItemRouter(VideoItemRouterImpl())
}
```

4. **使用 Router**（`videofeed:presentation`）：
```kotlin
val router = RouterRegistry.getUserProfileRouter()
val fragment = router?.createUserProfileFragment(userId, authorName, readOnly = true)
```

**效果**：
- ✅ 两个模块都只依赖 `shared:router`，不再互相依赖
- ✅ 通过接口解耦，符合依赖倒置原则（DIP）
- ✅ 编译时类型安全，不需要反射
- ✅ 代码清晰，易于维护和测试
- ✅ 彻底解决循环依赖问题

## 为什么这样可以解决循环依赖？

### 1. 依赖方向清晰

**修改前**：
```
videofeed:presentation ↔ user:presentation (循环依赖)
```

**修改后**：
```
shared:router (接口定义)
    ↑
    ├── videofeed:presentation (实现 VideoItemRouter)
    └── user:presentation (实现 UserProfileRouter)

app (注册 Router 实现)
    ├── videofeed:presentation
    └── user:presentation
```

### 2. 接口隔离

- 两个模块都依赖 `shared:router`（接口），而不是互相依赖（实现）
- 符合依赖倒置原则（DIP）：依赖抽象而不是具体实现

### 3. 运行时注册

- Router 实现在各自的模块中
- 在 app 模块启动时通过 `RouterRegistry` 注册
- 运行时通过 `RouterRegistry` 获取 Router 实例

## 最终依赖关系

```
shared:common (包含 VideoItem)
shared:router (包含 Router 接口)
    ↑
    ├── videofeed:presentation (实现 VideoItemRouter)
    └── user:presentation (实现 UserProfileRouter)

app
    ├── videofeed:presentation
    └── user:presentation
```

**依赖方向**：
- `videofeed:presentation` → `shared:common` ✅
- `videofeed:presentation` → `shared:router` ✅
- `user:presentation` → `shared:common` ✅
- `user:presentation` → `shared:router` ✅
- **两个模块不再互相依赖** ✅

## 优势对比

### Router 接口模式 vs 反射 + compileOnly

| 特性 | Router 接口模式 | 反射 + compileOnly |
|------|---------------|-------------------|
| 类型安全 | ✅ 编译时类型检查 | ❌ 运行时才能发现错误 |
| 代码清晰度 | ✅ 接口明确，易于理解 | ❌ 反射代码难以理解 |
| 维护性 | ✅ 接口变更时编译期报错 | ❌ 需要手动同步反射调用 |
| 性能 | ✅ 无反射开销 | ⚠️ 反射有性能开销 |
| 测试性 | ✅ 易于 Mock 接口 | ⚠️ 反射难以测试 |
| 架构设计 | ✅ 符合 Clean Architecture | ⚠️ 临时解决方案 |

## 注意事项

1. **Router 注册时机**：
   - 必须在 app 启动时注册 Router
   - 建议在 `Application.onCreate()` 中注册

2. **Router 实现位置**：
   - Router 实现在各自的模块中
   - 通过 `RouterRegistry` 在运行时注册

3. **接口设计**：
   - Router 接口应该只包含必要的方法
   - 避免在接口中暴露模块内部实现细节

4. **扩展性**：
   - 如果将来有其他模块需要类似的 Router，可以继续使用这个模式
   - 只需要在 `shared:router` 中定义新的接口

## 总结

通过以下两个方案解决了循环依赖问题：

1. ✅ **共享模型提取**：将 `VideoItem` 移到 `shared:common`
2. ✅ **Router 接口模式**：通过 `shared:router` 模块定义接口，各模块实现接口并通过 `RouterRegistry` 注册

**关键点**：
- **Router 接口模式是最优雅的解决方案**，符合 Clean Architecture 和依赖倒置原则
- 两个模块都只依赖 `shared:router`（接口），不再互相依赖（实现）
- 编译时类型安全，代码清晰，易于维护和测试
- 彻底解决循环依赖问题，且为未来的扩展提供了良好的架构基础

