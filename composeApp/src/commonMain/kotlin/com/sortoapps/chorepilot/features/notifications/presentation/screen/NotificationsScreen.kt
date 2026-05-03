package com.sortoapps.chorepilot.features.notifications.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AssignmentTurnedIn
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sortoapps.chorepilot.core.designsystem.ChorePilotGradientBackground
import com.sortoapps.chorepilot.core.designsystem.ChorePilotTheme
import com.sortoapps.chorepilot.core.designsystem.ChorePilotTopAppBar
import com.sortoapps.chorepilot.core.designsystem.ChorePilotTopBarIconButton
import com.sortoapps.chorepilot.core.preview.ChorePilotPreviewData
import com.sortoapps.chorepilot.core.preview.PreviewDeviceFrame
import com.sortoapps.chorepilot.features.notifications.presentation.viewmodel.NotificationRowUiModel
import com.sortoapps.chorepilot.features.notifications.presentation.viewmodel.NotificationSectionUiModel
import com.sortoapps.chorepilot.features.notifications.presentation.viewmodel.NotificationsUiEvent
import com.sortoapps.chorepilot.features.notifications.presentation.viewmodel.NotificationsUiState
import com.sortoapps.chorepilot.features.settings.domain.ThemeMode

private val NotificationsShape = RoundedCornerShape(8.dp)

@Composable
fun NotificationsScreen(
    state: NotificationsUiState,
    onEvent: (NotificationsUiEvent) -> Unit,
    onBack: () -> Unit,
) {
    ChorePilotGradientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                ChorePilotTopAppBar(
                    sectionTitle = "Notifications",
                    navigationIcon = {
                        ChorePilotTopBarIconButton(
                            icon = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            onClick = onBack,
                        )
                    },
                    actionIcon = {
                        if (state.unreadCount > 0) {
                            ChorePilotTopBarIconButton(
                                icon = Icons.Rounded.MarkEmailRead,
                                contentDescription = "Mark all read",
                                onClick = { onEvent(NotificationsUiEvent.MarkAllRead) },
                            )
                        }
                    },
                )
            },
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    InboxHeroCard(state = state)
                }

                when {
                    state.isLoading -> {
                        item { LoadingStateCard() }
                    }

                    state.errorMessage != null -> {
                        item {
                            EmptyStateCard(
                                title = "We couldn't load alerts",
                                body = state.errorMessage,
                                tone = EmptyStateTone.Error,
                            )
                        }
                    }

                    state.sections.isEmpty() -> {
                        item {
                            EmptyStateCard(
                                title = state.emptyTitle,
                                body = state.emptyBody,
                                tone = if (state.notificationsEnabled) EmptyStateTone.Calm else EmptyStateTone.Paused,
                            )
                        }
                    }

                    else -> {
                        state.sections.forEach { section ->
                            item {
                                NotificationSectionCard(
                                    section = section,
                                    onOpen = { notificationId ->
                                        onEvent(NotificationsUiEvent.NotificationOpened(notificationId))
                                    },
                                    onMarkRead = { notificationId ->
                                        onEvent(NotificationsUiEvent.MarkRead(notificationId))
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InboxHeroCard(
    state: NotificationsUiState,
) {
    NotificationSurface(
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(54.dp),
                shape = NotificationsShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = state.unreadCount.coerceAtMost(99).toString(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = state.heading,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = heroSummary(state),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun NotificationSectionCard(
    section: NotificationSectionUiModel,
    onOpen: (String) -> Unit,
    onMarkRead: (String) -> Unit,
) {
    NotificationSurface {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${section.notifications.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            section.notifications.forEachIndexed { index, row ->
                if (index > 0) {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                    )
                }
                NotificationRow(
                    notification = row,
                    onOpen = { onOpen(row.id) },
                    onMarkRead = { onMarkRead(row.id) },
                )
            }
        }
    }
}

@Composable
private fun NotificationRow(
    notification: NotificationRowUiModel,
    onOpen: () -> Unit,
    onMarkRead: () -> Unit,
) {
    val accent = notificationAccent(notification)
    val icon = notificationIcon(notification)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .width(4.dp)
                .height(42.dp)
                .background(
                    color = if (notification.unread) accent else MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                    shape = CircleShape,
                ),
        )

        Surface(
            modifier = Modifier.size(40.dp),
            shape = NotificationsShape,
            color = accent.copy(alpha = if (notification.unread) 0.14f else 0.08f),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (notification.unread) FontWeight.ExtraBold else FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = notification.timestampLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                NotificationBadge(
                    label = notification.badge,
                    accent = accent,
                    unread = notification.unread,
                )
            }

            Text(
                text = notification.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (notification.unread) {
                Text(
                    text = "Mark read",
                    modifier = Modifier.clickable(onClick = onMarkRead),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Read",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationBadge(
    label: String,
    accent: Color,
    unread: Boolean,
) {
    Surface(
        shape = NotificationsShape,
        color = accent.copy(alpha = if (unread) 0.12f else 0.08f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.12f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun LoadingStateCard() {
    NotificationSurface {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Loading alerts",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Pulling in the latest household updates.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private enum class EmptyStateTone {
    Calm,
    Paused,
    Error,
}

@Composable
private fun EmptyStateCard(
    title: String,
    body: String,
    tone: EmptyStateTone,
) {
    val accent = when (tone) {
        EmptyStateTone.Calm -> MaterialTheme.colorScheme.primary
        EmptyStateTone.Paused -> MaterialTheme.colorScheme.onSurfaceVariant
        EmptyStateTone.Error -> MaterialTheme.colorScheme.error
    }

    NotificationSurface(
        borderColor = accent.copy(alpha = 0.14f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = NotificationsShape,
                color = accent.copy(alpha = 0.12f),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (tone) {
                            EmptyStateTone.Calm -> Icons.Rounded.NotificationsActive
                            EmptyStateTone.Paused -> Icons.Rounded.MarkEmailRead
                            EmptyStateTone.Error -> Icons.Rounded.NotificationsActive
                        },
                        contentDescription = null,
                        tint = accent,
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
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
private fun NotificationSurface(
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = NotificationsShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        content()
    }
}

private fun heroSummary(state: NotificationsUiState): String = when {
    state.isLoading -> "Checking for the latest household updates."
    !state.notificationsEnabled -> "Inbox history stays visible, but new alerts are paused right now."
    state.unreadCount == 0 -> "Everything important has already been seen."
    else -> "${state.unreadCount} alert${if (state.unreadCount == 1) "" else "s"} still need a quick look."
}

@Composable
private fun notificationAccent(notification: NotificationRowUiModel): Color {
    val label = notification.badge.lowercase()
    return when {
        "approved" in label || "complete" in label || "live" in label -> MaterialTheme.colorScheme.secondary
        "sent back" in label || "needs proof" in label -> MaterialTheme.colorScheme.error
        "reassign" in label || "cover" in label -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
}

private fun notificationIcon(notification: NotificationRowUiModel): ImageVector {
    val label = notification.badge.lowercase()
    return when {
        "approved" in label || "complete" in label || "live" in label -> Icons.Rounded.CheckCircle
        "review" in label -> Icons.Rounded.AutoFixHigh
        "sent back" in label || "needs proof" in label -> Icons.Rounded.Close
        "reassign" in label || "cover" in label -> Icons.Rounded.Autorenew
        "due" in label || "overdue" in label -> Icons.Rounded.Schedule
        else -> Icons.Rounded.AssignmentTurnedIn
    }
}

@Composable
private fun NotificationsPreview(
    state: NotificationsUiState,
    themeMode: ThemeMode = ThemeMode.Light,
) {
    ChorePilotTheme(themeMode = themeMode) {
        PreviewDeviceFrame { modifier ->
            Box(modifier = modifier.fillMaxSize()) {
                NotificationsScreen(
                    state = state,
                    onEvent = {},
                    onBack = {},
                )
            }
        }
    }
}

@Preview(name = "Notifications Unread", widthDp = 393, heightDp = 852)
@Composable
private fun NotificationsScreenPreview() {
    NotificationsPreview(state = ChorePilotPreviewData.notificationsState())
}

@Preview(name = "Notifications Unread Dark", widthDp = 393, heightDp = 852)
@Composable
private fun NotificationsScreenDarkPreview() {
    NotificationsPreview(
        state = ChorePilotPreviewData.notificationsState(),
        themeMode = ThemeMode.Dark,
    )
}

@Preview(name = "Notifications Empty", widthDp = 393, heightDp = 852)
@Composable
private fun NotificationsScreenEmptyPreview() {
    NotificationsPreview(
        state = NotificationsUiState(
            isLoading = false,
            sections = emptyList(),
        ),
    )
}

@Preview(name = "Notifications Empty Dark", widthDp = 393, heightDp = 852)
@Composable
private fun NotificationsScreenEmptyDarkPreview() {
    NotificationsPreview(
        state = NotificationsUiState(
            isLoading = false,
            sections = emptyList(),
        ),
        themeMode = ThemeMode.Dark,
    )
}

@Preview(name = "Notifications Paused", widthDp = 393, heightDp = 852)
@Composable
private fun NotificationsScreenPausedPreview() {
    NotificationsPreview(
        state = NotificationsUiState(
            isLoading = false,
            notificationsEnabled = false,
            sections = emptyList(),
            emptyTitle = "Notifications are paused",
            emptyBody = "Existing inbox items stay here, but new household alerts won't be added while notifications are off.",
        ),
    )
}

@Preview(name = "Notifications Loading", widthDp = 393, heightDp = 852)
@Composable
private fun NotificationsScreenLoadingPreview() {
    NotificationsPreview(
        state = NotificationsUiState(isLoading = true),
    )
}
