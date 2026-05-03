package com.sortoapps.chorepilot.features.notifications.data

import com.sortoapps.chorepilot.core.platform.PlatformPreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PersistedNotificationsStore(
    val items: List<PersistedNotificationItem> = emptyList(),
)

@Serializable
data class PersistedNotificationItem(
    val id: String = "",
    val householdId: String = "",
    val choreId: String = "",
    val type: String = "",
    val title: String = "",
    val body: String = "",
    val createdAtEpochMs: Long = 0L,
    val readAtEpochMs: Long? = null,
)

interface NotificationsStorePersistence {
    suspend fun loadStore(householdId: String): PersistedNotificationsStore

    suspend fun saveStore(
        householdId: String,
        store: PersistedNotificationsStore,
    )
}

class PlatformPreferencesNotificationsStorePersistence(
    private val platformPreferences: PlatformPreferences,
) : NotificationsStorePersistence {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun loadStore(householdId: String): PersistedNotificationsStore {
        val persisted = platformPreferences.getString(storeKey(householdId))
            ?: return PersistedNotificationsStore()
        return runCatching {
            json.decodeFromString(PersistedNotificationsStore.serializer(), persisted)
        }.getOrDefault(PersistedNotificationsStore())
    }

    override suspend fun saveStore(
        householdId: String,
        store: PersistedNotificationsStore,
    ) {
        platformPreferences.putString(
            storeKey(householdId),
            json.encodeToString(PersistedNotificationsStore.serializer(), store),
        )
    }

    private fun storeKey(householdId: String): String = "notifications_store_${slugify(householdId)}"

    private fun slugify(value: String): String = value
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .ifBlank { "item" }
}
