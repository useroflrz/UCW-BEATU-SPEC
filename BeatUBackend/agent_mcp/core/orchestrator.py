"""智能体编排器"""

from typing import Dict, Any, List, Optional
from langchain_openai import ChatOpenAI
from langchain_mcp_adapters.client import MultiServerMCPClient
from langchain.agents import create_agent
from agent_mcp.agents.task_decomposer import TaskDecomposer
from agent_mcp.agents.tool_discovery_agent import ToolDiscoveryAgent
from agent_mcp.agents.response_synthesizer import ResponseSynthesizer
from agent_mcp.core.mcp_filesystem import MCPFilesystem
from agent_mcp.models.task_schema import TaskDecompositionResult, DiscoveryResult, SubTask
from agent_mcp.models.execution_plan import ExecutionPlan
from agent_mcp.models.mcp_schema import MCPDescriptor, MCPServerConfigFile
from agent_mcp.tools.core_primitives import initialize_filesystem
from agent_mcp.utils.prompts import EXECUTION_PLAN_PROMPT
from agent_mcp.utils.llm_utils import create_default_llm
from agent_mcp.utils.logger import setup_logger
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import PydanticOutputParser
from langchain_core.messages import BaseMessage
from pydantic import ValidationError
import json
import asyncio
import os
from pathlib import Path


class AgentOrchestrator:
    """智能体编排器
    
    协调整个工作流程，管理各个 Agent 的调用。
    """
    
    def __init__(
        self,
        llm: ChatOpenAI = None,
        mcp_filesystem_root: str = "mcp_registry"
    ):
        """初始化编排器
        
        Args:
            llm: LangChain LLM 实例
            mcp_filesystem_root: MCP 文件系统根路径
        """
        self.llm = create_default_llm(llm)
        
        # 初始化各个 Agent
        self.task_decomposer = TaskDecomposer(llm=self.llm)
        self.tool_discovery_agent = ToolDiscoveryAgent(llm=self.llm)
        self.response_synthesizer = ResponseSynthesizer(llm=self.llm)
        
        # 初始化 MCP 文件系统
        initialize_filesystem(root_path=mcp_filesystem_root)
        self.mcp_filesystem = MCPFilesystem(root_path=mcp_filesystem_root)
        
        # 初始化 MultiServerMCPClient
        self.mcp_client: Optional[MultiServerMCPClient] = None
        self.agent = None
        
        # 执行计划解析器
        self.plan_parser = PydanticOutputParser(pydantic_object=ExecutionPlan)
        self.plan_prompt_template = ChatPromptTemplate.from_template(
            EXECUTION_PLAN_PROMPT
        )
        
        # 存储 discovery_results（用于执行阶段）
        self._discovery_results: List[DiscoveryResult] = []
        self._discovery_map: Dict[str, DiscoveryResult] = {}
        self._task_map: Dict[str, SubTask] = {}
        self._mcp_content_cache: Dict[str, Any] = {}
        
        # 初始化日志记录器
        self.logger = setup_logger(__name__)
    
    def process_user_request(self, user_input: str) -> str:
        """处理用户请求（同步版本）
        
        Args:
            user_input: 用户输入的自然语言请求
        
        Returns:
            str: 最终响应
        """
        # 使用 asyncio 运行异步版本
        return asyncio.run(self.process_user_request_async(user_input))
    
    async def process_user_request_async(self, user_input: str) -> str:
        """处理用户请求（异步版本）
        
        Args:
            user_input: 用户输入的自然语言请求
        
        Returns:
            str: 最终响应
        """
        self.logger.info(f"开始处理用户请求: {user_input}")
        
        # Step 1: 任务分解
        self.logger.info("步骤 1: 任务分解")
        decomposition_result = self.task_decomposer.decompose(user_input)
        
        # 打印分解后的子任务，帮助理解大模型的决策过程
        print("\n" + "=" * 50)
        print("任务分解结果:")
        print("=" * 50)
        print(f"任务分析: {decomposition_result.analysis}")
        for i, sub_task in enumerate(decomposition_result.sub_tasks, 1):
            print(f"\n子任务 {i}:")
            print(f"  ID: {sub_task.id}")
            print(f"  目标: {sub_task.goal}")
            print(f"  所需工具类型: {sub_task.required_tool_type}")
            print(f"  提取参数: {sub_task.extracted_params}")
        print("=" * 50 + "\n")
        
        # Step 2: 工具发现（并行执行）
        self.logger.info("步骤 2: 工具发现")
        discovery_tasks = [
            self.tool_discovery_agent.discover_async(sub_task)
            for sub_task in decomposition_result.sub_tasks
        ]
        discovery_results = await asyncio.gather(*discovery_tasks)
        
        # Step 3: 制定执行计划
        self.logger.info("步骤 3: 制定执行计划")
        execution_plan = await self._formulate_execution_plan(
            user_input,
            decomposition_result,
            discovery_results
        )
        
        # Step 4: 初始化 MCP 客户端并执行计划
        self.logger.info("步骤 4: 执行计划")
        execution_results = await self._execute_plan_with_mcp_client(
            execution_plan,
            discovery_results
        )
        
        # Step 5: 结果整合
        self.logger.info("步骤 5: 结果整合")
        final_response = self.response_synthesizer.synthesize(
            user_input,
            execution_results
        )
        
        self.logger.info("请求处理完成")
        return final_response
    
    def _extract_json_from_markdown(self, content: str) -> str:
        """从 Markdown 代码块中提取 JSON 内容，并清理注释
        
        Args:
            content: 可能包含 Markdown 代码块的文本内容
            
        Returns:
            str: 提取并清理后的 JSON 字符串，如果未找到则返回原始内容
        """
        import re
        
        # 方法1: 查找 ```json ... ``` 或 ``` ... ``` 代码块
        json_block_pattern = r'```(?:json)?\s*(\{.*?\})\s*```'
        match = re.search(json_block_pattern, content, re.DOTALL)
        if match:
            extracted = match.group(1).strip()
            cleaned = self._clean_json(extracted)
            try:
                json.loads(cleaned)
                return cleaned
            except json.JSONDecodeError:
                pass
        
        # 方法2: 查找第一个完整的 JSON 对象（从 { 开始到匹配的 } 结束）
        # 使用更精确的匹配，找到最外层的 JSON 对象
        brace_count = 0
        start_idx = -1
        for i, char in enumerate(content):
            if char == '{':
                if start_idx == -1:
                    start_idx = i
                brace_count += 1
            elif char == '}':
                brace_count -= 1
                if brace_count == 0 and start_idx != -1:
                    # 找到了完整的 JSON 对象
                    json_str = content[start_idx:i+1]
                    cleaned = self._clean_json(json_str)
                    try:
                        json.loads(cleaned)
                        return cleaned.strip()
                    except json.JSONDecodeError:
                        # 继续查找下一个
                        start_idx = -1
                        brace_count = 0
        
        # 方法3: 如果都没有找到，尝试清理原始内容
        cleaned = self._clean_json(content)
        return cleaned
    
    def _clean_json(self, json_str: str) -> str:
        """清理 JSON 字符串，去除注释和无效字符
        
        Args:
            json_str: 可能包含注释的 JSON 字符串
            
        Returns:
            str: 清理后的 JSON 字符串
        """
        import re
        
        # 去除多行注释 /* ... */
        json_str = re.sub(r'/\*.*?\*/', '', json_str, flags=re.DOTALL)
        
        # 去除单行注释 // ...（但要小心字符串中的 //）
        # 使用更智能的方法：只在引号外去除注释
        lines = json_str.split('\n')
        cleaned_lines = []
        in_string = False
        escape_next = False
        
        for line in lines:
            cleaned_line = []
            i = 0
            while i < len(line):
                char = line[i]
                
                if escape_next:
                    cleaned_line.append(char)
                    escape_next = False
                    i += 1
                    continue
                
                if char == '\\':
                    escape_next = True
                    cleaned_line.append(char)
                    i += 1
                    continue
                
                if char == '"':
                    in_string = not in_string
                    cleaned_line.append(char)
                    i += 1
                    continue
                
                if not in_string and i < len(line) - 1 and line[i:i+2] == '//':
                    # 找到注释，跳过该行剩余部分
                    break
                
                cleaned_line.append(char)
                i += 1
            
            cleaned_line_str = ''.join(cleaned_line).rstrip()
            if cleaned_line_str:  # 只添加非空行
                cleaned_lines.append(cleaned_line_str)
        
        result = '\n'.join(cleaned_lines)
        
        # 去除尾随逗号（在 } 或 ] 之前）
        result = re.sub(r',(\s*[}\]])', r'\1', result)
        
        return result.strip()
    
    async def _formulate_execution_plan(
        self,
        user_input: str,
        decomposition_result: TaskDecompositionResult,
        discovery_results: List[DiscoveryResult]
    ) -> ExecutionPlan:
        """制定执行计划
        
        Args:
            user_input: 原始用户输入
            decomposition_result: 任务分解结果
            discovery_results: 工具发现结果
        
        Returns:
            ExecutionPlan: 执行计划
        """
        # 存储 discovery_results 以便后续使用
        self._discovery_results = discovery_results
        self._discovery_map = {dr.task_id: dr for dr in discovery_results}
        self._task_map = {task.id: task for task in decomposition_result.sub_tasks}
        self._mcp_content_cache = {}  # 使用 mcp_path 作为键
        
        # 构建发现结果文本
        discovery_text = []
        for task in decomposition_result.sub_tasks:
            discovery = self._discovery_map.get(task.id)
            mcp_path = discovery.mcp_path if discovery else ""
            mcp_desc = ""
            if discovery and mcp_path:
                # 使用 mcp_path 作为缓存键，避免重复加载
                content = self._load_mcp_content_cached(mcp_path, discovery)
                mcp_desc = self._serialize_mcp_content(content)
            else:
                # 如果 mcp_path 为空，仍然缓存 None
                if mcp_path:
                    self._mcp_content_cache[mcp_path] = None
            
            discovery_text.append(f"""
任务 {task.id}:
- 目标: {task.goal}
- 找到的MCP: {mcp_path}
- MCP描述: {mcp_desc}
- 提取的参数: {json.dumps(task.extracted_params, ensure_ascii=False)}
""")
        
        discovery_results_text = "\n".join(discovery_text)
        
        # 构建 Prompt
        prompt = self.plan_prompt_template.format_messages(
            user_input=user_input,
            discovery_results=discovery_results_text
        )
        
        # 调用 LLM
        response = self.llm.invoke(prompt)
        
        # 解析输出（改进：支持从 Markdown 代码块中提取 JSON）
        try:
            content = response.content.strip()
            # 尝试直接解析
            plan = self.plan_parser.parse(content)
            return plan
        except (ValidationError, json.JSONDecodeError, ValueError) as e:
            self.logger.warning(f"直接解析失败，尝试从 Markdown 中提取 JSON: {e}")
            self.logger.debug(f"LLM 原始输出（前500字符）: {response.content[:500]}")
            
            # 尝试从 Markdown 代码块中提取 JSON
            try:
                json_content = self._extract_json_from_markdown(response.content)
                if json_content and json_content != response.content:
                    self.logger.info("成功从 Markdown 中提取 JSON，重新解析...")
                    plan = self.plan_parser.parse(json_content)
                    return plan
            except Exception as e2:
                self.logger.error(f"从 Markdown 提取 JSON 也失败: {e2}", exc_info=True)
            
            # 如果都失败了，记录详细错误并抛出异常
            self.logger.error(f"解析执行计划失败: {e}", exc_info=True)
            self.logger.error(f"LLM 完整输出: {response.content}")
            raise ValueError(f"解析执行计划失败: {e}") from e
    
    async def _execute_plan_with_mcp_client(
        self,
        plan: ExecutionPlan,
        discovery_results: List[DiscoveryResult]
    ) -> Dict[str, Any]:
        """使用 MultiServerMCPClient 执行计划
        
        Args:
            plan: 执行计划
            discovery_results: 工具发现结果
        
        Returns:
            Dict[str, Any]: 执行结果，键为任务ID，值为执行结果
        """
        results: Dict[str, Any] = {}
        mcp_config = self._build_mcp_config(discovery_results)
        
        self.logger.debug(f"构建的 MCP 配置: {mcp_config}")
        
        if not mcp_config:
            self.logger.warning("未找到任何 MCP 服务器配置")
            for item in plan.plan:
                results[item.task_id] = {"error": "未找到 MCP 服务器配置"}
            return results
        
        try:
            self.logger.info("初始化 MultiServerMCPClient...")
            self.mcp_client = MultiServerMCPClient(mcp_config)
            tools = await self.mcp_client.get_tools()
            self.logger.info(f"获取到 {len(tools)} 个工具")
            self.agent = create_agent(model=self.llm, tools=tools)
        except Exception as e:
            self.logger.error(f"MCP 客户端初始化失败: {e}", exc_info=True)
            for item in plan.plan:
                results[item.task_id] = {"error": f"MCP 客户端初始化失败: {str(e)}"}
            return results
        
        # 并行执行所有任务
        async def execute_single_task(item):
            """执行单个任务的辅助函数"""
            task_id = item.task_id
            execution_item = item.mcp_to_execute
            self.logger.info(f"执行任务 {task_id}...")
            
            if execution_item is None:
                self.logger.warning(f"任务 {task_id} 缺少 MCP 执行项")
                return task_id, self._with_config_name(
                    {"error": "执行计划缺少 MCP 执行项"},
                    self._get_config_name_for_task(task_id),
                )
            
            server_name = execution_item.server_name
            if not server_name:
                self.logger.warning(f"任务 {task_id} 未指定 MCP 服务器")
                return task_id, self._with_config_name(
                    {"error": "执行计划未指定 MCP 服务器"},
                    self._get_config_name_for_task(task_id),
                )
            if server_name not in mcp_config:
                self.logger.warning(f"任务 {task_id} 未找到服务器配置: {server_name}")
                return task_id, self._with_config_name(
                    {"error": f"未找到服务器配置: {server_name}"},
                    self._get_config_name_for_task(task_id),
                )
            
            try:
                task_goal = self._task_map.get(task_id).goal if task_id in self._task_map else ""
            except Exception:
                task_goal = ""
            
            config_name = self._get_config_name_for_task(task_id)
            self.logger.debug(f"任务 {task_id} 使用的配置名称: {config_name}")
            
            # 确保 config_name 在异常处理中可用
            try:
                user_message = self._build_user_message(
                    server_name=server_name,
                    tool_name=execution_item.tool_name,
                    arguments=execution_item.arguments,
                    task_goal=task_goal
                )
                self.logger.debug(f"发送给 MCP 服务器的消息: {user_message}")
                
                # 记录将要调用的工具信息
                if execution_item.tool_name:
                    self.logger.debug(f"将要调用的工具: {execution_item.tool_name}")
                else:
                    self.logger.debug(f"将要调用的服务器: {server_name}")
                
                response = await self.agent.ainvoke({
                    "messages": [{"role": "user", "content": user_message}]
                })
                self.logger.info(f"任务 {task_id} 执行完成")
                
                return task_id, self._with_config_name(
                    {
                        "response": self._convert_to_serializable(response),
                    },
                    config_name,
                )
            except Exception as e:
                self.logger.error(f"任务 {task_id} 执行失败: {e}", exc_info=True)
                return task_id, self._with_config_name(
                    {"error": str(e)},
                    config_name,
                )
        
        # 并行执行所有任务
        execution_tasks = [execute_single_task(item) for item in plan.plan]
        task_results = await asyncio.gather(*execution_tasks)
        
        # 将结果转换为字典
        for task_id, result in task_results:
            results[task_id] = result
        
        return results
    
    def _build_user_message(
        self,
        server_name: str,
        tool_name: Optional[str],
        arguments: Dict[str, Any],
        task_goal: str = ""
    ) -> str:
        """构建用户消息
        
        Args:
            server_name: MCP 服务器名称
            tool_name: 具体工具名称，可选
            arguments: 执行参数
            task_goal: 任务目标上下文
        
        Returns:
            str: 用户消息
        """
        param_str = ", ".join([f"{k}={v}" for k, v in arguments.items()]) or "无"
        goal_text = f"任务目标: {task_goal}\n" if task_goal else ""
        if tool_name:
            return (
                f"{goal_text}请在 MCP 服务器 `{server_name}` 下调用工具 `{tool_name}`。"
                f"使用参数: {param_str}。返回执行结果。"
            )
        return (
            f"{goal_text}请在 MCP 服务器 `{server_name}` 中选择最合适的工具完成任务。"
            f"推荐参数: {param_str}。完成后请返回执行结果。"
        )

    def _build_mcp_config(
        self,
        discovery_results: List[DiscoveryResult]
    ) -> Dict[str, Dict[str, Any]]:
        """构建 MultiServerMCPClient 所需的服务器配置"""
        self.logger.debug(f"构建 MCP 配置，发现结果数量: {len(discovery_results)}")
        mcp_config: Dict[str, Dict[str, Any]] = {}
        for discovery_result in discovery_results:
            self.logger.debug(f"处理发现结果: {discovery_result}")
            if not discovery_result:
                self.logger.debug("跳过空的发现结果")
                continue
            # 使用 mcp_path 作为缓存键
            mcp_path = discovery_result.mcp_path or ""
            content = self._mcp_content_cache.get(mcp_path) if mcp_path else None
            self.logger.debug(f"从缓存获取内容(path={mcp_path}): {content}")
            if content is None and mcp_path:
                content = self._load_mcp_content_cached(mcp_path, discovery_result)
                self.logger.debug(f"加载后的内容: {content}")
            self._merge_mcp_config_from_content(mcp_config, content, discovery_result)
        self.logger.debug(f"最终构建的 MCP 配置: {mcp_config}")
        return mcp_config

    def _merge_mcp_config_from_content(
        self,
        target_config: Dict[str, Dict[str, Any]],
        content: Any,
        discovery_result: DiscoveryResult
    ):
        """将解析到的 MCP 信息合并到配置中"""
        self.logger.debug(f"合并 MCP 配置内容: content={content}")
        descriptor = self._try_parse_descriptor(content)
        if descriptor:
            self.logger.debug(f"解析到 MCP 描述符: {descriptor}")
            if descriptor.server_config:
                config = self._ensure_transport_key(descriptor.server_config)
                self.logger.debug(f"使用描述符服务器配置: {config}")
                target_config.setdefault(
                    descriptor.name,
                    config
                )
            else:
                config = {
                    "transport": "streamable_http",
                    "url": f"http://localhost:8000/mcp/{descriptor.name}"
                }
                self.logger.debug(f"使用默认服务器配置: {config}")
                target_config.setdefault(
                    descriptor.name,
                    config
                )
        
        server_config = self._try_parse_server_config(content)
        if server_config:
            self.logger.debug(f"解析到服务器配置文件: {server_config}")
            for server_name, cfg in server_config.mcpServers.items():
                # cfg.to_multiserver_config() 已经处理了 type -> transport 的转换，无需再次处理
                converted_config = cfg.to_multiserver_config()
                self.logger.debug(f"服务器 {server_name} 的配置: {converted_config}")
                target_config.setdefault(
                    server_name,
                    converted_config
                )
        
        if isinstance(content, dict) and "server_config" in content:
            server_name = content.get("name") or discovery_result.task_id
            raw_config = content.get("server_config") or {}
            if isinstance(raw_config, dict):
                converted_config = self._ensure_transport_key(raw_config)
                self.logger.debug(f"从字典解析的服务器配置: {converted_config}")
                target_config.setdefault(
                    server_name,
                    converted_config
                )
        self.logger.debug(f"合并后的目标配置: {target_config}")

    def _load_mcp_content_cached(self, mcp_path: str, discovery: DiscoveryResult):
        """根据 mcp_path 加载 MCP 描述或配置（带缓存）
        
        Args:
            mcp_path: MCP 文件路径（用作缓存键）
            discovery: 发现结果对象
        
        Returns:
            MCP 内容，如果加载失败则返回 None
        """
        # 检查缓存
        if mcp_path in self._mcp_content_cache:
            cached_content = self._mcp_content_cache[mcp_path]
            self.logger.debug(f"从缓存获取 MCP 内容 (path={mcp_path})")
            return cached_content
        
        # 如果没有缓存，加载内容
        self.logger.debug(f"加载 MCP 内容: task_id={discovery.task_id}, mcp_path={mcp_path}")
        
        # 优先使用 discovery 中缓存的描述符
        if discovery.mcp_descriptor is not None:
            self.logger.debug(f"使用 discovery 中缓存的 MCP 描述: {discovery.mcp_descriptor}")
            self._mcp_content_cache[mcp_path] = discovery.mcp_descriptor
            return discovery.mcp_descriptor
        
        # 从文件系统加载
        if mcp_path:
            try:
                content = self.mcp_filesystem.read_file_content(mcp_path)
                self.logger.debug(f"从文件加载 MCP 内容: {content}")
                self._mcp_content_cache[mcp_path] = content
                return content
            except (FileNotFoundError, ValueError, json.JSONDecodeError, OSError) as e:
                self.logger.error(f"加载 MCP 文件失败: {e}", exc_info=True)
                self._mcp_content_cache[mcp_path] = None
                return None
        
        self.logger.warning("未找到有效的 MCP 路径")
        self._mcp_content_cache[mcp_path] = None
        return None

    def _serialize_mcp_content(self, content: Any) -> str:
        """将 MCP 信息转为 JSON 字符串，用于提示词"""
        if content is None:
            return ""
        if isinstance(content, MCPDescriptor):
            return json.dumps(content.model_dump(), ensure_ascii=False)
        if isinstance(content, MCPServerConfigFile):
            return json.dumps(content.model_dump(by_alias=True), ensure_ascii=False)
        if isinstance(content, (dict, list)):
            try:
                return json.dumps(content, ensure_ascii=False)
            except TypeError:
                return str(content)
        return str(content)

    def _try_parse_descriptor(self, content: Any) -> Optional[MCPDescriptor]:
        """尝试将内容解析为 MCPDescriptor"""
        if isinstance(content, MCPDescriptor):
            return content
        if isinstance(content, dict):
            try:
                return MCPDescriptor(**content)
            except (ValidationError, TypeError, ValueError) as e:
                self.logger.debug(f"解析 MCPDescriptor 失败: {e}")
                return None
        return None

    def _try_parse_server_config(self, content: Any) -> Optional[MCPServerConfigFile]:
        """尝试将内容解析为 MCP 服务器配置"""
        if isinstance(content, MCPServerConfigFile):
            return content
        if isinstance(content, dict) and "mcpServers" in content:
            try:
                return MCPServerConfigFile(**content)
            except (ValidationError, TypeError, ValueError) as e:
                self.logger.debug(f"解析 MCPServerConfigFile 失败: {e}")
                return None
        return None

    @staticmethod
    def _ensure_transport_key(config: Dict[str, Any]) -> Dict[str, Any]:
        """确保配置中包含 transport 字段"""
        if config is None:
            return {}
        new_config = {k: v for k, v in dict(config).items() if v is not None}
        if "transport" in new_config:
            return new_config
        if "type" in new_config:
            new_config["transport"] = new_config.pop("type")
        return new_config
    
    def _get_config_name_for_task(self, task_id: str) -> Optional[str]:
        discovery = self._discovery_map.get(task_id)
        if not discovery or not discovery.mcp_path:
            return None
        return self._get_config_name(discovery.mcp_path)

    def _get_config_name(self, mcp_path: str) -> Optional[str]:
        base_dir = Path(mcp_path).parent.parent
        for filename in ("config.json", "config.txt"):
            try:
                config_path = base_dir / filename
                content = self.mcp_filesystem.read_file_content(str(config_path))
                if isinstance(content, dict):
                    name = content.get("name")
                    if isinstance(name, str):
                        return name
            except (FileNotFoundError, ValueError, json.JSONDecodeError, OSError):
                continue
        return None

    @staticmethod
    def _with_config_name(payload: Dict[str, Any], config_name: Optional[str]) -> Dict[str, Any]:
        if config_name:
            payload = dict(payload)
            payload["config_name"] = config_name
        return payload

    def _convert_to_serializable(self, data: Any) -> Any:
        if isinstance(data, dict):
            return {key: self._convert_to_serializable(value) for key, value in data.items()}
        if isinstance(data, list):
            return [self._convert_to_serializable(item) for item in data]
        if isinstance(data, BaseMessage):
            return {
                "type": data.type,
                "role": getattr(data, "role", None),
                "content": data.content,
            }
        if hasattr(data, "model_dump"):
            try:
                return self._convert_to_serializable(data.model_dump())
            except Exception:
                pass
        if hasattr(data, "__dict__"):
            try:
                return self._convert_to_serializable(vars(data))
            except Exception:
                pass
        return data
    
    async def close(self):
        """关闭资源"""
        if self.mcp_client:
            # MultiServerMCPClient 可能需要关闭连接
            # 根据实际 API 调整
            pass
