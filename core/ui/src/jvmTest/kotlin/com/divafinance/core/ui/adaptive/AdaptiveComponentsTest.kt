package com.divafinance.core.ui.adaptive

import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import com.divafinance.core.ui.theme.DivaPlatform
import com.divafinance.core.ui.theme.DivaTheme
import com.divafinance.core.ui.theme.ThemeMode
import com.divafinance.core.ui.component.DivaCalendar
import com.divafinance.core.ui.theme.currentPlatform
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Every case runs **twice**, once per [DivaPlatform].
 *
 * This host is the `jvm` target, which resolves to `MATERIAL`, so without explicitly
 * passing the other value the Cupertino half of the design system would never be
 * exercised anywhere. Retrofitting that later does not happen — so each assertion is
 * written against both branches from the start.
 */
@OptIn(ExperimentalTestApi::class)
class AdaptiveComponentsTest {

    private val platforms = DivaPlatform.entries

    @Test
    fun theJvmHostRendersAsMaterial() {
        assertEquals(DivaPlatform.MATERIAL, currentPlatform())
    }

    @Test
    fun scaffoldShowsItsTitleAndBackAffordance() = platforms.forEach { platform ->
        runComposeUiTest {
            var backs = 0
            setContent {
                DivaTheme(mode = ThemeMode.LIGHT, platform = platform) {
                    DivaScaffold(title = "Cards", onBack = { backs++ }) { Text("body") }
                }
            }
            onNodeWithText("Cards").assertIsDisplayed()
            // Spelled out identically in both branches so a selector never has to know
            // which one it is looking at.
            onNodeWithContentDescription("Back").performClick()
            assertEquals(1, backs, "back did not fire on $platform")
        }
    }

    @Test
    fun listScaffoldEmitsItsLargeTitleExactlyOnce() = platforms.forEach { platform ->
        runComposeUiTest {
            setContent {
                DivaTheme(mode = ThemeMode.LIGHT, platform = platform) {
                    DivaListScaffold(title = "Feed") { item { Text("first row") } }
                }
            }
            // Cupertino draws it in the list, Material in the app bar — either way there
            // is one of it, which is what stops the two branches doubling up.
            onNodeWithText("Feed").assertIsDisplayed()
            onNodeWithText("first row").assertIsDisplayed()
        }
    }

    @Test
    fun switchCarriesToggleSemanticsInBothBranches() = platforms.forEach { platform ->
        runComposeUiTest {
            var checked = false
            setContent {
                DivaTheme(mode = ThemeMode.LIGHT, platform = platform) {
                    DivaListRow(
                        title = "PIN protection",
                        trailing = { DivaSwitch(checked, onCheckedChange = { checked = it }) },
                    )
                }
            }
            // The Cupertino switch is hand-drawn; without an explicit `toggleable` with
            // Role.Switch these assertions would silently stop matching it.
            onNodeWithText("PIN protection").assertIsDisplayed()
            onNode(hasSwitchRole()).assertIsOff()
            onNode(hasSwitchRole()).performClick()
            assertTrue(checked, "switch did not toggle on $platform")
        }
    }

    @Test
    fun switchReportsItsOnState() = platforms.forEach { platform ->
        runComposeUiTest {
            setContent {
                DivaTheme(mode = ThemeMode.LIGHT, platform = platform) {
                    DivaSwitch(checked = true, onCheckedChange = {})
                }
            }
            onNode(hasSwitchRole()).assertIsOn()
        }
    }

    @Test
    fun chipFiresBothItsGestures() = platforms.forEach { platform ->
        runComposeUiTest {
            var taps = 0
            var holds = 0
            setContent {
                DivaTheme(mode = ThemeMode.LIGHT, platform = platform) {
                    DivaChip("Today", onClick = { taps++ }, onLongClick = { holds++ })
                }
            }
            onNodeWithText("Today").performClick()
            onNodeWithText("Today").performTouchInput { longClick() }
            assertEquals(1, taps, "tap did not fire on $platform")
            // The add-expense screen offers a fuller choice behind a hold; losing this
            // would make that unreachable without any compile error to warn us.
            assertEquals(1, holds, "hold did not fire on $platform")
        }
    }

    @Test
    fun tabBarExposesBothTabsAndTheComposeButton() = platforms.forEach { platform ->
        runComposeUiTest {
            var selected: DivaTab? = null
            var composed = 0
            setContent {
                DivaTheme(mode = ThemeMode.LIGHT, platform = platform) {
                    DivaTabBar(
                        selected = DivaTab.FEED,
                        onSelect = { selected = it },
                        onCompose = { composed++ },
                    )
                }
            }
            onNodeWithText("Feed").assertIsDisplayed()
            onNodeWithText("You").performClick()
            assertEquals(DivaTab.YOU, selected, "tab select failed on $platform")

            onNodeWithContentDescription("Add transaction").performClick()
            assertEquals(1, composed, "compose button did not fire on $platform")
        }
    }

    @Test
    fun groupedSectionRendersItsHeaderAndRows() = platforms.forEach { platform ->
        runComposeUiTest {
            setContent {
                DivaTheme(mode = ThemeMode.LIGHT, platform = platform) {
                    DivaGroupedSection(header = "Everything else", footer = "Local to this device") {
                        DivaListRow(title = "Cards", showChevron = true)
                        DivaRowDivider()
                        DivaListRow(title = "Graphs", value = "3")
                    }
                }
            }
            // The header is uppercased on Cupertino, so match it case-insensitively.
            onNodeWithText("Everything else", ignoreCase = true).assertIsDisplayed()
            onNodeWithText("Cards").assertIsDisplayed()
            onNodeWithText("Graphs").assertIsDisplayed()
            onNodeWithText("3").assertIsDisplayed()
            onNodeWithText("Local to this device").assertIsDisplayed()
        }
    }

    // --- the bottom sheet ----------------------------------------------------

    @Test
    fun bottomSheetShowsItsContentOnBothPlatforms() = platforms.forEach { platform ->
        runComposeUiTest {
            setContent {
                DivaTheme(mode = ThemeMode.LIGHT, platform = platform) {
                    DivaBottomSheet(onDismissRequest = {}) { Text("Select Date") }
                }
            }
            onNodeWithText("Select Date").assertIsDisplayed()
        }
    }

    // --- the calendar --------------------------------------------------------

    @Test
    fun calendarShowsTheMonthItIsAskedFor() = platforms.forEach { platform ->
        runComposeUiTest {
            setContent {
                DivaTheme(mode = ThemeMode.LIGHT, platform = platform) {
                    DivaCalendar(
                        selected = LocalDate(2026, 8, 24),
                        displayedMonth = LocalDate(2026, 8, 24),
                        onMonthChange = {},
                        onSelect = {},
                    )
                }
            }
            onNodeWithText("August 2026").assertIsDisplayed()
            onNodeWithText("31").assertIsDisplayed()
        }
    }

    @Test
    fun calendarSelectsTheDayThatWasTapped() = platforms.forEach { platform ->
        runComposeUiTest {
            var picked: LocalDate? = null
            setContent {
                DivaTheme(mode = ThemeMode.LIGHT, platform = platform) {
                    DivaCalendar(
                        selected = LocalDate(2026, 8, 24),
                        displayedMonth = LocalDate(2026, 8, 24),
                        onMonthChange = {},
                        onSelect = { picked = it },
                    )
                }
            }
            onNodeWithContentDescription("2026-08-12").performClick()
            assertEquals(LocalDate(2026, 8, 12), picked, "wrong day on $platform")
        }
    }

    /** A transaction that has not happened yet is not a record, so the future is inert. */
    @Test
    fun calendarRefusesDatesAfterItsMaximum() = platforms.forEach { platform ->
        runComposeUiTest {
            var picked: LocalDate? = null
            setContent {
                DivaTheme(mode = ThemeMode.LIGHT, platform = platform) {
                    DivaCalendar(
                        selected = LocalDate(2026, 8, 24),
                        displayedMonth = LocalDate(2026, 8, 24),
                        onMonthChange = {},
                        onSelect = { picked = it },
                        maxDate = LocalDate(2026, 8, 24),
                    )
                }
            }
            // Still drawn, so the shape of the month stays readable — just not tappable.
            onNodeWithContentDescription("2026-08-30").assertIsDisplayed().performClick()
            assertEquals(null, picked, "a future date was selectable on $platform")
        }
    }

    @Test
    fun calendarPagesToTheAdjacentMonth() = platforms.forEach { platform ->
        runComposeUiTest {
            var month: LocalDate? = null
            setContent {
                DivaTheme(mode = ThemeMode.LIGHT, platform = platform) {
                    DivaCalendar(
                        selected = LocalDate(2026, 1, 31),
                        displayedMonth = LocalDate(2026, 1, 31),
                        onMonthChange = { month = it },
                        onSelect = {},
                    )
                }
            }
            // The 31st of January stepping back a month has to land on a day February has.
            onNodeWithContentDescription("Previous month").performClick()
            assertEquals(LocalDate(2025, 12, 31), month, "wrong month on $platform")

            onNodeWithContentDescription("Next month").performClick()
            assertEquals(LocalDate(2026, 2, 28), month, "day was not clamped on $platform")
        }
    }
}

private fun hasSwitchRole() = androidx.compose.ui.test.SemanticsMatcher.expectValue(
    androidx.compose.ui.semantics.SemanticsProperties.Role,
    androidx.compose.ui.semantics.Role.Switch,
)
