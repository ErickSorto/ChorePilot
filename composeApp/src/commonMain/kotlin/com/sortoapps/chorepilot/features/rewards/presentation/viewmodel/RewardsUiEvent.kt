package com.sortoapps.chorepilot.features.rewards.presentation.viewmodel

sealed interface RewardsUiEvent {
    data object Refresh : RewardsUiEvent

    data object DismissError : RewardsUiEvent

    data object DismissCelebration : RewardsUiEvent

    data class CompletionPointsChanged(val value: Int) : RewardsUiEvent

    data class ProofSubmissionBonusChanged(val value: Int) : RewardsUiEvent

    data class ProofApprovalBonusChanged(val value: Int) : RewardsUiEvent

    data class DraftTitleChanged(val value: String) : RewardsUiEvent

    data class DraftDescriptionChanged(val value: String) : RewardsUiEvent

    data class DraftCostChanged(val value: String) : RewardsUiEvent

    data object SaveRewardDefinition : RewardsUiEvent

    data class DeleteRewardDefinition(val rewardId: String) : RewardsUiEvent

    data class RedeemReward(val rewardId: String) : RewardsUiEvent

}
