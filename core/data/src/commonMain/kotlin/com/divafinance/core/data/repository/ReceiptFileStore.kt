package com.divafinance.core.data.repository

/**
 * The image files a receipt owns, as distinct from its row.
 *
 * A one-method interface rather than `core:common`'s `FileSystem` because that is an
 * `expect class` whose constructor differs per platform — Android needs a `Context`, iOS
 * takes none — so it can only be built inside a platform Koin module and cannot be
 * constructed in `commonTest` at all. Depending on this instead keeps `:core:domain` free
 * of platform types and leaves receipt cleanup testable against a fake.
 */
fun interface ReceiptFileStore {
    /** Returns whether a file was actually removed. A path already gone is not an error. */
    fun delete(path: String): Boolean
}
