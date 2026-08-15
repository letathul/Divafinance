package com.divafinance.server.routing

import com.divafinance.core.data.repository.CardRepository
import com.divafinance.core.data.repository.RewardRepository
import com.divafinance.core.data.repository.TransactionRepository
import com.divafinance.server.auth.PinAuthProvider
import io.ktor.http.ContentType
import io.ktor.server.routing.Route

fun Route.configureRoutes(
    pinAuthProvider: PinAuthProvider,
    cardRepository: CardRepository,
    rewardRepository: RewardRepository,
    transactionRepository: TransactionRepository,
    webResources: Map<String, Pair<String, ContentType>>,
) {
    authRoutes(pinAuthProvider)
    cardRoutes(cardRepository, rewardRepository)
    transactionRoutes(transactionRepository)
    graphRoutes(transactionRepository)
    staticRoutes(webResources)
}
