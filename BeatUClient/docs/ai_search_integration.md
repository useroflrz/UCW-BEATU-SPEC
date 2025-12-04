# AI 搜索接口集成指南（Kotlin 客户端）

## 📋 概述

本文档提供在 Kotlin 客户端中集成 AI 搜索流式接口的完整示例，包括数据模型、网络请求、Repository、ViewModel 和 UI 层的实现。

## 🔗 接口信息

- **接口路径**: `POST /api/ai/search/stream`
- **请求格式**: JSON
- **响应格式**: Server-Sent Events (SSE)
- **Content-Type**: `text/event-stream`

### 请求体

```json
{
  "userQuery": "我想看一些搞笑视频"
}
```

### 响应格式（SSE）

```
data: {"chunkType": "answer", "content": "我", "isFinal": false}

data: {"chunkType": "answer", "content": "为", "isFinal": false}

data: {"chunkType": "answer", "content": "", "isFinal": true}

data: {"chunkType": "keywords", "content": "[\"搞笑\", \"视频\"]", "isFinal": true}

data: {"chunkType": "videoIds", "content": "[\"video_001\", \"video_002\"]", "isFinal": true}

data: {"chunkType": "localVideoIds", "content": "[\"local_001\"]", "isFinal": true}
```

## 📦 一、数据模型定义

### 1.1 请求模型

**位置**: `business/search/data/src/main/java/com/ucw/beatu/business/search/data/api/dto/AISearchRequest.kt`

```kotlin
package com.ucw.beatu.business.search.data.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * AI 搜索请求
 */
@JsonClass(generateAdapter = true)
data class AISearchRequest(
    @Json(name = "userQuery")
    val userQuery: String
)
```

### 1.2 响应数据块模型

**位置**: `business/search/data/src/main/java/com/ucw/beatu/business/search/data/api/dto/AISearchStreamChunk.kt`

```kotlin
package com.ucw.beatu.business.search.data.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * AI 搜索流式数据块
 */
@JsonClass(generateAdapter = true)
data class AISearchStreamChunk(
    @Json(name = "chunkType")
    val chunkType: String, // answer, keywords, videoIds, localVideoIds, error
    
    @Json(name = "content")
    val content: String,
    
    @Json(name = "isFinal")
    val isFinal: Boolean
)

/**
 * 数据块类型枚举
 */
enum class ChunkType {
    ANSWER,
    KEYWORDS,
    VIDEO_IDS,
    LOCAL_VIDEO_IDS,
    ERROR
}
```

### 1.3 搜索结果模型

**位置**: `business/search/domain/src/main/java/com/ucw/beatu/business/search/domain/model/AISearchResult.kt`

```kotlin
package com.ucw.beatu.business.search.domain.model

/**
 * AI 搜索结果
 */
data class AISearchResult(
    val aiAnswer: String = "",
    val keywords: List<String> = emptyList(),
    val videoIds: List<String> = emptyList(),
    val localVideoIds: List<String> = emptyList(),
    val error: String? = null
)
```

## 🌐 二、网络层实现

### 2.1 SSE 流式客户端

由于 Retrofit 不支持 SSE 流式传输，我们需要使用 OkHttp 直接处理。

**位置**: `business/search/data/src/main/java/com/ucw/beatu/business/search/data/api/AISearchApiService.kt`

```kotlin
package com.ucw.beatu.business.search.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.ucw.beatu.business.search.data.api.dto.AISearchRequest
import com.ucw.beatu.business.search.data.api.dto.AISearchStreamChunk
import com.ucw.beatu.shared.network.config.NetworkConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

/**
 * AI 搜索 API 服务
 * 使用 OkHttp 处理 SSE 流式传输
 */
class AISearchApiService @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val networkConfig: NetworkConfig
) {
    
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    
    private val requestAdapter = moshi.adapter(AISearchRequest::class.java)
    private val chunkAdapter = moshi.adapter(AISearchStreamChunk::class.java)
    
    /**
     * 执行 AI 搜索（流式）
     * 
     * @param userQuery 用户查询文本
     * @return Flow<AISearchStreamChunk> 流式数据块
     */
    fun searchStream(userQuery: String): Flow<AISearchStreamChunk> = flow {
        val requestBody = AISearchRequest(userQuery = userQuery)
        val jsonBody = requestAdapter.toJson(requestBody)
        
        val request = Request.Builder()
            .url("${networkConfig.baseUrl}api/ai/search/stream")
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .addHeader("Accept", "text/event-stream")
            .addHeader("Cache-Control", "no-cache")
            .build()
        
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                emit(
                    AISearchStreamChunk(
                        chunkType = "error",
                        content = "请求失败: ${response.code} ${response.message}",
                        isFinal = true
                    )
                )
                return@flow
            }
            
            response.body?.let { body ->
                BufferedReader(InputStreamReader(body.byteStream())).use { reader ->
                    var line: String?
                    var currentData = StringBuilder()
                    
                    while (reader.readLine().also { line = it } != null) {
                        line?.let { currentLine ->
                            when {
                                currentLine.startsWith("data: ") -> {
                                    // 解析 SSE 数据
                                    val data = currentLine.substring(6) // 移除 "data: " 前缀
                                    try {
                                        val chunk = chunkAdapter.fromJson(data)
                                        chunk?.let { emit(it) }
                                    } catch (e: Exception) {
                                        // 解析失败，发送错误块
                                        emit(
                                            AISearchStreamChunk(
                                                chunkType = "error",
                                                content = "解析数据失败: ${e.message}",
                                                isFinal = true
                                            )
                                        )
                                    }
                                }
                                currentLine.isEmpty() -> {
                                    // 空行表示一个事件结束
                                    currentData.clear()
                                }
                            }
                        }
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)
}
```

## 📚 三、Repository 层实现

**位置**: `business/search/data/src/main/java/com/ucw/beatu/business/search/data/repository/AISearchRepositoryImpl.kt`

```kotlin
package com.ucw.beatu.business.search.data.repository

import com.ucw.beatu.business.search.data.api.AISearchApiService
import com.ucw.beatu.business.search.data.api.dto.AISearchStreamChunk
import com.ucw.beatu.business.search.domain.model.AISearchResult
import com.ucw.beatu.business.search.domain.repository.AISearchRepository
import com.ucw.beatu.shared.common.logger.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import javax.inject.Inject

/**
 * AI 搜索 Repository 实现
 */
class AISearchRepositoryImpl @Inject constructor(
    private val apiService: AISearchApiService
) : AISearchRepository {
    
    private val logger = AppLogger.get("AISearchRepository")
    
    override fun searchStream(userQuery: String): Flow<AISearchResult> {
        return apiService.searchStream(userQuery)
            .map { chunk ->
                processChunk(chunk)
            }
    }
    
    /**
     * 处理数据块，转换为搜索结果
     */
    private fun processChunk(chunk: AISearchStreamChunk): AISearchResult {
        return when (chunk.chunkType) {
            "answer" -> {
                AISearchResult(aiAnswer = chunk.content)
            }
            "keywords" -> {
                try {
                    val keywords = parseJsonArray(chunk.content)
                    AISearchResult(keywords = keywords)
                } catch (e: Exception) {
                    logger.e("解析关键词失败", e)
                    AISearchResult()
                }
            }
            "videoIds" -> {
                try {
                    val videoIds = parseJsonArray(chunk.content)
                    AISearchResult(videoIds = videoIds)
                } catch (e: Exception) {
                    logger.e("解析视频 ID 失败", e)
                    AISearchResult()
                }
            }
            "localVideoIds" -> {
                try {
                    val localVideoIds = parseJsonArray(chunk.content)
                    AISearchResult(localVideoIds = localVideoIds)
                } catch (e: Exception) {
                    logger.e("解析本地视频 ID 失败", e)
                    AISearchResult()
                }
            }
            "error" -> {
                AISearchResult(error = chunk.content)
            }
            else -> {
                AISearchResult()
            }
        }
    }
    
    /**
     * 解析 JSON 数组字符串
     */
    private fun parseJsonArray(jsonString: String): List<String> {
        return try {
            val jsonArray = JSONArray(jsonString)
            (0 until jsonArray.length()).map { jsonArray.getString(it) }
        } catch (e: Exception) {
            logger.e("解析 JSON 数组失败: $jsonString", e)
            emptyList()
        }
    }
}
```

### 3.1 Repository 接口定义

**位置**: `business/search/domain/src/main/java/com/ucw/beatu/business/search/domain/repository/AISearchRepository.kt`

```kotlin
package com.ucw.beatu.business.search.domain.repository

import com.ucw.beatu.business.search.domain.model.AISearchResult
import kotlinx.coroutines.flow.Flow

/**
 * AI 搜索 Repository 接口
 */
interface AISearchRepository {
    /**
     * 执行 AI 搜索（流式）
     * 
     * @param userQuery 用户查询文本
     * @return Flow<AISearchResult> 流式搜索结果
     */
    fun searchStream(userQuery: String): Flow<AISearchResult>
}
```

## 🎯 四、ViewModel 实现

**位置**: `business/search/presentation/src/main/java/com/ucw/beatu/business/search/presentation/viewmodel/AISearchViewModel.kt`

```kotlin
package com.ucw.beatu.business.search.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ucw.beatu.business.search.domain.model.AISearchResult
import com.ucw.beatu.business.search.domain.repository.AISearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AI 搜索 UI 状态
 */
data class AISearchUiState(
    val aiAnswer: String = "",
    val keywords: List<String> = emptyList(),
    val videoIds: List<String> = emptyList(),
    val localVideoIds: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * AI 搜索 ViewModel
 */
@HiltViewModel
class AISearchViewModel @Inject constructor(
    private val repository: AISearchRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AISearchUiState())
    val uiState: StateFlow<AISearchUiState> = _uiState.asStateFlow()
    
    private var searchJob: Job? = null
    
    /**
     * 执行搜索
     */
    fun search(userQuery: String) {
        // 取消之前的搜索
        searchJob?.cancel()
        
        // 重置状态
        _uiState.value = AISearchUiState(
            isLoading = true,
            error = null
        )
        
        // 开始新的搜索
        searchJob = repository.searchStream(userQuery)
            .onEach { result ->
                // 累积更新状态
                _uiState.update { currentState ->
                    currentState.copy(
                        aiAnswer = if (result.aiAnswer.isNotEmpty()) {
                            // 流式累积 AI 回答
                            currentState.aiAnswer + result.aiAnswer
                        } else {
                            currentState.aiAnswer
                        },
                        keywords = result.keywords.ifEmpty { currentState.keywords },
                        videoIds = result.videoIds.ifEmpty { currentState.videoIds },
                        localVideoIds = result.localVideoIds.ifEmpty { currentState.localVideoIds },
                        isLoading = false,
                        error = result.error ?: currentState.error
                    )
                }
            }
            .catch { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "搜索失败: ${e.message}"
                )
            }
            .launchIn(viewModelScope)
    }
    
    /**
     * 清除搜索结果
     */
    fun clear() {
        searchJob?.cancel()
        _uiState.value = AISearchUiState()
    }
    
    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
    }
}

// StateFlow 扩展函数，用于更新状态
private fun <T> MutableStateFlow<T>.update(update: (T) -> T) {
    value = update(value)
}
```

## 🎨 五、UI 层使用示例

### 5.1 Fragment 实现

**位置**: `business/search/presentation/src/main/java/com/ucw/beatu/business/search/presentation/ui/AiSearchFragment.kt`

```kotlin
package com.ucw.beatu.business.search.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.ucw.beatu.business.search.presentation.viewmodel.AISearchViewModel
import com.ucw.beatu.databinding.FragmentAiSearchBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AiSearchFragment : Fragment() {
    
    private var _binding: FragmentAiSearchBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AISearchViewModel by viewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAiSearchBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViews()
        observeViewModel()
    }
    
    private fun setupViews() {
        // 发送按钮点击事件
        binding.btnSend.setOnClickListener {
            val query = binding.etQuery.text?.toString()?.trim()
            if (!query.isNullOrEmpty()) {
                viewModel.search(query)
                binding.etQuery.text?.clear()
            }
        }
        
        // 清除按钮点击事件
        binding.btnClear.setOnClickListener {
            viewModel.clear()
        }
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                // 更新 AI 回答显示
                binding.tvAiAnswer.text = state.aiAnswer
                
                // 更新关键词显示
                if (state.keywords.isNotEmpty()) {
                    binding.tvKeywords.text = "关键词: ${state.keywords.joinToString(", ")}"
                    binding.tvKeywords.visibility = View.VISIBLE
                } else {
                    binding.tvKeywords.visibility = View.GONE
                }
                
                // 更新视频 ID 显示（示例，实际应该显示视频列表）
                if (state.videoIds.isNotEmpty()) {
                    binding.tvVideoIds.text = "找到 ${state.videoIds.size} 个视频"
                    binding.tvVideoIds.visibility = View.VISIBLE
                } else {
                    binding.tvVideoIds.visibility = View.GONE
                }
                
                // 更新加载状态
                binding.progressBar.visibility = 
                    if (state.isLoading) View.VISIBLE else View.GONE
                
                // 显示错误信息
                state.error?.let { error ->
                    // 显示错误提示（可以使用 Snackbar 等）
                    binding.tvError.text = error
                    binding.tvError.visibility = View.VISIBLE
                } ?: run {
                    binding.tvError.visibility = View.GONE
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```

### 5.2 布局文件示例

**位置**: `business/search/presentation/src/main/res/layout/fragment_ai_search.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <!-- 输入区域 -->
    <EditText
        android:id="@+id/et_query"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="输入您想搜索的内容..."
        android:inputType="text" />

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal">

        <Button
            android:id="@+id/btn_send"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="搜索" />

        <Button
            android:id="@+id/btn_clear"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="清除" />
    </LinearLayout>

    <!-- 加载指示器 -->
    <ProgressBar
        android:id="@+id/progress_bar"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:visibility="gone" />

    <!-- AI 回答显示 -->
    <TextView
        android:id="@+id/tv_ai_answer"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp"
        android:textSize="16sp"
        android:text="AI 回答将在这里显示..." />

    <!-- 关键词显示 -->
    <TextView
        android:id="@+id/tv_keywords"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:textSize="14sp"
        android:visibility="gone" />

    <!-- 视频 ID 显示 -->
    <TextView
        android:id="@+id/tv_video_ids"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:textSize="14sp"
        android:visibility="gone" />

    <!-- 错误信息显示 -->
    <TextView
        android:id="@+id/tv_error"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:textColor="@android:color/holo_red_dark"
        android:textSize="14sp"
        android:visibility="gone" />

</LinearLayout>
```

## 🔧 六、依赖注入配置

### 6.1 创建 Hilt 模块

**位置**: `business/search/data/src/main/java/com/ucw/beatu/business/search/di/SearchModule.kt`

```kotlin
package com.ucw.beatu.business.search.di

import com.ucw.beatu.business.search.data.api.AISearchApiService
import com.ucw.beatu.business.search.data.repository.AISearchRepositoryImpl
import com.ucw.beatu.business.search.domain.repository.AISearchRepository
import com.ucw.beatu.shared.network.config.NetworkConfig
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SearchModule {
    
    @Binds
    @Singleton
    abstract fun bindAISearchRepository(
        impl: AISearchRepositoryImpl
    ): AISearchRepository
}

@Module
@InstallIn(SingletonComponent::class)
object SearchApiModule {
    
    @Provides
    @Singleton
    fun provideAISearchApiService(
        okHttpClient: OkHttpClient,
        networkConfig: NetworkConfig
    ): AISearchApiService {
        return AISearchApiService(okHttpClient, networkConfig)
    }
}
```

## 📝 七、使用流程总结

1. **用户输入查询** → 点击搜索按钮
2. **ViewModel 调用 Repository** → `viewModel.search(userQuery)`
3. **Repository 调用 API Service** → `repository.searchStream(userQuery)`
4. **API Service 使用 OkHttp** → 发送 POST 请求，接收 SSE 流
5. **解析 SSE 数据块** → 转换为 `AISearchStreamChunk`
6. **Repository 处理数据块** → 转换为 `AISearchResult`
7. **ViewModel 更新状态** → 累积更新 `AISearchUiState`
8. **UI 观察状态变化** → 实时显示 AI 回答、关键词、视频 ID

## ⚠️ 注意事项

1. **流式数据处理**：AI 回答是流式输出的，需要累积显示，而不是替换
2. **错误处理**：网络错误、解析错误等都需要妥善处理
3. **资源释放**：在 Fragment/Activity 销毁时，记得取消搜索任务
4. **线程切换**：网络请求在 IO 线程，UI 更新在主线程
5. **SSE 格式**：确保正确解析 SSE 格式（`data: {...}\n\n`）

## 🔍 调试建议

1. **日志记录**：在关键位置添加日志，追踪数据流
2. **网络拦截器**：使用 OkHttp 的日志拦截器查看请求和响应
3. **状态观察**：使用 Android Studio 的 LiveData/StateFlow 观察工具
4. **错误捕获**：在 Flow 的 `catch` 中记录详细错误信息

## 📚 相关文档

- [后端接口文档](../../BeatUBackend/interface_contract.md)
- [数据层架构文档](data-layer-architecture.md)
- [后端集成检查清单](backend_integration_checklist.md)

