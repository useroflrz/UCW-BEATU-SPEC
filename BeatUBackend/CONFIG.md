# 配置文件说明

## 概述

BeatU Backend 使用 `.env` 文件来管理配置，包括数据库连接、Redis连接等敏感信息。

**重要**：修改配置和运行服务时，请确保已激活 conda 虚拟环境 `beatu-backend`：

```bash
conda activate beatu-backend

uvicorn main:app --reload --host 0.0.0.0 --port 9306

```

## 配置步骤

### 1. 创建配置文件

复制 `.env.example` 文件为 `.env`：

```bash
cd BeatUBackend
cp .env.example .env
```

### 2. 编辑配置

打开 `.env` 文件，根据实际环境修改配置值：

```bash
# 数据库配置示例
DATABASE_URL=mysql+pymysql://用户名:密码@主机:端口/数据库名

# 例如：
DATABASE_URL=mysql+pymysql://jeecg:haomo123@192.168.1.206:3306/jeecg-boot3
```

### 3. 配置项说明

| 配置项 | 说明 | 默认值 | 示例 |
|--------|------|--------|------|
| `PROJECT_NAME` | 项目名称 | BeatU Backend | - |
| `VERSION` | 版本号 | 0.1.0 | - |
| `DEBUG` | 调试模式 | False | True/False |
| `DATABASE_URL` | 数据库连接URL | mysql+pymysql://jeecg:haomo123@192.168.1.206:3306/jeecg-boot3 | mysql+pymysql://user:pass@host:port/db |
| `REDIS_URL` | Redis连接URL | redis://localhost:6379/0 | redis://host:port/db |
| `API_KEY` | API密钥 | dev-key | - |
| `MCP_API_KEY` | MCP LLM API Key（用于 AgentMCP） | 空 | 你的 LLM API Key |
| `MCP_BASE_URL` | MCP LLM Base URL | https://dashscope.aliyuncs.com/compatible-mode/v1 | LLM 服务地址 |
| `MCP_MODEL` | MCP LLM Model | qwen-flash | 模型名称 |
| `MCP_REGISTRY_PATH` | MCP 注册表路径 | 空（默认使用 BeatUBackend/mcp_registry） | 自定义路径 |

### 4. 配置优先级

配置的优先级（从高到低）：
1. **环境变量** - 系统环境变量中的值
2. **.env 文件** - 项目根目录下的 `.env` 文件
3. **默认值** - `core/config.py` 中定义的默认值

### 5. 使用环境变量（推荐生产环境）

在生产环境中，建议使用环境变量而不是 `.env` 文件：

```bash
# 激活 conda 环境
conda activate beatu-backend

# 设置环境变量
export DATABASE_URL="mysql+pymysql://user:pass@host:port/db"
export DEBUG="False"

# 启动服务
uvicorn main:app --reload --host 0.0.0.0 --port 9306
```

## 安全注意事项

⚠️ **重要**：
- `.env` 文件包含敏感信息，**不要**提交到版本控制系统
- `.env` 文件已在 `.gitignore` 中，确保不会被意外提交
- 生产环境建议使用环境变量或密钥管理服务（如 AWS Secrets Manager）

## 验证配置

启动服务后，可以通过以下方式验证配置：

1. 查看日志输出，确认数据库连接成功
2. 访问 API 文档：http://localhost:9306/docs
3. 测试 API 接口是否正常工作

## 常见问题

### Q: 修改配置后不生效？
A: 
1. 确认已激活 conda 环境：`conda activate beatu-backend`
2. 需要重启服务才能生效。如果使用 `--reload` 参数，修改代码会自动重启，但修改 `.env` 文件需要手动重启。

### Q: 如何查看当前使用的配置？
A: 可以在代码中打印 `settings` 对象，或查看服务启动日志。

### Q: 开发环境和生产环境使用不同配置？
A: 可以创建多个 `.env` 文件（如 `.env.dev`, `.env.prod`），然后通过环境变量 `ENV_FILE` 指定要加载的文件，或直接使用环境变量。

