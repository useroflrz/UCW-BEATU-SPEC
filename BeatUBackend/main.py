from fastapi import FastAPI

from core.config import settings
from routes.ai import router as ai_router
from routes.metrics import router as metrics_router
from routes.users import router as user_router
from routes.videos import router as video_router


def create_app() -> FastAPI:
    """Application factory to keep tests lightweight."""
    app = FastAPI(title=settings.project_name, version=settings.version)

    api_prefix = "/api"
    app.include_router(video_router, prefix=api_prefix)
    app.include_router(user_router, prefix=api_prefix)
    app.include_router(ai_router, prefix=api_prefix)
    app.include_router(metrics_router, prefix=api_prefix)

    return app


app = create_app()


