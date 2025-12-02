from __future__ import annotations

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from database.models import Interaction, Video
from schemas.api import UserItem
from services.helpers import parse_bool_map


class UserService:
    def __init__(self, db: Session) -> None:
        self.db = db

    def get_user(self, user_id: str, current_user_id: str | None = None) -> UserItem:
        """获取用户信息"""
        # 从视频表中聚合用户信息（因为用户信息主要来自视频作者）
        videos = self.db.execute(
            select(Video).where(Video.author_id == user_id).limit(1)
        ).scalars().first()
        
        if not videos:
            # 如果用户不存在，返回默认值
            return UserItem(
                id=user_id,
                name="未知用户",
                avatar_url=None,
                bio=None,
                likes_count=0,
                following_count=0,
                followers_count=0,
            )
        
        # 计算用户的统计数据
        likes_count = self.db.scalar(
            select(func.sum(Video.like_count)).where(Video.author_id == user_id)
        ) or 0
        
        following_count = self.db.scalar(
            select(func.count(Interaction.id)).where(
                Interaction.user_id == user_id,
                Interaction.type == "FOLLOW_AUTHOR"
            )
        ) or 0
        
        followers_count = self.db.scalar(
            select(func.count(Interaction.id)).where(
                Interaction.author_id == user_id,
                Interaction.type == "FOLLOW_AUTHOR"
            )
        ) or 0
        
        return UserItem(
            id=user_id,
            name=videos.author_name,
            avatar_url=videos.author_avatar,
            bio=None,  # 暂时没有bio字段，后续可以扩展
            likes_count=int(likes_count),
            following_count=following_count,
            followers_count=followers_count,
        )

    def follow_user(self, user_id: str, target_user_id: str) -> dict:
        """关注用户"""
        interaction = (
            self.db.query(Interaction)
            .filter(
                Interaction.user_id == user_id,
                Interaction.author_id == target_user_id,
                Interaction.type == "FOLLOW_AUTHOR",
            )
            .one_or_none()
        )
        
        if interaction:
            return {"success": True, "message": "已关注"}
        
        entity = Interaction(
            user_id=user_id,
            author_id=target_user_id,
            type="FOLLOW_AUTHOR",
        )
        self.db.add(entity)
        self.db.commit()
        return {"success": True, "message": "关注成功"}

    def unfollow_user(self, user_id: str, target_user_id: str) -> dict:
        """取消关注用户"""
        interaction = (
            self.db.query(Interaction)
            .filter(
                Interaction.user_id == user_id,
                Interaction.author_id == target_user_id,
                Interaction.type == "FOLLOW_AUTHOR",
            )
            .one_or_none()
        )
        
        if interaction:
            self.db.delete(interaction)
            self.db.commit()
            return {"success": True, "message": "已取消关注"}
        
        return {"success": True, "message": "未关注"}

