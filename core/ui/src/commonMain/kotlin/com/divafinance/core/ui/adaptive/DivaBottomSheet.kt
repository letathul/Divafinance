package com.divafinance.core.ui.adaptive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.divafinance.core.ui.theme.diva
import com.divafinance.core.ui.theme.isCupertino

/**
 * The app's bottom sheet, in the platform's own shape.
 *
 * Both platforms use M3's `ModalBottomSheet` for the scrim, drag and predictive-back
 * behaviour — hand-rolling that is where sheets go wrong — and diverge only on shape and
 * handle. Cupertino uses a 14dp corner and no visible grabber by default, matching the
 * rest of the HIG branch; Material keeps the 28dp corner and M3's drag handle.
 *
 * [skipPartiallyExpanded] defaults to true because every sheet in this app is a short,
 * self-contained decision — a half-height resting state would only add a gesture between
 * the user and the choice.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DivaBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    showHandle: Boolean = true,
    skipPartiallyExpanded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val cupertino = isCupertino
    val shape: Shape = RoundedCornerShape(
        topStart = if (cupertino) 14.dp else 28.dp,
        topEnd = if (cupertino) 14.dp else 28.dp,
    )
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        // Built here rather than taken as a parameter: `SheetState` is an experimental
        // type, and exposing it would push that opt-in onto every screen that opens a sheet.
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded),
        shape = shape,
        containerColor = MaterialTheme.colorScheme.surface,
        // The Cupertino branch draws its own grabber below, so M3's is suppressed on both
        // paths and the two sheets never disagree about how much space sits above the title.
        dragHandle = null,
        content = {
            if (showHandle) DivaSheetHandle()
            content()
        },
    )
}

/** The grabber. A 36×4 rule at 28% — the same weight on both platforms. */
@Composable
fun DivaSheetHandle(modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(diva.muted.copy(alpha = 0.28f))
        )
    }
}
