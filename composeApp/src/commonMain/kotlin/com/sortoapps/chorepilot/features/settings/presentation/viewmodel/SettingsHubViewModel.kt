package com.sortoapps.chorepilot.features.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sortoapps.chorepilot.core.demo.DemoContent
import com.sortoapps.chorepilot.core.platform.AppBuildInfo
import com.sortoapps.chorepilot.core.ui.SnackbarController
import com.sortoapps.chorepilot.core.ui.SnackbarEvent
import com.sortoapps.chorepilot.core.ui.StringValue
import com.sortoapps.chorepilot.features.auth.domain.HouseholdRole
import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsHubViewModel(
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        SettingsHubUiState(
            profileSummary = "Manage account, reminders, billing, and rewards without digging through separate screens.",
            showDebugTools = AppBuildInfo.isDebugBuild,
            previewCards = DemoContent.previewCards,
        ),
    )
    val uiState: StateFlow<SettingsHubUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionRepository.session.collect { session ->
                _uiState.update {
                    it.copy(
                        displayName = session.displayName.ifBlank { "Your account" },
                        householdName = session.householdName.ifBlank { "Your household" },
                        activeHouseholdId = session.householdId,
                        availableHouseholds = session.availableHouseholds.map { household ->
                            SettingsHubHouseholdUiModel(
                                id = household.id,
                                name = household.name,
                                roleLabel = household.role.toDisplayLabel(),
                                premiumLabel = if (household.hasChorePilotPro) "Pro" else "",
                            )
                        },
                        profileSummary = "Manage account, reminders, billing, and rewards for ${session.householdName.ifBlank { "your household" }} without digging through separate screens.",
                        isSwitchingHousehold = false,
                        showDebugTools = AppBuildInfo.isDebugBuild,
                    )
                }
            }
        }
    }

    fun switchHousehold(householdId: String) {
        if (householdId.isBlank() || householdId == _uiState.value.activeHouseholdId || _uiState.value.isSwitchingHousehold) return
        val targetName = _uiState.value.availableHouseholds.firstOrNull { it.id == householdId }?.name.orEmpty()
        _uiState.update { it.copy(isSwitchingHousehold = true) }
        viewModelScope.launch {
            runCatching {
                sessionRepository.switchActiveHousehold(householdId)
            }.onSuccess {
                SnackbarController.sendEvent(
                    SnackbarEvent(
                        message = StringValue.DynamicString(
                            if (targetName.isBlank()) "Household switched." else "Switched to $targetName.",
                        ),
                    ),
                )
            }.onFailure { error ->
                _uiState.update { it.copy(isSwitchingHousehold = false) }
                SnackbarController.sendEvent(
                    SnackbarEvent(
                        message = StringValue.DynamicString(
                            error.message ?: "We couldn't switch households right now.",
                        ),
                    ),
                )
            }
        }
    }
}

private fun HouseholdRole.toDisplayLabel(): String = when (this) {
    HouseholdRole.Owner -> "Owner"
    HouseholdRole.Admin -> "Admin"
    HouseholdRole.Member -> "Member"
    HouseholdRole.Guest -> "Guest"
}
