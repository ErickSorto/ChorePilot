package com.sortoapps.chorepilot.features.chores.domain

import com.sortoapps.chorepilot.features.auth.domain.HouseholdRole
import com.sortoapps.chorepilot.features.auth.domain.hasHouseholdManagementAccess

fun HouseholdRole.canManageHouseholdChores(): Boolean =
    hasHouseholdManagementAccess()

fun canUndoChoreCompletion(
    chore: ChoreItem,
    viewerId: String,
    viewerName: String,
    viewerRole: HouseholdRole,
): Boolean {
    if (chore.status != ChoreItemStatus.Completed && chore.status != ChoreItemStatus.PendingProof) {
        return true
    }

    if (viewerRole.canManageHouseholdChores()) {
        return true
    }

    val assignedToViewerId = viewerId.isNotBlank() && chore.assigneeId.equals(viewerId, ignoreCase = true)
    val assignedToViewerName = chore.assignee.equals(viewerName, ignoreCase = true)
    return assignedToViewerId || assignedToViewerName
}
