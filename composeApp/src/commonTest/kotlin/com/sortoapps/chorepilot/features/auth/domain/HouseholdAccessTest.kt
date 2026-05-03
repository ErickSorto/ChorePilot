package com.sortoapps.chorepilot.features.auth.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HouseholdAccessTest {
    @Test
    fun memberRouteResolutionFallsBackToViewer() {
        assertEquals(
            "member-1",
            HouseholdRole.Member.resolveAccessibleFamilyMemberId(
                viewerId = "member-1",
                requestedMemberId = "owner-1",
            ),
        )
        assertTrue(!HouseholdRole.Member.canManageFamilyInviteFlow())
        assertTrue(!HouseholdRole.Member.canAccessFamilyMember("member-1", "owner-1"))
    }

    @Test
    fun adminRouteResolutionKeepsRequestedTarget() {
        assertEquals(
            "owner-1",
            HouseholdRole.Admin.resolveAccessibleFamilyMemberId(
                viewerId = "admin-1",
                requestedMemberId = "owner-1",
            ),
        )
        assertTrue(HouseholdRole.Admin.canManageFamilyInviteFlow())
        assertTrue(HouseholdRole.Admin.canAccessFamilyMember("admin-1", "owner-1"))
    }
}
