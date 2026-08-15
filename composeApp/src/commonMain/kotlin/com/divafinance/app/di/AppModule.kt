package com.divafinance.app.di

import org.koin.core.module.Module

fun appModules(): List<Module> = listOf(
    platformModule(),
    dataModule,
    domainModule,
    viewModelModule,
    serverModule,
)
