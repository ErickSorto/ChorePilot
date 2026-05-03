package com.sortoapps.chorepilot.features.auth.data

import com.sortoapps.chorepilot.core.firebase.FirebaseHouseholdStore
import com.sortoapps.chorepilot.core.firebase.HouseholdDocument
import com.sortoapps.chorepilot.core.firebase.HouseholdMemberDocument
import com.sortoapps.chorepilot.core.firebase.UserProfileDocument
import com.sortoapps.chorepilot.core.firebase.joinFailureMessage
import com.sortoapps.chorepilot.core.firebase.resolveInviteLifecycle
import com.sortoapps.chorepilot.core.platform.PlatformPreferences
import com.sortoapps.chorepilot.features.auth.domain.AuthFlowResult
import com.sortoapps.chorepilot.features.auth.domain.HouseholdRole
import com.sortoapps.chorepilot.features.auth.domain.InvitePreview
import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import com.sortoapps.chorepilot.features.auth.domain.SessionHouseholdOption
import com.sortoapps.chorepilot.features.auth.domain.SessionSnapshot
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class FirebaseSessionRepository(
    private val platformPreferences: PlatformPreferences,
    private val appScope: CoroutineScope,
    private val householdStore: FirebaseHouseholdStore,
) : SessionRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val auth get() = Firebase.auth

    private val _session = MutableStateFlow(loadLocalState())
    override val session: StateFlow<SessionSnapshot> = _session.asStateFlow()

    init {
        appScope.launch {
            runCatching {
                auth.authStateChanged.collectLatest { user ->
                    refreshSessionFromFirebase(userIdOverride = user?.uid)
                }
            }
        }
    }

    override suspend fun refreshSession() {
        refreshSessionFromFirebase()
    }

    override suspend fun completeOnboarding(displayName: String) {
        val normalizedName = displayName.ifBlank { _session.value.displayName }
        updateLocalState { state ->
            state.copy(
                onboardingCompleted = true,
                needsOnboardingFlow = false,
                needsInitialPremiumFlow = state.signedIn,
                displayName = normalizedName,
            )
        }
        val user = auth.currentUser ?: return
        runCatching {
            user.updateProfile(displayName = normalizedName)
            val existing = safeLoadUser(user.uid).orDefault(user = user, fallback = _session.value)
            saveUserSafely(
                existing.copy(
                    displayName = normalizedName,
                    onboardingCompleted = true,
                ),
            )
            refreshSessionFromFirebase(userIdOverride = user.uid)
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
        val credential = GoogleAuthProvider.credential(
            idToken = idToken,
            accessToken = accessToken,
        )
        val result = auth.signInWithCredential(credential)
        val user = result.user ?: error("We couldn't finish Google sign-in right now.")
        return completeSignIn(
            user = user,
            displayNameHint = displayNameHint,
            emailHint = emailHint,
        )
    }

    override suspend fun createHousehold(
        displayName: String,
        householdName: String,
        householdSizeLabel: String,
    ): AuthFlowResult {
        val user = auth.currentUser?.takeIf { !it.isAnonymous }
            ?: error("Sign in with Google before creating a household.")
        val normalizedName = displayName.ifBlank { user.displayName.orEmpty().ifBlank { _session.value.displayName } }
        if (normalizedName.isNotBlank()) {
            runCatching { user.updateProfile(displayName = normalizedName) }
        }

        val createdHousehold = runCatching {
            householdStore.createHousehold(
                ownerUserId = user.uid,
                ownerDisplayName = normalizedName,
                householdName = householdName,
                sizeLabel = householdSizeLabel,
            )
        }.getOrElse { error ->
            error("Household creation was blocked. Check Firestore rules for /households/{id} and /households/{id}/members/{uid}. Original error: ${error.message}")
        }

        val existing = safeLoadUser(user.uid).orDefault(user = user, fallback = _session.value)
        saveUserSafely(
            existing.copy(
                displayName = normalizedName,
                email = user.email.orEmpty(),
                activeHouseholdId = createdHousehold.id,
                householdIds = (existing.householdIds + createdHousehold.id).distinct(),
                onboardingCompleted = existing.onboardingCompleted,
                pendingPremiumAfterHouseholdCreation = false,
            ),
        )
        updateLocalState { state ->
            state.copy(
                signedIn = true,
                userId = user.uid,
                email = user.email.orEmpty().ifBlank { state.email },
                isAnonymous = false,
                displayName = normalizedName.ifBlank { state.displayName },
                householdId = createdHousehold.id,
                householdName = createdHousehold.name,
                householdSizeLabel = createdHousehold.sizeLabel,
                role = HouseholdRole.Owner,
                needsHouseholdSetup = false,
                needsOnboardingFlow = false,
                needsInitialPremiumFlow = state.onboardingCompleted,
            )
        }
        refreshSessionFromFirebase(userIdOverride = user.uid)
        runCatching {
            householdStore.createInvite(createdHousehold)
        }
        updateLocalState { state ->
            state.copy(
                needsHouseholdSetup = false,
                needsOnboardingFlow = false,
                needsInitialPremiumFlow = state.onboardingCompleted,
            )
        }
        return AuthFlowResult.Premium
    }

    override suspend fun joinHousehold(
        displayName: String,
        inviteCode: String,
    ): AuthFlowResult {
        val invite = householdStore.loadInvite(inviteCode)
            ?: error("That invite code doesn't exist.")
        val resolvedInvite = invite.resolveInviteLifecycle()
        if (!resolvedInvite.isJoinable) {
            error(resolvedInvite.lifecycleStatus.joinFailureMessage())
        }
        val user = auth.currentUser?.takeIf { !it.isAnonymous }
            ?: error("Sign in with Google before joining a household.")
        val normalizedName = displayName.ifBlank { user.displayName.orEmpty().ifBlank { _session.value.displayName } }
        if (normalizedName.isNotBlank()) {
            runCatching { user.updateProfile(displayName = normalizedName) }
        }
        val household = householdStore.loadHousehold(invite.householdId)
            ?: error("That household is no longer available.")
        val updatedHousehold = householdStore.attachMemberToHousehold(
            household = household,
            userId = user.uid,
            displayName = normalizedName,
            role = resolvedInvite.document.role,
        )
        val existing = safeLoadUser(user.uid).orDefault(user = user, fallback = _session.value)
        saveUserSafely(
            existing.copy(
                displayName = normalizedName,
                email = user.email.orEmpty(),
                activeHouseholdId = updatedHousehold.id,
                householdIds = (existing.householdIds + updatedHousehold.id).distinct(),
                onboardingCompleted = existing.onboardingCompleted,
                pendingPremiumAfterHouseholdCreation = false,
            ),
        )
        householdStore.markInviteUsed(resolvedInvite.document)
        refreshSessionFromFirebase(userIdOverride = user.uid)
        updateLocalState { state ->
            state.copy(
                needsHouseholdSetup = false,
                needsOnboardingFlow = false,
                needsInitialPremiumFlow = state.onboardingCompleted,
            )
        }
        return AuthFlowResult.Premium
    }

    override suspend fun previewInvite(inviteCode: String): InvitePreview? {
        val invite = runCatching { householdStore.loadInvite(inviteCode) }.getOrNull() ?: return null
        val resolvedInvite = invite.resolveInviteLifecycle()
        if (!resolvedInvite.isJoinable) return null
        return InvitePreview(
            householdId = resolvedInvite.document.householdId,
            householdName = resolvedInvite.document.householdName,
            role = resolvedInvite.document.role,
            memberCountLabel = "Invite ready to join",
        )
    }

    override suspend fun dismissInitialPremiumFlow() {
        updateLocalState { it.copy(needsInitialPremiumFlow = false) }
        val user = auth.currentUser ?: return
        val existing = safeLoadUser(user.uid).orDefault(user = user, fallback = _session.value)
        saveUserSafely(existing.copy(pendingPremiumAfterHouseholdCreation = false))
        refreshSessionFromFirebase(userIdOverride = user.uid)
    }

    override suspend fun switchActiveHousehold(householdId: String) {
        if (householdId.isBlank() || householdId == _session.value.householdId) return
        val user = auth.currentUser?.takeIf { !it.isAnonymous }
            ?: error("Sign in before switching households.")
        val profile = safeLoadUser(user.uid).orDefault(user = user, fallback = _session.value)
        val member = safeLoadMember(householdId, user.uid)
            ?: error("That household is no longer available for this account.")
        val household = safeLoadHousehold(householdId)
            ?: error("We couldn't load that household right now.")
        saveUserSafely(
            profile.copy(
                activeHouseholdId = householdId,
                householdIds = (profile.householdIds + householdId).distinct(),
            ),
        )
        updateLocalState { state ->
            state.copy(
                householdId = householdId,
                householdName = household.name.ifBlank { state.householdName },
                householdSizeLabel = household.sizeLabel.ifBlank { state.householdSizeLabel },
                householdHasChorePilotPro = household.hasChorePilotPro,
                role = member.role,
                needsHouseholdSetup = false,
                needsOnboardingFlow = false,
            )
        }
        refreshSessionFromFirebase(userIdOverride = user.uid)
    }

    override suspend fun signOut() {
        runCatching { auth.signOut() }
        updateLocalState { state ->
            state.copy(
                signedIn = false,
                userId = "",
                email = "",
                isAnonymous = false,
                householdId = "",
                householdName = "",
                householdSizeLabel = "",
                availableHouseholds = emptyList(),
                householdHasChorePilotPro = false,
                role = HouseholdRole.Member,
                needsHouseholdSetup = false,
                needsOnboardingFlow = false,
                needsInitialPremiumFlow = false,
            )
        }
    }

    override suspend fun resetDemo() {
        signOut()
        _session.value = SessionSnapshot()
        platformPreferences.remove(SESSION_KEY)
    }

    private suspend fun refreshSessionFromFirebase(
        userIdOverride: String? = auth.currentUser?.uid,
    ): SessionSnapshot {
        val local = _session.value
        val user = auth.currentUser?.takeIf { it.uid == userIdOverride } ?: auth.currentUser
        if (user == null) {
            val signedOut = local.copy(
                signedIn = false,
                userId = "",
                email = "",
                isAnonymous = false,
                householdId = "",
                householdName = "",
                householdSizeLabel = "",
                availableHouseholds = emptyList(),
                householdHasChorePilotPro = false,
                role = HouseholdRole.Member,
                needsHouseholdSetup = false,
                needsOnboardingFlow = false,
                needsInitialPremiumFlow = false,
            )
            persistState(signedOut)
            _session.value = signedOut
            return signedOut
        }

        val localHouseholdFallback = local.householdId.takeIf { local.userId == user.uid }.orEmpty()
        val profile = safeLoadUser(user.uid).orDefault(user = user, fallback = local)
        if (profile.uid.isBlank()) {
            saveUserSafely(profile.copy(uid = user.uid))
        }
        val candidateHouseholdIds = buildList {
            profile.activeHouseholdId
                .takeIf(String::isNotBlank)
                ?.let(::add)
            localHouseholdFallback
                .takeIf(String::isNotBlank)
                ?.let(::add)
            addAll(profile.householdIds.filter(String::isNotBlank))
        }.distinct()

        val availableHouseholds = candidateHouseholdIds.mapNotNull { householdId ->
            val member = safeLoadMember(householdId, user.uid) ?: return@mapNotNull null
            val household = safeLoadHousehold(householdId) ?: return@mapNotNull null
            SessionHouseholdCandidate(
                id = householdId,
                name = household.name.ifBlank { "Your household" },
                sizeLabel = household.sizeLabel,
                role = member.role,
                hasChorePilotPro = household.hasChorePilotPro,
            )
        }
        val activeHousehold = availableHouseholds.firstOrNull { it.id == profile.activeHouseholdId }
            ?: availableHouseholds.firstOrNull { it.id == localHouseholdFallback }
            ?: availableHouseholds.firstOrNull()
        val activeHouseholdId = activeHousehold?.id.orEmpty()
        val merged = local.copy(
            onboardingCompleted = local.onboardingCompleted || profile.onboardingCompleted,
            signedIn = true,
            userId = user.uid,
            email = user.email.orEmpty().ifBlank { profile.email.ifBlank { local.email } },
            isAnonymous = user.isAnonymous,
            displayName = profile.displayName.ifBlank { user.displayName.orEmpty().ifBlank { local.displayName } },
            householdId = activeHouseholdId,
            householdName = activeHousehold?.name ?: local.householdName.takeIf { activeHouseholdId == localHouseholdFallback }.orEmpty(),
            householdSizeLabel = activeHousehold?.sizeLabel ?: local.householdSizeLabel.takeIf { activeHouseholdId == localHouseholdFallback }.orEmpty(),
            availableHouseholds = availableHouseholds.map { household ->
                SessionHouseholdOption(
                    id = household.id,
                    name = household.name,
                    role = household.role,
                    hasChorePilotPro = household.hasChorePilotPro,
                )
            },
            householdHasChorePilotPro = activeHousehold?.hasChorePilotPro == true,
            role = activeHousehold?.role ?: local.role.takeIf { activeHouseholdId == localHouseholdFallback } ?: HouseholdRole.Member,
            needsHouseholdSetup = activeHouseholdId.isBlank(),
            needsOnboardingFlow = !profile.onboardingCompleted && activeHouseholdId.isBlank(),
            needsInitialPremiumFlow = activeHouseholdId.isNotBlank() &&
                profile.onboardingCompleted &&
                (local.needsInitialPremiumFlow || profile.pendingPremiumAfterHouseholdCreation),
        )
        val reconciledProfile = profile.copy(
            activeHouseholdId = if (activeHouseholdId.isNotBlank()) activeHouseholdId else profile.activeHouseholdId,
            householdIds = if (activeHouseholdId.isNotBlank()) {
                (profile.householdIds + activeHouseholdId).distinct()
            } else {
                profile.householdIds
            },
            onboardingCompleted = profile.onboardingCompleted || merged.onboardingCompleted,
        )
        if (reconciledProfile != profile) {
            saveUserSafely(reconciledProfile)
        }
        persistState(merged)
        _session.value = merged
        return merged
    }

    private fun loadLocalState(): SessionSnapshot {
        val persisted = platformPreferences.getString(SESSION_KEY) ?: return SessionSnapshot()
        return runCatching {
            json.decodeFromString<SessionSnapshot>(persisted)
        }.getOrElse {
            SessionSnapshot()
        }
    }

    private fun updateLocalState(transform: (SessionSnapshot) -> SessionSnapshot) {
        _session.update(transform)
        persistState(_session.value)
    }

    private fun persistState(snapshot: SessionSnapshot) {
        platformPreferences.putString(
            SESSION_KEY,
            json.encodeToString(SessionSnapshot.serializer(), snapshot),
        )
    }

    private suspend fun completeSignIn(
        user: dev.gitlive.firebase.auth.FirebaseUser,
        displayNameHint: String,
        emailHint: String,
    ): AuthFlowResult {
        val normalizedName = displayNameHint.ifBlank { user.displayName.orEmpty().ifBlank { _session.value.displayName } }
        if (normalizedName.isNotBlank()) {
            runCatching { user.updateProfile(displayName = normalizedName) }
        }
        val existing = safeLoadUser(user.uid).orDefault(user = user, fallback = _session.value)
        saveUserSafely(
            existing.copy(
                displayName = normalizedName,
                email = user.email.orEmpty().ifBlank { emailHint },
                onboardingCompleted = _session.value.onboardingCompleted || existing.onboardingCompleted,
            ),
        )
        val refreshed = refreshSessionFromFirebase(userIdOverride = user.uid)
        return when {
            refreshed.needsOnboardingFlow -> AuthFlowResult.Onboarding
            refreshed.householdId.isBlank() -> AuthFlowResult.HouseholdChoice
            refreshed.needsInitialPremiumFlow -> AuthFlowResult.Premium
            else -> AuthFlowResult.Main
        }
    }

    private suspend fun safeLoadUser(uid: String): UserProfileDocument? =
        runCatching { householdStore.loadUser(uid) }.getOrNull()

    private suspend fun safeLoadHousehold(householdId: String): HouseholdDocument? =
        runCatching { householdStore.loadHousehold(householdId) }.getOrNull()

    private suspend fun safeLoadMember(
        householdId: String,
        userId: String,
    ): HouseholdMemberDocument? = runCatching { householdStore.loadMember(householdId, userId) }.getOrNull()

    private suspend fun saveUserSafely(profile: UserProfileDocument) {
        runCatching { householdStore.saveUser(profile) }
    }

    private data class SessionHouseholdCandidate(
        val id: String,
        val name: String,
        val sizeLabel: String,
        val role: HouseholdRole,
        val hasChorePilotPro: Boolean,
    )

    private companion object {
        const val SESSION_KEY = "firebase_session_snapshot"
    }
}

private fun UserProfileDocument?.orDefault(
    user: dev.gitlive.firebase.auth.FirebaseUser,
    fallback: SessionSnapshot,
): UserProfileDocument = this ?: UserProfileDocument(
    uid = user.uid,
    displayName = user.displayName.orEmpty().ifBlank { fallback.displayName },
    email = user.email.orEmpty(),
    onboardingCompleted = fallback.onboardingCompleted,
    isAnonymous = user.isAnonymous,
    pendingPremiumAfterHouseholdCreation = false,
)
