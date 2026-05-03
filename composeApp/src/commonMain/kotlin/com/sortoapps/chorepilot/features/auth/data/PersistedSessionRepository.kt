package com.sortoapps.chorepilot.features.auth.data

import com.sortoapps.chorepilot.core.platform.PlatformPreferences
import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import com.sortoapps.chorepilot.features.auth.domain.AuthFlowResult
import com.sortoapps.chorepilot.features.auth.domain.HouseholdRole
import com.sortoapps.chorepilot.features.auth.domain.InvitePreview
import com.sortoapps.chorepilot.features.auth.domain.SessionSnapshot
import com.sortoapps.chorepilot.features.auth.domain.SessionHouseholdOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json

class PersistedSessionRepository(
    private val platformPreferences: PlatformPreferences,
) : SessionRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _session = MutableStateFlow(loadState())
    override val session: StateFlow<SessionSnapshot> = _session.asStateFlow()

    override suspend fun refreshSession() = Unit

    override suspend fun completeOnboarding(displayName: String) {
        updateSession { snapshot ->
            snapshot.copy(
                onboardingCompleted = true,
                needsOnboardingFlow = false,
                needsInitialPremiumFlow = snapshot.signedIn,
                displayName = displayName.ifBlank { snapshot.displayName },
            )
        }
    }

    override suspend fun signIn(
        email: String,
        password: String,
        displayNameHint: String,
    ): AuthFlowResult {
        error("Email and password sign-in is no longer supported. Use Google to continue.")
    }

    override suspend fun signInWithGoogle(
        idToken: String,
        accessToken: String?,
        displayNameHint: String,
        emailHint: String,
    ): AuthFlowResult {
        updateSession { snapshot ->
            snapshot.copy(
                onboardingCompleted = snapshot.onboardingCompleted,
                signedIn = true,
                needsOnboardingFlow = !snapshot.onboardingCompleted,
                needsInitialPremiumFlow = false,
                needsHouseholdSetup = true,
                displayName = displayNameHint.ifBlank { snapshot.displayName },
                email = emailHint.ifBlank { snapshot.email },
            )
        }
        return if (_session.value.needsOnboardingFlow) AuthFlowResult.Onboarding else AuthFlowResult.HouseholdChoice
    }

    override suspend fun createHousehold(
        displayName: String,
        householdName: String,
        householdSizeLabel: String,
    ): AuthFlowResult {
        check(_session.value.signedIn) { "Sign in with Google before creating a household." }
        updateSession { snapshot ->
            snapshot.copy(
                onboardingCompleted = snapshot.onboardingCompleted,
                signedIn = true,
                needsHouseholdSetup = false,
                needsOnboardingFlow = !snapshot.onboardingCompleted,
                needsInitialPremiumFlow = snapshot.onboardingCompleted,
                displayName = displayName.ifBlank { snapshot.displayName },
                householdId = if (snapshot.householdId.isBlank()) "local-household" else snapshot.householdId,
                householdName = householdName.ifBlank { snapshot.householdName },
                householdSizeLabel = householdSizeLabel.ifBlank { snapshot.householdSizeLabel },
                availableHouseholds = listOf(
                    SessionHouseholdOption(
                        id = if (snapshot.householdId.isBlank()) "local-household" else snapshot.householdId,
                        name = householdName.ifBlank { snapshot.householdName },
                        role = HouseholdRole.Owner,
                    ),
                ),
                role = HouseholdRole.Owner,
            )
        }
        return if (_session.value.needsOnboardingFlow) AuthFlowResult.Onboarding else AuthFlowResult.Premium
    }

    override suspend fun joinHousehold(displayName: String, inviteCode: String): AuthFlowResult {
        check(_session.value.signedIn) { "Sign in with Google before joining a household." }
        updateSession { snapshot ->
            snapshot.copy(
                onboardingCompleted = snapshot.onboardingCompleted,
                signedIn = true,
                needsHouseholdSetup = false,
                needsOnboardingFlow = !snapshot.onboardingCompleted,
                needsInitialPremiumFlow = snapshot.onboardingCompleted,
                displayName = displayName.ifBlank { snapshot.displayName },
                householdId = "joined-${inviteCode.lowercase()}",
                householdName = "Joined via ${inviteCode.uppercase()}",
                availableHouseholds = listOf(
                    SessionHouseholdOption(
                        id = "joined-${inviteCode.lowercase()}",
                        name = "Joined via ${inviteCode.uppercase()}",
                        role = HouseholdRole.Member,
                    ),
                ),
            )
        }
        return if (_session.value.needsOnboardingFlow) AuthFlowResult.Onboarding else AuthFlowResult.Premium
    }

    override suspend fun previewInvite(inviteCode: String): InvitePreview? = InvitePreview(
        householdId = "preview-${inviteCode.lowercase()}",
        householdName = "Joined via ${inviteCode.uppercase()}",
        role = HouseholdRole.Member,
        memberCountLabel = "4 people in the home",
    )

    override suspend fun dismissInitialPremiumFlow() {
        updateSession { snapshot ->
            snapshot.copy(needsInitialPremiumFlow = false)
        }
    }

    override suspend fun switchActiveHousehold(householdId: String) {
        updateSession { snapshot ->
            val selected = snapshot.availableHouseholds.firstOrNull { it.id == householdId } ?: return@updateSession snapshot
            snapshot.copy(
                householdId = selected.id,
                householdName = selected.name,
                householdHasChorePilotPro = selected.hasChorePilotPro,
                role = selected.role,
                needsHouseholdSetup = false,
            )
        }
    }

    override suspend fun signOut() {
        updateSession { _ ->
            SessionSnapshot()
        }
    }

    override suspend fun resetDemo() {
        _session.value = SessionSnapshot()
        platformPreferences.remove(SESSION_KEY)
    }

    private fun loadState(): SessionSnapshot {
        val persisted = platformPreferences.getString(SESSION_KEY) ?: return SessionSnapshot()
        return runCatching {
            json.decodeFromString<SessionSnapshot>(persisted)
        }.getOrElse {
            SessionSnapshot()
        }
    }

    private fun updateSession(transform: (SessionSnapshot) -> SessionSnapshot) {
        _session.update(transform)
        platformPreferences.putString(
            SESSION_KEY,
            json.encodeToString(SessionSnapshot.serializer(), _session.value),
        )
    }

    private companion object {
        const val SESSION_KEY = "session_snapshot"
    }
}
