from functools import lru_cache
from pathlib import Path
import os

from dotenv import load_dotenv
from pydantic import Field

# 兼容 Pydantic v1 和 v2
try:
    from pydantic_settings import BaseSettings
except ImportError:
    # Pydantic v1
    from pydantic import BaseSettings

# 加载 .env 文件（如果存在）
# ✅ 修复：在 BeatUBackend 目录下查找 .env 文件
env_file = Path(__file__).parent.parent / ".env"
env_file_path = str(env_file) if env_file.exists() else None
if env_file_path:
    load_dotenv(dotenv_path=env_file_path, override=True)


def build_database_url() -> str:
    """
    构建数据库连接URL
    优先级：
    1. DATABASE_URL 环境变量（优先从 .env 文件读取）
    2. 默认值（作为后备）
    
    注意：优先使用 .env 文件中的 DATABASE_URL 配置
    """
    # ✅ 修改：直接使用 DATABASE_URL 环境变量（优先从 .env 文件读取）
    database_url = os.getenv("DATABASE_URL")
    if database_url:
        return database_url
    
    # 如果没有设置 DATABASE_URL，使用默认值作为后备
    return "mysql+pymysql://jeecg:haomo123@192.168.1.206:3306/jeecg-boot3"


class Settings(BaseSettings):
    """
    应用配置类
    配置优先级（从高到低）：
    1. 系统环境变量
    2. .env 文件中的值（通过python-dotenv加载）
    3. 默认值
    
    使用说明：
    - 数据库连接：在 .env 文件中设置 DATABASE_URL 环境变量
    - 格式：DATABASE_URL=mysql+pymysql://用户名:密码@主机:端口/数据库名
    - 示例：DATABASE_URL=mysql+pymysql://jeecg:haomo123@192.168.1.206:3306/jeecg-boot3
    - 修改配置后重启服务生效
    """
    project_name: str = Field(default="BeatU Backend", description="项目名称")
    version: str = Field(default="0.1.0", description="版本号")
    debug: bool = Field(default=False, description="是否开启调试模式")
    database_url: str = Field(
        default_factory=build_database_url,
        description="数据库连接URL，格式：mysql+pymysql://用户名:密码@主机:端口/数据库名。优先从 .env 文件中的 DATABASE_URL 读取"
    )
    redis_url: str = Field(
        default="redis://localhost:6379/0",
        description="Redis连接URL，格式：redis://主机:端口/数据库编号"
    )
    api_key: str = Field(default="dev-key", description="API密钥，用于内部服务调用")
    api_prefix: str = Field(default="/api", description="API路由前缀")
    
    # 分页配置
    default_page_size: int = Field(default=10, ge=1, le=100, description="默认每页数量")
    max_page_size: int = Field(default=50, ge=1, le=200, description="最大每页数量")
    default_comment_page_size: int = Field(default=20, ge=1, le=100, description="评论默认每页数量")
    max_comment_page_size: int = Field(default=100, ge=1, le=200, description="评论最大每页数量")
    
    # 默认用户配置
    default_user_id: str = Field(default="BEATU", description="默认用户ID")
    default_user_name: str = Field(default="BEATU", description="默认用户名")
    
    # MCP 配置（AgentMCP 相关）
    mcp_api_key: str = Field(
        default="",
        description="MCP LLM API Key（用于 AgentMCP，对应环境变量 API_KEY）"
    )
    mcp_base_url: str = Field(
        default="https://dashscope.aliyuncs.com/compatible-mode/v1",
        description="MCP LLM Base URL（用于 AgentMCP，对应环境变量 BASE_URL）"
    )
    mcp_model: str = Field(
        default="qwen-flash",
        description="MCP LLM Model（用于 AgentMCP，对应环境变量 MODEL）"
    )
    mcp_registry_path: str = Field(
        default="",
        description="MCP 注册表路径（默认为 BeatUBackend/mcp_registry）"
    )

    # 兼容 Pydantic v1 和 v2
    class Config:
        env_file = env_file_path  # ✅ 修复：显式指定 .env 文件路径
        env_file_encoding = "utf-8"
        case_sensitive = False
        # Pydantic v2 兼容
        extra = "ignore"


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings()


settings = get_settings()
