package com.sortoapps.chorepilot.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

expect val platformModule: Module

private var isKoinInitialized = false

fun initKoin(config: KoinAppDeclaration? = null) {
    if (isKoinInitialized) {
        return
    }

    startKoin {
        config?.invoke(this)
        modules(
            platformModule,
            appModule,
            viewModelModule,
        )
    }

    isKoinInitialized = true
}
