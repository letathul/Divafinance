package com.divafinance.core.common

sealed class DivaResult<out T> {
    data class Success<T>(val data: T) : DivaResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : DivaResult<Nothing>()
    data object Loading : DivaResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = (this as? Success)?.data

    fun <R> map(transform: (T) -> R): DivaResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
        is Loading -> Loading
    }
}
