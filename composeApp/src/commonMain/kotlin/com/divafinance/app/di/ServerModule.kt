package com.divafinance.app.di

import com.divafinance.server.DivaServer
import com.divafinance.server.WebResourceProvider
import com.divafinance.server.auth.PinAuthProvider
import org.koin.dsl.module

val serverModule = module {
    single { PinAuthProvider(get()) }
    single {
        DivaServer(
            pinAuthProvider = get(),
            cardRepository = get(),
            rewardRepository = get(),
            transactionRepository = get(),
            webResources = WebResourceProvider.loadResources(),
            addressResolver = get(),
        )
    }
}
