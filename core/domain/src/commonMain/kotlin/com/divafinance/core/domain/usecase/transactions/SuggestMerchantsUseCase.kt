package com.divafinance.core.domain.usecase.transactions

import com.divafinance.core.data.repository.TransactionRepository

/**
 * Merchant autocomplete drawn from the user's own history.
 *
 * With no query this is the "recent merchants" row — the shops logged most often, which
 * for most people covers the majority of entries. With a query it ranks prefix matches
 * above mid-string ones, so typing "co" offers "Costa" before "Tesco".
 *
 * Matching happens here rather than in SQL deliberately: a leading-wildcard `LIKE` cannot
 * use the merchant index, and the distinct-merchant set is small enough that filtering it
 * in memory is cheaper than the query would be.
 */
class SuggestMerchantsUseCase(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(query: String = "", limit: Int = 5): List<String> {
        val known = runCatching { transactionRepository.getKnownMerchants() }
            .getOrDefault(emptyList())

        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return known.take(limit)

        // `known` arrives most-used first, and partitioning preserves that order within
        // each group, so usage still breaks ties inside a match tier.
        val (prefix, rest) = known
            .filter { it.lowercase().contains(needle) }
            .partition { it.lowercase().startsWith(needle) }

        return (prefix + rest)
            // An exact match is already in the field; offering it back is noise.
            .filterNot { it.equals(query.trim(), ignoreCase = true) }
            .take(limit)
    }
}
