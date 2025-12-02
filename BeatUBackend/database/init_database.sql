-- ============================================
-- BeatU 数据库初始化脚本
-- 数据库：jeecg-boot3
-- 服务器：192.168.1.206:3306
-- 用户：jeecg
-- 密码：haomo123
-- ============================================

-- 注意：数据库 jeecg-boot3 应该已经存在，这里不需要创建
-- 如果数据库不存在，请先创建：
-- CREATE DATABASE IF NOT EXISTS `jeecg-boot3` 
--     CHARACTER SET utf8mb4 
--     COLLATE utf8mb4_unicode_ci;

USE `jeecg-boot3`;

-- ============================================
-- 1. 删除现有表（如果存在）
-- ============================================
DROP TABLE IF EXISTS beatu_watch_history;
DROP TABLE IF EXISTS beatu_user_follows;
DROP TABLE IF EXISTS beatu_metrics_interaction;
DROP TABLE IF EXISTS beatu_metrics_playback;
DROP TABLE IF EXISTS beatu_interactions;
DROP TABLE IF EXISTS beatu_comments;
DROP TABLE IF EXISTS beatu_videos;
DROP TABLE IF EXISTS beatu_users;

-- 用户信息表
CREATE TABLE beatu_users (
    id VARCHAR(64) PRIMARY KEY COMMENT '用户唯一ID',
    nickname VARCHAR(100) NOT NULL COMMENT '昵称',
    avatar VARCHAR(500) DEFAULT NULL COMMENT '头像地址',
    bio VARCHAR(500) DEFAULT NULL COMMENT '个人简介',
    followers BIGINT NOT NULL DEFAULT 0 COMMENT '粉丝数',
    followings BIGINT NOT NULL DEFAULT 0 COMMENT '关注数',
    likes_received BIGINT NOT NULL DEFAULT 0 COMMENT '累计获赞',
    works_count BIGINT NOT NULL DEFAULT 0 COMMENT '作品数量',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_nickname (nickname)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户信息表';

-- 视频信息表
CREATE TABLE beatu_videos (
    id VARCHAR(64) PRIMARY KEY COMMENT '视频唯一ID',
    play_url VARCHAR(500) NOT NULL COMMENT '视频播放地址',
    cover_url VARCHAR(500) NOT NULL COMMENT '封面地址',
    title VARCHAR(200) NOT NULL COMMENT '视频标题',
    tags JSON DEFAULT NULL COMMENT '标签 JSON 数组',
    duration_ms BIGINT NOT NULL DEFAULT 0 COMMENT '视频时长（毫秒）',
    orientation ENUM('PORTRAIT', 'LANDSCAPE') NOT NULL DEFAULT 'PORTRAIT' COMMENT '视频方向',
    author_id VARCHAR(64) NOT NULL COMMENT '作者ID',
    author_name VARCHAR(100) NOT NULL COMMENT '作者名称',
    author_avatar VARCHAR(500) DEFAULT NULL COMMENT '作者头像',
    like_count BIGINT NOT NULL DEFAULT 0 COMMENT '点赞数',
    comment_count BIGINT NOT NULL DEFAULT 0 COMMENT '评论数',
    favorite_count BIGINT NOT NULL DEFAULT 0 COMMENT '收藏数',
    share_count BIGINT NOT NULL DEFAULT 0 COMMENT '分享数',
    view_count BIGINT NOT NULL DEFAULT 0 COMMENT '播放数',
    qualities JSON DEFAULT NULL COMMENT '可选清晰度信息',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_orientation_created (orientation, created_at DESC),
    INDEX idx_author_id (author_id),
    INDEX idx_duration (duration_ms),
    FOREIGN KEY (author_id) REFERENCES beatu_users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频信息表';

-- 评论表
CREATE TABLE beatu_comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    video_id VARCHAR(64) NOT NULL COMMENT '视频ID',
    author_id VARCHAR(64) NOT NULL COMMENT '作者ID（评论用户）',
    author_name VARCHAR(100) NOT NULL COMMENT '作者昵称',
    author_avatar VARCHAR(500) DEFAULT NULL COMMENT '作者头像',
    content TEXT NOT NULL COMMENT '评论内容',
    parent_id BIGINT DEFAULT NULL COMMENT '父评论ID（用于回复）',
    is_ai_reply BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否为AI回复',
    ai_model VARCHAR(64) DEFAULT NULL COMMENT 'AI模型',
    ai_source VARCHAR(32) DEFAULT NULL COMMENT 'AI来源',
    ai_confidence DOUBLE DEFAULT NULL COMMENT 'AI置信度',
    like_count BIGINT NOT NULL DEFAULT 0 COMMENT '评论点赞数',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    INDEX idx_video_created (video_id, created_at DESC),
    INDEX idx_parent_id (parent_id),
    FOREIGN KEY (video_id) REFERENCES beatu_videos(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_id) REFERENCES beatu_comments(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

-- 用户互动表
CREATE TABLE beatu_interactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    video_id VARCHAR(64) DEFAULT NULL COMMENT '视频ID（点赞/收藏场景）',
    author_id VARCHAR(64) DEFAULT NULL COMMENT '作者ID（关注场景）',
    type ENUM('LIKE', 'FAVORITE', 'FOLLOW_AUTHOR') NOT NULL COMMENT '互动类型',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    UNIQUE KEY uk_video_user_type (user_id, video_id, type),
    UNIQUE KEY uk_author_user_type (user_id, author_id, type),
    INDEX idx_user_video (user_id, video_id),
    FOREIGN KEY (video_id) REFERENCES beatu_videos(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES beatu_users(id) ON DELETE CASCADE,
    FOREIGN KEY (author_id) REFERENCES beatu_users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户互动表';

-- 用户关注关系表
CREATE TABLE beatu_user_follows (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    follower_id VARCHAR(64) NOT NULL COMMENT '粉丝ID',
    followee_id VARCHAR(64) NOT NULL COMMENT '被关注用户ID',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
    
    UNIQUE KEY uk_follow_pair (follower_id, followee_id),
    INDEX idx_followee (followee_id, created_at DESC),
    INDEX idx_follower (follower_id, created_at DESC),
    FOREIGN KEY (follower_id) REFERENCES beatu_users(id) ON DELETE CASCADE,
    FOREIGN KEY (followee_id) REFERENCES beatu_users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户关注关系表';

-- 用户观看历史聚合表
CREATE TABLE beatu_watch_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    video_id VARCHAR(64) NOT NULL COMMENT '视频ID',
    last_watch_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近观看时间',
    watch_count INT NOT NULL DEFAULT 1 COMMENT '累计观看次数',
    last_seek_ms BIGINT NOT NULL DEFAULT 0 COMMENT '最近一次的播放进度',
    last_duration_ms BIGINT NOT NULL DEFAULT 0 COMMENT '最近一次观看时长',
    total_duration_ms BIGINT NOT NULL DEFAULT 0 COMMENT '累计观看时长',
    completion_rate DOUBLE NOT NULL DEFAULT 0 COMMENT '最近一次播放完成度',
    
    UNIQUE KEY uk_user_video_history (user_id, video_id),
    INDEX idx_history_user (user_id, last_watch_at DESC),
    INDEX idx_history_video (video_id, last_watch_at DESC),
    FOREIGN KEY (user_id) REFERENCES beatu_users(id) ON DELETE CASCADE,
    FOREIGN KEY (video_id) REFERENCES beatu_videos(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户观看历史聚合表';

-- 播放指标表
CREATE TABLE beatu_metrics_playback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    video_id VARCHAR(64) NOT NULL,
    fps DOUBLE,
    start_up_ms BIGINT,
    rebuffer_count INT,
    memory_mb DOUBLE,
    channel VARCHAR(32),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_metric_video (video_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='播放指标';

-- 互动指标表
CREATE TABLE beatu_metrics_interaction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event VARCHAR(64) NOT NULL,
    video_id VARCHAR(64) DEFAULT NULL,
    latency_ms BIGINT,
    success BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_metric_event (event, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='互动指标';

-- ============================================
-- 3. 插入示例数据
-- ============================================

-- 插入用户数据
INSERT INTO beatu_users (
    id, nickname, avatar, bio,
    followers, followings, likes_received, works_count
) VALUES
('user_001', '云哥讲电影', 'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/avatars/user_001.jpg', '专注分享电影幕后故事', 1250000, 320, 3580000, 420),
('user_002', '美食博主小美', 'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/avatars/user_002.jpg', '热爱厨房与旅行的美食记录者', 980000, 210, 2100000, 180),
('user_003', '旅行摄影师', 'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/avatars/user_003.jpg', '记录世界的每一次日落', 560000, 120, 860000, 95),
('user_004', '游戏解说员', 'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/avatars/user_004.jpg', '硬核玩法讲解', 450000, 160, 1200000, 150),
('user_005', '电影爱好者', 'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/avatars/user_005.jpg', '每天一部好电影推荐', 300000, 200, 560000, 110),
('user_006', '健身教练', 'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/avatars/user_006.jpg', '科学健身，拒绝瞎练', 750000, 180, 980000, 205),
('user_007', '竖屏视频1', 'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/avatars/user_007.jpg', '生活方式记录者', 380000, 90, 420000, 88),
('user_008', '评论用户1', NULL, '热爱互动的评论家', 13000, 58, 2800, 12),
('user_009', '评论用户2', NULL, '喜欢分享观点', 9600, 32, 1500, 7),
('user_010', '评论用户3', NULL, '影评爱好者', 7800, 25, 1180, 5),
('user_011', '评论用户4', NULL, '旅行中', 5200, 18, 830, 3),
('demo-user', 'BeatU 用户', 'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/avatars/demo_user.jpg', '客户端默认登录用户', 36, 58, 1200, 8),
('ai_beatu', 'BeatU AI 助手', 'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/avatars/ai_beatu.jpg', '智能互动助手', 0, 0, 0, 0);

-- 插入视频数据（根据 MockVideoCatalog.kt 中的 mock 数据）
INSERT INTO beatu_videos (
    id, play_url, cover_url, title, tags, duration_ms, orientation,
    author_id, author_name, author_avatar,
    like_count, comment_count, favorite_count, share_count, view_count, qualities
) VALUES
-- video_0011 - 横屏
('video_0011',
 'http://vjs.zencdn.net/v/oceans.mp4',
 'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/covers/video_0011.jpg',
 '测试视频1',
 JSON_ARRAY('test', 'demo'),
 45000,
 'LANDSCAPE',
 'user_001',
 '云哥讲电影 视频1',
 'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/avatars/user_001.jpg',
 535, 43, 159, 59, 5000,
 NULL),

-- video_0012 - 横屏
('video_0012',
 'https://media.w3.org/2010/05/sintel/trailer.mp4',
 'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/covers/video_0012.jpg',
 'Sintel 高清预告片 - 奇幻冒险',
 JSON_ARRAY('movie', 'trailer', 'fantasy'),
 60000,
 'LANDSCAPE',
 'user_003',
 '视频3',
 'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/avatars/user_003.jpg',
 890, 67, 345, 123, 8000,
 NULL),

-- video_002 - 横屏
('video_002',
 'http://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/%E6%A8%AA%E5%B1%8F%E8%A7%86%E9%A2%911.mp4',
 'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/covers/video_002.jpg',
 '横屏视频1',
 JSON_ARRAY('landscape', 'demo'),
 90000,
 'LANDSCAPE',
 'user_002',
 '视频2',
 'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/avatars/user_002.jpg',
 1234, 89, 567, 234, 12000,
 NULL),

-- video_003 - 横屏
('video_003',
 'http://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/%E6%A8%AA%E5%B1%8F%E8%A7%86%E9%A2%912.mp4',
 'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/covers/video_003.jpg',
 '横屏视频2',
 JSON_ARRAY('landscape', 'demo'),
 60000,
 'LANDSCAPE',
 'user_003',
 '视频3',
 'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/avatars/user_003.jpg',
 1234, 89, 567, 234, 10000,
 NULL),

-- video_004 - 横屏
('video_004',
 'http://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/%E6%A8%AA%E5%B1%8F%E8%A7%86%E9%A2%913.mp4',
 'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/covers/video_004.jpg',
 '横屏视频3',
 JSON_ARRAY('landscape', 'demo'),
 90000,
 'LANDSCAPE',
 'user_004',
 '视频4',
 'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/avatars/user_004.jpg',
 1234, 89, 567, 234, 11000,
 NULL),

-- video_005 - 横屏（第一个）
('video_005',
 'http://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/%E6%A8%AA%E5%B1%8F%E8%A7%86%E9%A2%914.mp4',
 'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/covers/video_005.jpg',
 '横屏视频4',
 JSON_ARRAY('landscape', 'demo'),
 180000,
 'LANDSCAPE',
 'user_005',
 '视频5',
 'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/avatars/user_005.jpg',
 1234, 89, 567, 234, 15000,
 NULL),

-- video_006 - 横屏
('video_006',
 'http://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/%E6%A8%AA%E5%B1%8F%E8%A7%86%E9%A2%915.mp4',
 'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/covers/video_006.jpg',
 '横屏视频5',
 JSON_ARRAY('landscape', 'demo'),
 30000,
 'LANDSCAPE',
 'user_006',
 '视频6',
 'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/avatars/user_006.jpg',
 1234, 89, 567, 234, 9000,
 NULL),

-- video_007 - 竖屏
('video_007',
 'http://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/%E7%AB%96%E5%B1%8F%E8%A7%86%E9%A2%911.mp4',
 'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/covers/video_007.jpg',
 '竖屏视频1',
 JSON_ARRAY('portrait', 'demo'),
 30000,
 'PORTRAIT',
 'user_007',
 '竖屏视频1',
 'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/avatars/user_007.jpg',
 2345, 156, 789, 456, 20000,
 NULL),

-- video_008 - 竖屏
('video_008',
 'http://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/%E7%AB%96%E5%B1%8F%E7%AB%96%E5%B1%8F2.mp4',
 'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/covers/video_008.jpg',
 '竖屏视频2',
 JSON_ARRAY('portrait', 'demo'),
 60000,
 'PORTRAIT',
 'user_008',
 '竖屏视频2',
 'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/avatars/user_008.jpg',
 2345, 156, 789, 456, 18000,
 NULL),

-- video_009 - 竖屏
('video_009',
 'http://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/%E7%AB%96%E5%B1%8F%E8%A7%86%E9%A2%913.mp4',
 'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/covers/video_009.jpg',
 '竖屏视频3',
 JSON_ARRAY('portrait', 'demo'),
 30000,
 'PORTRAIT',
 'user_009',
 '竖屏视频3',
 'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/avatars/user_009.jpg',
 2345, 156, 789, 456, 17000,
 NULL),

-- video_010 - 竖屏
('video_010',
 'http://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/%E7%AB%96%E5%B1%8F%E8%A7%86%E9%A2%914.mp4',
 'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/covers/video_010.jpg',
 '竖屏视频4',
 JSON_ARRAY('portrait', 'demo'),
 60000,
 'PORTRAIT',
 'user_010',
 '竖屏视频4',
 'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/avatars/user_010.jpg',
 2345, 156, 789, 456, 16000,
 NULL),

-- video_011 - 竖屏
('video_011',
 'http://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/%E7%AB%96%E5%B1%8F%E7%AB%96%E5%B1%8F5.mp4',
 'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/covers/video_011.jpg',
 '竖屏视频5',
 JSON_ARRAY('portrait', 'demo'),
 30000,
 'PORTRAIT',
 'user_011',
 '竖屏视频5',
 'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/avatars/user_011.jpg',
 2345, 156, 789, 456, 15000,
 NULL);

-- 插入评论数据
INSERT INTO beatu_comments (
    video_id, author_id, author_name, author_avatar, content, 
    parent_id, is_ai_reply, like_count, created_at
) VALUES
-- video_0011的评论
('video_0011', 'user_008', '评论用户1', NULL, '太棒了！这个视频太燃了！🔥', NULL, FALSE, 45, NOW() - INTERVAL 2 DAY),
('video_0011', 'user_009', '评论用户2', NULL, '视频内容很棒，爱了爱了', NULL, FALSE, 32, NOW() - INTERVAL 1 DAY),
('video_0011', 'ai_beatu', '@元宝', NULL, '这个视频展现了很好的表现力，内容选择也很棒！', NULL, TRUE, 89, NOW() - INTERVAL 12 HOUR),

-- video_0012的评论
('video_0012', 'user_010', '评论用户3', NULL, '这个预告片太精彩了！', NULL, FALSE, 78, NOW() - INTERVAL 3 DAY),
('video_0012', 'user_011', '评论用户4', NULL, '已经收藏了，期待正片', NULL, FALSE, 56, NOW() - INTERVAL 2 DAY),
('video_0012', 'ai_beatu', '@元宝', NULL, '这个预告片制作精良，画面震撼，值得推荐。', NULL, TRUE, 120, NOW() - INTERVAL 1 DAY),

-- video_002的评论
('video_002', 'user_012', '评论用户5', NULL, '横屏视频效果不错！', NULL, FALSE, 234, NOW() - INTERVAL 5 DAY),
('video_002', 'user_013', '评论用户6', NULL, '画质很清晰', NULL, FALSE, 89, NOW() - INTERVAL 4 DAY),
('video_002', 'ai_beatu', '@元宝', NULL, '这个横屏视频画面质量很好，内容也很精彩。', NULL, TRUE, 456, NOW() - INTERVAL 3 DAY),

-- video_003的评论
('video_003', 'user_014', '游戏玩家1', NULL, '这个视频不错！', NULL, FALSE, 123, NOW() - INTERVAL 1 DAY),
('video_003', 'user_015', '游戏玩家2', NULL, '我也想要这样的效果', NULL, FALSE, 67, NOW() - INTERVAL 12 HOUR),

-- video_004的评论
('video_004', 'user_016', '电影迷1', NULL, '经典！', NULL, FALSE, 345, NOW() - INTERVAL 6 DAY),
('video_004', 'user_017', '电影迷2', NULL, '这个视频很精彩', NULL, FALSE, 234, NOW() - INTERVAL 5 DAY),

-- video_005的评论
('video_005', 'user_018', '用户1', NULL, '横屏视频4很不错！', NULL, FALSE, 200, NOW() - INTERVAL 3 DAY),
('video_005', 'user_019', '用户2', NULL, '内容很棒', NULL, FALSE, 150, NOW() - INTERVAL 2 DAY),

-- video_007的评论
('video_007', 'user_020', '用户3', NULL, '竖屏视频1很棒！', NULL, FALSE, 180, NOW() - INTERVAL 2 DAY),
('video_007', 'user_021', '用户4', NULL, '画质清晰，内容精彩', NULL, FALSE, 120, NOW() - INTERVAL 1 DAY),
('video_007', 'ai_beatu', '@元宝', NULL, '这个竖屏视频制作精良，内容很有吸引力。', NULL, TRUE, 300, NOW() - INTERVAL 12 HOUR);

-- 插入互动数据（点赞、收藏、关注）
INSERT INTO beatu_interactions (
    user_id, video_id, author_id, type, created_at
) VALUES
-- 点赞数据
('demo-user', 'video_0011', NULL, 'LIKE', NOW() - INTERVAL 1 DAY),
('demo-user', 'video_003', NULL, 'LIKE', NOW() - INTERVAL 2 DAY),
('demo-user', 'video_005', NULL, 'LIKE', NOW() - INTERVAL 3 DAY),
('demo-user', 'video_007', NULL, 'LIKE', NOW() - INTERVAL 1 DAY),

-- 收藏数据
('demo-user', 'video_0012', NULL, 'FAVORITE', NOW() - INTERVAL 1 DAY),
('demo-user', 'video_002', NULL, 'FAVORITE', NOW() - INTERVAL 2 DAY),
('demo-user', 'video_007', NULL, 'FAVORITE', NOW() - INTERVAL 1 DAY),
('demo-user', 'video_009', NULL, 'FAVORITE', NOW() - INTERVAL 2 DAY),

-- 关注数据
('demo-user', NULL, 'user_001', 'FOLLOW_AUTHOR', NOW() - INTERVAL 5 DAY),
('demo-user', NULL, 'user_002', 'FOLLOW_AUTHOR', NOW() - INTERVAL 3 DAY),
('demo-user', NULL, 'user_003', 'FOLLOW_AUTHOR', NOW() - INTERVAL 2 DAY);

-- 插入关注关系数据
INSERT INTO beatu_user_follows (
    follower_id, followee_id, created_at
) VALUES
('demo-user', 'user_001', NOW() - INTERVAL 5 DAY),
('demo-user', 'user_002', NOW() - INTERVAL 3 DAY),
('demo-user', 'user_003', NOW() - INTERVAL 2 DAY),
('user_002', 'user_001', NOW() - INTERVAL 10 DAY),
('user_003', 'user_002', NOW() - INTERVAL 8 DAY);

-- 插入观看历史聚合数据
INSERT INTO beatu_watch_history (
    user_id, video_id, last_watch_at, watch_count, last_seek_ms,
    last_duration_ms, total_duration_ms, completion_rate
) VALUES
('demo-user', 'video_0011', NOW() - INTERVAL 2 DAY, 3, 12000, 42000, 126000, 0.84),
('demo-user', 'video_0012', NOW() - INTERVAL 1 DAY, 2, 8000, 30000, 60000, 0.65),
('demo-user', 'video_007', NOW() - INTERVAL 3 DAY, 4, 0, 60000, 180000, 1.00),
('user_002', 'video_002', NOW() - INTERVAL 4 DAY, 1, 5000, 45000, 45000, 0.50);

-- 插入播放指标数据（示例）
INSERT INTO beatu_metrics_playback (
    video_id, fps, start_up_ms, rebuffer_count, memory_mb, channel, created_at
) VALUES
('video_0011', 30.0, 1200, 0, 256.5, 'recommend', NOW() - INTERVAL 1 DAY),
('video_0012', 30.0, 1500, 1, 320.8, 'recommend', NOW() - INTERVAL 2 DAY),
('video_002', 60.0, 800, 0, 512.3, 'follow', NOW() - INTERVAL 1 DAY),
('video_003', 30.0, 2000, 2, 456.7, 'recommend', NOW() - INTERVAL 3 DAY),
('video_007', 30.0, 1800, 0, 380.2, 'recommend', NOW() - INTERVAL 1 DAY);

-- ============================================
-- 4. 验证数据
-- ============================================
SELECT '数据库初始化完成！' AS message;
SELECT COUNT(*) AS video_count FROM beatu_videos;
SELECT COUNT(*) AS comment_count FROM beatu_comments;
SELECT COUNT(*) AS interaction_count FROM beatu_interactions;

