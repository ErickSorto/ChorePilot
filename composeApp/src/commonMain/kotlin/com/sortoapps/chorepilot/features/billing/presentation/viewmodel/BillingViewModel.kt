package com.sortoapps.chorepilot.features.billing.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.purchases.kmp.models.PurchasesTransactionException
import com.sortoapps.chorepilot.core.platform.VibratorUtil
import com.sortoapps.chorepilot.core.revenuecat.RevenueCatConfig
import com.sortoapps.chorepilot.core.revenuecat.SubscriptionPlanId
import com.sortoapps.chorepilot.core.revenuecat.SubscriptionPlanSnapshot
import com.sortoapps.chorepilot.core.revenuecat.SubscriptionRepository
import com.sortoapps.chorepilot.core.revenuecat.SubscriptionSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BillingViewModel(
    private val subscriptionRepository: SubscriptionRepository,
    private val vibratorUtil: VibratorUtil,
) : ViewModel() {
    private val _uiState = MutableStateFlow(BillingUiState())
    val uiState: StateFlow<BillingUiState> = _uiState.asStateFlow()

    private var latestSnapshot = SubscriptionSnapshot()
    private var pendingPurchasePlanId: SubscriptionPlanId? = null
    private var feedbackMessage: BillingMessageUiModel? = null

    init {
        viewModelScope.launch {
            subscriptionRepository.snapshot.collectLatest { snapshot ->
                latestSnapshot = snapshot
                if (snapshot.errorMessage != null) {
                    feedbackMessage = BillingMessageUiModel(
                        tone = BillingMessageTone.Error,
                        title = "Something went wrong",
                        body = snapshot.errorMessage,
                    )
                }
                render()
            }
        }

        viewModelScope.launch {
            subscriptionRepository.refresh()
        }
    }

    fun onEvent(event: BillingUiEvent) {
        when (event) {
            BillingUiEvent.TriggerVibration -> vibratorUtil.triggerVibration()
            BillingUiEvent.TriggerVibrationSuccess -> vibratorUtil.triggerVibrationSuccess()
            BillingUiEvent.ErrorDismissed -> {
                feedbackMessage = null
                subscriptionRepository.clearError()
                render()
            }

            BillingUiEvent.RestorePurchasesClicked -> restorePurchases()
            is BillingUiEvent.PurchasePlanClicked -> purchasePlan(event.planId)
        }
    }

    private fun purchasePlan(planId: SubscriptionPlanId) {
        if (latestSnapshot.hasChorePilotPro) {
            feedbackMessage = BillingMessageUiModel(
                tone = BillingMessageTone.Success,
                title = "Already active",
                body = "ChorePilot Pro is already active for this household. Use store management to change or review the plan.",
            )
            render()
            return
        }

        if (latestSnapshot.isLoading) return

        pendingPurchasePlanId = planId
        feedbackMessage = null
        render()

        viewModelScope.launch {
            subscriptionRepository.purchasePlan(planId)
                .onSuccess {
                    pendingPurchasePlanId = null
                    val hasPro = subscriptionRepository.snapshot.value.hasChorePilotPro
                    feedbackMessage = BillingMessageUiModel(
                        tone = if (hasPro) BillingMessageTone.Success else BillingMessageTone.Info,
                        title = if (hasPro) "Pro is active" else "Purchase finished",
                        body = if (hasPro) {
                            "ChorePilot Pro is now active for everyone in this household."
                        } else {
                            "Your purchase finished. Shared household access should appear as soon as the subscription sync completes."
                        },
                    )
                    if (hasPro) {
                        vibratorUtil.triggerVibrationSuccess()
                    }
                    render()
                }
                .onFailure { error ->
                    pendingPurchasePlanId = null
                    val message = when {
                        error is PurchasesTransactionException && error.userCancelled -> null
                        error.message?.contains("cancel", ignoreCase = true) == true -> null
                        else -> error.message ?: "We couldn't start your subscription yet."
                    }
                    feedbackMessage = message?.let {
                        BillingMessageUiModel(
                            tone = BillingMessageTone.Error,
                            title = "Purchase failed",
                            body = it,
                        )
                    }
                    render()
                }
        }
    }

    private fun restorePurchases() {
        feedbackMessage = null
        render()

        viewModelScope.launch {
            subscriptionRepository.restorePurchases()
                .onSuccess {
                    val hasPro = subscriptionRepository.snapshot.value.hasChorePilotPro
                    feedbackMessage = BillingMessageUiModel(
                        tone = if (hasPro) BillingMessageTone.Success else BillingMessageTone.Info,
                        title = if (hasPro) "Restore successful" else "Nothing to restore",
                        body = if (hasPro) {
                            "ChorePilot Pro is active for this household again."
                        } else {
                            "We didn't find an earlier ChorePilot Pro purchase to restore for this store account."
                        },
                    )
                    if (hasPro) {
                        vibratorUtil.triggerVibrationSuccess()
                    }
                    render()
                }
                .onFailure { error ->
                    feedbackMessage = BillingMessageUiModel(
                        tone = BillingMessageTone.Error,
                        title = "Restore failed",
                        body = error.message ?: "We couldn't restore purchases right now.",
                    )
                    render()
                }
        }
    }

    private fun render() {
        _uiState.value = latestSnapshot.toUiState(
            pendingPurchasePlanId = pendingPurchasePlanId,
            feedback = feedbackMessage,
        )
    }
}

private fun SubscriptionSnapshot.toUiState(
    pendingPurchasePlanId: SubscriptionPlanId?,
    feedback: BillingMessageUiModel?,
): BillingUiState {
    val accessState = when {
        isLoading -> BillingAccessState.Loading
        purchaserHasChorePilotPro -> BillingAccessState.PurchaserPro
        hasChorePilotPro -> BillingAccessState.HouseholdPro
        else -> BillingAccessState.NoPlan
    }

    val headline = when (accessState) {
        BillingAccessState.Loading -> "Checking your household plan"
        BillingAccessState.PurchaserPro -> "ChorePilot Pro is active"
        BillingAccessState.HouseholdPro -> "This household already has Pro"
        BillingAccessState.NoPlan -> "Start ChorePilot Pro"
    }

    val supportingText = when (accessState) {
        BillingAccessState.Loading -> "We’re loading your store offerings and household access."
        BillingAccessState.PurchaserPro -> "You’re the active purchaser for this household, so billing and management live here."
        BillingAccessState.HouseholdPro -> "Another active purchaser is currently keeping this household unlocked."
        BillingAccessState.NoPlan -> "Pick a monthly or yearly plan, restore an earlier purchase, or manage an existing subscription from your store account."
    }

    val householdStatusLabel = if (hasChorePilotPro) {
        "Reports, discovery mode, and proof-required chores are unlocked for everyone in this household."
    } else {
        "This household is still on the free plan."
    }

    val purchaserStatusLabel = when {
        purchaserHasChorePilotPro -> "This signed-in account currently holds the active store entitlement."
        isLoading -> "Checking whether this signed-in account already owns ChorePilot Pro."
        else -> "This signed-in account does not currently hold the active store entitlement."
    }

    val activePlanSummary = when {
        purchaserHasChorePilotPro && hasYearlyProductActive() ->
            "Yearly plan active"
        purchaserHasChorePilotPro && activeProductIds.isNotEmpty() ->
            "Monthly plan active"
        hasChorePilotPro ->
            "Household Pro active"
        else ->
            "No active plan"
    }

    val planPurchaseBlocked = hasChorePilotPro
    val plans = listOfNotNull(
        monthlyPlan?.toBillingPlanUiModel(
            isBlocked = planPurchaseBlocked,
            isLoading = pendingPurchasePlanId == SubscriptionPlanId.Monthly,
        ),
        yearlyPlan?.toBillingPlanUiModel(
            isBlocked = planPurchaseBlocked,
            isLoading = pendingPurchasePlanId == SubscriptionPlanId.Yearly,
        ),
    )

    return BillingUiState(
        isLoading = isLoading,
        isRestoring = isRestoring,
        accessState = accessState,
        hasChorePilotPro = hasChorePilotPro,
        headline = headline,
        supportingText = supportingText,
        householdStatusLabel = householdStatusLabel,
        purchaserStatusLabel = purchaserStatusLabel,
        plans = plans,
        restoreLabel = if (isRestoring) "Restoring..." else "Restore purchases",
        canRestore = !isRestoring && !isLoading,
        managementUrl = managementUrl,
        feedback = feedback,
        activePlanSummary = activePlanSummary,
        offeringUnavailableLabel = if (!isLoading && plans.isEmpty()) {
            "Plans aren’t available right now. Try refreshing Billing later or restore an earlier purchase."
        } else {
            ""
        },
        monthlyPlan = monthlyPlan,
        yearlyPlan = yearlyPlan,
    )
}

private fun SubscriptionSnapshot.hasYearlyProductActive(): Boolean {
    val configuredYearlyIds = buildSet {
        add(RevenueCatConfig.yearlyProductId)
        yearlyPlan?.storeProductId
            ?.takeIf(String::isNotBlank)
            ?.let(::add)
    }

    return activeProductIds.any { productId ->
        productId in configuredYearlyIds ||
            productId.contains("year", ignoreCase = true) ||
            productId.contains("annual", ignoreCase = true)
    }
}

private fun SubscriptionPlanSnapshot.toBillingPlanUiModel(
    isBlocked: Boolean,
    isLoading: Boolean,
): BillingPlanUiModel = BillingPlanUiModel(
    planId = id,
    title = title,
    priceLabel = priceLabel,
    billingPeriodLabel = billingPeriodLabel,
    perMonthLabel = perMonthLabel,
    highlightLabel = highlightLabel,
    purchaseLabel = when {
        isLoading -> "Starting..."
        isBlocked -> "Already active"
        id == SubscriptionPlanId.Yearly -> "Start yearly"
        else -> "Start monthly"
    },
    isEnabled = !isBlocked && !isLoading,
    isLoading = isLoading,
)
