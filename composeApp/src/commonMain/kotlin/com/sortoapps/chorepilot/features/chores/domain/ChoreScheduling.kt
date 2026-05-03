package com.sortoapps.chorepilot.features.chores.domain

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.math.absoluteValue

data class ChoreScheduleDay(
    val id: String,
    val weekdayName: String,
    val dayOffset: Int,
    val isToday: Boolean,
    val dateKey: String,
)

fun scheduleDateKey(
    dayOffset: Int,
    today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
): String = today.plus(DatePeriod(days = dayOffset)).toString()

fun weekdayNameForDayOffset(
    dayOffset: Int,
    today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
): String = today.plus(DatePeriod(days = dayOffset)).dayOfWeek.displayName()

fun todayScheduleDay(): ChoreScheduleDay = ChoreScheduleDay(
    id = "today",
    weekdayName = weekdayNameForDayOffset(dayOffset = 0),
    dayOffset = 0,
    isToday = true,
    dateKey = scheduleDateKey(dayOffset = 0),
)

fun upcomingScheduleDays(
    count: Int = 7,
): List<ChoreScheduleDay> = (0 until count).map { offset ->
    ChoreScheduleDay(
        id = "upcoming-$offset",
        weekdayName = weekdayNameForDayOffset(offset),
        dayOffset = offset,
        isToday = offset == 0,
        dateKey = scheduleDateKey(offset),
    )
}

fun currentWeekdayName(
    today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
): String = today.dayOfWeek.displayName()

data class ChoreAssignmentLoad(
    val assignee: String,
    val score: Int,
)

fun parseRelativeDayOffset(
    label: String,
    todayWeekdayName: String,
): Int {
    val normalized = label.trim()
    return when {
        normalized.equals("Today", ignoreCase = true) -> 0
        normalized.equals("Tomorrow", ignoreCase = true) -> 1
        normalized.startsWith("This ", ignoreCase = true) -> {
            daysUntilWeekday(
                todayWeekdayName = todayWeekdayName,
                targetWeekdayName = normalized.removePrefix("This ").trim(),
                forceNextWeek = false,
            )
        }

        normalized.startsWith("Next ", ignoreCase = true) -> {
            daysUntilWeekday(
                todayWeekdayName = todayWeekdayName,
                targetWeekdayName = normalized.removePrefix("Next ").trim(),
                forceNextWeek = true,
            )
        }

        else -> 0
    }
}

fun ChoreDraft.appearsOnDay(
    day: ChoreScheduleDay,
    todayWeekdayName: String,
): Boolean {
    val startOffset = parseRelativeDayOffset(
        label = if (repeatsEnabled) repeatStartDate else selectedOneTimeDate,
        todayWeekdayName = todayWeekdayName,
    )

    if (day.dayOffset < startOffset) return false
    if (!isActive) return false

    if (!repeatsEnabled) {
        return day.dayOffset == startOffset
    }

    return when (repeatFrequency) {
        ChoreRepeatFrequencyValue.Daily -> when (dailyRepeatMode) {
            ChoreDailyModeValue.EveryDay -> true
            ChoreDailyModeValue.Weekdays -> day.weekdayName in weekdayNames()
            ChoreDailyModeValue.SpecificDays -> day.weekdayName in selectedWeekdays
        }

        ChoreRepeatFrequencyValue.Weekly -> day.weekdayName in selectedWeekdays
        ChoreRepeatFrequencyValue.Monthly -> day.dayOffset == startOffset || (day.dayOffset - startOffset) % 30 == 0
        ChoreRepeatFrequencyValue.Custom -> {
            val interval = customPattern.filter(Char::isDigit).toIntOrNull() ?: 2
            day.dayOffset == startOffset || (day.dayOffset - startOffset) % (interval * 7) == 0
        }
    }
}

fun resolveAutoAssignee(
    draft: ChoreDraft,
    day: ChoreScheduleDay,
    loads: List<ChoreAssignmentLoad>,
    choreId: String,
    householdMembers: List<HouseholdSchedulingMember> = emptyList(),
    decisions: List<AvailabilitySuggestionDecision> = emptyList(),
    todayWeekdayName: String = currentWeekdayName(),
): String {
    return resolveAssignmentForDay(
        draft = draft,
        storedAssignee = draft.assignee,
        day = day,
        loads = loads,
        choreId = choreId,
        householdMembers = householdMembers,
        decisions = decisions,
        todayWeekdayName = todayWeekdayName,
    ).assignee
}

fun resolveAssignmentsForDay(
    sources: List<ChoreAssignmentSource>,
    day: ChoreScheduleDay,
    householdMembers: List<HouseholdSchedulingMember> = emptyList(),
    decisions: List<AvailabilitySuggestionDecision> = emptyList(),
    todayWeekdayName: String = currentWeekdayName(),
): Map<String, ChoreAssignmentResolution> {
    if (sources.isEmpty()) return emptyMap()

    val ledger = linkedMapOf<String, Int>()
    val occurrenceCounters = mutableMapOf<String, Int>()
    val resolvedForTargetDay = linkedMapOf<String, ChoreAssignmentResolution>()
    val startOffset = sources.minOfOrNull { source ->
        occurrenceStartOffset(
            draft = source.draft,
            todayWeekdayName = todayWeekdayName,
        )
    } ?: day.dayOffset

    if (day.dayOffset < startOffset) return emptyMap()

    for (offset in startOffset..day.dayOffset) {
        val occurrenceDay = ChoreScheduleDay(
            id = "resolved-$offset",
            weekdayName = weekdayNameForDayOffset(offset),
            dayOffset = offset,
            isToday = offset == 0,
            dateKey = scheduleDateKey(offset),
        )
        sources
            .filter { source -> source.draft.appearsOnDay(occurrenceDay, todayWeekdayName) }
            .sortedBy { it.choreId }
            .forEach { source ->
                val resolution = resolveAssignmentForOccurrence(
                    draft = source.draft,
                    storedAssignee = source.storedAssignee,
                    day = occurrenceDay,
                    loads = ledger.entries.map { (assignee, score) ->
                        ChoreAssignmentLoad(assignee = assignee, score = score)
                    },
                    choreId = source.choreId,
                    householdMembers = householdMembers,
                    decisions = decisions,
                    occurrenceIndex = occurrenceCounters[source.choreId] ?: 0,
                )
                if (resolution.countsTowardFairness && resolution.assignee.isNotBlank()) {
                    ledger[resolution.assignee] = (ledger[resolution.assignee] ?: 0) + source.effortScore
                    occurrenceCounters[source.choreId] = (occurrenceCounters[source.choreId] ?: 0) + 1
                }
                if (occurrenceDay.dateKey == day.dateKey) {
                    resolvedForTargetDay[source.choreId] = resolution
                }
            }
    }

    return resolvedForTargetDay
}

fun resolveAssignmentForDay(
    draft: ChoreDraft,
    storedAssignee: String,
    day: ChoreScheduleDay,
    loads: List<ChoreAssignmentLoad>,
    choreId: String,
    householdMembers: List<HouseholdSchedulingMember> = emptyList(),
    decisions: List<AvailabilitySuggestionDecision> = emptyList(),
    todayWeekdayName: String = currentWeekdayName(),
): ChoreAssignmentResolution {
    if (!draft.autoAssign) {
        return resolveDirectAssignment(
            storedAssignee = storedAssignee,
            day = day,
            loads = loads,
            choreId = choreId,
            householdMembers = householdMembers,
            decisions = decisions,
        )
    }

    return projectedFairAssignment(
        draft = draft,
        storedAssignee = storedAssignee,
        day = day,
        loads = loads,
        choreId = choreId,
        householdMembers = householdMembers,
        decisions = decisions,
        todayWeekdayName = todayWeekdayName,
    )
}

private fun resolveAssignmentForOccurrence(
    draft: ChoreDraft,
    storedAssignee: String,
    day: ChoreScheduleDay,
    loads: List<ChoreAssignmentLoad>,
    choreId: String,
    householdMembers: List<HouseholdSchedulingMember>,
    decisions: List<AvailabilitySuggestionDecision>,
    occurrenceIndex: Int,
): ChoreAssignmentResolution {
    if (!draft.autoAssign) {
        return resolveDirectAssignment(
            storedAssignee = storedAssignee,
            day = day,
            loads = loads,
            choreId = choreId,
            householdMembers = householdMembers,
            decisions = decisions,
        )
    }

    return when (draft.smartAssignmentMode) {
        ChoreSmartAssignmentModeValue.DayPlan -> {
            val dayPlanAssignee = draft.dayAssignments[day.weekdayName]
                ?.takeUnless { it == "Smart fair" }
            if (dayPlanAssignee != null) {
                resolveExplicitAssignment(
                    preferredAssignee = dayPlanAssignee,
                    fallbackPool = draft.assignmentPool.ifEmpty { setOf(dayPlanAssignee) },
                    day = day,
                    loads = loads,
                    choreId = choreId,
                    householdMembers = householdMembers,
                    decisions = decisions,
                )
            } else {
                resolveFairAssignment(
                    pool = draft.assignmentPool,
                    loads = loads,
                    choreId = choreId,
                    householdMembers = householdMembers,
                    decisions = decisions,
                    day = day,
                    occurrenceIndex = occurrenceIndex,
                )
            }
        }

        ChoreSmartAssignmentModeValue.FairRotation -> resolveFairAssignment(
            pool = draft.assignmentPool,
            loads = loads,
            choreId = choreId,
            householdMembers = householdMembers,
            decisions = decisions,
            day = day,
            occurrenceIndex = occurrenceIndex,
        )
    }
}

fun resolvedAssigneeForDay(
    draft: ChoreDraft,
    storedAssignee: String,
    day: ChoreScheduleDay,
    loads: List<ChoreAssignmentLoad>,
    choreId: String,
    householdMembers: List<HouseholdSchedulingMember> = emptyList(),
    decisions: List<AvailabilitySuggestionDecision> = emptyList(),
    todayWeekdayName: String = currentWeekdayName(),
): String = if (draft.autoAssign) {
    resolveAssignmentForDay(
        draft = draft,
        storedAssignee = storedAssignee,
        day = day,
        loads = loads,
        choreId = choreId,
        householdMembers = householdMembers,
        decisions = decisions,
        todayWeekdayName = todayWeekdayName,
    ).assignee
} else {
    resolveDirectAssignment(
        storedAssignee = storedAssignee,
        day = day,
        loads = loads,
        choreId = choreId,
        householdMembers = householdMembers,
        decisions = decisions,
    ).assignee
}

fun buildAvailabilitySuggestion(
    choreId: String,
    choreTitle: String,
    day: ChoreScheduleDay,
    resolution: ChoreAssignmentResolution,
    decisions: List<AvailabilitySuggestionDecision> = emptyList(),
): AvailabilitySuggestion? {
    if (
        resolution.originalAssignee.isBlank() ||
        resolution.assignee.isBlank() ||
        resolution.originalAssignee.equals(resolution.assignee, ignoreCase = true) ||
        resolution.suggestionScopeStartDate.isBlank() ||
        resolution.suggestionScopeEndDate.isBlank()
    ) {
        return null
    }
    if (decisions.any { it.appliesTo(choreId, resolution.originalAssignee, day.dateKey) }) {
        return null
    }
    return AvailabilitySuggestion(
        id = availabilitySuggestionId(
            choreId = choreId,
            originalAssignee = resolution.originalAssignee,
            scopeStartDate = resolution.suggestionScopeStartDate,
            scopeEndDate = resolution.suggestionScopeEndDate,
        ),
        choreId = choreId,
        choreTitle = choreTitle,
        dateKey = day.dateKey,
        weekdayLabel = day.weekdayName,
        originalAssignee = resolution.originalAssignee,
        recommendedCoverMember = resolution.assignee,
        reasonText = resolution.reason,
        scopeStartDate = resolution.suggestionScopeStartDate,
        scopeEndDate = resolution.suggestionScopeEndDate,
        outsideNormalRotation = resolution.outsideNormalRotation,
    )
}

fun sortScheduledChores(chores: List<ChoreItem>): List<ChoreItem> = chores.sortedWith(
    compareBy<ChoreItem> { statusPriority(it.status) }
        .thenBy { it.area.lowercase() }
        .thenBy { it.title.lowercase() },
)

fun nextCompletionStatus(
    currentStatus: ChoreItemStatus,
    proofRequired: Boolean,
): ChoreItemStatus = when (currentStatus) {
    ChoreItemStatus.Completed -> ChoreItemStatus.DueSoon
    ChoreItemStatus.PendingProof -> ChoreItemStatus.PendingProof
    else -> if (proofRequired) ChoreItemStatus.PendingProof else ChoreItemStatus.Completed
}

fun nextCompletionStatusForDay(
    currentStatus: ChoreItemStatus,
    proofRequired: Boolean,
    day: ChoreScheduleDay,
): ChoreItemStatus = when (currentStatus) {
    ChoreItemStatus.Completed,
    ChoreItemStatus.PendingProof,
    -> if (day.isToday) ChoreItemStatus.DueSoon else ChoreItemStatus.Scheduled

    else -> if (proofRequired) ChoreItemStatus.PendingProof else ChoreItemStatus.Completed
}

fun defaultStatusForDay(
    baseStatus: ChoreItemStatus,
    day: ChoreScheduleDay,
): ChoreItemStatus = if (day.isToday) baseStatus else ChoreItemStatus.Scheduled

fun approveProofStatus(
    currentStatus: ChoreItemStatus,
): ChoreItemStatus = if (currentStatus == ChoreItemStatus.PendingProof) {
    ChoreItemStatus.Completed
} else {
    currentStatus
}

fun requiresCompletionConfirmation(
    chore: ChoreItem,
    viewerName: String,
): Boolean = chore.assignee.isNotBlank() &&
    !chore.assignee.equals(viewerName, ignoreCase = true)

fun recordedCompletionName(
    assignee: String,
    viewerName: String,
): String = if (assignee.isBlank() || assignee.equals(viewerName, ignoreCase = true)) {
    ""
} else {
    viewerName
}

private fun resolveFairAssignment(
    day: ChoreScheduleDay,
    pool: Set<String>,
    loads: List<ChoreAssignmentLoad>,
    choreId: String,
    householdMembers: List<HouseholdSchedulingMember>,
    decisions: List<AvailabilitySuggestionDecision>,
    occurrenceIndex: Int,
): ChoreAssignmentResolution {
    val eligible = pool.ifEmpty { setOf("Erick") }.sorted()
    val preferredOccurrenceAssignee = chooseFairCandidate(
        candidates = eligible,
        loads = loads,
        choreId = choreId,
        occurrenceIndex = occurrenceIndex,
    )
    val preferredMember = householdMembers.firstOrNull { it.matchesAssignee(preferredOccurrenceAssignee) }
    preferredMember?.let { member ->
        acceptedDecisionOverride(
            decisions = decisions,
            choreId = choreId,
            originalAssignee = preferredOccurrenceAssignee,
            day = day,
            member = member,
        )?.let { return it }
    }

    val availableInPool = availableCandidates(
        pool = eligible.toSet(),
        householdMembers = householdMembers,
        day = day,
    )
    if (preferredOccurrenceAssignee in availableInPool || preferredMember == null) {
        return ChoreAssignmentResolution(assignee = preferredOccurrenceAssignee)
    }

    val broadenedFallback = if (availableInPool.isEmpty()) {
        broaderAvailableCandidates(
            householdMembers = householdMembers,
            excluded = eligible.toSet(),
            day = day,
        )
    } else {
        emptyList()
    }
    val candidatePool = availableInPool.ifEmpty { broadenedFallback }
    if (candidatePool.isEmpty()) {
        return ChoreAssignmentResolution(
            assignee = preferredOccurrenceAssignee,
            reason = "Everyone is marked away, so this stayed with $preferredOccurrenceAssignee.",
        )
    }
    val fallback = chooseFairCandidate(
        candidates = candidatePool.sorted(),
        loads = loads,
        choreId = choreId,
        occurrenceIndex = occurrenceIndex,
    )
    val outsideNormalRotation = broadenedFallback.isNotEmpty()
    return ChoreAssignmentResolution(
        assignee = fallback,
        reason = if (outsideNormalRotation) {
            "$fallback is covering outside the normal rotation while $preferredOccurrenceAssignee is away."
        } else {
            "Covering for $preferredOccurrenceAssignee while they're away."
        },
        originalAssignee = preferredOccurrenceAssignee,
        suggestionScopeStartDate = preferredMember.unavailabilityWindowOrNull()?.first.orEmpty(),
        suggestionScopeEndDate = preferredMember.unavailabilityWindowOrNull()?.second.orEmpty(),
        outsideNormalRotation = outsideNormalRotation,
    )
}

private fun projectedFairAssignment(
    draft: ChoreDraft,
    storedAssignee: String,
    day: ChoreScheduleDay,
    loads: List<ChoreAssignmentLoad>,
    choreId: String,
    householdMembers: List<HouseholdSchedulingMember>,
    decisions: List<AvailabilitySuggestionDecision>,
    todayWeekdayName: String,
): ChoreAssignmentResolution {
    val startOffset = occurrenceStartOffset(
        draft = draft,
        todayWeekdayName = todayWeekdayName,
    )
    if (day.dayOffset < startOffset) {
        return resolveAssignmentForOccurrence(
            draft = draft,
            storedAssignee = storedAssignee,
            day = day,
            loads = loads,
            choreId = choreId,
            householdMembers = householdMembers,
            decisions = decisions,
            occurrenceIndex = 0,
        )
    }

    val projectedLoads = loads
        .groupBy { it.assignee }
        .mapValues { (_, entries) -> entries.sumOf { it.score } }
        .toMutableMap()
    var occurrenceIndex = 0
    var targetResolution: ChoreAssignmentResolution? = null

    for (offset in startOffset..day.dayOffset) {
        val occurrenceDay = ChoreScheduleDay(
            id = "projected-$offset",
            weekdayName = weekdayNameForDayOffset(offset),
            dayOffset = offset,
            isToday = offset == 0,
            dateKey = scheduleDateKey(offset),
        )
        if (!draft.appearsOnDay(occurrenceDay, todayWeekdayName)) continue

        val resolution = resolveAssignmentForOccurrence(
            draft = draft,
            storedAssignee = storedAssignee,
            day = occurrenceDay,
            loads = projectedLoads.entries.map { (assignee, score) ->
                ChoreAssignmentLoad(assignee = assignee, score = score)
            },
            choreId = choreId,
            householdMembers = householdMembers,
            decisions = decisions,
            occurrenceIndex = occurrenceIndex,
        )
        if (resolution.countsTowardFairness && resolution.assignee.isNotBlank()) {
            projectedLoads[resolution.assignee] = (projectedLoads[resolution.assignee] ?: 0) + draft.effortLevel.score
        }
        if (occurrenceDay.dateKey == day.dateKey) {
            targetResolution = resolution
        }
        if (resolution.countsTowardFairness) {
            occurrenceIndex += 1
        }
    }

    return targetResolution ?: resolveAssignmentForOccurrence(
        draft = draft,
        storedAssignee = storedAssignee,
        day = day,
        loads = loads,
        choreId = choreId,
        householdMembers = householdMembers,
        decisions = decisions,
        occurrenceIndex = 0,
    )
}

private fun resolveDirectAssignment(
    storedAssignee: String,
    day: ChoreScheduleDay,
    loads: List<ChoreAssignmentLoad>,
    choreId: String,
    householdMembers: List<HouseholdSchedulingMember>,
    decisions: List<AvailabilitySuggestionDecision>,
): ChoreAssignmentResolution {
    if (storedAssignee.isBlank()) {
        return ChoreAssignmentResolution(assignee = storedAssignee)
    }
    val originalMember = householdMembers.firstOrNull { it.matchesAssignee(storedAssignee) }
    if (originalMember == null || !originalMember.isUnavailableOn(day)) {
        return ChoreAssignmentResolution(assignee = storedAssignee)
    }
    acceptedDecisionOverride(
        decisions = decisions,
        choreId = choreId,
        originalAssignee = storedAssignee,
        day = day,
        member = originalMember,
    )?.let { return it }
    val fallbackPool = householdMembers
        .filter { it.active && !it.matchesAssignee(storedAssignee) && !it.isUnavailableOn(day) }
        .map { it.displayName }
        .toSet()
    if (fallbackPool.isEmpty()) {
        return ChoreAssignmentResolution(
            assignee = storedAssignee,
            reason = "Everyone is marked away, so this stayed with $storedAssignee.",
        )
    }
    val fallback = chooseFairCandidate(
        candidates = fallbackPool.sorted(),
        loads = loads,
        choreId = choreId,
        occurrenceIndex = day.dayOffset.coerceAtLeast(0),
    )
    return ChoreAssignmentResolution(
        assignee = fallback,
        reason = "Covering for $storedAssignee while they're away.",
        originalAssignee = storedAssignee,
        suggestionScopeStartDate = originalMember.unavailabilityWindowOrNull()?.first.orEmpty(),
        suggestionScopeEndDate = originalMember.unavailabilityWindowOrNull()?.second.orEmpty(),
    )
}

private fun resolveExplicitAssignment(
    preferredAssignee: String,
    fallbackPool: Set<String>,
    day: ChoreScheduleDay,
    loads: List<ChoreAssignmentLoad>,
    choreId: String,
    householdMembers: List<HouseholdSchedulingMember>,
    decisions: List<AvailabilitySuggestionDecision>,
): ChoreAssignmentResolution {
    val preferredMember = householdMembers.firstOrNull { it.matchesAssignee(preferredAssignee) }
    if (preferredMember == null || !preferredMember.isUnavailableOn(day)) {
        return ChoreAssignmentResolution(assignee = preferredAssignee)
    }
    acceptedDecisionOverride(
        decisions = decisions,
        choreId = choreId,
        originalAssignee = preferredAssignee,
        day = day,
        member = preferredMember,
    )?.let { return it }
    val availableFallback = availableCandidates(
        pool = fallbackPool.ifEmpty { setOf(preferredAssignee) },
        householdMembers = householdMembers,
        day = day,
    ).filterNot { it.equals(preferredAssignee, ignoreCase = true) }
    val broadenedFallback = if (availableFallback.isEmpty()) {
        broaderAvailableCandidates(
            householdMembers = householdMembers,
            excluded = fallbackPool + preferredAssignee,
            day = day,
        )
    } else {
        emptyList()
    }
    if (availableFallback.isEmpty() && broadenedFallback.isEmpty()) {
        return ChoreAssignmentResolution(
            assignee = preferredAssignee,
            reason = "Everyone in this rotation is marked away, so this stayed with $preferredAssignee.",
        )
    }
    val fallback = chooseFairCandidate(
        candidates = (availableFallback.ifEmpty { broadenedFallback }).sorted(),
        loads = loads,
        choreId = choreId,
        occurrenceIndex = day.dayOffset.coerceAtLeast(0),
    )
    return ChoreAssignmentResolution(
        assignee = fallback,
        reason = if (broadenedFallback.isNotEmpty()) {
            "$fallback is covering outside the normal rotation while $preferredAssignee is away."
        } else {
            "Covering for $preferredAssignee while they're away."
        },
        originalAssignee = preferredAssignee,
        suggestionScopeStartDate = preferredMember.unavailabilityWindowOrNull()?.first.orEmpty(),
        suggestionScopeEndDate = preferredMember.unavailabilityWindowOrNull()?.second.orEmpty(),
        outsideNormalRotation = broadenedFallback.isNotEmpty(),
    )
}

private fun availableCandidates(
    pool: Set<String>,
    householdMembers: List<HouseholdSchedulingMember>,
    day: ChoreScheduleDay,
): List<String> {
    if (householdMembers.isEmpty()) return pool.sorted()
    val availableMemberNames = householdMembers
        .filter { member -> member.active && !member.isUnavailableOn(day) }
        .map { it.displayName }
    return pool.filter { candidate ->
        availableMemberNames.any { it.equals(candidate, ignoreCase = true) }
    }.sorted()
}

private fun broaderAvailableCandidates(
    householdMembers: List<HouseholdSchedulingMember>,
    excluded: Set<String>,
    day: ChoreScheduleDay,
): List<String> = householdMembers
    .filter { member ->
        member.active &&
            !member.isUnavailableOn(day) &&
            excluded.none { it.equals(member.displayName, ignoreCase = true) }
    }
    .map { it.displayName }
    .sorted()

private fun acceptedDecisionOverride(
    decisions: List<AvailabilitySuggestionDecision>,
    choreId: String,
    originalAssignee: String,
    day: ChoreScheduleDay,
    member: HouseholdSchedulingMember,
): ChoreAssignmentResolution? {
    val decision = decisions.firstOrNull { decision ->
        decision.status == AvailabilitySuggestionDecisionStatus.Accepted &&
            decision.appliesTo(
                choreId = choreId,
                originalAssignee = originalAssignee,
                dateKey = day.dateKey,
            )
    } ?: return null
    return ChoreAssignmentResolution(
        assignee = decision.coverMember,
        reason = "Covering for $originalAssignee while they're away.",
        originalAssignee = originalAssignee,
        suggestionScopeStartDate = decision.scopeStartDate.ifBlank {
            member.unavailabilityWindowOrNull()?.first.orEmpty()
        },
        suggestionScopeEndDate = decision.scopeEndDate.ifBlank {
            member.unavailabilityWindowOrNull()?.second.orEmpty()
        },
        outsideNormalRotation = false,
        countsTowardFairness = false,
    )
}

private fun chooseFairCandidate(
    candidates: List<String>,
    loads: List<ChoreAssignmentLoad>,
    choreId: String,
    occurrenceIndex: Int,
): String {
    val minimum = candidates.minOfOrNull { candidate ->
        loads.firstOrNull { it.assignee.equals(candidate, ignoreCase = true) }?.score ?: 0
    } ?: 0
    val tied = candidates.filter { candidate ->
        (loads.firstOrNull { it.assignee.equals(candidate, ignoreCase = true) }?.score ?: 0) == minimum
    }.sorted()
    val index = (occurrenceIndex + choreId.hashCode().absoluteValue) % tied.size.coerceAtLeast(1)
    return tied[index]
}

private fun occurrenceStartOffset(
    draft: ChoreDraft,
    todayWeekdayName: String,
): Int = parseRelativeDayOffset(
    label = if (draft.repeatsEnabled) draft.repeatStartDate else draft.selectedOneTimeDate,
    todayWeekdayName = todayWeekdayName,
)

private fun weekdayNames(): Set<String> = setOf(
    "Monday",
    "Tuesday",
    "Wednesday",
    "Thursday",
    "Friday",
)

private fun daysUntilWeekday(
    todayWeekdayName: String,
    targetWeekdayName: String,
    forceNextWeek: Boolean,
): Int {
    val weekdays = listOf(
        "Sunday",
        "Monday",
        "Tuesday",
        "Wednesday",
        "Thursday",
        "Friday",
        "Saturday",
    )
    val todayIndex = weekdays.indexOf(todayWeekdayName).coerceAtLeast(0)
    val targetIndex = weekdays.indexOf(targetWeekdayName).coerceAtLeast(todayIndex)
    val raw = (targetIndex - todayIndex + 7) % 7
    return when {
        forceNextWeek && raw == 0 -> 7
        forceNextWeek -> raw + 7
        else -> raw
    }
}

private fun statusPriority(status: ChoreItemStatus): Int = when (status) {
    ChoreItemStatus.Overdue -> 0
    ChoreItemStatus.PendingProof -> 1
    ChoreItemStatus.DueSoon -> 2
    ChoreItemStatus.Scheduled -> 3
    ChoreItemStatus.Completed -> 4
}

private val Int.absoluteValue: Int
    get() = if (this < 0) -this else this

private fun DayOfWeek.displayName(): String = name.lowercase()
    .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
