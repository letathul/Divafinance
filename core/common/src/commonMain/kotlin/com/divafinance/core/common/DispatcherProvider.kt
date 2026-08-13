package com.divafinance.core.common

import kotlinx.coroutines.CoroutineDispatcher

expect class DispatcherProvider() {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}
