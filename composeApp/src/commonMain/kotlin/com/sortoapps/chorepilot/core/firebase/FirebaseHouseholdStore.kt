package com.sortoapps.chorepilot.core.firebase

import com.sortoapps.chorepilot.features.auth.domain.HouseholdRole
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.functions.functions
import kotlin.random.Random
import kotlin.time.Clock

class FirebaseHouseholdStore(
    private val firestore: FirebaseFirestore = Firebase.firestore,
) {
    private val functions = Firebase.functions

    fun userRef(uid: String) = firestore.collection(FirestorePaths.Users).document(uid)

    fun householdRef(householdId: String) = firestore.collection(FirestorePaths.Households).document(householdId)

    fun inviteRef(inviteCode: String) = firestore.collection(FirestorePaths.HouseholdInvites).document(inviteCode.uppercase())

    fun memberRef(
        householdId: String,
        userId: String,
    ) = householdRef(householdId).collection(FirestorePaths.Members).document(userId)

    suspend fun loadUser(uid: String): UserProfileDocument? = userRef(uid).get().decodeOrNull()

    suspend fun loadHousehold(householdId: String): HouseholdDocument? = householdRef(householdId).get().decodeOrNull()

    suspend fun loadMember(
        householdId: String,
        userId: String,
    ): HouseholdMemberDocument? = memberRef(householdId, userId).get().decodeOrNull()

    suspend fun loadInvite(inviteCode: String): HouseholdInviteDocument? = inviteRef(inviteCode).get().decodeOrNull()

    suspend fun listMembers(householdId: String): List<HouseholdMemberDocument> =
        householdRef(householdId)
            .collection(FirestorePaths.Members)
            .get()
            .documents
            .mapNotNull { it.decodeOrNull<HouseholdMemberDocument>() }

    suspend fun saveUser(profile: UserProfileDocument) {
        userRef(profile.uid).set(profile, merge = true)
    }

    suspend fun saveHousehold(household: HouseholdDocument) {
        householdRef(household.id).set(household, merge = true)
    }

    suspend fun syncPurchaserPremiumAcrossHouseholds(
        userId: String,
        entitlementActive: Boolean,
    ): List<HouseholdDocument> {
        if (userId.isBlank()) return emptyList()
        val profile = loadUser(userId) ?: return emptyList()
        return profile.householdIds
            .distinct()
            .mapNotNull { householdId ->
                updatePremiumPurchaser(
                    householdId = householdId,
                    userId = userId,
                    entitlementActive = entitlementActive,
                )
            }
    }

    suspend fun removePremiumPurchaserFromHousehold(
        householdId: String,
        userId: String,
    ): HouseholdDocument? {
        if (householdId.isBlank() || userId.isBlank()) return null
        return updatePremiumPurchaser(
            householdId = householdId,
            userId = userId,
            entitlementActive = false,
        )
    }

    suspend fun saveMember(
        householdId: String,
        member: HouseholdMemberDocument,
    ) {
        memberRef(householdId, member.userId).set(member, merge = true)
    }

    suspend fun createHousehold(
        ownerUserId: String,
        ownerDisplayName: String,
        householdName: String,
        sizeLabel: String,
    ): HouseholdDocument {
        val document = firestore.collection(FirestorePaths.Households).document
        val household = HouseholdDocument(
            id = document.id,
            name = householdName,
            sizeLabel = sizeLabel,
            ownerUserId = ownerUserId,
            memberCount = 1,
        )
        document.set(household)
        saveMember(
            householdId = household.id,
            member = HouseholdMemberDocument(
                userId = ownerUserId,
                displayName = ownerDisplayName,
                role = HouseholdRole.Owner,
            ),
        )
        return household
    }

    suspend fun createInvite(
        household: HouseholdDocument,
        role: HouseholdRole = HouseholdRole.Member,
        allowAnonymous: Boolean = true,
    ): HouseholdInviteDocument {
        deactivateActiveInvite(household, HouseholdInviteLifecycleStatus.Replaced)
        val code = buildInviteCode()
        val createdAtEpochMs = Clock.System.now().toEpochMilliseconds()
        val invite = HouseholdInviteDocument(
            code = code,
            householdId = household.id,
            householdName = household.name,
            role = role,
            allowAnonymous = allowAnonymous,
            active = true,
            createdAtEpochMs = createdAtEpochMs,
            expiresAtEpochMs = buildInviteExpiryEpochMs(createdAtEpochMs),
            lifecycleStatus = HouseholdInviteLifecycleStatus.Active.name,
        )
        inviteRef(code).set(invite)
        saveHousehold(household.copy(activeInviteCode = code, lastInviteCode = code))
        return invite
    }

    suspend fun attachMemberToHousehold(
        household: HouseholdDocument,
        userId: String,
        displayName: String,
        role: HouseholdRole,
    ): HouseholdDocument {
        val existingMember = loadMember(household.id, userId)
        saveMember(
            householdId = household.id,
            member = HouseholdMemberDocument(
                userId = userId,
                displayName = displayName,
                role = role,
            ),
        )
        val nextMemberCount = if (existingMember == null) household.memberCount + 1 else household.memberCount
        val updatedHousehold = household.copy(memberCount = nextMemberCount)
        saveHousehold(updatedHousehold)
        return updatedHousehold
    }

    suspend fun cancelActiveInvite(
        household: HouseholdDocument,
    ): HouseholdDocument {
        deactivateActiveInvite(household, HouseholdInviteLifecycleStatus.Revoked)
        val updatedHousehold = household.copy(
            activeInviteCode = "",
            lastInviteCode = household.activeInviteCode.ifBlank { household.lastInviteCode },
        )
        saveHousehold(updatedHousehold)
        return updatedHousehold
    }

    suspend fun transferOwnership(
        household: HouseholdDocument,
        newOwnerId: String,
    ): HouseholdDocument {
        val currentOwnerId = household.ownerUserId
        if (newOwnerId == currentOwnerId) return household
        val currentOwner = loadMember(household.id, currentOwnerId)
        val nextOwner = loadMember(household.id, newOwnerId)
            ?: error("We couldn't find that member in the household anymore.")
        currentOwner
            ?.takeIf { it.active }
            ?.let {
                saveMember(
                    householdId = household.id,
                    member = it.copy(role = HouseholdRole.Admin),
                )
            }
        saveMember(
            householdId = household.id,
            member = nextOwner.copy(role = HouseholdRole.Owner, active = true),
        )
        val updatedHousehold = household.copy(ownerUserId = newOwnerId)
        saveHousehold(updatedHousehold)
        return updatedHousehold
    }

    suspend fun deactivateMember(
        household: HouseholdDocument,
        memberId: String,
    ): HouseholdDocument {
        val member = loadMember(household.id, memberId)
            ?: error("We couldn't find that member right now.")
        if (!member.active) return household
        saveMember(
            householdId = household.id,
            member = member.copy(active = false),
        )
        val updatedHousehold = household
            .copy(memberCount = (household.memberCount - 1).coerceAtLeast(0))
            .withPremiumPurchaser(
                userId = memberId,
                entitlementActive = false,
            )
        saveHousehold(updatedHousehold)
        return updatedHousehold
    }

    suspend fun removeMember(
        household: HouseholdDocument,
        memberId: String,
    ): HouseholdDocument {
        val member = loadMember(household.id, memberId)
        val updatedHousehold = household
            .copy(
                memberCount = if (member?.active == true) {
                    (household.memberCount - 1).coerceAtLeast(0)
                } else {
                    household.memberCount
                },
            )
            .withPremiumPurchaser(
                userId = memberId,
                entitlementActive = false,
            )
        saveHousehold(updatedHousehold)
        memberRef(household.id, memberId).delete()
        return updatedHousehold
    }

    suspend fun removeHouseholdFromUser(
        userId: String,
        householdId: String,
    ): UserProfileDocument? {
        val existing = loadUser(userId) ?: return null
        val remainingHouseholds = existing.householdIds.filterNot { it == householdId }
        val updated = existing.copy(
            householdIds = remainingHouseholds,
            activeHouseholdId = if (existing.activeHouseholdId == householdId) {
                remainingHouseholds.firstOrNull().orEmpty()
            } else {
                existing.activeHouseholdId
            },
        )
        saveUser(updated)
        return updated
    }

    suspend fun deleteHouseholdCascade(
        householdId: String,
    ) {
        functions
            .httpsCallable("deleteHouseholdCascade")
            .invoke(mapOf("householdId" to householdId))
    }

    suspend fun markInviteUsed(invite: HouseholdInviteDocument) {
        val nextUsageCount = invite.usageCount + 1
        val shouldRemainActive = invite.usageLimit == 0 || nextUsageCount < invite.usageLimit
        inviteRef(invite.code).set(
            invite.copy(
                usageCount = nextUsageCount,
                active = shouldRemainActive,
                lifecycleStatus = if (shouldRemainActive) {
                    HouseholdInviteLifecycleStatus.Active.name
                } else {
                    HouseholdInviteLifecycleStatus.Exhausted.name
                },
            ),
            merge = true,
        )
    }

    private suspend fun deactivateActiveInvite(
        household: HouseholdDocument,
        lifecycleStatus: HouseholdInviteLifecycleStatus,
    ) {
        household.activeInviteCode
            .takeIf(String::isNotBlank)
            ?.let { previousCode ->
                loadInvite(previousCode)
                    ?.takeIf { it.active }
                    ?.let { previousInvite ->
                        inviteRef(previousCode).set(
                            previousInvite.copy(
                                active = false,
                                lifecycleStatus = lifecycleStatus.name,
                            ),
                            merge = true,
                        )
                    }
            }
    }

    private suspend fun updatePremiumPurchaser(
        householdId: String,
        userId: String,
        entitlementActive: Boolean,
    ): HouseholdDocument? {
        val household = loadHousehold(householdId) ?: return null
        val updatedHousehold = household.withPremiumPurchaser(
            userId = userId,
            entitlementActive = entitlementActive,
        )
        saveHousehold(updatedHousehold)
        return updatedHousehold
    }

    private fun buildInviteCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val generatedId = firestore.collection(FirestorePaths.HouseholdInvites).document.id
        val normalized = buildString {
            generatedId.uppercase().forEach { char ->
                val mapped = when (char) {
                    '0', 'O' -> '8'
                    '1', 'I', 'L' -> '9'
                    else -> char
                }
                if (mapped in alphabet) {
                    append(mapped)
                }
            }
        }

        val codeBody = if (normalized.length >= 9) {
            normalized.take(9)
        } else {
            normalized + buildString(9 - normalized.length) {
                repeat(9 - normalized.length) {
                    append(alphabet[Random.nextInt(alphabet.length)])
                }
            }
        }

        return "${codeBody.take(5)}-${codeBody.drop(5)}"
    }
}

internal fun HouseholdDocument.withPremiumPurchaser(
    userId: String,
    entitlementActive: Boolean,
    syncedAtEpochMs: Long = Clock.System.now().toEpochMilliseconds(),
): HouseholdDocument {
    if (userId.isBlank()) return this
    val nextPurchaserIds = if (entitlementActive) {
        (premiumPurchaserIds + userId).filter(String::isNotBlank).distinct().sorted()
    } else {
        premiumPurchaserIds.filterNot { it == userId }.distinct().sorted()
    }
    return copy(
        hasChorePilotPro = nextPurchaserIds.isNotEmpty(),
        premiumPurchaserIds = nextPurchaserIds,
        premiumLastSyncedAtEpochMs = syncedAtEpochMs,
    )
}

private inline fun <reified T> DocumentSnapshot.decodeOrNull(): T? = if (exists) data<T>() else null
