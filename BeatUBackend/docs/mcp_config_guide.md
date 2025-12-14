# MCP 配置指南

## 概述

BeatUBackend 使用两类不同的服务，需要分别配置：

1. **LLM 服务**（大模型推理）：使用 OpenAI 兼容接口
2. **MCP 服务**（工具服务）：使用 IQS MCP Server

## 配置说明

### 1. LLM 配置（大模型服务）

用于 `ChatOpenAI` 的大模型推理，使用 OpenAI 兼容接口。

**配置项：**

```env
# LLM API Key（用于大模型推理，Authorization: Bearer）
LLM_API_KEY=sk-xxx
# 或使用 DASHSCOPE_API_KEY（通义千问推荐）
DASHSCOPE_API_KEY=sk-xxx

# LLM Base URL（OpenAI 兼容接口）
LLM_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1

# LLM Model（模型名称）
LLM_MODEL=qwen-flash
```

**说明：**
- `LLM_API_KEY` 或 `DASHSCOPE_API_KEY`：用于 `Authorization: Bearer` 请求头
- `LLM_BASE_URL`：OpenAI 兼容接口地址
- `LLM_MODEL`：模型名称（如 `qwen-flash`, `qwen-plus` 等）

**获取 API Key：**
- 访问 [阿里云百炼控制台](https://bailian.console.aliyun.com/?tab=model#/api-key)
- 创建或复制 API Key

### 2. MCP 配置（工具服务）

用于 `MultiServerMCPClient` 的 MCP 工具服务，使用 IQS MCP Server。

**配置项：**

```env
# MCP API Key（用于 IQS MCP Server 认证，X-API-Key 请求头）
MCP_API_KEY=your_iqs_api_key_here
```

**说明：**
- `MCP_API_KEY`：用于 `X-API-Key` 请求头，认证 IQS MCP Server
- MCP Server URL 在 `mcp_registry/` 目录下的配置文件中指定

**获取 MCP API Key：**
- 访问 [阿里云 IQS 控制台](https://iqs.console.aliyun.com/)
- 开通 IQS MCP Server 服务
- 创建或复制 API Key

## 完整配置示例

`.env` 文件示例：

```env
# ========== LLM 配置（大模型服务）==========
# 用于 ChatOpenAI 的大模型推理
LLM_API_KEY=sk-dc331c9519d847acba5a909b7f671f73
LLM_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
LLM_MODEL=qwen-plus

# ========== MCP 配置（工具服务）==========
# 用于 IQS MCP Server 的认证
MCP_API_KEY=your_iqs_api_key_here

# ========== 其他配置 ==========
DATABASE_URL=mysql+pymysql://jeecg:haomo123@192.168.1.206:3306/jeecg-boot3
```

## 向后兼容

为了兼容旧的配置方式，如果设置了 `MCP_API_KEY` 但没有设置 `LLM_API_KEY`，系统会尝试使用 `MCP_API_KEY` 作为 LLM API Key（向后兼容）。

**建议：** 明确区分 LLM 和 MCP 配置，使用新的 `LLM_*` 配置项。

## 配置优先级

配置的优先级（从高到低）：

1. **环境变量** - 系统环境变量中的值
2. **.env 文件** - 项目根目录下的 `.env` 文件
3. **默认值** - `core/config.py` 中定义的默认值

## 验证配置

重启服务后，查看日志确认配置是否正确：

```bash
conda activate beatu-backend
uvicorn main:app --reload --host 0.0.0.0 --port 9306
```

**预期日志：**
```
✅ LLM 配置已设置: BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1, MODEL=qwen-plus, API_KEY length=XX
✅ MCP API Key 已设置 (length: XX)
```

## 常见问题

### Q: LLM 和 MCP 可以使用同一个 API Key 吗？

A: 取决于您的 API Key 类型：
- 如果您的 API Key 同时支持 LLM 和 MCP 服务，可以使用同一个 Key
- 如果您的 API Key 只支持其中一种服务，需要分别配置

### Q: MCP_API_KEY 是必须的吗？

A: 不是必须的。如果您的 MCP Server 不需要认证，可以不配置 `MCP_API_KEY`。系统会记录警告，但不会阻止服务启动。

### Q: 如何确认配置是否正确？

A: 查看服务启动日志，确认：
1. LLM 配置已设置（必须有）
2. MCP API Key 已设置（可选，取决于您的 MCP Server）

## 参考文档

- [通义千问 API 文档](https://help.aliyun.com/zh/model-studio/developer-reference/api-details-9)
- [IQS MCP Server 接入指南](https://help.aliyun.com/document_detail/2881063.html)

