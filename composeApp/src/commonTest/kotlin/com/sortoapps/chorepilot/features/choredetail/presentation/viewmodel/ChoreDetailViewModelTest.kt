package com.sortoapps.chorepilot.features.choredetail.presentation.viewmodel

import com.sortoapps.chorepilot.core.platform.VibratorUtil
import com.sortoapps.chorepilot.features.auth.domain.AuthFlowResult
import com.sortoapps.chorepilot.features.auth.domain.HouseholdRole
import com.sortoapps.chorepilot.features.auth.domain.InvitePreview
import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import com.sortoapps.chorepilot.features.auth.domain.SessionSnapshot
import com.sortoapps.chorepilot.features.chores.domain.ChoreDraft
import com.sortoapps.chorepilot.features.chores.domain.ChoreItem
import com.sortoapps.chorepilot.features.chores.domain.ChoreItemStatus
import com.sortoapps.chorepilot.features.chores.domain.ApproveProofUseCase
import com.sortoapps.chorepilot.features.chores.domain.ChoreRepository
import com.sortoapps.chorepilot.features.chores.domain.ChoreScheduleDay
import com.sortoapps.chorepilot.features.chores.domain.ProofEvidence
import com.sortoapps.chorepilot.features.chores.domain.ProofReviewStatus
import com.sortoapps.chorepilot.features.chores.domain.ProofUploadPayload
import com.sortoapps.chorepilot.features.chores.domain.ProofUploadResult
import com.sortoapps.chorepilot.features.chores.domain.RoomDefinition
import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.ChoreLifecycleAction
import com.sortoapps.chorepilot.features.notifications.domain.HouseholdNotifications
import com.sortoapps.chorepilot.features.notifications.domain.ProofApprovedNotification
import com.sortoapps.chorepilot.features.notifications.domain.ProofSubmittedNotification
import com.sortoapps.chorepilot.features.rewards.domain.RewardDefinition
import com.sortoapps.chorepilot.features.rewards.domain.RewardDefinitionDraft
import com.sortoapps.chorepilot.features.rewards.domain.RewardPointEvent
import com.sortoapps.chorepilot.features.rewards.domain.RewardRedemption
import com.sortoapps.chorepilot.features.rewards.domain.RewardsCelebration
import com.sortoapps.chorepilot.features.rewards.domain.RewardsFamilyRules
import com.sortoapps.chorepilot.features.rewards.domain.RewardsMemberProgress
import com.sortoapps.chorepilot.features.rewards.domain.RewardsRepository
import com.sortoapps.chorepilot.features.rewards.domain.RewardsRepositoryState
import com.sortoapps.chorepilot.features.rewards.domain.RewardsSnapshot
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
class ChoreDetailViewModelTest {
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
    fun markCompletePersistsAndReloadsTodayState() = runTest(dispatcher) {
        val repository = FakeDetailChoreRepository(
            chore = baseChore(),
        )
        val viewModel = ChoreDetailViewModel(
            choreRepository = repository,
            sessionRepository = FakeDetailSessionRepository(),
            approveProofUseCase = ApproveProofUseCase(
                repository,
                FakeDetailSessionRepository(),
                FakeDetailRewardsRepository(),
                FakeHouseholdNotifications(),
            ),
            rewardsRepository = FakeDetailRewardsRepository(),
            vibratorUtil = FakeDetailVibratorUtil(),
        )

        viewModel.onEvent(ChoreDetailUiEvent.LoadChore("chore-1"))
        viewModel.onEvent(ChoreDetailUiEvent.MarkComplete)
        advanceUntilIdle()

        assertEquals(1, repository.toggleCalls)
        assertEquals(ChoreItemStatus.Completed, viewModel.uiState.value.status)
        assertEquals(ChoreDetailSurface.Overview, viewModel.uiState.value.surface)
        assertEquals("Completed", viewModel.uiState.value.dueLabel)
        assertEquals("Marked complete for today.", viewModel.uiState.value.feedbackMessage)
    }

    @Test
    fun completingForAnotherMemberRequiresConfirmationAndReloadsCompletedByName() = runTest(dispatcher) {
        val repository = FakeDetailChoreRepository(
            chore = baseChore(assignee = "Ava"),
        )
        val sessionRepository = FakeDetailSessionRepository()
        val viewModel = ChoreDetailViewModel(
            choreRepository = repository,
            sessionRepository = sessionRepository,
            approveProofUseCase = ApproveProofUseCase(
                repository,
                sessionRepository,
                FakeDetailRewardsRepository(),
                FakeHouseholdNotifications(),
            ),
            rewardsRepository = FakeDetailRewardsRepository(),
            vibratorUtil = FakeDetailVibratorUtil(),
        )

        viewModel.onEvent(ChoreDetailUiEvent.LoadChore("chore-1"))
        viewModel.onEvent(ChoreDetailUiEvent.MarkComplete)

        val prompt = viewModel.uiState.value.completionPrompt
        assertNotNull(prompt)
        assertEquals("Ava", prompt.assigneeName)
        assertEquals(0, repository.toggleCalls)

        viewModel.onEvent(ChoreDetailUiEvent.ConfirmCompletionForAnotherMember)
        advanceUntilIdle()

        assertEquals(1, repository.toggleCalls)
        assertNull(viewModel.uiState.value.completionPrompt)
        assertEquals("Erick", viewModel.uiState.value.completedByName)
        assertEquals("Completed for Ava.", viewModel.uiState.value.feedbackMessage)
    }

    @Test
    fun approveProofTransitionsToCompletedAndEmitsNotification() = runTest(dispatcher) {
        val repository = FakeDetailChoreRepository(
            chore = baseChore(
                status = ChoreItemStatus.PendingProof,
                dueLabel = "1 photo uploaded",
                proofPhotoCount = 1,
                proofRequired = true,
            ),
            proof = ProofEvidence(
                proofId = "proof-1",
                choreId = "chore-1",
                dateKey = "2026-04-08",
                uploadedByUserId = "ava-user",
                uploadedByName = "Ava",
                storagePath = "proofs/proof-1.jpg",
                downloadUrl = "https://example.com/proof.jpg",
                createdAtEpochMs = 123L,
                reviewStatus = ProofReviewStatus.Pending,
            ),
        )
        val sessionRepository = FakeDetailSessionRepository()
        val notifications = FakeHouseholdNotifications()
        val viewModel = ChoreDetailViewModel(
            choreRepository = repository,
            sessionRepository = sessionRepository,
            approveProofUseCase = ApproveProofUseCase(
                repository,
                sessionRepository,
                FakeDetailRewardsRepository(),
                notifications,
            ),
            rewardsRepository = FakeDetailRewardsRepository(),
            vibratorUtil = FakeDetailVibratorUtil(),
        )

        viewModel.onEvent(ChoreDetailUiEvent.LoadChore("chore-1"))
        viewModel.onEvent(ChoreDetailUiEvent.ApproveProof)
        advanceUntilIdle()

        assertEquals(1, repository.approveCalls)
        assertEquals(ChoreItemStatus.Completed, viewModel.uiState.value.status)
        assertEquals("Approved", viewModel.uiState.value.dueLabel)
        assertEquals("Ava's proof was approved and the chore is now complete.", viewModel.uiState.value.feedbackMessage)
        assertEquals(1, notifications.approved.size)
        assertEquals("Ava", notifications.approved.single().submittingMemberName)
        assertEquals("Erick", notifications.approved.single().reviewerName)
        assertEquals("2026-04-08", notifications.approved.single().dateKey)
        assertEquals(6, notifications.approved.single().awardedPoints)
    }

    @Test
    fun rejectProofReturnsChoreToResubmitState() = runTest(dispatcher) {
        val repository = FakeDetailChoreRepository(
            chore = baseChore(
                assignee = "Ava",
                status = ChoreItemStatus.PendingProof,
                dueLabel = "1 photo uploaded",
                proofPhotoCount = 1,
                proofRequired = true,
            ),
            proof = ProofEvidence(
                proofId = "proof-1",
                choreId = "chore-1",
                dateKey = "2026-04-08",
                uploadedByUserId = "ava-user",
                uploadedByName = "Ava",
                storagePath = "proofs/proof-1.jpg",
                downloadUrl = "https://example.com/proof.jpg",
                createdAtEpochMs = 123L,
                reviewStatus = ProofReviewStatus.Pending,
            ),
        )
        val sessionRepository = FakeDetailSessionRepository()
        val viewModel = ChoreDetailViewModel(
            choreRepository = repository,
            sessionRepository = sessionRepository,
            approveProofUseCase = ApproveProofUseCase(
                repository,
                sessionRepository,
                FakeDetailRewardsRepository(),
                FakeHouseholdNotifications(),
            ),
            rewardsRepository = FakeDetailRewardsRepository(),
            vibratorUtil = FakeDetailVibratorUtil(),
        )

        viewModel.onEvent(ChoreDetailUiEvent.LoadChore("chore-1"))
        viewModel.onEvent(ChoreDetailUiEvent.RejectProof)
        viewModel.onEvent(ChoreDetailUiEvent.ProofRejectionReasonChanged("Too dark"))
        viewModel.onEvent(ChoreDetailUiEvent.ConfirmRejectProof)
        advanceUntilIdle()

        assertEquals(1, repository.rejectCalls)
        assertEquals(ChoreItemStatus.DueSoon, viewModel.uiState.value.status)
        assertEquals(ProofReviewStatus.Rejected, viewModel.uiState.value.proofReviewStatus)
        assertEquals("Too dark", viewModel.uiState.value.proofRejectedReason)
        assertEquals("Needs new proof", viewModel.uiState.value.dueLabel)
    }

    @Test
    fun memberCannotApproveOrRejectProofFromDetailEvents() = runTest(dispatcher) {
        val repository = FakeDetailChoreRepository(
            chore = baseChore(
                assignee = "Ava",
                status = ChoreItemStatus.PendingProof,
                dueLabel = "1 photo uploaded",
                proofPhotoCount = 1,
                proofRequired = true,
            ),
            proof = ProofEvidence(
                proofId = "proof-1",
                choreId = "chore-1",
                dateKey = "2026-04-08",
                uploadedByUserId = "ava-user",
                uploadedByName = "Ava",
                storagePath = "proofs/proof-1.jpg",
                downloadUrl = "https://example.com/proof.jpg",
                createdAtEpochMs = 123L,
                reviewStatus = ProofReviewStatus.Pending,
            ),
        )
        val sessionRepository = FakeDetailSessionRepository(role = HouseholdRole.Member)
        val viewModel = ChoreDetailViewModel(
            choreRepository = repository,
            sessionRepository = sessionRepository,
            approveProofUseCase = ApproveProofUseCase(
                repository,
                sessionRepository,
                FakeDetailRewardsRepository(),
                FakeHouseholdNotifications(),
            ),
            rewardsRepository = FakeDetailRewardsRepository(),
            vibratorUtil = FakeDetailVibratorUtil(),
        )

        viewModel.onEvent(ChoreDetailUiEvent.LoadChore("chore-1"))
        viewModel.onEvent(ChoreDetailUiEvent.ApproveProof)
        viewModel.onEvent(ChoreDetailUiEvent.RejectProof)
        viewModel.onEvent(ChoreDetailUiEvent.ProofRejectionReasonChanged("Too dark"))
        viewModel.onEvent(ChoreDetailUiEvent.ConfirmRejectProof)
        advanceUntilIdle()

        assertEquals(0, repository.approveCalls)
        assertEquals(0, repository.rejectCalls)
        assertEquals(ChoreItemStatus.PendingProof, viewModel.uiState.value.status)
        assertEquals(ProofReviewStatus.Pending, viewModel.uiState.value.proofReviewStatus)
        assertTrue(!viewModel.uiState.value.showRejectProofDialog)
    }

    @Test
    fun memberCannotApproveOrRejectPendingDiscoveryFromDetailEvents() = runTest(dispatcher) {
        val repository = FakeDetailChoreRepository(
            chore = baseChore(
                pendingApproval = true,
                isActive = false,
                submitterName = "Ava",
                submitterUserId = "ava-user",
            ),
        )
        val sessionRepository = FakeDetailSessionRepository(role = HouseholdRole.Member)
        val viewModel = ChoreDetailViewModel(
            choreRepository = repository,
            sessionRepository = sessionRepository,
            approveProofUseCase = ApproveProofUseCase(
                repository,
                sessionRepository,
                FakeDetailRewardsRepository(),
                FakeHouseholdNotifications(),
            ),
            rewardsRepository = FakeDetailRewardsRepository(),
            vibratorUtil = FakeDetailVibratorUtil(),
        )

        viewModel.onEvent(ChoreDetailUiEvent.LoadChore("chore-1"))
        viewModel.onEvent(ChoreDetailUiEvent.ApprovePendingChore)
        viewModel.onEvent(ChoreDetailUiEvent.RejectPendingChore)
        viewModel.onEvent(ChoreDetailUiEvent.PendingRejectionReasonChanged("Needs a clearer title"))
        viewModel.onEvent(ChoreDetailUiEvent.ConfirmRejectPendingChore)
        advanceUntilIdle()

        assertEquals(0, repository.approvePendingCalls)
        assertEquals(null, repository.rejectedPendingReason)
        assertTrue(viewModel.uiState.value.pendingApproval)
        assertTrue(!viewModel.uiState.value.isActive)
        assertTrue(!viewModel.uiState.value.showRejectPendingChoreDialog)
    }

    @Test
    fun reactivateLifecycleReloadsInactiveChore() = runTest(dispatcher) {
        val repository = FakeDetailChoreRepository(
            chore = baseChore(isActive = false),
        )
        val sessionRepository = FakeDetailSessionRepository()
        val viewModel = ChoreDetailViewModel(
            choreRepository = repository,
            sessionRepository = sessionRepository,
            approveProofUseCase = ApproveProofUseCase(
                repository,
                sessionRepository,
                FakeDetailRewardsRepository(),
                FakeHouseholdNotifications(),
            ),
            rewardsRepository = FakeDetailRewardsRepository(),
            vibratorUtil = FakeDetailVibratorUtil(),
        )

        viewModel.onEvent(ChoreDetailUiEvent.LoadChore("chore-1"))
        viewModel.onEvent(ChoreDetailUiEvent.LifecycleActionRequested(ChoreLifecycleAction.Reactivate))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isActive)
        assertEquals("chore-1", repository.savedDrafts.last().id)
        assertTrue(repository.savedDrafts.last().isActive)
    }

    @Test
    fun loadingUnavailableChoreClearsStaleDetailState() = runTest(dispatcher) {
        val repository = FakeDetailChoreRepository(
            chore = baseChore(),
        )
        val sessionRepository = FakeDetailSessionRepository()
        val viewModel = ChoreDetailViewModel(
            choreRepository = repository,
            sessionRepository = sessionRepository,
            approveProofUseCase = ApproveProofUseCase(
                repository,
                sessionRepository,
                FakeDetailRewardsRepository(),
                FakeHouseholdNotifications(),
            ),
            rewardsRepository = FakeDetailRewardsRepository(),
            vibratorUtil = FakeDetailVibratorUtil(),
        )

        viewModel.onEvent(ChoreDetailUiEvent.LoadChore("chore-1"))
        advanceUntilIdle()
        assertEquals("Wipe the counters", viewModel.uiState.value.title)

        repository.deleteChore("chore-1")
        viewModel.onEvent(ChoreDetailUiEvent.LoadChore("missing-chore"))
        advanceUntilIdle()

        assertEquals("missing-chore", viewModel.uiState.value.choreId)
        assertEquals("", viewModel.uiState.value.title)
        assertEquals("This chore isn't available anymore. It may have been deleted or moved.", viewModel.uiState.value.unavailableMessage)
        assertTrue(!viewModel.uiState.value.isLoading)
    }

    @Test
    fun deleteLifecycleRequiresTypedConfirmationAndClosesDetail() = runTest(dispatcher) {
        val repository = FakeDetailChoreRepository(
            chore = baseChore(isActive = false),
        )
        val sessionRepository = FakeDetailSessionRepository()
        val viewModel = ChoreDetailViewModel(
            choreRepository = repository,
            sessionRepository = sessionRepository,
            approveProofUseCase = ApproveProofUseCase(
                repository,
                sessionRepository,
                FakeDetailRewardsRepository(),
                FakeHouseholdNotifications(),
            ),
            rewardsRepository = FakeDetailRewardsRepository(),
            vibratorUtil = FakeDetailVibratorUtil(),
        )

        viewModel.onEvent(ChoreDetailUiEvent.LoadChore("chore-1"))
        viewModel.onEvent(ChoreDetailUiEvent.LifecycleActionRequested(ChoreLifecycleAction.Delete))

        assertTrue(viewModel.uiState.value.lifecycleConfirmation != null)
        assertTrue(viewModel.uiState.value.lifecycleConfirmation?.confirmEnabled == false)

        viewModel.onEvent(ChoreDetailUiEvent.LifecycleConfirmationTextChanged("delete"))
        assertTrue(viewModel.uiState.value.lifecycleConfirmation?.confirmEnabled == true)

        viewModel.onEvent(ChoreDetailUiEvent.ConfirmLifecycleAction)
        advanceUntilIdle()

        assertEquals("chore-1", repository.deletedChoreId)
        assertTrue(viewModel.uiState.value.shouldCloseAfterDelete)
    }

    @Test
    fun rejectingPendingDiscoveryPersistsReasonAndReloadsOutcomeState() = runTest(dispatcher) {
        val repository = FakeDetailChoreRepository(
            chore = baseChore(
                assignee = "Mila",
                isActive = false,
                status = ChoreItemStatus.DueSoon,
                dueLabel = "Waiting for review",
                pendingApproval = true,
                submitterName = "Mila",
                submitterUserId = "member-1",
            ),
        )
        val sessionRepository = FakeDetailSessionRepository()
        val viewModel = ChoreDetailViewModel(
            choreRepository = repository,
            sessionRepository = sessionRepository,
            approveProofUseCase = ApproveProofUseCase(
                repository,
                sessionRepository,
                FakeDetailRewardsRepository(),
                FakeHouseholdNotifications(),
            ),
            rewardsRepository = FakeDetailRewardsRepository(),
            vibratorUtil = FakeDetailVibratorUtil(),
        )

        viewModel.onEvent(ChoreDetailUiEvent.LoadChore("chore-1"))
        viewModel.onEvent(ChoreDetailUiEvent.RejectPendingChore)
        viewModel.onEvent(ChoreDetailUiEvent.PendingRejectionReasonChanged("Needs a clearer title"))
        viewModel.onEvent(ChoreDetailUiEvent.ConfirmRejectPendingChore)
        advanceUntilIdle()

        assertEquals("Needs a clearer title", repository.rejectedPendingReason)
        assertEquals("Needs a clearer title", viewModel.uiState.value.reviewRejectedReason)
        assertEquals("Needs review updates", viewModel.uiState.value.dueLabel)
        assertEquals("Erick", viewModel.uiState.value.reviewedByName)
        assertEquals("Mila", viewModel.uiState.value.submitterName)
        assertTrue(!viewModel.uiState.value.pendingApproval)
    }

    private fun baseChore(
        assignee: String = "Erick",
        status: ChoreItemStatus = ChoreItemStatus.DueSoon,
        dueLabel: String = "Due later today",
        proofPhotoCount: Int = 0,
        proofRequired: Boolean = false,
        isActive: Boolean = true,
        pendingApproval: Boolean = false,
        submitterName: String = assignee,
        submitterUserId: String = assignee.lowercase(),
    ): ChoreItem = ChoreItem(
        id = "chore-1",
        title = "Wipe the counters",
        area = "Kitchen",
        cadence = "Today",
        assignee = assignee,
        assigneeId = assignee.lowercase(),
        status = status,
        durationLabel = "10 min",
        dueLabel = dueLabel,
        description = "Clear the kitchen counters.",
        reminder = "",
        tags = listOf("Kitchen"),
        isActive = isActive,
        pendingApproval = pendingApproval,
        proofRequired = proofRequired,
        proofPhotoCount = proofPhotoCount,
        submitterName = submitterName,
        submitterUserId = submitterUserId,
    )
}

private class FakeDetailChoreRepository(
    chore: ChoreItem,
    proof: ProofEvidence? = null,
) : ChoreRepository {
    private val choreState = MutableStateFlow(listOf(chore))
    private val roomState = MutableStateFlow(emptyList<RoomDefinition>())
    private var draftState = chore.toDraft()
    private var latestProof: ProofEvidence? = proof

    var toggleCalls: Int = 0
    var approveCalls: Int = 0
    var rejectCalls: Int = 0
    var approvePendingCalls: Int = 0
    var rejectedPendingReason: String? = null
    var deletedChoreId: String? = null
    val savedDrafts = mutableListOf<ChoreDraft>()

    override val chores: StateFlow<List<ChoreItem>> = choreState
    override val rooms: StateFlow<List<RoomDefinition>> = roomState
    override val householdMembers: StateFlow<List<com.sortoapps.chorepilot.features.chores.domain.HouseholdSchedulingMember>> = MutableStateFlow(emptyList())

    override fun getChores(): List<ChoreItem> = choreState.value

    override fun getChore(id: String, day: ChoreScheduleDay?): ChoreItem? = choreState.value.firstOrNull { it.id == id }

    override fun getLatestProofForChore(choreId: String, day: ChoreScheduleDay?): ProofEvidence? =
        latestProof?.takeIf { it.choreId == choreId }

    override fun getDraft(id: String): ChoreDraft? = draftState.takeIf { it.id == id }

    override fun getScheduledChores(day: ChoreScheduleDay): List<ChoreItem> = choreState.value

    override suspend fun refresh() = Unit

    override suspend fun saveChore(draft: ChoreDraft): String {
        val id = draft.id ?: "draft-1"
        val saved = draft.copy(id = id)
        savedDrafts += saved
        draftState = saved
        choreState.value = choreState.value.map { chore ->
            if (chore.id != id) chore else chore.copy(
                title = saved.title,
                area = saved.roomName,
                assignee = saved.assignee,
                assigneeId = saved.assignee.lowercase(),
                description = saved.additionalComments,
                proofRequired = saved.proofRequired,
                isActive = saved.isActive,
                pendingApproval = saved.pendingApproval,
            )
        }
        return id
    }

    override suspend fun deleteChore(choreId: String) {
        deletedChoreId = choreId
        choreState.value = choreState.value.filterNot { it.id == choreId }
    }

    override suspend fun toggleCompletion(
        choreId: String,
        viewerName: String,
        allowCompletingForSomeoneElse: Boolean,
        day: ChoreScheduleDay?,
    ) {
        val existing = getChore(choreId) ?: return
        if (existing.assignee != viewerName && !allowCompletingForSomeoneElse) return
        toggleCalls += 1
        val completedByName = if (existing.assignee.equals(viewerName, ignoreCase = true)) "" else viewerName
        choreState.value = choreState.value.map { chore ->
            if (chore.id != choreId) chore else chore.copy(
                status = ChoreItemStatus.Completed,
                dueLabel = "Completed",
                completedByName = completedByName,
            )
        }
    }

    override suspend fun submitProof(
        choreId: String,
        payload: ProofUploadPayload,
        day: ChoreScheduleDay?,
    ): ProofUploadResult = ProofUploadResult(
        proofId = "proof-1",
        storagePath = "proofs/proof-1.jpg",
        downloadUrl = "https://example.com/proof.jpg",
    )

    override suspend fun approveProof(choreId: String, day: ChoreScheduleDay?) {
        approveCalls += 1
        latestProof = latestProof?.copy(
            reviewStatus = ProofReviewStatus.Approved,
            reviewedByName = "Erick",
            reviewedAtEpochMs = 456L,
            rejectionReason = "",
        )
        choreState.value = choreState.value.map { chore ->
            if (chore.id != choreId) chore else chore.copy(
                status = ChoreItemStatus.Completed,
                dueLabel = "Approved",
            )
        }
    }

    override suspend fun rejectProof(choreId: String, reason: String, day: ChoreScheduleDay?) {
        rejectCalls += 1
        latestProof = latestProof?.copy(
            reviewStatus = ProofReviewStatus.Rejected,
            reviewedByName = "Erick",
            reviewedAtEpochMs = 789L,
            rejectionReason = reason,
        )
        choreState.value = choreState.value.map { chore ->
            if (chore.id != choreId) chore else chore.copy(
                status = ChoreItemStatus.DueSoon,
                dueLabel = "Needs new proof",
            )
        }
    }

    override suspend fun approvePendingChore(choreId: String) {
        approvePendingCalls += 1
        choreState.value = choreState.value.map { chore ->
            if (chore.id != choreId) chore else chore.copy(
                isActive = true,
                pendingApproval = false,
                reviewRejectedReason = "",
                reviewedByName = "Erick",
                reviewedAtEpochMs = 321L,
            )
        }
    }

    override suspend fun rejectPendingChore(choreId: String, reason: String) {
        rejectedPendingReason = reason
        choreState.value = choreState.value.map { chore ->
            if (chore.id != choreId) chore else chore.copy(
                isActive = false,
                pendingApproval = false,
                reviewRejectedReason = reason,
                reviewedByName = "Erick",
                reviewedAtEpochMs = 321L,
                dueLabel = "Needs review updates",
            )
        }
    }

    override suspend fun getAvailabilitySuggestions(days: List<ChoreScheduleDay>) = emptyList<com.sortoapps.chorepilot.features.chores.domain.AvailabilitySuggestion>()

    override suspend fun acceptAvailabilitySuggestion(suggestion: com.sortoapps.chorepilot.features.chores.domain.AvailabilitySuggestion) = Unit

    override suspend fun dismissAvailabilitySuggestion(suggestion: com.sortoapps.chorepilot.features.chores.domain.AvailabilitySuggestion) = Unit
}

private class FakeDetailSessionRepository(
    private val role: HouseholdRole = HouseholdRole.Owner,
) : SessionRepository {
    override val session: StateFlow<SessionSnapshot> = MutableStateFlow(
        SessionSnapshot(
            signedIn = true,
            userId = "erick",
            displayName = "Erick",
            householdId = "house-1",
            householdName = "River House",
            role = role,
        ),
    )

    override suspend fun refreshSession() = Unit

    override suspend fun completeOnboarding(displayName: String) = Unit

    override suspend fun signIn(email: String, password: String, displayNameHint: String): AuthFlowResult = AuthFlowResult.Main

    override suspend fun signInWithGoogle(
        idToken: String,
        accessToken: String?,
        displayNameHint: String,
        emailHint: String,
    ): AuthFlowResult = AuthFlowResult.Main

    override suspend fun createHousehold(
        displayName: String,
        householdName: String,
        householdSizeLabel: String,
    ): AuthFlowResult = AuthFlowResult.Main

    override suspend fun joinHousehold(displayName: String, inviteCode: String): AuthFlowResult = AuthFlowResult.Main

    override suspend fun previewInvite(inviteCode: String): InvitePreview? = null

    override suspend fun dismissInitialPremiumFlow() = Unit

    override suspend fun switchActiveHousehold(householdId: String) = Unit

    override suspend fun signOut() = Unit

    override suspend fun resetDemo() = Unit
}

private class FakeDetailVibratorUtil : VibratorUtil {
    override fun triggerVibration() = Unit

    override fun triggerVibrationSuccess() = Unit

    override fun cancelVibration() = Unit
}

private class FakeDetailRewardsRepository : RewardsRepository {
    override val state: StateFlow<RewardsRepositoryState> = MutableStateFlow(
        RewardsRepositoryState(
            isLoading = false,
            snapshot = RewardsSnapshot(
                householdName = "River House",
                viewerId = "erick",
                viewerName = "Erick",
                rules = RewardsFamilyRules(),
                availableRewards = listOf(
                    RewardDefinition(
                        id = "reward-1",
                        title = "Movie picker",
                        description = "Pick movie night.",
                        costPoints = 30,
                    ),
                ),
                memberProgress = listOf(
                    RewardsMemberProgress(
                        memberId = "erick",
                        memberName = "Erick",
                        totalEarnedPoints = 24,
                        availablePoints = 24,
                        currentStreak = 2,
                        completedChores = 2,
                        pendingProofCount = 0,
                        overdueCount = 0,
                        nextRewardGap = 6,
                    ),
                ),
                redemptionHistory = emptyList<RewardRedemption>(),
                recentPointEvents = emptyList<RewardPointEvent>(),
                celebration = RewardsCelebration(
                    title = "Nice work",
                    body = "Points are rolling in.",
                ),
            ),
        ),
    )

    override suspend fun refresh() = Unit

    override suspend fun updateRules(rules: RewardsFamilyRules) = Unit

    override suspend fun saveRewardDefinition(draft: RewardDefinitionDraft) = Unit

    override suspend fun deleteRewardDefinition(rewardId: String) = Unit

    override suspend fun redeemReward(rewardId: String) = Unit

    override suspend fun dismissCelebration() = Unit
}

private class FakeHouseholdNotifications : HouseholdNotifications {
    val submitted = mutableListOf<ProofSubmittedNotification>()
    val approved = mutableListOf<ProofApprovedNotification>()

    override suspend fun onProofSubmitted(notification: ProofSubmittedNotification) {
        submitted += notification
    }

    override suspend fun onProofApproved(notification: ProofApprovedNotification) {
        approved += notification
    }
}

private fun ChoreItem.toDraft(): ChoreDraft = ChoreDraft(
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
    proofRequired = proofRequired,
    durationMinutes = durationLabel.filter(Char::isDigit).toIntOrNull(),
    isActive = isActive,
    pendingApproval = pendingApproval,
)
