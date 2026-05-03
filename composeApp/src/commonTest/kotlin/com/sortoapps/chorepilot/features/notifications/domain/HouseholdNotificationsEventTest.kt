package com.sortoapps.chorepilot.features.notifications.domain

import com.sortoapps.chorepilot.features.auth.domain.AuthFlowResult
import com.sortoapps.chorepilot.features.auth.domain.HouseholdRole
import com.sortoapps.chorepilot.features.auth.domain.InvitePreview
import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import com.sortoapps.chorepilot.features.auth.domain.SessionSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HouseholdNotificationsEventTest {
    @Test
    fun discoverySubmissionRecordsDiscoverySubmittedEvent() = runTest {
        val repository = RecordingNotificationsRepository()
        val notifications = RepositoryBackedHouseholdNotifications(
            notificationsRepository = repository,
            deliveryGateway = RecordingNotificationDeliveryGateway(),
            appVisibilityTracker = AppVisibilityTracker(),
            sessionRepository = FakeEventSessionRepository(),
            notificationsEnabled = { true },
        )

        notifications.onDiscoveryChoreSubmitted(
            DiscoveryChoreSubmittedNotification(
                householdId = "house-1",
                choreId = "chore-discovery",
                choreTitle = "Straighten the shoe rack",
                submittingMemberName = "Mila",
            ),
        )

        assertEquals(listOf(NotificationType.DiscoverySubmitted), repository.recorded.map(NotificationItem::type))
        assertEquals("chore-discovery", repository.recorded.single().choreId)
    }

    @Test
    fun proofRejectionRecordsReasonForSubmittingMember() = runTest {
        val repository = RecordingNotificationsRepository()
        val notifications = RepositoryBackedHouseholdNotifications(
            notificationsRepository = repository,
            deliveryGateway = RecordingNotificationDeliveryGateway(),
            appVisibilityTracker = AppVisibilityTracker(),
            sessionRepository = FakeEventSessionRepository(),
            notificationsEnabled = { true },
        )

        notifications.onProofRejected(
            ProofRejectedNotification(
                householdId = "house-1",
                choreId = "chore-proof",
                choreTitle = "Kitchen reset",
                dateKey = "2026-04-16",
                assigneeName = "Mila",
                submittingMemberName = "Mila",
                reviewerName = "Erick",
                rejectionReason = "Please add a brighter photo of the whole counter.",
                recipientUserId = "mila-user",
            ),
        )

        assertEquals(listOf(NotificationType.ProofRejected), repository.recorded.map(NotificationItem::type))
        assertEquals(
            "Kitchen reset needs new proof from Mila. Please add a brighter photo of the whole counter.",
            repository.recorded.single().body,
        )
    }

    @Test
    fun dueSoonOverdueAndAssignmentChangedRecordExpectedEventTypes() = runTest {
        val repository = RecordingNotificationsRepository()
        val notifications = RepositoryBackedHouseholdNotifications(
            notificationsRepository = repository,
            deliveryGateway = RecordingNotificationDeliveryGateway(),
            appVisibilityTracker = AppVisibilityTracker(),
            sessionRepository = FakeEventSessionRepository(),
            notificationsEnabled = { true },
        )

        notifications.onDueSoon(
            DueSoonNotification(
                householdId = "house-1",
                choreId = "chore-due",
                choreTitle = "Dishes",
                dateKey = "2026-04-15",
                assigneeName = "Emma",
                dueLabel = "Due before dinner",
            ),
        )
        notifications.onOverdue(
            OverdueNotification(
                householdId = "house-1",
                choreId = "chore-late",
                choreTitle = "Trash",
                dateKey = "2026-04-15",
                assigneeName = "Erick",
                dueLabel = "Missed yesterday",
            ),
        )
        notifications.onAssignmentChanged(
            AssignmentChangedNotification(
                householdId = "house-1",
                choreId = "chore-cover",
                choreTitle = "Laundry",
                dateKey = "2026-04-15",
                originalAssigneeName = "Erick",
                newAssigneeName = "Emma",
            ),
        )

        assertEquals(
            listOf(
                NotificationType.DueSoon,
                NotificationType.Overdue,
                NotificationType.AssignmentChanged,
            ),
            repository.recorded.map(NotificationItem::type),
        )
        assertEquals(
            listOf("chore-due", "chore-late", "chore-cover"),
            repository.recorded.map(NotificationItem::choreId),
        )
    }
}

private class RecordingNotificationsRepository : NotificationsRepository {
    override val state: StateFlow<NotificationsRepositoryState> = MutableStateFlow(NotificationsRepositoryState())
    val recorded = mutableListOf<NotificationItem>()

    override suspend fun refresh() = Unit

    override suspend fun record(event: NotificationEvent): NotificationItem {
        val item = event.toNotificationItem()
        recorded += item
        return item
    }

    override suspend fun markRead(notificationId: String) = Unit

    override suspend fun markAllRead() = Unit
}

private class RecordingNotificationDeliveryGateway : NotificationDeliveryGateway {
    override suspend fun getAuthorizationStatus(): NotificationAuthorizationStatus = NotificationAuthorizationStatus.Granted

    override suspend fun requestAuthorization(): NotificationAuthorizationStatus = NotificationAuthorizationStatus.Granted

    override suspend fun deliver(item: NotificationItem) = Unit
}

private class FakeEventSessionRepository : SessionRepository {
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
