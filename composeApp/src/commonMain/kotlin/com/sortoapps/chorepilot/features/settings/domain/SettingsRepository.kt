package com.sortoapps.chorepilot.features.settings.domain

import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    val settings: StateFlow<SettingsSnapshot>

    suspend fun updateThemeMode(themeMode: ThemeMode)

    suspend fun updateAppearancePreset(appearancePreset: AppearancePreset) = Unit

    suspend fun updateEditorialWarmth(enabled: Boolean)

    suspend fun updateNotifications(enabled: Boolean)

    suspend fun updateWeekStartsOnMonday(enabled: Boolean)

    suspend fun updateAutoAssignSuggestions(enabled: Boolean)

    suspend fun updateLargerText(enabled: Boolean)

    suspend fun updateAlwaysShowLabels(enabled: Boolean)
}
