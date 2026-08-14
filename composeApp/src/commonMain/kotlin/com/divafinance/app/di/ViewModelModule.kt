package com.divafinance.app.di

import com.divafinance.feature.cards.CardsViewModel
import com.divafinance.feature.onboarding.OnboardingViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { OnboardingViewModel(get(), get()) }
    viewModel { CardsViewModel(get(), get(), get(), get()) }
}
