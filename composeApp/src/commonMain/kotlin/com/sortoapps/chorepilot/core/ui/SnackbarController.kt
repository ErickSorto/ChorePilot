package com.sortoapps.chorepilot.core.ui

import chorepilot.composeapp.generated.resources.Res
import chorepilot.composeapp.generated.resources.chore_complete_squirrel_acorn_check
import chorepilot.composeapp.generated.resources.chore_complete_squirrel_checklist_hug
import chorepilot.composeapp.generated.resources.chore_complete_squirrel_cheer
import chorepilot.composeapp.generated.resources.chore_complete_squirrel_confetti_jump
import chorepilot.composeapp.generated.resources.chore_complete_squirrel_golden_bell
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlin.math.absoluteValue
import org.jetbrains.compose.resources.DrawableResource

data class SnackbarEvent(
    val message: StringValue,
    val action: SnackbarAction? = null,
    val style: SnackbarStyle = SnackbarStyle.Standard,
    val icon: DrawableResource? = null,
    val duration: SnackbarEventDuration = SnackbarEventDuration.Short,
)

data class SnackbarAction(
    val name: StringValue,
    val action: () -> Unit,
)

enum class SnackbarStyle {
    Standard,
    Celebration,
}

enum class SnackbarEventDuration {
    Short,
    Long,
    Indefinite,
}

object SnackbarController {
    private val _events = Channel<SnackbarEvent>(capacity = Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    suspend fun sendEvent(event: SnackbarEvent) {
        _events.send(event)
    }
}

object SnackbarMascotIcons {
    private val celebrationIcons = listOf(
        Res.drawable.chore_complete_squirrel_cheer,
        Res.drawable.chore_complete_squirrel_acorn_check,
        Res.drawable.chore_complete_squirrel_confetti_jump,
        Res.drawable.chore_complete_squirrel_checklist_hug,
        Res.drawable.chore_complete_squirrel_golden_bell,
    )

    fun celebrationIconFor(seed: String): DrawableResource {
        val index = seed.hashCode().absoluteValue % celebrationIcons.size
        return celebrationIcons[index]
    }
}
