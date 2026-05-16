package com.sortoapps.chorepilot.core.revenuecat

actual object RevenueCatPlatformConfig {
    actual val apiKey: String = readBuildConfigString("REVENUECAT_API_KEY")
        .takeIf(String::isNotBlank)
        ?: RevenueCatConfig.debugApiKey.takeIf { readBuildConfigBoolean("DEBUG") ?: true }
        .orEmpty()
}

private fun readBuildConfigString(fieldName: String): String = runCatching {
    val buildConfigClass = Class.forName("com.sortoapps.chorepilot.BuildConfig")
    buildConfigClass.getField(fieldName).get(null) as? String
}.getOrNull().orEmpty()

private fun readBuildConfigBoolean(fieldName: String): Boolean? = runCatching {
    val buildConfigClass = Class.forName("com.sortoapps.chorepilot.BuildConfig")
    buildConfigClass.getField(fieldName).getBoolean(null)
}.getOrNull()
