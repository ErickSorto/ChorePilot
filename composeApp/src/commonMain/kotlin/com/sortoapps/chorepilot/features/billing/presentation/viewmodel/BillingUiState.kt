package com.sortoapps.chorepilot.features.billing.presentation.viewmodel

import com.sortoapps.chorepilot.core.revenuecat.SubscriptionPlanId
import com.sortoapps.chorepilot.core.revenuecat.SubscriptionPlanSnapshot

enum class BillingAccessState {
    Loading,
    NoPlan,
    HouseholdPro,
    PurchaserPro,
}

enum class BillingMessageTone {
    Info,
    Success,
    Error,
}

data class BillingMessageUiModel(
    val tone: BillingMessageTone,
    val title: String,
    val body: String,
)

data class BillingPlanUiModel(
    val planId: SubscriptionPlanId,
    val title: String,
    val priceLabel: String,
    val billingPeriodLabel: String,
    val perMonthLabel: String? = null,
    val highlightLabel: String? = null,
    val purchaseLabel: String,
    val isEnabled: Boolean = true,
    val isLoading: Boolean = false,
)

data class BillingUiState(
    val isLoading: Boolean = true,
    val isRestoring: Boolean = false,
    val accessState: BillingAccessState = BillingAccessState.Loading,
    val hasChorePilotPro: Boolean = false,
    val headline: String = "Checking your plan",
    val supportingText: String = "We’re loading your current ChorePilot Pro access.",
    val householdStatusLabel: String = "",
    val purchaserStatusLabel: String = "",
    val plans: List<BillingPlanUiModel> = emptyList(),
    val restoreLabel: String = "Restore purchases",
    val canRestore: Boolean = true,
    val managementUrl: String? = null,
    val managementLabel: String = "Manage in store",
    val feedback: BillingMessageUiModel? = null,
    val activePlanSummary: String = "",
    val offeringUnavailableLabel: String = "",
    val monthlyPlan: SubscriptionPlanSnapshot? = null,
    val yearlyPlan: SubscriptionPlanSnapshot? = null,
) {
    val canManageInStore: Boolean
        get() = !managementUrl.isNullOrBlank()
}
