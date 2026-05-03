package com.sortoapps.chorepilot.features.reports.presentation.viewmodel

import com.sortoapps.chorepilot.core.demo.DemoMetric
import com.sortoapps.chorepilot.core.demo.DemoTone
import com.sortoapps.chorepilot.core.platform.VibratorUtil
import com.sortoapps.chorepilot.core.revenuecat.SubscriptionPlanId
import com.sortoapps.chorepilot.core.revenuecat.SubscriptionRepository
import com.sortoapps.chorepilot.core.revenuecat.SubscriptionSnapshot
import com.sortoapps.chorepilot.features.chores.domain.ChoreDraft
import com.sortoapps.chorepilot.features.chores.domain.ChoreItem
import com.sortoapps.chorepilot.features.chores.domain.ChoreItemStatus
import com.sortoapps.chorepilot.features.chores.domain.ChoreRepository
import com.sortoapps.chorepilot.features.chores.domain.ChoreScheduleDay
import com.sortoapps.chorepilot.features.chores.domain.ProofEvidence
import com.sortoapps.chorepilot.features.chores.domain.ProofUploadPayload
import com.sortoapps.chorepilot.features.chores.domain.ProofUploadResult
import com.sortoapps.chorepilot.features.chores.domain.RoomDefinition
import com.sortoapps.chorepilot.features.reports.domain.ReportsBarPoint
import com.sortoapps.chorepilot.features.reports.domain.ReportsFirstFinisher
import com.sortoapps.chorepilot.features.reports.domain.ReportsLeaderboardEntry
import com.sortoapps.chorepilot.features.reports.domain.ReportsLegendItem
import com.sortoapps.chorepilot.features.reports.domain.ReportsNavigationSnapshot
import com.sortoapps.chorepilot.features.reports.domain.ReportsPeriodSelection
import com.sortoapps.chorepilot.features.reports.domain.ReportsPeriodSnapshot
import com.sortoapps.chorepilot.features.reports.domain.ReportsRepository
import com.sortoapps.chorepilot.features.reports.domain.ReportsTaskHighlight
import com.sortoapps.chorepilot.features.reports.presentation.viewmodel.ReportsRange
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun defaultStateOpensOnDaily() = runTest(dispatcher) {
        val viewModel = ReportsViewModel(
            reportsRepository = FakeReportsRepository(),
            choreRepository = FakeChoreRepository(),
            subscriptionRepository = FakeSubscriptionRepository(hasPro = true),
            vibratorUtil = FakeVibratorUtil(),
            computationDispatcher = dispatcher,
        )

        advanceUntilIdle()

        assertEquals(ReportsRange.Daily, viewModel.uiState.value.selectedRange)
        assertEquals("Today's household pulse", viewModel.uiState.value.headline)
        assertEquals(ReportsSurfaceMode.Content, viewModel.uiState.value.selectedSurface)
    }

    @Test
    fun selectingTabsUpdatesPresentationCopyAndBars() = runTest(dispatcher) {
        val viewModel = ReportsViewModel(
            reportsRepository = FakeReportsRepository(),
            choreRepository = FakeChoreRepository(),
            subscriptionRepository = FakeSubscriptionRepository(hasPro = true),
            vibratorUtil = FakeVibratorUtil(),
            computationDispatcher = dispatcher,
        )

        advanceUntilIdle()
        viewModel.onEvent(ReportsUiEvent.RangeSelected(ReportsRange.Monthly))
        advanceUntilIdle()
        assertEquals("Monthly consistency", viewModel.uiState.value.headline)
        assertTrue(viewModel.uiState.value.activeBars.first().label.contains("Week", ignoreCase = true))

        viewModel.onEvent(ReportsUiEvent.RangeSelected(ReportsRange.Yearly))
        advanceUntilIdle()
        assertEquals("Yearly outlook", viewModel.uiState.value.headline)
        assertTrue(viewModel.uiState.value.chartTitle.contains("month", ignoreCase = true))
    }

    @Test
    fun dailyRemainsAvailableWithoutProButLongerRangesStayLocked() = runTest(dispatcher) {
        val viewModel = ReportsViewModel(
            reportsRepository = FakeReportsRepository(),
            choreRepository = FakeChoreRepository(),
            subscriptionRepository = FakeSubscriptionRepository(hasPro = false),
            vibratorUtil = FakeVibratorUtil(),
            computationDispatcher = dispatcher,
        )

        advanceUntilIdle()
        assertEquals(ReportsSurfaceMode.Content, viewModel.uiState.value.selectedSurface)

        viewModel.onEvent(ReportsUiEvent.RangeSelected(ReportsRange.Weekly))
        advanceUntilIdle()
        assertEquals(ReportsSurfaceMode.Locked, viewModel.uiState.value.selectedSurface)
        assertTrue(viewModel.uiState.value.lockTitle.contains("Weekly", ignoreCase = true))

        viewModel.onEvent(ReportsUiEvent.RangeSelected(ReportsRange.Yearly))
        advanceUntilIdle()
        assertEquals(ReportsSurfaceMode.Locked, viewModel.uiState.value.selectedSurface)
        assertTrue(viewModel.uiState.value.lockTitle.contains("Yearly", ignoreCase = true))
    }

    @Test
    fun navigatingPeriodsUpdatesDailyAndWeeklyLabelsCleanly() = runTest(dispatcher) {
        val viewModel = ReportsViewModel(
            reportsRepository = FakeReportsRepository(),
            choreRepository = FakeChoreRepository(),
            subscriptionRepository = FakeSubscriptionRepository(hasPro = true),
            vibratorUtil = FakeVibratorUtil(),
            computationDispatcher = dispatcher,
        )

        advanceUntilIdle()
        assertEquals("Today", viewModel.uiState.value.navigation.title)

        viewModel.onEvent(ReportsUiEvent.PreviousPeriodRequested)
        advanceUntilIdle()
        assertEquals("1 day back", viewModel.uiState.value.navigation.title)
        assertTrue(viewModel.uiState.value.navigation.canGoForward)

        viewModel.onEvent(ReportsUiEvent.RangeSelected(ReportsRange.Weekly))
        advanceUntilIdle()
        assertEquals("This week", viewModel.uiState.value.navigation.title)

        viewModel.onEvent(ReportsUiEvent.PreviousPeriodRequested)
        advanceUntilIdle()
        assertEquals("Week 2 back", viewModel.uiState.value.navigation.title)

        viewModel.onEvent(ReportsUiEvent.NextPeriodRequested)
        advanceUntilIdle()
        assertEquals("This week", viewModel.uiState.value.navigation.title)
    }

    @Test
    fun choreUpdatesRebuildDailySnapshotWithoutManualRefresh() = runTest(dispatcher) {
        val reportsRepository = FakeReportsRepository()
        val choreRepository = FakeChoreRepository()
        val viewModel = ReportsViewModel(
            reportsRepository = reportsRepository,
            choreRepository = choreRepository,
            subscriptionRepository = FakeSubscriptionRepository(hasPro = true),
            vibratorUtil = FakeVibratorUtil(),
            computationDispatcher = dispatcher,
        )

        advanceUntilIdle()
        assertEquals(4, viewModel.uiState.value.activeBars.first().value)
        assertEquals("Summary copy", viewModel.uiState.value.rangeSummary)

        reportsRepository.dailyCompletedCount = 5
        reportsRepository.dailySummary = "5 of 5 chores were completed, with 0 still needing attention."
        choreRepository.emit(
            listOf(
                testChore(id = "done-1", status = ChoreItemStatus.Completed),
            ),
        )
        advanceUntilIdle()

        assertEquals(5, viewModel.uiState.value.activeBars.first().value)
        assertEquals("5 of 5 chores were completed, with 0 still needing attention.", viewModel.uiState.value.rangeSummary)
    }

    @Test
    fun selectingRangeOnlyRequestsThatRangeSnapshot() = runTest(dispatcher) {
        val reportsRepository = FakeReportsRepository()
        val viewModel = ReportsViewModel(
            reportsRepository = reportsRepository,
            choreRepository = FakeChoreRepository(),
            subscriptionRepository = FakeSubscriptionRepository(hasPro = true),
            vibratorUtil = FakeVibratorUtil(),
            computationDispatcher = dispatcher,
        )

        advanceUntilIdle()
        assertEquals(listOf(ReportsRange.Daily), reportsRepository.requestedRanges)

        reportsRepository.requestedRanges.clear()
        viewModel.onEvent(ReportsUiEvent.RangeSelected(ReportsRange.Monthly))
        advanceUntilIdle()

        assertEquals(listOf(ReportsRange.Monthly), reportsRepository.requestedRanges)
    }

    @Test
    fun gainingProUnlocksTheCurrentlySelectedRange() = runTest(dispatcher) {
        val subscriptionRepository = FakeSubscriptionRepository(hasPro = false)
        val viewModel = ReportsViewModel(
            reportsRepository = FakeReportsRepository(),
            choreRepository = FakeChoreRepository(),
            subscriptionRepository = subscriptionRepository,
            vibratorUtil = FakeVibratorUtil(),
            computationDispatcher = dispatcher,
        )

        advanceUntilIdle()
        viewModel.onEvent(ReportsUiEvent.RangeSelected(ReportsRange.Monthly))
        advanceUntilIdle()
        assertEquals(ReportsSurfaceMode.Locked, viewModel.uiState.value.selectedSurface)

        subscriptionRepository.emit(hasPro = true)
        advanceUntilIdle()

        assertEquals(ReportsRange.Monthly, viewModel.uiState.value.selectedRange)
        assertEquals(ReportsSurfaceMode.Content, viewModel.uiState.value.selectedSurface)
    }

    @Test
    fun nonPurchaserMemberStillUnlocksReportsWhenHouseholdHasPro() = runTest(dispatcher) {
        val viewModel = ReportsViewModel(
            reportsRepository = FakeReportsRepository(),
            choreRepository = FakeChoreRepository(),
            subscriptionRepository = FakeSubscriptionRepository(
                hasPro = true,
                purchaserHasPro = false,
                householdHasPro = true,
            ),
            vibratorUtil = FakeVibratorUtil(),
            computationDispatcher = dispatcher,
        )

        advanceUntilIdle()
        viewModel.onEvent(ReportsUiEvent.RangeSelected(ReportsRange.Weekly))
        advanceUntilIdle()

        assertEquals(ReportsSurfaceMode.Content, viewModel.uiState.value.selectedSurface)
        assertTrue(viewModel.uiState.value.hasChorePilotPro)
    }
}

private class FakeReportsRepository : ReportsRepository {
    var dailyCompletedCount: Int = 4
    var dailySummary: String = "Summary copy"
    val requestedRanges = mutableListOf<ReportsRange>()

    override fun getReportsPeriodSnapshot(
        range: ReportsRange,
        selection: ReportsPeriodSelection,
    ): ReportsPeriodSnapshot {
        requestedRanges += range
        return when (range) {
            ReportsRange.Daily -> buildSnapshot(
            headline = if (selection.dailyCursor == 0) "Today's household pulse" else "Past day report",
            navigation = ReportsNavigationSnapshot(
                title = if (selection.dailyCursor == 0) "Today" else "${selection.dailyCursor} day back",
                supportingText = "Friday, Apr 9",
                canGoForward = selection.dailyCursor > 0,
            ),
            chartTitle = "Household leaderboard",
            bars = listOf(
                ReportsBarPoint("Completed", dailyCompletedCount, "$dailyCompletedCount completed", "$dailyCompletedCount/5"),
                ReportsBarPoint("Pending proof", 1, "1 pending proof", "Needs review"),
                ReportsBarPoint("Due soon", 0, "0 due soon", "On track"),
                ReportsBarPoint("Overdue", 0, "0 overdue", "Clear"),
            ),
            firstFinisher = ReportsFirstFinisher(
                memberName = "Erick",
                taskTitle = "Counters reset",
                completedAtLabel = "7:18 AM",
            ),
            leaderboard = listOf(
                ReportsLeaderboardEntry("Erick", "ER", 2, 2, 0, 0, 100, 1, "Clean sweep"),
            ),
            taskHighlights = listOf(
                ReportsTaskHighlight("Emely", "Wash dishes", "Awaiting proof"),
            ),
            )
            ReportsRange.Weekly -> buildSnapshot(
            headline = "Daily momentum",
            navigation = ReportsNavigationSnapshot(
                title = if (selection.weeklyCursor == 0) "This week" else "Week ${selection.weeklyCursor + 1} back",
                supportingText = "Apr 3 to Apr 9",
                canGoForward = selection.weeklyCursor > 0,
            ),
            chartTitle = "Completed chores by day",
            bars = listOf(
                ReportsBarPoint("Mon", 2, "Mon", "Apr 6"),
                ReportsBarPoint("Tue", 3, "Tue", "Apr 7"),
                ReportsBarPoint("Wed", 4, "Wed", "Apr 8"),
            ),
            lockTitle = "Weekly insight stays in Pro",
            )
            ReportsRange.Monthly -> buildSnapshot(
            headline = "Monthly consistency",
            navigation = ReportsNavigationSnapshot(
                title = "April 2026",
                supportingText = "Weekly checkpoints",
                canGoForward = selection.monthlyCursor > 0,
            ),
            chartTitle = "Completed chores by week",
            bars = listOf(
                ReportsBarPoint("Week 1", 12, "Week 1", "Mar 10"),
                ReportsBarPoint("Week 2", 15, "Week 2", "Mar 17"),
            ),
            lockTitle = "Monthly insight stays in Pro",
            )
            ReportsRange.Yearly -> buildSnapshot(
            headline = "Yearly outlook",
            navigation = ReportsNavigationSnapshot(
                title = "2026",
                supportingText = "Month-by-month household follow-through",
                canGoForward = selection.yearlyCursor > 0,
            ),
            chartTitle = "Completed chores by month",
            bars = listOf(
                ReportsBarPoint("Jan", 44, "Jan", "2026"),
                ReportsBarPoint("Feb", 48, "Feb", "2026"),
            ),
            lockTitle = "Yearly insight stays in Pro",
            )
        }
    }

    private fun buildSnapshot(
        headline: String,
        navigation: ReportsNavigationSnapshot,
        chartTitle: String,
        bars: List<ReportsBarPoint>,
        firstFinisher: ReportsFirstFinisher? = null,
        leaderboard: List<ReportsLeaderboardEntry> = emptyList(),
        taskHighlights: List<ReportsTaskHighlight> = emptyList(),
        lockTitle: String = "Daily insight is included",
    ): ReportsPeriodSnapshot = ReportsPeriodSnapshot(
        headline = headline,
        supportingText = "Useful data, clean labels.",
        navigation = navigation,
        rangeSummary = dailySummary,
        chartTitle = chartTitle,
        chartSubtitle = "Chart subtitle",
        metrics = listOf(
            DemoMetric("5", "Scheduled", DemoTone.Primary),
            DemoMetric(dailyCompletedCount.toString(), "Completed", DemoTone.Success),
            DemoMetric("80%", "Completion rate", DemoTone.Secondary),
            DemoMetric("1", "Pending proof", DemoTone.Tertiary),
        ),
        bars = bars,
        firstFinisher = firstFinisher,
        leaderboard = leaderboard,
        taskHighlights = taskHighlights,
        legendItems = listOf(
            ReportsLegendItem("Meaning", "Bars are real counts."),
            ReportsLegendItem("Use", "Read the household rhythm."),
        ),
        emptyTitle = "No data",
        emptyBody = "No data body",
        emptyActionHint = "No data hint",
        lockTitle = lockTitle,
        lockBody = "Locked body",
        lockBenefits = listOf("Benefit 1", "Benefit 2"),
    )
}

private class FakeSubscriptionRepository(
    hasPro: Boolean,
    purchaserHasPro: Boolean = hasPro,
    householdHasPro: Boolean = hasPro,
) : SubscriptionRepository {
    private val snapshotFlow = MutableStateFlow(
        SubscriptionSnapshot(
            purchaserHasChorePilotPro = purchaserHasPro,
            householdHasChorePilotPro = householdHasPro,
            hasChorePilotPro = hasPro,
            isConfigured = true,
            isLoading = false,
        ),
    )
    override val snapshot: StateFlow<SubscriptionSnapshot> = snapshotFlow

    fun emit(
        hasPro: Boolean,
        purchaserHasPro: Boolean = hasPro,
        householdHasPro: Boolean = hasPro,
    ) {
        snapshotFlow.value = snapshotFlow.value.copy(
            purchaserHasChorePilotPro = purchaserHasPro,
            householdHasChorePilotPro = householdHasPro,
            hasChorePilotPro = hasPro,
        )
    }

    override suspend fun refresh() = Unit

    override suspend fun purchasePlan(planId: SubscriptionPlanId): Result<Unit> = Result.success(Unit)

    override suspend fun restorePurchases(): Result<Unit> = Result.success(Unit)

    override fun clearError() = Unit
}

private class FakeChoreRepository(
    initialChores: List<ChoreItem> = emptyList(),
) : ChoreRepository {
    private val choreFlow = MutableStateFlow(initialChores)

    override val chores: StateFlow<List<ChoreItem>> = choreFlow
    override val rooms: StateFlow<List<RoomDefinition>> = MutableStateFlow(emptyList())
    override val householdMembers: StateFlow<List<com.sortoapps.chorepilot.features.chores.domain.HouseholdSchedulingMember>> = MutableStateFlow(emptyList())

    fun emit(updated: List<ChoreItem>) {
        choreFlow.value = updated
    }

    override fun getChores(): List<ChoreItem> = choreFlow.value

    override fun getChore(id: String, day: ChoreScheduleDay?): ChoreItem? = choreFlow.value.firstOrNull { it.id == id }

    override fun getLatestProofForChore(
        choreId: String,
        day: ChoreScheduleDay?,
    ): ProofEvidence? = null

    override fun getDraft(id: String): ChoreDraft? = null

    override fun getScheduledChores(day: ChoreScheduleDay): List<ChoreItem> = choreFlow.value

    override suspend fun refresh() = Unit

    override suspend fun saveChore(draft: ChoreDraft): String = error("Not needed in reports tests")

    override suspend fun deleteChore(choreId: String) = error("Not needed in reports tests")

    override suspend fun toggleCompletion(
        choreId: String,
        viewerName: String,
        allowCompletingForSomeoneElse: Boolean,
        day: ChoreScheduleDay?,
    ) = error("Not needed in reports tests")

    override suspend fun submitProof(
        choreId: String,
        payload: ProofUploadPayload,
        day: ChoreScheduleDay?,
    ): ProofUploadResult = error("Not needed in reports tests")

    override suspend fun approveProof(
        choreId: String,
        day: ChoreScheduleDay?,
    ) = error("Not needed in reports tests")

    override suspend fun rejectProof(
        choreId: String,
        reason: String,
        day: ChoreScheduleDay?,
    ) = error("Not needed in reports tests")

    override suspend fun getAvailabilitySuggestions(days: List<ChoreScheduleDay>) = emptyList<com.sortoapps.chorepilot.features.chores.domain.AvailabilitySuggestion>()

    override suspend fun acceptAvailabilitySuggestion(suggestion: com.sortoapps.chorepilot.features.chores.domain.AvailabilitySuggestion) = Unit

    override suspend fun dismissAvailabilitySuggestion(suggestion: com.sortoapps.chorepilot.features.chores.domain.AvailabilitySuggestion) = Unit
}

private class FakeVibratorUtil : VibratorUtil {
    override fun triggerVibration() = Unit

    override fun triggerVibrationSuccess() = Unit

    override fun cancelVibration() = Unit
}

private fun testChore(
    id: String,
    status: ChoreItemStatus,
): ChoreItem = ChoreItem(
    id = id,
    title = "Kitchen reset",
    area = "Kitchen",
    cadence = "Daily",
    assignee = "Erick",
    assigneeId = "viewer",
    status = status,
    durationLabel = "10 min",
    dueLabel = "Today",
    description = "Reset the room.",
    reminder = "After school",
    tags = listOf("Daily"),
)
