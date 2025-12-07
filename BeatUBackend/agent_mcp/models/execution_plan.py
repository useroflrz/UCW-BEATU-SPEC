"""执行计划 Schema"""

from pydantic import BaseModel, Field
from typing import List, Dict, Any, Optional


class MCPExecutionItem(BaseModel):
    """MCP 执行项"""
    
    server_name: str = Field(..., description="MCP 服务器名称")
    tool_name: Optional[str] = Field(default=None, description="具体工具名称，可选")
    arguments: Dict[str, Any] = Field(default_factory=dict, description="执行参数")


class TaskExecutionItem(BaseModel):
    """任务执行项"""
    
    task_id: str = Field(..., description="任务ID")
    mcp_to_execute: Optional[MCPExecutionItem] = Field(default=None, description="要执行的MCP")


class ExecutionPlan(BaseModel):
    """执行计划"""
    
    plan: List[TaskExecutionItem] = Field(..., description="执行计划列表")
    
    class Config:
        json_schema_extra = {
            "example": {
                "plan": [
                    {
                        "task_id": "task_001",
                        "mcp_to_execute": {
                            "server_name": "weather-mcp",
                            "tool_name": "get_weather_forecast",
                            "arguments": {
                                "city": "北京",
                                "days": 1
                            }
                        }
                    }
                ]
            }
        }
