## API Reference

> 本文档基于 BeatUClient 客户端实际实现，列出所有需要接入后端的接口与数据模型。所有接口默认走 `BeatUGateway`，由网关转发到具体服务。

### 1. 数据模型（客户端 & 共用）

| 模型 | 字段 | 说明 |
|---|---|---|
| `Video` | `id: String`, `playUrl: String`, `coverUrl: String`, `title: String`, `author: UserSummary`, `tags: List<String>`, `durationMs: Long`, `orientation: Portrait|Landscape`, `qualities: List<VideoQuality>`, `stats: VideoStats` | Feed 基础信息 |
| `VideoQuality` | `label: String (Auto/HD/SD)`, `bitrate: Int`, `resolution: String`, `url: String` | 多码率源 |
| `VideoStats` | `likeCount: Long`, `commentCount: Long`, `favoriteCount: Long`, `shareCount: Long`, `viewCount: Long`, `isLiked: Boolean`, `isFavorited: Boolean`, `isFollowedAuthor: Boolean` | UI 状态 |
| `Comment` | `id`, `videoId`, `author: UserSummary`, `content`, `createdAt`, `isAiReply: Boolean`, `aiMeta?: AiMeta` | 评论 / AI 回复 |
| `AiMeta` | `model: String`, `confidence: Double`, `source: String` | AI 答案描述 |
| `User` | `id`, `avatarUrl`, `name`, `bio`, `likesCount`, `followingCount`, `followersCount` | 个人主页核心字段，映射 `UserEntity` |
| `UserSummary` | `id`, `nickname`, `avatarUrl`, `bio`, `followStatus` | 个人信息概览 |
| `UserVideoRelation` | `id (auto)`, `userId`, `videoId` | 用户与作品的连接表 |
| `InteractionState` | `videoId`, `liked`, `favorited`, `followed`, `lastSeekMs`, `defaultSpeed`, `defaultQuality` | 本地缓存 |

### 2. BeatUGateway（对客户端暴露）

#### 2.1 视频相关接口

| Method | Path | Query/Body | 响应 | 说明 | 后端 |
|---|---|---|---|---|---|
| GET | `/api/videos` | `page`, `limit`, `orientation?` | `ApiResponse<PageResponse<Video>>` | 获取视频列表（分页），支持横屏过滤 | ContentService |
| GET | `/api/videos/{id}` | - | `ApiResponse<Video>` | 获取视频详情 | ContentService |
| POST | `/api/videos/{id}/like` | - | `ApiResponse<Unit>` | 点赞视频 | ContentService |
| POST | `/api/videos/{id}/unlike` | - | `ApiResponse<Unit>` | 取消点赞 | ContentService |
| POST | `/api/videos/{id}/favorite` | - | `ApiResponse<Unit>` | 收藏视频 | ContentService |
| POST | `/api/videos/{id}/unfavorite` | - | `ApiResponse<Unit>` | 取消收藏 | ContentService |

**分页参数说明**:
- `page`: 页码，从 1 开始
- `limit`: 每页数量，默认 20
- `orientation`: 视频方向过滤（可选），`"portrait"` 或 `"landscape"`

**分页响应格式** (`PageResponse<T>`):
```json
{
  "items": [],
  "page": 1,
  "pageSize": 20,
  "total": 100,
  "totalPages": 5,
  "hasNext": true,
  "hasPrevious": false
}
```

#### 2.2 评论相关接口

| Method | Path | Query/Body | 响应 | 说明 | 后端 |
|---|---|---|---|---|---|
| GET | `/api/videos/{id}/comments` | `page`, `limit` | `ApiResponse<PageResponse<Comment>>` | 获取评论列表（分页，含 AI 回复） | ContentService |
| POST | `/api/videos/{id}/comments` | `{ content: String }` | `ApiResponse<Comment>` | 发布评论 | ContentService |
| POST | `/api/videos/{id}/comments/ai` | `{ question: String }` | `ApiResponse<Comment>` | `@元宝` AI 问答，返回机器人评论 | AIService |

#### 2.3 用户相关接口

| Method | Path | Query/Body | 响应 | 说明 | 后端 |
|---|---|---|---|---|---|
| GET | `/api/users/{id}` | - | `ApiResponse<User>` | 获取用户信息 | ContentService |
| POST | `/api/users/{id}/follow` | - | `ApiResponse<Unit>` | 关注用户 | ContentService |
| POST | `/api/users/{id}/unfollow` | - | `ApiResponse<Unit>` | 取消关注用户 | ContentService |

#### 2.4 AI 相关接口

| Method | Path | Query/Body | 响应 | 说明 | 后端 |
|---|---|---|---|---|---|
| POST | `/api/ai/recommend` | `{ videoId, consumedDurationMs, tags?, dwellMs }` | `ApiResponse<List<Video>>` | 视频播放完成后拉取推荐 | AIService |
| POST | `/api/ai/quality` | `{ videoId, networkStats, deviceStats }` | `ApiResponse<{ quality: String, reason: String }>` | AI 清晰度建议 | AIService |

#### 2.5 监控指标接口

| Method | Path | Query/Body | 响应 | 说明 | 后端 |
|---|---|---|---|---|---|
| POST | `/api/metrics/playback` | `{ videoId, fps, startUpMs, rebufferCount, memoryMb, channel }` | `ApiResponse<Unit>` | 播放性能指标上报 | Observability |

### 3. BeatUClient 本地接口约定

- Repository 层需实现以下接口：
  - `VideoRepository.getVideoFeed(page, limit, orientation?)` → `Flow<AppResult<List<Video>>>`
  - `VideoRepository.getVideoDetail(videoId)` → `AppResult<Video>`
  - `VideoRepository.likeVideo(videoId)` / `unlikeVideo(videoId)`
  - `VideoRepository.favoriteVideo(videoId)` / `unfavoriteVideo(videoId)`
  - `VideoRepository.getComments(videoId, page, limit)` → `Flow<AppResult<List<Comment>>>`
  - `VideoRepository.postComment(videoId, content)` → `AppResult<Comment>`
  - `UserRepository.getUserById(userId)` → `User?`
  - `UserRepository.observeUserById(userId)` → `Flow<User?>`
  - `AiRepository.ask(videoId, question)` → `AppResult<Comment>`（AI 问答）
  - `AiRepository.requestRecommendation(videoId, stats)` → `AppResult<List<Video>>`
- Player 交互：
  - `PlayerRepository.preload(videoId, quality)`：触发 `CacheDataSource`.
  - `PlayerRepository.reportMetrics(metrics: PlaybackMetrics)`：调用 `/api/metrics/playback`.

### 4. 服务端职责拆分

- **BeatUContentService**
  - 维护视频/评论/互动数据，提供 Feed、点赞、收藏、关注、评论接口。
  - 支持多码率源的元数据查询。
  - 提供用户信息查询接口。
- **BeatUAIService**
  - 推荐：根据用户画像 + 视频标签返回 `nextVideos`。
  - 评论 AI：基于视频标签 + 用户问题生成 `Comment`（`isAiReply=true`）。
  - 清晰度策略：结合网络/设备信息返回码率建议。
- **BeatUGateway**
  - 统一鉴权（Token/DeviceId）、熔断、版本灰度；对上游服务进行聚合。
- **BeatUObservability**
  - 接收 `/api/metrics/playback`、AI 命中率、评论响应时间等指标。

### 5. 协议与错误码

#### 4.1 统一响应格式

所有接口统一返回 `ApiResponse<T>` 格式：
```json
{
  "code": 0,
  "message": "OK",
  "data": {},
  "timestamp": 1234567890
}
```

**响应字段说明**:
- `code`: 状态码，`0` 或 `200` 表示成功
- `message`: 响应消息
- `data`: 响应数据（泛型）
- `timestamp`: 时间戳（可选）

**分页响应格式** (`PageResponse<T>`):
```json
{
  "items": [],
  "page": 1,
  "pageSize": 20,
  "total": 100,
  "totalPages": 5,
  "hasNext": true,
  "hasPrevious": false
}
```

#### 4.2 错误码

| 错误码 | 说明 | 处理建议 |
|---|---|---|
| `0` 或 `200` | 成功 | - |
| `401` 或 `403` | 鉴权失败 | 跳转登录页或刷新 Token |
| `1001` | 鉴权失败（自定义） | 跳转登录页或刷新 Token |
| `404` | 资源不存在 | 显示"资源不存在"提示 |
| `2001` | 视频不存在 | 显示"视频不存在"提示 |
| `2002` | 互动状态冲突 | 刷新状态后重试 |
| `3001` | AI 服务暂不可用 | 客户端需降级到缓存/默认答案 |
| `500x` | 后端异常 | 显示"服务异常，请稍后重试" |

#### 4.3 客户端异常处理

客户端使用 `DataException` 异常体系：
- `AuthException`: 认证失败（401/403/1001）
- `NotFoundException`: 资源不存在（404/2001）
- `ServerException`: 服务器错误（500x）
- `NetworkException`: 网络异常（无网络、超时等）



