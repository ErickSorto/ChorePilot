package com.sortoapps.chorepilot.core.preview

import androidx.compose.runtime.Composable
import com.sortoapps.chorepilot.core.demo.DemoChore
import com.sortoapps.chorepilot.core.demo.DemoChoreStatus
import com.sortoapps.chorepilot.core.demo.DemoContent
import com.sortoapps.chorepilot.core.demo.DemoTone
import com.sortoapps.chorepilot.core.demo.InMemoryDemoContentRepository
import com.sortoapps.chorepilot.core.designsystem.ChorePilotTheme
import com.sortoapps.chorepilot.features.account.presentation.viewmodel.AccountUiState
import com.sortoapps.chorepilot.features.auth.domain.HouseholdRole
import com.sortoapps.chorepilot.features.chores.domain.ChoreItemStatus
import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.ChoreListItemUiModel
import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.ChoreRoomSectionUiModel
import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.ChoreViewerMode
import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.ChoresUiState
import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.ProofSubmissionUiModel
import com.sortoapps.chorepilot.features.family.presentation.viewmodel.FamilyUiState
import com.sortoapps.chorepilot.features.home.presentation.viewmodel.HomeDayUiModel
import com.sortoapps.chorepilot.features.home.presentation.viewmodel.HomeSurfaceMode
import com.sortoapps.chorepilot.features.home.presentation.viewmodel.HomeUiState
import com.sortoapps.chorepilot.features.notifications.presentation.viewmodel.NotificationsUiState
import com.sortoapps.chorepilot.features.notifications.presentation.viewmodel.NotificationRowUiModel
import com.sortoapps.chorepilot.features.notifications.presentation.viewmodel.NotificationSectionUiModel
import com.sortoapps.chorepilot.features.onboarding.presentation.viewmodel.OnboardingUiState
import com.sortoapps.chorepilot.features.onboarding.presentation.viewmodel.OnboardingViewModel
import com.sortoapps.chorepilot.features.rewards.presentation.viewmodel.RewardsCatalogItemUiModel
import com.sortoapps.chorepilot.features.rewards.presentation.viewmodel.RewardsCelebrationUiModel
import com.sortoapps.chorepilot.features.rewards.presentation.viewmodel.RewardsEditorUiModel
import com.sortoapps.chorepilot.features.rewards.presentation.viewmodel.RewardsHeroUiModel
import com.sortoapps.chorepilot.features.rewards.presentation.viewmodel.RewardsHistoryRowUiModel
import com.sortoapps.chorepilot.features.rewards.presentation.viewmodel.RewardsMemberProgressUiModel
import com.sortoapps.chorepilot.features.rewards.presentation.viewmodel.RewardsRulesUiModel
import com.sortoapps.chorepilot.features.rewards.presentation.viewmodel.RewardsUiState
import com.sortoapps.chorepilot.features.reports.presentation.viewmodel.ReportsRange
import com.sortoapps.chorepilot.features.reports.presentation.viewmodel.ReportsLegendItemUiModel
import com.sortoapps.chorepilot.features.reports.presentation.viewmodel.ReportsNavigationUiModel
import com.sortoapps.chorepilot.features.reports.presentation.viewmodel.ReportsSurfaceMode
import com.sortoapps.chorepilot.features.reports.presentation.viewmodel.ReportsTaskHighlightUiModel
import com.sortoapps.chorepilot.features.reports.presentation.viewmodel.ReportsLeaderboardEntryUiModel
import com.sortoapps.chorepilot.features.reports.presentation.viewmodel.ReportsFirstFinisherUiModel
import com.sortoapps.chorepilot.features.reports.presentation.viewmodel.ReportsUiState
import com.sortoapps.chorepilot.features.settings.presentation.viewmodel.SettingsHubHouseholdUiModel
import com.sortoapps.chorepilot.features.settings.presentation.viewmodel.SettingsHubUiState

private val previewRepository = InMemoryDemoContentRepository()

@Composable
fun ChorePilotPreviewTheme(
    largerTextEnabled: Boolean = false,
    content: @Composable () -> Unit,
) {
    ChorePilotTheme(largerTextEnabled = largerTextEnabled, content = content)
}

object ChorePilotPreviewData {
    fun ownerChoresState(): ChoresUiState = choresState(viewerRole = HouseholdRole.Owner)

    fun memberChoresState(): ChoresUiState = choresState(viewerRole = HouseholdRole.Member)

    fun ownerFamilyState(): FamilyUiState = familyState(viewerRole = HouseholdRole.Owner)

    fun memberFamilyState(): FamilyUiState = familyState(viewerRole = HouseholdRole.Member)

    fun homeState(
        selectedSurface: HomeSurfaceMode = HomeSurfaceMode.Today,
    ): HomeUiState {
        val snapshot = previewRepository.getHomeSnapshot()
        val selectedDay = snapshot.days.firstOrNull { it.isToday } ?: snapshot.days.first()
        return HomeUiState(
            householdName = snapshot.householdName,
            viewerId = "erick",
            viewerName = "Erick",
            viewerRole = HouseholdRole.Owner,
            headline = snapshot.headline,
            supportingText = snapshot.supportingText,
            selectedDayLabel = selectedDay.label,
            days = snapshot.days.map { day ->
                HomeDayUiModel(
                    id = day.id,
                    fullLabel = day.label,
                    label = day.shortLabel,
                    supportingText = day.supportingText,
                    dayOffset = day.dayOffset,
                    dateKey = day.dateKey,
                    isSelected = day.id == selectedDay.id,
                    isToday = day.isToday,
                )
            },
            metrics = snapshot.metrics,
            focusChores = DemoContent.chores.map(::toHomeChore),
            selectedSurface = selectedSurface,
        )
    }

    fun choresState(
        viewerRole: HouseholdRole = HouseholdRole.Owner,
    ): ChoresUiState {
        val viewerMode = if (viewerRole == HouseholdRole.Owner || viewerRole == HouseholdRole.Admin) {
            ChoreViewerMode.Admin
        } else {
            ChoreViewerMode.Member
        }
        val chores = DemoContent.chores.map(::toChoreListItem)
        val activeSections = chores
            .filterNot { it.tags.any { tag -> tag.equals("Urgent", ignoreCase = true) } }
            .groupBy { it.area }
            .map { (area, items) -> ChoreRoomSectionUiModel(title = area, chores = items) }
        val unassignedSections = listOf(
            ChoreRoomSectionUiModel(
                title = "Backlog",
                chores = listOf(
                    chores.first().copy(
                        id = "pantry-reset",
                        title = "Pantry reset",
                        area = "Kitchen",
                        assignee = "Inactive",
                        isActive = false,
                        isUnassigned = false,
                        dueLabel = "Paused for spring cleanup",
                        tags = listOf("Kitchen", "Paused"),
                    ),
                ),
            ),
        )
        val needsOwnerSections = listOf(
            ChoreRoomSectionUiModel(
                title = "Kitchen",
                chores = listOf(
                    chores.first().copy(
                        id = "wipe-sink",
                        title = "Wipe sink",
                        assignee = "Unassigned",
                        isActive = true,
                        isUnassigned = true,
                        dueLabel = "Needs an owner",
                    ),
                ),
            ),
        )
        val memberChores = chores.filter { it.assignee.equals("Erick", ignoreCase = true) }
        return ChoresUiState(
            householdName = DemoContent.householdName,
            viewerName = "Erick",
            viewerId = "erick",
            viewerRole = viewerRole,
            selectedFilter = if (viewerMode == ChoreViewerMode.Admin) "All" else "Mine",
            viewerMode = viewerMode,
            canUseAdminExperience = viewerMode == ChoreViewerMode.Admin,
            chores = if (viewerMode == ChoreViewerMode.Admin) chores else memberChores,
            proofQueueSections = if (viewerMode == ChoreViewerMode.Admin) listOf(
                com.sortoapps.chorepilot.features.chores.presentation.viewmodel.ProofSubmissionDateSectionUiModel(
                    title = "Today",
                    proofs = DemoContent.chores
                        .filter { it.status == DemoChoreStatus.PendingProof }
                        .map { chore ->
                            ProofSubmissionUiModel(
                                choreId = chore.id,
                                title = chore.title,
                                area = chore.area,
                                memberName = chore.assignee,
                                submittedLabel = "Submitted 8 min ago",
                                photoCount = 2,
                                state = com.sortoapps.chorepilot.features.chores.presentation.viewmodel.ProofReviewState.AwaitingApproval,
                            )
                        },
                ),
            ) else emptyList(),
            inactiveSections = if (viewerMode == ChoreViewerMode.Admin) unassignedSections else emptyList(),
            needsOwnerSections = if (viewerMode == ChoreViewerMode.Admin) needsOwnerSections else emptyList(),
            activeSections = if (viewerMode == ChoreViewerMode.Admin) activeSections else emptyList(),
        )
    }

    fun familyState(
        viewerRole: HouseholdRole = HouseholdRole.Owner,
    ): FamilyUiState {
        val snapshot = previewRepository.getFamilySnapshot()
        val allMembers = snapshot.members.map { member ->
            com.sortoapps.chorepilot.features.family.presentation.viewmodel.FamilyMemberUiModel(
                id = member.id,
                name = member.name,
                role = member.role,
                statusLabel = member.statusLabel,
                activeChoresLabel = member.activeChoresLabel,
                householdImpact = member.householdImpact,
                avatarSeed = member.avatarSeed,
                tone = member.tone,
                assignedChoreCount = member.assignedChoreCount,
                proofReviewCount = member.proofReviewCount,
                isActiveSeat = member.isActiveSeat,
                seatLabel = if (member.isActiveSeat) "Active seat" else "Inactive seat",
                availabilityLabel = member.availability?.statusLabel.orEmpty(),
                availabilitySummary = member.availability?.summary.orEmpty(),
            )
        }
        val members = if (viewerRole == HouseholdRole.Owner || viewerRole == HouseholdRole.Admin) {
            allMembers
        } else {
            allMembers.filter { it.id == "member-1" }
        }
        val selectedMember = if (viewerRole == HouseholdRole.Owner || viewerRole == HouseholdRole.Admin) {
            members.firstOrNull()
        } else {
            members.firstOrNull()
        }
        val canManageInvites = viewerRole == HouseholdRole.Owner || viewerRole == HouseholdRole.Admin
        return FamilyUiState(
            isLoading = false,
            householdName = snapshot.householdName,
            pulseLabel = snapshot.pulseLabel,
            householdBadge = snapshot.householdBadge,
            summaryMetrics = snapshot.summaryMetrics,
            activeMembers = members.filter { it.isActiveSeat },
            inactiveMembers = members.filterNot { it.isActiveSeat },
            members = members,
            selectedMember = selectedMember,
            invite = com.sortoapps.chorepilot.features.family.presentation.viewmodel.FamilyInviteUiState(
                selectedRole = snapshot.activeInvite.role,
                code = snapshot.activeInvite.code,
                lifecycleStatus = snapshot.activeInvite.lifecycleStatus,
                statusLabel = snapshot.activeInvite.statusLabel,
                supportingText = snapshot.activeInvite.supportingText,
                generatedAtLabel = snapshot.activeInvite.generatedAtLabel,
                expiryLabel = snapshot.activeInvite.expiryLabel,
                usageLabel = snapshot.activeInvite.usageLabel,
                shareText = if (canManageInvites) snapshot.activeInvite.shareText else "",
                hasActiveInvite = snapshot.activeInvite.isJoinable,
                isJoinable = snapshot.activeInvite.isJoinable,
                canCopy = canManageInvites && snapshot.activeInvite.isJoinable && snapshot.activeInvite.code.isNotBlank(),
                canShare = canManageInvites && snapshot.activeInvite.isJoinable && snapshot.activeInvite.shareText.isNotBlank(),
                canManage = canManageInvites,
                canCancel = canManageInvites && snapshot.activeInvite.isJoinable,
                canRotate = canManageInvites,
                actionLabel = "Rotate ${snapshot.activeInvite.role.name.lowercase()} invite",
                seatImpactText = snapshot.activeInvite.seatImpactText,
            ),
            selectedMemberAdmin = com.sortoapps.chorepilot.features.family.presentation.viewmodel.FamilyMemberAdminUiState(
                canManage = canManageInvites,
                canChangeRole = canManageInvites,
                canDeactivate = canManageInvites,
                canRemove = canManageInvites,
                availableRoleOptions = if (canManageInvites) listOf(HouseholdRole.Admin, HouseholdRole.Guest) else emptyList(),
                roleHint = if (canManageInvites) {
                    "Owners can promote, demote, and transfer ownership across non-owner seats."
                } else {
                    "Only owners and admins can manage household seats."
                },
                seatImpactText = "Deactivating or removing this member frees one active household seat.",
                actionHint = if (canManageInvites) {
                    "This seat can be updated, deactivated, or removed safely from here."
                } else {
                    "You can still use day off and gone mode without admin access."
                },
            ),
            viewerRole = viewerRole,
            isSelectedMemberViewer = viewerRole != HouseholdRole.Owner && viewerRole != HouseholdRole.Admin,
            canOpenInviteFlow = canManageInvites,
            canShowInviteSummary = canManageInvites,
            canManageSelectedMemberAvailability = true,
            activeSeatSummary = "${snapshot.activeSeatCount} active seats are currently in the household.",
            inviteStatusSummary = if (canManageInvites) {
                "A ${snapshot.activeInvite.role.name.lowercase()} invite is active and ready to share."
            } else {
                ""
            },
            leaveHouseholdLabel = "Leave household",
            leaveHouseholdHint = "Transfer ownership before leaving this household.",
            canManageInvites = canManageInvites,
            canLeaveHousehold = false,
        )
    }

    fun reportsState(
        selectedSurface: ReportsSurfaceMode = ReportsSurfaceMode.Content,
        selectedRange: ReportsRange = ReportsRange.Daily,
    ): ReportsUiState {
        val snapshot = previewRepository.getReportsSnapshot()
        val period = when (selectedRange) {
            ReportsRange.Daily -> snapshot.daily
            ReportsRange.Weekly -> snapshot.weekly
            ReportsRange.Monthly -> snapshot.monthly
            ReportsRange.Yearly -> snapshot.yearly
        }
        return when (selectedSurface) {
            ReportsSurfaceMode.Content -> ReportsUiState(
                selectedRange = selectedRange,
                selectedSurface = selectedSurface,
                headline = period.headline,
                supportingText = period.supportingText,
                navigation = ReportsNavigationUiModel(
                    title = period.navigation.title,
                    supportingText = period.navigation.supportingText,
                    canGoForward = period.navigation.canGoForward,
                ),
                rangeSummary = period.rangeSummary,
                chartTitle = period.chartTitle,
                chartSubtitle = period.chartSubtitle,
                activeMetrics = period.metrics,
                activeBars = period.bars.map {
                    com.sortoapps.chorepilot.features.reports.presentation.viewmodel.ReportsChartBarUiModel(
                        label = it.label,
                        value = it.value,
                        markerLabel = it.markerLabel,
                        supportingLabel = it.supportingLabel,
                    )
                },
                firstFinisher = period.firstFinisher?.let {
                    ReportsFirstFinisherUiModel(
                        memberName = it.memberName,
                        taskTitle = it.taskTitle,
                        completedAtLabel = it.completedAtLabel,
                    )
                },
                leaderboard = period.leaderboard.map {
                    ReportsLeaderboardEntryUiModel(
                        memberName = it.memberName,
                        avatarSeed = it.avatarSeed,
                        completedCount = it.completedCount,
                        assignedCount = it.assignedCount,
                        pendingCount = it.pendingCount,
                        overdueCount = it.overdueCount,
                        completionRate = it.completionRate,
                        rank = it.rank,
                        highlightLabel = it.highlightLabel,
                    )
                },
                taskHighlights = period.taskHighlights.map {
                    ReportsTaskHighlightUiModel(
                        memberName = it.memberName,
                        taskTitle = it.taskTitle,
                        statusLabel = it.statusLabel,
                    )
                },
                legendItems = period.legendItems.map {
                    ReportsLegendItemUiModel(it.title, it.body)
                },
                hasChorePilotPro = true,
            )

            ReportsSurfaceMode.Locked -> ReportsUiState(
                selectedRange = selectedRange,
                selectedSurface = selectedSurface,
                headline = period.headline,
                supportingText = period.supportingText,
                navigation = ReportsNavigationUiModel(
                    title = period.navigation.title,
                    supportingText = period.navigation.supportingText,
                    canGoForward = period.navigation.canGoForward,
                ),
                lockTitle = period.lockTitle,
                lockBody = period.lockBody,
                lockBenefits = period.lockBenefits,
                hasChorePilotPro = false,
            )

            ReportsSurfaceMode.Empty,
            ReportsSurfaceMode.NoData -> ReportsUiState(
                selectedRange = selectedRange,
                selectedSurface = selectedSurface,
                headline = period.headline,
                supportingText = period.supportingText,
                navigation = ReportsNavigationUiModel(
                    title = period.navigation.title,
                    supportingText = period.navigation.supportingText,
                    canGoForward = period.navigation.canGoForward,
                ),
                emptyTitle = period.emptyTitle,
                emptyBody = period.emptyBody,
                emptyActionHint = period.emptyActionHint,
                hasChorePilotPro = true,
            )
        }
    }

    fun notificationsState(): NotificationsUiState {
        return NotificationsUiState(
            heading = "Household alerts",
            supportingText = "Proof updates, due work, and reassignment alerts stay collected here.",
            isLoading = false,
            unreadCount = 3,
            sections = listOf(
                NotificationSectionUiModel(
                    title = "Today",
                    notifications = listOf(
                        NotificationRowUiModel(
                            id = "preview-overdue",
                            choreId = "chore-4",
                            title = "Trash night is overdue",
                            body = "Erick still owns this one. Missed curbside pickup needs attention.",
                            timestampLabel = "8 min ago",
                            unread = true,
                            badge = "Overdue",
                        ),
                        NotificationRowUiModel(
                            id = "preview-proof-submitted",
                            choreId = "chore-1",
                            title = "Emma submitted proof",
                            body = "Kitchen reset for Emma is ready for review.",
                            timestampLabel = "12 min ago",
                            unread = true,
                            badge = "Proof waiting",
                        ),
                        NotificationRowUiModel(
                            id = "preview-proof-approved",
                            choreId = "chore-2",
                            title = "Erick approved proof",
                            body = "Counters wipedown from Mila is now complete.",
                            timestampLabel = "1 hr ago",
                            unread = false,
                            badge = "Approved",
                        ),
                    ),
                ),
                NotificationSectionUiModel(
                    title = "Earlier this week",
                    notifications = listOf(
                        NotificationRowUiModel(
                            id = "preview-reassigned",
                            choreId = "chore-5",
                            title = "Mila is covering Bathroom reset",
                            body = "Mila is covering for Emma while they're away.",
                            timestampLabel = "1 day ago",
                            unread = true,
                            badge = "Reassigned",
                        ),
                        NotificationRowUiModel(
                            id = "preview-read",
                            choreId = "chore-3",
                            title = "Dog walk is due soon",
                            body = "Emma is up next. Due before dinner.",
                            timestampLabel = "2 days ago",
                            unread = false,
                            badge = "Due soon",
                        ),
                    ),
                ),
            ),
        )
    }

    fun rewardsState(): RewardsUiState = RewardsUiState(
        isLoading = false,
        householdName = "River House",
        viewerName = "Erick",
        viewerRole = HouseholdRole.Owner,
        canManageRewards = true,
        hero = RewardsHeroUiModel(
            title = "Erick, the rewards loop is working.",
            body = "The household can feel progress now: completed chores, proof reviews, and approvals all move points forward.",
            pointsLabel = "62 pts",
            streakLabel = "6-day streak",
            supportingLabel = "7 completed, 1 awaiting proof review, 0 overdue.",
            nextRewardLabel = "You can redeem a reward right now.",
        ),
        leaderboard = listOf(
            RewardsMemberProgressUiModel(
                memberId = "erick",
                memberName = "Erick",
                avatarSeed = "ER",
                pointsLabel = "62 pts",
                streakLabel = "6-day streak",
                supportingText = "7 complete · 1 proof reviews · 0 overdue",
                tone = DemoTone.Primary,
                isViewer = true,
            ),
            RewardsMemberProgressUiModel(
                memberId = "emely",
                memberName = "Emely",
                avatarSeed = "EM",
                pointsLabel = "48 pts",
                streakLabel = "4-day streak",
                supportingText = "5 complete · 0 proof reviews · 1 overdue",
                tone = DemoTone.Secondary,
                isViewer = false,
            ),
        ),
        rewards = listOf(
            RewardsCatalogItemUiModel(
                id = "movie-picker",
                title = "Movie picker",
                description = "Choose the next family movie night without debate.",
                costLabel = "30 pts",
                availabilityLabel = "Ready",
                canRedeem = true,
                canDelete = true,
            ),
            RewardsCatalogItemUiModel(
                id = "dessert-choice",
                title = "Dessert choice",
                description = "Pick the weekend dessert or sweet treat.",
                costLabel = "45 pts",
                availabilityLabel = "Ready",
                canRedeem = true,
                canDelete = true,
            ),
            RewardsCatalogItemUiModel(
                id = "skip-one-chore",
                title = "Skip one chore",
                description = "Trade points for one lighter weeknight handoff.",
                costLabel = "65 pts",
                availabilityLabel = "3 pts left",
                canRedeem = false,
                canDelete = true,
            ),
        ),
        history = listOf(
            RewardsHistoryRowUiModel(
                title = "Proof approved",
                body = "Proof approval landed for Wash dishes.",
                timestampLabel = "Apr 9 at 7:42 PM",
                pointsLabel = "+6 pts",
                tone = DemoTone.Success,
            ),
            RewardsHistoryRowUiModel(
                title = "Chore closed",
                body = "Closed out Bathroom reset.",
                timestampLabel = "Apr 9 at 6:18 PM",
                pointsLabel = "+12 pts",
                tone = DemoTone.Primary,
            ),
            RewardsHistoryRowUiModel(
                title = "Reward redeemed",
                body = "Redeemed Dessert choice for 45 points.",
                timestampLabel = "Apr 8 at 8:05 PM",
                pointsLabel = "-45 pts",
                tone = DemoTone.Warning,
            ),
        ),
        rules = RewardsRulesUiModel(
            pointsPerCompletion = 12,
            proofSubmissionBonus = 4,
            proofApprovalBonus = 6,
        ),
        editor = RewardsEditorUiModel(
            title = "Later bedtime coupon",
            description = "Trade points for an extra 20 minutes on Friday night.",
            costText = "80",
            saveEnabled = true,
        ),
    )

    fun rewardsCelebrationState(): RewardsUiState = rewardsState().copy(
        celebration = RewardsCelebrationUiModel(
            title = "Reward unlocked",
            body = "Dessert choice is ready. Nice work keeping the streak alive.",
        ),
    )

    fun rewardsAdminEmptyState(): RewardsUiState = RewardsUiState(
        isLoading = false,
        householdName = "River House",
        viewerName = "Erick",
        viewerRole = HouseholdRole.Owner,
        canManageRewards = true,
        hero = RewardsHeroUiModel(
            title = "Create the first reward",
            body = "Add one simple reward so every completed chore has a clear payoff the household can feel.",
            pointsLabel = "0 pts",
            streakLabel = "0-day streak",
            supportingLabel = "No rewards are live yet, but points rules are ready.",
            nextRewardLabel = "Add the first reward below.",
        ),
        rules = RewardsRulesUiModel(),
        editor = RewardsEditorUiModel(
            title = "",
            description = "",
            costText = "25",
            saveEnabled = false,
        ),
        emptyTitle = "Add the first reward",
        emptyBody = "Create one reward below so points have a real payoff from day one.",
    )

    fun rewardsMemberEmptyState(): RewardsUiState = RewardsUiState(
        isLoading = false,
        householdName = "River House",
        viewerName = "Mila",
        viewerRole = HouseholdRole.Member,
        canManageRewards = false,
        hero = RewardsHeroUiModel(
            title = "Start earning now",
            body = "Every completed chore and approved proof can turn into points once your household publishes its first reward.",
            pointsLabel = "0 pts",
            streakLabel = "0-day streak",
            supportingLabel = "Keep showing up. Your reward progress starts with chores.",
            nextRewardLabel = "Your first reward will appear here soon.",
        ),
        emptyTitle = "Rewards are about to start",
        emptyBody = "Finish chores and keep proof moving. Your points will be waiting as soon as a reward is added.",
    )

    fun rewardsFirstRewardState(): RewardsUiState = rewardsState().copy(
        canManageRewards = false,
        viewerRole = HouseholdRole.Member,
        viewerName = "Mila",
        hero = RewardsHeroUiModel(
            title = "Mila, your first reward is live.",
            body = "Rewards are ready. Start closing chores and submitting proof to build points toward the first unlock.",
            pointsLabel = "18 pts",
            streakLabel = "2-day streak",
            supportingLabel = "2 completed, 0 awaiting proof review, 0 overdue.",
            nextRewardLabel = "12 pts until the next reward.",
        ),
        leaderboard = listOf(
            RewardsMemberProgressUiModel(
                memberId = "mila",
                memberName = "Mila",
                avatarSeed = "MI",
                pointsLabel = "18 pts",
                streakLabel = "2-day streak",
                supportingText = "2 complete · 0 proof reviews · 0 overdue",
                tone = DemoTone.Primary,
                isViewer = true,
            ),
            RewardsMemberProgressUiModel(
                memberId = "emely",
                memberName = "Emely",
                avatarSeed = "EM",
                pointsLabel = "31 pts",
                streakLabel = "3-day streak",
                supportingText = "4 complete · 1 proof reviews · 0 overdue",
                tone = DemoTone.Secondary,
                isViewer = false,
            ),
        ),
        rewards = listOf(
            RewardsCatalogItemUiModel(
                id = "movie-picker",
                title = "Movie picker",
                description = "Choose the next family movie night without debate.",
                costLabel = "30 pts",
                availabilityLabel = "12 pts left",
                canRedeem = false,
                canDelete = false,
            ),
        ),
        editor = RewardsEditorUiModel(),
    )

    fun rewardsRedemptionState(): RewardsUiState = rewardsState().copy(
        canManageRewards = false,
        viewerRole = HouseholdRole.Member,
        hero = RewardsHeroUiModel(
            title = "You banked enough for a reward.",
            body = "Proof approvals and completions kept stacking up, so you can cash in now.",
            pointsLabel = "49 pts",
            streakLabel = "5-day streak",
            supportingLabel = "5 completed, 1 awaiting proof review, 0 overdue.",
            nextRewardLabel = "You can redeem a reward right now.",
        ),
        rewards = listOf(
            RewardsCatalogItemUiModel(
                id = "dessert-choice",
                title = "Dessert choice",
                description = "Pick the weekend dessert or sweet treat.",
                costLabel = "45 pts",
                availabilityLabel = "Ready",
                canRedeem = true,
                canDelete = false,
            ),
            RewardsCatalogItemUiModel(
                id = "skip-one-chore",
                title = "Skip one chore",
                description = "Trade points for one lighter weeknight handoff.",
                costLabel = "65 pts",
                availabilityLabel = "16 pts left",
                canRedeem = false,
                canDelete = false,
            ),
        ),
        editor = RewardsEditorUiModel(),
    )

    fun onboardingState(
        stepIndex: Int,
        displayName: String = "Erick",
    ): OnboardingUiState = OnboardingViewModel.previewState(
        displayName = displayName,
        currentStepIndex = stepIndex,
    )

    fun accountState(): AccountUiState = AccountUiState(
        isLoading = false,
        displayName = "Erick Sorto",
        email = "erick@example.com",
        signedInMethodLabel = "Google",
        signedInMethodSummary = "Recoverable Google sign-in.",
        householdName = "River House",
        householdRoleLabel = "Owner",
        seatLabel = "Active owner seat",
        seatContextLabel = "4 to 5 members",
        accessSummary = "Premium is shared through River House.",
        premiumStatusLabel = "Household Pro active",
        premiumStatusSummary = "Premium is shared across the household.",
        signOutSummary = "Signs out this device only. Household data stays safe.",
    )

    fun settingsHubState(
        displayName: String = "Erick",
        householdName: String = "River House",
        profileSummary: String = "Keep account, reminders, billing, rewards, and sign-in details aligned from one place.",
    ): SettingsHubUiState = SettingsHubUiState(
        displayName = displayName,
        householdName = householdName,
        activeHouseholdId = "river-house",
        availableHouseholds = listOf(
            SettingsHubHouseholdUiModel(
                id = "river-house",
                name = householdName,
                roleLabel = "Owner",
                premiumLabel = "Pro",
            ),
            SettingsHubHouseholdUiModel(
                id = "lake-house",
                name = "Lake House",
                roleLabel = "Admin",
            ),
        ),
        profileSummary = profileSummary,
        previewCards = DemoContent.previewCards,
    )

    private fun toHomeChore(demo: DemoChore) = com.sortoapps.chorepilot.features.chores.domain.ChoreItem(
        id = demo.id,
        title = demo.title,
        area = demo.area,
        cadence = demo.cadence,
        assignee = demo.assignee,
        assigneeId = demo.assigneeId,
        status = demo.status.toChoreItemStatus(),
        durationLabel = demo.durationLabel,
        dueLabel = demo.dueLabel,
        description = demo.description,
        reminder = demo.reminder,
        tags = demo.tags,
        proofRequired = demo.tags.any { it.contains("Proof", ignoreCase = true) },
        proofPhotoCount = if (demo.status == DemoChoreStatus.PendingProof) 1 else 0,
    )

    private fun toChoreListItem(demo: DemoChore) = ChoreListItemUiModel(
        id = demo.id,
        title = demo.title,
        area = demo.area,
        cadence = demo.cadence,
        assignee = demo.assignee,
        isActive = true,
        isUnassigned = demo.assignee.isBlank(),
        pendingApproval = false,
        assignmentReason = "",
        submitterName = demo.assignee,
        reviewRejectedReason = "",
        completedByName = "",
        dueLabel = demo.dueLabel,
        durationLabel = demo.durationLabel,
        status = demo.status.toChoreItemStatus(),
        tags = demo.tags,
        isAssignedToViewer = demo.assignee.equals("Erick", ignoreCase = true),
        canToggleCompletion = demo.assignee.equals("Erick", ignoreCase = true) || demo.status == DemoChoreStatus.Completed,
    )

    private fun DemoChoreStatus.toChoreItemStatus(): ChoreItemStatus = when (this) {
        DemoChoreStatus.DueSoon -> ChoreItemStatus.DueSoon
        DemoChoreStatus.PendingProof -> ChoreItemStatus.PendingProof
        DemoChoreStatus.Completed -> ChoreItemStatus.Completed
        DemoChoreStatus.Overdue -> ChoreItemStatus.Overdue
        DemoChoreStatus.Scheduled -> ChoreItemStatus.Scheduled
    }
}
