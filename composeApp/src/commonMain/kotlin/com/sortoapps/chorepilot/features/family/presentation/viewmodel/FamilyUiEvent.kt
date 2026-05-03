package com.sortoapps.chorepilot.features.family.presentation.viewmodel

import com.sortoapps.chorepilot.features.auth.domain.HouseholdRole

sealed interface FamilyUiEvent {
    data object TriggerVibration : FamilyUiEvent

    data object TriggerVibrationSuccess : FamilyUiEvent

    data object Refresh : FamilyUiEvent

    data class MemberSelected(val memberId: String) : FamilyUiEvent

    data class InviteRoleSelected(val role: HouseholdRole) : FamilyUiEvent

    data object RotateInviteRequested : FamilyUiEvent

    data object CancelInviteRequested : FamilyUiEvent

    data object InviteCopied : FamilyUiEvent

    data object InviteShared : FamilyUiEvent

    data class ChangeMemberRoleRequested(val role: HouseholdRole) : FamilyUiEvent

    data object DeactivateMemberRequested : FamilyUiEvent

    data object RemoveMemberRequested : FamilyUiEvent

    data object LeaveHouseholdRequested : FamilyUiEvent

    data class ConfirmationTextChanged(val value: String) : FamilyUiEvent

    data object ConfirmPendingAction : FamilyUiEvent

    data object DismissConfirmation : FamilyUiEvent

    data object DismissFeedback : FamilyUiEvent

    data class DayOffDateSelected(val dateKey: String) : FamilyUiEvent

    data class GoneModeStartSelected(val dateKey: String) : FamilyUiEvent

    data class GoneModeEndSelected(val dateKey: String) : FamilyUiEvent

    data object ConfirmDayOff : FamilyUiEvent

    data object ConfirmGoneMode : FamilyUiEvent

    data object AvailabilityFeedbackDismissed : FamilyUiEvent

    data object ClearAvailabilityRequested : FamilyUiEvent
}
