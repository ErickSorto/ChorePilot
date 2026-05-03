package com.sortoapps.chorepilot.features.settings.presentation.viewmodel

import com.sortoapps.chorepilot.features.settings.domain.AppearancePreset
import com.sortoapps.chorepilot.features.settings.domain.ThemeMode

sealed interface SettingsUiEvent {
    data object TriggerVibration : SettingsUiEvent

    data object TriggerVibrationSuccess : SettingsUiEvent

    data class ThemeModeSelected(val themeMode: ThemeMode) : SettingsUiEvent

    data class AppearancePresetSelected(val appearancePreset: AppearancePreset) : SettingsUiEvent

    data class ToggleEditorialWarmth(val enabled: Boolean) : SettingsUiEvent

    data class ToggleNotifications(val enabled: Boolean) : SettingsUiEvent

    data class ToggleWeekStartsOnMonday(val enabled: Boolean) : SettingsUiEvent

    data class ToggleAutoAssignSuggestions(val enabled: Boolean) : SettingsUiEvent

    data class ToggleLargerText(val enabled: Boolean) : SettingsUiEvent

    data class ToggleAlwaysShowLabels(val enabled: Boolean) : SettingsUiEvent

    data object ResetDemo : SettingsUiEvent

    data object ClearSaveFeedback : SettingsUiEvent
}
