"""核心基础工具

这些工具供 LLM 可以直接调用的基础工具，用于探索 MCP 文件系统。"""

from langchain_core.tools import tool
from typing import Dict, Any, Union
import json
from agent_mcp.core.mcp_filesystem import MCPFilesystem
from agent_mcp.models.mcp_schema import MCPDescriptor, MCPServerConfigFile
from agent_mcp.utils.logger import setup_logger


# 全局 MCP 文件系统实例（在实际使用中，应该通过依赖注入）
_filesystem: MCPFilesystem = None
_logger = setup_logger(__name__)


def initialize_filesystem(root_path: str = "mcp_registry"):
    """初始化文件系统
    
    Args:
        root_path: MCP 注册表根路径
    """
    global _filesystem
    _filesystem = MCPFilesystem(root_path=root_path)


def get_filesystem() -> MCPFilesystem:
    """获取文件系统实例
    
    Returns:
        MCPFilesystem: 文件系统实例
        
    Raises:
        RuntimeError: 如果文件系统未初始化
    """
    if _filesystem is None:
        raise RuntimeError("文件系统未初始化，请先调用 initialize_filesystem()")
    return _filesystem


@tool
def read_file_structure(path: str) -> str:
    """读取指定路径下的目录结构。
    
    这个工具用于探索 MCP 文件系统的目录结构，类似于 ls 或 dir 命令。
    返回该目录下的子目录和文件列表。
    
    Args:
        path: 目录路径，例如 '/services/' 或 '/services/weather/'
    
    Returns:
        JSON 字符串，格式为：
        {
            "directories": ["finance", "weather", "maps"],
            "files": ["service1.json", "service2.json"]
        }
        
    Example:
        >>> read_file_structure("/services/")
        '{"directories": ["finance", "weather"], "files": []}'
    """
    _logger.debug(f"读取目录结构: {path}")
    try:
        filesystem = get_filesystem()
        structure = filesystem.read_file_structure(path)
        result = json.dumps(structure.to_dict(), ensure_ascii=False, indent=2)
        _logger.debug(f"目录结构读取结果: {result}")
        return result
    except Exception as e:
        error_result = json.dumps({
            "error": str(e),
            "directories": [],
            "files": []
        }, ensure_ascii=False)
        _logger.error(f"目录结构读取失败: {e}", exc_info=True)
        return error_result


@tool
def read_file_content(path: str) -> str:
    """读取 MCP 文件的详细描述。
    
    这个工具用于读取 MCP 服务描述文件的完整内容，包括服务名称、
    描述、参数、API 端点等信息。
    
    Args:
        path: MCP 文件路径，例如 '/services/weather/get_weather_forecast.json'
    
    Returns:
        JSON 字符串，包含 MCP 的完整描述，格式为：
        {
            "name": "get_weather_forecast",
            "description": "获取天气预报",
            "category": "weather",
            "api_endpoint": "/api/v1/weather/forecast",
            "method": "GET",
            "parameters": [...],
            "response_format": {...}
        }
        
    Example:
        >>> read_file_content("/services/weather/get_weather_forecast.json")
        '{"name": "get_weather_forecast", ...}'
    """
    _logger.debug(f"读取文件内容: {path}")
    try:
        filesystem = get_filesystem()
        content = filesystem.read_file_content(path)

        if isinstance(content, (MCPDescriptor, MCPServerConfigFile)):
            serialized: Union[Dict[str, Any], Any] = content.model_dump()
        elif isinstance(content, (dict, list)):
            serialized = content
        else:
            # 返回原始文本内容
            result = str(content)
            _logger.debug(f"文件内容读取结果: {result}")
            return result

        result = json.dumps(serialized, ensure_ascii=False, indent=2)
        _logger.debug(f"文件内容读取结果: {result}")
        return result
    except Exception as e:
        error_result = json.dumps({
            "error": str(e)
        }, ensure_ascii=False)
        _logger.error(f"文件内容读取失败: {e}", exc_info=True)
        return error_result


def get_core_tools():
    """获取所有核心工具列表
    
    Returns:
        List[Tool]: 核心工具列表
    """
    return [read_file_structure, read_file_content]
