package com.sortoapps.chorepilot.features.chores.presentation.viewmodel

import com.sortoapps.chorepilot.core.platform.VibratorUtil
import com.sortoapps.chorepilot.features.auth.domain.AuthFlowResult
import com.sortoapps.chorepilot.features.auth.domain.HouseholdRole
import com.sortoapps.chorepilot.features.auth.domain.InvitePreview
import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import com.sortoapps.chorepilot.features.auth.domain.SessionSnapshot
import com.sortoapps.chorepilot.features.chores.domain.AvailabilitySuggestion
import com.sortoapps.chorepilot.features.chores.domain.ApproveProofUseCase
import com.sortoapps.chorepilot.features.chores.domain.ChoreDraft
import com.sortoapps.chorepilot.features.chores.domain.ChoreItem
import com.sortoapps.chorepilot.features.chores.domain.ChoreItemStatus
import com.sortoapps.chorepilot.features.chores.domain.ChoreRepository
import com.sortoapps.chorepilot.features.chores.domain.ChoreScheduleDay
import com.sortoapps.chorepilot.features.chores.domain.ProofEvidence
import com.sortoapps.chorepilot.features.chores.domain.ProofUploadPayload
import com.sortoapps.chorepilot.features.chores.domain.ProofUploadResult
import com.sortoapps.chorepilot.features.chores.domain.RoomDefinition
import com.sortoapps.chorepilot.features.chores.domain.todayScheduleDay
import com.sortoapps.chorepilot.features.settings.domain.SettingsRepository
import com.sortoapps.chorepilot.features.settings.domain.SettingsSnapshot
import com.sortoapps.chorepilot.features.settings.domain.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChoresViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun adminLoadsWeeklyAvailabilitySuggestions() = runTest(dispatcher) {
        val repository = FakeSuggestionChoreRepository(
                suggestions = listOf(
                    testSuggestion(
                        id = "s1",
                        dateKey = todayScheduleDay().dateKey,
                        weekdayLabel = todayScheduleDay().weekdayName,
                    ),
                ),
            )

        val sessionRepository = FakeSuggestionSessionRepository()
        val viewModel = ChoresViewModel(
            choreRepository = repository,
            sessionRepository = sessionRepository,
            settingsRepository = FakeSuggestionSettingsRepository(),
            approveProofUseCase = ApproveProofUseCase(choreRepository = repository, sessionRepository = sessionRepository, householdNotifications = FakeHouseholdNotifications()),
            vibratorUtil = FakeSuggestionVibratorUtil(),
        )

        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.suggestionSections.size)
        assertEquals("Trash", viewModel.uiState.value.suggestionSections.first().suggestions.first().choreTitle)
    }

    @Test
    fun dismissingSuggestionRemovesItFromQueue() = runTest(dispatcher) {
        val repository = FakeSuggestionChoreRepository(
                suggestions = listOf(
                    testSuggestion(
                        id = "s1",
                        dateKey = todayScheduleDay().dateKey,
                        weekdayLabel = todayScheduleDay().weekdayName,
                    ),
                ),
            )

        val sessionRepository = FakeSuggestionSessionRepository()
        val viewModel = ChoresViewModel(
            choreRepository = repository,
            sessionRepository = sessionRepository,
            settingsRepository = FakeSuggestionSettingsRepository(),
            approveProofUseCase = ApproveProofUseCase(choreRepository = repository, sessionRepository = sessionRepository, householdNotifications = FakeHouseholdNotifications()),
            vibratorUtil = FakeSuggestionVibratorUtil(),
        )

        advanceUntilIdle()
        viewModel.onEvent(ChoresUiEvent.AvailabilitySuggestionDismissed("s1"))
        advanceUntilIdle()

        assertEquals("s1", repository.dismissedSuggestionId)
        assertTrue(viewModel.uiState.value.suggestionSections.isEmpty())
    }

    @Test
    fun inactiveFilterOnlyShowsInactiveChoresAndAllKeepsNeedsOwnerSeparate() = runTest(dispatcher) {
        val repository = FakeLifecycleListChoreRepository(
            chores = listOf(
                baseLifecycleChore(id = "active-1", title = "Counters", assignee = "Erick", isActive = true),
                baseLifecycleChore(id = "inactive-1", title = "Pantry", assignee = "Erick", isActive = false),
                baseLifecycleChore(id = "owner-1", title = "Laundry", assignee = "", isActive = true),
            ),
        )
        val sessionRepository = FakeSuggestionSessionRepository()
        val viewModel = ChoresViewModel(
            choreRepository = repository,
            sessionRepository = sessionRepository,
            settingsRepository = FakeSuggestionSettingsRepository(),
            approveProofUseCase = ApproveProofUseCase(choreRepository = repository, sessionRepository = sessionRepository, householdNotifications = FakeHouseholdNotifications()),
            vibratorUtil = FakeSuggestionVibratorUtil(),
        )

        advanceUntilIdle()
        viewModel.onEvent(ChoresUiEvent.FilterSelected("Inactive"))
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.inactiveSections.sumOf { it.chores.size })
        assertTrue(viewModel.uiState.value.needsOwnerSections.isEmpty())

        viewModel.onEvent(ChoresUiEvent.FilterSelected("All"))
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.needsOwnerSections.sumOf { it.chores.size })
    }

    @Test
    fun reactivateFromInactiveListMovesChoreBackToActiveSections() = runTest(dispatcher) {
        val repository = FakeLifecycleListChoreRepository(
            chores = listOf(
                baseLifecycleChore(id = "inactive-1", title = "Pantry", assignee = "Erick", isActive = false),
            ),
        )
        val sessionRepository = FakeSuggestionSessionRepository()
        val viewModel = ChoresViewModel(
            choreRepository = repository,
            sessionRepository = sessionRepository,
            settingsRepository = FakeSuggestionSettingsRepository(),
            approveProofUseCase = ApproveProofUseCase(choreRepository = repository, sessionRepository = sessionRepository, householdNotifications = FakeHouseholdNotifications()),
            vibratorUtil = FakeSuggestionVibratorUtil(),
        )

        advanceUntilIdle()
        viewModel.onEvent(ChoresUiEvent.ReactivateRequested("inactive-1"))
        advanceUntilIdle()

        assertTrue(repository.savedDrafts.last().isActive)
        assertEquals(1, viewModel.uiState.value.activeSections.sumOf { it.chores.size })
        assertTrue(viewModel.uiState.value.inactiveSections.isEmpty())
    }

    @Test
    fun approvingPendingDiscoveryMovesItOutOfReviewQueue() = runTest(dispatcher) {
        val repository = FakeLifecycleListChoreRepository(
            chores = listOf(
                baseLifecycleChore(
                    id = "pending-1",
                    title = "Line up shoes",
                    assignee = "Mila",
                    assigneeId = "member-1",
                    isActive = false,
                    pendingApproval = true,
                    submitterName = "Mila",
                    submitterUserId = "member-1",
                ),
            ),
        )
        val sessionRepository = FakeSuggestionSessionRepository()
        val viewModel = ChoresViewModel(
            choreRepository = repository,
            sessionRepository = sessionRepository,
            settingsRepository = FakeSuggestionSettingsRepository(),
            approveProofUseCase = ApproveProofUseCase(choreRepository = repository, sessionRepository = sessionRepository, householdNotifications = FakeHouseholdNotifications()),
            vibratorUtil = FakeSuggestionVibratorUtil(),
        )

        advanceUntilIdle()
        viewModel.onEvent(ChoresUiEvent.PendingApprovalApproved("pending-1"))
        advanceUntilIdle()

        assertEquals(1, repository.approvePendingCalls)
        assertTrue(viewModel.uiState.value.pendingApprovalSections.isEmpty())
        assertEquals(1, viewModel.uiState.value.activeSections.sumOf { it.chores.size })
    }

    @Test
    fun pendingDiscoveryQueueCarriesSubmitterAndScheduleContext() = runTest(dispatcher) {
        val repository = FakeLifecycleListChoreRepository(
            chores = listOf(
                baseLifecycleChore(
                    id = "pending-1",
                    title = "Line up shoes",
                    assignee = "Mila",
                    assigneeId = "member-1",
                    isActive = false,
                    pendingApproval = true,
                    submitterName = "Mila",
                    submitterUserId = "member-1",
                ),
            ),
        )
        val sessionRepository = FakeSuggestionSessionRepository()
        val viewModel = ChoresViewModel(
            choreRepository = repository,
            sessionRepository = sessionRepository,
            settingsRepository = FakeSuggestionSettingsRepository(),
            approveProofUseCase = ApproveProofUseCase(choreRepository = repository, sessionRepository = sessionRepository, householdNotifications = FakeHouseholdNotifications()),
            vibratorUtil = FakeSuggestionVibratorUtil(),
        )

        advanceUntilIdle()

        val pending = viewModel.uiState.value.pendingApprovalSections.single().chores.single()
        assertEquals("Mila", pending.submitterName)
        assertEquals("Weekly", pending.cadence)
        assertEquals("10 min", pending.durationLabel)
        assertTrue(pending.pendingApproval)
    }

    @Test
    fun rejectingPendingDiscoveryRequiresReasonAndRemovesItFromReviewQueue() = runTest(dispatcher) {
        val repository = FakeLifecycleListChoreRepository(
            chores = listOf(
                baseLifecycleChore(
                    id = "pending-1",
                    title = "Line up shoes",
                    assignee = "Mila",
                    assigneeId = "member-1",
                    isActive = false,
                    pendingApproval = true,
                    submitterName = "Mila",
                    submitterUserId = "member-1",
                ),
            ),
        )
        val sessionRepository = FakeSuggestionSessionRepository()
        val viewModel = ChoresViewModel(
            choreRepository = repository,
            sessionRepository = sessionRepository,
            settingsRepository = FakeSuggestionSettingsRepository(),
            approveProofUseCase = ApproveProofUseCase(choreRepository = repository, sessionRepository = sessionRepository, householdNotifications = FakeHouseholdNotifications()),
            vibratorUtil = FakeSuggestionVibratorUtil(),
        )

        advanceUntilIdle()
        viewModel.onEvent(ChoresUiEvent.PendingApprovalRejectRequested("pending-1"))
        viewModel.onEvent(ChoresUiEvent.PendingApprovalRejectionReasonChanged("Too vague"))
        viewModel.onEvent(ChoresUiEvent.PendingApprovalRejectConfirmed)
        advanceUntilIdle()

        assertEquals("Too vague", repository.rejectedPendingReason)
        assertTrue(viewModel.uiState.value.pendingApprovalSections.isEmpty())
        assertEquals(null, viewModel.uiState.value.pendingApprovalRejectionDialog)
    }

    @Test
    fun memberStartsInMemberModeAndCannotToggleIntoAdmin() = runTest(dispatcher) {
        val repository = FakeLifecycleListChoreRepository(
            chores = listOf(
                baseLifecycleChore(id = "assigned-1", title = "Counters", assignee = "Mila", assigneeId = "member-1", isActive = true),
            ),
        )
        val viewModel = ChoresViewModel(
            choreRepository = repository,
            sessionRepository = FakeSuggestionSessionRepository(
                role = HouseholdRole.Member,
                userId = "member-1",
                displayName = "Mila",
            ),
            settingsRepository = FakeSuggestionSettingsRepository(),
            approveProofUseCase = ApproveProofUseCase(
                choreRepository = repository,
                sessionRepository = FakeSuggestionSessionRepository(
                    role = HouseholdRole.Member,
                    userId = "member-1",
                    displayName = "Mila",
                ),
                householdNotifications = FakeHouseholdNotifications(),
            ),
            vibratorUtil = FakeSuggestionVibratorUtil(),
        )

        advanceUntilIdle()
        assertEquals(ChoreViewerMode.Member, viewModel.uiState.value.viewerMode)
        assertEquals("Mine", viewModel.uiState.value.selectedFilter)
        assertTrue(!viewModel.uiState.value.canUseAdminExperience)

        viewModel.onEvent(ChoresUiEvent.ViewerModeToggled)
        advanceUntilIdle()

        assertEquals(ChoreViewerMode.Member, viewModel.uiState.value.viewerMode)
        assertTrue(viewModel.uiState.value.proofQueueSections.isEmpty())
        assertTrue(viewModel.uiState.value.inactiveSections.isEmpty())
    }

    @Test
    fun memberOnlySeesOwnRelevantWorkAndAdminMutationsAreIgnored() = runTest(dispatcher) {
        val repository = FakeLifecycleListChoreRepository(
            chores = listOf(
                baseLifecycleChore(id = "mine-open", title = "Counters", assignee = "Mila", assigneeId = "member-1", isActive = true),
                baseLifecycleChore(
                    id = "mine-proof",
                    title = "Trash",
                    assignee = "Mila",
                    assigneeId = "member-1",
                    isActive = true,
                    status = ChoreItemStatus.PendingProof,
                ),
                baseLifecycleChore(
                    id = "mine-done",
                    title = "Laundry",
                    assignee = "Mila",
                    assigneeId = "member-1",
                    isActive = true,
                    status = ChoreItemStatus.Completed,
                ),
                baseLifecycleChore(id = "other-open", title = "Mirrors", assignee = "Ava", assigneeId = "admin-1", isActive = true),
                baseLifecycleChore(id = "inactive", title = "Pantry", assignee = "Mila", assigneeId = "member-1", isActive = false),
                baseLifecycleChore(id = "pending", title = "Discovery", assignee = "Mila", assigneeId = "member-1", isActive = true, pendingApproval = true),
                baseLifecycleChore(id = "unassigned", title = "Wipe sink", assignee = "", assigneeId = "", isActive = true),
            ),
        )
        val sessionRepository = FakeSuggestionSessionRepository(
            role = HouseholdRole.Member,
            userId = "member-1",
            displayName = "Mila",
        )
        val viewModel = ChoresViewModel(
            choreRepository = repository,
            sessionRepository = sessionRepository,
            settingsRepository = FakeSuggestionSettingsRepository(),
            approveProofUseCase = ApproveProofUseCase(choreRepository = repository, sessionRepository = sessionRepository, householdNotifications = FakeHouseholdNotifications()),
            vibratorUtil = FakeSuggestionVibratorUtil(),
        )

        advanceUntilIdle()
        assertEquals(listOf("mine-open", "mine-proof"), viewModel.uiState.value.chores.map { it.id })
        assertTrue(viewModel.uiState.value.suggestionSections.isEmpty())
        assertTrue(viewModel.uiState.value.pendingApprovalSections.isEmpty())
        assertTrue(viewModel.uiState.value.needsOwnerSections.isEmpty())

        viewModel.onEvent(ChoresUiEvent.FilterSelected("Done"))
        advanceUntilIdle()
        assertEquals(listOf("mine-done"), viewModel.uiState.value.chores.map { it.id })

        viewModel.onEvent(ChoresUiEvent.FilterSelected("Proof"))
        advanceUntilIdle()
        assertEquals(listOf("mine-proof"), viewModel.uiState.value.chores.map { it.id })

        viewModel.onEvent(ChoresUiEvent.ReactivateRequested("inactive"))
        viewModel.onEvent(ChoresUiEvent.PendingApprovalApproved("pending"))
        advanceUntilIdle()
        assertTrue(repository.savedDrafts.isEmpty())
    }

    @Test
    fun proofApprovalUsesSharedNotificationAndFeedbackPath() = runTest(dispatcher) {
        val repository = FakeLifecycleListChoreRepository(
            chores = listOf(
                baseLifecycleChore(
                    id = "proof-1",
                    title = "Trash",
                    assignee = "Ava",
                    assigneeId = "admin-1",
                    isActive = true,
                    status = ChoreItemStatus.PendingProof,
                    proofPhotoCount = 1,
                    dueLabel = "1 photo uploaded",
                ),
            ),
            proofs = mapOf(
                "proof-1" to ProofEvidence(
                    proofId = "proof-1",
                    choreId = "proof-1",
                    dateKey = "2026-04-08",
                    uploadedByUserId = "admin-1",
                    uploadedByName = "Ava",
                    storagePath = "proofs/proof-1.jpg",
                    downloadUrl = "https://example.com/proof.jpg",
                    createdAtEpochMs = 123L,
                ),
            ),
        )
        val sessionRepository = FakeSuggestionSessionRepository()
        val notifications = FakeHouseholdNotifications()
        val viewModel = ChoresViewModel(
            choreRepository = repository,
            sessionRepository = sessionRepository,
            settingsRepository = FakeSuggestionSettingsRepository(),
            approveProofUseCase = ApproveProofUseCase(choreRepository = repository, sessionRepository = sessionRepository, householdNotifications = notifications),
            vibratorUtil = FakeSuggestionVibratorUtil(),
        )

        advanceUntilIdle()
        viewModel.onEvent(ChoresUiEvent.ProofApproved("proof-1"))
        advanceUntilIdle()

        assertEquals(1, repository.approveCalls)
        assertEquals(ChoreItemStatus.Completed, repository.getChore("proof-1")?.status)
        assertEquals("Approved", repository.getChore("proof-1")?.dueLabel)
        assertEquals("Ava's proof was approved and the chore is now complete.", viewModel.uiState.value.feedbackMessage)
        assertEquals(1, notifications.approved.size)
        assertEquals("2026-04-08", notifications.approved.single().dateKey)
    }
}

private class FakeSuggestionChoreRepository(
    suggestions: List<AvailabilitySuggestion>,
) : ChoreRepository {
    override val chores: StateFlow<List<ChoreItem>> = MutableStateFlow(emptyList())
    override val rooms: StateFlow<List<RoomDefinition>> = MutableStateFlow(emptyList())
    override val householdMembers: StateFlow<List<com.sortoapps.chorepilot.features.chores.domain.HouseholdSchedulingMember>> = MutableStateFlow(emptyList())
    private val suggestionState = MutableStateFlow(suggestions)

    var dismissedSuggestionId: String? = null
        private set

    override fun getChores(): List<ChoreItem> = emptyList()

    override fun getChore(id: String, day: ChoreScheduleDay?): ChoreItem? = null

    override fun getLatestProofForChore(choreId: String, day: ChoreScheduleDay?): ProofEvidence? = null

    override fun getDraft(id: String): ChoreDraft? = null

    override fun getScheduledChores(day: ChoreScheduleDay): List<ChoreItem> = emptyList()

    override suspend fun getAvailabilitySuggestions(days: List<ChoreScheduleDay>): List<AvailabilitySuggestion> =
        suggestionState.value.filter { suggestion -> days.any { it.dateKey == suggestion.dateKey } }

    override suspend fun refresh() = Unit

    override suspend fun saveChore(draft: ChoreDraft): String = "new"

    override suspend fun deleteChore(choreId: String) = Unit

    override suspend fun toggleCompletion(
        choreId: String,
        viewerName: String,
        allowCompletingForSomeoneElse: Boolean,
        day: ChoreScheduleDay?,
    ) = Unit

    override suspend fun submitProof(
        choreId: String,
        payload: ProofUploadPayload,
        day: ChoreScheduleDay?,
    ): ProofUploadResult = ProofUploadResult("proof", "", "")

    override suspend fun approveProof(choreId: String, day: ChoreScheduleDay?) = Unit

    override suspend fun rejectProof(choreId: String, reason: String, day: ChoreScheduleDay?) = Unit

    override suspend fun acceptAvailabilitySuggestion(suggestion: AvailabilitySuggestion) {
        suggestionState.value = suggestionState.value.filterNot { it.id == suggestion.id }
    }

    override suspend fun dismissAvailabilitySuggestion(suggestion: AvailabilitySuggestion) {
        dismissedSuggestionId = suggestion.id
        suggestionState.value = suggestionState.value.filterNot { it.id == suggestion.id }
    }
}

private class FakeSuggestionSessionRepository(
    role: HouseholdRole = HouseholdRole.Owner,
    userId: String = "erick",
    displayName: String = "Erick",
) : SessionRepository {
    override val session: StateFlow<SessionSnapshot> = MutableStateFlow(
        SessionSnapshot(
            signedIn = true,
            userId = userId,
            displayName = displayName,
            householdId = "house-1",
            householdName = "River House",
            role = role,
        ),
    )

    override suspend fun refreshSession() = Unit
    override suspend fun completeOnboarding(displayName: String) = Unit
    override suspend fun signIn(email: String, password: String, displayNameHint: String): AuthFlowResult = AuthFlowResult.Main
    override suspend fun signInWithGoogle(idToken: String, accessToken: String?, displayNameHint: String, emailHint: String): AuthFlowResult = AuthFlowResult.Main
    override suspend fun createHousehold(displayName: String, householdName: String, householdSizeLabel: String): AuthFlowResult = AuthFlowResult.Main
    override suspend fun joinHousehold(displayName: String, inviteCode: String): AuthFlowResult = AuthFlowResult.Main
    override suspend fun previewInvite(inviteCode: String): InvitePreview? = null
    override suspend fun dismissInitialPremiumFlow() = Unit
    override suspend fun signOut() = Unit
    override suspend fun resetDemo() = Unit
}

private class FakeSuggestionSettingsRepository : SettingsRepository {
    override val settings: StateFlow<SettingsSnapshot> = MutableStateFlow(SettingsSnapshot(autoAssignSuggestions = true))

    override suspend fun updateThemeMode(themeMode: ThemeMode) = Unit
    override suspend fun updateEditorialWarmth(enabled: Boolean) = Unit
    override suspend fun updateNotifications(enabled: Boolean) = Unit
    override suspend fun updateWeekStartsOnMonday(enabled: Boolean) = Unit
    override suspend fun updateAutoAssignSuggestions(enabled: Boolean) = Unit
    override suspend fun updateLargerText(enabled: Boolean) = Unit
    override suspend fun updateAlwaysShowLabels(enabled: Boolean) = Unit
}

private class FakeSuggestionVibratorUtil : VibratorUtil {
    override fun triggerVibration() = Unit
    override fun triggerVibrationSuccess() = Unit
    override fun cancelVibration() = Unit
}

private class FakeHouseholdNotifications : com.sortoapps.chorepilot.features.notifications.domain.HouseholdNotifications {
    val approved = mutableListOf<com.sortoapps.chorepilot.features.notifications.domain.ProofApprovedNotification>()

    override suspend fun onProofSubmitted(notification: com.sortoapps.chorepilot.features.notifications.domain.ProofSubmittedNotification) = Unit

    override suspend fun onProofApproved(notification: com.sortoapps.chorepilot.features.notifications.domain.ProofApprovedNotification) {
        approved += notification
    }
}

private class FakeLifecycleListChoreRepository(
    chores: List<ChoreItem>,
    proofs: Map<String, ProofEvidence> = emptyMap(),
) : ChoreRepository {
    private val allChores = MutableStateFlow(chores)
    private val scheduledChores = MutableStateFlow(chores.filter { it.isActive })
    private val proofsByChoreId = proofs.toMutableMap()
    val savedDrafts = mutableListOf<ChoreDraft>()
    var approveCalls = 0
        private set
    var approvePendingCalls = 0
        private set
    var rejectedPendingReason: String? = null
        private set

    override val chores: StateFlow<List<ChoreItem>> = scheduledChores
    override val rooms: StateFlow<List<RoomDefinition>> = MutableStateFlow(emptyList())
    override val householdMembers: StateFlow<List<com.sortoapps.chorepilot.features.chores.domain.HouseholdSchedulingMember>> = MutableStateFlow(emptyList())

    override fun getChores(): List<ChoreItem> = allChores.value
    override fun getChore(id: String, day: ChoreScheduleDay?): ChoreItem? = allChores.value.firstOrNull { it.id == id }
    override fun getLatestProofForChore(choreId: String, day: ChoreScheduleDay?): ProofEvidence? = proofsByChoreId[choreId]
    override fun getDraft(id: String): ChoreDraft? = allChores.value.firstOrNull { it.id == id }?.toLifecycleDraft()
    override fun getScheduledChores(day: ChoreScheduleDay): List<ChoreItem> = scheduledChores.value
    override suspend fun getAvailabilitySuggestions(days: List<ChoreScheduleDay>) = emptyList<AvailabilitySuggestion>()
    override suspend fun refresh() = Unit

    override suspend fun saveChore(draft: ChoreDraft): String {
        val id = draft.id ?: "new"
        val saved = draft.copy(id = id)
        savedDrafts += saved
        allChores.value = allChores.value.map { chore ->
            if (chore.id != id) chore else chore.copy(
                title = saved.title,
                assignee = saved.assignee,
                assigneeId = saved.assignee.lowercase(),
                isActive = saved.isActive,
                pendingApproval = saved.pendingApproval,
            )
        }
        scheduledChores.value = allChores.value.filter { it.isActive }
        return id
    }

    override suspend fun deleteChore(choreId: String) {
        allChores.value = allChores.value.filterNot { it.id == choreId }
        scheduledChores.value = allChores.value.filter { it.isActive }
    }

    override suspend fun toggleCompletion(
        choreId: String,
        viewerName: String,
        allowCompletingForSomeoneElse: Boolean,
        day: ChoreScheduleDay?,
    ) = Unit

    override suspend fun submitProof(
        choreId: String,
        payload: ProofUploadPayload,
        day: ChoreScheduleDay?,
    ): ProofUploadResult = ProofUploadResult("proof", "", "")

    override suspend fun approveProof(choreId: String, day: ChoreScheduleDay?) {
        approveCalls += 1
        allChores.value = allChores.value.map { chore ->
            if (chore.id != choreId) chore else chore.copy(
                status = ChoreItemStatus.Completed,
                dueLabel = "Approved",
            )
        }
        scheduledChores.value = allChores.value.filter { it.isActive }
    }

    override suspend fun approvePendingChore(choreId: String) {
        approvePendingCalls += 1
        allChores.value = allChores.value.map { chore ->
            if (chore.id != choreId) chore else chore.copy(
                isActive = true,
                pendingApproval = false,
                reviewRejectedReason = "",
            )
        }
        scheduledChores.value = allChores.value.filter { it.isActive }
    }

    override suspend fun rejectPendingChore(choreId: String, reason: String) {
        rejectedPendingReason = reason
        allChores.value = allChores.value.map { chore ->
            if (chore.id != choreId) chore else chore.copy(
                isActive = false,
                pendingApproval = false,
                reviewRejectedReason = reason,
            )
        }
        scheduledChores.value = allChores.value.filter { it.isActive }
    }

    override suspend fun rejectProof(choreId: String, reason: String, day: ChoreScheduleDay?) = Unit

    override suspend fun acceptAvailabilitySuggestion(suggestion: AvailabilitySuggestion) = Unit
    override suspend fun dismissAvailabilitySuggestion(suggestion: AvailabilitySuggestion) = Unit
}

private fun testSuggestion(
    id: String,
    dateKey: String,
    weekdayLabel: String,
): AvailabilitySuggestion = AvailabilitySuggestion(
    id = id,
    choreId = "c1",
    choreTitle = "Trash",
    dateKey = dateKey,
    weekdayLabel = weekdayLabel,
    originalAssignee = "Erick",
    recommendedCoverMember = "Emma",
    reasonText = "Covering for Erick while they're away.",
    scopeStartDate = dateKey,
    scopeEndDate = dateKey,
    outsideNormalRotation = false,
)

private fun baseLifecycleChore(
    id: String,
    title: String,
    assignee: String,
    assigneeId: String = assignee.lowercase(),
    isActive: Boolean,
    status: ChoreItemStatus = ChoreItemStatus.DueSoon,
    pendingApproval: Boolean = false,
    submitterName: String = assignee,
    submitterUserId: String = assigneeId,
    proofPhotoCount: Int = 0,
    dueLabel: String = "Due later today",
): ChoreItem = ChoreItem(
    id = id,
    title = title,
    area = "Kitchen",
    cadence = "Weekly",
    assignee = assignee,
    assigneeId = assigneeId,
    status = status,
    durationLabel = "10 min",
    dueLabel = dueLabel,
    description = "",
    reminder = "",
    tags = listOf("Kitchen"),
    isActive = isActive,
    pendingApproval = pendingApproval,
    submitterName = submitterName,
    submitterUserId = submitterUserId,
    proofPhotoCount = proofPhotoCount,
)

private fun ChoreItem.toLifecycleDraft(): ChoreDraft = ChoreDraft(
    id = id,
    title = title,
    additionalComments = description,
    roomId = "room-kitchen",
    roomName = area,
    roomIconKey = "kitchen",
    roomColorKey = "teal",
    assignee = assignee,
    autoAssign = false,
    smartAssignmentMode = com.sortoapps.chorepilot.features.chores.domain.ChoreSmartAssignmentModeValue.FairRotation,
    assignmentPool = emptySet(),
    dayAssignments = emptyMap(),
    effortLevel = com.sortoapps.chorepilot.features.chores.domain.ChoreEffortValue.Standard,
    repeatsEnabled = true,
    repeatStartDate = "Today",
    repeatFrequency = com.sortoapps.chorepilot.features.chores.domain.ChoreRepeatFrequencyValue.Weekly,
    dailyRepeatMode = com.sortoapps.chorepilot.features.chores.domain.ChoreDailyModeValue.Weekdays,
    selectedWeekdays = setOf("Monday"),
    monthlyPattern = "",
    customPattern = "",
    selectedOneTimeDate = "Today",
    proofRequired = false,
    durationMinutes = 10,
    isActive = isActive,
    pendingApproval = pendingApproval,
    submitterUserId = submitterUserId,
    submitterName = submitterName,
    reviewRejectedReason = reviewRejectedReason,
    reviewedByName = reviewedByName,
    reviewedAtEpochMs = reviewedAtEpochMs,
)
