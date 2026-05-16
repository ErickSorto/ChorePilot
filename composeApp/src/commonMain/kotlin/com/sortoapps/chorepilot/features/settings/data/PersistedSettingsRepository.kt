package com.sortoapps.chorepilot.features.settings.data

import com.sortoapps.chorepilot.core.platform.PlatformPreferences
import com.sortoapps.chorepilot.features.feedbackpal.domain.FeedbackPalKind
import com.sortoapps.chorepilot.features.settings.domain.AppearancePreset
import com.sortoapps.chorepilot.features.settings.domain.SettingsRepository
import com.sortoapps.chorepilot.features.settings.domain.SettingsSnapshot
import com.sortoapps.chorepilot.features.settings.domain.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json

class PersistedSettingsRepository(
    private val platformPreferences: PlatformPreferences,
) : SettingsRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _settings = MutableStateFlow(loadState())
    override val settings: StateFlow<SettingsSnapshot> = _settings.asStateFlow()

    override suspend fun updateThemeMode(themeMode: ThemeMode) {
        updateSettings { it.copy(themeMode = themeMode) }
    }

    override suspend fun updateAppearancePreset(appearancePreset: AppearancePreset) {
        updateSettings {
            it.copy(
                appearancePreset = appearancePreset,
                editorialWarmthEnabled = appearancePreset != AppearancePreset.Classic,
            )
        }
    }

    override suspend fun updateEditorialWarmth(enabled: Boolean) {
        updateSettings {
            it.copy(
                editorialWarmthEnabled = enabled,
                appearancePreset = if (enabled) {
                    AppearancePreset.EditorialWarm
                } else {
                    AppearancePreset.Classic
                },
            )
        }
    }

    override suspend fun updateNotifications(enabled: Boolean) {
        updateSettings { it.copy(notificationsEnabled = enabled) }
    }

    override suspend fun updateWeekStartsOnMonday(enabled: Boolean) {
        updateSettings { it.copy(weekStartsOnMonday = enabled) }
    }

    override suspend fun updateAutoAssignSuggestions(enabled: Boolean) {
        updateSettings { it.copy(autoAssignSuggestions = enabled) }
    }

    override suspend fun updateLargerText(enabled: Boolean) {
        updateSettings { it.copy(largerText = enabled) }
    }

    override suspend fun updateAlwaysShowLabels(enabled: Boolean) {
        updateSettings { it.copy(alwaysShowLabels = enabled) }
    }

    override suspend fun updateFeedbackPalKind(kind: FeedbackPalKind) {
        updateSettings { it.copy(feedbackPalKind = kind) }
    }

    private fun loadState(): SettingsSnapshot {
        val persisted = platformPreferences.getString(SETTINGS_KEY) ?: return SettingsSnapshot()
        return runCatching {
            json.decodeFromString<SettingsSnapshot>(persisted)
        }.getOrElse {
            SettingsSnapshot()
        }
    }

    private fun updateSettings(transform: (SettingsSnapshot) -> SettingsSnapshot) {
        _settings.update(transform)
        platformPreferences.putString(
            SETTINGS_KEY,
            json.encodeToString(SettingsSnapshot.serializer(), _settings.value),
        )
    }

    private companion object {
        const val SETTINGS_KEY = "settings_snapshot"
    }
}
