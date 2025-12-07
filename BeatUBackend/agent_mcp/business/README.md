# AgentMCP 业务层

## 概述

业务层模块提供了面向特定业务场景的高级功能封装，将底层的 MCP 框架能力转化为可直接使用的业务服务。

## 模块列表

### AI 搜索 (`ai_search`)

提供完整的 AI 搜索功能，支持：
- 流式 AI 回答生成
- 关键词自动提取
- 本地数据库视频搜索
- 远程数据库视频搜索

详细文档请参考：[ai_search/README.md](ai_search/README.md)

## 使用方式

### 1. 直接使用服务

```python
from src.business.ai_search import AISearchService, AISearchRequest
import asyncio

async def main():
    service = AISearchService()
    
    request = AISearchRequest(user_query="我想看一些搞笑视频")
    response = await service.search(request)
    
    print(f"AI 回答: {response.ai_answer}")
    print(f"关键词: {response.keywords}")
    print(f"视频 ID: {response.video_ids}")
    
    service.close()

asyncio.run(main())
```

### 2. 集成到 FastAPI

```python
from fastapi import FastAPI
from src.business.ai_search.api import router as ai_search_router

app = FastAPI()
app.include_router(ai_search_router)
```

## 架构设计

业务层遵循以下设计原则：

1. **模块化**：每个业务功能独立封装，互不干扰
2. **可扩展**：易于添加新的业务模块
3. **统一接口**：提供一致的调用方式
4. **资源管理**：自动管理数据库连接等资源

## 扩展开发

### 添加新业务模块

1. 在 `src/business/` 下创建新目录
2. 实现以下文件：
   - `models.py`：数据模型
   - `service.py`：核心服务逻辑
   - `__init__.py`：模块导出
3. 可选文件：
   - `database.py`：数据库相关
   - `api.py`：FastAPI 集成
   - `README.md`：模块文档

### 示例结构

```
src/business/
├── new_module/
│   ├── __init__.py
│   ├── models.py
│   ├── service.py
│   └── README.md
└── README.md
```

## 注意事项

1. **资源管理**：使用完毕后记得调用 `service.close()` 关闭资源
2. **错误处理**：所有服务方法都包含异常处理，返回合理的错误信息
3. **配置管理**：通过环境变量配置数据库连接等参数

