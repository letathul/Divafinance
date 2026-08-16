package com.divafinance.server

/**
 * Resolves the address another device on the same Wi-Fi can actually reach this one at.
 *
 * The server binds the wildcard address, so it is listening on every interface — but the
 * user is only ever told one address, and telling them the wrong one is the same as
 * telling them nothing.
 */
fun interface LocalAddressResolver {
    fun lanAddress(): String?
}

/** For platforms with no reachable LAN address to offer; the UI falls back to localhost. */
val NoLocalAddressResolver = LocalAddressResolver { null }
