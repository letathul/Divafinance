package com.divafinance.feature.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.divafinance.core.ui.theme.AccentTheme
import com.divafinance.core.ui.theme.DivaTheme
import com.divafinance.core.ui.theme.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SettingsScreenTest {

    // DivaTheme provides the design tokens through a CompositionLocal, so an unwrapped
    // screen throws rather than rendering. Every screen and every UI test wraps in it.
    @Test
    fun displaysTitle() = runComposeUiTest {
        setContent {
            DivaTheme { SettingsScreen() }
        }
        onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun showsTheAppearanceSection() = runComposeUiTest {
        setContent {
            DivaTheme { SettingsScreen() }
        }
        onNodeWithText("Appearance").assertIsDisplayed()
        ThemeMode.entries.forEach { onNodeWithText(it.label).assertIsDisplayed() }
        AccentTheme.entries.forEach { onNodeWithText(it.label).assertIsDisplayed() }
    }

    @Test
    fun pickingAThemeModeReportsIt() = runComposeUiTest {
        var picked: ThemeMode? = null
        setContent {
            DivaTheme {
                SettingsScreen(themeMode = ThemeMode.SYSTEM, onThemeModeChange = { picked = it })
            }
        }
        onNodeWithText("Dark").performClick()
        assertEquals(ThemeMode.DARK, picked)
    }

    @Test
    fun pickingAnAccentReportsIt() = runComposeUiTest {
        var picked: AccentTheme? = null
        setContent {
            DivaTheme {
                SettingsScreen(accent = AccentTheme.SUNSET, onAccentChange = { picked = it })
            }
        }
        onNodeWithText("Ocean").performClick()
        assertEquals(AccentTheme.OCEAN, picked)
    }
}
