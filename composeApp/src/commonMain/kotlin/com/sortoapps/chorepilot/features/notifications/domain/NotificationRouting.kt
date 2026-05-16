package com.sortoapps.chorepilot.features.notifications.domain

import kotlinx.serialization.Serializable

@Serializable
data class NotificationTapPayload(
    val notificationId: String,
    val choreId: String,
    val type: NotificationType,
)

sealed interface NotificationOpenTarget {
    data object Inbox : NotificationOpenTarget

    data class Chore(val choreId: String) : NotificationOpenTarget
}

data class NotificationOpenRequest(
    val notificationId: String,
    val target: NotificationOpenTarget,
)

fun NotificationItem.toTapPayload(): NotificationTapPayload = NotificationTapPayload(
    notificationId = id,
    choreId = choreId,
    type = type,
)

fun NotificationItem.toOpenRequest(): NotificationOpenRequest = NotificationOpenRequest(
    notificationId = id,
    target = toOpenTarget(),
)

fun NotificationTapPayload.toOpenRequest(): NotificationOpenRequest = NotificationOpenRequest(
    notificationId = notificationId,
    target = toOpenTarget(),
)

fun NotificationItem.toOpenTarget(): NotificationOpenTarget = when (type) {
    NotificationType.ProofSubmitted,
    NotificationType.ProofApproved,
    NotificationType.ProofRejected,
    NotificationType.DiscoverySubmitted,
    NotificationType.DiscoveryApproved,
    NotificationType.DiscoveryRejected,
    NotificationType.DueSoon,
    NotificationType.Overdue,
    NotificationType.AssignmentChanged,
    -> choreId.takeIf(String::isNotBlank)?.let(NotificationOpenTarget::Chore) ?: NotificationOpenTarget.Inbox
}

fun NotificationTapPayload.toOpenTarget(): NotificationOpenTarget = when (type) {
    NotificationType.ProofSubmitted,
    NotificationType.ProofApproved,
    NotificationType.ProofRejected,
    NotificationType.DiscoverySubmitted,
    NotificationType.DiscoveryApproved,
    NotificationType.DiscoveryRejected,
    NotificationType.DueSoon,
    NotificationType.Overdue,
    NotificationType.AssignmentChanged,
    -> choreId.takeIf(String::isNotBlank)?.let(NotificationOpenTarget::Chore) ?: NotificationOpenTarget.Inbox
}

fun notificationTypeOrDefault(rawValue: String): NotificationType =
    runCatching { NotificationType.valueOf(rawValue) }.getOrDefault(NotificationType.ProofSubmitted)

fun NotificationEvent.toNotificationItem(): NotificationItem = when (this) {
    is NotificationEvent.ProofSubmitted -> NotificationItem(
        id = "proof-submitted|$householdId|$choreId|$dateKey|$createdAtEpochMs",
        householdId = householdId,
        choreId = choreId,
        type = NotificationType.ProofSubmitted,
        title = "$submittingMemberName submitted proof",
        body = buildString {
            append(choreTitle)
            if (assigneeName.isNotBlank()) append(" for $assigneeName")
            append(" is ready for review.")
        },
        createdAtEpochMs = createdAtEpochMs,
    )

    is NotificationEvent.ProofApproved -> NotificationItem(
        id = "proof-approved|$householdId|$choreId|$dateKey|$createdAtEpochMs",
        householdId = householdId,
        choreId = choreId,
        type = NotificationType.ProofApproved,
        title = if (awardedPoints > 0) {
            "Proof approved! +$awardedPoints points"
        } else {
            "Proof approved"
        },
        body = buildString {
            append(choreTitle)
            append(" is complete")
            if (submittingMemberName.isNotBlank()) {
                append(" for ")
                append(submittingMemberName)
            }
            append(".")
        },
        createdAtEpochMs = createdAtEpochMs,
    )

    is NotificationEvent.ProofRejected -> NotificationItem(
        id = "proof-rejected|$householdId|$choreId|$dateKey|$recipientUserId|$createdAtEpochMs",
        householdId = householdId,
        choreId = choreId,
        type = NotificationType.ProofRejected,
        title = "Proof needs another pass",
        body = buildString {
            append(choreTitle)
            append(" needs new proof")
            if (submittingMemberName.isNotBlank()) {
                append(" from ")
                append(submittingMemberName)
            } else if (assigneeName.isNotBlank()) {
                append(" from ")
                append(assigneeName)
            }
            append(".")
            if (rejectionReason.isNotBlank()) {
                append(" ")
                append(rejectionReason)
            }
        },
        createdAtEpochMs = createdAtEpochMs,
    )

    is NotificationEvent.DueSoon -> NotificationItem(
        id = "due-soon|$householdId|$choreId|$dateKey",
        householdId = householdId,
        choreId = choreId,
        type = NotificationType.DueSoon,
        title = "$choreTitle is due soon",
        body = buildString {
            if (assigneeName.isNotBlank()) {
                append(assigneeName)
                append(" is up next. ")
            }
            append(dueLabel.ifBlank { "This chore needs attention today." })
        },
        createdAtEpochMs = createdAtEpochMs,
    )

    is NotificationEvent.Overdue -> NotificationItem(
        id = "overdue|$householdId|$choreId|$dateKey",
        householdId = householdId,
        choreId = choreId,
        type = NotificationType.Overdue,
        title = "$choreTitle is overdue",
        body = buildString {
            if (assigneeName.isNotBlank()) {
                append(assigneeName)
                append(" still owns this one. ")
            }
            append(dueLabel.ifBlank { "This chore has slipped past its planned window." })
        },
        createdAtEpochMs = createdAtEpochMs,
    )

    is NotificationEvent.AssignmentChanged -> NotificationItem(
        id = "assignment-changed|$householdId|$choreId|$dateKey|${originalAssigneeName.lowercase()}|${newAssigneeName.lowercase()}",
        householdId = householdId,
        choreId = choreId,
        type = NotificationType.AssignmentChanged,
        title = "$newAssigneeName is covering $choreTitle",
        body = buildString {
            append(newAssigneeName)
            append(" is covering for ")
            append(originalAssigneeName)
            append(" while they're away.")
        },
        createdAtEpochMs = createdAtEpochMs,
    )

    is NotificationEvent.DiscoverySubmitted -> NotificationItem(
        id = "discovery-submitted|$householdId|$choreId|$createdAtEpochMs",
        householdId = householdId,
        choreId = choreId,
        type = NotificationType.DiscoverySubmitted,
        title = "Discovery chore needs review",
        body = buildString {
            append(submittingMemberName.ifBlank { "A household member" })
            append(" submitted ")
            append(choreTitle)
            append(" for approval.")
        },
        createdAtEpochMs = createdAtEpochMs,
    )

    is NotificationEvent.DiscoveryApproved -> NotificationItem(
        id = "discovery-approved|$householdId|$choreId|$recipientUserId|$createdAtEpochMs",
        householdId = householdId,
        choreId = choreId,
        type = NotificationType.DiscoveryApproved,
        title = "Discovery chore approved",
        body = buildString {
            append(choreTitle)
            append(" is now live")
            if (reviewerName.isNotBlank()) {
                append(". Approved by ")
                append(reviewerName)
            } else {
                append(".")
            }
        },
        createdAtEpochMs = createdAtEpochMs,
    )

    is NotificationEvent.DiscoveryRejected -> NotificationItem(
        id = "discovery-rejected|$householdId|$choreId|$recipientUserId|$createdAtEpochMs",
        householdId = householdId,
        choreId = choreId,
        type = NotificationType.DiscoveryRejected,
        title = "Discovery chore needs an update",
        body = buildString {
            append(choreTitle)
            append(" was not approved yet.")
            if (rejectionReason.isNotBlank()) {
                append(" ")
                append(rejectionReason)
            }
        },
        createdAtEpochMs = createdAtEpochMs,
    )
}
