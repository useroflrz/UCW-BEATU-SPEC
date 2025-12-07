"""工具发现 Agent"""

from typing import Optional
from langchain_openai import ChatOpenAI
from langchain.agents import create_agent
from agent_mcp.models.task_schema import SubTask, DiscoveryResult
from agent_mcp.models.mcp_schema import MCPDescriptor, MCPServerConfigFile
from agent_mcp.tools.core_primitives import get_core_tools, get_filesystem
from agent_mcp.utils.prompts import TOOL_DISCOVERY_PROMPT
from agent_mcp.utils.llm_utils import create_default_llm
from agent_mcp.utils.logger import setup_logger
import json


class ToolDiscoveryAgent:
    """工具发现 Agent
    
    使用 ReAct 模式探索 MCP 文件系统，找到合适的工具。
    """
    
    def __init__(self, llm: ChatOpenAI = None):
        """初始化工具发现 Agent
        
        Args:
            llm: LangChain LLM 实例，如果为 None 则使用默认配置
        """
        self.llm = create_default_llm(llm)
        self.tools = get_core_tools()
        self.logger = setup_logger(__name__)
        
        # 系统提示词
        system_prompt = """你是一个工具发现助手。你的任务是找到合适的 MCP 工具来完成用户的任务。

你可以使用以下工具来探索 MCP 文件系统：
- read_file_structure: 读取指定路径下的目录结构
- read_file_content: 读取 MCP 文件的详细描述

根目录是 `/`，每个服务位于 `/<分类>/<服务名称>/`。目录内通常包含：
- `config.json`：服务的基本描述（JSON 格式）
- `mcp/*.json`：包含 `mcpServers` 字段的 MCP 服务器配置

MCP 支持多种加载方式：
1. HTTP 方式: {"type": "streamable_http", "url": "远程地址"}
2. 本地进程方式: {"command": "命令", "args": ["参数1", "参数2"]}

为了高效查找，请遵循以下策略：
1. 首先使用 read_file_structure 查看根目录 '/' 下的一级目录
2. 根据任务类型判断哪个一级目录最可能包含合适的工具
3. 如果一级目录不匹配任务类型，直接返回未找到，无需继续深入
4. 如果一级目录匹配，进入该目录继续查找二级目录
5. 在匹配的二级目录中查找 config.json 文件
6. 读取 config.json 确认服务是否匹配任务需求
7. 如果匹配，找到对应的 mcp 目录下的具体配置文件（如 mcp/howtocook_mcp.json）

重要提示：
- 不要返回包含通配符 (*) 的路径
- 必须返回具体的文件路径，例如 "/food/howtocook/mcp/howtocook_mcp.json"
- 确保返回的路径指向实际存在的文件

当你找到合适的文件时，请以 JSON 格式返回结果：
{"task_id": "...", "mcp_path": "...", "status": "found"}"""
        
        # 创建 Agent (使用 LangChain v1 的 create_agent)
        self.agent = create_agent(
            model=self.llm,
            tools=self.tools,
            system_prompt=system_prompt
        )
    
    def discover(self, sub_task: SubTask) -> DiscoveryResult:
        """发现适合的子任务工具（同步版本）
        
        Args:
            sub_task: 子任务定义
        
        Returns:
            DiscoveryResult: 工具发现结果
        """
        import asyncio
        return asyncio.run(self.discover_async(sub_task))
    
    async def discover_async(self, sub_task: SubTask) -> DiscoveryResult:
        """发现适合的子任务工具（异步版本）
        
        Args:
            sub_task: 子任务定义
        
        Returns:
            DiscoveryResult: 工具发现结果
        """
        self.logger.info(f"开始为任务 {sub_task.id} 发现工具...")
        self.logger.debug(f"任务目标: {sub_task.goal}")
        self.logger.debug(f"所需工具类型: {sub_task.required_tool_type}")
        
        # 构建输入
        input_text = TOOL_DISCOVERY_PROMPT.format(
            task_id=sub_task.id,
            goal=sub_task.goal,
            required_tool_type=sub_task.required_tool_type,
            extracted_params=json.dumps(sub_task.extracted_params, ensure_ascii=False)
        )
        
        # 执行 Agent (使用 LangChain v1 的 ainvoke API)
        try:
            result = await self.agent.ainvoke({
                "messages": [{"role": "user", "content": input_text}]
            })
            
            # 从结果中提取最终答案
            # LangChain v1 返回的是消息列表，最后一条消息是最终答案
            if result and "messages" in result:
                messages = result["messages"]
                if messages:
                    final_message = messages[-1]
                    final_answer = final_message.content if hasattr(final_message, "content") else str(final_message)
                else:
                    final_answer = str(result)
            else:
                final_answer = str(result)
            
            self.logger.debug(f"LLM 返回结果: {final_answer}")
            
            # 解析结果
            discovery_result = self._parse_discovery_result(final_answer, sub_task.id)
            self.logger.info(f"解析后的发现结果: task_id={discovery_result.task_id}, mcp_path={discovery_result.mcp_path}, status={discovery_result.status}")
            return discovery_result
        except Exception as e:
            self.logger.error(f"工具发现过程中出现错误: {e}", exc_info=True)
            return DiscoveryResult(
                task_id=sub_task.id,
                mcp_path="",
                status="not_found"
            )
    
    def _parse_discovery_result(self, final_answer: str, task_id: str) -> DiscoveryResult:
        """解析发现结果
        
        Args:
            final_answer: Agent 的最终答案
            task_id: 任务ID
        
        Returns:
            DiscoveryResult: 解析后的结果
        """
        # 尝试从答案中提取 JSON
        try:
            # 查找 JSON 部分
            import re
            json_match = re.search(r'\{[^}]+\}', final_answer)
            if json_match:
                data = json.loads(json_match.group())
                result = DiscoveryResult(
                    task_id=data.get("task_id", task_id),
                    mcp_path=data.get("mcp_path", ""),
                    status=data.get("status", "found")
                )
                if result.mcp_path:
                    result.mcp_descriptor = self._load_descriptor_snapshot(result.mcp_path)
                return result
        except Exception:
            pass
        
        # 如果解析失败，返回未找到
        return DiscoveryResult(
            task_id=task_id,
            mcp_path="",
            status="not_found"
        )
    
    def _load_descriptor_snapshot(self, mcp_path: str):
        """读取 MCP 描述或服务器配置的快照，以便后续制定计划"""
        try:
            filesystem = get_filesystem()
            content = filesystem.read_file_content(mcp_path)
            if isinstance(content, (MCPDescriptor, MCPServerConfigFile)):
                return content.model_dump()
            if isinstance(content, (dict, list)):
                return content
            return {"raw": str(content)}
        except Exception:
            return None
