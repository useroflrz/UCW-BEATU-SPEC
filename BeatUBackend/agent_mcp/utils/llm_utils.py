"""LLM 工具函数"""

from typing import Optional
from langchain_openai import ChatOpenAI
import os


def create_default_llm(llm: Optional[ChatOpenAI] = None) -> ChatOpenAI:
    """创建默认�?LLM 实例
    
    如果提供�?llm 参数，则直接返回；否则从环境变量创建新的实例�?
    
    Args:
        llm: 可选的已有 LLM 实例
    
    Returns:
        ChatOpenAI: LLM 实例 """
    if llm is not None:
        return llm
    
    # ✅ 修复：优先使用 DASHSCOPE_API_KEY（通义千问文档推荐），然后尝试其他环境变量
    # 支持多种配置方式：LLM_API_KEY（新配置）、MCP_API_KEY（向后兼容）、DASHSCOPE_API_KEY、API_KEY、OPENAI_API_KEY
    API_KEY = (
        os.getenv("DASHSCOPE_API_KEY") or 
        os.getenv("LLM_API_KEY") or  # ✅ 新增：支持 LLM_API_KEY
        os.getenv("MCP_API_KEY") or  # ✅ 新增：向后兼容 MCP_API_KEY
        os.getenv("API_KEY") or 
        os.getenv("OPENAI_API_KEY")
    )
    BASE_URL = os.getenv("BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1")
    MODEL = os.getenv("MODEL", "qwen-flash")
    
    if not API_KEY:
        raise ValueError(
            "API Key 未配置！请设置 DASHSCOPE_API_KEY、API_KEY 或 OPENAI_API_KEY 环境变量。"
            "在 BeatUBackend 中，请在 .env 文件中设置 MCP_API_KEY 或 DASHSCOPE_API_KEY。"
            "获取 API Key：https://bailian.console.aliyun.com/?tab=model#/api-key"
        )
    
    # ✅ 根据通义千问文档，ChatOpenAI 会自动使用 stream=True 进行流式输出
    # 这里创建基础实例，实际流式调用在业务层使用 astream() 方法
    return ChatOpenAI(
        model=MODEL, 
        base_url=BASE_URL, 
        api_key=API_KEY,
        # 注意：stream 参数在调用时设置，不在初始化时设置
    )

