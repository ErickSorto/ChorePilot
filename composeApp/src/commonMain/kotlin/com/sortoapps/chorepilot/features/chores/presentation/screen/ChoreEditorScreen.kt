package com.sortoapps.chorepilot.features.chores.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bathtub
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Kitchen
import androidx.compose.material.icons.rounded.LocalLaundryService
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Recycling
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.TableRestaurant
import androidx.compose.material.icons.rounded.Toys
import androidx.compose.material.icons.rounded.Weekend
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sortoapps.chorepilot.core.designsystem.ChorePilotGradientBackground
import com.sortoapps.chorepilot.core.designsystem.ChorePilotTheme
import com.sortoapps.chorepilot.core.designsystem.ChorePilotTopBarIconButton
import com.sortoapps.chorepilot.core.designsystem.PrimaryButton
import com.sortoapps.chorepilot.core.preview.PreviewDeviceFrame
import com.sortoapps.chorepilot.features.chores.presentation.components.ChoreLifecycleConfirmationDialog
import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.ChoreLifecycleAction
import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.ChoreEditorUiEvent
import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.ChoreEditorUiState
import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.ChoreEffortLevel
import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.ChoreRepeatFrequency
import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.ChoreWeekday
import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.DailyRepeatMode
import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.RoomColorOptionUiModel
import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.RoomIconOptionUiModel
import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.RoomOptionUiModel
import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.SmartAssignmentMode
import com.sortoapps.chorepilot.features.settings.domain.ThemeMode

private val EditorCardShape = RoundedCornerShape(8.dp)

@Composable
fun ChoreEditorScreen(
    state: ChoreEditorUiState,
    onEvent: (ChoreEditorUiEvent) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onOpenPremium: () -> Unit,
) {
    val canSave = state.title.isNotBlank() && if (state.creatingRoom) state.newRoomName.isNotBlank() else state.selectedRoom.isNotBlank()

    ChorePilotGradientBackground {
        ChoreLifecycleConfirmationDialog(
            confirmation = state.lifecycleConfirmation,
            onConfirm = {
                onEvent(ChoreEditorUiEvent.TriggerVibration)
                onEvent(ChoreEditorUiEvent.ConfirmLifecycleAction)
            },
            onDismiss = {
                onEvent(ChoreEditorUiEvent.TriggerVibration)
                onEvent(ChoreEditorUiEvent.DismissLifecycleConfirmation)
            },
            onTextChange = { onEvent(ChoreEditorUiEvent.LifecycleConfirmationTextChanged(it)) },
        )
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 128.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    EditorTopBar(
                        title = if (state.choreId == null) "Create chore" else "Edit chore",
                        onBack = {
                            onEvent(ChoreEditorUiEvent.TriggerVibration)
                            onBack()
                        },
                    )
                }

                item {
                    EditorHero(
                        title = state.title,
                        repeatSummary = repeatSummary(state),
                        smartAssignmentEnabled = state.autoAssign,
                        smartAssignmentLabel = if (state.autoAssign) assignmentChipLabel(state) else "",
                        showLifecycleStatus = state.choreId != null,
                        isActive = state.isActive,
                        pendingApproval = state.pendingApproval,
                    )
                }

                state.errorMessage?.let { message ->
                    item {
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                item {
                    EditorSection(
                        title = "Task details",
                        subtitle = "What it is and where it lives",
                    ) {
                        TitleField(
                            value = state.title,
                            onValueChange = { onEvent(ChoreEditorUiEvent.TitleChanged(it)) },
                        )
                        AdditionalCommentsField(
                            value = state.additionalComments,
                            onValueChange = { onEvent(ChoreEditorUiEvent.AdditionalCommentsChanged(it)) },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f))
                        RowHeader(label = "Room", value = if (state.creatingRoom) "Create new room" else state.selectedRoom)
                        RoomPicker(
                            rooms = state.roomOptions,
                            selectedRoom = state.selectedRoom,
                            creatingRoom = state.creatingRoom,
                            newRoomName = state.newRoomName,
                            roomIcons = state.roomIconOptions,
                            roomColors = state.roomColorOptions,
                            selectedRoomIcon = state.selectedRoomIcon,
                            selectedRoomColor = state.selectedRoomColor,
                            onRoomSelected = {
                                onEvent(ChoreEditorUiEvent.TriggerVibration)
                                onEvent(ChoreEditorUiEvent.RoomSelected(it))
                            },
                            onCreateRoomChanged = {
                                onEvent(ChoreEditorUiEvent.TriggerVibration)
                                onEvent(ChoreEditorUiEvent.CreatingRoomChanged(it))
                            },
                            onNewRoomNameChanged = { onEvent(ChoreEditorUiEvent.NewRoomNameChanged(it)) },
                            onRoomIconSelected = {
                                onEvent(ChoreEditorUiEvent.TriggerVibration)
                                onEvent(ChoreEditorUiEvent.RoomIconSelected(it))
                            },
                            onRoomColorSelected = {
                                onEvent(ChoreEditorUiEvent.TriggerVibration)
                                onEvent(ChoreEditorUiEvent.RoomColorSelected(it))
                            },
                        )
                    }
                }

                item {
                    EditorSection(
                        title = "Ownership",
                        subtitle = "Keep it fixed or let the app handle fair rotation",
                    ) {
                        ToggleRow(
                            icon = Icons.Rounded.AutoAwesome,
                            label = "Smart assignment",
                            value = if (state.autoAssign) assignmentSummary(state) else "Pick one owner now",
                            checked = state.autoAssign,
                            onCheckedChange = {
                                onEvent(ChoreEditorUiEvent.TriggerVibration)
                                onEvent(ChoreEditorUiEvent.AutoAssignChanged(it))
                            },
                        )
                        if (state.activeAssigneeOptions.isEmpty()) {
                            HelperNote(
                                text = if (state.inactiveAssignmentNames.isEmpty()) {
                                    "No active household members are available yet. Add or reactivate a member before assigning this chore."
                                } else {
                                    "This chore still references saved household members, but no one is currently active."
                                },
                            )
                        }
                        if (state.inactiveAssignmentNames.isNotEmpty()) {
                            LegacyAssignmentNote(
                                names = state.inactiveAssignmentNames,
                            )
                        }
                        if (state.autoAssign) {
                            GuideLabel(
                                label = "Choose how the app should assign this chore",
                                helper = "Pick simple fair rotation or make a day-by-day plan.",
                            )
                            SmartAssignmentModePicker(
                                selected = state.smartAssignmentMode,
                                allowDayPlan = state.repeatsEnabled &&
                                    (state.repeatFrequency == ChoreRepeatFrequency.Daily || state.repeatFrequency == ChoreRepeatFrequency.Weekly),
                                onSelect = {
                                    onEvent(ChoreEditorUiEvent.TriggerVibration)
                                    onEvent(ChoreEditorUiEvent.SmartAssignmentModeChanged(it))
                                },
                            )
                            when (state.smartAssignmentMode) {
                                SmartAssignmentMode.FairRotation -> {
                                    RowHeader(
                                        label = "Rotate between",
                                        value = "${state.assignmentPool.size} people",
                                    )
                                    GuideLabel(
                                        label = "Choose who can do this chore",
                                        helper = "The app will spread the work fairly across these people.",
                                    )
                                    MultiSelectChoiceGroup(
                                        items = state.assigneeOptions,
                                        selected = state.assignmentPool,
                                        onToggle = {
                                            onEvent(ChoreEditorUiEvent.TriggerVibration)
                                            onEvent(ChoreEditorUiEvent.AssignmentPoolToggled(it))
                                        },
                                    )
                                    RowHeader(
                                        label = "Effort level",
                                        value = effortLabel(state.effortLevel),
                                    )
                                    GuideLabel(
                                        label = "Choose how big this chore feels",
                                        helper = "Heavier chores count more when fairness is calculated.",
                                    )
                                    EffortLevelPicker(
                                        selected = state.effortLevel,
                                        onSelect = {
                                            onEvent(ChoreEditorUiEvent.TriggerVibration)
                                            onEvent(ChoreEditorUiEvent.EffortLevelChanged(it))
                                        },
                                    )
                                    HelperNote(
                                        text = "ChorePilot balances weekly effort, not just task count. It also remembers the last eligible turn, so two-person chores can alternate cleanly week to week.",
                                    )
                                }

                                SmartAssignmentMode.DayPlan -> {
                                    RowHeader(
                                        label = "Rotation plan",
                                        value = "Pick who owns each day",
                                    )
                                    HelperNote(
                                        text = "Use fixed names for structure. Set any day to Smart fair when you want the app to rotate that one fairly from the selected people.",
                                    )
                                    MultiSelectChoiceGroup(
                                        items = state.assigneeOptions,
                                        selected = state.assignmentPool,
                                        onToggle = {
                                            onEvent(ChoreEditorUiEvent.TriggerVibration)
                                            onEvent(ChoreEditorUiEvent.AssignmentPoolToggled(it))
                                        },
                                    )
                                    GuideLabel(
                                        label = "Choose who can appear in the day plan",
                                        helper = "You can still leave a day on Smart fair for flexibility.",
                                    )
                                    DayPlanEditor(
                                        selectedDays = state.selectedWeekdays,
                                        assignmentPool = state.assignmentPool,
                                        assignments = state.dayAssignments,
                                        onAssign = { day, assignee ->
                                            onEvent(ChoreEditorUiEvent.TriggerVibration)
                                            onEvent(ChoreEditorUiEvent.DayAssignmentChanged(day, assignee))
                                        },
                                    )
                                }
                            }
                        } else {
                            GuideLabel(
                                label = "Choose who should do this chore",
                                helper = "Tap one person to make them the owner.",
                            )
                            FlowChoiceGroup(
                                items = state.assigneeOptions,
                                selected = state.assignee,
                                onSelect = {
                                    onEvent(ChoreEditorUiEvent.TriggerVibration)
                                    onEvent(ChoreEditorUiEvent.AssigneeChanged(it))
                                },
                            )
                        }
                    }
                }

                item {
                    EditorSection(
                        title = "Repeats",
                        subtitle = "Keep it one-time or let it come back automatically",
                    ) {
                        ToggleRow(
                            icon = Icons.Rounded.CalendarMonth,
                            label = "Repeats",
                            value = if (state.repeatsEnabled) repeatSummary(state) else "Scheduled once for ${state.selectedOneTimeDate}",
                            checked = state.repeatsEnabled,
                            onCheckedChange = {
                                onEvent(ChoreEditorUiEvent.TriggerVibration)
                                onEvent(ChoreEditorUiEvent.RepeatsEnabledChanged(it))
                            },
                        )
                        if (state.repeatsEnabled) {
                            RowHeader(label = "Starts on", value = state.repeatStartDate)
                            GuideLabel(
                                label = "Choose when the repeating chore starts",
                                helper = "It will not appear before this day.",
                            )
                            FlowChoiceGroup(
                                items = state.oneTimeDateOptions,
                                selected = state.repeatStartDate,
                                onSelect = {
                                    onEvent(ChoreEditorUiEvent.TriggerVibration)
                                    onEvent(ChoreEditorUiEvent.RepeatStartDateChanged(it))
                                },
                            )
                            RowHeader(label = "Frequency", value = state.repeatFrequency.name)
                            GuideLabel(
                                label = "Choose how often it repeats",
                                helper = "Start simple. You can always make it more specific.",
                            )
                            FrequencyPicker(
                                selected = state.repeatFrequency,
                                onSelect = {
                                    onEvent(ChoreEditorUiEvent.TriggerVibration)
                                    onEvent(ChoreEditorUiEvent.RepeatFrequencyChanged(it))
                                },
                            )
                            when (state.repeatFrequency) {
                                ChoreRepeatFrequency.Daily -> {
                                    DailyModePicker(
                                        selected = state.dailyRepeatMode,
                                        onSelect = {
                                            onEvent(ChoreEditorUiEvent.TriggerVibration)
                                            onEvent(ChoreEditorUiEvent.DailyRepeatModeChanged(it))
                                        },
                                    )
                                    if (state.dailyRepeatMode == DailyRepeatMode.SpecificDays) {
                                        WeekdayPicker(
                                            selectedDays = state.selectedWeekdays,
                                            onToggle = {
                                                onEvent(ChoreEditorUiEvent.TriggerVibration)
                                                onEvent(ChoreEditorUiEvent.WeekdayToggled(it))
                                            },
                                        )
                                    }
                                }

                                ChoreRepeatFrequency.Weekly -> {
                                    RowHeader(label = "On these days", value = "${state.selectedWeekdays.size} selected")
                                    WeekdayPicker(
                                        selectedDays = state.selectedWeekdays,
                                        onToggle = {
                                            onEvent(ChoreEditorUiEvent.TriggerVibration)
                                            onEvent(ChoreEditorUiEvent.WeekdayToggled(it))
                                        },
                                    )
                                }

                                ChoreRepeatFrequency.Monthly -> {
                                    RowHeader(label = "Repeats on", value = state.monthlyPattern)
                                    FlowChoiceGroup(
                                        items = listOf("1st", "15th", "Last Friday"),
                                        selected = state.monthlyPattern,
                                        onSelect = {
                                            onEvent(ChoreEditorUiEvent.TriggerVibration)
                                            onEvent(ChoreEditorUiEvent.MonthlyPatternChanged(it))
                                        },
                                    )
                                }

                                ChoreRepeatFrequency.Custom -> {
                                    RowHeader(label = "Interval", value = state.customPattern)
                                    FlowChoiceGroup(
                                        items = listOf("Every 2 weeks", "Every 3 weeks", "Every month"),
                                        selected = state.customPattern,
                                        onSelect = {
                                            onEvent(ChoreEditorUiEvent.TriggerVibration)
                                            onEvent(ChoreEditorUiEvent.CustomPatternChanged(it))
                                        },
                                    )
                                }
                            }
                        } else {
                            RowHeader(label = "Do once on", value = state.selectedOneTimeDate)
                            GuideLabel(
                                label = "Choose the day",
                                helper = "This chore will happen one time only.",
                            )
                            FlowChoiceGroup(
                                items = state.oneTimeDateOptions,
                                selected = state.selectedOneTimeDate,
                                onSelect = {
                                    onEvent(ChoreEditorUiEvent.TriggerVibration)
                                    onEvent(ChoreEditorUiEvent.OneTimeDateChanged(it))
                                },
                            )
                        }
                    }
                }

                item {
                    EditorSection(
                        title = "Checks",
                        subtitle = "Only add the controls that help the household",
                    ) {
                        ToggleRow(
                            icon = Icons.Rounded.Shield,
                            label = "Proof required",
                            value = if (state.proofRequired) "Require an image upload before approval" else "No upload needed",
                            checked = state.proofRequired,
                            onCheckedChange = {
                                onEvent(ChoreEditorUiEvent.TriggerVibration)
                                if (it && !state.hasChorePilotPro) {
                                    onEvent(ChoreEditorUiEvent.ProofRequiredUpgradeRequested(true))
                                    onOpenPremium()
                                } else {
                                    onEvent(ChoreEditorUiEvent.ProofRequiredChanged(it))
                                }
                            },
                        )
                        ToggleRow(
                            icon = Icons.Rounded.Schedule,
                            label = "Estimate duration",
                            value = if (state.durationEnabled) "${state.durationMinutes} min expected" else "No estimate",
                            checked = state.durationEnabled,
                            onCheckedChange = {
                                onEvent(ChoreEditorUiEvent.TriggerVibration)
                                onEvent(ChoreEditorUiEvent.DurationEnabledChanged(it))
                            },
                        )
                        if (state.durationEnabled) {
                            FlowChoiceGroup(
                                items = listOf("5", "10", "15", "20", "30", "45", "60"),
                                selected = state.durationMinutes.toString(),
                                suffix = " min",
                                onSelect = {
                                    onEvent(ChoreEditorUiEvent.TriggerVibration)
                                    onEvent(ChoreEditorUiEvent.DurationMinutesChanged(it.toInt()))
                                },
                            )
                        }
                    }
                }

                if (state.choreId != null && state.canManageLifecycle) {
                    item {
                        LifecycleSection(
                            state = state,
                            onEvent = onEvent,
                        )
                    }
                }
            }

            EditorFooter(
                modifier = Modifier.align(Alignment.BottomCenter),
                canSave = canSave && !state.isApplyingLifecycleAction,
                onCancel = {
                    onEvent(ChoreEditorUiEvent.TriggerVibration)
                    onBack()
                },
                onSave = {
                    onEvent(ChoreEditorUiEvent.TriggerVibration)
                    onSave()
                },
                isSaving = state.isSaving,
            )
        }
    }
}

@Composable
private fun EditorTopBar(
    title: String,
    onBack: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChorePilotTopBarIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                onClick = onBack,
            )
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = EditorCardShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                Text(
                    text = "Cancel",
                    modifier = Modifier
                        .clickable(onClick = onBack)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Build a clear routine once, then let the household follow it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EditorHero(
    title: String,
    repeatSummary: String,
    smartAssignmentEnabled: Boolean,
    smartAssignmentLabel: String,
    showLifecycleStatus: Boolean,
    isActive: Boolean,
    pendingApproval: Boolean,
) {
    EditorCard(borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (smartAssignmentEnabled) {
                    StatusPill(
                        text = smartAssignmentLabel,
                        icon = Icons.Rounded.AutoAwesome,
                        accent = MaterialTheme.colorScheme.primary,
                    )
                }

                if (showLifecycleStatus) {
                    LifecycleStatusChip(
                        isActive = isActive,
                        pendingApproval = pendingApproval,
                    )
                }
            }

            Text(
                text = if (title.isBlank()) "New household chore" else title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = repeatSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LifecycleStatusChip(
    isActive: Boolean,
    pendingApproval: Boolean,
) {
    val containerColor = when {
        pendingApproval -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f)
        isActive -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.28f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)
    }
    val contentColor = when {
        pendingApproval -> MaterialTheme.colorScheme.primary
        isActive -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val label = when {
        pendingApproval -> "Pending review"
        isActive -> "Active"
        else -> "Inactive"
    }
    StatusPill(
        text = label,
        icon = when {
            pendingApproval -> Icons.Rounded.AutoAwesome
            isActive -> Icons.Rounded.Shield
            else -> Icons.Rounded.DeleteOutline
        },
        accent = contentColor,
        containerColor = containerColor,
    )
}

@Composable
private fun EditorSection(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    EditorCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
        }
    }
}

@Composable
private fun LifecycleSection(
    state: ChoreEditorUiState,
    onEvent: (ChoreEditorUiEvent) -> Unit,
) {
    val supportingText = when {
        state.pendingApproval ->
            "This discovery chore is still waiting for approval. Delete it here if it should not move forward."

        state.isActive ->
            "Deactivate to stop future schedule appearances while keeping submitted proof, completion history, and discovery context attached."

        else ->
            "This chore stays out of the schedule until you reactivate it. Existing proof and completion history stay attached."
    }
    EditorSection(
        title = "Lifecycle",
        subtitle = supportingText,
    ) {
        state.lifecycleFeedbackMessage?.let { feedback ->
            HelperNote(text = feedback)
        }
        if (!state.pendingApproval) {
            LifecycleActionButton(
                icon = if (state.isActive) Icons.Rounded.Shield else Icons.Rounded.AutoAwesome,
                title = if (state.isActive) "Deactivate chore" else "Reactivate chore",
                body = if (state.isActive) {
                    "Hide it from active chore lists and future schedules without removing proof or history."
                } else {
                    "Return it to the active schedule and keep all existing proof and history."
                },
                destructive = state.isActive,
                enabled = !state.isApplyingLifecycleAction,
                onClick = {
                    onEvent(ChoreEditorUiEvent.TriggerVibration)
                    onEvent(
                        ChoreEditorUiEvent.LifecycleActionRequested(
                            if (state.isActive) ChoreLifecycleAction.Deactivate else ChoreLifecycleAction.Reactivate,
                        ),
                    )
                },
            )
        }
        LifecycleActionButton(
            icon = Icons.Rounded.DeleteOutline,
            title = "Delete permanently",
            body = "Remove this chore, its proof photos, completion history, and saved discovery context. Type delete to confirm.",
            destructive = true,
            enabled = !state.isApplyingLifecycleAction,
            onClick = {
                onEvent(ChoreEditorUiEvent.TriggerVibration)
                onEvent(ChoreEditorUiEvent.LifecycleActionRequested(ChoreLifecycleAction.Delete))
            },
        )
    }
}

@Composable
private fun LifecycleActionButton(
    icon: ImageVector,
    title: String,
    body: String,
    destructive: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val accent = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    EditorCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                interactionSource = MutableInteractionSource(),
                indication = null,
                onClick = onClick,
            ),
        backgroundColor = accent.copy(alpha = if (enabled) 0.08f else 0.04f),
        borderColor = accent.copy(alpha = if (enabled) 0.18f else 0.08f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                color = accent.copy(alpha = 0.14f),
                shape = EditorCardShape,
                shadowElevation = 0.dp,
            ) {
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent.copy(alpha = if (enabled) 1f else 0.56f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TitleField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GuideLabel(
            label = "Choose a name",
            helper = "Use simple words like Take out trash or Empty dishwasher.",
        )
        TextInputField(
            value = value,
            onValueChange = onValueChange,
            placeholder = "Empty dishwasher",
            singleLine = true,
        )
    }
}

@Composable
private fun AdditionalCommentsField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GuideLabel(
            label = "Additional comments",
            helper = "Add extra notes like where supplies are or what to watch for.",
        )
        TextInputField(
            value = value,
            onValueChange = onValueChange,
            placeholder = "Example: Use the small trash bags under the sink.",
            singleLine = false,
            minHeight = 112.dp,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

@Composable
private fun RowHeader(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun LegacyAssignmentNote(
    names: Set<String>,
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.34f),
        shape = MaterialTheme.shapes.medium,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Saved assignment still includes inactive members",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "${names.joinToString()} still appear on this chore so you can review and reassign intentionally.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TextInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean,
    minHeight: androidx.compose.ui.unit.Dp = 56.dp,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleSmall.copy(
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold,
    ),
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = EditorCardShape,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.TopStart,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = singleLine,
                textStyle = textStyle,
                decorationBox = { innerTextField ->
                    if (value.isBlank()) {
                        Text(
                            text = placeholder,
                            style = textStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                        )
                    }
                    innerTextField()
                },
            )
        }
    }
}

@Composable
private fun RoomPicker(
    rooms: List<RoomOptionUiModel>,
    selectedRoom: String,
    creatingRoom: Boolean,
    newRoomName: String,
    roomIcons: List<RoomIconOptionUiModel>,
    roomColors: List<RoomColorOptionUiModel>,
    selectedRoomIcon: String,
    selectedRoomColor: String,
    onRoomSelected: (String) -> Unit,
    onCreateRoomChanged: (Boolean) -> Unit,
    onNewRoomNameChanged: (String) -> Unit,
    onRoomIconSelected: (String) -> Unit,
    onRoomColorSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GuideLabel(
            label = "Choose a room",
            helper = "Pick an existing room or make a new one right now.",
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            rooms.forEach { room ->
                RoomChip(
                    room = room,
                    selected = !creatingRoom && room.name == selectedRoom,
                    onClick = { onRoomSelected(room.name) },
                )
            }
            PillTextButton(
                text = if (creatingRoom) "Use existing room" else "New room",
                selected = creatingRoom,
                onClick = { onCreateRoomChanged(!creatingRoom) },
            )
        }

        if (creatingRoom) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
                shape = EditorCardShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                shadowElevation = 0.dp,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    GuideLabel(
                        label = "Choose room name",
                        helper = "Use a name everyone in the house understands.",
                    )
                    TextInputField(
                        value = newRoomName,
                        onValueChange = onNewRoomNameChanged,
                        placeholder = "Laundry room",
                        singleLine = true,
                    )
                    RoomPreviewCard(
                        roomName = newRoomName,
                        iconKey = selectedRoomIcon,
                        colorKey = selectedRoomColor,
                    )
                    GuideLabel(
                        label = "Choose an icon",
                        helper = "Pick the picture that feels closest to the room or chore area.",
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        roomIcons.forEach { icon ->
                            IconChip(
                                option = icon,
                                selected = icon.key == selectedRoomIcon,
                                selectedColor = selectedRoomColor,
                                onClick = { onRoomIconSelected(icon.key) },
                            )
                        }
                    }
                    GuideLabel(
                        label = "Choose a color",
                        helper = "Color helps people spot the room faster.",
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        roomColors.forEach { color ->
                            ColorChip(
                                option = color,
                                selected = color.key == selectedRoomColor,
                                onClick = { onRoomColorSelected(color.key) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomChip(
    room: RoomOptionUiModel,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accent = roomColor(room.colorKey)
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = if (selected) accent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.16f),
        shape = CircleShape,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = roomIcon(room.iconKey),
                contentDescription = null,
                tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = room.name,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun IconChip(
    option: RoomIconOptionUiModel,
    selected: Boolean,
    selectedColor: String,
    onClick: () -> Unit,
) {
    val accent = roomColor(selectedColor)
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = if (selected) accent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.16f),
        shape = EditorCardShape,
        border = BorderStroke(1.dp, if (selected) accent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
        shadowElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(EditorCardShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = roomIcon(option.key),
                contentDescription = option.label,
                tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun ColorChip(
    option: RoomColorOptionUiModel,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color = roomColor(option.key)
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = Color.Transparent,
        shape = CircleShape,
        shadowElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                    width = if (selected) 2.dp else 0.dp,
                    color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                )
            }
        }
    }
}

@Composable
private fun RoomPreviewCard(
    roomName: String,
    iconKey: String,
    colorKey: String,
) {
    val accent = roomColor(colorKey)
    Surface(
        color = accent.copy(alpha = 0.12f),
        shape = EditorCardShape,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.14f)),
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = accent.copy(alpha = 0.18f),
                shape = CircleShape,
                shadowElevation = 0.dp,
            ) {
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = roomIcon(iconKey),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Preview",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = roomName.ifBlank { "New room" },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun FrequencyPicker(
    selected: ChoreRepeatFrequency,
    onSelect: (ChoreRepeatFrequency) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ChoreRepeatFrequency.entries.forEach { frequency ->
            PillTextButton(
                text = frequency.name,
                selected = frequency == selected,
                onClick = { onSelect(frequency) },
            )
        }
    }
}

@Composable
private fun DailyModePicker(
    selected: DailyRepeatMode,
    onSelect: (DailyRepeatMode) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RowHeader(label = "Daily rule", value = when (selected) {
            DailyRepeatMode.EveryDay -> "Every day"
            DailyRepeatMode.Weekdays -> "Weekdays"
            DailyRepeatMode.SpecificDays -> "Specific days"
        })
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DailyRepeatMode.entries.forEach { mode ->
                PillTextButton(
                    text = when (mode) {
                        DailyRepeatMode.EveryDay -> "Every day"
                        DailyRepeatMode.Weekdays -> "Weekdays"
                        DailyRepeatMode.SpecificDays -> "Specific days"
                    },
                    selected = selected == mode,
                    onClick = { onSelect(mode) },
                )
            }
        }
    }
}

@Composable
private fun WeekdayPicker(
    selectedDays: Set<ChoreWeekday>,
    onToggle: (ChoreWeekday) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ChoreWeekday.entries.forEach { weekday ->
            Surface(
                modifier = Modifier.clickable(onClick = { onToggle(weekday) }),
                color = if (weekday in selectedDays) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
                shape = CircleShape,
                shadowElevation = 0.dp,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = weekday.shortLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (weekday in selectedDays) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun FlowChoiceGroup(
    items: List<String>,
    selected: String,
    suffix: String = "",
    onSelect: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { item ->
            PillTextButton(
                text = if (suffix.isBlank()) item else "$item$suffix",
                selected = item == selected,
                onClick = { onSelect(item) },
            )
        }
    }
}

@Composable
private fun MultiSelectChoiceGroup(
    items: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { item ->
            PillTextButton(
                text = item,
                selected = item in selected,
                onClick = { onToggle(item) },
            )
        }
    }
}

@Composable
private fun SmartAssignmentModePicker(
    selected: SmartAssignmentMode,
    allowDayPlan: Boolean,
    onSelect: (SmartAssignmentMode) -> Unit,
) {
    val options = buildList {
        add(SmartAssignmentMode.FairRotation)
        if (allowDayPlan) add(SmartAssignmentMode.DayPlan)
    }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { mode ->
            PillTextButton(
                text = when (mode) {
                    SmartAssignmentMode.FairRotation -> "Fair rotation"
                    SmartAssignmentMode.DayPlan -> "Day-by-day plan"
                },
                selected = selected == mode,
                onClick = { onSelect(mode) },
            )
        }
    }
}

@Composable
private fun EffortLevelPicker(
    selected: ChoreEffortLevel,
    onSelect: (ChoreEffortLevel) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ChoreEffortLevel.entries.forEach { level ->
            PillTextButton(
                text = effortLabel(level),
                selected = selected == level,
                onClick = { onSelect(level) },
            )
        }
    }
}

@Composable
private fun DayPlanEditor(
    selectedDays: Set<ChoreWeekday>,
    assignmentPool: Set<String>,
    assignments: Map<ChoreWeekday, String>,
    onAssign: (ChoreWeekday, String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        selectedDays
            .sortedBy { it.ordinal }
            .forEach { weekday ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.16f),
                    shape = EditorCardShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                    shadowElevation = 0.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = weekday.fullLabel,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = assignments[weekday] ?: "Smart fair",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            PillTextButton(
                                text = "Smart fair",
                                selected = (assignments[weekday] ?: "Smart fair") == "Smart fair",
                                onClick = { onAssign(weekday, "Smart fair") },
                            )
                            assignmentPool.sorted().forEach { person ->
                                PillTextButton(
                                    text = person,
                                    selected = assignments[weekday] == person,
                                    onClick = { onAssign(weekday, person) },
                                )
                            }
                        }
                    }
                }
            }
    }
}

@Composable
private fun GuideLabel(
    label: String,
    helper: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = helper,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HelperNote(
    text: String,
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.28f),
        shape = EditorCardShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)),
        shadowElevation = 0.dp,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun PillTextButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.16f),
        shape = CircleShape,
        shadowElevation = 0.dp,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    label: String,
    value: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
        shape = EditorCardShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = EditorCardShape,
                    shadowElevation = 0.dp,
                ) {
                    Box(
                        modifier = Modifier.size(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

@Composable
private fun EditorFooter(
    modifier: Modifier = Modifier,
    canSave: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    isSaving: Boolean,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Cancel",
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = MutableInteractionSource(),
                        indication = null,
                        onClick = onCancel,
                    ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.weight(1f))
            PrimaryButton(
                text = if (isSaving) "Saving..." else "Save chore",
                modifier = Modifier.weight(2f),
                enabled = canSave && !isSaving,
                onClick = onSave,
            )
        }
    }
}

private fun repeatSummary(
    state: ChoreEditorUiState,
): String = if (!state.repeatsEnabled) {
    "Scheduled once for ${state.selectedOneTimeDate}."
} else {
    val cadence = when (state.repeatFrequency) {
        ChoreRepeatFrequency.Daily -> when (state.dailyRepeatMode) {
            DailyRepeatMode.EveryDay -> "Repeats daily, every day"
            DailyRepeatMode.Weekdays -> "Repeats daily on weekdays"
            DailyRepeatMode.SpecificDays -> "Repeats daily on ${weekdaySummary(state.selectedWeekdays)}"
        }

        ChoreRepeatFrequency.Weekly -> "Repeats weekly on ${weekdaySummary(state.selectedWeekdays)}"
        ChoreRepeatFrequency.Monthly -> "Repeats monthly on the ${state.monthlyPattern.lowercase()}"
        ChoreRepeatFrequency.Custom -> "Repeats ${state.customPattern.lowercase()}"
    }
    "$cadence, starting ${state.repeatStartDate.lowercase()}."
}

private fun assignmentSummary(
    state: ChoreEditorUiState,
): String = when (state.smartAssignmentMode) {
    SmartAssignmentMode.FairRotation -> "Balances ${state.assignmentPool.size} people by ${effortLabel(state.effortLevel).lowercase()} effort"
    SmartAssignmentMode.DayPlan -> "Lets you lock owners by day and leave flexible days on Smart fair"
}

private fun assignmentChipLabel(
    state: ChoreEditorUiState,
): String = when (state.smartAssignmentMode) {
    SmartAssignmentMode.FairRotation -> "Fair rotation on"
    SmartAssignmentMode.DayPlan -> "Day plan active"
}

private fun effortLabel(
    level: ChoreEffortLevel,
): String = when (level) {
    ChoreEffortLevel.Light -> "Light"
    ChoreEffortLevel.Standard -> "Standard"
    ChoreEffortLevel.Heavy -> "Heavy"
}

private fun weekdaySummary(
    weekdays: Set<ChoreWeekday>,
): String = weekdays
    .sortedBy { it.ordinal }
    .joinToString(", ") { it.fullLabel }
    .ifBlank { "Monday" }

private fun roomIcon(
    key: String,
): ImageVector = when (key) {
    "kitchen" -> Icons.Rounded.Kitchen
    "living" -> Icons.Rounded.Weekend
    "bathroom" -> Icons.Rounded.Bathtub
    "entry" -> Icons.Rounded.Home
    "outside" -> Icons.Rounded.CalendarMonth
    "bedroom" -> Icons.Rounded.Groups
    "laundry" -> Icons.Rounded.LocalLaundryService
    "pets" -> Icons.Rounded.Pets
    "trash" -> Icons.Rounded.DeleteOutline
    "clean" -> Icons.Rounded.CleaningServices
    "dining" -> Icons.Rounded.TableRestaurant
    "play" -> Icons.Rounded.Toys
    "recycle" -> Icons.Rounded.Recycling
    else -> Icons.Rounded.Checklist
}

private fun roomColor(
    key: String,
): Color = when (key) {
    "violet" -> Color(0xFF7C6BF7)
    "teal" -> Color(0xFF18B7A0)
    "amber" -> Color(0xFFFFB547)
    "rose" -> Color(0xFFFF7F8E)
    "sky" -> Color(0xFF5AA7FF)
    "sage" -> Color(0xFF6ABF7B)
    else -> Color(0xFF7C6BF7)
}

@Composable
private fun ChoreEditorPreview(
    state: ChoreEditorUiState,
    themeMode: ThemeMode = ThemeMode.Light,
) {
    ChorePilotTheme(themeMode = themeMode) {
        PreviewDeviceFrame { modifier ->
            Box(modifier = modifier.fillMaxSize()) {
                ChoreEditorScreen(
                    state = state,
                    onEvent = {},
                    onBack = {},
                    onSave = {},
                    onOpenPremium = {},
                )
            }
        }
    }
}

@Preview(name = "Chore Editor Active", widthDp = 393, heightDp = 852)
@Composable
private fun ChoreEditorActivePreview() {
    ChoreEditorPreview(state = previewChoreEditorState(isActive = true))
}

@Preview(name = "Chore Editor Active Dark", widthDp = 393, heightDp = 852)
@Composable
private fun ChoreEditorActiveDarkPreview() {
    ChoreEditorPreview(
        state = previewChoreEditorState(isActive = true),
        themeMode = ThemeMode.Dark,
    )
}

@Preview(name = "Chore Editor Inactive", widthDp = 393, heightDp = 852)
@Composable
private fun ChoreEditorInactivePreview() {
    ChoreEditorPreview(state = previewChoreEditorState(isActive = false))
}

@Preview(name = "Chore Editor Inactive Dark", widthDp = 393, heightDp = 852)
@Composable
private fun ChoreEditorInactiveDarkPreview() {
    ChoreEditorPreview(
        state = previewChoreEditorState(isActive = false),
        themeMode = ThemeMode.Dark,
    )
}

@Composable
private fun EditorCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        color = backgroundColor,
        shape = EditorCardShape,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        content()
    }
}

@Composable
private fun StatusPill(
    text: String,
    icon: ImageVector,
    accent: Color,
    containerColor: Color = accent.copy(alpha = 0.12f),
) {
    Surface(
        color = containerColor,
        shape = EditorCardShape,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.12f)),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun previewChoreEditorState(
    isActive: Boolean,
): ChoreEditorUiState = ChoreEditorUiState(
    choreId = "chore-kitchen-reset",
    canManageLifecycle = true,
    isActive = isActive,
    pendingApproval = false,
    title = "Kitchen reset",
    additionalComments = "Clear counters, start the dishwasher, and wipe the sink before the evening wind-down.",
    selectedRoom = "Kitchen",
    assigneeOptions = listOf("Erick", "Emma", "Lucas"),
    activeAssigneeOptions = setOf("Erick", "Emma", "Lucas"),
    assignee = "Emma",
    autoAssign = true,
    smartAssignmentMode = SmartAssignmentMode.DayPlan,
    assignmentPool = setOf("Erick", "Emma", "Lucas"),
    dayAssignments = mapOf(
        ChoreWeekday.Monday to "Emma",
        ChoreWeekday.Wednesday to "Erick",
        ChoreWeekday.Friday to "Lucas",
    ),
    effortLevel = ChoreEffortLevel.Standard,
    repeatsEnabled = true,
    repeatStartDate = "Today",
    repeatFrequency = ChoreRepeatFrequency.Weekly,
    selectedWeekdays = setOf(
        ChoreWeekday.Monday,
        ChoreWeekday.Wednesday,
        ChoreWeekday.Friday,
    ),
    proofRequired = true,
    durationMinutes = 20,
    hasChorePilotPro = true,
    lifecycleFeedbackMessage = if (isActive) {
        "This chore is active. Deactivating it will pause new schedule appearances but keep proof and completion history."
    } else {
        "This chore is inactive. Reactivating it will return it to the schedule with proof and completion history intact."
    },
)
