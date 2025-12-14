from __future__ import annotations

import json
from fastapi import APIRouter, Depends, HTTPException
from fastapi.responses import StreamingResponse
from sqlalchemy.orm import Session

from database.connection import get_db
from schemas.api import (
    AICommentQARequest,
    AIQualityRequest,
    AIRecommendRequest,
    AISearchRequest,
    CommentAIRequest,
    success_response,
)
from services.ai_service import AIService
from services.ai_search_service import AISearchService
from services.comment_service import CommentService


router = APIRouter(tags=["ai"])


def get_ai_service(db: Session = Depends(get_db)) -> AIService:
    return AIService(db)


def get_comment_service(db: Session = Depends(get_db)) -> CommentService:
    return CommentService(db)


@router.post("/ai/recommend")
def recommend(payload: AIRecommendRequest, service: AIService = Depends(get_ai_service)):
    data = service.recommend(payload)
    return success_response(data.dict(by_alias=True))


@router.post("/ai/quality")
def quality(payload: AIQualityRequest, service: AIService = Depends(get_ai_service)):
    data = service.quality(payload)
    return success_response(data.dict(by_alias=True))


@router.post("/ai/comment/qa")
def comment_qa(
    payload: AICommentQARequest,
    service: AIService = Depends(get_ai_service),
    comment_service: CommentService = Depends(get_comment_service),
):
    content = service.comment_qa(payload)
    ai_comment = comment_service.create_ai_comment(
        video_id=payload.video_id,
        payload=CommentAIRequest(question=payload.question),
        user_name="@元宝",
        override_content=content,
    )
    return success_response({"comment": ai_comment.dict(by_alias=True)})


# AI 搜索服务实例（单例模式）
_ai_search_service: AISearchService = None


def get_ai_search_service() -> AISearchService:
    """获取 AI 搜索服务实例（延迟初始化，简化版）"""
    global _ai_search_service
    if _ai_search_service is None:
        try:
            # ✅ 延迟初始化：只在第一次调用时创建，避免启动时阻塞
            # 简化版：直接使用 LLM，不需要 MCP Orchestrator
            _ai_search_service = AISearchService()
        except Exception as e:
            # ✅ 改进错误处理：记录详细错误信息
            import logging
            logger = logging.getLogger(__name__)
            logger.error(f"AI 搜索服务初始化失败: {e}", exc_info=True)
            raise HTTPException(
                status_code=503,
                detail=f"AI 搜索服务不可用: {str(e)}"
            )
    return _ai_search_service


@router.post("/ai/search/stream")
async def ai_search_stream(
    payload: AISearchRequest,
    service: AISearchService = Depends(get_ai_search_service),
):
    """
    AI 搜索流式接口（简化版：名词解释和问题应答）
    
    直接使用 LLM 进行名词解释或问题应答，以流式协议返回结果。
    
    返回 Server-Sent Events (SSE) 格式的流式响应，包含：
    - answer: LLM 生成的回答（流式输出）
    
    Args:
        payload: 搜索请求，包含 user_query 字段
        service: AI 搜索服务实例
    
    Returns:
        StreamingResponse: SSE 格式的流式响应
    """
    async def generate():
        """生成流式响应"""
        import logging
        logger = logging.getLogger(__name__)
        try:
            logger.info(f"开始处理 AI 搜索请求: user_query={payload.user_query}")
            chunk_count = 0
            async for chunk in service.search_stream(payload.user_query):
                chunk_count += 1
                logger.debug(f"生成 chunk #{chunk_count}: {chunk[:100] if len(chunk) > 100 else chunk}")
                yield chunk
            logger.info(f"AI 搜索请求处理完成: 共生成 {chunk_count} 个 chunk")
        except Exception as e:
            logger.error(f"生成流式响应失败: {e}", exc_info=True)
            error_data = {
                "chunkType": "error",
                "content": f"处理失败: {str(e)}",
                "isFinal": True
            }
            yield f"data: {json.dumps(error_data, ensure_ascii=False)}\n\n"
    
    return StreamingResponse(
        generate(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        }
    )

