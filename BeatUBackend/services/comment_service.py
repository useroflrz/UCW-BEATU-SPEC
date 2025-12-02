from __future__ import annotations

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from database.models import Comment, Video
from schemas.api import CommentAIRequest, CommentCreate, CommentItem, CommentList


class CommentService:
    def __init__(self, db: Session) -> None:
        self.db = db

    def list_comments(self, video_id: str, page: int, limit: int) -> CommentList:
        total = self.db.scalar(
            select(func.count(Comment.id)).where(Comment.video_id == video_id)
        ) or 0

        query = (
            select(Comment)
            .where(Comment.video_id == video_id)
            .order_by(Comment.created_at.desc())
            .offset((page - 1) * limit)
            .limit(limit)
        )
        items = self.db.execute(query).scalars().all()
        return CommentList.create(
            items=[self._to_schema(comment) for comment in items],
            total=total,
            page=page,
            limit=limit,
        )

    def create_comment(self, video_id: str, payload: CommentCreate, user_id: str, user_name: str) -> CommentItem:
        video = self.db.get(Video, video_id)
        if not video:
            raise ValueError("视频不存在")

        entity = Comment(
            video_id=video_id,
            author_id=user_id,
            author_name=user_name,
            content=payload.content,
            parent_id=int(payload.reply_to) if payload.reply_to else None,
        )
        self.db.add(entity)
        video.comment_count += 1
        self.db.commit()
        self.db.refresh(entity)
        return self._to_schema(entity)

    def create_ai_comment(
        self,
        video_id: str,
        payload: CommentAIRequest,
        user_name: str,
        override_content: str | None = None,
    ) -> CommentItem:
        video = self.db.get(Video, video_id)
        if not video:
            raise ValueError("视频不存在")
        content = override_content or self._compose_ai_answer(payload.question, video.title)
        entity = Comment(
            video_id=video_id,
            author_id="ai_beatu",
            author_name="@元宝",
            content=content,
            is_ai_reply=True,
            ai_model="beatu-mini",
            ai_source="qa",
            ai_confidence=0.87,
        )
        self.db.add(entity)
        video.comment_count += 1
        self.db.commit()
        self.db.refresh(entity)
        return self._to_schema(entity)

    def _to_schema(self, comment: Comment) -> CommentItem:
        return CommentItem(
            id=str(comment.id),
            video_id=comment.video_id,
            author_id=comment.author_id,
            author_name=comment.author_name,
            author_avatar=comment.author_avatar,
            content=comment.content,
            created_at=comment.created_at,
            is_ai_reply=comment.is_ai_reply,
            ai_model=comment.ai_model,
            ai_source=comment.ai_source,
            ai_confidence=comment.ai_confidence,
            like_count=comment.like_count,
        )

    def _compose_ai_answer(self, question: str, title: str) -> str:
        return f"基于《{title}》的内容，我认为：{question} —— 继续保持好奇，更多精彩马上呈现。"



