package com.sortoapps.chorepilot.features.chores.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chorepilot.composeapp.generated.resources.Res
import chorepilot.composeapp.generated.resources.snackbar_chore_completed_points
import com.sortoapps.chorepilot.core.platform.VibratorUtil
import com.sortoapps.chorepilot.core.ui.SnackbarCelebrationContent
import com.sortoapps.chorepilot.core.ui.SnackbarController
import com.sortoapps.chorepilot.core.ui.SnackbarEvent
import com.sortoapps.chorepilot.core.ui.SnackbarMascotIcons
import com.sortoapps.chorepilot.core.ui.SnackbarStyle
import com.sortoapps.chorepilot.core.ui.StringValue
import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import com.sortoapps.chorepilot.features.chores.domain.ApproveProofUseCase
import com.sortoapps.chorepilot.features.chores.domain.ChoreItem
import com.sortoapps.chorepilot.features.chores.domain.ChoreItemStatus
import com.sortoapps.chorepilot.features.chores.domain.ChoreRepository
import com.sortoapps.chorepilot.features.chores.domain.canManageHouseholdChores
import com.sortoapps.chorepilot.features.chores.domain.canUndoChoreCompletion
import com.sortoapps.chorepilot.features.chores.domain.proofReviewSectionTitle
import com.sortoapps.chorepilot.features.chores.domain.proofReviewSubmittedLabel
import com.sortoapps.chorepilot.features.chores.domain.upcomingScheduleDays
import com.sortoapps.chorepilot.features.rewards.domain.RewardDefinitionDraft
import com.sortoapps.chorepilot.features.rewards.domain.RewardsFamilyRules
import com.sortoapps.chorepilot.features.rewards.domain.RewardsRepository
import com.sortoapps.chorepilot.features.rewards.domain.RewardsRepositoryState
import com.sortoapps.chorepilot.features.settings.domain.SettingsRepository
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

class ChoresViewModel(
    private val choreRepository: ChoreRepository,
    private val sessionRepository: SessionRepository,
    private val settingsRepository: SettingsRepository,
    private val approveProofUseCase: ApproveProofUseCase,
    private val rewardsRepository: RewardsRepository = NoOpChoresRewardsRepository,
    private val vibratorUtil: VibratorUtil,
) : ViewModel() {
    private var autoAssignSuggestionsEnabled = true

    private val _uiState = MutableStateFlow(
        ChoresUiState(
            householdName = sessionRepository.session.value.householdName.ifBlank { "River House" },
            viewerName = sessionRepository.session.value.displayName.ifBlank { "Erick" },
            viewerId = sessionRepository.session.value.userId,
            viewerRole = sessionRepository.session.value.role,
            selectedFilter = defaultFilterFor(sessionRepository.session.value.role),
            viewerMode = defaultViewerModeFor(sessionRepository.session.value.role),
        ),
    )
    val uiState: StateFlow<ChoresUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            choreRepository.refresh()
            choreRepository.chores.collectLatest { chores ->
                render()
            }
        }
        viewModelScope.launch {
            settingsRepository.settings.collectLatest { settings ->
                autoAssignSuggestionsEnabled = settings.autoAssignSuggestions
                _uiState.update { it.copy(autoAssignSuggestionsEnabled = settings.autoAssignSuggestions) }
                refreshSuggestionSections()
            }
        }
    }

    fun onEvent(event: ChoresUiEvent) {
        when (event) {
            ChoresUiEvent.TriggerVibration -> vibratorUtil.triggerVibration()
            ChoresUiEvent.TriggerVibrationSuccess -> vibratorUtil.triggerVibrationSuccess()
            ChoresUiEvent.DismissFeedback -> _uiState.update { it.copy(feedbackMessage = null) }
            is ChoresUiEvent.FilterSelected -> {
                _uiState.update { it.copy(selectedFilter = event.filter) }
                render()
            }

            is ChoresUiEvent.QueryChanged -> {
                _uiState.update { it.copy(query = event.query) }
                render()
            }

            ChoresUiEvent.ViewerModeToggled -> {
                if (!_uiState.value.viewerRole.canManageHouseholdChores()) return
                _uiState.update { state ->
                    val nextViewerMode = if (state.viewerMode == ChoreViewerMode.Admin) {
                        ChoreViewerMode.Member
                    } else {
                        ChoreViewerMode.Admin
                    }
                    state.copy(
                        viewerMode = nextViewerMode,
                        selectedFilter = defaultFilterFor(state.viewerRole, nextViewerMode),
                    )
                }
                render()
            }

            is ChoresUiEvent.CompletionToggled -> {
                val chore = choreRepository.getChore(event.choreId) ?: return
                val session = sessionRepository.session.value
                if (isUndoRequest(chore) && !canUndoChoreCompletion(chore, session.userId, session.displayName, session.role)) {
                    return
                }
                if (shouldConfirmProofRemoval(chore)) {
                    _uiState.update {
                        it.copy(
                            completionPrompt = ChoreCompletionPromptUiModel(
                                choreId = chore.id,
                                choreTitle = chore.title,
                                kind = ChoreCompletionPromptKind.RemoveSubmittedProof,
                            ),
                        )
                    }
                } else {
                    viewModelScope.launch {
                        choreRepository.toggleCompletion(
                            choreId = event.choreId,
                            viewerName = sessionRepository.session.value.displayName,
                            allowCompletingForSomeoneElse = false,
                        )
                        if (isUndoRequest(chore)) {
                            vibratorUtil.triggerVibration()
                        } else {
                            vibratorUtil.triggerVibrationSuccess()
                            showChoreCompletedSnackbar(
                                choreId = event.choreId,
                                choreTitle = chore.title,
                            )
                        }
                    }
                }
            }

            ChoresUiEvent.ConfirmProofRemoval -> {
                val prompt = _uiState.value.completionPrompt ?: return
                viewModelScope.launch {
                    choreRepository.toggleCompletion(
                        choreId = prompt.choreId,
                        viewerName = sessionRepository.session.value.displayName,
                        allowCompletingForSomeoneElse = false,
                    )
                    vibratorUtil.triggerVibration()
                    _uiState.update { it.copy(completionPrompt = null) }
                }
            }

            ChoresUiEvent.DismissCompletionPrompt -> {
                _uiState.update { it.copy(completionPrompt = null) }
            }

            is ChoresUiEvent.ReactivateRequested -> {
                if (!_uiState.value.viewerRole.canManageChoreLifecycle()) return
                viewModelScope.launch {
                    val draft = choreRepository.getDraft(event.choreId) ?: return@launch
                    choreRepository.saveChore(
                        draft.copy(
                            isActive = true,
                            pendingApproval = false,
                        ),
                    )
                    vibratorUtil.triggerVibrationSuccess()
                    render()
                }
            }

            is ChoresUiEvent.DeleteRequested -> {
                if (!_uiState.value.viewerRole.canManageChoreLifecycle()) return
                val chore = choreRepository.getChore(event.choreId) ?: return
                _uiState.update {
                    it.copy(
                        lifecycleConfirmation = buildChoreLifecycleConfirmation(
                            action = ChoreLifecycleAction.Delete,
                            choreId = chore.id,
                            choreTitle = chore.title,
                        ),
                    )
                }
            }

            ChoresUiEvent.ConfirmLifecycleAction -> {
                val confirmation = _uiState.value.lifecycleConfirmation ?: return
                viewModelScope.launch {
                    choreRepository.deleteChore(confirmation.choreId)
                    vibratorUtil.triggerVibrationSuccess()
                    _uiState.update { it.copy(lifecycleConfirmation = null) }
                    render()
                }
            }

            ChoresUiEvent.DismissLifecycleConfirmation -> {
                _uiState.update { it.copy(lifecycleConfirmation = null) }
            }

            is ChoresUiEvent.LifecycleConfirmationTextChanged -> {
                _uiState.update { state ->
                    state.copy(
                        lifecycleConfirmation = state.lifecycleConfirmation?.withConfirmationText(event.value),
                    )
                }
            }

            is ChoresUiEvent.AvailabilitySuggestionAccepted -> {
                if (!_uiState.value.viewerRole.canManageHouseholdChores()) return
                handleSuggestionAction(event.suggestionId, accept = true)
            }

            is ChoresUiEvent.AvailabilitySuggestionDismissed -> {
                if (!_uiState.value.viewerRole.canManageHouseholdChores()) return
                handleSuggestionAction(event.suggestionId, accept = false)
            }

            is ChoresUiEvent.ProofApproved -> {
                if (!_uiState.value.viewerRole.canManageHouseholdChores()) return
                viewModelScope.launch {
                    val approval = approveProofUseCase(event.choreId)
                    vibratorUtil.triggerVibrationSuccess()
                    _uiState.update { it.copy(feedbackMessage = approval?.feedbackMessage) }
                    render()
                }
            }

            is ChoresUiEvent.PendingApprovalApproved -> {
                if (!_uiState.value.viewerRole.canManageHouseholdChores()) return
                viewModelScope.launch {
                    choreRepository.approvePendingChore(event.choreId)
                    choreRepository.refresh()
                    vibratorUtil.triggerVibrationSuccess()
                    render()
                }
            }

            is ChoresUiEvent.PendingApprovalRejectRequested -> {
                if (!_uiState.value.viewerRole.canManageHouseholdChores()) return
                val chore = choreRepository.getChore(event.choreId) ?: return
                _uiState.update {
                    it.copy(
                        pendingApprovalRejectionDialog = PendingApprovalRejectionDialogUiState(
                            choreId = chore.id,
                            choreTitle = chore.title,
                            submitterName = chore.submitterName.ifBlank { chore.assignee },
                        ),
                    )
                }
            }

            is ChoresUiEvent.PendingApprovalRejectionReasonChanged -> {
                _uiState.update { state ->
                    state.copy(
                        pendingApprovalRejectionDialog = state.pendingApprovalRejectionDialog?.copy(
                            reason = event.value.take(80),
                        ),
                    )
                }
            }

            ChoresUiEvent.PendingApprovalRejectConfirmed -> {
                if (!_uiState.value.viewerRole.canManageHouseholdChores()) return
                val dialog = _uiState.value.pendingApprovalRejectionDialog ?: return
                val reason = dialog.reason.trim()
                if (reason.isBlank()) return
                viewModelScope.launch {
                    choreRepository.rejectPendingChore(
                        choreId = dialog.choreId,
                        reason = reason,
                    )
                    choreRepository.refresh()
                    vibratorUtil.triggerVibrationSuccess()
                    _uiState.update { it.copy(pendingApprovalRejectionDialog = null) }
                    render()
                }
            }

            ChoresUiEvent.PendingApprovalRejectDismissed -> {
                _uiState.update { it.copy(pendingApprovalRejectionDialog = null) }
            }
        }
    }

    private fun render() {
        val state = _uiState.value
        val viewerName = sessionRepository.session.value.displayName.ifBlank { state.viewerName.ifBlank { "Erick" } }
        val viewerId = sessionRepository.session.value.userId.ifBlank { state.viewerId }
        val viewerRole = sessionRepository.session.value.role
        val householdName = sessionRepository.session.value.householdName.ifBlank { state.householdName.ifBlank { "River House" } }
        val canUseAdminExperience = viewerRole.canManageHouseholdChores()
        val viewerMode = if (canUseAdminExperience) state.viewerMode else ChoreViewerMode.Member
        val selectedFilter = normalizeFilter(
            role = viewerRole,
            viewerMode = viewerMode,
            selectedFilter = state.selectedFilter,
        )
        val allChores = choreRepository.getChores()

        val matching = allChores.filter { chore ->
            matchesQuery(chore = chore, query = state.query)
        }
        val filtered = matching.filter { chore ->
            matchesAssignedFilter(
                chore = chore,
                selectedFilter = selectedFilter,
                viewerMode = viewerMode,
                viewerId = viewerId,
                viewerName = viewerName,
            )
        }

        val proofQueue = matching
            .filter { it.proofPhotoCount > 0 && (it.status == ChoreItemStatus.PendingProof || it.status == ChoreItemStatus.Completed) }
            .map { chore ->
                val proof = choreRepository.getLatestProofForChore(chore.id)
                ProofSubmissionUiModel(
                    choreId = chore.id,
                    title = chore.title,
                    area = chore.area,
                    memberName = proof?.uploadedByName?.ifBlank { chore.assignee } ?: chore.assignee.ifBlank { "Unassigned" },
                    submittedLabel = proof?.createdAtEpochMs?.let(::proofReviewSubmittedLabel).orEmpty(),
                    photoCount = when {
                        chore.dueLabel.contains("photo", ignoreCase = true) -> chore.dueLabel.filter(Char::isDigit).toIntOrNull() ?: 1
                        else -> 1
                    },
                    submittedAtEpochMs = proof?.createdAtEpochMs ?: 0L,
                    state = if (chore.status == ChoreItemStatus.PendingProof) ProofReviewState.AwaitingApproval else ProofReviewState.Approved,
                    proofUrl = proof?.downloadUrl.orEmpty(),
                )
            }
            .sortedByDescending { it.submittedAtEpochMs }

        val proofQueueSections = proofQueue
            .groupBy { proof -> proof.submittedAtEpochMs.takeIf { it > 0L }?.let(::proofReviewSectionTitle) ?: "Unknown date" }
            .map { (title, proofs) ->
                ProofSubmissionDateSectionUiModel(
                    title = title,
                    proofs = proofs,
                )
            }
        val todaysProofQueueSections = proofQueue
            .filter(::isProofForToday)
            .groupBy { proof -> proof.submittedAtEpochMs.takeIf { it > 0L }?.let(::proofReviewSectionTitle) ?: "Today" }
            .map { (title, proofs) ->
                ProofSubmissionDateSectionUiModel(
                    title = title,
                    proofs = proofs,
                )
            }

        val activeSections = matching
            .filter { chore ->
                chore.isActive &&
                    !chore.pendingApproval &&
                    chore.assignee.isNotBlank() &&
                    !isProofReviewChore(chore)
            }
            .map { chore -> chore.toUiModel(viewerId = viewerId, viewerName = viewerName, viewerRole = viewerRole) }
            .groupBy { sectionTitle(it.area) }
            .toSectionModels()

        val pendingApprovalSections = matching
            .filter { chore -> chore.pendingApproval }
            .map { chore -> chore.toUiModel(viewerId = viewerId, viewerName = viewerName, viewerRole = viewerRole) }
            .groupBy { sectionTitle(it.area) }
            .toSectionModels()

        val inactiveSections = matching
            .filter { chore -> !chore.isActive && !chore.pendingApproval }
            .map { chore -> chore.toUiModel(viewerId = viewerId, viewerName = viewerName, viewerRole = viewerRole) }
            .groupBy { sectionTitle(it.area) }
            .toSectionModels()

        val needsOwnerSections = matching
            .filter { chore ->
                chore.isActive &&
                    !chore.pendingApproval &&
                    chore.assignee.isBlank()
            }
            .map { chore -> chore.toUiModel(viewerId = viewerId, viewerName = viewerName, viewerRole = viewerRole) }
            .groupBy { sectionTitle(it.area) }
            .toSectionModels()

        _uiState.update {
            it.copy(
                householdName = householdName,
                viewerName = viewerName,
                viewerId = viewerId,
                viewerRole = viewerRole,
                selectedFilter = selectedFilter,
                viewerMode = viewerMode,
                canUseAdminExperience = canUseAdminExperience,
                chores = filtered.map { chore -> chore.toUiModel(viewerId = viewerId, viewerName = viewerName, viewerRole = viewerRole) },
                feedbackMessage = state.feedbackMessage,
                proofQueueSections = if (viewerMode == ChoreViewerMode.Admin && selectedFilter != "Inactive" && selectedFilter != "Active") {
                    when (selectedFilter) {
                        "Proofs" -> proofQueueSections
                        "All" -> todaysProofQueueSections
                        else -> emptyList()
                    }
                } else {
                    emptyList()
                },
                pendingApprovalSections = if (viewerMode == ChoreViewerMode.Admin && selectedFilter != "Inactive" && selectedFilter != "Active") {
                    when (selectedFilter) {
                        "Proofs", "All" -> pendingApprovalSections
                        else -> emptyList()
                    }
                } else {
                    emptyList()
                },
                activeSections = if (viewerMode == ChoreViewerMode.Admin && selectedFilter != "Proofs" && selectedFilter != "Inactive") {
                    if (selectedFilter == "Active" || selectedFilter == "All") activeSections else emptyList()
                } else {
                    emptyList()
                },
                inactiveSections = if (viewerMode == ChoreViewerMode.Admin && selectedFilter != "Proofs" && selectedFilter != "Active") {
                    if (selectedFilter == "Inactive" || selectedFilter == "All") inactiveSections else emptyList()
                } else {
                    emptyList()
                },
                needsOwnerSections = if (viewerMode == ChoreViewerMode.Admin && selectedFilter != "Proofs") {
                    if (selectedFilter == "All") needsOwnerSections else emptyList()
                } else {
                    emptyList()
                },
                completionPrompt = state.completionPrompt,
                lifecycleConfirmation = state.lifecycleConfirmation,
                pendingApprovalRejectionDialog = state.pendingApprovalRejectionDialog,
            )
        }
        refreshSuggestionSections()
    }

    private fun ChoreItem.toUiModel(
        viewerId: String,
        viewerName: String,
        viewerRole: com.sortoapps.chorepilot.features.auth.domain.HouseholdRole,
    ): ChoreListItemUiModel = ChoreListItemUiModel(
        id = id,
        title = title,
        area = area,
        cadence = cadence,
        assignee = when {
            pendingApproval -> assignee.ifBlank { "Submitted for review" }
            !isActive -> "Inactive"
            assignee.isBlank() -> "Unassigned"
            else -> assignee
        },
        isActive = isActive,
        isUnassigned = assignee.isBlank(),
        pendingApproval = pendingApproval,
        assignmentReason = when {
            pendingApproval -> "Waiting for an admin to activate this chore."
            !isActive -> "Reactivate this chore to return it to the schedule. Existing proof and history stay attached."
            assignee.isBlank() -> "Assign an owner before this chore can show up in the active schedule."
            else -> assignmentReason
        },
        submitterName = submitterName.ifBlank { assignee },
        reviewRejectedReason = reviewRejectedReason,
        completedByName = completedByName,
        dueLabel = dueLabel,
        durationLabel = durationLabel,
        status = status,
        tags = tags,
        isAssignedToViewer = assignee.equals(viewerName, ignoreCase = true),
        canToggleCompletion = if (pendingApproval || !isActive || assignee.isBlank()) {
            false
        } else if (status == ChoreItemStatus.Completed || status == ChoreItemStatus.PendingProof) {
            canUndoChoreCompletion(this, viewerId, viewerName, viewerRole)
        } else {
            assignee.equals(viewerName, ignoreCase = true)
        },
        proofPhotoCount = proofPhotoCount,
    )

    private fun shouldConfirmProofRemoval(chore: ChoreItem): Boolean =
        chore.proofPhotoCount > 0 &&
            (chore.status == ChoreItemStatus.Completed || chore.status == ChoreItemStatus.PendingProof)

    private fun isUndoRequest(chore: ChoreItem): Boolean =
        chore.status == ChoreItemStatus.Completed || chore.status == ChoreItemStatus.PendingProof

    private suspend fun showChoreCompletedSnackbar(
        choreId: String,
        choreTitle: String,
    ) {
        val points = completionPoints()
        SnackbarController.sendEvent(
            SnackbarEvent(
                message = StringValue.Resource(
                    Res.string.snackbar_chore_completed_points,
                    points,
                ),
                style = SnackbarStyle.Celebration,
                icon = SnackbarMascotIcons.celebrationIconFor(choreId),
                celebration = SnackbarCelebrationContent(
                    choreName = choreTitle,
                    points = points,
                ),
            ),
        )
    }

    private fun completionPoints(): Int =
        rewardsRepository.state.value.snapshot?.rules?.pointsPerCompletion ?: 12

    private fun matchesQuery(
        chore: ChoreItem,
        query: String,
    ): Boolean = query.isBlank() ||
        chore.title.contains(query, ignoreCase = true) ||
        chore.area.contains(query, ignoreCase = true) ||
        chore.assignee.contains(query, ignoreCase = true)

    private fun matchesAssignedFilter(
        chore: ChoreItem,
        selectedFilter: String,
        viewerMode: ChoreViewerMode,
        viewerId: String,
        viewerName: String,
    ): Boolean = when (viewerMode) {
        ChoreViewerMode.Admin -> when (selectedFilter) {
            "Proofs" -> isProofReviewChore(chore) || chore.pendingApproval
            "Inactive" -> !chore.isActive && !chore.pendingApproval
            "Active" -> chore.isActive && !chore.pendingApproval && chore.assignee.isNotBlank() && chore.status != ChoreItemStatus.Completed && !isProofReviewChore(chore)
            else -> true
        }

        ChoreViewerMode.Member -> when (selectedFilter) {
            "Mine" -> isMemberRelevantChore(chore, viewerId, viewerName) && chore.status != ChoreItemStatus.Completed
            "Done" -> isMemberRelevantChore(chore, viewerId, viewerName) && chore.status == ChoreItemStatus.Completed
            "Proof" -> isMemberRelevantChore(chore, viewerId, viewerName) && chore.status == ChoreItemStatus.PendingProof
            else -> isMemberRelevantChore(chore, viewerId, viewerName) && chore.status != ChoreItemStatus.Completed
        }
    }

    private fun isMemberRelevantChore(
        chore: ChoreItem,
        viewerId: String,
        viewerName: String,
    ): Boolean {
        if (!chore.isActive || chore.pendingApproval || chore.assignee.isBlank()) {
            return false
        }
        val assignedToViewerId = viewerId.isNotBlank() && chore.assigneeId.equals(viewerId, ignoreCase = true)
        val assignedToViewerName = chore.assignee.equals(viewerName, ignoreCase = true)
        return assignedToViewerId || assignedToViewerName
    }

    private fun Map<String, List<ChoreListItemUiModel>>.toSectionModels(): List<ChoreRoomSectionUiModel> =
        entries
            .sortedBy { entry -> entry.key }
            .map { entry ->
                ChoreRoomSectionUiModel(
                    title = entry.key,
                    chores = entry.value,
                )
            }

private fun sectionTitle(area: String): String = when {
        area.contains("kitchen", ignoreCase = true) -> "Kitchen"
        area.contains("outside", ignoreCase = true) -> "Outside"
        area.contains("bath", ignoreCase = true) -> "Bathroom"
        area.contains("hall", ignoreCase = true) -> "Entry"
        area.contains("living", ignoreCase = true) -> "Living room"
        else -> area
    }

    private fun isProofReviewChore(chore: ChoreItem): Boolean =
        chore.proofPhotoCount > 0 &&
            (chore.status == ChoreItemStatus.PendingProof || chore.status == ChoreItemStatus.Completed)

    private fun isProofForToday(
        proof: ProofSubmissionUiModel,
    ): Boolean {
        val submittedAt = proof.submittedAtEpochMs
        if (submittedAt <= 0L) {
            return true
        }
        val timeZone = TimeZone.currentSystemDefault()
        val proofDate = kotlin.time.Instant.fromEpochMilliseconds(submittedAt).toLocalDateTime(timeZone).date
        val todayDate = Clock.System.now().toLocalDateTime(timeZone).date
        return proofDate == todayDate
    }

    private fun handleSuggestionAction(
        suggestionId: String,
        accept: Boolean,
    ) {
        val suggestion = _uiState.value.suggestionSections
            .flatMap { it.suggestions }
            .firstOrNull { it.id == suggestionId }
            ?: return
        viewModelScope.launch {
            if (accept) {
                choreRepository.acceptAvailabilitySuggestion(suggestion.toDomain())
                vibratorUtil.triggerVibrationSuccess()
            } else {
                choreRepository.dismissAvailabilitySuggestion(suggestion.toDomain())
                vibratorUtil.triggerVibration()
            }
            refreshSuggestionSections()
            render()
        }
    }

    private fun refreshSuggestionSections() {
        if (!_uiState.value.viewerRole.canManageHouseholdChores() || !autoAssignSuggestionsEnabled) {
            _uiState.update {
                it.copy(
                    isLoadingAvailabilitySuggestions = false,
                    suggestionSections = emptyList(),
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAvailabilitySuggestions = true) }
            val suggestions = choreRepository.getAvailabilitySuggestions(upcomingScheduleDays())
            _uiState.update {
                it.copy(
                    isLoadingAvailabilitySuggestions = false,
                    suggestionSections = suggestions
                        .groupBy { suggestion -> "${suggestion.weekdayLabel} • ${suggestion.dateKey}" }
                        .map { (title, entries) ->
                            ChoreAvailabilitySuggestionSectionUiModel(
                                title = title,
                                suggestions = entries.map { suggestion ->
                                    ChoreAvailabilitySuggestionUiModel(
                                        id = suggestion.id,
                                        choreId = suggestion.choreId,
                                        choreTitle = suggestion.choreTitle,
                                        dateKey = suggestion.dateKey,
                                        weekdayLabel = suggestion.weekdayLabel,
                                        originalAssignee = suggestion.originalAssignee,
                                        recommendedCoverMember = suggestion.recommendedCoverMember,
                                        reasonText = suggestion.reasonText,
                                        scopeStartDate = suggestion.scopeStartDate,
                                        scopeEndDate = suggestion.scopeEndDate,
                                        outsideNormalRotation = suggestion.outsideNormalRotation,
                                    )
                                },
                            )
                        },
                )
            }
        }
    }
}

private object NoOpChoresRewardsRepository : RewardsRepository {
    override val state = MutableStateFlow(RewardsRepositoryState(snapshot = null, isLoading = false))

    override suspend fun refresh() = Unit

    override suspend fun updateRules(rules: RewardsFamilyRules) = Unit

    override suspend fun saveRewardDefinition(draft: RewardDefinitionDraft) = Unit

    override suspend fun deleteRewardDefinition(rewardId: String) = Unit

    override suspend fun redeemReward(rewardId: String) = Unit

    override suspend fun dismissCelebration() = Unit
}

private fun defaultViewerModeFor(
    role: com.sortoapps.chorepilot.features.auth.domain.HouseholdRole,
): ChoreViewerMode = if (role.canManageHouseholdChores()) ChoreViewerMode.Admin else ChoreViewerMode.Member

private fun defaultFilterFor(
    role: com.sortoapps.chorepilot.features.auth.domain.HouseholdRole,
    viewerMode: ChoreViewerMode = defaultViewerModeFor(role),
): String = when (viewerMode) {
    ChoreViewerMode.Admin -> "All"
    ChoreViewerMode.Member -> "Mine"
}

private fun normalizeFilter(
    role: com.sortoapps.chorepilot.features.auth.domain.HouseholdRole,
    viewerMode: ChoreViewerMode,
    selectedFilter: String,
): String {
    val validFilters = when (viewerMode) {
        ChoreViewerMode.Admin -> setOf("All", "Proofs", "Active", "Inactive")
        ChoreViewerMode.Member -> setOf("Mine", "Proof", "Done")
    }
    return selectedFilter.takeIf(validFilters::contains) ?: defaultFilterFor(role, viewerMode)
}

private fun ChoreAvailabilitySuggestionUiModel.toDomain() =
    com.sortoapps.chorepilot.features.chores.domain.AvailabilitySuggestion(
        id = id,
        choreId = choreId,
        choreTitle = choreTitle,
        dateKey = dateKey,
        weekdayLabel = weekdayLabel,
        originalAssignee = originalAssignee,
        recommendedCoverMember = recommendedCoverMember,
        reasonText = reasonText,
        scopeStartDate = scopeStartDate,
        scopeEndDate = scopeEndDate,
        outsideNormalRotation = outsideNormalRotation,
    )
