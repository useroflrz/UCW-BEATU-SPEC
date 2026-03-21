from __future__ import annotations

import logging
import uuid

import bcrypt
from sqlalchemy.orm import Session

from database.models import User
from schemas.api import LoginOrRegisterResponse

logger = logging.getLogger(__name__)


class AuthService:
    def __init__(self, db: Session) -> None:
        self.db = db

    def login_or_register(self, user_name: str, password: str) -> LoginOrRegisterResponse:
        """
        若 userName 不存在则注册并登录；存在则校验密码后登录。
        """
        name = (user_name or "").strip()
        if not name:
            raise ValueError("用户名不能为空")
        if len(password) < 6:
            raise ValueError("密码至少 6 位")

        user = self.db.query(User).filter(User.userName == name).one_or_none()

        if user is None:
            uid = uuid.uuid4().hex[:32]
            if len(uid) < 8:
                uid = f"u{uuid.uuid4().hex}"
            pwd_hash = bcrypt.hashpw(password.encode("utf-8"), bcrypt.gensalt()).decode("utf-8")
            user = User(
                userId=uid,
                userName=name,
                passwordHash=pwd_hash,
                followerCount=0,
                followingCount=0,
            )
            self.db.add(user)
            self.db.commit()
            self.db.refresh(user)
            logger.info("auth: registered user_id=%s userName=%s", user.userId, name)
        else:
            if not user.passwordHash:
                raise ValueError("该账号为旧数据，请更换用户名注册新账号或联系管理员")
            if not bcrypt.checkpw(password.encode("utf-8"), user.passwordHash.encode("utf-8")):
                raise ValueError("密码错误")
            logger.info("auth: login user_id=%s userName=%s", user.userId, name)

        return LoginOrRegisterResponse(
            user_id=user.userId,
            user_name=user.userName,
            access_token="",
            token_type="none",
        )
