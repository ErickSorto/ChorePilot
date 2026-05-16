package com.sortoapps.chorepilot.features.settings.domain

import com.sortoapps.chorepilot.features.feedbackpal.domain.FeedbackPalKind
import kotlinx.serialization.Serializable

@Serializable
enum class ThemeMode {
    Light,
    Dark,
}

@Serializable
data class SettingsSnapshot(
    val themeMode: ThemeMode = ThemeMode.Light,
    val editorialWarmthEnabled: Boolean = false,
    val appearancePreset: AppearancePreset = if (editorialWarmthEnabled) {
        AppearancePreset.EditorialWarm
    } else {
        AppearancePreset.Classic
    },
    val notificationsEnabled: Boolean = true,
    val weekStartsOnMonday: Boolean = false,
    val autoAssignSuggestions: Boolean = true,
    val largerText: Boolean = false,
    val alwaysShowLabels: Boolean = true,
    val feedbackPalKind: FeedbackPalKind = FeedbackPalKind.Squirrel,
)
