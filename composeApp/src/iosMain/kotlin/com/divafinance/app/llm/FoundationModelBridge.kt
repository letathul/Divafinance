package com.divafinance.app.llm

/**
 * The Swift side of Apple's Foundation Models framework, seen from Kotlin.
 *
 * Foundation Models is Swift-only — it has no Objective-C interface — so unlike Vision,
 * CoreLocation or PhotosUI it cannot be reached through `platform.*` from Kotlin/Native. The
 * only way in is a protocol Kotlin declares and Swift implements, which is what this is.
 *
 * Two constraints shape the signatures:
 *
 * - It is declared **here in `:composeApp`** rather than in `:core:domain` so it is
 *   unconditionally exported into the `ComposeApp` framework header. A type declared in a
 *   dependency is only exported if that dependency is listed in `binaries.framework.export`,
 *   which would mean turning an `implementation` dependency into an `api` one across the graph.
 * - The methods are **callback-shaped, not `suspend`**. Kotlin/Native exports a `suspend`
 *   function to Swift as a completion handler, but it cannot *import* one: Swift has no way to
 *   implement a Kotlin `suspend` member. [FoundationModelReceiptExtractor] wraps these back
 *   into suspend functions on the Kotlin side.
 */
interface FoundationModelBridge {
    /** One of `READY`, `DOWNLOADING`, `UNSUPPORTED`. A string, because enums do not bridge. */
    fun availability(): String

    /**
     * Reads [ocrText] and calls back with a JSON object matching `ExtractedReceiptJson`, or
     * null if the model declined, errored, or is unavailable. Never throws across the boundary.
     */
    fun extract(ocrText: String, onResult: (String?) -> Unit)
}

/**
 * Set by `MainViewController` before Koin starts, because `platformModule()` takes no
 * arguments and the bridge can only come from the Swift side that hosts the app.
 *
 * Null on any iOS without Apple Intelligence, which is the case the whole feature is designed
 * to degrade into.
 */
object IosPlatformBridges {
    var foundationModel: FoundationModelBridge? = null
}
