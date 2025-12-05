import logging
from datetime import datetime

from fastapi import FastAPI
from fastapi.responses import JSONResponse

from core.config import settings
from core.middleware import RequestLoggingMiddleware
from routes.ai import router as ai_router
from routes.mcp import router as mcp_router
from routes.metrics import router as metrics_router
from routes.users import router as user_router
from routes.videos import router as video_router

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S'
)
logger = logging.getLogger(__name__)


def create_app() -> FastAPI:
    """Application factory to keep tests lightweight."""
    app = FastAPI(title=settings.project_name, version=settings.version)

    # 添加请求日志中间件（记录所有请求）
    app.add_middleware(RequestLoggingMiddleware)

    # 健康检查接口（用于快速验证服务是否正常运行）
    @app.get("/health")
    def health_check():
        """健康检查接口，用于验证服务是否正常运行"""
        return JSONResponse({
            "status": "ok",
            "timestamp": datetime.now().isoformat(),
            "service": settings.project_name,
            "version": settings.version
        })

    # 从配置文件读取API前缀
    api_prefix = settings.api_prefix
    app.include_router(video_router, prefix=api_prefix)
    app.include_router(user_router, prefix=api_prefix)
    app.include_router(ai_router, prefix=api_prefix)
    app.include_router(mcp_router, prefix=api_prefix)
    app.include_router(metrics_router, prefix=api_prefix)

    logger.info(f"BeatU Backend 服务启动成功 | 版本: {settings.version}")
    logger.info(f"API文档地址: http://0.0.0.0:9306/docs")
    logger.info(f"健康检查地址: http://0.0.0.0:9306/health")

    return app


app = create_app()


