package com.sortoapps.chorepilot.features.notifications.domain

import com.sortoapps.chorepilot.features.settings.domain.SettingsRepository

class NotificationSettingsController(
    private val settingsRepository: SettingsRepository,
    private val deliveryGateway: NotificationDeliveryGateway,
) {
    suspend fun setNotificationsEnabled(enabled: Boolean): NotificationSettingsUpdateResult {
        if (!enabled) {
            settingsRepository.updateNotifications(false)
            return NotificationSettingsUpdateResult.Disabled
        }

        val finalStatus = when (deliveryGateway.getAuthorizationStatus()) {
            NotificationAuthorizationStatus.Granted -> NotificationAuthorizationStatus.Granted
            NotificationAuthorizationStatus.NotDetermined,
            NotificationAuthorizationStatus.Denied,
            NotificationAuthorizationStatus.DeniedAlways -> deliveryGateway.requestAuthorization()
        }

        return when (finalStatus) {
            NotificationAuthorizationStatus.Granted -> {
                settingsRepository.updateNotifications(true)
                NotificationSettingsUpdateResult.Enabled
            }

            NotificationAuthorizationStatus.DeniedAlways -> {
                settingsRepository.updateNotifications(false)
                NotificationSettingsUpdateResult.PermissionDeniedAlways
            }

            NotificationAuthorizationStatus.Denied,
            NotificationAuthorizationStatus.NotDetermined -> {
                settingsRepository.updateNotifications(false)
                NotificationSettingsUpdateResult.PermissionDenied
            }
        }
    }

    suspend fun reconcileWithSystem() {
        if (!settingsRepository.settings.value.notificationsEnabled) return
        if (deliveryGateway.getAuthorizationStatus() != NotificationAuthorizationStatus.Granted) {
            settingsRepository.updateNotifications(false)
        }
    }

    suspend fun ensureAuthorizedForEnabledNotifications() {
        if (!settingsRepository.settings.value.notificationsEnabled) return
        val status = deliveryGateway.getAuthorizationStatus()
        val finalStatus = when (status) {
            NotificationAuthorizationStatus.Granted -> NotificationAuthorizationStatus.Granted
            NotificationAuthorizationStatus.NotDetermined -> deliveryGateway.requestAuthorization()
            NotificationAuthorizationStatus.Denied,
            NotificationAuthorizationStatus.DeniedAlways -> status
        }
        if (finalStatus != NotificationAuthorizationStatus.Granted) {
            settingsRepository.updateNotifications(false)
        }
    }
}
