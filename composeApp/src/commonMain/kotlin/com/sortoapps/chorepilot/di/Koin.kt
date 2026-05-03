package com.sortoapps.chorepilot.di

import dev.brewkits.grant.di.grantModule
import dev.brewkits.grant.di.grantPlatformModule
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
            grantModule,
            grantPlatformModule,
            platformModule,
            appModule,
            viewModelModule,
        )
    }

    isKoinInitialized = true
}
