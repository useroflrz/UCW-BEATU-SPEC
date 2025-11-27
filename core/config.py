from functools import lru_cache

from pydantic import BaseSettings, Field


class Settings(BaseSettings):
    project_name: str = "BeatU Backend"
    version: str = "0.1.0"
    debug: bool = Field(default=False)
    database_url: str = Field(
        default="mysql+pymysql://root:2218502641@localhost:3306/beatu_content"
    )
    redis_url: str = Field(default="redis://localhost:6379/0")
    api_key: str = Field(default="dev-key")

    class Config:
        env_file = ".env"
        case_sensitive = False


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings()


settings = get_settings()


