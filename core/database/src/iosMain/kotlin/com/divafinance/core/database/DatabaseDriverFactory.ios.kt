package com.divafinance.core.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual class DatabaseDriverFactory {
    actual fun create(): SqlDriver {
        return NativeSqliteDriver(DivaFinanceDb.Schema, "diva_finance.db")
    }
}
