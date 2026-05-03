package com.sortoapps.chorepilot.features.chorecapture.presentation.platform

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest

@Composable
fun ProofPreviewImage(
    model: String,
    modifier: Modifier = Modifier,
) {
    val platformContext = LocalPlatformContext.current
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(platformContext)
            .data(model)
            .build(),
        contentDescription = "Proof photo preview",
        modifier = modifier,
        contentScale = ContentScale.Crop,
        loading = {
            PreviewImageShimmer(modifier = Modifier.fillMaxSize())
        },
        error = {
            PreviewImageFallback(
                modifier = Modifier.fillMaxSize(),
                label = "Preview unavailable",
            )
        },
    )
}

@Composable
private fun PreviewImageShimmer(
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "proof-preview-shimmer")
    val progress = transition.animateFloat(
        initialValue = -0.8f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1150, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "proof-preview-shimmer-progress",
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.44f),
            MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.44f),
        ),
        start = Offset(x = progress.value * 900f, y = 0f),
        end = Offset(x = progress.value * 900f + 340f, y = 420f),
    )
    Box(
        modifier = modifier.background(
            brush = shimmerBrush,
            shape = RoundedCornerShape(18.dp),
        ),
    )
}

@Composable
private fun PreviewImageFallback(
    modifier: Modifier = Modifier,
    label: String,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Box {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}
