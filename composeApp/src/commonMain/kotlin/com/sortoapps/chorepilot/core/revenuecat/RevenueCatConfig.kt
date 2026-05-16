package com.sortoapps.chorepilot.core.revenuecat

object RevenueCatConfig {
    internal const val debugApiKey = "test_RwBAwEyvcmQvyxwDLqrNbIbcrcu"
    val apiKey: String
        get() = RevenueCatPlatformConfig.apiKey

    const val entitlementId = "ChorePilot Pro"
    const val defaultOfferingId = "default"
    const val monthlyProductId = "chorepilot_pro_monthly:monthly"
    const val yearlyProductId = "chorepilot_pro_yearly:yearly"
    const val monthlyStoreSubscriptionId = "chorepilot_pro_monthly"
    const val yearlyStoreSubscriptionId = "chorepilot_pro_yearly"
    const val monthlyPackageId = "monthly"
    const val yearlyPackageId = "yearly"
}

expect object RevenueCatPlatformConfig {
    val apiKey: String
}
