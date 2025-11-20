package com.ucw.beatu.core.common.result

sealed class AppResult<out T> {
    data class Success<T>(val data: T, val metadata: Map<String, Any?> = emptyMap()) : AppResult<T>()
    data class Error(
        val throwable: Throwable,
        val code: Int? = null,
        val message: String? = throwable.message
    ) : AppResult<Nothing>()
    data object Loading : AppResult<Nothing>()

    inline fun <R> map(transform: (T) -> R): AppResult<R> = when (this) {
        is Success -> Success(transform(data), metadata)
        is Error -> this
        Loading -> Loading
    }

    inline fun onSuccess(block: (T) -> Unit): AppResult<T> = apply {
        if (this is Success) block(data)
    }

    inline fun onError(block: (Throwable) -> Unit): AppResult<T> = apply {
        if (this is Error) block(throwable)
    }

    inline fun onLoading(block: () -> Unit): AppResult<T> = apply {
        if (this is Loading) block()
    }
}

suspend inline fun <T> runAppResult(crossinline block: suspend () -> T): AppResult<T> =
    try {
        AppResult.Success(block())
    } catch (t: Throwable) {
        AppResult.Error(t)
    }
