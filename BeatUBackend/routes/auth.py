from __future__ import annotations

import logging

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from database.connection import get_db
from schemas.api import LoginOrRegisterRequest, success_response
from services.auth_service import AuthService


router = APIRouter(tags=["auth"])


def get_auth_service(db: Session = Depends(get_db)) -> AuthService:
    return AuthService(db)


@router.post("/auth/login-or-register")
def login_or_register(
    payload: LoginOrRegisterRequest,
    service: AuthService = Depends(get_auth_service),
):
    """
    单一接口：无账号则自动注册；有账号则校验密码。
    客户端持久化 userId，请求头 X-User-Id 传递（无 JWT）。
    """
    try:
        data = service.login_or_register(
            user_name=payload.user_name,
            password=payload.password,
        )
        return success_response(data.model_dump(by_alias=True))
    except ValueError as e:
        # 业务规则：密码错误、旧账号无 passwordHash、用户名/密码长度等
        logger.warning("login-or-register rejected: %s", e)
        raise HTTPException(status_code=400, detail=str(e)) from e
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"登录失败: {e!s}") from e
