package com.sortoapps.chorepilot.features.notifications.domain

import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
enum class NotificationType {
    ProofSubmitted,
    ProofApproved,
    ProofRejected,
    DiscoverySubmitted,
    DiscoveryApproved,
    DiscoveryRejected,
    DueSoon,
    Overdue,
    AssignmentChanged,
}

@Serializable
data class NotificationItem(
    val id: String,
    val householdId: String,
    val choreId: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val createdAtEpochMs: Long,
    val readAtEpochMs: Long? = null,
)

enum class NotificationRecencyGroup {
    Today,
    EarlierThisWeek,
    Earlier,
}

data class NotificationsRepositoryState(
    val isLoading: Boolean = true,
    val items: List<NotificationItem> = emptyList(),
    val errorMessage: String? = null,
)

sealed interface NotificationEvent {
    val householdId: String
    val choreId: String
    val createdAtEpochMs: Long

    data class ProofSubmitted(
        override val householdId: String,
        override val choreId: String,
        val choreTitle: String,
        val dateKey: String,
        val assigneeName: String,
        val submittingMemberName: String,
        override val createdAtEpochMs: Long,
    ) : NotificationEvent

    data class ProofApproved(
        override val householdId: String,
        override val choreId: String,
        val choreTitle: String,
        val dateKey: String,
        val assigneeName: String,
        val submittingMemberName: String,
        val reviewerName: String,
        val completionOutcome: String,
        val recipientUserId: String,
        val awardedPoints: Int = 0,
        override val createdAtEpochMs: Long,
    ) : NotificationEvent

    data class ProofRejected(
        override val householdId: String,
        override val choreId: String,
        val choreTitle: String,
        val dateKey: String,
        val assigneeName: String,
        val submittingMemberName: String,
        val reviewerName: String,
        val rejectionReason: String,
        val recipientUserId: String,
        override val createdAtEpochMs: Long,
    ) : NotificationEvent

    data class DiscoverySubmitted(
        override val householdId: String,
        override val choreId: String,
        val choreTitle: String,
        val submittingMemberName: String,
        override val createdAtEpochMs: Long,
    ) : NotificationEvent

    data class DiscoveryApproved(
        override val householdId: String,
        override val choreId: String,
        val choreTitle: String,
        val submittingMemberName: String,
        val reviewerName: String,
        val recipientUserId: String,
        override val createdAtEpochMs: Long,
    ) : NotificationEvent

    data class DiscoveryRejected(
        override val householdId: String,
        override val choreId: String,
        val choreTitle: String,
        val submittingMemberName: String,
        val reviewerName: String,
        val rejectionReason: String,
        val recipientUserId: String,
        override val createdAtEpochMs: Long,
    ) : NotificationEvent

    data class DueSoon(
        override val householdId: String,
        override val choreId: String,
        val choreTitle: String,
        val dateKey: String,
        val assigneeName: String,
        val dueLabel: String,
        override val createdAtEpochMs: Long,
    ) : NotificationEvent

    data class Overdue(
        override val householdId: String,
        override val choreId: String,
        val choreTitle: String,
        val dateKey: String,
        val assigneeName: String,
        val dueLabel: String,
        override val createdAtEpochMs: Long,
    ) : NotificationEvent

    data class AssignmentChanged(
        override val householdId: String,
        override val choreId: String,
        val choreTitle: String,
        val dateKey: String,
        val originalAssigneeName: String,
        val newAssigneeName: String,
        override val createdAtEpochMs: Long,
    ) : NotificationEvent
}

interface NotificationsRepository {
    val state: StateFlow<NotificationsRepositoryState>
    val usesSharedRemotePush: Boolean get() = false

    suspend fun refresh()

    suspend fun record(event: NotificationEvent): NotificationItem?

    suspend fun markRead(notificationId: String)

    suspend fun markAllRead()
}
