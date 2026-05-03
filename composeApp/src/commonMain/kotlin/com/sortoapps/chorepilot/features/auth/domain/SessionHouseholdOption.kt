package com.sortoapps.chorepilot.features.auth.domain

import kotlinx.serialization.Serializable

@Serializable
data class SessionHouseholdOption(
    val id: String = "",
    val name: String = "",
    val role: HouseholdRole = HouseholdRole.Member,
    val hasChorePilotPro: Boolean = false,
)
