from __future__ import annotations

from fastapi import APIRouter, Depends, Header, Path

from database.connection import get_db
from schemas.api import success_response
from services.user_service import UserService
from sqlalchemy.orm import Session


router = APIRouter(tags=["users"])


def get_user_service(db: Session = Depends(get_db)) -> UserService:
    return UserService(db)


def resolve_user(x_user_id: str | None = Header(default=None, alias="X-User-Id")) -> str:
    """解析当前用户ID"""
    return x_user_id or "demo-user"


@router.get("/users/{user_id}")
def get_user(
    user_id: str = Path(...),
    service: UserService = Depends(get_user_service),
    current_user_id: str = Depends(resolve_user),
):
    """获取用户信息"""
    user = service.get_user(user_id, current_user_id=current_user_id)
    return success_response(user.dict(by_alias=True))


@router.post("/users/{user_id}/follow")
def follow_user(
    user_id: str = Path(...),
    service: UserService = Depends(get_user_service),
    current_user_id: str = Depends(resolve_user),
):
    """关注用户"""
    result = service.follow_user(current_user_id, user_id)
    return success_response(None)


@router.post("/users/{user_id}/unfollow")
def unfollow_user(
    user_id: str = Path(...),
    service: UserService = Depends(get_user_service),
    current_user_id: str = Depends(resolve_user),
):
    """取消关注用户"""
    result = service.unfollow_user(current_user_id, user_id)
    return success_response(None)

