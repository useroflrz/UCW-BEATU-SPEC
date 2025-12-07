# AI 搜索业务层

## 概述

AI 搜索业务层提供了完整的 AI 搜索功能，支持：
- 流式 AI 回答生成
- 关键词自动提取
- 本地数据库视频搜索
- 远程数据库视频搜索

## 功能特性

1. **流式回答生成**：支持实时流式输出 AI 回答，提升用户体验
2. **智能关键词提取**：从用户查询和 AI 回答中自动提取关键词
3. **多数据库支持**：同时支持本地 SQLite 和远程 MySQL 数据库查询
4. **统一接口**：提供同步和异步两种调用方式

## 目录结构

```
src/business/ai_search/
├── __init__.py          # 模块导出
├── models.py            # 数据模型
├── database.py          # 数据库管理
├── service.py           # 核心服务
├── api.py               # FastAPI 集成接口
├── example.py           # 使用示例
└── README.md            # 本文档
```

## 快速开始

### 1. 环境配置

确保已安装所需依赖：

```bash
pip install -r requirements.txt
```

### 2. 数据库配置

通过环境变量配置数据库连接：

```bash
# 本地数据库（SQLite）
export LOCAL_DB_PATH="beatu.db"

# 远程数据库（MySQL）
export REMOTE_DB_URL="mysql+pymysql://root:haomo123@192.168.1.206:3306/jeecg-boot3"
```

### 3. 基本使用

#### 同步搜索

```python
from src.business.ai_search import AISearchService, AISearchRequest
import asyncio

async def main():
    service = AISearchService()
    
    request = AISearchRequest(user_query="我想看一些搞笑视频")
    response = await service.search(request)
    
    print(f"AI 回答: {response.ai_answer}")
    print(f"关键词: {response.keywords}")
    print(f"本地视频 ID: {response.local_video_ids}")
    print(f"远程视频 ID: {response.video_ids}")
    
    service.close()

asyncio.run(main())
```

#### 流式搜索

```python
from src.business.ai_search import AISearchService, AISearchRequest
import asyncio

async def main():
    service = AISearchService()
    
    request = AISearchRequest(user_query="我想看一些美食视频")
    
    async for chunk in service.search_stream(request):
        if chunk.chunk_type == "answer":
            print(chunk.content, end="", flush=True)
        elif chunk.chunk_type == "keywords":
            keywords = json.loads(chunk.content)
            print(f"\n关键词: {keywords}")
        elif chunk.chunk_type == "video_ids":
            video_ids = json.loads(chunk.content)
            print(f"视频 ID: {video_ids}")
    
    service.close()

asyncio.run(main())
```

## API 接口

### 数据模型

#### AISearchRequest

```python
class AISearchRequest(BaseModel):
    user_query: str  # 用户查询文本（1-500 字符）
```

#### AISearchResponse

```python
class AISearchResponse(BaseModel):
    ai_answer: str              # AI 生成的文本回答
    keywords: List[str]         # 提取的关键词列表
    video_ids: List[str]        # 远程数据库的视频 ID 列表
    local_video_ids: List[str]   # 本地数据库的视频 ID 列表
```

#### StreamChunk

```python
class StreamChunk(BaseModel):
    chunk_type: str   # 数据块类型：answer/keywords/video_ids/error
    content: str      # 数据块内容
    is_final: bool    # 是否为最终数据块
```

### 服务方法

#### `search(request: AISearchRequest) -> AISearchResponse`

同步搜索方法，返回完整的搜索结果。

#### `search_stream(request: AISearchRequest) -> AsyncGenerator[StreamChunk, None]`

流式搜索方法，异步生成搜索结果数据块。

## FastAPI 集成

### 1. 在 FastAPI 应用中注册路由

```python
from fastapi import FastAPI
from src.business.ai_search.api import router as ai_search_router

app = FastAPI()
app.include_router(ai_search_router)
```

### 2. API 端点

#### POST `/ai-search/search`

同步搜索接口，返回完整的搜索结果。

**请求体**：
```json
{
  "user_query": "我想看一些搞笑视频"
}
```

**响应**：
```json
{
  "ai_answer": "我为您推荐一些搞笑视频...",
  "keywords": ["搞笑", "视频", "娱乐"],
  "video_ids": ["video_001", "video_002"],
  "local_video_ids": ["local_001"]
}
```

#### POST `/ai-search/search/stream`

流式搜索接口，返回 Server-Sent Events (SSE) 格式的流式响应。

**请求体**：
```json
{
  "user_query": "我想看一些美食视频"
}
```

**响应**（SSE 格式）：
```
data: {"chunk_type": "answer", "content": "我", "is_final": false}

data: {"chunk_type": "answer", "content": "为", "is_final": false}

data: {"chunk_type": "keywords", "content": "[\"美食\", \"视频\"]", "is_final": true}

data: {"chunk_type": "video_ids", "content": "[\"video_001\"]", "is_final": true}
```

## 工作原理

1. **用户查询输入**：接收用户的自然语言查询
2. **AI 回答生成**：使用 LLM 生成自然语言回答（支持流式输出）
3. **关键词提取**：从用户查询和 AI 回答中提取关键词
4. **数据库查询**：
   - 使用关键词在本地数据库（SQLite）中搜索视频
   - 使用关键词在远程数据库（MySQL）中搜索视频
5. **结果返回**：返回 AI 回答、关键词和视频 ID 列表

## 配置说明

### 数据库配置

- **本地数据库**：默认使用项目根目录下的 `beatu.db` SQLite 数据库
- **远程数据库**：默认连接到 `192.168.1.206:3306` 的 MySQL 数据库

### LLM 配置

LLM 配置通过环境变量设置（参考 `src/utils/llm_utils.py`）：

```bash
export API_KEY="your_api_key"
export BASE_URL="https://dashscope.aliyuncs.com/compatible-mode/v1"
export MODEL="qwen-flash"
```

## 注意事项

1. **数据库连接**：确保数据库连接配置正确，否则查询会失败
2. **关键词提取**：当前使用简单的正则表达式提取，可以根据需要优化
3. **流式输出**：流式输出需要客户端支持 SSE（Server-Sent Events）
4. **资源管理**：使用完毕后记得调用 `service.close()` 关闭数据库连接

## 扩展开发

### 自定义关键词提取

可以重写 `AISearchService._extract_keywords()` 方法，使用更高级的 NLP 技术（如分词、词性标注等）来提取关键词。

### 自定义数据库查询

可以扩展 `DatabaseManager` 类，添加更复杂的查询逻辑，如：
- 模糊匹配
- 相关性排序
- 多字段搜索

## 示例代码

完整示例请参考 `example.py` 文件。

