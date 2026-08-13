package com.divafinance.server

class DivaServer {
    private var isRunning = false

    fun start(port: Int = 8080) {
        isRunning = true
    }

    fun stop() {
        isRunning = false
    }

    fun isRunning(): Boolean = isRunning
}
