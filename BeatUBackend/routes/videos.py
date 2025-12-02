from __future__ import annotations

from fastapi import APIRouter, Depends, Header, Path, Query
from sqlalchemy.orm import Session

from database.connection import get_db
from schemas.api import (
    CommentAIRequest,
    CommentCreate,
    CommentList,
    FollowRequest,
    InteractionRequest,
    OperationResult,
    VideoItem,
    VideoList,
)
from schemas.api import success_response
from services.comment_service import CommentService
from services.video_service import VideoService


router = APIRouter(tags=["videos"])


def get_video_service(db: Session = Depends(get_db)) -> VideoService:
    return VideoService(db)


def get_comment_service(db: Session = Depends(get_db)) -> CommentService:
    return CommentService(db)


def resolve_user(x_user_id: str | None = Header(default=None)) -> str:
    return x_user_id or "demo-user"


def resolve_user_name(x_user_name: str | None = Header(default=None)) -> str:
    return x_user_name or "BeatU 用户"


@router.get("/videos")
def list_videos(
    page: int = Query(1, ge=1),
    limit: int = Query(10, ge=1, le=50),
    orientation: str | None = Query(default=None, regex="^(portrait|landscape)$"),
    channel: str | None = Query(default=None),
    service: VideoService = Depends(get_video_service),
    user_id: str = Depends(resolve_user),
):
    data = service.list_videos(
        page=page,
        limit=limit,
        orientation=orientation.lower() if orientation else None,
        channel=channel,
        user_id=user_id,
    )
    # 使用新的create方法生成包含pageSize等字段的响应
    response_data = VideoList.create(
        items=data.items,
        total=data.total,
        page=data.page,
        limit=data.limit
    )
    return success_response(response_data.dict(by_alias=True))


@router.get("/videos/{video_id}")
def get_video(
    video_id: str,
    service: VideoService = Depends(get_video_service),
    user_id: str = Depends(resolve_user),
):
    item = service.get_video(video_id, user_id=user_id)
    return success_response(item.dict(by_alias=True))


@router.post("/videos/{video_id}/like")
def like_video(
    video_id: str,
    service: VideoService = Depends(get_video_service),
    user_id: str = Depends(resolve_user),
):
    """点赞视频，客户端不需要传递body参数"""
    payload = InteractionRequest(action="LIKE")
    result = service.like_video(video_id, payload, user_id=user_id)
    return success_response(None)


@router.post("/videos/{video_id}/unlike")
def unlike_video(
    video_id: str,
    service: VideoService = Depends(get_video_service),
    user_id: str = Depends(resolve_user),
):
    """取消点赞视频"""
    payload = InteractionRequest(action="UNLIKE")
    result = service.like_video(video_id, payload, user_id=user_id)
    return success_response(None)


@router.post("/videos/{video_id}/favorite")
def favorite_video(
    video_id: str,
    service: VideoService = Depends(get_video_service),
    user_id: str = Depends(resolve_user),
):
    """收藏视频，客户端不需要传递body参数"""
    payload = InteractionRequest(action="SAVE")
    result = service.favorite_video(video_id, payload, user_id=user_id)
    return success_response(None)


@router.post("/videos/{video_id}/unfavorite")
def unfavorite_video(
    video_id: str,
    service: VideoService = Depends(get_video_service),
    user_id: str = Depends(resolve_user),
):
    """取消收藏视频"""
    payload = InteractionRequest(action="REMOVE")
    result = service.favorite_video(video_id, payload, user_id=user_id)
    return success_response(None)


# 旧的 /follow 接口保留以兼容，新的用户接口在 users.py 中实现
@router.post("/follow")
def follow_author(
    payload: FollowRequest,
    service: VideoService = Depends(get_video_service),
    user_id: str = Depends(resolve_user),
):
    result = service.follow_author(payload, user_id=user_id)
    return success_response(result.dict(by_alias=True))


@router.get("/videos/{video_id}/comments")
def list_comments(
    video_id: str = Path(...),
    page: int = Query(1, ge=1),
    limit: int = Query(20, ge=1, le=100),
    service: CommentService = Depends(get_comment_service),
):
    data = service.list_comments(video_id=video_id, page=page, limit=limit)
    return success_response(data.dict(by_alias=True))


@router.post("/videos/{video_id}/comments")
def create_comment(
    video_id: str,
    payload: CommentCreate,
    service: CommentService = Depends(get_comment_service),
    user_id: str = Depends(resolve_user),
    user_name: str = Depends(resolve_user_name),
):
    item = service.create_comment(video_id, payload, user_id=user_id, user_name=user_name)
    return success_response(item.dict(by_alias=True))


@router.post("/videos/{video_id}/comments/ai")
def create_ai_comment(
    video_id: str,
    payload: CommentAIRequest,
    service: CommentService = Depends(get_comment_service),
    user_name: str = Depends(resolve_user_name),
):
    item = service.create_ai_comment(video_id, payload, user_name=user_name)
    return success_response(item.dict(by_alias=True))



