package com.sortoapps.chorepilot.features.auth.presentation.viewmodel

import com.sortoapps.chorepilot.features.auth.domain.InvitePreview

data class AuthUiState(
    val displayName: String = "Erick",
    val email: String = "",
    val password: String = "",
    val householdName: String = "",
    val inviteCode: String = "",
    val lastUsedHousehold: String = "No household yet",
    val selectedHouseholdSize: String = "4 to 5 members",
    val householdSizeOptions: List<String> = listOf("3 members to start", "4 to 5 members", "6 or more members"),
    val invitePreview: InvitePreview? = null,
    val isInvitePreviewLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val googleSignInAvailable: Boolean = true,
    val errorMessage: String? = null,
)
