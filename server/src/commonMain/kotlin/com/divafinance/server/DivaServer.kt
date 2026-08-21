package com.divafinance.server

import com.divafinance.core.data.repository.CardRepository
import com.divafinance.core.data.repository.RewardRepository
import com.divafinance.core.data.repository.TransactionRepository
import com.divafinance.server.auth.DivaSession
import com.divafinance.server.auth.PinAuthProvider
import com.divafinance.server.routing.SESSION_AUTH
import com.divafinance.server.routing.authRoutes
import com.divafinance.server.routing.cardRoutes
import com.divafinance.server.routing.graphRoutes
import com.divafinance.server.routing.staticRoutes
import com.divafinance.server.routing.transactionRoutes
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.session
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class DivaServer(
    private val pinAuthProvider: PinAuthProvider,
    private val cardRepository: CardRepository,
    private val rewardRepository: RewardRepository,
    private val transactionRepository: TransactionRepository,
    private val webResources: Map<String, Pair<String, ContentType>> = emptyMap(),
    private val addressResolver: LocalAddressResolver = NoLocalAddressResolver,
) {
    private var server: EmbeddedServer<*, *>? = null
    private var startJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _state = MutableStateFlow<ServerState>(ServerState.Stopped)

    /**
     * The single source of truth for the UI. Binding happens off the caller's thread and
     * can fail, so callers observe this instead of trusting that `start()` worked.
     */
    val state: StateFlow<ServerState> = _state.asStateFlow()

    fun start(config: ServerConfig = ServerConfig()) {
        if (_state.value.isActive) return
        _state.value = ServerState.Starting

        startJob = scope.launch {
            try {
                val engine = embeddedServer(
                    factory = CIO,
                    port = config.port,
                    host = config.host,
                    // Ktor otherwise watches the working directory for auto-reload,
                    // which on Android only produces ClosedWatchServiceException noise
                    // from the finalizer.
                    watchPaths = emptyList(),
                ) {
                    configure()
                }
                // CIO surfaces bind failures (port in use, permission denied) out of
                // start(), so this try/catch is what turns them into a visible state.
                engine.start(wait = false)
                server = engine
                _state.value = ServerState.Running(addressResolver.lanAddress(), config.port)
            } catch (cause: Throwable) {
                server = null
                _state.value = ServerState.Failed(
                    cause.message ?: "Could not start the server on port ${config.port}",
                )
            }
        }
    }

    fun stop() {
        startJob?.cancel()
        startJob = null
        server?.stop(GRACE_PERIOD_MS, TIMEOUT_MS)
        server = null
        _state.value = ServerState.Stopped
    }

    fun isRunning(): Boolean = _state.value is ServerState.Running

    private fun io.ktor.server.application.Application.configure() {
        install(ContentNegotiation) {
            json(
                Json {
                    prettyPrint = true
                    ignoreUnknownKeys = true
                },
            )
        }

        install(Sessions) {
            cookie<DivaSession>("DIVA_SESSION") {
                cookie.path = "/"
                cookie.httpOnly = true
            }
        }

        // Guards every /api route as a group. The previous hand-rolled pipeline
        // interceptor depended on Routing internals; this is the framework's own path.
        install(Authentication) {
            session<DivaSession>(SESSION_AUTH) {
                validate { session -> session.takeIf { it.authenticated } }
                challenge {
                    call.respondText(
                        """{"error":"Unauthorized"}""",
                        ContentType.Application.Json,
                        HttpStatusCode.Unauthorized,
                    )
                }
            }
        }

        install(StatusPages) {
            // A malformed request is the client's fault, not ours; without this it was
            // reported as a 500 (e.g. an HTTP/1.0 request with no Host header).
            exception<BadRequestException> { call, cause ->
                call.respondText(
                    """{"error":"${cause.message ?: "Bad request"}"}""",
                    ContentType.Application.Json,
                    HttpStatusCode.BadRequest,
                )
            }
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

            authenticate(SESSION_AUTH) {
                cardRoutes(cardRepository, rewardRepository)
                transactionRoutes(transactionRepository)
                graphRoutes(transactionRepository)
            }
        }
    }

    private companion object {
        const val GRACE_PERIOD_MS = 1_000L
        const val TIMEOUT_MS = 2_000L
    }
}
