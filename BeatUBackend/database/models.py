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
    Integer,
    JSON,
    String,
    Text,
    UniqueConstraint,
)
from sqlalchemy.orm import declarative_base, relationship


Base = declarative_base()


class Video(Base):
    __tablename__ = "beatu_videos"

    id = Column(String(64), primary_key=True)
    play_url = Column(String(500), nullable=False)
    cover_url = Column(String(500), nullable=False)
    title = Column(String(200), nullable=False)
    tags = Column(JSON, default=list)
    duration_ms = Column(BigInteger, nullable=False, default=0)
    orientation = Column(Enum("PORTRAIT", "LANDSCAPE", name="video_orientation"), nullable=False, default="PORTRAIT")
    author_id = Column(String(64), nullable=False, index=True)
    author_name = Column(String(100), nullable=False)
    author_avatar = Column(String(500))
    like_count = Column(BigInteger, nullable=False, default=0)
    comment_count = Column(BigInteger, nullable=False, default=0)
    favorite_count = Column(BigInteger, nullable=False, default=0)
    share_count = Column(BigInteger, nullable=False, default=0)
    view_count = Column(BigInteger, nullable=False, default=0)
    qualities = Column(JSON, default=list)
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow, nullable=False)

    comments = relationship("Comment", back_populates="video", cascade="all, delete-orphan")


class Comment(Base):
    __tablename__ = "beatu_comments"

    id = Column(Integer, primary_key=True, autoincrement=True)
    video_id = Column(String(64), ForeignKey("beatu_videos.id"), nullable=False)
    author_id = Column(String(64), nullable=False)
    author_name = Column(String(100), nullable=False)
    author_avatar = Column(String(500))
    content = Column(Text, nullable=False)
    parent_id = Column(Integer, ForeignKey("beatu_comments.id"), nullable=True)
    is_ai_reply = Column(Boolean, default=False, nullable=False)
    ai_model = Column(String(64))
    ai_source = Column(String(32))
    ai_confidence = Column(Float)
    like_count = Column(BigInteger, default=0, nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)

    video = relationship("Video", back_populates="comments")
    parent = relationship("Comment", remote_side=[id])


class Interaction(Base):
    __tablename__ = "beatu_interactions"

    id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(String(64), nullable=False, index=True)
    video_id = Column(String(64), ForeignKey("beatu_videos.id"))
    author_id = Column(String(64))
    type = Column(
        Enum("LIKE", "FAVORITE", "FOLLOW_AUTHOR", name="interaction_type"),
        nullable=False,
    )
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)

    video = relationship("Video")

    __table_args__ = (
        UniqueConstraint("user_id", "video_id", "type", name="uniq_user_video_type"),
        UniqueConstraint("user_id", "author_id", "type", name="uniq_user_author_type"),
    )


class PlaybackMetric(Base):
    __tablename__ = "beatu_metrics_playback"

    id = Column(Integer, primary_key=True, autoincrement=True)
    video_id = Column(String(64), nullable=False)
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
    video_id = Column(String(64))
    latency_ms = Column(BigInteger)
    success = Column(Boolean, default=True)
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)


def serialize_json_field(value: Optional[list | dict], fallback):
    return value if value is not None else fallback



