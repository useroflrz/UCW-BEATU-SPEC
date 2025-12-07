"""智能体模块"""

# 延迟导入以避免循环依赖
__all__ = ["TaskDecomposer", "ToolDiscoveryAgent", "ResponseSynthesizer"]

def __getattr__(name):
    if name == "TaskDecomposer":
        from agent_mcp.agents.task_decomposer import TaskDecomposer
        return TaskDecomposer
    elif name == "ToolDiscoveryAgent":
        from agent_mcp.agents.tool_discovery_agent import ToolDiscoveryAgent
        return ToolDiscoveryAgent
    elif name == "ResponseSynthesizer":
        from agent_mcp.agents.response_synthesizer import ResponseSynthesizer
        return ResponseSynthesizer
    raise AttributeError(f"module {__name__!r} has no attribute {name!r}")
