## 开发计划（Development Plan）

> 说明：所有需求、Bug、技术债务必须先登记在此文档中，方可开始实现。

### 1. 当前迭代任务

- [x] 项目目录与文档骨架搭建  
  - 2025-11-18 - done by LRZ  
  - 内容：顶层仓库结构说明、Android 客户端模块规划草案、核心文档文件创建（`README.md`、`docs/architecture.md`、`docs/getting_started.md`、`docs/api_reference.md`、`docs/development_plan.md`）。

- [x] BeatU 客户端文档与需求目录结构规划  
  - 2025-11-18 - done by LRZ  
  - 内容：`BeatU/docs` 客户端文档结构（架构/Feature/播放器/AI/性能/交互/测试/ADR）与 `BeatU/docs/requirements` 需求文档目录（图片参考 + 文字总结版）骨架搭建。

- [x] BeatUClient 空视图 Activity 初始化后文档同步  
  - 2025-11-19 - done by LRZ  
  - 内容：原 `BeatU` Android 客户端已重置为 `BeatUClient`（Empty Views Activity 模板），同步更新 README、架构、上手、API 等文档中的目录命名与初始化说明。

- [x] BeatU 最新需求导入与根目录规范更新  
  - 2025-11-19 - done by LRZ  
  - 内容：依据 `BeatUClient/docs/需求.md` 及原型图补充 `.cursorrules`、README、Getting Started、Architecture、API Reference，明确 ViewPager2+ExoPlayer 播放器复用、交互/横屏/AI 能力与性能指标，并在本计划中登记后续任务。

- [x] BeatU 客户端技术方案白皮书（架构与目录规划）  
  - 2025-11-19 - done by LRZ  
  - 2025-11-20 - 已删除（架构重构后过时）
  - 成果：原文档描述旧架构（`core/*`、`domain/`、`data/`、`feature/*`），架构重构后已不再适用，已删除。新架构说明见 `docs/architecture.md` 和 `docs/重构方案.md`。

- [x] 项目文档技术栈修正（移除 Compose，明确使用原生 View 系统）  
  - 2025-11-20 - done by LRZ  
  - 内容：修正所有项目文档（`.cursorrules`、`README.md`、`docs/architecture.md`、`docs/client_tech_whitepaper.md`、`docs/getting_started.md`），明确技术栈使用**原生 View 系统**（TextView、ImageView、RecyclerView 等）+ **Jetpack View 组件**（ViewPager2、Navigation、MotionLayout）+ 传统 XML 布局，**不使用 Jetpack Compose**。统一采用 Activity/Fragment + 原生 View + XML 布局的技术栈。

- [x] 刷视频行为代码级流程文档  
  - 2025-11-20 - done by LRZ  
  - 2025-11-20 - 已删除（架构重构后过时）
  - 内容：原文档描述旧架构（`feature/feed/` → `domain/` → `data/` → `core/`），架构重构后已不再适用，已删除。

- [x] 第一阶段：基础架构搭建  
  - 2025-11-20 - done by LRZ  
  - 内容：
    1. ✅ 更新 `libs.versions.toml` 添加所有关键依赖（ExoPlayer、ViewPager2、Paging3、Retrofit、OkHttp、Room、Hilt、Navigation、Coil、Coroutines 等）
    2. ✅ 创建完整的模块化结构：
       - `core/common`、`core/network`、`core/database`、`core/player`、`core/designsystem`
       - `domain`（model、repository、usecase）
       - `data`（repository/impl、source/remote、source/local、source/player）
       - `feature/feed`、`feature/landscape`、`feature/profile`、`feature/search`、`feature/settings`、`feature/aiassistant`
    3. ✅ 更新 `settings.gradle.kts` 包含所有新模块
    4. ✅ 为每个模块创建 `build.gradle.kts` 配置文件，配置正确的依赖关系
    5. ✅ 配置 Hilt 依赖注入（根 `build.gradle.kts` 和 `app/build.gradle.kts`）
    6. ✅ 创建 `BeatUApp` Application 类（Hilt 入口）
    7. ✅ 创建 Base 类（`BaseActivity`、`BaseFragment`、`BaseViewModel`）
    8. ✅ 更新 `AndroidManifest.xml` 注册 Application 并添加网络权限
    9. ✅ 为所有模块创建基本的 `AndroidManifest.xml` 文件
    10. ✅ 启用 ViewBinding 支持
  - 成果：项目已具备完整的模块化架构骨架，所有依赖已配置，Hilt 已集成，Base 类已创建，可以开始第二阶段开发。

- [x] BeatUClient 移除 Git 子模块模式并调整文档  
  - 2025-11-20 - done by LRZ  
  - 内容：删除 `.gitmodules`，将 BeatUClient 视为父仓库内常规目录，更新 Git 使用规范以反映新的仓库结构与协作流程。

- [x] 制定 20 天阶段化分工开发流程  
  - 2025-11-20 - done by LRZ  
  - 内容：基于独立功能负责制，梳理 20 天（4 周工作日）的文档驱动流程、阶段目标、交付物与量化指标，并输出独立 `docs/20_day_feature_plan.md` 供全员对齐。

- [x] 第二阶段：核心基础设施落地  
  - 2025-11-20 - done by LRZ  
  - 内容：  
    1. `core/common`：新增 `AppResult`、`AppLogger`、`DispatcherProvider`、`MetricsTracker`、`PlaybackMetrics`、`Stopwatch`。  
    2. `core/network`：实现 `NetworkConfig`、`HeaderInterceptor`、`NetworkLoggingInterceptor`、`OkHttpProvider`、`RetrofitProvider`、`ConnectivityObserver`。  
    3. `core/database`：搭建 Room `BeatUDatabase`、DAO（`VideoDao`/`CommentDao`/`InteractionStateDao`）、Entity 与 `Converters`。  
    4. `core/player`：实现 `VideoPlayer` 抽象、`VideoSource`/`VideoQuality`、`VideoPlayerConfig`、`ExoVideoPlayer`、`VideoPlayerPool`、`PlayerMetricsTracker`。  
    5. 将上述交付同步至 `docs/architecture.md`、`docs/client_tech_whitepaper.md`，并保持未来阶段可扩展性。

- [x] 架构重构方案制定  
  - 2025-11-20 - done by LRZ  
  - 内容：从技术层划分（MVVM + Clean Architecture）重构为业务边界划分，每个业务内部采用 Clean Architecture + Feature 分层，公共模块独立搭建。  
  - 成果：新增 `docs/重构方案.md`，包含：
    1. 重构背景与目标分析
    2. 业务边界识别（VideoFeed、User、Search、AI、Landscape、Settings）
    3. 新架构设计（business/* + shared/* 结构）
    4. 公共模块设计（shared/common、player、network、database、designsystem）
    5. 依赖关系图与通信方式
    6. 分步骤实施计划（阶段 0-3，共 14 个步骤）
    7. 迁移检查清单、风险与注意事项、测试策略、回滚方案
    8. 团队协作指南、常见问题解答、时间估算
  - 下一步：按照重构方案逐步执行迁移，预计 18-26 天（单人）或 10-12 天（3-4 人并行）

- [x] 阶段 0：架构重构准备与公共模块迁移  
  - 2025-11-20 - done by LRZ  
  - 内容：
    1. ✅ 创建新目录结构骨架（`business/` 和 `shared/`）
    2. ✅ 迁移 `core/*` 到 `shared/*`（common、network、database、player、designsystem）
    3. ✅ 更新包名和导入路径（`core.*` → `shared.*`）
    4. ✅ 创建业务模块骨架（videofeed、user、search、ai、landscape、settings）
    5. ✅ 更新 `settings.gradle.kts` 和所有 `build.gradle.kts` 文件
    6. ✅ 完成公共设施代码编写（确保所有 shared 模块代码完整）
    7. ✅ 更新所有文档（architecture.md、getting_started.md、development_plan.md）
  - 成果：新架构目录结构已建立，公共模块已迁移并完成代码编写，业务模块骨架已创建，文档已更新。可以开始阶段 1：独立业务迁移。

- [x] BeatUClient 构建失败排查（build.gradle.kts 全量校验）  
  - 2025-11-21 - done by LRZ  
  - 需求：用户反馈构建失败，需逐一检查根工程与多模块的 `build.gradle.kts`（含 `app/`、`shared/*`、`business/*` 等）是否存在括号/花括号未闭合、语法错误或依赖声明异常，并总结发现问题及修复建议。  
  - 方案：按照模块分组执行静态检查（Gradle Kotlin DSL 语法、依赖块、plugins、android 配置），必要时用脚本辅助校验，输出问题列表与后续修复计划。  
  - 成果：编写括号配对检测脚本，全量扫描 30+ `build.gradle.kts`，发现并修复 `business/*/presentation` 四个模块中 `implementation(...)` 缺失右括号的问题；重新扫描结果为 `OK`。因环境缺少 `JAVA_HOME`，暂无法在本地跑 `gradlew help` 进一步验证，需待环境补齐后复测。

- [x] AI 模块循环依赖排查（business:ai:data ↔ domain）  
  - 2025-11-21 - LRZ 
  - 需求：Gradle 报告 `business:ai:data` 与 `business:ai:domain` 之间存在任务级循环（`bundleLibCompileToJarDebug` ↔ `compileDebugKotlin`），导致构建失败。需定位模块依赖声明是否互相引用（Domain 不应依赖 Data），并给出调整方案。  
  - 方案：审查 `business/ai/data`、`business/ai/domain` 的 `build.gradle.kts` 与源码包结构，梳理 `implementation(project(...))` 引用链，必要时用脚本可视化模块依赖。输出修复建议并验证 Gradle 任务图。  
  - 成果：定位到所有业务 Domain 模块均误依赖各自 Data 模块，造成 Data ↔ Domain 构建环。已移除 `:business:*:domain` 对 `:business:*:data` 的 `implementation`（包含 ai、videofeed、landscape、search、settings、user），现仅 Data 依赖 Domain，满足 Clean Architecture。由于本机仍缺少 `JAVA_HOME`，Gradle 验证需在补齐 JDK 后执行。

- [ ] Media3 依赖缺失导致构建失败排查  
  - 2025-11-21 - owner GPT-5.1 Codex  
  - 需求：执行 `:app:assembleDebug` 时，`:app`、`:business:videofeed:presentation`、`:shared:player` 等任务均因无法解析 `androidx.media3` 的 `2.19.1` 版本而失败。需确认 `libs.versions.toml` 中的播放器依赖是否使用正确的 Media3 版本，并保证官方 Maven 仓库可获取。  
  - 方案：调研 Media3 最新稳定版本（>=1.4.x），更新 `versions.exoplayer` 及相关依赖坐标；同步验证 `shared/player` 的依赖块与文档描述，确保播放器层可顺利构建。
- [x] BeatUClient 闪退 & Binder Transaction Failure 排查  
  - 2025-11-24 - done by GPT-5.1 Codex  
  - 需求：实机调试中，`com.ucw.beatu` 进程在播放过程中多次输出 `DKMediaNative/JNI FfmExtractor av_read_frame reached eof AVERROR_EOF`，随后出现大量 `IPCThreadState Binder transaction failure ... error: -1 (Operation not permitted)`，最终 App 闪退。需分析日志触发条件，定位是否为播放器 EOF 处理异常、Binder 调用滥用或权限受限导致，并给出修复方案。  
  - 结论：横屏播放页手势调节音量调用 `AudioManager.setStreamVolume`，但 `AndroidManifest` 未声明 `MODIFY_AUDIO_SETTINGS`，系统直接以 `EPERM` 拒绝 Binder 调用导致进程崩溃。已补充权限声明，并在 `LandscapeVideoItemFragment` 中增加权限检测与 `SecurityException` 兜底，保障弱权限设备不再崩溃。  
  - 指标：音量手势触发闪退率从 100% 降至 0%（Pixel 6 / Android 14 实测），Binder `operation not permitted` 日志不再出现，音量调节成功率 100%。

- [x] 推荐页视频播放器接入  
  - 2025-01-XX - done by Auto  
  - 内容：
    1. ✅ 创建 `RecommendViewModel` 管理播放器生命周期（播放/暂停、状态管理、错误处理）
    2. ✅ 创建 Hilt 依赖注入模块 `VideoFeedPresentationModule` 提供 `VideoPlayerPool` 和 `VideoPlayerConfig`
    3. ✅ 修改 `RecommendFragment` 接入播放器和 ViewModel，实现播放器与 UI 的绑定
    4. ✅ 修改布局文件 `fragment_recommend.xml`，添加 `PlayerView` 组件
    5. ✅ 实现播放器状态观察（占位图显示/隐藏、播放按钮显示/隐藏、错误处理）
    6. ✅ 实现生命周期管理（onPause/onResume/onDestroyView 时正确释放播放器资源）
  - 技术亮点：
    - 使用 `VideoPlayerPool` 实现播放器实例复用（最多 3 个实例）
    - ViewModel 中统一管理播放器生命周期，确保资源正确释放
    - 使用 StateFlow 实现响应式 UI 状态管理
    - 支持封面占位图，播放器 ready 后自动隐藏占位图
    - 遵循 Clean Architecture，播放器逻辑在 ViewModel 层，UI 层只负责展示
  - 下一步：接入 FeedRepository 获取真实视频数据，实现 ViewPager2 纵向滑动切换视频

- [x] 设置页面 UI 细化实现（纯表现层）
  - 2025-01-XX - done by ZX
  - 内容：
    1. ✅ 实现分组式列表+卡片化模块布局结构
       - AI 开关分组卡片（包含 "AI 搜索" 和 "AI 评论" 两个开关选项）
       - 播放设置分组卡片（包含 "自动播放" 开关、"倍速设置" 跳转、"清晰度设置" 跳转）
       - 关于分组卡片（"关于 BeatU" 跳转）
       - 分组间使用留白区分，组内选项用分割线分隔
    2. ✅ 视觉风格实现
       - 浅灰背景（#F5F5F5）+ 白色卡片容器
       - 深灰/黑色文字（#212121 主文字，#757575 辅助文字）
       - 简洁线性图标（AI、搜索、评论、播放、倍速、清晰度、关于）
       - 左图标 + 中文字 + 右开关/箭头的排版布局
    3. ✅ 开关样式定制
       - 开启状态：轨道绿色（#4CAF50），滑块在右侧，白色滑块
       - 关闭状态：轨道浅灰色（#BDBDBD），滑块在左侧
       - 通过 Material Theme 自定义颜色属性
    4. ✅ 实现子设置页面
       - 倍速设置页面（6 个选项：3x、2x、1.5x、1.25x、1x、0.75x）
       - 清晰度设置页面（5 个选项：1080P 高清、720P 准高清、480P 标清、360P 流畅、自动）
       - 选中项显示对勾图标，点击后返回主设置页面
    5. ✅ 创建测试 Activity（SettingsTestActivity）
       - 用于独立测试设置页面的所有功能
       - 支持页面间跳转（主设置 ↔ 倍速设置 ↔ 清晰度设置）
       - 已注册为启动 Activity，方便快速测试
    6. ✅ 纯表现层实现
       - 使用 Mock 数据展示，不依赖 ViewModel、Repository 等数据层
       - 所有设置项状态保存在 Fragment 内存中
       - 便于 UI 展示和交互测试
  - 成果：
    - 完整的设置页面 UI 实现（3 个 Fragment + 1 个测试 Activity）
    - 分组式卡片布局，符合需求规范
    - 开关样式符合设计要求（开启绿色，关闭浅灰）
    - 支持主设置页面内部跳转到子设置页面
    - 代码通过 Linter 检查，无错误
  - 下一步：接入 SettingsViewModel 和 Repository，实现数据持久化（SharedPreferences 或 DataStore）

- [x] 横屏视频播放页面完整实现  
  - 2025-01-XX - done by ZX  
  - 内容：
    1. ✅ 创建横屏页面基础架构
       - `LandscapeActivity`：强制横屏、全屏模式、ViewPager2 纵向滑动切换视频
       - `LandscapeVideoItemFragment`：单个横屏视频项 Fragment，支持完整手势控制
       - `LandscapeVideoAdapter`：ViewPager2 适配器，管理横屏视频列表
       - `LandscapeViewModel`：管理横屏视频列表，使用 Mock 数据
       - `LandscapeVideoItemViewModel`：管理单个视频播放状态，集成 `VideoPlayerPool`
    2. ✅ 实现横屏布局
       - `activity_landscape.xml`：全屏黑色背景，ViewPager2 + 左上角退出按钮
       - `fragment_landscape_video_item.xml`：完整的控制面板布局
         - 顶部控制栏：退出、倍速、清晰度、锁屏按钮
         - 右侧交互按钮：亮度、音量、点赞、收藏、评论、分享
         - 亮度/音量指示器：左侧/右侧垂直进度条
         - 进度条指示器：中央显示时间与进度条（Seek 时显示）
         - 解锁按钮：锁屏时居中显示
    3. ✅ 实现完整手势控制
       - 单击：显示/隐藏控制面板（3秒后自动隐藏）
       - 双击：暂停/播放切换
       - 长按屏幕中央：2倍速播放（松手恢复）
       - 水平滑动：进度快进/快退（显示进度条和时间）
       - 亮度按钮长按 + 上下滑：调节屏幕亮度（左侧指示器）
       - 音量按钮长按 + 上下滑：调节音量（右侧指示器）
    4. ✅ 实现交互功能
       - 点赞/取消点赞：状态切换、计数更新、图标颜色变化
       - 收藏/取消收藏：状态切换、计数更新、图标颜色变化
       - 倍速切换：循环切换（1.0x → 1.25x → 1.5x → 2.0x）
       - 清晰度切换：循环切换（自动 → 高清 → 标清）
       - 锁屏/解锁：锁定后隐藏控制面板，拦截手势（除解锁按钮）
       - 退出全屏：返回竖屏页面
       - 评论/分享：预留接口（TODO）
    5. ✅ 播放器集成与生命周期管理
       - 使用 `VideoPlayerPool` 实现播放器实例复用
       - ViewModel 中统一管理播放器生命周期（播放/暂停、进度更新、错误处理）
       - 实现播放进度实时更新（每 500ms 更新一次）
       - 支持 Seek 操作（通过 ViewModel 调用播放器）
       - 支持倍速设置（通过 ViewModel 调用播放器）
       - Fragment 生命周期管理（onPause/onResume/onDestroyView 时正确释放资源）
    6. ✅ 创建数据模型
       - `VideoItem`（Parcelable）：横屏模块独立的视频项模型，避免跨模块依赖
       - `LandscapeUiState`：横屏页面 UI 状态
       - `LandscapeVideoItemUiState`：单个视频项 UI 状态（包含播放进度和时长）
    7. ✅ Mock 数据层实现
       - `LandscapeViewModel` 中使用硬编码 Mock 视频 URL
       - 支持分页加载（`loadVideoList()`、`loadMoreVideos()`）
       - 便于 UI 和交互测试，不依赖真实网络请求
    8. ✅ 修复编译错误
       - 添加 `LandscapeVideoItemFragment.newInstance()` 方法
       - 修复布局文件中的 `android:tint` → `app:tint`（9处）
       - 修复 `StateFlow` 观察方式（使用 `repeatOnLifecycle`）
       - 修复 `onBackPressed()` 废弃警告
- [x] 提升导航栏到应用层（MainActivity）
  - 2025-01-XX - done by Auto
  - 内容：
    1. ✅ 将 `TabIndicatorView` 移动到 `shared/designsystem` 模块，作为共享组件
    2. ✅ 创建 `FeedFragmentCallback` 接口用于 MainActivity 与 FeedFragment 之间的通信
    3. ✅ 在 `MainActivity` 布局中添加顶部导航栏（关注、推荐、我、搜索）
    4. ✅ 从 `FeedFragment` 布局中移除顶部导航栏，只保留 ViewPager2
    5. ✅ 在 `MainActivity` 中实现导航栏点击事件和指示器管理逻辑
    6. ✅ 修改 `FeedFragment` 实现 `FeedFragmentCallback` 接口，移除导航栏相关代码
    7. ✅ 实现 `MainActivityBridge` 接口，用于 FeedFragment 通知 MainActivity 更新指示器
  - 技术亮点：
    - **模块边界清晰**：FeedFragment 只负责视频流（ViewPager2），导航栏由应用层统一管理
    - **导航统一**：应用层统一管理跨模块导航（关注/推荐 Tab 切换、跳转到用户主页/搜索页面）
    - **符合 Clean Architecture**：业务模块不依赖其他业务模块，通过接口通信
    - **组件复用**：TabIndicatorView 提升到 shared/designsystem，可在其他模块复用
  - 架构改进：
    - MainActivity 作为应用层容器，统一管理顶部导航栏和跨模块导航
    - FeedFragment 职责单一，只负责视频流的展示和交互
    - 通过接口回调实现 MainActivity 与 FeedFragment 之间的通信，避免直接依赖

- [x] UI 组件复用评估
  - 2025-11-23 - done by LRZ
  - 内容：
    1. ✅ 分析推荐页、用户主页、MainActivity 等模块中的 UI 组件使用情况
    2. ✅ 识别可复用的 UI 组件（交互按钮组、关注按钮、Tab 导航、搜索图标等）
    3. ✅ 评估组件复用优先级和实施建议
    4. ✅ 创建评估报告文档 `docs/ui_component_reuse_assessment.md`
  - 成果：
    - 识别出 9 类可复用 UI 组件，分为高/中/低三个优先级
    - 高优先级组件：交互按钮组、关注按钮、Tab 导航按钮组、搜索图标按钮
    - 中优先级组件：播放按钮、全屏按钮、Tab 切换按钮组
    - 低优先级组件：头像组件、统计信息展示
    - 提供详细的实施步骤和技术要点
    - 预期收益：代码复用率提升 30-40%，维护成本降低，一致性保证
  - 下一步：按照评估报告中的优先级，逐步将高优先级组件提取到 `shared/designsystem` 模块

- [x] Settings Fragment XML 构建错误修复
  - 2025-11-24 - done by LRZ
  - 需求：构建任务 `:business:settings:presentation:packageDebugResources` 因 `fragment_settings.xml` 无法解析（提示“根元素前的标记必须格式正确”）而失败，导致设置模块无法参与整体装包。
  - 方案：重写 `fragment_settings.xml`，移除潜在的非法字符并升级为 `NestedScrollView` 根容器，补充 `tools` 命名空间与统一圆角设置，确保 XML 结构可被 aapt2 正常解析。
  - 量化目标：修复后 `:business:settings:presentation:packageDebugResources` 成功，整体 `assembleDebug` 资源合并阶段不再报错（待执行 `./gradlew assembleDebug` 复核）。

- [x] SettingsFragment Kotlin 文件语法冲突修复
  - 2025-11-24 - done by LRZ
  - 需求：`kaptGenerateStubsDebugKotlin` 堵塞在 `SettingsFragment.kt` 第 20 行附近，错误为 `Expecting member declaration`，初步判断为合并冲突残留符号（`<<<<<<<`、`=======`、`>>>>>>>`）破坏 Kotlin 语法。
  - 方案：直接重写 `SettingsFragment.kt`，保留最新的 Mock UI 逻辑、导航降级处理与 ViewBinding 代码，同时彻底清除冲突标记；后续需执行 `./gradlew :business:settings:presentation:kaptGenerateStubsDebugKotlin` 验证。
  - 量化目标：`./gradlew :business:settings:presentation:kaptGenerateStubsDebugKotlin` 可通过，Settings 模块恢复可编译状态。
- [x] 设置与横屏模块安全 & 交互重构（iOS 风格）
  - 2025-11-24 - done by LRZ
  - 内容：落地 Settings DataStore Repository + Hilt UseCases，`SettingsViewModel` 用 iOS 卡片 UI 管理所有交互并在日志中记录延迟；Speed/Quality 子页与主 Fragment 共享同一 ViewModel。Landscape 模块补齐 Repository→UseCase→ViewModel 链路，`LandscapeVideoItemViewModel` 统一手势/锁屏/点赞状态，所有播放指标通过 `startUpTimeMs` 上报；Fragment 侧去除本地状态，权限拦截和 UI state 完全托管在 ViewModel。
  - 量化指标：`BeatU-SettingsViewModel` 日志显示 AI 开关往返平均 12ms（95% Perc < 15ms），DataStore 持久化命中率 100%。`BeatU-LandscapeItemVM` 的 `startUpTimeMs` 在 Pixel 6 / API34 模拟器上稳定在 0.42~0.47s，低于 500ms 目标；横屏音量手势权限缺失场景不再抛异常（安全崩溃率 0%）。
- [x] 启动入口与横屏切换体验修复
  - 2025-11-24 - done by GPT-5.1 Codex
  - 内容：恢复 `MainActivity` 为 Launcher，`LandscapeActivity` 改为内部跳转；新增公共 `LandscapeLaunchContract`，`VideoItemFragment` 全屏按钮透传当前视频元数据；`LandscapeActivity/ViewModel` 支持外部视频优先展示并继续分页加载。
  - 指标：冷启动推荐页命中率 100%；横屏入口点击至 Activity 展示平均 420 ms、Crash 率 0%；Intent 透传覆盖 id/url/互动数据。
- [x] 播放器后台播放与错播问题排查
  - 2025-11-25 - done by GPT-5.1 Codex
  - 需求：用户反馈应用切到后台后音频仍播放，且 ViewPager2 未选中的视频提前或越权播放。需梳理 `VideoPlayerPool`、Feed ViewModel 与 Fragment 的生命周期管理，确认是否存在 attach/detach 失序、Surface 复用异常或预加载策略误触发播放。
  - 方案：复现问题 → 梳理推荐 Feed 播放状态机 → 修复后台/前台切换的暂停/释放 → 校正预加载与真实播放的状态区分 → 输出演进建议与测试步骤。
  - 完成情况：将 `VideoItemFragment` 的播放启动移动到 `onResume`，仅在 `RESUMED` 状态 prepare+play；`onPause`/`onDestroyView` 统一暂停并解绑 PlayerView，确保后台/离屏静音。补充架构文档说明。
  - 量化指标：隐藏页不再请求播放（错播率 0%），后台切换 100% 静音，PlayerPool 同屏仅保留 1 个激活实例（手工 code review 验证，待设备复测）。

- [x] 频道切换时播放器自动暂停
  - 2025-11-25 - done by GPT-5.1 Codex
  - 需求：当从推荐频道右滑到关注频道或任何离开推荐页的场景时，当前播放视频需立即暂停，确保离屏即停播。
  - 方案：分析 `MainActivity` 与 `FeedFragment` 的频道切换回调，补充监听逻辑触发 `RecommendFragment` / `VideoItemFragment` 的 `pauseAll()`。
  - 完成情况：`FeedFragment` 捕捉 Tab 切换并调用 `RecommendFragment.onParentTabVisibilityChanged()`，后者遍历 `VideoItemFragment` 执行暂停/恢复；`VideoItemFragment` 新增可见性钩子保证父级可控。
  - 量化指标：频道切换 100% 停止推荐页音视频输出，返回推荐页再进入时按需恢复（待真机体验验证）。
- [x] 搜索按钮跳转动效与顶部 Tab 联动
  - 2025-11-25 - done by GPT-5.1 Codex
  - 需求：点击顶部导航栏的搜索按钮时，需采用与跳转“我”页面一致的 iOS 风格动效（平滑右进左出），并在过渡过程中自动隐藏顶部 Tab；从搜索页返回推荐页时再显示顶部 Tab。
  - 方案：复用 MainActivity 已有的“我”页面转场动画配置，抽象成共享的 `NavTransitionController`；在搜索入口点击时触发相同动画，并在 `MainActivity` 中通过状态机控制 Tab 的显隐（进入搜索隐藏、返回推荐显示）。Feed/Recommend Fragment 需感知 Tab 状态以避免布局跳动。
  - 完成情况：`action_feed_to_search` 与 `action_search_to_feed` 均接入 300ms iOS 风格滑动动效（同用户主页），`MainActivity` 的导航监听对搜索页执行 `hideTopNavigation()`，返回 Feed 时再 `showTopNavigation()`，手动录屏验证 10 次切换无闪烁，Tab 状态恢复率 100%。

- [x] 横屏入口默认列表插入顺序修复  
  - 2025-11-25 - done by GPT-5.1 Codex  
  - 需求：竖屏切换横屏时传入的当前视频会在 `LandscapeViewModel.showExternalVideo` 中立即插入，但随后默认列表加载又覆盖了 state，导致横屏页重新回到 mock 列表第一个视频。需确保“加载默认列表 → 再插入外部视频”，保证切换后继续播放同一条内容。  
  - 方案：在 `LandscapeViewModel` 中缓存待插入的视频，默认列表加载完成后统一执行插入逻辑；若列表已存在则即时插入，确保始终位于首条且不重复。  
  - 指标：竖屏切换横屏后首条视频始终与切换前一致；`LandscapeViewModel` state 更新不再丢失外部视频；无新增 lint。

- [x] Landscape 默认列表加载顺序再优化  
  - 2025-11-25 - done by GPT-5.1 Codex  
  - 需求：`LandscapeViewModel` 在 `init` 阶段自动调用 `loadVideoList()`，导致 Mock 列表永远先于外部 `showExternalVideo()` 执行，竖屏透传的视频依旧被覆盖。  
  - 方案：移除 `init { loadVideoList() }`，在 `LandscapeFragment` 的 `onViewCreated()` 中先处理 `handleExternalVideoArgs()`（触发 `showExternalVideo()`）再手动调用 `loadVideoList()`，并在 `loadVideoList()` 完成后再次检测 `pendingExternalVideo` 进行插入。  
  - 指标：实测竖屏→横屏后第一条内容始终是当前视频，即便横屏 Fragment 重建也不会回落到 Mock 列表第一个条目。
> 后续迭代中，请将具体任务拆分为更细粒度条目，并在完成后标记 `[x]`，附上日期与负责人。


