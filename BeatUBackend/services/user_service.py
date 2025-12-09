from __future__ import annotations

import logging
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from database.models import User, UserFollow, Video, VideoInteraction
from schemas.api import UserItem
from services.helpers import parse_bool_map


class UserService:
    def __init__(self, db: Session) -> None:
        self.db = db
        self.logger = logging.getLogger(__name__)

    def get_user(self, user_id: str, current_user_id: str | None = None) -> UserItem:
        """获取用户信息"""
        # ✅ 修改：直接从 User 表获取用户信息
        user = self.db.get(User, user_id)
        
        if not user:
            # 如果用户不存在，返回默认值
            self.logger.info("get_user: user not found, user_id=%s", user_id)
            return UserItem(
                id=user_id,
                username="未知用户",
                name="未知用户",
                avatar_url=None,
                bio=None,
                likes_count=0,
                following_count=0,
                followers_count=0,
            )
        
        # ✅ 修改：使用新的字段名；并兜底统计粉丝/关注数，避免数据库值为 NULL/未同步时返回 0
        followers = user.followerCount if user.followerCount is not None else self._count_followers(user.userId)
        followings = user.followingCount if user.followingCount is not None else self._count_followings(user.userId)

        self.logger.info(
            "get_user: loaded user_id=%s userName=%s followers=%s followings=%s bio=%s",
            user.userId,
            user.userName,
            followers,
            followings,
            user.bio,
        )

        return UserItem(
            id=user.userId,  # ✅ 修改：字段名从 id 改为 userId
            username=user.userName,  # ✅ 新增：返回username字段
            name=user.userName,  # ✅ 修改：字段名从 nickname 改为 userName
            avatar_url=user.avatarUrl,  # ✅ 修改：字段名从 avatar 改为 avatarUrl
            bio=user.bio,
            likes_count=0,  # ✅ 修改：新表结构中没有 likes_received 字段，可以通过视频聚合计算
            following_count=int(followings),  # ✅ 修改：字段名从 followings 改为 followingCount
            followers_count=int(followers),  # ✅ 修改：字段名从 followers 改为 followerCount
        )

    def get_user_by_name(self, user_name: str, current_user_id: str | None = None) -> UserItem:
        """根据用户名获取用户信息"""
        # ✅ 修改：从 User 表根据 userName 查询
        user = (
            self.db.query(User)
            .filter(User.userName == user_name)
            .one_or_none()
        )
        
        if not user:
            # 如果用户不存在，返回默认值
            self.logger.info("get_user_by_name: user not found, userName=%s", user_name)
            return UserItem(
                id="",
                username=user_name,
                name=user_name,
                avatar_url=None,
                bio=None,
                likes_count=0,
                following_count=0,
                followers_count=0,
            )
        
        # ✅ 修改：使用新的字段名
        self.logger.info(
            "get_user_by_name: loaded user_id=%s userName=%s followers=%s followings=%s bio=%s",
            user.userId,
            user.userName,
            user.followerCount,
            user.followingCount,
            user.bio,
        )

        return UserItem(
            id=user.userId,  # ✅ 修改：字段名从 id 改为 userId
            username=user.userName,  # ✅ 新增：返回username字段
            name=user.userName,  # ✅ 修改：字段名从 nickname 改为 userName
            avatar_url=user.avatarUrl,  # ✅ 修改：字段名从 avatar 改为 avatarUrl
            bio=user.bio,
            likes_count=0,  # ✅ 修改：新表结构中没有 likes_received 字段，可以通过视频聚合计算
            following_count=int(user.followingCount),  # ✅ 修改：字段名从 followings 改为 followingCount
            followers_count=int(user.followerCount),  # ✅ 修改：字段名从 followers 改为 followerCount
        )

    def follow_user(self, user_id: str, target_user_id: str) -> dict:
        """关注用户"""
        # ✅ 修改：使用新的 UserFollow 表
        follow = (
            self.db.query(UserFollow)
            .filter(
                UserFollow.userId == user_id,
                UserFollow.authorId == target_user_id,
            )
            .one_or_none()
        )
        
        if follow and follow.isFollowed:
            return {"success": True, "message": "已关注"}
        
        if follow:
            # 更新现有记录
            follow.isFollowed = True
            follow.isPending = False
        else:
            # 创建新记录
            entity = UserFollow(
                userId=user_id,
                authorId=target_user_id,
                isFollowed=True,
                isPending=False,
            )
            self.db.add(entity)
        
        # ✅ 修改：更新用户的关注数
        user = self.db.get(User, user_id)
        if user:
            user.followingCount += 1
        
        # ✅ 修改：更新被关注用户的粉丝数
        target_user = self.db.get(User, target_user_id)
        if target_user:
            target_user.followerCount += 1
        
        self.db.commit()
        return {"success": True, "message": "关注成功"}

    def unfollow_user(self, user_id: str, target_user_id: str) -> dict:
        """取消关注用户"""
        # ✅ 修改：使用新的 UserFollow 表
        follow = (
            self.db.query(UserFollow)
            .filter(
                UserFollow.userId == user_id,
                UserFollow.authorId == target_user_id,
            )
            .one_or_none()
        )
        
        if follow and follow.isFollowed:
            follow.isFollowed = False
            follow.isPending = False
            
            # ✅ 修改：更新用户的关注数
            user = self.db.get(User, user_id)
            if user and user.followingCount > 0:
                user.followingCount -= 1
            
            # ✅ 修改：更新被关注用户的粉丝数
            target_user = self.db.get(User, target_user_id)
            if target_user and target_user.followerCount > 0:
                target_user.followerCount -= 1
            
            self.db.commit()
            return {"success": True, "message": "已取消关注"}
        
        return {"success": True, "message": "未关注"}

    def get_all_users(self) -> list[UserItem]:
        """获取所有用户信息"""
        users = self.db.query(User).all()
        self.logger.info("get_all_users: total=%s", len(users))
        return [
            UserItem(
                id=user.userId,
                username=user.userName,  # ✅ 新增：返回username字段
                name=user.userName,
                avatar_url=user.avatarUrl,
                bio=user.bio,
                likes_count=0,
                following_count=int(user.followingCount if user.followingCount is not None else self._count_followings(user.userId)),
                followers_count=int(user.followerCount if user.followerCount is not None else self._count_followers(user.userId)),
            )
            for user in users
        ]

    def get_all_user_follows(self, user_id: str) -> list[dict]:
        """获取指定用户的所有关注关系"""
        follows = (
            self.db.query(UserFollow)
            .filter(UserFollow.userId == user_id)
            .all()
        )
        return [
            {
                "userId": follow.userId,
                "authorId": follow.authorId,
                "isFollowed": follow.isFollowed,
                "isPending": follow.isPending,
            }
            for follow in follows
        ]

    # -------------- 内部统计方法：兜底保证粉丝/关注数返回值 --------------
    def _count_followers(self, user_id: str) -> int:
        """统计粉丝数"""
        return (
            self.db.query(func.count(UserFollow.userId))
            .filter(UserFollow.authorId == user_id, UserFollow.isFollowed.is_(True))
            .scalar()
            or 0
        )

    def _count_followings(self, user_id: str) -> int:
        """统计关注数"""
        return (
            self.db.query(func.count(UserFollow.userId))
            .filter(UserFollow.userId == user_id, UserFollow.isFollowed.is_(True))
            .scalar()
            or 0
        )
