# BeatU 20 天分工开发流程（独立功能模式）

> 目标：在 20 个工作日内，按照“每人负责一个完整功能”的方式推进 BeatU 客户端与配套服务的 MVP。遵循 `.cursorrules` 的文档驱动链路，并持续量化播放/AI 指标。

## 0. 基本原则

- **文档先行**：所有任务先登记 `docs/development_plan.md`，完成后同步 README / Architecture / API / Getting Started。
- **Clean Architecture**：严格按照 Presentation → Domain → Data → Core 划分；任何跨层依赖需在 `docs/architecture.md` 记录。
- **量化指标**：至少跟踪首帧耗时、FPS、播放成功率、AI 触达率。阶段评审需提供 Profiler/埋点截图。
- **协作机制**：每日 Standup + 晚间代码 Review；每个 Feature Owner 维护自己模块的 README（位于模块 `docs/` 子目录）。

## 1. 角色与分工（3 人版本）

| 角色 | 负责模块 | 主要交付物 |
| --- | --- | --- |
| Owner-A（体验负责人） | `feature/feed`、`feature/profile`、交互/动画 | 纵向 Feed、双击点赞、个人主页与关注/收藏、预加载交互文档 |
| Owner-B（平台负责人） | `core/player`、`core/common`、`core/network`、`data`、`domain` | `VideoPlayer` 抽象、PlayerPool、Retrofit/Room 基建、Repository/UseCase、Metrics SDK |
| Owner-C（AI&服务负责人） | `feature/aiassistant`、`BeatUAIService` Mock、AI KPI、Observability/埋点 | AI 评论与推荐链路、Mock 服务、AI 指标面板、性能/AI 上报接口 |

> 3 人小组保持“1（体验）+1（平台）+1（AI&服务）”格局，如需新增能力（如 Landscape）再在 Owner-A 轨道上拆分子任务。

## 2. 20 天节奏规划

| Day | Owner-A（体验） | Owner-B（平台） | Owner-C（AI&服务） | 联合交付 / 验收 |
| --- | --- | --- | --- | --- |
| Day 1 | 梳理 Feed/个人主页 PRD，列关键交互与指标。 | 盘点核心模块依赖，确认 Gradle/DI 约束。 | 明确 AI 能力范围、数据需求与降级策略。 | Kickoff 纪要；`docs/development_plan.md` 登记子任务。 |
| Day 2 | 输出 Feed/个人主页 UX 草图与状态机；列动画/手势清单。 | 撰写 `VideoPlayer` 架构方案（类图、状态机、ExoPlayer 配置）。 | 起草 AI 评论/推荐数据流与 Mock 设计。 | 上传三份设计草案；更新 `docs/architecture.md` 相关段落。 |
| Day 3 | 定义 Feed Fragment 结构、ViewPager2 行为、UI 状态。 | 设计 `core/network` + `core/database`（Retrofit/OkHttp/Room schema）。 | 整理 AI API 契约、错误码、埋点需求。 | 设计评审会议；`docs/api_reference.md` 补充接口。 |
| Day 4 | 完成 Feed/ViewModel 状态图 & Interaction Spec。 | 搭建 `core/player` 基础代码骨架（接口 + DI）。 | 创建 AI Mock Server 脚手架（Swagger/JSON）。 | 代码仓编译通过；Mock 服务 README。 |
| Day 5 | 开始实现 Feed UI（骨架 + 占位数据）。 | 实现 `VideoPlayer` 基础播放、生命周期单测。 | 完成本地 AI Mock 首个接口（评论）。 | `./gradlew test` 截图；假数据 Feed Demo。 |
| Day 6 | 增加 Feed 手势（单击/双击/长按）、埋点接口。 | 搭建 `core/network` Retrofit Builder + 拦截器。 | 搭建 `BeatUAIService` Mock 启动脚本 + README。 | 手势演示视频；网络模块文档。 |
| Day 7 | 准备个人主页 UI 草图与数据模型。 | 配置 `core/database`（Entity/Dao）并通过单测。 | 实现 AI 推荐 Mock + SDK 封装接口。 | DB 单测报告；AI Mock Swagger。 |
| Day 8 | 将 Feed 接入 Paging3（使用假数据源）。 | 实现 Repository 接口与 UseCase 壳（Feed/Interaction/Ai）。 | 编写 AI Repository 适配器 + 降级策略说明。 | Domain/Data 编译通过；流程图更新。 |
| Day 9 | 联调 Feed + Repository（假/Mock 数据），输出 UI State 文档。 | 将 PlayerPool 接入 Feed 的页面切换事件。 | 为 AI 请求添加埋点、记录响应耗时。 | 首帧基线初稿；埋点配置。 |
| Day 10 | 完成点赞/收藏按钮行为与乐观 UI。 | 优化 PlayerPool（预加载、Surface 复用）并记录性能。 | 打通 AI 评论 UI 与 Mock 服务，提供 Demo。 | 性能截图；AI 评论演示。 |
| Day 11 | 启动个人主页开发（Tab + RecyclerView）。 | 接入真实/Mock Feed API，通过 Paging3 拉流。 | 完成推荐接口联调，更新推荐触达率指标。 | API 日志；推荐日志报表。 |
| Day 12 | 实现关注/收藏列表 UI，与 Feed 共用数据层。 | 扩展 Interaction Repository 支持关注/收藏 API。 | 设计 AI KPI Dashboard（数据结构/可视化需求）。 | 交互录像；KPI 文档。 |
| Day 13 | 打磨 UI 细节（动画、动效、Loading 状态）。 | 接入 Metrics SDK（FPS、首帧、卡顿）采集。 | 将 AI 指标写入 Observability（或日志）。 | Metrics 原型；AI 日志截图。 |
| Day 14 | 编写个人主页/Feed 模块 README，列测试用例。 | 完成核心模块自测（UseCase/Repository 单测 ≥70%）。 | 实现 AI 降级提示与用户反馈 UI。 | 单测报告；AI 降级演示。 |
| Day 15 | 联调 Feed 与个人主页（关注跳转、收藏回流）。 | 与 Owner-A 合作修正播放器/交互性能问题。 | 记录 AI 评论/推荐成功率并撰写分析。 | 联调清单；指标表格。 |
| Day 16 | 演练弱网、横屏场景，补充容错逻辑。 | 完善 Metrics 上报接口 `/metrics/playback`。 | 将 AI & 性能指标上报接入 Mock Observability。 | 接口合同更新；日志截图。 |
| Day 17 | 准备用户旅程录屏（Feed→AI→个人主页）。 | 处理剩余技术债（Memory leak、线程）。 | 优化 AI Mock 性能，支持队列/延迟配置。 | 自测 checklist；Mock release note。 |
| Day 18 | 端到端联调（Feed/Player/AI/Profile），记录问题清单。 | 统一修复核心缺陷并复测性能。 | 校验埋点/指标齐全，生成整合报告。 | 集成自测报告；JIRA/Issue 状态。 |
| Day 19 | 彩排 Demo：体验路径、性能展示、AI 指标讲解。 | 准备 Profiler 截图、基准表，确认冷启动指标。 | 汇总 AI 成果（触达率、延迟），制作图表。 | Demo 脚本；性能/AI 报告草稿。 |
| Day 20 | 全量文档回填（README/Getting Started/Architecture/Plan）。 | 归档代码、打标签、准备 PR 描述。 | 输出亮点清单（AI、性能、交互）与后续计划。 | `docs/` 更新记录；最终 Demo 包 + 亮点列表。 |

## 3. 评审节点

1. **设计评审（Day 4 结束）**：确认核心方案、接口契约、AI 数据流。
2. **中期评审（Day 12 结束）**：展示 Feed MVP、PlayerPool、AI Mock，检查指标采集。
3. **集成评审（Day 18）**：完成端到端体验与埋点校验。
4. **终验（Day 20）**：提交 Demo、性能表、AI 指标，准备 PR/评审材料。

## 4. 指标与交付物清单

- **性能**：首帧 < 500ms（目标）、FPS ≥ 55、卡顿率 < 2%、冷启动 < 2.5s。
- **AI**：AI 评论触达率 ≥ 80%、推荐点击率 ≥ 25%、AI 响应延迟 < 1.2s。
- **文档**：所有模块必须在 `docs/architecture.md`、`docs/api_reference.md`、模块自带 README 中有记录；计划完成后需在 `docs/development_plan.md` 标记 `[x]` 并附日期/负责人。
- **测试**：核心 UseCase/Repository 单测覆盖率 ≥ 70%；Presentation 层关键交互提供 Espresso/Robolectric 覆盖或录屏演示。

## 5. 后续拓展

- Day 20 之后可切入 `feature/landscape`、AI 清晰度、Observability 指标面板。
- 将 Profiler 数据与埋点上传至 `BeatUObservability` 仓库，并在下一轮迭代中制定 CI/Lint 门槛。

> 该文档会随着阶段推进不断更新，请在每次评审后补充实际进度与指标。

