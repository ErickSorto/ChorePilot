package com.sortoapps.chorepilot.features.family.presentation.viewmodel

import com.sortoapps.chorepilot.core.demo.DemoMetric
import com.sortoapps.chorepilot.core.demo.DemoTone
import com.sortoapps.chorepilot.core.firebase.HouseholdInviteLifecycleStatus
import com.sortoapps.chorepilot.features.auth.domain.HouseholdRole
import com.sortoapps.chorepilot.features.family.domain.FamilyAvailabilityMode

data class FamilyMemberUiModel(
    val id: String,
    val name: String,
    val role: HouseholdRole = HouseholdRole.Member,
    val statusLabel: String = "",
    val activeChoresLabel: String = "",
    val householdImpact: String = "",
    val avatarSeed: String = "",
    val tone: DemoTone = DemoTone.Primary,
    val assignedChoreCount: Int = 0,
    val proofReviewCount: Int = 0,
    val isActiveSeat: Boolean = true,
    val seatLabel: String = "Active seat",
    val availabilityLabel: String = "",
    val availabilitySummary: String = "",
)

data class FamilyInviteUiState(
    val selectedRole: HouseholdRole = HouseholdRole.Member,
    val code: String = "",
    val lifecycleStatus: HouseholdInviteLifecycleStatus? = null,
    val statusLabel: String = "No active invite",
    val supportingText: String = "Create a fresh invite code when you're ready to bring someone in.",
    val generatedAtLabel: String = "",
    val expiryLabel: String = "",
    val usageLabel: String = "",
    val shareText: String = "",
    val hasActiveInvite: Boolean = false,
    val isJoinable: Boolean = false,
    val canCopy: Boolean = false,
    val canShare: Boolean = false,
    val canManage: Boolean = false,
    val canCancel: Boolean = false,
    val canRotate: Boolean = false,
    val actionLabel: String = "Generate invite code",
    val seatImpactText: String = "This will use the next open household seat.",
)

data class FamilyDateOptionUiModel(
    val dateKey: String,
    val title: String,
    val supportingText: String,
    val isSelected: Boolean = false,
)

data class FamilyAvailabilityEditorUiState(
    val mode: FamilyAvailabilityMode,
    val memberId: String = "",
    val memberName: String = "Member",
    val statusLabel: String = "",
    val title: String = "",
    val subtitle: String = "",
    val selectedStartDate: String = "",
    val selectedEndDate: String = "",
    val startOptions: List<FamilyDateOptionUiModel> = emptyList(),
    val endOptions: List<FamilyDateOptionUiModel> = emptyList(),
    val impactTitle: String = "",
    val impactBody: String = "",
    val confirmLabel: String = "",
    val successTitle: String? = null,
    val successBody: String = "",
    val isSaving: Boolean = false,
)

data class FamilyMemberAdminUiState(
    val canManage: Boolean = false,
    val canChangeRole: Boolean = false,
    val canDeactivate: Boolean = false,
    val canRemove: Boolean = false,
    val availableRoleOptions: List<HouseholdRole> = emptyList(),
    val roleHint: String = "",
    val seatImpactText: String = "",
    val actionHint: String = "",
)

enum class FamilyConfirmationAction {
    ChangeRole,
    DeactivateMember,
    RemoveMember,
    CancelInvite,
    RotateInvite,
    LeaveHousehold,
    DeleteHousehold,
}

data class FamilyConfirmationUiState(
    val action: FamilyConfirmationAction,
    val title: String,
    val body: String,
    val confirmLabel: String,
    val isDestructive: Boolean = true,
    val targetRole: HouseholdRole? = null,
    val requiresTextConfirmation: Boolean = false,
    val requiredConfirmationText: String = "",
    val confirmationText: String = "",
    val confirmEnabled: Boolean = true,
)

data class FamilyFeedbackUiState(
    val message: String,
    val isError: Boolean = false,
)

data class FamilyUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val householdName: String = "",
    val pulseLabel: String = "",
    val householdBadge: String = "",
    val summaryMetrics: List<DemoMetric> = emptyList(),
    val activeMembers: List<FamilyMemberUiModel> = emptyList(),
    val inactiveMembers: List<FamilyMemberUiModel> = emptyList(),
    val members: List<FamilyMemberUiModel> = emptyList(),
    val selectedMember: FamilyMemberUiModel? = null,
    val invite: FamilyInviteUiState = FamilyInviteUiState(),
    val dayOffEditor: FamilyAvailabilityEditorUiState = FamilyAvailabilityEditorUiState(mode = FamilyAvailabilityMode.DayOff),
    val goneModeEditor: FamilyAvailabilityEditorUiState = FamilyAvailabilityEditorUiState(mode = FamilyAvailabilityMode.GoneMode),
    val selectedMemberAdmin: FamilyMemberAdminUiState = FamilyMemberAdminUiState(),
    val confirmation: FamilyConfirmationUiState? = null,
    val feedback: FamilyFeedbackUiState? = null,
    val viewerRole: HouseholdRole = HouseholdRole.Member,
    val isSelectedMemberViewer: Boolean = false,
    val canOpenInviteFlow: Boolean = false,
    val canShowInviteSummary: Boolean = false,
    val canManageSelectedMemberAvailability: Boolean = false,
    val activeSeatSummary: String = "",
    val inviteStatusSummary: String = "",
    val leaveHouseholdLabel: String = "Leave household",
    val leaveHouseholdHint: String = "",
    val canManageInvites: Boolean = false,
    val canLeaveHousehold: Boolean = false,
    val hasChorePilotPro: Boolean = false,
)
