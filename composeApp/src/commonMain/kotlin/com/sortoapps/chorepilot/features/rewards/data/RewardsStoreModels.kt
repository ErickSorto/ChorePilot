package com.sortoapps.chorepilot.features.rewards.data

import com.sortoapps.chorepilot.features.chores.domain.ChoreItemStatus
import com.sortoapps.chorepilot.features.rewards.domain.RewardDefinition
import com.sortoapps.chorepilot.features.rewards.domain.RewardPointEvent
import com.sortoapps.chorepilot.features.rewards.domain.RewardRedemption
import com.sortoapps.chorepilot.features.rewards.domain.RewardRedemptionStatus
import com.sortoapps.chorepilot.features.rewards.domain.RewardsCelebration
import com.sortoapps.chorepilot.features.rewards.domain.RewardsFamilyRules
import kotlinx.serialization.Serializable

@Serializable
internal data class RewardsPersistedHouseholdStore(
    val rules: RewardsFamilyRules = RewardsFamilyRules(),
    val rewardDefinitions: List<RewardDefinition> = emptyList(),
    val pointEvents: List<RewardPointEvent> = emptyList(),
    val redemptionHistory: List<RewardRedemption> = emptyList(),
    val trackedChores: Map<String, TrackedRewardChoreState> = emptyMap(),
    val celebration: RewardsCelebration? = null,
)

@Serializable
internal data class TrackedRewardChoreState(
    val status: ChoreItemStatus,
    val proofPhotoCount: Int,
    val completedByName: String,
    val assigneeName: String,
    val assigneeId: String,
)

internal fun RewardsPersistedHouseholdStore.hasSeedData(): Boolean =
    rewardDefinitions.isNotEmpty() ||
        pointEvents.isNotEmpty() ||
        redemptionHistory.isNotEmpty() ||
        trackedChores.isNotEmpty()

internal fun RewardsPersistedHouseholdStore.withDefaults(): RewardsPersistedHouseholdStore =
    copy(
        rules = rules.copy(redemptionRequiresApproval = false),
        rewardDefinitions = if (rewardDefinitions.isEmpty()) defaultRewardDefinitions() else rewardDefinitions,
        redemptionHistory = redemptionHistory.map { redemption ->
            if (redemption.status == RewardRedemptionStatus.PendingApproval) {
                redemption.copy(status = RewardRedemptionStatus.Approved)
            } else {
                redemption
            }
        },
    )

internal fun defaultRewardDefinitions(): List<RewardDefinition> = listOf(
    RewardDefinition(
        id = "reward-movie-picker",
        title = "Movie picker",
        description = "Choose the next family movie night without debate.",
        costPoints = 30,
    ),
    RewardDefinition(
        id = "reward-dessert-choice",
        title = "Dessert choice",
        description = "Pick the weekend dessert or sweet treat.",
        costPoints = 45,
    ),
    RewardDefinition(
        id = "reward-skip-one-chore",
        title = "Skip one chore",
        description = "Trade points for one lighter weeknight handoff.",
        costPoints = 65,
    ),
)
