package com.divafinance.app.di

import com.divafinance.core.domain.usecase.backup.ExportBackupUseCase
import com.divafinance.core.domain.usecase.backup.ImportBackupUseCase
import com.divafinance.core.domain.usecase.cards.AddCardUseCase
import com.divafinance.core.domain.usecase.cards.CalculateRewardValueUseCase
import com.divafinance.core.domain.usecase.cards.GetAllCardsUseCase
import com.divafinance.core.domain.usecase.cards.GetBestCardForCategoryUseCase
import com.divafinance.core.domain.usecase.cards.UpdateCardUseCase
import com.divafinance.core.domain.usecase.feed.GenerateDailyInsightUseCase
import com.divafinance.core.domain.usecase.feed.GetFeedPostsUseCase
import com.divafinance.core.domain.usecase.feed.PostTransactionToFeedUseCase
import com.divafinance.core.domain.usecase.graphs.ConfigureThresholdUseCase
import com.divafinance.core.domain.usecase.graphs.GetThresholdGraphDataUseCase
import com.divafinance.core.domain.usecase.location.GetSpendingByLocationUseCase
import com.divafinance.core.domain.usecase.location.TagTransactionLocationUseCase
import com.divafinance.core.domain.usecase.onboarding.CompleteOnboardingUseCase
import com.divafinance.core.domain.usecase.onboarding.InitializeDatabaseUseCase
import com.divafinance.core.domain.usecase.onboarding.ValidatePinUseCase
import com.divafinance.core.domain.usecase.scanner.ImportStatementUseCase
import com.divafinance.core.domain.usecase.scanner.ParseReceiptUseCase
import com.divafinance.core.domain.usecase.transactions.AddTransactionUseCase
import com.divafinance.core.domain.usecase.transactions.GetHighImpactTransactionsUseCase
import com.divafinance.core.domain.usecase.transactions.GetSpendingByCategoryUseCase
import com.divafinance.core.domain.usecase.transactions.GetTransactionsUseCase
import org.koin.dsl.module

val domainModule = module {
    // Cards
    factory { GetAllCardsUseCase(get()) }
    factory { GetBestCardForCategoryUseCase(get(), get(), get()) }
    factory { AddCardUseCase(get(), get()) }
    factory { UpdateCardUseCase(get(), get()) }
    factory { CalculateRewardValueUseCase(get()) }

    // Transactions
    factory { AddTransactionUseCase(get(), get()) }
    factory { GetTransactionsUseCase(get()) }
    factory { GetSpendingByCategoryUseCase(get()) }
    factory { GetHighImpactTransactionsUseCase(get(), get()) }

    // Onboarding
    factory { CompleteOnboardingUseCase(get()) }
    factory { ValidatePinUseCase(get()) }
    factory { InitializeDatabaseUseCase(get()) }

    // Backup
    factory { ExportBackupUseCase(get()) }
    factory { ImportBackupUseCase(get()) }

    // Graphs
    factory { GetThresholdGraphDataUseCase(get(), get()) }
    factory { ConfigureThresholdUseCase(get()) }

    // Location
    factory { TagTransactionLocationUseCase(get()) }
    factory { GetSpendingByLocationUseCase(get()) }

    // Feed
    factory { GetFeedPostsUseCase(get()) }
    factory { PostTransactionToFeedUseCase(get()) }
    factory { GenerateDailyInsightUseCase(get(), get()) }

    // Scanner
    factory { ParseReceiptUseCase(get()) }
    factory { ImportStatementUseCase(get()) }
}
