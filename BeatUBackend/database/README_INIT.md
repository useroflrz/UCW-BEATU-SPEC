# 数据库初始化说明

## 文件说明

- `init_database.sql` - 完整的数据库初始化脚本，包含表结构和示例数据

## 使用步骤（Navicat）

### 1. 连接数据库

1. 打开 Navicat
2. 创建新的 MySQL 连接：
   - 主机：`192.168.1.206`
   - 端口：`3306`
   - 用户名：`jeecg`
   - 密码：`haomo123`
   - 数据库：`jeecg-boot3`（可选，也可以在连接后选择）
3. 测试连接，确认连接成功
4. 连接到 `jeecg-boot3` 数据库

### 2. 执行初始化脚本

1. 在 Navicat 中打开连接
2. 右键点击连接，选择"新建查询"
3. 打开文件 `database/init_database.sql`
4. 点击"运行"或按 `F5` 执行脚本
5. 等待执行完成，查看执行结果

### 3. 验证数据

执行完成后，可以运行以下查询验证数据：

```sql
-- 查看用户数量
SELECT COUNT(*) AS user_count FROM beatu_users;

-- 查看视频数量
SELECT COUNT(*) AS video_count FROM beatu_videos;

-- 查看评论数量
SELECT COUNT(*) AS comment_count FROM beatu_comments;

-- 查看互动数据
SELECT COUNT(*) AS interaction_count FROM beatu_interactions;

-- 查看关注关系
SELECT COUNT(*) AS follow_count FROM beatu_user_follows;

-- 查看观看历史
SELECT COUNT(*) AS watch_history_count FROM beatu_watch_history;

-- 查看视频列表
SELECT id, title, author_name, like_count, view_count FROM beatu_videos ORDER BY created_at DESC;

-- 查看评论列表
SELECT c.id, v.title, c.author_name, c.content, c.is_ai_reply 
FROM beatu_comments c 
JOIN beatu_videos v ON c.video_id = v.id 
ORDER BY c.created_at DESC;
```

## 示例数据说明

### 视频数据（8个视频）

- **竖屏视频（6个）**：
  - video_001: 音乐舞蹈
  - video_002: 美食制作
  - video_003: 旅行风景
  - video_006: 宠物日常
  - video_007: 健身运动

- **横屏视频（3个）**：
  - video_004: 游戏操作
  - video_005: 电影片段
  - video_008: 科技评测

### 用户数据

- `user_001` ~ `user_007`：与示例视频作者一致，包含头像、简介与统计信息
- `demo-user`：客户端默认登录账号，便于联调
- `ai_beatu`：AI 助手账号，供评论等场景引用

### 关注关系

- `beatu_user_follows` 提前写入 `demo-user`、`user_002`、`user_003` 等关注行为，用于驱动“关注”频道

### 观看历史

- `beatu_watch_history` 记录 `demo-user`、`user_002` 的历史观看聚合数据，可直接验证“历史记录/继续播放”接口

### 评论数据

- 包含普通用户评论和 AI 回复（@元宝）
- 部分评论有回复关系（parent_id）

### 互动数据

- 点赞数据：demo-user 点赞了 3 个视频
- 收藏数据：demo-user 收藏了 3 个视频
- 关注数据：demo-user 关注了 3 个作者

## 注意事项

1. **执行前备份**：如果数据库中已有数据，请先备份
2. **脚本会删除现有表**：脚本开头会删除所有现有表，请谨慎操作
3. **视频URL**：示例数据中的视频URL为占位符，实际使用时需要替换为真实URL
4. **测试用户**：默认测试用户ID为 `demo-user`，可以在客户端使用此ID进行测试

## 后续操作

初始化完成后，可以：

1. 启动后端服务：`uvicorn main:app --reload`
2. 测试API接口：访问 `http://127.0.0.1:8000/docs`
3. 在客户端中测试数据加载和交互功能

