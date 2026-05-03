package com.sortoapps.chorepilot.features.notifications.presentation.viewmodel

import com.sortoapps.chorepilot.features.notifications.domain.NotificationItem
import com.sortoapps.chorepilot.features.notifications.domain.NotificationOpenTarget
import com.sortoapps.chorepilot.features.notifications.domain.NotificationType
import com.sortoapps.chorepilot.features.notifications.domain.NotificationsRepository
import com.sortoapps.chorepilot.features.notifications.domain.NotificationsRepositoryState
import com.sortoapps.chorepilot.features.settings.domain.SettingsRepository
import com.sortoapps.chorepilot.features.settings.domain.SettingsSnapshot
import com.sortoapps.chorepilot.features.settings.domain.ThemeMode
import com.sortoapps.chorepilot.features.settings.domain.AppearancePreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {
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
    fun groupsNotificationsByRecencyAndUnreadCount() = runTest(dispatcher) {
        val repository = FakeNotificationsRepository(
            listOf(
                notification("today", createdAtEpochMs = kotlin.time.Clock.System.now().toEpochMilliseconds(), readAtEpochMs = null),
                notification("older", createdAtEpochMs = kotlin.time.Clock.System.now().toEpochMilliseconds() - 2L * 24L * 60L * 60L * 1000L, readAtEpochMs = 2L),
            ),
        )
        val viewModel = NotificationsViewModel(
            notificationsRepository = repository,
            settingsRepository = FakeVmSettingsRepository(),
        )

        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.unreadCount)
        assertTrue(viewModel.uiState.value.sections.isNotEmpty())
    }

    @Test
    fun openingDueSoonNotificationMarksItReadAndExposesChoreNavigation() = runTest(dispatcher) {
        val repository = FakeNotificationsRepository(
            listOf(
                notification(
                    id = "n1",
                    choreId = "chore-1",
                    type = NotificationType.DueSoon,
                    createdAtEpochMs = kotlin.time.Clock.System.now().toEpochMilliseconds(),
                ),
            ),
        )
        val viewModel = NotificationsViewModel(
            notificationsRepository = repository,
            settingsRepository = FakeVmSettingsRepository(),
        )

        advanceUntilIdle()
        viewModel.onEvent(NotificationsUiEvent.NotificationOpened("n1"))
        advanceUntilIdle()

        assertEquals("n1", repository.lastMarkedReadId)
        assertEquals(
            NotificationOpenTarget.Chore("chore-1"),
            viewModel.uiState.value.pendingOpenRequest?.target,
        )

        viewModel.onEvent(NotificationsUiEvent.PendingOpenHandled)
        assertNull(viewModel.uiState.value.pendingOpenRequest)
    }

    @Test
    fun openingProofNotificationMarksItReadAndKeepsInboxTarget() = runTest(dispatcher) {
        val repository = FakeNotificationsRepository(
            listOf(
                notification(
                    id = "proof-1",
                    choreId = "chore-9",
                    type = NotificationType.ProofSubmitted,
                    createdAtEpochMs = kotlin.time.Clock.System.now().toEpochMilliseconds(),
                ),
            ),
        )
        val viewModel = NotificationsViewModel(
            notificationsRepository = repository,
            settingsRepository = FakeVmSettingsRepository(),
        )

        advanceUntilIdle()
        viewModel.onEvent(NotificationsUiEvent.NotificationOpened("proof-1"))
        advanceUntilIdle()

        assertEquals("proof-1", repository.lastMarkedReadId)
        assertEquals(NotificationOpenTarget.Inbox, viewModel.uiState.value.pendingOpenRequest?.target)

        viewModel.onEvent(NotificationsUiEvent.PendingOpenHandled)
        assertNull(viewModel.uiState.value.pendingOpenRequest)
    }

    @Test
    fun openingProofApprovedNotificationKeepsInboxTarget() = runTest(dispatcher) {
        val repository = FakeNotificationsRepository(
            listOf(
                notification(
                    id = "proof-approved-1",
                    choreId = "chore-9",
                    type = NotificationType.ProofApproved,
                    createdAtEpochMs = kotlin.time.Clock.System.now().toEpochMilliseconds(),
                ),
            ),
        )
        val viewModel = NotificationsViewModel(
            notificationsRepository = repository,
            settingsRepository = FakeVmSettingsRepository(),
        )

        advanceUntilIdle()
        viewModel.onEvent(NotificationsUiEvent.NotificationOpened("proof-approved-1"))
        advanceUntilIdle()

        assertEquals("proof-approved-1", repository.lastMarkedReadId)
        assertEquals(NotificationOpenTarget.Inbox, viewModel.uiState.value.pendingOpenRequest?.target)
    }

    @Test
    fun openingProofRejectedNotificationMarksItReadAndRoutesToChoreDetail() = runTest(dispatcher) {
        val repository = FakeNotificationsRepository(
            listOf(
                notification(
                    id = "proof-rejected-1",
                    choreId = "chore-44",
                    type = NotificationType.ProofRejected,
                    createdAtEpochMs = kotlin.time.Clock.System.now().toEpochMilliseconds(),
                ),
            ),
        )
        val viewModel = NotificationsViewModel(
            notificationsRepository = repository,
            settingsRepository = FakeVmSettingsRepository(),
        )

        advanceUntilIdle()
        viewModel.onEvent(NotificationsUiEvent.NotificationOpened("proof-rejected-1"))
        advanceUntilIdle()

        assertEquals("proof-rejected-1", repository.lastMarkedReadId)
        assertEquals(
            NotificationOpenTarget.Chore("chore-44"),
            viewModel.uiState.value.pendingOpenRequest?.target,
        )
        assertEquals(0, viewModel.uiState.value.unreadCount)
    }
}

private class FakeNotificationsRepository(
    items: List<NotificationItem>,
) : NotificationsRepository {
    private val backing = MutableStateFlow(
        NotificationsRepositoryState(
            isLoading = false,
            items = items,
        ),
    )

    override val state: StateFlow<NotificationsRepositoryState> = backing

    var lastMarkedReadId: String? = null
        private set

    override suspend fun refresh() = Unit

    override suspend fun record(event: com.sortoapps.chorepilot.features.notifications.domain.NotificationEvent): NotificationItem? = null

    override suspend fun markRead(notificationId: String) {
        lastMarkedReadId = notificationId
        backing.value = backing.value.copy(
            items = backing.value.items.map { item ->
                if (item.id == notificationId) item.copy(readAtEpochMs = 1L) else item
            },
        )
    }

    override suspend fun markAllRead() {
        backing.value = backing.value.copy(
            items = backing.value.items.map { it.copy(readAtEpochMs = 1L) },
        )
    }
}

private class FakeVmSettingsRepository : SettingsRepository {
    override val settings: StateFlow<SettingsSnapshot> = MutableStateFlow(SettingsSnapshot())

    override suspend fun updateThemeMode(themeMode: ThemeMode) = Unit
    override suspend fun updateAppearancePreset(appearancePreset: AppearancePreset) = Unit
    override suspend fun updateEditorialWarmth(enabled: Boolean) = Unit
    override suspend fun updateNotifications(enabled: Boolean) = Unit
    override suspend fun updateWeekStartsOnMonday(enabled: Boolean) = Unit
    override suspend fun updateAutoAssignSuggestions(enabled: Boolean) = Unit
    override suspend fun updateLargerText(enabled: Boolean) = Unit
    override suspend fun updateAlwaysShowLabels(enabled: Boolean) = Unit
}

private fun notification(
    id: String,
    choreId: String = "chore-1",
    type: NotificationType = NotificationType.ProofSubmitted,
    createdAtEpochMs: Long,
    readAtEpochMs: Long? = null,
): NotificationItem = NotificationItem(
    id = id,
    householdId = "house-1",
    choreId = choreId,
    type = type,
    title = "Emma submitted proof",
    body = "Kitchen reset is ready for review.",
    createdAtEpochMs = createdAtEpochMs,
    readAtEpochMs = readAtEpochMs,
)
