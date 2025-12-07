"""工具模块"""

# 延迟导入以避免循环导�?
__all__ = ["read_file_structure", "read_file_content"]

def __getattr__(name):
    if name == "read_file_structure":
        from agent_mcp.tools.core_primitives import read_file_structure
        return read_file_structure
    elif name == "read_file_content":
        from agent_mcp.tools.core_primitives import read_file_content
        return read_file_content
    raise AttributeError(f"module {__name__!r} has no attribute {name!r}")

