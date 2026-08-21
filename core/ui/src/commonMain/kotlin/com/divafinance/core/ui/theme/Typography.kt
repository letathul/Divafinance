package com.divafinance.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/*
 * No custom face is bundled, so everything renders in the platform default — Roboto on
 * Android, SF on iOS. That is the right translation of "interface in the native face"
 * rather than a compromise. What differs between the two is the *scale*: SF's interface
 * sizes (17pt body, 34pt large title) are not Material's, and using one platform's
 * metrics on the other is most of what makes a cross-platform app look wrong.
 */

/**
 * Every figure in the app: digits share one advance width so a column of amounts stays
 * aligned as values change.
 *
 * Platform-dependent, and this is not cosmetic. `FontFamily.Monospace` resolves to
 * **Courier** on iOS, which is jarring next to SF and the most obviously non-native
 * thing that can appear on the screen. SF supports the `tnum` feature directly, so the
 * Cupertino branch gets tabular figures in the interface face instead.
 *
 * This is a composable getter rather than a `val` so it can read the platform. Every
 * call site is already inside a composable — including the two that use it as a default
 * argument — so the change costs no edits.
 */
val NumericStyle: TextStyle
    @Composable @ReadOnlyComposable get() = when (LocalDivaPlatform.current) {
        DivaPlatform.MATERIAL -> MaterialNumericStyle
        DivaPlatform.CUPERTINO -> CupertinoNumericStyle
    }

private val MaterialNumericStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    lineHeight = 18.sp,
    fontFeatureSettings = "tnum",
)

private val CupertinoNumericStyle = TextStyle(
    fontWeight = FontWeight.Medium,
    fontSize = 15.sp,
    lineHeight = 20.sp,
    fontFeatureSettings = "tnum",
)

internal fun divaTypography(platform: DivaPlatform): Typography = when (platform) {
    DivaPlatform.MATERIAL -> MaterialTypography
    DivaPlatform.CUPERTINO -> CupertinoTypography
}

private val MaterialTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 44.sp,
        lineHeight = 50.sp,
        letterSpacing = (-1.4).sp,
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-1.0).sp,
    ),
    displaySmall = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.7).sp,
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    // The eyebrow above a figure: mono, uppercased at the call site, wide-tracked.
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.9.sp,
    ),
)

/*
 * SF's interface metrics. `displaySmall` is the 34pt large title — the size a screen
 * title is drawn at before it scrolls away — and `titleLarge` is the 17pt centred title
 * it collapses to. `bodyLarge` at 17pt is the body size for everything.
 */
private val CupertinoTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.8).sp,
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.7).sp,
    ),
    // The large title.
    displaySmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 41.sp,
        letterSpacing = (-0.7).sp,
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 23.sp,
        letterSpacing = (-0.2).sp,
    ),
    // The collapsed nav-bar title.
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp),
    // A list row's title.
    titleSmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 20.sp),
    // A row's grey subtitle.
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 21.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    // Kept mono in both branches: `Meta()` is the eyebrow above a figure, and half a
    // dozen screens rely on it reading as a label rather than as text.
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.9.sp,
    ),
)
