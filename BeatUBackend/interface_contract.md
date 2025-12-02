## BeatU 客户端调度接口说明（FastAPI 实现版）

> 服务基于 `FastAPI + SQLAlchemy + SQLite`，统一前缀 `https://api.beatu.com/api`（本地调试 `http://127.0.0.1:8000/api`）。所有响应遵循 `{ code, message, data }` 包装。

### 通用约定

- **鉴权**：临时使用 `X-User-Id`/`X-User-Name` 头表示当前用户，可由 Gateway 替换为真实 Token。
- **分页**：`page` 从 1 开始，`limit` 最大 50。
- **错误处理**：
  - `code=1001`：鉴权失败（缺少/非法头部）
  - `code=2001`：资源不存在（视频/评论）
  - `code=2002`：互动状态冲突（重复点赞等）
  - `code=3001`：AI 服务不可用/超时
  - `code=5000`：未知错误

### 1. Feed / Video

| Method | Path | Query | Body | data |
| --- | --- | --- | --- | --- |
| GET | `/videos` | `page`, `limit`, `orientation?`, `channel?` | - | `{ items, total, page, limit }` |
| GET | `/videos/{id}` | - | - | `Video` |

**Video 字段**：`id`, `playUrl`, `coverUrl`, `title`, `tags[]`, `durationMs`, `orientation`, `authorId/Name/Avatar`, `likeCount/commentCount/favoriteCount/shareCount/viewCount`, `isLiked/isFavorited/isFollowedAuthor`, `qualities[]`

示例：

```json
GET /api/videos?page=1&limit=10&orientation=portrait
{
  "code": 0,
  "message": "OK",
  "data": {
    "items": [{ "id": "video_001", "playUrl": "...", "...": "..." }],
    "total": 24,
    "page": 1,
    "limit": 10
  }
}
```

### 2. 互动（Like / Favorite / Follow）

| Method | Path | Body | data |
| --- | --- | --- | --- |
| POST | `/videos/{id}/like` | `{ "action": "LIKE" \| "UNLIKE" }` | `{ success, message }` |
| POST | `/videos/{id}/favorite` | `{ "action": "SAVE" \| "REMOVE" }` | `{ success, message }` |
| POST | `/follow` | `{ "authorId": "...", "action": "FOLLOW" \| "UNFOLLOW" }` | `{ success, message }` |

- 互动成功会实时更新 `likeCount` / `favoriteCount`。
- 并发冲突抛 `code=2002`，客户端需回滚 UI。

### 3. 评论 & AI

| Method | Path | Query/Body | data |
| --- | --- | --- | --- |
| GET | `/videos/{id}/comments` | `page`, `limit` | `{ items, total, page, limit }` |
| POST | `/videos/{id}/comments` | `{ content, replyTo? }` | `Comment` |
| POST | `/videos/{id}/comments/ai` | `{ question }` | `Comment`（由 `@元宝` 生成并入库） |

**Comment 字段**：`id`, `videoId`, `authorId/Name/Avatar`, `content`, `createdAt`, `isAiReply`, `aiModel`, `aiSource`, `aiConfidence`, `likeCount`.

### 4. AI 能力

| Method | Path | Body | data |
| --- | --- | --- | --- |
| POST | `/ai/recommend` | `{ videoId, dwellMs, consumedDurationMs, tags? }` | `{ nextVideos: [Video], reason }` |
| POST | `/ai/quality` | `{ videoId, networkStats, deviceStats }` | `{ quality, reason }` |
| POST | `/ai/comment/qa` | `{ videoId, question }` | `{ comment: Comment }`（即时生成 AI 回复并入库） |

当前实现内部通过启发式逻辑返回 Mock 结果，方便后续替换为真实模型。

### 5. 观测性

| Method | Path | Body | data |
| --- | --- | --- | --- |
| POST | `/metrics/playback` | `{ videoId, fps, startUpMs, rebufferCount, memoryMb, channel }` | `{ success: true }` |
| POST | `/metrics/interaction` | `{ event, videoId?, latencyMs?, success? }` | `{ success: true }` |

数据落地至 `metrics_playback` / `metrics_interaction`，可用于仪表盘与告警。

### 6. 本地运行

**重要**：必须使用 conda 虚拟环境 `beatu-backend` 运行。

```bash
cd BeatUBackend

# 激活 conda 环境（必须步骤）
conda activate beatu-backend

# 初始化数据库（首次运行）
python -m database.init_db --drop

# 运行服务
uvicorn main:app --reload --host 0.0.0.0 --port 9306
```

**注意**：
- 如果环境不存在，先执行：`conda env create -f environment.yml`
- 详细启动说明请参考 [README.md](README.md)

**数据库配置**：
- 默认使用 MySQL，配置在 `.env` 文件中（参考 [CONFIG.md](CONFIG.md)）
- 如需修改数据库连接，请编辑 `.env` 文件中的 `DATABASE_URL`
- 详细配置说明请参考 [README.md](README.md) 和 [CONFIG.md](CONFIG.md)


