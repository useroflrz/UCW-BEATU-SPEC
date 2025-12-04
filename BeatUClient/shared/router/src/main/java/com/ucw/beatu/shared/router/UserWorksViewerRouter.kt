package com.ucw.beatu.shared.router

/**
 * 用户作品播放器路由接口
 * 用于解耦 videofeed 和 user 模块之间的循环依赖
 * 
 * user 模块实现此接口，videofeed 模块使用此接口
 */
interface UserWorksViewerRouter {
    /**
     * 切换到指定索引的视频
     * @param index 视频索引
     * @return 是否成功切换（如果当前不在 UserWorksViewerFragment 中，返回 false）
     */
    fun switchToVideo(index: Int): Boolean
    
    /**
     * 获取当前用户 ID
     * @return 当前用户 ID，如果不在 UserWorksViewerFragment 中，返回 null
     */
    fun getCurrentUserId(): String?
}

