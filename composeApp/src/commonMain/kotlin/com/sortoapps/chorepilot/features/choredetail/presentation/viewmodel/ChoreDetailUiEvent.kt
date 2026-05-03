package com.sortoapps.chorepilot.features.choredetail.presentation.viewmodel

import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.ChoreLifecycleAction

sealed interface ChoreDetailUiEvent {
    data object TriggerVibration : ChoreDetailUiEvent

    data object TriggerVibrationSuccess : ChoreDetailUiEvent

    data class LoadChore(val choreId: String) : ChoreDetailUiEvent

    data object ApproveProof : ChoreDetailUiEvent

    data object RejectProof : ChoreDetailUiEvent

    data object ApprovePendingChore : ChoreDetailUiEvent

    data object RejectPendingChore : ChoreDetailUiEvent

    data object ConfirmRejectPendingChore : ChoreDetailUiEvent

    data object DismissRejectPendingChore : ChoreDetailUiEvent

    data class PendingRejectionReasonChanged(val value: String) : ChoreDetailUiEvent

    data object ConfirmRejectProof : ChoreDetailUiEvent

    data object DismissRejectProof : ChoreDetailUiEvent

    data class ProofRejectionReasonChanged(val value: String) : ChoreDetailUiEvent

    data object MarkComplete : ChoreDetailUiEvent

    data object ConfirmCompletionForAnotherMember : ChoreDetailUiEvent

    data object DismissCompletionPrompt : ChoreDetailUiEvent

    data object ShowPendingProof : ChoreDetailUiEvent

    data object ShowOverview : ChoreDetailUiEvent

    data class UpdateCompletionNote(val note: String) : ChoreDetailUiEvent

    data class LifecycleActionRequested(val action: ChoreLifecycleAction) : ChoreDetailUiEvent

    data object ConfirmLifecycleAction : ChoreDetailUiEvent

    data object DismissLifecycleConfirmation : ChoreDetailUiEvent

    data class LifecycleConfirmationTextChanged(val value: String) : ChoreDetailUiEvent

    data object DeleteNavigationHandled : ChoreDetailUiEvent
}
