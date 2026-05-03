package com.sortoapps.chorepilot.features.account.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sortoapps.chorepilot.core.platform.VibratorUtil
import com.sortoapps.chorepilot.features.auth.domain.HouseholdRole
import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AccountViewModel(
    sessionRepository: SessionRepository,
    private val vibratorUtil: VibratorUtil,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionRepository.session.collect { session ->
                val hasHousehold = session.householdId.isNotBlank()
                val householdName = session.householdName.ifBlank { "Your household" }
                _uiState.value = AccountUiState(
                    isLoading = false,
                    displayName = session.displayName.ifBlank { "Your account" },
                    email = session.email,
                    signedInMethodLabel = if (session.signedIn && !session.isAnonymous) "Google" else "Signed out",
                    signedInMethodSummary = if (session.signedIn && !session.isAnonymous) {
                        "Recoverable Google sign-in."
                    } else {
                        "Sign in with Google."
                    },
                    householdName = householdName,
                    householdRoleLabel = session.role.toDisplayLabel(),
                    seatLabel = if (hasHousehold) "Active ${session.role.toDisplayLabel().lowercase()} seat" else "No household seat",
                    seatContextLabel = if (hasHousehold) session.householdSizeLabel else "No household connected yet",
                    accessSummary = when {
                        !hasHousehold -> "Finish household setup."
                        session.householdHasChorePilotPro -> "Premium is shared through $householdName."
                        else -> "This household is on the standard plan."
                    },
                    premiumStatusLabel = if (session.householdHasChorePilotPro) "Household Pro active" else "Free household",
                    premiumStatusSummary = if (session.householdHasChorePilotPro) {
                        "Premium is shared across the household."
                    } else {
                        "Premium is not enabled."
                    },
                    signOutSummary = if (hasHousehold) {
                        "Signs out this device only. Household data stays safe."
                    } else {
                        "Signs out this device only."
                    },
                )
            }
        }
    }

    fun onEvent(event: AccountUiEvent) {
        when (event) {
            AccountUiEvent.TriggerVibration -> viewModelScope.launch {
                vibratorUtil.triggerVibration()
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
