## BeatU 后端 API 合同（Backend Contract）

> 参考 `docs/api_reference.md`，以下内容站在后端视角补充字段约束、错误码与落地建议。所有接口统一挂载在 `BeatUGateway`（下文以 `https://api.beatu.com` 为例），网关负责鉴权/限流/熔断，再路由到各下游服务。

### 1. 通用规范

- **Header**：`Authorization: Bearer <token>`（后续可扩展 `Device-Id`、`App-Version`）。
- **响应格式**：
  ```json
  {
    "code": 0,
    "message": "OK",
    "data": {}
  }
  ```
- **错误码**：
  | code | 说明 | 建议处理 |
  | --- | --- | --- |
  | `1001` | 鉴权失败 | 返回 401，客户端跳转登录或重试 token |
  | `2001` | 视频不存在/被删除 | 404，客户端提示“当前视频不可用”并刷新 |
  | `2002` | 互动状态冲突（重复点赞等） | 409，客户端回滚 UI |
  | `3001` | AI 服务不可用 | 503，客户端降级到默认文案 |
  | `5xx` | 网关/服务内部错误 | 500，客户端展示重试 CTA |

### 2. Feed / Video

| Method | Path | Query | Body | 响应（data） | 路由到 |
| --- | --- | --- | --- | --- | --- |
| GET | `/api/videos` | `page`(Int,1+), `limit`(Int,<=50), `orientation`?(`portrait`/`landscape`), `channel`?(`recommend`/`follow`) | - | `{ items: [Video], total, page, limit }` | ContentService |
| GET | `/api/videos/{id}` | - | - | `{ video: Video }` | ContentService |

**Video 模型关键字段**：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | String | 全局唯一 ID |
| `playUrl` | String | CDN/OSS 播放地址（HLS/DASH/MP4） |
| `coverUrl` | String | 封面图 |
| `title`, `tags[]` | String | 展示信息 |
| `orientation` | String | `portrait` / `landscape` |
| `durationMs` | Long | 播放时长 |
| `authorId`, `authorName`, `authorAvatar` | String | 作者信息 |
| `likeCount`, `commentCount`, `favoriteCount`, `shareCount`, `viewCount` | Long | 互动统计 |
| `isLiked`, `isFavorited`, `isFollowedAuthor` | Boolean | 当前用户态 |

**备注**：
- `orientation` 将被客户端用于切换竖/横屏模块。
- `playUrl` 建议返回多码率列表：`qualities: [{label:"1080P", url:"..."}, ...]`，客户端可选取 `defaultQuality`。

### 3. 互动（Like/Favorite/Follow）

| Method | Path | Body | 响应 | 路由 |
| --- | --- | --- | --- | --- |
| POST | `/api/videos/{id}/like` | `{ action: "LIKE" | "UNLIKE" }` | `{ success: true }` | ContentService |
| POST | `/api/videos/{id}/favorite` | `{ action: "SAVE" | "REMOVE" }` | `{ success: true }` | ContentService |
| POST | `/api/follow` | `{ authorId, action:"FOLLOW"|"UNFOLLOW" }` | `{ success: true }` | ContentService |

**延迟目标**：单次互动 300 ms 内完成；若写入失败需返回 `2002` 让客户端回滚。

### 4. 评论 / AI 问答

| Method | Path | Query/Body | 说明 |
| --- | --- | --- | --- |
| GET | `/api/videos/{id}/comments` | `page`, `limit` | 分页拉取评论（含 AI 回复） |
| POST | `/api/videos/{id}/comments` | `{ content, replyTo? }` | 用户评论，返回新 Comment |
| POST | `/api/videos/{id}/comments/ai` | `{ question }` | `@元宝` 触发，AIService 返回 Comment |

**Comment 字段**：`id`, `videoId`, `authorId`, `authorName`, `authorAvatar`, `content`, `createdAt`, `isAiReply`, `likeCount`.

**Android 客户端对接现状**：
- 已接入：`GET /api/videos/{id}/comments` 与 `POST /api/videos/{id}/comments`，通过 `VideoFeedApiService.getComments/postComment` → `VideoRepository` → `GetCommentsUseCase` / `PostCommentUseCase` → `VideoCommentsDialogFragment` 完成竖屏/横屏统一弹层展示与发布。
- 暂未接入：`POST /api/videos/{id}/comments/ai`（`@元宝` AI 问答），接口已在后端与合同中定义，后续在 AI 评论能力落地时由客户端复用本节契约。

### 5. AI 能力

| Method | Path | Body | 返回 | 说明 |
| --- | --- | --- | --- | --- |
| POST | `/api/ai/recommend` | `{ videoId, dwellMs, consumedDurationMs, tags? }` | `{ nextVideos: [Video], reason }` | 播放完成后请求推荐 |
| POST | `/api/ai/quality` | `{ videoId, networkStats, deviceStats }` | `{ quality: "HD", reason }` | 清晰度建议（可返回 `auto`） |
| POST | `/api/ai/comment/qa` | `{ videoId, question }` | `{ comment: Comment }` | `@元宝` 问答 |

短期内可由 AIService 返回 Mock 数据，字段保持一致以便后续上线真实模型。

### 6. 观测性

| Method | Path | Body | 说明 |
| --- | --- | --- | --- |
| POST | `/api/metrics/playback` | `{ videoId, fps, startUpMs, rebufferCount, memoryMb, channel }` | 由客户端在关键节点上报 |
| POST | `/api/metrics/interaction` | `{ event, videoId, latencyMs, success }` | 互动链路可选指标 |

Observability 服务需将数据落到 TSDB/日志系统，并提供告警（首帧>800ms、错误率>5% 等）。

### 7. 鉴权与限流建议

- 初期可采用静态 Token（配置于客户端和 Gateway），后续接入 OAuth/OIDC。
- 对高频接口设置限流：`/api/videos` 100 QPS / 用户，互动接口 50 QPS / 用户。

### 8. 待办 / 风险

1. `BASE_URL` 目前在客户端为占位符 (`NetworkModule.BASE_URL`)，后端部署完成后需同步正式域名。
2. 视频上传/转码接口尚未纳入本合同，后续 ContentService 提供管理面 API。
3. AI 能力若暂不可用，需保证接口可降级（返回默认推荐/评论），避免阻塞主流程。

> 以上合同将作为后端任务拆解与联调的依据，如有字段增减请同步更新本文件与 `docs/api_reference.md`。

