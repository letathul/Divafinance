import Foundation
import ComposeApp

#if canImport(FoundationModels)
import FoundationModels
#endif

/// The shape the model is asked to produce, and the shape Kotlin decodes.
///
/// Kept in lockstep with `ExtractedReceiptJson` in
/// `composeApp/src/iosMain/.../llm/FoundationModelReceiptExtractor.kt`. A drift between the two
/// fails to decode there and the whole pass is discarded, which is the intended outcome —
/// nothing downstream depends on this succeeding.
private struct ReceiptDraft: Codable {
    var merchant: String?
    var total: Double?
    var date: String?
    var subtotal: Double?
    var tax: Double?
    var tip: Double?
    var currency: String?
    var items: [ItemDraft]
}

private struct ItemDraft: Codable {
    var description: String
    var price: Double?
    var quantity: Double?
}

#if canImport(FoundationModels)

/// The same structure again, annotated for guided generation.
///
/// `@Generable` is what makes this worth doing over a plain prompt: the model is constrained to
/// emit a value of this type, so there is no free-form text to parse and no "the model replied
/// with an apology instead of JSON" failure mode.
@available(iOS 26.0, *)
@Generable
private struct GeneratedReceipt {
    @Guide(description: "The shop or restaurant name only, without any address or phone number")
    var merchant: String?

    @Guide(description: "The grand total actually charged, as a number")
    var total: Double?

    @Guide(description: "The purchase date in YYYY-MM-DD form, or omitted if not printed")
    var date: String?

    @Guide(description: "The pre-tax subtotal, if the receipt prints one")
    var subtotal: Double?

    @Guide(description: "Tax or VAT charged, if printed")
    var tax: Double?

    @Guide(description: "Tip or gratuity, if printed")
    var tip: Double?

    @Guide(description: "Three-letter ISO currency code, e.g. USD, only if it can be determined")
    var currency: String?

    @Guide(description: "One entry per purchased item. Do not include totals, tax or tip lines.")
    var items: [GeneratedItem]
}

@available(iOS 26.0, *)
@Generable
private struct GeneratedItem {
    @Guide(description: "What was bought, as printed")
    var description: String

    @Guide(description: "What this line was charged, as a number")
    var price: Double?

    @Guide(description: "How many, only if the receipt prints a count")
    var quantity: Double?
}

#endif

private let instructions = """
You extract structured data from the raw text of a shop receipt produced by OCR.
The text is noisy: lines may be out of order, characters may be misread, and parts may be
missing. Report only what the text actually supports. If a field is not present, omit it
rather than guessing. Never invent items that are not listed.
"""

/// Bridges Apple's on-device model to Kotlin.
///
/// Everything is availability-gated at runtime rather than by the deployment target, which
/// stays at iOS 17: `FoundationModels` is weak-linked and an older device simply reports
/// `UNSUPPORTED`, which is the same path a device with Apple Intelligence turned off takes.
final class FoundationModelBridgeImpl: NSObject, FoundationModelBridge {

    func availability() -> String {
        #if canImport(FoundationModels)
        guard #available(iOS 26.0, *) else { return "UNSUPPORTED" }
        switch SystemLanguageModel.default.availability {
        case .available:
            return "READY"
        case .unavailable(.modelNotReady):
            return "DOWNLOADING"
        default:
            // Device not eligible, Apple Intelligence off, or model disabled by the user.
            return "UNSUPPORTED"
        }
        #else
        return "UNSUPPORTED"
        #endif
    }

    func extract(ocrText: String, onResult: @escaping (String?) -> Void) {
        #if canImport(FoundationModels)
        guard #available(iOS 26.0, *), case .available = SystemLanguageModel.default.availability
        else {
            onResult(nil)
            return
        }

        Task {
            do {
                let session = LanguageModelSession(instructions: instructions)
                let response = try await session.respond(
                    to: ocrText,
                    generating: GeneratedReceipt.self
                )
                let generated = response.content
                let draft = ReceiptDraft(
                    merchant: generated.merchant,
                    total: generated.total,
                    date: generated.date,
                    subtotal: generated.subtotal,
                    tax: generated.tax,
                    tip: generated.tip,
                    currency: generated.currency,
                    items: generated.items.map {
                        ItemDraft(description: $0.description, price: $0.price, quantity: $0.quantity)
                    }
                )
                let data = try JSONEncoder().encode(draft)
                onResult(String(data: data, encoding: .utf8))
            } catch {
                // Guardrail refusals, context overflow and model errors all land here, and all
                // mean the same thing to the caller: no extra data this time.
                onResult(nil)
            }
        }
        #else
        onResult(nil)
        #endif
    }
}
