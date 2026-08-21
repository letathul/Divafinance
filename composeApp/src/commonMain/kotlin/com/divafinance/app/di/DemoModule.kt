package com.divafinance.app.di

import com.divafinance.feature.demo.DemoDataManager
import org.koin.dsl.module

val demoModule = module {
    single {
        DemoDataManager(
            settingsRepository = get(),
            accountRepository = get(),
            cardRepository = get(),
            rewardRepository = get(),
            transactionRepository = get(),
            receiptRepository = get(),
            feedRepository = get(),
            thresholdRepository = get(),
            personRepository = get(),
            ledgerRepository = get(),
            moduleInstaller = get(),
        )
    }
}
