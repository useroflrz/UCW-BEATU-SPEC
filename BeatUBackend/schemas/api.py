from __future__ import annotations

from datetime import datetime
from typing import Generic, List, Optional, TypeVar

from pydantic import AnyHttpUrl, BaseModel, Field


def to_camel(string: str) -> str:
    parts = string.split("_")
    return parts[0] + "".join(part.capitalize() for part in parts[1:])


class APIModel(BaseModel):
    class Config:
        populate_by_name = True  # �?修复：Pydantic V2 使用 populate_by_name 替代 allow_population_by_field_name
        alias_generator = to_camel
        from_attributes = True  # �?修复：Pydantic V2 使用 from_attributes 替代 orm_mode


class VideoQuality(APIModel):
    label: str
    bitrate: Optional[int] = None
    resolution: Optional[str] = None
    url: AnyHttpUrl


class VideoItem(APIModel):
    id: int  # �?修改：从 str 改为 int (Long)
    play_url: AnyHttpUrl
    cover_url: AnyHttpUrl
    title: str
    tags: List[str] = Field(default_factory=list)
    duration_ms: int
    orientation: str
    author_id: str
    author_name: str
    author_avatar: Optional[AnyHttpUrl] = None
    like_count: int
    comment_count: int
    favorite_count: int
    share_count: int
    view_count: int
    is_liked: bool = False
    is_favorited: bool = False
    is_followed_author: bool = False
    qualities: List[VideoQuality] = Field(default_factory=list)
    # Feed 内容类型�?
    # - VIDEO：常规短视频（有画面+声音�?
    # - IMAGE_POST：图�?音乐（多张图片轮�?+ 背景音乐，仅音频播放�?
    content_type: str = Field(default="VIDEO", alias="contentType")
    # 图文卡片专用字段：多张图片地址
    image_urls: List[AnyHttpUrl] = Field(default_factory=list, alias="imageUrls")
    # 图文+BGM 或视频自定义 BGM 的音频地址
    bgm_url: AnyHttpUrl | None = Field(default=None, alias="bgmUrl")


class Pagination(APIModel):
    total: int
    page: int
    limit: int


class VideoList(APIModel):
    items: List[VideoItem]
    total: int
    page: int
    page_size: int = Field(alias="pageSize")
    limit: int  # 保持兼容�?
    total_pages: int = Field(default=0, alias="totalPages")
    has_next: bool = Field(default=False, alias="hasNext")
    has_previous: bool = Field(default=False, alias="hasPrevious")
    
    @classmethod
    def create(cls, items: List[VideoItem], total: int, page: int, limit: int):
        """创建分页响应，自动计算totalPages、hasNext、hasPrevious"""
        total_pages = (total + limit - 1) // limit if limit > 0 else 0
        return cls(
            items=items,
            total=total,
            page=page,
            pageSize=limit,
            limit=limit,
            totalPages=total_pages,
            hasNext=page < total_pages,
            hasPrevious=page > 1
        )


class InteractionRequest(APIModel):
    action: str = Field(pattern="^(LIKE|UNLIKE|SAVE|REMOVE|FOLLOW|UNFOLLOW)$")  # �?修复：Pydantic V2 使用 pattern 替代 regex


class FollowRequest(APIModel):
    author_id: str
    action: str = Field(pattern="^(FOLLOW|UNFOLLOW)$")  # �?修复：Pydantic V2 使用 pattern 替代 regex


class OperationResult(APIModel):
    success: bool
    message: str = "OK"


class CommentItem(APIModel):
    id: str
    video_id: int  # �?修改：从 str 改为 int (Long)
    author_id: str
    author_name: str
    author_avatar: Optional[AnyHttpUrl] = None
    content: str
    created_at: datetime
    is_ai_reply: bool = False
    ai_model: Optional[str] = None
    ai_source: Optional[str] = None
    ai_confidence: Optional[float] = None
    like_count: int = 0


class CommentList(APIModel):
    items: List[CommentItem]
    total: int
    page: int
    page_size: int = Field(alias="pageSize")
    limit: int  # 保持兼容�?
    total_pages: int = Field(default=0, alias="totalPages")
    has_next: bool = Field(default=False, alias="hasNext")
    has_previous: bool = Field(default=False, alias="hasPrevious")
    
    @classmethod
    def create(cls, items: List[CommentItem], total: int, page: int, limit: int):
        """创建分页响应，自动计算totalPages、hasNext、hasPrevious"""
        total_pages = (total + limit - 1) // limit if limit > 0 else 0
        return cls(
            items=items,
            total=total,
            page=page,
            pageSize=limit,
            limit=limit,
            totalPages=total_pages,
            hasNext=page < total_pages,
            hasPrevious=page > 1
        )


class CommentCreate(APIModel):
    content: str = Field(..., min_length=1, max_length=500)
    reply_to: Optional[str] = Field(default=None, alias="replyTo")


class CommentAIRequest(APIModel):
    question: str = Field(..., min_length=1, max_length=200)


class AIRecommendRequest(APIModel):
    video_id: int  # �?修改：从 str 改为 int (Long)
    dwell_ms: int
    consumed_duration_ms: int
    tags: Optional[List[str]] = None


class AIRecommendResponse(APIModel):
    next_videos: List[VideoItem]
    reason: str


class AIQualityRequest(APIModel):
    video_id: int  # �?修改：从 str 改为 int (Long)
    network_stats: dict
    device_stats: dict


class AIQualityResponse(APIModel):
    quality: str
    reason: str


class AICommentQARequest(APIModel):
    video_id: int  # �?修改：从 str 改为 int (Long)
    question: str


class AISearchRequest(APIModel):
    """AI 搜索请求"""
    user_query: str = Field(..., min_length=1, max_length=500, description="用户查询文本")


class MetricsPlayback(APIModel):
    video_id: int  # �?修改：从 str 改为 int (Long)
    fps: Optional[float] = None
    start_up_ms: Optional[int] = None
    rebuffer_count: Optional[int] = None
    memory_mb: Optional[float] = None
    channel: Optional[str] = None


class MetricsInteraction(APIModel):
    event: str
    video_id: Optional[int] = None  # �?修改：从 Optional[str] 改为 Optional[int]
    latency_ms: Optional[int] = None
    success: Optional[bool] = True


class UserItem(APIModel):
    """用户信息模型"""
    id: str  # 用户ID (userId)
    username: str = Field(alias="userName")  # 用户名 (userName)，与name字段相同，为了兼容性同时返回
    avatar_url: Optional[AnyHttpUrl] = Field(default=None, alias="avatarUrl")
    name: str  # 用户名称 (userName)，与username字段相同
    bio: Optional[str] = None
    likes_count: int = Field(default=0, alias="likesCount")
    following_count: int = Field(default=0, alias="followingCount")
    followers_count: int = Field(default=0, alias="followersCount")


T = TypeVar("T")


class APIResponse(APIModel, Generic[T]):
    code: int = 0
    message: str = "OK"
    data: T


def success_response(data, timestamp: int | None = None):
    """创建成功响应，符合客户端ApiResponse格式"""
    response = {"code": 0, "message": "OK", "data": data}
    if timestamp is not None:
        response["timestamp"] = timestamp
    return response



