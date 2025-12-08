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
# 优先查找项目根目录下的 .env 文件
env_file = Path(__file__).parent.parent / ".env"
if env_file.exists():
    load_dotenv(dotenv_path=env_file)


def build_database_url() -> str:
    """
    构建数据库连接URL
    优先级：
    1. DATABASE_URL 环境变量（如果已设置）
    2. 从 DB_HOST, DB_PORT, DB_USER, DB_PASSWORD, DB_NAME 构建
    3. 默认值
    """
    # 如果已设置 DATABASE_URL，直接使用
    if os.getenv("DATABASE_URL"):
        return os.getenv("DATABASE_URL")
    
    # 从分离的变量构建
    db_host = os.getenv("DB_HOST", "localhost")
    db_port = os.getenv("DB_PORT", "3306")
    db_user = os.getenv("DB_USER", "beatu")
    db_password = os.getenv("DB_PASSWORD", "RXSSbTkGZWFkyThj")
    db_name = os.getenv("DB_NAME", "beatu")
    
    return f"mysql+pymysql://{db_user}:{db_password}@{db_host}:{db_port}/{db_name}"


class Settings(BaseSettings):
    """
    应用配置类
    配置优先级（从高到低）：
    1. 环境变量
    2. .env 文件中的值（通过python-dotenv加载）
    3. 默认值
    
    使用说明：
    - 可以直接设置 DATABASE_URL 环境变量
    - 或者设置 DB_HOST, DB_PORT, DB_USER, DB_PASSWORD, DB_NAME 变量
    - 修改配置后重启服务生效
    """
    project_name: str = Field(default="BeatU Backend", description="项目名称")
    version: str = Field(default="0.1.0", description="版本号")
    debug: bool = Field(default=False, description="是否开启调试模式")
    database_url: str = Field(
        default_factory=build_database_url,
        description="数据库连接URL，格式：mysql+pymysql://用户名:密码@主机:端口/数据库名"
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
        env_file_encoding = "utf-8"
        case_sensitive = False
        # Pydantic v2 兼容
        extra = "ignore"


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings()


settings = get_settings()
