package com.divafinance.core.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * Installs a Main dispatcher for tests that touch a ViewModel.
 *
 * `viewModelScope` dispatches on Main, and neither the desktop Compose test host nor a
 * plain JVM unit test installs one. Without this, every `stateIn(WhileSubscribed)` flow
 * silently stays on its initial value and screen assertions fail with an opaque timeout.
 *
 * [UnconfinedTestDispatcher] rather than `StandardTestDispatcher` so upstream flows emit
 * eagerly on subscription — a UI test asserts on rendered output and has no scheduler to
 * advance.
 *
 * Pair with [resetTestMainDispatcher]:
 * ```
 * @BeforeTest fun setUp() = installTestMainDispatcher()
 * @AfterTest fun tearDown() = resetTestMainDispatcher()
 * ```
 * (These are functions rather than a base class because `kotlin.test`'s annotations are
 * only wired into test compilations, and this is a `main` source set.)
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun installTestMainDispatcher() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
}

/** Undoes [installTestMainDispatcher]. Always call from an `@AfterTest`. */
fun resetTestMainDispatcher() {
    Dispatchers.resetMain()
}
