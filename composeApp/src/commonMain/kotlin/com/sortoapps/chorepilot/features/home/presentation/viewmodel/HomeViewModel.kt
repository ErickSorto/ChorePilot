package com.sortoapps.chorepilot.features.home.presentation.viewmodel

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
import com.sortoapps.chorepilot.features.chores.domain.ChoreItem
import com.sortoapps.chorepilot.features.chores.domain.ChoreItemStatus
import com.sortoapps.chorepilot.features.chores.domain.ChoreRepository
import com.sortoapps.chorepilot.features.chores.domain.ChoreScheduleDay
import com.sortoapps.chorepilot.features.chores.domain.canUndoChoreCompletion
import com.sortoapps.chorepilot.features.chores.domain.requiresCompletionConfirmation
import com.sortoapps.chorepilot.features.home.domain.HomeRepository
import com.sortoapps.chorepilot.features.home.domain.HomeDaySnapshot
import com.sortoapps.chorepilot.features.home.domain.HomeRepositoryState
import com.sortoapps.chorepilot.features.home.domain.HomeSnapshot
import com.sortoapps.chorepilot.features.rewards.domain.RewardDefinitionDraft
import com.sortoapps.chorepilot.features.rewards.domain.RewardsRepositoryState
import com.sortoapps.chorepilot.features.rewards.domain.RewardsRepository
import com.sortoapps.chorepilot.features.settings.domain.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val homeRepository: HomeRepository,
    private val choreRepository: ChoreRepository,
    private val rewardsRepository: RewardsRepository = NoOpHomeRewardsRepository,
    private val settingsRepository: SettingsRepository,
    private val vibratorUtil: VibratorUtil,
) : ViewModel() {
    private var currentSnapshot: HomeSnapshot? = null
    private var autoAssignSuggestionsEnabled = true

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            homeRepository.state.collectLatest(::applyRepositoryState)
        }
        viewModelScope.launch {
            homeRepository.refresh()
        }
        viewModelScope.launch {
            settingsRepository.settings.collectLatest { settings ->
                autoAssignSuggestionsEnabled = settings.autoAssignSuggestions
                _uiState.update {
                    it.copy(autoAssignSuggestionsEnabled = settings.autoAssignSuggestions)
                }
                refreshReassignmentSuggestions()
            }
        }
    }

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            HomeUiEvent.TriggerVibration -> vibratorUtil.triggerVibration()
            HomeUiEvent.TriggerVibrationSuccess -> vibratorUtil.triggerVibrationSuccess()
            is HomeUiEvent.DaySelected -> selectDay(event.dayId)
            is HomeUiEvent.ToggleChoreCompletion -> handleToggleCompletion(event.choreId)
            HomeUiEvent.ConfirmCompletionForAnotherMember -> {
                val prompt = _uiState.value.completionPrompt ?: return
                if (prompt.kind != HomeCompletionPromptKind.CompleteForAnotherMember) return
                viewModelScope.launch {
                    completeChore(
                        choreId = prompt.choreId,
                        allowCompletingForSomeoneElse = true,
                    )
                }
            }

            HomeUiEvent.ConfirmProofRemoval -> {
                val prompt = _uiState.value.completionPrompt ?: return
                if (prompt.kind != HomeCompletionPromptKind.RemoveSubmittedProof) return
                viewModelScope.launch {
                    completeChore(
                        choreId = prompt.choreId,
                        allowCompletingForSomeoneElse = false,
                    )
                }
            }

            HomeUiEvent.DismissCompletionPrompt -> {
                _uiState.update { it.copy(completionPrompt = null) }
            }

            is HomeUiEvent.SurfaceSelected -> {
                _uiState.update { it.copy(selectedSurface = event.surfaceMode) }
            }

            is HomeUiEvent.ReassignmentAccepted -> {
                handleSuggestionAction(
                    suggestionId = event.suggestionId,
                    accept = true,
                )
            }

            is HomeUiEvent.ReassignmentDismissed -> {
                handleSuggestionAction(
                    suggestionId = event.suggestionId,
                    accept = false,
                )
            }

            HomeUiEvent.Refresh -> {
                viewModelScope.launch {
                    homeRepository.refresh()
                    refreshReassignmentSuggestions()
                }
            }
        }
    }

    private fun applyRepositoryState(repositoryState: HomeRepositoryState) {
        val snapshot = repositoryState.snapshot
        if (snapshot == null) {
            _uiState.update { state ->
                state.copy(
                    isLoading = repositoryState.isLoading,
                    errorMessage = repositoryState.errorMessage,
                    focusChores = if (repositoryState.isLoading) emptyList() else state.focusChores,
                )
            }
            return
        }

        currentSnapshot = snapshot
        val selectedDayId = currentSelectedDayId(
            preferred = _uiState.value.days.firstOrNull { it.isSelected }?.id,
            snapshot = snapshot,
        )
        val selectedDay = snapshot.days.first { it.id == selectedDayId }

        _uiState.update { state ->
            state.copy(
                householdName = snapshot.householdName,
                viewerName = snapshot.viewerName,
                viewerId = snapshot.viewerId,
                viewerRole = snapshot.viewerRole,
                headline = snapshot.headline,
                supportingText = snapshot.supportingText,
                selectedDayLabel = selectedDay.label,
                days = snapshot.days.map { day -> day.toUiModel(isSelected = day.id == selectedDayId) },
                metrics = snapshot.metrics,
                focusChores = snapshot.choresByDay[selectedDayId].orEmpty(),
                isLoading = repositoryState.isLoading,
                errorMessage = repositoryState.errorMessage,
            )
        }
        refreshReassignmentSuggestions()
    }

    private fun selectDay(dayId: String) {
        val snapshot = currentSnapshot ?: return
        val selectedDay = snapshot.days.firstOrNull { it.id == dayId } ?: return
        _uiState.update { state ->
            state.copy(
                selectedDayLabel = selectedDay.label,
                days = state.days.map { day -> day.copy(isSelected = day.id == dayId) },
                focusChores = snapshot.choresByDay[dayId].orEmpty(),
            )
        }
        refreshReassignmentSuggestions()
    }

    private fun handleToggleCompletion(choreId: String) {
        val chore = _uiState.value.focusChores.firstOrNull { it.id == choreId }
            ?: choreRepository.getChore(choreId)
            ?: return
        val isUndoRequest = isUndoRequest(chore)
        if (
            isUndoRequest &&
            !canUndoChoreCompletion(
                chore = chore,
                viewerId = _uiState.value.viewerId,
                viewerName = _uiState.value.viewerName,
                viewerRole = _uiState.value.viewerRole,
            )
        ) {
            return
        }
        if (isUndoRequest && shouldConfirmProofRemoval(chore)) {
            _uiState.update { state ->
                state.copy(
                    completionPrompt = HomeCompletionPromptUiModel(
                        choreId = chore.id,
                        choreTitle = chore.title,
                        kind = HomeCompletionPromptKind.RemoveSubmittedProof,
                    ),
                )
            }
        } else if (isUndoRequest) {
            viewModelScope.launch {
                completeChore(
                    choreId = choreId,
                    allowCompletingForSomeoneElse = false,
                )
            }
        } else if (requiresCompletionConfirmation(chore, _uiState.value.viewerName)) {
            _uiState.update { state ->
                state.copy(
                    completionPrompt = HomeCompletionPromptUiModel(
                        choreId = chore.id,
                        choreTitle = chore.title,
                        assigneeName = chore.assignee,
                        kind = HomeCompletionPromptKind.CompleteForAnotherMember,
                    ),
                )
            }
        } else {
            viewModelScope.launch {
                completeChore(
                    choreId = choreId,
                    allowCompletingForSomeoneElse = false,
                )
            }
        }
    }

    private suspend fun completeChore(
        choreId: String,
        allowCompletingForSomeoneElse: Boolean,
    ) {
        val selectedDay = _uiState.value.days.firstOrNull { it.isSelected }?.toScheduleDay()
            ?: return
        val choreBeforeToggle = _uiState.value.focusChores.firstOrNull { it.id == choreId }
        val wasUndoRequest = choreBeforeToggle?.let(::isUndoRequest) == true
        val shouldAnimateRemoval = choreBeforeToggle != null &&
            choreBeforeToggle.status != ChoreItemStatus.Completed &&
            choreBeforeToggle.status != ChoreItemStatus.PendingProof &&
            !choreBeforeToggle.proofRequired

        if (shouldAnimateRemoval) {
            _uiState.update { state ->
                state.copy(animatedRemovalChoreIds = state.animatedRemovalChoreIds + choreId)
            }
        }

        choreRepository.toggleCompletion(
            choreId = choreId,
            viewerName = _uiState.value.viewerName,
            allowCompletingForSomeoneElse = allowCompletingForSomeoneElse,
            day = selectedDay,
        )

        if (wasUndoRequest) {
            vibratorUtil.triggerVibration()
        } else {
            vibratorUtil.triggerVibrationSuccess()
            showChoreCompletedSnackbar(
                choreId = choreId,
                choreTitle = choreBeforeToggle?.title.orEmpty(),
            )
        }

        _uiState.update { it.copy(completionPrompt = null) }

        if (shouldAnimateRemoval) {
            delay(260)
            _uiState.update { state ->
                state.copy(animatedRemovalChoreIds = state.animatedRemovalChoreIds - choreId)
            }
        }
    }

    private fun currentSelectedDayId(
        preferred: String?,
        snapshot: HomeSnapshot,
    ): String = preferred
        ?.takeIf { selectedId -> snapshot.days.any { it.id == selectedId } }
        ?: snapshot.days.firstOrNull { it.isToday }?.id
        ?: snapshot.days.firstOrNull()?.id
        ?: ""

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

    private fun handleSuggestionAction(
        suggestionId: String,
        accept: Boolean,
    ) {
        val suggestion = _uiState.value.reassignmentSuggestions.firstOrNull { it.id == suggestionId } ?: return
        viewModelScope.launch {
            if (accept) {
                choreRepository.acceptAvailabilitySuggestion(suggestion.toDomain())
                vibratorUtil.triggerVibrationSuccess()
            } else {
                choreRepository.dismissAvailabilitySuggestion(suggestion.toDomain())
                vibratorUtil.triggerVibration()
            }
            refreshReassignmentSuggestions()
        }
    }

    private fun refreshReassignmentSuggestions() {
        val snapshot = currentSnapshot ?: return
        val selectedDayId = _uiState.value.days.firstOrNull { it.isSelected }?.id
            ?: snapshot.days.firstOrNull { it.isToday }?.id
            ?: return
        val selectedDay = snapshot.days.firstOrNull { it.id == selectedDayId } ?: return
        if (_uiState.value.viewerRole == com.sortoapps.chorepilot.features.auth.domain.HouseholdRole.Member) {
            _uiState.update {
                it.copy(
                    reassignmentSuggestions = emptyList(),
                    isLoadingReassignmentSuggestions = false,
                )
            }
            return
        }
        if (!autoAssignSuggestionsEnabled) {
            _uiState.update {
                it.copy(
                    reassignmentSuggestions = emptyList(),
                    isLoadingReassignmentSuggestions = false,
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingReassignmentSuggestions = true) }
            val suggestions = choreRepository.getAvailabilitySuggestions(
                days = listOf(selectedDay.toSelectedScheduleDay()),
            )
            _uiState.update { state ->
                state.copy(
                    isLoadingReassignmentSuggestions = false,
                    reassignmentSuggestions = suggestions.map { suggestion ->
                        HomeReassignmentSuggestionUiModel(
                            id = suggestion.id,
                            choreId = suggestion.choreId,
                            title = suggestion.choreTitle,
                            dateKey = suggestion.dateKey,
                            originalAssignee = suggestion.originalAssignee,
                            coverMember = suggestion.recommendedCoverMember,
                            reasonText = suggestion.reasonText,
                            weekdayLabel = suggestion.weekdayLabel,
                            scopeStartDate = suggestion.scopeStartDate,
                            scopeEndDate = suggestion.scopeEndDate,
                            outsideNormalRotation = suggestion.outsideNormalRotation,
                        )
                    },
                )
            }
        }
    }
}

private object NoOpHomeRewardsRepository : RewardsRepository {
    override val state = MutableStateFlow(RewardsRepositoryState(snapshot = null, isLoading = false))

    override suspend fun refresh() = Unit

    override suspend fun updateRules(rules: com.sortoapps.chorepilot.features.rewards.domain.RewardsFamilyRules) = Unit

    override suspend fun saveRewardDefinition(draft: RewardDefinitionDraft) = Unit

    override suspend fun deleteRewardDefinition(rewardId: String) = Unit

    override suspend fun redeemReward(rewardId: String) = Unit

    override suspend fun dismissCelebration() = Unit
}

private fun HomeDaySnapshot.toUiModel(
    isSelected: Boolean,
): HomeDayUiModel = HomeDayUiModel(
    id = id,
    fullLabel = label,
    label = shortLabel,
    supportingText = supportingText,
    dayOffset = dayOffset,
    dateKey = dateKey,
    isSelected = isSelected,
    isToday = isToday,
)

private fun HomeReassignmentSuggestionUiModel.toDomain() =
    com.sortoapps.chorepilot.features.chores.domain.AvailabilitySuggestion(
        id = id,
        choreId = choreId,
        choreTitle = title,
        dateKey = dateKey,
        weekdayLabel = weekdayLabel,
        originalAssignee = originalAssignee,
        recommendedCoverMember = coverMember,
        reasonText = reasonText,
        scopeStartDate = scopeStartDate,
        scopeEndDate = scopeEndDate,
        outsideNormalRotation = outsideNormalRotation,
    )

private fun HomeDayUiModel.toSelectedScheduleDay(): ChoreScheduleDay = ChoreScheduleDay(
    id = id,
    weekdayName = fullLabel,
    dayOffset = dayOffset,
    isToday = isToday,
    dateKey = dateKey,
)

private fun HomeDaySnapshot.toSelectedScheduleDay(): ChoreScheduleDay = ChoreScheduleDay(
    id = id,
    weekdayName = label,
    dayOffset = dayOffset,
    isToday = isToday,
    dateKey = dateKey,
)

private fun HomeDayUiModel.toScheduleDay(): ChoreScheduleDay = ChoreScheduleDay(
    id = id,
    weekdayName = fullLabel,
    dayOffset = dayOffset,
    isToday = isToday,
    dateKey = dateKey,
)
