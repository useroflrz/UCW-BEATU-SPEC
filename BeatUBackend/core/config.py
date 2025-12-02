from functools import lru_cache
from pathlib import Path

from dotenv import load_dotenv
from pydantic import BaseSettings, Field

# 加载 .env 文件（如果存在）
# 优先查找项目根目录下的 .env 文件
env_file = Path(__file__).parent.parent / ".env"
if env_file.exists():
    load_dotenv(dotenv_path=env_file)


class Settings(BaseSettings):
    """
    应用配置类
    配置优先级（从高到低）：
    1. 环境变量
    2. .env 文件中的值（通过python-dotenv加载）
    3. 默认值
    
    使用说明：
    - 复制 .env.example 为 .env 并修改配置
    - 或通过环境变量设置（推荐生产环境）
    - 修改配置后重启服务生效
    """
    project_name: str = Field(default="BeatU Backend", description="项目名称")
    version: str = Field(default="0.1.0", description="版本号")
    debug: bool = Field(default=False, description="是否开启调试模式")
    database_url: str = Field(
        default="mysql+pymysql://jeecg:haomo123@192.168.1.206:3306/jeecg-boot3",
        description="数据库连接URL，格式：mysql+pymysql://用户名:密码@主机:端口/数据库名"
    )
    redis_url: str = Field(
        default="redis://localhost:6379/0",
        description="Redis连接URL，格式：redis://主机:端口/数据库编号"
    )
    api_key: str = Field(default="dev-key", description="API密钥，用于内部服务调用")

    class Config:
        env_file_encoding = "utf-8"
        case_sensitive = False


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings()


settings = get_settings()


