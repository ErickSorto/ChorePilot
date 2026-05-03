package com.sortoapps.chorepilot.features.chores.presentation.viewmodel

sealed interface ChoreEditorUiEvent {
    data object TriggerVibration : ChoreEditorUiEvent

    data object TriggerVibrationSuccess : ChoreEditorUiEvent

    data class LoadChore(val choreId: String?) : ChoreEditorUiEvent

    data class TitleChanged(val value: String) : ChoreEditorUiEvent

    data class AdditionalCommentsChanged(val value: String) : ChoreEditorUiEvent

    data class RoomSelected(val roomName: String) : ChoreEditorUiEvent

    data class CreatingRoomChanged(val enabled: Boolean) : ChoreEditorUiEvent

    data class NewRoomNameChanged(val value: String) : ChoreEditorUiEvent

    data class RoomIconSelected(val iconKey: String) : ChoreEditorUiEvent

    data class RoomColorSelected(val colorKey: String) : ChoreEditorUiEvent

    data class AssigneeChanged(val value: String) : ChoreEditorUiEvent

    data class AutoAssignChanged(val enabled: Boolean) : ChoreEditorUiEvent

    data class SmartAssignmentModeChanged(val mode: SmartAssignmentMode) : ChoreEditorUiEvent

    data class AssignmentPoolToggled(val member: String) : ChoreEditorUiEvent

    data class DayAssignmentChanged(val weekday: ChoreWeekday, val assignee: String) : ChoreEditorUiEvent

    data class EffortLevelChanged(val level: ChoreEffortLevel) : ChoreEditorUiEvent

    data class RepeatsEnabledChanged(val enabled: Boolean) : ChoreEditorUiEvent

    data class RepeatFrequencyChanged(val frequency: ChoreRepeatFrequency) : ChoreEditorUiEvent

    data class RepeatStartDateChanged(val value: String) : ChoreEditorUiEvent

    data class OneTimeDateChanged(val value: String) : ChoreEditorUiEvent

    data class DailyRepeatModeChanged(val mode: DailyRepeatMode) : ChoreEditorUiEvent

    data class WeekdayToggled(val weekday: ChoreWeekday) : ChoreEditorUiEvent

    data class MonthlyPatternChanged(val value: String) : ChoreEditorUiEvent

    data class CustomPatternChanged(val value: String) : ChoreEditorUiEvent

    data class ProofRequiredChanged(val enabled: Boolean) : ChoreEditorUiEvent

    data class ProofRequiredUpgradeRequested(val enabled: Boolean) : ChoreEditorUiEvent

    data class DurationEnabledChanged(val enabled: Boolean) : ChoreEditorUiEvent

    data class DurationMinutesChanged(val minutes: Int) : ChoreEditorUiEvent

    data class LifecycleActionRequested(val action: ChoreLifecycleAction) : ChoreEditorUiEvent

    data object ConfirmLifecycleAction : ChoreEditorUiEvent

    data object DismissLifecycleConfirmation : ChoreEditorUiEvent

    data class LifecycleConfirmationTextChanged(val value: String) : ChoreEditorUiEvent

    data object DeleteNavigationHandled : ChoreEditorUiEvent
}
