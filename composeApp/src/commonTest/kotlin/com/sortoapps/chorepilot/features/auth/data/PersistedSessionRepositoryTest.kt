package com.sortoapps.chorepilot.features.auth.data

import com.sortoapps.chorepilot.core.platform.PlatformPreferences
import com.sortoapps.chorepilot.features.auth.domain.AuthFlowResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PersistedSessionRepositoryTest {
    @Test
    fun googleSignInRoutesToOnboardingWhenOnboardingIsIncomplete() = kotlinx.coroutines.test.runTest {
        val repository = PersistedSessionRepository(InMemoryPlatformPreferences())

        val result = repository.signInWithGoogle(
            idToken = "token",
            accessToken = null,
            displayNameHint = "Erick",
            emailHint = "erick@example.com",
        )

        assertEquals(AuthFlowResult.Onboarding, result)
        assertEquals(true, repository.session.value.signedIn)
        assertEquals(true, repository.session.value.needsOnboardingFlow)
        assertEquals(true, repository.session.value.needsHouseholdSetup)
    }

    @Test
    fun googleSignInRoutesToHouseholdChoiceWhenOnboardingAlreadyDone() = kotlinx.coroutines.test.runTest {
        val repository = PersistedSessionRepository(InMemoryPlatformPreferences())
        repository.completeOnboarding("Erick")

        val result = repository.signInWithGoogle(
            idToken = "token",
            accessToken = null,
            displayNameHint = "Erick",
            emailHint = "erick@example.com",
        )

        assertEquals(AuthFlowResult.HouseholdChoice, result)
        assertEquals(false, repository.session.value.needsOnboardingFlow)
        assertEquals(true, repository.session.value.needsHouseholdSetup)
    }

    @Test
    fun createHouseholdRequiresSignedInAccount() = kotlinx.coroutines.test.runTest {
        val repository = PersistedSessionRepository(InMemoryPlatformPreferences())

        val error = assertFailsWith<IllegalStateException> {
            repository.createHousehold(
                displayName = "Erick",
                householdName = "River House",
                householdSizeLabel = "4 to 5 members",
            )
        }

        assertEquals("Sign in with Google before creating a household.", error.message)
    }

    @Test
    fun joinHouseholdRequiresSignedInAccount() = kotlinx.coroutines.test.runTest {
        val repository = PersistedSessionRepository(InMemoryPlatformPreferences())

        val error = assertFailsWith<IllegalStateException> {
            repository.joinHousehold(
                displayName = "Erick",
                inviteCode = "ABCD",
            )
        }

        assertEquals("Sign in with Google before joining a household.", error.message)
    }
}

private class InMemoryPlatformPreferences : PlatformPreferences {
    private val store = mutableMapOf<String, String>()

    override fun getString(key: String): String? = store[key]

    override fun putString(key: String, value: String) {
        store[key] = value
    }

    override fun remove(key: String) {
        store.remove(key)
    }
}
