package com.sortoapps.chorepilot.core.firebase

import com.sortoapps.chorepilot.features.auth.domain.HouseholdRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InviteLifecycleTest {
    @Test
    fun newInviteStaysJoinableBeforeExpiry() {
        val createdAt = 1_000L
        val invite = HouseholdInviteDocument(
            code = "RIVER-82",
            householdId = "house-1",
            householdName = "River House",
            role = HouseholdRole.Member,
            active = true,
            createdAtEpochMs = createdAt,
            expiresAtEpochMs = buildInviteExpiryEpochMs(createdAt),
            lifecycleStatus = HouseholdInviteLifecycleStatus.Active.name,
        )

        val resolved = invite.resolveInviteLifecycle(nowEpochMs = createdAt + 60_000L)

        assertEquals(HouseholdInviteLifecycleStatus.Active, resolved.lifecycleStatus)
        assertTrue(resolved.isJoinable)
    }

    @Test
    fun expiredInviteWinsEvenIfLegacyActiveFlagIsStillTrue() {
        val createdAt = 1_000L
        val invite = HouseholdInviteDocument(
            code = "RIVER-82",
            householdId = "house-1",
            householdName = "River House",
            active = true,
            createdAtEpochMs = createdAt,
            expiresAtEpochMs = buildInviteExpiryEpochMs(createdAt),
        )

        val resolved = invite.resolveInviteLifecycle(nowEpochMs = buildInviteExpiryEpochMs(createdAt) + 1L)

        assertEquals(HouseholdInviteLifecycleStatus.Expired, resolved.lifecycleStatus)
        assertFalse(resolved.isJoinable)
    }

    @Test
    fun usageLimitResolvesToExhausted() {
        val invite = HouseholdInviteDocument(
            code = "RIVER-82",
            householdId = "house-1",
            householdName = "River House",
            active = false,
            usageCount = 1,
            usageLimit = 1,
        )

        val resolved = invite.resolveInviteLifecycle(nowEpochMs = 10_000L)

        assertEquals(HouseholdInviteLifecycleStatus.Exhausted, resolved.lifecycleStatus)
        assertFalse(resolved.isJoinable)
    }

    @Test
    fun revokedAndReplacedStatusesRemainDistinct() {
        val revoked = HouseholdInviteDocument(
            code = "OLD-111",
            householdId = "house-1",
            householdName = "River House",
            active = false,
            lifecycleStatus = HouseholdInviteLifecycleStatus.Revoked.name,
        )
        val replaced = revoked.copy(
            code = "OLD-222",
            lifecycleStatus = HouseholdInviteLifecycleStatus.Replaced.name,
        )

        assertEquals(HouseholdInviteLifecycleStatus.Revoked, revoked.resolveInviteLifecycle().lifecycleStatus)
        assertEquals(HouseholdInviteLifecycleStatus.Replaced, replaced.resolveInviteLifecycle().lifecycleStatus)
    }

    @Test
    fun legacyInactiveInviteFallsBackToReplaced() {
        val invite = HouseholdInviteDocument(
            code = "OLD-333",
            householdId = "house-1",
            householdName = "River House",
            active = false,
        )

        val resolved = invite.resolveInviteLifecycle(nowEpochMs = 10_000L)

        assertEquals(HouseholdInviteLifecycleStatus.Replaced, resolved.lifecycleStatus)
        assertFalse(resolved.isJoinable)
    }

    @Test
    fun joinFailureMessagesMatchLifecycleStatus() {
        assertEquals("That invite was revoked.", HouseholdInviteLifecycleStatus.Revoked.joinFailureMessage())
        assertEquals("That code was replaced with a newer invite.", HouseholdInviteLifecycleStatus.Replaced.joinFailureMessage())
        assertEquals("That invite expired.", HouseholdInviteLifecycleStatus.Expired.joinFailureMessage())
        assertEquals("That invite has already been used up.", HouseholdInviteLifecycleStatus.Exhausted.joinFailureMessage())
    }
}
