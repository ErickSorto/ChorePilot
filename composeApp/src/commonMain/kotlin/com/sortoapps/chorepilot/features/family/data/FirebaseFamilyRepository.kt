package com.sortoapps.chorepilot.features.family.data

import com.sortoapps.chorepilot.core.demo.DemoMetric
import com.sortoapps.chorepilot.core.demo.DemoTone
import com.sortoapps.chorepilot.core.firebase.FirebaseHouseholdStore
import com.sortoapps.chorepilot.core.firebase.HouseholdAvailabilityMode
import com.sortoapps.chorepilot.core.firebase.HouseholdDocument
import com.sortoapps.chorepilot.core.firebase.HouseholdInviteDocument
import com.sortoapps.chorepilot.core.firebase.HouseholdInviteLifecycleStatus
import com.sortoapps.chorepilot.core.firebase.HouseholdMemberDocument
import com.sortoapps.chorepilot.core.firebase.ResolvedHouseholdInvite
import com.sortoapps.chorepilot.core.firebase.resolveInviteLifecycle
import com.sortoapps.chorepilot.features.auth.domain.HouseholdRole
import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import com.sortoapps.chorepilot.features.auth.domain.SessionSnapshot
import com.sortoapps.chorepilot.features.chores.domain.ChoreItem
import com.sortoapps.chorepilot.features.chores.domain.ChoreItemStatus
import com.sortoapps.chorepilot.features.chores.domain.ChoreRepository
import com.sortoapps.chorepilot.features.family.domain.FamilyAvailabilityMode
import com.sortoapps.chorepilot.features.family.domain.FamilyAvailabilitySnapshot
import com.sortoapps.chorepilot.features.family.domain.FamilyInviteSnapshot
import com.sortoapps.chorepilot.features.family.domain.FamilyMemberSnapshot
import com.sortoapps.chorepilot.features.family.domain.FamilyRepository
import com.sortoapps.chorepilot.features.family.domain.FamilyRepositoryState
import com.sortoapps.chorepilot.features.family.domain.FamilySnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock

class FirebaseFamilyRepository(
    private val sessionRepository: SessionRepository,
    private val householdStore: FirebaseHouseholdStore,
    private val choreRepository: ChoreRepository,
    appScope: CoroutineScope,
) : FamilyRepository {
    private val _state = MutableStateFlow(FamilyRepositoryState())
    override val familyState: StateFlow<FamilyRepositoryState> = _state.asStateFlow()

    init {
        appScope.launch {
            combine(sessionRepository.session, choreRepository.chores) { session, chores -> session to chores }
                .collectLatest { (session, chores) ->
                    publishSnapshot(
                        session = session,
                        chores = chores,
                        keepLoading = _state.value.isLoading,
                        errorMessage = _state.value.errorMessage,
                    )
                }
        }
    }

    override suspend fun refresh() {
        _state.value = _state.value.copy(isLoading = true, errorMessage = null)
        runCatching {
            sessionRepository.refreshSession()
            choreRepository.refresh()
            publishSnapshot(
                session = sessionRepository.session.value,
                chores = choreRepository.chores.value,
                keepLoading = false,
                errorMessage = null,
            )
        }.onFailure { error ->
            _state.value = _state.value.copy(
                isLoading = false,
                errorMessage = error.message ?: "We couldn't load your household right now.",
            )
        }
    }

    override suspend fun generateInvite(role: HouseholdRole): FamilyInviteSnapshot {
        val context = requireAdminContext()
        val invite = householdStore.createInvite(context.household, role = role)
        publishSnapshot(
            session = sessionRepository.session.value,
            chores = choreRepository.chores.value,
            keepLoading = false,
            errorMessage = null,
        )
        return invite.toSnapshot(householdName = context.household.name)
    }

    override suspend fun cancelInvite() {
        val context = requireAdminContext()
        householdStore.cancelActiveInvite(context.household)
        publishSnapshot(
            session = sessionRepository.session.value,
            chores = choreRepository.chores.value,
            keepLoading = false,
            errorMessage = null,
        )
    }

    override suspend fun rotateInvite(role: HouseholdRole): FamilyInviteSnapshot {
        val context = requireAdminContext()
        val invite = householdStore.createInvite(context.household, role = role)
        publishSnapshot(
            session = sessionRepository.session.value,
            chores = choreRepository.chores.value,
            keepLoading = false,
            errorMessage = null,
        )
        return invite.toSnapshot(householdName = context.household.name)
    }

    override suspend fun changeMemberRole(
        memberId: String,
        newRole: HouseholdRole,
    ) {
        val context = requireAdminContext()
        val target = context.members.firstOrNull { it.userId == memberId }
            ?: error("We couldn't find that member anymore.")
        validateRoleChange(
            viewer = context.viewer,
            target = target,
            newRole = newRole,
            household = context.household,
        )
        if (newRole == HouseholdRole.Owner) {
            householdStore.transferOwnership(
                household = context.household,
                newOwnerId = target.userId,
            )
            sessionRepository.refreshSession()
        } else {
            householdStore.saveMember(
                householdId = context.household.id,
                member = target.copy(role = newRole),
            )
            if (target.userId == context.session.userId) {
                sessionRepository.refreshSession()
            }
        }
        publishSnapshot(
            session = sessionRepository.session.value,
            chores = choreRepository.chores.value,
            keepLoading = false,
            errorMessage = null,
        )
    }

    override suspend fun deactivateMember(memberId: String) {
        val context = requireAdminContext()
        val target = context.members.firstOrNull { it.userId == memberId }
            ?: error("We couldn't find that member anymore.")
        validateMemberManagement(
            viewer = context.viewer,
            target = target,
            forbidSelf = true,
        )
        householdStore.deactivateMember(
            household = context.household,
            memberId = memberId,
        )
        choreRepository.refresh()
        publishSnapshot(
            session = sessionRepository.session.value,
            chores = choreRepository.chores.value,
            keepLoading = false,
            errorMessage = null,
        )
    }

    override suspend fun removeMember(memberId: String) {
        val context = requireAdminContext()
        val target = context.members.firstOrNull { it.userId == memberId }
            ?: error("We couldn't find that member anymore.")
        validateMemberManagement(
            viewer = context.viewer,
            target = target,
            forbidSelf = true,
        )
        householdStore.removeMember(
            household = context.household,
            memberId = memberId,
        )
        householdStore.removeHouseholdFromUser(
            userId = memberId,
            householdId = context.household.id,
        )
        choreRepository.refresh()
        publishSnapshot(
            session = sessionRepository.session.value,
            chores = choreRepository.chores.value,
            keepLoading = false,
            errorMessage = null,
        )
    }

    override suspend fun leaveHousehold() {
        val session = sessionRepository.session.value
        val householdId = session.householdId.takeIf(String::isNotBlank)
            ?: error("Join a household before leaving one.")
        val household = householdStore.loadHousehold(householdId)
            ?: error("We couldn't find your household right now.")
        val members = householdStore.listMembers(householdId)
        val member = householdStore.loadMember(householdId, session.userId)
            ?: error("We couldn't find your member profile right now.")
        if (member.role == HouseholdRole.Owner || household.ownerUserId == member.userId) {
            val otherActiveMembers = members.filter { it.active && it.userId != member.userId }
            if (otherActiveMembers.isNotEmpty()) {
                error("Transfer ownership before leaving the household.")
            }
            error("Delete the household instead of leaving it. You're the last active member.")
        }
        householdStore.removeMember(
            household = household,
            memberId = member.userId,
        )
        householdStore.removeHouseholdFromUser(
            userId = member.userId,
            householdId = householdId,
        )
        sessionRepository.refreshSession()
        choreRepository.refresh()
        publishSnapshot(
            session = sessionRepository.session.value,
            chores = choreRepository.chores.value,
            keepLoading = false,
            errorMessage = null,
        )
    }

    override suspend fun deleteHousehold() {
        val session = sessionRepository.session.value
        val householdId = session.householdId.takeIf(String::isNotBlank)
            ?: error("Join a household before deleting one.")
        val household = householdStore.loadHousehold(householdId)
            ?: error("We couldn't find your household right now.")
        val member = householdStore.loadMember(householdId, session.userId)
            ?: error("We couldn't verify your owner seat right now.")
        val activeMembers = householdStore.listMembers(householdId).filter { it.active }
        if (member.role != HouseholdRole.Owner || household.ownerUserId != member.userId) {
            error("Only the household owner can delete the household.")
        }
        if (activeMembers.any { it.userId != member.userId }) {
            error("Transfer ownership before leaving the household.")
        }
        householdStore.deleteHouseholdCascade(householdId)
        sessionRepository.refreshSession()
        choreRepository.refresh()
        publishSnapshot(
            session = sessionRepository.session.value,
            chores = choreRepository.chores.value,
            keepLoading = false,
            errorMessage = null,
        )
    }

    override suspend fun updateAvailability(
        memberId: String,
        mode: FamilyAvailabilityMode,
        startDate: String,
        endDate: String,
    ) {
        val session = sessionRepository.session.value
        val householdId = session.householdId.takeIf(String::isNotBlank)
            ?: return
        val current = householdStore.loadMember(householdId, memberId) ?: return
        val rangeLabel = buildRangeLabel(startDate = startDate, endDate = endDate)
        val statusLabel = when (mode) {
            FamilyAvailabilityMode.DayOff -> "Day off scheduled"
            FamilyAvailabilityMode.GoneMode -> "Gone mode active"
        }
        val summary = when (mode) {
            FamilyAvailabilityMode.DayOff -> "Away for $rangeLabel."
            FamilyAvailabilityMode.GoneMode -> "Away during $rangeLabel."
        }
        householdStore.saveMember(
            householdId = householdId,
            member = current.copy(
                availabilityMode = mode.toDocumentMode(),
                availabilityStartDate = startDate,
                availabilityEndDate = endDate,
                availabilityStatusLabel = statusLabel,
                availabilitySummary = summary,
            ),
        )
        choreRepository.refresh()
        publishSnapshot(
            session = session,
            chores = choreRepository.chores.value,
            keepLoading = false,
            errorMessage = null,
        )
    }

    override suspend fun clearAvailability(memberId: String) {
        val session = sessionRepository.session.value
        val householdId = session.householdId.takeIf(String::isNotBlank)
            ?: return
        val current = householdStore.loadMember(householdId, memberId) ?: return
        householdStore.saveMember(
            householdId = householdId,
            member = current.copy(
                availabilityMode = HouseholdAvailabilityMode.Available,
                availabilityStartDate = "",
                availabilityEndDate = "",
                availabilityStatusLabel = "",
                availabilitySummary = "",
            ),
        )
        choreRepository.refresh()
        publishSnapshot(
            session = session,
            chores = choreRepository.chores.value,
            keepLoading = false,
            errorMessage = null,
        )
    }

    private suspend fun requireAdminContext(): FamilyAdminContext {
        val session = sessionRepository.session.value
        val householdId = session.householdId.takeIf(String::isNotBlank)
            ?: error("Join a household before changing members.")
        val household = householdStore.loadHousehold(householdId)
            ?: error("We couldn't find your household right now.")
        val members = householdStore.listMembers(householdId)
        val viewer = members.firstOrNull { it.userId == session.userId && it.active }
            ?: error("We couldn't verify your household access right now.")
        if (viewer.role != HouseholdRole.Owner && viewer.role != HouseholdRole.Admin) {
            error("Only owners and admins can manage the household.")
        }
        return FamilyAdminContext(
            session = session,
            household = household,
            members = members,
            viewer = viewer,
        )
    }

    private fun validateRoleChange(
        viewer: HouseholdMemberDocument,
        target: HouseholdMemberDocument,
        newRole: HouseholdRole,
        household: HouseholdDocument,
    ) {
        if (!target.active) {
            error("Reactivate this member before changing their role.")
        }
        if (viewer.userId == target.userId && newRole != HouseholdRole.Owner) {
            error("Use leave household instead of changing your own admin role here.")
        }
        when (viewer.role) {
            HouseholdRole.Owner -> {
                if (target.userId == household.ownerUserId && newRole != HouseholdRole.Owner) {
                    error("Transfer ownership before changing the last owner.")
                }
            }

            HouseholdRole.Admin -> {
                if (target.role !in setOf(HouseholdRole.Member, HouseholdRole.Guest) ||
                    newRole !in setOf(HouseholdRole.Member, HouseholdRole.Guest)
                ) {
                    error("Admins can only switch members between member and guest.")
                }
            }

            HouseholdRole.Member,
            HouseholdRole.Guest,
            -> error("Only owners and admins can change roles.")
        }
    }

    private fun validateMemberManagement(
        viewer: HouseholdMemberDocument,
        target: HouseholdMemberDocument,
        forbidSelf: Boolean,
    ) {
        if (forbidSelf && viewer.userId == target.userId) {
            error("Use leave household if you want to remove yourself.")
        }
        when (viewer.role) {
            HouseholdRole.Owner -> {
                if (target.role == HouseholdRole.Owner) {
                    error("Transfer ownership before removing or deactivating the owner.")
                }
            }

            HouseholdRole.Admin -> {
                if (target.role !in setOf(HouseholdRole.Member, HouseholdRole.Guest)) {
                    error("Admins can only manage members and guests.")
                }
            }

            HouseholdRole.Member,
            HouseholdRole.Guest,
            -> error("Only owners and admins can manage members.")
        }
    }

    private suspend fun publishSnapshot(
        session: SessionSnapshot,
        chores: List<ChoreItem>,
        keepLoading: Boolean,
        errorMessage: String?,
    ) {
        val snapshot = buildSnapshot(session = session, chores = chores)
        _state.value = FamilyRepositoryState(
            isLoading = keepLoading,
            snapshot = snapshot,
            errorMessage = errorMessage,
        )
    }

    private suspend fun buildSnapshot(
        session: SessionSnapshot,
        chores: List<ChoreItem>,
    ): FamilySnapshot {
        if (session.householdId.isBlank()) {
            return emptySnapshot(session.householdName.ifBlank { "Your household" })
        }

        val household = runCatching { householdStore.loadHousehold(session.householdId) }.getOrNull()
        val members = runCatching { householdStore.listMembers(session.householdId) }.getOrDefault(emptyList())
        val activeMembers = members.filter { it.active }
        val orderedMembers = members.sortedWith(
            compareByDescending<HouseholdMemberDocument> { it.active }
                .thenBy { it.displayName.lowercase() },
        )
        val memberSnapshots = orderedMembers.mapIndexed { index, member ->
            member.toFamilyMemberSnapshot(
                index = index,
                chores = chores,
                allMembers = members,
            )
        }
        val inviteSnapshot = loadInviteSnapshot(household = household)

        return FamilySnapshot(
            householdName = household?.name.orEmpty().ifBlank { session.householdName.ifBlank { "Your household" } },
            pulseLabel = buildPulseLabel(memberSnapshots),
            householdBadge = buildHouseholdBadge(memberSnapshots),
            ownerUserId = household?.ownerUserId.orEmpty(),
            activeSeatCount = activeMembers.size,
            summaryMetrics = buildSummaryMetrics(
                household = household,
                members = memberSnapshots,
                invite = inviteSnapshot,
            ),
            members = memberSnapshots,
            activeInvite = inviteSnapshot,
        )
    }

    private suspend fun loadInviteSnapshot(
        household: HouseholdDocument?,
    ): FamilyInviteSnapshot {
        val inviteCode = household?.activeInviteCode
            ?.takeIf(String::isNotBlank)
            ?: household?.lastInviteCode.orEmpty()
        if (inviteCode.isBlank()) {
            return FamilyInviteSnapshot()
        }
        val invite = runCatching { householdStore.loadInvite(inviteCode) }.getOrNull()
        return invite?.toSnapshot(householdName = household?.name.orEmpty()) ?: FamilyInviteSnapshot()
    }

    private fun buildPulseLabel(
        members: List<FamilyMemberSnapshot>,
    ): String {
        val activeCount = members.count { it.isActiveSeat }
        return when {
            activeCount == 0 -> "No active members yet"
            activeCount == 1 -> "1 active seat"
            else -> "$activeCount active seats"
        }
    }

    private fun buildHouseholdBadge(
        members: List<FamilyMemberSnapshot>,
    ): String = when {
        members.none { it.isActiveSeat } -> "Starting fresh"
        members.any { it.isActiveSeat && it.availability != null } -> "Adaptive"
        members.count { it.isActiveSeat } >= 4 -> "Balanced"
        else -> "Growing"
    }

    private fun buildSummaryMetrics(
        household: HouseholdDocument?,
        members: List<FamilyMemberSnapshot>,
        invite: FamilyInviteSnapshot,
    ): List<DemoMetric> {
        val activeMembers = members.filter { it.isActiveSeat }
        val activeInviteLabel = when {
            household == null -> "None"
            invite.isJoinable -> "Active"
            invite.code.isNotBlank() -> "Past"
            else -> "None"
        }
        val awayCount = activeMembers.count { it.availability != null }
        val managers = activeMembers.count { it.role == HouseholdRole.Admin || it.role == HouseholdRole.Owner }.coerceAtLeast(1)
        return listOf(
            DemoMetric("${activeMembers.size}", "Seats", DemoTone.Primary),
            DemoMetric(activeInviteLabel, "Invite", DemoTone.Secondary),
            DemoMetric("$managers", "Managers", DemoTone.Warning),
            DemoMetric("$awayCount", "Away", DemoTone.Tertiary),
        )
    }

    private fun emptySnapshot(
        householdName: String,
    ): FamilySnapshot = FamilySnapshot(
        householdName = householdName,
        pulseLabel = "No active members yet",
        householdBadge = "Starting fresh",
        activeSeatCount = 0,
        summaryMetrics = listOf(
            DemoMetric("0", "Seats", DemoTone.Primary),
            DemoMetric("None", "Invite", DemoTone.Secondary),
            DemoMetric("1", "Managers", DemoTone.Warning),
            DemoMetric("0", "Away", DemoTone.Tertiary),
        ),
        members = emptyList(),
        activeInvite = FamilyInviteSnapshot(),
    )
}

private data class FamilyAdminContext(
    val session: SessionSnapshot,
    val household: HouseholdDocument,
    val members: List<HouseholdMemberDocument>,
    val viewer: HouseholdMemberDocument,
)

private fun HouseholdMemberDocument.toFamilyMemberSnapshot(
    index: Int,
    chores: List<ChoreItem>,
    allMembers: List<HouseholdMemberDocument>,
): FamilyMemberSnapshot {
    val tone = when (index % 4) {
        0 -> DemoTone.Primary
        1 -> DemoTone.Secondary
        2 -> DemoTone.Tertiary
        else -> DemoTone.Neutral
    }
    val assignedChores = chores.filter { chore ->
        chore.assignee.equals(displayName, ignoreCase = true) ||
            chore.assigneeId.equals(userId, ignoreCase = true)
    }
    val activeAssignedChores = assignedChores.count { it.status != ChoreItemStatus.Completed }
    val proofReviews = assignedChores.count { it.status == ChoreItemStatus.PendingProof }
    val availability = toAvailabilitySnapshot()
    val activeHouseholdMembers = allMembers.filter { it.active }
    val impact = when {
        !active -> if (activeAssignedChores > 0 || proofReviews > 0) {
            "${displayName.ifBlank { "This member" }} is inactive, but $activeAssignedChores chore${pluralize(activeAssignedChores)} and $proofReviews proof review${pluralize(proofReviews)} still point at this seat."
        } else {
            "${displayName.ifBlank { "This member" }} is outside the active household seats until invited back or reactivated."
        }

        availability == null && activeAssignedChores == 0 && proofReviews == 0 ->
            "No current chores or reviews need rebalancing for ${displayName.ifBlank { "this member" }}."

        availability != null -> {
            val fallbackName = activeHouseholdMembers
                .firstOrNull { it.userId != userId && it.active }
                ?.displayName
                ?.ifBlank { "another member" }
                ?: "the household"
            "$activeAssignedChores chore${pluralize(activeAssignedChores)} and $proofReviews proof review${pluralize(proofReviews)} may shift toward $fallbackName."
        }

        proofReviews > 0 -> "$proofReviews proof review${pluralize(proofReviews)} still need a quick household decision."
        else -> "$activeAssignedChores active chore${pluralize(activeAssignedChores)} are currently flowing through ${displayName.ifBlank { "this member" }}."
    }
    val statusLabel = if (!active) {
        "Inactive seat"
    } else {
        availability?.statusLabel ?: when (role) {
            HouseholdRole.Owner -> "Owner"
            HouseholdRole.Admin -> "Admin"
            HouseholdRole.Member -> if (activeAssignedChores > 0) "On track" else "Ready"
            HouseholdRole.Guest -> "Guest"
        }
    }
    val activeLabel = if (!active) {
        if (activeAssignedChores > 0 || proofReviews > 0) {
            "$activeAssignedChores chore${pluralize(activeAssignedChores)} and $proofReviews proof review${pluralize(proofReviews)} still reference this seat."
        } else {
            "No active chores are flowing through this seat right now."
        }
    } else {
        availability?.summary ?: when {
            activeAssignedChores > 0 && proofReviews > 0 -> "$activeAssignedChores chores in flow, $proofReviews waiting on proof."
            activeAssignedChores > 0 -> "$activeAssignedChores chore${pluralize(activeAssignedChores)} in flow right now."
            else -> "Ready for chores and approvals."
        }
    }
    return FamilyMemberSnapshot(
        id = userId,
        name = displayName.ifBlank { "Member" },
        role = role,
        statusLabel = statusLabel,
        activeChoresLabel = activeLabel,
        householdImpact = impact,
        avatarSeed = displayName.take(2).uppercase().ifBlank { "CP" },
        tone = tone,
        assignedChoreCount = activeAssignedChores,
        proofReviewCount = proofReviews,
        isActiveSeat = active,
        availability = availability,
    )
}

private fun HouseholdMemberDocument.toAvailabilitySnapshot(): FamilyAvailabilitySnapshot? {
    val mode = when (availabilityMode) {
        HouseholdAvailabilityMode.DayOff -> FamilyAvailabilityMode.DayOff
        HouseholdAvailabilityMode.GoneMode -> FamilyAvailabilityMode.GoneMode
        HouseholdAvailabilityMode.Available -> return null
    }
    return FamilyAvailabilitySnapshot(
        mode = mode,
        startDate = availabilityStartDate,
        endDate = availabilityEndDate.ifBlank { availabilityStartDate },
        statusLabel = availabilityStatusLabel.ifBlank {
            when (mode) {
                FamilyAvailabilityMode.DayOff -> "Day off scheduled"
                FamilyAvailabilityMode.GoneMode -> "Gone mode active"
            }
        },
        summary = availabilitySummary.ifBlank {
            buildRangeLabel(
                startDate = availabilityStartDate,
                endDate = availabilityEndDate.ifBlank { availabilityStartDate },
            )
        },
    )
}

private fun HouseholdInviteDocument.toSnapshot(
    householdName: String,
): FamilyInviteSnapshot {
    val resolvedInvite = resolveInviteLifecycle()
    return FamilyInviteSnapshot(
        code = code,
        role = role,
        lifecycleStatus = resolvedInvite.lifecycleStatus,
        statusLabel = resolvedInvite.lifecycleStatus.label,
        supportingText = inviteSupportingText(
            resolvedInvite = resolvedInvite,
            householdName = householdName,
        ),
        generatedAtLabel = createdAtEpochMs
            .takeIf { it > 0L }
            ?.let(::formatInviteTimestamp)
            .orEmpty(),
        expiryLabel = formatInviteExpiryLabel(
            expiresAtEpochMs = expiresAtEpochMs,
            lifecycleStatus = resolvedInvite.lifecycleStatus,
        ),
        usageLabel = formatInviteUsageLabel(
            usageCount = usageCount,
            usageLimit = usageLimit,
        ),
        shareText = if (resolvedInvite.isJoinable) {
            buildString {
                append("Join ")
                append(householdName.ifBlank { "our household" })
                append(" on ChorePilot with code ")
                append(code)
                append(". You'll join as ")
                append(role.name.lowercase())
                append(".")
            }
        } else {
            ""
        },
        isJoinable = resolvedInvite.isJoinable,
        seatImpactText = when (role) {
            HouseholdRole.Owner -> "This transfers household ownership."
            HouseholdRole.Admin -> "This fills an admin seat for chores, approvals, and settings help."
            HouseholdRole.Member -> "This uses the next household seat for chores, proof, and reminders."
            HouseholdRole.Guest -> "This uses the next guest seat with lighter access."
        },
    )
}

private val HouseholdInviteLifecycleStatus.label: String
    get() = when (this) {
        HouseholdInviteLifecycleStatus.Active -> "Active now"
        HouseholdInviteLifecycleStatus.Revoked -> "Revoked"
        HouseholdInviteLifecycleStatus.Replaced -> "Replaced"
        HouseholdInviteLifecycleStatus.Expired -> "Expired"
        HouseholdInviteLifecycleStatus.Exhausted -> "Used up"
    }

private fun inviteSupportingText(
    resolvedInvite: ResolvedHouseholdInvite,
    householdName: String,
): String = when (resolvedInvite.lifecycleStatus) {
    HouseholdInviteLifecycleStatus.Active ->
        "Share this code to add a ${resolvedInvite.document.role.name.lowercase()} to ${householdName.ifBlank { "your household" }}."

    HouseholdInviteLifecycleStatus.Revoked ->
        "This code was revoked and won't let anyone join."

    HouseholdInviteLifecycleStatus.Replaced ->
        "This code was replaced by a newer invite."

    HouseholdInviteLifecycleStatus.Expired ->
        "This code expired before it was used."

    HouseholdInviteLifecycleStatus.Exhausted ->
        "This code has already been used up."
}

private fun formatInviteExpiryLabel(
    expiresAtEpochMs: Long,
    lifecycleStatus: HouseholdInviteLifecycleStatus,
): String {
    if (expiresAtEpochMs <= 0L) return ""
    val prefix = if (lifecycleStatus == HouseholdInviteLifecycleStatus.Expired) "Expired" else "Expires"
    return "$prefix ${formatReadableDate(Instant.fromEpochMilliseconds(expiresAtEpochMs).toLocalDateTime(TimeZone.currentSystemDefault()).date.toString())}"
}

private fun formatInviteUsageLabel(
    usageCount: Int,
    usageLimit: Int,
): String = when {
    usageLimit <= 0 && usageCount <= 0 -> "Unlimited uses"
    usageLimit <= 0 -> "Used $usageCount time${pluralize(usageCount)}"
    else -> "${(usageLimit - usageCount).coerceAtLeast(0)} of $usageLimit uses left"
}

private fun FamilyAvailabilityMode.toDocumentMode(): HouseholdAvailabilityMode = when (this) {
    FamilyAvailabilityMode.DayOff -> HouseholdAvailabilityMode.DayOff
    FamilyAvailabilityMode.GoneMode -> HouseholdAvailabilityMode.GoneMode
}

private fun buildRangeLabel(
    startDate: String,
    endDate: String,
): String {
    if (startDate.isBlank()) return "the selected dates"
    if (startDate == endDate || endDate.isBlank()) return formatReadableDate(startDate)
    return "${formatReadableDate(startDate)} to ${formatReadableDate(endDate)}"
}

private fun formatInviteTimestamp(epochMs: Long): String {
    val instant = Instant.fromEpochMilliseconds(epochMs)
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val localDate = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
    val distance = today.daysUntil(localDate)
    return when {
        distance == 0 -> "Created today"
        distance == -1 -> "Created yesterday"
        else -> "Created ${formatReadableDate(localDate.toString())}"
    }
}

private fun formatReadableDate(dateKey: String): String = runCatching {
    val localDate = LocalDate.parse(dateKey)
    "${monthAbbreviation(localDate.monthNumber)} ${localDate.day}"
}.getOrDefault(dateKey)

private fun pluralize(count: Int): String = if (count == 1) "" else "s"

private fun monthAbbreviation(monthNumber: Int): String = when (monthNumber) {
    1 -> "Jan"
    2 -> "Feb"
    3 -> "Mar"
    4 -> "Apr"
    5 -> "May"
    6 -> "Jun"
    7 -> "Jul"
    8 -> "Aug"
    9 -> "Sep"
    10 -> "Oct"
    11 -> "Nov"
    else -> "Dec"
}
