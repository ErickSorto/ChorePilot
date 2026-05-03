package com.sortoapps.chorepilot.features.rewards.presentation.viewmodel

import com.sortoapps.chorepilot.core.demo.DemoTone
import com.sortoapps.chorepilot.features.auth.domain.HouseholdRole

data class RewardsUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val householdName: String = "River House",
    val viewerName: String = "Erick",
    val viewerRole: HouseholdRole = HouseholdRole.Member,
    val canManageRewards: Boolean = false,
    val hero: RewardsHeroUiModel = RewardsHeroUiModel(),
    val leaderboard: List<RewardsMemberProgressUiModel> = emptyList(),
    val rewards: List<RewardsCatalogItemUiModel> = emptyList(),
    val history: List<RewardsHistoryRowUiModel> = emptyList(),
    val rules: RewardsRulesUiModel = RewardsRulesUiModel(),
    val editor: RewardsEditorUiModel = RewardsEditorUiModel(),
    val celebration: RewardsCelebrationUiModel? = null,
    val emptyTitle: String = "",
    val emptyBody: String = "",
)

data class RewardsHeroUiModel(
    val title: String = "Household rewards",
    val body: String = "Turn consistent follow-through into something the whole household can feel.",
    val pointsLabel: String = "0 pts",
    val streakLabel: String = "0-day streak",
    val supportingLabel: String = "No reward progress yet",
    val nextRewardLabel: String = "Add a reward to get started",
)

data class RewardsMemberProgressUiModel(
    val memberId: String,
    val memberName: String,
    val avatarSeed: String,
    val pointsLabel: String,
    val streakLabel: String,
    val supportingText: String,
    val tone: DemoTone,
    val isViewer: Boolean,
)

data class RewardsCatalogItemUiModel(
    val id: String,
    val title: String,
    val description: String,
    val costLabel: String,
    val availabilityLabel: String,
    val canRedeem: Boolean,
    val canDelete: Boolean,
)

data class RewardsHistoryRowUiModel(
    val title: String,
    val body: String,
    val timestampLabel: String,
    val pointsLabel: String,
    val tone: DemoTone,
)

data class RewardsRulesUiModel(
    val pointsPerCompletion: Int = 12,
    val proofSubmissionBonus: Int = 4,
    val proofApprovalBonus: Int = 6,
)

data class RewardsEditorUiModel(
    val title: String = "",
    val description: String = "",
    val costText: String = "25",
    val saveEnabled: Boolean = false,
)

data class RewardsCelebrationUiModel(
    val title: String,
    val body: String,
)
