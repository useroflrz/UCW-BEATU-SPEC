# BeatU 快速上手指南

## 1. 项目结构

```
UCW-BEATU-SPEC/
├── BeatUClient/     # Android 客户端
├── BeatUBackend/    # FastAPI 后端
├── BeatUWeb/        # 前端（管理员 + 用户）
├── AgentMCP/        # 智能体子模块（可选）
└── docs/            # 文档
```

## 2. 后端启动（BeatUBackend）

```bash
cd BeatUBackend

# 1. 配置文件：.env 或 .env.mysql（后者优先）
cp .env.example .env
# 编辑 DATABASE_URL（默认 jeecg@192.168.1.206:3306/jeecg-boot3）

# 2. Conda 环境（必须）
conda env create -f environment.yml
conda activate beatu-backend

# 3. 初始化数据库
python -m database.init_db --drop

# 4. 启动（端口 9306）
uvicorn main:app --reload --host 0.0.0.0 --port 9306
```

- API 文档：http://localhost:9306/docs
- 健康检查：http://localhost:9306/health

## 3. 客户端启动（BeatUClient）

```bash
cd BeatUClient
./gradlew assembleDebug
# Windows: gradlew.bat assembleDebug
```

修改 `app/src/main/res/values/config.xml` 中 `base_url` 指向后端（开发可用 `http://127.0.0.1:9306/` 或局域网 IP）。

## 4. 前端启动（BeatUWeb）

```bash
cd BeatUWeb
npm install
npm run dev
```

默认端口 5174。API 代理到 `http://localhost:9306`，需先启动后端。

- **管理员**：`/admin/login` 登录后访问仪表盘、用户管理、视频管理
- **用户端**：`/` 首页、`/login` 登录、`/register` 注册

## 5. 核心文档

| 文档 | 说明 |
|------|------|
| [architecture.md](./architecture.md) | 架构与模块划分 |
| [api_reference.md](./api_reference.md) | API 接口与契约 |
| [development_plan.md](./development_plan.md) | 任务与开发计划 |

## 6. 开发流程

1. 任务登记在 `docs/development_plan.md`
2. 实现后更新架构/API 文档
3. 提交 PR 前检查 `.cursorrules` 约定
