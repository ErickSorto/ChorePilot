package com.sortoapps.chorepilot.features.chores.domain

import com.sortoapps.chorepilot.features.chores.data.InMemoryChoreRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class ChoreSchedulingTest {
    @Test
    fun smartRotationSkipsUnavailableMembersWithinDateWindow() {
        val draft = testDraft(
            smartAssignmentMode = ChoreSmartAssignmentModeValue.FairRotation,
            repeatFrequency = ChoreRepeatFrequencyValue.Daily,
            dailyRepeatMode = ChoreDailyModeValue.EveryDay,
            selectedWeekdays = emptySet(),
            dayAssignments = emptyMap(),
        )
        val members = listOf(
            schedulingMember(
                name = "Erick",
                mode = ChoreAvailabilityMode.GoneMode,
                startDate = scheduleDateKey(0),
                endDate = scheduleDateKey(1),
            ),
            schedulingMember(name = "Emma"),
        )

        val today = resolveAssignmentForDay(
            draft = draft,
            storedAssignee = "",
            day = dayAtOffset(0),
            loads = emptyList(),
            choreId = "rotation-availability",
            householdMembers = members,
            todayWeekdayName = currentWeekdayName(),
        )
        val afterReturn = resolveAssignmentForDay(
            draft = draft,
            storedAssignee = "",
            day = dayAtOffset(2),
            loads = emptyList(),
            choreId = "rotation-availability",
            householdMembers = members,
            todayWeekdayName = currentWeekdayName(),
        )

        assertEquals("Emma", today.assignee)
        assertEquals("Erick", afterReturn.assignee)
    }

    @Test
    fun dailyFairRotationAdvancesAcrossFutureDays() {
        val draft = testDraft(
            smartAssignmentMode = ChoreSmartAssignmentModeValue.FairRotation,
            repeatFrequency = ChoreRepeatFrequencyValue.Daily,
            dailyRepeatMode = ChoreDailyModeValue.EveryDay,
            selectedWeekdays = emptySet(),
            dayAssignments = emptyMap(),
        )
        val loads = emptyList<ChoreAssignmentLoad>()

        val todayAssignee = resolvedAssigneeForDay(
            draft = draft,
            storedAssignee = "",
            day = dayAtOffset(0),
            loads = loads,
            choreId = "rotation-daily",
            todayWeekdayName = currentWeekdayName(),
        )
        val tomorrowAssignee = resolvedAssigneeForDay(
            draft = draft,
            storedAssignee = "",
            day = dayAtOffset(1),
            loads = loads,
            choreId = "rotation-daily",
            todayWeekdayName = currentWeekdayName(),
        )
        val nextAssignee = resolvedAssigneeForDay(
            draft = draft,
            storedAssignee = "",
            day = dayAtOffset(2),
            loads = loads,
            choreId = "rotation-daily",
            todayWeekdayName = currentWeekdayName(),
        )

        assertNotEquals(todayAssignee, tomorrowAssignee)
        assertNotEquals(tomorrowAssignee, nextAssignee)
    }

    @Test
    fun weeklyFairRotationAdvancesOnFutureOccurrence() {
        val weekday = currentWeekdayName()
        val draft = testDraft(
            smartAssignmentMode = ChoreSmartAssignmentModeValue.FairRotation,
            repeatFrequency = ChoreRepeatFrequencyValue.Weekly,
            selectedWeekdays = setOf(weekday),
            dayAssignments = emptyMap(),
        )

        val thisWeekAssignee = resolvedAssigneeForDay(
            draft = draft,
            storedAssignee = "",
            day = dayAtOffset(0),
            loads = emptyList(),
            choreId = "rotation-weekly",
            todayWeekdayName = weekday,
        )
        val nextWeekAssignee = resolvedAssigneeForDay(
            draft = draft,
            storedAssignee = "",
            day = dayAtOffset(7),
            loads = emptyList(),
            choreId = "rotation-weekly",
            todayWeekdayName = weekday,
        )

        assertNotEquals(thisWeekAssignee, nextWeekAssignee)
    }

    @Test
    fun dayPlanKeepsFixedAssignmentsAndRotatesSmartFairDays() {
        val todayWeekday = currentWeekdayName()
        val tomorrowWeekday = weekdayNameForDayOffset(1)
        val dayAfterWeekday = weekdayNameForDayOffset(2)
        val draft = testDraft(
            smartAssignmentMode = ChoreSmartAssignmentModeValue.DayPlan,
            repeatFrequency = ChoreRepeatFrequencyValue.Daily,
            dailyRepeatMode = ChoreDailyModeValue.SpecificDays,
            selectedWeekdays = setOf(todayWeekday, tomorrowWeekday, dayAfterWeekday),
            dayAssignments = mapOf(
                todayWeekday to "Erick",
                tomorrowWeekday to "Smart fair",
                dayAfterWeekday to "Smart fair",
            ),
        )

        val fixedAssignee = resolvedAssigneeForDay(
            draft = draft,
            storedAssignee = "",
            day = dayAtOffset(0),
            loads = emptyList(),
            choreId = "rotation-day-plan",
            todayWeekdayName = todayWeekday,
        )
        val smartFairTomorrow = resolvedAssigneeForDay(
            draft = draft,
            storedAssignee = "",
            day = dayAtOffset(1),
            loads = emptyList(),
            choreId = "rotation-day-plan",
            todayWeekdayName = todayWeekday,
        )
        val smartFairDayAfter = resolvedAssigneeForDay(
            draft = draft,
            storedAssignee = "",
            day = dayAtOffset(2),
            loads = emptyList(),
            choreId = "rotation-day-plan",
            todayWeekdayName = todayWeekday,
        )

        assertEquals("Erick", fixedAssignee)
        assertNotEquals(smartFairTomorrow, smartFairDayAfter)
    }

    @Test
    fun inMemoryRepositoryReturnsRotatedAssigneesForFutureDates() = runTest {
        val repository = InMemoryChoreRepository(
            initialHouseholdMembers = listOf(
                schedulingMember(name = "Ava"),
                schedulingMember(name = "Liam"),
            ),
        )
        val choreId = repository.saveChore(
            testDraft(
                id = "rotation-repo",
                title = "Rotation Test Chore",
                smartAssignmentMode = ChoreSmartAssignmentModeValue.FairRotation,
                repeatFrequency = ChoreRepeatFrequencyValue.Daily,
                dailyRepeatMode = ChoreDailyModeValue.EveryDay,
                selectedWeekdays = emptySet(),
                assignee = "",
                autoAssign = true,
                dayAssignments = emptyMap(),
            ).copy(assignmentPool = setOf("Ava", "Liam")),
        )

        val todayAssignee = repository
            .getScheduledChores(dayAtOffset(0))
            .first { it.id == choreId }
            .assignee
        val tomorrowAssignee = repository
            .getScheduledChores(dayAtOffset(1))
            .first { it.id == choreId }
            .assignee
        val nextAssignee = repository
            .getScheduledChores(dayAtOffset(2))
            .first { it.id == choreId }
            .assignee

        assertTrue(todayAssignee.isNotBlank())
        assertNotEquals(todayAssignee, tomorrowAssignee)
        assertNotEquals(tomorrowAssignee, nextAssignee)
    }

    @Test
    fun allMembersAwayFallsBackToOriginalAssignee() {
        val directDraft = testDraft(
            assignee = "Erick",
            autoAssign = false,
            smartAssignmentMode = ChoreSmartAssignmentModeValue.FairRotation,
            repeatFrequency = ChoreRepeatFrequencyValue.Daily,
            dailyRepeatMode = ChoreDailyModeValue.EveryDay,
            selectedWeekdays = emptySet(),
            dayAssignments = emptyMap(),
        )
        val members = listOf(
            schedulingMember(
                name = "Erick",
                mode = ChoreAvailabilityMode.DayOff,
                startDate = scheduleDateKey(0),
                endDate = scheduleDateKey(2),
            ),
            schedulingMember(
                name = "Emma",
                mode = ChoreAvailabilityMode.GoneMode,
                startDate = scheduleDateKey(0),
                endDate = scheduleDateKey(2),
            ),
        )

        val resolution = resolveAssignmentForDay(
            draft = directDraft,
            storedAssignee = "Erick",
            day = dayAtOffset(1),
            loads = emptyList(),
            choreId = "all-away",
            householdMembers = members,
            todayWeekdayName = currentWeekdayName(),
        )

        assertEquals("Erick", resolution.assignee)
        assertTrue(resolution.reason.contains("Everyone is marked away"))
    }

    @Test
    fun inMemoryRepositoryReturnsToOriginalAssigneeAfterAvailabilityWindow() = runTest {
        val repository = InMemoryChoreRepository(
            initialHouseholdMembers = listOf(
                schedulingMember(
                    name = "Erick",
                    mode = ChoreAvailabilityMode.DayOff,
                    startDate = scheduleDateKey(0),
                    endDate = scheduleDateKey(1),
                ),
                schedulingMember(name = "Emma"),
            ),
        )
        val choreId = repository.saveChore(
            testDraft(
                id = "availability-repo",
                title = "Availability Test Chore",
                assignee = "Erick",
                autoAssign = false,
                smartAssignmentMode = ChoreSmartAssignmentModeValue.FairRotation,
                repeatFrequency = ChoreRepeatFrequencyValue.Daily,
                dailyRepeatMode = ChoreDailyModeValue.EveryDay,
                selectedWeekdays = emptySet(),
                dayAssignments = emptyMap(),
            ),
        )

        val awayDay = repository.getScheduledChores(dayAtOffset(0)).first { it.id == choreId }
        val backDay = repository.getScheduledChores(dayAtOffset(2)).first { it.id == choreId }

        assertEquals("Emma", awayDay.assignee)
        assertTrue(awayDay.assignmentReason.contains("Covering for Erick"))
        assertEquals("Erick", backDay.assignee)
        assertEquals("", backDay.assignmentReason)
    }

    @Test
    fun explicitAssignmentBroadensOutsideRotationWhenPoolIsAway() {
        val draft = testDraft(
            assignee = "",
            autoAssign = true,
            smartAssignmentMode = ChoreSmartAssignmentModeValue.DayPlan,
            repeatFrequency = ChoreRepeatFrequencyValue.Daily,
            dailyRepeatMode = ChoreDailyModeValue.EveryDay,
            selectedWeekdays = emptySet(),
            dayAssignments = mapOf(currentWeekdayName() to "Erick"),
        ).copy(assignmentPool = setOf("Erick", "Emma"))
        val members = listOf(
            schedulingMember(
                name = "Erick",
                mode = ChoreAvailabilityMode.DayOff,
                startDate = scheduleDateKey(0),
                endDate = scheduleDateKey(2),
            ),
            schedulingMember(
                name = "Emma",
                mode = ChoreAvailabilityMode.DayOff,
                startDate = scheduleDateKey(0),
                endDate = scheduleDateKey(2),
            ),
            schedulingMember(name = "Mila"),
        )

        val resolution = resolveAssignmentForDay(
            draft = draft,
            storedAssignee = "",
            day = dayAtOffset(0),
            loads = emptyList(),
            choreId = "outside-rotation",
            householdMembers = members,
            todayWeekdayName = currentWeekdayName(),
        )
        val suggestion = buildAvailabilitySuggestion(
            choreId = "outside-rotation",
            choreTitle = "Vacuum hallway",
            day = dayAtOffset(0),
            resolution = resolution,
        )

        assertEquals("Mila", resolution.assignee)
        assertTrue(resolution.outsideNormalRotation)
        assertTrue(suggestion?.outsideNormalRotation == true)
    }

    @Test
    fun acceptedDecisionOverridesCoverMemberWithinAwayWindow() {
        val members = listOf(
            schedulingMember(
                name = "Erick",
                mode = ChoreAvailabilityMode.DayOff,
                startDate = scheduleDateKey(0),
                endDate = scheduleDateKey(3),
            ),
            schedulingMember(name = "Emma"),
            schedulingMember(name = "Mila"),
        )

        val resolution = resolveAssignmentForDay(
            draft = testDraft(
                assignee = "Erick",
                autoAssign = false,
                smartAssignmentMode = ChoreSmartAssignmentModeValue.FairRotation,
                repeatFrequency = ChoreRepeatFrequencyValue.Daily,
                dailyRepeatMode = ChoreDailyModeValue.EveryDay,
                selectedWeekdays = emptySet(),
                dayAssignments = emptyMap(),
            ),
            storedAssignee = "Erick",
            day = dayAtOffset(1),
            loads = emptyList(),
            choreId = "accepted-cover",
            householdMembers = members,
            decisions = listOf(
                AvailabilitySuggestionDecision(
                    suggestionId = "accepted-cover",
                    choreId = "accepted-cover",
                    originalAssignee = "Erick",
                    coverMember = "Mila",
                    scopeStartDate = scheduleDateKey(0),
                    scopeEndDate = scheduleDateKey(3),
                    status = AvailabilitySuggestionDecisionStatus.Accepted,
                ),
            ),
            todayWeekdayName = currentWeekdayName(),
        )

        assertEquals("Mila", resolution.assignee)
        assertTrue(resolution.reason.contains("Covering for Erick"))
    }

    @Test
    fun inactiveMembersAreExcludedFromFairRotation() {
        val resolution = resolveAssignmentForDay(
            draft = testDraft(
                smartAssignmentMode = ChoreSmartAssignmentModeValue.FairRotation,
                repeatFrequency = ChoreRepeatFrequencyValue.Daily,
                dailyRepeatMode = ChoreDailyModeValue.EveryDay,
                selectedWeekdays = emptySet(),
                dayAssignments = emptyMap(),
            ),
            storedAssignee = "",
            day = dayAtOffset(0),
            loads = emptyList(),
            choreId = "inactive-member",
            householdMembers = listOf(
                schedulingMember(name = "Erick", active = false),
                schedulingMember(name = "Emma"),
            ),
            todayWeekdayName = currentWeekdayName(),
        )

        assertEquals("Emma", resolution.assignee)
    }

    @Test
    fun allMembersUnavailableDoesNotCreateActionableSuggestion() {
        val members = listOf(
            schedulingMember(
                name = "Erick",
                mode = ChoreAvailabilityMode.DayOff,
                startDate = scheduleDateKey(0),
                endDate = scheduleDateKey(2),
            ),
            schedulingMember(
                name = "Emma",
                mode = ChoreAvailabilityMode.DayOff,
                startDate = scheduleDateKey(0),
                endDate = scheduleDateKey(2),
            ),
        )
        val resolution = resolveAssignmentForDay(
            draft = testDraft(
                assignee = "Erick",
                autoAssign = false,
                smartAssignmentMode = ChoreSmartAssignmentModeValue.FairRotation,
                repeatFrequency = ChoreRepeatFrequencyValue.Daily,
                dailyRepeatMode = ChoreDailyModeValue.EveryDay,
                selectedWeekdays = emptySet(),
                dayAssignments = emptyMap(),
            ),
            storedAssignee = "Erick",
            day = dayAtOffset(1),
            loads = emptyList(),
            choreId = "nobody-available",
            householdMembers = members,
            todayWeekdayName = currentWeekdayName(),
        )

        assertEquals("Erick", resolution.assignee)
        assertEquals(
            null,
            buildAvailabilitySuggestion(
                choreId = "nobody-available",
                choreTitle = "Trash",
                day = dayAtOffset(1),
                resolution = resolution,
            ),
        )
    }

    private fun testDraft(
        id: String? = null,
        title: String = "Rotation Test",
        assignee: String = "",
        autoAssign: Boolean = true,
        smartAssignmentMode: ChoreSmartAssignmentModeValue,
        repeatFrequency: ChoreRepeatFrequencyValue,
        dailyRepeatMode: ChoreDailyModeValue = ChoreDailyModeValue.EveryDay,
        selectedWeekdays: Set<String>,
        dayAssignments: Map<String, String>,
    ): ChoreDraft = ChoreDraft(
        id = id,
        title = title,
        additionalComments = "",
        roomId = "room-kitchen",
        roomName = "Kitchen",
        roomIconKey = "kitchen",
        roomColorKey = "teal",
        assignee = assignee,
        autoAssign = autoAssign,
        smartAssignmentMode = smartAssignmentMode,
        assignmentPool = setOf("Erick", "Emma"),
        dayAssignments = dayAssignments,
        effortLevel = ChoreEffortValue.Standard,
        repeatsEnabled = true,
        repeatStartDate = "Today",
        repeatFrequency = repeatFrequency,
        dailyRepeatMode = dailyRepeatMode,
        selectedWeekdays = selectedWeekdays,
        monthlyPattern = "",
        customPattern = "",
        selectedOneTimeDate = "Today",
        proofRequired = false,
        durationMinutes = 15,
        isActive = true,
    )

    private fun dayAtOffset(dayOffset: Int): ChoreScheduleDay = ChoreScheduleDay(
        id = "day-$dayOffset",
        weekdayName = weekdayNameForDayOffset(dayOffset),
        dayOffset = dayOffset,
        isToday = dayOffset == 0,
        dateKey = scheduleDateKey(dayOffset),
    )

    private fun schedulingMember(
        name: String,
        mode: ChoreAvailabilityMode = ChoreAvailabilityMode.Available,
        startDate: String = "",
        endDate: String = "",
        active: Boolean = true,
    ): HouseholdSchedulingMember = HouseholdSchedulingMember(
        userId = name.lowercase(),
        displayName = name,
        active = active,
        availabilityMode = mode,
        availabilityStartDate = startDate,
        availabilityEndDate = endDate,
    )
}
