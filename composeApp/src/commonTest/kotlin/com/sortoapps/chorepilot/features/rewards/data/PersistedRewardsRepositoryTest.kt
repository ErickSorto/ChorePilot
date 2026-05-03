package com.sortoapps.chorepilot.features.rewards.data

import com.sortoapps.chorepilot.core.platform.PlatformPreferences
import com.sortoapps.chorepilot.features.auth.domain.AuthFlowResult
import com.sortoapps.chorepilot.features.auth.domain.HouseholdRole
import com.sortoapps.chorepilot.features.auth.domain.InvitePreview
import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import com.sortoapps.chorepilot.features.auth.domain.SessionSnapshot
import com.sortoapps.chorepilot.features.chores.domain.ChoreDraft
import com.sortoapps.chorepilot.features.chores.domain.ChoreItem
import com.sortoapps.chorepilot.features.chores.domain.ChoreItemStatus
import com.sortoapps.chorepilot.features.chores.domain.ChoreRepository
import com.sortoapps.chorepilot.features.chores.domain.ChoreScheduleDay
import com.sortoapps.chorepilot.features.chores.domain.ProofEvidence
import com.sortoapps.chorepilot.features.chores.domain.ProofUploadPayload
import com.sortoapps.chorepilot.features.chores.domain.ProofUploadResult
import com.sortoapps.chorepilot.features.chores.domain.RoomDefinition
import com.sortoapps.chorepilot.features.rewards.domain.RewardPointEventType
import com.sortoapps.chorepilot.features.rewards.domain.RewardRedemptionStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class PersistedRewardsRepositoryTest {
    @Test
    fun completedAndPendingProofChoresCreateAutomaticRewardsProgress() = runTest {
        val harness = buildRepository(
            chores = listOf(
                completedChore(
                    id = "dishes",
                    title = "Wash dishes",
                    assigneeId = "erick",
                    assignee = "Erick",
                    proofPhotoCount = 2,
                ),
                completedChore(
                    id = "trash",
                    title = "Take out trash",
                    assigneeId = "erick",
                    assignee = "Erick",
                ),
                pendingProofChore(
                    id = "bathroom",
                    title = "Bathroom reset",
                    assigneeId = "emely",
                    assignee = "Emely",
                ),
            ),
        )
        val repository = harness.repository

        repository.refresh()
        advanceUntilIdle()

        val snapshot = assertNotNull(repository.state.value.snapshot)
        val viewer = assertNotNull(snapshot.memberProgress.firstOrNull { it.memberId == "erick" })

        assertEquals(3, snapshot.availableRewards.size)
        assertEquals(34, viewer.availablePoints)
        assertEquals(34, viewer.totalEarnedPoints)
        assertEquals(1, viewer.currentStreak)
        assertEquals(2, viewer.completedChores)
        assertEquals(6, snapshot.recentPointEvents.size)
        assertTrue(
            snapshot.recentPointEvents.any {
                it.type == RewardPointEventType.Completion && it.description == "Closed out Wash dishes."
            },
        )

        harness.appScope.cancel()
    }

    @Test
    fun proofApprovalAutoAwardsConfiguredBonusWithoutPendingApproval() = runTest {
        val harness = buildRepository(
            chores = listOf(
                completedChore(id = "dishes", title = "Wash dishes", assigneeId = "erick", assignee = "Erick", proofPhotoCount = 2),
            ),
        )
        val repository = harness.repository

        repository.refresh()
        advanceUntilIdle()

        val snapshot = assertNotNull(repository.state.value.snapshot)
        val viewer = assertNotNull(snapshot.memberProgress.firstOrNull { it.memberId == "erick" })
        val approvalEvent = assertNotNull(
            snapshot.recentPointEvents.firstOrNull { it.type == RewardPointEventType.ProofApproved },
        )

        assertEquals(22, viewer.availablePoints)
        assertEquals(22, viewer.totalEarnedPoints)
        assertEquals(1, viewer.currentStreak)
        assertEquals(6, approvalEvent.pointsDelta)
        assertEquals("Proof approval landed for Wash dishes.", approvalEvent.description)

        harness.appScope.cancel()
    }

    @Test
    fun completionTransitionAutomaticallyAwardsConfiguredBasePoints() = runTest {
        val harness = buildRepository(
            chores = listOf(
                baseChore(
                    id = "dishes",
                    title = "Wash dishes",
                    assigneeId = "erick",
                    assignee = "Erick",
                    status = ChoreItemStatus.DueSoon,
                    proofPhotoCount = 0,
                ),
            ),
        )
        val repository = harness.repository

        repository.refresh()
        advanceUntilIdle()

        harness.choreRepository.updateChores(
            listOf(
                completedChore(
                    id = "dishes",
                    title = "Wash dishes",
                    assigneeId = "erick",
                    assignee = "Erick",
                ),
            ),
        )
        advanceUntilIdle()

        val snapshot = assertNotNull(repository.state.value.snapshot)
        val completionEvent = assertNotNull(snapshot.recentPointEvents.firstOrNull { it.type == RewardPointEventType.Completion })
        val viewer = assertNotNull(snapshot.memberProgress.firstOrNull { it.memberId == "erick" })

        assertEquals(12, completionEvent.pointsDelta)
        assertEquals("Closed out Wash dishes.", completionEvent.description)
        assertEquals(12, viewer.availablePoints)
        assertEquals(12, viewer.totalEarnedPoints)

        harness.appScope.cancel()
    }

    @Test
    fun proofApprovalTransitionAutomaticallyAwardsConfiguredBonus() = runTest {
        val harness = buildRepository(
            chores = listOf(
                pendingProofChore(
                    id = "bathroom",
                    title = "Bathroom reset",
                    assigneeId = "emely",
                    assignee = "Emely",
                ),
            ),
        )
        val repository = harness.repository

        repository.refresh()
        advanceUntilIdle()

        harness.choreRepository.updateChores(
            listOf(
                completedChore(
                    id = "bathroom",
                    title = "Bathroom reset",
                    assigneeId = "emely",
                    assignee = "Emely",
                    proofPhotoCount = 1,
                ),
            ),
        )
        advanceUntilIdle()

        val snapshot = assertNotNull(repository.state.value.snapshot)
        val approvalEvent = assertNotNull(snapshot.recentPointEvents.firstOrNull { it.type == RewardPointEventType.ProofApproved })
        val viewer = assertNotNull(snapshot.memberProgress.firstOrNull { it.memberId == "emely" })

        assertEquals(6, approvalEvent.pointsDelta)
        assertEquals("Proof approval landed for Bathroom reset.", approvalEvent.description)
        assertEquals(22, viewer.availablePoints)
        assertEquals(22, viewer.totalEarnedPoints)

        harness.appScope.cancel()
    }

    @Test
    fun redeemingRewardAppliesImmediatelyWithoutApprovalQueue() = runTest {
        val harness = buildRepository(
            chores = listOf(
                completedChore(
                    id = "dishes",
                    title = "Wash dishes",
                    assigneeId = "erick",
                    assignee = "Erick",
                    proofPhotoCount = 2,
                ),
                completedChore(
                    id = "trash",
                    title = "Take out trash",
                    assigneeId = "erick",
                    assignee = "Erick",
                ),
            ),
        )
        val repository = harness.repository

        repository.refresh()
        advanceUntilIdle()
        repository.redeemReward("reward-movie-picker")
        advanceUntilIdle()

        val snapshot = assertNotNull(repository.state.value.snapshot)
        val viewer = assertNotNull(snapshot.memberProgress.firstOrNull { it.memberId == "erick" })

        assertEquals(4, viewer.availablePoints)
        assertEquals(RewardRedemptionStatus.Approved, snapshot.redemptionHistory.first().status)
        assertEquals("Reward unlocked", snapshot.celebration?.title)
        assertTrue(snapshot.recentPointEvents.any { it.type == RewardPointEventType.RedemptionReserved })

        harness.appScope.cancel()
    }

    @Test
    fun memberCanRedeemRewardImmediatelyWithoutManagerAccess() = runTest {
        val harness = buildRepository(
            chores = listOf(
                completedChore(
                    id = "dishes",
                    title = "Wash dishes",
                    assigneeId = "erick",
                    assignee = "Erick",
                    proofPhotoCount = 2,
                ),
                completedChore(
                    id = "trash",
                    title = "Take out trash",
                    assigneeId = "erick",
                    assignee = "Erick",
                ),
            ),
            role = HouseholdRole.Member,
        )
        val repository = harness.repository

        repository.refresh()
        advanceUntilIdle()
        repository.redeemReward("reward-movie-picker")
        advanceUntilIdle()

        val snapshot = assertNotNull(repository.state.value.snapshot)
        val viewer = assertNotNull(snapshot.memberProgress.firstOrNull { it.memberId == "erick" })

        assertEquals(4, viewer.availablePoints)
        assertEquals(RewardRedemptionStatus.Approved, snapshot.redemptionHistory.first().status)
        assertEquals("Reward unlocked", snapshot.celebration?.title)
        assertTrue(snapshot.recentPointEvents.any { it.type == RewardPointEventType.RedemptionReserved })

        harness.appScope.cancel()
    }

    private fun TestScope.buildRepository(
        chores: List<ChoreItem>,
        role: HouseholdRole = HouseholdRole.Owner,
    ): RepositoryHarness {
        val appScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val choreRepository = FakeChoreRepository(chores)
        return RepositoryHarness(
            repository = PersistedRewardsRepository(
                platformPreferences = InMemoryPlatformPreferences(),
                sessionRepository = FakeSessionRepository(role),
                choreRepository = choreRepository,
                appScope = appScope,
            ),
            choreRepository = choreRepository,
            appScope = appScope,
        )
    }
}

private data class RepositoryHarness(
    val repository: PersistedRewardsRepository,
    val choreRepository: FakeChoreRepository,
    val appScope: CoroutineScope,
)

private class InMemoryPlatformPreferences : PlatformPreferences {
    private val store = mutableMapOf<String, String>()

    override fun getString(key: String): String? = store[key]

    override fun putString(key: String, value: String) {
        store[key] = value
    }

    override fun remove(key: String) {
        store.remove(key)
    }
}

private class FakeSessionRepository(
    role: HouseholdRole = HouseholdRole.Owner,
) : SessionRepository {
    override val session = MutableStateFlow(
        SessionSnapshot(
            signedIn = true,
            userId = "erick",
            displayName = "Erick",
            householdId = "river-house",
            householdName = "River House",
            role = HouseholdRole.Owner,
        ),
    )

    override suspend fun refreshSession() = Unit

    override suspend fun completeOnboarding(displayName: String) = Unit

    override suspend fun signIn(
        email: String,
        password: String,
        displayNameHint: String,
    ): AuthFlowResult = AuthFlowResult.Main

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

    override suspend fun joinHousehold(
        displayName: String,
        inviteCode: String,
    ): AuthFlowResult = AuthFlowResult.Main

    override suspend fun previewInvite(inviteCode: String): InvitePreview? = null

    override suspend fun dismissInitialPremiumFlow() = Unit

    override suspend fun switchActiveHousehold(householdId: String) = Unit

    override suspend fun signOut() = Unit

    override suspend fun resetDemo() = Unit
}

private class FakeChoreRepository(
    chores: List<ChoreItem>,
) : ChoreRepository {
    private val choresFlow = MutableStateFlow(chores)
    override val chores: StateFlow<List<ChoreItem>> = choresFlow
    override val rooms: StateFlow<List<RoomDefinition>> = MutableStateFlow(emptyList())
    override val householdMembers: StateFlow<List<com.sortoapps.chorepilot.features.chores.domain.HouseholdSchedulingMember>> = MutableStateFlow(emptyList())

    override fun getChores(): List<ChoreItem> = chores.value

    override fun getChore(id: String, day: ChoreScheduleDay?): ChoreItem? = chores.value.firstOrNull { it.id == id }

    override fun getLatestProofForChore(
        choreId: String,
        day: ChoreScheduleDay?,
    ): ProofEvidence? = null

    override fun getDraft(id: String): ChoreDraft? = null

    override fun getScheduledChores(day: ChoreScheduleDay): List<ChoreItem> = emptyList()

    override suspend fun refresh() = Unit

    override suspend fun saveChore(draft: ChoreDraft): String = draft.id
        ?: "draft"

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
    ): ProofUploadResult = ProofUploadResult(
        proofId = "proof-$choreId",
        storagePath = "/proof/$choreId",
        downloadUrl = "https://example.com/$choreId.jpg",
    )

    override suspend fun approveProof(choreId: String, day: ChoreScheduleDay?) = Unit

    override suspend fun rejectProof(choreId: String, reason: String, day: ChoreScheduleDay?) = Unit

    override suspend fun getAvailabilitySuggestions(days: List<ChoreScheduleDay>) = emptyList<com.sortoapps.chorepilot.features.chores.domain.AvailabilitySuggestion>()

    override suspend fun acceptAvailabilitySuggestion(suggestion: com.sortoapps.chorepilot.features.chores.domain.AvailabilitySuggestion) = Unit

    override suspend fun dismissAvailabilitySuggestion(suggestion: com.sortoapps.chorepilot.features.chores.domain.AvailabilitySuggestion) = Unit

    fun updateChores(next: List<ChoreItem>) {
        choresFlow.value = next
    }
}

private fun completedChore(
    id: String,
    title: String,
    assigneeId: String,
    assignee: String,
    proofPhotoCount: Int = 0,
): ChoreItem = baseChore(
    id = id,
    title = title,
    assigneeId = assigneeId,
    assignee = assignee,
    status = ChoreItemStatus.Completed,
    proofPhotoCount = proofPhotoCount,
)

private fun pendingProofChore(
    id: String,
    title: String,
    assigneeId: String,
    assignee: String,
): ChoreItem = baseChore(
    id = id,
    title = title,
    assigneeId = assigneeId,
    assignee = assignee,
    status = ChoreItemStatus.PendingProof,
    proofPhotoCount = 1,
)

private fun baseChore(
    id: String,
    title: String,
    assigneeId: String,
    assignee: String,
    status: ChoreItemStatus,
    proofPhotoCount: Int,
): ChoreItem = ChoreItem(
    id = id,
    title = title,
    area = "Kitchen",
    cadence = "Daily",
    assignee = assignee,
    assigneeId = assigneeId,
    status = status,
    durationLabel = "10 min",
    dueLabel = "Today",
    description = title,
    reminder = "After school",
    tags = emptyList(),
    proofRequired = proofPhotoCount > 0,
    proofPhotoCount = proofPhotoCount,
)
