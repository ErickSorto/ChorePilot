package com.sortoapps.chorepilot.core.revenuecat

import com.revenuecat.purchases.kmp.models.Offering
import kotlinx.coroutines.flow.StateFlow

enum class SubscriptionPlanId {
    Monthly,
    Yearly,
}

data class SubscriptionPlanSnapshot(
    val id: SubscriptionPlanId,
    val packageIdentifier: String,
    val storeProductId: String,
    val title: String,
    val priceLabel: String,
    val billingPeriodLabel: String,
    val perMonthLabel: String? = null,
    val highlightLabel: String? = null,
)

data class SubscriptionSnapshot(
    val isConfigured: Boolean = false,
    val isLoading: Boolean = true,
    val isRestoring: Boolean = false,
    val appUserId: String = "",
    val originalAppUserId: String = "",
    val entitlementId: String = RevenueCatConfig.entitlementId,
    val offeringId: String = "",
    val offering: Offering? = null,
    val purchaserHasChorePilotPro: Boolean = false,
    val householdHasChorePilotPro: Boolean = false,
    val hasChorePilotPro: Boolean = false,
    val managementUrl: String? = null,
    val monthlyPlan: SubscriptionPlanSnapshot? = null,
    val yearlyPlan: SubscriptionPlanSnapshot? = null,
    val activeProductIds: Set<String> = emptySet(),
    val errorMessage: String? = null,
)

interface SubscriptionRepository {
    val snapshot: StateFlow<SubscriptionSnapshot>

    suspend fun refresh()

    suspend fun purchasePlan(planId: SubscriptionPlanId): Result<Unit>

    suspend fun restorePurchases(): Result<Unit>

    fun clearError()
}
