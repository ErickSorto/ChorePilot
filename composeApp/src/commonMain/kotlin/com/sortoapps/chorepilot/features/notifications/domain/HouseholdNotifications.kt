package com.sortoapps.chorepilot.features.notifications.domain

import com.sortoapps.chorepilot.core.ui.SnackbarController
import com.sortoapps.chorepilot.core.ui.SnackbarEvent
import com.sortoapps.chorepilot.core.ui.StringValue
import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import kotlin.time.Clock

data class ProofSubmittedNotification(
    val householdId: String,
    val choreId: String,
    val choreTitle: String,
    val dateKey: String,
    val assigneeName: String,
    val submittingMemberName: String,
)

data class ProofApprovedNotification(
    val householdId: String,
    val choreId: String,
    val choreTitle: String,
    val dateKey: String,
    val assigneeName: String,
    val submittingMemberName: String,
    val reviewerName: String,
    val completionOutcome: String,
    val recipientUserId: String,
    val awardedPoints: Int = 0,
)

data class ProofRejectedNotification(
    val householdId: String,
    val choreId: String,
    val choreTitle: String,
    val dateKey: String,
    val assigneeName: String,
    val submittingMemberName: String,
    val reviewerName: String,
    val rejectionReason: String,
    val recipientUserId: String,
)

data class DiscoveryChoreSubmittedNotification(
    val householdId: String,
    val choreId: String,
    val choreTitle: String,
    val submittingMemberName: String,
)

data class DiscoveryChoreApprovedNotification(
    val householdId: String,
    val choreId: String,
    val choreTitle: String,
    val submittingMemberName: String,
    val reviewerName: String,
    val recipientUserId: String,
)

data class DiscoveryChoreRejectedNotification(
    val householdId: String,
    val choreId: String,
    val choreTitle: String,
    val submittingMemberName: String,
    val reviewerName: String,
    val rejectionReason: String,
    val recipientUserId: String,
)

data class DueSoonNotification(
    val householdId: String,
    val choreId: String,
    val choreTitle: String,
    val dateKey: String,
    val assigneeName: String,
    val dueLabel: String,
)

data class OverdueNotification(
    val householdId: String,
    val choreId: String,
    val choreTitle: String,
    val dateKey: String,
    val assigneeName: String,
    val dueLabel: String,
)

data class AssignmentChangedNotification(
    val householdId: String,
    val choreId: String,
    val choreTitle: String,
    val dateKey: String,
    val originalAssigneeName: String,
    val newAssigneeName: String,
)

interface HouseholdNotifications {
    suspend fun onProofSubmitted(notification: ProofSubmittedNotification)

    suspend fun onProofApproved(notification: ProofApprovedNotification)

    suspend fun onProofRejected(notification: ProofRejectedNotification) = Unit

    suspend fun onDiscoveryChoreSubmitted(notification: DiscoveryChoreSubmittedNotification) = Unit

    suspend fun onDiscoveryChoreApproved(notification: DiscoveryChoreApprovedNotification) = Unit

    suspend fun onDiscoveryChoreRejected(notification: DiscoveryChoreRejectedNotification) = Unit

    suspend fun onDueSoon(notification: DueSoonNotification) = Unit

    suspend fun onOverdue(notification: OverdueNotification) = Unit

    suspend fun onAssignmentChanged(notification: AssignmentChangedNotification) = Unit
}

class NoOpHouseholdNotifications : HouseholdNotifications {
    override suspend fun onProofSubmitted(notification: ProofSubmittedNotification) = Unit

    override suspend fun onProofApproved(notification: ProofApprovedNotification) = Unit
}

class RepositoryBackedHouseholdNotifications(
    private val notificationsRepository: NotificationsRepository,
    private val deliveryGateway: NotificationDeliveryGateway,
    private val appVisibilityTracker: AppVisibilityTracker,
    private val sessionRepository: SessionRepository,
    private val notificationsEnabled: () -> Boolean,
) : HouseholdNotifications {
    override suspend fun onProofSubmitted(notification: ProofSubmittedNotification) {
        recordAndDeliver(
            NotificationEvent.ProofSubmitted(
                householdId = notification.householdId,
                choreId = notification.choreId,
                choreTitle = notification.choreTitle,
                dateKey = notification.dateKey,
                assigneeName = notification.assigneeName,
                submittingMemberName = notification.submittingMemberName,
                createdAtEpochMs = Clock.System.now().toEpochMilliseconds(),
            ),
        )
    }

    override suspend fun onProofApproved(notification: ProofApprovedNotification) {
        recordAndDeliver(
            NotificationEvent.ProofApproved(
                householdId = notification.householdId,
                choreId = notification.choreId,
                choreTitle = notification.choreTitle,
                dateKey = notification.dateKey,
                assigneeName = notification.assigneeName,
                submittingMemberName = notification.submittingMemberName,
                reviewerName = notification.reviewerName,
                completionOutcome = notification.completionOutcome,
                recipientUserId = notification.recipientUserId,
                awardedPoints = notification.awardedPoints,
                createdAtEpochMs = Clock.System.now().toEpochMilliseconds(),
            ),
        )
    }

    override suspend fun onProofRejected(notification: ProofRejectedNotification) {
        recordAndDeliver(
            NotificationEvent.ProofRejected(
                householdId = notification.householdId,
                choreId = notification.choreId,
                choreTitle = notification.choreTitle,
                dateKey = notification.dateKey,
                assigneeName = notification.assigneeName,
                submittingMemberName = notification.submittingMemberName,
                reviewerName = notification.reviewerName,
                rejectionReason = notification.rejectionReason,
                recipientUserId = notification.recipientUserId,
                createdAtEpochMs = Clock.System.now().toEpochMilliseconds(),
            ),
        )
    }

    override suspend fun onDiscoveryChoreSubmitted(notification: DiscoveryChoreSubmittedNotification) {
        recordAndDeliver(
            NotificationEvent.DiscoverySubmitted(
                householdId = notification.householdId,
                choreId = notification.choreId,
                choreTitle = notification.choreTitle,
                submittingMemberName = notification.submittingMemberName,
                createdAtEpochMs = Clock.System.now().toEpochMilliseconds(),
            ),
        )
    }

    override suspend fun onDiscoveryChoreApproved(notification: DiscoveryChoreApprovedNotification) {
        recordAndDeliver(
            NotificationEvent.DiscoveryApproved(
                householdId = notification.householdId,
                choreId = notification.choreId,
                choreTitle = notification.choreTitle,
                submittingMemberName = notification.submittingMemberName,
                reviewerName = notification.reviewerName,
                recipientUserId = notification.recipientUserId,
                createdAtEpochMs = Clock.System.now().toEpochMilliseconds(),
            ),
        )
    }

    override suspend fun onDiscoveryChoreRejected(notification: DiscoveryChoreRejectedNotification) {
        recordAndDeliver(
            NotificationEvent.DiscoveryRejected(
                householdId = notification.householdId,
                choreId = notification.choreId,
                choreTitle = notification.choreTitle,
                submittingMemberName = notification.submittingMemberName,
                reviewerName = notification.reviewerName,
                rejectionReason = notification.rejectionReason,
                recipientUserId = notification.recipientUserId,
                createdAtEpochMs = Clock.System.now().toEpochMilliseconds(),
            ),
        )
    }

    override suspend fun onDueSoon(notification: DueSoonNotification) {
        recordAndDeliver(
            NotificationEvent.DueSoon(
                householdId = notification.householdId,
                choreId = notification.choreId,
                choreTitle = notification.choreTitle,
                dateKey = notification.dateKey,
                assigneeName = notification.assigneeName,
                dueLabel = notification.dueLabel,
                createdAtEpochMs = Clock.System.now().toEpochMilliseconds(),
            ),
        )
    }

    override suspend fun onOverdue(notification: OverdueNotification) {
        recordAndDeliver(
            NotificationEvent.Overdue(
                householdId = notification.householdId,
                choreId = notification.choreId,
                choreTitle = notification.choreTitle,
                dateKey = notification.dateKey,
                assigneeName = notification.assigneeName,
                dueLabel = notification.dueLabel,
                createdAtEpochMs = Clock.System.now().toEpochMilliseconds(),
            ),
        )
    }

    override suspend fun onAssignmentChanged(notification: AssignmentChangedNotification) {
        recordAndDeliver(
            NotificationEvent.AssignmentChanged(
                householdId = notification.householdId,
                choreId = notification.choreId,
                choreTitle = notification.choreTitle,
                dateKey = notification.dateKey,
                originalAssigneeName = notification.originalAssigneeName,
                newAssigneeName = notification.newAssigneeName,
                createdAtEpochMs = Clock.System.now().toEpochMilliseconds(),
            ),
        )
    }

    private suspend fun recordAndDeliver(event: NotificationEvent) {
        val recorded = notificationsRepository.record(event) ?: return
        if (notificationsRepository.usesSharedRemotePush) return
        if (!notificationsEnabled()) return
        if (event is NotificationEvent.ProofApproved && appVisibilityTracker.isForeground.value) {
            SnackbarController.sendEvent(
                SnackbarEvent(
                    message = StringValue.DynamicString(
                        if (event.awardedPoints > 0) {
                            "Proof approved! +${event.awardedPoints} points"
                        } else {
                            "Proof approved"
                        },
                    ),
                ),
            )
            return
        }
        deliveryGateway.deliver(recorded)
    }
}
