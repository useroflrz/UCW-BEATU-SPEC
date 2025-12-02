## 后端数据模型说明

> 模型命名与 `docs/api_reference.md` 对齐，以下列出核心字段、类型、约束及备注，供数据库设计与接口校验参考。

### 1. Video

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `id` | String | PK | 视频唯一 ID（可使用雪花/UUID） |
| `playUrl` | String | 非空 | CDN/OSS 播放地址，推荐 HLS/DASH |
| `coverUrl` | String | 非空 | 封面 URL |
| `title` | String | <=128 chars | 视频标题 |
| `tags` | List<String> | 可为空 | 标签列表 |
| `durationMs` | Long | >0 | 时长（毫秒） |
| `orientation` | Enum(`portrait`,`landscape`) | 非空 | 用于客户端横竖屏区分 |
| `authorId` | String | FK | 作者 ID |
| `authorName` | String | 非空 | 作者昵称 |
| `authorAvatar` | String | 可空 | 头像 URL |
| `likeCount` / `commentCount` / `favoriteCount` / `shareCount` / `viewCount` | Long | 默认 0 | 互动统计 |
| `isLiked` / `isFavorited` / `isFollowedAuthor` | Boolean | 默认 false | 与当前用户相关，需在接口层计算 |
| `qualities` | List<VideoQuality> | 可空 | 多码率信息，后续扩展 |

### 2. VideoQuality（可选）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `label` | String | 例如 `1080P`、`720P`、`AUTO` |
| `bitrate` | Int | 单位 kbps |
| `resolution` | String | `1920x1080` |
| `url` | String | 对应码率的播放地址 |

### 3. Comment

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `id` | String | PK | 评论 ID |
| `videoId` | String | FK | 视频 ID |
| `authorId` | String | FK | 评论用户 |
| `authorName` | String | 非空 | |
| `authorAvatar` | String | 可空 | |
| `content` | String | <= 500 chars | 评论正文 |
| `createdAt` | Long | 非空 | 时间戳（ms） |
| `isAiReply` | Boolean | 默认 false | 是否为 AI 回复 |
| `aiMeta` | AiMeta? | 可空 | 模型、置信度 |
| `likeCount` | Long | 默认 0 | 点赞数 |

### 4. AiMeta

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `model` | String | 使用的模型标识 |
| `confidence` | Double | 0~1 |
| `source` | String | 例如 `recommend`, `qa` |

### 5. UserSummary

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | String | 用户 ID |
| `nickname` | String | 昵称 |
| `avatarUrl` | String | 头像 |
| `bio` | String | 个人简介 |
| `followStatus` | Enum(`none`,`followed`,`mutual`) | 与当前用户关系 |

### 6. User（后端持久化）

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `id` | String | PK | 用户唯一 ID |
| `nickname` | String | 非空 | 昵称 |
| `avatar` | String | 可空 | 头像 URL |
| `bio` | String | 可空 | 个人简介 |
| `followings` | Long | 默认 0 | 关注数聚合 |
| `likesReceived` | Long | 默认 0 | 获赞量 |
| `worksCount` | Long | 默认 0 | 作品总数 |
| `createdAt` / `updatedAt` | DateTime | 自动 | 记录时间 |

### 7. UserFollow

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `id` | Long | PK | 自增主键 |
| `followerId` | String | FK -> User | 粉丝 ID |
| `followeeId` | String | FK -> User | 被关注用户 ID |
| `createdAt` | DateTime | 自动 | 关注时间 |

> 业务上需要维持 `followerId + followeeId` 唯一索引，方便查询“我关注谁 / 谁关注我”。

### 8. WatchHistory（用户观看历史聚合）

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `id` | Long | PK | 自增主键 |
| `userId` | String | FK -> User | 用户 ID |
| `videoId` | String | FK -> Video | 视频 ID |
| `lastWatchAt` | DateTime | 自动 | 最近观看时间 |
| `watchCount` | Int | 默认 1 | 累计观看次数 |
| `lastSeekMs` | Long | 默认 0 | 最近一次播放进度 |
| `lastDurationMs` | Long | 默认 0 | 最近一次观看时长 |
| `totalDurationMs` | Long | 默认 0 | 累计观看时长 |
| `completionRate` | Double | 默认 0 | 最近一次的完成度，0~1 |

> 该表按 `userId + videoId` 唯一，方便聚合查询“最近看过”、“播放历史”等模块，并提供播放进度同步能力。

### 6. InteractionState（本地缓存 & 观测）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `videoId` | String | |
| `liked` | Boolean | 客户端本地记录 |
| `favorited` | Boolean | |
| `followed` | Boolean | |
| `lastSeekMs` | Long | 最近播放进度 |
| `defaultSpeed` | Float | 来自 Settings |
| `defaultQuality` | String | 例如 `AUTO`、`HD` |

### 7. MetricsPayload（播放指标）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `videoId` | String | |
| `fps` | Float | 平均 FPS |
| `startUpMs` | Long | 首帧时间 |
| `rebufferCount` | Int | 卡顿次数 |
| `memoryMb` | Double | 播放期间内存峰值 |
| `channel` | String | `recommend` / `follow` / `landscape` |

### 8. 推荐请求

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `videoId` | String | 当前播放完的视频 |
| `dwellMs` | Long | 停留时长 |
| `consumedDurationMs` | Long | 播放完成度 |
| `tags` | List<String>? | 客户端可上传的标签 |

### 9. 清晰度策略请求

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `videoId` | String | |
| `networkStats` | Object | 包含 `bandwidthKbps`、`latencyMs`、`packetLoss` |
| `deviceStats` | Object | 包含 `cpuLoad`、`gpuLoad`、`batteryLevel`、`temperature` |

> 实际数据库建模可按表/索引拆分，上述字段用于对齐接口返回值与实体定义，新增字段请同步更新本文件与 `docs/api_reference.md`。

