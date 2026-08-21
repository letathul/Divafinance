package com.divafinance.core.ui.adaptive

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.divafinance.core.ui.theme.Space
import com.divafinance.core.ui.theme.diva
import com.divafinance.core.ui.theme.isCupertino

/** How a screen's title is drawn. [Large] is the 34sp title that scrolls away. */
enum class DivaTitleStyle { Large, Inline }

/**
 * The shared screen chrome.
 *
 * A drop-in for `Scaffold(topBar = { TopAppBar(…) })`: [content] receives the same
 * [PaddingValues] the M3 scaffold hands out, so converting a screen is a two-line diff.
 *
 * Material renders an M3 `TopAppBar`. Cupertino renders a 44dp translucent nav bar with a
 * centred title, a back affordance that carries its label, and a hairline bottom edge.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DivaScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backLabel: String = "Back",
    actions: @Composable RowScope.() -> Unit = {},
    titleContent: (@Composable () -> Unit)? = null,
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    containerColor: Color = diva.canvas,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = { DivaTopBar(title, onBack, backLabel, actions, titleContent) },
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        containerColor = containerColor,
        content = content,
    )
}

/**
 * For a screen that is one `LazyColumn`.
 *
 * The large title is emitted as the list's **first item** rather than pinned in the bar,
 * so on Cupertino it scrolls away exactly like a `UINavigationController` large title
 * with no nested-scroll plumbing at all. On Material the title stays in the app bar and
 * no in-list title is emitted.
 *
 * [titleContent] replaces the title text wherever the title is being drawn — the feed
 * puts `DivaLogo()` in that slot, which lands in the large title on Cupertino and in the
 * app bar on Material. The lambda can read `isCupertino` to size itself accordingly.
 */
@Composable
fun DivaListScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backLabel: String = "Back",
    actions: @Composable RowScope.() -> Unit = {},
    titleStyle: DivaTitleStyle = DivaTitleStyle.Large,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = divaContentPadding(),
    containerColor: Color = diva.canvas,
    titleContent: (@Composable () -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    // A large title on Cupertino lives in the list, so the bar above it carries only the
    // actions; on Material the bar always carries the title.
    // Read here, not inside the LazyListScope block below — that lambda is not composable.
    val largeTitleInList = isCupertino && titleStyle == DivaTitleStyle.Large
    val inlineTitle = if (largeTitleInList) "" else title

    DivaScaffold(
        title = inlineTitle,
        modifier = modifier,
        onBack = onBack,
        backLabel = backLabel,
        actions = actions,
        // While a large title is showing there is no title in the bar to replace.
        titleContent = titleContent.takeIf { !largeTitleInList },
        containerColor = containerColor,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            state = state,
            contentPadding = contentPadding,
        ) {
            if (largeTitleInList) {
                item(key = "diva:largeTitle") {
                    DivaLargeTitle(title, leading = titleContent)
                }
            }
            content()
        }
    }
}

/** The 34sp title a Cupertino screen opens with, before it scrolls away. */
@Composable
fun DivaLargeTitle(
    title: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = Space.pad, end = Space.pad, top = Space.xs, bottom = Space.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (leading != null) {
            leading()
        } else {
            Text(
                title,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
            content = trailing,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DivaTopBar(
    title: String,
    onBack: (() -> Unit)?,
    backLabel: String,
    actions: @Composable RowScope.() -> Unit,
    titleContent: (@Composable () -> Unit)? = null,
) {
    if (isCupertino) {
        CupertinoTopBar(title, onBack, backLabel, actions, titleContent)
    } else {
        TopAppBar(
            title = {
                if (titleContent != null) {
                    titleContent()
                } else {
                    Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            },
            navigationIcon = {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = diva.canvas,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
            ),
        )
    }
}

/**
 * 44dp, centred title, tinted affordances, hairline bottom edge.
 *
 * When the title is blank — which is what [DivaListScaffold] passes while a large title
 * is showing — the bar still occupies its height so the back button and actions stay
 * where they are as the list scrolls under them.
 */
@Composable
private fun CupertinoTopBar(
    title: String,
    onBack: (() -> Unit)?,
    backLabel: String,
    actions: @Composable RowScope.() -> Unit,
    titleContent: (@Composable () -> Unit)?,
) {
    val separator = diva.separator
    val hairline = diva.hairline
    val showRule = title.isNotEmpty() || titleContent != null

    Box(
        Modifier
            .fillMaxWidth()
            .background(diva.barFill.copy(alpha = if (showRule) 0.94f else 0f))
            .drawBehind {
                if (showRule) {
                    drawLine(
                        color = separator,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = hairline.toPx(),
                    )
                }
            }
            .statusBarsPadding(),
    ) {
        Box(Modifier.fillMaxWidth().height(44.dp)) {
            if (onBack != null) {
                Row(
                    Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = Space.sm)
                        .clickable(onClick = onBack)
                        .padding(horizontal = Space.xs, vertical = Space.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBackIos,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        backLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            if (titleContent != null) {
                Box(Modifier.align(Alignment.Center)) { titleContent() }
            } else if (title.isNotEmpty()) {
                Text(
                    title,
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 72.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                Modifier.align(Alignment.CenterEnd).padding(end = Space.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.md),
                content = actions,
            )
        }
    }
}

/**
 * The non-list variant: a `Column` body for the form screens that are not a `LazyColumn`.
 */
@Composable
fun DivaScaffoldColumn(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backLabel: String = "Back",
    actions: @Composable RowScope.() -> Unit = {},
    titleContent: (@Composable () -> Unit)? = null,
    bottomBar: @Composable () -> Unit = {},
    containerColor: Color = diva.canvas,
    content: @Composable (PaddingValues) -> Unit,
) = DivaScaffold(
    title = title,
    modifier = modifier,
    onBack = onBack,
    backLabel = backLabel,
    actions = actions,
    titleContent = titleContent,
    bottomBar = bottomBar,
    containerColor = containerColor,
    content = content,
)
