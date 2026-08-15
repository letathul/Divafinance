package com.divafinance.core.domain.usecase.scanner

import com.divafinance.core.data.repository.TransactionRepository
import com.divafinance.core.model.Transaction
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.model.enums.TransactionType
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ImportStatementUseCase(
    private val transactionRepository: TransactionRepository
) {
    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(
        csvContent: String,
        accountId: String,
        cardId: String?,
    ): Int {
        val lines = csvContent.lines().drop(1).filter { it.isNotBlank() }
        var imported = 0

        for (line in lines) {
            val fields = parseCsvLine(line)
            if (fields.size < 3) continue

            val date = runCatching { LocalDate.parse(fields[0].trim()) }.getOrNull() ?: continue
            val description = fields[1].trim()
            val amount = fields[2].trim().replace(",", "").toDoubleOrNull() ?: continue

            val transaction = Transaction(
                id = Uuid.random().toString(),
                accountId = accountId,
                cardId = cardId,
                amount = kotlin.math.abs(amount),
                category = SpendingCategory.OTHER,
                merchantName = description,
                date = date,
                type = if (amount < 0) TransactionType.DEBIT else TransactionType.CREDIT,
                createdAt = Clock.System.now(),
            )
            transactionRepository.insert(transaction)
            imported++
        }

        return imported
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
