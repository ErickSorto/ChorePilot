package com.sortoapps.chorepilot.features.chores.data

import com.sortoapps.chorepilot.features.chores.domain.ChoreDailyModeValue
import com.sortoapps.chorepilot.features.chores.domain.ChoreAvailabilityMode
import com.sortoapps.chorepilot.features.chores.domain.ChoreDraft
import com.sortoapps.chorepilot.features.chores.domain.ChoreEffortValue
import com.sortoapps.chorepilot.features.chores.domain.ChoreItemStatus
import com.sortoapps.chorepilot.features.chores.domain.ChoreRepeatFrequencyValue
import com.sortoapps.chorepilot.features.chores.domain.ChoreSmartAssignmentModeValue
import com.sortoapps.chorepilot.features.chores.domain.HouseholdSchedulingMember
import com.sortoapps.chorepilot.features.chores.domain.ProofReviewStatus
import com.sortoapps.chorepilot.features.chores.domain.ProofUploadPayload
import com.sortoapps.chorepilot.features.chores.domain.ChoreScheduleDay
import com.sortoapps.chorepilot.features.chores.domain.scheduleDateKey
import com.sortoapps.chorepilot.features.chores.domain.weekdayNameForDayOffset
import com.sortoapps.chorepilot.features.chores.domain.todayScheduleDay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class InMemoryChoreRepositoryTest {
    @Test
    fun saveAndRefreshPreservesDirectAssignee() = runTest {
        val repository = InMemoryChoreRepository(
            initialHouseholdMembers = listOf(
                HouseholdSchedulingMember(userId = "ava", displayName = "Ava", active = true),
            ),
        )

        val choreId = repository.saveChore(
            baseDraft(
                assignee = "Ava",
                autoAssign = false,
            ),
        )

        repository.refresh()
        val draft = repository.getDraft(choreId)

        assertEquals("Ava", draft?.assignee)
        assertEquals(false, draft?.autoAssign)
    }

    @Test
    fun saveAndRefreshPreservesAutoAssignPoolAndDayPlan() = runTest {
        val repository = InMemoryChoreRepository(
            initialHouseholdMembers = listOf(
                HouseholdSchedulingMember(userId = "ava", displayName = "Ava", active = true),
                HouseholdSchedulingMember(userId = "liam", displayName = "Liam", active = true),
            ),
        )

        val choreId = repository.saveChore(
            baseDraft(
                assignee = "Ava",
                autoAssign = true,
                smartAssignmentMode = ChoreSmartAssignmentModeValue.DayPlan,
                assignmentPool = setOf("Ava", "Liam"),
                dayAssignments = mapOf(
                    "Monday" to "Ava",
                    "Wednesday" to "Smart fair",
                    "Friday" to "Liam",
                ),
            ),
        )

        repository.refresh()
        val draft = repository.getDraft(choreId)

        assertEquals(true, draft?.autoAssign)
        assertEquals(ChoreSmartAssignmentModeValue.DayPlan, draft?.smartAssignmentMode)
        assertEquals(setOf("Ava", "Liam"), draft?.assignmentPool)
        assertEquals("Ava", draft?.dayAssignments?.get("Monday"))
        assertEquals("Smart fair", draft?.dayAssignments?.get("Wednesday"))
        assertEquals("Liam", draft?.dayAssignments?.get("Friday"))
    }

    @Test
    fun generatedDraftUsesConfiguredHouseholdMembersInsteadOfHardcodedDefaults() = runTest {
        val repository = InMemoryChoreRepository(
            initialHouseholdMembers = listOf(
                HouseholdSchedulingMember(userId = "zoe", displayName = "Zoe", active = true),
            ),
        )

        val draft = repository.getDraft("kitchen-cleanup")

        assertTrue(draft != null)
        assertEquals(setOf("Zoe"), draft.assignmentPool)
    }

    @Test
    fun savingExistingChorePreservesProofHistoryWhileChangingLifecycleState() = runTest {
        val repository = InMemoryChoreRepository(
            initialHouseholdMembers = listOf(
                HouseholdSchedulingMember(userId = "ava", displayName = "Ava", active = true),
            ),
        )

        val choreId = repository.saveChore(
            baseDraft(
                assignee = "Ava",
                autoAssign = false,
            ).copy(
                proofRequired = true,
            ),
        )
        repository.submitProof(
            choreId = choreId,
            payload = ProofUploadPayload(
                localPath = "/tmp/proof.jpg",
                previewUri = "file:///tmp/proof.jpg",
            ),
        )

        repository.saveChore(
            baseDraft(
                assignee = "Ava",
                autoAssign = false,
            ).copy(
                id = choreId,
                title = "Updated trash",
                proofRequired = true,
                isActive = false,
            ),
        )

        assertEquals(false, repository.getDraft(choreId)?.isActive)
        assertEquals(ChoreItemStatus.PendingProof, repository.getChore(choreId)?.status)
        assertEquals(1, repository.getChore(choreId)?.proofPhotoCount)
        assertTrue(repository.getLatestProofForChore(choreId) != null)
    }

    @Test
    fun deleteChoreRemovesDraftOccurrenceAndProofHistory() = runTest {
        val repository = InMemoryChoreRepository(
            initialHouseholdMembers = listOf(
                HouseholdSchedulingMember(userId = "ava", displayName = "Ava", active = true),
            ),
        )

        val choreId = repository.saveChore(
            baseDraft(
                assignee = "Ava",
                autoAssign = false,
            ).copy(
                proofRequired = true,
            ),
        )
        repository.submitProof(
            choreId = choreId,
            payload = ProofUploadPayload(
                localPath = "/tmp/proof.jpg",
                previewUri = "file:///tmp/proof.jpg",
            ),
        )

        repository.deleteChore(choreId)

        assertNull(repository.getDraft(choreId))
        assertNull(repository.getChore(choreId))
        assertNull(repository.getLatestProofForChore(choreId))
        assertTrue(repository.getScheduledChores(todayScheduleDay()).none { it.id == choreId })
    }

    @Test
    fun getChoreForDayMatchesScheduledChoreResolution() = runTest {
        val repository = InMemoryChoreRepository(
            initialHouseholdMembers = listOf(
                HouseholdSchedulingMember(userId = "ava", displayName = "Ava", active = true),
                HouseholdSchedulingMember(userId = "liam", displayName = "Liam", active = true),
            ),
        )

        val choreId = repository.saveChore(
            baseDraft(
                assignee = "",
                autoAssign = true,
                assignmentPool = setOf("Ava", "Liam"),
            ).copy(
                repeatFrequency = ChoreRepeatFrequencyValue.Daily,
                dailyRepeatMode = ChoreDailyModeValue.EveryDay,
                selectedWeekdays = emptySet(),
            ),
        )
        val tomorrow = dayAtOffset(1)

        val fromList = repository.getScheduledChores(tomorrow).first { it.id == choreId }
        val fromSingle = repository.getChore(choreId, tomorrow)

        assertEquals(fromList.assignee, fromSingle?.assignee)
        assertEquals(fromList.assignmentReason, fromSingle?.assignmentReason)
    }

    @Test
    fun repeatedReadsReturnSameResolvedAssigneeForSameDate() = runTest {
        val repository = InMemoryChoreRepository(
            initialHouseholdMembers = listOf(
                HouseholdSchedulingMember(userId = "ava", displayName = "Ava", active = true),
                HouseholdSchedulingMember(userId = "liam", displayName = "Liam", active = true),
            ),
        )

        val choreId = repository.saveChore(
            baseDraft(
                assignee = "",
                autoAssign = true,
                assignmentPool = setOf("Ava", "Liam"),
            ).copy(
                repeatFrequency = ChoreRepeatFrequencyValue.Weekly,
                selectedWeekdays = setOf(weekdayNameForDayOffset(0)),
            ),
        )
        val today = dayAtOffset(0)

        val first = repository.getScheduledChores(today).first { it.id == choreId }
        val second = repository.getScheduledChores(today).first { it.id == choreId }

        assertEquals(first.assignee, second.assignee)
        assertEquals(first.assignmentReason, second.assignmentReason)
    }

    @Test
    fun acceptedSuggestionResolvesSameAssigneeAcrossListAndSingleRead() = runTest {
        val repository = InMemoryChoreRepository(
            initialHouseholdMembers = listOf(
                HouseholdSchedulingMember(
                    userId = "ava",
                    displayName = "Ava",
                    active = true,
                    availabilityMode = com.sortoapps.chorepilot.features.chores.domain.ChoreAvailabilityMode.DayOff,
                    availabilityStartDate = scheduleDateKey(0),
                    availabilityEndDate = scheduleDateKey(0),
                ),
                HouseholdSchedulingMember(userId = "liam", displayName = "Liam", active = true),
            ),
        )

        val choreId = repository.saveChore(
            baseDraft(
                assignee = "Ava",
                autoAssign = false,
            ).copy(
                repeatFrequency = ChoreRepeatFrequencyValue.Daily,
                dailyRepeatMode = ChoreDailyModeValue.EveryDay,
                selectedWeekdays = emptySet(),
            ),
        )
        val today = dayAtOffset(0)
        val suggestion = repository.getAvailabilitySuggestions(listOf(today)).first { it.choreId == choreId }

        repository.acceptAvailabilitySuggestion(suggestion)

        val fromList = repository.getScheduledChores(today).first { it.id == choreId }
        val fromSingle = repository.getChore(choreId, today)

        assertEquals(suggestion.recommendedCoverMember, fromList.assignee)
        assertEquals(fromList.assignee, fromSingle?.assignee)
        assertEquals(fromList.assignmentReason, fromSingle?.assignmentReason)
    }

    @Test
    fun completedOccurrenceDoesNotCarryIntoNextDate() = runTest {
        val repository = InMemoryChoreRepository(
            initialHouseholdMembers = listOf(
                HouseholdSchedulingMember(
                    userId = "ava",
                    displayName = "Ava",
                    active = true,
                    availabilityMode = ChoreAvailabilityMode.DayOff,
                    availabilityStartDate = scheduleDateKey(0),
                    availabilityEndDate = scheduleDateKey(0),
                ),
                HouseholdSchedulingMember(userId = "liam", displayName = "Liam", active = true),
            ),
        )
        val choreId = repository.saveChore(
            baseDraft(
                assignee = "Ava",
                autoAssign = false,
            ).copy(
                repeatFrequency = ChoreRepeatFrequencyValue.Daily,
                dailyRepeatMode = ChoreDailyModeValue.EveryDay,
                selectedWeekdays = emptySet(),
            ),
        )
        val today = dayAtOffset(0)
        val nextDateAsToday = dayAtOffset(1).copy(
            id = "next-date-as-today",
            dayOffset = 0,
            isToday = true,
        )

        assertEquals("Liam", repository.getScheduledChores(today).first { it.id == choreId }.assignee)
        repository.toggleCompletion(
            choreId = choreId,
            viewerName = "Liam",
            day = today,
        )

        val completedToday = repository.getScheduledChores(today).first { it.id == choreId }
        val nextDate = repository.getScheduledChores(nextDateAsToday).first { it.id == choreId }

        assertEquals(ChoreItemStatus.Completed, completedToday.status)
        assertEquals(ChoreItemStatus.DueSoon, nextDate.status)
        assertEquals("Ava", nextDate.assignee)
        assertEquals("", nextDate.completedByName)
        assertEquals(0L, nextDate.completedAtEpochMs)
    }

    @Test
    fun completedOccurrenceRemainsVisibleWhenSelectedDateBecomesPast() = runTest {
        val repository = InMemoryChoreRepository(
            initialHouseholdMembers = listOf(
                HouseholdSchedulingMember(userId = "ava", displayName = "Ava", active = true),
            ),
        )
        val choreId = repository.saveChore(
            baseDraft(
                assignee = "Ava",
                autoAssign = false,
            ).copy(
                repeatFrequency = ChoreRepeatFrequencyValue.Daily,
                dailyRepeatMode = ChoreDailyModeValue.EveryDay,
                selectedWeekdays = emptySet(),
            ),
        )
        val occurrenceDay = dayAtOffset(0)

        repository.toggleCompletion(
            choreId = choreId,
            viewerName = "Ava",
            day = occurrenceDay,
        )

        val sameDateAfterToday = occurrenceDay.copy(
            id = "past-${occurrenceDay.dateKey}",
            dayOffset = -1,
            isToday = false,
        )
        val pastChore = repository.getScheduledChores(sameDateAfterToday).first { it.id == choreId }

        assertEquals(ChoreItemStatus.Completed, pastChore.status)
        assertEquals("Completed", pastChore.dueLabel)
    }

    @Test
    fun proofLifecycleSupportsApproveRejectUndoAndResubmit() = runTest {
        val repository = InMemoryChoreRepository(
            initialHouseholdMembers = listOf(
                HouseholdSchedulingMember(userId = "ava", displayName = "Ava", active = true),
            ),
        )
        val choreId = repository.saveChore(
            baseDraft(
                assignee = "Ava",
                autoAssign = false,
            ).copy(proofRequired = true),
        )

        repository.submitProof(
            choreId = choreId,
            payload = ProofUploadPayload(
                localPath = "/tmp/proof-1.jpg",
                previewUri = "file:///tmp/proof-1.jpg",
            ),
        )

        repository.approveProof(choreId)
        assertEquals(ChoreItemStatus.Completed, repository.getChore(choreId)?.status)
        assertEquals(ProofReviewStatus.Approved, repository.getLatestProofForChore(choreId)?.reviewStatus)

        repository.toggleCompletion(
            choreId = choreId,
            viewerName = "Ava",
        )
        assertEquals(ChoreItemStatus.DueSoon, repository.getChore(choreId)?.status)
        assertNull(repository.getLatestProofForChore(choreId))

        repository.submitProof(
            choreId = choreId,
            payload = ProofUploadPayload(
                localPath = "/tmp/proof-2.jpg",
                previewUri = "file:///tmp/proof-2.jpg",
            ),
        )
        repository.rejectProof(choreId, "Too blurry")

        val rejected = repository.getLatestProofForChore(choreId)
        assertEquals(ChoreItemStatus.DueSoon, repository.getChore(choreId)?.status)
        assertEquals(ProofReviewStatus.Rejected, rejected?.reviewStatus)
        assertEquals("Too blurry", rejected?.rejectionReason)
        assertEquals("Needs new proof", repository.getChore(choreId)?.dueLabel)

        repository.submitProof(
            choreId = choreId,
            payload = ProofUploadPayload(
                localPath = "/tmp/proof-3.jpg",
                previewUri = "file:///tmp/proof-3.jpg",
            ),
        )

        val resubmitted = repository.getLatestProofForChore(choreId)
        assertEquals(ChoreItemStatus.PendingProof, repository.getChore(choreId)?.status)
        assertEquals(ProofReviewStatus.Pending, resubmitted?.reviewStatus)
        assertEquals(2, repository.getChore(choreId)?.proofPhotoCount)
    }
}

private fun dayAtOffset(dayOffset: Int): ChoreScheduleDay = ChoreScheduleDay(
    id = "test-day-$dayOffset",
    weekdayName = weekdayNameForDayOffset(dayOffset),
    dayOffset = dayOffset,
    isToday = dayOffset == 0,
    dateKey = scheduleDateKey(dayOffset),
)

private fun baseDraft(
    assignee: String,
    autoAssign: Boolean,
    smartAssignmentMode: ChoreSmartAssignmentModeValue = ChoreSmartAssignmentModeValue.FairRotation,
    assignmentPool: Set<String> = emptySet(),
    dayAssignments: Map<String, String> = emptyMap(),
): ChoreDraft = ChoreDraft(
    title = "Take out trash",
    additionalComments = "",
    roomId = "room-kitchen",
    roomName = "Kitchen",
    roomIconKey = "kitchen",
    roomColorKey = "teal",
    assignee = assignee,
    autoAssign = autoAssign,
    smartAssignmentMode = smartAssignmentMode,
    assignmentPool = assignmentPool,
    dayAssignments = dayAssignments,
    effortLevel = ChoreEffortValue.Standard,
    repeatsEnabled = true,
    repeatStartDate = "Today",
    repeatFrequency = ChoreRepeatFrequencyValue.Weekly,
    dailyRepeatMode = ChoreDailyModeValue.Weekdays,
    selectedWeekdays = setOf("Monday", "Wednesday", "Friday"),
    monthlyPattern = "",
    customPattern = "",
    selectedOneTimeDate = "Today",
    proofRequired = false,
    durationMinutes = 15,
)
