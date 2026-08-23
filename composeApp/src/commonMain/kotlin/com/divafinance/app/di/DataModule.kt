package com.divafinance.app.di

import com.divafinance.core.common.FileSystem
import com.divafinance.core.data.repository.AccountRepository
import com.divafinance.core.data.repository.AccountRepositoryImpl
import com.divafinance.core.data.repository.BackupRepository
import com.divafinance.core.data.repository.BackupRepositoryImpl
import com.divafinance.core.data.repository.CardRepository
import com.divafinance.core.data.repository.CardRepositoryImpl
import com.divafinance.core.data.repository.FeedRepository
import com.divafinance.core.data.repository.FeedRepositoryImpl
import com.divafinance.core.data.repository.LedgerRepository
import com.divafinance.core.data.repository.LedgerRepositoryImpl
import com.divafinance.core.data.repository.PersonRepository
import com.divafinance.core.data.repository.PersonRepositoryImpl
import com.divafinance.core.data.repository.ReceiptFileStore
import com.divafinance.core.data.repository.ReceiptRepository
import com.divafinance.core.data.repository.ReceiptRepositoryImpl
import com.divafinance.core.data.repository.RewardRepository
import com.divafinance.core.data.repository.RewardRepositoryImpl
import com.divafinance.core.data.repository.SettingsRepository
import com.divafinance.core.data.repository.SettingsRepositoryImpl
import com.divafinance.core.data.repository.ThresholdRepository
import com.divafinance.core.data.repository.ThresholdRepositoryImpl
import com.divafinance.core.data.repository.TransactionRepository
import com.divafinance.core.data.repository.TransactionRepositoryImpl
import com.divafinance.core.database.DatabaseDriverFactory
import com.divafinance.core.database.DivaFinanceDb
import org.koin.dsl.module

val dataModule = module {
    single { get<DatabaseDriverFactory>().create() }
    single { DivaFinanceDb(get()) }

    single<AccountRepository> { AccountRepositoryImpl(get()) }
    single<RewardRepository> { RewardRepositoryImpl(get()) }
    single<CardRepository> { CardRepositoryImpl(get(), get()) }
    single<TransactionRepository> { TransactionRepositoryImpl(get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
    single<ReceiptRepository> { ReceiptRepositoryImpl(get()) }
    // The only binding here backed by the filesystem rather than the database. `FileSystem`
    // comes from `platformModule()`, which is why this can't live in :core:data itself.
    single<ReceiptFileStore> { ReceiptFileStore(get<FileSystem>()::deleteFile) }
    single<FeedRepository> { FeedRepositoryImpl(get()) }
    single<BackupRepository> { BackupRepositoryImpl(get()) }
    single<ThresholdRepository> { ThresholdRepositoryImpl(get()) }
    single<PersonRepository> { PersonRepositoryImpl(get()) }
    single<LedgerRepository> { LedgerRepositoryImpl(get()) }
}
