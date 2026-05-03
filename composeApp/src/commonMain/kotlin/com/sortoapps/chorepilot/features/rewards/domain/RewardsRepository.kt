package com.sortoapps.chorepilot.features.rewards.domain

import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
enum class RewardRedemptionStatus {
    PendingApproval,
    Approved,
    Rejected,
}

@Serializable
enum class RewardPointEventType {
    Completion,
    ProofSubmitted,
    ProofApproved,
    RedemptionReserved,
    RedemptionRejected,
}

@Serializable
data class RewardsFamilyRules(
    val pointsPerCompletion: Int = 12,
    val proofSubmissionBonus: Int = 4,
    val proofApprovalBonus: Int = 6,
    val redemptionRequiresApproval: Boolean = false,
)

@Serializable
data class RewardDefinition(
    val id: String,
    val title: String,
    val description: String,
    val costPoints: Int,
    val enabled: Boolean = true,
)

data class RewardDefinitionDraft(
    val title: String = "",
    val description: String = "",
    val costPoints: Int = 25,
)

@Serializable
data class RewardPointEvent(
    val id: String,
    val memberId: String,
    val memberName: String,
    val type: RewardPointEventType,
    val pointsDelta: Int,
    val dateKey: String,
    val createdAtEpochMs: Long,
    val choreId: String = "",
    val rewardId: String = "",
    val description: String,
)

@Serializable
data class RewardRedemption(
    val id: String,
    val rewardId: String,
    val rewardTitle: String,
    val memberId: String,
    val memberName: String,
    val costPoints: Int,
    val status: RewardRedemptionStatus,
    val redeemedAtEpochMs: Long,
    val redeemedDateKey: String,
)

data class RewardsMemberProgress(
    val memberId: String,
    val memberName: String,
    val totalEarnedPoints: Int,
    val availablePoints: Int,
    val currentStreak: Int,
    val completedChores: Int,
    val pendingProofCount: Int,
    val overdueCount: Int,
    val nextRewardGap: Int,
)

@Serializable
data class RewardsCelebration(
    val title: String,
    val body: String,
)

data class RewardsSnapshot(
    val householdName: String,
    val viewerId: String,
    val viewerName: String,
    val rules: RewardsFamilyRules,
    val availableRewards: List<RewardDefinition>,
    val memberProgress: List<RewardsMemberProgress>,
    val redemptionHistory: List<RewardRedemption>,
    val recentPointEvents: List<RewardPointEvent>,
    val celebration: RewardsCelebration? = null,
)

data class RewardsRepositoryState(
    val isLoading: Boolean = true,
    val snapshot: RewardsSnapshot? = null,
    val errorMessage: String? = null,
)

interface RewardsRepository {
    val state: StateFlow<RewardsRepositoryState>

    suspend fun refresh()

    suspend fun updateRules(rules: RewardsFamilyRules)

    suspend fun saveRewardDefinition(draft: RewardDefinitionDraft)

    suspend fun deleteRewardDefinition(rewardId: String)

    suspend fun redeemReward(rewardId: String)

    suspend fun dismissCelebration()
}
