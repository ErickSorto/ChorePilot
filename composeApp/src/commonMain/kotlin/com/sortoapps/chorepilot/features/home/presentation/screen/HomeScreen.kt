package com.sortoapps.chorepilot.features.home.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
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
import com.sortoapps.chorepilot.features.home.presentation.components.HomeAreaSection
import com.sortoapps.chorepilot.features.home.presentation.components.HomeFinishedRoomCard
import com.sortoapps.chorepilot.features.home.presentation.components.HomeModeTabs
import com.sortoapps.chorepilot.features.home.presentation.components.HomeNothingScheduledSection
import com.sortoapps.chorepilot.features.home.presentation.components.HomeScheduleReveal
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
    showScheduleHeader: Boolean = true,
    reserveScheduleToggleSpace: Boolean = false,
    onToggleScheduleHeader: (() -> Unit)? = null,
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
    val isAllDoneOnSelectedDay = state.focusChores.isNotEmpty() && activeTodayChores.isEmpty()
    val showFinishedRoomOnly = state.selectedSurface == HomeSurfaceMode.Today && isAllDoneOnSelectedDay
    val usesScheduleShade = reserveScheduleToggleSpace && onToggleScheduleHeader != null
    val topContentPadding by animateDpAsState(
        targetValue = when {
            !usesScheduleShade -> 10.dp
            showScheduleHeader -> 188.dp
            else -> 44.dp
        },
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "home-schedule-shade-content-padding",
    )

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

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = topContentPadding,
                    bottom = 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (!usesScheduleShade) {
                    item {
                        AnimatedVisibility(
                            visible = showScheduleHeader,
                            enter = expandVertically(
                                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
                                expandFrom = Alignment.Top,
                            ) + fadeIn(animationSpec = tween(durationMillis = 180)),
                            exit = shrinkVertically(
                                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                                shrinkTowards = Alignment.Top,
                            ) + fadeOut(animationSpec = tween(durationMillis = 120)),
                        ) {
                            HomeScheduleReveal(
                                headline = state.headline,
                                message = state.supportingText,
                                days = state.days,
                                onDaySelected = {
                                    onEvent(HomeUiEvent.TriggerVibration)
                                    onEvent(HomeUiEvent.DaySelected(it))
                                },
                                modifier = Modifier.padding(bottom = 10.dp),
                            )
                        }
                    }
                }

                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
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
                }

                if (showFinishedRoomOnly) {
                    item {
                        HomeFinishedRoomCard(
                            modifier = Modifier.padding(horizontal = 16.dp),
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
                            showIllustration = false,
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

                showFinishedRoomOnly -> Unit

                else -> when (state.selectedSurface) {
                HomeSurfaceMode.Today -> {
                    if (groupedChores.isEmpty()) {
                        if (!isAllDoneOnSelectedDay) {
                            item {
                                HomeNothingScheduledSection(
                                    drawable = Res.drawable.m1_empty_no_chores,
                                    title = if (selectedDay?.isToday == true) {
                                        "Nothing scheduled right now."
                                    } else {
                                        "No chores planned for ${selectedDay?.fullLabel ?: state.selectedDayLabel}."
                                    },
                                    body = if (selectedDay?.isToday == true) {
                                        "Enjoy your free time! We'll notify you when new chores are added."
                                    } else {
                                        "That day is clear for now. We'll notify you when new chores are added."
                                    },
                                )
                            }
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

            if (usesScheduleShade) {
                HomeSchedulePullDownShade(
                    headline = state.headline,
                    message = state.supportingText,
                    days = state.days,
                    expanded = showScheduleHeader,
                    onDaySelected = {
                        onEvent(HomeUiEvent.TriggerVibration)
                        onEvent(HomeUiEvent.DaySelected(it))
                    },
                    onToggle = requireNotNull(onToggleScheduleHeader),
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        }
    }
}

@Composable
private fun HomeSchedulePullDownShade(
    headline: String,
    message: String,
    days: List<HomeDayUiModel>,
    expanded: Boolean,
    onDaySelected: (String) -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(2f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
                expandFrom = Alignment.Top,
            ) + fadeIn(animationSpec = tween(durationMillis = 180)),
            exit = shrinkVertically(
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                shrinkTowards = Alignment.Top,
            ) + fadeOut(animationSpec = tween(durationMillis = 120)),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(
                    bottomStart = 20.dp,
                    bottomEnd = 20.dp,
                ),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f),
                ),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                HomeScheduleReveal(
                    headline = headline,
                    message = message,
                    days = days,
                    onDaySelected = onDaySelected,
                    modifier = Modifier.padding(top = 10.dp, bottom = 10.dp),
                )
            }
        }
        HomeScheduleHeaderToggle(
            expanded = expanded,
            onClick = onToggle,
            modifier = Modifier.zIndex(3f),
        )
    }
}

@Composable
private fun HomeScheduleHeaderToggle(
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val animationSpec = tween<androidx.compose.ui.unit.Dp>(
        durationMillis = 240,
        easing = FastOutSlowInEasing,
    )
    val tabWidth by animateDpAsState(
        targetValue = if (expanded) 56.dp else 50.dp,
        animationSpec = animationSpec,
        label = "home-calendar-toggle-width",
    )
    val tabHeight by animateDpAsState(
        targetValue = if (expanded) 32.dp else 28.dp,
        animationSpec = animationSpec,
        label = "home-calendar-toggle-height",
    )
    val containerColor by animateColorAsState(
        targetValue = if (expanded) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
        },
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "home-calendar-toggle-container",
    )
    val borderColor by animateColorAsState(
        targetValue = if (expanded) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)
        },
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "home-calendar-toggle-border",
    )
    val iconColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.primary,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "home-calendar-toggle-icon",
    )
    val tabShape = RoundedCornerShape(
        topStart = 0.dp,
        topEnd = 0.dp,
        bottomStart = 18.dp,
        bottomEnd = 18.dp,
    )

    Surface(
        modifier = modifier
            .zIndex(1f)
            .width(tabWidth)
            .height(tabHeight)
            .clip(tabShape)
            .clickable(onClick = onClick),
        shape = tabShape,
        color = containerColor,
        border = BorderStroke(
            width = 1.dp,
            color = borderColor,
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Rounded.CalendarMonth,
                contentDescription = if (expanded) "Hide date picker" else "Show date picker",
                tint = iconColor,
                modifier = Modifier.size(18.dp),
            )
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

@Preview(name = "Home Screen Condensed", widthDp = 393, heightDp = 852)
@Composable
private fun HomeScreenCondensedPreview() {
    ChorePilotPreviewTheme {
        CompactPreviewShell(selectedRoute = com.sortoapps.chorepilot.navigation.AppRoute.Home) { modifier ->
            Column(modifier = modifier) {
                HomeScreen(
                    state = ChorePilotPreviewData.homeState(),
                    onEvent = {},
                    onOpenChoreCapture = { _, _ -> },
                    onOpenChore = {},
                    showScheduleHeader = false,
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
