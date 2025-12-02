# BeatUClient 后端接入检查清单

## 📋 概述

本文档列出了 BeatUClient 客户端需要接入后端的所有点，帮助开发团队系统性地完成后端集成工作。

---

## ✅ 一、基础配置（必须完成）

### 1.1 后端服务地址配置
**位置**: `app/src/main/java/com/ucw/beatu/di/NetworkModule.kt`

**当前状态**: ⚠️ 使用占位符地址
```kotlin
private const val BASE_URL = "http://your-mysql-backend-server.com/"
```

**需要修改**:
- [ ] 将 `BASE_URL` 替换为实际的后端网关地址（BeatUGateway）
- [ ] 建议使用环境变量或 BuildConfig 区分开发/生产环境

**参考**: 根据 `docs/api_reference.md`，所有接口应通过 `BeatUGateway` 访问

---

### 1.2 认证 Token 配置
**位置**: `app/src/main/java/com/ucw/beatu/di/NetworkModule.kt`

**当前状态**: ⚠️ 未配置认证
```kotlin
// TODO: 如果需要token认证，可以在这里添加 Authorization header
```

**需要完成**:
- [ ] 实现 Token 存储（使用 `PreferencesDataStore`）
- [ ] 实现 Token 刷新机制
- [ ] 在 `NetworkConfig.defaultHeaders` 中添加 `Authorization` header
- [ ] 实现认证拦截器，自动添加 Token 到请求头
- [ ] 处理 401/403 认证失败情况（已有 `AuthException`，需要处理逻辑）

---

## ✅ 二、已实现的 API 接口（需要验证）

### 2.1 视频流相关接口
**位置**: `business/videofeed/data/src/main/java/com/ucw/beatu/business/videofeed/data/api/VideoFeedApiService.kt`

**已实现接口**:
- [x] `GET /api/videos` - 获取视频列表（分页，使用 `page`/`limit` 参数）
- [x] `GET /api/videos/{id}` - 获取视频详情
- [x] `GET /api/videos/{id}/comments` - 获取评论列表（分页，使用 `page`/`limit` 参数）
- [x] `POST /api/videos/{id}/like` - 点赞视频
- [x] `POST /api/videos/{id}/unlike` - 取消点赞
- [x] `POST /api/videos/{id}/favorite` - 收藏视频
- [x] `POST /api/videos/{id}/unfavorite` - 取消收藏
- [x] `POST /api/videos/{id}/comments` - 发布评论

**需要验证**:
- [ ] 验证后端接口路径是否匹配 `/api/videos/*`
- [ ] 验证分页参数（使用 `page`/`limit`，响应格式为 `PageResponse<T>`）
- [ ] 验证响应数据格式是否匹配 `ApiResponse<T>` 结构
- [ ] 测试所有接口的网络请求和响应解析

---

## ❌ 三、缺失的 API 接口（需要实现）

### 3.1 用户相关接口
**API 文档要求**: 
- `GET /api/users/{id}` - 获取用户信息
- `POST /api/users/{id}/follow` - 关注用户
- `POST /api/users/{id}/unfollow` - 取消关注用户

**当前状态**: ⚠️ 用户模块只使用本地数据源（Room 数据库），没有远程数据源

**需要实现**:
- [ ] 创建 `UserApiService` 接口
- [ ] 实现 `getUserById(userId)` 方法
- [ ] 实现 `followUser(userId)` 和 `unfollowUser(userId)` 方法
- [ ] 创建 `UserDto` 和相关的 DTO 类
- [ ] 创建 `UserRemoteDataSource` 接口和实现
- [ ] 在 `UserRepository` 中实现本地优先、远程补充的策略
- [ ] 在 `UserRepository` 中添加 `followUser()` 和 `unfollowUser()` 方法

**相关文件**:
- `business/user/data/src/main/java/com/ucw/beatu/business/user/data/api/UserApiService.kt`（需创建）
- `business/user/data/src/main/java/com/ucw/beatu/business/user/data/remote/UserRemoteDataSource.kt`（需创建）
- `business/user/data/src/main/java/com/ucw/beatu/business/user/data/repository/UserRepositoryImpl.kt`
- `business/user/domain/src/main/java/com/ucw/beatu/business/user/domain/repository/UserRepository.kt`

---

### 3.2 关注/取消关注作者接口（视频作者）
**说明**: 此接口用于关注视频作者，与用户模块的关注接口功能相同，但可能需要在视频上下文中使用

**API 文档要求**: `POST /api/users/{id}/follow` 和 `POST /api/users/{id}/unfollow`

**需要实现**:
- [ ] 在 `VideoRepository` 中添加 `followAuthor(authorId)` 和 `unfollowAuthor(authorId)` 方法
- [ ] 这些方法可以调用 `UserRepository` 的对应方法，或直接调用 `UserApiService`
- [ ] 在 `VideoRemoteDataSource` 中实现远程调用（如果需要）

**相关文件**:
- `business/videofeed/domain/src/main/java/com/ucw/beatu/business/videofeed/domain/repository/VideoRepository.kt`
- `business/videofeed/data/src/main/java/com/ucw/beatu/business/videofeed/data/remote/VideoRemoteDataSource.kt`

---

### 3.3 AI 相关接口

#### 3.3.1 AI 评论问答
**API 文档要求**: `POST /api/videos/{id}/comments/ai { question: String }`

**需要实现**:
- [ ] 创建 `AiApiService` 接口（或扩展 `VideoFeedApiService`）
- [ ] 实现 `askQuestion(videoId, question)` 方法
- [ ] 创建 `AiCommentRequest` DTO（包含 `question: String`）
- [ ] 创建 `AiRepository` 接口和实现
- [ ] 在评论界面集成 AI 问答功能

**相关文件**:
- `business/ai/data/src/main/java/com/ucw/beatu/business/ai/data/api/AiApiService.kt`（需创建）
- `business/ai/domain/src/main/java/com/ucw/beatu/business/ai/domain/repository/AiRepository.kt`（需创建）

---

#### 3.3.2 AI 推荐
**API 文档要求**: `POST /api/ai/recommend { videoId, consumedDurationMs, tags?, dwellMs }`

**需要实现**:
- [ ] 在 `AiApiService` 中添加 `requestRecommendation()` 方法
- [ ] 创建 `RecommendationRequest` DTO（包含 `videoId`, `consumedDurationMs`, `tags?`, `dwellMs`）
- [ ] 创建 `RecommendationResponse` DTO（`ApiResponse<List<Video>>`）
- [ ] 在 `AiRepository` 中实现 `requestRecommendation()` 方法
- [ ] 在视频播放完成后调用推荐接口

**相关文件**:
- `business/ai/data/src/main/java/com/ucw/beatu/business/ai/data/api/AiApiService.kt`
- `business/ai/domain/src/main/java/com/ucw/beatu/business/ai/domain/repository/AiRepository.kt`

---

#### 3.3.3 AI 清晰度建议
**API 文档要求**: `POST /api/ai/quality { videoId, networkStats, deviceStats }`

**需要实现**:
- [ ] 在 `AiApiService` 中添加 `requestQualitySuggestion()` 方法
- [ ] 创建 `QualityRequest` DTO（包含 `videoId`, `networkStats`, `deviceStats`）
- [ ] 创建 `QualityResponse` DTO（包含 `quality: String`, `reason: String`）
- [ ] 在 `AiRepository` 中实现 `requestQualitySuggestion()` 方法
- [ ] 在播放器初始化时调用，根据建议调整清晰度

**相关文件**:
- `business/ai/data/src/main/java/com/ucw/beatu/business/ai/data/api/AiApiService.kt`
- `business/ai/domain/src/main/java/com/ucw/beatu/business/ai/domain/repository/AiRepository.kt`

---

### 3.4 播放性能指标上报
**API 文档要求**: `POST /api/metrics/playback { videoId, fps, startUpMs, rebufferCount, memoryMb, channel }`

**当前状态**: ⚠️ 已有 `MetricsTracker` 和 `PlaybackMetrics`，但只记录日志，未上报后端

**需要实现**:
- [ ] 创建 `MetricsApiService` 接口
- [ ] 实现 `reportPlaybackMetrics()` 方法
- [ ] 创建 `PlaybackMetricsRequest` DTO（包含所有必需字段）
- [ ] 修改 `MetricsTracker` 的 `sink` 参数，接入后端上报
- [ ] 在 `NetworkModule` 或独立模块中提供 `MetricsTracker` 实例
- [ ] 实现批量上报和失败重试机制

**相关文件**:
- `shared/common/src/main/java/com/ucw/beatu/shared/common/metrics/MetricsTracker.kt`
- `shared/common/src/main/java/com/ucw/beatu/shared/common/metrics/PlaybackMetrics.kt`
- `shared/player/src/main/java/com/ucw/beatu/shared/player/metrics/PlayerMetricsTracker.kt`

---

## 📝 四、数据模型对齐

### 4.1 需要确认的字段映射
根据 `api_reference.md`，以下字段需要确认：

- [ ] `Video` 模型是否包含所有必需字段（`qualities`, `stats` 等）
- [ ] `Comment` 模型是否支持 `isAiReply` 和 `aiMeta` 字段
- [ ] `UserSummary` 模型是否包含 `followStatus` 字段
- [ ] `VideoStats` 模型是否包含所有互动状态字段

**相关文件**:
- `business/videofeed/domain/src/main/java/com/ucw/beatu/business/videofeed/domain/model/`
- `business/videofeed/data/src/main/java/com/ucw/beatu/business/videofeed/data/api/dto/`

---

## 🔐 五、错误处理

### 5.1 错误码处理
**API 文档定义的错误码**:
- `1001`: 鉴权失败
- `2001`: 视频不存在
- `2002`: 互动状态冲突
- `3001`: AI 服务暂不可用
- `500x`: 后端异常

**当前状态**: ✅ 已有 `DataException` 异常体系

**需要完成**:
- [ ] 在 `ApiResponse` 中处理所有错误码
- [ ] 在 `VideoRemoteDataSource` 中根据错误码抛出对应异常
- [ ] 在 UI 层显示友好的错误提示
- [ ] 实现 AI 服务降级逻辑（当返回 3001 时使用缓存/默认答案）

---

## 🧪 六、测试验证

### 6.1 接口测试清单
- [ ] 测试所有已实现的接口（视频列表、详情、评论、点赞、收藏）
- [ ] 测试网络异常情况（无网络、超时）
- [ ] 测试认证失败情况（Token 过期、未授权）
- [ ] 测试分页功能（使用 `page`/`limit` 参数）
- [ ] 测试用户信息接口（获取、关注、取消关注）
- [ ] 测试 AI 接口降级逻辑

---

## 📚 七、相关文档

- `docs/api_reference.md` - API 接口文档
- `docs/data-layer-architecture.md` - 数据层架构文档
- `business/videofeed/data/src/main/java/com/ucw/beatu/business/videofeed/data/api/VideoFeedApiService.kt` - 当前 API 服务实现

---

## 🎯 优先级建议

### 高优先级（必须完成）
1. ✅ 配置后端服务地址（BASE_URL）
2. ✅ 实现认证 Token 机制
3. ✅ 验证已实现接口的路径和参数（`/api/videos/*`，`page`/`limit`）
4. ✅ 实现用户信息接口（`GET /api/users/{id}`）
5. ✅ 实现关注/取消关注接口（`POST /api/users/{id}/follow`，`POST /api/users/{id}/unfollow`）

### 中优先级（重要功能）
6. ⚠️ 实现 AI 评论问答接口
7. ⚠️ 实现播放性能指标上报
8. ⚠️ 实现 AI 推荐接口

### 低优先级（优化功能）
9. ⚠️ 实现 AI 清晰度建议接口
10. ⚠️ 完善错误处理和降级逻辑

---

## 📝 备注

- 所有接口应通过 `BeatUGateway` 访问，而不是直接访问各个服务
- 接口路径统一使用 `/api/videos/*`、`/api/users/*`、`/api/ai/*`、`/api/metrics/*`
- 分页统一使用 `page` 和 `limit` 参数，响应格式为 `PageResponse<T>`
- 建议使用环境变量或 BuildConfig 管理不同环境的配置
- 所有网络请求应包含适当的错误处理和重试机制
- 响应格式统一为 `ApiResponse<T>`，包含 `code`、`message`、`data`、`timestamp` 字段

