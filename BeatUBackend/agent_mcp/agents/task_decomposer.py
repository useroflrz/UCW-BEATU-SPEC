"""任务分解 Agent"""

from typing import List
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import PydanticOutputParser
from agent_mcp.models.task_schema import TaskDecompositionResult, SubTask
from agent_mcp.utils.prompts import TASK_DECOMPOSITION_PROMPT
from agent_mcp.utils.llm_utils import create_default_llm
from agent_mcp.utils.logger import setup_logger


class TaskDecomposer:
    """任务分解 Agent
    
    负责将用户输入分解为多个子任务。
    """
    
    def __init__(self, llm: ChatOpenAI = None):
        """初始化任务分解器
        
        Args:
            llm: LangChain LLM 实例，如果为 None 则使用默认配置
        """
        self.llm = create_default_llm(llm)
        self.parser = PydanticOutputParser(pydantic_object=TaskDecompositionResult)
        self.logger = setup_logger(__name__)
        
        # 构建 Prompt 模板
        self.prompt_template = ChatPromptTemplate.from_template(
            TASK_DECOMPOSITION_PROMPT
        )
    
    def decompose(self, user_input: str) -> TaskDecompositionResult:
        """分解用户输入为子任务
        
        Args:
            user_input: 用户输入的自然语言请求
        
        Returns:
            TaskDecompositionResult: 任务分解结果
        """
        self.logger.info(f"开始分解用户输入: {user_input}")
        
        # 构建完整的 Prompt
        prompt = self.prompt_template.format_messages(
            user_input=user_input
        )
        
        # 调用 LLM
        self.logger.debug("调用 LLM 进行任务分解...")
        response = self.llm.invoke(prompt)
        
        # 解析输出
        try:
            result = self.parser.parse(response.content)
            self.logger.info(f"任务分解完成，共分解出 {len(result.sub_tasks)} 个子任务")
            for i, sub_task in enumerate(result.sub_tasks):
                self.logger.debug(f"子任务 {i+1}: {sub_task.goal}")
            return result
        except Exception as e:
            # 如果解析失败，尝试手动解析或返回错误
            self.logger.error(f"解析任务分解结果失败: {e}", exc_info=True)
            raise ValueError(f"解析任务分解结果失败: {e}")
