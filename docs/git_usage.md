# Git 使用说明文档

> 本文档面向 UCW-BEATU-SPEC 项目团队，说明如何使用 Git 子模块进行协作开发。

## 📋 目录

- [项目结构](#项目结构)
- [首次克隆项目](#首次克隆项目)
- [日常开发工作流](#日常开发工作流)
- [子模块操作指南](#子模块操作指南)
- [推送注意事项](#推送注意事项)
- [常见问题处理](#常见问题处理)
- [分支管理规范](#分支管理规范)

---

## 项目结构

本项目采用 **Git 子模块（Submodule）** 架构：

```
UCW-BEATU-SPEC (外层仓库)
├── .gitmodules          # 子模块配置文件
├── .cursorrules         # 项目规范
└── BeatU/               # 子模块（Android 项目）
    ├── app/
    ├── gradle/
    └── ...
```

**仓库信息：**
- **外层仓库**：`https://github.com/useroflrz/UCW-BEATU-SPEC.git`
- **子模块仓库**：`https://github.com/useroflrz/UCW-BEATU.git`

---

## 首次克隆项目

### 方法一：克隆时包含子模块（推荐）

```bash
# 克隆外层仓库并自动初始化子模块
git clone --recursive https://github.com/useroflrz/UCW-BEATU-SPEC.git

# 或者使用简写
git clone --recurse-submodules https://github.com/useroflrz/UCW-BEATU-SPEC.git
```

### 方法二：先克隆外层，再初始化子模块

```bash
# 1. 克隆外层仓库
git clone https://github.com/useroflrz/UCW-BEATU-SPEC.git
cd UCW-BEATU-SPEC

# 2. 初始化并更新子模块
git submodule update --init --recursive
```

### 切换到子模块的 main 分支

克隆完成后，**必须**手动切换到子模块的 `main` 分支：

```bash
# 进入子模块目录
cd BeatU

# 切换到 main 分支
git checkout main

# 确认分支
git branch
# 应该显示：* main

# 返回外层目录
cd ..
```

**⚠️ 重要：** 如果不切换到 `main` 分支，子模块会处于 "detached HEAD" 状态，后续提交可能丢失。

---

## 日常开发工作流

### 1. 更新项目（拉取最新代码）

#### 更新外层仓库

```bash
# 在外层目录执行
git pull origin main
```

#### 更新子模块

```bash
# 方法一：在外层目录更新所有子模块到最新提交
git submodule update --remote

# 方法二：进入子模块目录手动更新
cd BeatU
git pull origin main
cd ..
```

**推荐流程：**

```bash
# 1. 更新外层仓库
git pull origin main

# 2. 进入子模块
cd BeatU

# 3. 确保在 main 分支
git checkout main

# 4. 拉取最新代码
git pull origin main

# 5. 返回外层
cd ..
```

---

## 子模块操作指南

### 进入子模块进行开发

```bash
# 进入子模块目录
cd BeatU

# 确认当前分支（应该显示 main）
git branch

# 如果不是 main 分支，切换过去
git checkout main
```

### 在子模块中提交代码

```bash
# 1. 进入子模块目录
cd BeatU

# 2. 确保在 main 分支
git checkout main

# 3. 查看修改状态
git status

# 4. 添加文件到暂存区
git add .

# 5. 提交（按照提交规范）
git commit -m "feat: 添加视频播放功能"

# 6. 推送到子模块仓库
git push origin main

# 7. 返回外层目录
cd ..
```

### 更新外层仓库的子模块引用

在子模块中推送新提交后，需要更新外层仓库的子模块引用：

```bash
# 在外层目录执行
cd ..

# 查看子模块状态（应该显示子模块有新的提交）
git status
# 会显示类似：modified: BeatU (new commits)

# 添加子模块引用更新
git add BeatU

# 提交子模块引用更新
git commit -m "chore: 更新 BeatU 子模块到最新版本"

# 推送到外层仓库
git push origin main
```

**⚠️ 重要：** 如果只推送了子模块但忘记更新外层仓库的引用，其他团队成员无法获取到最新的子模块代码。

---

## 推送注意事项

### ✅ 正确的推送流程

```bash
# ========== 步骤 1: 推送子模块 ==========
cd BeatU

# 确保在 main 分支
git checkout main

# 添加、提交、推送子模块代码
git add .
git commit -m "feat: 你的功能描述"
git push origin main

cd ..

# ========== 步骤 2: 更新外层仓库引用 ==========
# 在外层目录添加子模块引用更新
git add BeatU

# 提交外层仓库
git commit -m "chore: 更新 BeatU 子模块到最新版本"

# 推送外层仓库
git push origin main
```

### ❌ 常见错误

1. **忘记切换到 main 分支**
   - 问题：在 "detached HEAD" 状态下提交，代码可能丢失
   - 解决：始终先 `git checkout main`

2. **只推送了子模块，忘记更新外层引用**
   - 问题：其他成员无法获取到最新代码
   - 解决：推送子模块后，必须在外层目录执行 `git add BeatU` 并提交推送

3. **在外层目录直接 `git add BeatU/`**
   - 问题：会破坏子模块结构
   - 解决：只能使用 `git add BeatU`（不带斜杠）来更新子模块引用

---

## 常见问题处理

### 问题 1: 子模块处于 "detached HEAD" 状态

**现象：**

```bash
cd BeatU
git status
# 显示：HEAD detached at abc1234
```

**解决：**

```bash
# 切换到 main 分支
git checkout main

# 如果 main 分支不存在，从远程创建
git checkout -b main origin/main
```

### 问题 2: 子模块代码落后

**现象：** 其他团队成员推送了新代码，你的子模块还是旧版本

**解决：**

```bash
cd BeatU
git checkout main
git pull origin main
cd ..
```

### 问题 3: 子模块有未提交的修改

**现象：**

```bash
git status
# 显示：modified: BeatU (modified content)
```

**原因：** 子模块目录中有未提交的修改

**解决：**

```bash
# 进入子模块查看并处理
cd BeatU
git status
git diff

# 选项 1: 提交修改
git add .
git commit -m "fix: 修复问题"
git push origin main

# 选项 2: 丢弃修改（危险操作，请谨慎）
git checkout .

cd ..
```

### 问题 4: 克隆后子模块目录为空

**现象：** `BeatU/` 目录存在但是空的

**原因：** 克隆时没有初始化子模块

**解决：**

```bash
# 初始化并更新子模块
git submodule update --init --recursive

# 进入子模块并切换分支
cd BeatU
git checkout main
cd ..
```

### 问题 5: 推送被拒绝

**原因：** 远程仓库有新的提交，需要先拉取

**解决：**

```bash
# 在子模块中
cd BeatU
git pull origin main
# 如果有冲突，解决冲突后再推送
git push origin main

# 在外层仓库中
cd ..
git pull origin main
git push origin main
```

---

## 分支管理规范

### 子模块分支策略

- **主分支：** `main`
- **功能分支：** 根据需要创建 `feature/xxx`
- **开发流程：**
  1. 从 `main` 创建功能分支
  2. 在功能分支上开发
  3. 完成后合并回 `main`
  4. 推送 `main` 分支

### 创建功能分支示例

```bash
# 进入子模块
cd BeatU

# 从 main 创建新分支
git checkout main
git pull origin main
git checkout -b feature/video-player

# 开发完成后，合并回 main
git checkout main
git merge feature/video-player
git push origin main

# 返回外层，更新子模块引用
cd ..
git add BeatU
git commit -m "chore: 更新 BeatU 子模块到 feature/video-player 合并后版本"
git push origin main
```

### 外层仓库分支策略

- **主分支：** `main`
- **工作流程：** 子模块更新后，及时更新外层仓库的子模块引用

---

## 快速参考命令

### 日常开发检查清单

```bash
# ✅ 1. 确认在正确的目录和分支
cd BeatU
git branch          # 应该显示: * main
git status          # 查看修改状态

# ✅ 2. 拉取最新代码
git pull origin main

# ✅ 3. 开发、提交、推送
git add .
git commit -m "feat: 功能描述"
git push origin main

# ✅ 4. 返回外层，更新子模块引用
cd ..
git add BeatU
git commit -m "chore: 更新 BeatU 子模块"
git push origin main
```

### 常用命令速查

| 操作 | 命令 |
|------|------|
| 克隆项目（包含子模块） | `git clone --recursive <url>` |
| 初始化子模块 | `git submodule update --init --recursive` |
| 进入子模块 | `cd BeatU` |
| 切换到 main 分支 | `git checkout main` |
| 更新子模块到最新 | `git submodule update --remote` |
| 查看子模块状态 | `git status`（在外层目录） |
| 查看子模块信息 | `git submodule status` |

---

## 团队协作最佳实践

1. **开发前先更新：** 每次开始工作前，先 `git pull` 更新代码
2. **及时推送：** 完成功能后及时推送，不要积压太多提交
3. **提交信息规范：** 使用清晰的提交信息（参考 Conventional Commits）
4. **保持同步：** 推送子模块后，立即更新外层仓库的引用
5. **定期同步：** 每天开始和结束工作时，都执行一次完整更新

---

## 提交信息规范建议

参考 [Conventional Commits](https://www.conventionalcommits.org/)：

```
<type>(<scope>): <subject>

<body>

<footer>
```

**常用 type：**
- `feat`: 新功能
- `fix`: 修复 bug
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 重构
- `test`: 测试相关
- `chore`: 构建/工具链相关

**示例：**

```bash
git commit -m "feat(video): 添加 ExoPlayer 播放器支持"
git commit -m "fix(player): 修复内存泄漏问题"
git commit -m "chore: 更新 BeatU 子模块到最新版本"
```

---

## 需要帮助？

如果遇到问题，请：
1. 先查看本文档的 [常见问题处理](#常见问题处理) 部分
2. 检查 Git 状态：`git status` 和 `git submodule status`
3. 联系团队成员或项目维护者

---

**文档更新时间：** 2025-01-XX  
**维护者：** UCW-BEATU 团队

