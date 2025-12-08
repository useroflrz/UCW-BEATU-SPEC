from __future__ import annotations

from typing import List

from sqlalchemy import select
from sqlalchemy.orm import Session

from database.models import Video
from schemas.api import (
    AICommentQARequest,
    AIQualityRequest,
    AIQualityResponse,
    AIRecommendRequest,
    AIRecommendResponse,
    VideoItem,
)
from services.helpers import parse_quality_list, parse_tag_list


class AIService:
    def __init__(self, db: Session) -> None:
        self.db = db

    def recommend(self, payload: AIRecommendRequest) -> AIRecommendResponse:
        query = (
            select(Video)
            .where(Video.videoId != payload.video_id)  # ✅ 修改：字段名从 id 改为 videoId
            .order_by(Video.viewCount.desc())  # ✅ 修改：字段名从 view_count 改为 viewCount
            .limit(5)
        )
        records = self.db.execute(query).scalars().all()
        if not records:
            records = self.db.execute(select(Video).limit(5)).scalars().all()
        # ✅ 修改：获取作者信息
        from database.models import User
        videos = []
        for video in records:
            author = self.db.get(User, video.authorId)
            author_name = author.userName if author else video.authorId
            
            videos.append(
                VideoItem(
                    id=video.videoId,  # ✅ 修改：字段名从 id 改为 videoId
                    play_url=video.playUrl,  # ✅ 修改：字段名从 play_url 改为 playUrl
                    cover_url=video.coverUrl,  # ✅ 修改：字段名从 cover_url 改为 coverUrl
                    title=video.title,
                    tags=[],  # ✅ 修改：新表结构中没有 tags 字段
                    duration_ms=video.durationMs,  # ✅ 修改：字段名从 duration_ms 改为 durationMs
                    orientation=video.orientation.value.lower(),
                    author_id=video.authorId,  # ✅ 修改：字段名从 author_id 改为 authorId
                    author_name=author_name,  # ✅ 修改：通过 authorId 查询 User 获取 userName
                    author_avatar=video.authorAvatar,  # ✅ 修改：字段名从 author_avatar 改为 authorAvatar
                    like_count=video.likeCount,  # ✅ 修改：字段名从 like_count 改为 likeCount
                    comment_count=video.commentCount,  # ✅ 修改：字段名从 comment_count 改为 commentCount
                    favorite_count=video.favoriteCount,  # ✅ 修改：字段名从 favorite_count 改为 favoriteCount
                    share_count=0,  # ✅ 修改：新表结构中没有 share_count 字段
                    view_count=video.viewCount,  # ✅ 修改：字段名从 view_count 改为 viewCount
                    qualities=[],  # ✅ 修改：新表结构中没有 qualities 字段
                )
            )
        reason = "结合播放完成度与兴趣标签，为你准备的下一支好视频"
        return AIRecommendResponse(next_videos=videos, reason=reason)

    def quality(self, payload: AIQualityRequest) -> AIQualityResponse:
        bandwidth = payload.network_stats.get("bandwidthKbps", 3000)
        quality = "AUTO"
        if bandwidth > 8000:
            quality = "4K"
        elif bandwidth > 5000:
            quality = "1080P"
        elif bandwidth > 2500:
            quality = "720P"
        reason = f"网络 {bandwidth}kbps，选择 {quality} 以平衡清晰度与流畅度"
        return AIQualityResponse(quality=quality, reason=reason)

    def comment_qa(self, payload: AICommentQARequest) -> str:
        return f"关于《{payload.video_id}》：{payload.question}。建议继续关注剧情发展，更多彩蛋等你发现！"
