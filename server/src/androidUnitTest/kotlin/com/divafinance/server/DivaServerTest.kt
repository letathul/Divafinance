package com.divafinance.server

import com.divafinance.core.common.SecurityUtils
import com.divafinance.core.data.repository.CardRepository
import com.divafinance.core.data.repository.RewardRepository
import com.divafinance.core.data.repository.SettingsRepository
import com.divafinance.core.data.repository.TransactionRepository
import com.divafinance.core.model.CardRewardRule
import com.divafinance.core.model.CreditCard
import com.divafinance.core.model.Transaction
import com.divafinance.core.model.UserSettings
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.server.auth.PinAuthProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import java.net.ServerSocket
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.LocalDate

private const val PIN = "1234"
private const val TEST_ADDRESS = "192.168.1.20"

class DivaServerTest {

    private val server = DivaServer(
        pinAuthProvider = PinAuthProvider(FakeSettingsRepository(PIN)),
        cardRepository = FakeCardRepository,
        rewardRepository = FakeRewardRepository,
        transactionRepository = FakeTransactionRepository,
        webResources = WebResourceProvider.loadResources(),
        addressResolver = { TEST_ADDRESS },
    )

    @AfterTest
    fun tearDown() = server.stop()

    @Test
    fun `serves the web ui and guards the api behind the pin`() = runBlocking {
        val port = freePort()
        server.start(ServerConfig(port = port))

        val running = withTimeout(SERVER_START_TIMEOUT_MS) {
            server.state.first { it !is ServerState.Starting && it !is ServerState.Stopped }
        }
        assertEquals(ServerState.Running(TEST_ADDRESS, port), running)

        HttpClient(CIO) { install(HttpCookies) }.use { client ->
            val base = "http://127.0.0.1:$port"

            val index = client.get("$base/")
            assertEquals(HttpStatusCode.OK, index.status)
            assertTrue(index.bodyAsText().contains("<title>Diva Finance</title>"))

            // Unauthenticated: the guard must reject before any repository is touched.
            assertEquals(HttpStatusCode.Unauthorized, client.get("$base/api/cards").status)
            assertEquals(HttpStatusCode.Unauthorized, client.get("$base/api/transactions").status)
            assertEquals(HttpStatusCode.Unauthorized, client.get("$base/api/graphs/spending").status)

            val badLogin = client.post("$base/auth/login") {
                contentType(ContentType.Application.Json)
                setBody("""{"pin":"9999"}""")
            }
            assertEquals(HttpStatusCode.Unauthorized, badLogin.status)

            val login = client.post("$base/auth/login") {
                contentType(ContentType.Application.Json)
                setBody("""{"pin":"$PIN"}""")
            }
            assertEquals(HttpStatusCode.OK, login.status)

            // The session cookie now unlocks every guarded route.
            assertEquals(HttpStatusCode.OK, client.get("$base/api/cards").status)
            assertEquals(HttpStatusCode.OK, client.get("$base/api/transactions").status)
            assertEquals(HttpStatusCode.OK, client.get("$base/api/graphs/spending").status)
            assertTrue(client.get("$base/auth/session").bodyAsText().contains("\"success\": true"))

            assertEquals(HttpStatusCode.OK, client.post("$base/auth/logout").status)
            assertEquals(HttpStatusCode.Unauthorized, client.get("$base/api/cards").status)
        }
    }

    @Test
    fun `reports a bind failure instead of silently staying stopped`() = runBlocking {
        val port = freePort()
        ServerSocket(port).use {
            server.start(ServerConfig(port = port))
            val state = withTimeout(SERVER_START_TIMEOUT_MS) {
                server.state.first { it is ServerState.Failed || it is ServerState.Running }
            }
            assertTrue(state is ServerState.Failed, "expected a Failed state but was $state")
        }
    }

    private fun freePort(): Int = ServerSocket(0).use { it.localPort }

    private companion object {
        const val SERVER_START_TIMEOUT_MS = 15_000L
    }
}

private class FakeSettingsRepository(pin: String) : SettingsRepository {
    private val salt = SecurityUtils.generateSalt()
    private val values = mutableMapOf(
        UserSettings.KEY_PIN_SALT to salt,
        UserSettings.KEY_PIN_HASH to SecurityUtils.hashPin(pin, salt),
    )

    override fun getAll(): Flow<List<UserSettings>> =
        flowOf(values.map { UserSettings(it.key, it.value) })

    override suspend fun get(key: String): String? = values[key]
    override suspend fun set(key: String, value: String) { values[key] = value }
    override suspend fun delete(key: String) { values.remove(key) }
    override suspend fun deleteAll() = values.clear()
}

private object FakeCardRepository : CardRepository {
    override fun getAll(): Flow<List<CreditCard>> = flowOf(emptyList())
    override suspend fun getById(id: String): CreditCard? = null
    override suspend fun getByAccountId(accountId: String): List<CreditCard> = emptyList()
    override suspend fun insert(card: CreditCard) = Unit
    override suspend fun update(card: CreditCard) = Unit
    override suspend fun updateBalance(id: String, balance: Double) = Unit
    override suspend fun delete(id: String) = Unit
    override suspend fun count(): Long = 0
}

private object FakeRewardRepository : RewardRepository {
    override fun getAll(): Flow<List<CardRewardRule>> = flowOf(emptyList())
    override suspend fun getByCardId(cardId: String): List<CardRewardRule> = emptyList()
    override suspend fun getByCategory(category: SpendingCategory): List<CardRewardRule> = emptyList()
    override suspend fun insert(rule: CardRewardRule) = Unit
    override suspend fun update(rule: CardRewardRule) = Unit
    override suspend fun deleteByCardId(cardId: String) = Unit
    override suspend fun delete(id: String) = Unit
}

private object FakeTransactionRepository : TransactionRepository {
    override fun getAll(): Flow<List<Transaction>> = flowOf(emptyList())
    override suspend fun getById(id: String): Transaction? = null
    override suspend fun getByAccountId(accountId: String): List<Transaction> = emptyList()
    override suspend fun getByCategory(category: String): List<Transaction> = emptyList()
    override suspend fun getByDateRange(startDate: LocalDate, endDate: LocalDate): List<Transaction> = emptyList()
    override suspend fun getByCategoryAndDateRange(
        category: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<Transaction> = emptyList()
    override suspend fun getWithLocation(): List<Transaction> = emptyList()
    override suspend fun getKnownMerchants(): List<String> = emptyList()
    override suspend fun getSpendingByCategory(startDate: LocalDate, endDate: LocalDate): Map<String, Double> =
        emptyMap()
    override suspend fun getTotalSpending(startDate: LocalDate, endDate: LocalDate): Double? = null
    override suspend fun insert(transaction: Transaction) = Unit
    override suspend fun update(transaction: Transaction) = Unit
    override suspend fun delete(id: String) = Unit
    override suspend fun count(): Long = 0
}
