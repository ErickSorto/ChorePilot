package com.sortoapps.chorepilot.features.notifications.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NotificationTapBridge {
    private val _pendingOpenRequest = MutableStateFlow<NotificationOpenRequest?>(null)
    val pendingOpenRequest: StateFlow<NotificationOpenRequest?> = _pendingOpenRequest.asStateFlow()

    fun queueOpenRequest(request: NotificationOpenRequest) {
        if (request.notificationId.isBlank()) return
        _pendingOpenRequest.value = request
    }

    fun queuePayload(payload: NotificationTapPayload) {
        queueOpenRequest(payload.toOpenRequest())
    }

    fun clearPendingRequest(notificationId: String? = null) {
        val current = _pendingOpenRequest.value ?: return
        if (notificationId == null || current.notificationId == notificationId) {
            _pendingOpenRequest.value = null
        }
    }
}

fun handleNotificationTap(
    notificationId: String,
    choreId: String,
    type: String,
) {
    NotificationTapBridge.queuePayload(
        NotificationTapPayload(
            notificationId = notificationId,
            choreId = choreId,
            type = notificationTypeOrDefault(type),
        ),
    )
}
