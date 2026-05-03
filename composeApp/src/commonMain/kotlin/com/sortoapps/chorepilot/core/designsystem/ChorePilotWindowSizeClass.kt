package com.sortoapps.chorepilot.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
enum class ChorePilotWindowSizeClass {
    Compact,
    Medium,
    Expanded,
}

fun calculateChorePilotWindowSizeClass(width: Dp): ChorePilotWindowSizeClass = when {
    width < 600.dp -> ChorePilotWindowSizeClass.Compact
    width < 840.dp -> ChorePilotWindowSizeClass.Medium
    else -> ChorePilotWindowSizeClass.Expanded
}

val LocalWindowSizeClass = staticCompositionLocalOf { ChorePilotWindowSizeClass.Compact }
