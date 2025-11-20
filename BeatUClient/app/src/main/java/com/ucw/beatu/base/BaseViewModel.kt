package com.ucw.beatu.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Base ViewModel 类
 * 提供通用的 ViewModel 功能
 */
abstract class BaseViewModel<UiState> : ViewModel() {
    
    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    /**
     * 创建初始 UI 状态
     */
    protected abstract fun createInitialState(): UiState
    
    /**
     * 更新 UI 状态
     */
    protected fun updateState(update: UiState.() -> UiState) {
        _uiState.value = _uiState.value.update()
    }
    
    /**
     * 设置 UI 状态
     */
    protected fun setState(newState: UiState) {
        _uiState.value = newState
    }
    
    /**
     * 获取当前 UI 状态
     */
    protected fun getCurrentState(): UiState = _uiState.value
    
    /**
     * 在 ViewModelScope 中启动协程，自动处理异常
     */
    protected fun launch(
        errorHandler: CoroutineExceptionHandler? = null,
        block: suspend CoroutineScope.() -> Unit
    ) {
        val handler = errorHandler ?: CoroutineExceptionHandler { _, throwable ->
            handleError(throwable)
        }
        viewModelScope.launch(handler, block = block)
    }
    
    /**
     * 处理错误
     * 子类可以重写此方法自定义错误处理逻辑
     */
    protected open fun handleError(throwable: Throwable) {
        // 默认错误处理，可以记录日志或上报
        throwable.printStackTrace()
    }
}

