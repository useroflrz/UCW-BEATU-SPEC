## API Reference

> 与 `BeatUClient/docs/需求.md` 对齐，列出首批需要实现/Mock 的接口与数据模型。所有接口默认走 `BeatUGateway`，由网关转发到具体服务。

### 1. 数据模型（客户端 & 共用）

| 模型 | 字段 | 说明 |
|---|---|---|
| `Video` | `id: String`, `playUrl: String`, `coverUrl: String`, `title: String`, `author: UserSummary`, `tags: List<String>`, `durationMs: Long`, `orientation: Portrait|Landscape`, `qualities: List<VideoQuality>`, `stats: VideoStats` | Feed 基础信息 |
| `VideoQuality` | `label: String (Auto/HD/SD)`, `bitrate: Int`, `resolution: String`, `url: String` | 多码率源 |
| `VideoStats` | `likeCount: Long`, `commentCount: Long`, `favoriteCount: Long`, `shareCount: Long`, `viewCount: Long`, `isLiked: Boolean`, `isFavorited: Boolean`, `isFollowedAuthor: Boolean` | UI 状态 |
| `Comment` | `id`, `videoId`, `author: UserSummary`, `content`, `createdAt`, `isAiReply: Boolean`, `aiMeta?: AiMeta` | 评论 / AI 回复 |
| `AiMeta` | `model: String`, `confidence: Double`, `source: String` | AI 答案描述 |
| `UserSummary` | `id`, `nickname`, `avatarUrl`, `bio`, `followStatus` | 个人信息概览 |
| `InteractionState` | `videoId`, `liked`, `favorited`, `followed`, `lastSeekMs`, `defaultSpeed`, `defaultQuality` | 本地缓存 |

### 2. BeatUGateway（对客户端暴露）

| Method | Path | Query/Body | 响应 | 说明 | 后端 |
|---|---|---|---|---|---|
| GET | `/api/v1/feed` | `cursor`, `pageSize`, `channel=recommend|follow|profile`, `orientation` | `FeedResponse`（`items: List<Video>`, `nextCursor`） | Paging3 数据源，支持频道切换、横屏过滤 | ContentService |
| POST | `/api/v1/interaction/like` | `{ videoId, action: LIKE|UNLIKE }` | `{ success }` | 双击点赞 / 取消 | ContentService |
| POST | `/api/v1/interaction/favorite` | `{ videoId, action: SAVE|REMOVE }` | `{ success }` | 收藏 | ContentService |
| POST | `/api/v1/interaction/follow` | `{ authorId, action: FOLLOW|UNFOLLOW }` | `{ success }` | 关注作者 | ContentService |
| GET | `/api/v1/comment/list` | `videoId`, `cursor`, `pageSize` | `{ comments: List<Comment>, nextCursor }` | 评论列表（含 AI 回复） | ContentService |
| POST | `/api/v1/comment/create` | `{ videoId, content, replyTo? }` | `{ comment: Comment }` | 用户评论 | ContentService |
| POST | `/api/v1/comment/ai` | `{ videoId, question }` | `{ comment: Comment }` | `@元宝` AI 问答，返回机器人评论 | AIService |
| POST | `/api/v1/ai/recommend` | `{ videoId, consumedDurationMs, tags?, dwellMs }` | `{ nextVideos: List<Video> }` | 视频播放完成后拉取推荐 | AIService |
| POST | `/api/v1/ai/quality` | `{ videoId, networkStats, deviceStats }` | `{ quality: String, reason }` | AI 清晰度建议 | AIService |
| POST | `/api/v1/metrics/playback` | `{ videoId, fps, startUpMs, rebufferCount, memoryMb, channel }` | `{ success }` | 播放性能指标上报 | Observability |

### 3. BeatUClient 本地接口约定

- Repository 层需实现以下接口：
  - `FeedRepository.fetchFeed(channel, cursor)` → `Flow<PagingData<Video>>`
  - `InteractionRepository.like(videoId, action)`
  - `CommentRepository.observe(videoId)`
  - `AiRepository.ask(videoId, question)` → `AiReply`
  - `AiRepository.requestRecommendation(videoId, stats)` → `List<Video>`
- Player 交互：
  - `PlayerRepository.preload(videoId, quality)`：触发 `CacheDataSource`.
  - `PlayerRepository.reportMetrics(metrics: PlaybackMetrics)`：调用 `/api/v1/metrics/playback`.

### 4. 服务端职责拆分

- **BeatUContentService**
  - 维护视频/评论/互动数据，提供 Feed、点赞、收藏、关注、评论接口。
  - 支持多码率源的元数据查询。
- **BeatUAIService**
  - 推荐：根据用户画像 + 视频标签返回 `nextVideos`。
  - 评论 AI：基于视频标签 + 用户问题生成 `Comment`（`isAiReply=true`）。
  - 清晰度策略：结合网络/设备信息返回码率建议。
- **BeatUGateway**
  - 统一鉴权（Token/DeviceId）、熔断、版本灰度；对上游服务进行聚合。
- **BeatUObservability**
  - 接收 `/api/v1/metrics/playback`、AI 命中率、评论响应时间等指标。

### 5. 协议与错误码

- 所有接口统一返回：
  ```json
  {
    "code": 0,
    "message": "OK",
    "data": {}
  }
  ```
- 错误码：
  - `1001`：鉴权失败
  - `2001`：视频不存在
  - `2002`：互动状态冲突
  - `3001`：AI 服务暂不可用（客户端需降级到缓存/默认答案）
  - `500x`：后端异常（客户端展示 Retry）



