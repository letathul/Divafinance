package com.divafinance.core.domain.usecase.scanner

import com.divafinance.core.data.repository.TransactionRepository
import com.divafinance.core.domain.engine.CategoryPredictionEngine
import com.divafinance.core.domain.engine.PredictionContext
import com.divafinance.core.model.Transaction
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.model.enums.TransactionType
import kotlinx.coroutines.flow.first
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ImportStatementUseCase(
    private val transactionRepository: TransactionRepository,
    private val categoryEngine: CategoryPredictionEngine = CategoryPredictionEngine(),
) {
    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(
        csvContent: String,
        accountId: String,
        cardId: String?,
    ): Int {
        val lines = csvContent.lines().drop(1).filter { it.isNotBlank() }
        var imported = 0

        // Read once, before the loop: a statement is imported wholesale, and re-reading
        // per row would make a 300-line export 300 queries. Rows imported in this pass
        // deliberately don't feed the next one — every row is judged against the history
        // that existed before the import, so the result doesn't depend on file order.
        val history = runCatching { transactionRepository.getAll().first() }
            .getOrDefault(emptyList())
        val zone = TimeZone.currentSystemDefault()

        for (line in lines) {
            val fields = parseCsvLine(line)
            if (fields.size < 3) continue

            val date = runCatching { LocalDate.parse(fields[0].trim()) }.getOrNull() ?: continue
            val description = fields[1].trim()
            val amount = fields[2].trim().replace(",", "").toDoubleOrNull() ?: continue

            val type = if (amount < 0) TransactionType.DEBIT else TransactionType.CREDIT
            val transaction = Transaction(
                id = Uuid.random().toString(),
                accountId = accountId,
                cardId = cardId,
                amount = kotlin.math.abs(amount),
                category = categoryFor(description, amount, date, type, history, zone),
                merchantName = description,
                date = date,
                type = type,
                createdAt = Clock.System.now(),
            )
            transactionRepository.insert(transaction)
            imported++
        }

        return imported
    }

    /**
     * The statement's description is a merchant name, which is the engine's strongest
     * signal — so an imported row lands in a real category rather than all of them landing
     * in [SpendingCategory.OTHER].
     *
     * Still falls back to `OTHER`: income has no spending category, and a first import on
     * an empty history has nothing to reason from. A guess is not a claim — this is the
     * same ranking the category chips offer during manual entry, and it is as correctable.
     */
    private fun categoryFor(
        description: String,
        amount: Double,
        date: LocalDate,
        type: TransactionType,
        history: List<Transaction>,
        zone: TimeZone,
    ): SpendingCategory {
        if (type != TransactionType.DEBIT || description.isBlank()) return SpendingCategory.OTHER
        return categoryEngine.predict(
            history = history,
            context = PredictionContext(
                merchantName = description,
                amount = kotlin.math.abs(amount),
                // The statement gives a date but no time. Midnight is the honest reading,
                // and the time-of-day signal is only one term among four.
                at = date.atStartOfDayIn(zone).toLocalDateTime(zone),
                zone = zone,
            ),
            limit = 1,
        ).firstOrNull()?.category ?: SpendingCategory.OTHER
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString())
        return result
    }
}
