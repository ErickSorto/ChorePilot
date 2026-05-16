package com.sortoapps.chorepilot.features.auth.presentation.viewmodel

import com.sortoapps.chorepilot.core.platform.VibratorUtil
import com.sortoapps.chorepilot.features.auth.domain.AuthFlowResult
import com.sortoapps.chorepilot.features.auth.domain.InvitePreview
import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import com.sortoapps.chorepilot.features.auth.domain.SessionSnapshot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelFirebaseConfigTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun serviceErrorFromSessionDisablesGoogleSignIn() = runTest(dispatcher) {
        val sessionRepository = FakeSessionRepository(
            SessionSnapshot(
                serviceErrorMessage = "Firebase isn't configured on Android. Add composeApp/google-services.json and rebuild the app.",
            ),
        )
        val viewModel = AuthViewModel(
            sessionRepository = sessionRepository,
            vibratorUtil = NoOpVibratorUtil(),
        )

        runCurrent()

        assertFalse(viewModel.uiState.value.googleSignInAvailable)
        assertEquals(
            "Firebase isn't configured on Android. Add composeApp/google-services.json and rebuild the app.",
            viewModel.uiState.value.errorMessage,
        )
    }

    @Test
    fun invitePreviewLoadsForCompleteCodes() = runTest(dispatcher) {
        val sessionRepository = FakeSessionRepository(
            initialSnapshot = SessionSnapshot(),
            invitePreview = InvitePreview(
                householdId = "household-1",
                householdName = "Sorto Family",
                role = com.sortoapps.chorepilot.features.auth.domain.HouseholdRole.Member,
                memberCountLabel = "4 active members",
            ),
        )
        val viewModel = AuthViewModel(
            sessionRepository = sessionRepository,
            vibratorUtil = NoOpVibratorUtil(),
        )

        viewModel.onEvent(AuthUiEvent.InviteCodeChanged("join42"))
        advanceUntilIdle()

        assertEquals("JOIN42", viewModel.uiState.value.inviteCode)
        assertFalse(viewModel.uiState.value.isInvitePreviewLoading)
        assertEquals("Sorto Family", viewModel.uiState.value.invitePreview?.householdName)
        assertEquals("JOIN42", sessionRepository.lastPreviewCode)
    }

    @Test
    fun shortInviteCodesClearPreviewWithoutLoading() = runTest(dispatcher) {
        val sessionRepository = FakeSessionRepository(
            initialSnapshot = SessionSnapshot(),
        )
        val viewModel = AuthViewModel(
            sessionRepository = sessionRepository,
            vibratorUtil = NoOpVibratorUtil(),
        )

        viewModel.onEvent(AuthUiEvent.InviteCodeChanged("ab"))
        runCurrent()

        assertEquals("AB", viewModel.uiState.value.inviteCode)
        assertFalse(viewModel.uiState.value.isInvitePreviewLoading)
        assertNull(viewModel.uiState.value.invitePreview)
        assertNull(sessionRepository.lastPreviewCode)
    }
}

private class FakeSessionRepository(
    initialSnapshot: SessionSnapshot,
    private val invitePreview: InvitePreview? = null,
) : SessionRepository {
    private val mutableSession = MutableStateFlow(initialSnapshot)
    var lastPreviewCode: String? = null

    override val session: StateFlow<SessionSnapshot> = mutableSession

    override suspend fun refreshSession() = Unit

    override suspend fun completeOnboarding(displayName: String) = Unit

    override suspend fun signIn(
        email: String,
        password: String,
        displayNameHint: String,
    ): AuthFlowResult = error("Not needed for this test")

    override suspend fun signInWithGoogle(
        idToken: String,
        accessToken: String?,
        displayNameHint: String,
        emailHint: String,
    ): AuthFlowResult = error("Not needed for this test")

    override suspend fun createHousehold(
        displayName: String,
        householdName: String,
        householdSizeLabel: String,
    ): AuthFlowResult = error("Not needed for this test")

    override suspend fun joinHousehold(
        displayName: String,
        inviteCode: String,
    ): AuthFlowResult = error("Not needed for this test")

    override suspend fun previewInvite(inviteCode: String): InvitePreview? {
        lastPreviewCode = inviteCode
        return invitePreview
    }

    override suspend fun dismissInitialPremiumFlow() = Unit

    override suspend fun signOut() = Unit

    override suspend fun resetDemo() = Unit
}

private class NoOpVibratorUtil : VibratorUtil {
    override fun triggerVibration() = Unit

    override fun triggerVibrationSuccess() = Unit

    override fun cancelVibration() = Unit
}
