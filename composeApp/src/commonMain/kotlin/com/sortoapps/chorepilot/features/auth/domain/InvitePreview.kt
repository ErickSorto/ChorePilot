package com.sortoapps.chorepilot.features.auth.domain

data class InvitePreview(
    val householdId: String,
    val householdName: String,
    val role: HouseholdRole,
    val memberCountLabel: String,
)
