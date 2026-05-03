package com.sortoapps.chorepilot.features.settings.presentation.viewmodel

import com.sortoapps.chorepilot.core.platform.VibratorUtil
import com.sortoapps.chorepilot.features.auth.domain.AuthFlowResult
import com.sortoapps.chorepilot.features.auth.domain.HouseholdRole
import com.sortoapps.chorepilot.features.auth.domain.InvitePreview
import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import com.sortoapps.chorepilot.features.auth.domain.SessionSnapshot
import com.sortoapps.chorepilot.features.notifications.domain.NotificationAuthorizationStatus
import com.sortoapps.chorepilot.features.notifications.domain.NotificationDeliveryGateway
import com.sortoapps.chorepilot.features.notifications.domain.NotificationItem
import com.sortoapps.chorepilot.features.notifications.domain.NotificationSettingsController
import com.sortoapps.chorepilot.features.settings.domain.SettingsRepository
import com.sortoapps.chorepilot.features.settings.domain.SettingsSnapshot
import com.sortoapps.chorepilot.features.settings.domain.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun selectingThemeUpdatesRepositoryAndShowsTransientFeedback() = runTest(dispatcher) {
        val settingsRepository = FakeSettingsRepository()
        val viewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            notificationSettingsController = NotificationSettingsController(
                settingsRepository = settingsRepository,
                deliveryGateway = FakeNotificationGateway(NotificationAuthorizationStatus.Granted),
            ),
            sessionRepository = FakeSessionRepository(),
            vibratorUtil = FakeVibratorUtil(),
        )

        advanceUntilIdle()
        viewModel.onEvent(SettingsUiEvent.ThemeModeSelected(ThemeMode.Dark))
        runCurrent()

        assertEquals(ThemeMode.Dark, settingsRepository.settings.value.themeMode)
        assertEquals("Theme saved", viewModel.uiState.value.saveFeedbackMessage)

        advanceTimeBy(1600L)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.saveFeedbackMessage)
    }

    @Test
    fun disablingNotificationsShowsSavedFeedback() = runTest(dispatcher) {
        val settingsRepository = FakeSettingsRepository()
        val viewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            notificationSettingsController = NotificationSettingsController(
                settingsRepository = settingsRepository,
                deliveryGateway = FakeNotificationGateway(NotificationAuthorizationStatus.Granted),
            ),
            sessionRepository = FakeSessionRepository(),
            vibratorUtil = FakeVibratorUtil(),
        )

        advanceUntilIdle()
        viewModel.onEvent(SettingsUiEvent.ToggleNotifications(false))
        runCurrent()

        assertFalse(settingsRepository.settings.value.notificationsEnabled)
        assertEquals("Notifications turned off", viewModel.uiState.value.saveFeedbackMessage)
    }

    @Test
    fun enablingNotificationsRequestsPermissionAndPersistsWhenGranted() = runTest(dispatcher) {
        val settingsRepository = FakeSettingsRepository(
            initial = SettingsSnapshot(notificationsEnabled = false),
        )
        val gateway = FakeNotificationGateway(
            initialStatus = NotificationAuthorizationStatus.NotDetermined,
            requestResult = NotificationAuthorizationStatus.Granted,
        )
        val viewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            notificationSettingsController = NotificationSettingsController(
                settingsRepository = settingsRepository,
                deliveryGateway = gateway,
            ),
            sessionRepository = FakeSessionRepository(),
            vibratorUtil = FakeVibratorUtil(),
        )

        advanceUntilIdle()
        viewModel.onEvent(SettingsUiEvent.ToggleNotifications(true))
        runCurrent()

        assertEquals(1, gateway.requestCount)
        assertEquals(true, settingsRepository.settings.value.notificationsEnabled)
        assertEquals("Notifications enabled", viewModel.uiState.value.saveFeedbackMessage)
        assertFalse(viewModel.uiState.value.isUpdatingNotifications)
    }

    @Test
    fun enablingNotificationsAutoRevertsWhenPermissionDenied() = runTest(dispatcher) {
        val settingsRepository = FakeSettingsRepository(
            initial = SettingsSnapshot(notificationsEnabled = false),
        )
        val gateway = FakeNotificationGateway(
            initialStatus = NotificationAuthorizationStatus.NotDetermined,
            requestResult = NotificationAuthorizationStatus.DeniedAlways,
        )
        val viewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            notificationSettingsController = NotificationSettingsController(
                settingsRepository = settingsRepository,
                deliveryGateway = gateway,
            ),
            sessionRepository = FakeSessionRepository(),
            vibratorUtil = FakeVibratorUtil(),
        )

        advanceUntilIdle()
        viewModel.onEvent(SettingsUiEvent.ToggleNotifications(true))
        runCurrent()

        assertEquals(1, gateway.requestCount)
        assertFalse(settingsRepository.settings.value.notificationsEnabled)
        assertEquals(
            "Turn on notifications in system settings to enable them here",
            viewModel.uiState.value.saveFeedbackMessage,
        )
    }

    @Test
    fun togglingWeekStartUpdatesRepositoryAndFeedback() = runTest(dispatcher) {
        val settingsRepository = FakeSettingsRepository()
        val viewModel = settingsViewModel(settingsRepository = settingsRepository)

        advanceUntilIdle()
        viewModel.onEvent(SettingsUiEvent.ToggleWeekStartsOnMonday(true))
        runCurrent()

        assertEquals(true, settingsRepository.settings.value.weekStartsOnMonday)
        assertEquals("Weekly reports now start on Monday", viewModel.uiState.value.saveFeedbackMessage)
    }

    @Test
    fun togglingLargerTextUpdatesRepositoryAndFeedback() = runTest(dispatcher) {
        val settingsRepository = FakeSettingsRepository()
        val viewModel = settingsViewModel(settingsRepository = settingsRepository)

        advanceUntilIdle()
        viewModel.onEvent(SettingsUiEvent.ToggleLargerText(true))
        runCurrent()

        assertEquals(true, settingsRepository.settings.value.largerText)
        assertEquals("Larger text enabled", viewModel.uiState.value.saveFeedbackMessage)
    }

    @Test
    fun togglingAlwaysShowLabelsUpdatesRepositoryAndFeedback() = runTest(dispatcher) {
        val settingsRepository = FakeSettingsRepository()
        val viewModel = settingsViewModel(settingsRepository = settingsRepository)

        advanceUntilIdle()
        viewModel.onEvent(SettingsUiEvent.ToggleAlwaysShowLabels(false))
        runCurrent()

        assertFalse(settingsRepository.settings.value.alwaysShowLabels)
        assertEquals("Bottom navigation labels now stay compact", viewModel.uiState.value.saveFeedbackMessage)
    }

    private fun settingsViewModel(
        settingsRepository: FakeSettingsRepository = FakeSettingsRepository(),
        gateway: FakeNotificationGateway = FakeNotificationGateway(NotificationAuthorizationStatus.Granted),
    ): SettingsViewModel = SettingsViewModel(
        settingsRepository = settingsRepository,
        notificationSettingsController = NotificationSettingsController(
            settingsRepository = settingsRepository,
            deliveryGateway = gateway,
        ),
        sessionRepository = FakeSessionRepository(),
        vibratorUtil = FakeVibratorUtil(),
    )
}

private class FakeSettingsRepository(
    initial: SettingsSnapshot = SettingsSnapshot(),
) : SettingsRepository {
    private val state = MutableStateFlow(initial)
    override val settings: StateFlow<SettingsSnapshot> = state

    override suspend fun updateThemeMode(themeMode: ThemeMode) {
        state.value = state.value.copy(themeMode = themeMode)
    }

    override suspend fun updateEditorialWarmth(enabled: Boolean) {
        state.value = state.value.copy(editorialWarmthEnabled = enabled)
    }

    override suspend fun updateNotifications(enabled: Boolean) {
        state.value = state.value.copy(notificationsEnabled = enabled)
    }

    override suspend fun updateWeekStartsOnMonday(enabled: Boolean) {
        state.value = state.value.copy(weekStartsOnMonday = enabled)
    }

    override suspend fun updateAutoAssignSuggestions(enabled: Boolean) {
        state.value = state.value.copy(autoAssignSuggestions = enabled)
    }

    override suspend fun updateLargerText(enabled: Boolean) {
        state.value = state.value.copy(largerText = enabled)
    }

    override suspend fun updateAlwaysShowLabels(enabled: Boolean) {
        state.value = state.value.copy(alwaysShowLabels = enabled)
    }
}

private class FakeNotificationGateway(
    initialStatus: NotificationAuthorizationStatus,
    private val requestResult: NotificationAuthorizationStatus = initialStatus,
) : NotificationDeliveryGateway {
    private var status = initialStatus

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

private class FakeSessionRepository : SessionRepository {
    override val session: StateFlow<SessionSnapshot> = MutableStateFlow(
        SessionSnapshot(
            signedIn = true,
            userId = "erick",
            displayName = "Erick",
            householdId = "house-1",
            householdName = "River House",
            role = HouseholdRole.Owner,
        ),
    )

    override suspend fun refreshSession() = Unit

    override suspend fun completeOnboarding(displayName: String) = Unit

    override suspend fun signIn(email: String, password: String, displayNameHint: String): AuthFlowResult = AuthFlowResult.Main

    override suspend fun signInWithGoogle(
        idToken: String,
        accessToken: String?,
        displayNameHint: String,
        emailHint: String,
    ): AuthFlowResult = AuthFlowResult.Main

    override suspend fun createHousehold(
        displayName: String,
        householdName: String,
        householdSizeLabel: String,
    ): AuthFlowResult = AuthFlowResult.Main

    override suspend fun joinHousehold(displayName: String, inviteCode: String): AuthFlowResult = AuthFlowResult.Main

    override suspend fun previewInvite(inviteCode: String): InvitePreview? = null

    override suspend fun dismissInitialPremiumFlow() = Unit

    override suspend fun signOut() = Unit

    override suspend fun resetDemo() = Unit
}

private class FakeVibratorUtil : VibratorUtil {
    override fun triggerVibration() = Unit

    override fun triggerVibrationSuccess() = Unit

    override fun cancelVibration() = Unit
}
