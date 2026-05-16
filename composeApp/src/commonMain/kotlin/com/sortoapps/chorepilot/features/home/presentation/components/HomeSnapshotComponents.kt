package com.sortoapps.chorepilot.features.home.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import chorepilot.composeapp.generated.resources.Res
import chorepilot.composeapp.generated.resources.home_snapshot_leaves
import chorepilot.composeapp.generated.resources.m1_success_celebration
import com.sortoapps.chorepilot.core.demo.DemoMetric
import com.sortoapps.chorepilot.core.designsystem.PremiumTabItem
import com.sortoapps.chorepilot.core.designsystem.PremiumTabSelector
import com.sortoapps.chorepilot.features.chores.domain.ChoreItem
import com.sortoapps.chorepilot.features.chores.domain.ChoreItemStatus
import com.sortoapps.chorepilot.features.home.presentation.viewmodel.HomeSurfaceMode
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.painterResource

@Composable
fun DailySnapshotCard(
    chores: List<ChoreItem>,
    snapshotTitle: String,
    modifier: Modifier = Modifier,
) {
    val completedCount = chores.count { it.status == ChoreItemStatus.Completed }
    val completionProgress = if (chores.isEmpty()) 0f else completedCount.toFloat() / chores.size.toFloat()
    val isComplete = chores.isNotEmpty() && completedCount == chores.size

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
        ) {
            Image(
                painter = painterResource(Res.drawable.home_snapshot_leaves),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(116.dp),
            )
            SnapshotSparkles(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 126.dp, end = 128.dp, top = 24.dp, bottom = 30.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 22.dp, top = 24.dp, end = 18.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SnapshotCopy(
                    snapshotTitle = snapshotTitle,
                    completedCount = completedCount,
                    totalCount = chores.size,
                    isComplete = isComplete,
                    modifier = Modifier.weight(1f),
                )

                SnapshotProgressDonut(
                    progress = completionProgress,
                    isComplete = isComplete,
                )
            }
        }
    }
}

@Composable
private fun SnapshotProgressDonut(
    progress: Float,
    isComplete: Boolean,
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
    val successGreen = Color(0xFF45D890)

    Box(
        modifier = Modifier.size(112.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 12.dp.toPx()
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
            modifier = Modifier.size(76.dp),
            color = MaterialTheme.colorScheme.surface,
            shape = CircleShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = if (isComplete) 10.dp else 0.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "$animatedPercent%",
                    style = if (isComplete) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                )
                if (isComplete) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "All done",
                            style = MaterialTheme.typography.labelSmall,
                            color = successGreen,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        )
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = successGreen,
                        )
                    }
                }
            }
        }
        if (isComplete) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(28.dp),
                color = successGreen,
                shape = CircleShape,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun SnapshotCopy(
    snapshotTitle: String,
    completedCount: Int,
    totalCount: Int,
    isComplete: Boolean,
    modifier: Modifier = Modifier,
) {
    val numberStyle = if (isComplete) {
        SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            fontSize = MaterialTheme.typography.titleLarge.fontSize,
            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
        )
    } else {
        SpanStyle(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = MaterialTheme.typography.titleLarge.fontSize,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        )
    }
    val supportingStyle = SpanStyle(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = MaterialTheme.typography.titleLarge.fontSize,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
    )
    val completionText = buildAnnotatedString {
        if (totalCount == 0) {
            append("0 scheduled")
        } else {
            withStyle(numberStyle) { append(completedCount.toString()) }
            withStyle(supportingStyle) { append(" of ") }
            withStyle(numberStyle) { append(totalCount.toString()) }
            withStyle(supportingStyle) { append(" complete") }
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = snapshotTitle,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
        )
        Text(
            text = completionText,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SnapshotSparkles(
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val sparkleColor = Color(0xFFE8E3FF).copy(alpha = 0.9f)
        val points = listOf(
            Offset(size.width * 0.18f, size.height * 0.24f) to 7.dp.toPx(),
            Offset(size.width * 0.56f, size.height * 0.54f) to 5.dp.toPx(),
            Offset(size.width * 0.82f, size.height * 0.18f) to 4.dp.toPx(),
            Offset(size.width * 0.72f, size.height * 0.80f) to 6.dp.toPx(),
        )
        points.forEach { (center, radius) ->
            drawLine(
                color = sparkleColor,
                start = Offset(center.x - radius, center.y),
                end = Offset(center.x + radius, center.y),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = sparkleColor,
                start = Offset(center.x, center.y - radius),
                end = Offset(center.x, center.y + radius),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
fun HomeFinishedRoomCard(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Image(
            painter = painterResource(Res.drawable.m1_success_celebration),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(208.dp),
        )
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
        items = HomeSurfaceMode.entries.map {
            PremiumTabItem(
                label = modeLabel(it),
                icon = modeIcon(it),
            )
        },
        selectedIndex = HomeSurfaceMode.entries.indexOf(selectedSurface),
        onSelect = { index -> onSurfaceSelected(HomeSurfaceMode.entries[index]) },
        modifier = modifier,
        highlightColor = MaterialTheme.colorScheme.primary,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        cornerRadius = 8.dp,
        indicatorCornerRadius = 8.dp,
        indicatorInset = 4.dp,
        labelTextStyle = MaterialTheme.typography.labelMedium,
        iconSize = 20.dp,
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

private fun modeIcon(mode: HomeSurfaceMode) = when (mode) {
    HomeSurfaceMode.Today -> Icons.Rounded.GridView
    HomeSurfaceMode.Celebration -> Icons.Rounded.Person
    HomeSurfaceMode.NoChores -> Icons.Rounded.CheckCircle
    HomeSurfaceMode.ProofQueue -> Icons.Rounded.PhotoCamera
}
