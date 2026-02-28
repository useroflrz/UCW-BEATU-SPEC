# BeatU 架构重构方案

> **目标**：从技术层划分（MVVM + Clean Architecture）重构为业务边界划分，每个业务内部采用 Clean Architecture + Feature 分层，公共模块独立搭建。

## 目录

1. [重构背景与目标](#1-重构背景与目标)
2. [业务边界识别](#2-业务边界识别)
3. [新架构设计](#3-新架构设计)
4. [公共模块设计](#4-公共模块设计)
5. [依赖关系图](#5-依赖关系图)
6. [迁移计划](#6-迁移计划)
7. [分步骤实施计划](#7-分步骤实施计划)

---

## 1. 重构背景与目标

### 1.1 当前架构问题

**现状**：
- 按技术层划分：`app/` → `feature/*` → `domain/` → `data/` → `core/*`
- 所有业务共享同一个 `domain/` 和 `data/` 层
- 开发人员需要跨多个技术层协作，不利于并行开发

**问题**：
1. **分工困难**：不同业务的功能代码分散在 `domain/` 和 `data/` 中，难以按业务分配任务
2. **耦合度高**：业务间通过共享的 Domain/Data 层产生隐式依赖
3. **扩展性差**：新增业务需要修改共享层，影响其他业务
4. **测试复杂**：业务边界不清晰，单元测试难以隔离

### 1.2 重构目标

1. **业务独立**：每个业务模块内部包含完整的 Presentation/Domain/Data 层
2. **并行开发**：不同业务可以独立开发、测试、发布
3. **清晰边界**：业务间通过明确的接口通信，避免隐式依赖
4. **公共能力复用**：播放器、网络、数据库等公共能力独立模块化
5. **易于维护**：业务代码集中，职责清晰

---

## 2. 业务边界识别

### 2.1 核心业务领域

基于当前功能模块和需求分析，识别以下业务边界：

#### 2.1.1 视频流业务（VideoFeed）
- **职责**：视频流展示、播放、上下滑动、点赞、收藏、评论、分享
- **核心功能**：
  - ViewPager2 纵向 Feed
  - 播放器生命周期管理
  - 手势交互（单击、双击、长按）
  - 评论半屏弹层
  - 播控面板
- **边界**：专注于视频内容的消费和互动

#### 2.1.2 用户业务（User）
- **职责**：用户信息、个人主页、关注/取消关注、作品列表、收藏列表
- **核心功能**：
  - 个人主页展示
  - 作者主页展示
  - 关注状态管理
  - 用户作品/收藏/点赞列表
- **边界**：专注于用户相关的所有功能

#### 2.1.3 搜索业务（Search）
- **职责**：频道切换、话题发现、内容搜索
- **核心功能**：
  - 频道 Tab 切换（推荐/关注/我）
  - 话题/标签搜索
  - 搜索结果展示
- **边界**：专注于内容发现和导航

#### 2.1.4 AI 业务（AI）
- **职责**：AI 评论助手、智能推荐、清晰度自适应
- **核心功能**：
  - 评论区 `@元宝` AI 问答
  - AI 内容理解与推荐
  - AI 清晰度切换（可选）
- **边界**：专注于 AI 能力的封装和调用

#### 2.1.5 横屏业务（Landscape）
- **职责**：横屏模式 UI、亮度/音量控制、锁屏、倍速/清晰度菜单
- **核心功能**：
  - 横屏模式切换
  - 手势控制（亮度、音量、快进）
  - 横屏评论浮层
- **边界**：专注于横屏场景的交互

#### 2.1.6 设置业务（Settings）
- **职责**：应用配置、用户偏好、功能开关
- **核心功能**：
  - AI 开关
  - 清晰度偏好
  - 倍速偏好
  - 横屏锁定
- **边界**：专注于配置管理

### 2.2 业务依赖关系

```
VideoFeed ──依赖──> User (显示作者信息、跳转主页)
VideoFeed ──依赖──> AI (评论助手、推荐)
VideoFeed ──依赖──> Landscape (横屏模式)
Search ──依赖──> VideoFeed (搜索结果展示)
Search ──依赖──> User (用户搜索结果)
Settings ──独立──> (配置管理，其他业务读取)
```

---

## 3. 新架构设计

### 3.1 整体架构图

```
┌─────────────────────────────────────────────────────────┐
│                      app/                                 │
│  (Application, Navigation, DI 入口)                      │
└─────────────────────────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
┌───────▼────────┐  ┌──────▼──────┐  ┌────────▼────────┐
│  business/     │  │  business/   │  │  business/      │
│  videofeed/    │  │  user/       │  │  search/        │
│                │  │              │  │                 │
│  presentation/ │  │  presentation│  │  presentation/   │
│  domain/       │  │  domain/     │  │  domain/        │
│  data/         │  │  data/       │  │  data/          │
└────────────────┘  └──────────────┘  └─────────────────┘
        │                   │                   │
        └───────────────────┼───────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
┌───────▼────────┐  ┌──────▼──────┐  ┌────────▼────────┐
│  shared/       │  │  shared/     │  │  shared/        │
│  player/       │  │  network/    │  │  database/      │
│                │  │              │  │                 │
│  (播放器抽象)   │  │  (网络栈)    │  │  (数据库)       │
└────────────────┘  └──────────────┘  └─────────────────┘
        │                   │                   │
        └───────────────────┼───────────────────┘
                            │
                ┌───────────▼───────────┐
                │   shared/common/      │
                │   (Result, Logger,   │
                │    Metrics, Utils)   │
                └───────────────────────┘
```

### 3.2 业务模块内部结构

每个业务模块遵循 Clean Architecture，包含三层：

```
business/videofeed/
├── presentation/
│   ├── ui/                    # Activity, Fragment, ViewHolder
│   ├── viewmodel/             # ViewModel
│   ├── state/                 # UIState, UIEvent
│   └── di/                    # 业务内 DI 模块
├── domain/
│   ├── model/                 # 业务领域模型
│   ├── repository/            # Repository 接口
│   ├── usecase/               # UseCase
│   └── exception/             # 业务异常
└── data/
    ├── repository/impl/        # Repository 实现
    ├── source/remote/          # Retrofit API
    ├── source/local/           # Room/DataStore
    └── mapper/                 # DTO ↔ Model 映射
```

### 3.3 目录结构对比

**重构前**：
```
BeatUClient/
├── app/
├── core/          # 基础设施
├── domain/        # 所有业务的领域层（共享）
├── data/          # 所有业务的数据层（共享）
└── feature/       # UI 层
```

**重构后**：
```
BeatUClient/
├── app/           # 入口、Navigation、全局 DI
├── business/      # 业务模块
│   ├── videofeed/
│   │   ├── presentation/
│   │   ├── domain/
│   │   └── data/
│   ├── user/
│   ├── search/
│   ├── ai/
│   ├── landscape/
│   └── settings/
└── shared/        # 公共模块（原 core/）
    ├── common/    # 通用工具
    ├── player/    # 播放器
    ├── network/   # 网络栈
    ├── database/  # 数据库
    └── designsystem/  # 设计系统
```

---

## 4. 公共模块设计

### 4.1 shared/common
- **职责**：跨业务通用工具
- **内容**：
  - `AppResult<T>`：统一结果包装
  - `AppLogger`：日志工具
  - `MetricsTracker`：性能指标采集
  - `DispatcherProvider`：协程调度器
  - `Stopwatch`：计时工具
- **依赖**：无业务依赖，纯工具类

### 4.2 shared/player
- **职责**：播放器抽象和实现
- **内容**：
  - `VideoPlayer` 接口
  - `ExoVideoPlayer` 实现
  - `VideoPlayerPool` 播放器池
  - `VideoSource`、`VideoQuality` 模型
  - `PlayerMetricsTracker` 指标采集
- **依赖**：`shared/common`

### 4.3 shared/network
- **职责**：网络请求基础设施
- **内容**：
  - `NetworkConfig` 配置
  - `OkHttpProvider`、`RetrofitProvider`
  - `HeaderInterceptor`、`NetworkLoggingInterceptor`
  - `ConnectivityObserver` 网络状态监听
- **依赖**：`shared/common`

### 4.4 shared/database
- **职责**：数据库基础设施
- **内容**：
  - `BeatUDatabase` Room 数据库
  - 通用 Entity（如 `UserEntity`、`VideoEntity`）
  - 通用 DAO 接口
  - TypeConverter
- **依赖**：`shared/common`
- **注意**：业务特定的 Entity 可以放在业务模块的 `data/` 层

### 4.5 shared/designsystem
- **职责**：UI 设计系统
- **内容**：
  - 主题、颜色、字体
  - 通用组件（Button、Card 等）
  - 动画资源（Lottie、MotionLayout）
  - 触觉反馈工具
- **依赖**：`shared/common`

### 4.6 公共模块使用原则

1. **只提供能力，不包含业务逻辑**
2. **接口优先**：通过接口定义能力，便于测试和替换
3. **配置化**：通过配置类控制行为，避免硬编码
4. **可观测性**：提供指标和日志能力

---

## 5. 依赖关系图

### 5.1 模块依赖矩阵

| 模块 | 可依赖的模块 |
|------|-------------|
| `app` | 所有 `business/*`、所有 `shared/*` |
| `business/videofeed` | `shared/*`、`business/user`（接口）、`business/ai`（接口）、`business/landscape`（接口） |
| `business/user` | `shared/*` |
| `business/search` | `shared/*`、`business/videofeed`（接口）、`business/user`（接口） |
| `business/ai` | `shared/*` |
| `business/landscape` | `shared/*`、`business/videofeed`（接口） |
| `business/settings` | `shared/*` |
| `shared/*` | 仅 `shared/common`（shared 内部可互相依赖） |

### 5.2 业务间通信方式

#### 方式 1：通过 Navigation（推荐）
- 使用 Jetpack Navigation 进行页面跳转
- 通过 Navigation Arguments 传递数据
- 适用于：页面跳转场景

#### 方式 2：通过接口（推荐）
- 在 `business/videofeed/domain/repository/` 定义 `UserRepository` 接口
- `business/user` 实现该接口并暴露给 `business/videofeed`
- 通过 DI 注入
- 适用于：数据获取、状态查询

#### 方式 3：通过 Event Bus（谨慎使用）
- 使用 SharedFlow/StateFlow 实现事件总线
- 适用于：跨业务状态同步（如关注状态变化）

### 5.3 依赖注入策略

**原则**：
- 每个业务模块有自己的 DI 模块（Hilt Module）
- 公共模块提供 Provider/Factory
- `app` 模块负责组装所有 DI 模块

**示例**：
```kotlin
// business/videofeed/presentation/di/VideoFeedModule.kt
@Module
@InstallIn(ViewModelComponent::class)
object VideoFeedModule {
    @Provides
    fun provideFeedRepository(
        remoteDataSource: FeedRemoteDataSource,
        localDataSource: FeedLocalDataSource
    ): FeedRepository = FeedRepositoryImpl(remoteDataSource, localDataSource)
}

// app/di/AppModule.kt
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // 组装所有业务模块的依赖
}
```

---

## 6. 迁移计划

### 6.1 迁移原则

1. **渐进式迁移**：不一次性重构，按业务模块逐步迁移
2. **向后兼容**：迁移过程中保持应用可运行
3. **测试保障**：每个迁移步骤都有测试验证
4. **文档同步**：及时更新架构文档

### 6.2 迁移步骤概览

1. **阶段 0：准备**（1-2 天）
   - 创建新目录结构
   - 迁移公共模块（shared/*）
   - 更新构建配置

2. **阶段 1：独立业务迁移**（每个业务 2-3 天）
   - Settings（最简单，无依赖）
   - User（相对独立）
   - AI（相对独立）

3. **阶段 2：核心业务迁移**（每个业务 3-4 天）
   - VideoFeed（核心业务，依赖多）
   - Landscape（依赖 VideoFeed）
   - Search（依赖 VideoFeed、User）

4. **阶段 3：清理与优化**（2-3 天）
   - 删除旧目录结构
   - 优化依赖关系
   - 更新文档

---

## 7. 分步骤实施计划

### 阶段 0：准备阶段（步骤 1-5）

#### 步骤 1：创建新目录结构骨架
- [ ] 创建 `business/` 目录
- [ ] 创建 `shared/` 目录（替代 `core/`）
- [ ] 为每个业务创建三层目录结构（presentation/domain/data）
- [ ] 更新 `settings.gradle.kts` 添加新模块

#### 步骤 2：迁移 shared/common
- [ ] 将 `core/common` 迁移到 `shared/common`
- [ ] 更新包名和导入路径
- [ ] 验证编译通过

#### 步骤 3：迁移 shared/network
- [ ] 将 `core/network` 迁移到 `shared/network`
- [ ] 更新依赖关系
- [ ] 验证编译通过

#### 步骤 4：迁移 shared/database
- [ ] 将 `core/database` 迁移到 `shared/database`
- [ ] 更新依赖关系
- [ ] 验证编译通过

#### 步骤 5：迁移 shared/player 和 shared/designsystem
- [ ] 将 `core/player` 迁移到 `shared/player`
- [ ] 将 `core/designsystem` 迁移到 `shared/designsystem`
- [ ] 更新依赖关系
- [ ] 验证编译通过

### 阶段 1：独立业务迁移（步骤 6-8）

#### 步骤 6：迁移 business/settings
- [ ] 创建 `business/settings` 模块结构
- [ ] 迁移 Settings 相关代码到新结构
- [ ] 实现 Settings 的 Domain 层（UseCase、Repository 接口）
- [ ] 实现 Settings 的 Data 层（Repository 实现、DataStore）
- [ ] 实现 Settings 的 Presentation 层（Activity/Fragment、ViewModel）
- [ ] 配置 DI 模块
- [ ] 编写单元测试
- [ ] 验证功能正常

#### 步骤 7：迁移 business/user
- [ ] 创建 `business/user` 模块结构
- [ ] 从 `domain/` 和 `data/` 中提取 User 相关代码
- [ ] 实现 User 的 Domain 层
- [ ] 实现 User 的 Data 层
- [ ] 实现 User 的 Presentation 层
- [ ] 定义 User 对外接口（供其他业务使用）
- [ ] 配置 DI 模块
- [ ] 编写单元测试
- [ ] 验证功能正常

#### 步骤 8：迁移 business/ai
- [ ] 创建 `business/ai` 模块结构
- [ ] 从 `domain/` 和 `data/` 中提取 AI 相关代码
- [ ] 实现 AI 的 Domain 层（AiRepository 接口、AiReplyUseCase、RecommendUseCase）
- [ ] 实现 AI 的 Data 层（AiRepositoryImpl、AiRemoteDataSource）
- [ ] 实现 AI 的 Presentation 层（AI 评论助手 UI、推荐入口）
- [ ] 定义 AI 对外接口（供 VideoFeed 使用）
- [ ] 配置 DI 模块
- [ ] 编写单元测试
- [ ] 验证功能正常

### 阶段 2：核心业务迁移（步骤 9-11）

#### 步骤 9：迁移 business/videofeed
- [ ] 创建 `business/videofeed` 模块结构
- [ ] 从 `feature/feed` 提取 UI 代码到 `presentation/ui/`
- [ ] 从 `domain/` 提取 Feed 相关代码到 `domain/`
- [ ] 从 `data/` 提取 Feed 相关代码到 `data/`
- [ ] 实现 VideoFeed 的 Domain 层：
  - [ ] `FeedRepository` 接口
  - [ ] `GetFeedUseCase`、`LikeVideoUseCase`、`CommentUseCase` 等
  - [ ] `VideoFeedModel`、`CommentModel` 等
- [ ] 实现 VideoFeed 的 Data 层：
  - [ ] `FeedRepositoryImpl`
  - [ ] `FeedRemoteDataSource`（Retrofit API）
  - [ ] `FeedLocalDataSource`（Room）
  - [ ] DTO 到 Model 的 Mapper
- [ ] 实现 VideoFeed 的 Presentation 层：
  - [ ] `FeedActivity`、`FeedFragment`
  - [ ] `FeedViewModel`（管理播放器生命周期）
  - [ ] `FeedUIState`、`FeedUIEvent`
  - [ ] ViewPager2 Adapter、ViewHolder
- [ ] 集成 `shared/player` 的播放器池
- [ ] 配置 DI 模块（依赖 User、AI、Landscape 接口）
- [ ] 编写单元测试和集成测试
- [ ] 验证播放、滑动、点赞、评论等功能正常

#### 步骤 10：迁移 business/landscape
- [ ] 创建 `business/landscape` 模块结构
- [ ] 从 `feature/landscape` 提取代码
- [ ] 实现 Landscape 的 Domain 层：
  - [ ] `LandscapeRepository` 接口（查询播放器状态）
  - [ ] `ControlBrightnessUseCase`、`ControlVolumeUseCase` 等
- [ ] 实现 Landscape 的 Data 层：
  - [ ] `LandscapeRepositoryImpl`（读取播放器状态）
- [ ] 实现 Landscape 的 Presentation 层：
  - [ ] `LandscapeActivity`、`LandscapeFragment`
  - [ ] `LandscapeViewModel`
  - [ ] 横屏手势处理（亮度、音量、快进）
  - [ ] 横屏控制面板 UI
- [ ] 配置 DI 模块（依赖 VideoFeed 接口）
- [ ] 编写单元测试
- [ ] 验证横屏切换、手势控制等功能正常

#### 步骤 11：迁移 business/search
- [ ] 创建 `business/search` 模块结构
- [ ] 从 `feature/search` 提取代码
- [ ] 实现 Search 的 Domain 层：
  - [ ] `SearchRepository` 接口
  - [ ] `SearchContentUseCase`、`SearchUserUseCase`、`GetChannelsUseCase`
- [ ] 实现 Search 的 Data 层：
  - [ ] `SearchRepositoryImpl`
  - [ ] `SearchRemoteDataSource`
- [ ] 实现 Search 的 Presentation 层：
  - [ ] `SearchActivity`、`SearchFragment`
  - [ ] `SearchViewModel`
  - [ ] 频道 Tab 切换 UI
  - [ ] 搜索结果列表（复用 VideoFeed 的 ViewHolder）
- [ ] 配置 DI 模块（依赖 VideoFeed、User 接口）
- [ ] 编写单元测试
- [ ] 验证搜索、频道切换等功能正常

### 阶段 3：清理与优化（步骤 12-14）

#### 步骤 12：更新 app 模块
- [ ] 更新 `app` 模块的依赖关系（移除对旧 `domain/`、`data/`、`core/` 的依赖）
- [ ] 更新 Navigation 路由（指向新的业务模块）
- [ ] 更新全局 DI 配置（组装所有业务模块的 DI）
- [ ] 更新 `BeatUApp` 初始化逻辑
- [ ] 验证应用启动和导航正常

#### 步骤 13：删除旧目录结构
- [ ] 确认所有业务已迁移完成
- [ ] 删除 `core/` 目录（已迁移到 `shared/`）
- [ ] 删除 `domain/` 目录（已迁移到各业务模块）
- [ ] 删除 `data/` 目录（已迁移到各业务模块）
- [ ] 删除 `feature/` 目录（已迁移到各业务模块的 `presentation/`）
- [ ] 清理 `settings.gradle.kts` 中的旧模块引用
- [ ] 验证编译通过

#### 步骤 14：文档更新与优化
- [ ] 更新 `docs/architecture.md` 反映新架构
- [ ] 更新 `docs/client_tech_whitepaper.md`
- [ ] 更新 `docs/development_plan.md` 标记重构完成
- [ ] 更新 `README.md` 中的架构说明
- [ ] 创建新架构的模块依赖图
- [ ] 编写迁移总结报告

---

## 8. 迁移检查清单

### 8.1 每个业务模块迁移检查项

#### Domain 层检查
- [ ] 所有 Repository 接口已定义
- [ ] 所有 UseCase 已实现
- [ ] 领域模型（Model）已定义
- [ ] 业务异常已定义
- [ ] 单元测试覆盖率达到 80%+

#### Data 层检查
- [ ] Repository 实现已完成
- [ ] RemoteDataSource 已实现（Retrofit API）
- [ ] LocalDataSource 已实现（Room/DataStore）
- [ ] DTO 到 Model 的 Mapper 已实现
- [ ] 错误处理已完善
- [ ] 单元测试已编写

#### Presentation 层检查
- [ ] Activity/Fragment 已实现
- [ ] ViewModel 已实现（使用 StateFlow）
- [ ] UIState 和 UIEvent 已定义
- [ ] ViewBinding 已配置
- [ ] 生命周期管理正确
- [ ] UI 测试已编写（可选）

#### DI 配置检查
- [ ] Hilt Module 已创建
- [ ] 所有依赖已正确注入
- [ ] 作用域配置正确（Singleton、ViewModel 等）

#### 集成检查
- [ ] 功能测试通过
- [ ] 与依赖业务的接口对接正常
- [ ] 性能指标正常（无回归）

### 8.2 公共模块迁移检查项

- [ ] 包名已更新（`core.*` → `shared.*`）
- [ ] 所有依赖关系已更新
- [ ] 编译通过
- [ ] 单元测试通过
- [ ] 被业务模块正确引用

---

## 9. 风险与注意事项

### 9.1 技术风险

1. **依赖循环风险**
   - **风险**：业务模块间可能产生循环依赖
   - **应对**：严格遵循依赖关系图，通过接口解耦

2. **编译时间增加**
   - **风险**：模块增多可能导致编译时间增加
   - **应对**：使用 Gradle 并行编译，优化模块依赖

3. **DI 配置复杂**
   - **风险**：模块增多导致 DI 配置复杂
   - **应对**：每个业务模块独立 DI 模块，`app` 模块统一组装

### 9.2 业务风险

1. **功能回归**
   - **风险**：迁移过程中可能引入 Bug
   - **应对**：每个步骤都有测试验证，保持应用可运行

2. **性能回归**
   - **风险**：架构变更可能影响性能
   - **应对**：迁移前后进行性能对比测试

### 9.3 团队协作风险

1. **代码冲突**
   - **风险**：多人并行开发可能产生冲突
   - **应对**：按业务模块分工，减少交叉修改

2. **理解成本**
   - **风险**：新架构需要团队学习
   - **应对**：及时更新文档，组织技术分享

---

## 10. 测试策略

### 10.1 单元测试

- **Domain 层**：测试 UseCase 逻辑，Mock Repository
- **Data 层**：测试 Repository 实现，Mock RemoteDataSource 和 LocalDataSource
- **Presentation 层**：测试 ViewModel 逻辑，Mock UseCase

### 10.2 集成测试

- **业务模块内集成**：测试 Presentation → Domain → Data 的完整流程
- **跨业务集成**：测试业务间接口调用
- **公共模块集成**：测试业务模块对公共模块的使用

### 10.3 UI 测试

- 使用 Espresso 测试关键用户流程
- 测试播放、滑动、点赞、评论等核心功能

### 10.4 性能测试

- 迁移前后对比：
  - 冷启动时间
  - 内存占用峰值
  - 播放性能指标（首帧时间、卡顿率等）

---

## 11. 回滚方案

### 11.1 回滚触发条件

- 迁移后出现严重 Bug，影响核心功能
- 性能指标严重下降（>20%）
- 编译时间增加过多（>50%）

### 11.2 回滚步骤

1. **保留旧代码**：迁移过程中不立即删除旧代码，保留在独立分支
2. **快速回滚**：通过 Git 回滚到迁移前的提交
3. **问题修复**：在旧架构基础上修复问题
4. **重新规划**：分析问题原因，调整迁移方案

### 11.3 部分回滚

- 如果只有某个业务模块有问题，可以只回滚该模块
- 其他已迁移的业务模块保持不变

---

## 12. 团队协作指南

### 12.1 分工原则

- **按业务模块分工**：每个开发人员负责一个或多个业务模块
- **公共模块专人负责**：指定专人维护 `shared/*` 模块
- **接口先行**：业务间接口定义要提前对齐

### 12.2 开发流程

1. **需求分析**：明确需求属于哪个业务模块
2. **接口设计**：如果涉及跨业务，先定义接口
3. **并行开发**：各业务模块独立开发
4. **集成测试**：开发完成后进行集成测试
5. **代码审查**：重点关注业务边界和接口使用

### 12.3 代码规范

- **包命名**：`com.ucw.beatu.business.{业务名}.{层级}`
- **接口命名**：业务对外接口以 `I{业务名}Repository` 命名
- **依赖原则**：只能依赖 `shared/*` 和已定义的业务接口

### 12.4 沟通机制

- **每日站会**：同步迁移进度和遇到的问题
- **技术评审**：接口设计和架构变更需要技术评审
- **文档更新**：及时更新架构文档和开发计划

---

## 13. 常见问题解答

### Q1: 业务模块间如何共享数据模型？

**A**: 
- 如果数据模型是业务特定的，放在各自业务模块的 `domain/model/` 中
- 如果是跨业务共享的（如 `User`、`Video`），可以：
  1. 放在 `shared/common/model/` 中（不推荐，违反业务独立原则）
  2. 通过接口传递，每个业务有自己的 Model，通过 Mapper 转换（推荐）

### Q2: 如何处理业务间的状态同步？

**A**: 
- **推荐方式**：通过接口查询（如 VideoFeed 需要用户信息时，调用 User 业务接口）
- **事件方式**：使用 SharedFlow/StateFlow 实现事件总线（谨慎使用，避免过度耦合）
- **避免方式**：不要直接访问其他业务的数据层

### Q3: 公共模块如何版本管理？

**A**: 
- 公共模块的变更需要谨慎，避免破坏性变更
- 如果必须变更，需要：
  1. 先通知所有依赖的业务模块
  2. 提供迁移指南
  3. 保持向后兼容（如通过重载方法）

### Q4: 如何保证业务模块的独立性？

**A**: 
- **编译时隔离**：通过 Gradle 模块依赖控制，业务模块不能直接依赖其他业务模块的实现
- **运行时隔离**：通过接口和 DI 实现运行时解耦
- **测试隔离**：每个业务模块的测试不依赖其他业务模块的实现

### Q5: 迁移过程中如何保证应用可运行？

**A**: 
- **渐进式迁移**：一次只迁移一个业务模块
- **双写策略**：迁移过程中，新旧代码并存，逐步切换
- **功能开关**：通过 Feature Flag 控制新旧代码的使用

---

## 14. 时间估算

### 14.1 阶段时间分配

| 阶段 | 预计时间 | 说明 |
|------|---------|------|
| 阶段 0：准备 | 1-2 天 | 创建结构、迁移公共模块 |
| 阶段 1：独立业务 | 6-9 天 | Settings(2天) + User(2-3天) + AI(2-3天) |
| 阶段 2：核心业务 | 9-12 天 | VideoFeed(4-5天) + Landscape(2-3天) + Search(3-4天) |
| 阶段 3：清理优化 | 2-3 天 | 删除旧代码、文档更新 |
| **总计** | **18-26 天** | 约 3.5-5 周（按 1 人计算） |

### 14.2 并行开发优化

如果团队有 3-4 人，可以并行开发：
- **阶段 1**：3 人分别负责 Settings、User、AI（3-4 天）
- **阶段 2**：2 人负责 VideoFeed，1 人负责 Landscape，1 人负责 Search（5-6 天）
- **总计**：约 **10-12 天**（1.5-2 周）

---

## 15. 成功标准

### 15.1 架构目标达成

- [ ] 所有业务模块已独立，包含完整的 Presentation/Domain/Data 层
- [ ] 公共模块已独立，为所有业务提供支持
- [ ] 业务间通过明确的接口通信
- [ ] 编译通过，无循环依赖

### 15.2 功能目标达成

- [ ] 所有功能正常运行，无回归
- [ ] 性能指标无下降（或下降 < 5%）
- [ ] 单元测试覆盖率 ≥ 80%

### 15.3 团队目标达成

- [ ] 团队成员理解新架构
- [ ] 可以按业务模块并行开发
- [ ] 文档已更新，便于后续维护

---

## 16. 后续优化方向

### 16.1 模块化进一步优化

- 考虑将大型业务模块（如 VideoFeed）进一步拆分为子模块
- 引入 Gradle Convention Plugin 统一模块配置

### 16.2 性能优化

- 优化模块依赖，减少编译时间
- 使用 Gradle Build Cache 加速构建

### 16.3 工具链优化

- 开发模块依赖分析工具
- 自动化架构检查（如禁止业务模块直接依赖）

---

## 附录

### A. 参考资源

- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Android 模块化最佳实践](https://developer.android.com/topic/modularization)
- [Hilt 依赖注入](https://developer.android.com/training/dependency-injection/hilt-android)

### B. 术语表

- **业务模块（Business Module）**：按业务边界划分的独立模块，包含完整的 Presentation/Domain/Data 层
- **公共模块（Shared Module）**：为多个业务模块提供通用能力的模块
- **接口（Interface）**：业务模块对外暴露的能力定义
- **DI（Dependency Injection）**：依赖注入，用于解耦模块依赖

---

**文档版本**：v1.0  
**最后更新**：2025-11-20  
**维护者**：架构团队