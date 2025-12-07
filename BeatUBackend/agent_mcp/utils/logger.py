"""日志配置"""

import logging
import sys
from typing import Optional


def setup_logger(
    name: str,
    level: Optional[int] = None,
    format_string: Optional[str] = None
) -> logging.Logger:
    """设置并返回日志记录器
    
    Args:
        name: 日志记录器名称
        level: 日志级别，如果为 None 则从环境变量 LOG_LEVEL 读取，默认为 INFO
        format_string: 日志格式字符串
    
    Returns:
        logging.Logger: 配置好的日志记录器"""
    logger = logging.getLogger(name)
    
    # 避免重复添加处理器
    if logger.handlers:
        return logger
    
    # 设置日志级别
    if level is None:
        import os
        level_str = os.getenv("LOG_LEVEL", "INFO").upper()
        level = logging._nameToLevel.get(level_str, logging.INFO)
    
    logger.setLevel(level)
    
    # 创建控制台处理器
    handler = logging.StreamHandler(sys.stdout)
    handler.setLevel(level)
    
    # 设置格式
    if format_string is None:
        format_string = '%(asctime)s - [%(name)s] - %(levelname)s - %(message)s'
    
    formatter = logging.Formatter(format_string, datefmt='%Y-%m-%d %H:%M:%S')
    handler.setFormatter(formatter)
    
    logger.addHandler(handler)
    
    return logger
