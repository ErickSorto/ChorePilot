package com.sortoapps.chorepilot.features.choredetail.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chorepilot.composeapp.generated.resources.Res
import chorepilot.composeapp.generated.resources.snackbar_chore_completed_points
import com.sortoapps.chorepilot.core.platform.VibratorUtil
import com.sortoapps.chorepilot.core.ui.SnackbarController
import com.sortoapps.chorepilot.core.ui.SnackbarEvent
import com.sortoapps.chorepilot.core.ui.SnackbarMascotIcons
import com.sortoapps.chorepilot.core.ui.SnackbarStyle
import com.sortoapps.chorepilot.core.ui.StringValue
import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import com.sortoapps.chorepilot.features.chores.domain.ChoreItem
import com.sortoapps.chorepilot.features.chores.domain.ChoreItemStatus
import com.sortoapps.chorepilot.features.chores.domain.ApproveProofUseCase
import com.sortoapps.chorepilot.features.chores.domain.ChoreRepository
import com.sortoapps.chorepilot.features.chores.domain.ProofReviewStatus
import com.sortoapps.chorepilot.features.chores.domain.canUndoChoreCompletion
import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.ChoreLifecycleAction
import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.buildChoreLifecycleConfirmation
import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.canManageChoreLifecycle
import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.withConfirmationText
import com.sortoapps.chorepilot.features.rewards.domain.RewardsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

class ChoreDetailViewModel(
    private val choreRepository: ChoreRepository,
    private val sessionRepository: SessionRepository,
    private val approveProofUseCase: ApproveProofUseCase,
    private val rewardsRepository: RewardsRepository,
    private val vibratorUtil: VibratorUtil,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChoreDetailUiState())
    val uiState: StateFlow<ChoreDetailUiState> = _uiState.asStateFlow()

    fun onEvent(event: ChoreDetailUiEvent) {
        when (event) {
            ChoreDetailUiEvent.TriggerVibration -> vibratorUtil.triggerVibration()
            ChoreDetailUiEvent.TriggerVibrationSuccess -> vibratorUtil.triggerVibrationSuccess()
            is ChoreDetailUiEvent.LoadChore -> loadChore(event.choreId)
            ChoreDetailUiEvent.ApprovePendingChore -> approvePendingChore()
            ChoreDetailUiEvent.RejectPendingChore -> {
                if (!_uiState.value.canManageLifecycle) return
                _uiState.update {
                    it.copy(
                        showRejectPendingChoreDialog = true,
                        pendingRejectionReasonInput = it.reviewRejectedReason.takeIf(String::isNotBlank).orEmpty(),
                    )
                }
            }
            ChoreDetailUiEvent.ConfirmRejectPendingChore -> rejectPendingChore()
            ChoreDetailUiEvent.DismissRejectPendingChore -> {
                _uiState.update {
                    it.copy(
                        showRejectPendingChoreDialog = false,
                        pendingRejectionReasonInput = "",
                    )
                }
            }
            is ChoreDetailUiEvent.PendingRejectionReasonChanged -> {
                _uiState.update {
                    it.copy(pendingRejectionReasonInput = event.value.take(80))
                }
            }
            ChoreDetailUiEvent.ApproveProof -> approveProof()
            ChoreDetailUiEvent.RejectProof -> {
                if (!_uiState.value.canManageLifecycle) return
                _uiState.update {
                    it.copy(
                        showRejectProofDialog = true,
                        proofRejectionReasonInput = it.proofRejectedReason.takeIf(String::isNotBlank).orEmpty(),
                    )
                }
            }
            ChoreDetailUiEvent.ConfirmRejectProof -> rejectProof()
            ChoreDetailUiEvent.DismissRejectProof -> {
                _uiState.update {
                    it.copy(
                        showRejectProofDialog = false,
                        proofRejectionReasonInput = "",
                    )
                }
            }
            is ChoreDetailUiEvent.ProofRejectionReasonChanged -> {
                _uiState.update {
                    it.copy(proofRejectionReasonInput = event.value.take(80))
                }
            }
            ChoreDetailUiEvent.MarkComplete -> markComplete(allowCompletingForSomeoneElse = false)
            ChoreDetailUiEvent.ConfirmCompletionForAnotherMember -> markComplete(allowCompletingForSomeoneElse = true)
            ChoreDetailUiEvent.DismissCompletionPrompt -> _uiState.update { it.copy(completionPrompt = null) }
            ChoreDetailUiEvent.ShowPendingProof -> {
                vibratorUtil.triggerVibration()
                _uiState.update { it.copy(surface = ChoreDetailSurface.PendingProof) }
            }
            ChoreDetailUiEvent.ShowOverview -> {
                vibratorUtil.triggerVibration()
                _uiState.update { it.copy(surface = ChoreDetailSurface.Overview) }
            }
            is ChoreDetailUiEvent.UpdateCompletionNote -> _uiState.update { it.copy(completionNote = event.note) }
            is ChoreDetailUiEvent.LifecycleActionRequested -> requestLifecycleAction(event.action)
            ChoreDetailUiEvent.ConfirmLifecycleAction -> confirmLifecycleAction()
            ChoreDetailUiEvent.DismissLifecycleConfirmation -> {
                _uiState.update { it.copy(lifecycleConfirmation = null) }
            }
            is ChoreDetailUiEvent.LifecycleConfirmationTextChanged -> {
                _uiState.update { state ->
                    state.copy(
                        lifecycleConfirmation = state.lifecycleConfirmation?.withConfirmationText(event.value),
                    )
                }
            }
            ChoreDetailUiEvent.DeleteNavigationHandled -> {
                _uiState.update { it.copy(shouldCloseAfterDelete = false) }
            }
        }
    }

    private fun requestLifecycleAction(
        action: ChoreLifecycleAction,
    ) {
        val state = _uiState.value
        if (!state.canManageLifecycle || state.choreId.isBlank()) return
        when (action) {
            ChoreLifecycleAction.Reactivate -> performLifecycleAction(action)
            ChoreLifecycleAction.Deactivate,
            ChoreLifecycleAction.Delete,
            -> _uiState.update {
                it.copy(
                    lifecycleFeedbackMessage = null,
                    lifecycleConfirmation = buildChoreLifecycleConfirmation(
                        action = action,
                        choreId = state.choreId,
                        choreTitle = state.title.ifBlank { "this chore" },
                    ),
                )
            }
        }
    }

    private fun confirmLifecycleAction() {
        val confirmation = _uiState.value.lifecycleConfirmation ?: return
        performLifecycleAction(confirmation.action)
    }

    private fun performLifecycleAction(
        action: ChoreLifecycleAction,
    ) {
        val current = _uiState.value
        if (current.choreId.isBlank()) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isApplyingLifecycleAction = true,
                    lifecycleConfirmation = null,
                    lifecycleFeedbackMessage = null,
                    feedbackMessage = null,
                )
            }
            runCatching {
                when (action) {
                    ChoreLifecycleAction.Delete -> choreRepository.deleteChore(current.choreId)
                    ChoreLifecycleAction.Deactivate -> {
                        val draft = choreRepository.getDraft(current.choreId) ?: error("We couldn't reload this chore.")
                        choreRepository.saveChore(draft.copy(isActive = false))
                    }

                    ChoreLifecycleAction.Reactivate -> {
                        val draft = choreRepository.getDraft(current.choreId) ?: error("We couldn't reload this chore.")
                        choreRepository.saveChore(
                            draft.copy(
                                isActive = true,
                                pendingApproval = false,
                            ),
                        )
                    }
                }
            }.onSuccess {
                when (action) {
                    ChoreLifecycleAction.Delete -> {
                        vibratorUtil.triggerVibrationSuccess()
                        _uiState.update {
                            it.copy(
                                isApplyingLifecycleAction = false,
                                shouldCloseAfterDelete = true,
                            )
                        }
                    }

                    ChoreLifecycleAction.Deactivate,
                    ChoreLifecycleAction.Reactivate,
                    -> {
                        val feedback = when (action) {
                            ChoreLifecycleAction.Deactivate ->
                                "${current.title.ifBlank { "This chore" }} is inactive. Proof and completion history stay attached."

                            ChoreLifecycleAction.Reactivate ->
                                "${current.title.ifBlank { "This chore" }} is active again and ready for the schedule."

                            ChoreLifecycleAction.Delete -> null
                        }
                        buildState(
                            choreId = current.choreId,
                            feedbackMessage = null,
                            isApprovingProof = false,
                            isMarkingComplete = false,
                            lifecycleFeedbackMessage = feedback,
                        )?.let { refreshed ->
                            _uiState.value = refreshed.copy(isApplyingLifecycleAction = false)
                        } ?: _uiState.update {
                            it.copy(
                                isApplyingLifecycleAction = false,
                                lifecycleFeedbackMessage = feedback,
                            )
                        }
                        vibratorUtil.triggerVibrationSuccess()
                    }
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isApplyingLifecycleAction = false,
                        lifecycleFeedbackMessage = error.message ?: "We couldn't update this chore yet.",
                    )
                }
            }
        }
    }

    private fun loadChore(choreId: String) {
        _uiState.update {
            it.copy(
                isLoading = true,
                unavailableMessage = null,
                feedbackMessage = null,
                lifecycleFeedbackMessage = null,
            )
        }
        val state = buildState(choreId = choreId)
        _uiState.value = state ?: ChoreDetailUiState(
            isLoading = false,
            unavailableMessage = "This chore isn't available anymore. It may have been deleted or moved.",
            choreId = choreId,
        )
    }

    private fun markComplete(
        allowCompletingForSomeoneElse: Boolean,
    ) {
        val current = _uiState.value
        if (current.choreId.isBlank() || current.isMarkingComplete || current.isApprovingProof || !current.isActive) return

        val viewerName = sessionRepository.session.value.displayName
        val chore = choreRepository.getChore(current.choreId) ?: return
        val isUndoRequest = current.status == ChoreItemStatus.Completed || current.status == ChoreItemStatus.PendingProof
        val requiresConfirmation = chore.assignee.isNotBlank() &&
            !chore.assignee.equals(viewerName, ignoreCase = true) &&
            !allowCompletingForSomeoneElse

        if (requiresConfirmation) {
            _uiState.update {
                it.copy(
                    completionPrompt = ChoreDetailCompletionPromptUiModel(
                        choreId = chore.id,
                        choreTitle = chore.title,
                        assigneeName = chore.assignee,
                        kind = ChoreDetailCompletionPromptKind.CompleteForAnotherMember,
                    ),
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isMarkingComplete = true,
                    completionPrompt = null,
                    feedbackMessage = null,
                    surface = ChoreDetailSurface.Overview,
                )
            }
            choreRepository.toggleCompletion(
                choreId = current.choreId,
                viewerName = viewerName,
                allowCompletingForSomeoneElse = allowCompletingForSomeoneElse,
            )
            val reloadedChore = choreRepository.getChore(current.choreId)
            val feedback = reloadedChore?.let { completionFeedback(it) }
            val refreshed = buildState(
                choreId = current.choreId,
                feedbackMessage = feedback,
                isMarkingComplete = false,
            )
            if (isUndoRequest) {
                vibratorUtil.triggerVibration()
            } else {
                vibratorUtil.triggerVibrationSuccess()
                showChoreCompletedSnackbar(current.choreId)
            }
            _uiState.value = refreshed ?: _uiState.value.copy(isMarkingComplete = false, feedbackMessage = feedback)
        }
    }

    private suspend fun showChoreCompletedSnackbar(
        choreId: String,
    ) {
        SnackbarController.sendEvent(
            SnackbarEvent(
                message = StringValue.Resource(
                    Res.string.snackbar_chore_completed_points,
                    completionPoints(),
                ),
                style = SnackbarStyle.Celebration,
                icon = SnackbarMascotIcons.celebrationIconFor(choreId),
            ),
        )
    }

    private fun completionPoints(): Int =
        rewardsRepository.state.value.snapshot?.rules?.pointsPerCompletion ?: 12

    private fun approveProof() {
        val current = _uiState.value
        if (
            current.choreId.isBlank() ||
            current.proofUrl.isBlank() ||
            current.isApprovingProof ||
            !current.canManageLifecycle
        ) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isApprovingProof = true,
                    feedbackMessage = null,
                    surface = ChoreDetailSurface.Overview,
                )
            }
            val approval = approveProofUseCase(current.choreId)
            val approvedAt = approval?.approvedAt ?: Clock.System.now()
            val feedback = approval?.feedbackMessage
            val refreshed = buildState(
                choreId = current.choreId,
                feedbackMessage = feedback,
                isApprovingProof = false,
                proofApprovedAt = approvedAt,
            )
            vibratorUtil.triggerVibrationSuccess()
            _uiState.value = refreshed ?: _uiState.value.copy(
                isApprovingProof = false,
                proofApprovedAt = approvedAt,
                feedbackMessage = feedback,
            )
        }
    }

    private fun approvePendingChore() {
        val current = _uiState.value
        if (
            current.choreId.isBlank() ||
            !current.pendingApproval ||
            current.isApprovingPendingChore ||
            !current.canManageLifecycle
        ) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isApprovingPendingChore = true,
                    feedbackMessage = null,
                    surface = ChoreDetailSurface.Overview,
                )
            }
            choreRepository.approvePendingChore(current.choreId)
            val feedback = "${current.title.ifBlank { "This chore" }} is approved and now part of the household schedule."
            val refreshed = buildState(
                choreId = current.choreId,
                feedbackMessage = feedback,
                isApprovingPendingChore = false,
            )
            vibratorUtil.triggerVibrationSuccess()
            _uiState.value = refreshed ?: _uiState.value.copy(
                isApprovingPendingChore = false,
                feedbackMessage = feedback,
            )
        }
    }

    private fun rejectProof() {
        val current = _uiState.value
        val reason = current.proofRejectionReasonInput.trim()
        if (
            current.choreId.isBlank() ||
            current.proofUrl.isBlank() ||
            current.isRejectingProof ||
            !current.canManageLifecycle ||
            reason.isBlank()
        ) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRejectingProof = true,
                    feedbackMessage = null,
                    showRejectProofDialog = false,
                    surface = ChoreDetailSurface.Overview,
                )
            }
            choreRepository.rejectProof(
                choreId = current.choreId,
                reason = reason,
            )
            val refreshed = buildState(
                choreId = current.choreId,
                feedbackMessage = rejectionFeedback(current.proofUploadedByName, reason),
                isRejectingProof = false,
            )
            vibratorUtil.triggerVibrationSuccess()
            _uiState.value = refreshed ?: _uiState.value.copy(
                isRejectingProof = false,
                feedbackMessage = rejectionFeedback(current.proofUploadedByName, reason),
            )
        }
    }

    private fun rejectPendingChore() {
        val current = _uiState.value
        val reason = current.pendingRejectionReasonInput.trim()
        if (
            current.choreId.isBlank() ||
            !current.pendingApproval ||
            current.isRejectingPendingChore ||
            !current.canManageLifecycle ||
            reason.isBlank()
        ) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRejectingPendingChore = true,
                    feedbackMessage = null,
                    showRejectPendingChoreDialog = false,
                    surface = ChoreDetailSurface.Overview,
                )
            }
            choreRepository.rejectPendingChore(
                choreId = current.choreId,
                reason = reason,
            )
            val feedback = "${current.title.ifBlank { "This chore" }} was sent back with a note for ${current.submitterName.ifBlank { current.assignee }}."
            val refreshed = buildState(
                choreId = current.choreId,
                feedbackMessage = feedback,
                isRejectingPendingChore = false,
            )
            vibratorUtil.triggerVibrationSuccess()
            _uiState.value = refreshed ?: _uiState.value.copy(
                isRejectingPendingChore = false,
                feedbackMessage = feedback,
            )
        }
    }

    private fun buildState(
        choreId: String,
        feedbackMessage: String? = null,
        isApprovingProof: Boolean = false,
        isRejectingProof: Boolean = false,
        isApprovingPendingChore: Boolean = false,
        isRejectingPendingChore: Boolean = false,
        isMarkingComplete: Boolean = false,
        proofApprovedAt: kotlin.time.Instant? = null,
        lifecycleFeedbackMessage: String? = null,
    ): ChoreDetailUiState? {
        val chore = choreRepository.getChore(choreId) ?: return null
        val proof = choreRepository.getLatestProofForChore(chore.id)
        val session = sessionRepository.session.value
        val proofApprovedInstant = when {
            proofApprovedAt != null -> proofApprovedAt
            proof?.reviewStatus == ProofReviewStatus.Approved && proof.reviewedAtEpochMs > 0L ->
                kotlin.time.Instant.fromEpochMilliseconds(proof.reviewedAtEpochMs)

            else -> null
        }
        return ChoreDetailUiState(
            isLoading = false,
            unavailableMessage = null,
            choreId = chore.id,
            title = chore.title,
            area = chore.area,
            cadence = chore.cadence,
            assignee = chore.assignee,
            assignmentReason = chore.assignmentReason,
            completedByName = chore.completedByName,
            dueLabel = chore.dueLabel,
            durationLabel = chore.durationLabel,
            description = chore.description,
            reminder = chore.reminder,
            tags = chore.tags,
            proofRequired = chore.proofRequired,
            proofPhotoCount = if (proof != null) maxOf(chore.proofPhotoCount, 1) else chore.proofPhotoCount,
            proofUrl = proof?.downloadUrl.orEmpty(),
            proofUploadedByName = proof?.uploadedByName.orEmpty(),
            proofReviewStatus = proof?.reviewStatus ?: ProofReviewStatus.Pending,
            proofRejectedReason = proof?.rejectionReason.orEmpty(),
            proofReviewedByName = proof?.reviewedByName.orEmpty(),
            proofApprovedAt = proofApprovedInstant,
            isApprovingProof = isApprovingProof,
            isRejectingProof = isRejectingProof,
            isMarkingComplete = isMarkingComplete,
            canManageLifecycle = session.role.canManageChoreLifecycle(),
            isActive = chore.isActive,
            pendingApproval = chore.pendingApproval,
            canUndoCompletion = canUndoChoreCompletion(
                chore = chore,
                viewerId = session.userId,
                viewerName = session.displayName,
                viewerRole = session.role,
            ),
            status = chore.status,
            surface = ChoreDetailSurface.Overview,
            feedbackMessage = feedbackMessage,
            lifecycleFeedbackMessage = lifecycleFeedbackMessage,
            showRejectProofDialog = false,
            proofRejectionReasonInput = "",
            isApprovingPendingChore = isApprovingPendingChore,
            isRejectingPendingChore = isRejectingPendingChore,
            showRejectPendingChoreDialog = false,
            pendingRejectionReasonInput = "",
            discoveryImageUrl = chore.discoveryImageUrl,
            discoveryGenerated = chore.discoveryGenerated,
            submitterName = chore.submitterName.ifBlank { chore.assignee },
            reviewRejectedReason = chore.reviewRejectedReason,
            reviewedByName = chore.reviewedByName,
            reviewedAtEpochMs = chore.reviewedAtEpochMs,
        )
    }

    private fun completionFeedback(
        chore: ChoreItem,
        ): String? = when (chore.status) {
        ChoreItemStatus.Completed -> if (chore.completedByName.isBlank()) {
            "Marked complete for today."
        } else {
            "Completed for ${chore.assignee}."
        }
        ChoreItemStatus.PendingProof -> "Marked done. Proof is now waiting for household review."
        else -> null
    }

    private fun rejectionFeedback(
        submittingMemberName: String,
        reason: String,
    ): String {
        val actor = submittingMemberName.ifBlank { "This proof" }
        return "$actor needs a new proof. Reason: $reason"
    }
}
