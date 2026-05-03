package com.sortoapps.chorepilot.features.notifications.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sortoapps.chorepilot.features.notifications.domain.NotificationItem
import com.sortoapps.chorepilot.features.notifications.domain.NotificationRecencyGroup
import com.sortoapps.chorepilot.features.notifications.domain.NotificationType
import com.sortoapps.chorepilot.features.notifications.domain.NotificationsRepository
import com.sortoapps.chorepilot.features.notifications.domain.NotificationsRepositoryState
import com.sortoapps.chorepilot.features.notifications.domain.toOpenRequest
import com.sortoapps.chorepilot.features.settings.domain.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class NotificationsViewModel(
    private val notificationsRepository: NotificationsRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                notificationsRepository.state,
                settingsRepository.settings,
            ) { repositoryState, settings ->
                repositoryState to settings.notificationsEnabled
            }.collectLatest { (repositoryState, notificationsEnabled) ->
                applyState(repositoryState, notificationsEnabled)
            }
        }
        viewModelScope.launch {
            notificationsRepository.refresh()
        }
    }

    fun onEvent(event: NotificationsUiEvent) {
        when (event) {
            NotificationsUiEvent.Refresh -> {
                viewModelScope.launch { notificationsRepository.refresh() }
            }

            NotificationsUiEvent.MarkAllRead -> {
                viewModelScope.launch { notificationsRepository.markAllRead() }
            }

            is NotificationsUiEvent.MarkRead -> {
                viewModelScope.launch { notificationsRepository.markRead(event.notificationId) }
            }

            is NotificationsUiEvent.NotificationOpened -> {
                val item = notificationsRepository.state.value.items.firstOrNull { it.id == event.notificationId } ?: return
                viewModelScope.launch {
                    notificationsRepository.markRead(event.notificationId)
                }
                _uiState.update { it.copy(pendingOpenRequest = item.toOpenRequest()) }
            }

            NotificationsUiEvent.PendingOpenHandled -> {
                _uiState.update { it.copy(pendingOpenRequest = null) }
            }
        }
    }

    private fun applyState(
        repositoryState: NotificationsRepositoryState,
        notificationsEnabled: Boolean,
    ) {
        val sections = repositoryState.items
            .groupBy(::recencyGroup)
            .entries
            .sortedBy { it.key.ordinal }
            .map { (group, items) ->
                NotificationSectionUiModel(
                    title = when (group) {
                        NotificationRecencyGroup.Today -> "Today"
                        NotificationRecencyGroup.EarlierThisWeek -> "Earlier this week"
                        NotificationRecencyGroup.Earlier -> "Earlier"
                    },
                    notifications = items.sortedByDescending { it.createdAtEpochMs }.map(::toRowUiModel),
                )
            }

        _uiState.update { current ->
            current.copy(
                isLoading = repositoryState.isLoading,
                notificationsEnabled = notificationsEnabled,
                unreadCount = repositoryState.items.count { it.readAtEpochMs == null },
                sections = sections,
                emptyTitle = if (notificationsEnabled) {
                    "No alerts yet"
                } else {
                    "Notifications are paused"
                },
                emptyBody = if (notificationsEnabled) {
                    "Proof updates, due work, and reassignment alerts will appear here."
                } else {
                    "Inbox updates still sync here, but this device will stay quiet while notifications are off."
                },
                errorMessage = repositoryState.errorMessage,
            )
        }
    }
}

private fun recencyGroup(item: NotificationItem): NotificationRecencyGroup {
    val timeZone = TimeZone.currentSystemDefault()
    val createdDate = kotlin.time.Instant.fromEpochMilliseconds(item.createdAtEpochMs).toLocalDateTime(timeZone).date
    val today = Clock.System.now().toLocalDateTime(timeZone).date
    return when {
        createdDate == today -> NotificationRecencyGroup.Today
        createdDate >= today.minus(DatePeriod(days = 7)) -> NotificationRecencyGroup.EarlierThisWeek
        else -> NotificationRecencyGroup.Earlier
    }
}

private fun toRowUiModel(item: NotificationItem): NotificationRowUiModel = NotificationRowUiModel(
    id = item.id,
    choreId = item.choreId,
    title = item.title,
    body = item.body,
    timestampLabel = timeAgoLabel(item.createdAtEpochMs),
    unread = item.readAtEpochMs == null,
    badge = when (item.type) {
        NotificationType.ProofSubmitted -> "Proof waiting"
        NotificationType.ProofApproved -> "Approved"
        NotificationType.ProofRejected -> "Needs proof"
        NotificationType.DiscoverySubmitted -> "Needs review"
        NotificationType.DiscoveryApproved -> "Live"
        NotificationType.DiscoveryRejected -> "Sent back"
        NotificationType.DueSoon -> "Due soon"
        NotificationType.Overdue -> "Overdue"
        NotificationType.AssignmentChanged -> "Reassigned"
    },
)

private fun timeAgoLabel(createdAtEpochMs: Long): String {
    val deltaMinutes = ((Clock.System.now().toEpochMilliseconds() - createdAtEpochMs) / 60_000L).coerceAtLeast(0L)
    return when {
        deltaMinutes < 1L -> "Just now"
        deltaMinutes < 60L -> "$deltaMinutes min ago"
        deltaMinutes < 24L * 60L -> "${deltaMinutes / 60L} hr ago"
        else -> "${deltaMinutes / (24L * 60L)} day ago"
    }
}
