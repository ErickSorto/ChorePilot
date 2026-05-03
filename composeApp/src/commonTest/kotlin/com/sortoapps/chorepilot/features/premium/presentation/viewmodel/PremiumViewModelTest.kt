package com.sortoapps.chorepilot.features.premium.presentation.viewmodel

import com.sortoapps.chorepilot.core.platform.VibratorUtil
import com.sortoapps.chorepilot.core.revenuecat.SubscriptionPlanId
import com.sortoapps.chorepilot.core.revenuecat.SubscriptionRepository
import com.sortoapps.chorepilot.core.revenuecat.SubscriptionSnapshot
import com.sortoapps.chorepilot.features.auth.domain.AuthFlowResult
import com.sortoapps.chorepilot.features.auth.domain.InvitePreview
import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import com.sortoapps.chorepilot.features.auth.domain.SessionSnapshot
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
class PremiumViewModelTest {
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
    fun purchaseSuccessWithEntitlementCompletesExactlyOnce() = runTest(dispatcher) {
        val subscriptionRepository = FakePremiumSubscriptionRepository(
            purchaseResult = Result.success(Unit),
            hasPro = false,
            grantEntitlementOnPurchase = true,
        )
        val viewModel = PremiumViewModel(
            sessionRepository = FakePremiumSessionRepository(),
            subscriptionRepository = subscriptionRepository,
            vibratorUtil = FakePremiumVibratorUtil(),
        )

        advanceUntilIdle()
        openOfferSheet(viewModel)
        viewModel.onEvent(PremiumUiEvent.PrimaryActionClicked)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.shouldCompleteFlow)
        viewModel.onEvent(PremiumUiEvent.FlowCompletionHandled)
        assertFalse(viewModel.uiState.value.shouldCompleteFlow)
    }

    @Test
    fun purchaseSuccessWithoutEntitlementDoesNotComplete() = runTest(dispatcher) {
        val subscriptionRepository = FakePremiumSubscriptionRepository(
            purchaseResult = Result.success(Unit),
            hasPro = false,
        )
        val viewModel = PremiumViewModel(
            sessionRepository = FakePremiumSessionRepository(),
            subscriptionRepository = subscriptionRepository,
            vibratorUtil = FakePremiumVibratorUtil(),
        )

        advanceUntilIdle()
        openOfferSheet(viewModel)
        viewModel.onEvent(PremiumUiEvent.PrimaryActionClicked)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.shouldCompleteFlow)
        assertEquals(
            "Your purchase finished, but ChorePilot Pro is not active yet.",
            viewModel.uiState.value.subscriptionStatusMessage,
        )
    }

    @Test
    fun purchaserEntitlementCompletesFlowBeforeHouseholdSyncRoundsBack() = runTest(dispatcher) {
        val subscriptionRepository = FakePremiumSubscriptionRepository(
            purchaseResult = Result.success(Unit),
            hasPro = false,
            grantEntitlementOnPurchase = true,
            grantHouseholdSyncOnPurchase = false,
        )
        val viewModel = PremiumViewModel(
            sessionRepository = FakePremiumSessionRepository(),
            subscriptionRepository = subscriptionRepository,
            vibratorUtil = FakePremiumVibratorUtil(),
        )

        advanceUntilIdle()
        openOfferSheet(viewModel)
        viewModel.onEvent(PremiumUiEvent.PrimaryActionClicked)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.shouldCompleteFlow)
        assertTrue(viewModel.uiState.value.hasChorePilotPro)
        assertEquals(
            "ChorePilot Pro is active for this household.",
            viewModel.uiState.value.subscriptionStatusMessage,
        )
    }

    @Test
    fun restoreSuccessWithEntitlementCompletes() = runTest(dispatcher) {
        val subscriptionRepository = FakePremiumSubscriptionRepository(
            restoreResult = Result.success(Unit),
            hasPro = false,
            grantEntitlementOnRestore = true,
        )
        val viewModel = PremiumViewModel(
            sessionRepository = FakePremiumSessionRepository(),
            subscriptionRepository = subscriptionRepository,
            vibratorUtil = FakePremiumVibratorUtil(),
        )

        advanceUntilIdle()
        viewModel.onEvent(PremiumUiEvent.RestorePurchasesClicked)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.shouldCompleteFlow)
        assertFalse(viewModel.uiState.value.isPurchaseInProgress)
    }

    @Test
    fun restoreSuccessWithoutEntitlementShowsNonErrorStatus() = runTest(dispatcher) {
        val subscriptionRepository = FakePremiumSubscriptionRepository(
            restoreResult = Result.success(Unit),
            hasPro = false,
        )
        val viewModel = PremiumViewModel(
            sessionRepository = FakePremiumSessionRepository(),
            subscriptionRepository = subscriptionRepository,
            vibratorUtil = FakePremiumVibratorUtil(),
        )

        advanceUntilIdle()
        viewModel.onEvent(PremiumUiEvent.RestorePurchasesClicked)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.shouldCompleteFlow)
        assertEquals(
            "No previous ChorePilot Pro purchase was found to restore.",
            viewModel.uiState.value.subscriptionStatusMessage,
        )
        assertNull(viewModel.uiState.value.subscriptionErrorMessage)
    }

    @Test
    fun purchaseCancelClearsProgressWithoutError() = runTest(dispatcher) {
        val subscriptionRepository = FakePremiumSubscriptionRepository(
            purchaseResult = Result.failure(IllegalStateException("User cancelled purchase")),
            hasPro = false,
        )
        val viewModel = PremiumViewModel(
            sessionRepository = FakePremiumSessionRepository(),
            subscriptionRepository = subscriptionRepository,
            vibratorUtil = FakePremiumVibratorUtil(),
        )

        advanceUntilIdle()
        openOfferSheet(viewModel)
        viewModel.onEvent(PremiumUiEvent.PrimaryActionClicked)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isPurchaseInProgress)
        assertNull(viewModel.uiState.value.subscriptionErrorMessage)
    }

    @Test
    fun purchaseFailureShowsErrorAndClearsProgress() = runTest(dispatcher) {
        val subscriptionRepository = FakePremiumSubscriptionRepository(
            purchaseResult = Result.failure(IllegalStateException("Store offline")),
            hasPro = false,
        )
        val viewModel = PremiumViewModel(
            sessionRepository = FakePremiumSessionRepository(),
            subscriptionRepository = subscriptionRepository,
            vibratorUtil = FakePremiumVibratorUtil(),
        )

        advanceUntilIdle()
        openOfferSheet(viewModel)
        viewModel.onEvent(PremiumUiEvent.PrimaryActionClicked)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isPurchaseInProgress)
        assertEquals("Store offline", viewModel.uiState.value.subscriptionErrorMessage)
    }

    @Test
    fun nonPurchaserInProHouseholdStillCompletesFlow() = runTest(dispatcher) {
        val viewModel = PremiumViewModel(
            sessionRepository = FakePremiumSessionRepository(),
            subscriptionRepository = FakePremiumSubscriptionRepository(
                hasPro = true,
                purchaserHasPro = false,
                householdHasPro = true,
            ),
            vibratorUtil = FakePremiumVibratorUtil(),
        )

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hasChorePilotPro)
        assertTrue(viewModel.uiState.value.currentStage.primaryActionLabel.contains("Continue", ignoreCase = true))
    }

    @Test
    fun initialOnboardingSkipsFeatureLockedStage() = runTest(dispatcher) {
        val viewModel = PremiumViewModel(
            sessionRepository = FakePremiumSessionRepository(),
            subscriptionRepository = FakePremiumSubscriptionRepository(hasPro = false),
            vibratorUtil = FakePremiumVibratorUtil(),
            entryMode = PremiumEntryMode.InitialOnboarding,
        )

        advanceUntilIdle()

        assertEquals(PremiumStageId.Promise, viewModel.uiState.value.currentStageId)
        assertFalse(viewModel.uiState.value.currentStage.headline.contains("locked", ignoreCase = true))
    }
}

private fun openOfferSheet(viewModel: PremiumViewModel) {
    repeat(3) {
        viewModel.onEvent(PremiumUiEvent.PrimaryActionClicked)
    }
}

private class FakePremiumSessionRepository : SessionRepository {
    override val session: StateFlow<SessionSnapshot> = MutableStateFlow(
        SessionSnapshot(displayName = "Erick", signedIn = true),
    )

    override suspend fun refreshSession() = Unit
    override suspend fun completeOnboarding(displayName: String) = Unit
    override suspend fun signIn(email: String, password: String, displayNameHint: String): AuthFlowResult = AuthFlowResult.Main
    override suspend fun signInWithGoogle(
        idToken: String,
        accessToken: String?,
        displayNameHint: String,
        emailHint: String,
    ): AuthFlowResult = AuthFlowResult.Main
    override suspend fun createHousehold(
        displayName: String,
        householdName: String,
        householdSizeLabel: String,
    ): AuthFlowResult = AuthFlowResult.Main
    override suspend fun joinHousehold(displayName: String, inviteCode: String): AuthFlowResult = AuthFlowResult.Main
    override suspend fun previewInvite(inviteCode: String): InvitePreview? = null
    override suspend fun dismissInitialPremiumFlow() = Unit
    override suspend fun signOut() = Unit
    override suspend fun resetDemo() = Unit
}

private class FakePremiumSubscriptionRepository(
    private val purchaseResult: Result<Unit> = Result.success(Unit),
    private val restoreResult: Result<Unit> = Result.success(Unit),
    hasPro: Boolean,
    purchaserHasPro: Boolean = hasPro,
    householdHasPro: Boolean = hasPro,
    private val grantEntitlementOnPurchase: Boolean = false,
    private val grantEntitlementOnRestore: Boolean = false,
    private val grantHouseholdSyncOnPurchase: Boolean = true,
    private val grantHouseholdSyncOnRestore: Boolean = true,
) : SubscriptionRepository {
    private val snapshotFlow = MutableStateFlow(
        SubscriptionSnapshot(
            purchaserHasChorePilotPro = purchaserHasPro,
            householdHasChorePilotPro = householdHasPro,
            hasChorePilotPro = hasPro,
            isConfigured = true,
            isLoading = false,
        ),
    )

    override val snapshot: StateFlow<SubscriptionSnapshot> = snapshotFlow

    override suspend fun refresh() = Unit

    override suspend fun purchasePlan(planId: SubscriptionPlanId): Result<Unit> = purchaseResult.onSuccess {
        if (grantEntitlementOnPurchase) {
            snapshotFlow.value = snapshotFlow.value.copy(
                purchaserHasChorePilotPro = true,
                householdHasChorePilotPro = grantHouseholdSyncOnPurchase,
                hasChorePilotPro = true,
            )
        }
    }

    override suspend fun restorePurchases(): Result<Unit> = restoreResult.onSuccess {
        if (grantEntitlementOnRestore) {
            snapshotFlow.value = snapshotFlow.value.copy(
                purchaserHasChorePilotPro = true,
                householdHasChorePilotPro = grantHouseholdSyncOnRestore,
                hasChorePilotPro = true,
            )
        }
    }

    override fun clearError() = Unit
}

private class FakePremiumVibratorUtil : VibratorUtil {
    override fun triggerVibration() = Unit
    override fun triggerVibrationSuccess() = Unit
    override fun cancelVibration() = Unit
}
