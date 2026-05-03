package com.sortoapps.chorepilot.features.chores.domain

data class RoomDefinition(
    val id: String,
    val name: String,
    val iconKey: String,
    val colorKey: String,
)

enum class ChoreRepeatFrequencyValue {
    Daily,
    Weekly,
    Monthly,
    Custom,
}

enum class ChoreDailyModeValue {
    EveryDay,
    Weekdays,
    SpecificDays,
}

enum class ChoreSmartAssignmentModeValue {
    FairRotation,
    DayPlan,
}

enum class ChoreEffortValue(
    val score: Int,
) {
    Light(1),
    Standard(2),
    Heavy(3),
}

data class ChoreDraft(
    val id: String? = null,
    val title: String,
    val additionalComments: String,
    val roomId: String,
    val roomName: String,
    val roomIconKey: String,
    val roomColorKey: String,
    val assignee: String,
    val autoAssign: Boolean,
    val smartAssignmentMode: ChoreSmartAssignmentModeValue,
    val assignmentPool: Set<String>,
    val dayAssignments: Map<String, String>,
    val effortLevel: ChoreEffortValue,
    val repeatsEnabled: Boolean,
    val repeatStartDate: String,
    val repeatFrequency: ChoreRepeatFrequencyValue,
    val dailyRepeatMode: ChoreDailyModeValue,
    val selectedWeekdays: Set<String>,
    val monthlyPattern: String,
    val customPattern: String,
    val selectedOneTimeDate: String,
    val proofRequired: Boolean,
    val durationMinutes: Int?,
    val isActive: Boolean = true,
    val pendingApproval: Boolean = false,
    val submitterUserId: String = "",
    val submitterName: String = "",
    val reviewRejectedReason: String = "",
    val reviewedByName: String = "",
    val reviewedAtEpochMs: Long = 0L,
    val discoverySessionId: String = "",
    val discoveryImageStoragePath: String = "",
    val discoveryImageUrl: String = "",
    val discoveryGenerated: Boolean = false,
)
