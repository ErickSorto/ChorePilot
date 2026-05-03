package com.sortoapps.chorepilot.features.notifications.data

import com.sortoapps.chorepilot.core.firebase.FirebaseHouseholdStore
import com.sortoapps.chorepilot.core.firebase.FirestorePaths
import com.sortoapps.chorepilot.features.auth.domain.HouseholdRole
import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import com.sortoapps.chorepilot.features.notifications.domain.NotificationEvent
import com.sortoapps.chorepilot.features.notifications.domain.NotificationsRepository
import com.sortoapps.chorepilot.features.notifications.domain.NotificationItem
import com.sortoapps.chorepilot.features.notifications.domain.NotificationsRepositoryState
import com.sortoapps.chorepilot.features.notifications.domain.notificationTypeOrDefault
import com.sortoapps.chorepilot.features.notifications.domain.toNotificationItem
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.time.Clock

internal class FirebaseNotificationsRepository(
    private val sharedStore: SharedNotificationsStore,
    private val sessionRepository: SessionRepository,
    appScope: CoroutineScope,
) : NotificationsRepository {
    private val _state = MutableStateFlow(NotificationsRepositoryState())
    override val state: StateFlow<NotificationsRepositoryState> = _state.asStateFlow()
    override val usesSharedRemotePush: Boolean = true

    private val locallyAuthoredIds = mutableSetOf<String>()
    private var observeJob: Job? = null

    init {
        appScope.launch {
            sessionRepository.session
                .map { session ->
                    ActiveNotificationScope(
                        householdId = session.householdId.takeIf(String::isNotBlank).orEmpty(),
                        userId = session.userId.takeIf { session.signedIn && it.isNotBlank() }.orEmpty(),
                    )
                }
                .distinctUntilChanged()
                .collectLatest { scope ->
                    observeJob?.cancel()
                    locallyAuthoredIds.clear()
                    if (scope.householdId.isBlank() || scope.userId.isBlank()) {
                        _state.value = NotificationsRepositoryState(isLoading = false)
                    } else {
                        observeJob = appScope.launch {
                            sharedStore.observeNotifications(
                                householdId = scope.householdId,
                                recipientUserId = scope.userId,
                            ).collectLatest { documents ->
                                applyDocuments(
                                    scope = scope,
                                    documents = documents,
                                )
                            }
                        }
                        refresh()
                    }
                }
        }
    }

    override suspend fun refresh() {
        val scope = activeScope() ?: run {
            _state.value = NotificationsRepositoryState(isLoading = false)
            return
        }
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        runCatching {
            sharedStore.loadNotifications(
                householdId = scope.householdId,
                recipientUserId = scope.userId,
            )
        }.onSuccess { documents ->
            setStateFromDocuments(scope, documents)
        }.onFailure { error ->
            _state.value = NotificationsRepositoryState(
                isLoading = false,
                items = emptyList(),
                errorMessage = error.message ?: "We couldn't load notifications right now.",
            )
        }
    }

    override suspend fun record(event: NotificationEvent): NotificationItem? {
        val scope = activeScope()
        val householdId = event.householdId.takeIf(String::isNotBlank) ?: return null

        val item = event.toNotificationItem()
        val existing = sharedStore.loadNotification(householdId, item.id)
        if (existing != null) {
            val existingItem = existing.toDomain(scope?.userId.orEmpty())
            if (scope?.householdId == householdId && scope.userId in existing.recipientUserIds) {
                upsertLocalItem(existingItem)
            }
            return null
        }

        val recipients = recipientsFor(event)
        if (recipients.isEmpty()) return null
        val document = HouseholdNotificationDocument(
            id = item.id,
            householdId = item.householdId,
            choreId = item.choreId,
            type = item.type.name,
            title = item.title,
            body = item.body,
            createdAtEpochMs = item.createdAtEpochMs,
            recipientUserIds = recipients,
            readReceipts = existing?.readReceipts.orEmpty(),
        )

        return runCatching {
            sharedStore.upsertNotification(document)
            if (scope?.householdId == householdId && scope.userId in recipients) {
                locallyAuthoredIds += item.id
                val existingReadAt = existing?.readReceipts
                    ?.firstOrNull { it.userId == scope.userId }
                    ?.readAtEpochMs
                upsertLocalItem(item.copy(readAtEpochMs = existingReadAt))
            }
            item
        }.onFailure { error ->
            _state.update {
                it.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "We couldn't update notifications right now.",
                )
            }
        }.getOrNull()
    }

    override suspend fun markRead(notificationId: String) {
        val scope = activeScope() ?: return
        runCatching {
            sharedStore.markRead(
                householdId = scope.householdId,
                notificationId = notificationId,
                userId = scope.userId,
                readAtEpochMs = Clock.System.now().toEpochMilliseconds(),
            )
            _state.update { state ->
                state.copy(
                    items = state.items.map { item ->
                        if (item.id == notificationId && item.readAtEpochMs == null) {
                            item.copy(readAtEpochMs = Clock.System.now().toEpochMilliseconds())
                        } else {
                            item
                        }
                    },
                )
            }
        }.onFailure { error ->
            _state.update {
                it.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "We couldn't update notifications right now.",
                )
            }
        }
    }

    override suspend fun markAllRead() {
        val scope = activeScope() ?: return
        val readAt = Clock.System.now().toEpochMilliseconds()
        runCatching {
            sharedStore.markAllRead(
                householdId = scope.householdId,
                recipientUserId = scope.userId,
                readAtEpochMs = readAt,
            )
            _state.update { state ->
                state.copy(
                    items = state.items.map { item ->
                        if (item.readAtEpochMs == null) item.copy(readAtEpochMs = readAt) else item
                    },
                )
            }
        }.onFailure { error ->
            _state.update {
                it.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "We couldn't update notifications right now.",
                )
            }
        }
    }

    private suspend fun applyDocuments(
        scope: ActiveNotificationScope,
        documents: List<HouseholdNotificationDocument>,
    ) {
        setStateFromDocuments(scope, documents)
        state.value.items.forEach { item ->
            locallyAuthoredIds.remove(item.id)
        }
    }

    private fun setStateFromDocuments(
        scope: ActiveNotificationScope,
        documents: List<HouseholdNotificationDocument>,
    ) {
        _state.value = NotificationsRepositoryState(
            isLoading = false,
            items = documents
                .map { it.toDomain(scope.userId) }
                .sortedByDescending(NotificationItem::createdAtEpochMs),
        )
    }

    private fun upsertLocalItem(item: NotificationItem) {
        _state.update { state ->
            val updatedItems = buildList {
                add(item)
                addAll(state.items.filterNot { existing -> existing.id == item.id })
            }.sortedByDescending(NotificationItem::createdAtEpochMs)
            state.copy(
                isLoading = false,
                items = updatedItems,
                errorMessage = null,
            )
        }
    }

    private fun activeScope(): ActiveNotificationScope? {
        val session = sessionRepository.session.value
        val householdId = session.householdId.takeIf(String::isNotBlank) ?: return null
        val userId = session.userId.takeIf { session.signedIn && it.isNotBlank() } ?: return null
        return ActiveNotificationScope(householdId = householdId, userId = userId)
    }

    private suspend fun recipientsFor(event: NotificationEvent): List<String> = when (event) {
        is NotificationEvent.ProofSubmitted ->
            sharedStore.activeRecipientUserIds(
                householdId = event.householdId,
                roles = setOf(HouseholdRole.Owner, HouseholdRole.Admin),
            )

        is NotificationEvent.ProofApproved ->
            listOf(event.recipientUserId).filter(String::isNotBlank)

        is NotificationEvent.DiscoverySubmitted ->
            sharedStore.activeRecipientUserIds(
                householdId = event.householdId,
                roles = setOf(HouseholdRole.Owner, HouseholdRole.Admin),
            )

        is NotificationEvent.DiscoveryApproved ->
            listOf(event.recipientUserId).filter(String::isNotBlank)

        is NotificationEvent.DiscoveryRejected ->
            listOf(event.recipientUserId).filter(String::isNotBlank)

        is NotificationEvent.ProofRejected ->
            listOf(event.recipientUserId).filter(String::isNotBlank)

        is NotificationEvent.DueSoon ->
            sharedStore.activeRecipientUserIdsForMembers(
                householdId = event.householdId,
                memberKeys = setOf(event.assigneeName),
            )

        is NotificationEvent.Overdue ->
            sharedStore.activeRecipientUserIdsForMembers(
                householdId = event.householdId,
                memberKeys = setOf(event.assigneeName),
            )

        is NotificationEvent.AssignmentChanged ->
            sharedStore.activeRecipientUserIdsForMembers(
                householdId = event.householdId,
                memberKeys = setOf(event.originalAssigneeName, event.newAssigneeName),
            )
    }
}

internal class FirebaseSharedNotificationsStore(
    private val householdStore: FirebaseHouseholdStore,
    private val firestore: FirebaseFirestore = Firebase.firestore,
) : SharedNotificationsStore {
    override fun observeNotifications(
        householdId: String,
        recipientUserId: String,
    ): Flow<List<HouseholdNotificationDocument>> =
        notificationsQuery(householdId, recipientUserId)
            .snapshots
            .map { snapshot ->
                snapshot.documents.mapNotNull { document ->
                    document.decodeOrNull<HouseholdNotificationDocument>()
                }
            }

    override suspend fun loadNotifications(
        householdId: String,
        recipientUserId: String,
    ): List<HouseholdNotificationDocument> =
        notificationsQuery(householdId, recipientUserId)
            .get()
            .documents
            .mapNotNull { document -> document.decodeOrNull<HouseholdNotificationDocument>() }

    override suspend fun loadNotification(
        householdId: String,
        notificationId: String,
    ): HouseholdNotificationDocument? =
        notificationRef(householdId, notificationId)
            .get()
            .decodeOrNull()

    override suspend fun upsertNotification(document: HouseholdNotificationDocument) {
        notificationRef(document.householdId, document.id).set(document, merge = true)
    }

    override suspend fun markRead(
        householdId: String,
        notificationId: String,
        userId: String,
        readAtEpochMs: Long,
    ) {
        val existing = loadNotification(householdId, notificationId) ?: return
        val updated = existing.copy(
            readReceipts = existing.readReceipts
                .filterNot { receipt -> receipt.userId == userId }
                .plus(HouseholdNotificationReadReceiptDocument(userId = userId, readAtEpochMs = readAtEpochMs))
                .sortedBy(HouseholdNotificationReadReceiptDocument::userId),
        )
        upsertNotification(updated)
    }

    override suspend fun markAllRead(
        householdId: String,
        recipientUserId: String,
        readAtEpochMs: Long,
    ) {
        loadNotifications(householdId, recipientUserId).forEach { document ->
            markRead(
                householdId = householdId,
                notificationId = document.id,
                userId = recipientUserId,
                readAtEpochMs = readAtEpochMs,
            )
        }
    }

    override suspend fun activeRecipientUserIds(householdId: String): List<String> =
        householdStore.listMembers(householdId)
            .filter { member -> member.active }
            .map { member -> member.userId }
            .filter(String::isNotBlank)
            .distinct()
            .sorted()

    override suspend fun activeRecipientUserIds(
        householdId: String,
        roles: Set<HouseholdRole>,
    ): List<String> =
        householdStore.listMembers(householdId)
            .filter { member -> member.active }
            .filter { member -> roles.isEmpty() || member.role in roles }
            .map { member -> member.userId }
            .filter(String::isNotBlank)
            .distinct()
            .sorted()

    override suspend fun activeRecipientUserIdsForMembers(
        householdId: String,
        memberKeys: Set<String>,
    ): List<String> {
        val normalizedKeys = memberKeys
            .map(::normalizeNotificationMemberKey)
            .filter(String::isNotBlank)
            .toSet()
        return householdStore.listMembers(householdId)
            .filter { member -> member.active }
            .filter { member ->
                member.role == HouseholdRole.Owner ||
                    member.role == HouseholdRole.Admin ||
                    normalizeNotificationMemberKey(member.userId) in normalizedKeys ||
                    normalizeNotificationMemberKey(member.displayName) in normalizedKeys
            }
            .map { member -> member.userId }
            .filter(String::isNotBlank)
            .distinct()
            .sorted()
    }

    private fun notificationsQuery(
        householdId: String,
        recipientUserId: String,
    ) = householdRef(householdId)
        .collection(FirestorePaths.Notifications)
        .where { "recipientUserIds" contains recipientUserId }

    private fun householdRef(householdId: String) =
        firestore.collection(FirestorePaths.Households).document(householdId)

    private fun notificationRef(
        householdId: String,
        notificationId: String,
    ) = householdRef(householdId)
        .collection(FirestorePaths.Notifications)
        .document(notificationId)
}

internal interface SharedNotificationsStore {
    fun observeNotifications(
        householdId: String,
        recipientUserId: String,
    ): Flow<List<HouseholdNotificationDocument>>

    suspend fun loadNotifications(
        householdId: String,
        recipientUserId: String,
    ): List<HouseholdNotificationDocument>

    suspend fun loadNotification(
        householdId: String,
        notificationId: String,
    ): HouseholdNotificationDocument?

    suspend fun upsertNotification(document: HouseholdNotificationDocument)

    suspend fun markRead(
        householdId: String,
        notificationId: String,
        userId: String,
        readAtEpochMs: Long,
    )

    suspend fun markAllRead(
        householdId: String,
        recipientUserId: String,
        readAtEpochMs: Long,
    )

    suspend fun activeRecipientUserIds(householdId: String): List<String>

    suspend fun activeRecipientUserIds(
        householdId: String,
        roles: Set<HouseholdRole>,
    ): List<String> = activeRecipientUserIds(householdId)

    suspend fun activeRecipientUserIdsForMembers(
        householdId: String,
        memberKeys: Set<String>,
    ): List<String> = activeRecipientUserIds(householdId)
}

@Serializable
data class HouseholdNotificationDocument(
    val id: String = "",
    val householdId: String = "",
    val choreId: String = "",
    val type: String = "",
    val title: String = "",
    val body: String = "",
    val createdAtEpochMs: Long = 0L,
    val recipientUserIds: List<String> = emptyList(),
    val readReceipts: List<HouseholdNotificationReadReceiptDocument> = emptyList(),
)

@Serializable
data class HouseholdNotificationReadReceiptDocument(
    val userId: String = "",
    val readAtEpochMs: Long = 0L,
)

private data class ActiveNotificationScope(
    val householdId: String,
    val userId: String,
)

private fun normalizeNotificationMemberKey(value: String): String =
    value.trim().lowercase()

private fun HouseholdNotificationDocument.toDomain(currentUserId: String): NotificationItem = NotificationItem(
    id = id,
    householdId = householdId,
    choreId = choreId,
    type = notificationTypeOrDefault(type),
    title = title,
    body = body,
    createdAtEpochMs = createdAtEpochMs,
    readAtEpochMs = readReceipts.firstOrNull { receipt -> receipt.userId == currentUserId }?.readAtEpochMs,
)

private inline fun <reified T> DocumentSnapshot.decodeOrNull(): T? = if (exists) data<T>() else null
