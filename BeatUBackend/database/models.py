from __future__ import annotations

from datetime import datetime
from typing import Optional

from sqlalchemy import (
    BigInteger,
    Boolean,
    Column,
    DateTime,
    Enum,
    Float,
    ForeignKey,
    Index,
    Integer,
    JSON,
    String,
    Text,
    UniqueConstraint,
)
from sqlalchemy.orm import declarative_base, relationship


Base = declarative_base()


class User(Base):
    __tablename__ = "beatu_user"  # ✅ 修改：表名从 beatu_users 改为 beatu_user

    userId = Column(String(64), primary_key=True)  # ✅ 修改：字段名从 id 改为 userId
    userName = Column(String(100), nullable=False, unique=True)  # ✅ 修改：字段名从 nickname 改为 userName，添加 unique
    avatarUrl = Column(String(500))  # ✅ 修改：字段名从 avatar 改为 avatarUrl
    bio = Column(Text)
    followerCount = Column(BigInteger, nullable=False, default=0)  # ✅ 修改：字段名从 followers 改为 followerCount
    followingCount = Column(BigInteger, nullable=False, default=0)  # ✅ 修改：字段名从 followings 改为 followingCount

    videos = relationship("Video", back_populates="author", cascade="all, delete-orphan", foreign_keys="Video.authorId")
    watch_histories = relationship("WatchHistory", back_populates="user", cascade="all, delete-orphan")


class Video(Base):
    __tablename__ = "beatu_video"  # ✅ 修改：表名从 beatu_videos 改为 beatu_video

    videoId = Column(BigInteger, primary_key=True)  # ✅ 修改：字段名从 id 改为 videoId
    playUrl = Column(String(500), nullable=False)  # ✅ 修改：字段名从 play_url 改为 playUrl
    coverUrl = Column(String(500), nullable=False)  # ✅ 修改：字段名从 cover_url 改为 coverUrl
    title = Column(String(200), nullable=False)
    authorId = Column(String(64), ForeignKey("beatu_user.userId"), nullable=False, index=True)  # ✅ 修改：字段名从 author_id 改为 authorId，外键引用 beatu_user.userId
    orientation = Column(Enum("PORTRAIT", "LANDSCAPE", name="video_orientation"), nullable=False, default="PORTRAIT")
    durationMs = Column(BigInteger, nullable=False, default=0)  # ✅ 修改：字段名从 duration_ms 改为 durationMs
    likeCount = Column(BigInteger, nullable=False, default=0)  # ✅ 修改：字段名从 like_count 改为 likeCount
    commentCount = Column(BigInteger, nullable=False, default=0)  # ✅ 修改：字段名从 comment_count 改为 commentCount
    favoriteCount = Column(BigInteger, nullable=False, default=0)  # ✅ 修改：字段名从 favorite_count 改为 favoriteCount
    viewCount = Column(BigInteger, nullable=False, default=0)  # ✅ 修改：字段名从 view_count 改为 viewCount
    authorAvatar = Column(String(500))  # ✅ 修改：字段名从 author_avatar 改为 authorAvatar
    shareUrl = Column(String(500))  # ✅ 新增：分享链接

    comments = relationship("Comment", back_populates="video", cascade="all, delete-orphan")
    author = relationship("User", back_populates="videos", foreign_keys=[authorId])
    watch_histories = relationship("WatchHistory", back_populates="video", cascade="all, delete-orphan")


class Comment(Base):
    __tablename__ = "beatu_comment"  # ✅ 修改：表名从 beatu_comments 改为 beatu_comment

    commentId = Column(String(64), primary_key=True)  # ✅ 修改：字段名从 id 改为 commentId，类型从 Integer 改为 String(64)
    videoId = Column(BigInteger, ForeignKey("beatu_video.videoId"), nullable=False)  # ✅ 修改：字段名从 video_id 改为 videoId，外键引用 beatu_video.videoId
    authorId = Column(String(64), nullable=False)  # ✅ 修改：字段名从 author_id 改为 authorId
    content = Column(Text, nullable=False)
    createdAt = Column(BigInteger, nullable=False)  # ✅ 修改：字段名从 created_at 改为 createdAt，类型从 DateTime 改为 BigInteger（Unix时间戳毫秒）
    likeCount = Column(BigInteger, default=0, nullable=False)  # ✅ 修改：字段名从 like_count 改为 likeCount
    isLiked = Column(Boolean, default=False, nullable=False)  # ✅ 新增：是否点赞
    isPending = Column(Boolean, default=False, nullable=False)  # ✅ 新增：本地待同步状态
    authorAvatar = Column(String(500))  # ✅ 修改：字段名从 author_avatar 改为 authorAvatar

    video = relationship("Video", back_populates="comments")


class VideoInteraction(Base):
    """用户-视频互动表（点赞/收藏）"""
    __tablename__ = "beatu_video_interaction"  # ✅ 修改：新表结构

    videoId = Column(BigInteger, ForeignKey("beatu_video.videoId"), primary_key=True, nullable=False)  # ✅ 修改：复合主键
    userId = Column(String(64), ForeignKey("beatu_user.userId"), primary_key=True, nullable=False)  # ✅ 修改：复合主键
    isLiked = Column(Boolean, default=False, nullable=False)  # ✅ 新增：是否点赞
    isFavorited = Column(Boolean, default=False, nullable=False)  # ✅ 新增：是否收藏
    isPending = Column(Boolean, default=False, nullable=False)  # ✅ 新增：本地待同步状态

    video = relationship("Video")
    user = relationship("User", foreign_keys=[userId])

    __table_args__ = (
        Index("idx_userId", "userId"),
        Index("idx_videoId", "videoId"),
        Index("idx_isPending", "isPending"),
    )


class PlaybackMetric(Base):
    __tablename__ = "beatu_metrics_playback"

    id = Column(Integer, primary_key=True, autoincrement=True)
    video_id = Column(BigInteger, nullable=False)  # ✅ 修改：从 String(64) 改为 BigInteger
    fps = Column(Float)
    start_up_ms = Column(BigInteger)
    rebuffer_count = Column(Integer)
    memory_mb = Column(Float)
    channel = Column(String(32))
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)


class InteractionMetric(Base):
    __tablename__ = "beatu_metrics_interaction"

    id = Column(Integer, primary_key=True, autoincrement=True)
    event = Column(String(64), nullable=False)
    video_id = Column(BigInteger)  # ✅ 修改：从 String(64) 改为 BigInteger
    latency_ms = Column(BigInteger)
    success = Column(Boolean, default=True)
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)


class UserFollow(Base):
    __tablename__ = "beatu_user_follow"  # ✅ 修改：表名从 beatu_user_follows 改为 beatu_user_follow

    userId = Column(String(64), ForeignKey("beatu_user.userId"), primary_key=True, nullable=False)  # ✅ 修改：字段名从 follower_id 改为 userId，复合主键
    authorId = Column(String(64), ForeignKey("beatu_user.userId"), primary_key=True, nullable=False)  # ✅ 修改：字段名从 followee_id 改为 authorId，复合主键
    isFollowed = Column(Boolean, default=False, nullable=False)  # ✅ 新增：是否关注
    isPending = Column(Boolean, default=False, nullable=False)  # ✅ 新增：本地待同步状态

    user = relationship("User", foreign_keys=[userId])
    author = relationship("User", foreign_keys=[authorId])

    __table_args__ = (
        Index("idx_userId", "userId"),
        Index("idx_authorId", "authorId"),
        Index("idx_isPending", "isPending"),
    )


class WatchHistory(Base):
    __tablename__ = "beatu_watch_history"

    videoId = Column(BigInteger, ForeignKey("beatu_video.videoId"), primary_key=True, nullable=False)  # ✅ 修改：字段名从 video_id 改为 videoId，复合主键
    userId = Column(String(64), ForeignKey("beatu_user.userId"), primary_key=True, nullable=False)  # ✅ 修改：字段名从 user_id 改为 userId，复合主键
    lastPlayPositionMs = Column(BigInteger, default=0, nullable=False)  # ✅ 修改：字段名从 last_seek_ms 改为 lastPlayPositionMs
    watchedAt = Column(BigInteger, nullable=False)  # ✅ 修改：字段名从 last_watch_at 改为 watchedAt，类型从 DateTime 改为 BigInteger（Unix时间戳毫秒）
    isPending = Column(Boolean, default=False, nullable=False)  # ✅ 新增：本地待同步状态（弱一致性数据）

    user = relationship("User", back_populates="watch_histories")
    video = relationship("Video", back_populates="watch_histories")

    __table_args__ = (
        Index("idx_userId", "userId"),
        Index("idx_videoId", "videoId"),
        Index("idx_userId_watchedAt", "userId", "watchedAt"),
        Index("idx_isPending", "isPending"),
    )


def serialize_json_field(value: Optional[list | dict], fallback):
    return value if value is not None else fallback
