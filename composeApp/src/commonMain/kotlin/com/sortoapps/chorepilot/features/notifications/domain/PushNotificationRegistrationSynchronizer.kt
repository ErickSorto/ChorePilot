package com.sortoapps.chorepilot.features.notifications.domain

import com.sortoapps.chorepilot.core.platform.PlatformPreferences
import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import com.sortoapps.chorepilot.features.notifications.data.PushDeviceRegistrationDocument
import com.sortoapps.chorepilot.features.notifications.data.PushDeviceRegistrationRepository
import com.sortoapps.chorepilot.features.settings.domain.SettingsRepository
import kotlin.random.Random
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class PushNotificationRegistrationSynchronizer(
    private val registrationRepository: PushDeviceRegistrationRepository,
    private val sessionRepository: SessionRepository,
    private val settingsRepository: SettingsRepository,
    private val pushTokenSource: PushTokenSource,
    private val platformPreferences: PlatformPreferences,
    appScope: CoroutineScope,
) {
    init {
        appScope.launch {
            combine(
                sessionRepository.session,
                settingsRepository.settings,
                pushTokenSource.tokens,
            ) { session, settings, token ->
                PushRegistrationSnapshot(
                    userId = session.userId.takeIf { session.signedIn && it.isNotBlank() }.orEmpty(),
                    householdId = session.householdId,
                    notificationsEnabled = settings.notificationsEnabled,
                    token = token?.takeIf(String::isNotBlank).orEmpty(),
                )
            }
                .distinctUntilChanged()
                .collect { snapshot ->
                    runCatching { synchronize(snapshot) }
                }
        }
    }

    private suspend fun synchronize(snapshot: PushRegistrationSnapshot) {
        val installationId = installationId()
        val previousUserId = platformPreferences.getString(LAST_SYNCED_USER_ID_KEY).orEmpty()
        if (previousUserId.isNotBlank() && previousUserId != snapshot.userId) {
            runCatching { registrationRepository.delete(previousUserId, installationId) }
        }

        if (snapshot.userId.isBlank()) {
            platformPreferences.remove(LAST_SYNCED_USER_ID_KEY)
            return
        }

        platformPreferences.putString(LAST_SYNCED_USER_ID_KEY, snapshot.userId)
        if (snapshot.token.isBlank()) return

        runCatching {
            registrationRepository.upsert(
                userId = snapshot.userId,
                installationId = installationId,
                document = PushDeviceRegistrationDocument(
                    installationId = installationId,
                    userId = snapshot.userId,
                    householdId = snapshot.householdId,
                    platform = pushTokenSource.platform,
                    fcmToken = snapshot.token,
                    notificationsEnabled = snapshot.notificationsEnabled,
                    updatedAtEpochMs = Clock.System.now().toEpochMilliseconds(),
                ),
            )
        }
    }

    private fun installationId(): String {
        val existing = platformPreferences.getString(INSTALLATION_ID_KEY)
        if (!existing.isNullOrBlank()) return existing
        val generated = buildString {
            repeat(4) {
                append(Random.nextInt().toUInt().toString(16).padStart(8, '0'))
            }
        }
        platformPreferences.putString(INSTALLATION_ID_KEY, generated)
        return generated
    }

    companion object {
        const val INSTALLATION_ID_KEY = "push.installation_id"
        const val LAST_SYNCED_USER_ID_KEY = "push.last_synced_user_id"
        const val TOKEN_KEY = "push.fcm_token"
    }
}

private data class PushRegistrationSnapshot(
    val userId: String,
    val householdId: String,
    val notificationsEnabled: Boolean,
    val token: String,
)
