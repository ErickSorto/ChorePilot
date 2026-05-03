package com.sortoapps.chorepilot.features.chores.data.model

import com.sortoapps.chorepilot.features.chores.domain.ChoreDailyModeValue
import com.sortoapps.chorepilot.features.chores.domain.ChoreDraft
import com.sortoapps.chorepilot.features.chores.domain.ChoreEffortValue
import com.sortoapps.chorepilot.features.chores.domain.ChoreItem
import com.sortoapps.chorepilot.features.chores.domain.ChoreItemStatus
import com.sortoapps.chorepilot.features.chores.domain.ProofReviewStatus
import com.sortoapps.chorepilot.features.chores.domain.ChoreRepeatFrequencyValue
import com.sortoapps.chorepilot.features.chores.domain.ChoreSmartAssignmentModeValue
import com.sortoapps.chorepilot.features.chores.domain.RoomDefinition
import kotlinx.serialization.Serializable

@Serializable
data class ChoreOccurrenceDocument(
    val status: ChoreItemStatus = ChoreItemStatus.Scheduled,
    val dueLabel: String = "",
    val completedByName: String = "",
    val completedAtEpochMs: Long = 0L,
    val proofPhotoCount: Int = 0,
)

@Serializable
data class ProofDocument(
    val id: String = "",
    val choreId: String = "",
    val dateKey: String = "",
    val choreTitle: String = "",
    val roomName: String = "",
    val uploadedByUserId: String = "",
    val uploadedByName: String = "",
    val storagePath: String = "",
    val downloadUrl: String = "",
    val contentType: String = "image/jpeg",
    val createdAtEpochMs: Long = 0L,
    val reviewStatus: ProofReviewStatus = ProofReviewStatus.Pending,
    val reviewedByName: String = "",
    val reviewedAtEpochMs: Long = 0L,
    val rejectionReason: String = "",
)

@Serializable
data class RoomDocument(
    val id: String = "",
    val name: String = "",
    val iconKey: String = "kitchen",
    val colorKey: String = "teal",
)

@Serializable
data class ChoreDocument(
    val id: String = "",
    val title: String = "",
    val additionalComments: String = "",
    val roomId: String = "",
    val roomName: String = "",
    val roomIconKey: String = "kitchen",
    val roomColorKey: String = "teal",
    val assignee: String = "",
    val assigneeId: String = "",
    val autoAssign: Boolean = false,
    val smartAssignmentMode: ChoreSmartAssignmentModeValue = ChoreSmartAssignmentModeValue.FairRotation,
    val assignmentPool: List<String> = emptyList(),
    val dayAssignments: Map<String, String> = emptyMap(),
    val effortLevel: ChoreEffortValue = ChoreEffortValue.Standard,
    val repeatsEnabled: Boolean = false,
    val repeatStartDate: String = "Today",
    val repeatFrequency: ChoreRepeatFrequencyValue = ChoreRepeatFrequencyValue.Daily,
    val dailyRepeatMode: ChoreDailyModeValue = ChoreDailyModeValue.Weekdays,
    val selectedWeekdays: List<String> = emptyList(),
    val monthlyPattern: String = "",
    val customPattern: String = "",
    val selectedOneTimeDate: String = "",
    val proofRequired: Boolean = false,
    val durationMinutes: Int? = null,
    val status: ChoreItemStatus = ChoreItemStatus.DueSoon,
    val isActive: Boolean = true,
    val pendingApproval: Boolean = false,
    val submitterUserId: String = "",
    val submitterName: String = "",
    val reviewRejectedReason: String = "",
    val reviewedByName: String = "",
    val reviewedAtEpochMs: Long = 0L,
    val dueLabel: String = "Due later today",
    val proofPhotoCount: Int = 0,
    val completedByName: String = "",
    val completedAtEpochMs: Long = 0L,
    val occurrences: Map<String, ChoreOccurrenceDocument> = emptyMap(),
    val discoverySessionId: String = "",
    val discoveryImageStoragePath: String = "",
    val discoveryImageUrl: String = "",
    val discoveryGenerated: Boolean = false,
)

fun RoomDocument.toRoomDefinition(): RoomDefinition = RoomDefinition(
    id = id,
    name = name,
    iconKey = iconKey,
    colorKey = colorKey,
)

fun ChoreDocument.toDraft(): ChoreDraft = ChoreDraft(
    id = id,
    title = title,
    additionalComments = additionalComments,
    roomId = roomId,
    roomName = roomName,
    roomIconKey = roomIconKey,
    roomColorKey = roomColorKey,
    assignee = assignee,
    autoAssign = autoAssign,
    smartAssignmentMode = smartAssignmentMode,
    assignmentPool = assignmentPool.toSet(),
    dayAssignments = dayAssignments,
    effortLevel = effortLevel,
    repeatsEnabled = repeatsEnabled,
    repeatStartDate = repeatStartDate,
    repeatFrequency = repeatFrequency,
    dailyRepeatMode = dailyRepeatMode,
    selectedWeekdays = selectedWeekdays.toSet(),
    monthlyPattern = monthlyPattern,
    customPattern = customPattern,
    selectedOneTimeDate = selectedOneTimeDate,
    proofRequired = proofRequired,
    durationMinutes = durationMinutes,
    isActive = isActive,
    pendingApproval = pendingApproval,
    submitterUserId = submitterUserId,
    submitterName = submitterName,
    reviewRejectedReason = reviewRejectedReason,
    reviewedByName = reviewedByName,
    reviewedAtEpochMs = reviewedAtEpochMs,
    discoverySessionId = discoverySessionId,
    discoveryImageStoragePath = discoveryImageStoragePath,
    discoveryImageUrl = discoveryImageUrl,
    discoveryGenerated = discoveryGenerated,
)

fun ChoreDocument.toChoreItem(): ChoreItem = ChoreItem(
    id = id,
    title = title,
    area = roomName,
    cadence = buildCadenceLabel(),
    assignee = assignee,
    assigneeId = assigneeId,
    status = status,
    durationLabel = durationMinutes?.let { "$it min" } ?: "No estimate",
    dueLabel = when {
        status == ChoreItemStatus.PendingProof && proofPhotoCount > 0 -> "$proofPhotoCount photos uploaded"
        else -> dueLabel
    },
    description = additionalComments,
    reminder = buildReminder(),
    tags = buildTags(),
    isActive = isActive,
    pendingApproval = pendingApproval,
    assignmentReason = "",
    assignmentOriginalAssignee = "",
    completedByName = completedByName,
    completedAtEpochMs = completedAtEpochMs,
    proofRequired = proofRequired,
    proofPhotoCount = proofPhotoCount,
    submitterUserId = submitterUserId,
    submitterName = submitterName,
    reviewRejectedReason = reviewRejectedReason,
    reviewedByName = reviewedByName,
    reviewedAtEpochMs = reviewedAtEpochMs,
    discoverySessionId = discoverySessionId,
    discoveryImageStoragePath = discoveryImageStoragePath,
    discoveryImageUrl = discoveryImageUrl,
    discoveryGenerated = discoveryGenerated,
)

fun ChoreDraft.toDocument(
    choreId: String,
    assigneeId: String = assignee.lowercase().replace(" ", "-"),
): ChoreDocument = ChoreDocument(
    id = choreId,
    title = title,
    additionalComments = additionalComments,
    roomId = roomId,
    roomName = roomName,
    roomIconKey = roomIconKey,
    roomColorKey = roomColorKey,
    assignee = assignee,
    assigneeId = assigneeId,
    autoAssign = autoAssign,
    smartAssignmentMode = smartAssignmentMode,
    assignmentPool = assignmentPool.toList(),
    dayAssignments = dayAssignments,
    effortLevel = effortLevel,
    repeatsEnabled = repeatsEnabled,
    repeatStartDate = repeatStartDate,
    repeatFrequency = repeatFrequency,
    dailyRepeatMode = dailyRepeatMode,
    selectedWeekdays = selectedWeekdays.toList(),
    monthlyPattern = monthlyPattern,
    customPattern = customPattern,
    selectedOneTimeDate = selectedOneTimeDate,
    proofRequired = proofRequired,
    durationMinutes = durationMinutes,
    status = ChoreItemStatus.DueSoon,
    isActive = isActive,
    pendingApproval = pendingApproval,
    submitterUserId = submitterUserId,
    submitterName = submitterName,
    reviewRejectedReason = reviewRejectedReason,
    reviewedByName = reviewedByName,
    reviewedAtEpochMs = reviewedAtEpochMs,
    dueLabel = if (repeatsEnabled) "Starts ${repeatStartDate.lowercase()}" else selectedOneTimeDate,
    proofPhotoCount = 0,
    completedByName = "",
    completedAtEpochMs = 0L,
    occurrences = emptyMap(),
    discoverySessionId = discoverySessionId,
    discoveryImageStoragePath = discoveryImageStoragePath,
    discoveryImageUrl = discoveryImageUrl,
    discoveryGenerated = discoveryGenerated,
)

fun ChoreDocument.updatedFromDraft(
    draft: ChoreDraft,
    assigneeId: String = draft.assignee.lowercase().replace(" ", "-"),
): ChoreDocument {
    val refreshed = draft.toDocument(
        choreId = id.ifBlank { draft.id.orEmpty() },
        assigneeId = assigneeId,
    )
    if (!hasRuntimeHistory()) {
        return refreshed.copy(occurrences = occurrences)
    }
    return refreshed.copy(
        status = status,
        dueLabel = dueLabel,
        proofPhotoCount = proofPhotoCount,
        completedByName = completedByName,
        completedAtEpochMs = completedAtEpochMs,
        occurrences = occurrences,
    )
}

private fun ChoreDocument.hasRuntimeHistory(): Boolean =
    status != ChoreItemStatus.DueSoon ||
        proofPhotoCount > 0 ||
        completedByName.isNotBlank() ||
        completedAtEpochMs > 0L ||
        occurrences.isNotEmpty()

private fun ChoreDocument.buildCadenceLabel(): String = if (!repeatsEnabled) {
    "Once on $selectedOneTimeDate"
} else {
    when (repeatFrequency) {
        ChoreRepeatFrequencyValue.Daily -> when (dailyRepeatMode) {
            ChoreDailyModeValue.EveryDay -> "Daily"
            ChoreDailyModeValue.Weekdays -> "Weekdays"
            ChoreDailyModeValue.SpecificDays -> "Daily on ${selectedWeekdays.joinToString()}"
        }

        ChoreRepeatFrequencyValue.Weekly -> "Weekly on ${selectedWeekdays.joinToString()}"
        ChoreRepeatFrequencyValue.Monthly -> "Monthly on $monthlyPattern"
        ChoreRepeatFrequencyValue.Custom -> customPattern
    }
}

private fun ChoreDocument.buildReminder(): String = when {
    autoAssign && smartAssignmentMode == ChoreSmartAssignmentModeValue.DayPlan -> "Structured day plan keeps ownership visible all week."
    autoAssign -> "Smart rotation balances weekly effort across the selected people."
    else -> "Assigned directly to $assignee."
}

private fun ChoreDocument.buildTags(): List<String> = buildList {
    add(roomName)
    add(if (proofRequired) "Proof required" else "No proof")
    add(
        when {
            pendingApproval -> "Waiting approval"
            isActive -> "Active"
            else -> "Deactivated"
        },
    )
    if (status == ChoreItemStatus.Overdue) add("Overdue")
}
