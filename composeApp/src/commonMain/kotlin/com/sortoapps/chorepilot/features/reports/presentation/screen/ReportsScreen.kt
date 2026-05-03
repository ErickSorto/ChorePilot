package com.sortoapps.chorepilot.features.reports.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import chorepilot.composeapp.generated.resources.Res
import chorepilot.composeapp.generated.resources.m3_locked_premium_empty
import chorepilot.composeapp.generated.resources.m3_report_empty
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.compose.cartesian.CartesianMeasuringContext
import com.patrykandpatrick.vico.compose.cartesian.axis.Axis
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.columnSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.component.LineComponent
import com.patrykandpatrick.vico.compose.common.component.TextComponent
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.sortoapps.chorepilot.core.demo.DemoMetric
import com.sortoapps.chorepilot.core.demo.DemoTone
import com.sortoapps.chorepilot.core.designsystem.HexagonBadge
import com.sortoapps.chorepilot.core.designsystem.IllustrationCard
import com.sortoapps.chorepilot.core.designsystem.PremiumTabItem
import com.sortoapps.chorepilot.core.designsystem.PremiumTabSelector
import com.sortoapps.chorepilot.core.designsystem.PrimaryButton
import com.sortoapps.chorepilot.core.preview.ChorePilotPreviewData
import com.sortoapps.chorepilot.core.preview.ChorePilotPreviewTheme
import com.sortoapps.chorepilot.core.preview.CompactPreviewShell
import com.sortoapps.chorepilot.features.reports.presentation.viewmodel.ReportsChartBarUiModel
import com.sortoapps.chorepilot.features.reports.presentation.viewmodel.ReportsFirstFinisherUiModel
import com.sortoapps.chorepilot.features.reports.presentation.viewmodel.ReportsLeaderboardEntryUiModel
import com.sortoapps.chorepilot.features.reports.presentation.viewmodel.ReportsRange
import com.sortoapps.chorepilot.features.reports.presentation.viewmodel.ReportsSurfaceMode
import com.sortoapps.chorepilot.features.reports.presentation.viewmodel.ReportsTaskHighlightUiModel
import com.sortoapps.chorepilot.features.reports.presentation.viewmodel.ReportsUiEvent
import com.sortoapps.chorepilot.features.reports.presentation.viewmodel.ReportsUiState

@Composable
fun ReportsScreen(
    state: ReportsUiState,
    onEvent: (ReportsUiEvent) -> Unit,
    onOpenPremium: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                ReportsRangeTabs(
                    selectedRange = state.selectedRange,
                    onSelect = { range ->
                        onEvent(ReportsUiEvent.TriggerVibration)
                        onEvent(ReportsUiEvent.RangeSelected(range))
                    },
                )
            }
            item {
                ReportsPeriodNavigator(
                    title = state.navigation.title,
                    supportingText = state.navigation.supportingText,
                    canGoForward = state.navigation.canGoForward,
                    onPrevious = {
                        onEvent(ReportsUiEvent.TriggerVibration)
                        onEvent(ReportsUiEvent.PreviousPeriodRequested)
                    },
                    onNext = {
                        onEvent(ReportsUiEvent.TriggerVibration)
                        onEvent(ReportsUiEvent.NextPeriodRequested)
                    },
                )
            }
            item {
                when (state.selectedSurface) {
                    ReportsSurfaceMode.Content -> {
                        if (state.selectedRange == ReportsRange.Daily) {
                            DailyReportsContent(state = state)
                        } else {
                            TimelineReportsContent(state = state)
                        }
                    }

                    ReportsSurfaceMode.Locked -> {
                        InsightLockState(
                            state = state,
                            onOpenPremium = {
                                onEvent(ReportsUiEvent.TriggerVibration)
                                onOpenPremium()
                            },
                        )
                    }

                    ReportsSurfaceMode.Empty,
                    ReportsSurfaceMode.NoData,
                    -> EmptyInsightState(state = state)
                }
            }
        }
    }
}

@Composable
private fun ReportsRangeTabs(
    selectedRange: ReportsRange,
    onSelect: (ReportsRange) -> Unit,
) {
    PremiumTabSelector(
        items = ReportsRange.entries.map { PremiumTabItem(label = it.name) },
        selectedIndex = ReportsRange.entries.indexOf(selectedRange),
        onSelect = { index -> onSelect(ReportsRange.entries[index]) },
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
private fun ReportsPeriodNavigator(
    title: String,
    supportingText: String,
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.25.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PeriodNavButton(
                contentDescription = "Previous period",
                icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                onClick = onPrevious,
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (supportingText.isNotBlank()) {
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            PeriodNavButton(
                contentDescription = "Next period",
                icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                enabled = canGoForward,
                onClick = onNext,
            )
        }
    }
}

@Composable
private fun PeriodNavButton(
    contentDescription: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        shape = CircleShape,
        color = if (enabled) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.26f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
        },
        border = BorderStroke(1.25.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
    ) {
        androidx.compose.material3.IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(44.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
            )
        }
    }
}

@Composable
private fun DailyReportsContent(
    state: ReportsUiState,
) {
    val visibleBars = remember(state.activeBars) {
        state.activeBars.filter { bar ->
            bar.value > 0 || bar.label.equals("Completed", ignoreCase = true)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LeaderboardCard(
            leaderboard = state.leaderboard,
        )
        VictoriesCard(
            firstFinisher = state.firstFinisher,
            leaderboard = state.leaderboard,
        )
        DailyStatusOverview(
            summary = state.rangeSummary,
            bars = visibleBars,
        )
        if (state.taskHighlights.isNotEmpty()) {
            OutstandingTasksCard(tasks = state.taskHighlights)
        }
    }
}

@Composable
private fun TimelineReportsContent(
    state: ReportsUiState,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TimelineChartCard(
            title = state.chartTitle,
            summary = state.rangeSummary,
            bars = state.activeBars,
        )
        CompactMetricGrid(metrics = state.activeMetrics)
    }
}

@Composable
private fun DailyStatusOverview(
    summary: String,
    bars: List<ReportsChartBarUiModel>,
) {
    val completedBar = bars.firstOrNull { it.label.equals("Completed", ignoreCase = true) }
    val total = bars.sumOf { it.value }.coerceAtLeast(completedBar?.value ?: 0)
    val completionProgress = if (total == 0) 0f else (completedBar?.value ?: 0).toFloat() / total.toFloat()
    val secondaryBars = bars.filterNot {
        it.label.equals("Completed", ignoreCase = true) ||
            it.label.equals("Open", ignoreCase = true)
    }
    ReportsSectionCard(
        title = "How today is moving",
        contentContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.18f),
        borderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            completedBar?.let { bar ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = bar.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = bar.value.toString(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.44f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)),
                            ) {
                                Text(
                                    text = if (completionProgress >= 0.999f) {
                                        "Perfect score"
                                    } else {
                                        "${(completionProgress * 100).toInt()}% complete"
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        if (summary.isNotBlank()) {
                            Text(
                                text = summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f), CircleShape),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(completionProgress.coerceIn(0f, 1f))
                                    .height(10.dp)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        listOf(
                                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.36f),
                                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.72f),
                                                MaterialTheme.colorScheme.secondary,
                                            ),
                                        ),
                                        shape = CircleShape,
                                    ),
                            )
                        }
                    }
                }
            }
            if (secondaryBars.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    secondaryBars.forEachIndexed { index, bar ->
                        val accent = compactMetricAccent(index)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 6.dp, height = 32.dp)
                                        .background(accent, CircleShape),
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Text(
                                        text = bar.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = bar.value.toString(),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
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

@Composable
private fun LeaderboardCard(
    leaderboard: List<ReportsLeaderboardEntryUiModel>,
) {
    ReportsSectionCard(
        title = "Household standings",
        contentContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.14f),
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                leaderboard.forEachIndexed { _, entry ->
                    LeaderboardRow(entry = entry)
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(
    entry: ReportsLeaderboardEntryUiModel,
) {
    val accent = rankAccent(entry.rank)
    val accentSoft = rankAccentSoft(entry.rank)
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 6.dp, height = 54.dp)
                    .background(accent, CircleShape),
            )
            HexagonBadge(
                text = entry.rank.toString(),
                fillColor = accentSoft,
                contentColor = accent,
                borderColor = accent.copy(alpha = 0.14f),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = entry.memberName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "${entry.completedCount} of ${entry.assignedCount} chores closed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    LeaderboardProgressRing(
                        progress = (entry.completionRate / 100f).coerceIn(0f, 1f),
                        label = "${entry.completionRate}%",
                        accent = accent,
                        track = accentSoft,
                    )
                }
            }
        }
    }
}

private data class VictoryUiModel(
    val title: String,
    val detail: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accent: Color,
    val accentSoft: Color,
)

@Composable
private fun VictoriesCard(
    firstFinisher: ReportsFirstFinisherUiModel?,
    leaderboard: List<ReportsLeaderboardEntryUiModel>,
) {
    val victories = buildVictoryModels(firstFinisher = firstFinisher, leaderboard = leaderboard)
    if (victories.isEmpty()) return

    ReportsSectionCard(
        title = "Victories",
        contentContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.10f),
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            victories.forEach { victory ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, victory.accent.copy(alpha = 0.14f)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 6.dp, height = 44.dp)
                                .background(victory.accent, CircleShape),
                        )
                        Box(
                            modifier = Modifier.size(42.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            HexagonBadge(
                                text = "",
                                fillColor = victory.accentSoft,
                                contentColor = victory.accent,
                                borderColor = victory.accent.copy(alpha = 0.16f),
                                modifier = Modifier.fillMaxSize(),
                            )
                            Icon(
                                imageVector = victory.icon,
                                contentDescription = null,
                                tint = victory.accent,
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(
                                text = victory.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = victory.detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardProgressRing(
    progress: Float,
    label: String,
    accent: Color,
    track: Color,
) {
    Box(
        modifier = Modifier.size(58.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(58.dp)) {
            val strokeWidth = 6.dp.toPx()
            val inset = strokeWidth / 2f
            drawCircle(
                color = track,
                radius = (size.minDimension / 2f) - inset,
                style = Stroke(width = strokeWidth),
            )
            if (progress > 0f) {
                drawArc(
                    color = accent,
                    startAngle = -90f,
                    sweepAngle = progress.coerceIn(0f, 1f) * 360f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = size.copy(
                        width = size.width - strokeWidth,
                        height = size.height - strokeWidth,
                    ),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun OutstandingTasksCard(
    tasks: List<ReportsTaskHighlightUiModel>,
) {
    ReportsSectionCard(
        title = "What is left today",
        contentContainerColor = reportsUrgencyContainerColor(),
        borderColor = reportsUrgencyBorderColor(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            tasks.forEach { task ->
                val accent = reportsUrgencyAccent()
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.14f)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 6.dp, height = 44.dp)
                                    .background(accent, CircleShape),
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = task.taskTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = task.memberName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                task.statusLabel
                                    .takeUnless { it.equals("Due soon", ignoreCase = true) }
                                    ?.takeIf { it.isNotBlank() }
                                    ?.let { status ->
                                        Text(
                                            text = status,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = accent,
                                            fontWeight = FontWeight.SemiBold,
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

@Composable
private fun TimelineChartCard(
    title: String,
    summary: String,
    bars: List<ReportsChartBarUiModel>,
) {
    ReportsSectionCard(
        title = title,
        subtitle = summary,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ReportsBarsChart(bars = bars)
            }
        }
    }
}

@Composable
private fun ReportsBarsChart(
    bars: List<ReportsChartBarUiModel>,
) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val chartNeutralTop = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    val chartNeutralMid = MaterialTheme.colorScheme.primary.copy(alpha = 0.52f)
    val chartNeutralBase = MaterialTheme.colorScheme.primary
    val chartNeutralDeep = MaterialTheme.colorScheme.secondary
    val bottomAxisValueFormatter = remember(bars) {
        object : CartesianValueFormatter {
            override fun format(
                context: CartesianMeasuringContext,
                value: Double,
                verticalAxisPosition: Axis.Position.Vertical?,
            ): CharSequence {
                if (bars.isEmpty()) return "0"
                val index = value.toInt().coerceIn(0, bars.lastIndex)
                return bars[index].axisLabel()
            }
        }
    }
    val marker = remember(bars) {
        object : DefaultCartesianMarker.ValueFormatter {
            override fun format(
                context: CartesianDrawingContext,
                targets: List<CartesianMarker.Target>,
            ): CharSequence = targets.firstOrNull()
                ?.let { target -> bars.getOrNull(target.x.toInt()) }
                ?.let { bar ->
                    buildString {
                        append(bar.markerLabel)
                        if (bar.supportingLabel.isNotBlank()) {
                            append(" • ")
                            append(bar.supportingLabel)
                        }
                    }
                }
                .orEmpty()
        }
    }
    val columnProvider = remember(chartNeutralTop, chartNeutralMid, chartNeutralBase, chartNeutralDeep) {
        ColumnCartesianLayer.ColumnProvider.series(
            LineComponent(
                fill = Fill(
                    Brush.verticalGradient(
                        listOf(
                            chartNeutralTop,
                            chartNeutralMid,
                            chartNeutralBase,
                            chartNeutralDeep,
                        ),
                    ),
                ),
                thickness = 22.dp,
                shape = RoundedCornerShape(20.dp),
            ),
        )
    }

    LaunchedEffect(bars) {
        modelProducer.runTransaction {
            columnSeries {
                series(bars.map { it.value })
            }
        }
    }

    ProvideVicoTheme(
        rememberM3VicoTheme(
            columnCartesianLayerColors = listOf(chartNeutralBase),
            lineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
            textColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberColumnCartesianLayer(columnProvider = columnProvider),
                bottomAxis = HorizontalAxis.rememberBottom(
                    valueFormatter = bottomAxisValueFormatter,
                ),
                marker = rememberDefaultCartesianMarker(
                    label = TextComponent(
                        textStyle = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ),
                    valueFormatter = marker,
                ),
            ),
            modelProducer = modelProducer,
            modifier = Modifier
                .fillMaxWidth()
                .height(214.dp),
        )
    }
}

@Composable
private fun CompactMetricGrid(
    metrics: List<DemoMetric>,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        maxItemsInEachRow = 2,
    ) {
        metrics.forEach { metric ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(22.dp),
                tonalElevation = 1.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = metric.value,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = metric.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportsSectionCard(
    title: String,
    subtitle: String? = null,
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
    contentContainerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.06f),
    content: @Composable () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                subtitle?.takeIf(String::isNotBlank)?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = contentContainerColor,
                shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f)),
            ) {
                content()
            }
        }
    }
}

private fun ReportsChartBarUiModel.axisLabel(): String = when {
    label.equals("Pending proof", ignoreCase = true) -> "Proof"
    label.length > 10 -> label.take(10)
    else -> label
}

private fun leaderboardFooterLabel(
    pendingCount: Int,
    overdueCount: Int,
    highlightLabel: String?,
): String = when {
    overdueCount > 0 -> "$overdueCount overdue"
    pendingCount > 0 -> "$pendingCount pending"
    !highlightLabel.isNullOrBlank() -> highlightLabel
    else -> "All clear"
}

private fun statusAccent(statusLabel: String): Color = when {
    statusLabel.contains("overdue", ignoreCase = true) -> toneColor(DemoTone.Warning)
    statusLabel.contains("proof", ignoreCase = true) -> Color(0xFF2F6BFF)
    statusLabel.contains("due", ignoreCase = true) || statusLabel.contains("open", ignoreCase = true) ->
        toneColor(DemoTone.Secondary)
    else -> toneColor(DemoTone.Primary)
}

@Composable
private fun buildVictoryModels(
    firstFinisher: ReportsFirstFinisherUiModel?,
    leaderboard: List<ReportsLeaderboardEntryUiModel>,
): List<VictoryUiModel> {
    val victories = mutableListOf<VictoryUiModel>()

    firstFinisher?.let { finisher ->
        victories += VictoryUiModel(
            title = "First task finished",
            detail = "${finisher.memberName} - ${finisher.taskTitle} - ${finisher.completedAtLabel}",
            icon = Icons.Rounded.EmojiEvents,
            accent = neutralAccent(0),
            accentSoft = neutralAccentSoft(0),
        )
    }

    leaderboard
        .firstOrNull { it.assignedCount > 0 && it.completedCount >= it.assignedCount }
        ?.let { closer ->
            victories += VictoryUiModel(
                title = "First full board cleared",
                detail = "${closer.memberName} - cleared all ${closer.assignedCount} tasks",
                icon = Icons.Rounded.CheckCircle,
                accent = neutralAccent(1),
                accentSoft = neutralAccentSoft(1),
            )
        }

    leaderboard
        .maxByOrNull { it.completedCount }
        ?.takeIf { it.completedCount > 0 }
        ?.let { leader ->
            victories += VictoryUiModel(
                title = "Most tasks closed",
                detail = "${leader.memberName} - ${leader.completedCount} tasks closed",
                icon = Icons.Rounded.TaskAlt,
                accent = neutralAccent(2),
                accentSoft = neutralAccentSoft(2),
            )
        }

    leaderboard
        .maxByOrNull { it.completionRate }
        ?.takeIf { it.assignedCount > 0 }
        ?.let { leader ->
            victories += VictoryUiModel(
                title = "Highest finish rate",
                detail = "${leader.memberName} - ${leader.completionRate}% complete",
                icon = Icons.Rounded.AutoAwesome,
                accent = neutralAccent(3),
                accentSoft = neutralAccentSoft(3),
            )
        }

    return victories.distinctBy { it.title }
}

@Composable
private fun InsightLockState(
    state: ReportsUiState,
    onOpenPremium: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        IllustrationCard(
            drawable = Res.drawable.m3_locked_premium_empty,
            contentDescription = "Locked premium insight",
        )
        Text(
            text = state.lockTitle,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = state.lockBody,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.lockBenefits.forEach { benefit ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .size(8.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                    )
                    Text(
                        text = benefit,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        PrimaryButton(
            text = "Unlock premium insight",
            onClick = onOpenPremium,
        )
    }
}

@Composable
private fun EmptyInsightState(
    state: ReportsUiState,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        IllustrationCard(
            drawable = Res.drawable.m3_report_empty,
            contentDescription = "Reports empty state",
        )
        Text(
            text = state.emptyTitle,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = state.emptyBody,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text(
                text = state.emptyActionHint,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun rankAccent(rank: Int): Color = if (reportsUseDarkPalette()) {
    when (rank.coerceAtLeast(1)) {
        1 -> Color(0xFFA9A5FF)
        2 -> Color(0xFF8E8AFB)
        3 -> Color(0xFF7CE7D4)
        else -> Color(0xFF5EEAD4)
    }
} else {
    when (rank.coerceAtLeast(1)) {
        1 -> Color(0xFF5554B1)
        2 -> Color(0xFF6B69C5)
        3 -> Color(0xFF016C5C)
        else -> Color(0xFF2A8D7D)
    }
}

@Composable
private fun rankAccentSoft(rank: Int): Color = if (reportsUseDarkPalette()) {
    when (rank.coerceAtLeast(1)) {
        1 -> Color(0xFF2B2F58)
        2 -> Color(0xFF252A4E)
        3 -> Color(0xFF163B38)
        else -> Color(0xFF123430)
    }
} else {
    when (rank.coerceAtLeast(1)) {
        1 -> Color(0xFFD9D7FF)
        2 -> Color(0xFFE6E4FF)
        3 -> Color(0xFFD7F2DE)
        else -> Color(0xFFE5F7F1)
    }
}

@Composable
private fun compactMetricAccent(index: Int): Color = if (reportsUseDarkPalette()) {
    when (index % 2) {
        0 -> Color(0xFF6AD9C4)
        else -> Color(0xFFA9A5FF)
    }
} else {
    when (index % 2) {
        0 -> Color(0xFF3E9D8F)
        else -> Color(0xFF5554B1)
    }
}

@Composable
private fun neutralAccent(index: Int): Color = if (reportsUseDarkPalette()) {
    when (index % 4) {
        0 -> Color(0xFFFFD36C)
        1 -> Color(0xFFFFC85C)
        2 -> Color(0xFFFFB84A)
        else -> Color(0xFFFFDF95)
    }
} else {
    when (index % 4) {
        0 -> Color(0xFFC69222)
        1 -> Color(0xFFD0A13A)
        2 -> Color(0xFFB9851C)
        else -> Color(0xFFD8AE57)
    }
}

@Composable
private fun neutralAccentSoft(index: Int): Color = if (reportsUseDarkPalette()) {
    when (index % 4) {
        0 -> Color(0xFF4B3A12)
        1 -> Color(0xFF554116)
        2 -> Color(0xFF603F10)
        else -> Color(0xFF5A451A)
    }
} else {
    when (index % 4) {
        0 -> Color(0xFFFFF1C9)
        1 -> Color(0xFFFFF3D6)
        2 -> Color(0xFFFFEDBF)
        else -> Color(0xFFFFF5DF)
    }
}

@Composable
private fun reportsUrgencyContainerColor(): Color = if (reportsUseDarkPalette()) {
    Color(0xFF2A171A)
} else {
    Color(0xFFFFF1F0)
}

@Composable
private fun reportsUrgencyBorderColor(): Color = if (reportsUseDarkPalette()) {
    Color(0xFF6D3B43)
} else {
    Color(0xFFF0C3BF)
}

@Composable
private fun reportsUrgencyAccent(): Color = if (reportsUseDarkPalette()) {
    Color(0xFFFF8A84)
} else {
    Color(0xFFD5564D)
}

@Composable
private fun reportsUseDarkPalette(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

private fun toneColor(tone: DemoTone) = when (tone) {
    DemoTone.Primary -> Color(0xFF5B6CFF)
    DemoTone.Secondary -> Color(0xFF3E9D8F)
    DemoTone.Success -> Color(0xFF2F9B5D)
    DemoTone.Warning -> Color(0xFFE08C2D)
    DemoTone.Tertiary -> Color(0xFF7C6DE8)
    DemoTone.Neutral -> Color(0xFF7E8798)
}

@Preview(name = "Reports Screen", widthDp = 393, heightDp = 852)
@Composable
private fun ReportsScreenPreview() {
    ChorePilotPreviewTheme {
        CompactPreviewShell(selectedRoute = com.sortoapps.chorepilot.navigation.AppRoute.Reports) { modifier ->
            Column(modifier = modifier) {
                ReportsScreen(
                    state = ChorePilotPreviewData.reportsState(),
                    onEvent = {},
                    onOpenPremium = {},
                )
            }
        }
    }
}

@Preview(name = "Reports Screen Dark", widthDp = 393, heightDp = 852)
@Composable
private fun ReportsScreenDarkPreview() {
    ChorePilotPreviewTheme {
        Surface(color = Color(0xFF0F1115)) {
            CompactPreviewShell(selectedRoute = com.sortoapps.chorepilot.navigation.AppRoute.Reports) { modifier ->
                Column(modifier = modifier) {
                    ReportsScreen(
                        state = ChorePilotPreviewData.reportsState(),
                        onEvent = {},
                        onOpenPremium = {},
                    )
                }
            }
        }
    }
}

@Preview(name = "Reports Screen Weekly", widthDp = 840, heightDp = 900)
@Composable
private fun ReportsScreenWidePreview() {
    ChorePilotPreviewTheme {
        ReportsScreen(
            state = ChorePilotPreviewData.reportsState(selectedRange = ReportsRange.Weekly),
            onEvent = {},
            onOpenPremium = {},
        )
    }
}
