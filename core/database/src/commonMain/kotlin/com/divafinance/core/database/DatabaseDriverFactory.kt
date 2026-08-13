package com.divafinance.core.database

import app.cash.sqldelight.db.SqlDriver

expect class DatabaseDriverFactory {
    fun create(): SqlDriver
}
