package com.divafinance.feature.quickadd

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.divafinance.core.ui.component.Avatar
import com.divafinance.core.ui.component.DivaButton
import com.divafinance.core.ui.component.DivaTextField
import com.divafinance.core.ui.component.Hairline
import com.divafinance.core.ui.component.PersonPalette
import com.divafinance.core.ui.component.initialsOf
import com.divafinance.core.ui.component.personColor
import com.divafinance.core.ui.component.toHex
import com.divafinance.core.ui.theme.Pill
import com.divafinance.core.ui.theme.Space
import com.divafinance.core.ui.theme.diva

/*
 * Drawn to the same spec as the rest of the add-transaction flow, so the geometry is
 * repeated here rather than reached for across files.
 */
private val SheetGutter = 20.dp
private val FieldRadius = 12.dp
private val RowRadius = 14.dp

/**
 * The roster picker, shown on the split sheet's own surface.
 *
 * A person in this app is a name and a colour. No phone number, no email, no account — a
 * split is settled between people who already know each other, and the app is local-first,
 * so there is nothing for an identifier to reach. That is what makes the address book
 * optional rather than the way in: Frequent builds itself out of splits already made, and
 * typing a name is always available beside it.
 *
 * Selection is a batch. Ticking three people and committing once means the bill changes
 * length once, rather than three times with the positional share lists reset at each step.
 */
@Composable
internal fun ColumnScope.AddPeopleBody(
    state: QuickAddUiState,
    onDismiss: () -> Unit,
    onPeopleSearchChange: (String) -> Unit,
    onPersonToggled: (String?, String) -> Unit,
    onNewPersonFormToggled: () -> Unit,
    onNewPersonNameChange: (String) -> Unit,
    onNewPersonColorChange: (String) -> Unit,
    onCreatePerson: () -> Unit,
    onImportContacts: () -> Unit,
    onConfirmPeople: () -> Unit,
) {
    val query = state.peopleSearch.trim()
    val matches: (String) -> Boolean = { name ->
        query.isEmpty() || name.contains(query, ignoreCase = true)
    }
    val known = state.peopleSuggestions.filter { matches(it.name) }
    val imported = state.importedContacts.filter { matches(it) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 620.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SheetGutter)
            .padding(bottom = Space.xl)
            .imePadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    "Add People",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "Split this expense with anyone you like",
                    style = MaterialTheme.typography.bodySmall,
                    color = diva.muted,
                )
            }
            SheetClose(onDismiss)
        }

        DivaTextField(
            value = state.peopleSearch,
            onValueChange = onPeopleSearchChange,
            label = "Search people",
            leadingIcon = {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = null,
                    tint = diva.muted,
                    modifier = Modifier.size(16.dp),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )

        if (known.isNotEmpty()) {
            SheetLabel("Frequent")
            Column {
                known.forEachIndexed { index, person ->
                    if (index > 0) Hairline()
                    PersonRow(
                        name = person.name,
                        color = personColor(person.name, person.colorHex),
                        caption = splitCountCaption(state.splitCountFor(person)),
                        selected = state.isPersonSelected(person.name),
                        onClick = { onPersonToggled(person.id, person.name) },
                    )
                }
            }
        } else if (query.isNotEmpty()) {
            Text(
                text = "Nobody by that name yet — add them below.",
                style = MaterialTheme.typography.bodySmall,
                color = diva.muted,
            )
        }

        if (imported.isNotEmpty()) {
            SheetLabel("From contacts")
            Column {
                imported.forEachIndexed { index, name ->
                    if (index > 0) Hairline()
                    PersonRow(
                        name = name,
                        color = personColor(name),
                        caption = "Not split yet",
                        selected = state.isPersonSelected(name),
                        // No person id: nobody has been created yet, and typing or picking
                        // the same name later still resolves to one person by name.
                        onClick = { onPersonToggled(null, name) },
                    )
                }
            }
        }

        NewPersonBlock(
            state = state,
            onNewPersonFormToggled = onNewPersonFormToggled,
            onNewPersonNameChange = onNewPersonNameChange,
            onNewPersonColorChange = onNewPersonColorChange,
            onCreatePerson = onCreatePerson,
        )

        if (state.contactsSupported) ImportContactsRow(onImportContacts)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${state.pendingPeople.size} selected",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = diva.accent,
                maxLines = 1,
                modifier = Modifier
                    .clip(Pill)
                    .background(diva.accent.copy(alpha = 0.14f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
            DivaButton(
                text = "Add to Split",
                onClick = onConfirmPeople,
                enabled = state.pendingPeople.isNotEmpty(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** "Split 6 times before" — how often, in the words the design uses for each count. */
private fun splitCountCaption(count: Int): String = when (count) {
    0 -> "Not split yet"
    1 -> "Split once before"
    else -> "Split $count times before"
}

/**
 * One selectable person.
 *
 * The trailing control is a check when they are on the bill and a plus when they are not,
 * rather than a checkbox in two states: the plus says what tapping will *do*, which is the
 * question on a list most of whose rows are unticked.
 */
@Composable
private fun PersonRow(
    name: String,
    color: Color,
    caption: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FieldRadius))
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 2.dp)
            // What the tap will do, not what the row currently is — the same reason the
            // trailing control is a plus rather than an empty checkbox.
            .semantics {
                contentDescription = if (selected) "Remove $name" else "Add $name"
            },
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(initials = initialsOf(name), color = color, size = 40.dp)
        Column(Modifier.weight(1f)) {
            Text(
                name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Text(caption, style = MaterialTheme.typography.bodySmall, color = diva.muted)
        }
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(7.dp))
                .then(
                    if (selected) {
                        Modifier.background(diva.accent)
                    } else {
                        Modifier.border(1.5.dp, diva.fgHair, RoundedCornerShape(7.dp))
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (selected) Icons.Outlined.Check else Icons.Outlined.Add,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.background else diva.muted,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

/**
 * The dashed way in for someone the app has never seen, and the form behind it.
 *
 * Dashed because it is not a person yet — the same encoding the empty seat on `PaidByRow`
 * uses. The colour is picked here rather than assigned because two flatmates called Sam
 * both hashing to the same green is exactly the case a roster has to survive.
 */
@Composable
private fun NewPersonBlock(
    state: QuickAddUiState,
    onNewPersonFormToggled: () -> Unit,
    onNewPersonNameChange: (String) -> Unit,
    onNewPersonColorChange: (String) -> Unit,
    onCreatePerson: () -> Unit,
) {
    val typed = state.newPersonName.trim()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RowRadius))
            .border(1.5.dp, diva.fgHair, RoundedCornerShape(RowRadius))
            .clickable(onClick = onNewPersonFormToggled)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(30.dp).clip(CircleShape).background(diva.keyFill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Add,
                contentDescription = null,
                tint = diva.muted,
                modifier = Modifier.size(15.dp),
            )
        }
        Text(
            "Add a new person by name",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }

    if (!state.newPersonFormOpen) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RowRadius))
            .background(diva.barFill)
            .padding(Space.md),
        verticalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(
                initials = initialsOf(typed.ifEmpty { "?" }),
                color = personColor(typed, state.newPersonColorHex),
                size = 48.dp,
            )
            DivaTextField(
                value = state.newPersonName,
                onValueChange = onNewPersonNameChange,
                label = "Name",
                modifier = Modifier.weight(1f),
            )
        }

        SheetLabel("Avatar color")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PersonPalette.forEach { swatch ->
                val hex = swatch.toHex()
                val chosen = state.newPersonColorHex.equals(hex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(swatch)
                        .then(
                            if (chosen) Modifier.border(2.dp, diva.accent, CircleShape)
                            else Modifier
                        )
                        .clickable { onNewPersonColorChange(hex) }
                        .semantics { contentDescription = "Avatar colour ${PersonPalette.indexOf(swatch) + 1}" },
                )
            }
        }

        DivaButton(
            text = if (typed.isEmpty()) "Add person" else "Add $typed",
            onClick = onCreatePerson,
            enabled = typed.isNotEmpty(),
        )

        Text(
            text = "No phone number or email needed — a name and a colour is enough to track " +
                "their share. They're saved to Frequent for next time.",
            style = MaterialTheme.typography.bodySmall,
            color = diva.muted,
        )
    }
}

/** Offered, never required: the sheet works end to end without ever reading the address book. */
@Composable
private fun ImportContactsRow(onImportContacts: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RowRadius))
            .background(diva.keyFill)
            .clickable(onClick = onImportContacts)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, diva.fgHair, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.People,
                contentDescription = null,
                tint = diva.muted,
                modifier = Modifier.size(15.dp),
            )
        }
        Column {
            Text(
                "Import from Contacts",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "One-time permission · names stay on this device",
                style = MaterialTheme.typography.bodySmall,
                color = diva.muted,
            )
        }
    }
}

@Composable
private fun SheetLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = diva.muted,
    )
}

@Composable
private fun SheetClose(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Outlined.Close,
            contentDescription = "Close",
            tint = diva.muted,
            modifier = Modifier.size(20.dp),
        )
    }
}
