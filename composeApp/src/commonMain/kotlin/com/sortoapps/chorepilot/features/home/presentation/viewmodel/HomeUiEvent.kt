package com.sortoapps.chorepilot.features.home.presentation.viewmodel

sealed interface HomeUiEvent {
    data object TriggerVibration : HomeUiEvent

    data object TriggerVibrationSuccess : HomeUiEvent

    data class DaySelected(val dayId: String) : HomeUiEvent

    data class ToggleChoreCompletion(val choreId: String) : HomeUiEvent

    data object ConfirmCompletionForAnotherMember : HomeUiEvent

    data object ConfirmProofRemoval : HomeUiEvent

    data object DismissCompletionPrompt : HomeUiEvent

    data class SurfaceSelected(val surfaceMode: HomeSurfaceMode) : HomeUiEvent

    data class ReassignmentAccepted(val suggestionId: String) : HomeUiEvent

    data class ReassignmentDismissed(val suggestionId: String) : HomeUiEvent

    data object Refresh : HomeUiEvent
}
