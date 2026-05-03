package com.sortoapps.chorepilot.features.auth.domain

fun HouseholdRole.hasHouseholdManagementAccess(): Boolean =
    this == HouseholdRole.Owner || this == HouseholdRole.Admin

fun HouseholdRole.canManageFamilyInviteFlow(): Boolean = hasHouseholdManagementAccess()

fun HouseholdRole.canAccessFamilyMember(
    viewerId: String,
    memberId: String,
): Boolean = hasHouseholdManagementAccess() || memberId.equals(viewerId, ignoreCase = true)

fun HouseholdRole.resolveAccessibleFamilyMemberId(
    viewerId: String,
    requestedMemberId: String?,
): String? = when {
    hasHouseholdManagementAccess() -> requestedMemberId
    viewerId.isNotBlank() -> viewerId
    else -> requestedMemberId
}

fun HouseholdRole.canManageFamilyAvailabilityFor(
    viewerId: String,
    memberId: String,
): Boolean = hasHouseholdManagementAccess() || memberId.equals(viewerId, ignoreCase = true)
