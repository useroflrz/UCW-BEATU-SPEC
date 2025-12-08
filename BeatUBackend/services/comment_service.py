from __future__ import annotations

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from database.models import Comment, Video, User
from datetime import datetime
from schemas.api import CommentAIRequest, CommentCreate, CommentItem, CommentList


class CommentService:
    def __init__(self, db: Session) -> None:
        self.db = db

    def list_comments(self, video_id: int, page: int, limit: int) -> CommentList:  # ✅ 修改：video_id 从 str 改为 int
        total = self.db.scalar(
            select(func.count(Comment.commentId)).where(Comment.videoId == video_id)  # ✅ 修改：字段名从 id/video_id 改为 commentId/videoId
        ) or 0

        query = (
            select(Comment)
            .where(Comment.videoId == video_id)  # ✅ 修改：字段名从 video_id 改为 videoId
            .order_by(Comment.createdAt.desc())  # ✅ 修改：字段名从 created_at 改为 createdAt
            .offset((page - 1) * limit)
            .limit(limit)
        )
        items = self.db.execute(query).scalars().all()
        
        # ✅ 优化：批量获取所有评论作者信息，避免 N+1 查询
        author_ids = {comment.authorId for comment in items}
        author_map = {}
        if author_ids:
            authors = self.db.query(User).filter(User.userId.in_(author_ids)).all()
            author_map = {author.userId: author for author in authors}
        
        return CommentList.create(
            items=[self._to_schema(comment, author_map) for comment in items],
            total=total,
            page=page,
            limit=limit,
        )

    def create_comment(self, video_id: int, payload: CommentCreate, user_id: str, user_name: str) -> CommentItem:  # ✅ 修改：video_id 从 str 改为 int
        video = self.db.get(Video, video_id)
        if not video:
            raise ValueError("视频不存在")

        # ✅ 修改：生成评论ID（使用时间戳+随机数）
        import uuid
        comment_id = f"comment_{int(datetime.utcnow().timestamp() * 1000)}_{uuid.uuid4().hex[:8]}"
        
        # ✅ 修改：获取用户信息
        user = self.db.get(User, user_id)
        author_avatar = user.avatarUrl if user else None

        entity = Comment(
            commentId=comment_id,  # ✅ 修改：字段名从 id 改为 commentId
            videoId=video_id,  # ✅ 修改：字段名从 video_id 改为 videoId
            authorId=user_id,  # ✅ 修改：字段名从 author_id 改为 authorId
            content=payload.content,
            createdAt=int(datetime.utcnow().timestamp() * 1000),  # ✅ 修改：字段名从 created_at 改为 createdAt，使用Unix时间戳毫秒
            likeCount=0,  # ✅ 修改：字段名从 like_count 改为 likeCount
            isLiked=False,  # ✅ 新增：是否点赞
            isPending=False,  # ✅ 新增：本地待同步状态
            authorAvatar=author_avatar,  # ✅ 修改：字段名从 author_avatar 改为 authorAvatar
        )
        self.db.add(entity)
        video.commentCount += 1  # ✅ 修改：字段名从 comment_count 改为 commentCount
        self.db.commit()
        self.db.refresh(entity)
        return self._to_schema(entity)

    def create_ai_comment(
        self,
        video_id: int,  # ✅ 修改：video_id 从 str 改为 int
        payload: CommentAIRequest,
        user_name: str,
        override_content: str | None = None,
    ) -> CommentItem:
        video = self.db.get(Video, video_id)
        if not video:
            raise ValueError("视频不存在")
        content = override_content or self._compose_ai_answer(payload.question, video.title)
        
        # ✅ 修改：生成评论ID
        import uuid
        comment_id = f"comment_ai_{int(datetime.utcnow().timestamp() * 1000)}_{uuid.uuid4().hex[:8]}"
        
        # ✅ 修改：获取AI用户信息
        ai_user = self.db.get(User, "ai_beatu")
        author_avatar = ai_user.avatarUrl if ai_user else None

        entity = Comment(
            commentId=comment_id,  # ✅ 修改：字段名从 id 改为 commentId
            videoId=video_id,  # ✅ 修改：字段名从 video_id 改为 videoId
            authorId="ai_beatu",  # ✅ 修改：字段名从 author_id 改为 authorId
            content=content,
            createdAt=int(datetime.utcnow().timestamp() * 1000),  # ✅ 修改：字段名从 created_at 改为 createdAt
            likeCount=0,  # ✅ 修改：字段名从 like_count 改为 likeCount
            isLiked=False,  # ✅ 新增：是否点赞
            isPending=False,  # ✅ 新增：本地待同步状态
            authorAvatar=author_avatar,  # ✅ 修改：字段名从 author_avatar 改为 authorAvatar
        )
        self.db.add(entity)
        video.commentCount += 1  # ✅ 修改：字段名从 comment_count 改为 commentCount
        self.db.commit()
        self.db.refresh(entity)
        return self._to_schema(entity)

    def _to_schema(self, comment: Comment, author_map: dict = None) -> CommentItem:
        # ✅ 优化：从批量查询的 author_map 中获取用户信息，避免 N+1 查询
        if author_map is None:
            # 兼容旧代码：如果没有传入 author_map，则单独查询（不推荐）
            user = self.db.get(User, comment.authorId)
            author_name = user.userName if user else comment.authorId
        else:
            user = author_map.get(comment.authorId)
            author_name = user.userName if user else comment.authorId
        
        # ✅ 修改：将 createdAt（Unix时间戳毫秒）转换为 ISO 8601 格式
        from datetime import datetime
        created_at_iso = datetime.fromtimestamp(comment.createdAt / 1000.0).isoformat() + "Z"
        
        return CommentItem(
            id=comment.commentId,  # ✅ 修改：字段名从 id 改为 commentId
            video_id=int(comment.videoId),  # ✅ 修改：字段名从 video_id 改为 videoId
            author_id=comment.authorId,  # ✅ 修改：字段名从 author_id 改为 authorId
            author_name=author_name,  # ✅ 修改：通过 authorId 查询 User 获取 userName
            author_avatar=comment.authorAvatar,  # ✅ 修改：字段名从 author_avatar 改为 authorAvatar
            content=comment.content,
            created_at=created_at_iso,  # ✅ 修改：将 Unix 时间戳转换为 ISO 8601 格式
            is_ai_reply=(comment.authorId == "ai_beatu"),  # ✅ 修改：通过 authorId 判断是否为 AI 回复
            ai_model=None,  # ✅ 修改：新表结构中没有 ai_model 字段
            ai_source=None,  # ✅ 修改：新表结构中没有 ai_source 字段
            ai_confidence=None,  # ✅ 修改：新表结构中没有 ai_confidence 字段
            like_count=comment.likeCount,  # ✅ 修改：字段名从 like_count 改为 likeCount
        )

    def _compose_ai_answer(self, question: str, title: str) -> str:
        return f"基于《{title}》的内容，我认为：{question} — 继续保持好奇，更多精彩马上呈现！"
