package com.sortoapps.chorepilot.core.revenuecat

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RevenueCatAccessTest {
    @Test
    fun purchaserAccessUnlocksPremiumEvenBeforeHouseholdRefreshCompletes() {
        assertTrue(
            purchaserOrHouseholdHasPro(
                purchaserHasPro = true,
                householdHasPro = false,
            ),
        )
    }

    @Test
    fun noPurchaserOrHouseholdAccessKeepsPremiumLocked() {
        assertFalse(
            purchaserOrHouseholdHasPro(
                purchaserHasPro = false,
                householdHasPro = false,
            ),
        )
    }
}
