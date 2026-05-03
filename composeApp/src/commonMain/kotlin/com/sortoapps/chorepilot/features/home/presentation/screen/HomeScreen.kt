package com.sortoapps.chorepilot.features.home.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import chorepilot.composeapp.generated.resources.Res
import chorepilot.composeapp.generated.resources.m1_empty_no_chores
import chorepilot.composeapp.generated.resources.m1_empty_no_proof
import chorepilot.composeapp.generated.resources.m1_success_celebration
import com.sortoapps.chorepilot.core.designsystem.ChorePilotGradientBackground
import com.sortoapps.chorepilot.core.preview.ChorePilotPreviewData
import com.sortoapps.chorepilot.core.preview.ChorePilotPreviewTheme
import com.sortoapps.chorepilot.core.preview.CompactPreviewShell
import com.sortoapps.chorepilot.features.chores.domain.ChoreItemStatus
import com.sortoapps.chorepilot.features.home.presentation.components.DailySnapshotCard
import com.sortoapps.chorepilot.features.home.presentation.components.DayCarousel
import com.sortoapps.chorepilot.features.home.presentation.components.GreetingSection
import com.sortoapps.chorepilot.features.home.presentation.components.HomeAreaSection
import com.sortoapps.chorepilot.features.home.presentation.components.HomeModeTabs
import com.sortoapps.chorepilot.features.home.presentation.components.StateSection
import com.sortoapps.chorepilot.features.home.presentation.components.WeeklyStatusCard
import com.sortoapps.chorepilot.features.home.presentation.components.sectionTitle
import com.sortoapps.chorepilot.features.home.presentation.viewmodel.HomeDayUiModel
import com.sortoapps.chorepilot.features.home.presentation.viewmodel.HomeSurfaceMode
import com.sortoapps.chorepilot.features.home.presentation.viewmodel.HomeCompletionPromptKind
import com.sortoapps.chorepilot.features.home.presentation.viewmodel.HomeUiEvent
import com.sortoapps.chorepilot.features.home.presentation.viewmodel.HomeReassignmentSuggestionUiModel
import com.sortoapps.chorepilot.features.home.presentation.viewmodel.HomeUiState

@Composable
fun HomeScreen(
    state: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    onOpenChoreCapture: (String, HomeDayUiModel) -> Unit,
    onOpenChore: (String) -> Unit,
) {
    val activeTodayChores = state.focusChores.filter { it.status != ChoreItemStatus.Completed }
    val groupedChores = activeTodayChores.groupBy { sectionTitle(it.area) }
    val myChores = state.focusChores.filter { it.assignee.equals(state.viewerName, ignoreCase = true) }
    val completedChores = state.focusChores.filter { it.status == ChoreItemStatus.Completed }
    val proofChores = state.focusChores.filter {
        it.proofPhotoCount > 0 &&
            (it.status == ChoreItemStatus.PendingProof || it.status == ChoreItemStatus.Completed)
    }
    val selectedDay = state.days.firstOrNull { it.isSelected }

    ChorePilotGradientBackground {
        state.completionPrompt?.let { prompt ->
            AlertDialog(
                onDismissRequest = { onEvent(HomeUiEvent.DismissCompletionPrompt) },
                title = {
                    Text(
                        text = when (prompt.kind) {
                            HomeCompletionPromptKind.CompleteForAnotherMember -> "Looks like this belongs to someone else."
                            HomeCompletionPromptKind.RemoveSubmittedProof -> "Remove submitted proof?"
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                text = {
                    Text(
                        text = when (prompt.kind) {
                            HomeCompletionPromptKind.CompleteForAnotherMember ->
                                "Complete \"${prompt.choreTitle}\" for ${prompt.assigneeName}?"
                            HomeCompletionPromptKind.RemoveSubmittedProof ->
                                "Unchecking \"${prompt.choreTitle}\" will delete the proof uploaded for this day."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onEvent(HomeUiEvent.TriggerVibration)
                            onEvent(
                                when (prompt.kind) {
                                    HomeCompletionPromptKind.CompleteForAnotherMember -> HomeUiEvent.ConfirmCompletionForAnotherMember
                                    HomeCompletionPromptKind.RemoveSubmittedProof -> HomeUiEvent.ConfirmProofRemoval
                                },
                            )
                        },
                    ) {
                        Text(
                            when (prompt.kind) {
                                HomeCompletionPromptKind.CompleteForAnotherMember -> "Complete for them"
                                HomeCompletionPromptKind.RemoveSubmittedProof -> "Delete proof"
                            },
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            onEvent(HomeUiEvent.TriggerVibration)
                            onEvent(HomeUiEvent.DismissCompletionPrompt)
                        },
                    ) {
                        Text("Cancel")
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                GreetingSection(
                    eyebrow = state.supportingText,
                    headline = state.headline,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            item {
                DayCarousel(
                    days = state.days,
                    onDaySelected = {
                        onEvent(HomeUiEvent.TriggerVibration)
                        onEvent(HomeUiEvent.DaySelected(it))
                    },
                )
            }

            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DailySnapshotCard(
                        chores = state.focusChores,
                        snapshotTitle = when {
                            selectedDay?.isToday == true -> "Today"
                            selectedDay != null -> selectedDay.fullLabel
                            else -> state.selectedDayLabel
                        },
                    )
                    HomeModeTabs(
                        selectedSurface = state.selectedSurface,
                        onSurfaceSelected = {
                            onEvent(HomeUiEvent.TriggerVibration)
                            onEvent(HomeUiEvent.SurfaceSelected(it))
                        },
                    )
                }
            }

            if (
                state.viewerRole != com.sortoapps.chorepilot.features.auth.domain.HouseholdRole.Member &&
                state.autoAssignSuggestionsEnabled &&
                (state.isLoadingReassignmentSuggestions || state.reassignmentSuggestions.isNotEmpty())
            ) {
                item {
                    CoverageNeededCard(
                        isLoading = state.isLoadingReassignmentSuggestions,
                        dayLabel = selectedDay?.fullLabel ?: state.selectedDayLabel,
                        suggestions = state.reassignmentSuggestions,
                        onAccept = { suggestionId ->
                            onEvent(HomeUiEvent.TriggerVibrationSuccess)
                            onEvent(HomeUiEvent.ReassignmentAccepted(suggestionId))
                        },
                        onDismiss = { suggestionId ->
                            onEvent(HomeUiEvent.TriggerVibration)
                            onEvent(HomeUiEvent.ReassignmentDismissed(suggestionId))
                        },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            when {
                state.isLoading && state.focusChores.isEmpty() -> {
                    item {
                        StateSection(
                            drawable = Res.drawable.m1_empty_no_chores,
                            title = "Loading your household...",
                            body = "We’re pulling in your chores, approvals, and this week’s rhythm.",
                        )
                    }
                }

                state.errorMessage != null && state.focusChores.isEmpty() -> {
                    item {
                        StateSection(
                            drawable = Res.drawable.m1_empty_no_chores,
                            title = "We couldn't load the dashboard.",
                            body = state.errorMessage,
                        )
                    }
                }

                else -> when (state.selectedSurface) {
                HomeSurfaceMode.Today -> {
                    if (groupedChores.isEmpty()) {
                        item {
                            StateSection(
                                drawable = Res.drawable.m1_empty_no_chores,
                                title = if (selectedDay?.isToday == true) {
                                    "Nothing scheduled right now."
                                } else {
                                    "No chores planned for ${selectedDay?.fullLabel ?: state.selectedDayLabel}."
                                },
                                body = if (selectedDay?.isToday == true) {
                                    "Enjoy the calm. New chores and proofs will appear here as the household updates."
                                } else {
                                    "That day is clear for now."
                                },
                            )
                        }
                    } else {
                        item {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(22.dp),
                            ) {
                                groupedChores.forEach { (_, chores) ->
                                    HomeAreaSection(
                                        title = sectionTitle(chores.first().area),
                                        chores = chores,
                                        animatedRemovalChoreIds = state.animatedRemovalChoreIds,
                                        viewerId = state.viewerId,
                                        viewerName = state.viewerName,
                                        viewerRole = state.viewerRole,
                                        onToggleComplete = { choreId ->
                                            onEvent(HomeUiEvent.TriggerVibration)
                                            onEvent(HomeUiEvent.ToggleChoreCompletion(choreId))
                                        },
                                        onOpenProofCapture = { choreId ->
                                            onEvent(HomeUiEvent.TriggerVibration)
                                            selectedDay?.let { onOpenChoreCapture(choreId, it) }
                                        },
                                        onOpenChore = onOpenChore,
                                    )
                                }
                                WeeklyStatusCard(metric = state.metrics.lastOrNull())
                            }
                        }
                    }
                }

                HomeSurfaceMode.Celebration -> {
                    if (myChores.isEmpty()) {
                        item {
                            StateSection(
                                drawable = Res.drawable.m1_empty_no_chores,
                                title = "Nothing assigned to you.",
                                body = "Your personal list is clear right now.",
                            )
                        }
                    } else {
                        item {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(22.dp),
                            ) {
                                myChores
                                    .groupBy { sectionTitle(it.area) }
                                    .forEach { (_, chores) ->
                                        HomeAreaSection(
                                            title = sectionTitle(chores.first().area),
                                            chores = chores,
                                            animatedRemovalChoreIds = state.animatedRemovalChoreIds,
                                            viewerId = state.viewerId,
                                            viewerName = state.viewerName,
                                            viewerRole = state.viewerRole,
                                            onToggleComplete = { choreId ->
                                                onEvent(HomeUiEvent.TriggerVibration)
                                                onEvent(HomeUiEvent.ToggleChoreCompletion(choreId))
                                            },
                                            onOpenProofCapture = { choreId ->
                                                onEvent(HomeUiEvent.TriggerVibration)
                                                selectedDay?.let { onOpenChoreCapture(choreId, it) }
                                            },
                                            onOpenChore = onOpenChore,
                                        )
                                    }
                            }
                        }
                    }
                }

                HomeSurfaceMode.NoChores -> {
                    if (completedChores.isEmpty()) {
                        item {
                            StateSection(
                                drawable = Res.drawable.m1_success_celebration,
                                title = "Nothing completed yet.",
                                body = "Completed chores will land here.",
                            )
                        }
                    } else {
                        item {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(22.dp),
                            ) {
                                completedChores
                                    .groupBy { sectionTitle(it.area) }
                                    .forEach { (_, chores) ->
                                        HomeAreaSection(
                                            title = sectionTitle(chores.first().area),
                                            chores = chores,
                                            animatedRemovalChoreIds = state.animatedRemovalChoreIds,
                                            viewerId = state.viewerId,
                                            viewerName = state.viewerName,
                                            viewerRole = state.viewerRole,
                                            onToggleComplete = { choreId ->
                                                onEvent(HomeUiEvent.TriggerVibration)
                                                onEvent(HomeUiEvent.ToggleChoreCompletion(choreId))
                                            },
                                            onOpenProofCapture = { choreId ->
                                                onEvent(HomeUiEvent.TriggerVibration)
                                                selectedDay?.let { onOpenChoreCapture(choreId, it) }
                                            },
                                            onOpenChore = onOpenChore,
                                        )
                                    }
                            }
                        }
                    }
                }

                HomeSurfaceMode.ProofQueue -> {
                    if (proofChores.isEmpty()) {
                        item {
                            StateSection(
                                drawable = Res.drawable.m1_empty_no_proof,
                                title = "No proof submitted yet.",
                                body = "Submitted proof for this day will appear here.",
                            )
                        }
                    } else {
                        item {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(22.dp),
                            ) {
                                proofChores
                                    .groupBy { sectionTitle(it.area) }
                                    .forEach { (_, chores) ->
                                        HomeAreaSection(
                                            title = sectionTitle(chores.first().area),
                                            chores = chores,
                                            animatedRemovalChoreIds = state.animatedRemovalChoreIds,
                                            viewerId = state.viewerId,
                                            viewerName = state.viewerName,
                                            viewerRole = state.viewerRole,
                                            onToggleComplete = { choreId ->
                                                onEvent(HomeUiEvent.TriggerVibration)
                                                onEvent(HomeUiEvent.ToggleChoreCompletion(choreId))
                                            },
                                            onOpenProofCapture = { choreId ->
                                                onEvent(HomeUiEvent.TriggerVibration)
                                                selectedDay?.let { onOpenChoreCapture(choreId, it) }
                                            },
                                            onOpenChore = onOpenChore,
                                        )
                                    }
                            }
                        }
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun CoverageNeededCard(
    isLoading: Boolean,
    dayLabel: String,
    suggestions: List<HomeReassignmentSuggestionUiModel>,
    onAccept: (String) -> Unit,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Coverage needed",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Smart reassignment checks for $dayLabel stay visible here for admins.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            when {
                isLoading -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(modifier = Modifier.padding(top = 2.dp), strokeWidth = 2.dp)
                        Text(
                            text = "Checking who might need coverage right now.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                else -> {
                    suggestions.forEach { suggestion ->
                        ElevatedCard(
                            shape = RoundedCornerShape(22.dp),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(
                                    text = suggestion.title,
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    text = "${suggestion.originalAssignee} -> ${suggestion.coverMember}",
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
                                        onClick = { onAccept(suggestion.id) },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text("Keep this cover")
                                    }
                                    OutlinedButton(
                                        onClick = { onDismiss(suggestion.id) },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text("Dismiss")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Home Screen", widthDp = 393, heightDp = 852)
@Composable
private fun HomeScreenPreview() {
    ChorePilotPreviewTheme {
        CompactPreviewShell(selectedRoute = com.sortoapps.chorepilot.navigation.AppRoute.Home) { modifier ->
            Column(modifier = modifier) {
                HomeScreen(
                    state = ChorePilotPreviewData.homeState().copy(
                        autoAssignSuggestionsEnabled = true,
                        reassignmentSuggestions = listOf(
                            HomeReassignmentSuggestionUiModel(
                                id = "preview",
                                choreId = "c1",
                                title = "Trash reset",
                                dateKey = "2026-04-10",
                                originalAssignee = "Erick",
                                coverMember = "Emma",
                                reasonText = "Covering for Erick while they're away.",
                                weekdayLabel = "Friday",
                                scopeStartDate = "2026-04-10",
                                scopeEndDate = "2026-04-12",
                                outsideNormalRotation = false,
                            ),
                        ),
                    ),
                    onEvent = {},
                    onOpenChoreCapture = { _, _ -> },
                    onOpenChore = {},
                )
            }
        }
    }
}

@Preview(name = "Home Coverage Empty", widthDp = 393, heightDp = 852)
@Composable
private fun HomeCoverageEmptyPreview() {
    ChorePilotPreviewTheme {
        CompactPreviewShell(selectedRoute = com.sortoapps.chorepilot.navigation.AppRoute.Home) { modifier ->
            Column(modifier = modifier) {
                HomeScreen(
                    state = ChorePilotPreviewData.homeState().copy(
                        autoAssignSuggestionsEnabled = true,
                        reassignmentSuggestions = emptyList(),
                    ),
                    onEvent = {},
                    onOpenChoreCapture = { _, _ -> },
                    onOpenChore = {},
                )
            }
        }
    }
}
