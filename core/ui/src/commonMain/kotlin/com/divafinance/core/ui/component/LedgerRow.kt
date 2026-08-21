package com.divafinance.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.divafinance.core.model.Transaction
import com.divafinance.core.model.enums.TransactionType
import com.divafinance.core.ui.theme.NumericStyle
import com.divafinance.core.ui.theme.Space
import com.divafinance.core.ui.theme.diva
import com.divafinance.core.ui.util.formatCurrency

/** Sticky-feeling day header: label left, day total right in tabular mono. */
@Composable
fun DayHeader(label: String, total: Double, modifier: Modifier = Modifier, currency: String = "USD") {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = Space.pad, end = Space.pad, top = 22.dp, bottom = Space.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        Text(
            formatCurrency(total, currency),
            style = NumericStyle.copy(fontSize = 12.sp),
            color = diva.muted,
        )
    }
}

/**
 * One line of the ledger. The subtitle collapses whatever is known about the spend into a
 * single dot-separated run, so the row height stays constant whether or not a merchant,
 * note or location was captured.
 */
@Composable
fun TransactionRow(
    transaction: Transaction,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: () -> Unit = {},
) {
    val isCredit = transaction.type == TransactionType.CREDIT
    Row(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = Space.pad, vertical = 9.dp)
            .heightIn(min = diva.rowMinHeight + 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        CategoryTile(transaction.category)
        Column(Modifier.weight(1f)) {
            Text(
                transaction.merchantName ?: transaction.category.displayName,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle ?: defaultSubtitle(transaction),
                style = MaterialTheme.typography.bodySmall,
                color = diva.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // formatCurrency drops the sign deliberately, so direction is rendered here.
        Text(
            (if (isCredit) "+" else "−") + formatCurrency(transaction.amount, transaction.currency),
            style = NumericStyle,
            color = if (isCredit) diva.positive else MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun defaultSubtitle(transaction: Transaction): String = buildList {
    add(transaction.category.displayName)
    transaction.location?.name?.takeIf { it.isNotBlank() }?.let { add(it) }
    if (transaction.othersShare > 0) add("split")
    if (transaction.isRecurring) add("recurring")
}.joinToString(" · ")
