package com.divafinance.feature.onboarding.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp

/**
 * The frame every wizard step shares: a scrolling body, and an action block pinned to the
 * bottom of whatever height the step is given.
 *
 * A step used to be one wrap-content `Column` that separated its fields from its buttons
 * with `Spacer(Modifier.weight(1f))`. That holds until the soft keyboard opens: the whole
 * wizard sits inside `imePadding()`, so the step is re-measured into a box half the
 * height, the un-scrollable body overflows it, and the Back / Next buttons are pushed off
 * the bottom — which is what made SECURITY a dead end, since its two PIN fields raise the
 * keyboard immediately and "Complete Setup" was the only way forward.
 *
 * [actions] is measured first and always keeps its space; the body takes what is left and
 * scrolls. [navigationBarsPadding] on the action block is a no-op while the keyboard is
 * up — the root's `imePadding()` has already consumed a larger inset — and clears the
 * gesture bar when it is down.
 */
@Composable
internal fun OnboardingStepLayout(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actions: @Composable ColumnScope.() -> Unit,
    body: @Composable ColumnScope.() -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            // Tap anywhere outside a field to dismiss. Not a nicety on iOS: the PIN
            // step's numeric keypad has no return key, so without this there is no way
            // to put the keyboard away at all. A tap a child consumed — a button, a
            // chip, the field itself — never reaches here.
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            },
    ) {
        Column(
            // Filling weight, so a short step (SECURITY is two fields) still puts its
            // actions on the bottom edge rather than floating them under the last field.
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = 8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            body()
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            actions()
        }
    }
}
