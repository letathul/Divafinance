package com.divafinance.core.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual class DispatcherProvider actual constructor() {
    actual val main: CoroutineDispatcher = Dispatchers.Main
    actual val io: CoroutineDispatcher = Dispatchers.IO
    actual val default: CoroutineDispatcher = Dispatchers.Default
}
