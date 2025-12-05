"""
请求日志中间件
用于记录所有API请求，方便调试和监控
"""
import logging
import time
from typing import Callable

from fastapi import Request, Response
from starlette.middleware.base import BaseHTTPMiddleware

logger = logging.getLogger(__name__)


class RequestLoggingMiddleware(BaseHTTPMiddleware):
    """记录所有HTTP请求的中间件"""

    async def dispatch(self, request: Request, call_next: Callable) -> Response:
        # 记录请求开始时间
        start_time = time.time()
        
        # 获取客户端IP地址
        client_ip = request.client.host if request.client else "unknown"
        
        # 记录请求信息
        logger.info(
            f"[请求] {request.method} {request.url.path} | "
            f"客户端IP: {client_ip} | "
            f"查询参数: {dict(request.query_params)}"
        )
        
        # 处理请求
        try:
            response = await call_next(request)
            
            # 计算处理时间
            process_time = time.time() - start_time
            
            # 记录响应信息
            logger.info(
                f"[响应] {request.method} {request.url.path} | "
                f"状态码: {response.status_code} | "
                f"处理时间: {process_time:.3f}秒 | "
                f"客户端IP: {client_ip}"
            )
            
            return response
            
        except Exception as e:
            # 记录异常
            process_time = time.time() - start_time
            logger.error(
                f"[错误] {request.method} {request.url.path} | "
                f"异常: {str(e)} | "
                f"处理时间: {process_time:.3f}秒 | "
                f"客户端IP: {client_ip}",
                exc_info=True
            )
            raise

