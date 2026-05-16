package com.sortoapps.chorepilot.features.notifications.domain

import com.sortoapps.chorepilot.features.feedbackpal.domain.FeedbackPalKind
import com.sortoapps.chorepilot.features.settings.domain.SettingsRepository
import com.sortoapps.chorepilot.features.settings.domain.SettingsSnapshot
import com.sortoapps.chorepilot.features.settings.domain.ThemeMode
import com.sortoapps.chorepilot.features.settings.domain.AppearancePreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationSettingsControllerTest {
    @Test
    fun permissionGrantedEnablesNotifications() = runTest {
        val settingsRepository = FakeControllerSettingsRepository(
            SettingsSnapshot(notificationsEnabled = false),
        )
        val gateway = FakeControllerGateway(
            status = NotificationAuthorizationStatus.NotDetermined,
            requestResult = NotificationAuthorizationStatus.Granted,
        )
        val controller = NotificationSettingsController(settingsRepository, gateway)

        val result = controller.setNotificationsEnabled(true)

        assertEquals(NotificationSettingsUpdateResult.Enabled, result)
        assertTrue(settingsRepository.settings.value.notificationsEnabled)
        assertEquals(1, gateway.requestCount)
    }

    @Test
    fun permissionDeniedLeavesNotificationsOff() = runTest {
        val settingsRepository = FakeControllerSettingsRepository(
            SettingsSnapshot(notificationsEnabled = false),
        )
        val gateway = FakeControllerGateway(
            status = NotificationAuthorizationStatus.NotDetermined,
            requestResult = NotificationAuthorizationStatus.Denied,
        )
        val controller = NotificationSettingsController(settingsRepository, gateway)

        val result = controller.setNotificationsEnabled(true)

        assertEquals(NotificationSettingsUpdateResult.PermissionDenied, result)
        assertFalse(settingsRepository.settings.value.notificationsEnabled)
        assertEquals(1, gateway.requestCount)
    }

    @Test
    fun disablingNotificationsSkipsPermissionRequest() = runTest {
        val settingsRepository = FakeControllerSettingsRepository(
            SettingsSnapshot(notificationsEnabled = true),
        )
        val gateway = FakeControllerGateway(
            status = NotificationAuthorizationStatus.Granted,
        )
        val controller = NotificationSettingsController(settingsRepository, gateway)

        val result = controller.setNotificationsEnabled(false)

        assertEquals(NotificationSettingsUpdateResult.Disabled, result)
        assertFalse(settingsRepository.settings.value.notificationsEnabled)
        assertEquals(0, gateway.requestCount)
    }

    @Test
    fun startupBootstrapRequestsPermissionWhenNotificationsAreEnabled() = runTest {
        val settingsRepository = FakeControllerSettingsRepository(
            SettingsSnapshot(notificationsEnabled = true),
        )
        val gateway = FakeControllerGateway(
            status = NotificationAuthorizationStatus.NotDetermined,
            requestResult = NotificationAuthorizationStatus.Granted,
        )
        val controller = NotificationSettingsController(settingsRepository, gateway)

        controller.ensureAuthorizedForEnabledNotifications()

        assertTrue(settingsRepository.settings.value.notificationsEnabled)
        assertEquals(1, gateway.requestCount)
    }

    @Test
    fun startupBootstrapTurnsNotificationsOffWhenPermissionIsDenied() = runTest {
        val settingsRepository = FakeControllerSettingsRepository(
            SettingsSnapshot(notificationsEnabled = true),
        )
        val gateway = FakeControllerGateway(
            status = NotificationAuthorizationStatus.Denied,
        )
        val controller = NotificationSettingsController(settingsRepository, gateway)

        controller.ensureAuthorizedForEnabledNotifications()

        assertFalse(settingsRepository.settings.value.notificationsEnabled)
        assertEquals(0, gateway.requestCount)
    }
}

private class FakeControllerSettingsRepository(
    initial: SettingsSnapshot,
) : SettingsRepository {
    private val state = MutableStateFlow(initial)
    override val settings: StateFlow<SettingsSnapshot> = state

    override suspend fun updateThemeMode(themeMode: ThemeMode) = Unit
    override suspend fun updateAppearancePreset(appearancePreset: AppearancePreset) = Unit

    override suspend fun updateEditorialWarmth(enabled: Boolean) = Unit

    override suspend fun updateNotifications(enabled: Boolean) {
        state.value = state.value.copy(notificationsEnabled = enabled)
    }

    override suspend fun updateWeekStartsOnMonday(enabled: Boolean) = Unit

    override suspend fun updateAutoAssignSuggestions(enabled: Boolean) = Unit

    override suspend fun updateLargerText(enabled: Boolean) = Unit

    override suspend fun updateAlwaysShowLabels(enabled: Boolean) = Unit
    override suspend fun updateFeedbackPalKind(kind: FeedbackPalKind) {
        TODO("Not yet implemented")
    }
}

private class FakeControllerGateway(
    private var status: NotificationAuthorizationStatus,
    private val requestResult: NotificationAuthorizationStatus = status,
) : NotificationDeliveryGateway {
    var requestCount: Int = 0
        private set

    override suspend fun getAuthorizationStatus(): NotificationAuthorizationStatus = status

    override suspend fun requestAuthorization(): NotificationAuthorizationStatus {
        requestCount += 1
        status = requestResult
        return status
    }

    override suspend fun deliver(item: NotificationItem) = Unit
}
