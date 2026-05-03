package com.sortoapps.chorepilot.features.reports.data

import com.sortoapps.chorepilot.core.demo.DemoMetric
import com.sortoapps.chorepilot.core.demo.DemoTone
import com.sortoapps.chorepilot.features.chores.domain.ChoreItem
import com.sortoapps.chorepilot.features.chores.domain.ChoreItemStatus
import com.sortoapps.chorepilot.features.chores.domain.ChoreRepository
import com.sortoapps.chorepilot.features.chores.domain.ChoreScheduleDay
import com.sortoapps.chorepilot.features.chores.domain.scheduleDateKey
import com.sortoapps.chorepilot.features.chores.domain.weekdayNameForDayOffset
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
import com.sortoapps.chorepilot.features.settings.domain.SettingsRepository
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.math.absoluteValue
import kotlin.time.Clock

class ProductionReportsRepository(
    private val choreRepository: ChoreRepository,
    private val settingsRepository: SettingsRepository,
) : ReportsRepository {
    override fun getReportsPeriodSnapshot(
        range: ReportsRange,
        selection: ReportsPeriodSelection,
    ): ReportsPeriodSnapshot {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        return when (range) {
            ReportsRange.Daily -> {
                val dailyDay = dayForDate(today.plus(DatePeriod(days = -selection.dailyCursor)), today)
                val dailyChores = choreRepository.getScheduledChores(dailyDay)
                buildDailySnapshot(
                    day = dailyDay,
                    cursor = selection.dailyCursor,
                    chores = dailyChores,
                )
            }

            ReportsRange.Weekly -> {
                val weeklyPoints = buildWeeklyPoints(
                    cursor = selection.weeklyCursor,
                    today = today,
                )
                buildTimelineSnapshot(
                    headline = "Daily momentum",
                    supportingText = "See which days actually carried the household instead of guessing from memory.",
                    navigation = ReportsNavigationSnapshot(
                        title = if (selection.weeklyCursor == 0) "This week" else "Week ${selection.weeklyCursor + 1} back",
                        supportingText = weeklyPoints.firstOrNull()?.let { first ->
                            weeklyPoints.lastOrNull()?.let { last ->
                                "${first.supportingLabel} to ${last.supportingLabel}"
                            }
                        }.orEmpty(),
                        canGoForward = selection.weeklyCursor > 0,
                    ),
                    rangeSummary = bestRangeSummary(
                        points = weeklyPoints,
                        emptyFallback = "This week has not logged enough activity to show a useful rhythm yet.",
                    ),
                    chartTitle = "Completed chores by day",
                    chartSubtitle = "A clean day-by-day view of follow-through across the selected week.",
                    points = weeklyPoints,
                    aggregateStats = weeklyPoints.fold(PeriodStats()) { acc, point -> addStats(acc, point.stats) },
                    emptyTitle = "No weekly activity yet",
                    emptyBody = "Once the household starts logging daily chore movement, this weekly view will show where momentum is strongest.",
                    emptyActionHint = "Complete or review a few chores across the week, then come back for the first real pattern.",
                    lockTitle = "Weekly insight stays in Pro",
                    lockBody = "Compare day-by-day completion, proof review, and missed work before the week gets away from the household.",
                    lockBenefits = listOf(
                        "See your strongest and weakest days",
                        "Catch proof bottlenecks earlier",
                        "Rebalance chores before the weekend stack-up",
                    ),
                )
            }

            ReportsRange.Monthly -> {
                val monthlyPoints = buildMonthlyPoints(
                    cursor = selection.monthlyCursor,
                    today = today,
                )
                buildTimelineSnapshot(
                    headline = "Monthly consistency",
                    supportingText = "Roll the selected month into clean weekly checkpoints so slowdowns and streaks stand out.",
                    navigation = ReportsNavigationSnapshot(
                        title = monthYearLabel(today.plus(DatePeriod(months = -selection.monthlyCursor))),
                        supportingText = "Weekly checkpoints inside the selected month",
                        canGoForward = selection.monthlyCursor > 0,
                    ),
                    rangeSummary = bestRangeSummary(
                        points = monthlyPoints,
                        emptyFallback = "There is not enough recent history to show a useful month-sized pattern yet.",
                    ),
                    chartTitle = "Completed chores by week",
                    chartSubtitle = "Each column bundles one week inside the selected month.",
                    points = monthlyPoints,
                    aggregateStats = monthlyPoints.fold(PeriodStats()) { acc, point -> addStats(acc, point.stats) },
                    emptyTitle = "No monthly pattern yet",
                    emptyBody = "Keep chores active through the month and this view will start to show whether the household is settling into a repeatable rhythm.",
                    emptyActionHint = "A couple of steady weeks is enough for this chart to become useful.",
                    lockTitle = "Monthly insight stays in Pro",
                    lockBody = "Zoom out from individual days and see whether the household rhythm is actually holding across the selected month.",
                    lockBenefits = listOf(
                        "See rolling week-to-week consistency",
                        "Track overdue work before it becomes a habit",
                        "Spot when proof review slows the household down",
                    ),
                )
            }

            ReportsRange.Yearly -> {
                val yearlyPoints = buildYearlyPoints(
                    cursor = selection.yearlyCursor,
                    today = today,
                )
                buildTimelineSnapshot(
                    headline = "Yearly outlook",
                    supportingText = "Step back and look at the longer household story with clean month-by-month completion data.",
                    navigation = ReportsNavigationSnapshot(
                        title = (today.year - selection.yearlyCursor).toString(),
                        supportingText = "Month-by-month household follow-through",
                        canGoForward = selection.yearlyCursor > 0,
                    ),
                    rangeSummary = bestRangeSummary(
                        points = yearlyPoints,
                        emptyFallback = "There is not enough long-range history yet to show a yearly trend.",
                    ),
                    chartTitle = "Completed chores by month",
                    chartSubtitle = "Each column represents one month of logged household work in the selected year.",
                    points = yearlyPoints,
                    aggregateStats = yearlyPoints.fold(PeriodStats()) { acc, point -> addStats(acc, point.stats) },
                    emptyTitle = "No yearly trend yet",
                    emptyBody = "This view becomes useful once the household has built up a longer trail of completed work.",
                    emptyActionHint = "Keep the daily and weekly flow healthy first, then use this view for the bigger pattern.",
                    lockTitle = "Yearly insight stays in Pro",
                    lockBody = "Watch the bigger household pattern over time, including which months hold steady and which ones start to slip.",
                    lockBenefits = listOf(
                        "See month-by-month completion clearly",
                        "Track longer fairness and follow-through shifts",
                        "Understand when the household rhythm is most fragile",
                    ),
                )
            }
        }
    }

    private fun buildDailySnapshot(
        day: ChoreScheduleDay,
        cursor: Int,
        chores: List<ChoreItem>,
    ): ReportsPeriodSnapshot {
        val stats = statsForChores(chores)
        val leaderboard = buildLeaderboard(chores)
        val taskHighlights = buildTaskHighlights(chores)
        return ReportsPeriodSnapshot(
            headline = if (cursor == 0) "Today's household pulse" else "${day.weekdayName} report",
            supportingText = "A leaderboard-style read on who finished their work and what still needs attention.",
            navigation = ReportsNavigationSnapshot(
                title = if (cursor == 0) "Today" else readableDate(day.dateKey),
                supportingText = day.weekdayName,
                canGoForward = cursor > 0,
            ),
            rangeSummary = if (stats.scheduled == 0) {
                "No chores were scheduled for this day."
            } else {
                "${stats.completed} of ${stats.scheduled} chores were completed, with ${taskHighlights.size} still needing attention."
            },
            chartTitle = "Household leaderboard",
            chartSubtitle = "The strongest finishers rise to the top. Open tasks stay grouped cleanly below.",
            metrics = buildMetrics(stats),
            bars = listOf(
                ReportsBarPoint("Completed", stats.completed, "${stats.completed} completed", "${stats.completed}/${stats.scheduled}"),
                ReportsBarPoint("Pending proof", stats.pendingProof, "${stats.pendingProof} awaiting review", "Proof queue"),
                ReportsBarPoint("Open", stats.dueSoon, "${stats.dueSoon} still open", "Due soon"),
                ReportsBarPoint("Overdue", stats.overdue, "${stats.overdue} overdue", "Needs rescue"),
            ),
            firstFinisher = buildFirstFinisher(chores),
            leaderboard = leaderboard,
            taskHighlights = taskHighlights,
            legendItems = listOf(
                ReportsLegendItem(
                    title = "Leaderboard logic",
                    body = "Members are ranked by completed chores first, then by completion rate and assigned volume.",
                ),
            ),
            emptyTitle = "Nothing is scheduled for this day",
            emptyBody = "When chores are assigned here, the daily report will rank members and list any unfinished work cleanly.",
            emptyActionHint = "Move back or forward through the daily navigator to inspect other dates.",
            lockTitle = "Daily insight is included",
            lockBody = "The daily report is always available.",
            lockBenefits = emptyList(),
        )
    }

    private fun buildTimelineSnapshot(
        headline: String,
        supportingText: String,
        navigation: ReportsNavigationSnapshot,
        rangeSummary: String,
        chartTitle: String,
        chartSubtitle: String,
        points: List<TimelinePoint>,
        aggregateStats: PeriodStats,
        emptyTitle: String,
        emptyBody: String,
        emptyActionHint: String,
        lockTitle: String,
        lockBody: String,
        lockBenefits: List<String>,
    ): ReportsPeriodSnapshot = ReportsPeriodSnapshot(
        headline = headline,
        supportingText = supportingText,
        navigation = navigation,
        rangeSummary = rangeSummary,
        chartTitle = chartTitle,
        chartSubtitle = chartSubtitle,
        metrics = buildMetrics(aggregateStats),
        bars = points.map { point ->
            ReportsBarPoint(
                label = point.label,
                value = point.stats.completed,
                markerLabel = "${point.label}: ${point.stats.completed} of ${point.stats.scheduled} chores completed",
                supportingLabel = point.supportingLabel,
            )
        },
        legendItems = listOf(
            ReportsLegendItem(
                title = "What the bars mean",
                body = "Bar height reflects completed chores, while labels keep the chosen day, week, month, or year easy to read.",
            ),
            ReportsLegendItem(
                title = "How to use this range",
                body = "Look for repeated dips or thinner months. That is usually where the household rhythm is slipping.",
            ),
        ),
        emptyTitle = emptyTitle,
        emptyBody = emptyBody,
        emptyActionHint = emptyActionHint,
        lockTitle = lockTitle,
        lockBody = lockBody,
        lockBenefits = lockBenefits,
    )

    private fun buildWeeklyPoints(
        cursor: Int,
        today: LocalDate,
    ): List<TimelinePoint> {
        val weekStartsOnMonday = settingsRepository.settings.value.weekStartsOnMonday
        val weekStart = startOfWeek(today, weekStartsOnMonday).plus(DatePeriod(days = -(cursor * 7)))
        return (0..6).map { dayIndex ->
            val date = weekStart.plus(DatePeriod(days = dayIndex))
            val day = dayForDate(date, today)
            TimelinePoint(
                label = day.weekdayName.take(3),
                supportingLabel = readableDate(day.dateKey),
                stats = statsForChores(choreRepository.getScheduledChores(day)),
            )
        }
    }

    private fun buildMonthlyPoints(
        cursor: Int,
        today: LocalDate,
    ): List<TimelinePoint> {
        val monthDate = today.plus(DatePeriod(months = -cursor))
        val monthStart = LocalDate(monthDate.year, monthDate.month, 1)
        val nextMonthStart = monthStart.plus(DatePeriod(months = 1))
        val buckets = MutableList(4) { mutableListOf<ChoreItem>() }
        var current = monthStart
        while (current < nextMonthStart) {
            val bucketIndex = (((current.day - 1) / 7)).coerceAtMost(3)
            buckets[bucketIndex] += choresForDate(current, today)
            current = current.plus(DatePeriod(days = 1))
        }
        return buckets.mapIndexed { index, bucket ->
            val rangeStart = monthStart.plus(DatePeriod(days = index * 7))
            val rangeEnd = if (index == buckets.lastIndex) {
                nextMonthStart.plus(DatePeriod(days = -1))
            } else {
                rangeStart.plus(DatePeriod(days = 6))
            }
            TimelinePoint(
                label = "Wk ${index + 1}",
                supportingLabel = "${monthAbbreviation(rangeStart.monthNumber)} ${rangeStart.day}-${rangeEnd.day.coerceAtMost(daysInMonth(monthDate))}",
                stats = statsForChores(bucket),
            )
        }
    }

    private fun buildYearlyPoints(
        cursor: Int,
        today: LocalDate,
    ): List<TimelinePoint> {
        val selectedYear = today.year - cursor
        return (1..12).map { monthNumber ->
            val monthStart = LocalDate(selectedYear, kotlinx.datetime.Month(monthNumber), 1)
            val nextMonthStart = monthStart.plus(DatePeriod(months = 1))
            var current = monthStart
            var stats = PeriodStats()
            while (current < nextMonthStart) {
                stats = addStats(stats, statsForChores(choresForDate(current, today)))
                current = current.plus(DatePeriod(days = 1))
            }
            TimelinePoint(
                label = monthAbbreviation(monthNumber),
                supportingLabel = selectedYear.toString(),
                stats = stats,
            )
        }
    }

    private fun buildLeaderboard(
        chores: List<ChoreItem>,
    ): List<ReportsLeaderboardEntry> = chores
        .groupBy { chore -> chore.assignee.ifBlank { "Unassigned" } }
        .map { (memberName, memberChores) ->
            val stats = statsForChores(memberChores)
            MemberScore(
                memberName = memberName,
                avatarSeed = avatarSeed(memberName),
                stats = stats,
            )
        }
        .sortedWith(
            compareByDescending<MemberScore> { it.stats.completed }
                .thenByDescending { it.stats.completionRate }
                .thenByDescending { it.stats.scheduled }
                .thenBy { it.memberName.lowercase() },
        )
        .mapIndexed { index, score ->
            ReportsLeaderboardEntry(
                memberName = score.memberName,
                avatarSeed = score.avatarSeed,
                completedCount = score.stats.completed,
                assignedCount = score.stats.scheduled,
                pendingCount = score.stats.pendingProof + score.stats.dueSoon,
                overdueCount = score.stats.overdue,
                completionRate = score.stats.completionRate,
                rank = index + 1,
                highlightLabel = when {
                    score.stats.overdue > 0 -> "${score.stats.overdue} overdue"
                    score.stats.pendingProof > 0 -> "${score.stats.pendingProof} awaiting proof"
                    score.stats.scheduled == 0 -> "No assignments"
                    score.stats.completed == score.stats.scheduled -> "Clean sweep"
                    else -> "${score.stats.completed}/${score.stats.scheduled} done"
                },
            )
        }

    private fun buildTaskHighlights(
        chores: List<ChoreItem>,
    ): List<ReportsTaskHighlight> = chores
        .filterNot { it.status == ChoreItemStatus.Completed }
        .sortedWith(
            compareByDescending<ChoreItem> { taskPriority(it.status) }
                .thenBy { it.assignee.lowercase() }
                .thenBy { it.title.lowercase() },
        )
        .take(8)
        .map { chore ->
            ReportsTaskHighlight(
                memberName = chore.assignee.ifBlank { "Unassigned" },
                taskTitle = chore.title,
                statusLabel = statusLabel(chore.status),
            )
        }

    private fun buildFirstFinisher(
        chores: List<ChoreItem>,
    ): ReportsFirstFinisher? = chores
        .filter { it.status == ChoreItemStatus.Completed || it.status == ChoreItemStatus.PendingProof }
        .filter { it.completedAtEpochMs > 0L }
        .minByOrNull { it.completedAtEpochMs }
        ?.let { chore ->
            ReportsFirstFinisher(
                memberName = chore.completedByName.ifBlank { chore.assignee.ifBlank { "Household member" } },
                taskTitle = chore.title,
                completedAtLabel = formatCompletionTime(chore.completedAtEpochMs),
            )
        }

    private fun choresForDate(
        target: LocalDate,
        today: LocalDate,
    ): List<ChoreItem> = choreRepository.getScheduledChores(dayForDate(target, today))

    private fun dayForDate(
        target: LocalDate,
        today: LocalDate,
    ): ChoreScheduleDay = scheduleDay(daysBetween(today = today, target = target), today)

    private fun scheduleDay(
        dayOffset: Int,
        today: LocalDate,
    ): ChoreScheduleDay = ChoreScheduleDay(
        id = "reports-$dayOffset",
        weekdayName = weekdayNameForDayOffset(dayOffset, today),
        dayOffset = dayOffset,
        isToday = dayOffset == 0,
        dateKey = scheduleDateKey(dayOffset, today),
    )

    private fun statsForChores(
        chores: List<ChoreItem>,
    ): PeriodStats = PeriodStats(
        scheduled = chores.size,
        completed = chores.count { it.status == ChoreItemStatus.Completed },
        pendingProof = chores.count { it.status == ChoreItemStatus.PendingProof },
        dueSoon = chores.count { it.status == ChoreItemStatus.DueSoon || it.status == ChoreItemStatus.Scheduled },
        overdue = chores.count { it.status == ChoreItemStatus.Overdue },
    )
}

private data class PeriodStats(
    val scheduled: Int = 0,
    val completed: Int = 0,
    val pendingProof: Int = 0,
    val dueSoon: Int = 0,
    val overdue: Int = 0,
) {
    val completionRate: Int
        get() = if (scheduled == 0) 0 else ((completed.toDouble() / scheduled.toDouble()) * 100).toInt()
}

private data class TimelinePoint(
    val label: String,
    val supportingLabel: String,
    val stats: PeriodStats,
)

private data class MemberScore(
    val memberName: String,
    val avatarSeed: String,
    val stats: PeriodStats,
)

private fun addStats(
    left: PeriodStats,
    right: PeriodStats,
): PeriodStats = PeriodStats(
    scheduled = left.scheduled + right.scheduled,
    completed = left.completed + right.completed,
    pendingProof = left.pendingProof + right.pendingProof,
    dueSoon = left.dueSoon + right.dueSoon,
    overdue = left.overdue + right.overdue,
)

private fun buildMetrics(
    stats: PeriodStats,
): List<DemoMetric> {
    val unresolvedMetric = if (stats.overdue > 0) {
        DemoMetric(stats.overdue.toString(), "Overdue", DemoTone.Warning)
    } else {
        DemoMetric(stats.pendingProof.toString(), "Pending proof", DemoTone.Tertiary)
    }
    return listOf(
        DemoMetric(stats.scheduled.toString(), "Scheduled", DemoTone.Primary),
        DemoMetric(stats.completed.toString(), "Completed", DemoTone.Success),
        DemoMetric("${stats.completionRate}%", "Completion rate", DemoTone.Secondary),
        unresolvedMetric,
    )
}

private fun bestRangeSummary(
    points: List<TimelinePoint>,
    emptyFallback: String,
): String {
    val strongestPoint = points.maxByOrNull { it.stats.completed }
        ?.takeIf { it.stats.scheduled > 0 }
        ?: return emptyFallback
    return "${strongestPoint.label} carried the strongest follow-through with ${strongestPoint.stats.completed} completed chores out of ${strongestPoint.stats.scheduled} scheduled."
}

private fun readableDate(dateKey: String): String = runCatching {
    val date = LocalDate.parse(dateKey)
    "${monthAbbreviation(date.monthNumber)} ${date.day}"
}.getOrDefault(dateKey)

private fun monthYearLabel(date: LocalDate): String = "${monthName(date.monthNumber)} ${date.year}"

private fun statusLabel(status: ChoreItemStatus): String = when (status) {
    ChoreItemStatus.Completed -> "Completed"
    ChoreItemStatus.PendingProof -> "Awaiting proof"
    ChoreItemStatus.DueSoon -> "Due soon"
    ChoreItemStatus.Overdue -> "Overdue"
    ChoreItemStatus.Scheduled -> "Open"
}

private fun formatCompletionTime(epochMs: Long): String = runCatching {
    val local = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(TimeZone.currentSystemDefault())
    val hour = when (val rawHour = local.hour % 12) {
        0 -> 12
        else -> rawHour
    }
    val minutes = local.minute.toString().padStart(2, '0')
    val meridiem = if (local.hour >= 12) "PM" else "AM"
    "$hour:$minutes $meridiem"
}.getOrDefault("")

private fun avatarSeed(name: String): String = name
    .split(" ")
    .filter { it.isNotBlank() }
    .take(2)
    .joinToString("") { it.first().uppercase() }
    .ifBlank { "?" }

private fun taskPriority(status: ChoreItemStatus): Int = when (status) {
    ChoreItemStatus.Overdue -> 4
    ChoreItemStatus.PendingProof -> 3
    ChoreItemStatus.DueSoon -> 2
    ChoreItemStatus.Scheduled -> 1
    ChoreItemStatus.Completed -> 0
}

private fun daysBetween(
    today: LocalDate,
    target: LocalDate,
): Int {
    var cursor = target
    var offset = 0
    while (cursor < today) {
        cursor = cursor.plus(DatePeriod(days = 1))
        offset -= 1
    }
    while (cursor > today) {
        cursor = cursor.plus(DatePeriod(days = -1))
        offset += 1
    }
    return offset
}

private fun daysInMonth(date: LocalDate): Int = when (date.monthNumber) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    else -> if (isLeapYear(date.year)) 29 else 28
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

private fun startOfWeek(
    date: LocalDate,
    weekStartsOnMonday: Boolean,
): LocalDate = if (weekStartsOnMonday) mondayStartForWeek(date) else sundayStartForWeek(date)

private fun isLeapYear(year: Int): Boolean = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

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

private fun monthName(monthNumber: Int): String = when (monthNumber) {
    1 -> "January"
    2 -> "February"
    3 -> "March"
    4 -> "April"
    5 -> "May"
    6 -> "June"
    7 -> "July"
    8 -> "August"
    9 -> "September"
    10 -> "October"
    11 -> "November"
    else -> "December"
}
