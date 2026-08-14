package com.divafinance.server

data class ServerConfig(
    val port: Int = 8080,
    val host: String = "0.0.0.0",
)
