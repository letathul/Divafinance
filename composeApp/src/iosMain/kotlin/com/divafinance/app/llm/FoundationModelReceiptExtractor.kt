package com.divafinance.app.llm

import com.divafinance.core.domain.usecase.scanner.ExtractedReceipt
import com.divafinance.core.domain.usecase.scanner.ExtractorAvailability
import com.divafinance.core.domain.usecase.scanner.ParsedLineItem
import com.divafinance.core.domain.usecase.scanner.ReceiptExtractor
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.coroutines.resume

/**
 * Wire format between the Swift bridge and here. It mirrors the `@Generable` struct Swift asks
 * the model to produce, so the model's guided-generation schema and this decoder cannot drift
 * apart silently — a mismatch fails to decode and the whole pass is discarded, which is the
 * intended outcome.
 */
@Serializable
private data class ExtractedReceiptJson(
    val merchant: String? = null,
    val total: Double? = null,
    /** ISO `YYYY-MM-DD`. Anything else is dropped rather than guessed at. */
    val date: String? = null,
    val subtotal: Double? = null,
    val tax: Double? = null,
    val tip: Double? = null,
    val currency: String? = null,
    val items: List<ExtractedItemJson> = emptyList(),
)

@Serializable
private data class ExtractedItemJson(
    val description: String = "",
    val price: Double? = null,
    val quantity: Double? = null,
)

/**
 * Adapts Apple's on-device model to the domain's [ReceiptExtractor] port.
 *
 * Nothing here is trusted: the JSON is decoded leniently, a malformed date is dropped rather
 * than repaired, and any failure at all surfaces as null. `ParseReceiptUseCase` only ever uses
 * this to fill fields its own rules left empty, so a bad answer costs a blank field, never a
 * wrong amount.
 */
class FoundationModelReceiptExtractor(
    private val bridge: FoundationModelBridge,
) : ReceiptExtractor {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    override suspend fun availability(): ExtractorAvailability =
        runCatching { ExtractorAvailability.valueOf(bridge.availability()) }
            .getOrDefault(ExtractorAvailability.UNSUPPORTED)

    override suspend fun extract(ocrText: String): ExtractedReceipt? {
        val raw = suspendCancellableCoroutine<String?> { continuation ->
            runCatching {
                bridge.extract(ocrText) { result ->
                    if (continuation.isActive) continuation.resume(result)
                }
            }.onFailure {
                if (continuation.isActive) continuation.resume(null)
            }
        } ?: return null

        val decoded = runCatching { json.decodeFromString<ExtractedReceiptJson>(raw) }
            .getOrNull()
            ?: return null

        return ExtractedReceipt(
            merchantName = decoded.merchant?.trim()?.takeIf { it.isNotBlank() },
            totalAmount = decoded.total?.takeIf { it > 0.0 },
            date = decoded.date?.let { runCatching { LocalDate.parse(it.trim()) }.getOrNull() },
            subtotal = decoded.subtotal,
            tax = decoded.tax,
            tip = decoded.tip,
            currency = decoded.currency?.trim()?.uppercase()?.takeIf { it.length == CURRENCY_LENGTH },
            items = decoded.items.mapNotNull { item ->
                val description = item.description.trim()
                if (description.isEmpty()) return@mapNotNull null
                ParsedLineItem(
                    description = description,
                    price = item.price,
                    quantity = item.quantity?.takeIf { it > 0.0 },
                )
            },
        )
    }

    private companion object {
        const val CURRENCY_LENGTH = 3
    }
}
