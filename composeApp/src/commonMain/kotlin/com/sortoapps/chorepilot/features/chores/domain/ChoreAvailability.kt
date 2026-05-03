package com.sortoapps.chorepilot.features.chores.domain

import kotlinx.datetime.LocalDate

enum class ChoreAvailabilityMode {
    Available,
    DayOff,
    GoneMode,
}

data class HouseholdSchedulingMember(
    val userId: String,
    val displayName: String,
    val active: Boolean = true,
    val availabilityMode: ChoreAvailabilityMode = ChoreAvailabilityMode.Available,
    val availabilityStartDate: String = "",
    val availabilityEndDate: String = "",
)

data class ChoreAssignmentResolution(
    val assignee: String,
    val reason: String = "",
    val originalAssignee: String = assignee,
    val suggestionScopeStartDate: String = "",
    val suggestionScopeEndDate: String = "",
    val outsideNormalRotation: Boolean = false,
    val countsTowardFairness: Boolean = true,
)

data class ChoreAssignmentSource(
    val choreId: String,
    val storedAssignee: String,
    val draft: ChoreDraft,
    val effortScore: Int,
)

data class ResolvedChoreOccurrence(
    val day: ChoreScheduleDay,
    val resolution: ChoreAssignmentResolution,
    val status: ChoreItemStatus,
    val dueLabel: String,
    val completedByName: String,
    val completedAtEpochMs: Long,
    val proofPhotoCount: Int,
)

internal fun HouseholdSchedulingMember.isUnavailableOn(
    day: ChoreScheduleDay,
): Boolean {
    if (!active || availabilityMode == ChoreAvailabilityMode.Available) return false
    val targetDate = runCatching { LocalDate.parse(day.dateKey) }.getOrNull() ?: return false
    val startDate = availabilityStartDate.takeIf(String::isNotBlank)
        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?: return false
    val endDate = availabilityEndDate.takeIf(String::isNotBlank)
        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?: startDate
    return targetDate >= startDate && targetDate <= endDate
}

internal fun HouseholdSchedulingMember.matchesAssignee(
    assignee: String,
): Boolean = displayName.equals(assignee, ignoreCase = true) ||
    userId.equals(assignee, ignoreCase = true)

internal fun HouseholdSchedulingMember.unavailabilityWindowOrNull(): Pair<String, String>? {
    if (!active || availabilityMode == ChoreAvailabilityMode.Available) return null
    val start = availabilityStartDate.takeIf(String::isNotBlank) ?: return null
    val end = availabilityEndDate.takeIf(String::isNotBlank) ?: start
    return start to end
}
