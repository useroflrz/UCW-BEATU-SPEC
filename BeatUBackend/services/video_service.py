from __future__ import annotations

from typing import List, Sequence

from sqlalchemy import func, select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from database.models import VideoInteraction, UserFollow, Video, User
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
        # ✅ 修改：使用新的字段名 videoId，按 videoId 降序排序
        query = select(Video).order_by(Video.videoId.desc())
        count_query = select(func.count(Video.videoId))

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

    def search_videos(
        self,
        *,
        query: str,
        page: int,
        limit: int,
        user_id: str,
    ) -> VideoList:
        """搜索视频（根据标题关键词）"""
        # 构建查询条件：在标题中搜索关键词
        search_query = select(Video).where(
            Video.title.like(f"%{query}%")
        ).order_by(Video.videoId.desc())
        
        count_query = select(func.count(Video.videoId)).where(
            Video.title.like(f"%{query}%")
        )
        
        total = self.db.scalar(count_query) or 0
        records = (
            self.db.execute(search_query.offset((page - 1) * limit).limit(limit)).scalars().all()
        )
        
        items = self._build_video_items(records, user_id=user_id, channel=None)
        return VideoList.create(items=items, total=total, page=page, limit=limit)

    def get_video(self, video_id: int, user_id: str) -> VideoItem:  # ✅ 修改：video_id 从 str 改为 int
        video = self.db.get(Video, video_id)
        if not video:
            raise ValueError("视频不存在")

        return self._build_video_items([video], user_id=user_id)[0]

    def like_video(self, video_id: int, payload: InteractionRequest, user_id: str) -> OperationResult:  # ✅ 修改：video_id 从 str 改为 int
        if payload.action not in ("LIKE", "UNLIKE"):
            raise ValueError("action 必须是 LIKE/UNLIKE")
        return self._handle_interaction(video_id, user_id, payload.action, "LIKE")

    def favorite_video(self, video_id: int, payload: InteractionRequest, user_id: str) -> OperationResult:  # ✅ 修改：video_id 从 str 改为 int
        if payload.action not in ("SAVE", "REMOVE"):
            raise ValueError("action 必须是 SAVE/REMOVE")
        action = "LIKE" if payload.action == "SAVE" else "UNLIKE"
        return self._handle_interaction(video_id, user_id, action, "FAVORITE")

    def share_video(self, video_id: int) -> OperationResult:  # ✅ 修改：video_id 从 str 改为 int
        """
        记录一次分享行为：将对应视频的 shareCount 自增 1。
        客户端负责实际的系统分享逻辑，这里只做统计。
        """
        video = self.db.get(Video, video_id)
        if not video:
            raise ValueError("视频不存在")

        # ✅ 修改：新表结构中没有 shareCount 字段，暂时不处理
        # 如果需要统计分享，可以单独创建分享统计表
        self.db.commit()

        return OperationResult(success=True, message="OK")

    def follow_author(self, payload: FollowRequest, user_id: str) -> OperationResult:
        author_id = payload.author_id
        # ✅ 修改：使用新的 UserFollow 表
        follow = (
            self.db.query(UserFollow)
            .filter(
                UserFollow.userId == user_id,
                UserFollow.authorId == author_id,
            )
            .one_or_none()
        )

        if payload.action == "FOLLOW":
            if follow and follow.isFollowed:
                return OperationResult(success=True, message="已关注")
            if follow:
                # 更新现有记录
                follow.isFollowed = True
                follow.isPending = False
            else:
                # 创建新记录
                entity = UserFollow(
                    userId=user_id,
                    authorId=author_id,
                    isFollowed=True,
                    isPending=False,
                )
                self.db.add(entity)
            
            # ✅ 修改：更新用户的关注数
            user = self.db.get(User, user_id)
            if user:
                user.followingCount += 1
            
            # ✅ 修改：更新被关注用户的粉丝数
            target_user = self.db.get(User, author_id)
            if target_user:
                target_user.followerCount += 1
            
            self.db.commit()
            return OperationResult(success=True, message="关注成功")
        else:
            # 取消关注
            if follow and follow.isFollowed:
                follow.isFollowed = False
                follow.isPending = False
                
                # ✅ 修改：更新用户的关注数
                user = self.db.get(User, user_id)
                if user and user.followingCount > 0:
                    user.followingCount -= 1
                
                # ✅ 修改：更新被关注用户的粉丝数
                target_user = self.db.get(User, author_id)
                if target_user and target_user.followerCount > 0:
                    target_user.followerCount -= 1
                
                self.db.commit()
            return OperationResult(success=True, message="已取消关注")

    def _handle_interaction(
        self,
        video_id: int,  # ✅ 修改：video_id 从 str 改为 int
        user_id: str,
        action: str,
        interaction_type: str,
    ) -> OperationResult:
        video = self.db.get(Video, video_id)
        if not video:
            raise ValueError("视频不存在")

        # ✅ 修改：使用新的 VideoInteraction 表
        interaction = (
            self.db.query(VideoInteraction)
            .filter(
                VideoInteraction.videoId == video_id,
                VideoInteraction.userId == user_id,
            )
            .one_or_none()
        )

        if action in ("LIKE", "SAVE"):
            # 点赞或收藏
            if interaction:
                # 更新现有记录
                if interaction_type == "LIKE":
                    interaction.isLiked = True
                elif interaction_type == "FAVORITE":
                    interaction.isFavorited = True
            else:
                # 创建新记录
                interaction = VideoInteraction(
                    videoId=video_id,
                    userId=user_id,
                    isLiked=(interaction_type == "LIKE"),
                    isFavorited=(interaction_type == "FAVORITE"),
                    isPending=False,
                )
                self.db.add(interaction)
            self._bump_counter(video, interaction_type, delta=1)
        else:
            # 取消点赞或取消收藏
            if interaction:
                if interaction_type == "LIKE":
                    interaction.isLiked = False
                elif interaction_type == "FAVORITE":
                    interaction.isFavorited = False
                # 如果既没有点赞也没有收藏，删除记录
                if not interaction.isLiked and not interaction.isFavorited:
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
            video.likeCount = max(0, video.likeCount + delta)  # ✅ 修改：字段名从 like_count 改为 likeCount
        elif interaction_type == "FAVORITE":
            video.favoriteCount = max(0, video.favoriteCount + delta)  # ✅ 修改：字段名从 favorite_count 改为 favoriteCount

    def _build_video_items(
        self,
        videos: Sequence[Video],
        user_id: str,
        channel: str | None = None,
    ) -> List[VideoItem]:
        video_ids = [video.videoId for video in videos]  # ✅ 修改：字段名从 id 改为 videoId
        author_ids = {video.authorId for video in videos}  # ✅ 修改：字段名从 author_id 改为 authorId

        # ✅ 修改：使用新的 VideoInteraction 表
        interactions = []
        if user_id:
            interactions = (
                self.db.query(VideoInteraction)
                .filter(VideoInteraction.userId == user_id, VideoInteraction.videoId.in_(video_ids))
                .all()
            )
        
        # ✅ 修改：使用新的 UserFollow 表
        follow_map = {}
        if user_id and author_ids:
            follows = (
                self.db.query(UserFollow)
                .filter(
                    UserFollow.userId == user_id,
                    UserFollow.authorId.in_(author_ids),
                    UserFollow.isFollowed == True,
                )
                .all()
            )
            follow_map = {follow.authorId: True for follow in follows}

        # ✅ 修改：构建互动映射
        interaction_map = {}
        for interaction in interactions:
            key = f"{interaction.videoId}"
            interaction_map[key] = {
                "isLiked": interaction.isLiked,
                "isFavorited": interaction.isFavorited,
            }

        # ✅ 优化：批量获取所有作者信息，避免 N+1 查询
        author_map = {}
        if author_ids:
            authors = self.db.query(User).filter(User.userId.in_(author_ids)).all()
            author_map = {author.userId: author for author in authors}

        items: List[VideoItem] = []
        for video in videos:
            video_key = f"{video.videoId}"
            interaction = interaction_map.get(video_key, {})
            
            # ✅ 优化：从批量查询的 author_map 中获取作者信息
            author = author_map.get(video.authorId)
            author_name = author.userName if author else video.authorId
            author_avatar = author.avatarUrl if author else None  # ✅ 修复：使用用户的 avatarUrl 而不是 video.authorAvatar
            
            items.append(
                VideoItem(
                    id=video.videoId,  # ✅ 修改：字段名从 id 改为 videoId
                    play_url=video.playUrl,  # ✅ 修改：字段名从 play_url 改为 playUrl
                    cover_url=video.coverUrl,  # ✅ 修改：字段名从 cover_url 改为 coverUrl
                    title=video.title,
                    tags=[],  # ✅ 修改：新表结构中没有 tags 字段
                    duration_ms=video.durationMs,  # ✅ 修改：字段名从 duration_ms 改为 durationMs
                    orientation=str(video.orientation).lower() if video.orientation else "portrait",
                    author_id=video.authorId,  # ✅ 修改：字段名从 author_id 改为 authorId
                    author_name=author_name,  # ✅ 修改：通过 authorId 查询 User 获取 userName
                    author_avatar=author_avatar,  # ✅ 修复：使用用户的 avatarUrl
                    like_count=video.likeCount,  # ✅ 修改：字段名从 like_count 改为 likeCount
                    comment_count=video.commentCount,  # ✅ 修改：字段名从 comment_count 改为 commentCount
                    favorite_count=video.favoriteCount,  # ✅ 修改：字段名从 favorite_count 改为 favoriteCount
                    share_count=0,  # ✅ 修改：新表结构中没有 share_count 字段
                    view_count=video.viewCount,  # ✅ 修改：字段名从 view_count 改为 viewCount
                    is_liked=interaction.get("isLiked", False),
                    is_favorited=interaction.get("isFavorited", False),
                    is_followed_author=follow_map.get(video.authorId, False),
                    qualities=[],  # ✅ 修改：新表结构中没有 qualities 字段
                    # 现有表中仅存储视频内容，统一标记为 VIDEO；图文卡片在后续注入时单独构造
                    contentType="VIDEO",
                    imageUrls=[],
                    bgmUrl=None,
                )
            )
        return items

    def get_all_video_interactions(self, user_id: str) -> list[dict]:
        """获取指定用户的所有视频交互"""
        interactions = (
            self.db.query(VideoInteraction)
            .filter(VideoInteraction.userId == user_id)
            .all()
        )
        return [
            {
                "videoId": interaction.videoId,
                "userId": interaction.userId,
                "isLiked": interaction.isLiked,
                "isFavorited": interaction.isFavorited,
                "isPending": interaction.isPending,
            }
            for interaction in interactions
        ]

    def build_mixed_feed(self, *, page: int, items: List[VideoItem]) -> List[VideoItem]:
        """
        根据页码对视频流做"图文+视频"混编：
        - 当前实现：在每一页的开头插入一条静态图文+BGM 卡片，便于前端体验图文页面
        - 后续如接入真实图文数据，可改为从数据库/推荐系统读取
        """
        if not items:
            return items

        # 当前实现：将数据库中的视频列表视为一个"块"，并为每一页构造一条图文+BGM
        # 然后在当前页范围内随机插入这条图文，以达到"随机混编"的效果
        #
        # 后续如果有真实图文数据，可以扩展为针对多条图文做随机插入/洗牌
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
        构造一条示例"图文+音乐"内容，由后端统一注入到推荐流中。
        说明：
        - id 使用 900000 + index 作为整数 ID，区分不同页的示例卡片
        - play_url 虽然必填，但在 IMAGE_POST 下前端不会使用，只要是合法 URL 即可
        """
        return VideoItem(
            id=900000 + index,  # ✅ 修改：从字符串改为整数 ID（使用 900000+ 范围避免与真实视频 ID 冲突）
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
