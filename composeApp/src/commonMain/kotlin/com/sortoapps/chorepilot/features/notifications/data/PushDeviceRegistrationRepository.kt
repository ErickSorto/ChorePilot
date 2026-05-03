package com.sortoapps.chorepilot.features.notifications.data

import com.sortoapps.chorepilot.core.firebase.FirestorePaths
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore
import kotlinx.serialization.Serializable

interface PushDeviceRegistrationRepository {
    suspend fun upsert(
        userId: String,
        installationId: String,
        document: PushDeviceRegistrationDocument,
    )

    suspend fun delete(
        userId: String,
        installationId: String,
    )
}

internal class FirebasePushDeviceRegistrationRepository(
    private val firestore: FirebaseFirestore = Firebase.firestore,
) : PushDeviceRegistrationRepository {
    override suspend fun upsert(
        userId: String,
        installationId: String,
        document: PushDeviceRegistrationDocument,
    ) {
        deviceRef(userId, installationId).set(document, merge = true)
    }

    override suspend fun delete(
        userId: String,
        installationId: String,
    ) {
        deviceRef(userId, installationId).delete()
    }

    private fun deviceRef(
        userId: String,
        installationId: String,
    ) = firestore.collection(FirestorePaths.Users)
        .document(userId)
        .collection(FirestorePaths.Devices)
        .document(installationId)
}

internal class NoOpPushDeviceRegistrationRepository : PushDeviceRegistrationRepository {
    override suspend fun upsert(
        userId: String,
        installationId: String,
        document: PushDeviceRegistrationDocument,
    ) = Unit

    override suspend fun delete(
        userId: String,
        installationId: String,
    ) = Unit
}

@Serializable
data class PushDeviceRegistrationDocument(
    val installationId: String = "",
    val userId: String = "",
    val householdId: String = "",
    val platform: String = "",
    val fcmToken: String = "",
    val notificationsEnabled: Boolean = false,
    val updatedAtEpochMs: Long = 0L,
)
