package com.sortoapps.chorepilot.features.auth.presentation.viewmodel

sealed interface AuthUiEvent {
    data object TriggerVibration : AuthUiEvent

    data object TriggerVibrationSuccess : AuthUiEvent

    data class DisplayNameChanged(val value: String) : AuthUiEvent

    data class EmailChanged(val value: String) : AuthUiEvent

    data class PasswordChanged(val value: String) : AuthUiEvent

    data class HouseholdNameChanged(val value: String) : AuthUiEvent

    data class HouseholdSizeSelected(val value: String) : AuthUiEvent

    data class InviteCodeChanged(val value: String) : AuthUiEvent

    data class GoogleSignInAvailabilityChanged(val value: Boolean) : AuthUiEvent

    data class GoogleSignInFailed(val message: String) : AuthUiEvent

    data object ErrorDismissed : AuthUiEvent
}
