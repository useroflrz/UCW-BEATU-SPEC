"""AI 搜索服务"""

from typing import AsyncGenerator, List, Optional
from langchain_openai import ChatOpenAI
from langchain_core.messages import HumanMessage
import json
import re

from agent_mcp.business.ai_search.models import AISearchRequest, AISearchResponse, StreamChunk
from agent_mcp.business.ai_search.database import DatabaseManager
from agent_mcp.utils.llm_utils import create_default_llm
from agent_mcp.utils.logger import setup_logger

logger = setup_logger(__name__)


class AISearchService:
    """AI 搜索服务"""
    
    def __init__(
        self,
        llm: Optional[ChatOpenAI] = None,
        db_manager: Optional[DatabaseManager] = None
    ):
        """
        初始化 AI 搜索服务
        
        Args:
            llm: LangChain LLM 实例
            db_manager: 数据库管理器
        """
        self.llm = create_default_llm(llm)
        self.db_manager = db_manager or DatabaseManager()
    
    async def search_stream(
        self,
        request: AISearchRequest
    ) -> AsyncGenerator[StreamChunk, None]:
        """
        流式搜索处理
        
        Args:
            request: 搜索请求
        
        Yields:
            StreamChunk: 流式数据块
        """
        try:
            # Step 1: 生成 AI 回答（流式）
            ai_answer = ""
            async for chunk in self._generate_ai_answer_stream(request.user_query):
                ai_answer += chunk
                yield StreamChunk(
                    chunk_type="answer",
                    content=chunk,
                    is_final=False
                )
            
            # 发送最终答案块
            yield StreamChunk(
                chunk_type="answer",
                content="",
                is_final=True
            )
            
            # Step 2: 提取关键词
            keywords = self._extract_keywords(ai_answer, request.user_query)
            if keywords:
                yield StreamChunk(
                    chunk_type="keywords",
                    content=json.dumps(keywords, ensure_ascii=False),
                    is_final=True
                )
            
            # Step 3: 查询本地数据库
            local_video_ids = self.db_manager.search_local_videos_by_keywords(keywords)
            if local_video_ids:
                yield StreamChunk(
                    chunk_type="local_video_ids",
                    content=json.dumps(local_video_ids, ensure_ascii=False),
                    is_final=True
                )
            
            # Step 4: 查询远程数据库
            remote_video_ids = self.db_manager.search_remote_videos_by_keywords(keywords)
            if remote_video_ids:
                yield StreamChunk(
                    chunk_type="video_ids",
                    content=json.dumps(remote_video_ids, ensure_ascii=False),
                    is_final=True
                )
        
        except Exception as e:
            logger.error(f"AI 搜索处理失败: {e}", exc_info=True)
            yield StreamChunk(
                chunk_type="error",
                content=f"处理失败: {str(e)}",
                is_final=True
            )
    
    async def search(
        self,
        request: AISearchRequest
    ) -> AISearchResponse:
        """
        同步搜索处理（非流式）
        
        Args:
            request: 搜索请求
        
        Returns:
            AISearchResponse: 搜索响应
        """
        try:
            # Step 1: 生成 AI 回答
            ai_answer = await self._generate_ai_answer(request.user_query)
            
            # Step 2: 提取关键词
            keywords = self._extract_keywords(ai_answer, request.user_query)
            
            # Step 3: 查询本地数据库
            local_video_ids = self.db_manager.search_local_videos_by_keywords(keywords)
            
            # Step 4: 查询远程数据库
            remote_video_ids = self.db_manager.search_remote_videos_by_keywords(keywords)
            
            return AISearchResponse(
                ai_answer=ai_answer,
                keywords=keywords,
                video_ids=remote_video_ids,
                local_video_ids=local_video_ids
            )
        
        except Exception as e:
            logger.error(f"AI 搜索处理失败: {e}", exc_info=True)
            return AISearchResponse(
                ai_answer=f"抱歉，处理您的请求时出现错误: {str(e)}",
                keywords=[],
                video_ids=[],
                local_video_ids=[]
            )
    
    async def _generate_ai_answer_stream(
        self,
        user_query: str
    ) -> AsyncGenerator[str, None]:
        """
        流式生成 AI 回答
        
        Args:
            user_query: 用户查询
        
        Yields:
            str: 回答文本片段
        """
        try:
            prompt = f"""你是一个智能视频搜索助手。用户询问：{user_query}

请用自然、友好的语言回答用户的问题，并尝试从回答中提取相关的关键词，这些关键词将用于搜索相关视频。

回答要求：
1. 回答要简洁明了，控制在 200 字以内
2. 回答中要包含可以用于搜索的关键词
3. 如果用户的问题与视频内容相关，请提供有用的建议

请直接回答，不要包含"关键词"等提示词。"""
            
            messages = [HumanMessage(content=prompt)]
            
            async for chunk in self.llm.astream(messages):
                # 处理不同类型的 chunk
                if hasattr(chunk, 'content'):
                    content = chunk.content
                    if content:
                        yield content
                elif isinstance(chunk, str):
                    yield chunk
                elif hasattr(chunk, 'text'):
                    yield chunk.text
        
        except Exception as e:
            logger.error(f"生成 AI 回答失败: {e}", exc_info=True)
            yield f"抱歉，生成回答时出现错误: {str(e)}"
    
    async def _generate_ai_answer(self, user_query: str) -> str:
        """
        生成 AI 回答（非流式）
        
        Args:
            user_query: 用户查询
        
        Returns:
            str: 完整回答
        """
        try:
            prompt = f"""你是一个智能视频搜索助手。用户询问：{user_query}

请用自然、友好的语言回答用户的问题，并尝试从回答中提取相关的关键词，这些关键词将用于搜索相关视频。

回答要求：
1. 回答要简洁明了，控制在 200 字以内
2. 回答中要包含可以用于搜索的关键词
3. 如果用户的问题与视频内容相关，请提供有用的建议

请直接回答，不要包含"关键词"等提示词。"""
            
            messages = [HumanMessage(content=prompt)]
            response = await self.llm.ainvoke(messages)
            return response.content if hasattr(response, 'content') else str(response)
        
        except Exception as e:
            logger.error(f"生成 AI 回答失败: {e}", exc_info=True)
            return f"抱歉，生成回答时出现错误: {str(e)}"
    
    def _extract_keywords(
        self,
        ai_answer: str,
        user_query: str
    ) -> List[str]:
        """
        从 AI 回答和用户查询中提取关键词
        
        Args:
            ai_answer: AI 生成的回答
            user_query: 用户原始查询
        
        Returns:
            List[str]: 关键词列表
        """
        keywords = []
        
        # 从用户查询中提取关键词（去除停用词）
        stop_words = {"的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", "一个", "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好", "自己", "这"}
        user_words = re.findall(r'[\u4e00-\u9fa5]+|[a-zA-Z]+', user_query)
        for word in user_words:
            if len(word) >= 2 and word not in stop_words:
                keywords.append(word)
        
        # 从 AI 回答中提取可能的关键词（名词、动词等）
        answer_words = re.findall(r'[\u4e00-\u9fa5]{2,}', ai_answer)
        for word in answer_words:
            if word not in stop_words and word not in keywords:
                keywords.append(word)
        
        # 去重并限制数量
        keywords = list(dict.fromkeys(keywords))[:10]
        
        logger.info(f"提取的关键词: {keywords}")
        return keywords
    
    def close(self):
        """关闭服务资源"""
        if self.db_manager:
            self.db_manager.close()
