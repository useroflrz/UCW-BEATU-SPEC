"""任务相关 Schema"""

from pydantic import BaseModel, Field
from typing import List, Dict, Any, Optional
from agent_mcp.models.mcp_schema import MCPDescriptor


class SubTask(BaseModel):
    """子任务定义"""
    
    id: str = Field(..., description="任务ID，例如：'task_001'")
    goal: str = Field(..., description="任务目标")
    required_tool_type: str = Field(..., description="所需工具类型描述")
    extracted_params: Dict[str, Any] = Field(
        default_factory=dict, description="从用户输入中提取的参数"
    )


class TaskDecompositionResult(BaseModel):
    """任务分解结果"""
    
    analysis: str = Field(..., description="任务分析说明")
    sub_tasks: List[SubTask] = Field(..., description="子任务列表")
    
    class Config:
        json_schema_extra = {
            "example": {
                "analysis": "用户请求包含两个独立的子任务：天气查询和股票价格查询",
                "sub_tasks": [
                    {
                        "id": "task_001",
                        "goal": "查询北京明天的天气",
                        "required_tool_type": "weather forecast, location-based service",
                        "extracted_params": {
                            "city": "北京",
                            "date": "明天"
                        }
                    }
                ]
            }
        }


class DiscoveryResult(BaseModel):
    """工具发现结果"""
    
    task_id: str = Field(..., description="关联的任务ID")
    mcp_path: str = Field(..., description="找到的MCP路径")
    status: str = Field(..., description="发现状态，例如 'found', 'not_found'")
    mcp_descriptor: Optional[MCPDescriptor] = Field(
        default=None, description="MCP描述信息"
    )
