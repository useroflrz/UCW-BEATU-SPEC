"""AI 搜索服务（简化为名词解释和问题应答）"""

from __future__ import annotations

import json
from typing import AsyncGenerator, Optional
from langchain_openai import ChatOpenAI
from langchain_core.messages import HumanMessage
from agent_mcp.utils.llm_utils import create_default_llm
import logging

logger = logging.getLogger(__name__)


class AISearchService:
    """AI 搜索服务
    
    简化为直接使用 LLM 进行名词解释和问题应答，不依赖 MCP Orchestrator。
    """
    
    def __init__(self, db: Optional[None] = None):
        """初始化 AI 搜索服务
        
        Args:
            db: 数据库会话（已废弃，保留以兼容接口）
        """
        # ✅ 确保从 .env 文件加载配置
        from dotenv import load_dotenv
        from pathlib import Path
        import os
        env_file = Path(__file__).parent.parent / ".env"
        if env_file.exists():
            load_dotenv(dotenv_path=str(env_file), override=True)
            
            # ✅ 如果 .env 文件中有 LLM_* 配置，设置对应的环境变量
            # 直接从文件读取，因为 load_dotenv 可能没有正确加载
            try:
                with open(env_file, 'r', encoding='utf-8') as f:
                    for line in f:
                        line = line.strip()
                        if line.startswith('LLM_API_KEY=') and not line.startswith('#'):
                            api_key = line.split('=', 1)[1].strip().strip('"').strip("'")
                            if api_key:
                                os.environ["LLM_API_KEY"] = api_key
                                os.environ["DASHSCOPE_API_KEY"] = api_key  # 兼容
                                os.environ["OPENAI_API_KEY"] = api_key  # 兼容
                        elif line.startswith('LLM_BASE_URL=') and not line.startswith('#'):
                            base_url = line.split('=', 1)[1].strip().strip('"').strip("'")
                            if base_url:
                                os.environ["BASE_URL"] = base_url
                        elif line.startswith('LLM_MODEL=') and not line.startswith('#'):
                            model = line.split('=', 1)[1].strip().strip('"').strip("'")
                            if model:
                                os.environ["MODEL"] = model
                        # 向后兼容：如果没有 LLM_API_KEY，使用 MCP_API_KEY
                        elif line.startswith('MCP_API_KEY=') and not line.startswith('#') and not os.getenv("LLM_API_KEY"):
                            api_key = line.split('=', 1)[1].strip().strip('"').strip("'")
                            if api_key:
                                os.environ["LLM_API_KEY"] = api_key
                                os.environ["DASHSCOPE_API_KEY"] = api_key
                                os.environ["OPENAI_API_KEY"] = api_key
            except Exception as e:
                self.logger.warning(f"读取 .env 文件失败: {e}")
        
        # ✅ 简化：直接使用 LLM，不需要 MCP Orchestrator
        self.llm = create_default_llm()
        self.logger = logger
    
    async def search_stream(
        self,
        user_query: str,
        db: Optional[None] = None
    ) -> AsyncGenerator[str, None]:
        """
        流式问答处理（简化版）
        
        直接使用 LLM 进行名词解释或问题应答，流式返回结果。
        
        Args:
            user_query: 用户查询文本
            db: 数据库会话（已废弃，保留以兼容接口）
        
        Yields:
            str: SSE 格式的数据块
        """
        try:
            # ✅ 简化：直接使用 LLM 进行问答
            prompt = f"""你是一个智能助手，擅长进行名词解释和问题回答。

用户询问：{user_query}

请用简洁、准确的语言直接回答用户的问题，要求：
1. 如果是名词，请提供清晰的定义和解释
2. 如果是问题，请直接回答，不要使用"根据"、"我认为"、"AI回答"等开头
3. 回答要控制在 200 字以内
4. 使用友好、自然的语言
5. **重要**：直接输出回答内容，不要包含任何前缀，如"回答："、"解释："、"AI回答："、"AI："等
6. **重要**：从第一句话开始就是回答内容，不要有任何提示词或标签"""
            
            messages = [HumanMessage(content=prompt)]
            
            # 流式返回 LLM 回答
            accumulated_content = ""
            chunk_count = 0
            has_content = False
            
            self.logger.info(f"开始流式生成回答: user_query={user_query}")
            
            async for chunk in self.llm.astream(messages):
                # ✅ 修复：处理 LangChain ChatOpenAI 的不同 chunk 格式
                content = ""
                
                # 方式1：AIMessageChunk 对象，content 属性
                if hasattr(chunk, 'content'):
                    content = str(chunk.content) if chunk.content else ""
                # 方式2：直接是字符串
                elif isinstance(chunk, str):
                    content = chunk
                # 方式3：有 text 属性
                elif hasattr(chunk, 'text'):
                    content = str(chunk.text) if chunk.text else ""
                # 方式4：AIMessageChunk，通过 delta 属性
                elif hasattr(chunk, 'delta'):
                    if hasattr(chunk.delta, 'content'):
                        content = str(chunk.delta.content) if chunk.delta.content else ""
                    elif isinstance(chunk.delta, str):
                        content = chunk.delta
                # 方式5：尝试转换为字符串
                else:
                    try:
                        content = str(chunk)
                    except:
                        self.logger.warning(f"无法提取 chunk 内容: {type(chunk)}")
                
                if content and content.strip():
                    has_content = True
                    accumulated_content += content
                    chunk_count += 1
                    # 流式返回每个 chunk
                    chunk_data = {
                        "chunkType": "answer",
                        "content": content,
                        "isFinal": False
                    }
                    yield f"data: {json.dumps(chunk_data, ensure_ascii=False)}\n\n"
                    self.logger.debug(f"发送 chunk #{chunk_count}: content length={len(content)}, preview={content[:20]}...")
            
            # 发送最后一个 chunk，标记为 final
            if has_content:
                self.logger.info(f"AI 回答完成: 总长度={len(accumulated_content)}, chunk 数量={chunk_count}")
                final_chunk = {
                    "chunkType": "answer",
                    "content": "",
                    "isFinal": True
                }
                yield f"data: {json.dumps(final_chunk, ensure_ascii=False)}\n\n"
            else:
                self.logger.warning(f"AI 回答为空，未收到任何内容。chunk 类型: {type(chunk) if 'chunk' in locals() else 'N/A'}")
                # 如果没有任何内容，返回一个错误提示
                error_chunk = {
                    "chunkType": "error",
                    "content": "AI 回答为空，请重试",
                    "isFinal": True
                }
                yield f"data: {json.dumps(error_chunk, ensure_ascii=False)}\n\n"
        
        except Exception as e:
            self.logger.error(f"AI 问答处理失败: {e}", exc_info=True)
            error_data = {
                "chunkType": "error",
                "content": f"处理失败: {str(e)}",
                "isFinal": True
            }
            yield f"data: {json.dumps(error_data, ensure_ascii=False)}\n\n"
    
    def close(self):
        """关闭服务资源"""
        # 简化版不需要关闭资源
        pass
