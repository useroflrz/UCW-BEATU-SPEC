-- BeatU 内容服务数据库表结构（与 FastAPI 实现对齐）

CREATE TABLE IF NOT EXISTS videos (
    id VARCHAR(64) PRIMARY KEY COMMENT '视频唯一ID',
    play_url VARCHAR(500) NOT NULL COMMENT '视频播放地址',
    cover_url VARCHAR(500) NOT NULL COMMENT '封面地址',
    title VARCHAR(200) NOT NULL COMMENT '视频标题',
    tags JSON DEFAULT (JSON_ARRAY()) COMMENT '标签 JSON 数组',
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
    qualities JSON DEFAULT (JSON_ARRAY()) COMMENT '可选清晰度信息',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_orientation_created (orientation, created_at DESC),
    INDEX idx_author_id (author_id),
    INDEX idx_duration (duration_ms)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频信息表';

CREATE TABLE IF NOT EXISTS comments (
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
    FOREIGN KEY (video_id) REFERENCES videos(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_id) REFERENCES comments(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

CREATE TABLE IF NOT EXISTS interactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    video_id VARCHAR(64) DEFAULT NULL COMMENT '视频ID（点赞/收藏场景）',
    author_id VARCHAR(64) DEFAULT NULL COMMENT '作者ID（关注场景）',
    type ENUM('LIKE', 'FAVORITE', 'FOLLOW_AUTHOR') NOT NULL COMMENT '互动类型',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    UNIQUE KEY uk_video_user_type (video_id, user_id, type),
    UNIQUE KEY uk_author_user_type (author_id, user_id, type),
    INDEX idx_user_video (user_id, video_id),
    FOREIGN KEY (video_id) REFERENCES videos(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户互动表';

CREATE TABLE IF NOT EXISTS metrics_playback (
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

CREATE TABLE IF NOT EXISTS metrics_interaction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event VARCHAR(64) NOT NULL,
    video_id VARCHAR(64) DEFAULT NULL,
    latency_ms BIGINT,
    success BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_metric_event (event, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='互动指标';