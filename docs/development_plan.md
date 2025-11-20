## 开发计划（Development Plan）

> 说明：所有需求、Bug、技术债务必须先登记在此文档中，方可开始实现。

### 1. 当前迭代任务

- [x] 项目目录与文档骨架搭建  
  - 2025-11-18 - done by AI-assistant  
  - 内容：顶层仓库结构说明、Android 客户端模块规划草案、核心文档文件创建（`README.md`、`docs/architecture.md`、`docs/getting_started.md`、`docs/api_reference.md`、`docs/development_plan.md`）。

- [x] BeatU 客户端文档与需求目录结构规划  
  - 2025-11-18 - done by AI-assistant  
  - 内容：`BeatU/docs` 客户端文档结构（架构/Feature/播放器/AI/性能/交互/测试/ADR）与 `BeatU/docs/requirements` 需求文档目录（图片参考 + 文字总结版）骨架搭建。

- [x] BeatUClient 空视图 Activity 初始化后文档同步  
  - 2025-11-19 - done by AI-assistant  
  - 内容：原 `BeatU` Android 客户端已重置为 `BeatUClient`（Empty Views Activity 模板），同步更新 README、架构、上手、API 等文档中的目录命名与初始化说明。

- [x] BeatU 最新需求导入与根目录规范更新  
  - 2025-11-19 - done by AI-assistant  
  - 内容：依据 `BeatUClient/docs/需求.md` 及原型图补充 `.cursorrules`、README、Getting Started、Architecture、API Reference，明确 ViewPager2+ExoPlayer 播放器复用、交互/横屏/AI 能力与性能指标，并在本计划中登记后续任务。

- [x] BeatU 客户端技术方案白皮书（架构与目录规划）  
  - 2025-11-19 - done by AI-assistant  
  - 成果：新增 `docs/client_tech_whitepaper.md`，总结 Clean Architecture 模块划分、播放器/性能/AI 策略与目录规划代码区，为后续模块化开发提供统一蓝本。

- [x] 项目文档技术栈修正（移除 Compose，明确使用原生 View 系统）  
  - 2025-01-XX - done by AI-assistant  
  - 内容：修正所有项目文档（`.cursorrules`、`README.md`、`docs/architecture.md`、`docs/client_tech_whitepaper.md`、`docs/getting_started.md`），明确技术栈使用**原生 View 系统**（TextView、ImageView、RecyclerView 等）+ **Jetpack View 组件**（ViewPager2、Navigation、MotionLayout）+ 传统 XML 布局，**不使用 Jetpack Compose**。统一采用 Activity/Fragment + 原生 View + XML 布局的技术栈。

- [x] 刷视频行为代码级流程文档  
  - 2025-01-XX - done by AI-assistant  
  - 内容：新增 `docs/code_flow_feed_scrolling.md`，详细描述用户从打开 App 到刷视频（上下滑动切换）的完整代码级流程，涵盖模块调用链（`app/` → `feature/feed/` → `domain/` → `data/` → `core/`）、数据流转路径（Retrofit → Repository → UseCase → ViewModel → UI）、播放器生命周期管理（PlayerPool、预加载、资源释放）与关键代码位置索引。为后续开发提供统一的代码流程参考。

### 2. 待规划任务示例（占位）

- [ ] BeatU Android 客户端模块化重构设计（按 `core/*` 与 `feature/*` 拆分）。
- [ ] 视频播放器抽象层设计与 ExoPlayer 集成方案（含生命周期与内存管理策略）。
- [ ] 视频流（Feed）页面交互与性能指标基线定义。
- [ ] AI 能力方案选型（清晰度切换 / 推荐 / 语音识别）与数据流设计。
- [ ] Feed MVP：`feature/feed` 使用 ViewPager2 + Paging3 + PlayerPool（含双击点赞、评论半屏、刷新/手势动画）。
- [ ] 横屏模式（Landscape）交互：亮度/音量/锁屏/倍速/清晰度菜单与 UI 状态管理。
- [ ] AI 评论助手（@元宝）与推荐闭环：定义请求/响应模型、埋点、降级策略及指标。

> 后续迭代中，请将具体任务拆分为更细粒度条目，并在完成后标记 `[x]`，附上日期与负责人。


