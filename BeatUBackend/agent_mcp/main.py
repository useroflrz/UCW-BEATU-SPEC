"""AgentMCP 主入口文件"""

import asyncio
import os
import sys
import logging
from pathlib import Path
from dotenv import load_dotenv

# 添加项目根目录到 Python 路径
project_root = Path(__file__).parent.parent
if str(project_root) not in sys.path:
    sys.path.insert(0, str(project_root))

from agent_mcp.core.orchestrator import AgentOrchestrator


# 加载环境变量
load_dotenv()

# 设置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


async def main():
    """主函数"""
    # 初始化编排器
    orchestrator = AgentOrchestrator()
    
    try:
        # 示例：处理用户请求
        user_input = "帮我规划一下今天的一日三餐吃啥，一个人吃。"
        
        print(f"用户输入: {user_input}")
        print("\n处理中...\n")
        
        # 处理请求
        response = await orchestrator.process_user_request_async(user_input)
        
        print("=" * 50)
        print("最终响应")
        print("=" * 50)
        print(response)
        
    except Exception as e:
        print(f"错误: {e}")
        import traceback
        traceback.print_exc()
    finally:
        # 关闭资源
        await orchestrator.close()


if __name__ == "__main__":
    asyncio.run(main())
