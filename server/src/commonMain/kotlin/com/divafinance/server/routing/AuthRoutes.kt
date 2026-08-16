package com.divafinance.server.routing

import com.divafinance.core.network.dto.AuthRequestDto
import com.divafinance.core.network.dto.AuthResponseDto
import com.divafinance.server.auth.DivaSession
import com.divafinance.server.auth.PinAuthProvider
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set

fun Route.authRoutes(pinAuthProvider: PinAuthProvider) {
    /**
     * Deliberately unauthenticated: the web UI calls this on load to decide whether to
     * show the PIN screen or go straight to the dashboard after a refresh.
     */
    get("/auth/session") {
        val authenticated = call.sessions.get<DivaSession>()?.authenticated == true
        call.respond(AuthResponseDto(success = authenticated))
    }

    post("/auth/login") {
        val request = call.receive<AuthRequestDto>()
        if (pinAuthProvider.validate(request.pin)) {
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
        // Clearing drops the cookie outright; overwriting it with authenticated=false
        // left a stale session hanging around on the client.
        call.sessions.clear<DivaSession>()
        call.respond(AuthResponseDto(success = true, message = "Logged out"))
    }
}
