## Getting Started（上手指南）

> 阅读顺序：先通读 `.cursorrules` 与 `BeatUClient/docs/需求.md`，再根据本指南准备环境、运行并校验关键指标。

### 1. 开发环境要求

- Android Studio Koala+ (2024.1) 或 IntelliJ with Android 插件，Gradle 8.7 / AGP 8.6.1。
- JDK 17（推荐使用 `Microsoft OpenJDK 17`）。
- Android SDK 35（至少安装 34）与 NDK 26.x；需要启用 `Android Emulator`、`Google Play services`。
- 必须安装 `adb`, `platform-tools` 并确保在 `PATH` 中。
- **技术栈说明**：本项目使用**原生 View 系统**（TextView、ImageView、RecyclerView 等）+ **Jetpack View 组件**（ViewPager2、Navigation、MotionLayout）+ 传统 XML 布局，**不使用 Jetpack Compose**。

### 2. 仓库结构

- `BeatUClient/`：Android 客户端。
- `BeatUAIService/`、`BeatUContentService/`、`BeatUGateway/`、`BeatUObservability/`：后端与观测性相关工程。
- `docs/`：统一文档。
- `BeatUClient/docs/`：客户端需求/原型/流程图资料，`需求.md` 为最新 PRD。

### 3. Android 客户端（BeatUClient）运行步骤

1. 在终端执行 `cd /d D:\Projects\andriod`（保持会话）并克隆/更新仓库。
2. 打开 `BeatUClient` 目录作为 Android Studio 工程，校验 `local.properties` 中的 SDK 路径。
3. 执行 `./gradlew clean assembleDebug`（Windows 可运行 `gradlew.bat clean assembleDebug`）验证构建。
4. 准备一台 8GB+ 内存的模拟器（Pixel 6 / Android 14）或真机，启用 60fps/120fps 显示。
5. 运行 `app` 模块，验证启动流程（Logo → Loading → Feed），记录首帧时间、FPS、内存占用等基线指标。
6. 若需复现 AI 评论/推荐链路，可通过 `BeatUAIService` 的 Mock API，自定义 `baseUrl` 于 `local.properties`。

### 4. 文档导航

- 架构说明：`docs/architecture.md`
- API 说明：`docs/api_reference.md`
- 开发计划：`docs/development_plan.md`
- Git 使用规范：`docs/git_usage.md`
- 需求/原型：`BeatUClient/docs/需求.md` + `BeatUClient/docs/原型图|流程图|UI`

### 5. 日常协作 checklist

1. 需求登记：在 `docs/development_plan.md` 中创建条目，链接到具体原型或章节。
2. 设计评审：同步到 `docs/architecture.md`，标注模块、数据流、性能/AI指标。
3. 开发 & 自测：遵循 Clean Architecture、模块边界、ktlint；产出 profiler 截图。
4. 文档回填：更新 README/Getting Started/API Reference/Architecture，确保 `.cursorrules` 要求满足。
5. 提交/PR：说明覆盖点、性能收益、AI 触达率，附测试与截图。


