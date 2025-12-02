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
            .where(Video.id != payload.video_id)
            .order_by(Video.view_count.desc())
            .limit(5)
        )
        records = self.db.execute(query).scalars().all()
        if not records:
            records = self.db.execute(select(Video).limit(5)).scalars().all()
        videos = [
            VideoItem(
                id=video.id,
                play_url=video.play_url,
                cover_url=video.cover_url,
                title=video.title,
                tags=parse_tag_list(video.tags),
                duration_ms=video.duration_ms,
                orientation=video.orientation.lower(),
                author_id=video.author_id,
                author_name=video.author_name,
                author_avatar=video.author_avatar,
                like_count=video.like_count,
                comment_count=video.comment_count,
                favorite_count=video.favorite_count,
                share_count=video.share_count,
                view_count=video.view_count,
                qualities=parse_quality_list(video.qualities),
            )
            for video in records
        ]
        reason = "结合播放完成度与兴趣标签，为你准备的下一支好片"
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
        return f"关于《{payload.video_id}》：{payload.question}。建议继续关注剧情发展，更多彩蛋等你发现。"




