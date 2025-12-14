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
        # ✅ 修复：分离 LLM 配置和 MCP 配置
        # LLM 配置：用于大模型推理（ChatOpenAI）
        # MCP 配置：用于 MCP 工具服务（MultiServerMCPClient）
        import logging
        logger = logging.getLogger(__name__)
        
        # 确保从 .env 文件加载配置
        from dotenv import load_dotenv
        from pathlib import Path
        env_file = Path(__file__).parent.parent / ".env"
        if env_file.exists():
            load_dotenv(dotenv_path=str(env_file), override=True)
        
        # ========== 1. 配置 LLM（大模型服务）==========
        # ✅ 修复：同时从 .env 文件直接读取，确保能读取到配置
        llm_api_key_from_file = None
        mcp_api_key_from_file = None
        if env_file.exists():
            try:
                with open(env_file, 'r', encoding='utf-8') as f:
                    for line in f:
                        line = line.strip()
                        # 尝试读取 LLM_API_KEY, DASHSCOPE_API_KEY, 或 MCP_API_KEY（向后兼容）
                        if line.startswith('LLM_API_KEY=') and not line.startswith('#'):
                            llm_api_key_from_file = line.split('=', 1)[1].strip().strip('"').strip("'")
                            logger.info(f"从文件直接读取 LLM_API_KEY: {llm_api_key_from_file[:10]}... (length: {len(llm_api_key_from_file)})")
                        elif line.startswith('DASHSCOPE_API_KEY=') and not line.startswith('#') and not llm_api_key_from_file:
                            llm_api_key_from_file = line.split('=', 1)[1].strip().strip('"').strip("'")
                            logger.info(f"从文件直接读取 DASHSCOPE_API_KEY: {llm_api_key_from_file[:10]}... (length: {len(llm_api_key_from_file)})")
                        elif line.startswith('MCP_API_KEY=') and not line.startswith('#'):
                            # 向后兼容：如果 MCP_API_KEY 存在但 LLM_API_KEY 不存在，使用 MCP_API_KEY
                            mcp_api_key_from_file = line.split('=', 1)[1].strip().strip('"').strip("'")
                            if not llm_api_key_from_file:
                                llm_api_key_from_file = mcp_api_key_from_file
                                logger.info(f"从文件直接读取 MCP_API_KEY（向后兼容，用作 LLM API Key）: {llm_api_key_from_file[:10]}... (length: {len(llm_api_key_from_file)})")
            except Exception as e:
                logger.warning(f"直接读取 .env 文件失败: {e}")
        
        # 优先级：环境变量 > 文件直接读取 > settings.llm_api_key > settings.mcp_api_key（向后兼容）
        llm_api_key = (
            os.getenv("LLM_API_KEY", "").strip() or
            os.getenv("DASHSCOPE_API_KEY", "").strip() or
            (llm_api_key_from_file.strip() if llm_api_key_from_file else "") or
            (settings.llm_api_key.strip() if settings.llm_api_key else "") or
            (settings.mcp_api_key.strip() if settings.mcp_api_key else "") or
            (mcp_api_key_from_file.strip() if mcp_api_key_from_file else "")  # 向后兼容
        )
        
        # ✅ 添加调试日志
        logger.info(f"配置读取检查 - LLM_API_KEY from env: {os.getenv('LLM_API_KEY', 'NOT_SET')[:10] if os.getenv('LLM_API_KEY') else 'EMPTY'}...")
        logger.info(f"配置读取检查 - DASHSCOPE_API_KEY from env: {os.getenv('DASHSCOPE_API_KEY', 'NOT_SET')[:10] if os.getenv('DASHSCOPE_API_KEY') else 'EMPTY'}...")
        logger.info(f"配置读取检查 - LLM_API_KEY from file: {llm_api_key_from_file[:10] if llm_api_key_from_file else 'EMPTY'}...")
        logger.info(f"配置读取检查 - settings.llm_api_key: {settings.llm_api_key[:10] if settings.llm_api_key else 'EMPTY'}...")
        logger.info(f"配置读取检查 - settings.mcp_api_key: {settings.mcp_api_key[:10] if settings.mcp_api_key else 'EMPTY'}...")
        logger.info(f"最终 LLM_API_KEY: {llm_api_key[:10] if llm_api_key else 'EMPTY'}... (length: {len(llm_api_key) if llm_api_key else 0})")
        llm_base_url = (
            os.getenv("LLM_BASE_URL", "").strip() or
            (settings.llm_base_url if settings.llm_base_url else "https://dashscope.aliyuncs.com/compatible-mode/v1")
        )
        llm_model = (
            os.getenv("LLM_MODEL", "").strip() or
            (settings.llm_model if settings.llm_model else "qwen-flash")
        )
        
        # 验证 LLM 配置
        if not llm_api_key or llm_api_key in ("", "your_api_key_here", "YOUR_API_KEY_HERE"):
            error_msg = (
                "LLM API Key 未配置！大模型推理功能将无法使用。\n"
                "请在 .env 文件中设置 LLM_API_KEY 或 DASHSCOPE_API_KEY 环境变量。\n"
                "获取 API Key：https://bailian.console.aliyun.com/?tab=model#/api-key"
            )
            logger.error(error_msg)
            raise ValueError(error_msg)
        
        # 设置 LLM 环境变量（用于 ChatOpenAI）
        os.environ["OPENAI_API_KEY"] = llm_api_key
        os.environ["DASHSCOPE_API_KEY"] = llm_api_key
        os.environ["BASE_URL"] = llm_base_url
        os.environ["MODEL"] = llm_model
        logger.info(f"✅ LLM 配置已设置: BASE_URL={llm_base_url}, MODEL={llm_model}, API_KEY length={len(llm_api_key)}")
        
        # ========== 2. 配置 MCP（工具服务）==========
        # MCP API Key 用于 IQS MCP Server 的 X-API-Key 认证
        mcp_api_key = (
            os.getenv("MCP_API_KEY", "").strip() or
            (settings.mcp_api_key.strip() if settings.mcp_api_key else "")
        )
        
        # 如果 MCP_API_KEY 未配置，记录警告但不阻止服务启动（某些 MCP Server 可能不需要认证）
        if not mcp_api_key or mcp_api_key in ("", "your_api_key_here", "YOUR_API_KEY_HERE"):
            logger.warning(
                "MCP_API_KEY 未配置！某些需要认证的 MCP Server 可能无法使用。\n"
                "如需使用 IQS MCP Server，请在 .env 文件中设置 MCP_API_KEY 环境变量。"
            )
        else:
            # 设置 MCP 环境变量（用于 MultiServerMCPClient 的 X-API-Key 认证）
            os.environ["MCP_API_KEY"] = mcp_api_key
            logger.info(f"✅ MCP API Key 已设置 (length: {len(mcp_api_key)})")
        
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
    """获取 MCP 编排器服务实例（单例模式，延迟初始化）"""
    global _mcp_service
    if _mcp_service is None:
        try:
            # ✅ 延迟初始化：只在第一次调用时创建，避免启动时阻塞
            _mcp_service = MCPOrchestratorService()
        except Exception as e:
            # ✅ 改进错误处理：记录详细错误信息，但不阻止服务启动
            import logging
            logger = logging.getLogger(__name__)
            logger.error(f"MCP 服务初始化失败: {e}", exc_info=True)
            # 重新抛出异常，让调用者处理
            raise
    return _mcp_service
