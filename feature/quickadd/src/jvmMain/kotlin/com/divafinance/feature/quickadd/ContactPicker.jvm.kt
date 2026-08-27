package com.divafinance.feature.quickadd

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * The desktop test host has no address book, so the Add People sheet never offers the
 * Import row here and this reports nothing rather than pretending to prompt.
 */
@Composable
actual fun rememberContactImporter(): ContactImporter =
    remember { ContactImporter { onResult -> onResult(emptyList()) } }

actual fun contactsSupported(): Boolean = false
