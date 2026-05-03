package com.sortoapps.chorepilot.features.family.presentation.viewmodel

import com.sortoapps.chorepilot.core.demo.DemoMetric
import com.sortoapps.chorepilot.core.firebase.HouseholdInviteLifecycleStatus
import com.sortoapps.chorepilot.core.platform.SystemTextActions
import com.sortoapps.chorepilot.core.platform.VibratorUtil
import com.sortoapps.chorepilot.core.revenuecat.SubscriptionPlanId
import com.sortoapps.chorepilot.core.revenuecat.SubscriptionRepository
import com.sortoapps.chorepilot.core.revenuecat.SubscriptionSnapshot
import com.sortoapps.chorepilot.features.auth.domain.AuthFlowResult
import com.sortoapps.chorepilot.features.auth.domain.HouseholdRole
import com.sortoapps.chorepilot.features.auth.domain.InvitePreview
import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import com.sortoapps.chorepilot.features.auth.domain.SessionSnapshot
import com.sortoapps.chorepilot.features.family.domain.FamilyAvailabilityMode
import com.sortoapps.chorepilot.features.family.domain.FamilyInviteSnapshot
import com.sortoapps.chorepilot.features.family.domain.FamilyMemberSnapshot
import com.sortoapps.chorepilot.features.family.domain.FamilyRepository
import com.sortoapps.chorepilot.features.family.domain.FamilyRepositoryState
import com.sortoapps.chorepilot.features.family.domain.FamilySnapshot
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
class FamilyViewModelTest {
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
    fun ownerCanRequestRoleChangeAndConfirmIt() = runTest(dispatcher) {
        val repository = FakeFamilyRepository()
        val session = FakeFamilySessionRepository(role = HouseholdRole.Owner)
        val viewModel = FamilyViewModel(
            familyRepository = repository,
            sessionRepository = session,
            subscriptionRepository = FakeFamilySubscriptionRepository(),
            vibratorUtil = FakeFamilyVibratorUtil(),
            systemTextActions = FakeSystemTextActions(),
        )

        advanceUntilIdle()
        viewModel.onEvent(FamilyUiEvent.MemberSelected("member-1"))
        viewModel.onEvent(FamilyUiEvent.ChangeMemberRoleRequested(HouseholdRole.Admin))

        val confirmation = viewModel.uiState.value.confirmation
        assertNotNull(confirmation)
        assertEquals(FamilyConfirmationAction.ChangeRole, confirmation.action)

        viewModel.onEvent(FamilyUiEvent.ConfirmPendingAction)
        advanceUntilIdle()

        assertEquals(listOf("member-1" to HouseholdRole.Admin), repository.roleChanges)
        assertNull(viewModel.uiState.value.confirmation)
        assertEquals("Mila is now admin.", viewModel.uiState.value.feedback?.message)
    }

    @Test
    fun adminCannotManageOwnerSeat() = runTest(dispatcher) {
        val repository = FakeFamilyRepository()
        val session = FakeFamilySessionRepository(role = HouseholdRole.Admin)
        val viewModel = FamilyViewModel(
            familyRepository = repository,
            sessionRepository = session,
            subscriptionRepository = FakeFamilySubscriptionRepository(),
            vibratorUtil = FakeFamilyVibratorUtil(),
            systemTextActions = FakeSystemTextActions(),
        )

        advanceUntilIdle()
        viewModel.onEvent(FamilyUiEvent.MemberSelected("owner-1"))
        viewModel.onEvent(FamilyUiEvent.RemoveMemberRequested)

        assertTrue(viewModel.uiState.value.feedback?.isError == true)
        assertTrue(viewModel.uiState.value.feedback?.message?.contains("owner", ignoreCase = true) == true)
        assertTrue(repository.removedMembers.isEmpty())
    }

    @Test
    fun memberCannotManageInvites() = runTest(dispatcher) {
        val repository = FakeFamilyRepository()
        val session = FakeFamilySessionRepository(role = HouseholdRole.Member)
        val viewModel = FamilyViewModel(
            familyRepository = repository,
            sessionRepository = session,
            subscriptionRepository = FakeFamilySubscriptionRepository(),
            vibratorUtil = FakeFamilyVibratorUtil(),
            systemTextActions = FakeSystemTextActions(),
        )

        advanceUntilIdle()
        viewModel.onEvent(FamilyUiEvent.RotateInviteRequested)

        assertTrue(viewModel.uiState.value.feedback?.isError == true)
        assertEquals(0, repository.rotatedInvites)
        assertNull(viewModel.uiState.value.confirmation)
    }

    @Test
    fun revokeInviteUpdatesFeedbackAndSummary() = runTest(dispatcher) {
        val repository = FakeFamilyRepository()
        val session = FakeFamilySessionRepository(role = HouseholdRole.Owner)
        val viewModel = FamilyViewModel(
            familyRepository = repository,
            sessionRepository = session,
            subscriptionRepository = FakeFamilySubscriptionRepository(),
            vibratorUtil = FakeFamilyVibratorUtil(),
            systemTextActions = FakeSystemTextActions(),
        )

        advanceUntilIdle()
        viewModel.onEvent(FamilyUiEvent.CancelInviteRequested)
        assertEquals(FamilyConfirmationAction.CancelInvite, viewModel.uiState.value.confirmation?.action)

        viewModel.onEvent(FamilyUiEvent.ConfirmPendingAction)
        advanceUntilIdle()

        assertEquals("Invite revoked.", viewModel.uiState.value.feedback?.message)
        assertEquals("Revoked", viewModel.uiState.value.invite.statusLabel)
        assertTrue(!viewModel.uiState.value.invite.hasActiveInvite)
        assertTrue(viewModel.uiState.value.inviteStatusSummary.contains("revoked", ignoreCase = true))
    }

    @Test
    fun ownerWithOtherActiveMembersCannotLeave() = runTest(dispatcher) {
        val repository = FakeFamilyRepository()
        val session = FakeFamilySessionRepository(role = HouseholdRole.Owner)
        val viewModel = FamilyViewModel(
            familyRepository = repository,
            sessionRepository = session,
            subscriptionRepository = FakeFamilySubscriptionRepository(),
            vibratorUtil = FakeFamilyVibratorUtil(),
            systemTextActions = FakeSystemTextActions(),
        )

        advanceUntilIdle()
        viewModel.onEvent(FamilyUiEvent.LeaveHouseholdRequested)

        assertTrue(viewModel.uiState.value.feedback?.isError == true)
        assertTrue(viewModel.uiState.value.feedback?.message?.contains("Transfer ownership", ignoreCase = true) == true)
        assertEquals(0, repository.leaveCalls)
    }

    @Test
    fun soleOwnerMustTypeDeleteBeforeDeletingHousehold() = runTest(dispatcher) {
        val repository = FakeFamilyRepository(onlyOwner = true)
        val session = FakeFamilySessionRepository(role = HouseholdRole.Owner)
        val viewModel = FamilyViewModel(
            familyRepository = repository,
            sessionRepository = session,
            subscriptionRepository = FakeFamilySubscriptionRepository(),
            vibratorUtil = FakeFamilyVibratorUtil(),
            systemTextActions = FakeSystemTextActions(),
        )

        advanceUntilIdle()
        assertEquals("Delete household", viewModel.uiState.value.leaveHouseholdLabel)

        viewModel.onEvent(FamilyUiEvent.LeaveHouseholdRequested)
        val confirmation = viewModel.uiState.value.confirmation
        assertNotNull(confirmation)
        assertEquals(FamilyConfirmationAction.DeleteHousehold, confirmation.action)
        assertTrue(!confirmation.confirmEnabled)

        viewModel.onEvent(FamilyUiEvent.ConfirmationTextChanged("delete"))
        assertTrue(viewModel.uiState.value.confirmation?.confirmEnabled == true)

        viewModel.onEvent(FamilyUiEvent.ConfirmPendingAction)
        advanceUntilIdle()

        assertEquals(1, repository.deleteCalls)
        assertEquals("Household deleted.", viewModel.uiState.value.feedback?.message)
    }

    @Test
    fun deactivateFlowMovesThroughConfirmationToSuccess() = runTest(dispatcher) {
        val repository = FakeFamilyRepository()
        val session = FakeFamilySessionRepository(role = HouseholdRole.Owner)
        val viewModel = FamilyViewModel(
            familyRepository = repository,
            sessionRepository = session,
            subscriptionRepository = FakeFamilySubscriptionRepository(),
            vibratorUtil = FakeFamilyVibratorUtil(),
            systemTextActions = FakeSystemTextActions(),
        )

        advanceUntilIdle()
        viewModel.onEvent(FamilyUiEvent.MemberSelected("member-1"))
        viewModel.onEvent(FamilyUiEvent.DeactivateMemberRequested)

        assertEquals(FamilyConfirmationAction.DeactivateMember, viewModel.uiState.value.confirmation?.action)

        viewModel.onEvent(FamilyUiEvent.ConfirmPendingAction)
        advanceUntilIdle()

        assertEquals(listOf("member-1"), repository.deactivatedMembers)
        assertEquals("Mila is no longer taking an active seat.", viewModel.uiState.value.feedback?.message)
        assertTrue(viewModel.uiState.value.activeMembers.none { it.id == "member-1" })
        assertTrue(viewModel.uiState.value.inactiveMembers.any { it.id == "member-1" })
        assertNull(viewModel.uiState.value.confirmation)
    }

    @Test
    fun removeFlowFullyRemovesMember() = runTest(dispatcher) {
        val repository = FakeFamilyRepository()
        val session = FakeFamilySessionRepository(role = HouseholdRole.Owner)
        val viewModel = FamilyViewModel(
            familyRepository = repository,
            sessionRepository = session,
            subscriptionRepository = FakeFamilySubscriptionRepository(),
            vibratorUtil = FakeFamilyVibratorUtil(),
            systemTextActions = FakeSystemTextActions(),
        )

        advanceUntilIdle()
        viewModel.onEvent(FamilyUiEvent.MemberSelected("member-1"))
        viewModel.onEvent(FamilyUiEvent.RemoveMemberRequested)
        viewModel.onEvent(FamilyUiEvent.ConfirmPendingAction)
        advanceUntilIdle()

        assertEquals(listOf("member-1"), repository.removedMembers)
        assertTrue(viewModel.uiState.value.members.none { it.id == "member-1" })
        assertTrue(viewModel.uiState.value.inactiveMembers.none { it.id == "member-1" })
    }

    @Test
    fun memberSelectionResolvesBackToSelfOnly() = runTest(dispatcher) {
        val repository = FakeFamilyRepository()
        val session = FakeFamilySessionRepository(
            role = HouseholdRole.Member,
            userId = "member-1",
            displayName = "Mila",
        )
        val viewModel = FamilyViewModel(
            familyRepository = repository,
            sessionRepository = session,
            subscriptionRepository = FakeFamilySubscriptionRepository(),
            vibratorUtil = FakeFamilyVibratorUtil(),
            systemTextActions = FakeSystemTextActions(),
        )

        advanceUntilIdle()
        assertEquals(listOf("member-1"), viewModel.uiState.value.members.map { it.id })

        viewModel.onEvent(FamilyUiEvent.MemberSelected("owner-1"))
        advanceUntilIdle()

        assertEquals("member-1", viewModel.uiState.value.selectedMember?.id)
        assertTrue(!viewModel.uiState.value.canOpenInviteFlow)
        assertTrue(!viewModel.uiState.value.canShowInviteSummary)
    }

    @Test
    fun memberCannotMutateAnotherMembersAvailability() = runTest(dispatcher) {
        val repository = FakeFamilyRepository()
        val session = FakeFamilySessionRepository(
            role = HouseholdRole.Member,
            userId = "member-1",
            displayName = "Mila",
        )
        val viewModel = FamilyViewModel(
            familyRepository = repository,
            sessionRepository = session,
            subscriptionRepository = FakeFamilySubscriptionRepository(),
            vibratorUtil = FakeFamilyVibratorUtil(),
            systemTextActions = FakeSystemTextActions(),
        )

        advanceUntilIdle()
        viewModel.onEvent(FamilyUiEvent.MemberSelected("owner-1"))
        viewModel.onEvent(FamilyUiEvent.ConfirmDayOff)
        advanceUntilIdle()

        assertTrue(repository.availabilityUpdates.all { it.memberId == "member-1" })
        assertTrue(viewModel.uiState.value.feedback?.isError != true)
    }

    @Test
    fun guestFollowsMemberRestrictions() = runTest(dispatcher) {
        val repository = FakeFamilyRepository()
        val session = FakeFamilySessionRepository(
            role = HouseholdRole.Guest,
            userId = "member-1",
            displayName = "Mila",
        )
        val viewModel = FamilyViewModel(
            familyRepository = repository,
            sessionRepository = session,
            subscriptionRepository = FakeFamilySubscriptionRepository(),
            vibratorUtil = FakeFamilyVibratorUtil(),
            systemTextActions = FakeSystemTextActions(),
        )

        advanceUntilIdle()
        assertEquals(listOf("member-1"), viewModel.uiState.value.members.map { it.id })

        viewModel.onEvent(FamilyUiEvent.RotateInviteRequested)
        advanceUntilIdle()

        assertEquals(0, repository.rotatedInvites)
        assertTrue(viewModel.uiState.value.feedback?.isError == true)
    }
}

private class FakeFamilyRepository(
    onlyOwner: Boolean = false,
) : FamilyRepository {
    private val initialSnapshot = FamilySnapshot(
            householdName = "River House",
            pulseLabel = if (onlyOwner) "1 active seat" else "3 active seats",
            householdBadge = "Balanced",
            ownerUserId = "owner-1",
            activeSeatCount = if (onlyOwner) 1 else 3,
            summaryMetrics = listOf(
                DemoMetric(if (onlyOwner) "1" else "3", "Seats"),
                DemoMetric("Active", "Invite"),
            ),
            members = buildList {
                add(
                    FamilyMemberSnapshot(
                        id = "owner-1",
                        name = "Erick",
                        role = HouseholdRole.Owner,
                        statusLabel = "Owner",
                        activeChoresLabel = "2 chores in flow right now.",
                        householdImpact = "Owner seat stays active.",
                        avatarSeed = "ER",
                        tone = com.sortoapps.chorepilot.core.demo.DemoTone.Primary,
                        assignedChoreCount = 2,
                        proofReviewCount = 1,
                    ),
                )
                if (!onlyOwner) {
                    add(
                        FamilyMemberSnapshot(
                            id = "admin-1",
                            name = "Ava",
                            role = HouseholdRole.Admin,
                            statusLabel = "Admin",
                            activeChoresLabel = "1 chore in flow right now.",
                            householdImpact = "Admin seat helps with proof review.",
                            avatarSeed = "AV",
                            tone = com.sortoapps.chorepilot.core.demo.DemoTone.Secondary,
                            assignedChoreCount = 1,
                            proofReviewCount = 1,
                        ),
                    )
                    add(
                        FamilyMemberSnapshot(
                            id = "member-1",
                            name = "Mila",
                            role = HouseholdRole.Member,
                            statusLabel = "On track",
                            activeChoresLabel = "1 chore in flow right now.",
                            householdImpact = "1 chore may shift if Mila is away.",
                            avatarSeed = "MI",
                            tone = com.sortoapps.chorepilot.core.demo.DemoTone.Tertiary,
                            assignedChoreCount = 1,
                            proofReviewCount = 0,
                        ),
                    )
                }
            },
            activeInvite = FamilyInviteSnapshot(
                code = "RIVER-82",
                role = HouseholdRole.Member,
                lifecycleStatus = HouseholdInviteLifecycleStatus.Active,
                statusLabel = "Active now",
                supportingText = "Share this code to add a member.",
                generatedAtLabel = "Created today",
                expiryLabel = "Expires Apr 19",
                usageLabel = "Unlimited uses",
                shareText = "Join River House with code RIVER-82.",
                isJoinable = true,
                seatImpactText = "This uses the next household seat for chores, proof, and reminders.",
            ),
        )

    private val _familyState = MutableStateFlow(
        FamilyRepositoryState(
            isLoading = false,
            snapshot = initialSnapshot,
        ),
    )

    override val familyState: StateFlow<FamilyRepositoryState> = _familyState

    val roleChanges = mutableListOf<Pair<String, HouseholdRole>>()
    val deactivatedMembers = mutableListOf<String>()
    val removedMembers = mutableListOf<String>()
    val availabilityUpdates = mutableListOf<AvailabilityUpdate>()
    val clearedAvailabilityMembers = mutableListOf<String>()
    var rotatedInvites: Int = 0
    var leaveCalls: Int = 0
    var deleteCalls: Int = 0

    override suspend fun refresh() = Unit

    override suspend fun generateInvite(role: HouseholdRole): FamilyInviteSnapshot = rotateInvite(role)

    override suspend fun cancelInvite() {
        _familyState.value = _familyState.value.copy(
            snapshot = _familyState.value.snapshot.copy(
                activeInvite = _familyState.value.snapshot.activeInvite.copy(
                    lifecycleStatus = HouseholdInviteLifecycleStatus.Revoked,
                    statusLabel = "Revoked",
                    supportingText = "This code was revoked and won't let anyone join.",
                    shareText = "",
                    isJoinable = false,
                ),
            ),
        )
    }

    override suspend fun rotateInvite(role: HouseholdRole): FamilyInviteSnapshot {
        rotatedInvites += 1
        val invite = _familyState.value.snapshot.activeInvite.copy(
            code = "NEW-123",
            role = role,
            lifecycleStatus = HouseholdInviteLifecycleStatus.Active,
            statusLabel = "Active now",
            supportingText = "Share this code to add a ${role.name.lowercase()}.",
            expiryLabel = "Expires Apr 19",
            usageLabel = "Unlimited uses",
            shareText = "Join with NEW-123.",
            isJoinable = true,
        )
        _familyState.value = _familyState.value.copy(
            snapshot = _familyState.value.snapshot.copy(activeInvite = invite),
        )
        return invite
    }

    override suspend fun changeMemberRole(memberId: String, newRole: HouseholdRole) {
        roleChanges += memberId to newRole
        _familyState.value = _familyState.value.copy(
            snapshot = _familyState.value.snapshot.copy(
                ownerUserId = if (newRole == HouseholdRole.Owner) memberId else _familyState.value.snapshot.ownerUserId,
                members = _familyState.value.snapshot.members.map { member ->
                    when {
                        newRole == HouseholdRole.Owner && member.id == _familyState.value.snapshot.ownerUserId ->
                            member.copy(role = HouseholdRole.Admin)

                        member.id == memberId -> member.copy(role = newRole)
                        else -> member
                    }
                },
            ),
        )
    }

    override suspend fun deactivateMember(memberId: String) {
        deactivatedMembers += memberId
        _familyState.value = _familyState.value.copy(
            snapshot = _familyState.value.snapshot.copy(
                activeSeatCount = (_familyState.value.snapshot.activeSeatCount - 1).coerceAtLeast(0),
                members = _familyState.value.snapshot.members.map { member ->
                    if (member.id != memberId) {
                        member
                    } else {
                        member.copy(
                            statusLabel = "Inactive seat",
                            activeChoresLabel = "This seat is paused and won't take new chores until it returns.",
                            householdImpact = "This seat is outside the active household flow until it is reactivated or removed.",
                            isActiveSeat = false,
                        )
                    }
                },
            ),
        )
    }

    override suspend fun removeMember(memberId: String) {
        removedMembers += memberId
        _familyState.value = _familyState.value.copy(
            snapshot = _familyState.value.snapshot.copy(
                activeSeatCount = (_familyState.value.snapshot.activeSeatCount - 1).coerceAtLeast(0),
                members = _familyState.value.snapshot.members.filterNot { it.id == memberId },
            ),
        )
    }

    override suspend fun leaveHousehold() {
        leaveCalls += 1
    }

    override suspend fun deleteHousehold() {
        deleteCalls += 1
        _familyState.value = FamilyRepositoryState(
            isLoading = false,
            snapshot = FamilySnapshot(
                householdName = "Your household",
                pulseLabel = "No active members yet",
                householdBadge = "Starting fresh",
                summaryMetrics = listOf(
                    DemoMetric("0", "Seats"),
                    DemoMetric("None", "Invite"),
                ),
            ),
        )
    }

    override suspend fun updateAvailability(
        memberId: String,
        mode: FamilyAvailabilityMode,
        startDate: String,
        endDate: String,
    ) {
        availabilityUpdates += AvailabilityUpdate(memberId, mode, startDate, endDate)
    }

    override suspend fun clearAvailability(memberId: String) {
        clearedAvailabilityMembers += memberId
    }
}

private class FakeFamilySessionRepository(
    role: HouseholdRole,
    userId: String = "owner-1",
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

    override suspend fun signOut() = Unit

    override suspend fun resetDemo() = Unit
}

private data class AvailabilityUpdate(
    val memberId: String,
    val mode: FamilyAvailabilityMode,
    val startDate: String,
    val endDate: String,
)

private class FakeFamilySubscriptionRepository : SubscriptionRepository {
    override val snapshot: StateFlow<SubscriptionSnapshot> = MutableStateFlow(
        SubscriptionSnapshot(
            isConfigured = true,
            isLoading = false,
            hasChorePilotPro = true,
        ),
    )

    override suspend fun refresh() = Unit

    override suspend fun purchasePlan(planId: SubscriptionPlanId): Result<Unit> = Result.success(Unit)

    override suspend fun restorePurchases(): Result<Unit> = Result.success(Unit)

    override fun clearError() = Unit
}

private class FakeFamilyVibratorUtil : VibratorUtil {
    override fun triggerVibration() = Unit

    override fun triggerVibrationSuccess() = Unit

    override fun cancelVibration() = Unit
}

private class FakeSystemTextActions : SystemTextActions {
    override fun copyText(text: String) = Unit

    override fun shareText(text: String) = Unit
}
