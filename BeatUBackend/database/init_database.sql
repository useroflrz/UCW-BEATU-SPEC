-- ============================================
-- 1. 删除现有表（如果存在）
-- ============================================
DROP TABLE IF EXISTS beatu_watch_history;
DROP TABLE IF EXISTS beatu_user_follow;
DROP TABLE IF EXISTS beatu_video_interaction;
DROP TABLE IF EXISTS beatu_comment;
DROP TABLE IF EXISTS beatu_video;
DROP TABLE IF EXISTS beatu_user;
DROP TABLE IF EXISTS beatu_metrics_interaction;
DROP TABLE IF EXISTS beatu_metrics_playback;

-- ============================================
-- 2. 创建新表结构（按照设计文档）
-- ============================================

-- 表：beatu_user
CREATE TABLE beatu_user (
    userId VARCHAR(64) PRIMARY KEY COMMENT '用户 ID (PK)',
    userName VARCHAR(100) NOT NULL UNIQUE COMMENT '用户昵称（唯一）',
    avatarUrl VARCHAR(500) DEFAULT NULL COMMENT '头像 URL',
    followerCount BIGINT NOT NULL DEFAULT 0 COMMENT '粉丝数',
    followingCount BIGINT NOT NULL DEFAULT 0 COMMENT '关注数',
    bio VARCHAR(500) DEFAULT NULL COMMENT '简介',
    INDEX idx_userName (userName)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户信息表';

-- 表：beatu_video
CREATE TABLE beatu_video (
    videoId BIGINT PRIMARY KEY COMMENT '视频 ID (PK)',
    playUrl VARCHAR(500) NOT NULL COMMENT '播放地址',
    coverUrl VARCHAR(500) NOT NULL COMMENT '封面地址',
    title VARCHAR(200) NOT NULL COMMENT '视频标题',
    authorId VARCHAR(64) NOT NULL COMMENT '作者 ID',
    orientation ENUM('PORTRAIT', 'LANDSCAPE') NOT NULL DEFAULT 'PORTRAIT' COMMENT '横屏/竖屏',
    durationMs BIGINT NOT NULL DEFAULT 0 COMMENT '视频时长（毫秒）',
    likeCount BIGINT NOT NULL DEFAULT 0 COMMENT '点赞数',
    commentCount BIGINT NOT NULL DEFAULT 0 COMMENT '评论数',
    favoriteCount BIGINT NOT NULL DEFAULT 0 COMMENT '收藏数',
    viewCount BIGINT NOT NULL DEFAULT 0 COMMENT '观看次数',
    authorAvatar VARCHAR(500) DEFAULT NULL COMMENT '作者头像',
    shareUrl VARCHAR(500) DEFAULT NULL COMMENT '分享链接',
    INDEX idx_authorId (authorId),
    INDEX idx_orientation (orientation),
    FOREIGN KEY (authorId) REFERENCES beatu_user(userId) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频内容表';

-- 表：beatu_video_interaction
CREATE TABLE beatu_video_interaction (
    videoId BIGINT NOT NULL COMMENT '视频 ID (PK)',
    userId VARCHAR(64) NOT NULL COMMENT '用户 ID (PK)',
    isLiked TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否点赞 (0/1)',
    isFavorited TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否收藏 (0/1)',
    isPending TINYINT(1) NOT NULL DEFAULT 0 COMMENT '本地待同步状态 (0/1)',
    PRIMARY KEY (videoId, userId),
    INDEX idx_userId (userId),
    INDEX idx_videoId (videoId),
    INDEX idx_isPending (isPending),
    FOREIGN KEY (videoId) REFERENCES beatu_video(videoId) ON DELETE CASCADE,
    FOREIGN KEY (userId) REFERENCES beatu_user(userId) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-视频互动表';

-- 表：beatu_user_follow
CREATE TABLE beatu_user_follow (
    userId VARCHAR(64) NOT NULL COMMENT '当前用户 ID (PK)',
    authorId VARCHAR(64) NOT NULL COMMENT '被关注的作者 ID (PK)',
    isFollowed TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否关注 (0/1)',
    isPending TINYINT(1) NOT NULL DEFAULT 0 COMMENT '本地待同步状态 (0/1)',
    PRIMARY KEY (userId, authorId),
    INDEX idx_userId (userId),
    INDEX idx_authorId (authorId),
    INDEX idx_isPending (isPending),
    FOREIGN KEY (userId) REFERENCES beatu_user(userId) ON DELETE CASCADE,
    FOREIGN KEY (authorId) REFERENCES beatu_user(userId) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-用户关注表';

-- 表：beatu_watch_history
CREATE TABLE beatu_watch_history (
    videoId BIGINT NOT NULL COMMENT '视频 ID (PK)',
    userId VARCHAR(64) NOT NULL COMMENT '用户 ID (PK)',
    lastPlayPositionMs BIGINT NOT NULL DEFAULT 0 COMMENT '上次播放进度（用于"从上次播放继续"）',
    watchedAt BIGINT NOT NULL COMMENT '最后观看时间（排序用，Unix 时间戳毫秒）',
    isPending TINYINT(1) NOT NULL DEFAULT 0 COMMENT '本地待同步状态 (0/1)',
    PRIMARY KEY (videoId, userId),
    INDEX idx_userId (userId),
    INDEX idx_videoId (videoId),
    INDEX idx_userId_watchedAt (userId, watchedAt),
    INDEX idx_isPending (isPending),
    FOREIGN KEY (videoId) REFERENCES beatu_video(videoId) ON DELETE CASCADE,
    FOREIGN KEY (userId) REFERENCES beatu_user(userId) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='观看历史表';

-- 表：beatu_comment
CREATE TABLE beatu_comment (
    commentId VARCHAR(64) PRIMARY KEY COMMENT '评论 ID (PK)',
    videoId BIGINT NOT NULL COMMENT '所属视频 ID',
    authorId VARCHAR(64) NOT NULL COMMENT '评论作者',
    content TEXT NOT NULL COMMENT '评论内容',
    createdAt BIGINT NOT NULL COMMENT '评论时间（Unix 时间戳毫秒）',
    likeCount BIGINT NOT NULL DEFAULT 0 COMMENT '点赞数',
    isLiked TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否点赞 (0/1)',
    isPending TINYINT(1) NOT NULL DEFAULT 0 COMMENT '本地待同步状态 (0/1)',
    authorAvatar VARCHAR(500) DEFAULT NULL COMMENT '作者头像',
    INDEX idx_videoId (videoId),
    INDEX idx_authorId (authorId),
    INDEX idx_createdAt (createdAt),
    FOREIGN KEY (videoId) REFERENCES beatu_video(videoId) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论内容表';

-- 播放指标表
CREATE TABLE beatu_metrics_playback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    video_id BIGINT NOT NULL,
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
    video_id BIGINT DEFAULT NULL,
    latency_ms BIGINT,
    success BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_metric_event (event, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='互动指标';

-- ============================================
-- 3. 插入数据
-- ============================================

-- 用户数据（15个真实用户）
INSERT INTO beatu_user (userId, userName, avatarUrl, followerCount, followingCount, bio)
VALUES
('12536984', '黑璇BLACK', 'https://i2.hdslb.com/bfs/face/05cec9b52c9aeb4ce214c26a5486cbb8a06259c6.jpg', 0, 0, NULL),
('14804670', '无穷小亮的科普日常', 'http://i0.hdslb.com/bfs/face/6de12181ed59518fc7beff2046fb3218d50206cc.jpg', 0, 0, NULL),
('483879799', '猛男舞团IconX', 'https://i1.hdslb.com/bfs/face/48503596681ab931d7d08a34071d3c8bf8b284bb.webp', 0, 0, NULL),
('11831050', 'RedialC', 'https://i1.hdslb.com/bfs/face/21eb08b4bc9835218ba9f1c2ebd152060f864f34.jpg', 0, 0, NULL),
('523064213', '海外视频', 'https://i2.hdslb.com/bfs/face/ecb0696c509831173929547693babd135b5b8867.jpg', 0, 0, NULL),
('2407474', '液老板', 'https://i1.hdslb.com/bfs/face/67cf24304f29b4addda4938b8bb0ee98cdd8e12f.jpg', 0, 0, NULL),
('296462800', '与优树', 'https://i1.hdslb.com/bfs/face/a15c18d253ca4f368064da9b5caf12860aedd005.jpg', 0, 0, NULL),
('3546708495567018', 'jagaimotatop', 'https://i0.hdslb.com/bfs/face/6decfb735be6078999a0c330b1b56b324694e873.jpg', 0, 0, NULL),
('15377173', '烟季', 'https://i1.hdslb.com/bfs/face/3c657487f9a7993f50bcbafa82f64f99ff1229bd.jpg', 0, 0, NULL),
('3546610069932716', 'JN-yxn', 'https://i2.hdslb.com/bfs/face/49fee719f87cc10f56bd3d280b53bf92aa2b9f40.jpg', 0, 0, NULL),
('598581575', '汤小圆鸽鸽鸽', 'https://i2.hdslb.com/bfs/face/8dcf9575286a3a6ae8b124d4b38c082c208d8a8e.jpg', 0, 0, NULL),
('652239032', 'IGN中国', 'https://i2.hdslb.com/bfs/face/4c9095a6fc7d6ef7bf97ee1c65767f537763c60c.jpg', 0, 0, NULL),
('669334488', '环球音乐中国', 'https://i0.hdslb.com/bfs/face/11ad74a0cf807b9896656ef9987e1b833aa11f8a.jpg', 0, 0, NULL),
('92850496', '变形菌纲', 'https://i1.hdslb.com/bfs/face/01b1fc94d9f2654c291f0634cc024d6eed12ed6a.jpg', 0, 0, NULL),
('291894729', '火大花haha', 'https://i0.hdslb.com/bfs/face/8fa1c5a5d8e23ae1bec7ef07e132daa7f08e5f62.jpg', 0, 0, NULL);

-- 评论用户（用于评论数据）
INSERT INTO beatu_user (userId, userName, avatarUrl, followerCount, followingCount, bio)
VALUES
('512407709', '好名字都被猫取了喵', 'https://i2.hdslb.com/bfs/face/5aad76e98b8ada49d59d93ee1774a28b5d12dff4.jpg', 0, 0, NULL),
('649342997', '晓春想摆烂', 'https://i2.hdslb.com/bfs/face/4ee3680a1bb4571e94028015347077bf36978c41.jpg', 0, 0, NULL),
('3493076209240846', '雁司绛', 'https://i1.hdslb.com/bfs/face/8b9cd257c86ca5f216330ecb5571f57a6d52f2f3.jpg', 0, 0, NULL),
('454507', '魔幻组曲棱镜娜娜', 'https://i1.hdslb.com/bfs/face/1907f22d45ea501b4006c09455a8699911dd50fc.jpg', 0, 0, NULL),
('124668043', '杜烁', 'https://i1.hdslb.com/bfs/face/0d9da902fc7db5fee44d30eaa249e8e6a35939a8.jpg', 0, 0, NULL),
('44745161', '阿斯嘉德三公主', 'https://i2.hdslb.com/bfs/face/37001a8c716ba4904e68de9d84f42be23bb4bf68.jpg', 0, 0, NULL),
('284208940', '卡涅棘刺', 'https://i1.hdslb.com/bfs/face/886ded3c03bf32bae9def6027539d9b7a83eea25.jpg', 0, 0, NULL),
('394196223', '九月贰拾陆', 'http://i0.hdslb.com/bfs/face/0b2b12c9faa9fca9107472ec99903264c7f48e6e.jpg', 0, 0, NULL),
('429532543', '无影ya', 'https://i1.hdslb.com/bfs/face/481dd54620a70415a965ce03013e7f9e1c5d7b79.jpg', 0, 0, NULL),
('516003403', '诸葛黛玉倒拔紫金葫芦', 'https://i1.hdslb.com/bfs/face/f259fe15491eb86731a4bf0b5ceba14b69c3a09d.jpg', 0, 0, NULL),
('500332621', '理五晨曦', 'https://i1.hdslb.com/bfs/face/5e60ad2184bf18ea01965d4e8db98184e125ccac.jpg', 0, 0, NULL),
('12746344', 'SpeedyWing', 'https://i1.hdslb.com/bfs/face/84a53815c6098d0572f4eeff5eac8fc479738a32.jpg', 0, 0, NULL),
('3546705091889223', '我凶的嘞', 'https://i0.hdslb.com/bfs/face/de045acf59df5a1eaccd059782ef2824337a561e.jpg', 0, 0, NULL),
('3546750843358055', '放弃取名字了龠字号', 'https://i2.hdslb.com/bfs/face/d7fff48a5e921072f520070ad818735cf73e7577.jpg', 0, 0, NULL),
('501937905', '爱上了你没什么道裡', 'https://i0.hdslb.com/bfs/face/1a9ed8a15f65cc75674371ac7ca82ec4bdccda3e.jpg', 0, 0, NULL),
('1762948', '烁砾', 'https://i1.hdslb.com/bfs/face/5dc022ad00f5ef7c0d2633176beedfdac64c0b09.jpg', 0, 0, NULL),
('264513499', '刀鸣集', 'https://i1.hdslb.com/bfs/face/8e8db0582ff5d4a6e90c06b40c643764a82adda7.jpg', 0, 0, NULL),
('695488101', '绝非鳝类o', 'https://i0.hdslb.com/bfs/face/4bbcdc27f3b227322c87bb5cb30600c371828e73.jpg', 0, 0, NULL),
('84511479', '初音太萌', 'https://i0.hdslb.com/bfs/face/f22d4da7b46c34c572c72f0d7ede76d34004d5ea.jpg', 0, 0, NULL),
('3546745751472938', '相遇的寻', 'https://i2.hdslb.com/bfs/face/d19a97fdd3430fab5df61d7ada433ae3a38b8eef.jpg', 0, 0, NULL),
('11822935', '银色亮片', 'https://i1.hdslb.com/bfs/baselabs/b84a4cb7eb0dc883e67c4766603e6c5b1e58421e.png', 0, 0, NULL),
('96640975', '蛀牙奶昔丷', 'https://i1.hdslb.com/bfs/face/816dc5224a4f5f2a1b351f6e18e4cd247ea4093b.jpg', 0, 0, NULL),
('225444889', 'YourF4u1t', 'https://i1.hdslb.com/bfs/face/ba268a6647eac4a07570e9e1b976512986951782.jpg', 0, 0, NULL),
('377572568', '周末酱想睡觉', 'https://i2.hdslb.com/bfs/face/0a8e6fbd90335c5de2235731f9e9b9b617bdef11.jpg', 0, 0, NULL),
('501483', 'kurundam', 'https://i0.hdslb.com/bfs/face/3e9d09fccb015a00a3057f2b0f9f6ca096b31377.jpg', 0, 0, NULL),
('104243347', '繁霜星落', 'https://i1.hdslb.com/bfs/face/d196dd031dc8ceb6b328812aacdaf441d28656dc.jpg', 0, 0, NULL),
('148957839', '隔壁闲人', 'https://i0.hdslb.com/bfs/face/733d8422f24f91fda9a95b6097a693dda2de1d24.jpg', 0, 0, NULL),
('389685337', '浪味鲜虾片', 'https://i1.hdslb.com/bfs/face/b28a351b7deea124b98c24486a2bb3c4a2deac0b.jpg', 0, 0, NULL),
('379390855', '瓦尔登湖上的丫丫', 'https://i1.hdslb.com/bfs/face/1cc39f40dfe0ae89270e7c9b4e4063ee5e348dda.jpg', 0, 0, NULL),
('3494361033608001', '偉yuan', 'https://i0.hdslb.com/bfs/face/f9963184a9bb5e65d75a04d7fc7a3ffcb33c5a0e.jpg', 0, 0, NULL),
('519801151', '就是救世啊', 'https://i1.hdslb.com/bfs/face/9df3c067507afcc99ad540bd9fdc38bb766c39cb.webp', 0, 0, NULL),
('360606596', '东方仗仗助', 'https://i2.hdslb.com/bfs/face/4c324330f2e1f76b1d72d780d8908cf94d1a8a88.jpg', 0, 0, NULL),
('3546390502312149', 'bili_96838051560', 'https://i0.hdslb.com/bfs/face/member/noface.jpg', 0, 0, NULL),
('20301524', 'Mousquetaire', 'https://i2.hdslb.com/bfs/face/dfa3dc4b52039c86774687bf7b49428a8c175958.jpg', 0, 0, NULL),
('213820482', '王高徐', 'https://i0.hdslb.com/bfs/face/2b4cfc2b6671ff201d7e69f975f268e1db47655b.jpg', 0, 0, NULL),
('499471102', '追丶者', 'https://i1.hdslb.com/bfs/face/f089e0bae1801e859caae60f1667e04b7337a315.jpg', 0, 0, NULL),
('3461567819549658', '发鬓', 'https://i0.hdslb.com/bfs/face/fd8681f451ed967e615f3632f72b9525737bb266.jpg', 0, 0, NULL),
('3546771223481092', '巧合很奇妙', 'https://i1.hdslb.com/bfs/face/192983acc366c0e035bc3dbdce74120641e9cdcc.jpg', 0, 0, NULL),
('1326922374', '藏狐108', 'https://i0.hdslb.com/bfs/face/84739ad43064d322acdfe5389593a33f51087686.jpg', 0, 0, NULL),
('693576322', 'AAA灰珀晶片星琼批发', 'https://i1.hdslb.com/bfs/face/1b9d5c5f9f7ffe5be04241702610db6e063460ad.jpg', 0, 0, NULL),
('179319934', '双海包子铺老板娘', 'https://i1.hdslb.com/bfs/face/6e1da67e693a19cc9d468a21e2c8bd30fe05c9b7.jpg', 0, 0, NULL),
('1069344105', 'XU_Beft', 'https://i0.hdslb.com/bfs/face/b461e0beb28bdeda355a4919b3a1c7b24a6ec8db.jpg', 0, 0, NULL),
('BEATU', 'BEATU', 'http://i0.hdslb.com/bfs/face/0e8212a0b68957b400fbaae570f58c686eebc205.jpg', 0, 0, NULL);

-- 视频数据（修正版，确保所有videoId唯一）
INSERT INTO beatu_video (
  videoId, playUrl, coverUrl, title,
  authorId, orientation,
  durationMs, likeCount, commentCount, favoriteCount, viewCount,
  authorAvatar, shareUrl
)
VALUES
-- 注意：videoId需要转换为数值类型，这里用伪转换，实际应该用唯一数值ID
(100001,
 'https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4',
 'https://i2.hdslb.com/bfs/face/05cec9b52c9aeb4ce214c26a5486cbb8a06259c6.jpg',
 'Big Buck Bunny（示例视频）',
 '12536984', 'LANDSCAPE', 50000, 40756, 447, 11254, 251956,
 'https://i2.hdslb.com/bfs/face/05cec9b52c9aeb4ce214c26a5486cbb8a06259c6.jpg', NULL),

(100002,
 'https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4',
 'http://i0.hdslb.com/bfs/face/6de12181ed59518fc7beff2046fb3218d50206cc.jpg',
 'Elephants Dream（示例视频）',
 '14804670', 'PORTRAIT', 699000, 436349, 12962, 29993, 4997211,
 'http://i0.hdslb.com/bfs/face/6de12181ed59518fc7beff2046fb3218d50206cc.jpg', NULL),

(100003,
 'https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4',
 'https://i1.hdslb.com/bfs/face/48503596681ab931d7d08a34071d3c8bf8b284bb.webp',
 'For Bigger Blazes（示例视频）',
 '483879799', 'LANDSCAPE', 116000, 3204017, 47621, 1084135, 51075881,
 'https://i1.hdslb.com/bfs/face/48503596681ab931d7d08a34071d3c8bf8b284bb.webp', NULL),

(100004,
 'https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4',
 'https://i1.hdslb.com/bfs/face/21eb08b4bc9835218ba9f1c2ebd152060f864f34.jpg',
 'For Bigger Escapes（示例视频）',
 '11831050', 'PORTRAIT', 169000, 10255, 292, 7353, 71685,
 'https://i1.hdslb.com/bfs/face/21eb08b4bc9835218ba9f1c2ebd152060f864f34.jpg', NULL),

(100005,
 'https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4',
 'https://i2.hdslb.com/bfs/face/ecb0696c509831173929547693babd135b5b8867.jpg',
 'For Bigger Fun（示例视频）',
 '523064213', 'PORTRAIT', 89000, 3304, 31, 1160, 23699,
 'https://i2.hdslb.com/bfs/face/ecb0696c509831173929547693babd135b5b8867.jpg', NULL),

(100006,
 'https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4',
 'https://i1.hdslb.com/bfs/face/67cf24304f29b4addda4938b8bb0ee98cdd8e12f.jpg',
 'For Bigger Joyrides（示例视频）',
 '2407474', 'PORTRAIT', 42000, 40326, 959, 9433, 250551,
 'https://i1.hdslb.com/bfs/face/67cf24304f29b4addda4938b8bb0ee98cdd8e12f.jpg', NULL),

(100007,
 'https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4',
 'https://i1.hdslb.com/bfs/face/a15c18d253ca4f368064da9b5caf12860aedd005.jpg',
 'For Bigger Meltdowns（示例视频）',
 '296462800', 'PORTRAIT', 55000, 1249, 54, 317, 3503,
 'https://i1.hdslb.com/bfs/face/a15c18d253ca4f368064da9b5caf12860aedd005.jpg', NULL),

(100008,
 'https://storage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4',
 'https://i0.hdslb.com/bfs/face/6decfb735be6078999a0c330b1b56b324694e873.jpg',
 'Sintel（示例视频）',
 '3546708495567018', 'PORTRAIT', 91000, 1198, 30, 933, 13279,
 'https://i0.hdslb.com/bfs/face/6decfb735be6078999a0c330b1b56b324694e873.jpg', NULL),

(100009,
 'https://storage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4',
 'https://i1.hdslb.com/bfs/face/3c657487f9a7993f50bcbafa82f64f99ff1229bd.jpg',
 'Subaru Outback On Street And Dirt（示例视频）',
 '15377173', 'LANDSCAPE', 120000, 11368, 147, 5155, 97432,
 'https://i1.hdslb.com/bfs/face/3c657487f9a7993f50bcbafa82f64f99ff1229bd.jpg', NULL),

(100010,
 'https://storage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4',
 'https://i2.hdslb.com/bfs/face/49fee719f87cc10f56bd3d280b53bf92aa2b9f40.jpg',
 'Tears Of Steel（示例视频）',
 '3546610069932716', 'LANDSCAPE', 50000, 50839, 1083, 8477, 655854,
 'https://i2.hdslb.com/bfs/face/49fee719f87cc10f56bd3d280b53bf92aa2b9f40.jpg', NULL),

(100011,
 'https://storage.googleapis.com/gtv-videos-bucket/sample/VolkswagenGTIReview.mp4',
 'https://i2.hdslb.com/bfs/face/8dcf9575286a3a6ae8b124d4b38c082c208d8a8e.jpg',
 'Volkswagen GTI Review（示例视频）',
 '598581575', 'LANDSCAPE', 26000, 30415, 135, 34213, 517941,
 'https://i2.hdslb.com/bfs/face/8dcf9575286a3a6ae8b124d4b38c082c208d8a8e.jpg', NULL),

(100012,
 'https://storage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4',
 'https://i2.hdslb.com/bfs/face/4c9095a6fc7d6ef7bf97ee1c65767f537763c60c.jpg',
 'We Are Going On Bullrun（示例视频）',
 '652239032', 'LANDSCAPE', 135000, 25744, 1594, 4025, 1059347,
 'https://i2.hdslb.com/bfs/face/4c9095a6fc7d6ef7bf97ee1c65767f537763c60c.jpg', NULL),

(100013,
 'https://storage.googleapis.com/gtv-videos-bucket/sample/WhatCarCanYouGetForAGrand.mp4',
 'https://i0.hdslb.com/bfs/face/11ad74a0cf807b9896656ef9987e1b833aa11f8a.jpg',
 'What Car Can You Get For A Grand（示例视频）',
 '669334488', 'LANDSCAPE', 294000, 573, 17, 499, 32214,
 'https://i0.hdslb.com/bfs/face/11ad74a0cf807b9896656ef9987e1b833aa11f8a.jpg', NULL),

(100014,
 'https://samplelib.com/lib/preview/mp4/sample-20s.mp4',
 'https://i1.hdslb.com/bfs/face/01b1fc94d9f2654c291f0634cc024d6eed12ed6a.jpg',
 'Sample Video 20s（示例视频）',
 '92850496', 'PORTRAIT', 100000, 34256, 285, 2673, 194806,
 'https://i1.hdslb.com/bfs/face/01b1fc94d9f2654c291f0634cc024d6eed12ed6a.jpg', NULL),

(100015,
 'https://samplelib.com/lib/preview/mp4/sample-15s.mp4',
 'https://i0.hdslb.com/bfs/face/8fa1c5a5d8e23ae1bec7ef07e132daa7f08e5f62.jpg',
 'Sample Video 15s（示例视频）',
 '291894729', 'LANDSCAPE', 196000, 64218, 961, 27095, 917923,
 'https://i0.hdslb.com/bfs/face/8fa1c5a5d8e23ae1bec7ef07e132daa7f08e5f62.jpg', NULL);

-- 评论数据（修正videoId为对应的数值ID）
INSERT INTO beatu_comment (
  commentId, videoId, authorId, content, createdAt, likeCount, isLiked, isPending, authorAvatar
)
VALUES
-- 视频100001 的评论 (3条)
('c001', 100001, '512407709', '天哪每个品质对应的角色都好适配，好厉害', 1764757824000, 624, 0, 0, 'https://i2.hdslb.com/bfs/face/5aad76e98b8ada49d59d93ee1774a28b5d12dff4.jpg'),
('c002', 100001, '649342997', '萌得我埋头做了十道高数题[大哭]', 1764770848000, 4148, 0, 0, 'https://i2.hdslb.com/bfs/face/4ee3680a1bb4571e94028015347077bf36978c41.jpg'),
('c003', 100001, '3493076209240846', '好萌……看得我哈特软软……', 1764740503000, 420, 0, 0, 'https://i1.hdslb.com/bfs/face/8b9cd257c86ca5f216330ecb5571f57a6d52f2f3.jpg'),

-- 视频100002 的评论 (3条)
('c004', 100002, '454507', '估计未来小亮的鉴定视频，有一半素材都是AI生成的假动物。[笑哭]', 1763975663000, 686, 0, 0, 'https://i1.hdslb.com/bfs/face/1907f22d45ea501b4006c09455a8699911dd50fc.jpg'),
('c005', 100002, '124668043', '04:37 老一辈儿还有说冬天东北的铁栏杆是甜的呢[妙啊][doge]', 1763976190000, 1573, 0, 0, 'https://i1.hdslb.com/bfs/face/0d9da902fc7db5fee44d30eaa249e8e6a35939a8.jpg'),
('c006', 100002, '44745161', '等我老了我也要胡说八道', 1763979311000, 3452, 0, 0, 'https://i2.hdslb.com/bfs/face/37001a8c716ba4904e68de9d84f42be23bb4bf68.jpg'),

-- 视频100003 的评论 (3条)
('c007', 100003, '284208940', '1.14版本主要修改:.增强了画质.移除了领舞的项链.增长了领舞的头发长度.胖哥 头发颜色更变.别的不知道了', 1625533567000, 228726, 0, 0, 'https://i1.hdslb.com/bfs/face/886ded3c03bf32bae9def6027539d9b7a83eea25.jpg'),
('c008', 100003, '394196223', '男  人  大  可  不  必  这  么  完  美[doge][doge]', 1625493967000, 157836, 0, 0, 'http://i0.hdslb.com/bfs/face/0b2b12c9faa9fca9107472ec99903264c7f48e6e.jpg'),
('c009', 100003, '429532543', '主C真的帅，自信的眼神，严肃的表情，舞蹈人般的身材，以及节奏非常强烈的肥肉抖动和收吸，加上浪子一样的发型，天王的崛起。[响指]', 1625488332000, 119404, 0, 0, 'https://i1.hdslb.com/bfs/face/481dd54620a70415a965ce03013e7f9e1c5d7b79.jpg'),

-- 视频100004 的评论 (3条)
('c010', 100004, '516003403', '大家可以试试跟着天依一起眨眼，这感觉就好像是天依在工作(录舞蹈视频)时看到你了，但舞不能停，就眨眨眼向你打个招呼，而你也眨眼回意。但跟着天依眨眼的次数多了，就好像情侣之间无事可做，看着对方的脸逗对方笑，结果一个视频下来给我看脸红了♡(๑•ω•๑)♡[tv_微笑]', 1663973694000, 655, 0, 0, 'https://i1.hdslb.com/bfs/face/f259fe15491eb86731a4bf0b5ceba14b69c3a09d.jpg'),
('c011', 100004, '500332621', '不知道为什么感觉好行，快乐！这模型做的好细看得......_(≧∇≦」∠)_', 1663988623000, 91, 0, 0, 'https://i1.hdslb.com/bfs/face/5e60ad2184bf18ea01965d4e8db98184e125ccac.jpg'),
('c012', 100004, '12746344', '[脱单doge]', 1672834185000, 61, 0, 0, 'https://i1.hdslb.com/bfs/face/84a53815c6098d0572f4eeff5eac8fc479738a32.jpg'),

-- 视频100005 的评论 (3条)
('c013', 100005, '3546705091889223', '你只看到我的表面贫瘠，没看到我的内心丰盈[星星眼]', 1745634415000, 29, 0, 0, 'https://i0.hdslb.com/bfs/face/de045acf59df5a1eaccd059782ef2824337a561e.jpg'),
('c014', 100005, '3546750843358055', '有一种笨拙而自由的快乐', 1744386935000, 29, 0, 0, 'https://i2.hdslb.com/bfs/face/d7fff48a5e921072f520070ad818735cf73e7577.jpg'),
('c015', 100005, '501937905', '没有人认出来吗，这是怪哥的张牙舞爪健身操啊[笑哭]', 1744445594000, 26, 0, 0, 'https://i0.hdslb.com/bfs/face/1a9ed8a15f65cc75674371ac7ca82ec4bdccda3e.jpg'),

-- 视频100006 的评论 (3条)
('c016', 100006, '1762948', '《冷 脸 热 舞 王》', 1742109056000, 5130, 0, 0, 'https://i1.hdslb.com/bfs/face/5dc022ad00f5ef7c0d2633176beedfdac64c0b09.jpg'),
('c017', 100006, '2407474', 'xhs那边今天被创了n次没想到B站这边也有觉得怪或者好笑的评论…真的心累了真的…把那边的解释复制过来一下……真的…姐们觉得好笑或者搞笑或者面部表情不够生动的请刷走可以吗？？？我做梦也想做的跟叠纸一样好啊但是个人能力仅限于此了', 1742136700000, 2574, 0, 0, 'https://i1.hdslb.com/bfs/face/67cf24304f29b4addda4938b8bb0ee98cdd8e12f.jpg'),
('c018', 100006, '264513499', '摸摸，up主没深度混过深空的社区吧……社区环境打架打厨子打得很厉害的，夏以昼作为最晚上线的男主被盯着打压是很正常的[笑哭]我不玩乙游仅仅是吃瓜，之前围观到这个男主的同人作者也有被一些不知道该怎么形容的其他男主的粉丝围攻的，然后会有很多装作是这个男主的推的披皮在搬弄是非，建议老师别太放在心上，作为老二次元来看你的mmd真的做得挺好的。', 1742227525000, 1724, 0, 0, 'https://i1.hdslb.com/bfs/face/8e8db0582ff5d4a6e90c06b40c643764a82adda7.jpg'),

-- 视频100007 的评论 (3条)
('c019', 100007, '695488101', '[打call][打call]', 1763996710000, 7, 0, 0, 'https://i0.hdslb.com/bfs/face/4bbcdc27f3b227322c87bb5cb30600c371828e73.jpg'),
('c020', 100007, '84511479', '@南望书斋', 1764069270000, 1, 0, 0, 'https://i0.hdslb.com/bfs/face/f22d4da7b46c34c572c72f0d7ede76d34004d5ea.jpg'),
('c021', 100007, '3546745751472938', '见K-ON必赞[星星眼][星星眼][给心心]', 1764034685000, 2, 0, 0, 'https://i2.hdslb.com/bfs/face/d19a97fdd3430fab5df61d7ada433ae3a38b8eef.jpg'),

-- 视频100008 的评论 (3条)
('c022', 100008, '11822935', '才刚开始降温我就要怀念起盛夏了[大哭]', 1729545571000, 23, 0, 0, 'https://i1.hdslb.com/bfs/baselabs/b84a4cb7eb0dc883e67c4766603e6c5b1e58421e.png'),
('c023', 100008, '96640975', '夏天有种生命力。', 1730520114000, 20, 0, 0, 'https://i1.hdslb.com/bfs/face/816dc5224a4f5f2a1b351f6e18e4cd247ea4093b.jpg'),
('c024', 100008, '225444889', '为什么呢', 1729832863000, 17, 0, 0, 'https://i1.hdslb.com/bfs/face/ba268a6647eac4a07570e9e1b976512986951782.jpg'),

-- 视频100009 的评论 (3条)
('c025', 100009, '377572568', 'on last kiss 响起时候真哭了[Mygo表情包_大哭][Mygo表情包_大哭][Mygo表情包_大哭]', 1761889794000, 167, 0, 0, 'https://i2.hdslb.com/bfs/face/0a8e6fbd90335c5de2235731f9e9b9b617bdef11.jpg'),
('c026', 100009, '501483', '神会说：欧咩得多', 1761885542000, 147, 0, 0, 'https://i0.hdslb.com/bfs/face/3e9d09fccb015a00a3057f2b0f9f6ca096b31377.jpg'),
('c027', 100009, '104243347', '和女朋友一起去的，我俩包场了[呲牙]', 1761919033000, 74, 0, 0, 'https://i1.hdslb.com/bfs/face/d196dd031dc8ceb6b328812aacdaf441d28656dc.jpg'),

-- 视频100010 的评论 (3条)
('c028', 100010, '148957839', '目前 B 站看到最用心的一个，花了不少功夫吧', 1761542269000, 8420, 0, 0, 'https://i0.hdslb.com/bfs/face/733d8422f24f91fda9a95b6097a693dda2de1d24.jpg'),
('c029', 100010, '389685337', '只要做的足够诚意，瑕疵就不会成为减分项，因为那叫成长空间。', 1761683544000, 5786, 0, 0, 'https://i1.hdslb.com/bfs/face/b28a351b7deea124b98c24486a2bb3c4a2deac0b.jpg'),
('c030', 100010, '379390855', '谁说的校园跑路线[无语]', 1761643204000, 3754, 0, 0, 'https://i1.hdslb.com/bfs/face/1cc39f40dfe0ae89270e7c9b4e4063ee5e348dda.jpg'),

-- 视频100011 的评论 (3条)
('c031', 100011, '3494361033608001', '我发现崩铁男角色老有魅力了，原神厨女角色多，崩铁厨男角色多[doge]', 1713027683000, 1361, 0, 0, 'https://i0.hdslb.com/bfs/face/f9963184a9bb5e65d75a04d7fc7a3ffcb33c5a0e.jpg'),
('c032', 100011, '519801151', '好耶，老师剪的好棒，这首歌感觉真的超适合砂金大人！[初音未来三连快乐表情包_三连快乐][初音未来三连快乐表情包_三连快乐][初音未来三连快乐表情包_三连快乐]', 1713024463000, 150, 0, 0, 'https://i1.hdslb.com/bfs/face/9df3c067507afcc99ad540bd9fdc38bb766c39cb.webp'),
('c033', 100011, '598581575', '砂金新作品！！BV1At42177Pe[星星眼]', 1713795906000, 68, 0, 0, 'https://i2.hdslb.com/bfs/face/8dcf9575286a3a6ae8b124d4b38c082c208d8a8e.jpg'),

-- 视频100012 的评论 (3条)
('c034', 100012, '360606596', 'OK了，官方还特意把尼克救溺水朱迪的画面闪了三回，很难让人不注意，是搭档是情侣我自有定夺[doge]', 1761621602000, 2888, 0, 0, 'https://i2.hdslb.com/bfs/face/4c324330f2e1f76b1d72d780d8908cf94d1a8a88.jpg'),
('c035', 100012, '3546390502312149', '[doge][思考]', 1761653215000, 1910, 0, 0, 'https://i0.hdslb.com/bfs/face/member/noface.jpg'),
('c036', 100012, '20301524', '注意到时候电影票别买成国语版[doge]', 1761620519000, 4106, 0, 0, 'https://i2.hdslb.com/bfs/face/dfa3dc4b52039c86774687bf7b49428a8c175958.jpg'),

-- 视频100013 的评论 (3条)
('c037', 100013, '213820482', '这（MV)莫名想到，那个''吹牛老爹''的事件', 1731223971000, 28, 0, 0, 'https://i0.hdslb.com/bfs/face/2b4cfc2b6671ff201d7e69f975f268e1db47655b.jpg'),
('c038', 100013, '499471102', '怎么没人', 1757747862000, 4, 0, 0, 'https://i1.hdslb.com/bfs/face/f089e0bae1801e859caae60f1667e04b7337a315.jpg'),
('c039', 100013, '3461567819549658', '有种经济上行的感觉', 1764442867000, 2, 0, 0, 'https://i0.hdslb.com/bfs/face/fd8681f451ed967e615f3632f72b9525737bb266.jpg'),

-- 视频100014 的评论 (3条)
('c040', 100014, '3546771223481092', '好可爱QWQ心血来潮画了一下', 1764684296000, 1961, 0, 0, 'https://i1.hdslb.com/bfs/face/192983acc366c0e035bc3dbdce74120641e9cdcc.jpg'),
('c041', 100014, '1326922374', '求做这个', 1764668875000, 890, 0, 0, 'https://i0.hdslb.com/bfs/face/84739ad43064d322acdfe5389593a33f51087686.jpg'),
('c042', 100014, '693576322', '菌老师，求这个[打call]', 1764684134000, 389, 0, 0, 'https://i1.hdslb.com/bfs/face/1b9d5c5f9f7ffe5be04241702610db6e063460ad.jpg'),

-- 视频100015 的评论 (3条)
('c043', 100015, '179319934', '[妙啊]虽然模仿的很像，但是感觉没有很好的模仿出她对于钱如痴如狂的情感呢', 1764390768000, 107, 0, 0, 'https://i1.hdslb.com/bfs/face/6e1da67e693a19cc9d468a21e2c8bd30fe05c9b7.jpg'),
('c044', 100015, '1069344105', '歌词大意：来财，来，来财[doge]', 1764383515000, 1113, 0, 0, 'https://i0.hdslb.com/bfs/face/b461e0beb28bdeda355a4919b3a1c7b24a6ec8db.jpg'),
('c045', 100015, '176618655', '原版是老阿姨装嫩，这个是小可爱装老阿姨。非常有意思[doge_金箍]', 1764391082000, 2877, 0, 0, 'http://i0.hdslb.com/bfs/face/0e8212a0b68957b400fbaae570f58c686eebc205.jpg');

-- ============================================
-- 4. 验证数据
-- ============================================
SELECT '数据库初始化完成！' AS message;
SELECT COUNT(*) AS user_count FROM beatu_user;
SELECT COUNT(*) AS video_count FROM beatu_video;
SELECT COUNT(*) AS comment_count FROM beatu_comment;
SELECT COUNT(*) AS interaction_count FROM beatu_video_interaction;
SELECT COUNT(*) AS follow_count FROM beatu_user_follow;
SELECT COUNT(*) AS watch_history_count FROM beatu_watch_history;