package com.sortoapps.chorepilot.features.notifications.domain

import com.sortoapps.chorepilot.core.platform.PlatformPreferences
import com.sortoapps.chorepilot.features.auth.domain.AuthFlowResult
import com.sortoapps.chorepilot.features.auth.domain.HouseholdRole
import com.sortoapps.chorepilot.features.auth.domain.InvitePreview
import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import com.sortoapps.chorepilot.features.auth.domain.SessionSnapshot
import com.sortoapps.chorepilot.features.notifications.data.PushDeviceRegistrationDocument
import com.sortoapps.chorepilot.features.notifications.data.PushDeviceRegistrationRepository
import com.sortoapps.chorepilot.features.settings.domain.SettingsRepository
import com.sortoapps.chorepilot.features.settings.domain.SettingsSnapshot
import com.sortoapps.chorepilot.features.settings.domain.ThemeMode
import com.sortoapps.chorepilot.features.settings.domain.AppearancePreset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class PushNotificationRegistrationSynchronizerTest {
    @Test
    fun tokenAndSignedInSessionRegisterCurrentDevice() = runTest {
        val repository = FakePushDeviceRegistrationRepository()
        val sessionRepository = FakePushSessionRepository(
            SessionSnapshot(
                signedIn = true,
                userId = "user-1",
                householdId = "house-1",
                role = HouseholdRole.Member,
            ),
        )
        val settingsRepository = FakePushSettingsRepository(SettingsSnapshot(notificationsEnabled = true))
        val tokenSource = FakePushTokenSource(platform = "ios", initialToken = "token-123")
        val preferences = FakePushPlatformPreferences()
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())

        PushNotificationRegistrationSynchronizer(
            registrationRepository = repository,
            sessionRepository = sessionRepository,
            settingsRepository = settingsRepository,
            pushTokenSource = tokenSource,
            platformPreferences = preferences,
            appScope = scope,
        )
        advanceUntilIdle()

        val registration = repository.upserts.single()
        assertEquals("user-1", registration.first)
        assertEquals("user-1", registration.second.userId)
        assertEquals("house-1", registration.second.householdId)
        assertEquals("ios", registration.second.platform)
        assertEquals("token-123", registration.second.fcmToken)
        scope.cancel()
    }

    @Test
    fun signOutRemovesPreviousDeviceRegistration() = runTest {
        val repository = FakePushDeviceRegistrationRepository()
        val sessionState = MutableStateFlow(
            SessionSnapshot(
                signedIn = true,
                userId = "user-1",
                householdId = "house-1",
                role = HouseholdRole.Member,
            ),
        )
        val sessionRepository = FakePushSessionRepository(sessionState)
        val settingsRepository = FakePushSettingsRepository(SettingsSnapshot(notificationsEnabled = true))
        val tokenSource = FakePushTokenSource(platform = "android", initialToken = "token-123")
        val preferences = FakePushPlatformPreferences()
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())

        PushNotificationRegistrationSynchronizer(
            registrationRepository = repository,
            sessionRepository = sessionRepository,
            settingsRepository = settingsRepository,
            pushTokenSource = tokenSource,
            platformPreferences = preferences,
            appScope = scope,
        )
        advanceUntilIdle()

        sessionState.value = SessionSnapshot()
        advanceUntilIdle()

        assertEquals("user-1", repository.deletes.single().first)
        scope.cancel()
    }
}

private class FakePushTokenSource(
    override val platform: String,
    initialToken: String,
) : PushTokenSource {
    override val tokens = MutableStateFlow(initialToken)
}

private class FakePushPlatformPreferences : PlatformPreferences {
    private val values = mutableMapOf<String, String>()

    override fun getString(key: String): String? = values[key]

    override fun putString(key: String, value: String) {
        values[key] = value
    }

    override fun remove(key: String) {
        values.remove(key)
    }
}

private class FakePushDeviceRegistrationRepository : PushDeviceRegistrationRepository {
    val upserts = mutableListOf<Pair<String, PushDeviceRegistrationDocument>>()
    val deletes = mutableListOf<Pair<String, String>>()

    override suspend fun upsert(
        userId: String,
        installationId: String,
        document: PushDeviceRegistrationDocument,
    ) {
        upserts += userId to document.copy(installationId = installationId)
    }

    override suspend fun delete(
        userId: String,
        installationId: String,
    ) {
        deletes += userId to installationId
    }
}

private class FakePushSessionRepository(
    override val session: StateFlow<SessionSnapshot>,
) : SessionRepository {
    constructor(initial: SessionSnapshot) : this(MutableStateFlow(initial))

    override suspend fun refreshSession() = Unit
    override suspend fun completeOnboarding(displayName: String) = Unit
    override suspend fun signIn(email: String, password: String, displayNameHint: String): AuthFlowResult = AuthFlowResult.Main
    override suspend fun signInWithGoogle(idToken: String, accessToken: String?, displayNameHint: String, emailHint: String): AuthFlowResult = AuthFlowResult.Main
    override suspend fun createHousehold(displayName: String, householdName: String, householdSizeLabel: String): AuthFlowResult = AuthFlowResult.Main
    override suspend fun joinHousehold(displayName: String, inviteCode: String): AuthFlowResult = AuthFlowResult.Main
    override suspend fun previewInvite(inviteCode: String): InvitePreview? = null
    override suspend fun dismissInitialPremiumFlow() = Unit
    override suspend fun switchActiveHousehold(householdId: String) = Unit
    override suspend fun signOut() = Unit
    override suspend fun resetDemo() = Unit
}

private class FakePushSettingsRepository(
    initial: SettingsSnapshot,
) : SettingsRepository {
    private val state = MutableStateFlow(initial)
    override val settings: StateFlow<SettingsSnapshot> = state

    override suspend fun updateThemeMode(themeMode: ThemeMode) = Unit
    override suspend fun updateAppearancePreset(appearancePreset: AppearancePreset) = Unit
    override suspend fun updateEditorialWarmth(enabled: Boolean) = Unit
    override suspend fun updateNotifications(enabled: Boolean) {
        state.value = state.value.copy(notificationsEnabled = enabled)
    }
    override suspend fun updateWeekStartsOnMonday(enabled: Boolean) = Unit
    override suspend fun updateAutoAssignSuggestions(enabled: Boolean) = Unit
    override suspend fun updateLargerText(enabled: Boolean) = Unit
    override suspend fun updateAlwaysShowLabels(enabled: Boolean) = Unit
}
