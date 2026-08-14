package com.divafinance.app.di

import com.divafinance.feature.cards.CardsViewModel
import com.divafinance.feature.dashboard.DashboardViewModel
import com.divafinance.feature.graphs.GraphsViewModel
import com.divafinance.feature.onboarding.OnboardingViewModel
import com.divafinance.feature.transactions.TransactionsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { OnboardingViewModel(get(), get()) }
    viewModel { CardsViewModel(get(), get(), get(), get()) }
    viewModel { TransactionsViewModel(get(), get(), get()) }
    viewModel { DashboardViewModel(get(), get()) }
    viewModel { GraphsViewModel(get(), get(), get()) }
}
