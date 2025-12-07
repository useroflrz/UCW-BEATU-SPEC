"""结果整合 Agent"""

from typing import Dict, Any
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.messages import BaseMessage
from agent_mcp.utils.prompts import RESPONSE_SYNTHESIS_PROMPT
from agent_mcp.utils.llm_utils import create_default_llm
from agent_mcp.utils.logger import setup_logger
import json


class ResponseSynthesizer:
    """结果整合 Agent
    
    负责整合所有执行结果，生成最终的用户响应。
    """
    
    def __init__(self, llm: ChatOpenAI = None):
        """初始化结果整合器
        
        Args:
            llm: LangChain LLM 实例，如果为 None 则使用默认配置
        """
        self.llm = create_default_llm(llm)
        self.logger = setup_logger(__name__)
        
        # 构建 Prompt 模板
        self.prompt_template = ChatPromptTemplate.from_template(
            RESPONSE_SYNTHESIS_PROMPT
        )
    
    def synthesize(self, user_input: str, execution_results: Dict[str, Any]) -> str:
        """整合执行结果，生成最终响应
        
        Args:
            user_input: 原始用户输入
            execution_results: 执行结果字典，键为任务ID，值为执行结果
        
        Returns:
            str: 最终的用户响应
        """
        self.logger.info(f"开始整合 {len(execution_results)} 个执行结果...")
        
        # 格式化执行结果
        serialized_results = self._convert_to_serializable(execution_results)
        results_text = json.dumps(serialized_results, ensure_ascii=False, indent=2)
        self.logger.debug(f"执行结果: {results_text}")
        
        # 构建完整的 Prompt
        prompt = self.prompt_template.format_messages(
            user_input=user_input,
            execution_results=results_text
        )
        
        # 调用 LLM
        self.logger.debug("调用 LLM 生成最终响应...")
        response = self.llm.invoke(prompt)
        
        self.logger.info("最终响应生成完成")
        return response.content

    def _convert_to_serializable(self, data: Any) -> Any:
        """将执行结果转换为可 JSON 序列化的结构"""
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
