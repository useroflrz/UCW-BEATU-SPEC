"""AI 搜索服务使用示例"""

import asyncio
from agent_mcp.business.ai_search import AISearchService, AISearchRequest


async def example_stream_search():
    """流式搜索示例"""
    service = AISearchService()
    
    request = AISearchRequest(user_query="我想看一些搞笑视�?")
    
    print("开始流式搜�?..")
    async for chunk in service.search_stream(request):
        print(f"[{chunk.chunk_type}] {chunk.content[:50]}... (final: {chunk.is_final})")
    
    service.close()


async def example_sync_search():
    """同步搜索示例"""
    service = AISearchService()
    
    request = AISearchRequest(user_query="我想看一些美食视�?")
    
    print("开始同步搜�?..")
    response = await service.search(request)
    
    print(f"\nAI 回答: {response.ai_answer}")
    print(f"关键�? {response.keywords}")
    print(f"本地视频 ID: {response.local_video_ids}")
    print(f"远程视频 ID: {response.video_ids}")
    
    service.close()


if __name__ == "__main__":
    # 运行流式搜索示例
    # asyncio.run(example_stream_search())
    
    # 运行同步搜索示例
    asyncio.run(example_sync_search())

