package com.divafinance.app.di

import com.divafinance.feature.backup.BackupViewModel
import com.divafinance.feature.cards.CardsViewModel
import com.divafinance.feature.feed.FeedViewModel
import com.divafinance.feature.dashboard.DashboardViewModel
import com.divafinance.feature.graphs.GraphsViewModel
import com.divafinance.feature.map.MapViewModel
import com.divafinance.feature.onboarding.OnboardingViewModel
import com.divafinance.feature.automation.AutomationHandler
import com.divafinance.feature.automation.AutomationViewModel
import com.divafinance.feature.scanner.ScannerViewModel
import com.divafinance.feature.scanner.ocr.OcrEngine
import com.divafinance.feature.settings.SettingsViewModel
import com.divafinance.feature.transactions.TransactionsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    single { OcrEngine() }
    single { AutomationHandler() }
    viewModel { OnboardingViewModel(get(), get(), get()) }
    viewModel { CardsViewModel(get(), get(), get(), get()) }
    viewModel { TransactionsViewModel(get(), get(), get(), get()) }
    viewModel { DashboardViewModel(get(), get()) }
    viewModel { GraphsViewModel(get(), get(), get()) }
    viewModel { BackupViewModel(get(), get(), get()) }
    viewModel { FeedViewModel(get(), get()) }
    viewModel { MapViewModel(get(), get()) }
    viewModel { ScannerViewModel(get(), get(), get()) }
    viewModel { AutomationViewModel(get()) }
    viewModel { SettingsViewModel(get(), get(), get(), get()) }
}
