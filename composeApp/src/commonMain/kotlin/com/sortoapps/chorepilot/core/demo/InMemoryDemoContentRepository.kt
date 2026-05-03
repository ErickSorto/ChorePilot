package com.sortoapps.chorepilot.core.demo

import com.sortoapps.chorepilot.core.firebase.HouseholdInviteLifecycleStatus
import com.sortoapps.chorepilot.features.auth.domain.HouseholdRole
import com.sortoapps.chorepilot.features.family.domain.FamilyAvailabilityMode
import com.sortoapps.chorepilot.features.family.domain.FamilyAvailabilitySnapshot
import com.sortoapps.chorepilot.features.family.domain.FamilyInviteSnapshot
import com.sortoapps.chorepilot.features.family.domain.FamilyMemberSnapshot
import com.sortoapps.chorepilot.features.family.domain.FamilyRepository
import com.sortoapps.chorepilot.features.family.domain.FamilyRepositoryState
import com.sortoapps.chorepilot.features.family.domain.FamilySnapshot
import com.sortoapps.chorepilot.features.home.domain.HomeDaySnapshot
import com.sortoapps.chorepilot.features.home.domain.HomeRepository
import com.sortoapps.chorepilot.features.home.domain.HomeRepositoryState
import com.sortoapps.chorepilot.features.home.domain.HomeSnapshot
import com.sortoapps.chorepilot.features.reports.domain.ReportsRepository
import com.sortoapps.chorepilot.features.reports.domain.ReportsBarPoint
import com.sortoapps.chorepilot.features.reports.domain.ReportsFirstFinisher
import com.sortoapps.chorepilot.features.reports.domain.ReportsLeaderboardEntry
import com.sortoapps.chorepilot.features.reports.domain.ReportsLegendItem
import com.sortoapps.chorepilot.features.reports.domain.ReportsNavigationSnapshot
import com.sortoapps.chorepilot.features.reports.domain.ReportsPeriodSelection
import com.sortoapps.chorepilot.features.reports.domain.ReportsPeriodSnapshot
import com.sortoapps.chorepilot.features.reports.domain.ReportsSnapshot
import com.sortoapps.chorepilot.features.reports.domain.ReportsTaskHighlight
import com.sortoapps.chorepilot.features.reports.presentation.viewmodel.ReportsRange
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemoryDemoContentRepository :
    HomeRepository,
    FamilyRepository,
    ReportsRepository {
    private val _homeState = MutableStateFlow(
        HomeRepositoryState(
            isLoading = false,
            snapshot = demoHomeSnapshot(),
        ),
    )
    override val state: StateFlow<HomeRepositoryState> = _homeState.asStateFlow()

    private val _familyState = MutableStateFlow(
        FamilyRepositoryState(
            isLoading = false,
            snapshot = demoFamilySnapshot(),
        ),
    )
    override val familyState: StateFlow<FamilyRepositoryState> = _familyState.asStateFlow()

    fun getHomeSnapshot(): HomeSnapshot = demoHomeSnapshot()

    fun getFamilySnapshot(): FamilySnapshot = _familyState.value.snapshot

    override suspend fun refresh() = Unit

    private fun demoHomeSnapshot(): HomeSnapshot = HomeSnapshot(
        householdName = DemoContent.householdName,
        viewerName = "Erick",
        viewerId = "erick",
        viewerRole = HouseholdRole.Owner,
        headline = "Evening, Erick",
        supportingText = "Your sanctuary is waiting",
        days = DemoContent.days.mapIndexed { index, day ->
            HomeDaySnapshot(
                id = day.id,
                label = day.label,
                shortLabel = day.shortLabel,
                supportingText = day.completionLabel,
                dayOffset = index - DemoContent.days.indexOfFirst { it.isToday }.coerceAtLeast(0),
                dateKey = "2026-04-${(12 + index).toString().padStart(2, '0')}",
                isToday = day.isToday,
            )
        },
        metrics = DemoContent.todayMetrics,
        focusChores = DemoContent.chores,
    )

    private fun demoFamilySnapshot(): FamilySnapshot = FamilySnapshot(
        householdName = DemoContent.householdName,
        pulseLabel = "4 active members",
        householdBadge = "Balanced",
        ownerUserId = DemoContent.members.firstOrNull()?.id.orEmpty(),
        activeSeatCount = 4,
        summaryMetrics = listOf(
            DemoMetric("4", "Seats", DemoTone.Primary),
            DemoMetric("Active", "Invite", DemoTone.Secondary),
            DemoMetric("2", "Managers", DemoTone.Warning),
            DemoMetric("1", "Away", DemoTone.Tertiary),
        ),
        members = DemoContent.members.mapIndexed { index, member ->
            FamilyMemberSnapshot(
                id = member.id,
                name = member.name,
                role = when (member.role) {
                    "Owner" -> HouseholdRole.Owner
                    "Admin" -> HouseholdRole.Admin
                    else -> HouseholdRole.Member
                },
                statusLabel = member.statusLabel,
                activeChoresLabel = member.activeChoresLabel,
                householdImpact = "2 chores and 1 proof review may shift while ${member.name} is away.",
                avatarSeed = member.avatarSeed,
                tone = member.tone,
                assignedChoreCount = 2 - (index % 2),
                proofReviewCount = index % 2,
                isActiveSeat = true,
                availability = if (index == 1) {
                    FamilyAvailabilitySnapshot(
                        mode = FamilyAvailabilityMode.GoneMode,
                        startDate = "2026-04-24",
                        endDate = "2026-04-28",
                        statusLabel = "Gone mode active",
                        summary = "Away during Apr 24 to Apr 28.",
                    )
                } else {
                    null
                },
            )
        },
        activeInvite = FamilyInviteSnapshot(
            code = "RIVER-82",
            role = HouseholdRole.Member,
            lifecycleStatus = HouseholdInviteLifecycleStatus.Active,
            statusLabel = "Active now",
            supportingText = "Share this code to add someone to River House.",
            generatedAtLabel = "Created today",
            expiryLabel = "Expires Apr 19",
            usageLabel = "Unlimited uses",
            shareText = "Join River House on ChorePilot with code RIVER-82.",
            isJoinable = true,
            seatImpactText = "This uses the next household seat for chores, proof, and reminders.",
        ),
    )

    override suspend fun generateInvite(
        role: HouseholdRole,
    ): FamilyInviteSnapshot {
        val code = if (role == HouseholdRole.Admin) "ADMIN-42" else "RIVER-82"
        val invite = demoFamilySnapshot().activeInvite.copy(
            code = code,
            role = role,
            lifecycleStatus = HouseholdInviteLifecycleStatus.Active,
            supportingText = "Share this code to add a ${role.name.lowercase()} to River House.",
            expiryLabel = "Expires Apr 19",
            usageLabel = "Unlimited uses",
            shareText = "Join River House on ChorePilot with code $code.",
            isJoinable = true,
            seatImpactText = if (role == HouseholdRole.Admin) {
                "This fills an admin seat for chores, approvals, and settings help."
            } else {
                "This uses the next household seat for chores, proof, and reminders."
            },
        )
        _familyState.value = FamilyRepositoryState(
            isLoading = false,
            snapshot = demoFamilySnapshot().copy(activeInvite = invite),
        )
        return invite
    }

    override suspend fun cancelInvite() {
        _familyState.value = _familyState.value.copy(
            snapshot = _familyState.value.snapshot.copy(
                activeInvite = _familyState.value.snapshot.activeInvite.copy(
                    lifecycleStatus = HouseholdInviteLifecycleStatus.Revoked,
                    statusLabel = "Revoked",
                    supportingText = "This code was revoked and won't let anyone join.",
                    shareText = "",
                    isJoinable = false,
                ),
            ),
        )
    }

    override suspend fun rotateInvite(role: HouseholdRole): FamilyInviteSnapshot = generateInvite(role)

    override suspend fun changeMemberRole(memberId: String, newRole: HouseholdRole) {
        _familyState.value = _familyState.value.copy(
            snapshot = _familyState.value.snapshot.copy(
                ownerUserId = if (newRole == HouseholdRole.Owner) memberId else _familyState.value.snapshot.ownerUserId,
                members = _familyState.value.snapshot.members.map { member ->
                    when {
                        newRole == HouseholdRole.Owner && member.id == _familyState.value.snapshot.ownerUserId ->
                            member.copy(role = HouseholdRole.Admin)

                        member.id == memberId -> member.copy(role = newRole)
                        else -> member
                    }
                },
            ),
        )
    }

    override suspend fun deactivateMember(memberId: String) {
        _familyState.value = _familyState.value.copy(
            snapshot = _familyState.value.snapshot.copy(
                activeSeatCount = (_familyState.value.snapshot.activeSeatCount - 1).coerceAtLeast(0),
                members = _familyState.value.snapshot.members.map { member ->
                    if (member.id != memberId) {
                        member
                    } else {
                        member.copy(
                            statusLabel = "Inactive seat",
                            activeChoresLabel = "This seat is paused and won't take new chores until it returns.",
                            householdImpact = "This seat is outside the active household flow until it is reactivated or removed.",
                            isActiveSeat = false,
                            availability = null,
                        )
                    }
                },
            ),
        )
    }

    override suspend fun removeMember(memberId: String) {
        deactivateMember(memberId)
    }

    override suspend fun leaveHousehold() {
        _familyState.value = FamilyRepositoryState(
            isLoading = false,
            snapshot = FamilySnapshot(
                householdName = "Your household",
                pulseLabel = "No active members yet",
                householdBadge = "Starting fresh",
                summaryMetrics = listOf(
                    DemoMetric("0", "Seats", DemoTone.Primary),
                    DemoMetric("None", "Invite", DemoTone.Secondary),
                    DemoMetric("1", "Managers", DemoTone.Warning),
                    DemoMetric("0", "Away", DemoTone.Tertiary),
                ),
            ),
        )
    }

    override suspend fun deleteHousehold() {
        leaveHousehold()
    }

    override suspend fun updateAvailability(
        memberId: String,
        mode: FamilyAvailabilityMode,
        startDate: String,
        endDate: String,
    ) {
        _familyState.value = FamilyRepositoryState(
            isLoading = false,
            snapshot = _familyState.value.snapshot.copy(
                members = _familyState.value.snapshot.members.map { member ->
                    if (member.id != memberId) {
                        member
                    } else {
                        member.copy(
                            statusLabel = if (mode == FamilyAvailabilityMode.DayOff) "Day off scheduled" else "Gone mode active",
                            activeChoresLabel = "Away during Apr 24 to Apr 28.",
                            availability = FamilyAvailabilitySnapshot(
                                mode = mode,
                                startDate = startDate,
                                endDate = endDate,
                                statusLabel = if (mode == FamilyAvailabilityMode.DayOff) "Day off scheduled" else "Gone mode active",
                                summary = "Away during Apr 24 to Apr 28.",
                            ),
                        )
                    }
                },
            ),
        )
    }

    override suspend fun clearAvailability(memberId: String) {
        _familyState.value = FamilyRepositoryState(
            isLoading = false,
            snapshot = _familyState.value.snapshot.copy(
                members = _familyState.value.snapshot.members.map { member ->
                    if (member.id != memberId) {
                        member
                    } else {
                        member.copy(
                            statusLabel = "On track",
                            activeChoresLabel = "Ready for chores and approvals.",
                            availability = null,
                        )
                    }
                },
            ),
        )
    }

    override fun getReportsPeriodSnapshot(
        range: ReportsRange,
        selection: ReportsPeriodSelection,
    ): ReportsPeriodSnapshot {
        val dailyCursor = selection.dailyCursor
        val weeklyCursor = selection.weeklyCursor
        val monthlyCursor = selection.monthlyCursor
        val yearlyCursor = selection.yearlyCursor
        val dailyMetrics = listOf(
            DemoMetric("7", "Scheduled", DemoTone.Primary),
            DemoMetric("6", "Completed", DemoTone.Success),
            DemoMetric("86%", "Completion rate", DemoTone.Secondary),
            DemoMetric("1", "Pending proof", DemoTone.Tertiary),
        )
        val weeklyMetrics = listOf(
            DemoMetric("31", "Scheduled", DemoTone.Primary),
            DemoMetric("27", "Completed", DemoTone.Success),
            DemoMetric("87%", "Completion rate", DemoTone.Secondary),
            DemoMetric("3", "Overdue", DemoTone.Warning),
        )
        val weeklyBars = listOf(
            ReportsBarPoint("Mon", 3, "Mon: 3 of 5 chores completed", "Apr 6"),
            ReportsBarPoint("Tue", 5, "Tue: 5 of 6 chores completed", "Apr 7"),
            ReportsBarPoint("Wed", 4, "Wed: 4 of 5 chores completed", "Apr 8"),
            ReportsBarPoint("Thu", 6, "Thu: 6 of 6 chores completed", "Apr 9"),
            ReportsBarPoint("Fri", 5, "Fri: 5 of 6 chores completed", "Apr 10"),
            ReportsBarPoint("Sat", 4, "Sat: 4 of 5 chores completed", "Apr 11"),
            ReportsBarPoint("Sun", 3, "Sun: 3 of 4 chores completed", "Apr 12"),
        )
        val monthlyBars = listOf(
            ReportsBarPoint("Week 1", 18, "Week 1: 18 of 23 chores completed", "Mar 10"),
            ReportsBarPoint("Week 2", 20, "Week 2: 20 of 24 chores completed", "Mar 17"),
            ReportsBarPoint("Week 3", 21, "Week 3: 21 of 25 chores completed", "Mar 24"),
            ReportsBarPoint("Week 4", 23, "Week 4: 23 of 26 chores completed", "Mar 31"),
            ReportsBarPoint("Week 5", 24, "Week 5: 24 of 27 chores completed", "Apr 7"),
        )
        val yearlyBars = listOf(
            ReportsBarPoint("May", 54, "May: 54 of 65 chores completed", "2025"),
            ReportsBarPoint("Jun", 57, "Jun: 57 of 67 chores completed", "2025"),
            ReportsBarPoint("Jul", 63, "Jul: 63 of 71 chores completed", "2025"),
            ReportsBarPoint("Aug", 59, "Aug: 59 of 69 chores completed", "2025"),
            ReportsBarPoint("Sep", 61, "Sep: 61 of 72 chores completed", "2025"),
            ReportsBarPoint("Oct", 65, "Oct: 65 of 74 chores completed", "2025"),
            ReportsBarPoint("Nov", 60, "Nov: 60 of 73 chores completed", "2025"),
            ReportsBarPoint("Dec", 58, "Dec: 58 of 70 chores completed", "2025"),
            ReportsBarPoint("Jan", 62, "Jan: 62 of 75 chores completed", "2026"),
            ReportsBarPoint("Feb", 64, "Feb: 64 of 74 chores completed", "2026"),
            ReportsBarPoint("Mar", 66, "Mar: 66 of 77 chores completed", "2026"),
            ReportsBarPoint("Apr", 29, "Apr: 29 of 33 chores completed", "2026"),
        )
        val legendItems = listOf(
            ReportsLegendItem(
                title = "What the columns mean",
                body = "Column height reflects completed chores, while the marker text shows completed versus scheduled work.",
            ),
            ReportsLegendItem(
                title = "How to use this view",
                body = "Look for repeat dips and slower proof review windows before they become a household pattern.",
            ),
        )
        val daily = ReportsPeriodSnapshot(
                headline = if (dailyCursor == 0) "Today's household pulse" else "Past day report",
                supportingText = "A leaderboard-style read on the selected day.",
                navigation = ReportsNavigationSnapshot(
                    title = if (dailyCursor == 0) "Today" else "${dailyCursor} day${if (dailyCursor == 1) "" else "s"} back",
                    supportingText = if (dailyCursor == 0) "Friday, Apr 9" else "Friday snapshot",
                    canGoForward = dailyCursor > 0,
                ),
                rangeSummary = "6 of 7 scheduled chores are already completed today.",
                chartTitle = "Household leaderboard",
                chartSubtitle = "See who closed their work and what still needs attention.",
                metrics = dailyMetrics,
                bars = listOf(
                    ReportsBarPoint("Completed", 6, "6 chores completed today", "6/7"),
                    ReportsBarPoint("Pending proof", 1, "1 chore is waiting on proof review", "Needs review"),
                    ReportsBarPoint("Due soon", 0, "No chores are still open today", "On track"),
                    ReportsBarPoint("Overdue", 0, "Nothing is overdue today", "Clear"),
                ),
                firstFinisher = ReportsFirstFinisher(
                    memberName = "Erick",
                    taskTitle = "Counters reset",
                    completedAtLabel = "7:18 AM",
                ),
                leaderboard = listOf(
                    ReportsLeaderboardEntry("Erick", "ER", 2, 2, 0, 0, 100, 1, "Clean sweep"),
                    ReportsLeaderboardEntry("Emely", "EM", 2, 3, 1, 0, 66, 2, "1 awaiting proof"),
                    ReportsLeaderboardEntry("Brianna", "BR", 1, 1, 0, 0, 100, 3, "Closed out"),
                    ReportsLeaderboardEntry("Brayan", "BY", 1, 1, 0, 0, 100, 4, "Closed out"),
                    ReportsLeaderboardEntry("Kelvin", "KE", 0, 1, 0, 1, 0, 5, "1 overdue"),
                ),
                taskHighlights = listOf(
                    ReportsTaskHighlight("Kelvin", "Clean toilet", "Overdue"),
                    ReportsTaskHighlight("Emely", "Wash dishes", "Awaiting proof"),
                ),
                legendItems = legendItems,
                emptyTitle = "Nothing is scheduled today",
                emptyBody = "Once chores land on today's schedule, this chart will split them into clean status buckets.",
                emptyActionHint = "Add a few chores and the daily chart will wake up.",
                lockTitle = "Daily insight is included",
                lockBody = "The daily view is always available.",
                lockBenefits = emptyList(),
            )
        val weekly = ReportsPeriodSnapshot(
                headline = "Daily momentum",
                supportingText = "See which days actually carried the household this week.",
                navigation = ReportsNavigationSnapshot(
                    title = if (weeklyCursor == 0) "This week" else "Week ${weeklyCursor + 1} back",
                    supportingText = "Apr 3 to Apr 9",
                    canGoForward = weeklyCursor > 0,
                ),
                rangeSummary = "Thu carried the strongest follow-through with 6 completed chores out of 6 scheduled.",
                chartTitle = "Completed chores by day",
                chartSubtitle = "Each column shows completed chores. Markers show completed versus scheduled work.",
                metrics = weeklyMetrics,
                bars = weeklyBars,
                legendItems = legendItems,
                emptyTitle = "No weekly activity yet",
                emptyBody = "Complete a few chores across the week and this view will turn into a real trend.",
                emptyActionHint = "Use the weekly tab once a few days of history are in place.",
                lockTitle = "Weekly insight stays in Pro",
                lockBody = "Compare day-by-day completion, proof review, and missed work before the week gets away from the household.",
                lockBenefits = listOf("See your strongest days", "Catch proof slowdowns", "Rebalance earlier"),
            )
        val monthly = ReportsPeriodSnapshot(
                headline = "Monthly consistency",
                supportingText = "Roll the last few weeks together to see whether routines are settling in.",
                navigation = ReportsNavigationSnapshot(
                    title = if (monthlyCursor == 0) "April 2026" else "March 2026",
                    supportingText = "Weekly checkpoints inside the month",
                    canGoForward = monthlyCursor > 0,
                ),
                rangeSummary = "Week 5 carried the strongest follow-through with 24 completed chores out of 27 scheduled.",
                chartTitle = "Completed chores by week",
                chartSubtitle = "Each column bundles a rolling week of household work.",
                metrics = weeklyMetrics,
                bars = monthlyBars,
                legendItems = legendItems,
                emptyTitle = "No monthly pattern yet",
                emptyBody = "Keep chores active for a few weeks and this view will start to show a household rhythm.",
                emptyActionHint = "A couple of steady weeks is enough for this chart to become useful.",
                lockTitle = "Monthly insight stays in Pro",
                lockBody = "Zoom out from individual days and see whether the household rhythm is holding across the last few weeks.",
                lockBenefits = listOf("See rolling consistency", "Track overdue work", "Spot proof-review drag"),
            )
        val yearly = ReportsPeriodSnapshot(
                headline = "Yearly outlook",
                supportingText = "A clean month-by-month read on the bigger household pattern.",
                navigation = ReportsNavigationSnapshot(
                    title = if (yearlyCursor == 0) "2026" else "2025",
                    supportingText = "Month-by-month household follow-through",
                    canGoForward = yearlyCursor > 0,
                ),
                rangeSummary = "Mar carried the strongest follow-through with 66 completed chores out of 77 scheduled.",
                chartTitle = "Completed chores by month",
                chartSubtitle = "Each column represents one month of logged household work.",
                metrics = weeklyMetrics,
                bars = yearlyBars,
                legendItems = legendItems,
                emptyTitle = "No yearly trend yet",
                emptyBody = "Once the household builds more history, this view becomes a useful longer-range trend line.",
                emptyActionHint = "Keep the weekly flow healthy first, then use this tab for the bigger story.",
                lockTitle = "Yearly insight stays in Pro",
                lockBody = "Watch the bigger household pattern over time, including which months hold steady and which ones slip.",
                lockBenefits = listOf("See month-by-month completion", "Track longer-term shifts", "Understand fragile seasons"),
            )
        return when (range) {
            ReportsRange.Daily -> daily
            ReportsRange.Weekly -> weekly
            ReportsRange.Monthly -> monthly
            ReportsRange.Yearly -> yearly
        }
    }
}
