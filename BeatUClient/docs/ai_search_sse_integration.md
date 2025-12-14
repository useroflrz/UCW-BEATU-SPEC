# AI 搜索 SSE 流式传输对接说明

## 后端返回格式

后端通过 Server-Sent Events (SSE) 协议返回流式数据，格式如下：

```
data: {"chunkType": "answer", "content": "《", "isFinal": false}

data: {"chunkType": "answer", "content": "疯狂", "isFinal": false}

data: {"chunkType": "answer", "content": "动物", "isFinal": false}

data: {"chunkType": "answer", "content": "城", "isFinal": false}

data: {"chunkType": "answer", "content": "》是一部由迪士尼", "isFinal": false}

...
```

### Chunk 数据结构

```json
{
  "chunkType": "answer",  // 类型：answer, keywords, videoIds, localVideoIds, error
  "content": "文本内容",   // 内容（根据 chunkType 不同，格式不同）
  "isFinal": false        // 是否为最后一个 chunk
}
```

## 客户端对接流程

### 1. API 层：解析 SSE 流

**文件**：`business/search/data/src/main/java/com/ucw/beatu/business/search/data/api/AISearchApiService.kt`

```kotlin
// 1. 发送 HTTP POST 请求，Accept 头设置为 text/event-stream
val request = Request.Builder()
    .url(url)
    .post(jsonBody.toRequestBody("application/json".toMediaType()))
    .addHeader("Accept", "text/event-stream")
    .addHeader("Cache-Control", "no-cache")
    .build()

// 2. 读取响应流，逐行解析 SSE 格式
okHttpClient.newCall(request).execute().use { response ->
    BufferedReader(InputStreamReader(body.byteStream(), "UTF-8")).use { reader ->
        while (reader.readLine().also { line = it } != null) {
            when {
                currentLine.startsWith("data: ") -> {
                    // 提取 "data: " 后面的 JSON 字符串
                    val data = currentLine.substring(6)
                    // 使用 Moshi 解析 JSON 为 AISearchStreamChunk 对象
                    val chunk = chunkAdapter.fromJson(data)
                    chunk?.let { emit(it) }
                }
                // ... 处理其他 SSE 行（空行、event、id 等）
            }
        }
    }
}
```

**关键点**：
- 使用 `BufferedReader` 逐行读取 SSE 流
- 识别 `data: ` 前缀，提取 JSON 数据
- 使用 Moshi 将 JSON 解析为 `AISearchStreamChunk` 对象
- 通过 Flow 发送每个 chunk

### 2. Repository 层：转换数据模型

**文件**：`business/search/data/src/main/java/com/ucw/beatu/business/search/data/repository/AISearchRepositoryImpl.kt`

```kotlin
override fun searchStream(userQuery: String): Flow<AISearchResult> {
    return apiService.searchStream(userQuery)
        .map { chunk ->
            processChunk(chunk)  // 将 AISearchStreamChunk 转换为 AISearchResult
        }
}

private fun processChunk(chunk: AISearchStreamChunk): AISearchResult {
    return when (chunk.chunkType) {
        "answer" -> {
            AISearchResult(aiAnswer = chunk.content)  // 直接返回文本内容
        }
        "keywords" -> {
            val keywords = parseJsonArray(chunk.content)  // 解析 JSON 数组
            AISearchResult(keywords = keywords)
        }
        "videoIds" -> {
            val videoIds = parseJsonArrayToLong(chunk.content)
            AISearchResult(videoIds = videoIds)
        }
        "error" -> {
            AISearchResult(error = chunk.content)
        }
        // ...
    }
}
```

**关键点**：
- 根据 `chunkType` 处理不同类型的 chunk
- `answer` 类型直接返回文本内容，用于流式显示
- 其他类型（keywords、videoIds）需要解析 JSON 数组

### 3. ViewModel 层：累积状态

**文件**：`business/search/presentation/src/main/java/com/ucw/beatu/business/search/presentation/viewmodel/AISearchViewModel.kt`

```kotlin
fun search(userQuery: String) {
    searchJob = repository.searchStream(userQuery)
        .onEach { result ->
            _uiState.update { currentState ->
                currentState.copy(
                    // ✅ 流式累积：每次收到新的 answer chunk，追加到现有文本
                    aiAnswer = if (result.aiAnswer.isNotEmpty()) {
                        currentState.aiAnswer + result.aiAnswer
                    } else {
                        currentState.aiAnswer
                    },
                    isLoading = false,
                    error = result.error ?: currentState.error
                )
            }
        }
        .launchIn(viewModelScope)
}
```

**关键点**：
- 使用 `StateFlow` 管理 UI 状态
- 每次收到新的 `answer` chunk，通过 `currentState.aiAnswer + result.aiAnswer` 累积文本
- UI 会自动响应状态变化，实时更新显示

### 4. UI 层：实时显示

**文件**：`business/search/presentation/src/main/java/com/ucw/beatu/business/search/presentation/ui/SearchResultFragment.kt`

```kotlin
private fun observeAISearchViewModel() {
    viewLifecycleOwner.lifecycleScope.launch {
        aiSearchViewModel.uiState.collect { state ->
            updateAISearchUI(state)
        }
    }
}

private fun updateAISearchUI(state: AISearchUiState) {
    when {
        state.aiAnswer.isNotEmpty() -> {
            // ✅ 实时更新文本：每次 StateFlow 更新，TextView 自动刷新
            aiAnswerText.text = state.aiAnswer
            aiAnswerText.isVisible = true
            aiLoadingProgress.isVisible = false
        }
        state.isLoading -> {
            aiLoadingProgress.isVisible = true
        }
        state.error != null -> {
            aiErrorText.text = state.error
            aiErrorText.isVisible = true
        }
    }
}
```

**关键点**：
- 通过 `collect` 观察 `StateFlow`，自动响应状态变化
- 每次 `aiAnswer` 更新，`TextView` 自动刷新，实现流式显示效果

## 数据流图

```
后端 SSE 流
  ↓
data: {"chunkType": "answer", "content": "《", "isFinal": false}
  ↓
AISearchApiService (解析 SSE，提取 JSON)
  ↓
AISearchStreamChunk(chunkType="answer", content="《")
  ↓
AISearchRepositoryImpl (转换数据模型)
  ↓
AISearchResult(aiAnswer="《")
  ↓
AISearchViewModel (累积状态: "" + "《" = "《")
  ↓
StateFlow<AISearchUiState> (通知 UI)
  ↓
SearchResultFragment (更新 TextView: "《")
  ↓
用户看到流式文本显示
```

## 关键实现细节

### 1. SSE 格式解析

SSE 协议格式：
- `data: <JSON>` - 数据行
- 空行 - 事件分隔符
- `event: <type>` - 事件类型（可选）
- `id: <id>` - 事件 ID（可选）

客户端只处理 `data: ` 行，忽略其他行。

### 2. 流式文本累积

```kotlin
// ✅ 正确：累积文本
aiAnswer = currentState.aiAnswer + result.aiAnswer

// ❌ 错误：覆盖文本
aiAnswer = result.aiAnswer
```

### 3. 错误处理

- 网络错误：在 `AISearchApiService` 的 `catch` 块中处理
- 解析错误：在 JSON 解析的 `try-catch` 中处理
- 业务错误：后端返回 `error` 类型的 chunk

### 4. 生命周期管理

- 使用 `viewModelScope` 管理协程生命周期
- 使用 `viewLifecycleOwner.lifecycleScope` 管理 UI 观察者
- 搜索时取消之前的任务：`searchJob?.cancel()`

## 测试验证

### 后端日志
```
开始处理 AI 搜索请求: user_query=疯狂动物城
开始流式生成回答: user_query=疯狂动物城
发送 chunk #1: {"chunkType": "answer", "content": "《", "isFinal": false}
发送 chunk #2: {"chunkType": "answer", "content": "疯狂", "isFinal": false}
...
```

### 客户端日志
```
🔍 开始 AI 搜索请求: query=疯狂动物城
🔍 响应状态码: 200, 是否成功: true
🔍 开始读取 SSE 流
🔍 读取第 1 行: data: {"chunkType": "answer", "content": "《", "isFinal": false}
✅ 成功解析 chunk: type=answer, content length=1, isFinal=false
处理 chunk: type=answer, content=《
...
```

## 常见问题

### Q1: 为什么文本没有实时显示？

**A**: 检查以下几点：
1. 确认 `StateFlow` 的观察者已正确设置（`collect`）
2. 确认 ViewModel 中正确累积文本（`currentState.aiAnswer + result.aiAnswer`）
3. 确认 UI 更新在主线程（`StateFlow` 默认在主线程）

### Q2: 为什么收到多个 chunk 但只显示最后一个？

**A**: 检查 ViewModel 中的累积逻辑，确保使用 `+` 而不是直接赋值。

### Q3: 为什么解析失败？

**A**: 检查以下几点：
1. 确认后端返回的 JSON 格式正确
2. 确认 `AISearchStreamChunk` 的字段名与 JSON 匹配（使用 `@Json` 注解）
3. 查看日志中的 "❌ 解析 SSE 数据失败" 错误信息

## 总结

客户端已完整实现 SSE 流式传输的对接：
1. ✅ **API 层**：正确解析 SSE 格式，提取 JSON 数据
2. ✅ **Repository 层**：正确转换数据模型
3. ✅ **ViewModel 层**：正确累积流式文本
4. ✅ **UI 层**：正确实时更新显示

整个流程符合响应式编程范式，使用 Kotlin Flow + StateFlow 实现数据流的自动传播和 UI 的自动更新。

