package com.sortoapps.chorepilot.features.chores.presentation.viewmodel

import com.sortoapps.chorepilot.features.auth.domain.HouseholdRole
import com.sortoapps.chorepilot.features.auth.domain.hasHouseholdManagementAccess

enum class ChoreLifecycleAction {
    Deactivate,
    Reactivate,
    Delete,
}

data class ChoreLifecycleConfirmationUiState(
    val action: ChoreLifecycleAction,
    val choreId: String,
    val choreTitle: String,
    val title: String,
    val body: String,
    val confirmLabel: String,
    val requiresTextConfirmation: Boolean = false,
    val requiredConfirmationText: String = "",
    val confirmationText: String = "",
    val confirmEnabled: Boolean = true,
)

internal fun buildChoreLifecycleConfirmation(
    action: ChoreLifecycleAction,
    choreId: String,
    choreTitle: String,
): ChoreLifecycleConfirmationUiState = when (action) {
    ChoreLifecycleAction.Deactivate -> ChoreLifecycleConfirmationUiState(
        action = action,
        choreId = choreId,
        choreTitle = choreTitle,
        title = "Deactivate $choreTitle?",
        body = "$choreTitle will stop appearing in active schedules until you reactivate it. Submitted proof, completion history, and discovery source photos stay attached.",
        confirmLabel = "Deactivate chore",
    )

    ChoreLifecycleAction.Reactivate -> ChoreLifecycleConfirmationUiState(
        action = action,
        choreId = choreId,
        choreTitle = choreTitle,
        title = "Reactivate $choreTitle?",
        body = "$choreTitle will return to active schedules and keep all existing proof and history.",
        confirmLabel = "Reactivate chore",
    )

    ChoreLifecycleAction.Delete -> ChoreLifecycleConfirmationUiState(
        action = action,
        choreId = choreId,
        choreTitle = choreTitle,
        title = "Delete $choreTitle permanently?",
        body = "This removes the chore, all submitted proof photos, completion history, and saved discovery context. Type delete to unlock the final action.",
        confirmLabel = "Delete chore",
        requiresTextConfirmation = true,
        requiredConfirmationText = "delete",
        confirmEnabled = false,
    )
}

internal fun ChoreLifecycleConfirmationUiState.withConfirmationText(
    text: String,
): ChoreLifecycleConfirmationUiState = copy(
    confirmationText = text,
    confirmEnabled = !requiresTextConfirmation || text.trim().equals(requiredConfirmationText, ignoreCase = true),
)

internal fun HouseholdRole.canManageChoreLifecycle(): Boolean =
    hasHouseholdManagementAccess()
