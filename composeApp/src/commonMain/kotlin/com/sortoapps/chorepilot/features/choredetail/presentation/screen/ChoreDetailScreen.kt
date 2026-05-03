package com.sortoapps.chorepilot.features.choredetail.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.AssignmentTurnedIn
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import chorepilot.composeapp.generated.resources.Res
import chorepilot.composeapp.generated.resources.m1_success_celebration
import com.sortoapps.chorepilot.core.designsystem.ChorePilotGradientBackground
import com.sortoapps.chorepilot.core.designsystem.ChorePilotTopAppBar
import com.sortoapps.chorepilot.core.designsystem.ChorePilotTopBarIconButton
import com.sortoapps.chorepilot.core.designsystem.IllustrationCard
import com.sortoapps.chorepilot.core.designsystem.PrimaryButton
import com.sortoapps.chorepilot.core.designsystem.SecondaryButton
import com.sortoapps.chorepilot.core.designsystem.ChorePilotTheme
import com.sortoapps.chorepilot.core.preview.PreviewDeviceFrame
import com.sortoapps.chorepilot.features.settings.domain.ThemeMode
import com.sortoapps.chorepilot.features.chores.presentation.components.ChoreLifecycleConfirmationDialog
import com.sortoapps.chorepilot.features.chores.domain.ChoreItemStatus
import com.sortoapps.chorepilot.features.chores.domain.ProofReviewStatus
import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.ChoreLifecycleAction
import com.sortoapps.chorepilot.features.choredetail.presentation.platform.ProofRemoteImage
import com.sortoapps.chorepilot.features.choredetail.presentation.viewmodel.ChoreDetailCompletionPromptKind
import com.sortoapps.chorepilot.features.choredetail.presentation.viewmodel.ChoreDetailSurface
import com.sortoapps.chorepilot.features.choredetail.presentation.viewmodel.ChoreDetailUiEvent
import com.sortoapps.chorepilot.features.choredetail.presentation.viewmodel.ChoreDetailUiState

private val DetailCardShape = RoundedCornerShape(8.dp)

@Composable
fun ChoreDetailScreen(
    state: ChoreDetailUiState,
    onEvent: (ChoreDetailUiEvent) -> Unit,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onCaptureProof: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    ChorePilotGradientBackground {
        state.completionPrompt?.let { prompt ->
            AlertDialog(
                onDismissRequest = { onEvent(ChoreDetailUiEvent.DismissCompletionPrompt) },
                title = {
                    Text(
                        text = "Looks like this belongs to someone else.",
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                text = {
                    Text(
                        text = "Complete \"${prompt.choreTitle}\" for ${prompt.assigneeName}?",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onEvent(ChoreDetailUiEvent.TriggerVibration)
                            onEvent(ChoreDetailUiEvent.ConfirmCompletionForAnotherMember)
                        },
                    ) {
                        Text(
                            when (prompt.kind) {
                                ChoreDetailCompletionPromptKind.CompleteForAnotherMember -> "Complete for them"
                            },
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            onEvent(ChoreDetailUiEvent.TriggerVibration)
                            onEvent(ChoreDetailUiEvent.DismissCompletionPrompt)
                        },
                    ) {
                        Text("Cancel")
                    }
                },
            )
        }
        if (state.showRejectProofDialog) {
            AlertDialog(
                onDismissRequest = { onEvent(ChoreDetailUiEvent.DismissRejectProof) },
                title = {
                    Text(
                        text = "Send proof back",
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Add a short reason so ${state.proofUploadedByName.ifBlank { state.assignee }} knows what to fix.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        OutlinedTextField(
                            value = state.proofRejectionReasonInput,
                            onValueChange = { onEvent(ChoreDetailUiEvent.ProofRejectionReasonChanged(it)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            supportingText = {
                                Text("${state.proofRejectionReasonInput.length}/80")
                            },
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onEvent(ChoreDetailUiEvent.TriggerVibration)
                            onEvent(ChoreDetailUiEvent.ConfirmRejectProof)
                        },
                        enabled = state.proofRejectionReasonInput.isNotBlank() && !state.isRejectingProof,
                    ) {
                        Text(if (state.isRejectingProof) "Sending…" else "Reject proof")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            onEvent(ChoreDetailUiEvent.TriggerVibration)
                            onEvent(ChoreDetailUiEvent.DismissRejectProof)
                        },
                        enabled = !state.isRejectingProof,
                    ) {
                        Text("Cancel")
                    }
                },
            )
        }
        if (state.showRejectPendingChoreDialog) {
            AlertDialog(
                onDismissRequest = { onEvent(ChoreDetailUiEvent.DismissRejectPendingChore) },
                title = {
                    Text(
                        text = "Send chore back",
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Add a short reason so ${state.submitterName.ifBlank { state.assignee }} knows what to adjust.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        OutlinedTextField(
                            value = state.pendingRejectionReasonInput,
                            onValueChange = { onEvent(ChoreDetailUiEvent.PendingRejectionReasonChanged(it)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            supportingText = {
                                Text("${state.pendingRejectionReasonInput.length}/80")
                            },
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onEvent(ChoreDetailUiEvent.TriggerVibration)
                            onEvent(ChoreDetailUiEvent.ConfirmRejectPendingChore)
                        },
                        enabled = state.pendingRejectionReasonInput.isNotBlank() && !state.isRejectingPendingChore,
                    ) {
                        Text(if (state.isRejectingPendingChore) "Sending…" else "Reject chore")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            onEvent(ChoreDetailUiEvent.TriggerVibration)
                            onEvent(ChoreDetailUiEvent.DismissRejectPendingChore)
                        },
                        enabled = !state.isRejectingPendingChore,
                    ) {
                        Text("Cancel")
                    }
                },
            )
        }
        ChoreLifecycleConfirmationDialog(
            confirmation = state.lifecycleConfirmation,
            onConfirm = {
                onEvent(ChoreDetailUiEvent.TriggerVibration)
                onEvent(ChoreDetailUiEvent.ConfirmLifecycleAction)
            },
            onDismiss = {
                onEvent(ChoreDetailUiEvent.TriggerVibration)
                onEvent(ChoreDetailUiEvent.DismissLifecycleConfirmation)
            },
            onTextChange = { onEvent(ChoreDetailUiEvent.LifecycleConfirmationTextChanged(it)) },
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                ChorePilotTopAppBar(
                    sectionTitle = if (state.proofUrl.isNotBlank()) "Proof review" else "Chore details",
                    navigationIcon = {
                        ChorePilotTopBarIconButton(
                            icon = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            onClick = onBack,
                        )
                    },
                    actionIcon = {
                        if (state.canManageLifecycle) {
                            ChorePilotTopBarIconButton(
                                icon = Icons.Rounded.Edit,
                                contentDescription = "Edit chore",
                                emphasized = true,
                                onClick = onEdit,
                            )
                        }
                    },
                )
            },
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (state.isLoading) {
                    item {
                        DetailSectionCard {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Text(
                                    text = "Loading this chore…",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    return@LazyColumn
                }

                if (state.unavailableMessage != null) {
                    item {
                        DetailSectionCard {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                Text(
                                    text = "This chore isn't available",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = state.unavailableMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                SecondaryButton(
                                    text = "Back to chores",
                                    onClick = onBack,
                                )
                            }
                        }
                    }
                    return@LazyColumn
                }

                item {
                    ChoreHeaderBlock(state = state)
                }

                if (state.proofUrl.isNotBlank()) {
                    item {
                        ProofReviewCard(
                            state = state,
                            onOpenFullImage = { uriHandler.openUri(state.proofUrl) },
                        )
                    }
                }

                if (state.discoveryGenerated && state.discoveryImageUrl.isNotBlank()) {
                    item {
                        DiscoverySourceCard(
                            url = state.discoveryImageUrl,
                            onOpenFullImage = { uriHandler.openUri(state.discoveryImageUrl) },
                        )
                    }
                }

                if (state.description.isNotBlank()) {
                    item {
                        DetailCard(
                            title = "Description",
                            body = state.description,
                        )
                    }
                }

                item {
                    when (state.surface) {
                        ChoreDetailSurface.Overview -> ActionCard(
                            state = state,
                            onEditDraft = {
                                onEvent(ChoreDetailUiEvent.TriggerVibration)
                                onEdit()
                            },
                            onCaptureProof = {
                                onEvent(ChoreDetailUiEvent.TriggerVibration)
                                onCaptureProof()
                            },
                            onApproveProof = {
                                onEvent(ChoreDetailUiEvent.TriggerVibration)
                                onEvent(ChoreDetailUiEvent.ApproveProof)
                            },
                            onApprovePending = {
                                onEvent(ChoreDetailUiEvent.TriggerVibration)
                                onEvent(ChoreDetailUiEvent.ApprovePendingChore)
                            },
                            onRejectPending = {
                                onEvent(ChoreDetailUiEvent.TriggerVibration)
                                onEvent(ChoreDetailUiEvent.RejectPendingChore)
                            },
                            onRejectProof = {
                                onEvent(ChoreDetailUiEvent.TriggerVibration)
                                onEvent(ChoreDetailUiEvent.RejectProof)
                            },
                            onMarkComplete = { onEvent(ChoreDetailUiEvent.MarkComplete) },
                        )

                        ChoreDetailSurface.Success -> SuccessCard(onBack = onBack)

                        ChoreDetailSurface.PendingProof -> PendingProofCard(
                            state = state,
                            onBack = onBack,
                            onShowOverview = { onEvent(ChoreDetailUiEvent.ShowOverview) },
                        )
                    }
                }

                if (state.canManageLifecycle) {
                    item {
                        LifecycleCard(
                            state = state,
                            onEvent = onEvent,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoverySourceCard(
    url: String,
    onOpenFullImage: () -> Unit,
) {
    DetailSectionCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            DetailSectionHeader(
                title = "Discovery source",
                supportingText = "This is the room photo used to generate the original AI chore draft.",
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(232.dp)
                    .clip(DetailCardShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                        shape = DetailCardShape,
                    ),
            ) {
                ProofRemoteImage(
                    url = url,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            DetailInlineAction(
                label = "Open source image",
                icon = Icons.AutoMirrored.Rounded.OpenInNew,
                onClick = onOpenFullImage,
            )
        }
    }
}

@Composable
private fun ChoreHeaderBlock(
    state: ChoreDetailUiState,
) {
    DetailSectionCard(
        borderColor = when {
            state.pendingApproval -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
            state.status == ChoreItemStatus.Completed -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)
            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        },
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = state.area.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = state.title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = detailStatusSummary(state),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Surface(
                shape = DetailCardShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.26f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DetailMetaItem(
                            modifier = Modifier.weight(1f),
                            label = "Assigned",
                            value = state.assignee,
                            icon = Icons.Rounded.AssignmentTurnedIn,
                        )
                        DetailMetaItem(
                            modifier = Modifier.weight(1f),
                            label = "Repeats",
                            value = state.cadence,
                            icon = Icons.Rounded.EventRepeat,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DetailMetaItem(
                            modifier = Modifier.weight(1f),
                            label = "Today",
                            value = detailStatusLabel(state),
                            icon = Icons.Rounded.Verified,
                        )
                        DetailMetaItem(
                            modifier = Modifier.weight(1f),
                            label = "Time",
                            value = state.durationLabel,
                            icon = Icons.Rounded.AccessTime,
                        )
                    }
                }
            }

            if (state.assignmentReason.isNotBlank()) {
                DetailSupportCallout(
                    message = state.assignmentReason,
                    accentColor = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private fun detailStatusLabel(
    state: ChoreDetailUiState,
): String {
    if (state.reviewRejectedReason.isNotBlank()) {
        return "Needs updates"
    }
    if (state.pendingApproval) {
        return "Waiting for review"
    }
    if (state.status != ChoreItemStatus.Completed) {
        return state.dueLabel
    }
    val actor = state.completedByName.trim()
    return if (actor.isBlank() || actor.equals(state.assignee, ignoreCase = true)) {
        "Completed"
    } else {
        "Completed by $actor"
    }
}

private fun detailStatusSummary(
    state: ChoreDetailUiState,
): String {
    if (state.pendingApproval) {
        return buildString {
            append("Submitted by ")
            append(state.submitterName.ifBlank { state.assignee })
            append(" and waiting for admin review.")
        }
    }
    if (state.reviewRejectedReason.isNotBlank()) {
        return buildString {
            append("Sent back to ")
            append(state.submitterName.ifBlank { state.assignee })
            append(" with updates requested.")
        }
    }
    val ownership = buildString {
        append("Assigned to ")
        append(state.assignee)
        if (state.completedByName.isNotBlank() &&
            !state.completedByName.equals(state.assignee, ignoreCase = true) &&
            state.status == ChoreItemStatus.Completed
        ) {
            append(" • completed by ")
            append(state.completedByName)
        }
    }
    return ownership
}

@Composable
private fun ProofReviewCard(
    state: ChoreDetailUiState,
    onOpenFullImage: () -> Unit,
) {
    val proofStatusLabel = when {
        state.proofReviewStatus == ProofReviewStatus.Rejected -> "Proof needs a resubmission"
        state.proofApprovedAt != null || state.status == ChoreItemStatus.Completed -> "Approved proof"
        state.status == ChoreItemStatus.PendingProof -> "Proof awaiting approval"
        else -> "Submitted proof"
    }

    DetailSectionCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            DetailSectionHeader(
                title = proofStatusLabel,
                supportingText = if (state.proofUploadedByName.isNotBlank()) {
                    if (state.proofReviewStatus == ProofReviewStatus.Rejected) {
                        "Uploaded by ${state.proofUploadedByName}. It was sent back for another proof."
                    } else if (state.proofApprovedAt != null || state.status == ChoreItemStatus.Completed) {
                        "Uploaded by ${state.proofUploadedByName} and approved for this day."
                    } else {
                        "Uploaded by ${state.proofUploadedByName} and waiting on review."
                    }
                } else {
                    "Proof is attached to this chore for review."
                },
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(232.dp)
                    .clip(DetailCardShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                        shape = DetailCardShape,
                    ),
            ) {
                ProofRemoteImage(
                    url = state.proofUrl,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            DetailInlineAction(
                label = "Open full image",
                icon = Icons.AutoMirrored.Rounded.OpenInNew,
                onClick = onOpenFullImage,
            )
            if (state.proofReviewStatus == ProofReviewStatus.Rejected && state.proofRejectedReason.isNotBlank()) {
                DetailSupportCallout(
                    message = buildString {
                        append("Reason: ")
                        append(state.proofRejectedReason)
                        if (state.proofReviewedByName.isNotBlank()) {
                            append(" • ")
                            append(state.proofReviewedByName)
                        }
                    },
                    accentColor = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun DetailCard(
    title: String,
    body: String,
) {
    DetailSectionCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DetailSectionHeader(title = title)
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ActionCard(
    state: ChoreDetailUiState,
    onEditDraft: () -> Unit,
    onCaptureProof: () -> Unit,
    onApproveProof: () -> Unit,
    onApprovePending: () -> Unit,
    onRejectPending: () -> Unit,
    onRejectProof: () -> Unit,
    onMarkComplete: () -> Unit,
) {
    DetailSectionCard(
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DetailSectionHeader(
                title = "Next step",
                supportingText = when {
                    state.pendingApproval && state.canManageLifecycle ->
                        "Review this discovery chore and decide whether it should join the active household schedule."
                    state.pendingApproval -> "This chore is still waiting for approval before it can join the active schedule."
                    state.reviewRejectedReason.isNotBlank() ->
                        "This discovery chore was sent back with feedback. The latest note is saved below."
                    state.reviewedAtEpochMs > 0L && state.discoveryGenerated ->
                        "This discovery chore has already been reviewed and its latest outcome is saved here."
                    state.proofReviewStatus == ProofReviewStatus.Rejected ->
                        "The last proof was sent back. Upload a new photo so this can be reviewed again."
                    state.proofUrl.isNotBlank() -> "Review the submitted proof and decide whether it is ready to count."
                    !state.isActive -> "This chore is inactive, so history stays visible but new completions are paused."
                    state.proofRequired -> "This chore needs photo proof before it can be reviewed."
                    else -> if (state.status == ChoreItemStatus.Completed) {
                        "This chore is already complete for today."
                    } else {
                        "Everything is ready for the next completion."
                    }
                },
            )
            if (state.pendingApproval) {
                DetailSupportCallout(
                    message = if (state.canManageLifecycle) {
                        "Approve to publish it to the schedule, or send it back with a short note."
                    } else {
                        "Discovery chores stay paused until an admin approves them."
                    },
                    accentColor = MaterialTheme.colorScheme.primary,
                )
                DetailSupportCallout(
                    message = "Submitted by ${state.submitterName.ifBlank { state.assignee }} • ${state.cadence} • ${state.durationLabel}",
                    accentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.canManageLifecycle) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ApprovalButton(
                            approved = false,
                            enabled = !state.isApprovingPendingChore,
                            isLoading = state.isApprovingPendingChore,
                            label = "Approve chore",
                            loadingLabel = "Approving…",
                            onClick = onApprovePending,
                        )
                        SecondaryButton(
                            text = if (state.isRejectingPendingChore) "Sending back…" else "Reject with note",
                            onClick = onRejectPending,
                            enabled = !state.isRejectingPendingChore,
                        )
                    }
                }
            } else if (state.reviewRejectedReason.isNotBlank()) {
                DetailSupportCallout(
                    message = "Reason: ${state.reviewRejectedReason}",
                    accentColor = MaterialTheme.colorScheme.error,
                )
                if (state.reviewedByName.isNotBlank()) {
                    DetailSupportCallout(
                        message = "Reviewed by ${state.reviewedByName}. Open the draft, make the update, and submit it again.",
                        accentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!state.canManageLifecycle) {
                    PrimaryButton(
                        text = "Update draft",
                        onClick = onEditDraft,
                    )
                }
            } else if (state.reviewedAtEpochMs > 0L && state.discoveryGenerated) {
                DetailSupportCallout(
                    message = if (state.reviewedByName.isNotBlank()) {
                        "Approved by ${state.reviewedByName}."
                    } else {
                        "This discovery chore has been approved."
                    },
                    accentColor = MaterialTheme.colorScheme.secondary,
                )
            } else if (state.proofReviewStatus == ProofReviewStatus.Rejected) {
                if (state.proofRejectedReason.isNotBlank()) {
                    DetailSupportCallout(
                        message = "Reason: ${state.proofRejectedReason}",
                        accentColor = MaterialTheme.colorScheme.error,
                    )
                }
                PrimaryButton(text = "Upload new proof", onClick = onCaptureProof)
            } else if (state.proofUrl.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ApprovalButton(
                        approved = state.status == ChoreItemStatus.Completed,
                        enabled = !state.isApprovingProof && state.status == ChoreItemStatus.PendingProof,
                        isLoading = state.isApprovingProof,
                        onClick = onApproveProof,
                    )
                    SecondaryButton(
                        text = if (state.isRejectingProof) "Sending back…" else "Reject with note",
                        onClick = onRejectProof,
                        enabled = state.status == ChoreItemStatus.PendingProof && !state.isRejectingProof,
                    )
                }
            } else if (!state.isActive) {
                DetailSupportCallout(
                    message = "Reactivate it from Lifecycle when this routine should rejoin the schedule.",
                    accentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (state.proofRequired) {
                PrimaryButton(text = "Upload proof", onClick = onCaptureProof)
            } else {
                if (state.status != ChoreItemStatus.Completed || state.canUndoCompletion) {
                    PrimaryButton(
                        text = when {
                            state.isMarkingComplete && state.status == ChoreItemStatus.Completed -> "Marking undone…"
                            state.isMarkingComplete -> "Marking done…"
                            state.status == ChoreItemStatus.Completed -> "Mark undone"
                            else -> "Mark done now"
                        },
                        onClick = onMarkComplete,
                        enabled = !state.isMarkingComplete,
                    )
                }
            }

            if (state.feedbackMessage != null) {
                FeedbackCallout(message = state.feedbackMessage)
            }
        }
    }
}

@Composable
private fun LifecycleCard(
    state: ChoreDetailUiState,
    onEvent: (ChoreDetailUiEvent) -> Unit,
) {
    DetailSectionCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DetailSectionHeader(
                title = "Lifecycle",
                supportingText = when {
                    state.pendingApproval ->
                        "This discovery chore is waiting for approval. Delete it here if it should not move forward."

                    state.isActive ->
                        "Deactivate this chore to stop future schedule appearances while keeping proof and completion history attached."

                    else ->
                        "This chore is inactive. Reactivate it to return it to the schedule, or delete it permanently from the household."
                },
            )
            state.lifecycleFeedbackMessage?.let { feedback ->
                FeedbackCallout(message = feedback)
            }
            if (!state.pendingApproval) {
                LifecycleDetailButton(
                    title = if (state.isActive) "Deactivate chore" else "Reactivate chore",
                    body = if (state.isActive) {
                        "Keep proof, history, and discovery context while taking it out of active lists."
                    } else {
                        "Return it to active schedules with all current proof and history intact."
                    },
                    icon = if (state.isActive) Icons.Rounded.Verified else Icons.Rounded.Edit,
                    destructive = state.isActive,
                    enabled = !state.isApplyingLifecycleAction,
                    onClick = {
                        onEvent(ChoreDetailUiEvent.TriggerVibration)
                        onEvent(
                            ChoreDetailUiEvent.LifecycleActionRequested(
                                if (state.isActive) ChoreLifecycleAction.Deactivate else ChoreLifecycleAction.Reactivate,
                            ),
                        )
                    },
                )
            }
            LifecycleDetailButton(
                title = "Delete permanently",
                body = "Remove this chore, its proof photos, completion history, and saved discovery context. Type delete to confirm.",
                icon = Icons.Rounded.DeleteOutline,
                destructive = true,
                enabled = !state.isApplyingLifecycleAction,
                onClick = {
                    onEvent(ChoreDetailUiEvent.TriggerVibration)
                    onEvent(ChoreDetailUiEvent.LifecycleActionRequested(ChoreLifecycleAction.Delete))
                },
            )
        }
    }
}

@Composable
private fun LifecycleDetailButton(
    title: String,
    body: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    destructive: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val accent = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    DetailSectionCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        backgroundColor = accent.copy(alpha = 0.06f),
        borderColor = accent.copy(alpha = 0.16f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = accent.copy(alpha = 0.14f),
                shape = DetailCardShape,
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
private fun SuccessCard(
    onBack: () -> Unit,
) {
    DetailSectionCard(
        borderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            IllustrationCard(
                drawable = Res.drawable.m1_success_celebration,
                contentDescription = "Completion celebration",
            )
            Text(
                text = "Nice work",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "This chore has been marked complete and the day plan is already calmer.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PrimaryButton(text = "Back to today", onClick = onBack)
        }
    }
}

@Composable
private fun PendingProofCard(
    state: ChoreDetailUiState,
    onBack: () -> Unit,
    onShowOverview: () -> Unit,
) {
    DetailSectionCard(
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(DetailCardShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Verified,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Awaiting review",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (state.proofUploadedByName.isNotBlank()) {
                            "${state.proofUploadedByName}'s proof is ready for admin review."
                        } else {
                            "Proof has been submitted and is ready for review."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            PrimaryButton(text = "Back to today", onClick = onBack)
            SecondaryButton(text = "Return to overview", onClick = onShowOverview)
        }
    }
}

@Composable
private fun ApprovalButton(
    approved: Boolean,
    enabled: Boolean,
    isLoading: Boolean,
    label: String = "Approve",
    loadingLabel: String = "Approving…",
    onClick: () -> Unit,
) {
    val containerColor = if (approved) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
    val contentColor = if (approved) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimary

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DetailCardShape)
            .clickable(enabled = enabled, onClick = onClick),
        color = containerColor.copy(alpha = if (enabled || approved) 1f else 0.7f),
        shape = DetailCardShape,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = when {
                    approved -> "Approved!"
                    isLoading -> loadingLabel
                    else -> label
                },
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

@Composable
private fun FeedbackCallout(
    message: String,
) {
    DetailSupportCallout(
        message = message,
        accentColor = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun DetailSectionCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        color = backgroundColor,
        shape = DetailCardShape,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        content()
    }
}

@Composable
private fun DetailSectionHeader(
    title: String,
    supportingText: String? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
        if (!supportingText.isNullOrBlank()) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DetailMetaItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.24f),
            shape = DetailCardShape,
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
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun DetailInlineAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
        shape = DetailCardShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun DetailSupportCallout(
    message: String,
    accentColor: Color,
) {
    Surface(
        color = accentColor.copy(alpha = 0.08f),
        shape = DetailCardShape,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.12f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ChoreDetailPreview(
    state: ChoreDetailUiState,
    themeMode: ThemeMode = ThemeMode.Light,
) {
    ChorePilotTheme(themeMode = themeMode) {
        PreviewDeviceFrame { modifier ->
            Box(modifier = modifier.fillMaxSize()) {
                ChoreDetailScreen(
                    state = state,
                    onEvent = {},
                    onBack = {},
                    onEdit = {},
                    onCaptureProof = {},
                )
            }
        }
    }
}

@Preview(name = "Chore Detail Active", widthDp = 393, heightDp = 852)
@Composable
private fun ChoreDetailActivePreview() {
    ChoreDetailPreview(state = previewActiveChoreDetailState())
}

@Preview(name = "Chore Detail Active Dark", widthDp = 393, heightDp = 852)
@Composable
private fun ChoreDetailActiveDarkPreview() {
    ChoreDetailPreview(
        state = previewActiveChoreDetailState(),
        themeMode = ThemeMode.Dark,
    )
}

@Preview(name = "Chore Detail Inactive", widthDp = 393, heightDp = 852)
@Composable
private fun ChoreDetailInactivePreview() {
    ChoreDetailPreview(state = previewInactiveChoreDetailState())
}

@Preview(name = "Chore Detail Inactive Dark", widthDp = 393, heightDp = 852)
@Composable
private fun ChoreDetailInactiveDarkPreview() {
    ChoreDetailPreview(
        state = previewInactiveChoreDetailState(),
        themeMode = ThemeMode.Dark,
    )
}

@Preview(name = "Chore Detail Discovery Sent Back", widthDp = 393, heightDp = 852)
@Composable
private fun ChoreDetailDiscoveryRejectedPreview() {
    ChoreDetailPreview(state = previewRejectedDiscoveryDetailState())
}

private fun previewActiveChoreDetailState(): ChoreDetailUiState = ChoreDetailUiState(
    choreId = "chore-kitchen-reset",
    title = "Kitchen reset",
    area = "Kitchen",
    cadence = "Every Monday, Wednesday, and Friday",
    assignee = "Emma",
    assignmentReason = "Fair rotation kept tonight balanced after Erick covered the bathroom yesterday.",
    dueLabel = "Due tonight",
    durationLabel = "20 min",
    description = "Clear the counters, run the dishwasher, and leave the sink ready for the next morning.",
    tags = listOf("Kitchen", "Evening"),
    proofRequired = true,
    canManageLifecycle = true,
    isActive = true,
    status = ChoreItemStatus.DueSoon,
    lifecycleFeedbackMessage = "Deactivate stops future schedule appearances while keeping proof and completion history attached.",
)

private fun previewInactiveChoreDetailState(): ChoreDetailUiState = ChoreDetailUiState(
    choreId = "chore-kitchen-reset",
    title = "Kitchen reset",
    area = "Kitchen",
    cadence = "Every Monday, Wednesday, and Friday",
    assignee = "Emma",
    assignmentReason = "Paused after spring break cleanup. Reactivate it when this routine should rejoin the weekly plan.",
    completedByName = "Emma",
    dueLabel = "Paused on Saturday",
    durationLabel = "20 min",
    description = "Clear the counters, run the dishwasher, and leave the sink ready for the next morning.",
    tags = listOf("Kitchen", "Evening"),
    proofRequired = true,
    proofPhotoCount = 2,
    proofUrl = "https://images.unsplash.com/photo-1556911220-bff31c812dba?auto=format&fit=crop&w=1200&q=80",
    proofUploadedByName = "Emma",
    canManageLifecycle = true,
    isActive = false,
    status = ChoreItemStatus.Completed,
    feedbackMessage = "Last completion was approved before the chore was deactivated.",
    lifecycleFeedbackMessage = "Inactive chores keep proof, completion history, and discovery context visible until they are deleted.",
    discoveryImageUrl = "https://images.unsplash.com/photo-1484154218962-a197022b5858?auto=format&fit=crop&w=1200&q=80",
    discoveryGenerated = true,
)

private fun previewRejectedDiscoveryDetailState(): ChoreDetailUiState = ChoreDetailUiState(
    choreId = "chore-discovery-1",
    title = "Reset the snack drawer",
    area = "Kitchen",
    cadence = "Every Friday",
    assignee = "Mila",
    submitterName = "Mila",
    dueLabel = "Needs updates",
    durationLabel = "10 min",
    description = "Straighten the snacks, wipe crumbs, and make space for the new grocery run.",
    tags = listOf("Kitchen", "Discovery"),
    isActive = false,
    pendingApproval = false,
    reviewRejectedReason = "Add a clearer title and mention the crumb wipe in the description.",
    reviewedByName = "Erick",
    reviewedAtEpochMs = 321L,
    discoveryGenerated = true,
    discoveryImageUrl = "https://images.unsplash.com/photo-1484154218962-a197022b5858?auto=format&fit=crop&w=1200&q=80",
)
