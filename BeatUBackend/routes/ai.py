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
    """获取 AI 搜索服务实例"""
    global _ai_search_service
    if _ai_search_service is None:
        try:
            _ai_search_service = AISearchService()
        except Exception as e:
            raise HTTPException(
                status_code=503,
                detail=f"AI 搜索服务不可用: {str(e)}"
            )
    return _ai_search_service


@router.post("/ai/search/stream")
async def ai_search_stream(
    payload: AISearchRequest,
    service: AISearchService = Depends(get_ai_search_service),
    db: Session = Depends(get_db),
):
    """
    AI 搜索流式接口
    
    使用 MCP Agent 异步联网搜索用户的关键字，并以流式协议返回结果。
    同时根据关键词搜索相关视频并返回。
    
    返回 Server-Sent Events (SSE) 格式的流式响应，包含：
    - answer: Agent 联网搜索的文本结果（流式输出）
    - keywords: 提取的关键词列表
    - videos: 根据关键词搜索到的视频 ID 列表
    
    Args:
        payload: 搜索请求，包含 user_query 字段
        service: AI 搜索服务实例
        db: 数据库会话（用于搜索视频）
    
    Returns:
        StreamingResponse: SSE 格式的流式响应
    """
    async def generate():
        """生成流式响应"""
        try:
            async for chunk in service.search_stream(payload.user_query, db=db):
                yield chunk
        except Exception as e:
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

