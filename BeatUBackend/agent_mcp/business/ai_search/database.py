"""数据库连接和查询模块"""

from typing import List, Optional
from sqlalchemy import create_engine, text, select, or_
from sqlalchemy.orm import Session, sessionmaker
from sqlalchemy.pool import QueuePool
import os
from agent_mcp.utils.logger import setup_logger

logger = setup_logger(__name__)


class DatabaseManager:
    """数据库管理器"""
    
    def __init__(
        self,
        local_db_path: Optional[str] = None,
        remote_db_url: Optional[str] = None
    ):
        """
        初始化数据库管理�?
        
        Args:
            local_db_path: 本地 SQLite 数据库路�?
            remote_db_url: 远程 MySQL 数据库连�?URL
        """
        self.local_db_path = local_db_path or os.getenv(
            "LOCAL_DB_PATH",
            "beatu.db"
        )
        self.remote_db_url = remote_db_url or os.getenv(
            "REMOTE_DB_URL",
            "mysql+pymysql://root:haomo123@192.168.1.206:3306/jeecg-boot3"
        )
        
        # 初始化本地数据库连接
        self.local_engine = None
        self.local_session = None
        if self.local_db_path:
            try:
                self.local_engine = create_engine(
                    f"sqlite:///{self.local_db_path}",
                    echo=False,
                    poolclass=QueuePool,
                    pool_pre_ping=True
                )
                self.local_session = sessionmaker(bind=self.local_engine)
                logger.info(f"本地数据库连接成�? {self.local_db_path}")
            except Exception as e:
                logger.error(f"本地数据库连接失�? {e}")
        
        # 初始化远程数据库连接
        self.remote_engine = None
        self.remote_session = None
        if self.remote_db_url:
            try:
                self.remote_engine = create_engine(
                    self.remote_db_url,
                    echo=False,
                    poolclass=QueuePool,
                    pool_pre_ping=True,
                    pool_size=5,
                    max_overflow=10
                )
                self.remote_session = sessionmaker(bind=self.remote_engine)
                logger.info("远程数据库连接成�?")
            except Exception as e:
                logger.error(f"远程数据库连接失�? {e}")
    
    def search_local_videos_by_keywords(
        self,
        keywords: List[str],
        limit: int = 10
    ) -> List[str]:
        """
        根据关键词在本地数据库搜索视�?
        
        Args:
            keywords: 关键词列�?
            limit: 返回结果数量限制
        
        Returns:
            视频 ID 列表
        """
        if not self.local_session or not keywords:
            return []
        
        try:
            with self.local_session() as session:
                # 构建查询条件：在 title �?tags 中搜索关键词
                # 使用参数化查询避�?SQL 注入
                conditions_list = []
                params = {}
                
                for i, keyword in enumerate(keywords):
                    keyword_param = f"keyword_{i}"
                    conditions_list.append(
                        f"(title LIKE :{keyword_param} OR tags LIKE :{keyword_param})"
                    )
                    params[keyword_param] = f"%{keyword}%"
                
                if not conditions_list:
                    return []
                
                query_str = f"""
                    SELECT id FROM beatu_videos 
                    WHERE {' OR '.join(conditions_list)}
                    LIMIT :limit
                """
                params["limit"] = limit
                
                result = session.execute(text(query_str), params)
                video_ids = [row[0] for row in result]
                logger.info(f"本地数据库搜索到 {len(video_ids)} 个视�?")
                return video_ids
        except Exception as e:
            logger.error(f"本地数据库搜索失�? {e}", exc_info=True)
            return []
    
    def search_remote_videos_by_keywords(
        self,
        keywords: List[str],
        limit: int = 10
    ) -> List[str]:
        """
        根据关键词在远程数据库搜索视�?
        
        Args:
            keywords: 关键词列�?
            limit: 返回结果数量限制
        
        Returns:
            视频 ID 列表
        """
        if not self.remote_session or not keywords:
            return []
        
        try:
            with self.remote_session() as session:
                # 构建查询条件：使用参数化查询避免 SQL 注入
                conditions_list = []
                params = {}
                
                for i, keyword in enumerate(keywords):
                    keyword_param = f"keyword_{i}"
                    conditions_list.append(
                        f"(title LIKE :{keyword_param} OR tags LIKE :{keyword_param})"
                    )
                    params[keyword_param] = f"%{keyword}%"
                
                if not conditions_list:
                    return []
                
                query_str = f"""
                    SELECT id FROM beatu_videos 
                    WHERE {' OR '.join(conditions_list)}
                    LIMIT :limit
                """
                params["limit"] = limit
                
                result = session.execute(text(query_str), params)
                video_ids = [row[0] for row in result]
                logger.info(f"远程数据库搜索到 {len(video_ids)} 个视�?")
                return video_ids
        except Exception as e:
            logger.error(f"远程数据库搜索失�? {e}", exc_info=True)
            return []
    
    def close(self):
        """关闭数据库连�?"""
        if self.local_engine:
            self.local_engine.dispose()
        if self.remote_engine:
            self.remote_engine.dispose()

