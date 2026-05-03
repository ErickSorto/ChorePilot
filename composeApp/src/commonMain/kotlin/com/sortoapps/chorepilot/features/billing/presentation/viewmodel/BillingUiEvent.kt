package com.sortoapps.chorepilot.features.billing.presentation.viewmodel

import com.sortoapps.chorepilot.core.revenuecat.SubscriptionPlanId

sealed interface BillingUiEvent {
    data object TriggerVibration : BillingUiEvent
    data object TriggerVibrationSuccess : BillingUiEvent
    data class PurchasePlanClicked(val planId: SubscriptionPlanId) : BillingUiEvent
    data object RestorePurchasesClicked : BillingUiEvent
    data object ErrorDismissed : BillingUiEvent
}
