package com.sortoapps.chorepilot.features.chores.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sortoapps.chorepilot.core.platform.VibratorUtil
import com.sortoapps.chorepilot.core.revenuecat.SubscriptionRepository
import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import com.sortoapps.chorepilot.features.chores.domain.ChoreDailyModeValue
import com.sortoapps.chorepilot.features.chores.domain.ChoreDraft
import com.sortoapps.chorepilot.features.chores.domain.ChoreEffortValue
import com.sortoapps.chorepilot.features.chores.domain.ChoreRepeatFrequencyValue
import com.sortoapps.chorepilot.features.chores.domain.ChoreRepository
import com.sortoapps.chorepilot.features.chores.domain.ChoreSmartAssignmentModeValue
import com.sortoapps.chorepilot.features.chores.domain.HouseholdSchedulingMember
import com.sortoapps.chorepilot.features.chores.domain.RoomDefinition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChoreEditorViewModel(
    private val choreRepository: ChoreRepository,
    private val sessionRepository: SessionRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val vibratorUtil: VibratorUtil,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        editorBaseState(
            canManageLifecycle = sessionRepository.session.value.role.canManageChoreLifecycle(),
        ),
    )
    val uiState: StateFlow<ChoreEditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            choreRepository.refresh()
        }
        viewModelScope.launch {
            choreRepository.rooms.collectLatest { rooms ->
                _uiState.update { state ->
                    val mappedRooms = rooms.toRoomOptions().ifEmpty { defaultRoomOptions() }
                    val selected = mappedRooms.firstOrNull { it.name == state.selectedRoom }
                        ?: mappedRooms.firstOrNull()
                    state.copy(
                        roomOptions = mappedRooms,
                        selectedRoom = selected?.name ?: state.selectedRoom,
                        selectedRoomIcon = if (state.creatingRoom) state.selectedRoomIcon else selected?.iconKey ?: state.selectedRoomIcon,
                        selectedRoomColor = if (state.creatingRoom) state.selectedRoomColor else selected?.colorKey ?: state.selectedRoomColor,
                    )
                }
            }
        }
        viewModelScope.launch {
            choreRepository.householdMembers.collectLatest { members ->
                _uiState.update { state ->
                    state.synchronizedWithMembers(members)
                }
            }
        }
        viewModelScope.launch {
            subscriptionRepository.snapshot.collectLatest { snapshot ->
                _uiState.update { state ->
                    state.copy(
                        hasChorePilotPro = snapshot.hasChorePilotPro,
                        proofRequired = if (snapshot.hasChorePilotPro && state.pendingPremiumProofUnlock) {
                            true
                        } else {
                            state.proofRequired
                        },
                        pendingPremiumProofUnlock = if (snapshot.hasChorePilotPro) {
                            false
                        } else {
                            state.pendingPremiumProofUnlock
                        },
                    )
                }
            }
        }
    }

    fun onEvent(event: ChoreEditorUiEvent) {
        when (event) {
            ChoreEditorUiEvent.TriggerVibration -> vibratorUtil.triggerVibration()
            ChoreEditorUiEvent.TriggerVibrationSuccess -> vibratorUtil.triggerVibrationSuccess()
            is ChoreEditorUiEvent.LoadChore -> loadChore(event.choreId)
            is ChoreEditorUiEvent.TitleChanged -> _uiState.update { it.copy(title = event.value, errorMessage = null) }
            is ChoreEditorUiEvent.AdditionalCommentsChanged -> _uiState.update { it.copy(additionalComments = event.value, errorMessage = null) }
            is ChoreEditorUiEvent.RoomSelected -> {
                _uiState.update { state ->
                    val room = state.roomOptions.firstOrNull { it.name == event.roomName }
                    state.copy(
                        selectedRoom = event.roomName,
                        creatingRoom = false,
                        selectedRoomIcon = room?.iconKey ?: state.selectedRoomIcon,
                        selectedRoomColor = room?.colorKey ?: state.selectedRoomColor,
                        errorMessage = null,
                    )
                }
            }

            is ChoreEditorUiEvent.CreatingRoomChanged -> {
                _uiState.update { state ->
                    state.copy(
                        creatingRoom = event.enabled,
                        newRoomName = if (event.enabled) state.newRoomName else "",
                        errorMessage = null,
                    )
                }
            }

            is ChoreEditorUiEvent.NewRoomNameChanged -> {
                _uiState.update { state ->
                    state.copy(
                        newRoomName = event.value,
                        selectedRoom = event.value.ifBlank { state.selectedRoom },
                        errorMessage = null,
                    )
                }
            }

            is ChoreEditorUiEvent.RoomIconSelected -> _uiState.update { it.copy(selectedRoomIcon = event.iconKey) }
            is ChoreEditorUiEvent.RoomColorSelected -> _uiState.update { it.copy(selectedRoomColor = event.colorKey) }
            is ChoreEditorUiEvent.AssigneeChanged -> _uiState.update { it.copy(assignee = event.value, errorMessage = null) }
            is ChoreEditorUiEvent.AutoAssignChanged -> {
                _uiState.update { state ->
                    state.copy(
                        autoAssign = event.enabled,
                        assignee = if (event.enabled) {
                            state.assignee
                        } else {
                            state.assignee.ifBlank {
                                state.assigneeOptions.firstOrNull().orEmpty()
                            }
                        },
                        smartAssignmentMode = if (!event.enabled) SmartAssignmentMode.FairRotation else state.smartAssignmentMode,
                        errorMessage = null,
                    )
                }
            }

            is ChoreEditorUiEvent.SmartAssignmentModeChanged -> _uiState.update { it.copy(smartAssignmentMode = event.mode) }
            is ChoreEditorUiEvent.AssignmentPoolToggled -> {
                _uiState.update { state ->
                    val updatedPool = state.assignmentPool.toMutableSet()
                    if (updatedPool.contains(event.member)) {
                        if (updatedPool.size > 1) {
                            updatedPool.remove(event.member)
                        }
                    } else {
                        updatedPool.add(event.member)
                    }
                    val updatedDayAssignments = state.dayAssignments.mapValues { (_, assignee) ->
                        if (assignee == event.member && event.member !in updatedPool) "Smart fair" else assignee
                    }
                    state.copy(
                        assignmentPool = updatedPool,
                        dayAssignments = updatedDayAssignments,
                    )
                }
            }

            is ChoreEditorUiEvent.DayAssignmentChanged -> {
                _uiState.update { state ->
                    state.copy(
                        dayAssignments = state.dayAssignments + (event.weekday to event.assignee),
                        assignmentPool = if (event.assignee == "Smart fair") state.assignmentPool else state.assignmentPool + event.assignee,
                    )
                }
            }

            is ChoreEditorUiEvent.EffortLevelChanged -> _uiState.update { it.copy(effortLevel = event.level) }
            is ChoreEditorUiEvent.RepeatsEnabledChanged -> {
                _uiState.update { state ->
                    state.copy(
                        repeatsEnabled = event.enabled,
                        smartAssignmentMode = if (!event.enabled && state.smartAssignmentMode == SmartAssignmentMode.DayPlan) {
                            SmartAssignmentMode.FairRotation
                        } else {
                            state.smartAssignmentMode
                        },
                    )
                }
            }

            is ChoreEditorUiEvent.RepeatFrequencyChanged -> {
                _uiState.update { state ->
                    state.copy(
                        repeatFrequency = event.frequency,
                        smartAssignmentMode = if (event.frequency == ChoreRepeatFrequency.Monthly || event.frequency == ChoreRepeatFrequency.Custom) {
                            SmartAssignmentMode.FairRotation
                        } else {
                            state.smartAssignmentMode
                        },
                        selectedWeekdays = when (event.frequency) {
                            ChoreRepeatFrequency.Daily -> defaultWeekdays()
                            ChoreRepeatFrequency.Weekly -> state.selectedWeekdays.ifEmpty { setOf(ChoreWeekday.Tuesday) }
                            else -> state.selectedWeekdays
                        },
                    )
                }
            }

            is ChoreEditorUiEvent.RepeatStartDateChanged -> _uiState.update { it.copy(repeatStartDate = event.value) }
            is ChoreEditorUiEvent.OneTimeDateChanged -> _uiState.update { it.copy(selectedOneTimeDate = event.value) }
            is ChoreEditorUiEvent.DailyRepeatModeChanged -> {
                _uiState.update { state ->
                    state.copy(
                        dailyRepeatMode = event.mode,
                        selectedWeekdays = when (event.mode) {
                            DailyRepeatMode.EveryDay -> ChoreWeekday.entries.toSet()
                            DailyRepeatMode.Weekdays -> defaultWeekdays()
                            DailyRepeatMode.SpecificDays -> state.selectedWeekdays.ifEmpty { setOf(ChoreWeekday.Monday, ChoreWeekday.Wednesday, ChoreWeekday.Friday) }
                        },
                    )
                }
            }

            is ChoreEditorUiEvent.WeekdayToggled -> {
                _uiState.update { state ->
                    val updated = state.selectedWeekdays.toMutableSet()
                    if (updated.contains(event.weekday)) {
                        if (updated.size > 1) {
                            updated.remove(event.weekday)
                        }
                    } else {
                        updated.add(event.weekday)
                    }
                    state.copy(selectedWeekdays = updated)
                }
            }

            is ChoreEditorUiEvent.MonthlyPatternChanged -> _uiState.update { it.copy(monthlyPattern = event.value) }
            is ChoreEditorUiEvent.CustomPatternChanged -> _uiState.update { it.copy(customPattern = event.value) }
            is ChoreEditorUiEvent.ProofRequiredChanged -> _uiState.update {
                it.copy(
                    proofRequired = event.enabled,
                    pendingPremiumProofUnlock = false,
                )
            }
            is ChoreEditorUiEvent.ProofRequiredUpgradeRequested -> _uiState.update {
                it.copy(pendingPremiumProofUnlock = event.enabled)
            }
            is ChoreEditorUiEvent.DurationEnabledChanged -> _uiState.update { it.copy(durationEnabled = event.enabled) }
            is ChoreEditorUiEvent.DurationMinutesChanged -> _uiState.update { it.copy(durationMinutes = event.minutes) }
            is ChoreEditorUiEvent.LifecycleActionRequested -> requestLifecycleAction(event.action)
            ChoreEditorUiEvent.ConfirmLifecycleAction -> confirmLifecycleAction()
            ChoreEditorUiEvent.DismissLifecycleConfirmation -> {
                _uiState.update { it.copy(lifecycleConfirmation = null) }
            }
            is ChoreEditorUiEvent.LifecycleConfirmationTextChanged -> {
                _uiState.update { state ->
                    state.copy(
                        lifecycleConfirmation = state.lifecycleConfirmation?.withConfirmationText(event.value),
                    )
                }
            }
            ChoreEditorUiEvent.DeleteNavigationHandled -> {
                _uiState.update { it.copy(shouldCloseAfterDelete = false) }
            }
        }
    }

    private fun requestLifecycleAction(
        action: ChoreLifecycleAction,
    ) {
        val state = _uiState.value
        val choreId = state.choreId ?: return
        if (!state.canManageLifecycle || state.isSaving || state.isApplyingLifecycleAction) return
        when (action) {
            ChoreLifecycleAction.Reactivate -> performLifecycleAction(
                action = action,
                choreId = choreId,
                choreTitle = state.title,
            )

            ChoreLifecycleAction.Deactivate,
            ChoreLifecycleAction.Delete,
            -> _uiState.update {
                it.copy(
                    lifecycleFeedbackMessage = null,
                    lifecycleConfirmation = buildChoreLifecycleConfirmation(
                        action = action,
                        choreId = choreId,
                        choreTitle = state.title.ifBlank { "this chore" },
                    ),
                )
            }
        }
    }

    private fun confirmLifecycleAction() {
        val confirmation = _uiState.value.lifecycleConfirmation ?: return
        performLifecycleAction(
            action = confirmation.action,
            choreId = confirmation.choreId,
            choreTitle = confirmation.choreTitle,
        )
    }

    private fun performLifecycleAction(
        action: ChoreLifecycleAction,
        choreId: String,
        choreTitle: String,
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isApplyingLifecycleAction = true,
                    lifecycleConfirmation = null,
                    errorMessage = null,
                    lifecycleFeedbackMessage = null,
                )
            }
            runCatching {
                when (action) {
                    ChoreLifecycleAction.Delete -> choreRepository.deleteChore(choreId)
                    ChoreLifecycleAction.Deactivate -> {
                        val draft = choreRepository.getDraft(choreId) ?: error("We couldn't reload this chore.")
                        choreRepository.saveChore(draft.copy(isActive = false))
                    }

                    ChoreLifecycleAction.Reactivate -> {
                        val draft = choreRepository.getDraft(choreId) ?: error("We couldn't reload this chore.")
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
                        loadChore(choreId)
                        vibratorUtil.triggerVibrationSuccess()
                        _uiState.update {
                            it.copy(
                                isApplyingLifecycleAction = false,
                                lifecycleFeedbackMessage = when (action) {
                                    ChoreLifecycleAction.Deactivate ->
                                        "${choreTitle.ifBlank { "This chore" }} is inactive. Proof and completion history stay attached."

                                    ChoreLifecycleAction.Reactivate ->
                                        "${choreTitle.ifBlank { "This chore" }} is active again and ready for the schedule."

                                    ChoreLifecycleAction.Delete -> null
                                },
                            )
                        }
                    }
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isApplyingLifecycleAction = false,
                        errorMessage = error.message ?: "We couldn't update this chore yet.",
                    )
                }
            }
        }
    }

    fun save(onSaved: (String) -> Unit) {
        viewModelScope.launch {
            val validationError = _uiState.value.validateForSave()
            if (validationError != null) {
                _uiState.update { it.copy(errorMessage = validationError) }
                return@launch
            }
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching {
                val state = _uiState.value
                val room = if (state.creatingRoom) {
                    RoomOptionUiModel(
                        id = "",
                        name = state.newRoomName.ifBlank { state.selectedRoom },
                        iconKey = state.selectedRoomIcon,
                        colorKey = state.selectedRoomColor,
                    )
                } else {
                    state.roomOptions.firstOrNull { it.name == state.selectedRoom }
                        ?: RoomOptionUiModel(
                            id = "",
                            name = state.selectedRoom,
                            iconKey = state.selectedRoomIcon,
                            colorKey = state.selectedRoomColor,
                        )
                }
                choreRepository.saveChore(
                    state.toDraft(room),
                )
            }.onSuccess { choreId ->
                vibratorUtil.triggerVibrationSuccess()
                onSaved(choreId)
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message ?: "We couldn't save this chore yet.") }
            }
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    private fun loadChore(choreId: String?) {
        val roomOptions = choreRepository.rooms.value.toRoomOptions().ifEmpty { defaultRoomOptions() }
        val householdMembers = choreRepository.householdMembers.value
        val canManageLifecycle = sessionRepository.session.value.role.canManageChoreLifecycle()
        if (choreId == null) {
            _uiState.value = editorBaseState(
                choreId = null,
                roomOptions = roomOptions,
                canManageLifecycle = canManageLifecycle,
            )
                .synchronizedWithMembers(householdMembers)
            return
        }

        val draft = choreRepository.getDraft(choreId)
        if (draft == null) {
            _uiState.value = editorBaseState(
                choreId = choreId,
                roomOptions = roomOptions,
                canManageLifecycle = canManageLifecycle,
            )
                .synchronizedWithMembers(householdMembers)
            return
        }

        val selectedRoom = roomOptions.firstOrNull { it.id == draft.roomId || it.name == draft.roomName }
        _uiState.value = editorBaseState(
            choreId = choreId,
            roomOptions = roomOptions,
            canManageLifecycle = canManageLifecycle,
        ).copy(
            choreId = draft.id,
            isActive = draft.isActive,
            pendingApproval = draft.pendingApproval,
            title = draft.title,
            additionalComments = draft.additionalComments,
            selectedRoom = selectedRoom?.name ?: draft.roomName,
            selectedRoomIcon = draft.roomIconKey,
            selectedRoomColor = draft.roomColorKey,
            assignee = draft.assignee,
            autoAssign = draft.autoAssign,
            smartAssignmentMode = draft.smartAssignmentMode.toUiMode(),
            assignmentPool = draft.assignmentPool,
            dayAssignments = draft.dayAssignments.entries.associate { (day, assignee) ->
                ChoreWeekday.valueOf(day) to assignee
            },
            effortLevel = draft.effortLevel.toUiEffort(),
            repeatsEnabled = draft.repeatsEnabled,
            repeatStartDate = draft.repeatStartDate.ifBlank { "Today" },
            repeatFrequency = draft.repeatFrequency.toUiFrequency(),
            dailyRepeatMode = draft.dailyRepeatMode.toUiDailyMode(),
            selectedWeekdays = draft.selectedWeekdays.mapNotNull { name ->
                ChoreWeekday.entries.firstOrNull { it.name == name }
            }.toSet().ifEmpty { defaultWeekdays() },
            monthlyPattern = draft.monthlyPattern.ifBlank { "15th" },
            customPattern = draft.customPattern.ifBlank { "Every 2 weeks" },
            selectedOneTimeDate = draft.selectedOneTimeDate.ifBlank { "Today" },
            proofRequired = draft.proofRequired,
            durationEnabled = draft.durationMinutes != null,
            durationMinutes = draft.durationMinutes ?: 15,
            hasChorePilotPro = subscriptionRepository.snapshot.value.hasChorePilotPro,
        ).synchronizedWithMembers(householdMembers)
    }
}

private fun editorBaseState(
    choreId: String? = null,
    roomOptions: List<RoomOptionUiModel> = defaultRoomOptions(),
    canManageLifecycle: Boolean = false,
): ChoreEditorUiState = ChoreEditorUiState(
    choreId = choreId,
    canManageLifecycle = canManageLifecycle,
    isActive = true,
    pendingApproval = false,
    title = "",
    additionalComments = "",
    roomOptions = roomOptions,
    selectedRoom = roomOptions.firstOrNull()?.name ?: "Kitchen",
    creatingRoom = false,
    newRoomName = "",
    roomIconOptions = defaultRoomIcons(),
    roomColorOptions = defaultRoomColors(),
    selectedRoomIcon = roomOptions.firstOrNull()?.iconKey ?: "kitchen",
    selectedRoomColor = roomOptions.firstOrNull()?.colorKey ?: "teal",
    autoAssign = false,
    smartAssignmentMode = SmartAssignmentMode.FairRotation,
    assignmentPool = emptySet(),
    dayAssignments = emptyMap(),
    effortLevel = ChoreEffortLevel.Standard,
    repeatsEnabled = false,
    repeatStartDate = "Today",
    repeatFrequency = ChoreRepeatFrequency.Daily,
    dailyRepeatMode = DailyRepeatMode.Weekdays,
    selectedWeekdays = defaultWeekdays(),
    monthlyPattern = "15th",
    customPattern = "Every 2 weeks",
    oneTimeDateOptions = defaultOneTimeDateOptions(),
    selectedOneTimeDate = "Today",
    proofRequired = false,
    durationEnabled = true,
    durationMinutes = 15,
    hasChorePilotPro = false,
)

private fun ChoreEditorUiState.synchronizedWithMembers(
    members: List<HouseholdSchedulingMember>,
): ChoreEditorUiState {
    val activeNames = members
        .filter { it.active }
        .map { it.displayName.trim() }
        .filter(String::isNotBlank)
        .distinctCaseInsensitive()
        .sortedBy { it.lowercase() }
    val retainedNames = buildSet {
        assignee.takeIf(String::isNotBlank)?.let(::add)
        assignmentPool.filter(String::isNotBlank).forEach(::add)
        dayAssignments.values
            .filter { it.isNotBlank() && it != SMART_FAIR_ASSIGNEE }
            .forEach(::add)
    }
        .distinctCaseInsensitive()
        .filterNot { retained ->
            activeNames.any { active -> active.equals(retained, ignoreCase = true) }
        }
        .sortedBy { it.lowercase() }
    val mergedOptions = (activeNames + retainedNames)
        .distinctCaseInsensitive()
        .sortedBy { it.lowercase() }
    val sanitizedPool = assignmentPool
        .filter { candidate ->
            candidate.isNotBlank() &&
                mergedOptions.any { option -> option.equals(candidate, ignoreCase = true) }
        }
        .toSet()
        .ifEmpty {
            if (assignmentPool.isEmpty()) {
                activeNames.toSet()
            } else {
                emptySet()
            }
        }
    val sanitizedDayAssignments = dayAssignments.mapNotNull { (weekday, assignee) ->
        when {
            assignee == SMART_FAIR_ASSIGNEE -> weekday to assignee
            mergedOptions.any { option -> option.equals(assignee, ignoreCase = true) } -> weekday to assignee
            else -> null
        }
    }.toMap()
    val resolvedAssignee = when {
        assignee.isNotBlank() &&
            mergedOptions.any { option -> option.equals(assignee, ignoreCase = true) } -> assignee
        assignee.isBlank() -> mergedOptions.firstOrNull().orEmpty()
        else -> assignee
    }
    return copy(
        assigneeOptions = mergedOptions,
        activeAssigneeOptions = activeNames.toSet(),
        inactiveAssignmentNames = retainedNames.toSet(),
        assignee = resolvedAssignee,
        assignmentPool = sanitizedPool,
        dayAssignments = sanitizedDayAssignments,
    )
}

private fun List<RoomDefinition>.toRoomOptions(): List<RoomOptionUiModel> = map { room ->
    RoomOptionUiModel(
        id = room.id,
        name = room.name,
        iconKey = room.iconKey,
        colorKey = room.colorKey,
    )
}

private fun ChoreEditorUiState.toDraft(
    room: RoomOptionUiModel,
): ChoreDraft = ChoreDraft(
    id = choreId,
    title = title.trim(),
    additionalComments = additionalComments.trim(),
    roomId = room.id,
    roomName = room.name,
    roomIconKey = room.iconKey,
    roomColorKey = room.colorKey,
    assignee = assignee,
    autoAssign = autoAssign,
    smartAssignmentMode = smartAssignmentMode.toDomainMode(),
    assignmentPool = assignmentPool,
    dayAssignments = dayAssignments.mapKeys { it.key.name },
    effortLevel = effortLevel.toDomainEffort(),
    repeatsEnabled = repeatsEnabled,
    repeatStartDate = repeatStartDate,
    repeatFrequency = repeatFrequency.toDomainFrequency(),
    dailyRepeatMode = dailyRepeatMode.toDomainDailyMode(),
    selectedWeekdays = selectedWeekdays.map { it.name }.toSet(),
    monthlyPattern = monthlyPattern,
    customPattern = customPattern,
    selectedOneTimeDate = selectedOneTimeDate,
    proofRequired = proofRequired,
    durationMinutes = durationMinutes.takeIf { durationEnabled },
    isActive = isActive,
    pendingApproval = pendingApproval,
)

private fun ChoreEditorUiState.validateForSave(): String? = when {
    assigneeOptions.isEmpty() && inactiveAssignmentNames.isEmpty() ->
        "Add an active household member before assigning this chore."
    !autoAssign && assignee.isBlank() ->
        "Choose who should own this chore before saving."
    autoAssign && smartAssignmentMode == SmartAssignmentMode.FairRotation && assignmentPool.isEmpty() ->
        "Choose at least one household member for rotation before saving."
    autoAssign &&
        smartAssignmentMode == SmartAssignmentMode.DayPlan &&
        assignmentPool.isEmpty() &&
        dayAssignments.values.none { it != SMART_FAIR_ASSIGNEE } ->
        "Choose at least one household member for this day plan before saving."
    else -> null
}

private fun Iterable<String>.distinctCaseInsensitive(): List<String> {
    val seen = linkedSetOf<String>()
    return buildList {
        for (value in this@distinctCaseInsensitive) {
            val normalized = value.lowercase()
            if (seen.add(normalized)) {
                add(value)
            }
        }
    }
}

private const val SMART_FAIR_ASSIGNEE = "Smart fair"

private fun ChoreSmartAssignmentModeValue.toUiMode(): SmartAssignmentMode = when (this) {
    ChoreSmartAssignmentModeValue.FairRotation -> SmartAssignmentMode.FairRotation
    ChoreSmartAssignmentModeValue.DayPlan -> SmartAssignmentMode.DayPlan
}

private fun SmartAssignmentMode.toDomainMode(): ChoreSmartAssignmentModeValue = when (this) {
    SmartAssignmentMode.FairRotation -> ChoreSmartAssignmentModeValue.FairRotation
    SmartAssignmentMode.DayPlan -> ChoreSmartAssignmentModeValue.DayPlan
}

private fun ChoreEffortValue.toUiEffort(): ChoreEffortLevel = when (this) {
    ChoreEffortValue.Light -> ChoreEffortLevel.Light
    ChoreEffortValue.Standard -> ChoreEffortLevel.Standard
    ChoreEffortValue.Heavy -> ChoreEffortLevel.Heavy
}

private fun ChoreEffortLevel.toDomainEffort(): ChoreEffortValue = when (this) {
    ChoreEffortLevel.Light -> ChoreEffortValue.Light
    ChoreEffortLevel.Standard -> ChoreEffortValue.Standard
    ChoreEffortLevel.Heavy -> ChoreEffortValue.Heavy
}

private fun ChoreRepeatFrequencyValue.toUiFrequency(): ChoreRepeatFrequency = when (this) {
    ChoreRepeatFrequencyValue.Daily -> ChoreRepeatFrequency.Daily
    ChoreRepeatFrequencyValue.Weekly -> ChoreRepeatFrequency.Weekly
    ChoreRepeatFrequencyValue.Monthly -> ChoreRepeatFrequency.Monthly
    ChoreRepeatFrequencyValue.Custom -> ChoreRepeatFrequency.Custom
}

private fun ChoreRepeatFrequency.toDomainFrequency(): ChoreRepeatFrequencyValue = when (this) {
    ChoreRepeatFrequency.Daily -> ChoreRepeatFrequencyValue.Daily
    ChoreRepeatFrequency.Weekly -> ChoreRepeatFrequencyValue.Weekly
    ChoreRepeatFrequency.Monthly -> ChoreRepeatFrequencyValue.Monthly
    ChoreRepeatFrequency.Custom -> ChoreRepeatFrequencyValue.Custom
}

private fun ChoreDailyModeValue.toUiDailyMode(): DailyRepeatMode = when (this) {
    ChoreDailyModeValue.EveryDay -> DailyRepeatMode.EveryDay
    ChoreDailyModeValue.Weekdays -> DailyRepeatMode.Weekdays
    ChoreDailyModeValue.SpecificDays -> DailyRepeatMode.SpecificDays
}

private fun DailyRepeatMode.toDomainDailyMode(): ChoreDailyModeValue = when (this) {
    DailyRepeatMode.EveryDay -> ChoreDailyModeValue.EveryDay
    DailyRepeatMode.Weekdays -> ChoreDailyModeValue.Weekdays
    DailyRepeatMode.SpecificDays -> ChoreDailyModeValue.SpecificDays
}
