"""MCP 编排器服务

封装 AgentMCP 的 Orchestrator 功能，提供 MCP 服务调用能力。
"""

from __future__ import annotations

import os
from pathlib import Path
from typing import Optional

# 从本地 mcp 模块导入（已迁移到 BeatUBackend/mcp/）
from agent_mcp.core.orchestrator import AgentOrchestrator

# 导入配置以设置环境变量
from core.config import settings


class MCPOrchestratorService:
    """MCP 编排器服务
    
    封装 AgentMCP 的 AgentOrchestrator，提供异步处理用户请求的能力。
    """
    
    def __init__(self, mcp_filesystem_root: Optional[str] = None):
        """初始化 MCP 编排器服务
        
        Args:
            mcp_filesystem_root: MCP 文件系统根路径，默认为 BeatUBackend/mcp_registry
        """
        # 设置 AgentMCP 所需的环境变量（从 BeatUBackend 配置读取）
        if settings.mcp_api_key:
            os.environ["API_KEY"] = settings.mcp_api_key
        else:
            # 如果 API key 未配置，记录警告
            import logging
            logger = logging.getLogger(__name__)
            logger.warning(
                "MCP_API_KEY 未配置！AI 搜索功能将无法使用。"
                "请在 .env 文件中设置 MCP_API_KEY 环境变量，或设置系统环境变量。"
            )
        if settings.mcp_base_url:
            os.environ["BASE_URL"] = settings.mcp_base_url
        if settings.mcp_model:
            os.environ["MODEL"] = settings.mcp_model
        
        if mcp_filesystem_root is None:
            if settings.mcp_registry_path:
                mcp_filesystem_root = settings.mcp_registry_path
            else:
                # 默认使用 BeatUBackend 下的 mcp_registry 目录
                backend_root = Path(__file__).parent.parent
                mcp_filesystem_root = str(backend_root / "mcp_registry")
        
        self.orchestrator = AgentOrchestrator(
            llm=None,  # 使用默认 LLM 配置（会从环境变量读取）
            mcp_filesystem_root=mcp_filesystem_root
        )
    
    async def process_request(self, user_input: str) -> str:
        """处理用户请求
        
        Args:
            user_input: 用户输入的自然语言请求
        
        Returns:
            str: 处理结果
        """
        return await self.orchestrator.process_user_request_async(user_input)
    
    async def close(self):
        """关闭资源"""
        await self.orchestrator.close()


# 全局服务实例（单例模式）
_mcp_service: Optional[MCPOrchestratorService] = None


def get_mcp_service() -> MCPOrchestratorService:
    """获取 MCP 编排器服务实例（单例模式）"""
    global _mcp_service
    if _mcp_service is None:
        _mcp_service = MCPOrchestratorService()
    return _mcp_service
