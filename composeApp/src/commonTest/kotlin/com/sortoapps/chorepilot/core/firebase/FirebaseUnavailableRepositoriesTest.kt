package com.sortoapps.chorepilot.core.firebase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlinx.coroutines.test.runTest

class FirebaseUnavailableRepositoriesTest {
    @Test
    fun unavailableSessionRepositoryExposesClearFailureState() = runTest {
        val repository = FirebaseUnavailableSessionRepository(
            FirebaseAvailability(
                status = FirebaseAvailabilityStatus.MissingPlatformConfig,
                message = "Add composeApp/google-services.json.",
            ),
        )

        assertFalse(repository.session.value.signedIn)
        assertEquals("Add composeApp/google-services.json.", repository.session.value.serviceErrorMessage)

        val error = assertFailsWith<IllegalStateException> {
            repository.signInWithGoogle(
                idToken = "token",
                accessToken = null,
                displayNameHint = "Erick",
                emailHint = "erick@example.com",
            )
        }

        assertEquals("Add composeApp/google-services.json.", error.message)
    }

    @Test
    fun unavailableChoreRepositoryStaysEmptyAndFailsMutationsClearly() = runTest {
        val repository = FirebaseUnavailableChoreRepository(
            FirebaseAvailability(
                status = FirebaseAvailabilityStatus.InitializationFailed,
                message = "Firebase failed to initialize on iOS.",
            ),
        )

        assertEquals(emptyList(), repository.getChores())

        val error = assertFailsWith<IllegalStateException> {
            repository.deleteChore("chore-1")
        }

        assertEquals("Firebase failed to initialize on iOS.", error.message)
    }
}
