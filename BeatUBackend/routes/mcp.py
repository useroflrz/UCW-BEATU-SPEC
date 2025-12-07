"""MCP 相关路由

提供 MCP 服务调用接口。
"""

from __future__ import annotations

import json
from fastapi import APIRouter, Depends, HTTPException
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field

from schemas.api import success_response
from services.mcp_orchestrator_service import get_mcp_service, MCPOrchestratorService


router = APIRouter(tags=["mcp"])


class MCPRequest(BaseModel):
    """MCP 请求模型"""
    user_input: str = Field(..., description="用户输入的自然语言请求")


class MCPResponse(BaseModel):
    """MCP 响应模型"""
    response: str = Field(..., description="处理结果")


@router.post("/mcp/process", response_model=dict)
async def process_mcp_request(
    payload: MCPRequest,
    service: MCPOrchestratorService = Depends(get_mcp_service),
):
    """
    处理 MCP 请求
    
    接收用户输入的自然语言请求，通过 MCP 编排器处理并返回结果。
    
    Args:
        payload: 包含 user_input 字段的请求体
        service: MCP 编排器服务实例
    
    Returns:
        包含处理结果的响应
    """
    try:
        response = await service.process_request(payload.user_input)
        return success_response({"response": response})
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"MCP 处理失败: {str(e)}"
        )


@router.post("/mcp/process/stream")
async def process_mcp_request_stream(
    payload: MCPRequest,
    service: MCPOrchestratorService = Depends(get_mcp_service),
):
    """
    流式处理 MCP 请求
    
    接收用户输入的自然语言请求，通过 MCP 编排器处理并返回流式响应。
    
    Args:
        payload: 包含 user_input 字段的请求体
        service: MCP 编排器服务实例
    
    Returns:
        StreamingResponse: SSE 格式的流式响应
    """
    async def generate():
        """生成流式响应"""
        try:
            # 注意：AgentMCP 的 orchestrator 目前不支持流式输出
            # 这里先返回完整结果，后续可以扩展为真正的流式输出
            response = await service.process_request(payload.user_input)
            
            # 将结果分块发送（模拟流式输出）
            chunk_size = 50
            for i in range(0, len(response), chunk_size):
                chunk = response[i:i + chunk_size]
                chunk_data = {
                    "chunkType": "content",
                    "content": chunk,
                    "isFinal": i + chunk_size >= len(response)
                }
                yield f"data: {json.dumps(chunk_data, ensure_ascii=False)}\n\n"
        except Exception as e:
            error_data = {
                "chunkType": "error",
                "content": f"处理失败: {str(e)}",
                "isFinal": True
            }
            yield f"data: {json.dumps(error_data, ensure_ascii=False)}\n\n"
    
    return StreamingResponse(
        generate(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        }
    )
