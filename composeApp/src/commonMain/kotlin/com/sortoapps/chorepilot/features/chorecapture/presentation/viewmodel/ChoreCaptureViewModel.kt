package com.sortoapps.chorepilot.features.chorecapture.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sortoapps.chorepilot.core.platform.VibratorUtil
import com.sortoapps.chorepilot.core.revenuecat.SubscriptionRepository
import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import com.sortoapps.chorepilot.features.auth.domain.HouseholdRole
import com.sortoapps.chorepilot.features.chorecapture.domain.DiscoveryAnalysisRepository
import com.sortoapps.chorepilot.features.chorecapture.domain.DiscoveryDraftItem
import com.sortoapps.chorepilot.features.chores.domain.ChoreDailyModeValue
import com.sortoapps.chorepilot.features.chores.domain.ChoreDraft
import com.sortoapps.chorepilot.features.chores.domain.ChoreEffortValue
import com.sortoapps.chorepilot.features.chores.domain.ChoreRepeatFrequencyValue
import com.sortoapps.chorepilot.features.chores.domain.ChoreRepository
import com.sortoapps.chorepilot.features.chores.domain.ChoreScheduleDay
import com.sortoapps.chorepilot.features.chores.domain.ChoreSmartAssignmentModeValue
import com.sortoapps.chorepilot.features.chores.domain.ProofUploadPayload
import com.sortoapps.chorepilot.features.chores.domain.RoomDefinition
import com.sortoapps.chorepilot.features.notifications.domain.HouseholdNotifications
import com.sortoapps.chorepilot.features.notifications.domain.DiscoveryChoreSubmittedNotification
import com.sortoapps.chorepilot.features.notifications.domain.ProofSubmittedNotification
import com.sortoapps.chorepilot.features.chores.domain.todayScheduleDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

class ChoreCaptureViewModel(
    private val choreRepository: ChoreRepository,
    private val discoveryAnalysisRepository: DiscoveryAnalysisRepository,
    private val sessionRepository: SessionRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val householdNotifications: HouseholdNotifications,
    private val vibratorUtil: VibratorUtil,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChoreCaptureUiState())
    val uiState: StateFlow<ChoreCaptureUiState> = _uiState.asStateFlow()

    private var pendingPayload: ProofUploadPayload? = null
    private var pendingDay: ChoreScheduleDay? = null
    private var draftCounter = 0

    init {
        viewModelScope.launch {
            subscriptionRepository.snapshot.collectLatest { snapshot ->
                var shouldResumeDiscovery = false
                _uiState.update { state ->
                    shouldResumeDiscovery = !state.hasChorePilotPro &&
                        snapshot.hasChorePilotPro &&
                        state.isDiscoveryMode &&
                        state.hasCapturedPhoto &&
                        pendingPayload != null &&
                        state.discoveryPhase == DiscoveryCapturePhase.Capture
                    state.copy(
                        hasChorePilotPro = snapshot.hasChorePilotPro,
                        shouldOpenPremium = if (snapshot.hasChorePilotPro) {
                            false
                        } else {
                            state.shouldOpenPremium
                        },
                    )
                }
                if (shouldResumeDiscovery) {
                    retryDiscoveryAnalysis()
                }
            }
        }
    }

    fun onEvent(event: ChoreCaptureUiEvent) {
        when (event) {
            ChoreCaptureUiEvent.TriggerVibration -> vibratorUtil.triggerVibration()
            ChoreCaptureUiEvent.TriggerVibrationSuccess -> vibratorUtil.triggerVibrationSuccess()
            is ChoreCaptureUiEvent.LoadChore -> loadChore(event.choreId, event.day)
            is ChoreCaptureUiEvent.ProofCaptured -> onProofCaptured(event.payload)
            is ChoreCaptureUiEvent.CameraFailed -> {
                _uiState.update { it.copy(errorMessage = event.message) }
            }

            ChoreCaptureUiEvent.UploadProof -> uploadProof()
            ChoreCaptureUiEvent.RetryDiscoveryAnalysis -> retryDiscoveryAnalysis()
            ChoreCaptureUiEvent.AddDiscoveryDraft -> addBlankDiscoveryDraft()
            is ChoreCaptureUiEvent.DeleteDiscoveryDraft -> {
                _uiState.update { state ->
                    state.copy(discoveryDrafts = state.discoveryDrafts.filterNot { it.id == event.draftId })
                }
            }
            is ChoreCaptureUiEvent.DiscoveryDraftTitleChanged -> updateDraft(event.draftId) { copy(title = event.value) }
            is ChoreCaptureUiEvent.DiscoveryDraftRoomChanged -> updateDraft(event.draftId) { copy(roomName = event.value) }
            is ChoreCaptureUiEvent.DiscoveryDraftEffortChanged -> updateDraft(event.draftId) { copy(effort = event.value) }
            is ChoreCaptureUiEvent.DiscoveryDraftProofRequiredChanged -> updateDraft(event.draftId) { copy(proofNeededSuggested = event.enabled) }
            is ChoreCaptureUiEvent.DiscoveryDraftRepeatsChanged -> updateDraft(event.draftId) { copy(repeatsEnabled = event.enabled) }
            is ChoreCaptureUiEvent.DiscoveryDraftRepeatFrequencyChanged -> updateDraft(event.draftId) {
                copy(
                    repeatFrequency = event.value,
                    selectedWeekdays = when (event.value) {
                        ChoreRepeatFrequencyValue.Daily -> selectedWeekdays.ifEmpty { setOf("Monday", "Wednesday", "Friday") }
                        ChoreRepeatFrequencyValue.Weekly -> selectedWeekdays.ifEmpty { setOf("Saturday") }
                        ChoreRepeatFrequencyValue.Monthly,
                        ChoreRepeatFrequencyValue.Custom,
                        -> selectedWeekdays
                    },
                )
            }
            is ChoreCaptureUiEvent.DiscoveryDraftDailyModeChanged -> updateDraft(event.draftId) {
                copy(
                    dailyMode = event.value,
                    selectedWeekdays = when (event.value) {
                        ChoreDailyModeValue.SpecificDays -> selectedWeekdays.ifEmpty { setOf("Monday", "Wednesday", "Friday") }
                        else -> selectedWeekdays
                    },
                )
            }
            is ChoreCaptureUiEvent.DiscoveryDraftWeekdayToggled -> updateDraft(event.draftId) {
                val updated = selectedWeekdays.toMutableSet()
                if (!updated.add(event.value)) {
                    updated.remove(event.value)
                }
                copy(selectedWeekdays = updated)
            }
            is ChoreCaptureUiEvent.DiscoveryDraftOneTimeDateChanged -> updateDraft(event.draftId) { copy(selectedOneTimeDate = event.value) }
            ChoreCaptureUiEvent.SaveDiscoveryDrafts -> saveDiscoveryDrafts()
            ChoreCaptureUiEvent.PremiumNavigationHandled -> _uiState.update { it.copy(shouldOpenPremium = false) }
            ChoreCaptureUiEvent.DiscoveryReturnHandled -> _uiState.update { it.copy(shouldReturnToList = false) }
            ChoreCaptureUiEvent.ClearError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun loadChore(
        choreId: String,
        day: ChoreScheduleDay?,
    ) {
        val session = sessionRepository.session.value
        val isAdminFlow = session.role == HouseholdRole.Owner || session.role == HouseholdRole.Admin
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            unavailableMessage = null,
            errorMessage = null,
            shouldOpenPremium = false,
            shouldReturnToList = false,
        )
        if (choreId.isBlank()) {
            pendingPayload = null
            pendingDay = null
            _uiState.value = ChoreCaptureUiState(
                isLoading = false,
                isDiscoveryMode = true,
                isAdminFlow = isAdminFlow,
                title = "Scan for chores",
                subtitle = "Take one room photo and turn what you spot into editable chore drafts.",
                helperTitle = "Show the full room",
                helperBody = "Wider, brighter photos help AI catch more chores on the first pass.",
                primaryActionLabel = "Take room photo",
                discoverySaveActionLabel = if (isAdminFlow) "Activate chores" else "Submit for review",
                hasChorePilotPro = subscriptionRepository.snapshot.value.hasChorePilotPro,
                discoveryPhase = DiscoveryCapturePhase.Capture,
            )
            return
        }

        val chore = choreRepository.getChore(choreId)
        if (chore == null) {
            pendingPayload = null
            pendingDay = day
            _uiState.value = ChoreCaptureUiState(
                isLoading = false,
                choreId = choreId,
                isAdminFlow = isAdminFlow,
                title = "Chore unavailable",
                subtitle = "This chore isn't ready for proof right now.",
                helperTitle = "Back out and refresh",
                helperBody = "It may have been deleted, reassigned, or moved to a different day.",
                unavailableMessage = "Open the chore list and pick the latest version before uploading proof.",
                primaryActionLabel = "Back to chores",
                hasChorePilotPro = subscriptionRepository.snapshot.value.hasChorePilotPro,
            )
            return
        }
        pendingPayload = null
        pendingDay = day
        _uiState.value = ChoreCaptureUiState(
            isLoading = false,
            isDiscoveryMode = false,
            isAdminFlow = isAdminFlow,
            choreId = chore.id,
            dayLabel = when {
                day == null -> ""
                day.isToday -> "Today"
                else -> day.weekdayName
            },
            choreTitle = chore.title,
            assigneeName = chore.assignee,
            roomName = chore.area,
            dueLabel = chore.dueLabel,
            title = "Upload proof",
            subtitle = "Add one clear photo of the finished chore.",
            helperTitle = "Show the finished result",
            helperBody = "One clear photo is enough.",
            hasCapturedPhoto = false,
            previewUri = null,
            captureLabel = if (chore.proofPhotoCount > 0) {
                "${chore.proofPhotoCount} proof photo${if (chore.proofPhotoCount == 1) "" else "s"} already submitted for this day."
            } else {
                ""
            },
            primaryActionLabel = "Take photo",
            uploadComplete = false,
            errorMessage = null,
            hasChorePilotPro = subscriptionRepository.snapshot.value.hasChorePilotPro,
        )
    }

    private fun onProofCaptured(payload: ProofUploadPayload) {
        vibratorUtil.triggerVibration()
        pendingPayload = payload
        val current = _uiState.value
        if (!current.isDiscoveryMode) {
            _uiState.update {
                it.copy(
                    previewUri = payload.previewUri,
                    hasCapturedPhoto = true,
                    uploadComplete = false,
                    errorMessage = null,
                    captureLabel = "Photo ready to submit.",
                    primaryActionLabel = "Submit for review",
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                previewUri = payload.previewUri,
                hasCapturedPhoto = true,
                uploadComplete = false,
                errorMessage = null,
                successMessage = null,
                captureLabel = if (it.hasChorePilotPro) "Analyzing your room photo…" else "Photo ready. Discovery mode is part of ChorePilot Pro.",
                primaryActionLabel = if (it.hasChorePilotPro) "Analyzing…" else "Unlock discovery mode",
                discoveryPhase = if (it.hasChorePilotPro) DiscoveryCapturePhase.Analyzing else DiscoveryCapturePhase.Capture,
                shouldOpenPremium = !it.hasChorePilotPro,
            )
        }
        if (current.hasChorePilotPro || subscriptionRepository.snapshot.value.hasChorePilotPro) {
            analyzeDiscoveryPhoto()
        }
    }

    private fun retryDiscoveryAnalysis() {
        val payload = pendingPayload ?: return
        _uiState.update {
            it.copy(
                errorMessage = null,
                captureLabel = "Analyzing your room photo…",
                primaryActionLabel = "Analyzing…",
                discoveryPhase = DiscoveryCapturePhase.Analyzing,
            )
        }
        if (!_uiState.value.hasChorePilotPro) {
            _uiState.update { it.copy(shouldOpenPremium = true, discoveryPhase = DiscoveryCapturePhase.Capture) }
            return
        }
        pendingPayload = payload
        analyzeDiscoveryPhoto()
    }

    private fun analyzeDiscoveryPhoto() {
        val payload = pendingPayload ?: return
        viewModelScope.launch {
            runCatching {
                discoveryAnalysisRepository.analyzeRoomPhoto(payload)
            }.onSuccess { result ->
                val mappedDrafts = result.drafts.map { it.toUiModel() }
                _uiState.update {
                    it.copy(
                        discoveryPhase = DiscoveryCapturePhase.ReviewDrafts,
                        discoveryDrafts = mappedDrafts,
                        discoverySummary = "We found ${mappedDrafts.size} draft chore${if (mappedDrafts.size == 1) "" else "s"} for you to review.",
                        captureLabel = "Review the drafts, tweak anything, then save them into chores.",
                        primaryActionLabel = "Save drafts",
                        errorMessage = null,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        discoveryPhase = DiscoveryCapturePhase.Error,
                        errorMessage = error.message ?: "We couldn't analyze that room photo yet.",
                        captureLabel = "Try again with better light or a wider room shot.",
                        primaryActionLabel = "Retry analysis",
                    )
                }
            }
        }
    }

    private fun addBlankDiscoveryDraft() {
        _uiState.update { state ->
            state.copy(
                discoveryDrafts = state.discoveryDrafts + DiscoveryDraftUiModel(
                    id = nextDraftId(),
                    title = "",
                    roomName = guessPrimaryRoomName(),
                    effort = ChoreEffortValue.Standard,
                    proofNeededSuggested = false,
                    repeatsEnabled = false,
                    repeatFrequency = ChoreRepeatFrequencyValue.Weekly,
                    dailyMode = ChoreDailyModeValue.Weekdays,
                    selectedWeekdays = setOf("Saturday"),
                    selectedOneTimeDate = "Tomorrow",
                ),
            )
        }
    }

    private fun updateDraft(
        draftId: String,
        transform: DiscoveryDraftUiModel.() -> DiscoveryDraftUiModel,
    ) {
        _uiState.update { state ->
            state.copy(
                discoveryDrafts = state.discoveryDrafts.map { draft ->
                    if (draft.id == draftId) draft.transform() else draft
                },
            )
        }
    }

    private fun uploadProof() {
        if (_uiState.value.isDiscoveryMode) {
            saveDiscoveryDrafts()
            return
        }
        val choreId = _uiState.value.choreId
        val payload = pendingPayload ?: return
        if (choreId.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            runCatching {
                choreRepository.submitProof(
                    choreId = choreId,
                    payload = payload,
                    day = pendingDay,
                )
            }.onSuccess {
                runCatching {
                    val session = sessionRepository.session.value
                    householdNotifications.onProofSubmitted(
                        ProofSubmittedNotification(
                            householdId = session.householdId,
                            choreId = choreId,
                            choreTitle = _uiState.value.choreTitle.ifBlank { choreRepository.getChore(choreId)?.title.orEmpty() },
                            dateKey = (pendingDay ?: todayScheduleDay()).dateKey,
                            assigneeName = _uiState.value.assigneeName.ifBlank { choreRepository.getChore(choreId)?.assignee.orEmpty() },
                            submittingMemberName = session.displayName,
                        ),
                    )
                }
                pendingPayload = null
                vibratorUtil.triggerVibrationSuccess()
                _uiState.update { state ->
                    state.copy(
                        isSubmitting = false,
                        hasCapturedPhoto = true,
                        uploadComplete = true,
                        captureLabel = "Submitted for review.",
                        primaryActionLabel = "Back to chore",
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = error.message ?: "We couldn't upload that proof photo.",
                    )
                }
            }
        }
    }

    private fun saveDiscoveryDrafts() {
        val payload = pendingPayload ?: return
        val state = _uiState.value
        if (!state.isDiscoveryMode || !state.canSaveDiscoveryDrafts || state.isSubmitting) return
        val session = sessionRepository.session.value
        val isAdminFlow = session.role == HouseholdRole.Owner || session.role == HouseholdRole.Admin

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSubmitting = true,
                    discoveryPhase = DiscoveryCapturePhase.Saving,
                    errorMessage = null,
                    captureLabel = if (isAdminFlow) {
                        "Activating these chores for the household…"
                    } else {
                        "Submitting these chores for household review…"
                    },
                )
            }
            runCatching {
                val discoverySessionId = buildDiscoverySessionId()
                val storedImage = discoveryAnalysisRepository.persistAcceptedRoomPhoto(
                    discoverySessionId = discoverySessionId,
                    payload = payload,
                )
                state.discoveryDrafts
                    .filter { it.isValid }
                    .forEach { draft ->
                        val savedChoreId = choreRepository.saveChore(
                            draft.toChoreDraft(
                                creatorUserId = session.userId,
                                creatorName = session.displayName,
                                room = resolveRoomDefinition(draft.roomName),
                                storedImage = storedImage,
                                pendingApproval = !isAdminFlow,
                            ),
                        )
                        if (!isAdminFlow) {
                            householdNotifications.onDiscoveryChoreSubmitted(
                                DiscoveryChoreSubmittedNotification(
                                    householdId = session.householdId,
                                    choreId = savedChoreId,
                                    choreTitle = draft.title.trim(),
                                    submittingMemberName = session.displayName,
                                ),
                            )
                        }
                    }
            }.onSuccess {
                pendingPayload = null
                choreRepository.refresh()
                vibratorUtil.triggerVibrationSuccess()
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        discoveryPhase = DiscoveryCapturePhase.Saved,
                        successMessage = if (isAdminFlow) {
                            "New chores are now active in your household list."
                        } else {
                            "Your new chores were submitted for household approval."
                        },
                        captureLabel = if (isAdminFlow) {
                            "Everything is active. Heading back to chores…"
                        } else {
                            "Submitted for review. Heading back to chores…"
                        },
                        shouldReturnToList = true,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        discoveryPhase = DiscoveryCapturePhase.Error,
                        errorMessage = error.message ?: "We couldn't save those draft chores yet.",
                        captureLabel = "Nothing was saved yet. You can retry after checking the drafts.",
                    )
                }
            }
        }
    }

    private fun resolveRoomDefinition(
        roomName: String,
    ): RoomDefinition {
        val normalizedName = roomName.trim()
        return choreRepository.rooms.value.firstOrNull { it.name.equals(normalizedName, ignoreCase = true) }
            ?: inferredRoomDefinition(normalizedName)
    }

    private fun inferredRoomDefinition(
        roomName: String,
    ): RoomDefinition {
        val normalized = roomName.lowercase()
        val iconKey = when {
            "bath" in normalized -> "bathroom"
            "bed" in normalized -> "bedroom"
            "living" in normalized || "family" in normalized || "den" in normalized -> "living"
            "entry" in normalized || "hall" in normalized -> "entry"
            "outside" in normalized || "garage" in normalized || "yard" in normalized -> "outside"
            "laundry" in normalized -> "laundry"
            else -> "kitchen"
        }
        val colorKey = when (iconKey) {
            "bathroom" -> "sky"
            "bedroom" -> "amber"
            "living" -> "violet"
            "entry" -> "rose"
            "outside" -> "sage"
            else -> "teal"
        }
        return RoomDefinition(
            id = "",
            name = roomName.ifBlank { "Kitchen" },
            iconKey = iconKey,
            colorKey = colorKey,
        )
    }

    private fun guessPrimaryRoomName(): String = choreRepository.rooms.value.firstOrNull()?.name ?: "Kitchen"

    private fun nextDraftId(): String {
        draftCounter += 1
        return "draft-${Clock.System.now().toEpochMilliseconds()}-$draftCounter"
    }

    private fun buildDiscoverySessionId(): String = "discovery-${Clock.System.now().toEpochMilliseconds()}"
}

private fun DiscoveryDraftItem.toUiModel(): DiscoveryDraftUiModel = DiscoveryDraftUiModel(
    id = id,
    title = title,
    description = description,
    roomName = roomName,
    effort = effort,
    proofNeededSuggested = proofNeededSuggested,
    repeatsEnabled = repeatsEnabled,
    repeatFrequency = repeatFrequency,
    dailyMode = dailyMode,
    selectedWeekdays = selectedWeekdays,
    selectedOneTimeDate = selectedOneTimeDate,
)

private fun DiscoveryDraftUiModel.toChoreDraft(
    creatorUserId: String,
    creatorName: String,
    room: RoomDefinition,
    storedImage: com.sortoapps.chorepilot.features.chorecapture.domain.DiscoverySourceImage,
    pendingApproval: Boolean,
): ChoreDraft = ChoreDraft(
    title = title.trim(),
    additionalComments = description.trim().ifBlank { "Generated from a room photo in discovery mode." },
    roomId = room.id,
    roomName = room.name,
    roomIconKey = room.iconKey,
    roomColorKey = room.colorKey,
    assignee = creatorName,
    autoAssign = false,
    smartAssignmentMode = ChoreSmartAssignmentModeValue.FairRotation,
    assignmentPool = emptySet(),
    dayAssignments = emptyMap(),
    effortLevel = effort,
    repeatsEnabled = repeatsEnabled,
    repeatStartDate = if (repeatsEnabled) "Today" else "",
    repeatFrequency = repeatFrequency,
    dailyRepeatMode = dailyMode,
    selectedWeekdays = selectedWeekdays.ifEmpty { setOf("Saturday") },
    monthlyPattern = "15th",
    customPattern = "Every 2 weeks",
    selectedOneTimeDate = if (repeatsEnabled) "" else selectedOneTimeDate.ifBlank { "Tomorrow" },
    proofRequired = proofNeededSuggested,
    durationMinutes = when (effort) {
        ChoreEffortValue.Light -> 10
        ChoreEffortValue.Standard -> 20
        ChoreEffortValue.Heavy -> 35
    },
    isActive = !pendingApproval,
    pendingApproval = pendingApproval,
    submitterUserId = creatorUserId,
    submitterName = creatorName,
    discoverySessionId = storedImage.discoverySessionId,
    discoveryImageStoragePath = storedImage.storagePath,
    discoveryImageUrl = storedImage.downloadUrl,
    discoveryGenerated = true,
)
