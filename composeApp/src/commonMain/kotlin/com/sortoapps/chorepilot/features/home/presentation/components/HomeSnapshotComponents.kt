package com.sortoapps.chorepilot.features.home.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.sortoapps.chorepilot.core.demo.DemoMetric
import com.sortoapps.chorepilot.core.designsystem.PremiumTabItem
import com.sortoapps.chorepilot.core.designsystem.PremiumTabSelector
import com.sortoapps.chorepilot.features.chores.domain.ChoreItem
import com.sortoapps.chorepilot.features.chores.domain.ChoreItemStatus
import com.sortoapps.chorepilot.features.home.presentation.viewmodel.HomeSurfaceMode
import kotlin.math.roundToInt

@Composable
fun DailySnapshotCard(
    chores: List<ChoreItem>,
    snapshotTitle: String,
    modifier: Modifier = Modifier,
) {
    val completedCount = chores.count { it.status == ChoreItemStatus.Completed }
    val completionProgress = if (chores.isEmpty()) 0f else completedCount.toFloat() / chores.size.toFloat()

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = snapshotTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                )
                Text(
                    text = if (chores.isEmpty()) "No chores scheduled." else "$completedCount of ${chores.size} complete",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SnapshotProgressDonut(progress = completionProgress)
        }
    }
}

@Composable
private fun SnapshotProgressDonut(
    progress: Float,
) {
    val (startTarget, endTarget) = progressGradient(progress)
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "snapshot-donut-progress",
    )
    val animatedStart by animateColorAsState(
        targetValue = startTarget,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "snapshot-donut-start",
    )
    val animatedEnd by animateColorAsState(
        targetValue = endTarget,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "snapshot-donut-end",
    )
    val animatedPercent = (animatedProgress * 100f).roundToInt()
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)

    Box(
        modifier = Modifier.size(88.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 10.dp.toPx()
            val inset = strokeWidth / 2f
            drawCircle(
                color = trackColor,
                radius = (size.minDimension / 2f) - inset,
                center = Offset(size.width / 2f, size.height / 2f),
                style = Stroke(width = strokeWidth),
            )
            if (animatedProgress > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(animatedStart, animatedEnd, animatedStart),
                    ),
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = size.copy(
                        width = size.width - strokeWidth,
                        height = size.height - strokeWidth,
                    ),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
        }
        Surface(
            modifier = Modifier.size(62.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f),
            shape = CircleShape,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "$animatedPercent%",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                )
            }
        }
    }
}

private fun progressGradient(progress: Float): Pair<Color, Color> = when {
    progress < 0.34f -> Color(0xFFE86372) to Color(0xFFFF9A7A)
    progress < 0.67f -> Color(0xFFF0B53A) to Color(0xFFFFE27A)
    else -> Color(0xFF2DCB87) to Color(0xFF79F0B4)
}

@Composable
fun HomeModeTabs(
    selectedSurface: HomeSurfaceMode,
    onSurfaceSelected: (HomeSurfaceMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumTabSelector(
        items = HomeSurfaceMode.entries.map { PremiumTabItem(label = modeLabel(it)) },
        selectedIndex = HomeSurfaceMode.entries.indexOf(selectedSurface),
        onSelect = { index -> onSurfaceSelected(HomeSurfaceMode.entries[index]) },
        modifier = modifier,
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
fun WeeklyStatusCard(
    metric: DemoMetric?,
) {
    if (metric == null) return

    val progress = metric.label.filter { it.isDigit() }.toFloatOrNull()?.div(100f) ?: 0.64f

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = "Weekly status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                )
                Text(
                    text = metric.label,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = metric.value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary),
                )
            }
        }
    }
}

private fun modeLabel(mode: HomeSurfaceMode): String = when (mode) {
    HomeSurfaceMode.Today -> "All"
    HomeSurfaceMode.Celebration -> "Mine"
    HomeSurfaceMode.NoChores -> "Done"
    HomeSurfaceMode.ProofQueue -> "Proof"
}
