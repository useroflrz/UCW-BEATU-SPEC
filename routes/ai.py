from __future__ import annotations

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from database.connection import get_db
from schemas.api import (
    AICommentQARequest,
    AIQualityRequest,
    AIRecommendRequest,
    CommentAIRequest,
    success_response,
)
from services.ai_service import AIService
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


