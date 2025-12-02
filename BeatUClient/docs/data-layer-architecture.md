# 数据层架构文档

## 概述

本文档描述BeatU项目的数据层架构，包括Room数据库、DataStore、MySQL后端服务的配置和使用方法。

## 架构设计

数据层采用Clean Architecture设计，分为以下几个层次：

```
Domain Layer (业务层)
    ↓
Repository (仓储层) - 统一数据访问接口
    ↓
├─→ Remote DataSource (远程数据源) - MySQL后端服务
└─→ Local DataSource (本地数据源) - Room数据库
```

## 一、公共基础设施

### 1. Room数据库配置

**位置**: `shared/database/src/main/java/com/ucw/beatu/shared/database/`

**核心类**:
- `BeatUDatabase`: Room数据库主类
- `VideoDao`: 视频数据访问对象
- `CommentDao`: 评论数据访问对象
- `UserDao`: 用户数据访问对象
- `UserVideoRelationDao`: 用户-作品关系 DAO
- `UserFollowDao`: 用户关注关系 DAO（本地缓存后端 `beatu_user_follows`）
- `UserInteractionDao`: 用户互动 DAO（本地缓存后端 `beatu_interactions`）
- `WatchHistoryDao`: 观看历史 DAO（本地缓存后端 `beatu_watch_history`）

**实体类**:
- `VideoEntity`: 视频实体
- `CommentEntity`: 评论实体
- `UserEntity`: 用户实体
- `UserVideoRelationEntity`: 用户-作品关系实体
- `UserFollowEntity`: 用户关注关系实体
- `UserInteractionEntity`: 用户互动实体（点赞/收藏/关注作者）
- `WatchHistoryEntity`: 观看历史聚合实体

**使用示例**:
```kotlin
// 通过依赖注入获取数据库实例
@Inject lateinit var database: BeatUDatabase
val videoDao = database.videoDao()
```

### 2. DataStore配置

**位置**: `shared/database/src/main/java/com/ucw/beatu/shared/database/datastore/PreferencesDataStore.kt`

**功能**: 用于存储简单的键值对配置数据（如用户设置、token等）

**使用示例**:
```kotlin
@Inject lateinit var dataStore: PreferencesDataStore

// 保存数据
dataStore.putString("user_token", "abc123")
dataStore.putBoolean("auto_play", true)

// 读取数据（Flow）
val token: Flow<String> = dataStore.getString("user_token")

// 读取数据（同步）
val tokenSync = withContext(Dispatchers.IO) {
    dataStore.getStringSync("user_token")
}
```

### 3. 网络/MySQL配置

**位置**: `app/src/main/java/com/ucw/beatu/di/NetworkModule.kt`

**配置项**:
- `BASE_URL`: MySQL后端服务地址（需在`NetworkModule.kt`中配置）
- `NetworkConfig`: 网络配置（超时时间、日志等）
- `Retrofit`: HTTP客户端
- `OkHttpClient`: OkHttp客户端

**TODO**: 在`NetworkModule.kt`中修改`BASE_URL`为实际的MySQL后端地址

### 4. 公共数据层基建

**位置**: `shared/common/src/main/java/com/ucw/beatu/shared/common/`

**核心类**:
- `ApiResponse<T>`: 统一API响应格式
- `PageResponse<T>`: 分页响应格式
- `DataException`: 数据层异常定义
- `AppResult<T>`: 统一结果类型（Success/Error/Loading）

## 二、业务数据层实现（以videofeed为例）

### 目录结构

```
business/videofeed/
├── domain/                    # 业务层
│   ├── model/                 # 领域模型
│   │   ├── Video.kt
│   │   └── Comment.kt
│   └── repository/            # Repository接口
│       └── VideoRepository.kt
└── data/                      # 数据层
    ├── api/                   # API接口定义
    │   ├── dto/               # 数据传输对象
    │   │   ├── VideoDto.kt
    │   │   └── CommentDto.kt
    │   └── VideoFeedApiService.kt
    ├── mapper/                # 数据转换器
    │   ├── VideoMapper.kt
    │   └── CommentMapper.kt
    ├── remote/                # 远程数据源
    │   └── VideoRemoteDataSource.kt
    ├── local/                 # 本地数据源
    │   └── VideoLocalDataSource.kt
    ├── repository/            # Repository实现
    │   └── VideoRepositoryImpl.kt
    └── di/                    # 依赖注入
        └── VideoDataModule.kt
```

### 数据流

```
UI层调用 Repository
    ↓
Repository协调两个数据源：
    ├─→ LocalDataSource (Room数据库)
    │   └─→ 立即返回缓存数据（如果有）
    │
    └─→ RemoteDataSource (MySQL后端)
        └─→ 获取最新数据并更新缓存
```

### 使用示例：首页视频流「本地/Mock 先展示，远端异步刷新」

```kotlin
@Inject lateinit var videoRepository: VideoRepository

// 获取视频列表（Flow响应式）
viewModelScope.launch {
    videoRepository.getVideoFeed(page = 1, limit = 20)
        .collect { result ->
            when (result) {
                is AppResult.Loading -> {
                    // 显示加载状态（首帧 shimmer 等）
                }
                is AppResult.Success -> {
                    // 首先可能是本地缓存 / Mock，稍后会被远端最新数据刷新
                    val videos = result.data
                    // 渲染视频列表
                }
                is AppResult.Error -> {
                    // 所有兜底方案都失败时才会走到这里
                    val error = result.throwable
                    // 显示错误信息 / 空态
                }
            }
        }
}

// 点赞视频
viewModelScope.launch {
    when (val result = videoRepository.likeVideo(videoId)) {
        is AppResult.Success -> {
            // 点赞成功
        }
        is AppResult.Error -> {
            // 处理错误
        }
    }
}
```

## 三、依赖注入配置

### App层模块

**位置**: `app/src/main/java/com/ucw/beatu/di/`

- `DatabaseModule.kt`: 提供Room数据库和DataStore实例
- `NetworkModule.kt`: 提供网络相关实例（Retrofit、OkHttp等）

### 业务数据层模块

**位置**: `business/videofeed/data/src/main/java/com/ucw/beatu/business/videofeed/data/di/VideoDataModule.kt`

- 提供API Service实例
- 绑定DataSource接口和实现
- 绑定Repository接口和实现

## 四、缓存策略

### 视频列表缓存

1. **第一页数据**:
   - 先从本地缓存读取并立即显示
   - 同时从远程获取最新数据
   - 远程数据到达后更新UI并保存到本地

2. **后续页数据**:
   - 直接从远程获取
   - 不进行本地缓存（仅第一页缓存）

### 视频详情缓存

1. 先检查本地缓存
2. 如果本地有，立即返回并后台更新
3. 如果本地没有，从远程获取并保存

### 评论列表缓存

与视频列表缓存策略相同（仅第一页缓存）

## 五、错误处理

数据层使用统一的异常类型：

- `NetworkException`: 网络异常（无网络、超时等）
- `ServerException`: 服务器异常（5xx错误等）
- `AuthException`: 认证异常（401、403等）
- `NotFoundException`: 资源未找到（404）
- `DatabaseException`: 数据库异常
- `UnknownException`: 未知异常

所有异常都会被包装在`AppResult.Error`中返回给业务层。

## 六、配置检查清单

- [x] Room数据库配置完成
- [x] DataStore配置完成
- [x] 网络模块配置完成
- [x] 公共数据层基建完成
- [x] videofeed业务数据层实现完成
- [ ] 配置MySQL后端服务地址（在`NetworkModule.kt`中）
- [ ] 配置API认证Token（如需要，在`NetworkModule.kt`中）
- [ ] 测试网络连接和API调用
- [ ] 测试本地缓存功能

## 七、后续工作

1. 为其他业务模块（user、search、ai等）实现数据层
2. 添加数据层单元测试
3. 优化缓存策略
4. 添加离线模式支持
5. 实现数据同步机制

## 八、常见问题

### Q: 如何修改MySQL后端地址？

A: 在`app/src/main/java/com/ucw/beatu/di/NetworkModule.kt`中修改`BASE_URL`常量。

### Q: 如何添加新的API接口？

A: 
1. 在对应的`*ApiService`接口中添加方法
2. 在`RemoteDataSource`接口和实现中添加对应方法
3. 在`Repository`接口和实现中添加对应方法

### Q: 如何添加新的数据表？

A:
1. 创建Entity类（继承Room Entity）
2. 创建Dao接口
3. 在`BeatUDatabase`中注册Entity和Dao
4. 升级数据库版本

### Q: 数据同步策略是什么？

A: 当前实现为"缓存优先"策略：
- 先显示本地缓存数据
- 后台同步远程最新数据
- 远程数据到达后更新UI

