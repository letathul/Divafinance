package com.divafinance.core.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

/**
 * Desktop-JVM driver, in-memory by default.
 *
 * The JVM target exists to host Compose and repository tests, so the default is a throwaway
 * database rather than a file — each factory call yields a fresh schema with no cleanup.
 */
actual class DatabaseDriverFactory(private val jdbcUrl: String = JdbcSqliteDriver.IN_MEMORY) {
    actual fun create(): SqlDriver {
        return JdbcSqliteDriver(jdbcUrl).also { DivaFinanceDb.Schema.create(it) }
    }
}
