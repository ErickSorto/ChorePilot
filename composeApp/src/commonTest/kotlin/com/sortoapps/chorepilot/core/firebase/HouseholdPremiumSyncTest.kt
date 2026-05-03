package com.sortoapps.chorepilot.core.firebase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HouseholdPremiumSyncTest {
    @Test
    fun activePurchaserAddsSharedHouseholdAccess() {
        val updated = HouseholdDocument(id = "house-1")
            .withPremiumPurchaser(
                userId = "owner-1",
                entitlementActive = true,
                syncedAtEpochMs = 123L,
            )

        assertTrue(updated.hasChorePilotPro)
        assertEquals(listOf("owner-1"), updated.premiumPurchaserIds)
        assertEquals(123L, updated.premiumLastSyncedAtEpochMs)
    }

    @Test
    fun removingOnePurchaserKeepsHouseholdProWhileAnotherRemains() {
        val updated = HouseholdDocument(
            id = "house-1",
            hasChorePilotPro = true,
            premiumPurchaserIds = listOf("owner-1", "member-2"),
        ).withPremiumPurchaser(
            userId = "owner-1",
            entitlementActive = false,
            syncedAtEpochMs = 456L,
        )

        assertTrue(updated.hasChorePilotPro)
        assertEquals(listOf("member-2"), updated.premiumPurchaserIds)
        assertEquals(456L, updated.premiumLastSyncedAtEpochMs)
    }

    @Test
    fun removingLastPurchaserTurnsHouseholdProOff() {
        val updated = HouseholdDocument(
            id = "house-1",
            hasChorePilotPro = true,
            premiumPurchaserIds = listOf("owner-1"),
        ).withPremiumPurchaser(
            userId = "owner-1",
            entitlementActive = false,
            syncedAtEpochMs = 789L,
        )

        assertFalse(updated.hasChorePilotPro)
        assertTrue(updated.premiumPurchaserIds.isEmpty())
        assertEquals(789L, updated.premiumLastSyncedAtEpochMs)
    }
}
