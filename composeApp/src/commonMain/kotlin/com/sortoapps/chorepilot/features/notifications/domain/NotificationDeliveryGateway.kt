package com.sortoapps.chorepilot.features.notifications.domain

enum class NotificationAuthorizationStatus {
    Granted,
    Denied,
    DeniedAlways,
    NotDetermined,
}

interface NotificationDeliveryGateway {
    suspend fun getAuthorizationStatus(): NotificationAuthorizationStatus

    suspend fun requestAuthorization(): NotificationAuthorizationStatus

    suspend fun deliver(item: NotificationItem)
}

sealed interface NotificationSettingsUpdateResult {
    data object Enabled : NotificationSettingsUpdateResult

    data object Disabled : NotificationSettingsUpdateResult

    data object PermissionDenied : NotificationSettingsUpdateResult

    data object PermissionDeniedAlways : NotificationSettingsUpdateResult
}
