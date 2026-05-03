package com.sortoapps.chorepilot.core.firebase

import kotlin.time.Clock

private const val HOUSEHOLD_INVITE_EXPIRY_WINDOW_MS = 7L * 24L * 60L * 60L * 1000L

enum class HouseholdInviteLifecycleStatus {
    Active,
    Revoked,
    Replaced,
    Expired,
    Exhausted,
}

data class ResolvedHouseholdInvite(
    val document: HouseholdInviteDocument,
    val lifecycleStatus: HouseholdInviteLifecycleStatus,
    val isJoinable: Boolean,
)

fun buildInviteExpiryEpochMs(createdAtEpochMs: Long): Long = createdAtEpochMs + HOUSEHOLD_INVITE_EXPIRY_WINDOW_MS

fun HouseholdInviteDocument.resolveInviteLifecycle(
    nowEpochMs: Long = Clock.System.now().toEpochMilliseconds(),
): ResolvedHouseholdInvite {
    val storedStatus = lifecycleStatus.toInviteLifecycleStatusOrNull()
    val resolvedStatus = when {
        storedStatus == HouseholdInviteLifecycleStatus.Revoked -> HouseholdInviteLifecycleStatus.Revoked
        storedStatus == HouseholdInviteLifecycleStatus.Replaced -> HouseholdInviteLifecycleStatus.Replaced
        expiresAtEpochMs > 0L && nowEpochMs >= expiresAtEpochMs -> HouseholdInviteLifecycleStatus.Expired
        usageLimit > 0 && usageCount >= usageLimit -> HouseholdInviteLifecycleStatus.Exhausted
        active -> HouseholdInviteLifecycleStatus.Active
        storedStatus == HouseholdInviteLifecycleStatus.Expired -> HouseholdInviteLifecycleStatus.Expired
        storedStatus == HouseholdInviteLifecycleStatus.Exhausted -> HouseholdInviteLifecycleStatus.Exhausted
        else -> HouseholdInviteLifecycleStatus.Replaced
    }
    return ResolvedHouseholdInvite(
        document = this,
        lifecycleStatus = resolvedStatus,
        isJoinable = resolvedStatus == HouseholdInviteLifecycleStatus.Active,
    )
}

fun HouseholdInviteLifecycleStatus.joinFailureMessage(): String = when (this) {
    HouseholdInviteLifecycleStatus.Active -> "That invite is still valid."
    HouseholdInviteLifecycleStatus.Revoked -> "That invite was revoked."
    HouseholdInviteLifecycleStatus.Replaced -> "That code was replaced with a newer invite."
    HouseholdInviteLifecycleStatus.Expired -> "That invite expired."
    HouseholdInviteLifecycleStatus.Exhausted -> "That invite has already been used up."
}

fun String.toInviteLifecycleStatusOrNull(): HouseholdInviteLifecycleStatus? = when (this) {
    HouseholdInviteLifecycleStatus.Active.name -> HouseholdInviteLifecycleStatus.Active
    HouseholdInviteLifecycleStatus.Revoked.name -> HouseholdInviteLifecycleStatus.Revoked
    HouseholdInviteLifecycleStatus.Replaced.name -> HouseholdInviteLifecycleStatus.Replaced
    HouseholdInviteLifecycleStatus.Expired.name -> HouseholdInviteLifecycleStatus.Expired
    HouseholdInviteLifecycleStatus.Exhausted.name -> HouseholdInviteLifecycleStatus.Exhausted
    else -> null
}
