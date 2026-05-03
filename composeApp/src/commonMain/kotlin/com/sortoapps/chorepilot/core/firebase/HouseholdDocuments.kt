package com.sortoapps.chorepilot.core.firebase

import com.sortoapps.chorepilot.features.auth.domain.HouseholdRole
import kotlinx.serialization.Serializable

@Serializable
enum class HouseholdAvailabilityMode {
    Available,
    DayOff,
    GoneMode,
}

@Serializable
data class UserProfileDocument(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val activeHouseholdId: String = "",
    val householdIds: List<String> = emptyList(),
    val onboardingCompleted: Boolean = false,
    val isAnonymous: Boolean = false,
    val pendingPremiumAfterHouseholdCreation: Boolean = false,
)

@Serializable
data class HouseholdDocument(
    val id: String = "",
    val name: String = "",
    val sizeLabel: String = "",
    val ownerUserId: String = "",
    val memberCount: Int = 1,
    val activeInviteCode: String = "",
    val lastInviteCode: String = "",
    val hasChorePilotPro: Boolean = false,
    val premiumPurchaserIds: List<String> = emptyList(),
    val premiumLastSyncedAtEpochMs: Long = 0L,
)

@Serializable
data class HouseholdMemberDocument(
    val userId: String = "",
    val displayName: String = "",
    val role: HouseholdRole = HouseholdRole.Member,
    val active: Boolean = true,
    val weeklyEffortScore: Int = 0,
    val lastAssignedCycleKey: String = "",
    val availabilityMode: HouseholdAvailabilityMode = HouseholdAvailabilityMode.Available,
    val availabilityStartDate: String = "",
    val availabilityEndDate: String = "",
    val availabilityStatusLabel: String = "",
    val availabilitySummary: String = "",
)

@Serializable
data class HouseholdInviteDocument(
    val code: String = "",
    val householdId: String = "",
    val householdName: String = "",
    val role: HouseholdRole = HouseholdRole.Member,
    val allowAnonymous: Boolean = true,
    val active: Boolean = true,
    val usageCount: Int = 0,
    val usageLimit: Int = 0,
    val createdAtEpochMs: Long = 0L,
    val expiresAtEpochMs: Long = 0L,
    val lifecycleStatus: String = "",
)
