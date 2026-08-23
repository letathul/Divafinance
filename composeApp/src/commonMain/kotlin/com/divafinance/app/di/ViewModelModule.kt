package com.divafinance.app.di

import com.divafinance.feature.activity.ActivityViewModel
import com.divafinance.feature.activity.PersonDetailViewModel
import com.divafinance.feature.backup.BackupViewModel
import com.divafinance.feature.cards.CardsViewModel
import com.divafinance.feature.feed.FeedViewModel
import com.divafinance.feature.graphs.GraphsViewModel
import com.divafinance.feature.map.MapViewModel
import com.divafinance.feature.onboarding.OnboardingViewModel
import com.divafinance.feature.automation.AutomationViewModel
import com.divafinance.feature.quickadd.QuickAddViewModel
import com.divafinance.feature.scanner.ScannerViewModel
import com.divafinance.feature.scanner.ocr.OcrEngine
import com.divafinance.feature.scanner.review.ReceiptReviewViewModel
import com.divafinance.feature.settings.AppearanceStore
import com.divafinance.feature.settings.YouViewModel
import com.divafinance.feature.settings.SettingsViewModel
import com.divafinance.feature.transactions.ReportViewModel
import com.divafinance.feature.transactions.TransactionsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    single { OcrEngine() }
    // Held as a singleton so the app root and the settings screen read one stream.
    single { AppearanceStore(get()) }
    viewModel { OnboardingViewModel(get(), get(), get()) }
    viewModel { CardsViewModel(get(), get(), get(), get()) }
    viewModel { TransactionsViewModel(get(), get(), get(), get()) }
    viewModel { ActivityViewModel(get(), get()) }
    // personId comes from the nav argument, so it is passed in rather than resolved.
    viewModel { params -> PersonDetailViewModel(params.get(), get(), get()) }
    viewModel { QuickAddViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { GraphsViewModel(get(), get(), get()) }
    viewModel { BackupViewModel(get(), get(), get()) }
    viewModel { FeedViewModel(get(), get()) }
    viewModel { YouViewModel(get(), get(), get()) }
    // Period and anchor come from the nav route, so they are passed in rather than resolved.
    viewModel { params -> ReportViewModel(params.get(), params.get(), get(), get()) }
    viewModel { MapViewModel(get(), get()) }
    viewModel { ScannerViewModel(get(), get(), get(), get(), get(), get()) }
    // Scoped to one receipt, so the id comes from the nav route rather than the graph.
    viewModel { params ->
        ReceiptReviewViewModel(params.get(), get(), get(), get(), get(), get(), get())
    }
    viewModel { AutomationViewModel(get(), get()) }
    viewModel { SettingsViewModel(get(), get(), get(), get(), get(), get()) }
}
