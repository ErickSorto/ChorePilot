package com.sortoapps.chorepilot.features.chores.presentation.viewmodel

sealed interface ChoresUiEvent {
    data object TriggerVibration : ChoresUiEvent

    data object TriggerVibrationSuccess : ChoresUiEvent

    data class QueryChanged(val query: String) : ChoresUiEvent

    data object DismissFeedback : ChoresUiEvent

    data class FilterSelected(val filter: String) : ChoresUiEvent

    data object ViewerModeToggled : ChoresUiEvent

    data class CompletionToggled(val choreId: String) : ChoresUiEvent
    data object ConfirmProofRemoval : ChoresUiEvent
    data object DismissCompletionPrompt : ChoresUiEvent
    data class AvailabilitySuggestionAccepted(val suggestionId: String) : ChoresUiEvent
    data class AvailabilitySuggestionDismissed(val suggestionId: String) : ChoresUiEvent

    data class ProofApproved(val choreId: String) : ChoresUiEvent

    data class PendingApprovalApproved(val choreId: String) : ChoresUiEvent

    data class PendingApprovalRejectRequested(val choreId: String) : ChoresUiEvent

    data class PendingApprovalRejectionReasonChanged(val value: String) : ChoresUiEvent

    data object PendingApprovalRejectConfirmed : ChoresUiEvent

    data object PendingApprovalRejectDismissed : ChoresUiEvent

    data class ReactivateRequested(val choreId: String) : ChoresUiEvent

    data class DeleteRequested(val choreId: String) : ChoresUiEvent

    data object ConfirmLifecycleAction : ChoresUiEvent

    data object DismissLifecycleConfirmation : ChoresUiEvent

    data class LifecycleConfirmationTextChanged(val value: String) : ChoresUiEvent
}
