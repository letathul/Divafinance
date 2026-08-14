package com.divafinance.server.routing

import com.divafinance.core.data.repository.TransactionRepository
import com.divafinance.core.network.dto.TransactionDto
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.coroutines.flow.first

fun Route.transactionRoutes(transactionRepository: TransactionRepository) {
    get("/api/transactions") {
        val transactions = transactionRepository.getAll().first()
        val dtos = transactions.map { txn ->
            TransactionDto(
                id = txn.id,
                accountId = txn.accountId,
                cardId = txn.cardId,
                amount = txn.amount,
                currency = txn.currency,
                category = txn.category.name,
                merchantName = txn.merchantName,
                note = txn.note,
                date = txn.date.toString(),
                type = txn.type.name,
                latitude = txn.location?.latitude,
                longitude = txn.location?.longitude,
                locationName = txn.location?.name,
            )
        }
        call.respond(dtos)
    }

    get("/api/transactions/{id}") {
        val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        val txn = transactionRepository.getById(id)
            ?: return@get call.respond(HttpStatusCode.NotFound)
        call.respond(
            TransactionDto(
                id = txn.id,
                accountId = txn.accountId,
                cardId = txn.cardId,
                amount = txn.amount,
                currency = txn.currency,
                category = txn.category.name,
                merchantName = txn.merchantName,
                note = txn.note,
                date = txn.date.toString(),
                type = txn.type.name,
                latitude = txn.location?.latitude,
                longitude = txn.location?.longitude,
                locationName = txn.location?.name,
            ),
        )
    }
}
