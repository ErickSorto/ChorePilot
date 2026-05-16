package com.sortoapps.chorepilot.features.feedbackpal.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.sortoapps.chorepilot.core.designsystem.ChorePilotGradientBackground
import com.sortoapps.chorepilot.core.preview.ChorePilotPreviewTheme
import com.sortoapps.chorepilot.core.ui.SnackbarController
import com.sortoapps.chorepilot.core.ui.SnackbarEvent
import com.sortoapps.chorepilot.core.ui.StringValue
import com.sortoapps.chorepilot.features.feedbackpal.domain.FeedbackIntent
import com.sortoapps.chorepilot.features.feedbackpal.domain.FeedbackPalKind
import com.sortoapps.chorepilot.features.feedbackpal.domain.FeedbackPalPose
import com.sortoapps.chorepilot.features.feedbackpal.domain.FeedbackPalRepository
import com.sortoapps.chorepilot.features.feedbackpal.domain.FeedbackPalTriageResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun FeedbackPalHost(
    selectedPal: FeedbackPalKind,
    modifier: Modifier = Modifier,
    screenRoute: String = "Main",
    bottomPadding: Dp = 104.dp,
    endPadding: Dp = 18.dp,
    feedbackPalRepository: FeedbackPalRepository = koinInject(),
) {
    var sheetOpen by remember { mutableStateOf(false) }
    var lastDraftSaved by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }
    var triageResult by remember { mutableStateOf<FeedbackPalTriageResult?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(10f),
    ) {
        DraggableFeedbackPal(
            selectedPal = selectedPal,
            lastDraftSaved = lastDraftSaved,
            bottomPadding = bottomPadding,
            endPadding = endPadding,
            onOpenSheet = { sheetOpen = true },
        )
    }

    if (sheetOpen) {
        FeedbackPalSheet(
            selectedPal = selectedPal,
            screenRoute = screenRoute,
            isSubmitting = isSubmitting,
            submitError = submitError,
            triageResult = triageResult,
            onDismiss = { sheetOpen = false },
            onSubmit = { intent, message ->
                coroutineScope.launch {
                    isSubmitting = true
                    submitError = null
                    runCatching {
                        feedbackPalRepository.submitFeedback(
                            reportedIntent = intent,
                            message = message,
                            screenRoute = screenRoute,
                            palKind = selectedPal,
                        )
                    }.onSuccess { result ->
                        triageResult = result
                        lastDraftSaved = true
                        SnackbarController.sendEvent(
                            SnackbarEvent(
                                message = StringValue.DynamicString("Feedback sent: ${result.type} ${result.priority}."),
                            ),
                        )
                    }.onFailure { error ->
                        submitError = error.message ?: "Feedback could not be sent right now."
                    }
                    isSubmitting = false
                }
            },
        )
    }
}

@Composable
private fun DraggableFeedbackPal(
    selectedPal: FeedbackPalKind,
    lastDraftSaved: Boolean,
    bottomPadding: Dp,
    endPadding: Dp,
    onOpenSheet: () -> Unit,
) {
    val density = LocalDensity.current
    val interactionSource = remember { MutableInteractionSource() }
    var position by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var placed by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    var pose by remember { mutableStateOf(FeedbackPalPose.Front) }
    var dragPoseDistance by remember { mutableFloatStateOf(0f) }
    var bubbleVisible by remember { mutableStateOf(false) }
    var bubbleMessage by remember { mutableStateOf("I'm here to help!") }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val spriteSize = 86.dp
        val edgePaddingPx = with(density) { endPadding.toPx() }
        val topPaddingPx = with(density) { 18.dp.toPx() }
        val bottomPaddingPx = with(density) { bottomPadding.toPx() }
        val spritePx = with(density) { spriteSize.toPx() }
        val containerWidthPx = with(density) { maxWidth.toPx() }
        val containerHeightPx = with(density) { maxHeight.toPx() }
        val minX = edgePaddingPx
        val maxX = (containerWidthPx - spritePx - edgePaddingPx).coerceAtLeast(minX)
        val minY = topPaddingPx
        val maxY = (containerHeightPx - spritePx - bottomPaddingPx).coerceAtLeast(minY)
        val poseSwitchThresholdPx = with(density) { 9.dp.toPx() }

        LaunchedEffect(containerWidthPx, containerHeightPx, bottomPaddingPx, edgePaddingPx) {
            if (!placed) {
                position = androidx.compose.ui.geometry.Offset(maxX, maxY)
                placed = true
            } else {
                position = position.copy(
                    x = position.x.coerceIn(minX, maxX),
                    y = position.y.coerceIn(minY, maxY),
                )
            }
        }

        LaunchedEffect(selectedPal, lastDraftSaved) {
            delay(1400)
            bubbleMessage = if (lastDraftSaved) {
                "Got it. I'll keep that safe."
            } else {
                "I'm here to help!"
            }
            bubbleVisible = true
            delay(3600)
            bubbleVisible = false
            while (true) {
                delay(18000)
                bubbleMessage = if (lastDraftSaved) {
                    "Nice note. A human can review it."
                } else {
                    listOf(
                        "I'm here to help!",
                        "Tap me for bugs or ideas.",
                        "Great job. Keep going!",
                    ).random()
                }
                bubbleVisible = true
                delay(4200)
                bubbleVisible = false
            }
        }

        val animatedX by animateFloatAsState(
            targetValue = position.x,
            animationSpec = if (isDragging) {
                tween(durationMillis = 0)
            } else {
                spring(dampingRatio = 0.72f, stiffness = 320f)
            },
            label = "feedback-pal-x",
        )
        val animatedY by animateFloatAsState(
            targetValue = position.y,
            animationSpec = if (isDragging) {
                tween(durationMillis = 0)
            } else {
                spring(dampingRatio = 0.72f, stiffness = 320f)
            },
            label = "feedback-pal-y",
        )
        val onLeftEdge = animatedX < containerWidthPx / 2f

        FeedbackPalSpeechBubble(
            visible = bubbleVisible && !isDragging,
            message = bubbleMessage,
            onLeftEdge = onLeftEdge,
            modifier = Modifier.offset {
                val bubbleWidthPx = with(density) { 164.dp.toPx() }
                val bubbleY = (animatedY - with(density) { 12.dp.toPx() }).coerceIn(8f, maxY)
                val bubbleX = if (onLeftEdge) {
                    (animatedX + spritePx - with(density) { 34.dp.toPx() })
                        .coerceAtMost(containerWidthPx - bubbleWidthPx - edgePaddingPx)
                } else {
                    (animatedX - bubbleWidthPx + with(density) { 34.dp.toPx() })
                        .coerceAtLeast(edgePaddingPx)
                }
                IntOffset(bubbleX.roundToInt(), bubbleY.roundToInt())
            },
        )

        FeedbackPalSprite(
            kind = selectedPal,
            pose = pose,
            size = spriteSize,
            shadowElevation = 0.dp,
            modifier = Modifier
                .offset { IntOffset(animatedX.roundToInt(), animatedY.roundToInt()) }
                .pointerInput(containerWidthPx, containerHeightPx) {
                    detectDragGestures(
                        onDragStart = {
                            isDragging = true
                            bubbleVisible = false
                            dragPoseDistance = 0f
                        },
                        onDragEnd = {
                            isDragging = false
                            val leftDistance = abs(position.x - minX)
                            val rightDistance = abs(maxX - position.x)
                            val snapX = if (leftDistance < rightDistance) minX else maxX
                            pose = if (snapX < position.x) FeedbackPalPose.Left else FeedbackPalPose.Right
                            position = position.copy(x = snapX)
                        },
                        onDragCancel = {
                            isDragging = false
                            pose = FeedbackPalPose.Front
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragPoseDistance += abs(dragAmount.x) + abs(dragAmount.y)
                            if (dragPoseDistance >= poseSwitchThresholdPx) {
                                pose = when {
                                    dragAmount.y < -abs(dragAmount.x) -> FeedbackPalPose.Jump
                                    dragAmount.x < -1f -> FeedbackPalPose.Left
                                    dragAmount.x > 1f -> FeedbackPalPose.Right
                                    else -> pose
                                }
                                dragPoseDistance = 0f
                            }
                            position = androidx.compose.ui.geometry.Offset(
                                x = (position.x + dragAmount.x).coerceIn(minX, maxX),
                                y = (position.y + dragAmount.y).coerceIn(minY, maxY),
                            )
                        },
                    )
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onOpenSheet,
                ),
        )

        LaunchedEffect(isDragging, position.x) {
            if (!isDragging) {
                delay(520)
                pose = FeedbackPalPose.Front
            }
        }
    }
}

@Composable
private fun FeedbackPalSpeechBubble(
    visible: Boolean,
    message: String,
    onLeftEdge: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(180)) + scaleIn(
            initialScale = 0.92f,
            animationSpec = tween(220, easing = FastOutSlowInEasing),
        ),
        exit = fadeOut(animationSpec = tween(140)) + scaleOut(
            targetScale = 0.94f,
            animationSpec = tween(140),
        ),
        modifier = modifier,
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 164.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            tonalElevation = 0.dp,
            shadowElevation = 8.dp,
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedbackPalSheet(
    selectedPal: FeedbackPalKind,
    screenRoute: String,
    isSubmitting: Boolean,
    submitError: String?,
    triageResult: FeedbackPalTriageResult?,
    onDismiss: () -> Unit,
    onSubmit: (FeedbackIntent, String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedIntent by remember { mutableStateOf(FeedbackIntent.Bug) }
    var message by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 18.dp, vertical = 8.dp)
                .widthIn(max = 720.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FeedbackPalSheetHeader(
                selectedPal = selectedPal,
                onDismiss = onDismiss,
            )
            FeedbackIntentSelector(
                selectedIntent = selectedIntent,
                onSelect = { intent ->
                    selectedIntent = intent
                    if (message.isBlank()) {
                        message = intent.starterPrompt()
                    }
                },
            )
            FeedbackTriageCard(intent = selectedIntent)
            OutlinedTextField(
                value = message,
                onValueChange = {
                    message = it
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                label = { Text("Tell ${selectedPal.displayName()} what happened") },
                placeholder = { Text(selectedIntent.placeholder()) },
                shape = RoundedCornerShape(8.dp),
            )
            FeedbackContextRow(screenRoute = screenRoute)
            submitError?.let { error ->
                SubmissionStatusCard(
                    message = error,
                    isError = true,
                )
            }
            triageResult?.let { result ->
                TriageResultCard(result = result)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Not now")
                }
                Button(
                    onClick = {
                        onSubmit(selectedIntent, message)
                    },
                    enabled = message.isNotBlank() && !isSubmitting,
                    modifier = Modifier.weight(1.35f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(if (isSubmitting) "Sending" else "Send feedback")
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun FeedbackPalSheetHeader(
    selectedPal: FeedbackPalKind,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FeedbackPalSprite(
                kind = selectedPal,
                pose = FeedbackPalPose.Front,
                size = 58.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "${selectedPal.displayName()} is listening",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Capture bugs, ideas, confusing flows, or little wins before they disappear.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                onClick = onDismiss,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedbackIntentSelector(
    selectedIntent: FeedbackIntent,
    onSelect: (FeedbackIntent) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FeedbackIntent.entries.forEach { intent ->
            val selected = selectedIntent == intent
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f)
                },
                border = BorderStroke(
                    1.dp,
                    if (selected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)
                    },
                ),
                onClick = { onSelect(intent) },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = intent.icon(),
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(17.dp),
                    )
                    Text(
                        text = intent.label(),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedbackTriageCard(intent: FeedbackIntent) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "Review queue signal",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = intent.triageHint(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FeedbackContextRow(
    screenRoute: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "Context",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Sent from $screenRoute with your household and member context.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SubmissionStatusCard(
    message: String,
    isError: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = if (isError) {
            MaterialTheme.colorScheme.error.copy(alpha = 0.10f)
        } else {
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
        },
        border = BorderStroke(
            1.dp,
            if (isError) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.18f)
            } else {
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isError) Icons.Rounded.BugReport else Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun TriageResultCard(result: FeedbackPalTriageResult) {
    SubmissionStatusCard(
        message = "${result.type} ${result.priority} (${result.effort}). ${result.summary}",
        isError = false,
    )
}

fun FeedbackPalKind.displayName(): String = when (this) {
    FeedbackPalKind.Squirrel -> "Squirrel"
    FeedbackPalKind.Penguin -> "Penguin"
    FeedbackPalKind.Cat -> "Cat"
}

fun FeedbackPalKind.shortLabel(): String = when (this) {
    FeedbackPalKind.Squirrel -> "Squirrel"
    FeedbackPalKind.Penguin -> "Penguin"
    FeedbackPalKind.Cat -> "Cat"
}

fun FeedbackPalKind.description(): String = when (this) {
    FeedbackPalKind.Squirrel -> "Warm, familiar, and best for quick household bug reports."
    FeedbackPalKind.Penguin -> "Calm, steady, and best for confusing flows or stuck moments."
    FeedbackPalKind.Cat -> "Curious, expressive, and best for new feature ideas."
}

private fun FeedbackIntent.label(): String = when (this) {
    FeedbackIntent.Bug -> "Report bug"
    FeedbackIntent.FeatureIdea -> "New idea"
    FeedbackIntent.ConfusingFlow -> "Confusing"
    FeedbackIntent.Praise -> "Worked well"
}

private fun FeedbackIntent.icon(): ImageVector = when (this) {
    FeedbackIntent.Bug -> Icons.Rounded.BugReport
    FeedbackIntent.FeatureIdea -> Icons.Rounded.Lightbulb
    FeedbackIntent.ConfusingFlow -> Icons.Rounded.TipsAndUpdates
    FeedbackIntent.Praise -> Icons.Rounded.CheckCircle
}

private fun FeedbackIntent.starterPrompt(): String = when (this) {
    FeedbackIntent.Bug -> "I found a bug when..."
    FeedbackIntent.FeatureIdea -> "I wish ChorePilot could..."
    FeedbackIntent.ConfusingFlow -> "This part felt confusing because..."
    FeedbackIntent.Praise -> "This worked really well because..."
}

private fun FeedbackIntent.placeholder(): String = when (this) {
    FeedbackIntent.Bug -> "What did you expect, what happened instead, and can it happen again?"
    FeedbackIntent.FeatureIdea -> "What would this help your household do faster or with less stress?"
    FeedbackIntent.ConfusingFlow -> "Where did you get stuck, and what were you trying to finish?"
    FeedbackIntent.Praise -> "What should stay exactly like this in future updates?"
}

private fun FeedbackIntent.triageHint(): String = when (this) {
    FeedbackIntent.Bug -> "Scored by severity, reproducibility, affected screens, and screenshot clarity."
    FeedbackIntent.FeatureIdea -> "Scored by household impact, frequency, retention value, and likely build effort."
    FeedbackIntent.ConfusingFlow -> "Scored by task interruption, number of affected users, and whether copy or UI structure caused the issue."
    FeedbackIntent.Praise -> "Saved as product signal so strong moments can be protected during future changes."
}

@Preview(name = "Feedback Pal Host", widthDp = 393, heightDp = 852)
@Composable
private fun FeedbackPalHostPreview() {
    ChorePilotPreviewTheme {
        ChorePilotGradientBackground {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)),
                        ),
                    ),
            ) {
                FeedbackPalHost(
                    selectedPal = FeedbackPalKind.Penguin,
                    bottomPadding = 48.dp,
                    feedbackPalRepository = PreviewFeedbackPalRepository,
                )
            }
        }
    }
}

private object PreviewFeedbackPalRepository : FeedbackPalRepository {
    override suspend fun submitFeedback(
        reportedIntent: FeedbackIntent,
        message: String,
        screenRoute: String,
        palKind: FeedbackPalKind,
    ): FeedbackPalTriageResult = FeedbackPalTriageResult(
        id = "preview",
        type = reportedIntent.label(),
        priority = "P2",
        priorityScore = 62,
        effort = "Medium",
        summary = "Preview feedback is ready for review.",
        signals = listOf("preview"),
    )
}
