package com.sortoapps.chorepilot.features.chores.data

import com.sortoapps.chorepilot.core.firebase.FirestorePaths
import com.sortoapps.chorepilot.core.firebase.FirebaseHouseholdStore
import com.sortoapps.chorepilot.core.firebase.HouseholdAvailabilityMode
import com.sortoapps.chorepilot.core.firebase.HouseholdMemberDocument
import com.sortoapps.chorepilot.core.firebase.StoragePaths
import com.sortoapps.chorepilot.core.platform.localImagePathToFirebaseStorageData
import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import com.sortoapps.chorepilot.features.chores.data.model.ChoreDocument
import com.sortoapps.chorepilot.features.chores.data.model.ChoreOccurrenceDocument
import com.sortoapps.chorepilot.features.chores.data.model.ProofDocument
import com.sortoapps.chorepilot.features.chores.data.model.ReassignmentDecisionDocument
import com.sortoapps.chorepilot.features.chores.data.model.RoomDocument
import com.sortoapps.chorepilot.features.chores.data.model.toDecision
import com.sortoapps.chorepilot.features.chores.data.model.toDocument
import com.sortoapps.chorepilot.features.chores.data.model.toChoreItem
import com.sortoapps.chorepilot.features.chores.data.model.toDraft
import com.sortoapps.chorepilot.features.chores.data.model.toRoomDefinition
import com.sortoapps.chorepilot.features.chores.data.model.updatedFromDraft
import com.sortoapps.chorepilot.features.chores.domain.AvailabilitySuggestion
import com.sortoapps.chorepilot.features.chores.domain.AvailabilitySuggestionDecision
import com.sortoapps.chorepilot.features.chores.domain.AvailabilitySuggestionDecisionStatus
import com.sortoapps.chorepilot.features.chores.domain.ChoreDraft
import com.sortoapps.chorepilot.features.chores.domain.ChoreEffortValue
import com.sortoapps.chorepilot.features.chores.domain.ChoreItem
import com.sortoapps.chorepilot.features.chores.domain.ChoreItemStatus
import com.sortoapps.chorepilot.features.chores.domain.ChoreRepository
import com.sortoapps.chorepilot.features.chores.domain.ChoreScheduleDay
import com.sortoapps.chorepilot.features.chores.domain.ChoreSmartAssignmentModeValue
import com.sortoapps.chorepilot.features.chores.domain.ProofUploadPayload
import com.sortoapps.chorepilot.features.chores.domain.ProofEvidence
import com.sortoapps.chorepilot.features.chores.domain.ProofReviewStatus
import com.sortoapps.chorepilot.features.chores.domain.ProofUploadResult
import com.sortoapps.chorepilot.features.chores.domain.RoomDefinition
import com.sortoapps.chorepilot.features.chores.domain.ChoreAssignmentSource
import com.sortoapps.chorepilot.features.chores.domain.ChoreAvailabilityMode
import com.sortoapps.chorepilot.features.chores.domain.appearsOnDay
import com.sortoapps.chorepilot.features.chores.domain.approveProofStatus
import com.sortoapps.chorepilot.features.chores.domain.currentWeekdayName
import com.sortoapps.chorepilot.features.chores.domain.defaultStatusForDay
import com.sortoapps.chorepilot.features.chores.domain.HouseholdSchedulingMember
import com.sortoapps.chorepilot.features.chores.domain.availabilitySuggestionId
import com.sortoapps.chorepilot.features.chores.domain.buildAvailabilitySuggestion
import com.sortoapps.chorepilot.features.chores.domain.canManageHouseholdChores
import com.sortoapps.chorepilot.features.chores.domain.canUndoChoreCompletion
import com.sortoapps.chorepilot.features.chores.domain.nextCompletionStatus
import com.sortoapps.chorepilot.features.chores.domain.nextCompletionStatusForDay
import com.sortoapps.chorepilot.features.chores.domain.recordedCompletionName
import com.sortoapps.chorepilot.features.chores.domain.resolveAssignmentForDay
import com.sortoapps.chorepilot.features.chores.domain.resolveAssignmentsForDay
import com.sortoapps.chorepilot.features.chores.domain.resolvedAssigneeForDay
import com.sortoapps.chorepilot.features.chores.domain.requiresCompletionConfirmation
import com.sortoapps.chorepilot.features.chores.domain.sortScheduledChores
import com.sortoapps.chorepilot.features.chores.domain.todayScheduleDay
import com.sortoapps.chorepilot.features.notifications.domain.AssignmentChangedNotification
import com.sortoapps.chorepilot.features.notifications.domain.DiscoveryChoreApprovedNotification
import com.sortoapps.chorepilot.features.notifications.domain.DiscoveryChoreRejectedNotification
import com.sortoapps.chorepilot.features.notifications.domain.DueSoonNotification
import com.sortoapps.chorepilot.features.notifications.domain.HouseholdNotifications
import com.sortoapps.chorepilot.features.notifications.domain.NoOpHouseholdNotifications
import com.sortoapps.chorepilot.features.notifications.domain.OverdueNotification
import com.sortoapps.chorepilot.features.notifications.domain.ProofRejectedNotification
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.options
import dev.gitlive.firebase.storage.storage
import dev.gitlive.firebase.storage.storageMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.time.Clock

class FirebaseChoreRepository(
    private val sessionRepository: SessionRepository,
    private val householdStore: FirebaseHouseholdStore,
    appScope: CoroutineScope,
    private val householdNotifications: HouseholdNotifications = NoOpHouseholdNotifications(),
) : ChoreRepository {
    private val storage = Firebase.options.storageBucket
        ?.takeIf(String::isNotBlank)
        ?.let(::normalizeStorageBucketUrl)
        ?.let { Firebase.storage(it) }
        ?: Firebase.storage
    private val choreDocuments = linkedMapOf<String, ChoreDocument>()
    private val roomDefinitions = linkedMapOf<String, RoomDefinition>()
    private val proofDocuments = mutableListOf<ProofDocument>()
    private val reassignmentDecisions = mutableListOf<AvailabilitySuggestionDecision>()
    private var schedulingMembers = emptyList<HouseholdSchedulingMember>()

    private val _chores = MutableStateFlow(emptyList<ChoreItem>())
    override val chores: StateFlow<List<ChoreItem>> = _chores.asStateFlow()

    private val _rooms = MutableStateFlow(defaultRooms())
    override val rooms: StateFlow<List<RoomDefinition>> = _rooms.asStateFlow()

    private val _householdMembers = MutableStateFlow(emptyList<HouseholdSchedulingMember>())
    override val householdMembers: StateFlow<List<HouseholdSchedulingMember>> = _householdMembers.asStateFlow()

    init {
        appScope.launch {
            sessionRepository.session
                .map { session -> session.householdId to session.userId }
                .distinctUntilChanged()
                .collectLatest {
                    runCatching { refresh() }
                }
        }
    }

    override fun getChores(): List<ChoreItem> {
        val scheduledById = getScheduledChores(todayScheduleDay()).associateBy(ChoreItem::id)
        return choreDocuments.values.map { document ->
            scheduledById[document.id] ?: document.toChoreItem()
        }
    }

    override fun getChore(id: String, day: ChoreScheduleDay?): ChoreItem? =
        getScheduledChores(day ?: todayScheduleDay()).firstOrNull { it.id == id }
            ?: choreDocuments[id]?.toChoreItem()

    override fun getLatestProofForChore(
        choreId: String,
        day: ChoreScheduleDay?,
    ): ProofEvidence? = proofDocuments
        .asSequence()
        .filter { it.choreId == choreId }
        .filter { proof -> day == null || proof.dateKey == day.dateKey }
        .maxByOrNull { it.createdAtEpochMs }
        ?.let { proof ->
            ProofEvidence(
                proofId = proof.id,
                choreId = proof.choreId,
                dateKey = proof.dateKey,
                uploadedByUserId = proof.uploadedByUserId,
                uploadedByName = proof.uploadedByName,
                storagePath = proof.storagePath,
                downloadUrl = proof.downloadUrl,
                createdAtEpochMs = proof.createdAtEpochMs,
                reviewStatus = proof.reviewStatus,
                reviewedByName = proof.reviewedByName,
                reviewedAtEpochMs = proof.reviewedAtEpochMs,
                rejectionReason = proof.rejectionReason,
            )
        }

    override fun getDraft(id: String): ChoreDraft? = choreDocuments[id]?.toDraft()

    override fun getScheduledChores(day: ChoreScheduleDay): List<ChoreItem> {
        val todayWeekdayName = currentWeekdayName()
        val resolvedAssignments = resolvedAssignmentsFor(day, todayWeekdayName)
        return sortScheduledChores(
            choreDocuments.values
            .filter { document ->
                document.toDraft().appearsOnDay(
                    day = day,
                    todayWeekdayName = todayWeekdayName,
                )
            }
            .map { document ->
                val resolution = resolvedAssignments[document.id]
                    ?: resolveAssignmentForDay(
                        draft = document.toDraft(),
                        storedAssignee = document.assignee,
                        day = day,
                        loads = emptyList(),
                        choreId = document.id,
                        householdMembers = schedulingMembers,
                        decisions = reassignmentDecisions,
                        todayWeekdayName = todayWeekdayName,
                    )
                val occurrence = document.occurrences[day.dateKey]
                val scheduledStatus = occurrence?.status ?: defaultStatusForDay(document.status, day)
                val proofPhotoCount = occurrence?.proofPhotoCount ?: if (day.isToday) document.proofPhotoCount else 0
                val completedByName = occurrence?.completedByName ?: if (day.isToday) document.completedByName else ""
                val completedAtEpochMs = occurrence?.completedAtEpochMs ?: if (day.isToday) document.completedAtEpochMs else 0L
                val latestProof = latestProofDocumentFor(document.id, day.dateKey)
                val dueLabel = occurrence?.dueLabel
                    ?.takeIf(String::isNotBlank)
                    ?: if (day.isToday) {
                        document.toChoreItem().dueLabel
                    } else {
                        "Scheduled for ${day.weekdayName}"
                    }
                document.toChoreItem().copy(
                    assignee = resolution.assignee,
                    assigneeId = resolution.assignee.lowercase().replace(" ", "-"),
                    status = scheduledStatus,
                    assignmentReason = resolution.reason,
                    assignmentOriginalAssignee = resolution.originalAssignee.takeIf { original ->
                        original.isNotBlank() && !original.equals(resolution.assignee, ignoreCase = true)
                    }.orEmpty(),
                    dueLabel = if (scheduledStatus == ChoreItemStatus.PendingProof && proofPhotoCount > 0) {
                        "$proofPhotoCount photo${if (proofPhotoCount == 1) "" else "s"} uploaded"
                    } else if (latestProof?.reviewStatus == ProofReviewStatus.Rejected) {
                        "Needs new proof"
                    } else {
                        dueLabel
                    },
                    completedByName = completedByName,
                    completedAtEpochMs = completedAtEpochMs,
                    proofPhotoCount = proofPhotoCount,
                )
            },
        )
    }

    override suspend fun refresh() {
        val session = sessionRepository.session.value
        val householdId = session.householdId
        if (householdId.isBlank()) {
            clearLoadedState()
            return
        }

        val verifiedMembership = session.userId
            .takeIf(String::isNotBlank)
            ?.let { userId ->
                runCatching { householdStore.loadMember(householdId, userId) }.getOrNull()
            }
        if (verifiedMembership == null) {
            clearLoadedState()
            return
        }

        val roomsSnapshot = householdStore.householdRef(householdId).collection(FirestorePaths.Rooms).get()
        val roomDocs = roomsSnapshot.documents.mapNotNull { it.dataOrNull<RoomDocument>() }
        val seededRooms = if (roomDocs.isEmpty()) {
            val defaults = defaultRooms().map { room ->
                RoomDocument(
                    id = room.id,
                    name = room.name,
                    iconKey = room.iconKey,
                    colorKey = room.colorKey,
                )
            }
            defaults.forEach { document ->
                householdStore.householdRef(householdId).collection(FirestorePaths.Rooms).document(document.id).set(document)
            }
            defaults
        } else {
            roomDocs
        }

        roomDefinitions.clear()
        seededRooms.forEach { roomDefinitions[it.id] = it.toRoomDefinition() }
        _rooms.value = roomDefinitions.values.toList()

        val choreSnapshot = householdStore.householdRef(householdId).collection(FirestorePaths.Chores).get()
        choreDocuments.clear()
        choreSnapshot.documents.mapNotNull { it.dataOrNull<ChoreDocument>() }.forEach { document ->
            choreDocuments[document.id] = document
        }
        schedulingMembers = householdStore.householdRef(householdId)
            .collection("members")
            .get()
            .documents
            .mapNotNull { it.dataOrNull<HouseholdMemberDocument>() }
            .map { it.toSchedulingMember() }
        _householdMembers.value = schedulingMembers
        val proofsSnapshot = householdStore.householdRef(householdId).collection(FirestorePaths.Proofs).get()
        proofDocuments.clear()
        proofsSnapshot.documents.mapNotNull { it.dataOrNull<ProofDocument>() }.forEach { document ->
            proofDocuments += document
        }
        val decisionsSnapshot = householdStore.householdRef(householdId)
            .collection(FirestorePaths.ReassignmentDecisions)
            .get()
        reassignmentDecisions.clear()
        decisionsSnapshot.documents
            .mapNotNull { it.dataOrNull<ReassignmentDecisionDocument>() }
            .mapTo(reassignmentDecisions) { it.toDecision() }
        refreshTodaySchedule()
    }

    private fun clearLoadedState() {
        choreDocuments.clear()
        roomDefinitions.clear()
        proofDocuments.clear()
        reassignmentDecisions.clear()
        schedulingMembers = emptyList()
        _rooms.value = defaultRooms()
        _householdMembers.value = emptyList()
        _chores.value = emptyList()
    }

    override suspend fun getAvailabilitySuggestions(days: List<ChoreScheduleDay>): List<AvailabilitySuggestion> {
        val todayWeekdayName = currentWeekdayName()
        return days.flatMap { day ->
            val resolvedAssignments = resolvedAssignmentsFor(day, todayWeekdayName)
            choreDocuments.values.mapNotNull { document ->
                val draft = document.toDraft()
                if (!draft.appearsOnDay(day, todayWeekdayName)) return@mapNotNull null
                val resolution = resolvedAssignments[document.id] ?: return@mapNotNull null
                buildAvailabilitySuggestion(
                    choreId = document.id,
                    choreTitle = document.title,
                    day = day,
                    resolution = resolution,
                    decisions = reassignmentDecisions,
                )
            }
        }.sortedWith(compareBy<AvailabilitySuggestion>({ it.dateKey }, { it.weekdayLabel }, { it.choreTitle.lowercase() }))
    }

    override suspend fun saveChore(draft: ChoreDraft): String {
        ensureCanSaveChore(draft)
        val householdId = sessionRepository.session.value.householdId
            .takeIf(String::isNotBlank)
            ?: error("Create or join a household before saving chores.")

        val room = ensureRoom(
            householdId = householdId,
            draft = draft,
        )
        val choreId = draft.id ?: householdStore.householdRef(householdId).collection(FirestorePaths.Chores).document.id
        val normalizedDraft = draft.copy(
            id = choreId,
            roomId = room.id,
            roomName = room.name,
            roomIconKey = room.iconKey,
            roomColorKey = room.colorKey,
        )
        val existing = choreDocuments[choreId]
        val document = existing?.updatedFromDraft(normalizedDraft)
            ?: normalizedDraft.toDocument(choreId = choreId)
        householdStore.householdRef(householdId).collection(FirestorePaths.Chores).document(choreId).set(document)
        choreDocuments[choreId] = document
        refreshTodaySchedule()
        return choreId
    }

    override suspend fun approvePendingChore(choreId: String) {
        ensureCanManageHouseholdChores()
        val householdId = sessionRepository.session.value.householdId
            .takeIf(String::isNotBlank)
            ?: return
        val existing = choreDocuments[choreId] ?: return
        if (!existing.pendingApproval) return
        val reviewedAtEpochMs = Clock.System.now().toEpochMilliseconds()
        val reviewedByName = sessionRepository.session.value.displayName
        val updated = existing.copy(
            isActive = true,
            pendingApproval = false,
            reviewRejectedReason = "",
            reviewedByName = reviewedByName,
            reviewedAtEpochMs = reviewedAtEpochMs,
        )
        householdStore.householdRef(householdId).collection(FirestorePaths.Chores).document(choreId).set(updated)
        choreDocuments[choreId] = updated
        refreshTodaySchedule()
        runCatching {
            householdNotifications.onDiscoveryChoreApproved(
                DiscoveryChoreApprovedNotification(
                    householdId = householdId,
                    choreId = choreId,
                    choreTitle = updated.title,
                    submittingMemberName = updated.submitterName.ifBlank { updated.assignee },
                    reviewerName = reviewedByName,
                    recipientUserId = updated.submitterUserId,
                ),
            )
        }
    }

    override suspend fun rejectPendingChore(
        choreId: String,
        reason: String,
    ) {
        ensureCanManageHouseholdChores()
        val householdId = sessionRepository.session.value.householdId
            .takeIf(String::isNotBlank)
            ?: return
        val existing = choreDocuments[choreId] ?: return
        if (!existing.pendingApproval) return
        val rejectionReason = reason.trim()
        if (rejectionReason.isBlank()) return
        val reviewedAtEpochMs = Clock.System.now().toEpochMilliseconds()
        val reviewedByName = sessionRepository.session.value.displayName
        val updated = existing.copy(
            isActive = false,
            pendingApproval = false,
            reviewRejectedReason = rejectionReason,
            reviewedByName = reviewedByName,
            reviewedAtEpochMs = reviewedAtEpochMs,
            dueLabel = "Needs review updates",
        )
        householdStore.householdRef(householdId).collection(FirestorePaths.Chores).document(choreId).set(updated)
        choreDocuments[choreId] = updated
        refreshTodaySchedule()
        runCatching {
            householdNotifications.onDiscoveryChoreRejected(
                DiscoveryChoreRejectedNotification(
                    householdId = householdId,
                    choreId = choreId,
                    choreTitle = updated.title,
                    submittingMemberName = updated.submitterName.ifBlank { updated.assignee },
                    reviewerName = reviewedByName,
                    rejectionReason = rejectionReason,
                    recipientUserId = updated.submitterUserId,
                ),
            )
        }
    }

    override suspend fun deleteChore(choreId: String) {
        ensureCanManageHouseholdChores()
        val householdId = sessionRepository.session.value.householdId
            .takeIf(String::isNotBlank)
            ?: return
        val existing = choreDocuments[choreId] ?: return
        deleteProofsForChore(
            householdId = householdId,
            choreId = choreId,
        )
        deleteReassignmentDecisionsForChore(
            householdId = householdId,
            choreId = choreId,
        )
        deleteDiscoveryImageIfLastReference(
            deletedChoreId = choreId,
            existing = existing,
        )
        householdStore.householdRef(householdId).collection(FirestorePaths.Chores).document(choreId).delete()
        choreDocuments.remove(choreId)
        proofDocuments.removeAll { it.choreId == choreId }
        reassignmentDecisions.removeAll { it.choreId == choreId }
        refreshTodaySchedule()
    }

    override suspend fun toggleCompletion(
        choreId: String,
        viewerName: String,
        allowCompletingForSomeoneElse: Boolean,
        day: ChoreScheduleDay?,
    ) {
        val householdId = sessionRepository.session.value.householdId
            .takeIf(String::isNotBlank)
            ?: return
        val existing = choreDocuments[choreId] ?: return
        val targetDay = day ?: todayScheduleDay()
        val scheduledChore = getScheduledChores(targetDay).firstOrNull { it.id == choreId } ?: existing.toChoreItem()
        ensureCanMutateChoreProgress(scheduledChore)
        val currentOccurrence = existing.occurrences[targetDay.dateKey]
        val currentStatus = currentOccurrence?.status ?: defaultStatusForDay(existing.status, targetDay)
        val currentProofPhotoCount = currentOccurrence?.proofPhotoCount ?: if (targetDay.isToday) existing.proofPhotoCount else 0
        if (
            requiresCompletionConfirmation(scheduledChore, viewerName) &&
            !allowCompletingForSomeoneElse
        ) return

        val nextStatus = nextCompletionStatusForDay(
            currentStatus = currentStatus,
            proofRequired = existing.proofRequired,
            day = targetDay,
        )
        val shouldDeleteProof = currentProofPhotoCount > 0 &&
            (currentStatus == ChoreItemStatus.Completed || currentStatus == ChoreItemStatus.PendingProof) &&
            (nextStatus == ChoreItemStatus.DueSoon || nextStatus == ChoreItemStatus.Scheduled)
        if (shouldDeleteProof) {
            val deleted = deleteProofForDay(
                householdId = householdId,
                choreId = choreId,
                dateKey = targetDay.dateKey,
            )
            if (!deleted) return
        }
        val updatedOccurrence = ChoreOccurrenceDocument(
            status = nextStatus,
            dueLabel = refreshedDueLabel(nextStatus, targetDay),
            proofPhotoCount = when (nextStatus) {
                ChoreItemStatus.PendingProof -> if (shouldDeleteProof) 0 else currentProofPhotoCount
                else -> 0
            },
            completedByName = if (nextStatus == ChoreItemStatus.Completed || nextStatus == ChoreItemStatus.PendingProof) {
                recordedCompletionName(scheduledChore.assignee, viewerName)
            } else {
                ""
            },
            completedAtEpochMs = when (nextStatus) {
                ChoreItemStatus.Completed,
                ChoreItemStatus.PendingProof -> (currentOccurrence?.completedAtEpochMs ?: 0L).takeIf { it > 0L }
                    ?: Clock.System.now().toEpochMilliseconds()

                else -> 0L
            },
        )
        val updated = existing.copy(
            status = if (targetDay.isToday) nextStatus else existing.status,
            dueLabel = if (targetDay.isToday) refreshedDueLabel(nextStatus, targetDay) else existing.dueLabel,
            proofPhotoCount = if (targetDay.isToday) updatedOccurrence.proofPhotoCount else existing.proofPhotoCount,
            completedByName = if (targetDay.isToday) updatedOccurrence.completedByName else existing.completedByName,
            completedAtEpochMs = if (targetDay.isToday) updatedOccurrence.completedAtEpochMs else existing.completedAtEpochMs,
            occurrences = existing.occurrences + (targetDay.dateKey to updatedOccurrence),
        )
        householdStore.householdRef(householdId).collection(FirestorePaths.Chores).document(choreId).set(updated)
        choreDocuments[choreId] = updated
        refreshTodaySchedule()
    }

    override suspend fun approveProof(choreId: String, day: ChoreScheduleDay?) {
        ensureCanManageHouseholdChores()
        val householdId = sessionRepository.session.value.householdId
            .takeIf(String::isNotBlank)
            ?: return
        val existing = choreDocuments[choreId] ?: return
        val targetDay = day ?: todayScheduleDay()
        val currentOccurrence = existing.occurrences[targetDay.dateKey]
        val currentStatus = currentOccurrence?.status ?: defaultStatusForDay(existing.status, targetDay)
        if (currentStatus != ChoreItemStatus.PendingProof) return
        val proofToApprove = latestProofDocumentFor(
            choreId = choreId,
            dateKey = targetDay.dateKey,
            reviewStatus = ProofReviewStatus.Pending,
        ) ?: return
        val reviewedAtEpochMs = Clock.System.now().toEpochMilliseconds()
        val reviewedByName = sessionRepository.session.value.displayName

        val updatedOccurrence = ChoreOccurrenceDocument(
            status = approveProofStatus(currentStatus),
            dueLabel = "Approved",
            completedByName = currentOccurrence?.completedByName ?: if (targetDay.isToday) existing.completedByName else "",
            completedAtEpochMs = currentOccurrence?.completedAtEpochMs
                ?: if (targetDay.isToday) existing.completedAtEpochMs else Clock.System.now().toEpochMilliseconds(),
            proofPhotoCount = currentOccurrence?.proofPhotoCount ?: if (targetDay.isToday) existing.proofPhotoCount else 0,
        )
        val reviewedProof = proofToApprove.copy(
            reviewStatus = ProofReviewStatus.Approved,
            reviewedByName = reviewedByName,
            reviewedAtEpochMs = reviewedAtEpochMs,
            rejectionReason = "",
        )
        val updated = existing.copy(
            status = if (targetDay.isToday) updatedOccurrence.status else existing.status,
            dueLabel = if (targetDay.isToday) updatedOccurrence.dueLabel else existing.dueLabel,
            completedByName = if (targetDay.isToday) updatedOccurrence.completedByName else existing.completedByName,
            completedAtEpochMs = if (targetDay.isToday) updatedOccurrence.completedAtEpochMs else existing.completedAtEpochMs,
            proofPhotoCount = if (targetDay.isToday) updatedOccurrence.proofPhotoCount else existing.proofPhotoCount,
            occurrences = existing.occurrences + (targetDay.dateKey to updatedOccurrence),
        )
        householdStore.householdRef(householdId)
            .collection(FirestorePaths.Proofs)
            .document(reviewedProof.id)
            .set(reviewedProof)
        proofDocuments.indices.forEach { index ->
            if (proofDocuments[index].id == reviewedProof.id) {
                proofDocuments[index] = reviewedProof
            }
        }
        householdStore.householdRef(householdId).collection(FirestorePaths.Chores).document(choreId).set(updated)
        choreDocuments[choreId] = updated
        refreshTodaySchedule()
    }

    override suspend fun rejectProof(
        choreId: String,
        reason: String,
        day: ChoreScheduleDay?,
    ) {
        ensureCanManageHouseholdChores()
        val householdId = sessionRepository.session.value.householdId
            .takeIf(String::isNotBlank)
            ?: return
        val existing = choreDocuments[choreId] ?: return
        val targetDay = day ?: todayScheduleDay()
        val currentOccurrence = existing.occurrences[targetDay.dateKey]
        val currentStatus = currentOccurrence?.status ?: defaultStatusForDay(existing.status, targetDay)
        if (currentStatus != ChoreItemStatus.PendingProof) return
        val proofToReject = latestProofDocumentFor(
            choreId = choreId,
            dateKey = targetDay.dateKey,
            reviewStatus = ProofReviewStatus.Pending,
        ) ?: return
        val reviewedAtEpochMs = Clock.System.now().toEpochMilliseconds()
        val rejectionReason = reason.trim()
        val reviewedByName = sessionRepository.session.value.displayName
        val updatedOccurrence = ChoreOccurrenceDocument(
            status = if (targetDay.isToday) ChoreItemStatus.DueSoon else ChoreItemStatus.Scheduled,
            dueLabel = "Needs new proof",
            completedByName = currentOccurrence?.completedByName ?: if (targetDay.isToday) existing.completedByName else "",
            completedAtEpochMs = currentOccurrence?.completedAtEpochMs ?: if (targetDay.isToday) existing.completedAtEpochMs else 0L,
            proofPhotoCount = currentOccurrence?.proofPhotoCount ?: if (targetDay.isToday) existing.proofPhotoCount else 0,
        )
        val rejectedProof = proofToReject.copy(
            reviewStatus = ProofReviewStatus.Rejected,
            reviewedByName = reviewedByName,
            reviewedAtEpochMs = reviewedAtEpochMs,
            rejectionReason = rejectionReason,
        )
        val updated = existing.copy(
            status = if (targetDay.isToday) updatedOccurrence.status else existing.status,
            dueLabel = if (targetDay.isToday) updatedOccurrence.dueLabel else existing.dueLabel,
            completedByName = if (targetDay.isToday) updatedOccurrence.completedByName else existing.completedByName,
            completedAtEpochMs = if (targetDay.isToday) updatedOccurrence.completedAtEpochMs else existing.completedAtEpochMs,
            proofPhotoCount = if (targetDay.isToday) updatedOccurrence.proofPhotoCount else existing.proofPhotoCount,
            occurrences = existing.occurrences + (targetDay.dateKey to updatedOccurrence),
        )
        householdStore.householdRef(householdId)
            .collection(FirestorePaths.Proofs)
            .document(rejectedProof.id)
            .set(rejectedProof)
        proofDocuments.indices.forEach { index ->
            if (proofDocuments[index].id == rejectedProof.id) {
                proofDocuments[index] = rejectedProof
            }
        }
        householdStore.householdRef(householdId).collection(FirestorePaths.Chores).document(choreId).set(updated)
        choreDocuments[choreId] = updated
        runCatching {
            householdNotifications.onProofRejected(
                ProofRejectedNotification(
                    householdId = householdId,
                    choreId = choreId,
                    choreTitle = existing.title,
                    dateKey = targetDay.dateKey,
                    assigneeName = existing.assignee,
                    submittingMemberName = rejectedProof.uploadedByName.ifBlank { existing.assignee },
                    reviewerName = reviewedByName,
                    rejectionReason = rejectionReason,
                    recipientUserId = rejectedProof.uploadedByUserId,
                ),
            )
        }
        refreshTodaySchedule()
    }

    override suspend fun acceptAvailabilitySuggestion(suggestion: AvailabilitySuggestion) {
        ensureCanManageHouseholdChores()
        persistReassignmentDecision(
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
        runCatching {
            householdNotifications.onAssignmentChanged(
                AssignmentChangedNotification(
                    householdId = sessionRepository.session.value.householdId,
                    choreId = suggestion.choreId,
                    choreTitle = suggestion.choreTitle,
                    dateKey = suggestion.dateKey,
                    originalAssigneeName = suggestion.originalAssignee,
                    newAssigneeName = suggestion.recommendedCoverMember,
                ),
            )
        }
    }

    override suspend fun dismissAvailabilitySuggestion(suggestion: AvailabilitySuggestion) {
        ensureCanManageHouseholdChores()
        persistReassignmentDecision(
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

    override suspend fun submitProof(
        choreId: String,
        payload: ProofUploadPayload,
        day: ChoreScheduleDay?,
    ): ProofUploadResult {
        val session = sessionRepository.session.value
        val householdId = session.householdId.takeIf(String::isNotBlank) ?: error("Join a household before uploading proof.")
        val userId = session.userId.takeIf(String::isNotBlank) ?: error("Sign in before uploading proof.")
        val existing = choreDocuments[choreId] ?: error("Chore not found.")
        val targetDay = day ?: todayScheduleDay()
        val scheduledChore = getScheduledChores(targetDay).firstOrNull { it.id == choreId } ?: existing.toChoreItem()
        ensureCanMutateChoreProgress(scheduledChore)
        val currentOccurrence = existing.occurrences[targetDay.dateKey]
        val currentProofPhotoCount = currentOccurrence?.proofPhotoCount ?: if (targetDay.isToday) existing.proofPhotoCount else 0
        val proofRef = householdStore.householdRef(householdId).collection(FirestorePaths.Proofs).document
        val extension = payload.fileExtension.trimStart('.').ifBlank { "jpg" }
        val storagePath = StoragePaths.proofImage(
            householdId = householdId,
            choreId = choreId,
            userId = userId,
            proofId = proofRef.id,
            fileExtension = extension,
        )
        val storageRef = storage.reference(storagePath)
        storageRef.putData(
            data = localImagePathToFirebaseStorageData(payload.localPath),
            metadata = storageMetadata {
                contentType = payload.mimeType
                setCustomMetadata("householdId", householdId)
                setCustomMetadata("choreId", choreId)
                setCustomMetadata("userId", userId)
            },
        )
        val downloadUrl = storageRef.getDownloadUrl()
        val createdAtEpochMs = Clock.System.now().toEpochMilliseconds()
        val proofDocument = ProofDocument(
            id = proofRef.id,
            choreId = choreId,
            dateKey = targetDay.dateKey,
            choreTitle = existing.title,
            roomName = existing.roomName,
            uploadedByUserId = userId,
            uploadedByName = session.displayName,
            storagePath = storagePath,
            downloadUrl = downloadUrl,
            contentType = payload.mimeType,
            createdAtEpochMs = createdAtEpochMs,
        )
        proofRef.set(
            proofDocument,
        )
        proofDocuments += proofDocument
        val updatedOccurrence = ChoreOccurrenceDocument(
            status = ChoreItemStatus.PendingProof,
            dueLabel = "${currentProofPhotoCount + 1} photo${if (currentProofPhotoCount + 1 == 1) "" else "s"} uploaded",
            proofPhotoCount = currentProofPhotoCount + 1,
            completedByName = recordedCompletionName(
                assignee = scheduledChore.assignee,
                viewerName = session.displayName,
            ),
            completedAtEpochMs = currentOccurrence?.completedAtEpochMs
                ?: Clock.System.now().toEpochMilliseconds(),
        )
        val updated = existing.copy(
            status = if (targetDay.isToday) updatedOccurrence.status else existing.status,
            dueLabel = if (targetDay.isToday) updatedOccurrence.dueLabel else existing.dueLabel,
            proofPhotoCount = if (targetDay.isToday) updatedOccurrence.proofPhotoCount else existing.proofPhotoCount,
            completedByName = if (targetDay.isToday) updatedOccurrence.completedByName else existing.completedByName,
            completedAtEpochMs = if (targetDay.isToday) updatedOccurrence.completedAtEpochMs else existing.completedAtEpochMs,
            occurrences = existing.occurrences + (targetDay.dateKey to updatedOccurrence),
        )
        householdStore.householdRef(householdId).collection(FirestorePaths.Chores).document(choreId).set(updated)
        choreDocuments[choreId] = updated
        refreshTodaySchedule()
        return ProofUploadResult(
            proofId = proofRef.id,
            storagePath = storagePath,
            downloadUrl = downloadUrl,
        )
    }

    private suspend fun ensureRoom(
        householdId: String,
        draft: ChoreDraft,
    ): RoomDefinition {
        val existing = draft.roomId.takeIf(String::isNotBlank)?.let(roomDefinitions::get)
            ?: roomDefinitions.values.firstOrNull { it.name.equals(draft.roomName, ignoreCase = true) }
        if (existing != null) {
            return existing
        }

        val document = householdStore.householdRef(householdId).collection(FirestorePaths.Rooms).document
        val room = RoomDefinition(
            id = document.id,
            name = draft.roomName,
            iconKey = draft.roomIconKey,
            colorKey = draft.roomColorKey,
        )
        document.set(
            RoomDocument(
                id = room.id,
                name = room.name,
                iconKey = room.iconKey,
                colorKey = room.colorKey,
            ),
        )
        roomDefinitions[room.id] = room
        _rooms.value = roomDefinitions.values.toList()
        return room
    }

    private fun resolvedAssignmentsFor(
        day: ChoreScheduleDay,
        todayWeekdayName: String,
    ): Map<String, com.sortoapps.chorepilot.features.chores.domain.ChoreAssignmentResolution> =
        resolveAssignmentsForDay(
            sources = choreDocuments.values.map { document ->
                ChoreAssignmentSource(
                    choreId = document.id,
                    storedAssignee = document.assignee,
                    draft = document.toDraft(),
                    effortScore = document.effortLevel.score,
                )
            },
            day = day,
            householdMembers = schedulingMembers,
            decisions = reassignmentDecisions,
            todayWeekdayName = todayWeekdayName,
        )

    private suspend fun persistReassignmentDecision(decision: AvailabilitySuggestionDecision) {
        val householdId = sessionRepository.session.value.householdId
            .takeIf(String::isNotBlank)
            ?: return
        val documentId = availabilitySuggestionId(
            choreId = decision.choreId,
            originalAssignee = decision.originalAssignee,
            scopeStartDate = decision.scopeStartDate,
            scopeEndDate = decision.scopeEndDate,
        )
        householdStore.householdRef(householdId)
            .collection(FirestorePaths.ReassignmentDecisions)
            .document(documentId)
            .set(decision.copy(suggestionId = documentId).toDocument())
        reassignmentDecisions.removeAll { existing ->
            existing.choreId == decision.choreId &&
                existing.originalAssignee.equals(decision.originalAssignee, ignoreCase = true) &&
                existing.scopeStartDate == decision.scopeStartDate &&
                existing.scopeEndDate == decision.scopeEndDate
        }
        reassignmentDecisions += decision.copy(suggestionId = documentId)
        refreshTodaySchedule()
    }

    private suspend fun refreshTodaySchedule() {
        val today = todayScheduleDay()
        val scheduled = getScheduledChores(today)
        _chores.value = scheduled
        emitStatusNotifications(
            householdId = sessionRepository.session.value.householdId,
            day = today,
            chores = scheduled,
        )
    }

    private suspend fun emitStatusNotifications(
        householdId: String,
        day: ChoreScheduleDay,
        chores: List<ChoreItem>,
    ) {
        if (householdId.isBlank()) return
        chores.forEach { chore ->
            runCatching {
                when (chore.status) {
                    ChoreItemStatus.DueSoon -> householdNotifications.onDueSoon(
                        DueSoonNotification(
                            householdId = householdId,
                            choreId = chore.id,
                            choreTitle = chore.title,
                            dateKey = day.dateKey,
                            assigneeName = chore.assignee,
                            dueLabel = chore.dueLabel,
                        ),
                    )

                    ChoreItemStatus.Overdue -> householdNotifications.onOverdue(
                        OverdueNotification(
                            householdId = householdId,
                            choreId = chore.id,
                            choreTitle = chore.title,
                            dateKey = day.dateKey,
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
    }

    private fun refreshedDueLabel(
        nextStatus: ChoreItemStatus,
        day: ChoreScheduleDay,
    ): String = when (nextStatus) {
        ChoreItemStatus.PendingProof -> "Waiting for admin review"
        ChoreItemStatus.Completed -> "Completed"
        ChoreItemStatus.Scheduled -> "Scheduled for ${day.weekdayName}"
        else -> "Due later today"
    }

    private suspend fun deleteProofForDay(
        householdId: String,
        choreId: String,
        dateKey: String,
    ): Boolean {
        val proofsCollection = householdStore.householdRef(householdId).collection(FirestorePaths.Proofs)
        val proofsSnapshot = proofsCollection.get()
        proofsSnapshot.documents.forEach { documentSnapshot ->
            val proof = documentSnapshot.dataOrNull<ProofDocument>() ?: return@forEach
            if (proof.choreId != choreId || proof.dateKey != dateKey) return@forEach
            val normalizedStoragePath = proof.storagePath.trimStart('/')
            val objectDeleted = normalizedStoragePath.isBlank() || runCatching {
                storage.reference(normalizedStoragePath).delete()
            }.isSuccess
            if (!objectDeleted) return false
            proofsCollection.document(proof.id).delete()
            proofDocuments.removeAll { it.id == proof.id }
        }
        return true
    }

    private fun latestProofDocumentFor(
        choreId: String,
        dateKey: String,
        reviewStatus: ProofReviewStatus? = null,
    ): ProofDocument? = proofDocuments
        .asSequence()
        .filter { it.choreId == choreId && it.dateKey == dateKey }
        .filter { reviewStatus == null || it.reviewStatus == reviewStatus }
        .maxByOrNull { it.createdAtEpochMs }

    private suspend fun deleteProofsForChore(
        householdId: String,
        choreId: String,
    ) {
        val proofsCollection = householdStore.householdRef(householdId).collection(FirestorePaths.Proofs)
        proofDocuments
            .filter { it.choreId == choreId }
            .toList()
            .forEach { proof ->
                val normalizedStoragePath = proof.storagePath.trimStart('/')
                if (normalizedStoragePath.isNotBlank()) {
                    storage.reference(normalizedStoragePath).delete()
                }
                proofsCollection.document(proof.id).delete()
            }
    }

    private suspend fun deleteReassignmentDecisionsForChore(
        householdId: String,
        choreId: String,
    ) {
        val decisionsCollection = householdStore.householdRef(householdId).collection(FirestorePaths.ReassignmentDecisions)
        reassignmentDecisions
            .filter { it.choreId == choreId }
            .toList()
            .forEach { decision ->
                decisionsCollection.document(decision.suggestionId).delete()
            }
    }

    private suspend fun deleteDiscoveryImageIfLastReference(
        deletedChoreId: String,
        existing: ChoreDocument,
    ) {
        val normalizedStoragePath = existing.discoveryImageStoragePath.trimStart('/')
        if (normalizedStoragePath.isBlank()) return
        val stillReferenced = choreDocuments.values.any { document ->
            document.id != deletedChoreId && (
                document.discoveryImageStoragePath.trimStart('/') == normalizedStoragePath ||
                    (
                        existing.discoverySessionId.isNotBlank() &&
                            document.discoverySessionId == existing.discoverySessionId
                        )
                )
        }
        if (!stillReferenced) {
            runCatching {
                storage.reference(normalizedStoragePath).delete()
            }
        }
    }

    private fun defaultRooms(): List<RoomDefinition> = listOf(
        RoomDefinition(id = "room-kitchen", name = "Kitchen", iconKey = "kitchen", colorKey = "teal"),
        RoomDefinition(id = "room-living", name = "Living room", iconKey = "living", colorKey = "violet"),
        RoomDefinition(id = "room-bedroom", name = "Bedroom", iconKey = "bedroom", colorKey = "amber"),
        RoomDefinition(id = "room-bathroom", name = "Bathroom", iconKey = "bathroom", colorKey = "sky"),
        RoomDefinition(id = "room-entry", name = "Entry", iconKey = "entry", colorKey = "rose"),
        RoomDefinition(id = "room-outside", name = "Outside", iconKey = "outside", colorKey = "sage"),
    )

    private fun ensureCanManageHouseholdChores() {
        if (!sessionRepository.session.value.role.canManageHouseholdChores()) {
            error("Only owners and admins can manage household chores.")
        }
    }

    private fun ensureCanSaveChore(draft: ChoreDraft) {
        if (sessionRepository.session.value.role.canManageHouseholdChores()) return
        val canSubmitDiscoveryForReview = draft.discoveryGenerated && draft.pendingApproval
        if (!canSubmitDiscoveryForReview) {
            error("Only owners and admins can manage household chores.")
        }
    }

    private fun ensureCanMutateChoreProgress(chore: ChoreItem) {
        val session = sessionRepository.session.value
        if (session.role.canManageHouseholdChores()) return
        val assignedToViewerId = session.userId.isNotBlank() &&
            chore.assigneeId.equals(session.userId, ignoreCase = true)
        val assignedToViewerName = chore.assignee.equals(session.displayName, ignoreCase = true)
        val assignedToViewer = assignedToViewerId || assignedToViewerName
        val canUndo = chore.status == ChoreItemStatus.Completed &&
            canUndoChoreCompletion(
                chore = chore,
                viewerId = session.userId,
                viewerName = session.displayName,
                viewerRole = session.role,
            )
        if (!(assignedToViewer || canUndo)) {
            error("You can only update chores assigned to you.")
        }
    }
}

private fun HouseholdMemberDocument.toSchedulingMember(): HouseholdSchedulingMember = HouseholdSchedulingMember(
    userId = userId,
    displayName = displayName,
    active = active,
    availabilityMode = when (availabilityMode) {
        HouseholdAvailabilityMode.Available -> ChoreAvailabilityMode.Available
        HouseholdAvailabilityMode.DayOff -> ChoreAvailabilityMode.DayOff
        HouseholdAvailabilityMode.GoneMode -> ChoreAvailabilityMode.GoneMode
    },
    availabilityStartDate = availabilityStartDate,
    availabilityEndDate = availabilityEndDate,
)

private fun normalizeStorageBucketUrl(bucket: String): String = when {
    bucket.startsWith("gs://") -> bucket
    bucket.startsWith("https://") -> bucket
    else -> "gs://$bucket"
}

@Serializable
private data class DocumentRoomLookup(
    val id: String = "",
    val name: String = "",
)

private inline fun <reified T> dev.gitlive.firebase.firestore.DocumentSnapshot.dataOrNull(): T? = if (exists) data<T>() else null
