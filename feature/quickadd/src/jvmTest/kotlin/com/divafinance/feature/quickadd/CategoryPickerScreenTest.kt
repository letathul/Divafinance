package com.divafinance.feature.quickadd

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.divafinance.core.model.CustomCategory
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.ui.theme.DivaTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

@OptIn(ExperimentalTestApi::class)
class CategoryPickerScreenTest {

    private val ramen = CustomCategory(
        id = "c1",
        name = "Ramen",
        iconKey = "restaurant",
        colorHex = "#0F9D6E",
        parent = SpendingCategory.DINING,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    @Test
    fun listsEveryBuiltInCategory() = runComposeUiTest {
        setContent { DivaTheme { CategoryPickerContent(QuickAddUiState()) } }

        onNodeWithText("Dining").assertIsDisplayed()
        // The one the chip row on the add screen would never have offered — which is the
        // whole reason this screen exists.
        onNodeWithText("Healthcare").assertIsDisplayed()
    }

    @Test
    fun offersTheUsersOwnCategoriesAlongsideTheBuiltIns() = runComposeUiTest {
        setContent {
            DivaTheme {
                CategoryPickerContent(QuickAddUiState(customCategories = listOf(ramen)))
            }
        }
        onNodeWithText("Ramen").assertIsDisplayed()
        onNodeWithText("Dining").assertIsDisplayed()
    }

    @Test
    fun searchNarrowsTheGrid() = runComposeUiTest {
        setContent {
            DivaTheme {
                CategoryPickerContent(QuickAddUiState(customCategories = listOf(ramen)))
            }
        }
        onNodeWithText("Search categories").performTextInput("ram")

        onNodeWithText("Ramen").assertIsDisplayed()
        onNodeWithText("Healthcare").assertDoesNotExist()
    }

    @Test
    fun pickingACategoryReportsIt() = runComposeUiTest {
        var picked: SpendingCategory? = null
        setContent {
            DivaTheme {
                CategoryPickerContent(QuickAddUiState(), onCategoryChange = { picked = it })
            }
        }
        onNodeWithText("Travel").performClick()

        assertEquals(SpendingCategory.TRAVEL, picked)
    }

    @Test
    fun pickingACustomCategoryReportsIt() = runComposeUiTest {
        var picked: CustomCategory? = null
        setContent {
            DivaTheme {
                CategoryPickerContent(
                    QuickAddUiState(customCategories = listOf(ramen)),
                    onCustomCategoryChange = { picked = it },
                )
            }
        }
        onNodeWithText("Ramen").performClick()

        assertEquals("c1", picked?.id)
    }

    /**
     * The creator asks for a parent because a category with none would earn no rewards and
     * predict from no history — so the created category has to carry the one that was picked.
     */
    @Test
    fun theCreatorCarriesTheParentThatWasChosen() = runComposeUiTest {
        var name: String? = null
        var iconKey: String? = null
        var parent: SpendingCategory? = null
        setContent {
            DivaTheme {
                CategoryPickerContent(
                    QuickAddUiState(),
                    onCreateCustomCategory = { n, i, _, p -> name = n; iconKey = i; parent = p },
                )
            }
        }
        onNodeWithText("Add custom").performClick()
        onNodeWithText("Name").performTextInput("Ramen")
        onNodeWithContentDescription("restaurant").performClick()
        // The grid behind the sheet still carries a "Travel" tile, so the second match is
        // the sheet's own parent chip.
        onAllNodesWithText("Travel")[1].performClick()
        onNodeWithText("Add category").performClick()

        assertEquals("Ramen", name)
        assertEquals("restaurant", iconKey)
        assertEquals(SpendingCategory.TRAVEL, parent)
    }
}
