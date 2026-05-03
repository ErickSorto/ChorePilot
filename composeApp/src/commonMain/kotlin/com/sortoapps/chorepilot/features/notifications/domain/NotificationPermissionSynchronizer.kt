package com.sortoapps.chorepilot.features.notifications.domain

import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import com.sortoapps.chorepilot.features.settings.domain.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class NotificationPermissionSynchronizer(
    sessionRepository: SessionRepository,
    settingsRepository: SettingsRepository,
    notificationSettingsController: NotificationSettingsController,
    appScope: CoroutineScope,
) {
    init {
        appScope.launch {
            combine(
                sessionRepository.session,
                settingsRepository.settings,
            ) { session, settings ->
                session.signedIn && settings.notificationsEnabled
            }
                .distinctUntilChanged()
                .collect { shouldEnsurePermission ->
                    if (shouldEnsurePermission) {
                        notificationSettingsController.ensureAuthorizedForEnabledNotifications()
                    }
                }
        }
    }
}
