package com.sortoapps.chorepilot.core.platform

import kotlin.native.Platform
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
actual object AppBuildInfo {
    actual val isDebugBuild: Boolean = Platform.isDebugBinary
}
