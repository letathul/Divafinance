package com.divafinance.server.routing

import com.divafinance.core.data.repository.CardRepository
import com.divafinance.core.data.repository.RewardRepository
import com.divafinance.core.network.dto.CardDto
import com.divafinance.core.network.dto.RewardRuleDto
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.coroutines.flow.first

fun Route.cardRoutes(
    cardRepository: CardRepository,
    rewardRepository: RewardRepository,
) {
    get("/api/cards") {
        val cards = cardRepository.getAll().first()
        val dtos = cards.map { card ->
            val rules = rewardRepository.getByCardId(card.id)
            CardDto(
                id = card.id,
                accountId = card.accountId,
                name = card.name,
                lastFour = card.lastFour,
                network = card.network.name,
                color = card.color,
                creditLimit = card.creditLimit,
                currentBalance = card.currentBalance,
                availableCredit = card.creditLimit - card.currentBalance,
                statementDate = card.statementDate,
                dueDate = card.dueDate,
                annualFee = card.annualFee,
                rewardRules = rules.map { rule ->
                    RewardRuleDto(
                        id = rule.id,
                        category = rule.category.name,
                        multiplier = rule.multiplier,
                        rewardType = rule.rewardType.name,
                        capAmount = rule.capAmount,
                        capPeriod = rule.capPeriod?.name,
                    )
                },
            )
        }
        call.respond(dtos)
    }

    get("/api/cards/{id}") {
        val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        val card = cardRepository.getById(id)
            ?: return@get call.respond(HttpStatusCode.NotFound)
        val rules = rewardRepository.getByCardId(card.id)
        call.respond(
            CardDto(
                id = card.id,
                accountId = card.accountId,
                name = card.name,
                lastFour = card.lastFour,
                network = card.network.name,
                color = card.color,
                creditLimit = card.creditLimit,
                currentBalance = card.currentBalance,
                availableCredit = card.creditLimit - card.currentBalance,
                statementDate = card.statementDate,
                dueDate = card.dueDate,
                annualFee = card.annualFee,
                rewardRules = rules.map { rule ->
                    RewardRuleDto(
                        id = rule.id,
                        category = rule.category.name,
                        multiplier = rule.multiplier,
                        rewardType = rule.rewardType.name,
                        capAmount = rule.capAmount,
                        capPeriod = rule.capPeriod?.name,
                    )
                },
            ),
        )
    }
}
