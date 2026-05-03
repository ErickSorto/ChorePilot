package com.sortoapps.chorepilot.features.home.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAddCheck
import androidx.compose.material.icons.rounded.Bathtub
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Kitchen
import androidx.compose.material.icons.rounded.Weekend
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.sortoapps.chorepilot.features.auth.domain.HouseholdRole
import com.sortoapps.chorepilot.features.chores.domain.ChoreItem
import com.sortoapps.chorepilot.features.chores.domain.ChoreItemStatus
import com.sortoapps.chorepilot.features.chores.domain.canUndoChoreCompletion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeAreaSection(
    title: String,
    chores: List<ChoreItem>,
    animatedRemovalChoreIds: Set<String>,
    viewerId: String,
    viewerName: String,
    viewerRole: HouseholdRole,
    onToggleComplete: (String) -> Unit,
    onOpenProofCapture: (String) -> Unit,
    onOpenChore: (String) -> Unit,
) {
    val accent = areaAccent(title)
    var renderedChores by remember(title) { mutableStateOf(chores) }
    var visibleChoreIds by remember(title) { mutableStateOf(chores.map { it.id }.toSet()) }

    LaunchedEffect(chores) {
        val currentIds = chores.map { it.id }.toSet()
        val existingById = renderedChores.associateBy { it.id }
        renderedChores = buildList {
            addAll(chores)
            addAll(renderedChores.filter { it.id !in currentIds })
        }.distinctBy { it.id }
        visibleChoreIds = visibleChoreIds + currentIds

        val removedIds = existingById.keys - currentIds
        removedIds.forEach { removedId ->
            val shouldAnimateRemoval = removedId in animatedRemovalChoreIds
            if (shouldAnimateRemoval) {
                visibleChoreIds = visibleChoreIds - removedId
                launch {
                    delay(220)
                    renderedChores = renderedChores.filterNot { it.id == removedId }
                }
            } else {
                visibleChoreIds = visibleChoreIds - removedId
                renderedChores = renderedChores.filterNot { it.id == removedId }
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.animateContentSize(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = sectionIcon(title),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = "${chores.size} task${if (chores.size == 1) "" else "s"}",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.92f),
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            renderedChores.forEach { chore ->
                AnimatedVisibility(
                    visible = chore.id in visibleChoreIds,
                    enter = fadeIn(animationSpec = tween(durationMillis = 160)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 140)) + shrinkVertically(animationSpec = tween(durationMillis = 220)),
                ) {
                    HomeChoreRow(
                        chore = chore,
                        accent = accent,
                        viewerId = viewerId,
                        viewerName = viewerName,
                        viewerRole = viewerRole,
                        onToggleComplete = { onToggleComplete(chore.id) },
                        onOpenProofCapture = { onOpenProofCapture(chore.id) },
                        onOpen = { onOpenChore(chore.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeChoreRow(
    chore: ChoreItem,
    accent: Color,
    viewerId: String,
    viewerName: String,
    viewerRole: HouseholdRole,
    onToggleComplete: () -> Unit,
    onOpenProofCapture: () -> Unit,
    onOpen: () -> Unit,
) {
    val completed = chore.status == ChoreItemStatus.Completed
    val pendingProof = chore.status == ChoreItemStatus.PendingProof
    val assignedToViewer = chore.assignee.equals(viewerName, ignoreCase = true)
    val canToggle = if (chore.status == ChoreItemStatus.Completed || chore.status == ChoreItemStatus.PendingProof) {
        canUndoChoreCompletion(chore, viewerId, viewerName, viewerRole)
    } else {
        true
    }
    val rowColor = when {
        pendingProof -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
    }
    val ownerLabel = if (assignedToViewer) "Me" else chore.assignee

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        color = rowColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = if (assignedToViewer) 0.18f else 0.08f),
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
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
                    .background(accent.copy(alpha = if (completed) 0.32f else 0.88f)),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = chore.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (assignedToViewer) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (completed) TextDecoration.LineThrough else TextDecoration.None,
                )
                Text(
                    text = homeSupportingLine(
                        ownerLabel = ownerLabel,
                        dueLabel = chore.dueLabel,
                        assigneeName = chore.assignee,
                        completedByName = chore.completedByName,
                        completed = completed,
                        pendingProof = pendingProof,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            CompletionButton(
                completed = completed,
                pending = pendingProof,
                enabled = canToggle,
                accent = accent,
                onClick = if (chore.proofRequired && !completed && chore.status != ChoreItemStatus.PendingProof) {
                    onOpenProofCapture
                } else {
                    onToggleComplete
                },
            )
        }
    }
}

@Composable
private fun CompletionButton(
    completed: Boolean,
    pending: Boolean,
    enabled: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    val settledScale by animateFloatAsState(
        targetValue = if (completed) 1f else 0.92f,
        animationSpec = tween(durationMillis = 180),
        label = "home_completion_scale",
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
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.92f)),
                    )
                }
            }
        }
        CompletionPopEffect(
            completed = completed,
            tint = accent,
            iconSize = 18.dp,
        )
    }
}

private fun homeSupportingLine(
    ownerLabel: String,
    dueLabel: String,
    assigneeName: String,
    completedByName: String,
    completed: Boolean,
    pendingProof: Boolean,
): String = when {
    completed -> "$ownerLabel • ${homeCompletedLabel(assigneeName, completedByName)}"
    pendingProof -> "$ownerLabel • Approval pending"
    else -> "$ownerLabel • $dueLabel"
}

private fun homeCompletedLabel(
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

internal fun sectionTitle(area: String): String = when {
    area.contains("kitchen", ignoreCase = true) -> "Kitchen"
    area.contains("outside", ignoreCase = true) -> "Outside"
    area.contains("bath", ignoreCase = true) -> "Bathroom"
    area.contains("living", ignoreCase = true) -> "Living Room"
    area.contains("hall", ignoreCase = true) -> "Entry"
    else -> area
}

private fun sectionIcon(title: String): ImageVector = when {
    title.contains("kitchen", ignoreCase = true) -> Icons.Rounded.Kitchen
    title.contains("outside", ignoreCase = true) -> Icons.Rounded.Weekend
    title.contains("bath", ignoreCase = true) -> Icons.Rounded.Bathtub
    title.contains("entry", ignoreCase = true) || title.contains("living", ignoreCase = true) -> Icons.Rounded.Home
    else -> Icons.AutoMirrored.Rounded.PlaylistAddCheck
}

@Composable
@ReadOnlyComposable
private fun areaAccent(title: String): Color = when {
    title.contains("kitchen", ignoreCase = true) -> Color(0xFF5F5BC8)
    title.contains("bath", ignoreCase = true) -> Color(0xFF5B5BC8)
    title.contains("outside", ignoreCase = true) -> Color(0xFF28A38B)
    title.contains("entry", ignoreCase = true) -> Color(0xFF5E7CB1)
    title.contains("bed", ignoreCase = true) -> Color(0xFF79B8F5)
    else -> Color(0xFF8F8CF2)
}
