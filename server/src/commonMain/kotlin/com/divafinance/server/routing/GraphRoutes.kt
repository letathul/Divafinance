package com.divafinance.server.routing

import com.divafinance.core.data.repository.TransactionRepository
import com.divafinance.core.network.dto.CategorySpendingDto
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

fun Route.graphRoutes(transactionRepository: TransactionRepository) {
    get("/api/graphs/spending") {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val startDate = now.minus(30, DateTimeUnit.DAY)
        val spending = transactionRepository.getSpendingByCategory(startDate, now)
        val total = spending.values.sum()
        val dtos = spending.map { (category, amount) ->
            CategorySpendingDto(
                category = category,
                total = amount,
                percentage = if (total > 0) (amount / total) * 100 else 0.0,
            )
        }.sortedByDescending { it.total }
        call.respond(dtos)
    }
}
