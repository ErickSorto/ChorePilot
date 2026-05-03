package com.sortoapps.chorepilot.features.premium.presentation.viewmodel

import com.revenuecat.purchases.kmp.models.Offering
import com.sortoapps.chorepilot.core.revenuecat.SubscriptionPlanSnapshot
import org.jetbrains.compose.resources.DrawableResource

enum class PremiumStageId {
    Promise,
    TrialClarity,
    Unlocks,
    OfferSheet,
    GiftReveal,
    OneTimeOffer,
}

enum class PremiumPlanOption {
    Monthly,
    Annual,
}

enum class PremiumEntryMode {
    FeatureGate,
    InitialOnboarding,
}

data class PremiumCalloutUiModel(
    val title: String,
    val description: String,
)

data class PremiumTimelineItemUiModel(
    val label: String,
    val title: String,
    val description: String,
)

data class PremiumStageUiModel(
    val id: PremiumStageId,
    val screenLabel: String,
    val boardTitle: String,
    val boardSubtitle: String,
    val eyebrow: String,
    val headline: String,
    val body: String,
    val phaseLabels: List<String>,
    val activePhaseIndex: Int,
    val image: DrawableResource? = null,
    val callouts: List<PremiumCalloutUiModel> = emptyList(),
    val timeline: List<PremiumTimelineItemUiModel> = emptyList(),
    val primaryActionLabel: String,
    val secondaryActionLabel: String? = null,
    val emphasizeOffer: Boolean = false,
    val warmVisual: Boolean = false,
)

data class PremiumUiState(
    val displayName: String = "Erick",
    val selectedPlan: PremiumPlanOption = PremiumPlanOption.Annual,
    val currentStageId: PremiumStageId = PremiumStageId.TrialClarity,
    val stages: List<PremiumStageUiModel> = emptyList(),
    val monthlyPlan: SubscriptionPlanSnapshot? = null,
    val yearlyPlan: SubscriptionPlanSnapshot? = null,
    val offering: Offering? = null,
    val hasChorePilotPro: Boolean = false,
    val isSubscriptionLoading: Boolean = true,
    val isPurchaseInProgress: Boolean = false,
    val subscriptionErrorMessage: String? = null,
    val subscriptionStatusMessage: String? = null,
    val shouldCompleteFlow: Boolean = false,
) {
    val currentStage: PremiumStageUiModel
        get() = stages.first { it.id == currentStageId }

    val stageIndex: Int
        get() = stages.indexOfFirst { it.id == currentStageId }.coerceAtLeast(0)

    val progressLabel: String
        get() = "${stageIndex + 1} / ${stages.size}"

    val progressFraction: Float
        get() = (stageIndex + 1).toFloat() / stages.size.coerceAtLeast(1)

    val canGoBackInFlow: Boolean
        get() = currentStageId != PremiumStageId.Promise
}
