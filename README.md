## BeatU 项目总览

- **目标**：沉浸式短视频流 App 及配套服务（AIService、ContentService、Gateway、Observability），重点展示架构设计、性能优化、交互体验和 AI 融合能力。
- **当前阶段**：同步最新需求文档，完善架构/规范，后续迭代将按照需求逐步实现客户端与服务端能力。

### 仓库结构（顶层）

- `BeatUClient/`：Android 客户端 App（Kotlin + Jetpack View + 原生 View 系统 + ExoPlayer，**不使用 Jetpack Compose**）。
- `BeatUAIService/`：AI 能力相关服务（推荐、清晰度调节、语音识别等）。
- `BeatUContentService/`：内容管理与视频资源服务。
- `BeatUGateway/`：网关/聚合层服务。
- `BeatUObservability/`：观测性与监控相关组件。
- `docs/`：跨项目文档（架构、API、开发计划、上手指南等）。

详细架构和模块划分请参考 `docs/architecture.md` 与 `docs/development_plan.md`。

### 最新需求与亮点对齐

- **需求来源**：`BeatUClient/docs/需求.md`（配套原型/流程图位于 `BeatUClient/docs/` 子目录），描述了 Feed 交互、横屏模式、AI 评论助手等完整 PRD。
- **客户端技术要点**：
  - MVVM + Clean Architecture，`ViewPager2` 纵向滑动承载视频流，ExoPlayer（Media3）作为统一播放器，支持预加载与 1~3 实例复用池。
  - 手势体系：双击点赞、长按倍速/Seek、横向频道切换、评论半屏弹层、横屏亮度/音量/锁屏手势等。
  - NFR：首帧 < 500 ms、FPS 55~60、播放成功率 ≥ 99%、卡顿率 < 2%、冷启动 < 2.5 s。
- **AI 能力方向**：优先验证“AI 内容理解/推荐 + 评论区 @元宝 问答”，并在后续迭代扩展到自适应清晰度或实时字幕。数据流/接口约束见 `docs/architecture.md` 与 `docs/api_reference.md`。

### 文档驱动工作流

所有需求、设计、开发、测试、性能与文档更新必须遵循 `.cursorrules` 约定：任务先登记在 `docs/development_plan.md`，实现后更新架构/API/上手文档，并附可量化指标与证据。


