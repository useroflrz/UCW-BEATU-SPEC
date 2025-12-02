# 配置文件说明

## 概述

BeatUClient Android 客户端使用 XML 资源文件来管理网络配置，包括后端服务地址、超时设置等。

## 配置文件位置

### 开发环境配置
- **文件路径**: `app/src/main/res/values/config.xml`
- **用途**: 开发环境使用的配置

### 生产环境配置
- **文件路径**: `app/src/main/res/values/config_release.xml`
- **用途**: 生产环境使用的配置（需要在 build.gradle.kts 中配置 buildTypes）

## 配置项说明

### config.xml / config_release.xml

| 配置项 | 类型 | 说明 | 默认值 |
|--------|------|------|--------|
| `base_url` | string | 后端服务基础URL | http://192.168.1.181:9306/ |
| `connect_timeout_seconds` | integer | 连接超时时间（秒） | 15 |
| `read_timeout_seconds` | integer | 读取超时时间（秒） | 15 |
| `write_timeout_seconds` | integer | 写入超时时间（秒） | 15 |
| `enable_network_logging` | bool | 是否启用网络日志 | true（开发）/ false（生产） |

## 修改配置

### 修改开发环境配置

编辑 `app/src/main/res/values/config.xml`：

```xml
<resources>
    <!-- 修改后端服务地址 -->
    <string name="base_url">http://192.168.1.181:9306/</string>
    
    <!-- 修改超时时间 -->
    <integer name="connect_timeout_seconds">20</integer>
    
    <!-- 启用/禁用日志 -->
    <bool name="enable_network_logging">true</bool>
</resources>
```

### 修改生产环境配置

编辑 `app/src/main/res/values/config_release.xml`：

```xml
<resources>
    <!-- 生产环境后端服务地址 -->
    <string name="base_url">https://api.beatu.com/</string>
    
    <!-- 生产环境关闭日志 -->
    <bool name="enable_network_logging">false</bool>
</resources>
```

## 使用不同配置的构建类型

### 当前配置

当前项目使用统一的 `config.xml` 文件。如果需要为不同的构建类型（debug/release）使用不同的配置：

1. 创建 `app/src/main/res/values-release/config.xml`（release 构建类型专用）
2. 在 `build.gradle.kts` 中配置 buildTypes：

```kotlin
buildTypes {
    debug {
        // 使用默认的 config.xml
    }
    release {
        isMinifyEnabled = false
        // 会自动使用 values-release/config.xml
    }
}
```

### 推荐做法

- **开发环境**: 使用 `config.xml`，启用日志，使用本地开发服务器地址
- **生产环境**: 使用 `config_release.xml`，关闭日志，使用生产服务器地址

## 配置读取

配置在 `NetworkModule.kt` 中自动读取：

```kotlin
@Provides
@Singleton
fun provideNetworkConfig(
    @ApplicationContext context: Context
): NetworkConfig {
    val baseUrl = context.getString(R.string.base_url)
    val connectTimeout = context.resources.getInteger(R.integer.connect_timeout_seconds).toLong()
    // ...
}
```

## 常见问题

### Q: 修改配置后不生效？
A: 需要重新编译并运行应用。如果使用 Android Studio，可以点击 "Sync Project with Gradle Files" 然后重新运行。

### Q: 如何查看当前使用的配置？
A: 可以在 `NetworkModule.kt` 中添加日志输出，或通过调试查看 `NetworkConfig` 对象的值。

### Q: 如何为不同的开发机器使用不同的配置？
A: 可以创建多个配置文件（如 `config_local.xml`, `config_team.xml`），然后通过 Git 忽略其中一个，或者使用 Gradle 的 flavor 功能。

### Q: IP 地址变化后如何快速修改？
A: 只需修改 `config.xml` 中的 `base_url` 值即可，无需修改代码。

## 安全注意事项

⚠️ **重要**：
- 生产环境的配置（`config_release.xml`）应使用 HTTPS 协议
- 不要在配置文件中硬编码敏感信息（如 API 密钥），应使用安全的存储方式
- 生产环境应关闭网络日志，避免泄露敏感信息

