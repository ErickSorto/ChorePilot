package com.sortoapps.chorepilot

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import com.sortoapps.chorepilot.app.presentation.AppScaffold
import com.sortoapps.chorepilot.core.designsystem.ChorePilotTheme
import com.sortoapps.chorepilot.core.revenuecat.SubscriptionRepository
import com.sortoapps.chorepilot.navigation.ChorePilotNavHost
import com.sortoapps.chorepilot.features.settings.domain.SettingsRepository
import org.koin.compose.koinInject

@Composable
@Preview
fun App() {
    koinInject<SubscriptionRepository>()
    val settingsRepository = koinInject<SettingsRepository>()
    val settings by settingsRepository.settings.collectAsState()

    ChorePilotTheme(
        themeMode = settings.themeMode,
        appearancePreset = settings.appearancePreset,
        largerTextEnabled = settings.largerText,
    ) {
        AppScaffold {
            ChorePilotNavHost()
        }
    }
}
