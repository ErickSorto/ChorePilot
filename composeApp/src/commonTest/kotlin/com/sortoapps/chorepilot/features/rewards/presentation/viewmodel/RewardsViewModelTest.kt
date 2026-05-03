package com.sortoapps.chorepilot.features.rewards.presentation.viewmodel

import com.sortoapps.chorepilot.core.demo.DemoTone
import com.sortoapps.chorepilot.core.platform.VibratorUtil
import com.sortoapps.chorepilot.features.auth.domain.AuthFlowResult
import com.sortoapps.chorepilot.features.auth.domain.HouseholdRole
import com.sortoapps.chorepilot.features.auth.domain.InvitePreview
import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import com.sortoapps.chorepilot.features.auth.domain.SessionSnapshot
import com.sortoapps.chorepilot.features.rewards.domain.RewardDefinition
import com.sortoapps.chorepilot.features.rewards.domain.RewardDefinitionDraft
import com.sortoapps.chorepilot.features.rewards.domain.RewardPointEvent
import com.sortoapps.chorepilot.features.rewards.domain.RewardPointEventType
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
class RewardsViewModelTest {
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
    fun ownerSeesManagementSurfacesAndCanSaveRewardDraft() = runTest(dispatcher) {
        val rewardsRepository = FakeRewardsRepository()
        val viewModel = RewardsViewModel(
            rewardsRepository = rewardsRepository,
            sessionRepository = FakeRewardsSessionRepository(role = HouseholdRole.Owner),
            vibratorUtil = FakeVibratorUtil(),
        )

        advanceUntilIdle()
        viewModel.onEvent(RewardsUiEvent.DraftTitleChanged("Movie night pick"))
        viewModel.onEvent(RewardsUiEvent.DraftDescriptionChanged("Choose the next movie."))
        viewModel.onEvent(RewardsUiEvent.DraftCostChanged("35"))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.canManageRewards)
        assertTrue(viewModel.uiState.value.editor.saveEnabled)

        viewModel.onEvent(RewardsUiEvent.SaveRewardDefinition)
        advanceUntilIdle()

        assertEquals("Movie night pick", rewardsRepository.savedDrafts.single().title)
    }

    @Test
    fun memberStateHidesManagementAndStillAllowsRedeeming() = runTest(dispatcher) {
        val rewardsRepository = FakeRewardsRepository()
        val viewModel = RewardsViewModel(
            rewardsRepository = rewardsRepository,
            sessionRepository = FakeRewardsSessionRepository(role = HouseholdRole.Member),
            vibratorUtil = FakeVibratorUtil(),
        )

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.canManageRewards)
        assertEquals("Reward unlocked", viewModel.uiState.value.celebration?.title)
        assertEquals(DemoTone.Primary, viewModel.uiState.value.leaderboard.first().tone)

        viewModel.onEvent(RewardsUiEvent.RedeemReward("movie-picker"))
        advanceUntilIdle()

        assertEquals(listOf("movie-picker"), rewardsRepository.redeemedIds)
        assertEquals(null, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun rewardsCopyShowsAutomaticPointsWithoutApprovalLanguage() = runTest(dispatcher) {
        val rewardsRepository = FakeRewardsRepository()
        val viewModel = RewardsViewModel(
            rewardsRepository = rewardsRepository,
            sessionRepository = FakeRewardsSessionRepository(role = HouseholdRole.Member),
            vibratorUtil = FakeVibratorUtil(),
        )

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.history.any { it.title == "Proof approved" && it.pointsLabel == "+6 pts" })
        assertEquals("42 pts", viewModel.uiState.value.hero.pointsLabel)
        assertTrue(viewModel.uiState.value.history.first().timestampLabel.isNotBlank())
    }

    @Test
    fun memberCannotSaveRewardDraftAndSeesClearError() = runTest(dispatcher) {
        val rewardsRepository = FakeRewardsRepository()
        val viewModel = RewardsViewModel(
            rewardsRepository = rewardsRepository,
            sessionRepository = FakeRewardsSessionRepository(role = HouseholdRole.Member),
            vibratorUtil = FakeVibratorUtil(),
        )

        advanceUntilIdle()
        viewModel.onEvent(RewardsUiEvent.DraftTitleChanged("Movie night pick"))
        viewModel.onEvent(RewardsUiEvent.DraftDescriptionChanged("Choose the next movie."))
        viewModel.onEvent(RewardsUiEvent.DraftCostChanged("35"))
        advanceUntilIdle()

        viewModel.onEvent(RewardsUiEvent.SaveRewardDefinition)
        advanceUntilIdle()

        assertTrue(rewardsRepository.savedDrafts.isEmpty())
        assertEquals("Only owners and admins can edit reward settings.", viewModel.uiState.value.errorMessage)
    }
}

private class FakeRewardsRepository : RewardsRepository {
    override val state = MutableStateFlow(
        RewardsRepositoryState(
            isLoading = false,
            snapshot = RewardsSnapshot(
                householdName = "River House",
                viewerId = "erick",
                viewerName = "Erick",
                rules = RewardsFamilyRules(),
                availableRewards = listOf(
                    RewardDefinition(
                        id = "movie-picker",
                        title = "Movie picker",
                        description = "Choose the next movie night.",
                        costPoints = 30,
                    ),
                ),
                memberProgress = listOf(
                    RewardsMemberProgress(
                        memberId = "erick",
                        memberName = "Erick",
                        totalEarnedPoints = 42,
                        availablePoints = 42,
                        currentStreak = 4,
                        completedChores = 5,
                        pendingProofCount = 1,
                        overdueCount = 0,
                        nextRewardGap = 0,
                    ),
                ),
                redemptionHistory = emptyList(),
                recentPointEvents = listOf(
                    RewardPointEvent(
                        id = "event-1",
                        memberId = "erick",
                        memberName = "Erick",
                        type = RewardPointEventType.ProofApproved,
                        pointsDelta = 6,
                        dateKey = "2026-04-09",
                        createdAtEpochMs = 1L,
                        description = "Proof approval landed for Wash dishes.",
                    ),
                ),
                celebration = RewardsCelebration(
                    title = "Reward unlocked",
                    body = "Movie picker is ready right now.",
                ),
            ),
        ),
    )
    val savedDrafts = mutableListOf<RewardDefinitionDraft>()
    val redeemedIds = mutableListOf<String>()

    override suspend fun refresh() = Unit

    override suspend fun updateRules(rules: RewardsFamilyRules) = Unit

    override suspend fun saveRewardDefinition(draft: RewardDefinitionDraft) {
        savedDrafts += draft
    }

    override suspend fun deleteRewardDefinition(rewardId: String) = Unit

    override suspend fun redeemReward(rewardId: String) {
        redeemedIds += rewardId
    }

    override suspend fun dismissCelebration() = Unit
}

private class FakeRewardsSessionRepository(
    role: HouseholdRole,
) : SessionRepository {
    override val session: StateFlow<SessionSnapshot> = MutableStateFlow(
        SessionSnapshot(
            signedIn = true,
            userId = "erick",
            displayName = "Erick",
            householdId = "river-house",
            householdName = "River House",
            role = role,
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

private class FakeVibratorUtil : VibratorUtil {
    override fun triggerVibration() = Unit

    override fun triggerVibrationSuccess() = Unit

    override fun cancelVibration() = Unit
}
