package com.divafinance.feature.automation

import com.divafinance.core.model.UserSettings
import com.divafinance.core.testing.fake.FakeSettingsRepository
import com.divafinance.core.testing.installTestMainDispatcher
import com.divafinance.core.testing.resetTestMainDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AutomationScreenTest {

    // The ViewModel now reads the stored set in `init` and writes on every toggle, both
    // through viewModelScope — without this they never run.
    @BeforeTest
    fun setUpMainDispatcher() = installTestMainDispatcher()

    @AfterTest
    fun tearDownMainDispatcher() = resetTestMainDispatcher()

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
        val viewModel = AutomationViewModel(InMemoryShortcutRegistrar(), FakeSettingsRepository())
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

    @Test
    fun enablingAnActionRegistersItsShortcut() {
        val registrar = InMemoryShortcutRegistrar()
        val viewModel = AutomationViewModel(registrar, FakeSettingsRepository())

        viewModel.toggleAction("quick_expense")

        val registered = registrar.getRegisteredShortcuts()
        assertEquals(1, registered.size)
        assertEquals("quick_expense", registered.single().id)
        assertEquals("divafinance://automation/quick_expense", registered.single().deepLinkUri)
    }

    /** `registerShortcuts` replaces the whole set rather than appending — by design. */
    @Test
    fun disablingAnActionRemovesItsShortcut() {
        val registrar = InMemoryShortcutRegistrar()
        val viewModel = AutomationViewModel(registrar, FakeSettingsRepository())

        viewModel.toggleAction("quick_expense")
        viewModel.toggleAction("budget_alert")
        assertEquals(2, registrar.getRegisteredShortcuts().size)

        viewModel.toggleAction("quick_expense")

        assertEquals(listOf("budget_alert"), registrar.getRegisteredShortcuts().map { it.id })
    }

    @Test
    fun togglesSurviveANewViewModel() = runTest {
        val settings = FakeSettingsRepository()
        AutomationViewModel(InMemoryShortcutRegistrar(), settings).toggleAction("android_nfc")

        val registrar = InMemoryShortcutRegistrar()
        val reopened = AutomationViewModel(registrar, settings)

        assertEquals(
            listOf("android_nfc"),
            reopened.uiState.value.actions.filter { it.enabled }.map { it.id },
        )
        // Re-registered, not merely remembered: the OS keeps its own copy across launches.
        assertEquals(listOf("android_nfc"), registrar.getRegisteredShortcuts().map { it.id })
    }

    @Test
    fun anEmptyStoredSetEnablesNothing() = runTest {
        val settings = FakeSettingsRepository()
        settings.set(UserSettings.KEY_ENABLED_AUTOMATIONS, "")

        val viewModel = AutomationViewModel(InMemoryShortcutRegistrar(), settings)

        assertEquals(0, viewModel.uiState.value.actions.count { it.enabled })
    }

    @Test
    fun handleDeepLinkResolvesARegisteredShortcut() {
        val registrar = InMemoryShortcutRegistrar()
        AutomationViewModel(registrar, FakeSettingsRepository()).toggleAction("ios_siri")

        assertEquals(
            "ios_siri",
            registrar.handleDeepLink("divafinance://automation/ios_siri")?.id,
        )
        assertNull(registrar.handleDeepLink("divafinance://automation/nope"))
    }
}
