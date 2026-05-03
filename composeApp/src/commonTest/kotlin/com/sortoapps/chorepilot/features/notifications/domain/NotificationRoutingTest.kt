package com.sortoapps.chorepilot.features.notifications.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class NotificationRoutingTest {
    @Test
    fun proofApprovedEventsUsePointsFirstCopy() {
        val item = NotificationEvent.ProofApproved(
            householdId = "house-1",
            choreId = "chore-1",
            choreTitle = "Kitchen reset",
            dateKey = "2026-04-15",
            assigneeName = "Erick",
            submittingMemberName = "Erick",
            reviewerName = "Emma",
            completionOutcome = "approved_completed",
            recipientUserId = "erick-user",
            awardedPoints = 25,
            createdAtEpochMs = 42L,
        ).toNotificationItem()

        assertEquals(NotificationType.ProofApproved, item.type)
        assertEquals("Proof approved! +25 points", item.title)
        assertEquals("Kitchen reset is complete for Erick.", item.body)
    }

    @Test
    fun assignmentChangedEventsMapToStableNotificationCopy() {
        val item = NotificationEvent.AssignmentChanged(
            householdId = "house-1",
            choreId = "chore-1",
            choreTitle = "Kitchen reset",
            dateKey = "2026-04-15",
            originalAssigneeName = "Erick",
            newAssigneeName = "Emma",
            createdAtEpochMs = 42L,
        ).toNotificationItem()

        assertEquals(NotificationType.AssignmentChanged, item.type)
        assertEquals("assignment-changed|house-1|chore-1|2026-04-15|erick|emma", item.id)
        assertEquals("Emma is covering Kitchen reset", item.title)
        assertEquals("Emma is covering for Erick while they're away.", item.body)
    }

    @Test
    fun proofPayloadRoutesBackToInbox() {
        val request = NotificationTapPayload(
            notificationId = "proof-1",
            choreId = "chore-1",
            type = NotificationType.ProofSubmitted,
        ).toOpenRequest()

        assertEquals("proof-1", request.notificationId)
        assertEquals(NotificationOpenTarget.Inbox, request.target)
    }

    @Test
    fun proofApprovedPayloadRoutesBackToInbox() {
        val request = NotificationTapPayload(
            notificationId = "proof-approved-1",
            choreId = "chore-1",
            type = NotificationType.ProofApproved,
        ).toOpenRequest()

        assertEquals(NotificationOpenTarget.Inbox, request.target)
    }

    @Test
    fun proofRejectedEventsKeepReasonAndRouteToChoreDetail() {
        val item = NotificationEvent.ProofRejected(
            householdId = "house-1",
            choreId = "chore-9",
            choreTitle = "Kitchen reset",
            dateKey = "2026-04-16",
            assigneeName = "Ava",
            submittingMemberName = "Ava",
            reviewerName = "Erick",
            rejectionReason = "Please show the sink and counters in one photo.",
            recipientUserId = "ava-user",
            createdAtEpochMs = 84L,
        ).toNotificationItem()

        assertEquals(NotificationType.ProofRejected, item.type)
        assertEquals("Proof needs another pass", item.title)
        assertEquals(
            "Kitchen reset needs new proof from Ava. Please show the sink and counters in one photo.",
            item.body,
        )
        assertEquals(NotificationOpenTarget.Chore("chore-9"), item.toOpenRequest().target)
    }

    @Test
    fun chorePayloadRoutesToChoreDetail() {
        val request = NotificationTapPayload(
            notificationId = "due-1",
            choreId = "chore-7",
            type = NotificationType.Overdue,
        ).toOpenRequest()

        assertEquals(NotificationOpenTarget.Chore("chore-7"), request.target)
    }

    @Test
    fun discoveryReviewPayloadRoutesToChoreDetail() {
        val request = NotificationTapPayload(
            notificationId = "discovery-1",
            choreId = "chore-42",
            type = NotificationType.DiscoverySubmitted,
        ).toOpenRequest()

        assertEquals(NotificationOpenTarget.Chore("chore-42"), request.target)
    }
}
