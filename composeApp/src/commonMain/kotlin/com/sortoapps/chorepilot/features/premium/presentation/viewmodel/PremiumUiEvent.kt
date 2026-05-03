package com.sortoapps.chorepilot.features.premium.presentation.viewmodel

sealed interface PremiumUiEvent {
    data object TriggerVibration : PremiumUiEvent
    data object TriggerVibrationSuccess : PremiumUiEvent

    data object BackInFlowClicked : PremiumUiEvent
    data object PrimaryActionClicked : PremiumUiEvent
    data object SecondaryActionClicked : PremiumUiEvent
    data object RestorePurchasesClicked : PremiumUiEvent
    data object PaywallDismissed : PremiumUiEvent
    data object PaywallPurchaseCompleted : PremiumUiEvent
    data object FlowCompletionHandled : PremiumUiEvent
    data object SubscriptionErrorDismissed : PremiumUiEvent

    data class PlanSelected(val option: PremiumPlanOption) : PremiumUiEvent
}
