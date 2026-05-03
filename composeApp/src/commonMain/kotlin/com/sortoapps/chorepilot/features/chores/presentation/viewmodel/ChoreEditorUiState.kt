package com.sortoapps.chorepilot.features.chores.presentation.viewmodel

enum class ChoreRepeatFrequency {
    Daily,
    Weekly,
    Monthly,
    Custom,
}

enum class DailyRepeatMode {
    EveryDay,
    Weekdays,
    SpecificDays,
}

enum class SmartAssignmentMode {
    FairRotation,
    DayPlan,
}

enum class ChoreEffortLevel {
    Light,
    Standard,
    Heavy,
}

enum class ChoreWeekday(
    val shortLabel: String,
    val fullLabel: String,
) {
    Sunday("S", "Sunday"),
    Monday("M", "Monday"),
    Tuesday("T", "Tuesday"),
    Wednesday("W", "Wednesday"),
    Thursday("T", "Thursday"),
    Friday("F", "Friday"),
    Saturday("S", "Saturday"),
}

data class RoomOptionUiModel(
    val id: String,
    val name: String,
    val iconKey: String,
    val colorKey: String,
)

data class RoomIconOptionUiModel(
    val key: String,
    val label: String,
)

data class RoomColorOptionUiModel(
    val key: String,
    val label: String,
)

data class ChoreEditorUiState(
    val choreId: String? = null,
    val canManageLifecycle: Boolean = false,
    val isActive: Boolean = true,
    val pendingApproval: Boolean = false,
    val title: String = "",
    val additionalComments: String = "",
    val roomOptions: List<RoomOptionUiModel> = defaultRoomOptions(),
    val selectedRoom: String = "Kitchen",
    val creatingRoom: Boolean = false,
    val newRoomName: String = "",
    val roomIconOptions: List<RoomIconOptionUiModel> = defaultRoomIcons(),
    val roomColorOptions: List<RoomColorOptionUiModel> = defaultRoomColors(),
    val selectedRoomIcon: String = "kitchen",
    val selectedRoomColor: String = "teal",
    val assigneeOptions: List<String> = emptyList(),
    val activeAssigneeOptions: Set<String> = emptySet(),
    val inactiveAssignmentNames: Set<String> = emptySet(),
    val assignee: String = "",
    val autoAssign: Boolean = false,
    val smartAssignmentMode: SmartAssignmentMode = SmartAssignmentMode.FairRotation,
    val assignmentPool: Set<String> = emptySet(),
    val dayAssignments: Map<ChoreWeekday, String> = emptyMap(),
    val effortLevel: ChoreEffortLevel = ChoreEffortLevel.Standard,
    val repeatsEnabled: Boolean = false,
    val repeatStartDate: String = "Today",
    val repeatFrequency: ChoreRepeatFrequency = ChoreRepeatFrequency.Daily,
    val dailyRepeatMode: DailyRepeatMode = DailyRepeatMode.Weekdays,
    val selectedWeekdays: Set<ChoreWeekday> = defaultWeekdays(),
    val monthlyPattern: String = "15th",
    val customPattern: String = "Every 2 weeks",
    val oneTimeDateOptions: List<String> = defaultOneTimeDateOptions(),
    val selectedOneTimeDate: String = "Today",
    val proofRequired: Boolean = true,
    val durationEnabled: Boolean = true,
    val durationMinutes: Int = 15,
    val hasChorePilotPro: Boolean = false,
    val pendingPremiumProofUnlock: Boolean = false,
    val isSaving: Boolean = false,
    val isApplyingLifecycleAction: Boolean = false,
    val lifecycleConfirmation: ChoreLifecycleConfirmationUiState? = null,
    val lifecycleFeedbackMessage: String? = null,
    val shouldCloseAfterDelete: Boolean = false,
    val errorMessage: String? = null,
)

fun defaultRoomOptions(): List<RoomOptionUiModel> = listOf(
    RoomOptionUiModel(id = "room-kitchen", name = "Kitchen", iconKey = "kitchen", colorKey = "teal"),
    RoomOptionUiModel(id = "room-living", name = "Living room", iconKey = "living", colorKey = "violet"),
    RoomOptionUiModel(id = "room-bedroom", name = "Bedroom", iconKey = "bedroom", colorKey = "amber"),
    RoomOptionUiModel(id = "room-bathroom", name = "Bathroom", iconKey = "bathroom", colorKey = "sky"),
    RoomOptionUiModel(id = "room-entry", name = "Entry", iconKey = "entry", colorKey = "rose"),
    RoomOptionUiModel(id = "room-outside", name = "Outside", iconKey = "outside", colorKey = "sage"),
)

fun defaultRoomIcons(): List<RoomIconOptionUiModel> = listOf(
    RoomIconOptionUiModel(key = "kitchen", label = "Kitchen"),
    RoomIconOptionUiModel(key = "living", label = "Living"),
    RoomIconOptionUiModel(key = "bedroom", label = "Bedroom"),
    RoomIconOptionUiModel(key = "bathroom", label = "Bath"),
    RoomIconOptionUiModel(key = "entry", label = "Entry"),
    RoomIconOptionUiModel(key = "outside", label = "Outside"),
    RoomIconOptionUiModel(key = "laundry", label = "Laundry"),
    RoomIconOptionUiModel(key = "pets", label = "Pets"),
    RoomIconOptionUiModel(key = "trash", label = "Trash"),
    RoomIconOptionUiModel(key = "clean", label = "Cleaning"),
    RoomIconOptionUiModel(key = "dining", label = "Dining"),
    RoomIconOptionUiModel(key = "play", label = "Play"),
    RoomIconOptionUiModel(key = "recycle", label = "Recycle"),
)

fun defaultRoomColors(): List<RoomColorOptionUiModel> = listOf(
    RoomColorOptionUiModel(key = "violet", label = "Violet"),
    RoomColorOptionUiModel(key = "teal", label = "Teal"),
    RoomColorOptionUiModel(key = "amber", label = "Amber"),
    RoomColorOptionUiModel(key = "rose", label = "Rose"),
    RoomColorOptionUiModel(key = "sky", label = "Sky"),
    RoomColorOptionUiModel(key = "sage", label = "Sage"),
)

fun defaultOneTimeDateOptions(): List<String> = listOf(
    "Today",
    "Tomorrow",
    "This Friday",
    "Next Monday",
)

fun defaultWeekdays(): Set<ChoreWeekday> = setOf(
    ChoreWeekday.Monday,
    ChoreWeekday.Tuesday,
    ChoreWeekday.Wednesday,
    ChoreWeekday.Thursday,
    ChoreWeekday.Friday,
)
