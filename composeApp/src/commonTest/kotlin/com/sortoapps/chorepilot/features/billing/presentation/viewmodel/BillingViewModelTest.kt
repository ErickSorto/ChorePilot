package com.sortoapps.chorepilot.features.billing.presentation.viewmodel

import com.sortoapps.chorepilot.core.platform.VibratorUtil
import com.sortoapps.chorepilot.core.revenuecat.SubscriptionPlanId
import com.sortoapps.chorepilot.core.revenuecat.SubscriptionPlanSnapshot
import com.sortoapps.chorepilot.core.revenuecat.SubscriptionRepository
import com.sortoapps.chorepilot.core.revenuecat.SubscriptionSnapshot
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class BillingViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun nonPurchaserStillSeesHouseholdProStatus() = runTest(dispatcher) {
        val viewModel = BillingViewModel(
            subscriptionRepository = FakeBillingSubscriptionRepository(
                SubscriptionSnapshot(
                    isConfigured = true,
                    isLoading = false,
                    purchaserHasChorePilotPro = false,
                    householdHasChorePilotPro = true,
                    hasChorePilotPro = true,
                    appUserId = "member-2",
                ),
            ),
            vibratorUtil = FakeBillingVibratorUtil(),
        )

        advanceUntilIdle()

        assertEquals(BillingAccessState.HouseholdPro, viewModel.uiState.value.accessState)
        assertEquals("Household Pro active", viewModel.uiState.value.activePlanSummary)
        assertTrue(viewModel.uiState.value.householdStatusLabel.contains("unlocked"))
    }

    @Test
    fun purchaseFailureShowsUserFacingMessage() = runTest(dispatcher) {
        val repository = FakeBillingSubscriptionRepository(
            initialSnapshot = baseSnapshot(),
            purchaseResult = Result.failure(IllegalStateException("Store offline")),
        )
        val viewModel = BillingViewModel(
            subscriptionRepository = repository,
            vibratorUtil = FakeBillingVibratorUtil(),
        )

        advanceUntilIdle()
        viewModel.onEvent(BillingUiEvent.PurchasePlanClicked(SubscriptionPlanId.Yearly))
        advanceUntilIdle()

        assertEquals(BillingMessageTone.Error, viewModel.uiState.value.feedback?.tone)
        assertEquals("Purchase failed", viewModel.uiState.value.feedback?.title)
        assertEquals("Store offline", viewModel.uiState.value.feedback?.body)
        assertNull(viewModel.uiState.value.plans.first { it.planId == SubscriptionPlanId.Yearly }.takeIf { it.isLoading })
    }

    @Test
    fun restoreSuccessShowsSuccessMessageWhenHouseholdBecomesPro() = runTest(dispatcher) {
        val repository = FakeBillingSubscriptionRepository(
            initialSnapshot = baseSnapshot(),
            restoreResult = Result.success(Unit),
            grantHouseholdProOnRestore = true,
        )
        val viewModel = BillingViewModel(
            subscriptionRepository = repository,
            vibratorUtil = FakeBillingVibratorUtil(),
        )

        advanceUntilIdle()
        viewModel.onEvent(BillingUiEvent.RestorePurchasesClicked)
        advanceUntilIdle()

        assertEquals(BillingMessageTone.Success, viewModel.uiState.value.feedback?.tone)
        assertEquals("Restore successful", viewModel.uiState.value.feedback?.title)
        assertTrue(viewModel.uiState.value.hasChorePilotPro)
    }

    @Test
    fun yearlySummaryUsesResolvedProductIdNotStringGuessing() = runTest(dispatcher) {
        val yearlyProductId = "cp_pro_4999"
        val monthlyProductId = "cp_pro_699"
        val viewModel = BillingViewModel(
            subscriptionRepository = FakeBillingSubscriptionRepository(
                SubscriptionSnapshot(
                    isConfigured = true,
                    isLoading = false,
                    purchaserHasChorePilotPro = true,
                    householdHasChorePilotPro = false,
                    hasChorePilotPro = true,
                    activeProductIds = setOf(yearlyProductId),
                    monthlyPlan = baseSnapshot().monthlyPlan?.copy(storeProductId = monthlyProductId),
                    yearlyPlan = baseSnapshot().yearlyPlan?.copy(storeProductId = yearlyProductId),
                ),
            ),
            vibratorUtil = FakeBillingVibratorUtil(),
        )

        advanceUntilIdle()

        assertEquals("Yearly plan active", viewModel.uiState.value.activePlanSummary)
    }

    @Test
    fun alreadyActiveProDoesNotStartAnotherPurchase() = runTest(dispatcher) {
        val repository = FakeBillingSubscriptionRepository(
            initialSnapshot = baseSnapshot().copy(
                purchaserHasChorePilotPro = true,
                householdHasChorePilotPro = true,
                hasChorePilotPro = true,
            ),
        )
        val viewModel = BillingViewModel(
            subscriptionRepository = repository,
            vibratorUtil = FakeBillingVibratorUtil(),
        )

        advanceUntilIdle()
        viewModel.onEvent(BillingUiEvent.PurchasePlanClicked(SubscriptionPlanId.Monthly))
        advanceUntilIdle()

        assertEquals(0, repository.purchaseCalls)
        assertEquals(BillingMessageTone.Success, viewModel.uiState.value.feedback?.tone)
        assertEquals("Already active", viewModel.uiState.value.feedback?.title)
    }
}

private fun baseSnapshot(): SubscriptionSnapshot = SubscriptionSnapshot(
    isConfigured = true,
    isLoading = false,
    monthlyPlan = SubscriptionPlanSnapshot(
        id = SubscriptionPlanId.Monthly,
        packageIdentifier = "\$rc_monthly",
        storeProductId = "monthly",
        title = "Monthly",
        priceLabel = "\$6.99",
        billingPeriodLabel = "Billed monthly",
    ),
    yearlyPlan = SubscriptionPlanSnapshot(
        id = SubscriptionPlanId.Yearly,
        packageIdentifier = "\$rc_yearly",
        storeProductId = "yearly",
        title = "Yearly",
        priceLabel = "\$49.99",
        billingPeriodLabel = "Billed yearly",
        perMonthLabel = "\$4.16 per month equivalent",
        highlightLabel = "Best value",
    ),
)

private class FakeBillingSubscriptionRepository(
    initialSnapshot: SubscriptionSnapshot,
    private val purchaseResult: Result<Unit> = Result.success(Unit),
    private val restoreResult: Result<Unit> = Result.success(Unit),
    private val grantHouseholdProOnRestore: Boolean = false,
) : SubscriptionRepository {
    private val snapshotFlow = MutableStateFlow(initialSnapshot)
    var purchaseCalls: Int = 0
        private set

    override val snapshot: StateFlow<SubscriptionSnapshot> = snapshotFlow

    override suspend fun refresh() = Unit

    override suspend fun purchasePlan(planId: SubscriptionPlanId): Result<Unit> {
        purchaseCalls += 1
        return purchaseResult
    }

    override suspend fun restorePurchases(): Result<Unit> = restoreResult.onSuccess {
        if (grantHouseholdProOnRestore) {
            snapshotFlow.value = snapshotFlow.value.copy(
                householdHasChorePilotPro = true,
                hasChorePilotPro = true,
            )
        }
    }

    override fun clearError() {
        snapshotFlow.value = snapshotFlow.value.copy(errorMessage = null)
    }
}

private class FakeBillingVibratorUtil : VibratorUtil {
    override fun triggerVibration() = Unit

    override fun triggerVibrationSuccess() = Unit

    override fun cancelVibration() = Unit
}
