package com.sortoapps.chorepilot.features.premium.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chorepilot.composeapp.generated.resources.cheerful_squirrel_golden_bell
import chorepilot.composeapp.generated.resources.Res
import chorepilot.composeapp.generated.resources.premium_feature_locked_squirrel
import chorepilot.composeapp.generated.resources.premium_gift_reveal
import chorepilot.composeapp.generated.resources.premium_offer_support
import chorepilot.composeapp.generated.resources.premium_promise_hero
import chorepilot.composeapp.generated.resources.premium_trial_gift_squirrel
import chorepilot.composeapp.generated.resources.premium_unlocks_squirrel
import com.revenuecat.purchases.kmp.models.PurchasesTransactionException
import com.sortoapps.chorepilot.core.platform.VibratorUtil
import com.sortoapps.chorepilot.core.revenuecat.SubscriptionPlanId
import com.sortoapps.chorepilot.core.revenuecat.SubscriptionPlanSnapshot
import com.sortoapps.chorepilot.core.revenuecat.SubscriptionRepository
import com.sortoapps.chorepilot.core.revenuecat.SubscriptionSnapshot
import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PremiumViewModel(
    sessionRepository: SessionRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val vibratorUtil: VibratorUtil,
    private val entryMode: PremiumEntryMode = PremiumEntryMode.FeatureGate,
) : ViewModel() {
    private val displayName = sessionRepository.session.value.displayName.ifBlank { "Erick" }

    private val _uiState = MutableStateFlow(
        PremiumUiState(
            displayName = displayName,
            currentStageId = initialStage(entryMode),
            stages = buildStages(displayName = displayName),
        ),
    )
    val uiState: StateFlow<PremiumUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            subscriptionRepository.snapshot.collectLatest { snapshot ->
                _uiState.update { current ->
                    current.applySubscriptionSnapshot(snapshot = snapshot, displayName = displayName)
                }
            }
        }

        viewModelScope.launch {
            subscriptionRepository.refresh()
        }
    }

    fun onEvent(event: PremiumUiEvent) {
        when (event) {
            PremiumUiEvent.TriggerVibration -> vibratorUtil.triggerVibration()
            PremiumUiEvent.TriggerVibrationSuccess -> vibratorUtil.triggerVibrationSuccess()
            PremiumUiEvent.BackInFlowClicked -> {
                _uiState.update { state ->
                    state.copy(currentStageId = previousStage(state.currentStageId))
                }
            }

            PremiumUiEvent.PrimaryActionClicked -> {
                when (val stageId = _uiState.value.currentStageId) {
                    PremiumStageId.OfferSheet -> {
                        if (_uiState.value.hasChorePilotPro) {
                            _uiState.update { it.copy(shouldCompleteFlow = true) }
                        } else {
                            purchasePlan(_uiState.value.selectedPlan.toSubscriptionPlanId())
                        }
                    }

                    PremiumStageId.OneTimeOffer -> {
                        if (_uiState.value.hasChorePilotPro) {
                            _uiState.update { it.copy(shouldCompleteFlow = true) }
                        } else {
                            purchasePlan(SubscriptionPlanId.Yearly)
                        }
                    }

                    else -> _uiState.update { state ->
                        state.copy(currentStageId = nextPrimaryStage(stageId))
                    }
                }
            }

            PremiumUiEvent.SecondaryActionClicked -> {
                _uiState.update { state ->
                    state.copy(currentStageId = secondaryStage(state.currentStageId))
                }
            }

            PremiumUiEvent.RestorePurchasesClicked -> {
                viewModelScope.launch {
                    _uiState.update {
                        it.copy(
                            isPurchaseInProgress = true,
                            subscriptionErrorMessage = null,
                            subscriptionStatusMessage = null,
                        )
                    }
                    subscriptionRepository.restorePurchases()
                        .onSuccess {
                            val hasPro = subscriptionRepository.snapshot.value.hasChorePilotPro
                            if (hasPro) {
                                vibratorUtil.triggerVibrationSuccess()
                            }
                            _uiState.update {
                                it.copy(
                                    isPurchaseInProgress = false,
                                    shouldCompleteFlow = hasPro,
                                    subscriptionStatusMessage = if (hasPro) {
                                        "ChorePilot Pro is active for this household."
                                    } else {
                                        "No previous ChorePilot Pro purchase was found to restore."
                                    },
                                )
                            }
                        }
                        .onFailure { error ->
                            _uiState.update {
                                it.copy(
                                    isPurchaseInProgress = false,
                                    subscriptionErrorMessage = error.message ?: "We couldn't restore purchases yet.",
                                    subscriptionStatusMessage = null,
                                )
                            }
                        }
                }
            }

            PremiumUiEvent.PaywallDismissed -> Unit

            PremiumUiEvent.PaywallPurchaseCompleted -> Unit

            PremiumUiEvent.FlowCompletionHandled -> {
                _uiState.update { state -> state.copy(shouldCompleteFlow = false) }
            }

            PremiumUiEvent.SubscriptionErrorDismissed -> {
                subscriptionRepository.clearError()
                _uiState.update {
                    it.copy(
                        subscriptionErrorMessage = null,
                        subscriptionStatusMessage = null,
                    )
                }
            }

            is PremiumUiEvent.PlanSelected -> {
                _uiState.update { state -> state.copy(selectedPlan = event.option) }
            }
        }
    }

    private fun purchasePlan(planId: SubscriptionPlanId) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isPurchaseInProgress = true,
                    subscriptionErrorMessage = null,
                    subscriptionStatusMessage = null,
                )
            }

            subscriptionRepository.purchasePlan(planId)
                .onSuccess {
                    val hasPro = subscriptionRepository.snapshot.value.hasChorePilotPro
                    if (hasPro) {
                        vibratorUtil.triggerVibrationSuccess()
                    }
                    _uiState.update {
                        it.copy(
                            isPurchaseInProgress = false,
                            shouldCompleteFlow = hasPro,
                            subscriptionStatusMessage = if (hasPro) {
                                "ChorePilot Pro is active for this household."
                            } else {
                                "Your purchase finished, but ChorePilot Pro is not active yet."
                            },
                        )
                    }
                }
                .onFailure { error ->
                    val message = when {
                        error is PurchasesTransactionException && error.userCancelled -> null
                        error.message?.contains("cancel", ignoreCase = true) == true -> null
                        else -> error.message ?: "We couldn't start membership yet."
                    }
                    _uiState.update {
                        it.copy(
                            isPurchaseInProgress = false,
                            subscriptionErrorMessage = message,
                            subscriptionStatusMessage = null,
                        )
                    }
                }
        }
    }

    private fun previousStage(stageId: PremiumStageId): PremiumStageId = when (stageId) {
        PremiumStageId.TrialClarity -> PremiumStageId.TrialClarity
        PremiumStageId.Promise -> PremiumStageId.TrialClarity
        PremiumStageId.Unlocks -> PremiumStageId.Promise
        PremiumStageId.OfferSheet -> PremiumStageId.Unlocks
        PremiumStageId.GiftReveal -> PremiumStageId.OfferSheet
        PremiumStageId.OneTimeOffer -> PremiumStageId.GiftReveal
    }

    private fun nextPrimaryStage(stageId: PremiumStageId): PremiumStageId = when (stageId) {
        PremiumStageId.TrialClarity -> PremiumStageId.Promise
        PremiumStageId.Promise -> PremiumStageId.Unlocks
        PremiumStageId.Unlocks -> PremiumStageId.OfferSheet
        PremiumStageId.OfferSheet -> PremiumStageId.OfferSheet
        PremiumStageId.GiftReveal -> PremiumStageId.OneTimeOffer
        PremiumStageId.OneTimeOffer -> PremiumStageId.OneTimeOffer
    }

    private fun secondaryStage(stageId: PremiumStageId): PremiumStageId = when (stageId) {
        PremiumStageId.OfferSheet -> PremiumStageId.GiftReveal
        PremiumStageId.OneTimeOffer -> PremiumStageId.OfferSheet
        else -> stageId
    }
}

private fun initialStage(entryMode: PremiumEntryMode): PremiumStageId = when (entryMode) {
    PremiumEntryMode.FeatureGate -> PremiumStageId.TrialClarity
    PremiumEntryMode.InitialOnboarding -> PremiumStageId.Promise
}

private fun PremiumUiState.applySubscriptionSnapshot(
    snapshot: SubscriptionSnapshot,
    displayName: String,
): PremiumUiState = copy(
    monthlyPlan = snapshot.monthlyPlan,
    yearlyPlan = snapshot.yearlyPlan,
    offering = snapshot.offering,
    hasChorePilotPro = snapshot.hasChorePilotPro,
    isSubscriptionLoading = snapshot.isLoading,
    subscriptionErrorMessage = snapshot.errorMessage ?: subscriptionErrorMessage,
    stages = buildStages(
        displayName = displayName,
        monthlyPlan = snapshot.monthlyPlan,
        yearlyPlan = snapshot.yearlyPlan,
        hasChorePilotPro = snapshot.hasChorePilotPro,
    ),
)

private fun buildStages(
    displayName: String,
    monthlyPlan: SubscriptionPlanSnapshot? = null,
    yearlyPlan: SubscriptionPlanSnapshot? = null,
    hasChorePilotPro: Boolean = false,
): List<PremiumStageUiModel> {
    val monthlyPrice = monthlyPlan?.priceLabel ?: "Monthly pricing loads from RevenueCat"
    val yearlyPrice = yearlyPlan?.priceLabel ?: "Yearly pricing loads from RevenueCat"

    return listOf(
        PremiumStageUiModel(
            id = PremiumStageId.TrialClarity,
            screenLabel = "Screen 01",
            boardTitle = "Premium locked",
            boardSubtitle = "Start with the blocked feature, then show the value.",
            eyebrow = "",
            headline = "This feature is locked by ChorePilot Pro.",
            body = "Unlock AI room scans, deeper household insights, proof-required chores, and the smarter tools that keep everything calm.",
            phaseLabels = listOf("Free trial", "Promise", "Unlocks"),
            activePhaseIndex = 0,
            image = Res.drawable.premium_feature_locked_squirrel,
            timeline = emptyList(),
            primaryActionLabel = "Continue",
        ),
        PremiumStageUiModel(
            id = PremiumStageId.Promise,
            screenLabel = "Screen 02",
            boardTitle = "Promise",
            boardSubtitle = "Start with the simple value first.",
            eyebrow = "",
            headline = "$displayName, keep your home calm before chores pile up.",
            body = "ChorePilot Pro keeps routines clear, fair, and beautifully in sync.",
            phaseLabels = listOf("Free trial", "Promise", "Unlocks"),
            activePhaseIndex = 1,
            image = Res.drawable.premium_promise_hero,
            callouts = listOf(
                PremiumCalloutUiModel(
                    title = "Clearer fairness",
                    description = "Recurring work stops drifting to one person.",
                ),
                PremiumCalloutUiModel(
                    title = "Weekly clarity",
                    description = "See what needs attention before the house feels behind.",
                ),
            ),
            primaryActionLabel = "Continue",
        ),
        PremiumStageUiModel(
            id = PremiumStageId.Unlocks,
            screenLabel = "Screen 03",
            boardTitle = "Premium unlocks",
            boardSubtitle = "Keep this short and useful.",
            eyebrow = "",
            headline = "Stronger tools for the whole household.",
            body = "A calmer home, with better help.",
            phaseLabels = listOf("Promise", "Unlocks", "Offer sheet"),
            activePhaseIndex = 1,
            image = Res.drawable.premium_unlocks_squirrel,
            callouts = listOf(
                PremiumCalloutUiModel(
                    title = "Deeper reports",
                    description = "Spot weekly and yearly patterns fast.",
                ),
                PremiumCalloutUiModel(
                    title = "Fairer balance",
                    description = "Share chores more evenly at home.",
                ),
                PremiumCalloutUiModel(
                    title = "Proof + AI",
                    description = "Unlock proof chores and AI drafting.",
                ),
                PremiumCalloutUiModel(
                    title = "AI scanner",
                    description = "Catch messy rooms faster.",
                ),
                PremiumCalloutUiModel(
                    title = "House visualizer",
                    description = "See the whole home at a glance.",
                ),
                PremiumCalloutUiModel(
                    title = "AI Alerts",
                    description = "Smarter nudges right when they help.",
                ),
            ),
            primaryActionLabel = "See the offer",
        ),
        PremiumStageUiModel(
            id = PremiumStageId.OfferSheet,
            screenLabel = "Screen 04",
            boardTitle = "Offer sheet",
            boardSubtitle = "This is the first place the price appears.",
            eyebrow = "",
            headline = "",
            body = "",
            phaseLabels = listOf("Unlocks", "Offer sheet", "Rescue"),
            activePhaseIndex = 1,
            image = Res.drawable.cheerful_squirrel_golden_bell,
            callouts = emptyList(),
            primaryActionLabel = if (hasChorePilotPro) "Continue with Pro" else "Start membership",
            emphasizeOffer = true,
        ),
        PremiumStageUiModel(
            id = PremiumStageId.GiftReveal,
            screenLabel = "Rescue 01",
            boardTitle = "Gift reveal",
            boardSubtitle = "Make rescue feel special, not desperate.",
            eyebrow = "Before you leave",
            headline = "Before you leave, here’s the simplest annual option.",
            body = "If you want one clean decision, we can keep it to the yearly plan only.",
            phaseLabels = listOf("Offer sheet", "Gift reveal", "One-time offer"),
            activePhaseIndex = 1,
            image = Res.drawable.premium_gift_reveal,
            callouts = listOf(
                PremiumCalloutUiModel(
                    title = "Included if you continue",
                    description = "First-week launch guide, reward template ideas, and a calm reset checklist.",
                ),
            ),
            primaryActionLabel = "See annual option",
            warmVisual = true,
        ),
        PremiumStageUiModel(
            id = PremiumStageId.OneTimeOffer,
            screenLabel = "Rescue 02",
            boardTitle = "One-time annual offer",
            boardSubtitle = "Annual only, simple CTA, no fake urgency.",
            eyebrow = "One-time annual option",
            headline = if (hasChorePilotPro) {
                "Your household already has ChorePilot Pro."
            } else {
                "Keep the whole household on one steady plan."
            },
            body = if (hasChorePilotPro) "Already unlocked" else yearlyPrice,
            phaseLabels = listOf("Gift reveal", "One-time offer", "Decision"),
            activePhaseIndex = 1,
            callouts = listOf(
                PremiumCalloutUiModel(
                    title = "Annual only",
                    description = "One clean decision, no fragmented upgrade path.",
                ),
                PremiumCalloutUiModel(
                    title = "Cancel if it is not right",
                    description = "Try the first week, then decide if it earns a place in the home.",
                ),
            ),
            primaryActionLabel = if (hasChorePilotPro) "Continue with Pro" else "Start yearly membership",
            secondaryActionLabel = "Go back to the main offer",
        ),
    )
}

private fun PremiumPlanOption.toSubscriptionPlanId(): SubscriptionPlanId = when (this) {
    PremiumPlanOption.Monthly -> SubscriptionPlanId.Monthly
    PremiumPlanOption.Annual -> SubscriptionPlanId.Yearly
}
