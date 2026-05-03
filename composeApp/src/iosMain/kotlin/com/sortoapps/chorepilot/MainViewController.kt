package com.sortoapps.chorepilot

import androidx.compose.ui.window.ComposeUIViewController
import com.sortoapps.chorepilot.di.initKoin
import com.sortoapps.chorepilot.di.resolveIosFirebaseAvailability

fun MainViewController() = run {
    resolveIosFirebaseAvailability()
    initKoin()
    ComposeUIViewController { App() }
}
