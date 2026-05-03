package com.sortoapps.chorepilot.features.choredetail.presentation.viewmodel

import com.sortoapps.chorepilot.features.chores.domain.ChoreItemStatus
import com.sortoapps.chorepilot.features.chores.domain.ProofReviewStatus
import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.ChoreLifecycleConfirmationUiState
import kotlin.time.Instant

enum class ChoreDetailSurface {
    Overview,
    Success,
    PendingProof,
}

enum class ChoreDetailCompletionPromptKind {
    CompleteForAnotherMember,
}

data class ChoreDetailCompletionPromptUiModel(
    val choreId: String,
    val choreTitle: String,
    val assigneeName: String,
    val kind: ChoreDetailCompletionPromptKind,
)

data class ChoreDetailUiState(
    val isLoading: Boolean = false,
    val unavailableMessage: String? = null,
    val choreId: String = "",
    val title: String = "",
    val area: String = "",
    val cadence: String = "",
    val assignee: String = "",
    val assignmentReason: String = "",
    val completedByName: String = "",
    val dueLabel: String = "",
    val durationLabel: String = "",
    val description: String = "",
    val reminder: String = "",
    val tags: List<String> = emptyList(),
    val completionNote: String = "Counter is wiped, table is clear, and the dishwasher is running.",
    val surface: ChoreDetailSurface = ChoreDetailSurface.Overview,
    val proofRequired: Boolean = false,
    val proofPhotoCount: Int = 0,
    val proofUrl: String = "",
    val proofUploadedByName: String = "",
    val proofReviewStatus: ProofReviewStatus = ProofReviewStatus.Pending,
    val proofRejectedReason: String = "",
    val proofReviewedByName: String = "",
    val proofApprovedAt: Instant? = null,
    val isApprovingProof: Boolean = false,
    val isRejectingProof: Boolean = false,
    val showRejectProofDialog: Boolean = false,
    val proofRejectionReasonInput: String = "",
    val isApprovingPendingChore: Boolean = false,
    val isRejectingPendingChore: Boolean = false,
    val showRejectPendingChoreDialog: Boolean = false,
    val pendingRejectionReasonInput: String = "",
    val isMarkingComplete: Boolean = false,
    val isApplyingLifecycleAction: Boolean = false,
    val canUndoCompletion: Boolean = true,
    val canManageLifecycle: Boolean = false,
    val isActive: Boolean = true,
    val pendingApproval: Boolean = false,
    val submitterName: String = "",
    val reviewRejectedReason: String = "",
    val reviewedByName: String = "",
    val reviewedAtEpochMs: Long = 0L,
    val status: ChoreItemStatus = ChoreItemStatus.DueSoon,
    val feedbackMessage: String? = null,
    val lifecycleFeedbackMessage: String? = null,
    val completionPrompt: ChoreDetailCompletionPromptUiModel? = null,
    val lifecycleConfirmation: ChoreLifecycleConfirmationUiState? = null,
    val shouldCloseAfterDelete: Boolean = false,
    val discoveryImageUrl: String = "",
    val discoveryGenerated: Boolean = false,
)
