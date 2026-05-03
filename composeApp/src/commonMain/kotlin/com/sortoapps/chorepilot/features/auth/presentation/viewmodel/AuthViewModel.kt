package com.sortoapps.chorepilot.features.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sortoapps.chorepilot.core.platform.VibratorUtil
import com.sortoapps.chorepilot.features.auth.domain.AuthFlowResult
import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import com.sortoapps.chorepilot.features.auth.presentation.platform.GoogleSignInToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val sessionRepository: SessionRepository,
    private val vibratorUtil: VibratorUtil,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        applySessionSnapshot(sessionRepository.session.value)
        viewModelScope.launch {
            sessionRepository.session.collect { snapshot ->
                applySessionSnapshot(snapshot)
            }
        }
    }

    private fun applySessionSnapshot(snapshot: com.sortoapps.chorepilot.features.auth.domain.SessionSnapshot) {
        _uiState.update { state ->
            val nextState = state.copy(
                displayName = snapshot.displayName,
                householdName = snapshot.householdName,
                lastUsedHousehold = snapshot.householdName.ifBlank { "No household yet" },
            )
            snapshot.serviceErrorMessage?.let { message ->
                nextState.copy(
                    googleSignInAvailable = false,
                    errorMessage = message,
                )
            } ?: nextState
        }
    }

    fun onEvent(event: AuthUiEvent) {
        when (event) {
            AuthUiEvent.TriggerVibration -> vibratorUtil.triggerVibration()
            AuthUiEvent.TriggerVibrationSuccess -> vibratorUtil.triggerVibrationSuccess()
            is AuthUiEvent.DisplayNameChanged -> _uiState.update { it.copy(displayName = event.value) }
            is AuthUiEvent.EmailChanged -> _uiState.update { it.copy(email = event.value) }
            is AuthUiEvent.PasswordChanged -> _uiState.update { it.copy(password = event.value) }
            is AuthUiEvent.HouseholdNameChanged -> _uiState.update { it.copy(householdName = event.value) }
            is AuthUiEvent.HouseholdSizeSelected -> _uiState.update { it.copy(selectedHouseholdSize = event.value) }
            is AuthUiEvent.GoogleSignInAvailabilityChanged -> _uiState.update { it.copy(googleSignInAvailable = event.value) }
            is AuthUiEvent.GoogleSignInFailed -> _uiState.update { it.copy(errorMessage = event.message) }
            is AuthUiEvent.InviteCodeChanged -> {
                val normalized = event.value.uppercase()
                _uiState.update {
                    it.copy(
                        inviteCode = normalized,
                        errorMessage = null,
                    )
                }
                previewInvite(normalized)
            }

            AuthUiEvent.ErrorDismissed -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    fun signIn(onSuccess: (AuthFlowResult) -> Unit) {
        viewModelScope.launch {
            submit {
                sessionRepository.signIn(
                    email = _uiState.value.email,
                    password = _uiState.value.password,
                    displayNameHint = _uiState.value.displayName,
                ).also {
                    vibratorUtil.triggerVibrationSuccess()
                    onSuccess(it)
                }
            }
        }
    }

    fun signInWithGoogle(
        token: GoogleSignInToken,
        onSuccess: (AuthFlowResult) -> Unit,
    ) {
        viewModelScope.launch {
            submit {
                sessionRepository.signInWithGoogle(
                    idToken = token.idToken,
                    accessToken = token.accessToken,
                    displayNameHint = token.displayName.ifBlank { _uiState.value.displayName },
                    emailHint = token.email,
                ).also {
                    vibratorUtil.triggerVibrationSuccess()
                    onSuccess(it)
                }
            }
        }
    }

    fun createHousehold(onSuccess: (AuthFlowResult) -> Unit) {
        viewModelScope.launch {
            submit {
                sessionRepository.createHousehold(
                    displayName = _uiState.value.displayName,
                    householdName = _uiState.value.householdName,
                    householdSizeLabel = _uiState.value.selectedHouseholdSize,
                ).also {
                    vibratorUtil.triggerVibrationSuccess()
                    onSuccess(it)
                }
            }
        }
    }

    fun joinHousehold(onSuccess: (AuthFlowResult) -> Unit) {
        viewModelScope.launch {
            submit {
                sessionRepository.joinHousehold(
                    displayName = _uiState.value.displayName,
                    inviteCode = _uiState.value.inviteCode,
                ).also {
                    vibratorUtil.triggerVibrationSuccess()
                    onSuccess(it)
                }
            }
        }
    }

    private fun previewInvite(code: String) {
        if (code.length < 4) {
            _uiState.update { it.copy(invitePreview = null, isInvitePreviewLoading = false) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isInvitePreviewLoading = true) }
            val preview = runCatching { sessionRepository.previewInvite(code) }.getOrNull()
            _uiState.update { it.copy(invitePreview = preview, isInvitePreviewLoading = false) }
        }
    }

    private suspend fun submit(
        block: suspend () -> Unit,
    ) {
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        runCatching { block() }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        errorMessage = error.message ?: "Something went wrong. Please try again.",
                    )
                }
            }
        _uiState.update { it.copy(isSubmitting = false) }
    }
}
