package com.sortoapps.chorepilot.features.chores.data

import com.sortoapps.chorepilot.core.demo.DemoChoreStatus
import com.sortoapps.chorepilot.core.demo.DemoContent
import com.sortoapps.chorepilot.features.chores.domain.ChoreDraft
import com.sortoapps.chorepilot.features.chores.domain.ChoreEffortValue
import com.sortoapps.chorepilot.features.chores.domain.ChoreItem
import com.sortoapps.chorepilot.features.chores.domain.ChoreItemStatus
import com.sortoapps.chorepilot.features.chores.domain.ChoreScheduleDay
import com.sortoapps.chorepilot.features.chores.domain.ChoreRepeatFrequencyValue
import com.sortoapps.chorepilot.features.chores.domain.ChoreRepository
import com.sortoapps.chorepilot.features.chores.domain.ChoreAssignmentSource
import com.sortoapps.chorepilot.features.chores.domain.ChoreSmartAssignmentModeValue
import com.sortoapps.chorepilot.features.chores.domain.ChoreDailyModeValue
import com.sortoapps.chorepilot.features.chores.domain.AvailabilitySuggestion
import com.sortoapps.chorepilot.features.chores.domain.AvailabilitySuggestionDecision
import com.sortoapps.chorepilot.features.chores.domain.AvailabilitySuggestionDecisionStatus
import com.sortoapps.chorepilot.features.chores.domain.appearsOnDay
import com.sortoapps.chorepilot.features.chores.domain.availabilitySuggestionId
import com.sortoapps.chorepilot.features.chores.domain.buildAvailabilitySuggestion
import com.sortoapps.chorepilot.features.chores.domain.currentWeekdayName
import com.sortoapps.chorepilot.features.chores.domain.defaultStatusForDay
import com.sortoapps.chorepilot.features.chores.domain.nextCompletionStatusForDay
import com.sortoapps.chorepilot.features.chores.domain.ProofUploadPayload
import com.sortoapps.chorepilot.features.chores.domain.ProofEvidence
import com.sortoapps.chorepilot.features.chores.domain.ProofReviewStatus
import com.sortoapps.chorepilot.features.chores.domain.ProofUploadResult
import com.sortoapps.chorepilot.features.chores.domain.RoomDefinition
import com.sortoapps.chorepilot.features.chores.domain.sortScheduledChores
import com.sortoapps.chorepilot.features.chores.domain.todayScheduleDay
import com.sortoapps.chorepilot.features.chores.domain.ChoreAvailabilityMode
import com.sortoapps.chorepilot.features.chores.domain.HouseholdSchedulingMember
import com.sortoapps.chorepilot.features.chores.domain.resolveAssignmentForDay
import com.sortoapps.chorepilot.features.chores.domain.resolveAssignmentsForDay
import com.sortoapps.chorepilot.features.notifications.domain.AssignmentChangedNotification
import com.sortoapps.chorepilot.features.notifications.domain.DiscoveryChoreApprovedNotification
import com.sortoapps.chorepilot.features.notifications.domain.DiscoveryChoreRejectedNotification
import com.sortoapps.chorepilot.features.notifications.domain.DueSoonNotification
import com.sortoapps.chorepilot.features.notifications.domain.HouseholdNotifications
import com.sortoapps.chorepilot.features.notifications.domain.NoOpHouseholdNotifications
import com.sortoapps.chorepilot.features.notifications.domain.OverdueNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Clock

class InMemoryChoreRepository(
    private val initialHouseholdMembers: List<HouseholdSchedulingMember> = defaultSchedulingMembers(),
    private val householdNotifications: HouseholdNotifications = NoOpHouseholdNotifications(),
) : ChoreRepository {
    private data class OccurrenceState(
        val status: ChoreItemStatus,
        val dueLabel: String,
        val completedByName: String,
        val completedAtEpochMs: Long,
        val proofPhotoCount: Int,
    )

    private val backingChores = DemoContent.chores.map { chore ->
        ChoreItem(
            id = chore.id,
            title = chore.title,
            area = chore.area,
            cadence = chore.cadence,
            assignee = chore.assignee,
            assigneeId = chore.assigneeId,
            status = when (chore.status) {
                DemoChoreStatus.DueSoon -> ChoreItemStatus.DueSoon
                DemoChoreStatus.PendingProof -> ChoreItemStatus.PendingProof
                DemoChoreStatus.Completed -> ChoreItemStatus.Completed
                DemoChoreStatus.Overdue -> ChoreItemStatus.Overdue
                DemoChoreStatus.Scheduled -> ChoreItemStatus.Scheduled
            },
            durationLabel = chore.durationLabel,
            dueLabel = chore.dueLabel,
            description = chore.description,
            reminder = chore.reminder,
            tags = chore.tags,
            proofRequired = chore.tags.any { it.contains("Proof", ignoreCase = true) },
            proofPhotoCount = if (chore.status == DemoChoreStatus.PendingProof) 1 else 0,
        )
    }.toMutableList()

    private val backingRooms = listOf(
        RoomDefinition(id = "room-kitchen", name = "Kitchen", iconKey = "kitchen", colorKey = "teal"),
        RoomDefinition(id = "room-living", name = "Living room", iconKey = "living", colorKey = "violet"),
        RoomDefinition(id = "room-bedroom", name = "Bedroom", iconKey = "bedroom", colorKey = "amber"),
        RoomDefinition(id = "room-bathroom", name = "Bathroom", iconKey = "bathroom", colorKey = "sky"),
    )
    private val draftsById = mutableMapOf<String, ChoreDraft>()

    private val occurrenceStates = mutableMapOf<String, MutableMap<String, OccurrenceState>>()
    private val proofEvidence = mutableListOf<ProofEvidence>()
    private val reassignmentDecisions = mutableListOf<AvailabilitySuggestionDecision>()

    private val _chores = MutableStateFlow(backingChores.toList())
    override val chores: StateFlow<List<ChoreItem>> = _chores

    private val _rooms = MutableStateFlow(backingRooms)
    override val rooms: StateFlow<List<RoomDefinition>> = _rooms

    private val _householdMembers = MutableStateFlow(initialHouseholdMembers)
    override val householdMembers: StateFlow<List<HouseholdSchedulingMember>> = _householdMembers

    override fun getChores(): List<ChoreItem> {
        val scheduledById = getScheduledChores(todayScheduleDay()).associateBy(ChoreItem::id)
        return backingChores.map { chore ->
            scheduledById[chore.id] ?: chore
        }
    }

    override fun getChore(id: String, day: ChoreScheduleDay?): ChoreItem? =
        getScheduledChores(day ?: todayScheduleDay()).firstOrNull { it.id == id }
            ?: backingChores.firstOrNull { it.id == id }

    override fun getLatestProofForChore(
        choreId: String,
        day: ChoreScheduleDay?,
    ): ProofEvidence? = proofEvidence
        .asSequence()
        .filter { it.choreId == choreId }
        .filter { evidence -> day == null || evidence.dateKey == day.dateKey }
        .maxByOrNull { it.createdAtEpochMs }

    override fun getDraft(id: String): ChoreDraft? = draftsById[id] ?: backingChores.firstOrNull { it.id == id }?.let { chore ->
        val activeMemberNames = householdMembers.value
            .filter { it.active }
            .map { it.displayName }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }
        ChoreDraft(
            id = chore.id,
            title = chore.title,
            additionalComments = chore.description,
            roomId = backingRooms.firstOrNull { it.name == chore.area }?.id ?: "room-kitchen",
            roomName = chore.area,
            roomIconKey = "kitchen",
            roomColorKey = "teal",
            assignee = chore.assignee,
            autoAssign = chore.assignee.isBlank(),
            smartAssignmentMode = ChoreSmartAssignmentModeValue.FairRotation,
            assignmentPool = activeMemberNames.toSet(),
            dayAssignments = emptyMap(),
            effortLevel = ChoreEffortValue.Standard,
            repeatsEnabled = true,
            repeatStartDate = "Today",
            repeatFrequency = ChoreRepeatFrequencyValue.Weekly,
            dailyRepeatMode = ChoreDailyModeValue.Weekdays,
            selectedWeekdays = setOf("Monday", "Wednesday", "Friday"),
            monthlyPattern = "15th",
            customPattern = "Every 2 weeks",
            selectedOneTimeDate = "Today",
            proofRequired = chore.tags.any { it.contains("Proof", ignoreCase = true) },
            durationMinutes = chore.durationLabel.filter(Char::isDigit).toIntOrNull(),
            isActive = chore.isActive,
            pendingApproval = chore.pendingApproval,
            submitterUserId = chore.submitterUserId,
            submitterName = chore.submitterName,
            reviewRejectedReason = chore.reviewRejectedReason,
            reviewedByName = chore.reviewedByName,
            reviewedAtEpochMs = chore.reviewedAtEpochMs,
            discoverySessionId = chore.discoverySessionId,
            discoveryImageStoragePath = chore.discoveryImageStoragePath,
            discoveryImageUrl = chore.discoveryImageUrl,
            discoveryGenerated = chore.discoveryGenerated,
        )
    }

    override fun getScheduledChores(day: ChoreScheduleDay): List<ChoreItem> {
        val todayWeekdayName = currentWeekdayName()
        val resolvedAssignments = resolvedAssignmentsFor(day, todayWeekdayName)
        return sortScheduledChores(
            backingChores
            .filter { chore ->
                val draft = draftsById[chore.id] ?: return@filter true
                draft.appearsOnDay(day = day, todayWeekdayName = todayWeekdayName)
            }
            .map { chore ->
                val draft = draftsById[chore.id]
                val occurrence = occurrenceStates[chore.id]?.get(day.dateKey)
                val status = occurrence?.status ?: defaultStatusForDay(chore.status, day)
                val proofPhotoCount = occurrence?.proofPhotoCount ?: if (day.isToday) chore.proofPhotoCount else 0
                val dueLabel = occurrence?.dueLabel?.takeIf(String::isNotBlank)
                    ?: if (day.isToday) chore.dueLabel else "Scheduled for ${day.weekdayName}"
                val latestProof = latestProofFor(chore.id, day.dateKey)
                val resolution = resolvedAssignments[chore.id] ?: draft?.let {
                    resolveAssignmentForDay(
                        draft = it,
                        storedAssignee = chore.assignee,
                        day = day,
                        loads = emptyList(),
                        choreId = chore.id,
                        householdMembers = householdMembers.value,
                        decisions = reassignmentDecisions,
                        todayWeekdayName = todayWeekdayName,
                    )
                }
                chore.copy(
                    assignee = resolution?.assignee ?: chore.assignee,
                    assigneeId = (resolution?.assignee ?: chore.assignee).lowercase().replace(" ", "-"),
                    status = status,
                    assignmentReason = resolution?.reason.orEmpty(),
                    assignmentOriginalAssignee = resolution?.originalAssignee?.takeIf { original ->
                        original.isNotBlank() &&
                            !original.equals(resolution.assignee, ignoreCase = true)
                    }.orEmpty(),
                    dueLabel = if (status == ChoreItemStatus.PendingProof && proofPhotoCount > 0) {
                        "$proofPhotoCount photo${if (proofPhotoCount == 1) "" else "s"} uploaded"
                    } else if (latestProof?.reviewStatus == ProofReviewStatus.Rejected) {
                        "Needs new proof"
                    } else {
                        dueLabel
                    },
                    proofPhotoCount = proofPhotoCount,
                    completedByName = occurrence?.completedByName ?: if (day.isToday) chore.completedByName else "",
                    completedAtEpochMs = occurrence?.completedAtEpochMs ?: if (day.isToday) chore.completedAtEpochMs else 0L,
                )
            },
        )
    }

    override suspend fun refresh() {
        refreshTodaySchedule()
    }

    override suspend fun getAvailabilitySuggestions(days: List<ChoreScheduleDay>): List<AvailabilitySuggestion> {
        val todayWeekdayName = currentWeekdayName()
        return days.flatMap { day ->
            val resolvedAssignments = resolvedAssignmentsFor(day, todayWeekdayName)
            draftsById.values.mapNotNull { draft ->
                val choreId = draft.id.orEmpty()
                if (choreId.isBlank() || !draft.appearsOnDay(day, todayWeekdayName)) return@mapNotNull null
                val resolution = resolvedAssignments[choreId] ?: return@mapNotNull null
                buildAvailabilitySuggestion(
                    choreId = choreId,
                    choreTitle = draft.title,
                    day = day,
                    resolution = resolution,
                    decisions = reassignmentDecisions,
                )
            }
        }.sortedWith(compareBy<AvailabilitySuggestion>({ it.dateKey }, { it.choreTitle.lowercase() }))
    }

    override suspend fun saveChore(draft: ChoreDraft): String {
        val id = draft.id ?: "chore-${backingChores.size + 1}"
        val existing = backingChores.firstOrNull { it.id == id }
        val preserveRuntimeState = existing?.let { chore ->
            hasRuntimeHistory(
                chore = chore,
                occurrenceCount = occurrenceStates[id]?.size ?: 0,
            )
        } == true
        val preservedItem = if (preserveRuntimeState) requireNotNull(existing) else null
        val preservedStatus = preservedItem?.status ?: ChoreItemStatus.DueSoon
        val preservedDueLabel = preservedItem?.dueLabel ?: defaultDueLabel(draft)
        val preservedCompletedByName = preservedItem?.completedByName.orEmpty()
        val preservedCompletedAtEpochMs = preservedItem?.completedAtEpochMs ?: 0L
        val preservedProofPhotoCount = preservedItem?.proofPhotoCount ?: 0
        draftsById[id] = draft.copy(id = id)
        val item = ChoreItem(
            id = id,
            title = draft.title,
            area = draft.roomName,
            cadence = if (draft.repeatsEnabled) draft.repeatFrequency.name else "Once",
            assignee = draft.assignee,
            assigneeId = draft.assignee.lowercase().replace(" ", "-"),
            status = preservedStatus,
            durationLabel = draft.durationMinutes?.let { "$it min" } ?: "No estimate",
            dueLabel = preservedDueLabel,
            description = draft.additionalComments,
            reminder = "",
            tags = listOf(draft.roomName),
            isActive = draft.isActive,
            pendingApproval = draft.pendingApproval,
            submitterUserId = draft.submitterUserId,
            submitterName = draft.submitterName,
            reviewRejectedReason = draft.reviewRejectedReason,
            reviewedByName = draft.reviewedByName,
            reviewedAtEpochMs = draft.reviewedAtEpochMs,
            assignmentReason = existing?.assignmentReason.orEmpty(),
            assignmentOriginalAssignee = existing?.assignmentOriginalAssignee.orEmpty(),
            completedByName = preservedCompletedByName,
            completedAtEpochMs = preservedCompletedAtEpochMs,
            proofRequired = draft.proofRequired,
            proofPhotoCount = preservedProofPhotoCount,
            discoverySessionId = draft.discoverySessionId,
            discoveryImageStoragePath = draft.discoveryImageStoragePath,
            discoveryImageUrl = draft.discoveryImageUrl,
            discoveryGenerated = draft.discoveryGenerated,
        )
        backingChores.removeAll { it.id == id }
        backingChores.add(item)
        refreshTodaySchedule()
        return id
    }

    override suspend fun deleteChore(choreId: String) {
        backingChores.removeAll { it.id == choreId }
        draftsById.remove(choreId)
        occurrenceStates.remove(choreId)
        proofEvidence.removeAll { it.choreId == choreId }
        reassignmentDecisions.removeAll { it.choreId == choreId }
        refreshTodaySchedule()
    }

    override suspend fun approvePendingChore(choreId: String) {
        val index = backingChores.indexOfFirst { it.id == choreId }
        if (index == -1) return
        val current = backingChores[index]
        if (!current.pendingApproval) return
        val reviewedAtEpochMs = Clock.System.now().toEpochMilliseconds()
        val reviewedByName = "Household admin"
        val updated = current.copy(
            isActive = true,
            pendingApproval = false,
            reviewRejectedReason = "",
            reviewedByName = reviewedByName,
            reviewedAtEpochMs = reviewedAtEpochMs,
            dueLabel = current.dueLabel.ifBlank { "Due later today" },
        )
        backingChores[index] = updated
        draftsById[choreId] = draftsById[choreId]?.copy(
            isActive = true,
            pendingApproval = false,
            reviewRejectedReason = "",
            reviewedByName = reviewedByName,
            reviewedAtEpochMs = reviewedAtEpochMs,
        ) ?: return
        runCatching {
            householdNotifications.onDiscoveryChoreApproved(
                DiscoveryChoreApprovedNotification(
                    householdId = "demo-household",
                    choreId = updated.id,
                    choreTitle = updated.title,
                    submittingMemberName = updated.submitterName.ifBlank { updated.assignee },
                    reviewerName = reviewedByName,
                    recipientUserId = updated.submitterUserId,
                ),
            )
        }
        refreshTodaySchedule()
    }

    override suspend fun rejectPendingChore(
        choreId: String,
        reason: String,
    ) {
        val rejectionReason = reason.trim()
        if (rejectionReason.isBlank()) return
        val index = backingChores.indexOfFirst { it.id == choreId }
        if (index == -1) return
        val current = backingChores[index]
        if (!current.pendingApproval) return
        val reviewedAtEpochMs = Clock.System.now().toEpochMilliseconds()
        val reviewedByName = "Household admin"
        val updated = current.copy(
            isActive = false,
            pendingApproval = false,
            reviewRejectedReason = rejectionReason,
            reviewedByName = reviewedByName,
            reviewedAtEpochMs = reviewedAtEpochMs,
            dueLabel = "Needs review updates",
        )
        backingChores[index] = updated
        draftsById[choreId] = draftsById[choreId]?.copy(
            isActive = false,
            pendingApproval = false,
            reviewRejectedReason = rejectionReason,
            reviewedByName = reviewedByName,
            reviewedAtEpochMs = reviewedAtEpochMs,
        ) ?: return
        runCatching {
            householdNotifications.onDiscoveryChoreRejected(
                DiscoveryChoreRejectedNotification(
                    householdId = "demo-household",
                    choreId = updated.id,
                    choreTitle = updated.title,
                    submittingMemberName = updated.submitterName.ifBlank { updated.assignee },
                    reviewerName = reviewedByName,
                    rejectionReason = rejectionReason,
                    recipientUserId = updated.submitterUserId,
                ),
            )
        }
        refreshTodaySchedule()
    }

    override suspend fun toggleCompletion(
        choreId: String,
        viewerName: String,
        allowCompletingForSomeoneElse: Boolean,
        day: ChoreScheduleDay?,
    ) {
        val index = backingChores.indexOfFirst { it.id == choreId }
        if (index == -1) return
        val existing = backingChores[index]
        val targetDay = day ?: todayScheduleDay()
        val scheduledChore = getScheduledChores(targetDay).firstOrNull { it.id == choreId } ?: existing
        val occurrence = occurrenceStates[choreId]?.get(targetDay.dateKey)
        val currentStatus = occurrence?.status ?: defaultStatusForDay(existing.status, targetDay)
        val requiresConfirmation = existing.assignee.isNotBlank() &&
            !scheduledChore.assignee.equals(viewerName, ignoreCase = true)
        if (requiresConfirmation && !allowCompletingForSomeoneElse) return
        val shouldDeleteProof = currentStatus == ChoreItemStatus.Completed || currentStatus == ChoreItemStatus.PendingProof
        val nextStatus = nextCompletionStatusForDay(
            currentStatus = currentStatus,
            proofRequired = existing.proofRequired,
            day = targetDay,
        )
        if (shouldDeleteProof) {
            proofEvidence.removeAll { proof ->
                proof.choreId == choreId && proof.dateKey == targetDay.dateKey
            }
        }
        val completedAtEpochMs = when (nextStatus) {
            ChoreItemStatus.Completed,
            ChoreItemStatus.PendingProof -> (occurrence?.completedAtEpochMs ?: 0L).takeIf { it > 0L }
                ?: Clock.System.now().toEpochMilliseconds()

            else -> 0L
        }
        occurrenceStates.getOrPut(choreId) { mutableMapOf() }[targetDay.dateKey] = OccurrenceState(
            status = nextStatus,
            dueLabel = when (nextStatus) {
                ChoreItemStatus.Completed -> "Completed"
                ChoreItemStatus.PendingProof -> "Waiting for admin review"
                ChoreItemStatus.Scheduled -> "Scheduled for ${targetDay.weekdayName}"
                else -> "Due later today"
            },
            completedByName = if (nextStatus == ChoreItemStatus.Completed || nextStatus == ChoreItemStatus.PendingProof) {
                if (scheduledChore.assignee.equals(viewerName, ignoreCase = true)) "" else viewerName
            } else {
                ""
            },
            completedAtEpochMs = completedAtEpochMs,
            proofPhotoCount = if (shouldDeleteProof) {
                0
            } else {
                occurrence?.proofPhotoCount ?: if (targetDay.isToday) existing.proofPhotoCount else 0
            },
        )
        if (targetDay.isToday) {
            val todayOccurrence = occurrenceStates[choreId]?.get(targetDay.dateKey)!!
            backingChores[index] = existing.copy(
                status = todayOccurrence.status,
                dueLabel = todayOccurrence.dueLabel,
                completedByName = todayOccurrence.completedByName,
                completedAtEpochMs = todayOccurrence.completedAtEpochMs,
                proofPhotoCount = todayOccurrence.proofPhotoCount,
            )
            refreshTodaySchedule()
        } else {
            refreshTodaySchedule()
        }
    }

    override suspend fun approveProof(choreId: String, day: ChoreScheduleDay?) {
        val index = backingChores.indexOfFirst { it.id == choreId }
        if (index == -1) return
        val existing = backingChores[index]
        val targetDay = day ?: todayScheduleDay()
        val proofIndex = latestProofIndexFor(
            choreId = choreId,
            dateKey = targetDay.dateKey,
            reviewStatus = ProofReviewStatus.Pending,
        )
        if (proofIndex == -1) return
        val reviewedAtEpochMs = Clock.System.now().toEpochMilliseconds()
        occurrenceStates.getOrPut(choreId) { mutableMapOf() }[targetDay.dateKey] = OccurrenceState(
            status = ChoreItemStatus.Completed,
            dueLabel = "Approved",
            completedByName = occurrenceStates[choreId]?.get(targetDay.dateKey)?.completedByName
                ?: if (targetDay.isToday) existing.completedByName else "",
            completedAtEpochMs = occurrenceStates[choreId]?.get(targetDay.dateKey)?.completedAtEpochMs
                ?: if (targetDay.isToday) existing.completedAtEpochMs else Clock.System.now().toEpochMilliseconds(),
            proofPhotoCount = occurrenceStates[choreId]?.get(targetDay.dateKey)?.proofPhotoCount
                ?: if (targetDay.isToday) existing.proofPhotoCount else 0,
        )
        proofEvidence[proofIndex] = proofEvidence[proofIndex].copy(
            reviewStatus = ProofReviewStatus.Approved,
            reviewedByName = "Admin",
            reviewedAtEpochMs = reviewedAtEpochMs,
            rejectionReason = "",
        )
        if (targetDay.isToday) {
            val todayOccurrence = occurrenceStates[choreId]?.get(targetDay.dateKey)!!
            backingChores[index] = existing.copy(
                status = todayOccurrence.status,
                dueLabel = todayOccurrence.dueLabel,
                completedByName = todayOccurrence.completedByName,
                completedAtEpochMs = todayOccurrence.completedAtEpochMs,
                proofPhotoCount = todayOccurrence.proofPhotoCount,
            )
            refreshTodaySchedule()
        } else {
            refreshTodaySchedule()
        }
    }

    override suspend fun rejectProof(
        choreId: String,
        reason: String,
        day: ChoreScheduleDay?,
    ) {
        val index = backingChores.indexOfFirst { it.id == choreId }
        if (index == -1) return
        val existing = backingChores[index]
        val targetDay = day ?: todayScheduleDay()
        val proofIndex = latestProofIndexFor(
            choreId = choreId,
            dateKey = targetDay.dateKey,
            reviewStatus = ProofReviewStatus.Pending,
        )
        if (proofIndex == -1) return
        val occurrence = occurrenceStates[choreId]?.get(targetDay.dateKey)
        occurrenceStates.getOrPut(choreId) { mutableMapOf() }[targetDay.dateKey] = OccurrenceState(
            status = if (targetDay.isToday) ChoreItemStatus.DueSoon else ChoreItemStatus.Scheduled,
            dueLabel = "Needs new proof",
            completedByName = occurrence?.completedByName ?: if (targetDay.isToday) existing.completedByName else "",
            completedAtEpochMs = occurrence?.completedAtEpochMs ?: if (targetDay.isToday) existing.completedAtEpochMs else 0L,
            proofPhotoCount = occurrence?.proofPhotoCount ?: if (targetDay.isToday) existing.proofPhotoCount else 0,
        )
        proofEvidence[proofIndex] = proofEvidence[proofIndex].copy(
            reviewStatus = ProofReviewStatus.Rejected,
            reviewedByName = "Admin",
            reviewedAtEpochMs = Clock.System.now().toEpochMilliseconds(),
            rejectionReason = reason.trim(),
        )
        runCatching {
            householdNotifications.onProofRejected(
                com.sortoapps.chorepilot.features.notifications.domain.ProofRejectedNotification(
                    householdId = "demo-household",
                    choreId = choreId,
                    choreTitle = existing.title,
                    dateKey = targetDay.dateKey,
                    assigneeName = existing.assignee,
                    submittingMemberName = proofEvidence[proofIndex].uploadedByName.ifBlank { existing.assignee },
                    reviewerName = "Admin",
                    rejectionReason = reason.trim(),
                    recipientUserId = existing.assigneeId,
                ),
            )
        }
        if (targetDay.isToday) {
            val todayOccurrence = occurrenceStates[choreId]?.get(targetDay.dateKey)!!
            backingChores[index] = existing.copy(
                status = todayOccurrence.status,
                dueLabel = todayOccurrence.dueLabel,
                completedByName = todayOccurrence.completedByName,
                completedAtEpochMs = todayOccurrence.completedAtEpochMs,
                proofPhotoCount = todayOccurrence.proofPhotoCount,
            )
            refreshTodaySchedule()
        } else {
            refreshTodaySchedule()
        }
    }

    override suspend fun submitProof(
        choreId: String,
        payload: ProofUploadPayload,
        day: ChoreScheduleDay?,
    ): ProofUploadResult {
        val index = backingChores.indexOfFirst { it.id == choreId }
        if (index == -1) error("Chore not found.")
        val existing = backingChores[index]
        val targetDay = day ?: todayScheduleDay()
        val scheduledChore = getScheduledChores(targetDay).firstOrNull { it.id == choreId } ?: existing
        val currentOccurrence = occurrenceStates[choreId]?.get(targetDay.dateKey)
        val currentProofPhotoCount = currentOccurrence?.proofPhotoCount ?: if (targetDay.isToday) existing.proofPhotoCount else 0
        val proofId = "proof-${existing.id}-${currentProofPhotoCount + 1}"
        val createdAt = Clock.System.now().toEpochMilliseconds()
        occurrenceStates.getOrPut(choreId) { mutableMapOf() }[targetDay.dateKey] = OccurrenceState(
            status = ChoreItemStatus.PendingProof,
            dueLabel = "${currentProofPhotoCount + 1} photo${if (currentProofPhotoCount + 1 == 1) "" else "s"} uploaded",
            completedByName = currentOccurrence?.completedByName ?: "",
            completedAtEpochMs = currentOccurrence?.completedAtEpochMs
                ?: Clock.System.now().toEpochMilliseconds(),
            proofPhotoCount = currentProofPhotoCount + 1,
        )
        proofEvidence.add(
            ProofEvidence(
                proofId = proofId,
                choreId = choreId,
                dateKey = targetDay.dateKey,
                uploadedByUserId = "",
                uploadedByName = currentOccurrence?.completedByName ?: scheduledChore.assignee,
                storagePath = "debug/$proofId.${payload.fileExtension}",
                downloadUrl = "debug://$proofId",
                createdAtEpochMs = createdAt,
                reviewStatus = ProofReviewStatus.Pending,
            ),
        )
        if (targetDay.isToday) {
            val todayOccurrence = occurrenceStates[choreId]?.get(targetDay.dateKey)!!
            backingChores[index] = existing.copy(
                status = todayOccurrence.status,
                dueLabel = todayOccurrence.dueLabel,
                proofPhotoCount = todayOccurrence.proofPhotoCount,
                completedByName = todayOccurrence.completedByName,
                completedAtEpochMs = todayOccurrence.completedAtEpochMs,
            )
            refreshTodaySchedule()
        } else {
            refreshTodaySchedule()
        }
        return ProofUploadResult(
            proofId = proofId,
            storagePath = "debug/$proofId.${payload.fileExtension}",
            downloadUrl = "debug://$proofId",
        )
    }

    override suspend fun acceptAvailabilitySuggestion(suggestion: AvailabilitySuggestion) {
        saveDecision(
            AvailabilitySuggestionDecision(
                suggestionId = suggestion.id,
                choreId = suggestion.choreId,
                originalAssignee = suggestion.originalAssignee,
                coverMember = suggestion.recommendedCoverMember,
                scopeStartDate = suggestion.scopeStartDate,
                scopeEndDate = suggestion.scopeEndDate,
                status = AvailabilitySuggestionDecisionStatus.Accepted,
                createdAtEpochMs = Clock.System.now().toEpochMilliseconds(),
            ),
        )
        householdNotifications.onAssignmentChanged(
            AssignmentChangedNotification(
                householdId = "demo-household",
                choreId = suggestion.choreId,
                choreTitle = suggestion.choreTitle,
                dateKey = suggestion.dateKey,
                originalAssigneeName = suggestion.originalAssignee,
                newAssigneeName = suggestion.recommendedCoverMember,
            ),
        )
    }

    override suspend fun dismissAvailabilitySuggestion(suggestion: AvailabilitySuggestion) {
        saveDecision(
            AvailabilitySuggestionDecision(
                suggestionId = suggestion.id,
                choreId = suggestion.choreId,
                originalAssignee = suggestion.originalAssignee,
                coverMember = suggestion.recommendedCoverMember,
                scopeStartDate = suggestion.scopeStartDate,
                scopeEndDate = suggestion.scopeEndDate,
                status = AvailabilitySuggestionDecisionStatus.Dismissed,
                createdAtEpochMs = Clock.System.now().toEpochMilliseconds(),
            ),
        )
    }

    private fun resolvedAssignmentsFor(
        day: ChoreScheduleDay,
        todayWeekdayName: String,
    ): Map<String, com.sortoapps.chorepilot.features.chores.domain.ChoreAssignmentResolution> =
        resolveAssignmentsForDay(
            sources = backingChores.mapNotNull { chore ->
                val draft = draftsById[chore.id] ?: getDraft(chore.id) ?: return@mapNotNull null
                ChoreAssignmentSource(
                    choreId = chore.id,
                    storedAssignee = chore.assignee,
                    draft = draft,
                    effortScore = draft.effortLevel.score,
                )
            },
            day = day,
            householdMembers = householdMembers.value,
            decisions = reassignmentDecisions,
            todayWeekdayName = todayWeekdayName,
        )

    private suspend fun saveDecision(decision: AvailabilitySuggestionDecision) {
        val normalized = decision.copy(
            suggestionId = availabilitySuggestionId(
                choreId = decision.choreId,
                originalAssignee = decision.originalAssignee,
                scopeStartDate = decision.scopeStartDate,
                scopeEndDate = decision.scopeEndDate,
            ),
        )
        reassignmentDecisions.removeAll { existing ->
            existing.choreId == normalized.choreId &&
                existing.originalAssignee.equals(normalized.originalAssignee, ignoreCase = true) &&
                existing.scopeStartDate == normalized.scopeStartDate &&
                existing.scopeEndDate == normalized.scopeEndDate
        }
        reassignmentDecisions += normalized
        refreshTodaySchedule()
    }

    private suspend fun refreshTodaySchedule() {
        val today = todayScheduleDay()
        val scheduled = getScheduledChores(today)
        _chores.value = scheduled
        scheduled.forEach { chore ->
            when (chore.status) {
                ChoreItemStatus.DueSoon -> householdNotifications.onDueSoon(
                    DueSoonNotification(
                        householdId = "demo-household",
                        choreId = chore.id,
                        choreTitle = chore.title,
                        dateKey = today.dateKey,
                        assigneeName = chore.assignee,
                        dueLabel = chore.dueLabel,
                    ),
                )

                ChoreItemStatus.Overdue -> householdNotifications.onOverdue(
                    OverdueNotification(
                        householdId = "demo-household",
                        choreId = chore.id,
                        choreTitle = chore.title,
                        dateKey = today.dateKey,
                        assigneeName = chore.assignee,
                        dueLabel = chore.dueLabel,
                    ),
                )

                ChoreItemStatus.PendingProof,
                ChoreItemStatus.Completed,
                ChoreItemStatus.Scheduled,
                -> Unit
            }
        }
    }

    private fun latestProofFor(
        choreId: String,
        dateKey: String,
    ): ProofEvidence? = proofEvidence
        .asSequence()
        .filter { it.choreId == choreId && it.dateKey == dateKey }
        .maxByOrNull { it.createdAtEpochMs }

    private fun latestProofIndexFor(
        choreId: String,
        dateKey: String,
        reviewStatus: ProofReviewStatus,
    ): Int = proofEvidence
        .withIndex()
        .filter { indexed ->
            indexed.value.choreId == choreId &&
                indexed.value.dateKey == dateKey &&
                indexed.value.reviewStatus == reviewStatus
        }
        .maxByOrNull { it.value.createdAtEpochMs }
        ?.index ?: -1
}

private fun defaultSchedulingMembers(): List<HouseholdSchedulingMember> = listOf(
    HouseholdSchedulingMember(
        userId = "erick",
        displayName = "Erick",
        availabilityMode = ChoreAvailabilityMode.Available,
    ),
    HouseholdSchedulingMember(
        userId = "emma",
        displayName = "Emma",
        availabilityMode = ChoreAvailabilityMode.Available,
    ),
)

private fun defaultDueLabel(
    draft: ChoreDraft,
): String = if (draft.repeatsEnabled) {
    "Starts ${draft.repeatStartDate.lowercase()}"
} else {
    draft.selectedOneTimeDate
}

private fun hasRuntimeHistory(
    chore: ChoreItem,
    occurrenceCount: Int,
): Boolean = chore.status != ChoreItemStatus.DueSoon ||
    chore.proofPhotoCount > 0 ||
    chore.completedByName.isNotBlank() ||
    chore.completedAtEpochMs > 0L ||
    occurrenceCount > 0
