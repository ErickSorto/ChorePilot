package com.sortoapps.chorepilot.features.chorecapture.presentation.viewmodel

import com.sortoapps.chorepilot.core.platform.VibratorUtil
import com.sortoapps.chorepilot.core.revenuecat.SubscriptionPlanId
import com.sortoapps.chorepilot.core.revenuecat.SubscriptionRepository
import com.sortoapps.chorepilot.core.revenuecat.SubscriptionSnapshot
import com.sortoapps.chorepilot.features.auth.domain.AuthFlowResult
import com.sortoapps.chorepilot.features.auth.domain.HouseholdRole
import com.sortoapps.chorepilot.features.auth.domain.InvitePreview
import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import com.sortoapps.chorepilot.features.auth.domain.SessionSnapshot
import com.sortoapps.chorepilot.features.chorecapture.domain.DiscoveryAnalysisRepository
import com.sortoapps.chorepilot.features.chorecapture.domain.DiscoveryAnalysisResult
import com.sortoapps.chorepilot.features.chorecapture.domain.DiscoveryDraftItem
import com.sortoapps.chorepilot.features.chorecapture.domain.DiscoverySourceImage
import com.sortoapps.chorepilot.features.chores.domain.ChoreDailyModeValue
import com.sortoapps.chorepilot.features.chores.domain.ChoreDraft
import com.sortoapps.chorepilot.features.chores.domain.ChoreEffortValue
import com.sortoapps.chorepilot.features.chores.domain.ChoreItem
import com.sortoapps.chorepilot.features.chores.domain.ChoreItemStatus
import com.sortoapps.chorepilot.features.chores.domain.ChoreRepeatFrequencyValue
import com.sortoapps.chorepilot.features.chores.domain.ChoreRepository
import com.sortoapps.chorepilot.features.chores.domain.ChoreScheduleDay
import com.sortoapps.chorepilot.features.chores.domain.ProofEvidence
import com.sortoapps.chorepilot.features.chores.domain.ProofUploadPayload
import com.sortoapps.chorepilot.features.chores.domain.ProofUploadResult
import com.sortoapps.chorepilot.features.chores.domain.RoomDefinition
import com.sortoapps.chorepilot.features.notifications.domain.HouseholdNotifications
import com.sortoapps.chorepilot.features.notifications.domain.DiscoveryChoreSubmittedNotification
import com.sortoapps.chorepilot.features.notifications.domain.ProofApprovedNotification
import com.sortoapps.chorepilot.features.notifications.domain.ProofSubmittedNotification
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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
class ChoreCaptureViewModelTest {
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
    fun discoveryModeWithoutProOpensPremiumInsteadOfAnalyzing() = runTest(dispatcher) {
        val discoveryRepository = FakeDiscoveryAnalysisRepository()
        val viewModel = ChoreCaptureViewModel(
            choreRepository = FakeChoreRepository(),
            discoveryAnalysisRepository = discoveryRepository,
            sessionRepository = FakeSessionRepository(),
            subscriptionRepository = FakeSubscriptionRepository(hasPro = false),
            householdNotifications = FakeHouseholdNotifications(),
            vibratorUtil = FakeVibratorUtil(),
        )

        viewModel.onEvent(ChoreCaptureUiEvent.LoadChore(choreId = "", day = null))
        viewModel.onEvent(ChoreCaptureUiEvent.ProofCaptured(photoPayload()))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.shouldOpenPremium)
        assertEquals(0, discoveryRepository.analyzeCalls)
    }

    @Test
    fun proUserCapturePopulatesReviewDrafts() = runTest(dispatcher) {
        val discoveryRepository = FakeDiscoveryAnalysisRepository(
            result = DiscoveryAnalysisResult(
                drafts = listOf(
                    DiscoveryDraftItem(
                        id = "draft-1",
                        title = "Clear the counters",
                        roomName = "Kitchen",
                        effort = ChoreEffortValue.Standard,
                        proofNeededSuggested = true,
                        repeatsEnabled = true,
                        repeatFrequency = ChoreRepeatFrequencyValue.Weekly,
                        dailyMode = ChoreDailyModeValue.Weekdays,
                        selectedWeekdays = setOf("Sunday"),
                        selectedOneTimeDate = "Tomorrow",
                    ),
                ),
            ),
        )
        val viewModel = ChoreCaptureViewModel(
            choreRepository = FakeChoreRepository(),
            discoveryAnalysisRepository = discoveryRepository,
            sessionRepository = FakeSessionRepository(),
            subscriptionRepository = FakeSubscriptionRepository(hasPro = true),
            householdNotifications = FakeHouseholdNotifications(),
            vibratorUtil = FakeVibratorUtil(),
        )

        viewModel.onEvent(ChoreCaptureUiEvent.LoadChore(choreId = "", day = null))
        viewModel.onEvent(ChoreCaptureUiEvent.ProofCaptured(photoPayload()))
        advanceUntilIdle()

        assertEquals(DiscoveryCapturePhase.ReviewDrafts, viewModel.uiState.value.discoveryPhase)
        assertEquals(1, viewModel.uiState.value.discoveryDrafts.size)
        assertEquals("Clear the counters", viewModel.uiState.value.discoveryDrafts.first().title)
    }

    @Test
    fun analysisFailureKeepsPhotoAndShowsRetryableError() = runTest(dispatcher) {
        val discoveryRepository = FakeDiscoveryAnalysisRepository(
            failure = IllegalStateException("Image too dark"),
        )
        val viewModel = ChoreCaptureViewModel(
            choreRepository = FakeChoreRepository(),
            discoveryAnalysisRepository = discoveryRepository,
            sessionRepository = FakeSessionRepository(),
            subscriptionRepository = FakeSubscriptionRepository(hasPro = true),
            householdNotifications = FakeHouseholdNotifications(),
            vibratorUtil = FakeVibratorUtil(),
        )

        viewModel.onEvent(ChoreCaptureUiEvent.LoadChore(choreId = "", day = null))
        viewModel.onEvent(ChoreCaptureUiEvent.ProofCaptured(photoPayload()))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hasCapturedPhoto)
        assertEquals(DiscoveryCapturePhase.Error, viewModel.uiState.value.discoveryPhase)
        assertEquals("Image too dark", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun missingProofChoreShowsUnavailableStateInsteadOfStaleContent() = runTest(dispatcher) {
        val viewModel = ChoreCaptureViewModel(
            choreRepository = FakeChoreRepository(),
            discoveryAnalysisRepository = FakeDiscoveryAnalysisRepository(),
            sessionRepository = FakeSessionRepository(),
            subscriptionRepository = FakeSubscriptionRepository(hasPro = true),
            householdNotifications = FakeHouseholdNotifications(),
            vibratorUtil = FakeVibratorUtil(),
        )

        viewModel.onEvent(
            ChoreCaptureUiEvent.LoadChore(
                choreId = "missing-chore",
                day = null,
            ),
        )
        advanceUntilIdle()

        assertEquals("missing-chore", viewModel.uiState.value.choreId)
        assertEquals("Chore unavailable", viewModel.uiState.value.title)
        assertEquals(
            "Open the chore list and pick the latest version before uploading proof.",
            viewModel.uiState.value.unavailableMessage,
        )
        assertFalse(viewModel.uiState.value.isDiscoveryMode)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun savingAcceptedDraftsPersistsImageMetadataAndCompletesFlow() = runTest(dispatcher) {
        val choreRepository = FakeChoreRepository()
        val discoveryRepository = FakeDiscoveryAnalysisRepository(
            result = DiscoveryAnalysisResult(
                drafts = listOf(
                    DiscoveryDraftItem(
                        id = "draft-1",
                        title = "Pick up toys",
                        roomName = "Living room",
                        effort = ChoreEffortValue.Heavy,
                        proofNeededSuggested = false,
                        repeatsEnabled = false,
                        repeatFrequency = ChoreRepeatFrequencyValue.Weekly,
                        dailyMode = ChoreDailyModeValue.Weekdays,
                        selectedWeekdays = emptySet(),
                        selectedOneTimeDate = "Tomorrow",
                    ),
                ),
            ),
            sourceImage = DiscoverySourceImage(
                discoverySessionId = "session-1",
                storagePath = "households/house-1/images/discovery/erick/session-1.jpg",
                downloadUrl = "https://example.com/discovery.jpg",
            ),
        )
        val viewModel = ChoreCaptureViewModel(
            choreRepository = choreRepository,
            discoveryAnalysisRepository = discoveryRepository,
            sessionRepository = FakeSessionRepository(),
            subscriptionRepository = FakeSubscriptionRepository(hasPro = true),
            householdNotifications = FakeHouseholdNotifications(),
            vibratorUtil = FakeVibratorUtil(),
        )

        viewModel.onEvent(ChoreCaptureUiEvent.LoadChore(choreId = "", day = null))
        viewModel.onEvent(ChoreCaptureUiEvent.ProofCaptured(photoPayload()))
        advanceUntilIdle()
        viewModel.onEvent(ChoreCaptureUiEvent.SaveDiscoveryDrafts)
        advanceUntilIdle()

        assertEquals(1, choreRepository.savedDrafts.size)
        assertEquals("session-1", choreRepository.savedDrafts.first().discoverySessionId)
        assertEquals("https://example.com/discovery.jpg", choreRepository.savedDrafts.first().discoveryImageUrl)
        assertTrue(choreRepository.savedDrafts.first().discoveryGenerated)
        assertEquals(DiscoveryCapturePhase.Saved, viewModel.uiState.value.discoveryPhase)
        assertTrue(viewModel.uiState.value.shouldReturnToList)
    }

    @Test
    fun memberDiscoveryDraftsAreSubmittedForReview() = runTest(dispatcher) {
        val choreRepository = FakeChoreRepository()
        val notifications = FakeHouseholdNotifications()
        val discoveryRepository = FakeDiscoveryAnalysisRepository(
            result = DiscoveryAnalysisResult(
                drafts = listOf(
                    DiscoveryDraftItem(
                        id = "draft-1",
                        title = "Pick up towels",
                        roomName = "Bathroom",
                        effort = ChoreEffortValue.Standard,
                        proofNeededSuggested = false,
                        repeatsEnabled = false,
                        repeatFrequency = ChoreRepeatFrequencyValue.Weekly,
                        dailyMode = ChoreDailyModeValue.Weekdays,
                        selectedWeekdays = emptySet(),
                        selectedOneTimeDate = "Tomorrow",
                    ),
                ),
            ),
        )
        val viewModel = ChoreCaptureViewModel(
            choreRepository = choreRepository,
            discoveryAnalysisRepository = discoveryRepository,
            sessionRepository = FakeSessionRepository(role = HouseholdRole.Member),
            subscriptionRepository = FakeSubscriptionRepository(hasPro = true),
            householdNotifications = notifications,
            vibratorUtil = FakeVibratorUtil(),
        )

        viewModel.onEvent(ChoreCaptureUiEvent.LoadChore(choreId = "", day = null))
        viewModel.onEvent(ChoreCaptureUiEvent.ProofCaptured(photoPayload()))
        advanceUntilIdle()
        viewModel.onEvent(ChoreCaptureUiEvent.SaveDiscoveryDrafts)
        advanceUntilIdle()

        assertEquals("Submit for review", viewModel.uiState.value.discoverySaveActionLabel)
        assertEquals(false, choreRepository.savedDrafts.first().isActive)
        assertEquals(true, choreRepository.savedDrafts.first().pendingApproval)
        assertEquals("Pick up towels", notifications.discoverySubmitted.single().choreTitle)
    }

    @Test
    fun discoveryResumesAnalysisAfterPremiumUnlockWithoutRecapturing() = runTest(dispatcher) {
        val discoveryRepository = FakeDiscoveryAnalysisRepository(
            result = DiscoveryAnalysisResult(
                drafts = listOf(
                    DiscoveryDraftItem(
                        id = "draft-1",
                        title = "Fold blankets",
                        roomName = "Living room",
                        effort = ChoreEffortValue.Standard,
                        proofNeededSuggested = false,
                    ),
                ),
            ),
        )
        val subscriptionRepository = FakeSubscriptionRepository(hasPro = false)
        val viewModel = ChoreCaptureViewModel(
            choreRepository = FakeChoreRepository(),
            discoveryAnalysisRepository = discoveryRepository,
            sessionRepository = FakeSessionRepository(),
            subscriptionRepository = subscriptionRepository,
            householdNotifications = FakeHouseholdNotifications(),
            vibratorUtil = FakeVibratorUtil(),
        )

        viewModel.onEvent(ChoreCaptureUiEvent.LoadChore(choreId = "", day = null))
        viewModel.onEvent(ChoreCaptureUiEvent.ProofCaptured(photoPayload()))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.shouldOpenPremium)
        assertEquals(0, discoveryRepository.analyzeCalls)

        viewModel.onEvent(ChoreCaptureUiEvent.PremiumNavigationHandled)
        subscriptionRepository.emit(hasPro = true)
        advanceUntilIdle()

        assertEquals(1, discoveryRepository.analyzeCalls)
        assertEquals(DiscoveryCapturePhase.ReviewDrafts, viewModel.uiState.value.discoveryPhase)
        assertEquals("Fold blankets", viewModel.uiState.value.discoveryDrafts.first().title)
    }

    @Test
    fun nonPurchaserMemberCanUseDiscoveryWhenHouseholdHasPro() = runTest(dispatcher) {
        val discoveryRepository = FakeDiscoveryAnalysisRepository(
            result = DiscoveryAnalysisResult(
                drafts = listOf(
                    DiscoveryDraftItem(
                        id = "draft-1",
                        title = "Wipe the mirror",
                        roomName = "Bathroom",
                        effort = ChoreEffortValue.Standard,
                        proofNeededSuggested = false,
                    ),
                ),
            ),
        )
        val viewModel = ChoreCaptureViewModel(
            choreRepository = FakeChoreRepository(),
            discoveryAnalysisRepository = discoveryRepository,
            sessionRepository = FakeSessionRepository(),
            subscriptionRepository = FakeSubscriptionRepository(
                hasPro = true,
                purchaserHasPro = false,
                householdHasPro = true,
            ),
            householdNotifications = FakeHouseholdNotifications(),
            vibratorUtil = FakeVibratorUtil(),
        )

        viewModel.onEvent(ChoreCaptureUiEvent.LoadChore(choreId = "", day = null))
        viewModel.onEvent(ChoreCaptureUiEvent.ProofCaptured(photoPayload()))
        advanceUntilIdle()

        assertEquals(1, discoveryRepository.analyzeCalls)
        assertEquals(DiscoveryCapturePhase.ReviewDrafts, viewModel.uiState.value.discoveryPhase)
    }

    private fun photoPayload(): ProofUploadPayload = ProofUploadPayload(
        localPath = "/tmp/proof.jpg",
        previewUri = "file:///tmp/proof.jpg",
        mimeType = "image/jpeg",
        fileExtension = "jpg",
    )
}

private class FakeDiscoveryAnalysisRepository(
    private val result: DiscoveryAnalysisResult = DiscoveryAnalysisResult(emptyList()),
    private val failure: Throwable? = null,
    private val sourceImage: DiscoverySourceImage = DiscoverySourceImage(
        discoverySessionId = "session-0",
        storagePath = "debug/session-0.jpg",
        downloadUrl = "",
    ),
) : DiscoveryAnalysisRepository {
    var analyzeCalls: Int = 0

    override suspend fun analyzeRoomPhoto(payload: ProofUploadPayload): DiscoveryAnalysisResult {
        analyzeCalls += 1
        failure?.let { throw it }
        return result
    }

    override suspend fun persistAcceptedRoomPhoto(
        discoverySessionId: String,
        payload: ProofUploadPayload,
    ): DiscoverySourceImage = sourceImage
}

private class FakeChoreRepository : ChoreRepository {
    private val choreState = MutableStateFlow(emptyList<ChoreItem>())
    private val roomState = MutableStateFlow(
        listOf(
            RoomDefinition("room-kitchen", "Kitchen", "kitchen", "teal"),
            RoomDefinition("room-living", "Living room", "living", "violet"),
        ),
    )

    val savedDrafts = mutableListOf<ChoreDraft>()

    override val chores: StateFlow<List<ChoreItem>> = choreState
    override val rooms: StateFlow<List<RoomDefinition>> = roomState
    override val householdMembers: StateFlow<List<com.sortoapps.chorepilot.features.chores.domain.HouseholdSchedulingMember>> = MutableStateFlow(emptyList())

    override fun getChores(): List<ChoreItem> = choreState.value

    override fun getChore(id: String, day: ChoreScheduleDay?): ChoreItem? = choreState.value.firstOrNull { it.id == id }

    override fun getLatestProofForChore(choreId: String, day: ChoreScheduleDay?): ProofEvidence? = null

    override fun getDraft(id: String): ChoreDraft? = null

    override fun getScheduledChores(day: ChoreScheduleDay): List<ChoreItem> = choreState.value

    override suspend fun refresh() = Unit

    override suspend fun saveChore(draft: ChoreDraft): String {
        savedDrafts += draft
        return draft.id ?: "saved-${savedDrafts.size}"
    }

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
    ): ProofUploadResult = ProofUploadResult("proof-1", "proofs/proof-1.jpg", "https://example.com/proof.jpg")

    override suspend fun approveProof(choreId: String, day: ChoreScheduleDay?) = Unit

    override suspend fun rejectProof(choreId: String, reason: String, day: ChoreScheduleDay?) = Unit

    override suspend fun getAvailabilitySuggestions(days: List<ChoreScheduleDay>) = emptyList<com.sortoapps.chorepilot.features.chores.domain.AvailabilitySuggestion>()

    override suspend fun acceptAvailabilitySuggestion(suggestion: com.sortoapps.chorepilot.features.chores.domain.AvailabilitySuggestion) = Unit

    override suspend fun dismissAvailabilitySuggestion(suggestion: com.sortoapps.chorepilot.features.chores.domain.AvailabilitySuggestion) = Unit
}

private class FakeSessionRepository(
    role: HouseholdRole = HouseholdRole.Owner,
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

private class FakeSubscriptionRepository(
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

private class FakeVibratorUtil : VibratorUtil {
    override fun triggerVibration() = Unit

    override fun triggerVibrationSuccess() = Unit

    override fun cancelVibration() = Unit
}

private class FakeHouseholdNotifications : HouseholdNotifications {
    val submitted = mutableListOf<ProofSubmittedNotification>()
    val approved = mutableListOf<ProofApprovedNotification>()
    val discoverySubmitted = mutableListOf<DiscoveryChoreSubmittedNotification>()

    override suspend fun onProofSubmitted(notification: ProofSubmittedNotification) {
        submitted += notification
    }

    override suspend fun onProofApproved(notification: ProofApprovedNotification) {
        approved += notification
    }

    override suspend fun onDiscoveryChoreSubmitted(notification: DiscoveryChoreSubmittedNotification) {
        discoverySubmitted += notification
    }
}
