package com.sortoapps.chorepilot.features.notifications.data

import com.sortoapps.chorepilot.features.auth.domain.AuthFlowResult
import com.sortoapps.chorepilot.features.auth.domain.HouseholdRole
import com.sortoapps.chorepilot.features.auth.domain.InvitePreview
import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import com.sortoapps.chorepilot.features.auth.domain.SessionSnapshot
import com.sortoapps.chorepilot.features.feedbackpal.domain.FeedbackPalKind
import com.sortoapps.chorepilot.features.notifications.domain.NotificationEvent
import com.sortoapps.chorepilot.features.notifications.domain.NotificationAuthorizationStatus
import com.sortoapps.chorepilot.features.notifications.domain.NotificationDeliveryGateway
import com.sortoapps.chorepilot.features.notifications.domain.NotificationItem
import com.sortoapps.chorepilot.features.notifications.domain.ProofApprovedNotification
import com.sortoapps.chorepilot.features.notifications.domain.ProofRejectedNotification
import com.sortoapps.chorepilot.features.notifications.domain.ProofSubmittedNotification
import com.sortoapps.chorepilot.features.notifications.domain.RepositoryBackedHouseholdNotifications
import com.sortoapps.chorepilot.features.notifications.domain.AppVisibilityTracker
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class StorageBackedNotificationsRepositoryTest {
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
    fun proofSubmittedCreatesUnreadNotificationWhenEnabled() = runTest(dispatcher) {
        val deliveryGateway = FakeNotificationDeliveryGateway()
        val repository = StorageBackedNotificationsRepository(
            persistence = FakeNotificationsStorePersistence(),
            sessionRepository = FakeNotificationsSessionRepository(),
            appScope = backgroundScope,
        )
        val lifecycle = RepositoryBackedHouseholdNotifications(
            notificationsRepository = repository,
            deliveryGateway = deliveryGateway,
            appVisibilityTracker = AppVisibilityTracker(),
            sessionRepository = FakeNotificationsSessionRepository(),
            notificationsEnabled = { true },
        )

        advanceUntilIdle()
        lifecycle.onProofSubmitted(
            ProofSubmittedNotification(
                householdId = "house-1",
                choreId = "chore-1",
                choreTitle = "Kitchen reset",
                dateKey = "2026-04-10",
                assigneeName = "Emma",
                submittingMemberName = "Emma",
            ),
        )
        advanceUntilIdle()

        val item = repository.state.value.items.firstOrNull()
        assertNotNull(item)
        assertEquals("chore-1", item.choreId)
        assertNull(item.readAtEpochMs)
        assertEquals(item, deliveryGateway.delivered.single())
    }

    @Test
    fun notificationsDisabledKeepsRecordingButSkipsNativeDelivery() = runTest(dispatcher) {
        val deliveryGateway = FakeNotificationDeliveryGateway()
        val repository = StorageBackedNotificationsRepository(
            persistence = FakeNotificationsStorePersistence(),
            sessionRepository = FakeNotificationsSessionRepository(),
            appScope = backgroundScope,
        )
        repository.record(
            NotificationEvent.ProofSubmitted(
                householdId = "house-1",
                choreId = "seed",
                choreTitle = "Seed chore",
                dateKey = "2026-04-10",
                assigneeName = "Emma",
                submittingMemberName = "Emma",
                createdAtEpochMs = 1L,
            ),
        )
        val lifecycle = RepositoryBackedHouseholdNotifications(
            notificationsRepository = repository,
            deliveryGateway = deliveryGateway,
            appVisibilityTracker = AppVisibilityTracker(),
            sessionRepository = FakeNotificationsSessionRepository(),
            notificationsEnabled = { false },
        )

        advanceUntilIdle()
        lifecycle.onProofApproved(
            ProofApprovedNotification(
                householdId = "house-1",
                choreId = "chore-1",
                choreTitle = "Kitchen reset",
                dateKey = "2026-04-10",
                assigneeName = "Emma",
                submittingMemberName = "Emma",
                reviewerName = "Erick",
                completionOutcome = "approved_completed",
                recipientUserId = "emma-user",
                awardedPoints = 25,
            ),
        )
        advanceUntilIdle()

        assertEquals(2, repository.state.value.items.size)
        assertEquals("chore-1", repository.state.value.items.first().choreId)
        assertEquals(emptyList(), deliveryGateway.delivered)
    }

    @Test
    fun proofApprovedDeliversTheRecordedNotificationItem() = runTest(dispatcher) {
        val deliveryGateway = FakeNotificationDeliveryGateway()
        val appVisibilityTracker = AppVisibilityTracker()
        val repository = StorageBackedNotificationsRepository(
            persistence = FakeNotificationsStorePersistence(),
            sessionRepository = FakeNotificationsSessionRepository(),
            appScope = backgroundScope,
        )
        val lifecycle = RepositoryBackedHouseholdNotifications(
            notificationsRepository = repository,
            deliveryGateway = deliveryGateway,
            appVisibilityTracker = appVisibilityTracker,
            sessionRepository = FakeNotificationsSessionRepository(),
            notificationsEnabled = { true },
        )

        advanceUntilIdle()
        lifecycle.onProofApproved(
            ProofApprovedNotification(
                householdId = "house-1",
                choreId = "approved-1",
                choreTitle = "Kitchen reset",
                dateKey = "2026-04-10",
                assigneeName = "Emma",
                submittingMemberName = "Emma",
                reviewerName = "Erick",
                completionOutcome = "approved_completed",
                recipientUserId = "emma-user",
                awardedPoints = 25,
            ),
        )
        advanceUntilIdle()

        assertEquals("approved-1", repository.state.value.items.firstOrNull()?.choreId)
        assertEquals(repository.state.value.items.firstOrNull(), deliveryGateway.delivered.single())
    }

    @Test
    fun proofApprovedWhileAppIsOpenUpdatesInboxWithoutNativeDelivery() = runTest(dispatcher) {
        val deliveryGateway = FakeNotificationDeliveryGateway()
        val appVisibilityTracker = AppVisibilityTracker().apply { setForeground(true) }
        val repository = StorageBackedNotificationsRepository(
            persistence = FakeNotificationsStorePersistence(),
            sessionRepository = FakeNotificationsSessionRepository(),
            appScope = backgroundScope,
        )
        val lifecycle = RepositoryBackedHouseholdNotifications(
            notificationsRepository = repository,
            deliveryGateway = deliveryGateway,
            appVisibilityTracker = appVisibilityTracker,
            sessionRepository = FakeNotificationsSessionRepository(),
            notificationsEnabled = { true },
        )

        lifecycle.onProofApproved(
            ProofApprovedNotification(
                householdId = "house-1",
                choreId = "approved-foreground",
                choreTitle = "Kitchen reset",
                dateKey = "2026-04-10",
                assigneeName = "Emma",
                submittingMemberName = "Emma",
                reviewerName = "Erick",
                completionOutcome = "approved_completed",
                recipientUserId = "emma-user",
                awardedPoints = 25,
            ),
        )
        advanceUntilIdle()

        assertEquals("approved-foreground", repository.state.value.items.firstOrNull()?.choreId)
        assertEquals(emptyList(), deliveryGateway.delivered)
    }

    @Test
    fun proofRejectedDeliversWhileAppIsBackgrounded() = runTest(dispatcher) {
        val deliveryGateway = FakeNotificationDeliveryGateway()
        val repository = StorageBackedNotificationsRepository(
            persistence = FakeNotificationsStorePersistence(),
            sessionRepository = FakeNotificationsSessionRepository(),
            appScope = backgroundScope,
        )
        val lifecycle = RepositoryBackedHouseholdNotifications(
            notificationsRepository = repository,
            deliveryGateway = deliveryGateway,
            appVisibilityTracker = AppVisibilityTracker(),
            sessionRepository = FakeNotificationsSessionRepository(),
            notificationsEnabled = { true },
        )

        lifecycle.onProofRejected(
            ProofRejectedNotification(
                householdId = "house-1",
                choreId = "rejected-bg",
                choreTitle = "Kitchen reset",
                dateKey = "2026-04-16",
                assigneeName = "Emma",
                submittingMemberName = "Emma",
                reviewerName = "Erick",
                rejectionReason = "Please include the stovetop too.",
                recipientUserId = "emma-user",
            ),
        )
        advanceUntilIdle()

        assertEquals("rejected-bg", repository.state.value.items.firstOrNull()?.choreId)
        assertEquals(repository.state.value.items.firstOrNull(), deliveryGateway.delivered.single())
    }

    @Test
    fun proofRejectedStillDeliversWhileAppIsForegrounded() = runTest(dispatcher) {
        val deliveryGateway = FakeNotificationDeliveryGateway()
        val appVisibilityTracker = AppVisibilityTracker().apply { setForeground(true) }
        val repository = StorageBackedNotificationsRepository(
            persistence = FakeNotificationsStorePersistence(),
            sessionRepository = FakeNotificationsSessionRepository(),
            appScope = backgroundScope,
        )
        val lifecycle = RepositoryBackedHouseholdNotifications(
            notificationsRepository = repository,
            deliveryGateway = deliveryGateway,
            appVisibilityTracker = appVisibilityTracker,
            sessionRepository = FakeNotificationsSessionRepository(),
            notificationsEnabled = { true },
        )

        lifecycle.onProofRejected(
            ProofRejectedNotification(
                householdId = "house-1",
                choreId = "rejected-fg",
                choreTitle = "Kitchen reset",
                dateKey = "2026-04-16",
                assigneeName = "Emma",
                submittingMemberName = "Emma",
                reviewerName = "Erick",
                rejectionReason = "Please include the sink corners.",
                recipientUserId = "emma-user",
            ),
        )
        advanceUntilIdle()

        assertEquals("rejected-fg", repository.state.value.items.firstOrNull()?.choreId)
        assertEquals(repository.state.value.items.firstOrNull(), deliveryGateway.delivered.single())
    }

    @Test
    fun markReadAndMarkAllReadPersistReadState() = runTest(dispatcher) {
        val repository = StorageBackedNotificationsRepository(
            persistence = FakeNotificationsStorePersistence(),
            sessionRepository = FakeNotificationsSessionRepository(),
            appScope = backgroundScope,
        )
        repository.record(
            NotificationEvent.ProofSubmitted(
                householdId = "house-1",
                choreId = "one",
                choreTitle = "One",
                dateKey = "2026-04-10",
                assigneeName = "Emma",
                submittingMemberName = "Emma",
                createdAtEpochMs = 10L,
            ),
        )
        repository.record(
            NotificationEvent.ProofApproved(
                householdId = "house-1",
                choreId = "two",
                choreTitle = "Two",
                dateKey = "2026-04-10",
                assigneeName = "Emma",
                submittingMemberName = "Emma",
                reviewerName = "Erick",
                completionOutcome = "approved_completed",
                recipientUserId = "emma-user",
                awardedPoints = 25,
                createdAtEpochMs = 20L,
            ),
        )
        advanceUntilIdle()

        val firstId = repository.state.value.items.first().id
        repository.markRead(firstId)
        advanceUntilIdle()
        assertNotNull(repository.state.value.items.first { it.id == firstId }.readAtEpochMs)

        repository.markAllRead()
        advanceUntilIdle()
        assertEquals(0, repository.state.value.items.count { it.readAtEpochMs == null })
    }

    @Test
    fun recordReturnsTheExactPersistedNotificationItem() = runTest(dispatcher) {
        val repository = StorageBackedNotificationsRepository(
            persistence = FakeNotificationsStorePersistence(),
            sessionRepository = FakeNotificationsSessionRepository(),
            appScope = backgroundScope,
        )
        repository.record(
            NotificationEvent.ProofSubmitted(
                householdId = "house-1",
                choreId = "existing",
                choreTitle = "Existing chore",
                dateKey = "2026-04-09",
                assigneeName = "Emma",
                submittingMemberName = "Emma",
                createdAtEpochMs = 10L,
            ),
        )

        val recorded = repository.record(
            NotificationEvent.ProofApproved(
                householdId = "house-1",
                choreId = "fresh",
                choreTitle = "Fresh chore",
                dateKey = "2026-04-10",
                assigneeName = "Emma",
                submittingMemberName = "Emma",
                reviewerName = "Erick",
                completionOutcome = "approved_completed",
                recipientUserId = "emma-user",
                createdAtEpochMs = 20L,
            ),
        )
        advanceUntilIdle()

        assertNotNull(recorded)
        assertEquals("fresh", recorded.choreId)
        assertEquals(recorded, repository.state.value.items.first())
    }

}

private class FakeNotificationDeliveryGateway : NotificationDeliveryGateway {
    val delivered = mutableListOf<NotificationItem>()

    override suspend fun getAuthorizationStatus(): NotificationAuthorizationStatus =
        NotificationAuthorizationStatus.Granted

    override suspend fun requestAuthorization(): NotificationAuthorizationStatus =
        NotificationAuthorizationStatus.Granted

    override suspend fun deliver(item: NotificationItem) {
        delivered += item
    }
}

private class FakeNotificationsStorePersistence : NotificationsStorePersistence {
    private val stores = mutableMapOf<String, PersistedNotificationsStore>()

    override suspend fun loadStore(householdId: String): PersistedNotificationsStore =
        stores[householdId] ?: PersistedNotificationsStore()

    override suspend fun saveStore(
        householdId: String,
        store: PersistedNotificationsStore,
    ) {
        stores[householdId] = store
    }
}

private class FakeNotificationsSessionRepository : SessionRepository {
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
    override suspend fun signInWithGoogle(idToken: String, accessToken: String?, displayNameHint: String, emailHint: String): AuthFlowResult = AuthFlowResult.Main
    override suspend fun createHousehold(displayName: String, householdName: String, householdSizeLabel: String): AuthFlowResult = AuthFlowResult.Main
    override suspend fun joinHousehold(displayName: String, inviteCode: String): AuthFlowResult = AuthFlowResult.Main
    override suspend fun previewInvite(inviteCode: String): InvitePreview? = null
    override suspend fun dismissInitialPremiumFlow() = Unit
    override suspend fun switchActiveHousehold(householdId: String) = Unit
    override suspend fun signOut() = Unit
    override suspend fun resetDemo() = Unit
}

private class FakeNotificationsSettingsRepository(
    enabled: Boolean = true,
) : SettingsRepository {
    override val settings: StateFlow<SettingsSnapshot> = MutableStateFlow(
        SettingsSnapshot(notificationsEnabled = enabled),
    )

    override suspend fun updateThemeMode(themeMode: ThemeMode) = Unit
    override suspend fun updateAppearancePreset(appearancePreset: AppearancePreset) = Unit
    override suspend fun updateEditorialWarmth(enabled: Boolean) = Unit
    override suspend fun updateNotifications(enabled: Boolean) = Unit
    override suspend fun updateWeekStartsOnMonday(enabled: Boolean) = Unit
    override suspend fun updateAutoAssignSuggestions(enabled: Boolean) = Unit
    override suspend fun updateLargerText(enabled: Boolean) = Unit
    override suspend fun updateAlwaysShowLabels(enabled: Boolean) = Unit
    override suspend fun updateFeedbackPalKind(kind: FeedbackPalKind) {
        TODO("Not yet implemented")
    }
}
