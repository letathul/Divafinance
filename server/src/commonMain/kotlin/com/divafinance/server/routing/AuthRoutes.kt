package com.divafinance.server.routing

import com.divafinance.core.network.dto.AuthRequestDto
import com.divafinance.core.network.dto.AuthResponseDto
import com.divafinance.server.auth.DivaSession
import com.divafinance.server.auth.PinAuthProvider
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set

fun Route.authRoutes(pinAuthProvider: PinAuthProvider) {
    post("/auth/login") {
        val request = call.receive<AuthRequestDto>()
        val valid = pinAuthProvider.validate(request.pin)
        if (valid) {
            call.sessions.set(DivaSession(token = "authenticated", authenticated = true))
            call.respond(AuthResponseDto(success = true))
        } else {
            call.respond(
                HttpStatusCode.Unauthorized,
                AuthResponseDto(success = false, message = "Invalid PIN"),
            )
        }
    }

    post("/auth/logout") {
        call.sessions.set(DivaSession(token = "", authenticated = false))
        call.respond(AuthResponseDto(success = true, message = "Logged out"))
    }
}
