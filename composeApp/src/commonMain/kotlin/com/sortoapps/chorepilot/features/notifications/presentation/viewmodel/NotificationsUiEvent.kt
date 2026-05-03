package com.sortoapps.chorepilot.features.notifications.presentation.viewmodel

sealed interface NotificationsUiEvent {
    data object Refresh : NotificationsUiEvent

    data object MarkAllRead : NotificationsUiEvent

    data class NotificationOpened(val notificationId: String) : NotificationsUiEvent

    data class MarkRead(val notificationId: String) : NotificationsUiEvent

    data object PendingOpenHandled : NotificationsUiEvent
}
