package com.sortoapps.chorepilot.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import chorepilot.composeapp.generated.resources.Res
import chorepilot.composeapp.generated.resources.chore_complete_squirrel_cheer
import com.sortoapps.chorepilot.core.preview.ChorePilotPreviewTheme
import com.sortoapps.chorepilot.core.ui.SnackbarStyle
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

data class ChorePilotSnackbarVisuals(
    override val message: String,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false,
    override val duration: SnackbarDuration = SnackbarDuration.Short,
    val style: SnackbarStyle = SnackbarStyle.Standard,
    val icon: DrawableResource? = null,
) : SnackbarVisuals

private val SnackbarShape = RoundedCornerShape(8.dp)

@Composable
fun ChorePilotSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier.fillMaxWidth(),
    ) { data ->
        val visuals = data.visuals as? ChorePilotSnackbarVisuals ?: remember(data) {
            ChorePilotSnackbarVisuals(message = data.visuals.message)
        }
        when (visuals.style) {
            SnackbarStyle.Standard -> StandardSnackbar(
                data = data,
                visuals = visuals,
            )

            SnackbarStyle.Celebration -> CelebrationSnackbar(
                data = data,
                visuals = visuals,
            )
        }
    }
}

@Composable
private fun StandardSnackbar(
    data: SnackbarData,
    visuals: ChorePilotSnackbarVisuals,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp),
        shape = SnackbarShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(10.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.82f),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {}
            Text(
                text = visuals.message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            visuals.actionLabel?.let { actionLabel ->
                TextButton(onClick = data::performAction) {
                    Text(
                        text = actionLabel,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun CelebrationSnackbar(
    data: SnackbarData,
    visuals: ChorePilotSnackbarVisuals,
) {
    var showConfetti by remember(visuals.message, visuals.icon) { mutableStateOf(true) }
    var snackbarSize by remember(visuals.message, visuals.icon) { mutableStateOf(IntSize.Zero) }
    val canRenderConfetti = showConfetti && snackbarSize.width > 0 && snackbarSize.height > 0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .widthIn(max = 560.dp)
            .onSizeChanged { snackbarSize = it },
    ) {
        if (canRenderConfetti) {
            Box(modifier = Modifier.matchParentSize()) {
                ConfettiEffect(
                    modifier = Modifier.matchParentSize(),
                    confettiCount = 20,
                    durationMillis = 1350,
                    burstOriginX = 36f,
                    burstOriginY = snackbarSize.height * 0.5f,
                    onAnimationEnd = { showConfetti = false },
                )
                ConfettiEffect(
                    modifier = Modifier.matchParentSize(),
                    confettiCount = 20,
                    durationMillis = 1350,
                    burstOriginX = (snackbarSize.width - 36).coerceAtLeast(36).toFloat(),
                    burstOriginY = snackbarSize.height * 0.5f,
                    onAnimationEnd = { showConfetti = false },
                )
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth(),
            shape = SnackbarShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 11.dp, bottom = 11.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = visuals.message,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
                visuals.actionLabel?.let { actionLabel ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)),
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                    ) {
                        TextButton(onClick = data::performAction) {
                            Text(
                                text = actionLabel,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }

        if (canRenderConfetti) {
            Box(modifier = Modifier.matchParentSize()) {
                ConfettiEffect(
                    modifier = Modifier.matchParentSize(),
                    confettiCount = 24,
                    durationMillis = 1100,
                    burstOriginX = 36f,
                    burstOriginY = snackbarSize.height * 0.5f,
                    onAnimationEnd = { showConfetti = false },
                )
                ConfettiEffect(
                    modifier = Modifier.matchParentSize(),
                    confettiCount = 24,
                    durationMillis = 1100,
                    burstOriginX = (snackbarSize.width - 36).coerceAtLeast(36).toFloat(),
                    burstOriginY = snackbarSize.height * 0.5f,
                    onAnimationEnd = { showConfetti = false },
                )
            }
        }

        visuals.icon?.let { icon ->
            Box(modifier = Modifier.matchParentSize()) {
                Image(
                    painter = painterResource(icon),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = (0).dp, y = 14.dp)
                        .graphicsLayer {
                            scaleX = 1.68f
                            scaleY = 1.68f
                            transformOrigin = TransformOrigin(0.32f, 1f)
                        },
                )
            }
        }
    }
}

private class PreviewSnackbarData(
    override val visuals: SnackbarVisuals,
) : SnackbarData {
    override fun dismiss() = Unit
    override fun performAction() = Unit
}

@Preview(name = "Standard Snackbar", widthDp = 393)
@Composable
private fun StandardSnackbarPreview() {
    ChorePilotPreviewTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
        ) {
            StandardSnackbar(
                data = PreviewSnackbarData(
                    ChorePilotSnackbarVisuals(
                        message = "Theme saved",
                    ),
                ),
                visuals = ChorePilotSnackbarVisuals(
                    message = "Theme saved",
                ),
            )
        }
    }
}

@Preview(name = "Celebration Snackbar", widthDp = 393)
@Composable
private fun CelebrationSnackbarPreview() {
    ChorePilotPreviewTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
        ) {
            CelebrationSnackbar(
                data = PreviewSnackbarData(
                    ChorePilotSnackbarVisuals(
                        message = "Chore completed +12 points!",
                        style = SnackbarStyle.Celebration,
                        icon = Res.drawable.chore_complete_squirrel_cheer,
                    ),
                ),
                visuals = ChorePilotSnackbarVisuals(
                    message = "Chore completed +12 points!",
                    style = SnackbarStyle.Celebration,
                    icon = Res.drawable.chore_complete_squirrel_cheer,
                ),
            )
        }
    }
}
