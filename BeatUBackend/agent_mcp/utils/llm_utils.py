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
    
    API_KEY = os.getenv("API_KEY")
    BASE_URL = os.getenv("BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1")
    MODEL = os.getenv("MODEL", "qwen-flash")
    return ChatOpenAI(model=MODEL, base_url=BASE_URL, api_key=API_KEY)

