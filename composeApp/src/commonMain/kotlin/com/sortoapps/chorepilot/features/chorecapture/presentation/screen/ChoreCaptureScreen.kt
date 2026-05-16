package com.sortoapps.chorepilot.features.chorecapture.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import chorepilot.composeapp.generated.resources.Res
import chorepilot.composeapp.generated.resources.found_something_observer
import chorepilot.composeapp.generated.resources.proof_capture_mascot_camera
import com.sortoapps.chorepilot.core.designsystem.ChorePilotTheme
import com.sortoapps.chorepilot.core.designsystem.ChorePilotGradientBackground
import com.sortoapps.chorepilot.core.designsystem.ChorePilotTopAppBar
import com.sortoapps.chorepilot.core.designsystem.ChorePilotTopBarIconButton
import com.sortoapps.chorepilot.core.preview.PreviewDeviceFrame
import com.sortoapps.chorepilot.features.chorecapture.presentation.platform.ProofPreviewImage
import com.sortoapps.chorepilot.features.chorecapture.presentation.platform.rememberProofCameraHandler
import com.sortoapps.chorepilot.features.chorecapture.presentation.viewmodel.ChoreCaptureUiEvent
import com.sortoapps.chorepilot.features.chorecapture.presentation.viewmodel.ChoreCaptureUiState
import com.sortoapps.chorepilot.features.chorecapture.presentation.viewmodel.DiscoveryCapturePhase
import com.sortoapps.chorepilot.features.chorecapture.presentation.viewmodel.DiscoveryDraftUiModel
import com.sortoapps.chorepilot.features.chores.domain.ChoreDailyModeValue
import com.sortoapps.chorepilot.features.chores.domain.ChoreEffortValue
import com.sortoapps.chorepilot.features.chores.domain.ChoreRepeatFrequencyValue
import com.sortoapps.chorepilot.features.settings.domain.ThemeMode
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource

@Composable
fun ChoreCaptureScreen(
    state: ChoreCaptureUiState,
    onEvent: (ChoreCaptureUiEvent) -> Unit,
    onBack: () -> Unit,
    onOpenPremium: () -> Unit,
) {
    val cameraHandler = rememberProofCameraHandler(
        onSuccess = { payload ->
            onEvent(ChoreCaptureUiEvent.ProofCaptured(payload))
        },
        onFailure = { message ->
            onEvent(ChoreCaptureUiEvent.CameraFailed(message))
        },
    )

    LaunchedEffect(state.shouldOpenPremium) {
        if (state.shouldOpenPremium) {
            onOpenPremium()
            onEvent(ChoreCaptureUiEvent.PremiumNavigationHandled)
        }
    }

    LaunchedEffect(state.shouldReturnToList) {
        if (state.shouldReturnToList) {
            delay(1000L)
            onEvent(ChoreCaptureUiEvent.DiscoveryReturnHandled)
            onBack()
        }
    }

    ChorePilotGradientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                ChorePilotTopAppBar(
                    sectionTitle = state.title,
                    navigationIcon = {
                        ChorePilotTopBarIconButton(
                            icon = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            onClick = {
                                onEvent(ChoreCaptureUiEvent.TriggerVibration)
                                onBack()
                            },
                        )
                    },
                )
            },
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = state.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = state.subtitle,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                item {
                    ProofHeroPanel(
                        discoveryMode = state.isDiscoveryMode,
                        title = heroTitle(state),
                        body = heroBody(state),
                    )
                }

                if (state.isLoading) {
                    item {
                        ProofPanel {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "Loading this capture flow…",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                if (!state.isDiscoveryMode && state.choreTitle.isNotBlank()) {
                    item {
                        ProofPanel {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = state.choreTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = proofSummaryLine(state),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }

                state.previewUri?.let { previewUri ->
                    item {
                        ProofPanel {
                            ProofPreview(previewUri = previewUri)
                        }
                    }
                }

                state.errorMessage?.let { message ->
                    item {
                        ProofPanel(borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.22f)) {
                            ProofSectionLabel(
                                title = "Something went wrong",
                                body = message,
                                accent = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }

                state.unavailableMessage?.let { message ->
                    item {
                        ProofPanel(borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.18f)) {
                            ProofSectionLabel(
                                title = "This chore isn't ready for proof",
                                body = message,
                                accent = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }

                if (state.isDiscoveryMode) {
                    when (state.discoveryPhase) {
                        DiscoveryCapturePhase.Capture -> {
                            if (state.hasCapturedPhoto && !state.hasChorePilotPro) {
                                item {
                                    DiscoveryStatusCard(
                                        title = "Discovery mode is part of ChorePilot Pro",
                                        body = "Unlock AI room analysis to turn one photo into editable chore drafts.",
                                    )
                                }
                            }
                        }

                        DiscoveryCapturePhase.Analyzing -> {
                            item {
                                DiscoveryStatusCard(
                                    title = "Analyzing your room",
                                    body = "We’re spotting chores, estimating effort, and suggesting proof needs for each draft.",
                                )
                            }
                        }

                        DiscoveryCapturePhase.ReviewDrafts,
                        DiscoveryCapturePhase.Saving,
                        DiscoveryCapturePhase.Saved,
                        DiscoveryCapturePhase.Error,
                        -> {
                            if (state.discoverySummary.isNotBlank()) {
                                item {
                                    DiscoveryStatusCard(
                                        title = "Review the draft list",
                                        body = state.discoverySummary,
                                    )
                                }
                            }
                            items(
                                items = state.discoveryDrafts,
                                key = { it.id },
                            ) { draft ->
                                DiscoveryDraftCard(
                                    draft = draft,
                                    enabled = state.discoveryPhase != DiscoveryCapturePhase.Saving,
                                    onTitleChanged = { onEvent(ChoreCaptureUiEvent.DiscoveryDraftTitleChanged(draft.id, it)) },
                                    onRoomChanged = { onEvent(ChoreCaptureUiEvent.DiscoveryDraftRoomChanged(draft.id, it)) },
                                    onEffortChanged = { onEvent(ChoreCaptureUiEvent.DiscoveryDraftEffortChanged(draft.id, it)) },
                                    onProofChanged = { onEvent(ChoreCaptureUiEvent.DiscoveryDraftProofRequiredChanged(draft.id, it)) },
                                    onRepeatsChanged = { onEvent(ChoreCaptureUiEvent.DiscoveryDraftRepeatsChanged(draft.id, it)) },
                                    onRepeatFrequencyChanged = { onEvent(ChoreCaptureUiEvent.DiscoveryDraftRepeatFrequencyChanged(draft.id, it)) },
                                    onDailyModeChanged = { onEvent(ChoreCaptureUiEvent.DiscoveryDraftDailyModeChanged(draft.id, it)) },
                                    onWeekdayToggled = { onEvent(ChoreCaptureUiEvent.DiscoveryDraftWeekdayToggled(draft.id, it)) },
                                    onOneTimeDateChanged = { onEvent(ChoreCaptureUiEvent.DiscoveryDraftOneTimeDateChanged(draft.id, it)) },
                                    onDelete = { onEvent(ChoreCaptureUiEvent.DeleteDiscoveryDraft(draft.id)) },
                                )
                            }
                            item {
                                FlatActionButton(
                                    text = "Add another chore",
                                    onClick = {
                                        onEvent(ChoreCaptureUiEvent.TriggerVibration)
                                        onEvent(ChoreCaptureUiEvent.AddDiscoveryDraft)
                                    },
                                    enabled = state.discoveryPhase != DiscoveryCapturePhase.Saving,
                                    filled = false,
                                    icon = Icons.Rounded.Add,
                                )
                            }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.captureLabel
                            .takeIf { it.isNotBlank() }
                            ?.let { label ->
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = when {
                                        state.uploadComplete || state.discoveryPhase == DiscoveryCapturePhase.Saved -> MaterialTheme.colorScheme.secondary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    fontWeight = if (state.uploadComplete || state.discoveryPhase == DiscoveryCapturePhase.Saved) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Medium
                                    },
                                )
                            }
                        when {
                            state.unavailableMessage != null -> {
                                FlatActionButton(
                                    text = "Back to chores",
                                    enabled = true,
                                    onClick = {
                                        onEvent(ChoreCaptureUiEvent.TriggerVibration)
                                        onBack()
                                    },
                                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                                    filled = true,
                                )
                            }

                            state.uploadComplete -> {
                                FlatActionButton(
                                    text = state.primaryActionLabel,
                                    onClick = {
                                        onEvent(ChoreCaptureUiEvent.TriggerVibration)
                                        onBack()
                                    },
                                    icon = Icons.Rounded.CheckCircle,
                                    filled = true,
                                )
                            }

                            state.isDiscoveryMode -> {
                                DiscoveryActionBlock(
                                    state = state,
                                    onEvent = onEvent,
                                    onLaunchCamera = { cameraHandler.launch() },
                                    onOpenPremium = onOpenPremium,
                                )
                            }

                            state.hasCapturedPhoto -> {
                                FlatActionButton(
                                    text = if (state.isSubmitting) "Submitting…" else state.primaryActionLabel,
                                    enabled = !state.isSubmitting,
                                    onClick = {
                                        onEvent(ChoreCaptureUiEvent.TriggerVibration)
                                        onEvent(ChoreCaptureUiEvent.UploadProof)
                                    },
                                    icon = Icons.Rounded.Upload,
                                    filled = true,
                                )
                                FlatActionButton(
                                    text = "Retake photo",
                                    enabled = !state.isSubmitting,
                                    onClick = {
                                        onEvent(ChoreCaptureUiEvent.TriggerVibration)
                                        cameraHandler.launch()
                                    },
                                    filled = false,
                                )
                            }

                            else -> {
                                FlatActionButton(
                                    text = state.primaryActionLabel,
                                    enabled = !state.isSubmitting,
                                    onClick = {
                                        onEvent(ChoreCaptureUiEvent.TriggerVibration)
                                        cameraHandler.launch()
                                    },
                                    icon = Icons.Rounded.Upload,
                                    filled = true,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoveryActionBlock(
    state: ChoreCaptureUiState,
    onEvent: (ChoreCaptureUiEvent) -> Unit,
    onLaunchCamera: () -> Unit,
    onOpenPremium: () -> Unit,
) {
    when {
        !state.hasCapturedPhoto -> {
            FlatActionButton(
                text = state.primaryActionLabel,
                enabled = !state.isSubmitting,
                onClick = {
                    onEvent(ChoreCaptureUiEvent.TriggerVibration)
                    onLaunchCamera()
                },
                icon = Icons.Rounded.Upload,
                filled = true,
            )
        }

        state.discoveryPhase == DiscoveryCapturePhase.Capture && !state.hasChorePilotPro -> {
            FlatActionButton(
                text = "Unlock discovery mode",
                enabled = !state.isSubmitting,
                onClick = {
                    onEvent(ChoreCaptureUiEvent.TriggerVibration)
                    onOpenPremium()
                },
                icon = Icons.Rounded.AutoFixHigh,
                filled = true,
            )
            FlatActionButton(
                text = "Retake room photo",
                enabled = !state.isSubmitting,
                onClick = {
                    onEvent(ChoreCaptureUiEvent.TriggerVibration)
                    onLaunchCamera()
                },
                filled = false,
            )
        }

        state.discoveryPhase == DiscoveryCapturePhase.Capture && state.hasChorePilotPro -> {
            FlatActionButton(
                text = "Analyze room",
                enabled = !state.isSubmitting,
                onClick = {
                    onEvent(ChoreCaptureUiEvent.TriggerVibration)
                    onEvent(ChoreCaptureUiEvent.RetryDiscoveryAnalysis)
                },
                icon = Icons.Rounded.AutoFixHigh,
                filled = true,
            )
            FlatActionButton(
                text = "Retake room photo",
                enabled = !state.isSubmitting,
                onClick = {
                    onEvent(ChoreCaptureUiEvent.TriggerVibration)
                    onLaunchCamera()
                },
                filled = false,
            )
        }

        state.discoveryPhase == DiscoveryCapturePhase.Analyzing -> {
            FlatActionButton(
                text = "Analyzing…",
                enabled = false,
                onClick = {},
                icon = Icons.Rounded.AutoFixHigh,
                filled = true,
            )
        }

        state.discoveryPhase == DiscoveryCapturePhase.Error -> {
            FlatActionButton(
                text = "Retry analysis",
                enabled = !state.isSubmitting && state.hasChorePilotPro,
                onClick = {
                    onEvent(ChoreCaptureUiEvent.TriggerVibration)
                    onEvent(ChoreCaptureUiEvent.RetryDiscoveryAnalysis)
                },
                icon = Icons.Rounded.Refresh,
                filled = true,
            )
            FlatActionButton(
                text = "Retake room photo",
                enabled = !state.isSubmitting,
                onClick = {
                    onEvent(ChoreCaptureUiEvent.TriggerVibration)
                    onLaunchCamera()
                },
                filled = false,
            )
        }

        state.discoveryPhase == DiscoveryCapturePhase.ReviewDrafts ||
            state.discoveryPhase == DiscoveryCapturePhase.Saving ||
            state.discoveryPhase == DiscoveryCapturePhase.Saved -> {
            FlatActionButton(
                text = if (state.discoveryPhase == DiscoveryCapturePhase.Saving) {
                    if (state.isAdminFlow) "Activating chores…" else "Submitting for review…"
                } else {
                    state.discoverySaveActionLabel
                },
                enabled = state.canSaveDiscoveryDrafts && state.discoveryPhase != DiscoveryCapturePhase.Saving,
                onClick = {
                    onEvent(ChoreCaptureUiEvent.TriggerVibration)
                    onEvent(ChoreCaptureUiEvent.SaveDiscoveryDrafts)
                },
                icon = Icons.Rounded.CheckCircle,
                filled = true,
            )
            FlatActionButton(
                text = "Retake room photo",
                enabled = state.discoveryPhase != DiscoveryCapturePhase.Saving,
                onClick = {
                    onEvent(ChoreCaptureUiEvent.TriggerVibration)
                    onLaunchCamera()
                },
                filled = false,
            )
        }
    }
}

@Composable
private fun DiscoveryDraftCard(
    draft: DiscoveryDraftUiModel,
    enabled: Boolean,
    onTitleChanged: (String) -> Unit,
    onRoomChanged: (String) -> Unit,
    onEffortChanged: (ChoreEffortValue) -> Unit,
    onProofChanged: (Boolean) -> Unit,
    onRepeatsChanged: (Boolean) -> Unit,
    onRepeatFrequencyChanged: (ChoreRepeatFrequencyValue) -> Unit,
    onDailyModeChanged: (ChoreDailyModeValue) -> Unit,
    onWeekdayToggled: (String) -> Unit,
    onOneTimeDateChanged: (String) -> Unit,
    onDelete: () -> Unit,
) {
    ProofPanel {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = draft.title.ifBlank { "Draft chore" },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = draftStatusLine(draft),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete draft",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable(enabled = enabled, onClick = onDelete)
                        .padding(4.dp),
                )
            }
            OutlinedTextField(
                value = draft.title,
                onValueChange = onTitleChanged,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Title") },
                singleLine = true,
            )
            OutlinedTextField(
                value = draft.roomName,
                onValueChange = onRoomChanged,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Room") },
                singleLine = true,
            )
            if (draft.description.isNotBlank()) {
                DiscoveryDraftNote(
                    title = "AI note",
                    body = draft.description,
                )
            }
            DraftControlHeader(
                title = "Effort",
                value = effortLabel(draft.effort),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ChoreEffortValue.entries.forEach { effort ->
                    EffortPill(
                        label = effortLabel(effort),
                        selected = draft.effort == effort,
                        enabled = enabled,
                        onClick = { onEffortChanged(effort) },
                    )
                }
            }
            ToggleRow(
                title = "Require proof",
                checked = draft.proofNeededSuggested,
                enabled = enabled,
                onCheckedChange = onProofChanged,
            )
            ToggleRow(
                title = "Repeat this chore",
                checked = draft.repeatsEnabled,
                enabled = enabled,
                onCheckedChange = onRepeatsChanged,
            )
            DiscoveryScheduleControls(
                draft = draft,
                enabled = enabled,
                onRepeatFrequencyChanged = onRepeatFrequencyChanged,
                onDailyModeChanged = onDailyModeChanged,
                onWeekdayToggled = onWeekdayToggled,
                onOneTimeDateChanged = onOneTimeDateChanged,
            )
        }
    }
}

@Composable
private fun DiscoveryScheduleControls(
    draft: DiscoveryDraftUiModel,
    enabled: Boolean,
    onRepeatFrequencyChanged: (ChoreRepeatFrequencyValue) -> Unit,
    onDailyModeChanged: (ChoreDailyModeValue) -> Unit,
    onWeekdayToggled: (String) -> Unit,
    onOneTimeDateChanged: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DraftControlHeader(
            title = if (draft.repeatsEnabled) "Schedule" else "One-time date",
            value = scheduleSummary(draft),
        )
        if (draft.repeatsEnabled) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ChoreRepeatFrequencyValue.entries.forEach { frequency ->
                    SchedulePill(
                        text = frequency.name,
                        selected = draft.repeatFrequency == frequency,
                        enabled = enabled,
                        onClick = { onRepeatFrequencyChanged(frequency) },
                    )
                }
            }
            when (draft.repeatFrequency) {
                ChoreRepeatFrequencyValue.Daily -> {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ChoreDailyModeValue.entries.forEach { mode ->
                            SchedulePill(
                                text = dailyModeLabel(mode),
                                selected = draft.dailyMode == mode,
                                enabled = enabled,
                                onClick = { onDailyModeChanged(mode) },
                            )
                        }
                    }
                    if (draft.dailyMode == ChoreDailyModeValue.SpecificDays) {
                        WeekdayChipRow(
                            selectedDays = draft.selectedWeekdays,
                            enabled = enabled,
                            onToggle = onWeekdayToggled,
                        )
                    }
                }

                ChoreRepeatFrequencyValue.Weekly -> {
                    WeekdayChipRow(
                        selectedDays = draft.selectedWeekdays,
                        enabled = enabled,
                        onToggle = onWeekdayToggled,
                    )
                }

                ChoreRepeatFrequencyValue.Monthly,
                ChoreRepeatFrequencyValue.Custom,
                -> {
                    DiscoveryDraftNote(
                        title = "Saved cadence",
                        body = scheduleSummary(draft),
                    )
                }
            }
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                oneTimeDateOptions(draft.selectedOneTimeDate).forEach { option ->
                    SchedulePill(
                        text = option,
                        selected = draft.selectedOneTimeDate == option,
                        enabled = enabled,
                        onClick = { onOneTimeDateChanged(option) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DraftControlHeader(
    title: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DiscoveryDraftNote(
    title: String,
    body: String,
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.24f),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun WeekdayChipRow(
    selectedDays: Set<String>,
    enabled: Boolean,
    onToggle: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        WeekdayOptions.forEach { day ->
            Surface(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable(enabled = enabled, onClick = { onToggle(day.fullLabel) }),
                color = if (day.fullLabel in selectedDays) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
                },
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(
                    1.dp,
                    if (day.fullLabel in selectedDays) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.84f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    },
                ),
                shadowElevation = 0.dp,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 38.dp, height = 34.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = day.shortLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (day.fullLabel in selectedDays) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

private data class WeekdayOption(
    val fullLabel: String,
    val shortLabel: String,
)

private val WeekdayOptions = listOf(
    WeekdayOption("Sunday", "S"),
    WeekdayOption("Monday", "M"),
    WeekdayOption("Tuesday", "T"),
    WeekdayOption("Wednesday", "W"),
    WeekdayOption("Thursday", "T"),
    WeekdayOption("Friday", "F"),
    WeekdayOption("Saturday", "S"),
)

@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun EffortPill(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else 0.42f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        ),
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SchedulePill(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = enabled, onClick = onClick),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 0.14f else 0.08f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
        },
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            1.dp,
            if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 0.32f else 0.18f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f)
            },
        ),
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
private fun DiscoveryStatusCard(
    title: String,
    body: String,
) {
    ProofPanel {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProofHeroPanel(
    discoveryMode: Boolean,
    title: String,
    body: String,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(
                painter = painterResource(
                    if (discoveryMode) Res.drawable.found_something_observer
                    else Res.drawable.proof_capture_mascot_camera,
                ),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(MaterialTheme.shapes.large),
                contentScale = ContentScale.Crop,
            )
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ProofPanel(
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    content: @Composable () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

private fun proofSummaryLine(
    state: ChoreCaptureUiState,
): String = listOfNotNull(
    state.roomName.takeIf { it.isNotBlank() },
    state.dayLabel.takeIf { it.isNotBlank() },
).joinToString(" • ")

private fun draftStatusLine(
    draft: DiscoveryDraftUiModel,
): String = listOf(
    draft.roomName.ifBlank { "Room needed" },
    effortLabel(draft.effort),
    scheduleSummary(draft),
).joinToString(" • ")

private fun scheduleSummary(
    draft: DiscoveryDraftUiModel,
): String {
    if (!draft.repeatsEnabled) {
        return "Once on ${draft.selectedOneTimeDate.ifBlank { "Tomorrow" }}"
    }
    return when (draft.repeatFrequency) {
        ChoreRepeatFrequencyValue.Daily -> when (draft.dailyMode) {
            ChoreDailyModeValue.EveryDay -> "Daily"
            ChoreDailyModeValue.Weekdays -> "Weekdays"
            ChoreDailyModeValue.SpecificDays -> "Daily on ${weekdaySummary(draft.selectedWeekdays)}"
        }

        ChoreRepeatFrequencyValue.Weekly -> "Weekly on ${weekdaySummary(draft.selectedWeekdays)}"
        ChoreRepeatFrequencyValue.Monthly -> "Monthly"
        ChoreRepeatFrequencyValue.Custom -> "Custom"
    }
}

private fun weekdaySummary(
    weekdays: Set<String>,
): String = weekdays
    .ifEmpty { setOf("Saturday") }
    .joinToString(", ") { day -> day.take(3) }

private fun effortLabel(
    effort: ChoreEffortValue,
): String = when (effort) {
    ChoreEffortValue.Light -> "Light"
    ChoreEffortValue.Standard -> "Standard"
    ChoreEffortValue.Heavy -> "Heavy"
}

private fun dailyModeLabel(
    mode: ChoreDailyModeValue,
): String = when (mode) {
    ChoreDailyModeValue.EveryDay -> "Every day"
    ChoreDailyModeValue.Weekdays -> "Weekdays"
    ChoreDailyModeValue.SpecificDays -> "Specific days"
}

private fun oneTimeDateOptions(
    selected: String,
): List<String> = buildList {
    add("Today")
    add("Tomorrow")
    add("This Friday")
    add("Next Monday")
    if (selected.isNotBlank() && selected !in this) {
        add(selected)
    }
}

@Composable
private fun ProofSectionLabel(
    title: String,
    body: String,
    accent: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = accent,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ProofPreview(previewUri: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Photo preview",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        ProofPreviewImage(
            model = previewUri,
            modifier = Modifier
                .fillMaxWidth()
                .height(196.dp)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        )
    }
}

@Composable
private fun FlatActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    filled: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = enabled, onClick = onClick),
        color = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(
            1.dp,
            if (filled) MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.48f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (filled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                )
                Box(modifier = Modifier.padding(start = 8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = if (filled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun heroTitle(
    state: ChoreCaptureUiState,
): String = when {
    state.isDiscoveryMode && state.discoveryPhase == DiscoveryCapturePhase.ReviewDrafts -> "Review AI chore drafts"
    state.isDiscoveryMode && state.discoveryPhase == DiscoveryCapturePhase.Analyzing -> "Reading the room"
    state.isDiscoveryMode && state.discoveryPhase == DiscoveryCapturePhase.Saved -> "Drafts saved"
    state.isDiscoveryMode && state.hasCapturedPhoto -> "Room photo ready"
    state.isDiscoveryMode -> state.helperTitle
    state.uploadComplete -> "Proof submitted"
    state.hasCapturedPhoto -> "Ready for review"
    else -> state.helperTitle
}

private fun heroBody(
    state: ChoreCaptureUiState,
): String = when {
    state.isDiscoveryMode && state.discoveryPhase == DiscoveryCapturePhase.ReviewDrafts -> "Edit, delete, or add chores before they land in your household list."
    state.isDiscoveryMode && state.discoveryPhase == DiscoveryCapturePhase.Analyzing -> "We’re turning what’s visible in the room into a draft chore list."
    state.isDiscoveryMode && state.discoveryPhase == DiscoveryCapturePhase.Saved -> state.successMessage ?: "Your new chores are ready."
    state.isDiscoveryMode && state.hasCapturedPhoto && !state.hasChorePilotPro -> "Discovery mode is premium. Unlock AI drafting to keep going with this photo."
    state.isDiscoveryMode && state.hasCapturedPhoto -> "This room photo is ready for analysis."
    state.uploadComplete -> "Your photo is attached and waiting for approval."
    state.hasCapturedPhoto -> "This shot looks good. Submit it when you're ready."
    else -> state.helperBody
}

@Composable
private fun ChoreCapturePreview(
    state: ChoreCaptureUiState,
    themeMode: ThemeMode = ThemeMode.Light,
) {
    ChorePilotTheme(themeMode = themeMode) {
        PreviewDeviceFrame { modifier ->
            Box(modifier = modifier.fillMaxSize()) {
                ChoreCaptureScreen(
                    state = state,
                    onEvent = {},
                    onBack = {},
                    onOpenPremium = {},
                )
            }
        }
    }
}

@Preview(name = "Discovery Capture", widthDp = 393, heightDp = 852)
@Composable
private fun DiscoveryCapturePreview() {
    ChoreCapturePreview(state = discoveryPreviewState())
}

@Preview(name = "Discovery Analyzing Dark", widthDp = 393, heightDp = 852)
@Composable
private fun DiscoveryAnalyzingDarkPreview() {
    ChoreCapturePreview(
        state = discoveryPreviewState(
            phase = DiscoveryCapturePhase.Analyzing,
            hasCapturedPhoto = true,
            previewUri = PreviewPhotoUri,
            captureLabel = "Analyzing your room photo…",
            primaryActionLabel = "Analyzing…",
        ),
        themeMode = ThemeMode.Dark,
    )
}

@Preview(name = "Discovery Review Drafts", widthDp = 393, heightDp = 852)
@Composable
private fun DiscoveryReviewDraftsPreview() {
    ChoreCapturePreview(
        state = discoveryPreviewState(
            phase = DiscoveryCapturePhase.ReviewDrafts,
            hasCapturedPhoto = true,
            previewUri = PreviewPhotoUri,
            discoveryDrafts = previewDiscoveryDrafts(),
            discoverySummary = "We found 3 draft chores for you to review.",
            captureLabel = "Review the drafts, tweak anything, then save them into chores.",
        ),
    )
}

@Preview(name = "Discovery Saving", widthDp = 393, heightDp = 852)
@Composable
private fun DiscoverySavingPreview() {
    ChoreCapturePreview(
        state = discoveryPreviewState(
            phase = DiscoveryCapturePhase.Saving,
            hasCapturedPhoto = true,
            previewUri = PreviewPhotoUri,
            discoveryDrafts = previewDiscoveryDrafts(),
            discoverySummary = "We found 3 draft chores for you to review.",
            captureLabel = "Activating these chores for the household…",
            isSubmitting = true,
        ),
    )
}

@Preview(name = "Discovery Saved Dark", widthDp = 393, heightDp = 852)
@Composable
private fun DiscoverySavedDarkPreview() {
    ChoreCapturePreview(
        state = discoveryPreviewState(
            phase = DiscoveryCapturePhase.Saved,
            hasCapturedPhoto = true,
            previewUri = PreviewPhotoUri,
            discoveryDrafts = previewDiscoveryDrafts().take(2),
            discoverySummary = "We found 2 draft chores for you to review.",
            captureLabel = "Everything is active. Heading back to chores…",
            successMessage = "New chores are now active in your household list.",
        ),
        themeMode = ThemeMode.Dark,
    )
}

@Preview(name = "Discovery Error", widthDp = 393, heightDp = 852)
@Composable
private fun DiscoveryErrorPreview() {
    ChoreCapturePreview(
        state = discoveryPreviewState(
            phase = DiscoveryCapturePhase.Error,
            hasCapturedPhoto = true,
            previewUri = PreviewPhotoUri,
            captureLabel = "Try again with better light or a wider room shot.",
            errorMessage = "We couldn't analyze that room photo yet.",
        ),
    )
}

@Preview(name = "Proof Upload Ready", widthDp = 393, heightDp = 852)
@Composable
private fun ProofUploadReadyPreview() {
    ChoreCapturePreview(
        state = ChoreCaptureUiState(
            isDiscoveryMode = false,
            choreId = "chore-entry-reset",
            dayLabel = "Today",
            choreTitle = "Reset the entryway",
            assigneeName = "Erick",
            roomName = "Entry",
            dueLabel = "Due tonight",
            title = "Upload proof",
            subtitle = "Add one clear photo of the finished chore.",
            helperTitle = "Show the finished result",
            helperBody = "One clear photo is enough.",
            previewUri = PreviewPhotoUri,
            hasCapturedPhoto = true,
            captureLabel = "Photo ready to submit.",
            primaryActionLabel = "Submit for review",
            hasChorePilotPro = true,
        ),
    )
}

private const val PreviewPhotoUri = "https://example.com/chorepilot-proof-preview.jpg"

private fun discoveryPreviewState(
    phase: DiscoveryCapturePhase = DiscoveryCapturePhase.Capture,
    hasCapturedPhoto: Boolean = false,
    previewUri: String? = null,
    discoveryDrafts: List<DiscoveryDraftUiModel> = emptyList(),
    discoverySummary: String = "",
    captureLabel: String = "",
    primaryActionLabel: String = "Take room photo",
    isSubmitting: Boolean = false,
    errorMessage: String? = null,
    successMessage: String? = null,
): ChoreCaptureUiState = ChoreCaptureUiState(
    isDiscoveryMode = true,
    isAdminFlow = true,
    title = "Scan for chores",
    subtitle = "Take one room photo and turn what you spot into editable chore drafts.",
    helperTitle = "Show the full room",
    helperBody = "Wider, brighter photos help AI catch more chores on the first pass.",
    primaryActionLabel = primaryActionLabel,
    previewUri = previewUri,
    hasCapturedPhoto = hasCapturedPhoto,
    captureLabel = captureLabel,
    isSubmitting = isSubmitting,
    errorMessage = errorMessage,
    hasChorePilotPro = true,
    discoveryPhase = phase,
    discoveryDrafts = discoveryDrafts,
    discoverySummary = discoverySummary,
    discoverySaveActionLabel = "Activate chores",
    successMessage = successMessage,
)

private fun previewDiscoveryDrafts(): List<DiscoveryDraftUiModel> = listOf(
    DiscoveryDraftUiModel(
        id = "draft-dishes",
        title = "Clear the dinner dishes",
        description = "Sink and counter both have visible dishes.",
        roomName = "Kitchen",
        effort = ChoreEffortValue.Standard,
        proofNeededSuggested = true,
        repeatsEnabled = true,
        repeatFrequency = ChoreRepeatFrequencyValue.Daily,
        dailyMode = ChoreDailyModeValue.Weekdays,
        selectedWeekdays = setOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday"),
    ),
    DiscoveryDraftUiModel(
        id = "draft-couch",
        title = "Reset couch pillows",
        description = "Living room seating could use a quick reset.",
        roomName = "Living room",
        effort = ChoreEffortValue.Light,
        proofNeededSuggested = false,
        repeatsEnabled = true,
        repeatFrequency = ChoreRepeatFrequencyValue.Weekly,
        selectedWeekdays = setOf("Saturday"),
    ),
    DiscoveryDraftUiModel(
        id = "draft-entry",
        title = "Put shoes in cubbies",
        roomName = "Entry",
        effort = ChoreEffortValue.Light,
        proofNeededSuggested = false,
        repeatsEnabled = false,
        selectedOneTimeDate = "Today",
    ),
)
