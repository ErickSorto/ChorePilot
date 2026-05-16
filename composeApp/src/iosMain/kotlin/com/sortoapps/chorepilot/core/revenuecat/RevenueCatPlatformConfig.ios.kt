package com.sortoapps.chorepilot.core.revenuecat

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform
import platform.Foundation.NSBundle

@OptIn(ExperimentalNativeApi::class)
actual object RevenueCatPlatformConfig {
    actual val apiKey: String = readInfoPlistRevenueCatApiKey()
        ?: RevenueCatConfig.debugApiKey.takeIf { Platform.isDebugBinary }
        .orEmpty()
}

private fun readInfoPlistRevenueCatApiKey(): String? {
    val value = NSBundle.mainBundle.objectForInfoDictionaryKey("RevenueCatApiKey") as? String
    return value?.takeUnless { apiKey ->
        apiKey.isBlank() || apiKey.contains("\$(")
    }
}
