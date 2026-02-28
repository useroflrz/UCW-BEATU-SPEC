# 线程兼容层治理清单（最终去兼容计划）

## 分级规则

- 可删：当前无业务引用，删除后不影响主链路
- 待删：仍有调用，但存在明确替代路径
- 长期保留：主链路高频依赖，短期不建议迁移

## 可删（本批已执行）

- `BeatUBackend/core/security.py` 中旧函数 `parse_bearer_token()`：已无引用，已删除。
- `BeatUClient/app/src/main/java/com/ucw/beatu/auth/AppCurrentUserProvider.kt`：未被 DI 绑定，已删除。
- `BeatUClient/shared/common/src/main/java/com/ucw/beatu/shared/common/mock/`：无业务引用，已删除目录内 mock 文件。

## 待删（需分批替换）

- `BeatUClient/shared/common/src/main/java/com/ucw/beatu/shared/common/threading/SuspendBridge.kt`
  - 现状：多模块仍直接调用。
  - 替代：收敛为统一 `ThreadTaskManager` 模板调用，避免桥接层扩散。
- `BeatUClient/shared/common/src/main/java/com/ucw/beatu/shared/common/threading/LifecycleFlowObserver.kt`
  - 现状：主要通过 `ThreadTaskPatterns.collectUiState` 间接使用。
  - 替代：逐步改为统一 UI 订阅模板后再下线底层 observer。
- `BeatUClient/shared/common/src/main/java/com/ucw/beatu/shared/common/reflect/ReflectionBridge.kt`
  - 现状：`search`、`landscape` 仍有反射创建链路。
  - 替代：补统一 router 契约后再替换。

## 长期保留（当前版本）

- `BeatUClient/shared/common/src/main/java/com/ucw/beatu/shared/common/threading/ThreadTaskManager.kt`
- `BeatUClient/shared/common/src/main/java/com/ucw/beatu/shared/common/threading/ThreadObservable.kt`
- `BeatUClient/shared/common/src/main/java/com/ucw/beatu/shared/common/threading/ThreadTaskPatterns.kt`

原因：调用面广，短期删除会引入大范围回归；应在后续版本通过“先替换调用、后删底座”的方式处理。

## 删除前置条件（统一）

1. 全局调用数降为 0（代码与资源双检查）。
2. 客户端关键模块编译通过 + `ReadLints` 无新增错误。
3. 主流程回归通过：刷视频、搜索、登录注册、发布。
4. 文档同步：`docs/development_plan.md` 与 `docs/architecture.md` 标注治理状态。

