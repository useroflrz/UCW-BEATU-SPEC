from __future__ import annotations

from typing import List, Sequence

from sqlalchemy import func, select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from database.models import VideoInteraction, UserFollow, Video, User, WatchHistory
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
        import logging
        self.logger = logging.getLogger(__name__)

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
        """
        幂等处理点赞/收藏：
        - 如果状态未变化，直接返回成功
        - 尝试两次（遇到唯一键/外键冲突回滚后重试一次）
        """
        for attempt in range(2):
            try:
                video = self.db.get(Video, video_id)
                if not video:
                    raise ValueError("视频不存在")

                # 确保用户存在，避免外键约束导致首次互动失败
                user = self.db.get(User, user_id)
                if not user:
                    placeholder_user = User(
                        userId=user_id,
                        userName=user_id,
                        avatarUrl=None,
                        bio=None,
                        followerCount=0,
                        followingCount=0,
                    )
                    self.db.add(placeholder_user)

                interaction = (
                    self.db.query(VideoInteraction)
                    .filter(
                        VideoInteraction.videoId == video_id,
                        VideoInteraction.userId == user_id,
                    )
                    .one_or_none()
                )

                current_liked = interaction.isLiked if interaction else False
                current_favorited = interaction.isFavorited if interaction else False

                target_liked = current_liked
                target_favorited = current_favorited

                if action in ("LIKE", "SAVE"):
                    if interaction_type == "LIKE":
                        target_liked = True
                    elif interaction_type == "FAVORITE":
                        target_favorited = True
                else:  # UNLIKE / REMOVE
                    if interaction_type == "LIKE":
                        target_liked = False
                    elif interaction_type == "FAVORITE":
                        target_favorited = False

                # 幂等：状态未变化直接返回
                if (
                    interaction
                    and target_liked == current_liked
                    and target_favorited == current_favorited
                ):
                    return OperationResult(success=True, message="OK")

                # 计算计数增量
                delta_like = 0
                delta_fav = 0
                if target_liked != current_liked:
                    delta_like = 1 if target_liked else -1
                if target_favorited != current_favorited:
                    delta_fav = 1 if target_favorited else -1

                # 写入交互记录
                if interaction:
                    interaction.isLiked = target_liked
                    interaction.isFavorited = target_favorited
                    if not interaction.isLiked and not interaction.isFavorited:
                        self.db.delete(interaction)
                else:
                    # 创建新记录
                    interaction = VideoInteraction(
                        videoId=video_id,
                        userId=user_id,
                        isLiked=target_liked,
                        isFavorited=target_favorited,
                        isPending=False,
                    )
                    self.db.add(interaction)

                # 更新计数（保持非负）
                if delta_like:
                    self._bump_counter(video, "LIKE", delta_like)
                if delta_fav:
                    self._bump_counter(video, "FAVORITE", delta_fav)

                self.db.commit()
                return OperationResult(success=True, message="OK")

            except IntegrityError as exc:
                # 可能是并发导致的唯一键/外键冲突，回滚后重试一次
                self.db.rollback()
                if attempt == 1:
                    detail = str(getattr(exc, "orig", exc))
                    raise ValueError(f"互动状态冲突: {detail}")

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

    def get_all_watch_histories(self, user_id: str) -> list[dict]:
        """获取指定用户的所有观看历史"""
        histories = (
            self.db.query(WatchHistory)
            .filter(WatchHistory.userId == user_id)
            .all()
        )
        return [
            {
                "videoId": history.videoId,
                "userId": history.userId,
                "lastPlayPositionMs": history.lastPlayPositionMs,
                "watchedAt": history.watchedAt,
                "isPending": history.isPending,
            }
            for history in histories
        ]

    def sync_watch_histories(self, user_id: str, histories: list[dict]) -> OperationResult:
        """同步观看历史（批量更新或创建）"""
        try:
            self.logger.info(f"收到观看历史同步请求：user_id={user_id}, 历史记录数量={len(histories)}")
            if histories:
                self.logger.info(f"第一条历史记录示例：{histories[0]}")
            
            success_count = 0
            error_count = 0
            
            for idx, history_data in enumerate(histories):
                try:
                    self.logger.debug(f"处理历史记录[{idx}]：{history_data}")
                    
                    # 尝试不同的字段名变体
                    video_id = history_data.get("videoId") or history_data.get("video_id")
                    last_play_position_ms = history_data.get("lastPlayPositionMs") or history_data.get("last_play_position_ms") or history_data.get("lastPlayPositionMs", 0)
                    watched_at = history_data.get("watchedAt") or history_data.get("watched_at")
                    
                    self.logger.debug(f"解析后的值：video_id={video_id}, last_play_position_ms={last_play_position_ms}, watched_at={watched_at}")
                    
                    # 类型转换
                    if video_id is not None:
                        if isinstance(video_id, str):
                            try:
                                video_id = int(video_id)
                            except (ValueError, TypeError):
                                self.logger.warning(f"历史记录[{idx}]：videoId 无法转换为整数：{video_id}")
                                error_count += 1
                                continue
                        elif isinstance(video_id, float):
                            video_id = int(video_id)
                    
                    if watched_at is not None:
                        if isinstance(watched_at, str):
                            try:
                                watched_at = int(watched_at)
                            except (ValueError, TypeError):
                                self.logger.warning(f"历史记录[{idx}]：watchedAt 无法转换为整数：{watched_at}")
                                error_count += 1
                                continue
                        elif isinstance(watched_at, float):
                            watched_at = int(watched_at)
                    
                    if last_play_position_ms is not None:
                        if isinstance(last_play_position_ms, str):
                            try:
                                last_play_position_ms = int(last_play_position_ms)
                            except (ValueError, TypeError):
                                last_play_position_ms = 0
                        elif isinstance(last_play_position_ms, float):
                            last_play_position_ms = int(last_play_position_ms)
                    else:
                        last_play_position_ms = 0
                    
                    if not video_id or not watched_at:
                        self.logger.warning(f"历史记录[{idx}]：缺少必要字段，video_id={video_id}, watched_at={watched_at}")
                        error_count += 1
                        continue
                
                    # 检查视频是否存在（外键约束检查）
                    video_exists = (
                        self.db.query(Video)
                        .filter(Video.videoId == video_id)
                        .first()
                    )
                    
                    if not video_exists:
                        self.logger.warning(f"历史记录[{idx}]：视频不存在，跳过同步，video_id={video_id}")
                        error_count += 1
                        continue
                
                    # 查询现有记录
                    existing = (
                        self.db.query(WatchHistory)
                        .filter(
                            WatchHistory.videoId == video_id,
                            WatchHistory.userId == user_id
                        )
                        .one_or_none()
                    )
                    
                    if existing:
                        # 更新现有记录（保留最新的观看时间和进度）
                        if watched_at > existing.watchedAt:
                            existing.lastPlayPositionMs = last_play_position_ms
                            existing.watchedAt = watched_at
                            existing.isPending = False
                            self.logger.debug(f"历史记录[{idx}]：更新现有记录，video_id={video_id}")
                        else:
                            self.logger.debug(f"历史记录[{idx}]：跳过更新（时间戳更旧），video_id={video_id}")
                        success_count += 1
                    else:
                        # 创建新记录
                        new_history = WatchHistory(
                            videoId=video_id,
                            userId=user_id,
                            lastPlayPositionMs=last_play_position_ms,
                            watchedAt=watched_at,
                            isPending=False
                        )
                        self.db.add(new_history)
                        self.logger.debug(f"历史记录[{idx}]：创建新记录，video_id={video_id}")
                        success_count += 1
                except Exception as e:
                    self.logger.error(f"处理历史记录[{idx}]时出错：{e}", exc_info=True)
                    error_count += 1
                    continue
            
            self.db.commit()
            self.logger.info(f"观看历史同步完成：成功={success_count}, 失败={error_count}")
            return OperationResult(success=True, message=f"同步成功：成功={success_count}, 失败={error_count}")
        except Exception as e:
            self.db.rollback()
            return OperationResult(success=False, message=f"同步失败: {str(e)}")

    def build_mixed_feed(self, *, page: int, items: List[VideoItem]) -> List[VideoItem]:
        """
        根据页码对视频流做"图文+视频"混编：
        - 当前实现：在每一页中插入多条静态图文+BGM 卡片，便于前端体验图文页面
        - 后续如接入真实图文数据，可改为从数据库/推荐系统读取
        """
        if not items:
            return items

        from random import Random

        rng = Random()
        rng.seed(page)

        mixed: List[VideoItem] = list(items)
        
        # 获取所有可用的图文内容模板
        image_posts = self._get_all_mock_image_posts()
        
        # 根据页码选择要插入的图文内容（确保每页都有不同的图文）
        # 每页插入 1-2 条图文内容
        num_posts = min(2, len(image_posts))
        posts_to_insert = image_posts[:num_posts]
        
        # 随机插入位置，确保图文内容分散在视频流中
        for post in posts_to_insert:
            # 为每条图文生成唯一的ID（基于页码和索引）
            # 统计当前列表中已有的图文内容数量
            existing_image_posts = [x for x in mixed if x.content_type == "IMAGE_POST"]
            post_id = 900000 + page * 100 + len(existing_image_posts)
            post.id = post_id
            post.title = f"{post.title}（第 {page} 页）"
            
            # 在 [0, len(mixed)] 区间内随机选择插入位置（包括尾部）
            insert_index = rng.randint(0, len(mixed))
            mixed.insert(insert_index, post)
        
        return mixed

    def _get_all_mock_image_posts(self) -> List[VideoItem]:
        """
        获取所有写死的图文内容模板。
        返回多条不同主题的图文内容，用于混编到视频流中。
        """
        return [
            self._create_mock_image_post(
                title="🌅 日出东方，新的一天开始了",
                image_urls=[
                    "https://images.pexels.com/photos/572897/pexels-photo-572897.jpeg",
                    "https://images.pexels.com/photos/210186/pexels-photo-210186.jpeg",
                    "https://images.pexels.com/photos/1103970/pexels-photo-1103970.jpeg",
                ],
                bgm_url="https://samplelib.com/lib/preview/mp3/sample-6s.mp3",
                like_count=1314,
                comment_count=99,
                favorite_count=520,
            ),
            self._create_mock_image_post(
                title="🌸 春天的花海，美不胜收",
                image_urls=[
                    "https://images.pexels.com/photos/1324803/pexels-photo-1324803.jpeg",
                    "https://images.pexels.com/photos/1408221/pexels-photo-1408221.jpeg",
                    "https://images.pexels.com/photos/147411/italy-mountains-dawn-daybreak-147411.jpeg",
                ],
                bgm_url="https://samplelib.com/lib/preview/mp3/sample-6s.mp3",
                like_count=888,
                comment_count=66,
                favorite_count=333,
            ),
            self._create_mock_image_post(
                title="🏔️ 雪山之巅，一览众山小",
                image_urls=[
                    "https://images.pexels.com/photos/417074/pexels-photo-417074.jpeg",
                    "https://images.pexels.com/photos/1365425/pexels-photo-1365425.jpeg",
                    "https://images.pexels.com/photos/2387418/pexels-photo-2387418.jpeg",
                ],
                bgm_url="https://samplelib.com/lib/preview/mp3/sample-6s.mp3",
                like_count=2024,
                comment_count=168,
                favorite_count=666,
            ),
            self._create_mock_image_post(
                title="🌊 海浪拍岸，心旷神怡",
                image_urls=[
                    "https://images.pexels.com/photos/1007657/pexels-photo-1007657.jpeg",
                    "https://images.pexels.com/photos/1533720/pexels-photo-1533720.jpeg",
                    "https://images.pexels.com/photos/1631677/pexels-photo-1631677.jpeg",
                ],
                bgm_url="https://samplelib.com/lib/preview/mp3/sample-6s.mp3",
                like_count=999,
                comment_count=88,
                favorite_count=444,
            ),
            self._create_mock_image_post(
                title="🌙 夜晚的城市，灯火通明",
                image_urls=[
                    "https://images.pexels.com/photos/169647/pexels-photo-169647.jpeg",
                    "https://images.pexels.com/photos/417074/pexels-photo-417074.jpeg",
                    "https://images.pexels.com/photos/1365425/pexels-photo-1365425.jpeg",
                ],
                bgm_url="https://samplelib.com/lib/preview/mp3/sample-6s.mp3",
                like_count=777,
                comment_count=55,
                favorite_count=222,
            ),
        ]

    def _create_mock_image_post(
        self,
        title: str,
        image_urls: List[str],
        bgm_url: str,
        like_count: int = 0,
        comment_count: int = 0,
        favorite_count: int = 0,
        index: int = 0,
    ) -> VideoItem:
        """
        构造一条示例"图文+音乐"内容，由后端统一注入到推荐流中。
        
        参数：
        - title: 图文标题
        - image_urls: 图片URL列表（至少1张）
        - bgm_url: 背景音乐URL
        - like_count: 点赞数
        - comment_count: 评论数
        - favorite_count: 收藏数
        - index: 索引（用于生成唯一ID）
        
        说明：
        - id 使用 900000 + index 作为整数 ID，区分不同页的示例卡片
        - play_url 虽然必填，但在 IMAGE_POST 下前端不会使用，只要是合法 URL 即可
        """
        # 使用第一张图片作为封面
        cover_url = image_urls[0] if image_urls else "https://images.pexels.com/photos/572897/pexels-photo-572897.jpeg"
        
        return VideoItem(
            id=900000 + index,  # ✅ 修改：从字符串改为整数 ID（使用 900000+ 范围避免与真实视频 ID 冲突）
            play_url="https://samplelib.com/lib/preview/mp4/sample-5s.mp4",
            cover_url=cover_url,
            title=title,
            tags=[],
            duration_ms=0,
            orientation="portrait",
            author_id="beatu-official",
            author_name="BeatU 官方",
            author_avatar=None,
            like_count=like_count,
            comment_count=comment_count,
            favorite_count=favorite_count,
            share_count=66,
            view_count=0,
            is_liked=False,
            is_favorited=False,
            is_followed_author=False,
            qualities=[],
            contentType="IMAGE_POST",
            imageUrls=image_urls,
            bgmUrl=bgm_url,
        )
