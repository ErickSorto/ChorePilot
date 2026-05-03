package com.sortoapps.chorepilot.features.chores.presentation.viewmodel

import com.sortoapps.chorepilot.features.auth.domain.HouseholdRole
import com.sortoapps.chorepilot.features.chores.domain.ChoreItemStatus
import com.sortoapps.chorepilot.features.chores.domain.canManageHouseholdChores

enum class ChoreViewerMode {
    Admin,
    Member,
}

enum class ChoreCompletionPromptKind {
    RemoveSubmittedProof,
}

data class ChoreCompletionPromptUiModel(
    val choreId: String,
    val choreTitle: String,
    val kind: ChoreCompletionPromptKind,
)

enum class ProofReviewState {
    AwaitingApproval,
    Approved,
}

data class ProofSubmissionUiModel(
    val choreId: String,
    val title: String,
    val area: String,
    val memberName: String,
    val submittedLabel: String,
    val photoCount: Int,
    val submittedAtEpochMs: Long = 0L,
    val state: ProofReviewState = ProofReviewState.AwaitingApproval,
    val proofUrl: String = "",
)

data class ProofSubmissionDateSectionUiModel(
    val title: String,
    val proofs: List<ProofSubmissionUiModel>,
)

data class ChoreListItemUiModel(
    val id: String,
    val title: String,
    val area: String,
    val cadence: String,
    val assignee: String,
    val isActive: Boolean,
    val isUnassigned: Boolean,
    val pendingApproval: Boolean,
    val assignmentReason: String,
    val submitterName: String,
    val reviewRejectedReason: String,
    val completedByName: String,
    val dueLabel: String,
    val durationLabel: String,
    val status: ChoreItemStatus,
    val tags: List<String>,
    val isAssignedToViewer: Boolean,
    val canToggleCompletion: Boolean,
    val proofPhotoCount: Int = 0,
)

data class ChoreRoomSectionUiModel(
    val title: String,
    val chores: List<ChoreListItemUiModel>,
)

data class ChoreAvailabilitySuggestionUiModel(
    val id: String,
    val choreId: String,
    val choreTitle: String,
    val dateKey: String,
    val weekdayLabel: String,
    val originalAssignee: String,
    val recommendedCoverMember: String,
    val reasonText: String,
    val scopeStartDate: String,
    val scopeEndDate: String,
    val outsideNormalRotation: Boolean,
)

data class ChoreAvailabilitySuggestionSectionUiModel(
    val title: String,
    val suggestions: List<ChoreAvailabilitySuggestionUiModel>,
)

data class PendingApprovalRejectionDialogUiState(
    val choreId: String,
    val choreTitle: String,
    val submitterName: String,
    val reason: String = "",
)

data class ChoresUiState(
    val householdName: String = "",
    val viewerName: String = "",
    val viewerId: String = "",
    val viewerRole: HouseholdRole = HouseholdRole.Member,
    val query: String = "",
    val selectedFilter: String = "Mine",
    val viewerMode: ChoreViewerMode = ChoreViewerMode.Member,
    val canUseAdminExperience: Boolean = viewerRole.canManageHouseholdChores(),
    val chores: List<ChoreListItemUiModel> = emptyList(),
    val feedbackMessage: String? = null,
    val completionPrompt: ChoreCompletionPromptUiModel? = null,
    val autoAssignSuggestionsEnabled: Boolean = true,
    val isLoadingAvailabilitySuggestions: Boolean = false,
    val suggestionSections: List<ChoreAvailabilitySuggestionSectionUiModel> = emptyList(),
    val proofQueueSections: List<ProofSubmissionDateSectionUiModel> = emptyList(),
    val pendingApprovalSections: List<ChoreRoomSectionUiModel> = emptyList(),
    val inactiveSections: List<ChoreRoomSectionUiModel> = emptyList(),
    val needsOwnerSections: List<ChoreRoomSectionUiModel> = emptyList(),
    val activeSections: List<ChoreRoomSectionUiModel> = emptyList(),
    val lifecycleConfirmation: ChoreLifecycleConfirmationUiState? = null,
    val pendingApprovalRejectionDialog: PendingApprovalRejectionDialogUiState? = null,
)
