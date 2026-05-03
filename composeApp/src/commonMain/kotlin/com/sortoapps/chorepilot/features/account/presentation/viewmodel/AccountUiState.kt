package com.sortoapps.chorepilot.features.account.presentation.viewmodel

data class AccountUiState(
    val isLoading: Boolean = true,
    val displayName: String = "Your account",
    val email: String = "",
    val signedInMethodLabel: String = "Google",
    val signedInMethodSummary: String = "Recoverable Google sign-in keeps this household tied to a real account.",
    val householdName: String = "Your household",
    val householdRoleLabel: String = "Member",
    val seatLabel: String = "Active seat",
    val seatContextLabel: String = "",
    val accessSummary: String = "Join or create a household to unlock shared chores, reports, and rewards.",
    val premiumStatusLabel: String = "Free household",
    val premiumStatusSummary: String = "Household premium access has not been enabled yet.",
    val signOutSummary: String = "Signing out on this device keeps your household data and purchases intact.",
)

