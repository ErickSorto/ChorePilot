package com.sortoapps.chorepilot.features.rewards.data

import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import com.sortoapps.chorepilot.features.auth.domain.SessionSnapshot
import com.sortoapps.chorepilot.features.chores.domain.ChoreItem
import com.sortoapps.chorepilot.features.chores.domain.ChoreItemStatus
import com.sortoapps.chorepilot.features.chores.domain.ChoreRepository
import com.sortoapps.chorepilot.features.auth.domain.hasHouseholdManagementAccess
import com.sortoapps.chorepilot.features.rewards.domain.RewardDefinition
import com.sortoapps.chorepilot.features.rewards.domain.RewardDefinitionDraft
import com.sortoapps.chorepilot.features.rewards.domain.RewardPointEvent
import com.sortoapps.chorepilot.features.rewards.domain.RewardPointEventType
import com.sortoapps.chorepilot.features.rewards.domain.RewardRedemption
import com.sortoapps.chorepilot.features.rewards.domain.RewardRedemptionStatus
import com.sortoapps.chorepilot.features.rewards.domain.RewardsCelebration
import com.sortoapps.chorepilot.features.rewards.domain.RewardsFamilyRules
import com.sortoapps.chorepilot.features.rewards.domain.RewardsMemberProgress
import com.sortoapps.chorepilot.features.rewards.domain.RewardsRepository
import com.sortoapps.chorepilot.features.rewards.domain.RewardsRepositoryState
import com.sortoapps.chorepilot.features.rewards.domain.RewardsSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

internal class StorageBackedRewardsRepository(
    private val persistence: RewardsStorePersistence,
    private val sessionRepository: SessionRepository,
    private val choreRepository: ChoreRepository,
    appScope: CoroutineScope,
) : RewardsRepository {
    private val _state = MutableStateFlow(RewardsRepositoryState())
    override val state: StateFlow<RewardsRepositoryState> = _state.asStateFlow()

    init {
        appScope.launch {
            combine(sessionRepository.session, choreRepository.chores) { session, chores ->
                session to chores
            }.collectLatest { (session, chores) ->
                runCatching {
                    publishState(
                        session = session,
                        chores = chores,
                        keepLoading = false,
                        errorMessage = null,
                    )
                }.onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "We couldn't refresh rewards right now.",
                    )
                }
            }
        }
    }

    override suspend fun refresh() {
        _state.value = _state.value.copy(isLoading = true)
        runCatching {
            sessionRepository.refreshSession()
            choreRepository.refresh()
            publishState(
                session = sessionRepository.session.value,
                chores = choreRepository.chores.value,
                keepLoading = false,
                errorMessage = null,
            )
        }.onFailure { error ->
            _state.value = _state.value.copy(
                isLoading = false,
                errorMessage = error.message ?: "We couldn't refresh rewards right now.",
            )
        }
    }

    override suspend fun updateRules(rules: RewardsFamilyRules) {
        ensureCanManageRewards()
        mutateActiveStore("We couldn't update family rules right now.") { current ->
            current.copy(rules = rules.copy(redemptionRequiresApproval = false))
        }
    }

    override suspend fun saveRewardDefinition(draft: RewardDefinitionDraft) {
        ensureCanManageRewards()
        val normalizedTitle = draft.title.trim()
        val normalizedDescription = draft.description.trim()
        if (normalizedTitle.isBlank()) {
            _state.value = _state.value.copy(errorMessage = "Give the reward a title first.")
            return
        }
        if (draft.costPoints <= 0) {
            _state.value = _state.value.copy(errorMessage = "Reward cost needs to be at least 1 point.")
            return
        }
        mutateActiveStore("We couldn't save that reward yet.") { current ->
            val rewardId = buildId(prefix = "reward", seed = normalizedTitle)
            current.copy(
                rewardDefinitions = current.rewardDefinitions
                    .filterNot { it.id == rewardId }
                    .plus(
                        RewardDefinition(
                            id = rewardId,
                            title = normalizedTitle,
                            description = normalizedDescription,
                            costPoints = draft.costPoints,
                        ),
                    )
                    .sortedBy { it.costPoints },
            )
        }
    }

    override suspend fun deleteRewardDefinition(rewardId: String) {
        ensureCanManageRewards()
        mutateActiveStore("We couldn't remove that reward right now.") { current ->
            current.copy(
                rewardDefinitions = current.rewardDefinitions.filterNot { it.id == rewardId },
            )
        }
    }

    override suspend fun redeemReward(rewardId: String) {
        val session = sessionRepository.session.value
        val householdId = session.householdId.takeIf(String::isNotBlank)
        if (householdId == null) {
            _state.value = _state.value.copy(errorMessage = "Join a household before using rewards.")
            return
        }
        val store = loadActiveStore(householdId).withDefaults()
        val reward = store.rewardDefinitions.firstOrNull { it.id == rewardId }
        if (reward == null) {
            _state.value = _state.value.copy(errorMessage = "That reward is no longer available.")
            return
        }
        val progress = buildMemberProgress(
            pointEvents = store.pointEvents,
            rewardDefinitions = store.rewardDefinitions,
            currentChores = choreRepository.chores.value,
        ).firstOrNull { it.memberId == resolveViewerMemberId(session) }
        val availablePoints = progress?.availablePoints ?: 0
        if (availablePoints < reward.costPoints) {
            _state.value = _state.value.copy(
                errorMessage = "You need ${reward.costPoints - availablePoints} more points for ${reward.title}.",
            )
            return
        }
        mutateActiveStore("We couldn't redeem that reward right now.") { current ->
            val redemption = RewardRedemption(
                id = buildId(prefix = "redemption", seed = reward.title),
                rewardId = reward.id,
                rewardTitle = reward.title,
                memberId = resolveViewerMemberId(session),
                memberName = session.displayName.ifBlank { "Household member" },
                costPoints = reward.costPoints,
                status = RewardRedemptionStatus.Approved,
                redeemedAtEpochMs = nowEpochMs(),
                redeemedDateKey = todayDateKey(),
            )
            val reservation = RewardPointEvent(
                id = buildId(prefix = "event", seed = reward.title),
                memberId = redemption.memberId,
                memberName = redemption.memberName,
                type = RewardPointEventType.RedemptionReserved,
                pointsDelta = -reward.costPoints,
                dateKey = redemption.redeemedDateKey,
                createdAtEpochMs = redemption.redeemedAtEpochMs,
                rewardId = reward.id,
                description = "Redeemed ${reward.title} for ${reward.costPoints} points.",
            )
            current.copy(
                pointEvents = current.pointEvents + reservation,
                redemptionHistory = listOf(redemption) + current.redemptionHistory,
                celebration = RewardsCelebration(
                    title = "Reward unlocked",
                    body = "${reward.title} is ready. Nice work keeping the streak moving.",
                ),
            )
        }
    }

    override suspend fun dismissCelebration() {
        mutateActiveStore("We couldn't clear that celebration yet.") { current ->
            current.copy(celebration = null)
        }
    }

    private fun ensureCanManageRewards() {
        if (!sessionRepository.session.value.role.hasHouseholdManagementAccess()) {
            error("Only owners and admins can edit reward settings.")
        }
    }

    private suspend fun publishState(
        session: SessionSnapshot,
        chores: List<ChoreItem>,
        keepLoading: Boolean,
        errorMessage: String?,
    ) {
        val householdId = session.householdId
        if (householdId.isBlank()) {
            _state.value = RewardsRepositoryState(
                isLoading = keepLoading,
                snapshot = RewardsSnapshot(
                    householdName = session.householdName,
                    viewerId = resolveViewerMemberId(session),
                    viewerName = session.displayName,
                    rules = RewardsFamilyRules(),
                    availableRewards = emptyList(),
                    memberProgress = emptyList(),
                    redemptionHistory = emptyList(),
                    recentPointEvents = emptyList(),
                ),
                errorMessage = errorMessage,
            )
            return
        }

        val existing = loadActiveStore(householdId)
        val seeded = existing.withDefaults()
        val reconciled = seeded.reconcilePoints(chores)
        if (reconciled != existing) {
            saveStore(householdId, reconciled)
        }
        val memberProgress = buildMemberProgress(
            pointEvents = reconciled.pointEvents,
            rewardDefinitions = reconciled.rewardDefinitions,
            currentChores = chores,
        )
        _state.value = RewardsRepositoryState(
            isLoading = keepLoading,
            snapshot = RewardsSnapshot(
                householdName = session.householdName.ifBlank { "Your household" },
                viewerId = resolveViewerMemberId(session),
                viewerName = session.displayName.ifBlank { "Household member" },
                rules = reconciled.rules,
                availableRewards = reconciled.rewardDefinitions.filter { it.enabled },
                memberProgress = memberProgress,
                redemptionHistory = reconciled.redemptionHistory.sortedByDescending { it.redeemedAtEpochMs },
                recentPointEvents = reconciled.pointEvents.sortedByDescending { it.createdAtEpochMs }.take(8),
                celebration = reconciled.celebration,
            ),
            errorMessage = errorMessage,
        )
    }

    private suspend fun mutateActiveStore(
        defaultError: String,
        transform: (RewardsPersistedHouseholdStore) -> RewardsPersistedHouseholdStore,
    ) {
        val session = sessionRepository.session.value
        val householdId = session.householdId
        if (householdId.isBlank()) {
            _state.value = _state.value.copy(errorMessage = "Join a household before using rewards.")
            return
        }
        runCatching {
            val updated = transform(loadActiveStore(householdId).withDefaults())
            saveStore(householdId, updated)
            publishState(
                session = session,
                chores = choreRepository.chores.value,
                keepLoading = false,
                errorMessage = null,
            )
        }.onFailure { error ->
            _state.value = _state.value.copy(errorMessage = error.message ?: defaultError)
        }
    }

    private suspend fun loadActiveStore(
        householdId: String,
    ): RewardsPersistedHouseholdStore = persistence.loadStore(householdId)

    private suspend fun saveStore(
        householdId: String,
        store: RewardsPersistedHouseholdStore,
    ) {
        persistence.saveStore(householdId, store)
    }

    private fun RewardsPersistedHouseholdStore.reconcilePoints(
        chores: List<ChoreItem>,
    ): RewardsPersistedHouseholdStore {
        val nextTracked = chores.associate { chore ->
            chore.id to TrackedRewardChoreState(
                status = chore.status,
                proofPhotoCount = chore.proofPhotoCount,
                completedByName = chore.completedByName,
                assigneeName = chore.assignee,
                assigneeId = chore.assigneeId,
            )
        }
        var updated = copy(trackedChores = nextTracked)
        chores.forEach { chore ->
            val previous = trackedChores[chore.id]
            updated = when {
                previous == null -> updated.applyBootstrap(chore)
                previous != nextTracked[chore.id] -> updated.applyTransition(previous = previous, current = chore)
                else -> updated
            }
        }
        return updated
    }

    private fun RewardsPersistedHouseholdStore.applyBootstrap(
        chore: ChoreItem,
    ): RewardsPersistedHouseholdStore = when (chore.status) {
        ChoreItemStatus.PendingProof -> appendPointEvents(
            completionEventFor(chore = chore, points = rules.pointsPerCompletion),
            proofSubmittedEventFor(chore = chore, points = rules.proofSubmissionBonus),
        )

        ChoreItemStatus.Completed -> {
            val events = buildList {
                add(completionEventFor(chore = chore, points = rules.pointsPerCompletion))
                if (chore.proofPhotoCount > 0) {
                    add(proofSubmittedEventFor(chore = chore, points = rules.proofSubmissionBonus))
                    add(proofApprovedEventFor(chore = chore, points = rules.proofApprovalBonus))
                }
            }
            appendPointEvents(*events.toTypedArray())
        }

        else -> this
    }

    private fun RewardsPersistedHouseholdStore.applyTransition(
        previous: TrackedRewardChoreState,
        current: ChoreItem,
    ): RewardsPersistedHouseholdStore {
        val reversalEvents = when (previous.status) {
            ChoreItemStatus.PendingProof -> {
                if (current.status != ChoreItemStatus.PendingProof && current.status != ChoreItemStatus.Completed) {
                    listOf(
                        completionEventFor(chore = current, points = -rules.pointsPerCompletion),
                        proofSubmittedEventFor(chore = current, points = -rules.proofSubmissionBonus),
                    )
                } else {
                    emptyList()
                }
            }

            ChoreItemStatus.Completed -> {
                if (current.status != ChoreItemStatus.Completed) {
                    buildList {
                        add(completionEventFor(chore = current, points = -rules.pointsPerCompletion))
                        if (previous.proofPhotoCount > 0) {
                            add(proofSubmittedEventFor(chore = current, points = -rules.proofSubmissionBonus))
                            add(proofApprovedEventFor(chore = current, points = -rules.proofApprovalBonus))
                        }
                    }
                } else {
                    emptyList()
                }
            }

            else -> emptyList()
        }
        if (reversalEvents.isNotEmpty()) {
            return appendPointEvents(*reversalEvents.toTypedArray())
        }
        return when {
            current.status == ChoreItemStatus.PendingProof && previous.status != ChoreItemStatus.PendingProof -> {
                appendPointEvents(
                    completionEventFor(chore = current, points = rules.pointsPerCompletion),
                    proofSubmittedEventFor(chore = current, points = rules.proofSubmissionBonus),
                )
            }

            current.status == ChoreItemStatus.Completed && previous.status == ChoreItemStatus.PendingProof -> {
                if (current.proofPhotoCount > 0) {
                    appendPointEvents(proofApprovedEventFor(chore = current, points = rules.proofApprovalBonus))
                } else {
                    this
                }
            }

            current.status == ChoreItemStatus.Completed && previous.status != ChoreItemStatus.Completed -> {
                val events = buildList {
                    add(completionEventFor(chore = current, points = rules.pointsPerCompletion))
                    if (current.proofPhotoCount > 0) {
                        add(proofSubmittedEventFor(chore = current, points = rules.proofSubmissionBonus))
                        add(proofApprovedEventFor(chore = current, points = rules.proofApprovalBonus))
                    }
                }
                appendPointEvents(*events.toTypedArray())
            }

            else -> this
        }
    }

    private fun RewardsPersistedHouseholdStore.appendPointEvents(
        vararg events: RewardPointEvent,
    ): RewardsPersistedHouseholdStore {
        val filtered = events.filter { it.pointsDelta != 0 }
        if (filtered.isEmpty()) return this
        return copy(pointEvents = pointEvents + filtered)
    }

    private fun buildMemberProgress(
        pointEvents: List<RewardPointEvent>,
        rewardDefinitions: List<RewardDefinition>,
        currentChores: List<ChoreItem>,
    ): List<RewardsMemberProgress> {
        val choreStats = currentChores.groupBy { chore ->
            memberIdentity(
                memberId = if (chore.completedByName.isNotBlank()) "" else chore.assigneeId,
                memberName = if (chore.completedByName.isNotBlank()) chore.completedByName else chore.assignee,
            )
        }
        val pointMembers = pointEvents.groupBy { it.memberId to it.memberName }
        val members = (choreStats.keys + pointMembers.keys).distinct()
        return members.map { key ->
            val events = pointMembers[key].orEmpty()
            val chores = choreStats[key].orEmpty()
            val totalEarned = events
                .filter { it.pointsDelta > 0 }
                .sumOf { it.pointsDelta }
            val availablePoints = events.sumOf { it.pointsDelta }.coerceAtLeast(0)
            val cheapestRewardGap = rewardDefinitions
                .filter { it.enabled }
                .minOfOrNull { reward -> (reward.costPoints - availablePoints).coerceAtLeast(0) }
                ?: 0
            RewardsMemberProgress(
                memberId = key.first,
                memberName = key.second,
                totalEarnedPoints = totalEarned,
                availablePoints = availablePoints,
                currentStreak = calculateStreak(events),
                completedChores = chores.count { it.status == ChoreItemStatus.Completed },
                pendingProofCount = chores.count { it.status == ChoreItemStatus.PendingProof },
                overdueCount = chores.count { it.status == ChoreItemStatus.Overdue },
                nextRewardGap = cheapestRewardGap,
            )
        }.sortedWith(
            compareByDescending<RewardsMemberProgress> { it.availablePoints }
                .thenByDescending { it.currentStreak }
                .thenBy { it.memberName },
        )
    }

    private fun calculateStreak(
        events: List<RewardPointEvent>,
    ): Int {
        if (events.isEmpty()) return 0
        val byDay = events
            .filter(::countsTowardStreak)
            .groupBy { it.dateKey }
            .mapValues { (_, dayEvents) -> dayEvents.sumOf { it.pointsDelta } }
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        var streak = 0
        var cursor = today
        while (true) {
            val dateKey = cursor.toString()
            val points = byDay[dateKey] ?: break
            if (points <= 0) break
            streak += 1
            cursor = cursor.minus(DatePeriod(days = 1))
        }
        return streak
    }

    private fun countsTowardStreak(
        event: RewardPointEvent,
    ): Boolean = when (event.type) {
        RewardPointEventType.Completion,
        RewardPointEventType.ProofSubmitted,
        RewardPointEventType.ProofApproved -> event.pointsDelta > 0

        RewardPointEventType.RedemptionReserved,
        RewardPointEventType.RedemptionRejected -> false
    }

    private fun completionEventFor(
        chore: ChoreItem,
        points: Int,
    ): RewardPointEvent {
        val member = memberIdentity(
            memberId = if (chore.completedByName.isNotBlank()) "" else chore.assigneeId,
            memberName = if (chore.completedByName.isNotBlank()) chore.completedByName else chore.assignee,
        )
        return RewardPointEvent(
            id = buildId(prefix = "event", seed = "${chore.id}-completion-${nowEpochMs()}"),
            memberId = member.first,
            memberName = member.second,
            type = RewardPointEventType.Completion,
            pointsDelta = points,
            dateKey = todayDateKey(),
            createdAtEpochMs = nowEpochMs(),
            choreId = chore.id,
            description = if (points >= 0) {
                "Closed out ${chore.title}."
            } else {
                "Reopened ${chore.title}."
            },
        )
    }

    private fun proofSubmittedEventFor(
        chore: ChoreItem,
        points: Int,
    ): RewardPointEvent {
        val member = memberIdentity(
            memberId = if (chore.completedByName.isNotBlank()) "" else chore.assigneeId,
            memberName = if (chore.completedByName.isNotBlank()) chore.completedByName else chore.assignee,
        )
        return RewardPointEvent(
            id = buildId(prefix = "event", seed = "${chore.id}-proof-${nowEpochMs()}"),
            memberId = member.first,
            memberName = member.second,
            type = RewardPointEventType.ProofSubmitted,
            pointsDelta = points,
            dateKey = todayDateKey(),
            createdAtEpochMs = nowEpochMs(),
            choreId = chore.id,
            description = if (points >= 0) {
                "Submitted proof for ${chore.title}."
            } else {
                "Removed proof for ${chore.title}."
            },
        )
    }

    private fun proofApprovedEventFor(
        chore: ChoreItem,
        points: Int,
    ): RewardPointEvent {
        val member = memberIdentity(
            memberId = if (chore.completedByName.isNotBlank()) "" else chore.assigneeId,
            memberName = if (chore.completedByName.isNotBlank()) chore.completedByName else chore.assignee,
        )
        return RewardPointEvent(
            id = buildId(prefix = "event", seed = "${chore.id}-approved-${nowEpochMs()}"),
            memberId = member.first,
            memberName = member.second,
            type = RewardPointEventType.ProofApproved,
            pointsDelta = points,
            dateKey = todayDateKey(),
            createdAtEpochMs = nowEpochMs(),
            choreId = chore.id,
            description = if (points >= 0) {
                "Proof approval landed for ${chore.title}."
            } else {
                "Proof approval was reversed for ${chore.title}."
            },
        )
    }

    private fun resolveViewerMemberId(
        session: SessionSnapshot,
    ): String = session.userId.takeIf { it.isNotBlank() } ?: slugify(session.displayName.ifBlank { "member" })

    private fun memberIdentity(
        memberId: String,
        memberName: String,
    ): Pair<String, String> {
        val normalizedName = memberName.ifBlank { "Household member" }
        val normalizedId = memberId.takeIf { it.isNotBlank() } ?: slugify(normalizedName)
        return normalizedId to normalizedName
    }

    private fun buildId(prefix: String, seed: String): String = "$prefix-${slugify(seed)}-${nowEpochMs()}"

    private fun todayDateKey(): String = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()

    private fun nowEpochMs(): Long = Clock.System.now().toEpochMilliseconds()

    private fun slugify(value: String): String = value
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .ifBlank { "item" }
}
