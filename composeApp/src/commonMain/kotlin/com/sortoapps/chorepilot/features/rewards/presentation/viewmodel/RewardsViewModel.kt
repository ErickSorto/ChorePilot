package com.sortoapps.chorepilot.features.rewards.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sortoapps.chorepilot.core.demo.DemoTone
import com.sortoapps.chorepilot.core.platform.VibratorUtil
import com.sortoapps.chorepilot.features.auth.domain.HouseholdRole
import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import com.sortoapps.chorepilot.features.auth.domain.hasHouseholdManagementAccess
import com.sortoapps.chorepilot.features.rewards.domain.RewardDefinitionDraft
import com.sortoapps.chorepilot.features.rewards.domain.RewardPointEvent
import com.sortoapps.chorepilot.features.rewards.domain.RewardPointEventType
import com.sortoapps.chorepilot.features.rewards.domain.RewardsFamilyRules
import com.sortoapps.chorepilot.features.rewards.domain.RewardsMemberProgress
import com.sortoapps.chorepilot.features.rewards.domain.RewardsRepository
import com.sortoapps.chorepilot.features.rewards.domain.RewardsRepositoryState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class RewardsViewModel(
    private val rewardsRepository: RewardsRepository,
    private val sessionRepository: SessionRepository,
    private val vibratorUtil: VibratorUtil,
) : ViewModel() {
    private var latestRepositoryState = RewardsRepositoryState()
    private var latestSession = sessionRepository.session.value

    private val _draft = MutableStateFlow(RewardsEditorUiModel())
    private val _uiState = MutableStateFlow(RewardsUiState())
    val uiState: StateFlow<RewardsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            rewardsRepository.state.collectLatest { repositoryState ->
                latestRepositoryState = repositoryState
                render()
            }
        }
        viewModelScope.launch {
            sessionRepository.session.collectLatest { session ->
                latestSession = session
                render()
            }
        }
        viewModelScope.launch {
            rewardsRepository.refresh()
        }
    }

    fun onEvent(event: RewardsUiEvent) {
        when (event) {
            RewardsUiEvent.Refresh -> viewModelScope.launch {
                rewardsRepository.refresh()
            }

            RewardsUiEvent.DismissError -> {
                latestRepositoryState = latestRepositoryState.copy(errorMessage = null)
                render()
            }

            RewardsUiEvent.DismissCelebration -> viewModelScope.launch {
                rewardsRepository.dismissCelebration()
            }

            is RewardsUiEvent.CompletionPointsChanged -> updateRules {
                copy(pointsPerCompletion = event.value.coerceIn(1, 50))
            }

            is RewardsUiEvent.ProofSubmissionBonusChanged -> updateRules {
                copy(proofSubmissionBonus = event.value.coerceIn(0, 25))
            }

            is RewardsUiEvent.ProofApprovalBonusChanged -> updateRules {
                copy(proofApprovalBonus = event.value.coerceIn(0, 25))
            }

            is RewardsUiEvent.DraftTitleChanged -> updateDraft { copy(title = event.value) }
            is RewardsUiEvent.DraftDescriptionChanged -> updateDraft { copy(description = event.value) }
            is RewardsUiEvent.DraftCostChanged -> updateDraft { copy(costText = event.value.filter(Char::isDigit)) }

            RewardsUiEvent.SaveRewardDefinition -> viewModelScope.launch {
                if (!latestSession.role.hasHouseholdManagementAccess()) {
                    showManagementError()
                    return@launch
                }
                val cost = _draft.value.costText.toIntOrNull() ?: 0
                rewardsRepository.saveRewardDefinition(
                    RewardDefinitionDraft(
                        title = _draft.value.title,
                        description = _draft.value.description,
                        costPoints = cost,
                    ),
                )
                _draft.value = RewardsEditorUiModel()
                vibratorUtil.triggerVibrationSuccess()
                render()
            }

            is RewardsUiEvent.DeleteRewardDefinition -> viewModelScope.launch {
                if (!latestSession.role.hasHouseholdManagementAccess()) {
                    showManagementError()
                    return@launch
                }
                rewardsRepository.deleteRewardDefinition(event.rewardId)
                vibratorUtil.triggerVibration()
            }

            is RewardsUiEvent.RedeemReward -> viewModelScope.launch {
                rewardsRepository.redeemReward(event.rewardId)
                vibratorUtil.triggerVibrationSuccess()
            }
        }
    }

    private fun updateRules(
        transform: RewardsFamilyRules.() -> RewardsFamilyRules,
    ) {
        val snapshot = latestRepositoryState.snapshot ?: return
        if (!latestSession.role.hasHouseholdManagementAccess()) {
            showManagementError()
            return
        }
        viewModelScope.launch {
            rewardsRepository.updateRules(snapshot.rules.transform())
            vibratorUtil.triggerVibration()
        }
    }

    private fun updateDraft(
        transform: RewardsEditorUiModel.() -> RewardsEditorUiModel,
    ) {
        _draft.update(transform)
        render()
    }

    private fun render() {
        val snapshot = latestRepositoryState.snapshot
        val viewerRole = latestSession.role
        val canManageRewards = viewerRole == HouseholdRole.Owner || viewerRole == HouseholdRole.Admin
        val hasHousehold = latestSession.householdId.isNotBlank()
        val viewerId = snapshot?.viewerId
            ?: latestSession.userId.takeIf { it.isNotBlank() }
            ?: latestSession.displayName.lowercase().replace(" ", "-")
        val viewerProgress = snapshot?.memberProgress?.firstOrNull { it.memberId == viewerId }
            ?: snapshot?.memberProgress?.firstOrNull { it.memberName.equals(latestSession.displayName, ignoreCase = true) }
        val draft = _draft.value
        val rewards = snapshot?.availableRewards.orEmpty()
        val history = snapshot?.recentPointEvents.orEmpty()
        val leaderboard = snapshot?.memberProgress.orEmpty()
        val isRewardCatalogEmpty = rewards.isEmpty()
        val isFirstUseEmpty = hasHousehold && isRewardCatalogEmpty && history.isEmpty()
        val hero = buildHero(
            canManageRewards = canManageRewards,
            hasHousehold = hasHousehold,
            viewerName = latestSession.displayName.ifBlank { "Household member" },
            viewerProgress = viewerProgress,
            rewards = rewards,
        )
        _uiState.value = RewardsUiState(
            isLoading = latestRepositoryState.isLoading,
            errorMessage = latestRepositoryState.errorMessage,
            householdName = snapshot?.householdName ?: latestSession.householdName,
            viewerName = latestSession.displayName.ifBlank { "Household member" },
            viewerRole = viewerRole,
            canManageRewards = canManageRewards,
            hero = hero,
            leaderboard = leaderboard.mapIndexed { index, progress ->
                RewardsMemberProgressUiModel(
                    memberId = progress.memberId,
                    memberName = progress.memberName,
                    avatarSeed = initials(progress.memberName),
                    pointsLabel = "${progress.availablePoints} pts",
                    streakLabel = "${progress.currentStreak}-day streak",
                    supportingText = "${progress.completedChores} complete · ${progress.pendingProofCount} proof reviews · ${progress.overdueCount} overdue",
                    tone = when {
                        progress.isViewer(viewerId = viewerId, viewerName = latestSession.displayName) -> DemoTone.Primary
                        index == 0 -> DemoTone.Secondary
                        progress.overdueCount > 0 -> DemoTone.Warning
                        else -> DemoTone.Tertiary
                    },
                    isViewer = progress.isViewer(viewerId = viewerId, viewerName = latestSession.displayName),
                )
            },
            rewards = rewards.map { reward ->
                val availablePoints = viewerProgress?.availablePoints ?: 0
                RewardsCatalogItemUiModel(
                    id = reward.id,
                    title = reward.title,
                    description = reward.description,
                    costLabel = "${reward.costPoints} pts",
                    availabilityLabel = if (reward.costPoints <= availablePoints) {
                        "Ready"
                    } else {
                        "${reward.costPoints - availablePoints} pts left"
                    },
                    canRedeem = reward.costPoints <= availablePoints,
                    canDelete = canManageRewards,
                )
            },
            history = history.map { event -> event.toUiModel() },
            rules = snapshot?.rules?.toUiModel() ?: RewardsRulesUiModel(),
            editor = draft.copy(
                saveEnabled = canManageRewards &&
                    draft.title.isNotBlank() &&
                    (draft.costText.toIntOrNull() ?: 0) > 0,
            ),
            celebration = snapshot?.celebration?.let {
                RewardsCelebrationUiModel(title = it.title, body = it.body)
            },
            emptyTitle = when {
                !hasHousehold -> "Join a household to use rewards"
                isFirstUseEmpty && canManageRewards -> "Add the first reward"
                isFirstUseEmpty -> "Rewards are about to start"
                else -> ""
            },
            emptyBody = when {
                !hasHousehold -> "Rewards only make sense once chores and members belong to a real household."
                isFirstUseEmpty && canManageRewards ->
                    "Create one reward below so points have a real payoff from day one."
                isFirstUseEmpty ->
                    "Finish chores and keep proof moving. Your points are already tracking here."
                else -> ""
            },
        )
    }

    private fun showManagementError() {
        latestRepositoryState = latestRepositoryState.copy(
            errorMessage = "Only owners and admins can edit reward settings.",
        )
        render()
    }
}

private fun buildHero(
    canManageRewards: Boolean,
    hasHousehold: Boolean,
    viewerName: String,
    viewerProgress: RewardsMemberProgress?,
    rewards: List<com.sortoapps.chorepilot.features.rewards.domain.RewardDefinition>,
): RewardsHeroUiModel {
    if (!hasHousehold) {
        return RewardsHeroUiModel(
            title = "Rewards live inside a household",
            body = "Join or create a household first, then points and rewards will start tracking together.",
            pointsLabel = "0 pts",
            streakLabel = "0-day streak",
            supportingLabel = "No household is connected yet.",
            nextRewardLabel = "Join a household to unlock rewards.",
        )
    }

    if (rewards.isEmpty()) {
        return if (canManageRewards) {
            RewardsHeroUiModel(
                title = "Create the first reward",
                body = "Add one simple reward so every completed chore has a clear payoff the household can feel.",
                pointsLabel = "${viewerProgress?.availablePoints ?: 0} pts",
                streakLabel = "${viewerProgress?.currentStreak ?: 0}-day streak",
                supportingLabel = viewerProgress?.let {
                    "${it.completedChores} completed, ${it.pendingProofCount} awaiting proof review, ${it.overdueCount} overdue."
                } ?: "No rewards are live yet, but points rules are ready.",
                nextRewardLabel = "Add the first reward below.",
            )
        } else {
            RewardsHeroUiModel(
                title = "Start earning now",
                body = "Completed chores and approved proof turn straight into points.",
                pointsLabel = "${viewerProgress?.availablePoints ?: 0} pts",
                streakLabel = "${viewerProgress?.currentStreak ?: 0}-day streak",
                supportingLabel = viewerProgress?.let {
                    "${it.completedChores} completed, ${it.pendingProofCount} awaiting proof review, ${it.overdueCount} overdue."
                } ?: "Keep showing up. Your reward progress starts with chores.",
                nextRewardLabel = "Your first reward will appear here soon.",
            )
        }
    }

    if (viewerProgress == null) {
        return RewardsHeroUiModel(
            title = "$viewerName, your first reward is live.",
            body = "Rewards are ready. Start closing chores and submitting proof to build points toward the first unlock.",
            pointsLabel = "0 pts",
            streakLabel = "0-day streak",
            supportingLabel = "No reward activity is tracked for you yet.",
            nextRewardLabel = "Start earning toward ${rewards.first().title}.",
        )
    }

    return RewardsHeroUiModel(
        title = "${viewerProgress.memberName}, you're building real momentum.",
        body = if (canManageRewards) {
            "Completed chores and proof follow-through turn straight into points and unlocked rewards."
        } else {
            "Completed chores and proof approvals keep pushing your next reward closer."
        },
        pointsLabel = "${viewerProgress.availablePoints} pts",
        streakLabel = "${viewerProgress.currentStreak}-day streak",
        supportingLabel = "${viewerProgress.completedChores} completed, ${viewerProgress.pendingProofCount} awaiting proof review, ${viewerProgress.overdueCount} overdue.",
        nextRewardLabel = when {
            viewerProgress.nextRewardGap == 0 -> "You can redeem a reward right now."
            else -> "${viewerProgress.nextRewardGap} pts until the next reward."
        },
    )
}

private fun RewardsFamilyRules.toUiModel(): RewardsRulesUiModel = RewardsRulesUiModel(
    pointsPerCompletion = pointsPerCompletion,
    proofSubmissionBonus = proofSubmissionBonus,
    proofApprovalBonus = proofApprovalBonus,
)

private fun RewardPointEvent.toUiModel(): RewardsHistoryRowUiModel = RewardsHistoryRowUiModel(
    title = when (type) {
        RewardPointEventType.Completion -> "Chore closed"
        RewardPointEventType.ProofSubmitted -> "Proof submitted"
        RewardPointEventType.ProofApproved -> "Proof approved"
        RewardPointEventType.RedemptionReserved -> "Reward redeemed"
        RewardPointEventType.RedemptionRejected -> "Points returned"
    },
    body = description,
    timestampLabel = createdAtEpochMs.toHistoryTimestampLabel(),
    pointsLabel = if (pointsDelta > 0) "+$pointsDelta pts" else "$pointsDelta pts",
    tone = when (type) {
        RewardPointEventType.Completion -> DemoTone.Primary
        RewardPointEventType.ProofSubmitted -> DemoTone.Secondary
        RewardPointEventType.ProofApproved -> DemoTone.Success
        RewardPointEventType.RedemptionReserved -> DemoTone.Warning
        RewardPointEventType.RedemptionRejected -> DemoTone.Tertiary
    },
)

private fun Long.toHistoryTimestampLabel(): String {
    val local = kotlin.time.Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault())
    val hour = ((local.hour + 11) % 12) + 1
    val minute = local.minute.toString().padStart(2, '0')
    val meridiem = if (local.hour >= 12) "PM" else "AM"
    return "${local.month.name.lowercase().replaceFirstChar(Char::uppercase)} ${local.dayOfMonth} at $hour:$minute $meridiem"
}

private fun RewardsMemberProgress.isViewer(
    viewerId: String,
    viewerName: String,
): Boolean = memberId == viewerId || memberName.equals(viewerName, ignoreCase = true)

private fun initials(name: String): String = name
    .split(" ")
    .filter { it.isNotBlank() }
    .take(2)
    .joinToString("") { it.first().uppercase() }
    .ifBlank { "CP" }
