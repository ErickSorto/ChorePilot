package com.sortoapps.chorepilot.features.settings.presentation.viewmodel

import com.sortoapps.chorepilot.features.settings.domain.AppearancePreset
import com.sortoapps.chorepilot.features.settings.domain.ThemeMode

data class SettingsUiState(
    val displayName: String = "Erick",
    val email: String = "",
    val themeMode: ThemeMode = ThemeMode.Light,
    val appearancePreset: AppearancePreset = AppearancePreset.Classic,
    val notificationsEnabled: Boolean = true,
    val weekStartsOnMonday: Boolean = false,
    val autoAssignSuggestions: Boolean = true,
    val largerText: Boolean = false,
    val alwaysShowLabels: Boolean = true,
    val isUpdatingNotifications: Boolean = false,
    val saveFeedbackMessage: String? = null,
    val showDebugActions: Boolean = false,
)
