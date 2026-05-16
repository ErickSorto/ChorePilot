package com.sortoapps.chorepilot.features.family.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sortoapps.chorepilot.core.firebase.HouseholdInviteLifecycleStatus
import com.sortoapps.chorepilot.core.platform.SystemTextActions
import com.sortoapps.chorepilot.core.platform.VibratorUtil
import com.sortoapps.chorepilot.core.revenuecat.SubscriptionRepository
import com.sortoapps.chorepilot.features.auth.domain.HouseholdRole
import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import com.sortoapps.chorepilot.features.auth.domain.SessionSnapshot
import com.sortoapps.chorepilot.features.auth.domain.canManageFamilyAvailabilityFor
import com.sortoapps.chorepilot.features.auth.domain.canManageFamilyInviteFlow
import com.sortoapps.chorepilot.features.auth.domain.resolveAccessibleFamilyMemberId
import com.sortoapps.chorepilot.features.family.domain.FamilyAvailabilityMode
import com.sortoapps.chorepilot.features.family.domain.FamilyInviteSnapshot
import com.sortoapps.chorepilot.features.family.domain.FamilyMemberSnapshot
import com.sortoapps.chorepilot.features.family.domain.FamilyRepository
import com.sortoapps.chorepilot.features.family.domain.FamilyRepositoryState
import com.sortoapps.chorepilot.features.family.domain.FamilySnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

class FamilyViewModel(
    private val familyRepository: FamilyRepository,
    private val sessionRepository: SessionRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val vibratorUtil: VibratorUtil,
    private val systemTextActions: SystemTextActions,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FamilyUiState())
    val uiState: StateFlow<FamilyUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            familyRepository.familyState.collectLatest(::applyRepositoryState)
        }
        viewModelScope.launch {
            sessionRepository.session.collectLatest(::applySessionState)
        }
        viewModelScope.launch {
            subscriptionRepository.snapshot.collectLatest { snapshot ->
                _uiState.update { it.copy(hasChorePilotPro = snapshot.hasChorePilotPro) }
            }
        }
        viewModelScope.launch {
            familyRepository.refresh()
        }
    }

    fun onEvent(event: FamilyUiEvent) {
        when (event) {
            FamilyUiEvent.TriggerVibration -> vibratorUtil.triggerVibration()
            FamilyUiEvent.TriggerVibrationSuccess -> vibratorUtil.triggerVibrationSuccess()
            FamilyUiEvent.Refresh -> viewModelScope.launch { familyRepository.refresh() }

            is FamilyUiEvent.MemberSelected -> {
                _uiState.update { current ->
                    rebuildUiState(
                        current = current,
                        snapshot = familyRepository.familyState.value.snapshot,
                        session = sessionRepository.session.value,
                        selectedMemberId = resolveSelectedMemberId(event.memberId),
                    )
                }
            }

            is FamilyUiEvent.InviteRoleSelected -> {
                _uiState.update { current ->
                    val selectedRole = event.role
                    current.copy(
                        invite = current.invite.copy(
                            selectedRole = selectedRole,
                            actionLabel = inviteActionLabel(
                                selectedRole = selectedRole,
                                activeInvite = familyRepository.familyState.value.snapshot.activeInvite,
                            ),
                            seatImpactText = inviteSeatImpact(selectedRole),
                        ),
                    )
                }
            }

            FamilyUiEvent.RotateInviteRequested -> requestRotateInvite()
            FamilyUiEvent.CancelInviteRequested -> requestCancelInvite()
            FamilyUiEvent.InviteCopied -> copyInvite()
            FamilyUiEvent.InviteShared -> shareInvite()

            is FamilyUiEvent.ChangeMemberRoleRequested -> requestRoleChange(event.role)
            FamilyUiEvent.DeactivateMemberRequested -> requestDeactivateMember()
            FamilyUiEvent.RemoveMemberRequested -> requestRemoveMember()
            FamilyUiEvent.LeaveHouseholdRequested -> requestLeaveHousehold()
            is FamilyUiEvent.ConfirmationTextChanged -> {
                _uiState.update { state ->
                    val confirmation = state.confirmation ?: return@update state
                    state.copy(
                        confirmation = confirmation.copy(
                            confirmationText = event.value,
                            confirmEnabled = if (confirmation.requiresTextConfirmation) {
                                confirmation.requiredConfirmationText.equals(
                                    event.value.trim(),
                                    ignoreCase = true,
                                )
                            } else {
                                true
                            },
                        ),
                    )
                }
            }
            FamilyUiEvent.ConfirmPendingAction -> confirmPendingAction()
            FamilyUiEvent.DismissConfirmation -> _uiState.update { it.copy(confirmation = null) }
            FamilyUiEvent.DismissFeedback -> _uiState.update { it.copy(feedback = null) }

            is FamilyUiEvent.DayOffDateSelected -> {
                _uiState.update { state ->
                    val selectedMember = state.selectedMember
                    if (selectedMember == null) {
                        state
                    } else {
                        state.copy(
                            dayOffEditor = buildAvailabilityEditor(
                                member = selectedMember,
                                mode = FamilyAvailabilityMode.DayOff,
                                selectedStart = event.dateKey,
                                selectedEnd = event.dateKey,
                                current = state.dayOffEditor,
                            ),
                        )
                    }
                }
            }

            is FamilyUiEvent.GoneModeStartSelected -> {
                _uiState.update { state ->
                    val selectedMember = state.selectedMember
                    if (selectedMember == null) {
                        state
                    } else {
                        val endDate = state.goneModeEditor.selectedEndDate.takeIf { it >= event.dateKey } ?: event.dateKey
                        state.copy(
                            goneModeEditor = buildAvailabilityEditor(
                                member = selectedMember,
                                mode = FamilyAvailabilityMode.GoneMode,
                                selectedStart = event.dateKey,
                                selectedEnd = endDate,
                                current = state.goneModeEditor,
                            ),
                        )
                    }
                }
            }

            is FamilyUiEvent.GoneModeEndSelected -> {
                _uiState.update { state ->
                    val selectedMember = state.selectedMember
                    if (selectedMember == null) {
                        state
                    } else {
                        state.copy(
                            goneModeEditor = buildAvailabilityEditor(
                                member = selectedMember,
                                mode = FamilyAvailabilityMode.GoneMode,
                                selectedStart = state.goneModeEditor.selectedStartDate,
                                selectedEnd = event.dateKey,
                                current = state.goneModeEditor,
                            ),
                        )
                    }
                }
            }

            FamilyUiEvent.ConfirmDayOff -> saveAvailability(FamilyAvailabilityMode.DayOff)
            FamilyUiEvent.ConfirmGoneMode -> saveAvailability(FamilyAvailabilityMode.GoneMode)
            FamilyUiEvent.AvailabilityFeedbackDismissed -> {
                _uiState.update { state ->
                    state.copy(
                        dayOffEditor = state.dayOffEditor.copy(successTitle = null, successBody = ""),
                        goneModeEditor = state.goneModeEditor.copy(successTitle = null, successBody = ""),
                    )
                }
            }

            FamilyUiEvent.ClearAvailabilityRequested -> clearAvailability()
        }
    }

    private fun applyRepositoryState(repositoryState: FamilyRepositoryState) {
        _uiState.update { current ->
            rebuildUiState(
                current = current.copy(
                    isLoading = repositoryState.isLoading,
                    errorMessage = repositoryState.errorMessage,
                ),
                snapshot = repositoryState.snapshot,
                session = sessionRepository.session.value,
                selectedMemberId = current.selectedMember?.id ?: repositoryState.snapshot.members.firstOrNull()?.id,
            )
        }
    }

    private fun applySessionState(session: SessionSnapshot) {
        _uiState.update { current ->
            rebuildUiState(
                current = current.copy(viewerRole = session.role),
                snapshot = familyRepository.familyState.value.snapshot,
                session = session,
                selectedMemberId = current.selectedMember?.id ?: familyRepository.familyState.value.snapshot.members.firstOrNull()?.id,
            )
        }
    }

    private fun requestRotateInvite() {
        val state = _uiState.value
        if (!state.canOpenInviteFlow) {
            showPermissionFeedback("Only owners and admins can manage invite codes.")
            return
        }
        val selectedRole = state.invite.selectedRole
        _uiState.update {
            it.copy(
                confirmation = FamilyConfirmationUiState(
                    action = FamilyConfirmationAction.RotateInvite,
                    title = if (state.invite.hasActiveInvite) {
                        "Generate fresh code?"
                    } else {
                        "Generate invite?"
                    },
                    body = if (state.invite.hasActiveInvite) {
                        "This replaces the current code and creates a new ${selectedRole.name.lowercase()} invite."
                    } else {
                        "This creates a fresh ${selectedRole.name.lowercase()} invite you can share right away."
                    },
                    confirmLabel = if (state.invite.hasActiveInvite) "Generate fresh code" else "Generate invite",
                    isDestructive = state.invite.hasActiveInvite,
                ),
            )
        }
    }

    private fun requestCancelInvite() {
        val state = _uiState.value
        if (!state.canOpenInviteFlow) {
            showPermissionFeedback("Only owners and admins can manage invite codes.")
            return
        }
        if (!state.invite.canCancel) {
            showPermissionFeedback("There isn't a live invite to revoke right now.")
            return
        }
        _uiState.update {
            it.copy(
                confirmation = FamilyConfirmationUiState(
                    action = FamilyConfirmationAction.CancelInvite,
                    title = "Revoke current invite?",
                    body = "The current invite code will stop working immediately until you generate a fresh one.",
                    confirmLabel = "Revoke invite",
                ),
            )
        }
    }

    private fun copyInvite() {
        if (!_uiState.value.canOpenInviteFlow) {
            showPermissionFeedback("Only owners and admins can manage invite codes.")
            return
        }
        val text = _uiState.value.invite.shareText.ifBlank { _uiState.value.invite.code }
        if (text.isBlank()) {
            showPermissionFeedback("Generate an invite first so there's something to copy.")
            return
        }
        runCatching { systemTextActions.copyText(text) }
            .onSuccess {
                vibratorUtil.triggerVibrationSuccess()
                _uiState.update { it.copy(feedback = FamilyFeedbackUiState("Invite copied.")) }
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        feedback = FamilyFeedbackUiState(
                            message = error.message ?: "We couldn't copy that invite right now.",
                            isError = true,
                        ),
                    )
                }
            }
    }

    private fun shareInvite() {
        if (!_uiState.value.canOpenInviteFlow) {
            showPermissionFeedback("Only owners and admins can manage invite codes.")
            return
        }
        val text = _uiState.value.invite.shareText.ifBlank { _uiState.value.invite.code }
        if (text.isBlank()) {
            showPermissionFeedback("Generate an invite first so there's something to share.")
            return
        }
        runCatching { systemTextActions.shareText(text) }
            .onSuccess {
                vibratorUtil.triggerVibrationSuccess()
                _uiState.update { it.copy(feedback = FamilyFeedbackUiState("Share sheet opened.")) }
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        feedback = FamilyFeedbackUiState(
                            message = error.message ?: "We couldn't open sharing right now.",
                            isError = true,
                        ),
                    )
                }
            }
    }

    private fun requestRoleChange(role: HouseholdRole) {
        val state = _uiState.value
        val member = state.selectedMember ?: return
        if (!state.selectedMemberAdmin.availableRoleOptions.contains(role)) {
            showPermissionFeedback(state.selectedMemberAdmin.actionHint.ifBlank {
                "You can't change this member's role from your current household seat."
            })
            return
        }
        val title = if (role == HouseholdRole.Owner) {
            "Transfer ownership to ${member.name}?"
        } else {
            "Change ${member.name} to ${role.name.lowercase()}?"
        }
        val body = if (role == HouseholdRole.Owner) {
            "This makes ${member.name} the new household owner and moves your seat down to admin."
        } else {
            "${member.name} will switch to ${role.name.lowercase()} access for this household."
        }
        _uiState.update {
            it.copy(
                confirmation = FamilyConfirmationUiState(
                    action = FamilyConfirmationAction.ChangeRole,
                    title = title,
                    body = body,
                    confirmLabel = if (role == HouseholdRole.Owner) "Transfer ownership" else "Change role",
                    targetRole = role,
                    isDestructive = role == HouseholdRole.Owner,
                ),
            )
        }
    }

    private fun requestDeactivateMember() {
        val state = _uiState.value
        val member = state.selectedMember ?: return
        if (!state.selectedMemberAdmin.canDeactivate) {
            showPermissionFeedback(state.selectedMemberAdmin.actionHint.ifBlank {
                "You can't deactivate this household seat."
            })
            return
        }
        _uiState.update {
            it.copy(
                confirmation = FamilyConfirmationUiState(
                    action = FamilyConfirmationAction.DeactivateMember,
                    title = "Deactivate ${member.name}?",
                    body = "${member.name} will keep their history, but they'll stop using an active household seat until invited back later.",
                    confirmLabel = "Deactivate seat",
                ),
            )
        }
    }

    private fun requestRemoveMember() {
        val state = _uiState.value
        val member = state.selectedMember ?: return
        if (!state.selectedMemberAdmin.canRemove) {
            showPermissionFeedback(state.selectedMemberAdmin.actionHint.ifBlank {
                "You can't remove this member from the household."
            })
            return
        }
        _uiState.update {
            it.copy(
                confirmation = FamilyConfirmationUiState(
                    action = FamilyConfirmationAction.RemoveMember,
                    title = "Remove ${member.name} from the household?",
                    body = "${member.name} will lose access to this household and will need a new invite to come back.",
                    confirmLabel = "Remove member",
                ),
            )
        }
    }

    private fun requestLeaveHousehold() {
        val state = _uiState.value
        if (!state.canLeaveHousehold) {
            showPermissionFeedback(
                state.leaveHouseholdHint.ifBlank { "Transfer ownership before leaving this household." },
            )
            return
        }
        val deleteHousehold = state.leaveHouseholdLabel.contains("Delete", ignoreCase = true)
        _uiState.update {
            it.copy(
                confirmation = FamilyConfirmationUiState(
                    action = if (deleteHousehold) FamilyConfirmationAction.DeleteHousehold else FamilyConfirmationAction.LeaveHousehold,
                    title = if (deleteHousehold) {
                        "Delete this household?"
                    } else {
                        "Leave this household?"
                    },
                    body = if (deleteHousehold) {
                        "You're the last active household member. Deleting the household removes chores, proofs, invites, and saved household images for everyone. Type delete to unlock the final action."
                    } else {
                        "You'll lose access to this household and will need a fresh invite if you want to come back later."
                    },
                    confirmLabel = state.leaveHouseholdLabel,
                    requiresTextConfirmation = deleteHousehold,
                    requiredConfirmationText = if (deleteHousehold) "delete" else "",
                    confirmEnabled = !deleteHousehold,
                ),
            )
        }
    }

    private fun confirmPendingAction() {
        val confirmation = _uiState.value.confirmation ?: return
        when (confirmation.action) {
            FamilyConfirmationAction.ChangeRole -> {
                val member = _uiState.value.selectedMember ?: return
                val role = confirmation.targetRole ?: return
                performMutation(
                    successMessage = if (role == HouseholdRole.Owner) {
                        "Ownership moved to ${member.name}."
                    } else {
                        "${member.name} is now ${role.name.lowercase()}."
                    },
                ) {
                    familyRepository.changeMemberRole(member.id, role)
                }
            }

            FamilyConfirmationAction.DeactivateMember -> {
                val member = _uiState.value.selectedMember ?: return
                performMutation("${member.name} is no longer taking an active seat.") {
                    familyRepository.deactivateMember(member.id)
                }
            }

            FamilyConfirmationAction.RemoveMember -> {
                val member = _uiState.value.selectedMember ?: return
                performMutation("${member.name} was removed from the household.") {
                    familyRepository.removeMember(member.id)
                }
            }

            FamilyConfirmationAction.CancelInvite -> {
                performMutation("Invite revoked.") {
                    familyRepository.cancelInvite()
                }
            }

            FamilyConfirmationAction.RotateInvite -> {
                val hadActiveInvite = _uiState.value.invite.hasActiveInvite
                performMutation(
                    successMessage = if (hadActiveInvite) "Fresh invite ready to share." else "Invite ready to share.",
                ) {
                    familyRepository.rotateInvite(_uiState.value.invite.selectedRole)
                }
            }

            FamilyConfirmationAction.LeaveHousehold -> {
                performMutation("You left the household.") {
                    familyRepository.leaveHousehold()
                }
            }

            FamilyConfirmationAction.DeleteHousehold -> {
                performMutation("Household deleted.") {
                    familyRepository.deleteHousehold()
                }
            }
        }
    }

    private fun performMutation(
        successMessage: String,
        block: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess {
                    vibratorUtil.triggerVibrationSuccess()
                    _uiState.update {
                        it.copy(
                            confirmation = null,
                            feedback = FamilyFeedbackUiState(successMessage),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            confirmation = null,
                            feedback = FamilyFeedbackUiState(
                                message = error.message ?: "We couldn't finish that household change right now.",
                                isError = true,
                            ),
                        )
                    }
                }
        }
    }

    private fun clearAvailability() {
        val memberId = _uiState.value.selectedMember?.id ?: return
        val session = sessionRepository.session.value
        if (!session.role.canManageFamilyAvailabilityFor(session.userId, memberId)) {
            showPermissionFeedback("You can only update your own availability.")
            return
        }
        viewModelScope.launch {
            runCatching { familyRepository.clearAvailability(memberId) }
                .onSuccess {
                    vibratorUtil.triggerVibrationSuccess()
                    _uiState.update { state ->
                        state.copy(
                            dayOffEditor = state.dayOffEditor.copy(
                                successTitle = "Availability cleared",
                                successBody = "${state.selectedMember?.name ?: "Member"} is back in the normal household flow.",
                            ),
                            goneModeEditor = state.goneModeEditor.copy(
                                successTitle = "Availability cleared",
                                successBody = "${state.selectedMember?.name ?: "Member"} is back in the normal household flow.",
                            ),
                            feedback = FamilyFeedbackUiState("Availability cleared."),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            feedback = FamilyFeedbackUiState(
                                message = error.message ?: "We couldn't clear availability right now.",
                                isError = true,
                            ),
                        )
                    }
                }
        }
    }

    private fun saveAvailability(mode: FamilyAvailabilityMode) {
        viewModelScope.launch {
            val state = _uiState.value
            val member = state.selectedMember ?: return@launch
            val session = sessionRepository.session.value
            if (!session.role.canManageFamilyAvailabilityFor(session.userId, member.id)) {
                showPermissionFeedback("You can only update your own availability.")
                return@launch
            }
            val editor = if (mode == FamilyAvailabilityMode.DayOff) state.dayOffEditor else state.goneModeEditor
            _uiState.update { current ->
                if (mode == FamilyAvailabilityMode.DayOff) {
                    current.copy(dayOffEditor = current.dayOffEditor.copy(isSaving = true))
                } else {
                    current.copy(goneModeEditor = current.goneModeEditor.copy(isSaving = true))
                }
            }
            runCatching {
                familyRepository.updateAvailability(
                    memberId = member.id,
                    mode = mode,
                    startDate = editor.selectedStartDate,
                    endDate = editor.selectedEndDate,
                )
            }.onSuccess {
                vibratorUtil.triggerVibrationSuccess()
                _uiState.update { current ->
                    val successTitle = if (mode == FamilyAvailabilityMode.DayOff) {
                        "${member.name} has the day off"
                    } else {
                        "Gone mode is set"
                    }
                    val successBody = if (mode == FamilyAvailabilityMode.DayOff) {
                        "The household will keep ${member.name} out of the flow for ${editor.startOptions.firstOrNull { it.dateKey == editor.selectedStartDate }?.title ?: "that day"}."
                    } else {
                        "${member.name}'s household status is updated for the selected travel window."
                    }
                    if (mode == FamilyAvailabilityMode.DayOff) {
                        current.copy(
                            dayOffEditor = current.dayOffEditor.copy(
                                isSaving = false,
                                successTitle = successTitle,
                                successBody = successBody,
                            ),
                            feedback = FamilyFeedbackUiState(successTitle),
                        )
                    } else {
                        current.copy(
                            goneModeEditor = current.goneModeEditor.copy(
                                isSaving = false,
                                successTitle = successTitle,
                                successBody = successBody,
                            ),
                            feedback = FamilyFeedbackUiState(successTitle),
                        )
                    }
                }
            }.onFailure { error ->
                _uiState.update { current ->
                    val message = error.message ?: "We couldn't save that household update right now."
                    if (mode == FamilyAvailabilityMode.DayOff) {
                        current.copy(
                            dayOffEditor = current.dayOffEditor.copy(
                                isSaving = false,
                                successTitle = "Couldn't save day off",
                                successBody = message,
                            ),
                            feedback = FamilyFeedbackUiState(message, isError = true),
                        )
                    } else {
                        current.copy(
                            goneModeEditor = current.goneModeEditor.copy(
                                isSaving = false,
                                successTitle = "Couldn't save gone mode",
                                successBody = message,
                            ),
                            feedback = FamilyFeedbackUiState(message, isError = true),
                        )
                    }
                }
            }
        }
    }

    private fun showPermissionFeedback(message: String) {
        _uiState.update {
            it.copy(
                feedback = FamilyFeedbackUiState(
                    message = message,
                    isError = true,
                ),
            )
        }
    }

    private fun resolveSelectedMemberId(requestedMemberId: String?): String? =
        sessionRepository.session.value.role.resolveAccessibleFamilyMemberId(
            viewerId = sessionRepository.session.value.userId,
            requestedMemberId = requestedMemberId,
        )
}

private fun rebuildUiState(
    current: FamilyUiState,
    snapshot: FamilySnapshot,
    session: SessionSnapshot,
    selectedMemberId: String?,
): FamilyUiState {
    val allMembers = snapshot.members.map(::toMemberUiModel)
    val isHouseholdOwner = session.userId.isNotBlank() &&
        snapshot.ownerUserId.equals(session.userId, ignoreCase = true)
    val effectiveViewerRole = if (isHouseholdOwner) HouseholdRole.Owner else session.role
    val canManageInvites = effectiveViewerRole.canManageFamilyInviteFlow()
    val members = if (canManageInvites) {
        allMembers
    } else {
        allMembers.filter { it.id == session.userId }
    }
    val activeMembers = members.filter { it.isActiveSeat }
    val inactiveMembers = members.filterNot { it.isActiveSeat }
    val selectedMember = members.firstOrNull {
        it.id == effectiveViewerRole.resolveAccessibleFamilyMemberId(
            viewerId = session.userId,
            requestedMemberId = selectedMemberId,
        )
    }
        ?: activeMembers.firstOrNull()
        ?: inactiveMembers.firstOrNull()
        ?: allMembers.firstOrNull { it.id == session.userId }
        ?: allMembers.firstOrNull()
    val selectedRole = current.invite.selectedRole
    val memberAdmin = buildMemberAdminUiState(
        viewerId = session.userId,
        viewerRole = effectiveViewerRole,
        selectedMember = selectedMember,
    )
    val canManageSelectedMemberAvailability = selectedMember != null &&
        effectiveViewerRole.canManageFamilyAvailabilityFor(session.userId, selectedMember.id)
    val isSelectedMemberViewer = selectedMember?.id.equals(session.userId, ignoreCase = true)
    val ownerCanDeleteHousehold = session.householdId.isNotBlank() &&
        effectiveViewerRole == HouseholdRole.Owner &&
        allMembers.count { it.isActiveSeat && it.id != session.userId } == 0 &&
        allMembers.any { it.isActiveSeat && it.id == session.userId }
    val canLeaveHousehold = session.householdId.isNotBlank() &&
        (effectiveViewerRole != HouseholdRole.Owner || ownerCanDeleteHousehold)
    return current.copy(
        householdName = snapshot.householdName,
        pulseLabel = snapshot.pulseLabel,
        householdBadge = snapshot.householdBadge,
        summaryMetrics = snapshot.summaryMetrics,
        activeMembers = activeMembers,
        inactiveMembers = inactiveMembers,
        members = members,
        selectedMember = selectedMember,
        invite = current.invite.copy(
            selectedRole = selectedRole,
            code = snapshot.activeInvite.code,
            lifecycleStatus = snapshot.activeInvite.lifecycleStatus,
            statusLabel = snapshot.activeInvite.statusLabel,
            supportingText = snapshot.activeInvite.supportingText,
            generatedAtLabel = snapshot.activeInvite.generatedAtLabel,
            expiryLabel = snapshot.activeInvite.expiryLabel,
            usageLabel = snapshot.activeInvite.usageLabel,
            shareText = if (canManageInvites) snapshot.activeInvite.shareText else "",
            hasActiveInvite = snapshot.activeInvite.isJoinable,
            isJoinable = snapshot.activeInvite.isJoinable,
            canCopy = canManageInvites && snapshot.activeInvite.isJoinable && snapshot.activeInvite.code.isNotBlank(),
            canShare = canManageInvites && snapshot.activeInvite.isJoinable && snapshot.activeInvite.shareText.isNotBlank(),
            canManage = canManageInvites,
            canCancel = canManageInvites && snapshot.activeInvite.isJoinable,
            canRotate = canManageInvites,
            actionLabel = inviteActionLabel(
                selectedRole = selectedRole,
                activeInvite = snapshot.activeInvite,
            ),
            seatImpactText = inviteSeatImpact(selectedRole),
        ),
        dayOffEditor = selectedMember?.let {
            buildAvailabilityEditor(
                member = it,
                mode = FamilyAvailabilityMode.DayOff,
                selectedStart = current.dayOffEditor.selectedStartDate,
                selectedEnd = current.dayOffEditor.selectedEndDate,
                current = current.dayOffEditor,
            )
        } ?: FamilyAvailabilityEditorUiState(mode = FamilyAvailabilityMode.DayOff),
        goneModeEditor = selectedMember?.let {
            buildAvailabilityEditor(
                member = it,
                mode = FamilyAvailabilityMode.GoneMode,
                selectedStart = current.goneModeEditor.selectedStartDate,
                selectedEnd = current.goneModeEditor.selectedEndDate,
                current = current.goneModeEditor,
            )
        } ?: FamilyAvailabilityEditorUiState(mode = FamilyAvailabilityMode.GoneMode),
        selectedMemberAdmin = memberAdmin,
        viewerRole = effectiveViewerRole,
        isSelectedMemberViewer = isSelectedMemberViewer,
        canOpenInviteFlow = canManageInvites,
        canShowInviteSummary = canManageInvites,
        canManageSelectedMemberAvailability = canManageSelectedMemberAvailability,
        activeSeatSummary = if (snapshot.activeSeatCount == 0) {
            "No active seats are filled yet."
        } else {
            "${snapshot.activeSeatCount} active seat${pluralize(snapshot.activeSeatCount)} are currently in the household."
        },
        inviteStatusSummary = inviteStatusSummary(snapshot.activeInvite),
        leaveHouseholdHint = if (session.householdId.isBlank()) {
            ""
        } else if (ownerCanDeleteHousehold) {
            "You're the last active member. Deleting the household removes chores, proofs, invites, and saved household images."
        } else if (effectiveViewerRole == HouseholdRole.Owner) {
            "Transfer ownership before leaving this household."
        } else {
            "Leaving removes this household from your account until you're invited back."
        },
        leaveHouseholdLabel = if (ownerCanDeleteHousehold) "Delete household" else "Leave household",
        canManageInvites = canManageInvites,
        canLeaveHousehold = canLeaveHousehold,
    )
}

private fun toMemberUiModel(
    snapshot: FamilyMemberSnapshot,
): FamilyMemberUiModel = FamilyMemberUiModel(
    id = snapshot.id,
    name = snapshot.name,
    role = snapshot.role,
    statusLabel = snapshot.statusLabel,
    activeChoresLabel = snapshot.activeChoresLabel,
    householdImpact = snapshot.householdImpact,
    avatarSeed = snapshot.avatarSeed,
    tone = snapshot.tone,
    assignedChoreCount = snapshot.assignedChoreCount,
    proofReviewCount = snapshot.proofReviewCount,
    isActiveSeat = snapshot.isActiveSeat,
    seatLabel = if (snapshot.isActiveSeat) "Active seat" else "Inactive seat",
    availabilityLabel = snapshot.availability?.statusLabel.orEmpty(),
    availabilitySummary = snapshot.availability?.summary.orEmpty(),
)

private fun inviteActionLabel(
    selectedRole: HouseholdRole,
    activeInvite: FamilyInviteSnapshot,
): String = when {
    activeInvite.code.isBlank() -> "Generate ${selectedRole.name.lowercase()} invite"
    activeInvite.isJoinable && activeInvite.role == selectedRole -> "Generate fresh ${selectedRole.name.lowercase()} code"
    activeInvite.isJoinable -> "Generate fresh ${selectedRole.name.lowercase()} code"
    else -> "Generate ${selectedRole.name.lowercase()} invite"
}

private fun inviteStatusSummary(
    invite: FamilyInviteSnapshot,
): String = when {
    invite.code.isBlank() -> "No active invite is live right now."
    invite.isJoinable -> "A ${invite.role.name.lowercase()} invite is active and ready to share."
    invite.lifecycleStatus == null -> "No active invite is live right now."
    invite.lifecycleStatus == HouseholdInviteLifecycleStatus.Revoked -> "The last invite was revoked."
    invite.lifecycleStatus == HouseholdInviteLifecycleStatus.Expired -> "The last invite expired."
    invite.lifecycleStatus == HouseholdInviteLifecycleStatus.Exhausted -> "The last invite has already been used up."
    else -> "The last invite was replaced by a newer code."
}

private fun inviteSeatImpact(
    selectedRole: HouseholdRole,
): String = when (selectedRole) {
    HouseholdRole.Owner -> "This transfers household ownership to a new owner."
    HouseholdRole.Admin -> "This uses the next seat for someone who can help manage chores and approvals."
    HouseholdRole.Member -> "This uses the next seat for chores, proof, and reminders."
    HouseholdRole.Guest -> "This uses the next guest seat with lighter access."
}

private fun buildMemberAdminUiState(
    viewerId: String,
    viewerRole: HouseholdRole,
    selectedMember: FamilyMemberUiModel?,
): FamilyMemberAdminUiState {
    if (selectedMember == null) return FamilyMemberAdminUiState()
    val seatImpact = if (selectedMember.isActiveSeat) {
        "Deactivating or removing ${selectedMember.name} frees one active household seat."
    } else {
        "${selectedMember.name} is already outside the active household seats."
    }
    return when (viewerRole) {
        HouseholdRole.Owner -> {
            val isSelf = selectedMember.id == viewerId
            val availableRoles = if (isSelf) {
                emptyList()
            } else {
                when (selectedMember.role) {
                    HouseholdRole.Owner -> emptyList()
                    HouseholdRole.Admin -> listOf(HouseholdRole.Owner, HouseholdRole.Member, HouseholdRole.Guest)
                    HouseholdRole.Member -> listOf(HouseholdRole.Owner, HouseholdRole.Admin, HouseholdRole.Guest)
                    HouseholdRole.Guest -> listOf(HouseholdRole.Owner, HouseholdRole.Admin, HouseholdRole.Member)
                }
            }
            FamilyMemberAdminUiState(
                canManage = !isSelf || selectedMember.role != HouseholdRole.Owner,
                canChangeRole = availableRoles.isNotEmpty(),
                canDeactivate = !isSelf && selectedMember.role != HouseholdRole.Owner && selectedMember.isActiveSeat,
                canRemove = !isSelf && selectedMember.role != HouseholdRole.Owner,
                availableRoleOptions = availableRoles,
                roleHint = "Owners can promote, demote, and transfer ownership across non-owner seats.",
                seatImpactText = seatImpact,
                actionHint = if (isSelf) {
                    "Transfer ownership before stepping out of your own seat."
                } else {
                    "This seat can be updated, deactivated, or removed safely from here."
                },
            )
        }

        HouseholdRole.Admin -> {
            val manageable = selectedMember.id != viewerId && selectedMember.role in setOf(HouseholdRole.Member, HouseholdRole.Guest)
            val availableRoles = when (selectedMember.role) {
                HouseholdRole.Member -> listOf(HouseholdRole.Guest)
                HouseholdRole.Guest -> listOf(HouseholdRole.Member)
                else -> emptyList()
            }
            FamilyMemberAdminUiState(
                canManage = manageable,
                canChangeRole = manageable && availableRoles.isNotEmpty(),
                canDeactivate = manageable && selectedMember.isActiveSeat,
                canRemove = manageable,
                availableRoleOptions = if (manageable) availableRoles else emptyList(),
                roleHint = "Admins can only switch household members between member and guest.",
                seatImpactText = seatImpact,
                actionHint = if (manageable) {
                    "Admins can manage members and guests, but not owners or other admins."
                } else {
                    "This seat needs an owner to make deeper changes."
                },
            )
        }

        HouseholdRole.Member,
        HouseholdRole.Guest,
        -> FamilyMemberAdminUiState(
            canManage = false,
            roleHint = "Only owners and admins can manage household seats.",
            seatImpactText = seatImpact,
            actionHint = "You can still use day off and gone mode without admin access.",
        )
    }
}

private fun buildAvailabilityEditor(
    member: FamilyMemberUiModel,
    mode: FamilyAvailabilityMode,
    selectedStart: String,
    selectedEnd: String,
    current: FamilyAvailabilityEditorUiState,
): FamilyAvailabilityEditorUiState {
    val dateOptions = buildDateOptions()
    val defaultStart = when {
        selectedStart.isNotBlank() -> selectedStart
        else -> dateOptions.getOrNull(1)?.dateKey ?: dateOptions.firstOrNull()?.dateKey.orEmpty()
    }
    val defaultEnd = when {
        selectedEnd.isNotBlank() -> selectedEnd
        mode == FamilyAvailabilityMode.DayOff -> defaultStart
        else -> dateOptions.getOrNull(3)?.dateKey ?: defaultStart
    }
    val normalizedEnd = if (defaultEnd < defaultStart) defaultStart else defaultEnd
    val selectedStartTitle = dateOptions.firstOrNull { it.dateKey == defaultStart }?.title ?: "that start date"
    val selectedEndTitle = dateOptions.firstOrNull { it.dateKey == normalizedEnd }?.title ?: "that return date"
    val impactBody = when (mode) {
        FamilyAvailabilityMode.DayOff ->
            "${member.name}'s ${member.assignedChoreCount} chore${pluralize(member.assignedChoreCount)} and ${member.proofReviewCount} proof review${pluralize(member.proofReviewCount)} will stay out of their queue for $selectedStartTitle."

        FamilyAvailabilityMode.GoneMode ->
            "From $selectedStartTitle through $selectedEndTitle, ${member.name}'s ${member.assignedChoreCount} chore${pluralize(member.assignedChoreCount)} and ${member.proofReviewCount} proof review${pluralize(member.proofReviewCount)} can be absorbed by the rest of the household."
    }
    return current.copy(
        mode = mode,
        memberId = member.id,
        memberName = member.name,
        statusLabel = member.statusLabel,
        title = if (mode == FamilyAvailabilityMode.DayOff) {
            "Give ${member.name} a lighter day"
        } else {
            "Take ${member.name} out of the flow for a bit"
        },
        subtitle = if (mode == FamilyAvailabilityMode.DayOff) {
            "Pick one day and preview how the household will absorb the gap."
        } else {
            "Choose a travel window and preview the household impact before you confirm it."
        },
        selectedStartDate = defaultStart,
        selectedEndDate = normalizedEnd,
        startOptions = dateOptions.map { option -> option.copy(isSelected = option.dateKey == defaultStart) },
        endOptions = dateOptions
            .filter { it.dateKey >= defaultStart }
            .map { option -> option.copy(isSelected = option.dateKey == normalizedEnd) },
        impactTitle = if (mode == FamilyAvailabilityMode.DayOff) "Household impact preview" else "Household rebalance preview",
        impactBody = impactBody,
        confirmLabel = if (mode == FamilyAvailabilityMode.DayOff) "Confirm day off" else "Activate gone mode",
    )
}

private fun buildDateOptions(): List<FamilyDateOptionUiModel> {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    return (0..13).map { index ->
        val date = today.plus(DatePeriod(days = index))
        FamilyDateOptionUiModel(
            dateKey = date.toString(),
            title = when (index) {
                0 -> "Today"
                1 -> "Tomorrow"
                else -> weekdayTitle(date)
            },
            supportingText = readableDate(date),
        )
    }
}

private fun weekdayTitle(date: LocalDate): String = date.dayOfWeek.name
    .lowercase()
    .replaceFirstChar(Char::titlecase)

private fun readableDate(date: LocalDate): String = "${date.month.name.lowercase().replaceFirstChar(Char::titlecase)} ${date.day}"

private fun pluralize(count: Int): String = if (count == 1) "" else "s"
