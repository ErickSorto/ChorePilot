package com.sortoapps.chorepilot.features.auth.domain

import kotlinx.coroutines.flow.StateFlow

interface SessionRepository {
    val session: StateFlow<SessionSnapshot>

    suspend fun refreshSession()

    suspend fun completeOnboarding(displayName: String)

    suspend fun signIn(
        email: String,
        password: String,
        displayNameHint: String,
    ): AuthFlowResult

    suspend fun signInWithGoogle(
        idToken: String,
        accessToken: String?,
        displayNameHint: String,
        emailHint: String,
    ): AuthFlowResult

    suspend fun createHousehold(
        displayName: String,
        householdName: String,
        householdSizeLabel: String,
    ): AuthFlowResult

    suspend fun joinHousehold(
        displayName: String,
        inviteCode: String,
    ): AuthFlowResult

    suspend fun previewInvite(inviteCode: String): InvitePreview?

    suspend fun dismissInitialPremiumFlow()

    suspend fun switchActiveHousehold(householdId: String) = Unit

    suspend fun signOut()

    suspend fun resetDemo()
}
