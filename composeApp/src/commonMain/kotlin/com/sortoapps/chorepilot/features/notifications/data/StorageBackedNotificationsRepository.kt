package com.sortoapps.chorepilot.features.notifications.data

import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import com.sortoapps.chorepilot.features.notifications.domain.NotificationEvent
import com.sortoapps.chorepilot.features.notifications.domain.NotificationItem
import com.sortoapps.chorepilot.features.notifications.domain.NotificationsRepository
import com.sortoapps.chorepilot.features.notifications.domain.NotificationsRepositoryState
import com.sortoapps.chorepilot.features.notifications.domain.notificationTypeOrDefault
import com.sortoapps.chorepilot.features.notifications.domain.toNotificationItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class StorageBackedNotificationsRepository(
    private val persistence: NotificationsStorePersistence,
    private val sessionRepository: SessionRepository,
    appScope: CoroutineScope,
) : NotificationsRepository {
    private val _state = MutableStateFlow(NotificationsRepositoryState())
    override val state: StateFlow<NotificationsRepositoryState> = _state.asStateFlow()

    init {
        appScope.launch {
            sessionRepository.session.collectLatest { session ->
                val householdId = session.householdId.takeIf(String::isNotBlank)
                if (householdId == null) {
                    _state.value = NotificationsRepositoryState(isLoading = false)
                } else {
                    refresh()
                }
            }
        }
    }

    override suspend fun refresh() {
        val householdId = activeHouseholdId() ?: run {
            _state.value = NotificationsRepositoryState(isLoading = false)
            return
        }
        _state.value = _state.value.copy(isLoading = true, errorMessage = null)
        runCatching {
            val persistedItems = persistence.loadStore(householdId).items
            persistedItems.map { item -> item.toDomain() }
                .sortedByDescending { item -> item.createdAtEpochMs }
        }.onSuccess { items ->
            _state.value = NotificationsRepositoryState(
                isLoading = false,
                items = items,
            )
        }.onFailure { error ->
            _state.value = NotificationsRepositoryState(
                isLoading = false,
                items = emptyList(),
                errorMessage = error.message ?: "We couldn't load notifications right now.",
            )
        }
    }

    override suspend fun record(event: NotificationEvent): NotificationItem? {
        val householdId = event.householdId.takeIf(String::isNotBlank) ?: return null
        val item = event.toNotificationItem()
        val inserted = runCatching {
            val current = persistence.loadStore(householdId)
            if (current.items.any { persisted -> persisted.id == item.id }) {
                false
            } else {
                val store = current.copy(
                    items = (listOf(item.toPersisted()) + current.items)
                        .sortedByDescending { it.createdAtEpochMs },
                )
                persistence.saveStore(householdId, store)
                _state.value = NotificationsRepositoryState(
                    isLoading = false,
                    items = store.items.map { persisted -> persisted.toDomain() }
                        .sortedByDescending { persisted -> persisted.createdAtEpochMs },
                )
                true
            }
        }.onFailure { error ->
            _state.value = _state.value.copy(
                isLoading = false,
                errorMessage = error.message ?: "We couldn't update notifications right now.",
            )
        }.getOrDefault(false)
        return item.takeIf { inserted }
    }

    override suspend fun markRead(notificationId: String) {
        val householdId = activeHouseholdId() ?: return
        mutateStore(householdId) { store ->
            store.copy(
                items = store.items.map { item ->
                    if (item.id == notificationId && item.readAtEpochMs == null) {
                        item.copy(readAtEpochMs = kotlin.time.Clock.System.now().toEpochMilliseconds())
                    } else {
                        item
                    }
                },
            )
        }
    }

    override suspend fun markAllRead() {
        val householdId = activeHouseholdId() ?: return
        val markedAt = kotlin.time.Clock.System.now().toEpochMilliseconds()
        mutateStore(householdId) { store ->
            store.copy(
                items = store.items.map { item ->
                    if (item.readAtEpochMs == null) item.copy(readAtEpochMs = markedAt) else item
                },
            )
        }
    }

    private suspend fun mutateStore(
        householdId: String,
        transform: (PersistedNotificationsStore) -> PersistedNotificationsStore,
    ): PersistedNotificationsStore? =
        runCatching {
            val current = persistence.loadStore(householdId)
            val updated = transform(current)
            persistence.saveStore(householdId, updated)
            val items = updated.items.map { item -> item.toDomain() }
                .sortedByDescending { item -> item.createdAtEpochMs }
            _state.value = NotificationsRepositoryState(
                isLoading = false,
                items = items,
            )
            updated
        }.onFailure { error ->
            _state.value = _state.value.copy(
                isLoading = false,
                errorMessage = error.message ?: "We couldn't update notifications right now.",
            )
        }.getOrNull()

    private fun activeHouseholdId(): String? =
        sessionRepository.session.value.householdId.takeIf(String::isNotBlank)
}

private fun PersistedNotificationItem.toDomain(): NotificationItem = NotificationItem(
    id = id,
    householdId = householdId,
    choreId = choreId,
    type = notificationTypeOrDefault(type),
    title = title,
    body = body,
    createdAtEpochMs = createdAtEpochMs,
    readAtEpochMs = readAtEpochMs,
)

private fun NotificationItem.toPersisted(): PersistedNotificationItem = PersistedNotificationItem(
    id = id,
    householdId = householdId,
    choreId = choreId,
    type = type.name,
    title = title,
    body = body,
    createdAtEpochMs = createdAtEpochMs,
    readAtEpochMs = readAtEpochMs,
)
