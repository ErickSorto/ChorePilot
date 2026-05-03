package com.sortoapps.chorepilot.features.family.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import chorepilot.composeapp.generated.resources.Res
import chorepilot.composeapp.generated.resources.m2_family_empty
import chorepilot.composeapp.generated.resources.m2_family_overview_hero
import chorepilot.composeapp.generated.resources.m2_vacation_helper
import com.sortoapps.chorepilot.core.designsystem.AvatarBadge
import com.sortoapps.chorepilot.core.designsystem.ChorePilotGradientBackground
import com.sortoapps.chorepilot.core.designsystem.ChorePilotScreen
import com.sortoapps.chorepilot.core.designsystem.IllustrationCard
import com.sortoapps.chorepilot.core.designsystem.MetricRow
import com.sortoapps.chorepilot.core.designsystem.PrimaryButton
import com.sortoapps.chorepilot.core.designsystem.SecondaryButton
import com.sortoapps.chorepilot.core.designsystem.ToneChip
import com.sortoapps.chorepilot.core.preview.ChorePilotPreviewData
import com.sortoapps.chorepilot.core.preview.ChorePilotPreviewTheme
import com.sortoapps.chorepilot.core.preview.CompactPreviewShell
import com.sortoapps.chorepilot.features.auth.domain.HouseholdRole
import com.sortoapps.chorepilot.features.family.domain.FamilyAvailabilityMode
import com.sortoapps.chorepilot.features.family.presentation.viewmodel.FamilyAvailabilityEditorUiState
import com.sortoapps.chorepilot.features.family.presentation.viewmodel.FamilyConfirmationUiState
import com.sortoapps.chorepilot.features.family.presentation.viewmodel.FamilyFeedbackUiState
import com.sortoapps.chorepilot.features.family.presentation.viewmodel.FamilyInviteUiState
import com.sortoapps.chorepilot.features.family.presentation.viewmodel.FamilyMemberUiModel
import com.sortoapps.chorepilot.features.family.presentation.viewmodel.FamilyUiEvent
import com.sortoapps.chorepilot.features.family.presentation.viewmodel.FamilyUiState

@Composable
fun FamilyScreen(
    state: FamilyUiState,
    onEvent: (FamilyUiEvent) -> Unit,
    onOpenMember: (String) -> Unit,
    onInviteMember: () -> Unit,
    onOpenPremium: () -> Unit,
) {
    ChorePilotGradientBackground {
        ChorePilotScreen(
            title = state.householdName,
            subtitle = "${state.pulseLabel} • ${state.householdBadge}",
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FamilyFeedbackCard(
                    feedback = state.feedback,
                    onDismiss = {
                        onEvent(FamilyUiEvent.TriggerVibration)
                        onEvent(FamilyUiEvent.DismissFeedback)
                    },
                )
                FamilyOverviewCard(
                    householdBadge = state.householdBadge,
                    activeSeatSummary = state.activeSeatSummary.ifBlank { "Seats loading" },
                    inviteStatusSummary = if (state.canShowInviteSummary) state.inviteStatusSummary else "",
                )
                if (state.errorMessage != null) {
                    HelperCard(
                        title = "We couldn't refresh the household just now.",
                        body = state.errorMessage,
                        icon = Icons.Rounded.Groups,
                    )
                }
                MetricRow(metrics = state.summaryMetrics)
                if (state.members.isEmpty() && !state.isLoading) {
                    if (state.canOpenInviteFlow) {
                        FamilyEmptyStateScreen(
                            onInviteMember = {
                                onEvent(FamilyUiEvent.TriggerVibration)
                                onInviteMember()
                            },
                        )
                    } else {
                        HelperCard(
                            title = "Your household view is still syncing.",
                            body = "Only your own family seat is visible from this role. Check back in a moment if your household hasn't loaded yet.",
                            icon = Icons.Rounded.Groups,
                        )
                    }
                } else {
                    SectionHeading(
                        title = if (state.canOpenInviteFlow) "Active household seats" else "Your household seat",
                        subtitle = if (state.canOpenInviteFlow) {
                            "People currently taking chores, proof review, and household reminders."
                        } else {
                            "Your chore load and availability live here."
                        },
                    )
                    state.activeMembers.forEach { member ->
                        MemberRow(
                            member = member,
                            onClick = {
                                onEvent(FamilyUiEvent.TriggerVibration)
                                onEvent(FamilyUiEvent.MemberSelected(member.id))
                                onOpenMember(member.id)
                            },
                        )
                    }
                    if (state.inactiveMembers.isNotEmpty() && state.canOpenInviteFlow) {
                        SectionHeading(
                            title = "Inactive seats",
                            subtitle = "These members stay visible so you can review role, load, and removal decisions safely.",
                        )
                        state.inactiveMembers.forEach { member ->
                            MemberRow(
                                member = member,
                                onClick = {
                                    onEvent(FamilyUiEvent.TriggerVibration)
                                    onEvent(FamilyUiEvent.MemberSelected(member.id))
                                    onOpenMember(member.id)
                                },
                            )
                        }
                    }
                }
                if (state.canOpenInviteFlow) {
                    PrimaryButton(
                        text = "Invite member",
                        onClick = {
                            onEvent(FamilyUiEvent.TriggerVibration)
                            onInviteMember()
                        },
                    )
                }
                if (state.canLeaveHousehold) {
                    SecondaryButton(
                        text = state.leaveHouseholdLabel,
                        onClick = {
                            onEvent(FamilyUiEvent.TriggerVibration)
                            onEvent(FamilyUiEvent.LeaveHouseholdRequested)
                        },
                    )
                } else if (state.leaveHouseholdHint.isNotBlank()) {
                    HelperCard(
                        title = "Leaving safely",
                        body = state.leaveHouseholdHint,
                        icon = Icons.AutoMirrored.Rounded.ExitToApp,
                    )
                }
            }
        }
        FamilyConfirmationDialog(
            confirmation = state.confirmation,
            onConfirm = {
                onEvent(FamilyUiEvent.TriggerVibration)
                onEvent(FamilyUiEvent.ConfirmPendingAction)
            },
            onTextChange = { onEvent(FamilyUiEvent.ConfirmationTextChanged(it)) },
            onDismiss = { onEvent(FamilyUiEvent.DismissConfirmation) },
        )
    }
}

@Composable
fun MemberProfileScreen(
    state: FamilyUiState,
    onEvent: (FamilyUiEvent) -> Unit,
    onDayOff: (String) -> Unit,
    onGoneMode: (String) -> Unit,
    onBack: () -> Unit,
) {
    val member = state.selectedMember
    if (member == null) {
        ChorePilotGradientBackground {
            ChorePilotScreen(
                title = "Member profile",
                subtitle = if (state.isLoading) {
                    "Loading this household seat"
                } else {
                    "This seat isn't available anymore"
                },
                actionLabel = "Back",
                onActionClick = onBack,
            ) {
                HelperCard(
                    title = if (state.isLoading) {
                        "Pulling member details"
                    } else {
                        "This member isn't available anymore."
                    },
                    body = if (state.isLoading) {
                        "We're syncing the latest family details now."
                    } else {
                        "They may have been removed, deactivated, or you may no longer have access to this seat."
                    },
                    icon = Icons.Rounded.Groups,
                )
            }
        }
        return
    }
    ChorePilotGradientBackground {
        ChorePilotScreen(
            title = member.name,
            subtitle = "${member.role.name} • ${member.statusLabel}",
            actionLabel = "Back",
            onActionClick = onBack,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FamilyFeedbackCard(
                    feedback = state.feedback,
                    onDismiss = {
                        onEvent(FamilyUiEvent.TriggerVibration)
                        onEvent(FamilyUiEvent.DismissFeedback)
                    },
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AvatarBadge(seed = member.avatarSeed, tone = member.tone)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                ToneChip(text = member.role.name, tone = member.tone)
                                ToneChip(text = member.seatLabel)
                                if (member.availabilityLabel.isNotBlank()) {
                                    ToneChip(text = member.availabilityLabel)
                                }
                            }
                            Text(
                                member.activeChoresLabel,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                HelperCard(
                    title = if (member.availabilityLabel.isNotBlank()) member.availabilityLabel else "Current household status",
                    body = if (member.availabilitySummary.isNotBlank()) member.availabilitySummary else member.activeChoresLabel,
                    icon = if (member.availabilityLabel.isNotBlank()) Icons.Rounded.TravelExplore else Icons.Rounded.Groups,
                )
                MetricRow(
                    metrics = listOf(
                        com.sortoapps.chorepilot.core.demo.DemoMetric(
                            label = member.assignedChoreCount.toString(),
                            value = "Assigned",
                        ),
                        com.sortoapps.chorepilot.core.demo.DemoMetric(
                            label = member.proofReviewCount.toString(),
                            value = "Proof reviews",
                            tone = com.sortoapps.chorepilot.core.demo.DemoTone.Secondary,
                        ),
                        com.sortoapps.chorepilot.core.demo.DemoMetric(
                            label = member.role.name,
                            value = "Role",
                            tone = com.sortoapps.chorepilot.core.demo.DemoTone.Warning,
                        ),
                        com.sortoapps.chorepilot.core.demo.DemoMetric(
                            label = if (member.isActiveSeat) "Active" else "Inactive",
                            value = "Seat",
                            tone = com.sortoapps.chorepilot.core.demo.DemoTone.Tertiary,
                        ),
                    ),
                )
                HelperCard(
                    title = "Household impact",
                    body = member.householdImpact,
                    icon = Icons.Rounded.Shield,
                )
                if (state.selectedMemberAdmin.canManage || state.selectedMemberAdmin.availableRoleOptions.isNotEmpty()) {
                    HelperCard(
                        title = "Admin controls",
                        body = listOf(
                            state.selectedMemberAdmin.roleHint,
                            state.selectedMemberAdmin.seatImpactText,
                            state.selectedMemberAdmin.actionHint,
                        ).filter(String::isNotBlank).joinToString(" "),
                        icon = Icons.Rounded.AdminPanelSettings,
                    )
                }
                if (state.selectedMemberAdmin.availableRoleOptions.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        state.selectedMemberAdmin.availableRoleOptions.forEach { role ->
                            SecondaryActionChip(
                                icon = Icons.Rounded.SwapHoriz,
                                text = "Make ${role.name}",
                                enabled = true,
                                onClick = {
                                    onEvent(FamilyUiEvent.TriggerVibration)
                                    onEvent(FamilyUiEvent.ChangeMemberRoleRequested(role))
                                },
                            )
                        }
                    }
                }
                if (state.selectedMemberAdmin.canDeactivate) {
                    SecondaryButton(
                        text = "Deactivate seat",
                        onClick = {
                            onEvent(FamilyUiEvent.TriggerVibration)
                            onEvent(FamilyUiEvent.DeactivateMemberRequested)
                        },
                    )
                }
                if (state.selectedMemberAdmin.canRemove) {
                    SecondaryButton(
                        text = "Remove from household",
                        onClick = {
                            onEvent(FamilyUiEvent.TriggerVibration)
                            onEvent(FamilyUiEvent.RemoveMemberRequested)
                        },
                    )
                }
                if (member.availabilityLabel.isNotBlank() && state.canManageSelectedMemberAvailability) {
                    SecondaryButton(
                        text = "Clear availability",
                        onClick = {
                            onEvent(FamilyUiEvent.TriggerVibration)
                            onEvent(FamilyUiEvent.ClearAvailabilityRequested)
                        },
                    )
                }
                if (state.canManageSelectedMemberAvailability) {
                    PrimaryButton(
                        text = "Give ${member.name} a day off",
                        onClick = {
                            onEvent(FamilyUiEvent.TriggerVibration)
                            onDayOff(member.id)
                        },
                    )
                    SecondaryButton(
                        text = "Activate gone mode",
                        onClick = {
                            onEvent(FamilyUiEvent.TriggerVibration)
                            onGoneMode(member.id)
                        },
                    )
                }
            }
        }
        FamilyConfirmationDialog(
            confirmation = state.confirmation,
            onConfirm = {
                onEvent(FamilyUiEvent.TriggerVibration)
                onEvent(FamilyUiEvent.ConfirmPendingAction)
            },
            onTextChange = { onEvent(FamilyUiEvent.ConfirmationTextChanged(it)) },
            onDismiss = { onEvent(FamilyUiEvent.DismissConfirmation) },
        )
    }
}

@Composable
fun InviteMemberScreen(
    state: FamilyUiState,
    onEvent: (FamilyUiEvent) -> Unit,
    onBack: () -> Unit,
) {
    ChorePilotGradientBackground {
        ChorePilotScreen(
            title = "Invite someone safely",
            subtitle = "Pick the role, check the seat impact, then share or rotate one active code at a time.",
            actionLabel = "Back",
            onActionClick = onBack,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FamilyFeedbackCard(
                    feedback = state.feedback,
                    onDismiss = {
                        onEvent(FamilyUiEvent.TriggerVibration)
                        onEvent(FamilyUiEvent.DismissFeedback)
                    },
                )
                RoleChoiceRow(
                    title = "Member",
                    body = "Can complete chores, upload proof, and manage personal reminders.",
                    selected = state.invite.selectedRole == HouseholdRole.Member,
                    icon = Icons.Rounded.PersonAdd,
                    onClick = {
                        onEvent(FamilyUiEvent.TriggerVibration)
                        onEvent(FamilyUiEvent.InviteRoleSelected(HouseholdRole.Member))
                    },
                )
                RoleChoiceRow(
                    title = "Admin",
                    body = "Can edit chores, approve proof, and help keep household settings steady.",
                    selected = state.invite.selectedRole == HouseholdRole.Admin,
                    icon = Icons.Rounded.AdminPanelSettings,
                    onClick = {
                        onEvent(FamilyUiEvent.TriggerVibration)
                        onEvent(FamilyUiEvent.InviteRoleSelected(HouseholdRole.Admin))
                    },
                )
                RoleChoiceRow(
                    title = "Guest",
                    body = "Best for lighter visibility without the full household controls.",
                    selected = state.invite.selectedRole == HouseholdRole.Guest,
                    icon = Icons.Rounded.Shield,
                    onClick = {
                        onEvent(FamilyUiEvent.TriggerVibration)
                        onEvent(FamilyUiEvent.InviteRoleSelected(HouseholdRole.Guest))
                    },
                )
                InviteCodeCard(
                    invite = state.invite,
                    onCopy = {
                        onEvent(FamilyUiEvent.TriggerVibration)
                        onEvent(FamilyUiEvent.InviteCopied)
                    },
                    onShare = {
                        onEvent(FamilyUiEvent.TriggerVibration)
                        onEvent(FamilyUiEvent.InviteShared)
                    },
                    onPrimaryAction = {
                        onEvent(FamilyUiEvent.TriggerVibration)
                        onEvent(FamilyUiEvent.RotateInviteRequested)
                    },
                    onCancelInvite = {
                        onEvent(FamilyUiEvent.TriggerVibration)
                        onEvent(FamilyUiEvent.CancelInviteRequested)
                    },
                )
                HelperCard(
                    title = "Seat impact",
                    body = state.invite.seatImpactText,
                    icon = Icons.Rounded.Groups,
                )
                HelperCard(
                    title = "Current invite role",
                    body = "This invite grants ${state.invite.selectedRole.name.lowercase()} access and ${if (state.invite.hasActiveInvite) "replaces the currently active code when you generate a fresh one." else "will become the only active code once generated."}",
                    icon = Icons.Rounded.AdminPanelSettings,
                )
            }
        }
        FamilyConfirmationDialog(
            confirmation = state.confirmation,
            onConfirm = {
                onEvent(FamilyUiEvent.TriggerVibration)
                onEvent(FamilyUiEvent.ConfirmPendingAction)
            },
            onTextChange = { onEvent(FamilyUiEvent.ConfirmationTextChanged(it)) },
            onDismiss = { onEvent(FamilyUiEvent.DismissConfirmation) },
        )
    }
}

@Composable
fun DayOffScreen(
    state: FamilyUiState,
    onEvent: (FamilyUiEvent) -> Unit,
    onBack: () -> Unit,
) {
    AvailabilityEditorScreen(
        editor = state.dayOffEditor,
        onEvent = onEvent,
        onBack = onBack,
    )
}

@Composable
fun GoneModeScreen(
    state: FamilyUiState,
    onEvent: (FamilyUiEvent) -> Unit,
    onBack: () -> Unit,
) {
    AvailabilityEditorScreen(
        editor = state.goneModeEditor,
        onEvent = onEvent,
        onBack = onBack,
    )
}

@Composable
private fun AvailabilityEditorScreen(
    editor: FamilyAvailabilityEditorUiState,
    onEvent: (FamilyUiEvent) -> Unit,
    onBack: () -> Unit,
) {
    ChorePilotGradientBackground {
        ChorePilotScreen(
            title = editor.title.ifBlank { "Update availability" },
            subtitle = editor.subtitle,
            actionLabel = "Back",
            onActionClick = onBack,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                IllustrationCard(
                    drawable = Res.drawable.m2_vacation_helper,
                    contentDescription = editor.title,
                )
                if (editor.successTitle != null) {
                    HelperCard(
                        title = editor.successTitle,
                        body = editor.successBody,
                        icon = Icons.Rounded.Shield,
                    )
                    SecondaryButton(
                        text = "Keep editing",
                        onClick = {
                            onEvent(FamilyUiEvent.TriggerVibration)
                            onEvent(FamilyUiEvent.AvailabilityFeedbackDismissed)
                        },
                    )
                }
                HelperCard(
                    title = editor.statusLabel.ifBlank { editor.memberName },
                    body = "You're updating availability for ${editor.memberName}.",
                    icon = if (editor.mode == FamilyAvailabilityMode.DayOff) Icons.Rounded.CalendarMonth else Icons.Rounded.TravelExplore,
                )
                DateChoiceSection(
                    title = if (editor.mode == FamilyAvailabilityMode.DayOff) "Choose the day" else "Departure",
                    options = editor.startOptions,
                    onClick = { dateKey ->
                        onEvent(FamilyUiEvent.TriggerVibration)
                        if (editor.mode == FamilyAvailabilityMode.DayOff) {
                            onEvent(FamilyUiEvent.DayOffDateSelected(dateKey))
                        } else {
                            onEvent(FamilyUiEvent.GoneModeStartSelected(dateKey))
                        }
                    },
                )
                if (editor.mode == FamilyAvailabilityMode.GoneMode) {
                    DateChoiceSection(
                        title = "Return",
                        options = editor.endOptions,
                        onClick = { dateKey ->
                            onEvent(FamilyUiEvent.TriggerVibration)
                            onEvent(FamilyUiEvent.GoneModeEndSelected(dateKey))
                        },
                    )
                }
                HelperCard(
                    title = editor.impactTitle,
                    body = editor.impactBody,
                    icon = Icons.Rounded.Shield,
                )
                PrimaryButton(
                    text = editor.confirmLabel,
                    onClick = {
                        onEvent(FamilyUiEvent.TriggerVibration)
                        if (editor.mode == FamilyAvailabilityMode.DayOff) {
                            onEvent(FamilyUiEvent.ConfirmDayOff)
                        } else {
                            onEvent(FamilyUiEvent.ConfirmGoneMode)
                        }
                    },
                )
            }
        }
    }
}

@Composable
fun FamilyEmptyStateScreen(
    onInviteMember: () -> Unit,
) {
    ChorePilotGradientBackground {
        ChorePilotScreen(
            title = "No family members have joined yet.",
            subtitle = "Invite someone in so chores, approvals, and availability can feel shared instead of implied.",
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                IllustrationCard(
                    drawable = Res.drawable.m2_family_empty,
                    contentDescription = "Family empty state",
                )
                PrimaryButton(text = "Invite the first member", onClick = onInviteMember)
            }
        }
    }
}

@Composable
private fun FamilyOverviewCard(
    householdBadge: String,
    activeSeatSummary: String,
    inviteStatusSummary: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(4.dp),
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.74f),
                    shadowElevation = 0.dp,
                ) {}
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(46.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(8.dp),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Groups,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = "Household overview",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = householdBadge,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IllustrationCard(
                drawable = Res.drawable.m2_family_overview_hero,
                contentDescription = "Family overview",
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FamilyOverviewStat(
                    modifier = Modifier.weight(if (inviteStatusSummary.isNotBlank()) 1f else 2f),
                    label = "Seats",
                    value = activeSeatSummary,
                    icon = Icons.Rounded.AdminPanelSettings,
                )
                if (inviteStatusSummary.isNotBlank()) {
                    FamilyOverviewStat(
                        modifier = Modifier.weight(1f),
                        label = "Invite",
                        value = inviteStatusSummary,
                        icon = Icons.Rounded.Link,
                    )
                }
            }
        }
    }
}

@Composable
private fun FamilyOverviewStat(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
) {
    Surface(
        modifier = modifier.height(88.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MemberRow(
    member: FamilyMemberUiModel,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AvatarBadge(seed = member.avatarSeed, tone = member.tone)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    "${member.name} · ${member.role.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    member.activeChoresLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ToneChip(text = member.statusLabel, tone = member.tone)
                    ToneChip(text = member.seatLabel)
                    if (member.availabilityLabel.isNotBlank()) {
                        ToneChip(text = member.availabilityLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(
    title: String,
    subtitle: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun InviteCodeCard(
    invite: FamilyInviteUiState,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onPrimaryAction: () -> Unit,
    onCancelInvite: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Invite status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(invite.statusLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ToneChip(text = invite.selectedRole.name)
                if (invite.lifecycleStatus != null) {
                    ToneChip(text = invite.statusLabel)
                }
            }
            Text(
                text = invite.code.ifBlank { "No active code yet" },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = invite.supportingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (invite.generatedAtLabel.isNotBlank()) {
                Text(
                    text = invite.generatedAtLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (invite.expiryLabel.isNotBlank() || invite.usageLabel.isNotBlank()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (invite.expiryLabel.isNotBlank()) {
                        ToneChip(text = invite.expiryLabel)
                    }
                    if (invite.usageLabel.isNotBlank()) {
                        ToneChip(text = invite.usageLabel)
                    }
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SecondaryActionChip(
                    icon = Icons.Rounded.ContentCopy,
                    text = "Copy",
                    enabled = invite.canCopy,
                    onClick = onCopy,
                )
                SecondaryActionChip(
                    icon = Icons.Rounded.Share,
                    text = "Share",
                    enabled = invite.canShare,
                    onClick = onShare,
                )
                if (invite.canCancel) {
                    SecondaryActionChip(
                        icon = Icons.Rounded.Block,
                        text = "Revoke",
                        enabled = true,
                        onClick = onCancelInvite,
                    )
                }
            }
            PrimaryButton(
                text = invite.actionLabel,
                onClick = onPrimaryAction,
            )
        }
    }
}

@Composable
private fun RoleChoiceRow(
    title: String,
    body: String,
    selected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    StyledChoiceRow(
        title = title,
        description = body,
        selected = selected,
        icon = icon,
        onClick = onClick,
    )
}

@Composable
private fun DateChoiceSection(
    title: String,
    options: List<com.sortoapps.chorepilot.features.family.presentation.viewmodel.FamilyDateOptionUiModel>,
    onClick: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        options.forEach { option ->
            StyledChoiceRow(
                title = option.title,
                description = option.supportingText,
                selected = option.isSelected,
                icon = Icons.Rounded.CalendarMonth,
                onClick = { onClick(option.dateKey) },
            )
        }
    }
}

@Composable
private fun StyledChoiceRow(
    title: String,
    description: String,
    selected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
        else MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = if (selected) 0.14f else 0.1f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = if (selected) 0.18f else 0.12f)),
                shape = RoundedCornerShape(8.dp),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun HelperCard(
    title: String,
    body: String,
    icon: ImageVector,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(8.dp),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun FamilyFeedbackCard(
    feedback: FamilyFeedbackUiState?,
    onDismiss: () -> Unit,
) {
    if (feedback == null) return
    HelperCard(
        title = if (feedback.isError) "Something needs attention" else "Household updated",
        body = feedback.message,
        icon = if (feedback.isError) Icons.Rounded.Block else Icons.Rounded.Shield,
    )
    SecondaryButton(
        text = "Dismiss",
        onClick = onDismiss,
    )
}

@Composable
private fun FamilyConfirmationDialog(
    confirmation: FamilyConfirmationUiState?,
    onConfirm: () -> Unit,
    onTextChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    if (confirmation == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(confirmation.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(confirmation.body)
                if (confirmation.requiresTextConfirmation) {
                    OutlinedTextField(
                        value = confirmation.confirmationText,
                        onValueChange = onTextChange,
                        label = { Text("Type ${confirmation.requiredConfirmationText}") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = confirmation.confirmEnabled,
            ) {
                Text(confirmation.confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun SecondaryActionChip(
    icon: ImageVector,
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
        color = if (enabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (enabled) 0.12f else 0.06f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else 0.46f),
                modifier = Modifier.size(18.dp),
            )
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview(name = "Family Screen", widthDp = 393, heightDp = 852)
@Composable
private fun FamilyScreenOwnerPreview() {
    ChorePilotPreviewTheme {
        CompactPreviewShell(selectedRoute = com.sortoapps.chorepilot.navigation.AppRoute.Family) { modifier ->
            Column(modifier = modifier) {
                FamilyScreen(
                    state = ChorePilotPreviewData.ownerFamilyState(),
                    onEvent = {},
                    onOpenMember = {},
                    onInviteMember = {},
                    onOpenPremium = {},
                )
            }
        }
    }
}

@Preview(name = "Family Screen Member", widthDp = 393, heightDp = 852)
@Composable
private fun FamilyScreenMemberPreview() {
    ChorePilotPreviewTheme {
        CompactPreviewShell(selectedRoute = com.sortoapps.chorepilot.navigation.AppRoute.Family) { modifier ->
            Column(modifier = modifier) {
                FamilyScreen(
                    state = ChorePilotPreviewData.memberFamilyState(),
                    onEvent = {},
                    onOpenMember = {},
                    onInviteMember = {},
                    onOpenPremium = {},
                )
            }
        }
    }
}

@Preview(name = "Member Profile Member", widthDp = 393, heightDp = 852)
@Composable
private fun MemberProfileMemberPreview() {
    ChorePilotPreviewTheme {
        CompactPreviewShell(selectedRoute = com.sortoapps.chorepilot.navigation.AppRoute.Family) { modifier ->
            Column(modifier = modifier) {
                MemberProfileScreen(
                    state = ChorePilotPreviewData.memberFamilyState(),
                    onEvent = {},
                    onDayOff = {},
                    onGoneMode = {},
                    onBack = {},
                )
            }
        }
    }
}
