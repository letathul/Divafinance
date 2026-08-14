package com.divafinance.core.common

expect object SecurityUtils {
    fun generateSalt(): String
    fun hashPin(pin: String, salt: String): String
}
