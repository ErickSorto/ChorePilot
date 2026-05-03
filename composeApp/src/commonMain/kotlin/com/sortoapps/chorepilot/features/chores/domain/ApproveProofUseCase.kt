package com.sortoapps.chorepilot.features.chores.domain

import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import com.sortoapps.chorepilot.features.notifications.domain.HouseholdNotifications
import com.sortoapps.chorepilot.features.notifications.domain.ProofApprovedNotification
import com.sortoapps.chorepilot.features.rewards.domain.RewardsFamilyRules
import com.sortoapps.chorepilot.features.rewards.domain.RewardDefinitionDraft
import com.sortoapps.chorepilot.features.rewards.domain.RewardsRepositoryState
import com.sortoapps.chorepilot.features.rewards.domain.RewardsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Clock
import kotlin.time.Instant

data class ApproveProofResult(
    val feedbackMessage: String,
    val approvedAt: Instant,
    val approvedDateKey: String,
)

class ApproveProofUseCase(
    private val choreRepository: ChoreRepository,
    private val sessionRepository: SessionRepository,
    private val rewardsRepository: RewardsRepository = NoOpRewardsRepository,
    private val householdNotifications: HouseholdNotifications,
) {
    suspend operator fun invoke(
        choreId: String,
        day: ChoreScheduleDay? = null,
    ): ApproveProofResult? {
        val chore = choreRepository.getChore(choreId, day) ?: return null
        val proof = choreRepository.getLatestProofForChore(choreId, day)
        val targetDay = day ?: proof?.dateKey?.let(::buildScheduleDay) ?: todayScheduleDay()

        choreRepository.approveProof(choreId, targetDay)

        val session = sessionRepository.session.value
        val awardedPoints = rewardsRepository.state.value.snapshot?.rules?.proofApprovalBonus
            ?: RewardsFamilyRules().proofApprovalBonus
        runCatching {
            householdNotifications.onProofApproved(
                ProofApprovedNotification(
                    householdId = session.householdId,
                    choreId = choreId,
                    choreTitle = chore.title,
                    dateKey = targetDay.dateKey,
                    assigneeName = chore.assignee,
                    submittingMemberName = proof?.uploadedByName?.ifBlank { chore.assignee } ?: chore.assignee,
                    reviewerName = session.displayName,
                    completionOutcome = "approved_completed",
                    recipientUserId = proof?.uploadedByUserId.orEmpty(),
                    awardedPoints = awardedPoints,
                ),
            )
        }

        val approvedAt = Clock.System.now()
        return ApproveProofResult(
            feedbackMessage = approvalFeedback(
                submittingMemberName = proof?.uploadedByName.orEmpty(),
                assigneeName = chore.assignee,
            ),
            approvedAt = approvedAt,
            approvedDateKey = targetDay.dateKey,
        )
    }
}

private object NoOpRewardsRepository : RewardsRepository {
    override val state = MutableStateFlow(RewardsRepositoryState(snapshot = null, isLoading = false))

    override suspend fun refresh() = Unit

    override suspend fun updateRules(rules: RewardsFamilyRules) = Unit

    override suspend fun saveRewardDefinition(draft: RewardDefinitionDraft) = Unit

    override suspend fun deleteRewardDefinition(rewardId: String) = Unit

    override suspend fun redeemReward(rewardId: String) = Unit

    override suspend fun dismissCelebration() = Unit
}

fun approvalFeedback(
    submittingMemberName: String,
    assigneeName: String,
): String {
    val actor = submittingMemberName.ifBlank { assigneeName }.ifBlank { "This" }
    return "$actor's proof was approved and the chore is now complete."
}

private fun buildScheduleDay(
    dateKey: String,
): ChoreScheduleDay = ChoreScheduleDay(
    id = dateKey,
    weekdayName = dateKey,
    dayOffset = 0,
    isToday = dateKey == todayScheduleDay().dateKey,
    dateKey = dateKey,
)
