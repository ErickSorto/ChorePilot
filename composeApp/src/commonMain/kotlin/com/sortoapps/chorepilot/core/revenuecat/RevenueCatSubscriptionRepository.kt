package com.sortoapps.chorepilot.core.revenuecat

import com.revenuecat.purchases.kmp.LogLevel
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesConfiguration
import com.revenuecat.purchases.kmp.PurchasesDelegate
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.Offerings
import com.revenuecat.purchases.kmp.models.Package
import com.revenuecat.purchases.kmp.models.PackageType
import com.revenuecat.purchases.kmp.models.PurchasesError
import com.revenuecat.purchases.kmp.models.PurchasesException
import com.revenuecat.purchases.kmp.models.StoreProduct
import com.revenuecat.purchases.kmp.models.StoreTransaction
import com.revenuecat.purchases.kmp.result.awaitCustomerInfoResult
import com.revenuecat.purchases.kmp.result.awaitLogInResult
import com.revenuecat.purchases.kmp.result.awaitLogOutResult
import com.revenuecat.purchases.kmp.result.awaitOfferingsResult
import com.revenuecat.purchases.kmp.ktx.awaitPurchase
import com.revenuecat.purchases.kmp.result.awaitRestoreResult
import com.sortoapps.chorepilot.core.firebase.FirebaseHouseholdStore
import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RevenueCatSubscriptionRepository(
    private val appScope: CoroutineScope,
    private val sessionRepository: SessionRepository,
    private val householdStore: FirebaseHouseholdStore,
) : SubscriptionRepository, PurchasesDelegate {
    private val configurationMutex = Mutex()
    private val _snapshot = MutableStateFlow(SubscriptionSnapshot())
    private var latestOfferings: Offerings? = null

    override val snapshot: StateFlow<SubscriptionSnapshot> = _snapshot.asStateFlow()

    init {
        appScope.launch {
            val initialUserId = currentRevenueCatAppUserId()
            ensureConfigured(initialUserId)
            synchronizeIdentity(initialUserId)
            refresh()
        }

        appScope.launch {
            sessionRepository.session
                .map { session ->
                    RevenueCatSessionState(
                        appUserId = session.userId.takeIf { session.signedIn && it.isNotBlank() }.orEmpty(),
                        householdId = session.householdId,
                        householdHasPro = session.householdHasChorePilotPro,
                    )
                }
                .distinctUntilChanged()
                .collect { sessionState ->
                    applyHouseholdAccess(sessionState.householdHasPro)
                    val appUserId = sessionState.appUserId.ifBlank { null }
                    ensureConfigured(appUserId)
                    synchronizeIdentity(appUserId)
                    refresh()
                }
        }
    }

    override suspend fun refresh() {
        ensureConfigured(currentRevenueCatAppUserId())
        _snapshot.update { state ->
            state.copy(
                isConfigured = Purchases.isConfigured,
                isLoading = true,
            )
        }

        val purchases = Purchases.sharedInstance
        coroutineScope {
            val offeringsDeferred = async { purchases.awaitOfferingsResult() }
            val customerInfoDeferred = async { purchases.awaitCustomerInfoResult() }

            val offeringsResult = offeringsDeferred.await()
            val customerInfoResult = customerInfoDeferred.await()

            offeringsResult
                .onSuccess { applyOfferings(it) }
                .onFailure { updateError(it) }

            customerInfoResult
                .onSuccess { customerInfo ->
                    val purchaserHasPro = applyCustomerInfo(customerInfo)
                    syncPurchaserEntitlement(purchaserHasPro)
                }
                .onFailure { updateError(it) }
        }

        _snapshot.update { state ->
            state.copy(
                isConfigured = Purchases.isConfigured,
                isLoading = false,
                appUserId = Purchases.sharedInstance.appUserID,
            )
        }
    }

    override suspend fun purchasePlan(planId: SubscriptionPlanId): Result<Unit> {
        ensureConfigured(currentRevenueCatAppUserId())
        val offering = latestOfferings?.resolveLiveOffering()
        val packageToPurchase = when (planId) {
            SubscriptionPlanId.Monthly -> offering?.resolveMonthlyPackage()
            SubscriptionPlanId.Yearly -> offering?.resolveYearlyPackage()
        }

        if (packageToPurchase == null) {
            val error = IllegalStateException("RevenueCat could not find the ${planId.name.lowercase()} package.")
            updateError(error)
            return Result.failure(error)
        }

        return runCatching {
            val purchase = Purchases.sharedInstance.awaitPurchase(packageToPurchase)
            val purchaserHasPro = applyCustomerInfo(purchase.customerInfo)
            syncPurchaserEntitlement(purchaserHasPro, propagateFailure = true)
            Unit
        }.onFailure { updateError(it) }
    }

    override suspend fun restorePurchases(): Result<Unit> {
        ensureConfigured(currentRevenueCatAppUserId())
        _snapshot.update { it.copy(isRestoring = true, errorMessage = null) }

        return Purchases.sharedInstance.awaitRestoreResult()
            .map { customerInfo ->
                val purchaserHasPro = applyCustomerInfo(customerInfo)
                syncPurchaserEntitlement(purchaserHasPro, propagateFailure = true)
                Unit
            }
            .onFailure { updateError(it) }
            .also {
                _snapshot.update { state -> state.copy(isRestoring = false) }
            }
    }

    override fun clearError() {
        _snapshot.update { it.copy(errorMessage = null) }
    }

    override fun onPurchasePromoProduct(
        product: StoreProduct,
        startPurchase: (
            onError: (error: PurchasesError, userCancelled: Boolean) -> Unit,
            onSuccess: (storeTransaction: StoreTransaction, customerInfo: CustomerInfo) -> Unit,
        ) -> Unit,
    ) {
        startPurchase(
            { error, _ -> updateError(error) },
            { _, customerInfo ->
                val purchaserHasPro = applyCustomerInfo(customerInfo)
                appScope.launch {
                    syncPurchaserEntitlement(purchaserHasPro)
                }
            },
        )
    }

    override fun onCustomerInfoUpdated(customerInfo: CustomerInfo) {
        val purchaserHasPro = applyCustomerInfo(customerInfo)
        appScope.launch {
            syncPurchaserEntitlement(purchaserHasPro)
        }
    }

    private suspend fun ensureConfigured(appUserId: String?) {
        configurationMutex.withLock {
            if (Purchases.isConfigured) {
                Purchases.sharedInstance.delegate = this
                _snapshot.update { state ->
                    state.copy(
                        isConfigured = true,
                        appUserId = Purchases.sharedInstance.appUserID,
                    )
                }
                return
            }

            Purchases.logLevel = LogLevel.INFO
            val configuration = PurchasesConfiguration.Builder(RevenueCatConfig.apiKey).apply {
                appUserId?.let { this.appUserId = it }
            }.build()

            Purchases.configure(configuration).delegate = this
            _snapshot.update { state ->
                state.copy(
                    isConfigured = true,
                    appUserId = Purchases.sharedInstance.appUserID,
                )
            }
        }
    }

    private suspend fun synchronizeIdentity(targetAppUserId: String?) {
        if (!Purchases.isConfigured) return

        val purchases = Purchases.sharedInstance
        when {
            !targetAppUserId.isNullOrBlank() && purchases.appUserID != targetAppUserId -> {
                purchases.awaitLogInResult(targetAppUserId)
                    .onSuccess { login ->
                        val purchaserHasPro = applyCustomerInfo(login.customerInfo)
                        syncPurchaserEntitlement(purchaserHasPro, propagateFailure = true)
                    }
                    .onFailure { updateError(it) }
            }

            targetAppUserId.isNullOrBlank() && !purchases.isAnonymous -> {
                purchases.awaitLogOutResult()
                    .onSuccess { customerInfo ->
                        applyCustomerInfo(customerInfo)
                        applyHouseholdAccess(false)
                    }
                    .onFailure { updateError(it) }
            }
        }

        _snapshot.update { state ->
            state.copy(
                appUserId = purchases.appUserID,
                isConfigured = true,
            )
        }
    }

    private fun applyOfferings(offerings: Offerings) {
        latestOfferings = offerings
        val currentOffering = offerings.resolveLiveOffering()
        val monthlyPlan = currentOffering?.resolveMonthlyPackage()?.toPlanSnapshot(SubscriptionPlanId.Monthly)
        val yearlyPlan = currentOffering?.resolveYearlyPackage()?.toPlanSnapshot(SubscriptionPlanId.Yearly)

        _snapshot.update { state ->
            state.copy(
                offeringId = currentOffering?.identifier.orEmpty(),
                offering = currentOffering,
                monthlyPlan = monthlyPlan,
                yearlyPlan = yearlyPlan,
                errorMessage = if (currentOffering == null) {
                    "RevenueCat did not return a usable offering."
                } else {
                    state.errorMessage
                },
            )
        }
    }

    private fun applyHouseholdAccess(householdHasPro: Boolean) {
        _snapshot.update { state ->
            state.copy(
                householdHasChorePilotPro = householdHasPro,
                hasChorePilotPro = purchaserOrHouseholdHasPro(
                    purchaserHasPro = state.purchaserHasChorePilotPro,
                    householdHasPro = householdHasPro,
                ),
            )
        }
    }

    private suspend fun syncPurchaserEntitlement(
        purchaserHasPro: Boolean,
        propagateFailure: Boolean = false,
    ) {
        val session = sessionRepository.session.value
        val userId = session.userId.takeIf { session.signedIn && it.isNotBlank() } ?: return
        val syncResult = runCatching {
            householdStore.syncPurchaserPremiumAcrossHouseholds(
                userId = userId,
                entitlementActive = purchaserHasPro,
            )
            sessionRepository.refreshSession()
            applyHouseholdAccess(sessionRepository.session.value.householdHasChorePilotPro)
        }

        syncResult.onFailure { updateError(it) }
        if (propagateFailure) {
            syncResult.getOrThrow()
        }
    }

    private fun applyCustomerInfo(customerInfo: CustomerInfo): Boolean {
        val purchaserHasPro = customerInfo.entitlements.active.containsKey(RevenueCatConfig.entitlementId)
        _snapshot.update { state ->
            state.copy(
                appUserId = Purchases.sharedInstance.appUserID,
                originalAppUserId = customerInfo.originalAppUserId,
                purchaserHasChorePilotPro = purchaserHasPro,
                hasChorePilotPro = purchaserOrHouseholdHasPro(
                    purchaserHasPro = purchaserHasPro,
                    householdHasPro = state.householdHasChorePilotPro,
                ),
                managementUrl = customerInfo.managementUrlString,
                activeProductIds = customerInfo.activeSubscriptions,
                errorMessage = null,
            )
        }
        return purchaserHasPro
    }

    private fun updateError(error: Any) {
        val message = when (error) {
            is PurchasesException -> error.underlyingErrorMessage
                ?.takeIf(String::isNotBlank)
                ?: error.message
            is PurchasesError -> error.underlyingErrorMessage
                ?.takeIf(String::isNotBlank)
                ?: error.message
            is Throwable -> error.message
            else -> null
        } ?: "RevenueCat hit a snag. Please try again."

        _snapshot.update { it.copy(errorMessage = message) }
    }

    private fun currentRevenueCatAppUserId(): String? {
        val session = sessionRepository.session.value
        return session.userId.takeIf { session.signedIn && it.isNotBlank() }
    }
}

private data class RevenueCatSessionState(
    val appUserId: String,
    val householdId: String,
    val householdHasPro: Boolean,
)

internal fun purchaserOrHouseholdHasPro(
    purchaserHasPro: Boolean,
    householdHasPro: Boolean,
): Boolean = purchaserHasPro || householdHasPro

private fun Offerings.resolveLiveOffering(): com.revenuecat.purchases.kmp.models.Offering? = current
    ?: getOffering(RevenueCatConfig.defaultOfferingId)
    ?: all.values.firstOrNull()

private fun com.revenuecat.purchases.kmp.models.Offering.resolveMonthlyPackage(): Package? = monthly
    ?: getPackage(RevenueCatConfig.monthlyPackageId)
    ?: availablePackages.firstOrNull { rcPackage ->
        rcPackage.packageType == PackageType.MONTHLY ||
            rcPackage.identifier.equals(RevenueCatConfig.monthlyPackageId, ignoreCase = true) ||
            rcPackage.storeProduct.id == RevenueCatConfig.monthlyProductId ||
            rcPackage.identifier.contains("month", ignoreCase = true) ||
            rcPackage.storeProduct.id.contains("month", ignoreCase = true)
    }

private fun com.revenuecat.purchases.kmp.models.Offering.resolveYearlyPackage(): Package? = annual
    ?: getPackage(RevenueCatConfig.yearlyPackageId)
    ?: availablePackages.firstOrNull { rcPackage ->
        rcPackage.packageType == PackageType.ANNUAL ||
            rcPackage.identifier.equals(RevenueCatConfig.yearlyPackageId, ignoreCase = true) ||
            rcPackage.storeProduct.id == RevenueCatConfig.yearlyProductId ||
            rcPackage.identifier.contains("year", ignoreCase = true) ||
            rcPackage.identifier.contains("annual", ignoreCase = true) ||
            rcPackage.storeProduct.id.contains("year", ignoreCase = true) ||
            rcPackage.storeProduct.id.contains("annual", ignoreCase = true)
    }

private fun Package.toPlanSnapshot(id: SubscriptionPlanId): SubscriptionPlanSnapshot {
    val product = storeProduct
    val title = when (id) {
        SubscriptionPlanId.Monthly -> "Monthly"
        SubscriptionPlanId.Yearly -> "Yearly"
    }

    return SubscriptionPlanSnapshot(
        id = id,
        packageIdentifier = identifier,
        storeProductId = product.id,
        title = title,
        priceLabel = product.price.formatted,
        billingPeriodLabel = when (id) {
            SubscriptionPlanId.Monthly -> "Billed monthly"
            SubscriptionPlanId.Yearly -> "Billed yearly"
        },
        perMonthLabel = if (id == SubscriptionPlanId.Yearly) {
            product.pricePerMonth?.formatted?.let { "$it per month equivalent" }
        } else {
            null
        },
        highlightLabel = if (id == SubscriptionPlanId.Yearly) "Best value" else null,
    )
}
