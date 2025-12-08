"""AI 搜索服务（使用 MCP Orchestrator 进行联网搜索）"""

from __future__ import annotations

import asyncio
import json
import re
from typing import AsyncGenerator, List, Optional
from sqlalchemy import or_, select
from sqlalchemy.orm import Session

from database.connection import get_db
from database.models import Video
from services.mcp_orchestrator_service import get_mcp_service
from services.video_service import VideoService
import logging

logger = logging.getLogger(__name__)


class AISearchService:
    """AI 搜索服务
    
    使用 MCP Orchestrator 进行联网搜索，同时根据关键词搜索视频。
    """
    
    def __init__(self, db: Optional[Session] = None):
        """初始化 AI 搜索服务
        
        Args:
            db: 数据库会话，如果是 None 则使用默认连接
        """
        self.mcp_service = get_mcp_service()
        self.db = db
        self.logger = logger
    
    async def search_stream(
        self,
        user_query: str,
        db: Optional[Session] = None
    ) -> AsyncGenerator[str, None]:
        """
        流式搜索处理
        
        1. 使用 MCP Orchestrator 异步联网搜索用户的关键字
        2. 流式返回 Agent 搜索结果
        3. 提取关键词并搜索相关视频
        4. 返回视频列表
        
        Args:
            user_query: 用户查询文本
            db: 数据库会话（用于搜索视频）
        
        Yields:
            str: SSE 格式的数据块
        """
        try:
            # 使用传入的 db 或实例的 db
            search_db = db or self.db
            
            # Step 1: 异步启动 Agent 联网搜索
            agent_task = asyncio.create_task(
                self._agent_search_async(user_query)
            )
            
            # Step 2: 流式返回 Agent 搜索结果
            agent_result = ""
            async for chunk in self._stream_agent_result(agent_task):
                agent_result += chunk.get("content", "")
                yield f"data: {json.dumps(chunk, ensure_ascii=False)}\n\n"
            
            # Step 3: 从 Agent 结果中提取关键词
            keywords = self._extract_keywords(agent_result, user_query)
            if keywords:
                keywords_chunk = {
                    "chunkType": "keywords",
                    "content": json.dumps(keywords, ensure_ascii=False),
                    "isFinal": True
                }
                yield f"data: {json.dumps(keywords_chunk, ensure_ascii=False)}\n\n"
            
            # Step 4: 根据关键词搜索视频（如果提供了数据库会话）
            if search_db and keywords:
                video_ids = await self._search_videos_by_keywords(search_db, keywords)
                if video_ids:
                    videos_chunk = {
                        "chunkType": "videos",
                        "content": json.dumps(video_ids, ensure_ascii=False),
                        "isFinal": True
                    }
                    yield f"data: {json.dumps(videos_chunk, ensure_ascii=False)}\n\n"
        
        except Exception as e:
            self.logger.error(f"AI 搜索处理失败: {e}", exc_info=True)
            error_data = {
                "chunkType": "error",
                "content": f"处理失败: {str(e)}",
                "isFinal": True
            }
            yield f"data: {json.dumps(error_data, ensure_ascii=False)}\n\n"
    
    async def _agent_search_async(self, user_query: str) -> str:
        """异步执行 Agent 联网搜索
        
        Args:
            user_query: 用户查询文本
        
        Returns:
            str: Agent 搜索结果
        """
        try:
            # 构建搜索请求，让 Agent 联网搜索
            search_prompt = f"请帮我搜索关于 '{user_query}' 的最新信息，并提供详细的搜索结果。"
            result = await self.mcp_service.process_request(search_prompt)
            return result
        except Exception as e:
            self.logger.error(f"Agent 搜索失败: {e}", exc_info=True)
            return f"搜索过程中出现错误: {str(e)}"
    
    async def _stream_agent_result(self, agent_task: asyncio.Task) -> AsyncGenerator[dict, None]:
        """流式返回 Agent 搜索结果
        
        由于 MCP Orchestrator 返回完整字符串，我们模拟流式输出。
        
        Args:
            agent_task: Agent 搜索任务
        
        Yields:
            dict: 数据块字典
        """
        try:
            # 等待 Agent 搜索完成
            result = await agent_task
            
            # 模拟流式输出：将结果分块发送
            chunk_size = 50  # 每块 50 个字符
            for i in range(0, len(result), chunk_size):
                chunk = result[i:i + chunk_size]
                is_final = i + chunk_size >= len(result)
                
                chunk_data = {
                    "chunkType": "answer",
                    "content": chunk,
                    "isFinal": is_final
                }
                yield chunk_data
                
                # 添加小延迟，模拟真实流式输出
                await asyncio.sleep(0.05)
        
        except Exception as e:
            self.logger.error(f"流式输出失败: {e}", exc_info=True)
            error_chunk = {
                "chunkType": "error",
                "content": f"流式输出失败: {str(e)}",
                "isFinal": True
            }
            yield error_chunk
    
    def _extract_keywords(self, agent_result: str, user_query: str) -> List[str]:
        """从 Agent 结果和用户查询中提取关键词
        
        Args:
            agent_result: Agent 搜索结果
            user_query: 用户查询文本
        
        Returns:
            List[str]: 关键词列表
        """
        keywords = []
        
        # 1. 从用户查询中提取关键词（去除停用词）
        stop_words = {"的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", "一个", "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好", "自己", "这"}
        query_words = re.findall(r'\w+', user_query)
        for word in query_words:
            if len(word) > 1 and word not in stop_words:
                keywords.append(word)
        
        # 2. 从 Agent 结果中提取可能的实体和关键词
        # 提取中文词汇（2-4 个字符）
        chinese_words = re.findall(r'[\u4e00-\u9fa5]{2,4}', agent_result)
        for word in chinese_words[:5]:  # 最多取 5 个
            if word not in keywords and word not in stop_words:
                keywords.append(word)
        
        # 去重并限制数量
        keywords = list(dict.fromkeys(keywords))[:10]  # 最多 10 个关键词
        
        return keywords
    
    async def _search_videos_by_keywords(
        self,
        db: Session,
        keywords: List[str],
        limit: int = 20
    ) -> List[int]:
        """根据关键词搜索视频
        
        Args:
            db: 数据库会话
            keywords: 关键词列表
            limit: 返回结果数量限制
        
        Returns:
            List[int]: 视频 ID 列表（Long 类型）
        """
        if not keywords:
            return []
        
        try:
            # 构建查询条件：在标题中搜索关键词
            # 对于 tags JSON 字段，先简单处理：只搜索标题
            # 后续可以优化为使用 MySQL JSON 函数搜索 tags
            conditions = []
            for keyword in keywords:
                # 在标题中搜索
                conditions.append(Video.title.like(f"%{keyword}%"))
            
            # 执行查询
            query = select(Video.videoId).where(  # ✅ 修改：字段名从 id 改为 videoId
                or_(*conditions)
            ).limit(limit)
            
            result = db.execute(query)
            video_ids = [int(row[0]) for row in result]  # ✅ 修改：确保返回 int (Long) 类型
            
            self.logger.info(f"根据关键词 {keywords} 搜索到 {len(video_ids)} 个视频")
            return video_ids
        
        except Exception as e:
            self.logger.error(f"视频搜索失败: {e}", exc_info=True)
            return []
    
    def close(self):
        """关闭服务资源"""
        # MCP 服务使用单例模式，不需要关闭
        pass
