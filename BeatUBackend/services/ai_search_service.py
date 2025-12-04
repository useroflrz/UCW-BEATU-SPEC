"""AI 搜索服务（集成 AgentMCP 业务层）"""

from __future__ import annotations

import sys
from pathlib import Path
from typing import AsyncGenerator, Optional
import json

# 添加 AgentMCP 项目路径到 sys.path
agentmcp_path = Path(__file__).parent.parent.parent / "AgentMCP"
if str(agentmcp_path) not in sys.path:
    sys.path.insert(0, str(agentmcp_path))

try:
    from src.business.ai_search import AISearchService as AgentAISearchService, AISearchRequest as AgentAISearchRequest
    from src.utils.logger import setup_logger
except ImportError as e:
    # 如果导入失败，创建一个占位类
    AgentAISearchService = None
    AgentAISearchRequest = None
    setup_logger = None
    import logging
    logging.warning(f"无法导入 AgentMCP 模块: {e}")


class AISearchService:
    """AI 搜索服务（封装 AgentMCP 业务层）"""
    
    def __init__(self):
        """初始化 AI 搜索服务"""
        if AgentAISearchService is None:
            raise ImportError("AgentMCP 模块未找到，请确保 AgentMCP 项目在正确的位置")
        
        if setup_logger:
            self.logger = setup_logger(__name__)
        else:
            import logging
            self.logger = logging.getLogger(__name__)
        
        # 初始化 AgentMCP 的 AI 搜索服务
        # 使用 BeatUBackend 的数据库配置
        from core.config import settings
        import os
        
        # 设置本地数据库路径（如果存在）
        local_db_path = Path(__file__).parent.parent / "beatu.db"
        if local_db_path.exists():
            os.environ["LOCAL_DB_PATH"] = str(local_db_path)
        
        # 设置远程数据库 URL
        if settings.database_url:
            os.environ["REMOTE_DB_URL"] = settings.database_url
        
        self.agent_service = AgentAISearchService()
        self.logger.info("AI 搜索服务初始化成功")
    
    async def search_stream(
        self,
        user_query: str
    ) -> AsyncGenerator[str, None]:
        """
        流式搜索处理
        
        Args:
            user_query: 用户查询文本
        
        Yields:
            str: SSE 格式的数据块
        """
        try:
            request = AgentAISearchRequest(user_query=user_query)
            
            async for chunk in self.agent_service.search_stream(request):
                # 将 StreamChunk 转换为 SSE 格式
                chunk_data = {
                    "chunkType": chunk.chunk_type,
                    "content": chunk.content,
                    "isFinal": chunk.is_final
                }
                yield f"data: {json.dumps(chunk_data, ensure_ascii=False)}\n\n"
        
        except Exception as e:
            self.logger.error(f"AI 搜索处理失败: {e}", exc_info=True)
            error_data = {
                "chunkType": "error",
                "content": f"处理失败: {str(e)}",
                "isFinal": True
            }
            yield f"data: {json.dumps(error_data, ensure_ascii=False)}\n\n"
    
    def close(self):
        """关闭服务资源"""
        if hasattr(self, 'agent_service'):
            self.agent_service.close()

