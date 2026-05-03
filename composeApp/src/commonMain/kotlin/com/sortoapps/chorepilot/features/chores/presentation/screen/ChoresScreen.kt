package com.sortoapps.chorepilot.features.chores.presentation.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewTimeline
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import chorepilot.composeapp.generated.resources.Res
import chorepilot.composeapp.generated.resources.found_something_lady
import com.sortoapps.chorepilot.core.designsystem.ChorePilotGradientBackground
import com.sortoapps.chorepilot.core.designsystem.PremiumTabItem
import com.sortoapps.chorepilot.core.designsystem.PremiumTabSelector
import com.sortoapps.chorepilot.core.preview.ChorePilotPreviewData
import com.sortoapps.chorepilot.core.preview.ChorePilotPreviewTheme
import com.sortoapps.chorepilot.core.preview.CompactPreviewShell
import com.sortoapps.chorepilot.features.chores.domain.ChoreItemStatus
import com.sortoapps.chorepilot.features.chores.presentation.components.ChoreLifecycleConfirmationDialog
import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.ChoreListItemUiModel
import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.ChoreCompletionPromptKind
import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.ChoreAvailabilitySuggestionSectionUiModel
import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.ChoreAvailabilitySuggestionUiModel
import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.ChoreRoomSectionUiModel
import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.ChoreViewerMode
import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.ChoresUiEvent
import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.ChoresUiState
import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.ProofReviewState
import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.ProofSubmissionDateSectionUiModel
import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.ProofSubmissionUiModel
import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.canManageChoreLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

@Composable
fun ChoresScreen(
    state: ChoresUiState,
    onEvent: (ChoresUiEvent) -> Unit,
    onOpenChore: (String) -> Unit,
    onCreateChore: () -> Unit,
    onEditChore: (String) -> Unit,
    onOpenFoundSomething: () -> Unit,
) {
    val filters = availableFilters(state.viewerMode)

    ChorePilotGradientBackground {
        state.completionPrompt?.let { prompt ->
            AlertDialog(
                onDismissRequest = {
                    onEvent(ChoresUiEvent.TriggerVibration)
                    onEvent(ChoresUiEvent.DismissCompletionPrompt)
                },
                title = {
                    Text(
                        text = when (prompt.kind) {
                            ChoreCompletionPromptKind.RemoveSubmittedProof -> "Remove submitted proof?"
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                text = {
                    Text(
                        text = when (prompt.kind) {
                            ChoreCompletionPromptKind.RemoveSubmittedProof ->
                                "Unchecking \"${prompt.choreTitle}\" will delete the proof uploaded for this day."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onEvent(ChoresUiEvent.TriggerVibration)
                            onEvent(ChoresUiEvent.ConfirmProofRemoval)
                        },
                    ) {
                        Text("Delete proof")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            onEvent(ChoresUiEvent.TriggerVibration)
                            onEvent(ChoresUiEvent.DismissCompletionPrompt)
                        },
                    ) {
                        Text("Cancel")
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
            )
        }
        state.pendingApprovalRejectionDialog?.let { dialog ->
            AlertDialog(
                onDismissRequest = {
                    onEvent(ChoresUiEvent.TriggerVibration)
                    onEvent(ChoresUiEvent.PendingApprovalRejectDismissed)
                },
                title = {
                    Text(
                        text = "Send chore back",
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Add a short reason so ${dialog.submitterName.ifBlank { "the member" }} knows what to adjust.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        OutlinedTextField(
                            value = dialog.reason,
                            onValueChange = { onEvent(ChoresUiEvent.PendingApprovalRejectionReasonChanged(it)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            supportingText = {
                                Text("${dialog.reason.length}/80")
                            },
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onEvent(ChoresUiEvent.TriggerVibration)
                            onEvent(ChoresUiEvent.PendingApprovalRejectConfirmed)
                        },
                        enabled = dialog.reason.isNotBlank(),
                    ) {
                        Text("Reject chore")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            onEvent(ChoresUiEvent.TriggerVibration)
                            onEvent(ChoresUiEvent.PendingApprovalRejectDismissed)
                        },
                    ) {
                        Text("Cancel")
                    }
                },
            )
        }
        ChoreLifecycleConfirmationDialog(
            confirmation = state.lifecycleConfirmation,
            onConfirm = {
                onEvent(ChoresUiEvent.TriggerVibration)
                onEvent(ChoresUiEvent.ConfirmLifecycleAction)
            },
            onDismiss = {
                onEvent(ChoresUiEvent.TriggerVibration)
                onEvent(ChoresUiEvent.DismissLifecycleConfirmation)
            },
            onTextChange = { onEvent(ChoresUiEvent.LifecycleConfirmationTextChanged(it)) },
        )
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item {
                    ChoresHeader(
                        viewerMode = state.viewerMode,
                        canUseAdminExperience = state.canUseAdminExperience,
                        onViewerModeSelected = { selectedMode ->
                            if (selectedMode != state.viewerMode) {
                                onEvent(ChoresUiEvent.TriggerVibration)
                                onEvent(ChoresUiEvent.ViewerModeToggled)
                            }
                        },
                    )
                }

                item {
                    SearchBar(
                        query = state.query,
                        canUseAdminExperience = state.canUseAdminExperience,
                        onQueryChanged = { onEvent(ChoresUiEvent.QueryChanged(it)) },
                    )
                }

                state.feedbackMessage?.let { feedback ->
                    item {
                        ProofApprovalFeedbackCard(
                            message = feedback,
                            onDismiss = { onEvent(ChoresUiEvent.DismissFeedback) },
                        )
                    }
                }

                item {
                    FilterTabs(
                        filters = filters,
                        selectedFilter = state.selectedFilter,
                        onFilterSelected = {
                            onEvent(ChoresUiEvent.TriggerVibration)
                            onEvent(ChoresUiEvent.FilterSelected(it))
                        },
                    )
                }

                if (state.canUseAdminExperience) {
                    item {
                        FoundSomethingSpotlight(
                            onClick = onOpenFoundSomething,
                        )
                    }
                }

                if (
                    state.viewerMode == ChoreViewerMode.Admin &&
                    state.autoAssignSuggestionsEnabled &&
                    (state.isLoadingAvailabilitySuggestions || state.suggestionSections.isNotEmpty())
                ) {
                    item {
                        AvailabilitySuggestionQueue(
                            isLoading = state.isLoadingAvailabilitySuggestions,
                            sections = state.suggestionSections,
                            onAccept = { suggestionId ->
                                onEvent(ChoresUiEvent.TriggerVibrationSuccess)
                                onEvent(ChoresUiEvent.AvailabilitySuggestionAccepted(suggestionId))
                            },
                            onDismiss = { suggestionId ->
                                onEvent(ChoresUiEvent.TriggerVibration)
                                onEvent(ChoresUiEvent.AvailabilitySuggestionDismissed(suggestionId))
                            },
                        )
                    }
                }

                if (state.viewerMode == ChoreViewerMode.Admin) {
                    item {
                        if (
                            state.proofQueueSections.isEmpty() &&
                            state.pendingApprovalSections.isEmpty() &&
                            state.inactiveSections.isEmpty() &&
                            state.needsOwnerSections.isEmpty() &&
                            state.activeSections.isEmpty()
                        ) {
                            EmptyChoresState(viewerMode = state.viewerMode)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                if (state.proofQueueSections.isNotEmpty()) {
                                    val proofs = state.proofQueueSections.flatMap { it.proofs }
                                    val pendingCount = proofs.count { it.state == ProofReviewState.AwaitingApproval }
                                    val approvedCount = proofs.count { it.state == ProofReviewState.Approved }
                                    AdminSection(
                                        title = "Proof review",
                                        subtitle = buildString {
                                            append("$pendingCount awaiting")
                                            if (approvedCount > 0) {
                                                append(" • $approvedCount approved")
                                            }
                                        },
                                        tint = MaterialTheme.colorScheme.error,
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Rounded.Notifications,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        },
                                    ) {
                                        state.proofQueueSections.forEach { section ->
                                            ProofReviewDateSection(
                                                section = section,
                                                onOpenChore = onOpenChore,
                                                onApprove = { choreId -> onEvent(ChoresUiEvent.ProofApproved(choreId)) },
                                            )
                                        }
                                    }
                                }

                                if (state.pendingApprovalSections.isNotEmpty()) {
                                    val pendingCount = state.pendingApprovalSections.sumOf { it.chores.size }
                                    AdminSection(
                                        title = "Waiting for approval",
                                        subtitle = "$pendingCount discovery chore${if (pendingCount == 1) "" else "s"} submitted for activation",
                                        tint = MaterialTheme.colorScheme.primary,
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Rounded.AutoFixHigh,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        },
                                    ) {
                                        state.pendingApprovalSections.forEach { section ->
                                            AdminRoomSection(
                                                section = section,
                                                viewerMode = state.viewerMode,
                                                canManageLifecycle = state.viewerRole.canManageChoreLifecycle(),
                                                viewerName = state.viewerName,
                                                onOpenChore = onOpenChore,
                                                onEditChore = onEditChore,
                                                onToggleComplete = { choreId ->
                                                    onEvent(ChoresUiEvent.TriggerVibration)
                                                    onEvent(ChoresUiEvent.CompletionToggled(choreId))
                                                },
                                                onApprovePending = { choreId ->
                                                    onEvent(ChoresUiEvent.TriggerVibration)
                                                    onEvent(ChoresUiEvent.PendingApprovalApproved(choreId))
                                                },
                                                onRejectPending = { choreId ->
                                                    onEvent(ChoresUiEvent.TriggerVibration)
                                                    onEvent(ChoresUiEvent.PendingApprovalRejectRequested(choreId))
                                                },
                                                onReactivate = {},
                                                onDelete = {},
                                            )
                                        }
                                    }
                                }

                                if (state.activeSections.isNotEmpty()) {
                                    AdminSection(
                                        title = "Current active chores",
                                        subtitle = "Assigned work moving through today's rooms",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Rounded.ViewTimeline,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        },
                                    ) {
                                        state.activeSections.forEach { section ->
                                            AdminRoomSection(
                                                section = section,
                                                viewerMode = state.viewerMode,
                                                canManageLifecycle = state.viewerRole.canManageChoreLifecycle(),
                                                viewerName = state.viewerName,
                                                onOpenChore = onOpenChore,
                                                onEditChore = onEditChore,
                                                onToggleComplete = { choreId ->
                                                    onEvent(ChoresUiEvent.TriggerVibration)
                                                    onEvent(ChoresUiEvent.CompletionToggled(choreId))
                                                },
                                                onApprovePending = {},
                                                onRejectPending = {},
                                                onReactivate = {},
                                                onDelete = {},
                                            )
                                        }
                                    }
                                }

                                if (state.needsOwnerSections.isNotEmpty()) {
                                    val needsOwnerCount = state.needsOwnerSections.sumOf { it.chores.size }
                                    AdminSection(
                                        title = "Needs owner",
                                        subtitle = "$needsOwnerCount active chore${if (needsOwnerCount == 1) "" else "s"} still need an assignee",
                                        tint = MaterialTheme.colorScheme.primary,
                                        muted = true,
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Rounded.Groups,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        },
                                    ) {
                                        state.needsOwnerSections.forEach { section ->
                                            AdminRoomSection(
                                                section = section,
                                                viewerMode = state.viewerMode,
                                                canManageLifecycle = state.viewerRole.canManageChoreLifecycle(),
                                                viewerName = state.viewerName,
                                                onOpenChore = onOpenChore,
                                                onEditChore = onEditChore,
                                                onToggleComplete = { choreId ->
                                                    onEvent(ChoresUiEvent.TriggerVibration)
                                                    onEvent(ChoresUiEvent.CompletionToggled(choreId))
                                                },
                                                onApprovePending = {},
                                                onRejectPending = {},
                                                onReactivate = {},
                                                onDelete = {},
                                                muted = true,
                                            )
                                        }
                                    }
                                }

                                if (state.inactiveSections.isNotEmpty()) {
                                    val inactiveCount = state.inactiveSections.sumOf { it.chores.size }
                                    AdminSection(
                                        title = "Inactive chores",
                                        subtitle = "$inactiveCount chore${if (inactiveCount == 1) "" else "s"} parked for now. Reactivate to bring them back, or delete old work permanently.",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        muted = true,
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Rounded.Groups,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        },
                                    ) {
                                        state.inactiveSections.forEach { section ->
                                            AdminRoomSection(
                                                section = section,
                                                viewerMode = state.viewerMode,
                                                canManageLifecycle = state.viewerRole.canManageChoreLifecycle(),
                                                viewerName = state.viewerName,
                                                onOpenChore = onOpenChore,
                                                onEditChore = onEditChore,
                                                onToggleComplete = { choreId ->
                                                    onEvent(ChoresUiEvent.TriggerVibration)
                                                    onEvent(ChoresUiEvent.CompletionToggled(choreId))
                                                },
                                                onApprovePending = {},
                                                onRejectPending = {},
                                                onReactivate = { choreId ->
                                                    onEvent(ChoresUiEvent.ReactivateRequested(choreId))
                                                },
                                                onDelete = { choreId ->
                                                    onEvent(ChoresUiEvent.DeleteRequested(choreId))
                                                },
                                                muted = true,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val memberSections = state.chores
                        .groupBy { sectionTitle(it.area) }
                        .entries
                        .sortedBy { it.key }

                    if (memberSections.isEmpty()) {
                        item {
                            EmptyChoresState(viewerMode = state.viewerMode)
                        }
                    } else {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                memberSections.forEach { entry ->
                                    AdminRoomSection(
                                        section = ChoreRoomSectionUiModel(
                                            title = entry.key,
                                            chores = entry.value,
                                        ),
                                        viewerMode = state.viewerMode,
                                        canManageLifecycle = false,
                                        viewerName = state.viewerName,
                                        onOpenChore = onOpenChore,
                                        onEditChore = onEditChore,
                                        onToggleComplete = { choreId ->
                                            onEvent(ChoresUiEvent.TriggerVibration)
                                            onEvent(ChoresUiEvent.CompletionToggled(choreId))
                                        },
                                        onApprovePending = {},
                                        onRejectPending = {},
                                        onReactivate = {},
                                        onDelete = {},
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (state.viewerMode == ChoreViewerMode.Admin && state.canUseAdminExperience) {
                AdminFab(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp),
                    onClick = {
                        onEvent(ChoresUiEvent.TriggerVibration)
                        onCreateChore()
                    },
                )
            }
        }
    }
}

@Composable
private fun ProofApprovalFeedbackCard(
    message: String,
    onDismiss: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Dismiss approval feedback",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FoundSomethingSpotlight(
    onClick: () -> Unit,
) {
    val ctaBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
        ),
    )

    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "See something to do?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Upload a room photo and let AI draft chores it spots for admin review.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Surface(
                    modifier = Modifier.clickable(onClick = onClick),
                    color = Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                ) {
                    Row(
                        modifier = Modifier
                            .background(ctaBrush)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CameraAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "Scan room",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.size(width = 112.dp, height = 136.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.16f),
                shadowElevation = 0.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
            ) {
                Image(
                    painter = painterResource(Res.drawable.found_something_lady),
                    contentDescription = "Woman spotting chores around the house",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

@Composable
private fun ChoresHeader(
    viewerMode: ChoreViewerMode,
    canUseAdminExperience: Boolean,
    onViewerModeSelected: (ChoreViewerMode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Chores",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
        if (canUseAdminExperience) {
            ViewerModeToggle(
                selectedMode = viewerMode,
                onSelected = onViewerModeSelected,
            )
        }
    }
}

@Composable
private fun ViewerModeToggle(
    selectedMode: ChoreViewerMode,
    onSelected: (ChoreViewerMode) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChoreViewerMode.entries.forEach { mode ->
                val selected = mode == selectedMode
                Surface(
                    modifier = Modifier.clickable(enabled = !selected) { onSelected(mode) },
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    border = if (selected) {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    } else {
                        null
                    },
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp,
                ) {
                    Text(
                        text = if (mode == ChoreViewerMode.Admin) "Admin" else "Member",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    canUseAdminExperience: Boolean,
    onQueryChanged: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            BasicTextField(
                value = query,
                onValueChange = onQueryChanged,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                ),
                decorationBox = { innerTextField ->
                    if (query.isBlank()) {
                        Text(
                            text = if (canUseAdminExperience) {
                                "Search chores, rooms, or owners..."
                            } else {
                                "Search your chores or rooms..."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    innerTextField()
                },
            )
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f),
                shape = RoundedCornerShape(8.dp),
            ) {
                Box(
                    modifier = Modifier.size(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterTabs(
    filters: List<String>,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
) {
    PremiumTabSelector(
        items = filters.map { PremiumTabItem(label = it) },
        selectedIndex = filters.indexOf(selectedFilter).coerceAtLeast(0),
        onSelect = { index -> onFilterSelected(filters[index]) },
        highlightColor = MaterialTheme.colorScheme.primary,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        cornerRadius = 8.dp,
        indicatorCornerRadius = 8.dp,
        indicatorInset = 4.dp,
        flatStyle = true,
    )
}

@Composable
private fun AdminSection(
    title: String,
    subtitle: String,
    tint: Color,
    muted: Boolean = false,
    icon: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (muted) 0.78f else 0.92f),
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = if (muted) {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
            } else {
                tint.copy(alpha = 0.14f)
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .background(
                        color = if (muted) MaterialTheme.colorScheme.outline.copy(alpha = 0.18f) else tint.copy(alpha = 0.72f),
                        shape = CircleShape,
                    ),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = tint.copy(alpha = if (muted) 0.08f else 0.12f),
                    shape = CircleShape,
                ) {
                    Box(
                        modifier = Modifier.size(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        icon()
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun ProofReviewDateSection(
    section: ProofSubmissionDateSectionUiModel,
    onOpenChore: (String) -> Unit,
    onApprove: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = section.title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.ExtraBold,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            section.proofs.forEach { proof ->
                ProofReviewRow(
                    proof = proof,
                    onOpen = { onOpenChore(proof.choreId) },
                    onApprove = { onApprove(proof.choreId) },
                )
            }
        }
    }
}

@Composable
private fun ProofReviewRow(
    proof: ProofSubmissionUiModel,
    onOpen: () -> Unit,
    onApprove: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                shape = CircleShape,
            ) {
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = proof.area.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = proof.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${proof.memberName} • ${proof.submittedLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ProofUploadPreview(photoCount = proof.photoCount)
                    Text(
                        text = "${proof.photoCount} photo${if (proof.photoCount == 1) "" else "s"} uploaded",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                    )
                }
                if (proof.proofUrl.isNotBlank()) {
                    Text(
                        text = "View proof",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            uriHandler.openUri(proof.proofUrl)
                        },
                    )
                }
            }

            if (proof.state == ProofReviewState.AwaitingApproval) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Approval pending",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                    )
                    ApproveButton(onClick = onApprove)
                }
            } else {
                Text(
                    text = "Approved",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun ProofUploadPreview(
    photoCount: Int,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(photoCount.coerceIn(1, 3)) { index ->
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(
                        if (index == 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                        else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.64f),
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.small,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Checklist,
                    contentDescription = null,
                    tint = if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}

@Composable
private fun ApproveButton(
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f),
        shape = CircleShape,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "Approve",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun AdminRoomSection(
    section: ChoreRoomSectionUiModel,
    viewerMode: ChoreViewerMode,
    canManageLifecycle: Boolean,
    viewerName: String,
    onOpenChore: (String) -> Unit,
    onEditChore: (String) -> Unit,
    onToggleComplete: (String) -> Unit,
    onApprovePending: (String) -> Unit,
    onRejectPending: (String) -> Unit,
    onReactivate: (String) -> Unit,
    onDelete: (String) -> Unit,
    muted: Boolean = false,
) {
    val dividerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawLine(
                        color = dividerColor,
                        start = androidx.compose.ui.geometry.Offset(0f, size.height),
                        end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = roomSectionIcon(section.title),
                    contentDescription = null,
                    tint = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else roomAccent(section.title),
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = section.title.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
                shape = CircleShape,
            ) {
                Text(
                    text = "${section.chores.size} chore${if (section.chores.size == 1) "" else "s"}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            section.chores.forEach { chore ->
                CompactChoreRow(
                    chore = chore,
                    viewerMode = viewerMode,
                    canManageLifecycle = canManageLifecycle,
                    viewerName = viewerName,
                    muted = muted,
                    onOpen = { onOpenChore(chore.id) },
                    onEdit = { onEditChore(chore.id) },
                    onToggleComplete = { onToggleComplete(chore.id) },
                    onApprovePending = { onApprovePending(chore.id) },
                    onRejectPending = { onRejectPending(chore.id) },
                    onReactivate = { onReactivate(chore.id) },
                    onDelete = { onDelete(chore.id) },
                )
            }
        }
    }
}

@Composable
private fun CompactChoreRow(
    chore: ChoreListItemUiModel,
    viewerMode: ChoreViewerMode,
    canManageLifecycle: Boolean,
    viewerName: String,
    muted: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onToggleComplete: () -> Unit,
    onApprovePending: () -> Unit,
    onRejectPending: () -> Unit,
    onReactivate: () -> Unit,
    onDelete: () -> Unit,
) {
    val accent = rowAccent(chore)
    val isHighlighted = chore.isAssignedToViewer
    val isCompleted = chore.status == ChoreItemStatus.Completed
    val isPendingProof = chore.status == ChoreItemStatus.PendingProof
    val showStatusChip = chore.pendingApproval ||
        chore.status == ChoreItemStatus.PendingProof ||
        chore.status == ChoreItemStatus.Overdue ||
        !chore.isActive ||
        chore.isUnassigned

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        color = if (isPendingProof) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = if (muted) 0.74f else 1f)
        },
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = if (isHighlighted) 0.18f else 0.08f),
        ),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = if (isCompleted) 0.32f else 0.88f)),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (chore.pendingApproval) {
                    PendingReviewMetaRow(chore = chore)
                }
                Text(
                    text = chore.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isCompleted || muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                )
                Text(
                    text = compactSupportingLine(chore = chore),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (chore.pendingApproval) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (showStatusChip) {
                StatusChip(
                    text = rowStatusLabel(chore),
                    color = rowStatusColor(chore),
                )
            }

            when (viewerMode) {
                ChoreViewerMode.Admin -> if (chore.pendingApproval) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ApproveButton(onClick = onApprovePending)
                        InlineActionButton(
                            icon = Icons.Rounded.Close,
                            text = "Reject",
                            destructive = true,
                            onClick = onRejectPending,
                        )
                    }
                } else if (!chore.isActive && canManageLifecycle) {
                    InactiveLifecycleActions(
                        onReactivate = onReactivate,
                        onDelete = onDelete,
                    )
                } else {
                    EditActionButton(onClick = onEdit)
                }
                ChoreViewerMode.Member -> MemberCompletionButton(
                    completed = isCompleted,
                    pending = isPendingProof,
                    enabled = chore.canToggleCompletion,
                    accent = accent,
                    onClick = onToggleComplete,
                )
            }
        }
    }
}

@Composable
private fun PendingReviewMetaRow(
    chore: ChoreListItemUiModel,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ReviewMetaPill(
            label = "From ${chore.submitterName.ifBlank { "member" }}",
            accent = MaterialTheme.colorScheme.primary,
        )
        ReviewMetaPill(
            label = chore.cadence,
            accent = MaterialTheme.colorScheme.secondary,
        )
        ReviewMetaPill(
            label = chore.durationLabel,
            accent = MaterialTheme.colorScheme.tertiary,
        )
    }
}

@Composable
private fun ReviewMetaPill(
    label: String,
    accent: Color,
) {
    Surface(
        color = accent.copy(alpha = 0.12f),
        shape = CircleShape,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.16f)),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun InactiveLifecycleActions(
    onReactivate: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InlineActionButton(
            icon = Icons.Rounded.Restore,
            text = "Reactivate",
            onClick = onReactivate,
        )
        InlineActionButton(
            icon = Icons.Rounded.DeleteOutline,
            text = "Delete",
            destructive = true,
            onClick = onDelete,
        )
    }
}

@Composable
private fun InlineActionButton(
    icon: ImageVector,
    text: String,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val accent = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ),
        color = accent.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.16f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun EditActionButton(
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = CircleShape,
        shadowElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier.size(28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = "Edit chore",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun MemberCompletionButton(
    completed: Boolean,
    pending: Boolean,
    enabled: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    val settledScale by animateFloatAsState(
        targetValue = if (completed) 1f else 0.92f,
        animationSpec = tween(durationMillis = 180),
        label = "chores_completion_scale",
    )
    Box(
        modifier = Modifier
            .size(28.dp)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (completed) {
            Surface(
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        scaleX = settledScale
                        scaleY = settledScale
                    },
                color = accent.copy(alpha = 0.18f),
                shape = CircleShape,
                shadowElevation = 0.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Completed",
                        tint = accent,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        color = if (enabled) accent.copy(alpha = 0.38f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
                        shape = CircleShape,
                    )
                    .alpha(if (enabled) 1f else 0.56f),
                contentAlignment = Alignment.Center,
            ) {
                if (pending) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.94f)),
                    )
                }
            }
        }
        CompletionPopEffect(
            completed = completed,
            tint = accent,
            iconSize = 16.dp,
        )
    }
}

@Composable
private fun CompletionPopEffect(
    completed: Boolean,
    tint: Color,
    iconSize: androidx.compose.ui.unit.Dp,
) {
    val overlayAlpha = remember { Animatable(0f) }
    val overlayScale = remember { Animatable(0.6f) }
    var previousCompleted by remember { mutableStateOf(completed) }

    LaunchedEffect(completed) {
        if (completed && !previousCompleted) {
            overlayAlpha.snapTo(0.9f)
            overlayScale.snapTo(0.6f)
            launch {
                overlayScale.animateTo(
                    targetValue = 1.55f,
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                )
            }
            overlayAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 220, easing = LinearOutSlowInEasing),
            )
        }
        previousCompleted = completed
    }

    if (overlayAlpha.value > 0.01f) {
        Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer {
                    alpha = overlayAlpha.value
                    scaleX = overlayScale.value
                    scaleY = overlayScale.value
                },
        )
    }
}

@Composable
private fun StatusChip(
    text: String,
    color: Color,
) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = CircleShape,
        shadowElevation = 0.dp,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun EmptyChoresState(
    viewerMode: ChoreViewerMode,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (viewerMode == ChoreViewerMode.Admin) "No admin work matches that filter." else "Nothing is waiting for you here.",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (viewerMode == ChoreViewerMode.Admin) {
                    "Try another filter or search to find approvals, active chores, or deactivated rooms."
                } else {
                    "Try a different search or filter."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AdminFab(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.primary,
        shape = CircleShape,
        shadowElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier.size(52.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Add chore",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

private fun compactSupportingLine(
    chore: ChoreListItemUiModel,
): String {
    val ownerLabel = if (chore.isAssignedToViewer) "Me" else chore.assignee
    return when {
        chore.pendingApproval -> buildString {
            append("Submitted by ")
            append(chore.submitterName.ifBlank { ownerLabel })
            if (chore.reviewRejectedReason.isNotBlank()) {
                append(" • Last note: ")
                append(chore.reviewRejectedReason)
            } else {
                append(" • Waiting for review")
            }
        }
        chore.reviewRejectedReason.isNotBlank() -> "Sent back • ${chore.reviewRejectedReason}"
        chore.assignee.equals("Unassigned", ignoreCase = true) -> "No owner • ${chore.dueLabel}"
        chore.status == ChoreItemStatus.Completed -> "$ownerLabel • ${completedLabel(chore.assignee, chore.completedByName)}"
        chore.status == ChoreItemStatus.PendingProof -> "$ownerLabel • ${chore.dueLabel}"
        chore.status == ChoreItemStatus.Overdue -> "$ownerLabel • ${chore.dueLabel}"
        else -> "$ownerLabel • ${chore.dueLabel}"
    }
}

private fun completedLabel(
    assigneeName: String,
    completedByName: String,
): String {
    val actor = completedByName.trim()
    return if (actor.isBlank() || actor.equals(assigneeName, ignoreCase = true)) {
        "Completed"
    } else {
        "Completed by $actor"
    }
}

private fun availableFilters(
    viewerMode: ChoreViewerMode,
): List<String> = when (viewerMode) {
    ChoreViewerMode.Admin -> listOf("All", "Proofs", "Active", "Inactive")
    ChoreViewerMode.Member -> listOf("All", "Mine", "Done", "Proof")
}

private fun sectionTitle(area: String): String = when {
    area.contains("kitchen", ignoreCase = true) -> "Kitchen"
    area.contains("outside", ignoreCase = true) -> "Outside"
    area.contains("bath", ignoreCase = true) -> "Bathroom"
    area.contains("hall", ignoreCase = true) -> "Entry"
    area.contains("living", ignoreCase = true) -> "Living room"
    else -> area
}

private fun roomSectionIcon(title: String) = when {
    title.contains("kitchen", ignoreCase = true) -> Icons.Rounded.Checklist
    title.contains("outside", ignoreCase = true) -> Icons.Rounded.ViewTimeline
    title.contains("bath", ignoreCase = true) -> Icons.Rounded.Shield
    else -> Icons.Rounded.Checklist
}

@Composable
private fun roomAccent(title: String): Color = when {
    title.contains("bath", ignoreCase = true) || title.contains("outside", ignoreCase = true) -> MaterialTheme.colorScheme.secondary
    else -> MaterialTheme.colorScheme.primary
}

@Composable
private fun rowAccent(
    chore: ChoreListItemUiModel,
): Color = when {
    chore.pendingApproval -> MaterialTheme.colorScheme.primary
    chore.status == ChoreItemStatus.Overdue -> MaterialTheme.colorScheme.error
    chore.area.contains("kitchen", ignoreCase = true) -> MaterialTheme.colorScheme.secondary
    chore.area.contains("bath", ignoreCase = true) -> MaterialTheme.colorScheme.tertiary
    chore.area.contains("bed", ignoreCase = true) -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.primary
}

@Composable
private fun statusColor(
    status: ChoreItemStatus,
    pendingApproval: Boolean,
): Color = if (pendingApproval) {
    MaterialTheme.colorScheme.primary
} else {
    when (status) {
        ChoreItemStatus.Completed -> MaterialTheme.colorScheme.secondary
        ChoreItemStatus.PendingProof -> MaterialTheme.colorScheme.primary
        ChoreItemStatus.Overdue -> MaterialTheme.colorScheme.error
        ChoreItemStatus.DueSoon -> MaterialTheme.colorScheme.secondary
        ChoreItemStatus.Scheduled -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

private fun statusLabel(
    status: ChoreItemStatus,
    pendingApproval: Boolean,
): String = if (pendingApproval) {
    "Review"
} else {
    when (status) {
        ChoreItemStatus.Completed -> "Done"
        ChoreItemStatus.PendingProof -> "Proof"
        ChoreItemStatus.Overdue -> "Overdue"
        ChoreItemStatus.DueSoon -> "Soon"
        ChoreItemStatus.Scheduled -> "Open"
    }
}

@Composable
private fun rowStatusColor(
    chore: ChoreListItemUiModel,
): Color = when {
    chore.reviewRejectedReason.isNotBlank() -> MaterialTheme.colorScheme.error
    !chore.isActive -> MaterialTheme.colorScheme.onSurfaceVariant
    chore.isUnassigned -> MaterialTheme.colorScheme.primary
    else -> statusColor(
        status = chore.status,
        pendingApproval = chore.pendingApproval,
    )
}

private fun rowStatusLabel(
    chore: ChoreListItemUiModel,
): String = when {
    chore.reviewRejectedReason.isNotBlank() -> "Sent back"
    !chore.isActive -> "Inactive"
    chore.isUnassigned -> "Needs owner"
    else -> statusLabel(
        status = chore.status,
        pendingApproval = chore.pendingApproval,
    )
}

@Composable
private fun AvailabilitySuggestionQueue(
    isLoading: Boolean,
    sections: List<ChoreAvailabilitySuggestionSectionUiModel>,
    onAccept: (String) -> Unit,
    onDismiss: (String) -> Unit,
) {
    ElevatedCard(
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoFixHigh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Availability coverage",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Automatic away-mode handoffs stay visible here so admins can keep or hide them.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            when {
                isLoading -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Checking the next 7 days for coverage gaps.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                else -> {
                    sections.forEach { section ->
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = section.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            section.suggestions.forEach { suggestion ->
                                AvailabilitySuggestionRow(
                                    suggestion = suggestion,
                                    onAccept = { onAccept(suggestion.id) },
                                    onDismiss = { onDismiss(suggestion.id) },
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
private fun AvailabilitySuggestionRow(
    suggestion: ChoreAvailabilitySuggestionUiModel,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = suggestion.choreTitle,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = "${suggestion.originalAssignee} -> ${suggestion.recommendedCoverMember}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = suggestion.reasonText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (suggestion.outsideNormalRotation) {
                Text(
                    text = "Suggested outside the usual rotation",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ElevatedButton(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Keep cover")
                }
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Dismiss")
                }
            }
        }
    }
}

@Preview(name = "Chores Screen Admin", widthDp = 393, heightDp = 852)
@Composable
private fun ChoresScreenAdminPreview() {
    ChorePilotPreviewTheme {
        CompactPreviewShell(selectedRoute = com.sortoapps.chorepilot.navigation.AppRoute.Chores) { modifier ->
            Box(modifier = modifier) {
                ChoresScreen(
                    state = ChorePilotPreviewData.ownerChoresState().copy(
                        autoAssignSuggestionsEnabled = true,
                        suggestionSections = listOf(
                            ChoreAvailabilitySuggestionSectionUiModel(
                                title = "Friday • 2026-04-10",
                                suggestions = listOf(
                                    ChoreAvailabilitySuggestionUiModel(
                                        id = "preview",
                                        choreId = "c1",
                                        choreTitle = "Trash reset",
                                        dateKey = "2026-04-10",
                                        weekdayLabel = "Friday",
                                        originalAssignee = "Erick",
                                        recommendedCoverMember = "Emma",
                                        reasonText = "Covering for Erick while they're away.",
                                        scopeStartDate = "2026-04-10",
                                        scopeEndDate = "2026-04-12",
                                        outsideNormalRotation = false,
                                    ),
                                ),
                            ),
                        ),
                    ),
                    onEvent = {},
                    onOpenChore = {},
                    onCreateChore = {},
                    onEditChore = {},
                    onOpenFoundSomething = {},
                )
            }
        }
    }
}

@Preview(name = "Chores Screen Member", widthDp = 393, heightDp = 852)
@Composable
private fun ChoresScreenMemberPreview() {
    ChorePilotPreviewTheme {
        CompactPreviewShell(selectedRoute = com.sortoapps.chorepilot.navigation.AppRoute.Chores) { modifier ->
            Box(modifier = modifier) {
                ChoresScreen(
                    state = ChorePilotPreviewData.memberChoresState(),
                    onEvent = {},
                    onOpenChore = {},
                    onCreateChore = {},
                    onEditChore = {},
                    onOpenFoundSomething = {},
                )
            }
        }
    }
}

@Preview(name = "Chores Suggestions Empty", widthDp = 393, heightDp = 852)
@Composable
private fun ChoresSuggestionsEmptyPreview() {
    ChorePilotPreviewTheme {
        CompactPreviewShell(selectedRoute = com.sortoapps.chorepilot.navigation.AppRoute.Chores) { modifier ->
            Box(modifier = modifier) {
                ChoresScreen(
                    state = ChorePilotPreviewData.ownerChoresState().copy(
                        autoAssignSuggestionsEnabled = true,
                        suggestionSections = emptyList(),
                    ),
                    onEvent = {},
                    onOpenChore = {},
                    onCreateChore = {},
                    onEditChore = {},
                    onOpenFoundSomething = {},
                )
            }
        }
    }
}

@Preview(name = "Chores Inactive Filter", widthDp = 393, heightDp = 852)
@Composable
private fun ChoresInactiveFilterPreview() {
    ChorePilotPreviewTheme {
        CompactPreviewShell(selectedRoute = com.sortoapps.chorepilot.navigation.AppRoute.Chores) { modifier ->
            Box(modifier = modifier) {
                ChoresScreen(
                    state = ChorePilotPreviewData.ownerChoresState().copy(
                        selectedFilter = "Inactive",
                        activeSections = emptyList(),
                        needsOwnerSections = emptyList(),
                        proofQueueSections = emptyList(),
                        suggestionSections = emptyList(),
                    ),
                    onEvent = {},
                    onOpenChore = {},
                    onCreateChore = {},
                    onEditChore = {},
                    onOpenFoundSomething = {},
                )
            }
        }
    }
}

@Preview(name = "Chores Discovery Review Queue", widthDp = 393, heightDp = 852)
@Composable
private fun ChoresDiscoveryReviewPreview() {
    ChorePilotPreviewTheme {
        CompactPreviewShell(selectedRoute = com.sortoapps.chorepilot.navigation.AppRoute.Chores) { modifier ->
            Box(modifier = modifier) {
                ChoresScreen(
                    state = ChorePilotPreviewData.ownerChoresState().copy(
                        selectedFilter = "All",
                        proofQueueSections = emptyList(),
                        suggestionSections = emptyList(),
                        activeSections = emptyList(),
                        inactiveSections = emptyList(),
                        needsOwnerSections = emptyList(),
                        pendingApprovalSections = listOf(
                            ChoreRoomSectionUiModel(
                                title = "Kitchen",
                                chores = listOf(
                                    ChoreListItemUiModel(
                                        id = "discovery-preview",
                                        title = "Reset the snack drawer",
                                        area = "Kitchen",
                                        cadence = "Every Friday",
                                        assignee = "Mila",
                                        isActive = true,
                                        isUnassigned = false,
                                        pendingApproval = true,
                                        assignmentReason = "Waiting for an admin to activate this chore.",
                                        submitterName = "Mila",
                                        reviewRejectedReason = "",
                                        completedByName = "",
                                        dueLabel = "Waiting for review",
                                        durationLabel = "10 min",
                                        status = ChoreItemStatus.Scheduled,
                                        tags = listOf("Kitchen", "Discovery"),
                                        isAssignedToViewer = false,
                                        canToggleCompletion = false,
                                    ),
                                ),
                            ),
                        ),
                    ),
                    onEvent = {},
                    onOpenChore = {},
                    onCreateChore = {},
                    onEditChore = {},
                    onOpenFoundSomething = {},
                )
            }
        }
    }
}
