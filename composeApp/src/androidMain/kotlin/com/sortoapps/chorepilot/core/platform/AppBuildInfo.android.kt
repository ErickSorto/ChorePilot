package com.sortoapps.chorepilot.core.platform

actual object AppBuildInfo {
    actual val isDebugBuild: Boolean = runCatching {
        val buildConfigClass = Class.forName("com.sortoapps.chorepilot.BuildConfig")
        buildConfigClass.getField("DEBUG").getBoolean(null)
    }.getOrDefault(true)
}
