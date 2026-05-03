package com.sortoapps.chorepilot.features.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sortoapps.chorepilot.core.platform.AppBuildInfo
import com.sortoapps.chorepilot.core.platform.VibratorUtil
import com.sortoapps.chorepilot.core.ui.SnackbarController
import com.sortoapps.chorepilot.core.ui.SnackbarEvent
import com.sortoapps.chorepilot.core.ui.StringValue
import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import com.sortoapps.chorepilot.features.notifications.domain.NotificationSettingsController
import com.sortoapps.chorepilot.features.notifications.domain.NotificationSettingsUpdateResult
import com.sortoapps.chorepilot.features.settings.domain.AppearancePreset
import com.sortoapps.chorepilot.features.settings.domain.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val notificationSettingsController: NotificationSettingsController,
    private val sessionRepository: SessionRepository,
    private val vibratorUtil: VibratorUtil,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionRepository.session.collect { session ->
                _uiState.update {
                    it.copy(
                        displayName = session.displayName.ifBlank { "Erick" },
                        email = session.email,
                        showDebugActions = AppBuildInfo.isDebugBuild,
                    )
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update {
                    it.copy(
                        themeMode = settings.themeMode,
                        appearancePreset = settings.appearancePreset,
                        notificationsEnabled = settings.notificationsEnabled,
                        weekStartsOnMonday = settings.weekStartsOnMonday,
                        autoAssignSuggestions = settings.autoAssignSuggestions,
                        largerText = settings.largerText,
                        alwaysShowLabels = settings.alwaysShowLabels,
                        isUpdatingNotifications = it.isUpdatingNotifications,
                        showDebugActions = AppBuildInfo.isDebugBuild,
                    )
                }
            }
        }
        viewModelScope.launch {
            notificationSettingsController.reconcileWithSystem()
        }
    }

    fun onEvent(event: SettingsUiEvent) {
        viewModelScope.launch {
            when (event) {
                SettingsUiEvent.TriggerVibration -> vibratorUtil.triggerVibration()
                SettingsUiEvent.TriggerVibrationSuccess -> vibratorUtil.triggerVibrationSuccess()
                is SettingsUiEvent.ThemeModeSelected -> {
                    settingsRepository.updateThemeMode(event.themeMode)
                    showSaveFeedback("Theme saved")
                }
                is SettingsUiEvent.AppearancePresetSelected -> {
                    settingsRepository.updateAppearancePreset(event.appearancePreset)
                    showSaveFeedback(
                        when (event.appearancePreset) {
                            AppearancePreset.Classic -> "Classic look enabled"
                            AppearancePreset.EditorialWarm -> "Warm editorial style enabled"
                            AppearancePreset.SunDrenchedEditorial -> "Sun-drenched editorial style enabled"
                        },
                    )
                }
                is SettingsUiEvent.ToggleEditorialWarmth -> {
                    settingsRepository.updateEditorialWarmth(event.enabled)
                    showSaveFeedback(if (event.enabled) "Warm accents enabled" else "Warm accents turned off")
                }
                is SettingsUiEvent.ToggleNotifications -> {
                    _uiState.update { it.copy(isUpdatingNotifications = true, showDebugActions = AppBuildInfo.isDebugBuild) }
                    val result = notificationSettingsController.setNotificationsEnabled(event.enabled)
                    _uiState.update { it.copy(isUpdatingNotifications = false, showDebugActions = AppBuildInfo.isDebugBuild) }
                    showSaveFeedback(
                        when (result) {
                            NotificationSettingsUpdateResult.Enabled -> "Notifications enabled"
                            NotificationSettingsUpdateResult.Disabled -> "Notifications turned off"
                            NotificationSettingsUpdateResult.PermissionDenied -> "Notifications stay off until permission is granted"
                            NotificationSettingsUpdateResult.PermissionDeniedAlways -> "Turn on notifications in system settings to enable them here"
                        },
                    )
                }
                is SettingsUiEvent.ToggleWeekStartsOnMonday -> {
                    settingsRepository.updateWeekStartsOnMonday(event.enabled)
                    showSaveFeedback(
                        if (event.enabled) {
                            "Weekly reports now start on Monday"
                        } else {
                            "Weekly reports now start on Sunday"
                        },
                    )
                }
                is SettingsUiEvent.ToggleAutoAssignSuggestions -> {
                    settingsRepository.updateAutoAssignSuggestions(event.enabled)
                    showSaveFeedback(
                        if (event.enabled) {
                            "Smart reassignment suggestions enabled"
                        } else {
                            "Smart reassignment suggestions turned off"
                        },
                    )
                }
                is SettingsUiEvent.ToggleLargerText -> {
                    settingsRepository.updateLargerText(event.enabled)
                    showSaveFeedback(if (event.enabled) "Larger text enabled" else "Larger text turned off")
                }
                is SettingsUiEvent.ToggleAlwaysShowLabels -> {
                    settingsRepository.updateAlwaysShowLabels(event.enabled)
                    showSaveFeedback(
                        if (event.enabled) {
                            "Bottom navigation labels always stay visible"
                        } else {
                            "Bottom navigation labels now stay compact"
                        },
                    )
                }
                SettingsUiEvent.ResetDemo -> {
                    sessionRepository.resetDemo()
                    showSaveFeedback("Demo household reset")
                    vibratorUtil.triggerVibrationSuccess()
                }
                SettingsUiEvent.ClearSaveFeedback -> {
                    _uiState.update { it.copy(saveFeedbackMessage = null) }
                }
            }
        }
    }

    private fun showSaveFeedback(message: String) {
        _uiState.update { it.copy(saveFeedbackMessage = message) }
        viewModelScope.launch {
            SnackbarController.sendEvent(
                SnackbarEvent(
                    message = StringValue.DynamicString(message),
                ),
            )
        }
        viewModelScope.launch {
            delay(1500L)
            _uiState.update { current ->
                if (current.saveFeedbackMessage == message) {
                    current.copy(saveFeedbackMessage = null)
                } else {
                    current
                }
            }
        }
    }
}
