"""MCP 虚拟文件系统"""

from typing import Dict, Optional, Any, Union
from pathlib import Path
import json
from agent_mcp.models.mcp_schema import (
    MCPDescriptor,
    FileStructure,
    MCPServerConfigFile,
)


class MCPFilesystem:
    """MCP 虚拟文件系统
    
    提供类似文件系统的接口来访问 MCP 服务描述符。
    支持目录浏览和文件读取。
    """
    
    def __init__(self, root_path: str = "mcp_registry"):
        """初始化 MCP 文件系统
        
        Args:
            root_path: MCP 注册表的根路径
        """
        self.root_path = Path(root_path)
        self._cache: Dict[str, Any] = {}
        self._ensure_root_exists()
    
    def _ensure_root_exists(self):
        """确保根目录存在"""
        self.root_path.mkdir(parents=True, exist_ok=True)
    
    def read_file_structure(self, path: str) -> FileStructure:
        """读取指定路径下的目录结构
        
        Args:
            path: 目录路径，例如：'/services/' 或 '/services/weather/'
                支持以 '/' 开头（绝对路径）或相对路径
        
        Returns:
            FileStructure: 包含子目录和文件列表的结构
            
        Raises:
            ValueError: 如果路径无效或不存在
        """
        # 规范化路径
        normalized_path = self._normalize_path(path)
        full_path = self.root_path / normalized_path.lstrip("/")
        
        if not full_path.exists():
            raise ValueError(f"路径不存在: {path}")
        
        if not full_path.is_dir():
            raise ValueError(f"路径不是目录: {path}")
        
        # 读取目录内容
        directories = []
        files = []
        
        try:
            for item in full_path.iterdir():
                if item.is_dir():
                    directories.append(item.name)
                elif item.is_file() and item.suffix == ".json":
                    files.append(item.name)
        except Exception as e:
            raise ValueError(f"读取目录失败: {e}")
        
        return FileStructure(
            directories=sorted(directories),
            files=sorted(files)
        )
    
    def filter_directories(self, path: str, filter_keyword: str) -> FileStructure:
        """根据关键词过滤目录结构
        
        Args:
            path: 目录路径
            filter_keyword: 过滤关键词
        
        Returns:
            FileStructure: 包含过滤后的子目录和文件列表的结构
        """
        # 获取完整的目录结构
        full_structure = self.read_file_structure(path)
        
        # 根据关键词过滤目录
        filtered_directories = [
            directory for directory in full_structure.directories
            if filter_keyword.lower() in directory.lower()
        ]
        
        # 返回过滤后的结构
        return FileStructure(
            directories=filtered_directories,
            files=full_structure.files
        )
    
    def read_file_content(
        self,
        path: str
    ) -> Union[MCPDescriptor, MCPServerConfigFile, Dict[str, Any], str]:
        """读取 MCP 文件内容
        
        Args:
            path: MCP 文件路径，例如：'/services/weather/get_weather_forecast.json'
        
        Returns:
            MCPDescriptor | MCPServerConfigFile | Dict[str, Any] | str: 文件内容
            
        Raises:
            ValueError: 如果文件不存在或格式无效
        """
        # 检查缓存
        if path in self._cache:
            return self._cache[path]
        
        # 规范化路径
        normalized_path = self._normalize_path(path)
        full_path = self.root_path / normalized_path.lstrip("/")
        
        if not full_path.exists():
            raise ValueError(f"文件不存在: {path}")
        
        if not full_path.is_file():
            raise ValueError(f"路径不是文件: {path}")
        
        try:
            suffix = full_path.suffix.lower()
            raw_text = full_path.read_text(encoding="utf-8")
            
            # 针对 JSON / 文本内容尝试解析
            if suffix in {".json", ".txt"}:
                parsed = self._try_parse_json(raw_text)
                if parsed is not None:
                    content = parsed
                    
                    if isinstance(content, dict):
                        # 尝试解析为 MCPDescriptor
                        if self._looks_like_descriptor(content):
                            descriptor = MCPDescriptor(**content)
                            self._cache[path] = descriptor
                            return descriptor
                        
                        # 尝试解析为 MCPServerConfigFile
                        if "mcpServers" in content:
                            server_config = MCPServerConfigFile(**content)
                            self._cache[path] = server_config
                            return server_config
                    
                    # 默认返回 dict/list
                    self._cache[path] = content
                    return content
            
            # 默认返回原始文本
            self._cache[path] = raw_text
            return raw_text
        except json.JSONDecodeError as e:
            raise ValueError(f"JSON 解析失败: {e}")
        except Exception as e:
            raise ValueError(f"读取文件失败: {e}")

    def get_server_config(self, path: str) -> MCPServerConfigFile:
        """读取并返回 MCP 服务器配置文件
        
        Args:
            path: MCP 服务器配置文件路径
        
        Returns:
            MCPServerConfigFile: 服务器配置
        """
        content = self.read_file_content(path)
        if isinstance(content, MCPServerConfigFile):
            return content
        if isinstance(content, dict) and "mcpServers" in content:
            server_config = MCPServerConfigFile(**content)
            self._cache[path] = server_config
            return server_config
        raise ValueError(f"文件不是有效的 MCP 服务器配置: {path}")
    
    def _normalize_path(self, path: str) -> str:
        """规范化路径
        
        Args:
            path: 原始路径
        
        Returns:
            规范化后的路径
        """
        # 移除开头的斜杠（Windows 兼容性）
        if path.startswith("/"):
            path = path[1:]
        # 规范化路径分隔符
        return path.replace("\\", "/")
    
    def clear_cache(self):
        """清空缓存"""
        self._cache.clear()

    @staticmethod
    def _try_parse_json(raw_text: str) -> Optional[Any]:
        """尝试将文本解析为 JSON"""
        stripped = raw_text.lstrip()
        if not stripped:
            return None
        if stripped[0] not in ("{", "["):
            return None
        try:
            return json.loads(raw_text)
        except json.JSONDecodeError:
            return None

    @staticmethod
    def _looks_like_descriptor(data: Dict[str, Any]) -> bool:
        """判断数据结构是否符合 MCPDescriptor"""
        required_keys = {"name", "description", "category", "api_endpoint"}
        return required_keys.issubset(data.keys())
