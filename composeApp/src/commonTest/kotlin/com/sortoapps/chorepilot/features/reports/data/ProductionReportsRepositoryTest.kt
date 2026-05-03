package com.sortoapps.chorepilot.features.reports.data

import com.sortoapps.chorepilot.features.chores.domain.ChoreDraft
import com.sortoapps.chorepilot.features.chores.domain.ChoreItem
import com.sortoapps.chorepilot.features.chores.domain.ChoreItemStatus
import com.sortoapps.chorepilot.features.chores.domain.ChoreRepository
import com.sortoapps.chorepilot.features.chores.domain.ChoreScheduleDay
import com.sortoapps.chorepilot.features.chores.domain.ProofEvidence
import com.sortoapps.chorepilot.features.chores.domain.ProofUploadPayload
import com.sortoapps.chorepilot.features.chores.domain.ProofUploadResult
import com.sortoapps.chorepilot.features.chores.domain.RoomDefinition
import com.sortoapps.chorepilot.features.chores.domain.scheduleDateKey
import com.sortoapps.chorepilot.features.reports.domain.ReportsPeriodSelection
import com.sortoapps.chorepilot.features.reports.presentation.viewmodel.ReportsRange
import com.sortoapps.chorepilot.features.settings.domain.SettingsRepository
import com.sortoapps.chorepilot.features.settings.domain.SettingsSnapshot
import com.sortoapps.chorepilot.features.settings.domain.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

class ProductionReportsRepositoryTest {
    @Test
    fun dailySnapshotCountsEachStatusBucket() {
        val todayKey = scheduleDateKey(dayOffset = 0)
        val repository = ProductionReportsRepository(
            FakeChoreRepository(
                scheduledByDateKey = mapOf(
                    todayKey to listOf(
                        chore(id = "c1", status = ChoreItemStatus.Completed),
                        chore(id = "c2", status = ChoreItemStatus.PendingProof, proofRequired = true),
                        chore(id = "c3", status = ChoreItemStatus.Scheduled),
                        chore(id = "c4", status = ChoreItemStatus.DueSoon),
                        chore(id = "c5", status = ChoreItemStatus.Overdue),
                    ),
                ),
            ),
            FakeSettingsRepository(),
        )

        val snapshot = repository.getReportsSnapshot()

        assertEquals(
            listOf("Completed", "Pending proof", "Open", "Overdue"),
            snapshot.daily.bars.map { it.label },
        )
        assertEquals(listOf(1, 1, 2, 1), snapshot.daily.bars.map { it.value })
        assertEquals("5", snapshot.daily.metrics.metricNumber("Scheduled"))
        assertEquals("1", snapshot.daily.metrics.metricNumber("Completed"))
        assertEquals("20%", snapshot.daily.metrics.metricNumber("Completion rate"))
        assertEquals("1", snapshot.daily.metrics.metricNumber("Overdue"))
        assertEquals("Today", snapshot.daily.navigation.title)
        assertEquals("M", snapshot.daily.leaderboard.first().avatarSeed)
        assertTrue(snapshot.daily.taskHighlights.any { it.taskTitle.isNotBlank() })
        assertTrue(snapshot.daily.bars.first().markerLabel.contains("completed", ignoreCase = true))
    }

    @Test
    fun weeklySnapshotBuildsSevenLabeledBarsFromRecentDays() {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val sunday = sundayStartForWeek(today)
        val repository = ProductionReportsRepository(
            FakeChoreRepository(
                scheduledByDateKey = mapOf(
                    sunday.toString() to listOf(
                        chore(id = "w1", status = ChoreItemStatus.Completed),
                        chore(id = "w2", status = ChoreItemStatus.Scheduled),
                    ),
                    sunday.plus(DatePeriod(days = 5)).toString() to listOf(
                        chore(id = "w3", status = ChoreItemStatus.Completed),
                        chore(id = "w4", status = ChoreItemStatus.Completed),
                        chore(id = "w5", status = ChoreItemStatus.Overdue),
                    ),
                    sunday.plus(DatePeriod(days = 6)).toString() to listOf(
                        chore(id = "w6", status = ChoreItemStatus.PendingProof, proofRequired = true),
                    ),
                ),
            ),
            FakeSettingsRepository(),
        )

        val snapshot = repository.getReportsSnapshot()

        assertEquals(listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"), snapshot.weekly.bars.map { it.label })
        assertEquals(7, snapshot.weekly.bars.size)
        assertEquals(listOf(1, 0, 0, 0, 0, 2, 0), snapshot.weekly.bars.map { it.value })
        assertEquals("6", snapshot.weekly.metrics.metricNumber("Scheduled"))
        assertEquals("3", snapshot.weekly.metrics.metricNumber("Completed"))
        assertEquals("50%", snapshot.weekly.metrics.metricNumber("Completion rate"))
        assertEquals("1", snapshot.weekly.metrics.metricNumber("Overdue"))
        assertTrue(snapshot.weekly.bars[5].markerLabel.contains("2 of 3 chores completed"))
    }

    @Test
    fun weeklySnapshotStartsOnMondayWhenSettingEnabled() {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val monday = mondayStartForWeek(today)
        val repository = ProductionReportsRepository(
            FakeChoreRepository(
                scheduledByDateKey = mapOf(
                    monday.toString() to listOf(
                        chore(id = "m1", status = ChoreItemStatus.Completed),
                    ),
                    monday.plus(DatePeriod(days = 6)).toString() to listOf(
                        chore(id = "m2", status = ChoreItemStatus.Completed),
                        chore(id = "m3", status = ChoreItemStatus.Overdue),
                    ),
                ),
            ),
            FakeSettingsRepository(SettingsSnapshot(weekStartsOnMonday = true)),
        )

        val snapshot = repository.getReportsSnapshot()

        assertEquals(listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"), snapshot.weekly.bars.map { it.label })
        assertTrue(snapshot.weekly.navigation.supportingText.startsWith(readableMonthDay(monday)))
        assertTrue(snapshot.weekly.navigation.supportingText.endsWith(readableMonthDay(monday.plus(DatePeriod(days = 6)))))
    }

    @Test
    fun monthlyAndYearlySnapshotsRollUpCompletedCountsIntoUsefulBuckets() {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val previousMonthDate = today.plus(DatePeriod(months = -1))
        val repository = ProductionReportsRepository(
            FakeChoreRepository(
                scheduledByDateKey = mapOf(
                    scheduleDateKey(dayOffset = 0) to listOf(
                        chore(id = "m1", status = ChoreItemStatus.Completed),
                        chore(id = "m2", status = ChoreItemStatus.Overdue),
                    ),
                    scheduleDateKey(dayOffset = -2) to listOf(
                        chore(id = "m3", status = ChoreItemStatus.Completed),
                    ),
                    scheduleDateKey(dayOffset = -8) to listOf(
                        chore(id = "m4", status = ChoreItemStatus.Completed),
                    ),
                    previousMonthDate.toString() to listOf(
                        chore(id = "y1", status = ChoreItemStatus.Completed),
                    ),
                ),
            ),
            FakeSettingsRepository(),
        )

        val snapshot = repository.getReportsSnapshot()

        val expectedMonthlyBuckets = IntArray(4)
        expectedMonthlyBuckets[monthlyBucketIndex(today.day)] += 1
        expectedMonthlyBuckets[monthlyBucketIndex(today.day)] += 0
        expectedMonthlyBuckets[monthlyBucketIndex(today.plus(DatePeriod(days = -2)).day)] += 1
        if (today.plus(DatePeriod(days = -8)).month == today.month) {
            expectedMonthlyBuckets[monthlyBucketIndex(today.plus(DatePeriod(days = -8)).day)] += 1
        }

        assertEquals(4, snapshot.monthly.bars.size)
        assertEquals(expectedMonthlyBuckets.toList(), snapshot.monthly.bars.map { it.value })
        assertTrue(snapshot.monthly.bars.any { it.markerLabel.contains("chores completed") })

        assertEquals(12, snapshot.yearly.bars.size)
        val previousMonthIndex = previousMonthDate.month.ordinal
        val currentMonthIndex = today.month.ordinal
        assertEquals(monthAbbreviation(previousMonthDate.monthNumber), snapshot.yearly.bars[previousMonthIndex].label)
        assertEquals(1, snapshot.yearly.bars[previousMonthIndex].value)
        assertEquals(monthAbbreviation(today.monthNumber), snapshot.yearly.bars[currentMonthIndex].label)
        assertEquals(3, snapshot.yearly.bars[currentMonthIndex].value)
        assertTrue(snapshot.yearly.bars[currentMonthIndex].markerLabel.contains("3 of 4 chores completed"))
    }

    @Test
    fun dailySnapshotUsesEarliestCompletionForFirstFinisher() {
        val todayKey = scheduleDateKey(dayOffset = 0)
        val repository = ProductionReportsRepository(
            FakeChoreRepository(
                scheduledByDateKey = mapOf(
                    todayKey to listOf(
                        chore(
                            id = "first",
                            title = "Counters reset",
                            assignee = "Erick",
                            status = ChoreItemStatus.Completed,
                            completedAtEpochMs = 1_000L,
                        ),
                        chore(
                            id = "second",
                            title = "Trash run",
                            assignee = "Emely",
                            completedByName = "Emely",
                            status = ChoreItemStatus.Completed,
                            completedAtEpochMs = 2_000L,
                        ),
                    ),
                ),
            ),
            FakeSettingsRepository(),
        )

        val snapshot = repository.getReportsSnapshot()

        assertEquals("Erick", snapshot.daily.firstFinisher?.memberName)
        assertEquals("Counters reset", snapshot.daily.firstFinisher?.taskTitle)
        assertTrue(snapshot.daily.firstFinisher?.completedAtLabel?.isNotBlank() == true)
    }

    @Test
    fun periodSnapshotOnlyBuildsRequestedRange() {
        val choreRepository = CountingChoreRepository()
        val repository = ProductionReportsRepository(
            choreRepository,
            FakeSettingsRepository(),
        )

        repository.getReportsPeriodSnapshot(
            range = ReportsRange.Daily,
            selection = ReportsPeriodSelection(),
        )

        assertEquals(1, choreRepository.scheduledRequestCount)
    }
}

private fun monthlyBucketIndex(day: Int): Int = ((day - 1) / 7).coerceAtMost(3)

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

private fun sundayStartForWeek(date: LocalDate): LocalDate {
    val sundayStartIndex = when (date.dayOfWeek) {
        DayOfWeek.SUNDAY -> 0
        DayOfWeek.MONDAY -> 1
        DayOfWeek.TUESDAY -> 2
        DayOfWeek.WEDNESDAY -> 3
        DayOfWeek.THURSDAY -> 4
        DayOfWeek.FRIDAY -> 5
        DayOfWeek.SATURDAY -> 6
    }
    return date.plus(DatePeriod(days = -sundayStartIndex))
}

private fun mondayStartForWeek(date: LocalDate): LocalDate {
    val mondayStartIndex = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> 0
        DayOfWeek.TUESDAY -> 1
        DayOfWeek.WEDNESDAY -> 2
        DayOfWeek.THURSDAY -> 3
        DayOfWeek.FRIDAY -> 4
        DayOfWeek.SATURDAY -> 5
        DayOfWeek.SUNDAY -> 6
    }
    return date.plus(DatePeriod(days = -mondayStartIndex))
}

private fun readableMonthDay(date: LocalDate): String = "${monthAbbreviation(date.monthNumber)} ${date.day}"

private class FakeSettingsRepository(
    initial: SettingsSnapshot = SettingsSnapshot(),
) : SettingsRepository {
    override val settings: StateFlow<SettingsSnapshot> = MutableStateFlow(initial)

    override suspend fun updateThemeMode(themeMode: ThemeMode) = Unit

    override suspend fun updateEditorialWarmth(enabled: Boolean) = Unit

    override suspend fun updateNotifications(enabled: Boolean) = Unit

    override suspend fun updateWeekStartsOnMonday(enabled: Boolean) = Unit

    override suspend fun updateAutoAssignSuggestions(enabled: Boolean) = Unit

    override suspend fun updateLargerText(enabled: Boolean) = Unit

    override suspend fun updateAlwaysShowLabels(enabled: Boolean) = Unit
}

private class FakeChoreRepository(
    private val scheduledByDateKey: Map<String, List<ChoreItem>>,
) : ChoreRepository {
    override val chores: StateFlow<List<ChoreItem>> = MutableStateFlow(scheduledByDateKey.values.flatten())
    override val rooms: StateFlow<List<RoomDefinition>> = MutableStateFlow(emptyList())
    override val householdMembers: StateFlow<List<com.sortoapps.chorepilot.features.chores.domain.HouseholdSchedulingMember>> = MutableStateFlow(emptyList())

    override fun getChores(): List<ChoreItem> = chores.value

    override fun getChore(id: String, day: ChoreScheduleDay?): ChoreItem? = chores.value.find { it.id == id }

    override fun getLatestProofForChore(
        choreId: String,
        day: ChoreScheduleDay?,
    ): ProofEvidence? = null

    override fun getDraft(id: String): ChoreDraft? = null

    override fun getScheduledChores(day: ChoreScheduleDay): List<ChoreItem> = scheduledByDateKey[day.dateKey].orEmpty()

    override suspend fun refresh() = Unit

    override suspend fun saveChore(draft: ChoreDraft): String = error("Not used in report aggregation tests")

    override suspend fun deleteChore(choreId: String) = error("Not used in report aggregation tests")

    override suspend fun toggleCompletion(
        choreId: String,
        viewerName: String,
        allowCompletingForSomeoneElse: Boolean,
        day: ChoreScheduleDay?,
    ) = error("Not used in report aggregation tests")

    override suspend fun submitProof(
        choreId: String,
        payload: ProofUploadPayload,
        day: ChoreScheduleDay?,
    ): ProofUploadResult = error("Not used in report aggregation tests")

    override suspend fun approveProof(
        choreId: String,
        day: ChoreScheduleDay?,
    ) = error("Not used in report aggregation tests")

    override suspend fun rejectProof(
        choreId: String,
        reason: String,
        day: ChoreScheduleDay?,
    ) = error("Not used in report aggregation tests")

    override suspend fun getAvailabilitySuggestions(days: List<ChoreScheduleDay>) = emptyList<com.sortoapps.chorepilot.features.chores.domain.AvailabilitySuggestion>()

    override suspend fun acceptAvailabilitySuggestion(suggestion: com.sortoapps.chorepilot.features.chores.domain.AvailabilitySuggestion) = Unit

    override suspend fun dismissAvailabilitySuggestion(suggestion: com.sortoapps.chorepilot.features.chores.domain.AvailabilitySuggestion) = Unit
}

private class CountingChoreRepository : ChoreRepository {
    var scheduledRequestCount: Int = 0

    override val chores: StateFlow<List<ChoreItem>> = MutableStateFlow(emptyList())
    override val rooms: StateFlow<List<RoomDefinition>> = MutableStateFlow(emptyList())
    override val householdMembers: StateFlow<List<com.sortoapps.chorepilot.features.chores.domain.HouseholdSchedulingMember>> = MutableStateFlow(emptyList())

    override fun getChores(): List<ChoreItem> = emptyList()

    override fun getChore(id: String, day: ChoreScheduleDay?): ChoreItem? = null

    override fun getLatestProofForChore(
        choreId: String,
        day: ChoreScheduleDay?,
    ): ProofEvidence? = null

    override fun getDraft(id: String): ChoreDraft? = null

    override fun getScheduledChores(day: ChoreScheduleDay): List<ChoreItem> {
        scheduledRequestCount += 1
        return emptyList()
    }

    override suspend fun refresh() = Unit

    override suspend fun saveChore(draft: ChoreDraft): String = error("Not used in report aggregation tests")

    override suspend fun deleteChore(choreId: String) = error("Not used in report aggregation tests")

    override suspend fun toggleCompletion(
        choreId: String,
        viewerName: String,
        allowCompletingForSomeoneElse: Boolean,
        day: ChoreScheduleDay?,
    ) = error("Not used in report aggregation tests")

    override suspend fun submitProof(
        choreId: String,
        payload: ProofUploadPayload,
        day: ChoreScheduleDay?,
    ): ProofUploadResult = error("Not used in report aggregation tests")

    override suspend fun approveProof(
        choreId: String,
        day: ChoreScheduleDay?,
    ) = error("Not used in report aggregation tests")

    override suspend fun rejectProof(
        choreId: String,
        reason: String,
        day: ChoreScheduleDay?,
    ) = error("Not used in report aggregation tests")

    override suspend fun getAvailabilitySuggestions(days: List<ChoreScheduleDay>) = emptyList<com.sortoapps.chorepilot.features.chores.domain.AvailabilitySuggestion>()

    override suspend fun acceptAvailabilitySuggestion(suggestion: com.sortoapps.chorepilot.features.chores.domain.AvailabilitySuggestion) = Unit

    override suspend fun dismissAvailabilitySuggestion(suggestion: com.sortoapps.chorepilot.features.chores.domain.AvailabilitySuggestion) = Unit
}

private fun chore(
    id: String,
    title: String = "Kitchen reset",
    assignee: String = "Mila",
    completedByName: String = "",
    status: ChoreItemStatus,
    completedAtEpochMs: Long = 0L,
    proofRequired: Boolean = false,
): ChoreItem = ChoreItem(
    id = id,
    title = title,
    area = "Kitchen",
    cadence = "Daily",
    assignee = assignee,
    assigneeId = "member-1",
    status = status,
    durationLabel = "10 min",
    dueLabel = "Today",
    description = "Reset the room.",
    reminder = "After school",
    tags = listOf("Daily"),
    completedByName = completedByName,
    completedAtEpochMs = completedAtEpochMs,
    proofRequired = proofRequired,
)

private fun List<com.sortoapps.chorepilot.core.demo.DemoMetric>.metricNumber(
    title: String,
): String = first { it.value == title }.label
