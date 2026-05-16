package com.sortoapps.chorepilot.app.presentation

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sortoapps.chorepilot.core.designsystem.ChorePilotSnackbarVisuals
import com.sortoapps.chorepilot.core.ui.SnackbarController
import com.sortoapps.chorepilot.core.ui.SnackbarEvent
import com.sortoapps.chorepilot.core.ui.SnackbarEventDuration
import com.sortoapps.chorepilot.features.notifications.domain.AppVisibilityTracker
import org.koin.compose.koinInject

val LocalAppSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> {
    error("LocalAppSnackbarHostState not provided")
}

@Composable
fun AppScaffold(
    content: @Composable () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val appVisibilityTracker = koinInject<AppVisibilityTracker>()

    DisposableEffect(lifecycleOwner, appVisibilityTracker) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START,
                Lifecycle.Event.ON_RESUME,
                -> appVisibilityTracker.setForeground(true)

                Lifecycle.Event.ON_STOP,
                -> appVisibilityTracker.setForeground(false)

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            appVisibilityTracker.setForeground(false)
        }
    }

    SnackbarHandler(snackbarHostState = snackbarHostState)

    CompositionLocalProvider(LocalAppSnackbarHostState provides snackbarHostState) {
        content()
    }
}

@Composable
fun SnackbarHandler(
    snackbarHostState: SnackbarHostState,
) {
    var eventToShow by remember { mutableStateOf<SnackbarEvent?>(null) }

    LaunchedEffect(Unit) {
        SnackbarController.events.collect { event ->
            eventToShow = event
        }
    }

    val currentEvent = eventToShow
    if (currentEvent != null) {
        val message = currentEvent.message.asString()
        val actionLabel = currentEvent.action?.name?.asString()

        LaunchedEffect(currentEvent, message, actionLabel) {
            val result = snackbarHostState.showSnackbar(
                ChorePilotSnackbarVisuals(
                    message = message,
                    actionLabel = actionLabel,
                    duration = currentEvent.toComposeDuration(),
                    style = currentEvent.style,
                    icon = currentEvent.icon,
                    celebration = currentEvent.celebration,
                ),
            )
            if (result == SnackbarResult.ActionPerformed) {
                currentEvent.action?.action?.invoke()
            }
            eventToShow = null
        }
    }
}

private fun SnackbarEvent.toComposeDuration(): SnackbarDuration = duration.toComposeDuration()

private fun SnackbarEventDuration.toComposeDuration(): SnackbarDuration = when (this) {
    SnackbarEventDuration.Short -> SnackbarDuration.Short
    SnackbarEventDuration.Long -> SnackbarDuration.Long
    SnackbarEventDuration.Indefinite -> SnackbarDuration.Indefinite
}
