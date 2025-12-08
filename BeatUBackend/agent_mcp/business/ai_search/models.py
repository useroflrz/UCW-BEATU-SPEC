"""AI 搜索数据模型"""

from typing import List, Optional
from pydantic import BaseModel, Field


class AISearchRequest(BaseModel):
    """AI 搜索请求"""
    user_query: str = Field(..., description="用户查询文本", min_length=1, max_length=500)


class AISearchResponse(BaseModel):
    """AI 搜索响应"""
    ai_answer: str = Field(..., description="AI 生成的文本回�?")
    keywords: List[str] = Field(default_factory=list, description="提取的关键词列表")
    video_ids: List[int] = Field(default_factory=list, description="远程数据库的视频 ID 列表（Long 类型）")
    local_video_ids: List[int] = Field(default_factory=list, description="本地数据库的视频 ID 列表（Long 类型）")


class StreamChunk(BaseModel):
    """流式输出数据�?"""
    chunk_type: str = Field(..., description="数据块类型：answer/keywords/video_ids")
    content: str = Field(..., description="数据块内�?")
    is_final: bool = Field(default=False, description="是否为最终数据块")

