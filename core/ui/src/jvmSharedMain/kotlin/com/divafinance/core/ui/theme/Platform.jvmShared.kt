package com.divafinance.core.ui.theme

/**
 * Android and the JVM test host share this actual.
 *
 * The `jvm` target is the stand-in for Android — it exists only so `runComposeUiTest` has
 * a host — so a default test run exercises the branch that actually ships on Android.
 * Cupertino coverage comes from passing `platform = CUPERTINO` to `DivaTheme` explicitly.
 */
actual fun currentPlatform(): DivaPlatform = DivaPlatform.MATERIAL
