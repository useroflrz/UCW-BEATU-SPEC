"""MCP 执行引擎"""

import os
from typing import Dict, Any, Optional
import httpx
from agent_mcp.models.mcp_schema import MCPDescriptor


class MCPExecutionEngine:
    """MCP 执行引擎
    
    负责执行 MCP 服务调用，处�?HTTP 请求和响应�?
    """
    
    def __init__(
        self,
        base_url: Optional[str] = None,
        timeout: int = 30,
    ):
        """初始化执行引�?
        
        Args:
            base_url: API 基础 URL，如果为 None 则从环境变量读取
            timeout: 请求超时时间（秒�?
        """
        self.base_url = base_url or os.getenv("MCP_BASE_URL", "https://api.example.com")
        self.timeout = timeout or int(os.getenv("MCP_TIMEOUT", "30"))
        self.client = httpx.AsyncClient(timeout=self.timeout)
    
    async def execute_mcp(
        self,
        mcp: MCPDescriptor,
        arguments: Dict[str, Any]
    ) -> Dict[str, Any]:
        """执行 MCP 服务
        
        Args:
            mcp: MCP 服务描述
            arguments: 执行参数
        
        Returns:
            执行结果字典
            
        Raises:
            ValueError: 如果参数验证失败
            httpx.HTTPError: 如果 HTTP 请求失败
        """
        # 验证参数
        self._validate_arguments(mcp, arguments)
        
        # 构建请求 URL
        url = f"{self.base_url}{mcp.api_endpoint}"
        
        # 根据 HTTP 方法执行请求
        try:
            if mcp.method.upper() == "GET":
                response = await self.client.get(url, params=arguments)
            elif mcp.method.upper() == "POST":
                response = await self.client.post(url, json=arguments)
            elif mcp.method.upper() == "PUT":
                response = await self.client.put(url, json=arguments)
            elif mcp.method.upper() == "DELETE":
                response = await self.client.delete(url, params=arguments)
            else:
                raise ValueError(f"不支持的 HTTP 方法: {mcp.method}")
            
            response.raise_for_status()
            return response.json()
            
        except httpx.HTTPError as e:
            raise Exception(f"HTTP 请求失败: {e}")
        except Exception as e:
            raise Exception(f"执行 MCP 失败: {e}")
    
    def _validate_arguments(
        self,
        mcp: MCPDescriptor,
        arguments: Dict[str, Any]
    ):
        """验证参数
        
        Args:
            mcp: MCP 服务描述
            arguments: 执行参数
            
        Raises:
            ValueError: 如果参数验证失败
        """
        # 检查必需参数
        for param in mcp.parameters:
            if param.required and param.name not in arguments:
                raise ValueError(f"缺少必需参数: {param.name}")
        
        # 检查参数类型（简单验证）
        # 这里可以添加更复杂的类型验证逻辑
    
    async def close(self):
        """关闭 HTTP 客户�?"""
        await self.client.aclose()
    
    async def __aenter__(self):
        """异步上下文管理器入口"""
        return self
    
    async def __aexit__(self, exc_type, exc_val, exc_tb):
        """异步上下文管理器出口"""
        await self.close()

