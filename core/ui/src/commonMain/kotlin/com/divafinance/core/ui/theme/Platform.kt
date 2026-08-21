package com.divafinance.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Which visual language the tree renders in.
 *
 * Deliberately not "which OS": the value is a [androidx.compose.runtime.CompositionLocal]
 * so a test — or a preview — can render either branch on any host. Every Compose UI test
 * in this repo runs on the `jvm` target, which resolves to [MATERIAL]; without an
 * overridable value the Cupertino half of the design system would be untestable.
 */
enum class DivaPlatform { MATERIAL, CUPERTINO }

/** The build target's natural language. Android and the JVM test host are [DivaPlatform.MATERIAL]. */
expect fun currentPlatform(): DivaPlatform

/**
 * Unlike [LocalDivaTokens] this does **not** throw when it is read outside [DivaTheme].
 * A component that escapes the theme should still pick a sane branch; one failure mode
 * for a missing theme is enough.
 */
val LocalDivaPlatform = staticCompositionLocalOf { currentPlatform() }

/** The branch test every adaptive component reads. */
val isCupertino: Boolean
    @Composable @ReadOnlyComposable get() = LocalDivaPlatform.current == DivaPlatform.CUPERTINO
