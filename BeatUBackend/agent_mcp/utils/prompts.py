"""Prompt 模板"""

# 任务分解 Prompt
TASK_DECOMPOSITION_PROMPT = """你是我的智能助手。你的任务是分析用户问题，并将其分解为需要不同类型工具才能解决的子任务。

请仔细分析用户的问题，识别出所有独立的子任务，并为每个子任务提取必要的参数。

请以 JSON 格式输出，格式如下：
{{
    "analysis": "对用户请求的分析说明",
    "sub_tasks": [
        {{
            "id": "task_001",
            "goal": "任务目标描述",
            "required_tool_type": "所需工具类型描述，例如：'weather forecast, location-based service'",
            "extracted_params": {{
                "参数名": "参数值"
            }}
        }}
    ]
}}

用户问题: {user_input}

请开始分析："""

# 工具发现 Prompt
TOOL_DISCOVERY_PROMPT = """现在需要为任务寻找一个合适的工具。你可以使用以下两个基础工具来探索 MCP 文件系统：

1. read_file_structure(path: str): 查看目录内容，返回该目录下的子目录和文件名列表
2. read_file_content(path: str): 查看 MCP 文件的详细描述，返回 MCP 的完整信息

文件系统的根目录是 /。每个服务位于 `/分类/服务名称/` 目录下，通常包含 `config.json`（服务元数据）以及 `mcp/` 子目录中的 JSON 配置（包含 `mcpServers` 字段描述可用的 MCP 服务器）。

任务信息：
- 任务ID: {task_id}
- 任务目标: {goal}
- 所需工具类型: {required_tool_type}
- 提取的参数: {extracted_params}

请规划你的探索步骤，使用上述工具来找到合适的 MCP 服务。当你找到合适的工具后，请告诉我工具路径和确认信息。"""

# 执行计划制定 Prompt
EXECUTION_PLAN_PROMPT = """工具发现已完成。请根据以下信息，为原始用户请求制定一个最终的、可执行的 MCP 调用计划。

原始用户请求: {user_input}

发现结果：
{discovery_results}

**重要要求：请只返回纯 JSON 格式的执行计划，不要包含任何 Markdown 代码块标记（如 ```json 或 ```）、解释性文本、注释或其他内容。直接返回 JSON 对象即可。**

执行计划 JSON 格式（请严格按照此格式返回，不要添加任何额外内容）：
{{
    "plan": [
        {{
            "task_id": "task_001",
            "mcp_to_execute": {{
                "server_name": "MCP服务器名称（来自 mcpServers 的键）",
                "tool_name": "需要调用的工具名称（如果已知，可来自 MCP 服务器暴露的工具）",
                "arguments": {{
                    "参数名": "参数值"
                }}
            }}
        }}
    ]
}}

请确保：
1. server_name 必须与发现结果中 MCP 服务器配置的键一致
2. 若能确定 tool_name，请与 MCP 服务器所提供的工具名称匹配；若无法确定，可留空
3. 参数值从 extracted_params 中正确映射，并保证格式合法
4. **只返回 JSON 对象，不要包含 ```json 代码块标记、不要包含任何解释性文字、不要包含注释**

现在请直接返回执行计划的 JSON："""

# 结果整合 Prompt
RESPONSE_SYNTHESIS_PROMPT = """所有任务已执行完毕。请根据以下信息，为用户生成一个自然、流畅的最终答复。

原始问题: {user_input}

执行结果:
{execution_results}

请生成一个友好、清晰、完整的回答，整合所有执行结果。
如果执行结果中包含 `config_name` 字段，请在回答中明确点名对应的 MCP 配置名称（例如直接引用 `@{{config_name}}`）。"""
