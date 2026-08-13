package com.divafinance.core.network

object ApiRoutes {
    const val BASE = "/api"
    const val AUTH = "/auth"
    const val LOGIN = "$AUTH/login"
    const val LOGOUT = "$AUTH/logout"

    const val CARDS = "$BASE/cards"
    const val CARD_BY_ID = "$CARDS/{id}"
    const val CARD_REWARDS = "$CARDS/{id}/rewards"
    const val BEST_CARD = "$CARDS/best/{category}"

    const val TRANSACTIONS = "$BASE/transactions"
    const val TRANSACTION_BY_ID = "$TRANSACTIONS/{id}"

    const val ACCOUNTS = "$BASE/accounts"
    const val ACCOUNT_BY_ID = "$ACCOUNTS/{id}"

    const val GRAPHS = "$BASE/graphs"
    const val GRAPHS_SPENDING = "$GRAPHS/spending"
    const val GRAPHS_THRESHOLDS = "$GRAPHS/thresholds"

    const val DASHBOARD = "$BASE/dashboard"
    const val FEED = "$BASE/feed"

    const val BACKUP = "$BASE/backup"
    const val BACKUP_EXPORT = "$BACKUP/export"
    const val BACKUP_IMPORT = "$BACKUP/import"
}
