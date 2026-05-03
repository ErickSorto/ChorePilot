package com.sortoapps.chorepilot.features.home.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sortoapps.chorepilot.features.home.presentation.viewmodel.HomeDayUiModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

@Composable
fun HomeTopBar(
    viewerName: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = CircleShape,
            shadowElevation = 0.dp,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initialsFor(viewerName),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                )
            }
        }

        Text(
            text = "ChorePilot",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
        )

        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            shape = CircleShape,
            shadowElevation = 0.dp,
        ) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun GreetingSection(
    eyebrow: String,
    headline: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Text(
            text = headline,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        )
    }
}

@Composable
fun DayCarousel(
    days: List<HomeDayUiModel>,
    onDaySelected: (String) -> Unit,
) {
    if (days.isEmpty()) return

    val selectedIndex = days.indexOfFirst { it.isSelected }.coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val itemWidth = 58.dp

    LaunchedEffect(selectedIndex) {
        if (days.indices.contains(selectedIndex)) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    LaunchedEffect(listState, days) {
        snapshotFlow { listState.isScrollInProgress }
            .map { scrolling -> if (scrolling) null else centeredDayId(listState, days) }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { dayId ->
                if (dayId != days.firstOrNull { it.isSelected }?.id) {
                    onDaySelected(dayId)
                }
            }
    }

    BoxWithConstraints {
        val sidePadding = (maxWidth / 2f) - (itemWidth / 2f)

        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = sidePadding.coerceAtLeast(20.dp)),
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
        ) {
            itemsIndexed(days, key = { _, day -> day.id }) { index, day ->
                DayCarouselCard(
                    day = day,
                    displayNumber = dayNumber(day.id, index),
                    width = itemWidth,
                    onClick = { onDaySelected(day.id) },
                )
            }
        }
    }
}

private fun centeredDayId(
    listState: LazyListState,
    days: List<HomeDayUiModel>,
): String? {
    val layoutInfo = listState.layoutInfo
    val center = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
    val item = layoutInfo.visibleItemsInfo.minByOrNull { info ->
        kotlin.math.abs((info.offset + info.size / 2) - center)
    } ?: return null
    return days.getOrNull(item.index)?.id
}

@Composable
private fun DayCarouselCard(
    day: HomeDayUiModel,
    displayNumber: String,
    width: Dp,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .width(width)
            .height(74.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (day.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(
            width = 1.dp,
            color = if (day.isSelected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
            },
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = if (day.isToday) "Today" else day.label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = if (day.isSelected) {
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
            )
            Text(
                text = displayNumber,
                style = MaterialTheme.typography.titleMedium,
                color = if (day.isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
            )
        }
    }
}

private fun dayNumber(dayId: String, fallbackIndex: Int): String = when (dayId) {
    "sun" -> "12"
    "mon" -> "13"
    "tue" -> "14"
    "wed" -> "15"
    "thu" -> "16"
    "fri" -> "17"
    "sat" -> "18"
    else -> (12 + fallbackIndex).toString()
}

private fun initialsFor(name: String): String = name
    .trim()
    .split(" ")
    .filter { it.isNotBlank() }
    .take(2)
    .joinToString("") { it.first().uppercase() }
    .ifBlank { "CP" }
