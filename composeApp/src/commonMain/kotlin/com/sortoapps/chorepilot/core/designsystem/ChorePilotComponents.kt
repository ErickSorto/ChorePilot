package com.sortoapps.chorepilot.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sortoapps.chorepilot.core.demo.DemoChartPoint
import com.sortoapps.chorepilot.core.demo.DemoMetric
import com.sortoapps.chorepilot.core.demo.DemoTone
import chorepilot.composeapp.generated.resources.Res
import chorepilot.composeapp.generated.resources.acorn_clock_logo
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Shape

@Composable
fun ChorePilotGradientBackground(
    modifier: Modifier = Modifier,
    dark: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val useDarkGradient = dark ?: (MaterialTheme.colorScheme.background.luminance() < 0.45f)
    val brush = when (ChorePilotThemeTokens.appearancePreset) {
        com.sortoapps.chorepilot.features.settings.domain.AppearancePreset.Classic -> if (useDarkGradient) {
            Brush.verticalGradient(
                listOf(Color(0xFF08111F), Color(0xFF0A1423), Color(0xFF0C1728)),
            )
        } else {
            Brush.verticalGradient(
                listOf(Color(0xFFF8F9FE), Color(0xFFF7F4FF), Color(0xFFF1FBF8)),
            )
        }

        com.sortoapps.chorepilot.features.settings.domain.AppearancePreset.EditorialWarm -> if (useDarkGradient) {
            Brush.verticalGradient(
                listOf(Color(0xFF191412), Color(0xFF211915), Color(0xFF2B1F1A)),
            )
        } else {
            Brush.verticalGradient(
                listOf(Color(0xFFFEF8F4), Color(0xFFF9F0E8), Color(0xFFF5EAE1)),
            )
        }

        com.sortoapps.chorepilot.features.settings.domain.AppearancePreset.SunDrenchedEditorial -> if (useDarkGradient) {
            Brush.verticalGradient(
                listOf(Color(0xFF1A110D), Color(0xFF241712), Color(0xFF302019)),
            )
        } else {
            Brush.verticalGradient(
                listOf(Color(0xFFFFF8F6), Color(0xFFFFF0E7), Color(0xFFF7E7DE)),
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush),
    ) {
        content()
    }
}

@Composable
fun ChorePilotWordmark(
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    compact: Boolean = false,
) {
    val badgeSize = if (compact) 40.dp else 48.dp
    val titleStyle = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChorePilotLogoBadge(size = badgeSize)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "ChorePilot",
                style = titleStyle,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun ChorePilotLogoBadge(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 40.dp,
    containerAlpha: Float = 0.12f,
    borderAlpha: Float = 0.12f,
    logoPaddingFraction: Float = 0.18f,
) {
    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = containerAlpha),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha),
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.acorn_clock_logo),
                contentDescription = "ChorePilot logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(size * logoPaddingFraction),
            )
        }
    }
}

@Composable
fun AnimatedProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    accentBrush: Brush = Brush.horizontalGradient(
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary,
        ),
    ),
) {
    val animatedProgress = animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500),
        label = "chorepilot-progress",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress.value.coerceAtLeast(0.04f))
                .height(8.dp)
                .clip(CircleShape)
                .background(accentBrush),
        )
    }
}

@Composable
fun ProgressPill(
    text: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
) {
    Surface(
        modifier = modifier,
        color = if (active) accentColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f),
        shape = CircleShape,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontWeight = FontWeight.Bold,
        )
    }
}

data class PremiumTabItem(
    val label: String,
    val icon: ImageVector? = null,
)

@Composable
fun PremiumTabSelector(
    items: List<PremiumTabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    highlightColor: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.74f),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    inactiveContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    cornerRadius: androidx.compose.ui.unit.Dp = 28.dp,
    indicatorCornerRadius: androidx.compose.ui.unit.Dp = 20.dp,
    indicatorInset: androidx.compose.ui.unit.Dp = 6.dp,
    labelTextStyle: TextStyle = MaterialTheme.typography.labelLarge,
    iconSize: androidx.compose.ui.unit.Dp = 24.dp,
    flatStyle: Boolean = false,
) {
    if (items.isEmpty()) return

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        color = containerColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        PrimaryTabRow(
            selectedTabIndex = selectedIndex.coerceIn(items.indices),
            modifier = Modifier.fillMaxWidth(),
            containerColor = Color.Transparent,
            divider = {},
            indicator = {
                val safeIndex = selectedIndex.coerceAtLeast(0)
                Box(
                    modifier = Modifier
                        .tabIndicatorOffset(safeIndex, matchContentSize = false)
                        .fillMaxSize()
                        .padding(horizontal = indicatorInset, vertical = indicatorInset)
                        .then(
                            if (flatStyle) {
                                Modifier
                            } else {
                                Modifier.shadow(
                                    elevation = 14.dp,
                                    shape = RoundedCornerShape(indicatorCornerRadius),
                                    spotColor = highlightColor.copy(alpha = 0.20f),
                                    ambientColor = highlightColor.copy(alpha = 0.12f),
                                )
                            }
                        )
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    highlightColor.copy(alpha = if (flatStyle) 0.18f else 0.24f),
                                    highlightColor.copy(alpha = if (flatStyle) 0.08f else 0.10f),
                                ),
                            ),
                            shape = RoundedCornerShape(indicatorCornerRadius),
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = if (flatStyle) 0.18f else 0.38f),
                                    highlightColor.copy(alpha = if (flatStyle) 0.14f else 0.22f),
                                ),
                            ),
                            shape = RoundedCornerShape(indicatorCornerRadius),
                        ),
                )
            },
        ) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                val tabContentColor by animateColorAsState(
                    targetValue = if (selected) contentColor else inactiveContentColor,
                    animationSpec = tween(durationMillis = 300),
                    label = "premium-tab-color",
                )
                Tab(
                    selected = selected,
                    onClick = { onSelect(index) },
                    selectedContentColor = tabContentColor,
                    unselectedContentColor = tabContentColor,
                    text = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            item.icon?.let { icon ->
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(iconSize),
                                    tint = tabContentColor,
                                )
                            }
                            Text(
                                text = item.label,
                                color = tabContentColor,
                                style = labelTextStyle,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
fun HexagonBadge(
    text: String,
    modifier: Modifier = Modifier,
    fillColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
) {
    val shape = rememberHexagonShape()
    Surface(
        modifier = modifier,
        shape = shape,
        color = fillColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 4.dp,
        tonalElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier.size(42.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun rememberHexagonShape(): Shape = GenericShape { size, _ ->
    val horizontalInset = size.width * 0.18f
    val verticalInset = size.height * 0.08f
    val round = size.width * 0.10f
    moveTo(horizontalInset + round, verticalInset)
    lineTo(size.width - horizontalInset - round, verticalInset)
    quadraticTo(size.width - horizontalInset / 2f, verticalInset, size.width - horizontalInset / 3f, size.height * 0.26f)
    quadraticTo(size.width, size.height / 2f, size.width - horizontalInset / 3f, size.height * 0.74f)
    quadraticTo(size.width - horizontalInset / 2f, size.height - verticalInset, size.width - horizontalInset - round, size.height - verticalInset)
    lineTo(horizontalInset + round, size.height - verticalInset)
    quadraticTo(horizontalInset / 2f, size.height - verticalInset, horizontalInset / 3f, size.height * 0.74f)
    quadraticTo(0f, size.height / 2f, horizontalInset / 3f, size.height * 0.26f)
    quadraticTo(horizontalInset / 2f, verticalInset, horizontalInset + round, verticalInset)
    close()
}

@Composable
fun ChorePilotScreen(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(24.dp),
    content: @Composable () -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (actionLabel != null && onActionClick != null) {
                    OutlinedButton(
                        onClick = onActionClick,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(actionLabel)
                    }
                }
            }
        }

        item {
            content()
        }
    }
}

@Composable
fun GradientButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    textStyle: TextStyle = MaterialTheme.typography.labelLarge,
    shape: Shape = MaterialTheme.shapes.medium,
    brush: Brush = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
        ),
    ),
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick),
        color = Color.Transparent,
        shape = shape,
        shadowElevation = 16.dp,
    ) {
        Box(
            modifier = Modifier
                .background(brush)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = textStyle,
                color = contentColor.copy(alpha = if (enabled) 1f else 0.6f),
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    ElevatedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Text(text)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Text(text)
    }
}

@Composable
fun IllustrationCard(
    drawable: DrawableResource,
    contentDescription: String,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 1f,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(drawable),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
    }
}

@Composable
fun MetricRow(metrics: List<DemoMetric>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        maxItemsInEachRow = 2,
    ) {
        metrics.forEach { metric ->
            MetricCard(metric = metric, modifier = Modifier.weight(1f, fill = true))
        }
    }
}

@Composable
fun MetricCard(
    metric: DemoMetric,
    modifier: Modifier = Modifier,
) {
    val accent = when (metric.tone) {
        DemoTone.Secondary, DemoTone.Success -> MaterialTheme.colorScheme.secondary
        DemoTone.Neutral -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = if (metric.tone == DemoTone.Neutral) 0.45f else 0.7f)),
            )
            Text(
                text = metric.label,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = metric.value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun ToneChip(
    text: String,
    tone: DemoTone = DemoTone.Primary,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = toneColor(tone).copy(alpha = 0.14f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, toneColor(tone).copy(alpha = 0.12f)),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = toneColor(tone),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
fun AvatarBadge(
    seed: String,
    tone: DemoTone,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(toneColor(tone).copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = seed,
            style = MaterialTheme.typography.labelLarge,
            color = toneColor(tone),
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun ProgressBarsChart(
    points: List<DemoChartPoint>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val primaryColor = MaterialTheme.colorScheme.primary
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
            ) {
                val spacing = 16.dp.toPx()
                val barWidth = (size.width - spacing * (points.size - 1)) / points.size
                points.forEachIndexed { index, point ->
                    val left = index * (barWidth + spacing)
                    val top = size.height * (1f - point.value)
                    drawRoundRect(
                        color = primaryColor.copy(alpha = 0.85f),
                        topLeft = androidx.compose.ui.geometry.Offset(left, top),
                        size = androidx.compose.ui.geometry.Size(barWidth, size.height - top),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                points.forEach { point ->
                    Text(
                        text = point.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun ChoiceRow(
    title: String,
    description: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    leadingLabel: String? = null,
    showChevron: Boolean = true,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
        else MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, borderColor.copy(alpha = if (selected) 0.36f else 0.18f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            if (leadingLabel != null) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = leadingLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (showChevron) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.graphicsLayer(alpha = if (selected) 1f else 0.55f),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                            shape = CircleShape,
                        )
                        .background(
                            color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent,
                            shape = CircleShape,
                        ),
                )
            }
        }
    }
}

@Composable
fun TimelineStep(
    dayLabel: String,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                Text(
                    text = dayLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            Spacer(
                modifier = Modifier
                    .width(2.dp)
                    .height(56.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun PreviewCard(
    title: String,
    body: String,
    actionLabel: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun toneColor(tone: DemoTone): Color = when (tone) {
    DemoTone.Primary -> MaterialTheme.colorScheme.primary
    DemoTone.Secondary -> MaterialTheme.colorScheme.secondary
    DemoTone.Tertiary -> MaterialTheme.colorScheme.primary
    DemoTone.Success -> MaterialTheme.colorScheme.secondary
    DemoTone.Warning -> MaterialTheme.colorScheme.primary
    DemoTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
}
