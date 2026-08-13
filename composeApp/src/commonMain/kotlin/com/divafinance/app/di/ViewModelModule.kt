package com.divafinance.app.di

import org.koin.dsl.module

val viewModelModule = module {
    // ViewModels will be registered here as feature screens are implemented
    // in Increments 6-16. Each feature module's ViewModel will be added like:
    // viewModel { DashboardViewModel(get(), get()) }
}
