package com.divafinance.feature.automation

import kotlin.test.Test
import kotlin.test.assertEquals

class AutomationScreenTest {

    @Test
    fun automationPlatformEnumValues() {
        assertEquals(3, AutomationPlatform.entries.size)
        assertEquals(AutomationPlatform.ANDROID, AutomationPlatform.entries[0])
        assertEquals(AutomationPlatform.IOS, AutomationPlatform.entries[1])
        assertEquals(AutomationPlatform.CROSS_PLATFORM, AutomationPlatform.entries[2])
    }

    @Test
    fun automationUiStateDefaults() {
        val state = AutomationUiState()
        assertEquals(7, state.actions.size)
        assertEquals(false, state.actions.all { it.enabled })
    }

    @Test
    fun defaultActionsHaveCorrectPlatforms() {
        val actions = defaultActions()
        val crossPlatform = actions.filter { it.platform == AutomationPlatform.CROSS_PLATFORM }
        val android = actions.filter { it.platform == AutomationPlatform.ANDROID }
        val ios = actions.filter { it.platform == AutomationPlatform.IOS }
        assertEquals(3, crossPlatform.size)
        assertEquals(2, android.size)
        assertEquals(2, ios.size)
    }

    @Test
    fun toggleActionFlipsEnabled() {
        val viewModel = AutomationViewModel()
        val initialState = viewModel.uiState.value
        val firstAction = initialState.actions.first()
        assertEquals(false, firstAction.enabled)

        viewModel.toggleAction(firstAction.id)
        val updatedState = viewModel.uiState.value
        assertEquals(true, updatedState.actions.first().enabled)

        viewModel.toggleAction(firstAction.id)
        val revertedState = viewModel.uiState.value
        assertEquals(false, revertedState.actions.first().enabled)
    }
}
