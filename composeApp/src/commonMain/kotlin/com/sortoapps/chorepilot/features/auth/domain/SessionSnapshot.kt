package com.sortoapps.chorepilot.features.auth.domain

import kotlinx.serialization.Serializable

@Serializable
data class SessionSnapshot(
    val onboardingCompleted: Boolean = false,
    val signedIn: Boolean = false,
    val userId: String = "",
    val email: String = "",
    val isAnonymous: Boolean = false,
    val displayName: String = "Erick",
    val householdId: String = "",
    val householdName: String = "River House",
    val householdSizeLabel: String = "4 to 5 members",
    val availableHouseholds: List<SessionHouseholdOption> = emptyList(),
    val householdHasChorePilotPro: Boolean = false,
    val role: HouseholdRole = HouseholdRole.Member,
    val needsHouseholdSetup: Boolean = false,
    val needsOnboardingFlow: Boolean = false,
    val needsInitialPremiumFlow: Boolean = false,
    val serviceErrorMessage: String? = null,
)
