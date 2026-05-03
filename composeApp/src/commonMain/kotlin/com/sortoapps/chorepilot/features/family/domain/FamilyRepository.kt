package com.sortoapps.chorepilot.features.family.domain

import com.sortoapps.chorepilot.core.demo.DemoMetric
import com.sortoapps.chorepilot.core.demo.DemoTone
import com.sortoapps.chorepilot.core.firebase.HouseholdInviteLifecycleStatus
import com.sortoapps.chorepilot.features.auth.domain.HouseholdRole
import kotlinx.coroutines.flow.StateFlow

enum class FamilyAvailabilityMode {
    DayOff,
    GoneMode,
}

data class FamilyAvailabilitySnapshot(
    val mode: FamilyAvailabilityMode,
    val startDate: String,
    val endDate: String,
    val statusLabel: String,
    val summary: String,
)

data class FamilyMemberSnapshot(
    val id: String,
    val name: String,
    val role: HouseholdRole,
    val statusLabel: String,
    val activeChoresLabel: String,
    val householdImpact: String,
    val avatarSeed: String,
    val tone: DemoTone,
    val assignedChoreCount: Int,
    val proofReviewCount: Int,
    val isActiveSeat: Boolean = true,
    val availability: FamilyAvailabilitySnapshot? = null,
)

data class FamilyInviteSnapshot(
    val code: String = "",
    val role: HouseholdRole = HouseholdRole.Member,
    val lifecycleStatus: HouseholdInviteLifecycleStatus? = null,
    val statusLabel: String = "No active invite",
    val supportingText: String = "Create a fresh code when you're ready to add someone.",
    val generatedAtLabel: String = "",
    val expiryLabel: String = "",
    val usageLabel: String = "",
    val shareText: String = "",
    val isJoinable: Boolean = false,
    val seatImpactText: String = "This will use the next open household seat.",
)

data class FamilySnapshot(
    val householdName: String = "",
    val pulseLabel: String = "",
    val householdBadge: String = "",
    val ownerUserId: String = "",
    val activeSeatCount: Int = 0,
    val summaryMetrics: List<DemoMetric> = emptyList(),
    val members: List<FamilyMemberSnapshot> = emptyList(),
    val activeInvite: FamilyInviteSnapshot = FamilyInviteSnapshot(),
)

data class FamilyRepositoryState(
    val isLoading: Boolean = true,
    val snapshot: FamilySnapshot = FamilySnapshot(),
    val errorMessage: String? = null,
)

interface FamilyRepository {
    val familyState: StateFlow<FamilyRepositoryState>

    suspend fun refresh()

    suspend fun generateInvite(
        role: HouseholdRole = HouseholdRole.Member,
    ): FamilyInviteSnapshot

    suspend fun cancelInvite()

    suspend fun rotateInvite(
        role: HouseholdRole = HouseholdRole.Member,
    ): FamilyInviteSnapshot

    suspend fun changeMemberRole(
        memberId: String,
        newRole: HouseholdRole,
    )

    suspend fun deactivateMember(memberId: String)

    suspend fun removeMember(memberId: String)

    suspend fun leaveHousehold()

    suspend fun deleteHousehold()

    suspend fun updateAvailability(
        memberId: String,
        mode: FamilyAvailabilityMode,
        startDate: String,
        endDate: String,
    )

    suspend fun clearAvailability(memberId: String)
}
