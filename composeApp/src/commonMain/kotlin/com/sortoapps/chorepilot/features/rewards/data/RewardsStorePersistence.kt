package com.sortoapps.chorepilot.features.rewards.data

import com.sortoapps.chorepilot.core.firebase.FirebaseHouseholdStore
import com.sortoapps.chorepilot.core.firebase.FirestorePaths
import com.sortoapps.chorepilot.core.platform.PlatformPreferences
import dev.gitlive.firebase.firestore.DocumentSnapshot
import kotlinx.serialization.json.Json

internal interface RewardsStorePersistence {
    suspend fun loadStore(householdId: String): RewardsPersistedHouseholdStore

    suspend fun saveStore(
        householdId: String,
        store: RewardsPersistedHouseholdStore,
    )
}

internal class PlatformPreferencesRewardsStorePersistence(
    private val platformPreferences: PlatformPreferences,
) : RewardsStorePersistence {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun loadStore(
        householdId: String,
    ): RewardsPersistedHouseholdStore {
        val persisted = platformPreferences.getString(storeKey(householdId))
            ?: return RewardsPersistedHouseholdStore()
        return runCatching {
            json.decodeFromString(RewardsPersistedHouseholdStore.serializer(), persisted)
        }.getOrDefault(RewardsPersistedHouseholdStore())
    }

    override suspend fun saveStore(
        householdId: String,
        store: RewardsPersistedHouseholdStore,
    ) {
        platformPreferences.putString(
            storeKey(householdId),
            json.encodeToString(RewardsPersistedHouseholdStore.serializer(), store),
        )
    }

    private fun storeKey(householdId: String): String = "rewards_store_${slugify(householdId)}"

    private fun slugify(value: String): String = value
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .ifBlank { "item" }
}

internal class FirebaseRewardsStorePersistence(
    private val householdStore: FirebaseHouseholdStore,
) : RewardsStorePersistence {
    override suspend fun loadStore(
        householdId: String,
    ): RewardsPersistedHouseholdStore = householdStore
        .householdRef(householdId)
        .collection(FirestorePaths.Rewards)
        .document(RewardsStateDocumentId)
        .get()
        .dataOrNull<RewardsPersistedHouseholdStore>()
        ?: RewardsPersistedHouseholdStore()

    override suspend fun saveStore(
        householdId: String,
        store: RewardsPersistedHouseholdStore,
    ) {
        householdStore
            .householdRef(householdId)
            .collection(FirestorePaths.Rewards)
            .document(RewardsStateDocumentId)
            .set(store, merge = true)
    }
}

private const val RewardsStateDocumentId = "state"

private inline fun <reified T> DocumentSnapshot.dataOrNull(): T? = if (exists) data<T>() else null
