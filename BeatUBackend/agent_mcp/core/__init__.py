"""核心组件模块"""

# 延迟导入以避免循环导�?
__all__ = ["AgentOrchestrator", "MCPFilesystem", "MCPExecutionEngine"]

def __getattr__(name):
    if name == "AgentOrchestrator":
        from agent_mcp.core.orchestrator import AgentOrchestrator
        return AgentOrchestrator
    elif name == "MCPFilesystem":
        from agent_mcp.core.mcp_filesystem import MCPFilesystem
        return MCPFilesystem
    elif name == "MCPExecutionEngine":
        from agent_mcp.core.execution_engine import MCPExecutionEngine
        return MCPExecutionEngine
    raise AttributeError(f"module {__name__!r} has no attribute {name!r}")

