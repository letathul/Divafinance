package com.divafinance.server

import com.divafinance.core.data.repository.CardRepository
import com.divafinance.core.data.repository.RewardRepository
import com.divafinance.core.data.repository.TransactionRepository
import com.divafinance.server.auth.DivaSession
import com.divafinance.server.auth.PinAuthProvider
import com.divafinance.server.routing.authRoutes
import com.divafinance.server.routing.cardRoutes
import com.divafinance.server.routing.graphRoutes
import com.divafinance.server.routing.staticRoutes
import com.divafinance.server.routing.transactionRoutes
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.stop
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class DivaServer(
    private val pinAuthProvider: PinAuthProvider,
    private val cardRepository: CardRepository,
    private val rewardRepository: RewardRepository,
    private val transactionRepository: TransactionRepository,
    private val webResources: Map<String, Pair<String, ContentType>> = emptyMap(),
) {
    private var server: io.ktor.server.engine.EmbeddedServer<*, *>? = null
    private var running = false
    private val scope = CoroutineScope(Dispatchers.Default)

    fun start(config: ServerConfig = ServerConfig()) {
        if (running) return
        scope.launch {
            server = embeddedServer(CIO, port = config.port, host = config.host) {
                install(ContentNegotiation) {
                    json(Json {
                        prettyPrint = true
                        ignoreUnknownKeys = true
                    })
                }

                install(Sessions) {
                    cookie<DivaSession>("DIVA_SESSION") {
                        cookie.path = "/"
                        cookie.httpOnly = true
                    }
                }

                install(StatusPages) {
                    exception<Throwable> { call, cause ->
                        call.respondText(
                            """{"error":"${cause.message ?: "Internal server error"}"}""",
                            ContentType.Application.Json,
                            HttpStatusCode.InternalServerError,
                        )
                    }
                }

                routing {
                    staticRoutes(webResources)
                    authRoutes(pinAuthProvider)

                    intercept(io.ktor.server.application.ApplicationCallPipeline.Call) {
                        val path = call.request.local.uri
                        if (path.startsWith("/api/")) {
                            val session = call.sessions.get<DivaSession>()
                            if (session?.authenticated != true) {
                                call.respondText(
                                    """{"error":"Unauthorized"}""",
                                    ContentType.Application.Json,
                                    HttpStatusCode.Unauthorized,
                                )
                                finish()
                            }
                        }
                    }

                    cardRoutes(cardRepository, rewardRepository)
                    transactionRoutes(transactionRepository)
                    graphRoutes(transactionRepository)
                }
            }.start(wait = false)
            running = true
        }
    }

    fun stop() {
        server?.stop(1000, 2000)
        server = null
        running = false
    }

    fun isRunning(): Boolean = running
}
