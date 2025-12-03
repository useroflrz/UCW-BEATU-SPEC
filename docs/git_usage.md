# Git 使用规范

> 目标：统一 UCW-BEATU-SPEC 仓库的 Git 协作方式。自 2025-11-20 起，`BeatUClient/` 作为父仓库中的常规目录维护，不再使用 Git 子模块；所有成员直接在同一仓库提交代码。
> 
> **注意**：`AgentMCP/` 是一个 Git 子模块（用于 MCP 智能体系统），需要单独初始化，详见下方说明。

---

## 1. 仓库结构与职责

```
UCW-BEATU-SPEC/
├── BeatUClient/         # Android 客户端（多 Gradle Module）
├── BeatUBackend/        # 后端服务（FastAPI）
├── AgentMCP/            # MCP 智能体系统（Git 子模块）
├── docs/                # 所有跨项目文档
└── ...
```

- **统一仓库**：大部分代码直接托管在 `UCW-BEATU-SPEC`，无需额外克隆子仓库。
- **AgentMCP 子模块**：`AgentMCP/` 是一个 Git 子模块，需要单独初始化（见下方说明）。
- **Gradle Module ≠ Git 子模块**：`BeatUClient` 内部的 `shared/*`、`business/*` 均通过 Gradle 子模块实现，与 Git 无关。
- **协作约束**：持续遵循 `.cursorrules` 的文档驱动流程，开发前后务必同步 `docs/*`。

---

## 2. 初次克隆与更新

### 2.1 克隆仓库

```bash
# 克隆仓库（包含子模块）
git clone --recurse-submodules https://github.com/useroflrz/UCW-BEATU-SPEC.git

# 或者先克隆，再初始化子模块
git clone https://github.com/useroflrz/UCW-BEATU-SPEC.git
cd UCW-BEATU-SPEC
git submodule update --init --recursive AgentMCP
```

### 2.2 更新代码

```bash
# 拉取最新代码
git pull origin main

# 更新子模块（如果需要）
git submodule update --remote AgentMCP
```

- **AgentMCP 子模块**：首次克隆后需要初始化，后续更新时如需获取子模块最新代码，使用 `git submodule update --remote AgentMCP`。
- **其他目录**：更新时只需 `git pull`，即可获取 `BeatUClient`、`BeatUBackend` 与其它目录的最新修改。

---

## 3. 日常开发流程（Git Flow / GitHub Flow）

1. **同步主分支**
   ```bash
   git checkout main
   git pull origin main
   ```
2. **创建功能分支**
   ```bash
   git checkout -b feature/feed-player-pool
   ```
3. **开发与提交**
   ```bash
   git status
   git add BeatUClient/business/videofeed/...
   git commit -m "feat(feed): 新增播放器复用池骨架"
   ```
4. **推送远端**
   ```bash
   git push origin feature/videofeed-player-pool
   ```
5. **提 PR & 更新文档**
   - PR 描述列出覆盖模块、测试结果、性能/AI 指标。
   - 若涉及架构/接口/流程，必须同步更新 `docs/architecture.md`、`docs/api_reference.md`、`docs/development_plan.md`。
6. **合并回 main**
   - 通过 GitHub PR 合并，或在本地 rebase → fast-forward。

---

## 4. BeatUClient 协作要点

- `BeatUClient` 直接受主仓库版本控制，`git status` 会显示其内部所有文件的改动。
- 提交前务必在仓库根目录检查状态，避免遗漏 `BeatUClient` 内部更新。
- 历史记录直接使用 `git log -- BeatUClient/...` 查询，无需再进入独立仓库。
- 若需只关注客户端，可使用 `git sparse-checkout` 或 IDE 的 working set 功能，但推送前必须同步根目录。

---

## 5. 常用命令速查

| 场景 | 命令 |
| --- | --- |
| 查看当前状态 | `git status` |
| 查看 BeatUClient 变更 | `git status BeatUClient` |
| 仅暂存客户端改动 | `git add BeatUClient/...` |
| 丢弃客户端未暂存修改 | `git restore BeatUClient/...` |
| 查看单个文件历史 | `git log -- BeatUClient/app/src/...` |
| 分支差异对比 | `git diff main...feature/videofeed-player` |

---

## 6. 常见问题 & 解决方案

### Q1：`BeatUClient` 不再是子模块，会影响历史吗？
- 不会。删除 `.gitmodules` 后，Git 把 `BeatUClient/` 视为普通目录。旧提交中若存在子模块引用，在历史回溯时可能提示“已移除”，但不会影响当前开发。

### Q2：本地仍然存在 `.gitmodules` 或 `.git/modules`？
- 删除这些遗留文件夹并重新 `git pull`。确保 `BeatUClient/` 内 **没有** 独立 `.git`。

### Q3：如何保持客户端与其它目录同步？
- 在仓库根目录执行 `git pull` 即可，不再需要“更新子模块引用”。

### Q4：还能使用 `git submodule` 命令吗？
- `AgentMCP/` 是一个 Git 子模块，可以使用 `git submodule` 命令管理。执行 `git submodule status` 应该显示 `AgentMCP` 子模块的状态。

### Q5：如何初始化 AgentMCP 子模块？
- 如果克隆时未使用 `--recurse-submodules`，可以执行：
  ```bash
  git submodule update --init --recursive AgentMCP
  ```
- 初始化后，进入 `AgentMCP/` 目录，按照其 `README.md` 中的说明创建虚拟环境并安装依赖。

---

## 7. 提交流程 Checklist

1. `git status` 确认只有预期文件变动。
2. `./gradlew lintDebug testDebug`（或至少 `assembleDebug`）跑通。
3. 结合 `.cursorrules` 完成文档回填（开发计划、架构、API、README 等）。
4. 提交信息遵循 `<type>(scope): description`，示例：`chore(git): 移除 BeatUClient 子模块配置`。
5. 推送功能分支并创建 PR，附性能/AI 指标或 profiler 截图。

---

## 8. 迁移记录（2025-11-20）

- 删除 `.gitmodules`，清理所有子模块配置。
- `BeatUClient/` 直接纳入父仓库版本控制，所有提交通过常规 Git 流程完成。
- `docs/git_usage.md` 改为“统一仓库协作流程”，与 `README.md`、`docs/architecture.md` 要求保持一致。

---

## 9. 参考命令示例

```bash
# 查看是否仍存在子模块配置
git config --show-origin --get-regexp submodule || echo "no submodule"

# 仅提交客户端与文档
git add BeatUClient docs
git commit -m "feat(videofeed): 完成播放器池 + 更新文档"

# 推送与同步
git push origin feature/videofeed-player
git checkout main
git pull origin main
```

---

## 10. FAQ（持续补充）

| 问题 | 结论 |
| --- | --- |
| 需要单独初始化 BeatUClient 吗？ | 不需要，克隆仓库即包含全部文件。 |
| 可以把 BeatUClient 拆到独立仓库吗？ | 如需拆分，请先在 `docs/development_plan.md` 登记并经评审确认。 |
| 如何防止误删其它目录？ | 可使用 `git sparse-checkout` 或 IDE working set，但提交前务必检查仓库根目录状态。 |

---

> 若在使用 Git 过程中遇到新的问题，请在 `docs/git_usage.md` 中补充场景，同时在 `docs/development_plan.md` 记录变更并链接相关 PR。
