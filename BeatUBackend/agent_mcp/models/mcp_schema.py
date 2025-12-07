"""MCP 服务描述 Schema"""

from pydantic import BaseModel, Field, model_validator
from typing import List, Dict, Any, Optional, Literal


class Parameter(BaseModel):
    """MCP 参数定义"""
    
    name: str = Field(..., description="参数名称")
    type: str = Field(..., description="参数类型，例如：'string', 'number', 'array[string]'")
    description: str = Field(..., description="参数描述")
    required: bool = Field(default=True, description="是否必需")
    default: Optional[Any] = Field(default=None, description="默认值")


class MCPDescriptor(BaseModel):
    """MCP 服务描述"""
    
    name: str = Field(..., description="MCP 服务名称")
    description: str = Field(..., description="MCP 服务功能描述")
    category: str = Field(..., description="服务分类，例如：'finance', 'weather'")
    api_endpoint: str = Field(..., description="API 端点路径")
    method: str = Field(default="GET", description="HTTP 方法")
    parameters: List[Parameter] = Field(default_factory=list, description="参数列表")
    response_format: Dict[str, Any] = Field(
        default_factory=dict, description="响应格式定义"
    )
    # MultiServerMCPClient 服务器配置
    server_config: Optional[Dict[str, Any]] = Field(
        default=None,
        description="MultiServerMCPClient 服务器配置，包含 transport, command, args 或 url"
    )
    
    class Config:
        json_schema_extra = {
            "example": {
                "name": "get_stock_price",
                "description": "获取一个或多个股票代码的实时市场价格和基本信息",
                "category": "finance",
                "api_endpoint": "/api/v1/finance/stock/price",
                "method": "GET",
                "parameters": [
                    {
                        "name": "symbols",
                        "type": "array[string]",
                        "description": "股票代码数组，例如：['AAPL', 'GOOG']",
                        "required": True
                    }
                ],
                "response_format": {
                    "type": "object",
                    "properties": {
                        "symbol": {"type": "string"},
                        "price": {"type": "number"},
                        "change_percent": {"type": "number"}
                    }
                }
            }
        }


class FileStructure(BaseModel):
    """文件结构响应"""
    
    directories: List[str] = Field(default_factory=list, description="子目录列表")
    files: List[str] = Field(default_factory=list, description="文件列表")
    
    def to_dict(self) -> Dict[str, List[str]]:
        """转换为字典格式"""
        return {
            "directories": self.directories,
            "files": self.files
        }


class MCPServerTransportConfig(BaseModel):
    """MCP 服务器传输配置"""

    type: Optional[Literal[
        "streamable_http",
        "stdio",
        "websocket",
        "generic_http",
        "grpc"
    ]] = Field(default=None, description="MCP 传输方式")
    url: Optional[str] = Field(default=None, description="远程 MCP 服务地址")
    command: Optional[str] = Field(default=None, description="本地进程命令")
    args: Optional[List[str]] = Field(default=None, description="命令参数列表")
    env: Optional[Dict[str, str]] = Field(default=None, description="进程环境变量")
    cwd: Optional[str] = Field(default=None, description="进程工作目录")
    timeout: Optional[int] = Field(default=None, description="进程超时时间（秒）")
    
    @model_validator(mode='after')
    def infer_type(self) -> 'MCPServerTransportConfig':
        """根据存在的字段自动推断传输类型"""
        if self.type is None:
            if self.command is not None:
                self.type = "stdio"
            elif self.url is not None:
                self.type = "streamable_http"
            else:
                # 如果没有足够的信息推断类型，抛出错误
                raise ValueError("无法推断传输类型：必须提供 'type' 或 'command' 或 'url' 字段之一")
        return self

    def to_multiserver_config(self) -> Dict[str, Any]:
        """转换为 MultiServerMCPClient 接受的配置结构"""
        data = self.model_dump()
        transport = data.pop("type", None)
        if transport:
            data["transport"] = transport
        # 移除值为 None 的字段，避免向客户端传递无效参数
        return {key: value for key, value in data.items() if value is not None}


class MCPServerConfigFile(BaseModel):
    """MCP 多服务器配置文件"""

    mcpServers: Dict[str, MCPServerTransportConfig] = Field(
        default_factory=dict,
        description="服务器名称到传输配置的映射"
    )
