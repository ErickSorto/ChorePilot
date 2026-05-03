package com.sortoapps.chorepilot.features.auth.domain

import kotlinx.serialization.Serializable

@Serializable
enum class HouseholdRole {
    Owner,
    Admin,
    Member,
    Guest,
}
