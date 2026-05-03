package com.sortoapps.chorepilot.features.chores.presentation.viewmodel

import com.sortoapps.chorepilot.core.platform.VibratorUtil
import com.sortoapps.chorepilot.core.revenuecat.SubscriptionPlanId
import com.sortoapps.chorepilot.core.revenuecat.SubscriptionRepository
import com.sortoapps.chorepilot.core.revenuecat.SubscriptionSnapshot
import com.sortoapps.chorepilot.features.auth.domain.AuthFlowResult
import com.sortoapps.chorepilot.features.auth.domain.HouseholdRole
import com.sortoapps.chorepilot.features.auth.domain.InvitePreview
import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import com.sortoapps.chorepilot.features.auth.domain.SessionSnapshot
import com.sortoapps.chorepilot.features.chores.domain.ChoreDraft
import com.sortoapps.chorepilot.features.chores.domain.ChoreItem
import com.sortoapps.chorepilot.features.chores.domain.ChoreRepository
import com.sortoapps.chorepilot.features.chores.domain.ChoreScheduleDay
import com.sortoapps.chorepilot.features.chores.domain.ChoreSmartAssignmentModeValue
import com.sortoapps.chorepilot.features.chores.domain.HouseholdSchedulingMember
import com.sortoapps.chorepilot.features.chores.domain.ProofEvidence
import com.sortoapps.chorepilot.features.chores.domain.ProofUploadPayload
import com.sortoapps.chorepilot.features.chores.domain.ProofUploadResult
import com.sortoapps.chorepilot.features.chores.domain.RoomDefinition
import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.ChoreLifecycleAction
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class ChoreEditorViewModelTest {
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
    fun proofRequiredUpgradeResumesAfterEntitlementActivation() = runTest(dispatcher) {
        val subscriptionRepository = FakeEditorSubscriptionRepository(hasPro = false)
        val viewModel = ChoreEditorViewModel(
            choreRepository = FakeEditorChoreRepository(),
            sessionRepository = FakeEditorSessionRepository(),
            subscriptionRepository = subscriptionRepository,
            vibratorUtil = FakeEditorVibratorUtil(),
        )

        advanceUntilIdle()
        viewModel.onEvent(ChoreEditorUiEvent.LoadChore(null))
        advanceUntilIdle()
        viewModel.onEvent(ChoreEditorUiEvent.ProofRequiredChanged(false))
        viewModel.onEvent(ChoreEditorUiEvent.ProofRequiredUpgradeRequested(true))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.pendingPremiumProofUnlock)
        assertFalse(viewModel.uiState.value.proofRequired)

        subscriptionRepository.emit(hasPro = true)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.pendingPremiumProofUnlock)
        assertTrue(viewModel.uiState.value.proofRequired)
    }

    @Test
    fun proofRequiredUpgradeResumesForNonPurchaserWhenHouseholdHasPro() = runTest(dispatcher) {
        val subscriptionRepository = FakeEditorSubscriptionRepository(
            hasPro = false,
            purchaserHasPro = false,
            householdHasPro = false,
        )
        val viewModel = ChoreEditorViewModel(
            choreRepository = FakeEditorChoreRepository(),
            sessionRepository = FakeEditorSessionRepository(),
            subscriptionRepository = subscriptionRepository,
            vibratorUtil = FakeEditorVibratorUtil(),
        )

        advanceUntilIdle()
        viewModel.onEvent(ChoreEditorUiEvent.LoadChore(null))
        viewModel.onEvent(ChoreEditorUiEvent.ProofRequiredUpgradeRequested(true))
        advanceUntilIdle()

        subscriptionRepository.emit(
            hasPro = true,
            purchaserHasPro = false,
            householdHasPro = true,
        )
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.proofRequired)
        assertFalse(viewModel.uiState.value.pendingPremiumProofUnlock)
    }

    @Test
    fun createModeUsesActiveHouseholdMembersForDefaults() = runTest(dispatcher) {
        val repository = FakeEditorChoreRepository(
            householdMembers = listOf(
                HouseholdSchedulingMember(userId = "ava", displayName = "Ava", active = true),
                HouseholdSchedulingMember(userId = "liam", displayName = "Liam", active = true),
                HouseholdSchedulingMember(userId = "milo", displayName = "Milo", active = false),
            ),
        )
        val viewModel = ChoreEditorViewModel(
            choreRepository = repository,
            sessionRepository = FakeEditorSessionRepository(),
            subscriptionRepository = FakeEditorSubscriptionRepository(hasPro = true),
            vibratorUtil = FakeEditorVibratorUtil(),
        )

        advanceUntilIdle()
        viewModel.onEvent(ChoreEditorUiEvent.LoadChore(null))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("Ava", "Liam"), state.assigneeOptions)
        assertEquals("Ava", state.assignee)
        assertEquals(setOf("Ava", "Liam"), state.assignmentPool)
        assertTrue(state.dayAssignments.isEmpty())
        assertTrue(state.inactiveAssignmentNames.isEmpty())
    }

    @Test
    fun editModePreservesLegacyAssigneesAlongsideActiveMembers() = runTest(dispatcher) {
        val repository = FakeEditorChoreRepository(
            householdMembers = listOf(
                HouseholdSchedulingMember(userId = "ava", displayName = "Ava", active = true),
            ),
            drafts = mapOf(
                "legacy" to ChoreDraft(
                    id = "legacy",
                    title = "Take out trash",
                    additionalComments = "",
                    roomId = "room-kitchen",
                    roomName = "Kitchen",
                    roomIconKey = "kitchen",
                    roomColorKey = "teal",
                    assignee = "Rosa",
                    autoAssign = true,
                    smartAssignmentMode = ChoreSmartAssignmentModeValue.DayPlan,
                    assignmentPool = setOf("Rosa"),
                    dayAssignments = mapOf("Monday" to "Rosa"),
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
                    durationMinutes = 15,
                ),
            ),
        )
        val viewModel = ChoreEditorViewModel(
            choreRepository = repository,
            sessionRepository = FakeEditorSessionRepository(),
            subscriptionRepository = FakeEditorSubscriptionRepository(hasPro = true),
            vibratorUtil = FakeEditorVibratorUtil(),
        )

        advanceUntilIdle()
        viewModel.onEvent(ChoreEditorUiEvent.LoadChore("legacy"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(setOf("Rosa"), state.inactiveAssignmentNames)
        assertEquals(listOf("Ava", "Rosa"), state.assigneeOptions)
        assertEquals(setOf("Rosa"), state.assignmentPool)
        assertEquals("Rosa", state.dayAssignments[ChoreWeekday.Monday])
    }

    @Test
    fun editModeKeepsInactiveAndPendingApprovalFlagsWhenSaving() = runTest(dispatcher) {
        val repository = FakeEditorChoreRepository(
            householdMembers = listOf(
                HouseholdSchedulingMember(userId = "ava", displayName = "Ava", active = true),
            ),
            drafts = mapOf(
                "inactive" to ChoreDraft(
                    id = "inactive",
                    title = "Pantry reset",
                    additionalComments = "",
                    roomId = "room-kitchen",
                    roomName = "Kitchen",
                    roomIconKey = "kitchen",
                    roomColorKey = "teal",
                    assignee = "Ava",
                    autoAssign = false,
                    smartAssignmentMode = ChoreSmartAssignmentModeValue.FairRotation,
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
                    durationMinutes = 15,
                    isActive = false,
                    pendingApproval = true,
                ),
            ),
        )
        val viewModel = ChoreEditorViewModel(
            choreRepository = repository,
            sessionRepository = FakeEditorSessionRepository(),
            subscriptionRepository = FakeEditorSubscriptionRepository(hasPro = true),
            vibratorUtil = FakeEditorVibratorUtil(),
        )

        advanceUntilIdle()
        viewModel.onEvent(ChoreEditorUiEvent.LoadChore("inactive"))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isActive)
        assertTrue(viewModel.uiState.value.pendingApproval)

        viewModel.onEvent(ChoreEditorUiEvent.TitleChanged("Pantry reset updated"))
        viewModel.save { }
        advanceUntilIdle()

        val saved = repository.savedDrafts.last()
        assertFalse(saved.isActive)
        assertTrue(saved.pendingApproval)
    }

    @Test
    fun deleteLifecycleRequiresTypedConfirmationAndSetsCloseFlag() = runTest(dispatcher) {
        val repository = FakeEditorChoreRepository(
            householdMembers = listOf(
                HouseholdSchedulingMember(userId = "ava", displayName = "Ava", active = true),
            ),
            drafts = mapOf(
                "cleanup" to baseEditorDraft(id = "cleanup"),
            ),
        )
        val viewModel = ChoreEditorViewModel(
            choreRepository = repository,
            sessionRepository = FakeEditorSessionRepository(),
            subscriptionRepository = FakeEditorSubscriptionRepository(hasPro = true),
            vibratorUtil = FakeEditorVibratorUtil(),
        )

        advanceUntilIdle()
        viewModel.onEvent(ChoreEditorUiEvent.LoadChore("cleanup"))
        advanceUntilIdle()
        viewModel.onEvent(ChoreEditorUiEvent.LifecycleActionRequested(ChoreLifecycleAction.Delete))

        assertFalse(viewModel.uiState.value.lifecycleConfirmation?.confirmEnabled ?: true)

        viewModel.onEvent(ChoreEditorUiEvent.LifecycleConfirmationTextChanged("delete"))
        assertTrue(viewModel.uiState.value.lifecycleConfirmation?.confirmEnabled == true)

        viewModel.onEvent(ChoreEditorUiEvent.ConfirmLifecycleAction)
        advanceUntilIdle()

        assertEquals("cleanup", repository.deletedChoreId)
        assertTrue(viewModel.uiState.value.shouldCloseAfterDelete)
    }

    @Test
    fun saveBlocksWhenNoAssignableMembersExistForNewChore() = runTest(dispatcher) {
        val repository = FakeEditorChoreRepository()
        val viewModel = ChoreEditorViewModel(
            choreRepository = repository,
            sessionRepository = FakeEditorSessionRepository(),
            subscriptionRepository = FakeEditorSubscriptionRepository(hasPro = true),
            vibratorUtil = FakeEditorVibratorUtil(),
        )

        advanceUntilIdle()
        viewModel.onEvent(ChoreEditorUiEvent.LoadChore(null))
        viewModel.onEvent(ChoreEditorUiEvent.TitleChanged("Wipe counters"))
        advanceUntilIdle()

        var savedId: String? = null
        viewModel.save { savedId = it }
        advanceUntilIdle()

        assertNull(savedId)
        assertEquals(0, repository.savedDrafts.size)
        assertEquals(
            "Add an active household member before assigning this chore.",
            viewModel.uiState.value.errorMessage,
        )
    }

    @Test
    fun togglingAutoAssignOffFallsBackToRealMember() = runTest(dispatcher) {
        val repository = FakeEditorChoreRepository(
            householdMembers = listOf(
                HouseholdSchedulingMember(userId = "zoe", displayName = "Zoe", active = true),
                HouseholdSchedulingMember(userId = "ava", displayName = "Ava", active = true),
            ),
        )
        val viewModel = ChoreEditorViewModel(
            choreRepository = repository,
            sessionRepository = FakeEditorSessionRepository(),
            subscriptionRepository = FakeEditorSubscriptionRepository(hasPro = true),
            vibratorUtil = FakeEditorVibratorUtil(),
        )

        advanceUntilIdle()
        viewModel.onEvent(ChoreEditorUiEvent.LoadChore(null))
        viewModel.onEvent(ChoreEditorUiEvent.AutoAssignChanged(true))
        viewModel.onEvent(ChoreEditorUiEvent.AssigneeChanged(""))
        viewModel.onEvent(ChoreEditorUiEvent.AutoAssignChanged(false))
        advanceUntilIdle()

        assertEquals("Ava", viewModel.uiState.value.assignee)
    }
}

private class FakeEditorSubscriptionRepository(
    hasPro: Boolean,
    purchaserHasPro: Boolean = hasPro,
    householdHasPro: Boolean = hasPro,
) : SubscriptionRepository {
    private val snapshotFlow = MutableStateFlow(
        SubscriptionSnapshot(
            purchaserHasChorePilotPro = purchaserHasPro,
            householdHasChorePilotPro = householdHasPro,
            hasChorePilotPro = hasPro,
            isConfigured = true,
            isLoading = false,
        ),
    )

    override val snapshot: StateFlow<SubscriptionSnapshot> = snapshotFlow

    fun emit(
        hasPro: Boolean,
        purchaserHasPro: Boolean = hasPro,
        householdHasPro: Boolean = hasPro,
    ) {
        snapshotFlow.value = snapshotFlow.value.copy(
            purchaserHasChorePilotPro = purchaserHasPro,
            householdHasChorePilotPro = householdHasPro,
            hasChorePilotPro = hasPro,
        )
    }

    override suspend fun refresh() = Unit
    override suspend fun purchasePlan(planId: SubscriptionPlanId): Result<Unit> = Result.success(Unit)
    override suspend fun restorePurchases(): Result<Unit> = Result.success(Unit)
    override fun clearError() = Unit
}

private class FakeEditorSessionRepository : SessionRepository {
    override val session: StateFlow<SessionSnapshot> = MutableStateFlow(
        SessionSnapshot(
            signedIn = true,
            userId = "erick",
            displayName = "Erick",
            householdId = "house-1",
            householdName = "River House",
            role = HouseholdRole.Owner,
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

private class FakeEditorChoreRepository(
    householdMembers: List<HouseholdSchedulingMember> = emptyList(),
    drafts: Map<String, ChoreDraft> = emptyMap(),
) : ChoreRepository {
    override val chores: StateFlow<List<ChoreItem>> = MutableStateFlow(emptyList())
    override val rooms: StateFlow<List<RoomDefinition>> = MutableStateFlow(emptyList())
    override val householdMembers: StateFlow<List<HouseholdSchedulingMember>> = MutableStateFlow(householdMembers)
    private val draftsById = drafts.toMutableMap()
    val savedDrafts = mutableListOf<ChoreDraft>()
    var deletedChoreId: String? = null

    override fun getChores(): List<ChoreItem> = emptyList()
    override fun getChore(id: String, day: ChoreScheduleDay?): ChoreItem? = null
    override fun getLatestProofForChore(choreId: String, day: ChoreScheduleDay?): ProofEvidence? = null
    override fun getDraft(id: String): ChoreDraft? = draftsById[id]
    override fun getScheduledChores(day: ChoreScheduleDay): List<ChoreItem> = emptyList()
    override suspend fun refresh() = Unit
    override suspend fun saveChore(draft: ChoreDraft): String {
        val id = draft.id ?: "draft"
        val saved = draft.copy(id = id)
        savedDrafts += saved
        draftsById[id] = saved
        return id
    }
    override suspend fun deleteChore(choreId: String) {
        deletedChoreId = choreId
        draftsById.remove(choreId)
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
    ): ProofUploadResult = ProofUploadResult(
        proofId = "proof",
        storagePath = "",
        downloadUrl = "",
    )

    override suspend fun approveProof(choreId: String, day: ChoreScheduleDay?) = Unit

    override suspend fun rejectProof(choreId: String, reason: String, day: ChoreScheduleDay?) = Unit

    override suspend fun getAvailabilitySuggestions(days: List<ChoreScheduleDay>) = emptyList<com.sortoapps.chorepilot.features.chores.domain.AvailabilitySuggestion>()

    override suspend fun acceptAvailabilitySuggestion(suggestion: com.sortoapps.chorepilot.features.chores.domain.AvailabilitySuggestion) = Unit

    override suspend fun dismissAvailabilitySuggestion(suggestion: com.sortoapps.chorepilot.features.chores.domain.AvailabilitySuggestion) = Unit
}

private class FakeEditorVibratorUtil : VibratorUtil {
    override fun triggerVibration() = Unit
    override fun triggerVibrationSuccess() = Unit
    override fun cancelVibration() = Unit
}

private fun baseEditorDraft(
    id: String,
): ChoreDraft = ChoreDraft(
    id = id,
    title = "Wipe counters",
    additionalComments = "",
    roomId = "room-kitchen",
    roomName = "Kitchen",
    roomIconKey = "kitchen",
    roomColorKey = "teal",
    assignee = "Ava",
    autoAssign = false,
    smartAssignmentMode = ChoreSmartAssignmentModeValue.FairRotation,
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
    durationMinutes = 15,
)
