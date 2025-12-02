from __future__ import annotations

from typing import List, Sequence

from sqlalchemy import func, select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from database.models import Interaction, Video
from schemas.api import (
    FollowRequest,
    InteractionRequest,
    OperationResult,
    VideoItem,
    VideoList,
)
from services.helpers import parse_bool_map, parse_quality_list, parse_tag_list


class VideoService:
    def __init__(self, db: Session) -> None:
        self.db = db

    def list_videos(
        self,
        *,
        page: int,
        limit: int,
        orientation: str | None,
        channel: str | None,
        user_id: str,
    ) -> VideoList:
        query = select(Video).order_by(Video.created_at.desc())
        count_query = select(func.count(Video.id))

        if orientation:
            orm_orientation = orientation.upper()
            query = query.where(Video.orientation == orm_orientation)
            count_query = count_query.where(Video.orientation == orm_orientation)

        total = self.db.scalar(count_query) or 0
        records = (
            self.db.execute(query.offset((page - 1) * limit).limit(limit)).scalars().all()
        )

        items = self._build_video_items(records, user_id=user_id, channel=channel)
        return VideoList.create(items=items, total=total, page=page, limit=limit)

    def get_video(self, video_id: str, user_id: str) -> VideoItem:
        video = self.db.get(Video, video_id)
        if not video:
            raise ValueError("视频不存在")

        return self._build_video_items([video], user_id=user_id)[0]

    def like_video(self, video_id: str, payload: InteractionRequest, user_id: str) -> OperationResult:
        if payload.action not in ("LIKE", "UNLIKE"):
            raise ValueError("action 必须是 LIKE/UNLIKE")
        return self._handle_interaction(video_id, user_id, payload.action, "LIKE")

    def favorite_video(self, video_id: str, payload: InteractionRequest, user_id: str) -> OperationResult:
        if payload.action not in ("SAVE", "REMOVE"):
            raise ValueError("action 必须是 SAVE/REMOVE")
        action = "LIKE" if payload.action == "SAVE" else "UNLIKE"
        return self._handle_interaction(video_id, user_id, action, "FAVORITE")

    def follow_author(self, payload: FollowRequest, user_id: str) -> OperationResult:
        author_id = payload.author_id
        interaction = (
            self.db.query(Interaction)
            .filter(
                Interaction.user_id == user_id,
                Interaction.author_id == author_id,
                Interaction.type == "FOLLOW_AUTHOR",
            )
            .one_or_none()
        )
        if payload.action == "FOLLOW":
            if interaction:
                return OperationResult(success=True, message="已关注")
            entity = Interaction(
                user_id=user_id,
                author_id=author_id,
                type="FOLLOW_AUTHOR",
            )
            self.db.add(entity)
            self.db.commit()
            return OperationResult(success=True, message="关注成功")
        if interaction:
            self.db.delete(interaction)
            self.db.commit()
        return OperationResult(success=True, message="已取消关注")

    def _handle_interaction(
        self,
        video_id: str,
        user_id: str,
        action: str,
        interaction_type: str,
    ) -> OperationResult:
        video = self.db.get(Video, video_id)
        if not video:
            raise ValueError("视频不存在")

        interaction = (
            self.db.query(Interaction)
            .filter(
                Interaction.video_id == video_id,
                Interaction.user_id == user_id,
                Interaction.type == interaction_type,
            )
            .one_or_none()
        )

        if action in ("LIKE", "SAVE"):
            if interaction:
                return OperationResult(success=True, message="已处理")
            entity = Interaction(
                video_id=video_id,
                user_id=user_id,
                type=interaction_type,
            )
            self.db.add(entity)
            self._bump_counter(video, interaction_type, delta=1)
        else:
            if interaction:
                self.db.delete(interaction)
                self._bump_counter(video, interaction_type, delta=-1)

        try:
            self.db.commit()
        except IntegrityError:
            self.db.rollback()
            raise ValueError("互动状态冲突")

        return OperationResult(success=True, message="OK")

    def _bump_counter(self, video: Video, interaction_type: str, delta: int) -> None:
        if interaction_type == "LIKE":
            video.like_count = max(0, video.like_count + delta)
        elif interaction_type == "FAVORITE":
            video.favorite_count = max(0, video.favorite_count + delta)

    def _build_video_items(
        self,
        videos: Sequence[Video],
        user_id: str,
        channel: str | None = None,
    ) -> List[VideoItem]:
        video_ids = [video.id for video in videos]
        author_ids = {video.author_id for video in videos}

        interactions = []
        if user_id:
            interactions = (
                self.db.query(Interaction)
                .filter(Interaction.user_id == user_id, Interaction.video_id.in_(video_ids))
                .all()
            )
        follow_map = {}
        if user_id and author_ids:
            follow_map = parse_bool_map(
                self.db.query(Interaction)
                .filter(
                    Interaction.user_id == user_id,
                    Interaction.author_id.in_(author_ids),
                    Interaction.type == "FOLLOW_AUTHOR",
                )
                .all(),
                key=lambda it: it.author_id,
            )

        interaction_map = parse_bool_map(interactions, key=lambda it: f"{it.video_id}:{it.type}")

        items: List[VideoItem] = []
        for video in videos:
            items.append(
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
                    is_liked=interaction_map.get(f"{video.id}:LIKE", False),
                    is_favorited=interaction_map.get(f"{video.id}:FAVORITE", False),
                    is_followed_author=follow_map.get(video.author_id, False),
                    qualities=parse_quality_list(video.qualities) or [],
                    # 现有表中仅存储视频内容，统一标记为 VIDEO；图文卡片在后续注入时单独构造
                    contentType="VIDEO",
                    imageUrls=[],
                    bgmUrl=None,
                )
            )
        return items

    def build_mixed_feed(self, *, page: int, items: List[VideoItem]) -> List[VideoItem]:
        """
        根据页码对视频流做“图文+视频”混编：
        - 当前实现：在每一页的开头插入一条静态图文+BGM 卡片，便于前端体验图文页。
        - 后续如接入真实图文数据，可改为从数据库/推荐系统读取。
        """
        if not items:
            return items

        # 当前实现：将数据库中的视频列表视为一个“块”，并为每一页构造一条图文+BGM，
        # 然后在当前页范围内随机插入这条图文，以达到“随机混编”的效果。
        #
        # 后续如果有真实图文数据，可以扩展为针对多条图文做随机插入/洗牌。
        from random import Random

        rng = Random()
        rng.seed(page)

        mixed: List[VideoItem] = list(items)
        image_post = self._create_mock_image_post(page)

        # 在 [0, len(mixed)] 区间内随机选择插入位置（包括尾部）
        insert_index = rng.randint(0, len(mixed))
        mixed.insert(insert_index, image_post)
        return mixed

    def _create_mock_image_post(self, index: int) -> VideoItem:
        """
        构造一条示例“图文+音乐”内容，由后端统一注入到推荐流中。
        说明：
        - id 以 image_post_mock_{index} 区分不同页的示例卡片
        - play_url 虽然必填，但在 IMAGE_POST 下前端不会使用，只要是合法 URL 即可
        """
        return VideoItem(
            id=f"image_post_mock_{index}",
            play_url="https://samplelib.com/lib/preview/mp4/sample-5s.mp4",
            cover_url="https://images.pexels.com/photos/572897/pexels-photo-572897.jpeg",
            title=f"这是一个图文+BGM 示例（第 {index} 段，由后端注入）",
            tags=[],
            duration_ms=0,
            orientation="portrait",
            author_id="beatu-official",
            author_name="BeatU 官方",
            author_avatar=None,
            like_count=1314,
            comment_count=99,
            favorite_count=520,
            share_count=66,
            view_count=0,
            is_liked=False,
            is_favorited=False,
            is_followed_author=False,
            qualities=[],
            contentType="IMAGE_POST",
            imageUrls=[
                "https://images.pexels.com/photos/572897/pexels-photo-572897.jpeg",
                "https://images.pexels.com/photos/210186/pexels-photo-210186.jpeg",
                "https://images.pexels.com/photos/1103970/pexels-photo-1103970.jpeg",
            ],
            bgmUrl="https://samplelib.com/lib/preview/mp3/sample-6s.mp3",
        )



