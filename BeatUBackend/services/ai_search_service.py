"""AI 搜索服务（云 LLM 流式版）"""

from __future__ import annotations

import json
from typing import AsyncGenerator, Optional
from sqlalchemy.orm import Session
import logging
from langchain_openai import ChatOpenAI
from langchain_core.messages import HumanMessage

from core.config import settings

logger = logging.getLogger(__name__)


class AISearchService:
    """AI 搜索服务
    
    调用云 LLM 生成 markdown 回答，并通过 SSE 流式返回给客户端。
    """
    
    def __init__(self, db: Optional[Session] = None):
        """初始化 AI 搜索服务
        
        Args:
            db: 数据库会话，如果是 None 则使用默认连接
        """
        self.db = db
        self.logger = logger
        self.llm = self._build_llm()
    
    async def search_stream(
        self,
        user_query: str
    ) -> AsyncGenerator[str, None]:
        """
        流式搜索处理
        
        1. 调用云 LLM 生成 markdown 回答（标题/关键词加粗）
        2. 以 SSE 协议流式返回 answer 数据块
        
        Args:
            user_query: 用户查询文本
        
        Yields:
            str: SSE 格式的数据块
        """
        try:
            query = (user_query or "").strip()
            if not query:
                empty_data = {
                    "chunkType": "error",
                    "content": "请先输入搜索内容",
                    "isFinal": True
                }
                yield f"data: {json.dumps(empty_data, ensure_ascii=False)}\n\n"
                return

            prompt = self._build_markdown_prompt(query)
            messages = [HumanMessage(content=prompt)]
            streamed_any = False

            async for chunk in self.llm.astream(messages):
                content = self._extract_chunk_content(chunk)
                if not content:
                    continue
                streamed_any = True
                chunk_data = {
                    "chunkType": "answer",
                    "content": content,
                    "isFinal": False
                }
                yield f"data: {json.dumps(chunk_data, ensure_ascii=False)}\n\n"

            final_data = {
                "chunkType": "answer",
                "content": "",
                "isFinal": True
            }
            if not streamed_any:
                # 兜底：云端无增量输出时，仍保证前端结束 loading
                final_data["content"] = "## 搜索结果\n\n暂未返回内容，请稍后重试。"
            yield f"data: {json.dumps(final_data, ensure_ascii=False)}\n\n"
        
        except Exception as e:
            self.logger.error(f"AI 搜索处理失败: {e}", exc_info=True)
            error_data = {
                "chunkType": "error",
                "content": "AI 搜索不可用",
                "isFinal": True
            }
            yield f"data: {json.dumps(error_data, ensure_ascii=False)}\n\n"

    def _build_llm(self) -> ChatOpenAI:
        """构建云 LLM 客户端，优先使用 MCP 配置。"""
        api_key = settings.mcp_api_key
        if not api_key:
            raise RuntimeError("MCP_API_KEY 未配置")
        return ChatOpenAI(
            model=settings.mcp_model,
            base_url=settings.mcp_base_url,
            api_key=api_key,
            temperature=0.4,
        )

    def _build_markdown_prompt(self, query: str) -> str:
        """生成要求 markdown + 关键词高亮的提示词。"""
        return (
            "你是短视频搜索助手。请根据用户搜索词输出结构化 markdown，要求：\n"
            "1) 必须以二级标题开头：`## 搜索结果`\n"
            "2) 紧接着给一段 2-4 句的结果概述\n"
            "3) 抽取 3-6 个关键词，并在正文中使用 **关键词** 形式高亮\n"
            "4) 不要输出 JSON，不要输出代码块，不要解释你在做什么\n"
            "5) 全文使用中文\n\n"
            f"用户搜索词：{query}"
        )

    def _extract_chunk_content(self, chunk: object) -> str:
        """兼容不同 chunk 结构，提取文本内容。"""
        if isinstance(chunk, str):
            return chunk
        content = getattr(chunk, "content", "")
        if isinstance(content, str):
            return content
        if isinstance(content, list):
            texts = []
            for item in content:
                if isinstance(item, str):
                    texts.append(item)
                elif isinstance(item, dict):
                    text = item.get("text")
                    if isinstance(text, str):
                        texts.append(text)
            return "".join(texts)
        return ""
    
