package com.sortoapps.chorepilot.features.notifications.presentation.viewmodel

import com.sortoapps.chorepilot.features.notifications.domain.NotificationOpenRequest

data class NotificationRowUiModel(
    val id: String,
    val choreId: String,
    val title: String,
    val body: String,
    val timestampLabel: String,
    val unread: Boolean,
    val badge: String,
)

data class NotificationSectionUiModel(
    val title: String,
    val notifications: List<NotificationRowUiModel>,
)

data class NotificationsUiState(
    val heading: String = "Household alerts",
    val supportingText: String = "Proof updates, due work, and reassignment alerts stay collected here.",
    val isLoading: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val unreadCount: Int = 0,
    val sections: List<NotificationSectionUiModel> = emptyList(),
    val emptyTitle: String = "No alerts yet",
    val emptyBody: String = "Proof updates, due work, and reassignment alerts will appear here.",
    val errorMessage: String? = null,
    val pendingOpenRequest: NotificationOpenRequest? = null,
)
