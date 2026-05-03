package com.sortoapps.chorepilot.core.firebase

import com.sortoapps.chorepilot.features.auth.domain.AuthFlowResult
import com.sortoapps.chorepilot.features.auth.domain.HouseholdRole
import com.sortoapps.chorepilot.features.auth.domain.InvitePreview
import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import com.sortoapps.chorepilot.features.auth.domain.SessionSnapshot
import com.sortoapps.chorepilot.features.chorecapture.domain.DiscoveryAnalysisRepository
import com.sortoapps.chorepilot.features.chorecapture.domain.DiscoveryAnalysisResult
import com.sortoapps.chorepilot.features.chorecapture.domain.DiscoverySourceImage
import com.sortoapps.chorepilot.features.chores.domain.AvailabilitySuggestion
import com.sortoapps.chorepilot.features.chores.domain.ChoreDraft
import com.sortoapps.chorepilot.features.chores.domain.ChoreItem
import com.sortoapps.chorepilot.features.chores.domain.ChoreRepository
import com.sortoapps.chorepilot.features.chores.domain.ChoreScheduleDay
import com.sortoapps.chorepilot.features.chores.domain.HouseholdSchedulingMember
import com.sortoapps.chorepilot.features.chores.domain.ProofEvidence
import com.sortoapps.chorepilot.features.chores.domain.ProofUploadPayload
import com.sortoapps.chorepilot.features.chores.domain.ProofUploadResult
import com.sortoapps.chorepilot.features.chores.domain.RoomDefinition
import com.sortoapps.chorepilot.features.family.domain.FamilyAvailabilityMode
import com.sortoapps.chorepilot.features.family.domain.FamilyInviteSnapshot
import com.sortoapps.chorepilot.features.family.domain.FamilyRepository
import com.sortoapps.chorepilot.features.family.domain.FamilyRepositoryState
import com.sortoapps.chorepilot.features.reports.domain.ReportsBarPoint
import com.sortoapps.chorepilot.features.reports.domain.ReportsLegendItem
import com.sortoapps.chorepilot.features.reports.domain.ReportsNavigationSnapshot
import com.sortoapps.chorepilot.features.reports.domain.ReportsPeriodSelection
import com.sortoapps.chorepilot.features.reports.domain.ReportsPeriodSnapshot
import com.sortoapps.chorepilot.features.reports.domain.ReportsRepository
import com.sortoapps.chorepilot.features.rewards.domain.RewardDefinitionDraft
import com.sortoapps.chorepilot.features.rewards.domain.RewardsFamilyRules
import com.sortoapps.chorepilot.features.rewards.domain.RewardsRepository
import com.sortoapps.chorepilot.features.rewards.domain.RewardsRepositoryState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FirebaseUnavailableSessionRepository(
    private val firebaseAvailability: FirebaseAvailability,
) : SessionRepository {
    private val unavailableMessage = firebaseAvailability.message
        ?: "Firebase is not configured for this build."

    private val _session = MutableStateFlow(
        SessionSnapshot(
            signedIn = false,
            serviceErrorMessage = unavailableMessage,
        ),
    )

    override val session: StateFlow<SessionSnapshot> = _session.asStateFlow()

    override suspend fun refreshSession() = Unit

    override suspend fun completeOnboarding(displayName: String) = Unit

    override suspend fun signIn(
        email: String,
        password: String,
        displayNameHint: String,
    ): AuthFlowResult = error(unavailableMessage)

    override suspend fun signInWithGoogle(
        idToken: String,
        accessToken: String?,
        displayNameHint: String,
        emailHint: String,
    ): AuthFlowResult = error(unavailableMessage)

    override suspend fun createHousehold(
        displayName: String,
        householdName: String,
        householdSizeLabel: String,
    ): AuthFlowResult = error(unavailableMessage)

    override suspend fun joinHousehold(
        displayName: String,
        inviteCode: String,
    ): AuthFlowResult = error(unavailableMessage)

    override suspend fun previewInvite(inviteCode: String): InvitePreview? = null

    override suspend fun dismissInitialPremiumFlow() = Unit

    override suspend fun switchActiveHousehold(householdId: String) = error(unavailableMessage)

    override suspend fun signOut() = Unit

    override suspend fun resetDemo() = Unit
}

class FirebaseUnavailableChoreRepository(
    private val firebaseAvailability: FirebaseAvailability,
) : ChoreRepository {
    private val unavailableMessage = firebaseAvailability.message
        ?: "Firebase is not configured for this build."

    private val _chores = MutableStateFlow(emptyList<ChoreItem>())
    override val chores: StateFlow<List<ChoreItem>> = _chores.asStateFlow()

    private val _rooms = MutableStateFlow(emptyList<RoomDefinition>())
    override val rooms: StateFlow<List<RoomDefinition>> = _rooms.asStateFlow()

    private val _householdMembers = MutableStateFlow(emptyList<HouseholdSchedulingMember>())
    override val householdMembers: StateFlow<List<HouseholdSchedulingMember>> = _householdMembers.asStateFlow()

    override fun getChores(): List<ChoreItem> = emptyList()

    override fun getChore(id: String, day: ChoreScheduleDay?): ChoreItem? = null

    override fun getLatestProofForChore(
        choreId: String,
        day: ChoreScheduleDay?,
    ): ProofEvidence? = null

    override fun getDraft(id: String): ChoreDraft? = null

    override fun getScheduledChores(day: ChoreScheduleDay): List<ChoreItem> = emptyList()

    override suspend fun getAvailabilitySuggestions(days: List<ChoreScheduleDay>): List<AvailabilitySuggestion> = emptyList()

    override suspend fun refresh() = Unit

    override suspend fun saveChore(draft: ChoreDraft): String = error(unavailableMessage)

    override suspend fun approvePendingChore(choreId: String) = error(unavailableMessage)

    override suspend fun rejectPendingChore(choreId: String, reason: String) = error(unavailableMessage)

    override suspend fun deleteChore(choreId: String) = error(unavailableMessage)

    override suspend fun toggleCompletion(
        choreId: String,
        viewerName: String,
        allowCompletingForSomeoneElse: Boolean,
        day: ChoreScheduleDay?,
    ) = error(unavailableMessage)

    override suspend fun submitProof(
        choreId: String,
        payload: ProofUploadPayload,
        day: ChoreScheduleDay?,
    ): ProofUploadResult = error(unavailableMessage)

    override suspend fun approveProof(choreId: String, day: ChoreScheduleDay?) = error(unavailableMessage)

    override suspend fun rejectProof(choreId: String, reason: String, day: ChoreScheduleDay?) = error(unavailableMessage)

    override suspend fun acceptAvailabilitySuggestion(suggestion: AvailabilitySuggestion) = error(unavailableMessage)

    override suspend fun dismissAvailabilitySuggestion(suggestion: AvailabilitySuggestion) = error(unavailableMessage)
}

class FirebaseUnavailableDiscoveryAnalysisRepository(
    private val firebaseAvailability: FirebaseAvailability,
) : DiscoveryAnalysisRepository {
    private val unavailableMessage = firebaseAvailability.message
        ?: "Firebase is not configured for this build."

    override suspend fun analyzeRoomPhoto(
        payload: ProofUploadPayload,
    ): DiscoveryAnalysisResult = error(unavailableMessage)

    override suspend fun persistAcceptedRoomPhoto(
        discoverySessionId: String,
        payload: ProofUploadPayload,
    ): DiscoverySourceImage = error(unavailableMessage)
}

class FirebaseUnavailableFamilyRepository(
    private val firebaseAvailability: FirebaseAvailability,
) : FamilyRepository {
    private val unavailableMessage = firebaseAvailability.message
        ?: "Firebase is not configured for this build."

    private val _state = MutableStateFlow(
        FamilyRepositoryState(
            isLoading = false,
            errorMessage = unavailableMessage,
        ),
    )

    override val familyState: StateFlow<FamilyRepositoryState> = _state.asStateFlow()

    override suspend fun refresh() = Unit

    override suspend fun generateInvite(role: HouseholdRole): FamilyInviteSnapshot = error(unavailableMessage)

    override suspend fun cancelInvite() = error(unavailableMessage)

    override suspend fun rotateInvite(role: HouseholdRole): FamilyInviteSnapshot = error(unavailableMessage)

    override suspend fun changeMemberRole(memberId: String, newRole: HouseholdRole) = error(unavailableMessage)

    override suspend fun deactivateMember(memberId: String) = error(unavailableMessage)

    override suspend fun removeMember(memberId: String) = error(unavailableMessage)

    override suspend fun leaveHousehold() = error(unavailableMessage)

    override suspend fun deleteHousehold() = error(unavailableMessage)

    override suspend fun updateAvailability(
        memberId: String,
        mode: FamilyAvailabilityMode,
        startDate: String,
        endDate: String,
    ) = error(unavailableMessage)

    override suspend fun clearAvailability(memberId: String) = error(unavailableMessage)
}

class FirebaseUnavailableRewardsRepository(
    private val firebaseAvailability: FirebaseAvailability,
) : RewardsRepository {
    private val unavailableMessage = firebaseAvailability.message
        ?: "Firebase is not configured for this build."

    private val _state = MutableStateFlow(
        RewardsRepositoryState(
            isLoading = false,
            errorMessage = unavailableMessage,
        ),
    )

    override val state: StateFlow<RewardsRepositoryState> = _state.asStateFlow()

    override suspend fun refresh() = Unit

    override suspend fun updateRules(rules: RewardsFamilyRules) = error(unavailableMessage)

    override suspend fun saveRewardDefinition(draft: RewardDefinitionDraft) = error(unavailableMessage)

    override suspend fun deleteRewardDefinition(rewardId: String) = error(unavailableMessage)

    override suspend fun redeemReward(rewardId: String) = error(unavailableMessage)

    override suspend fun dismissCelebration() = Unit
}

class FirebaseUnavailableReportsRepository(
    private val firebaseAvailability: FirebaseAvailability,
) : ReportsRepository {
    private val unavailableMessage = firebaseAvailability.message
        ?: "Firebase is not configured for this build."

    override fun getReportsPeriodSnapshot(
        range: com.sortoapps.chorepilot.features.reports.presentation.viewmodel.ReportsRange,
        selection: ReportsPeriodSelection,
    ): ReportsPeriodSnapshot = ReportsPeriodSnapshot(
        headline = "Firebase setup required",
        supportingText = unavailableMessage,
        navigation = ReportsNavigationSnapshot(
            title = "Unavailable",
            supportingText = "Firebase configuration missing",
            canGoForward = false,
        ),
        rangeSummary = unavailableMessage,
        chartTitle = "Reports unavailable",
        chartSubtitle = unavailableMessage,
        metrics = emptyList(),
        bars = emptyList<ReportsBarPoint>(),
        legendItems = listOf(
            ReportsLegendItem(
                title = "Configuration required",
                body = unavailableMessage,
            ),
        ),
        emptyTitle = "Reports unavailable",
        emptyBody = unavailableMessage,
        emptyActionHint = "Add the platform Firebase config files and try again.",
        lockTitle = "Reports unavailable",
        lockBody = unavailableMessage,
        lockBenefits = emptyList(),
    )
}
