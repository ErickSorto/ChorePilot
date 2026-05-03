package com.sortoapps.chorepilot.features.reports.domain

import com.sortoapps.chorepilot.core.demo.DemoMetric

data class ReportsPeriodSelection(
    val dailyCursor: Int = 0,
    val weeklyCursor: Int = 0,
    val monthlyCursor: Int = 0,
    val yearlyCursor: Int = 0,
) {
    fun cursorFor(range: com.sortoapps.chorepilot.features.reports.presentation.viewmodel.ReportsRange): Int = when (range) {
        com.sortoapps.chorepilot.features.reports.presentation.viewmodel.ReportsRange.Daily -> dailyCursor
        com.sortoapps.chorepilot.features.reports.presentation.viewmodel.ReportsRange.Weekly -> weeklyCursor
        com.sortoapps.chorepilot.features.reports.presentation.viewmodel.ReportsRange.Monthly -> monthlyCursor
        com.sortoapps.chorepilot.features.reports.presentation.viewmodel.ReportsRange.Yearly -> yearlyCursor
    }

    fun moveBackward(range: com.sortoapps.chorepilot.features.reports.presentation.viewmodel.ReportsRange): ReportsPeriodSelection = when (range) {
        com.sortoapps.chorepilot.features.reports.presentation.viewmodel.ReportsRange.Daily -> copy(dailyCursor = dailyCursor + 1)
        com.sortoapps.chorepilot.features.reports.presentation.viewmodel.ReportsRange.Weekly -> copy(weeklyCursor = weeklyCursor + 1)
        com.sortoapps.chorepilot.features.reports.presentation.viewmodel.ReportsRange.Monthly -> copy(monthlyCursor = monthlyCursor + 1)
        com.sortoapps.chorepilot.features.reports.presentation.viewmodel.ReportsRange.Yearly -> copy(yearlyCursor = yearlyCursor + 1)
    }

    fun moveForward(range: com.sortoapps.chorepilot.features.reports.presentation.viewmodel.ReportsRange): ReportsPeriodSelection = when (range) {
        com.sortoapps.chorepilot.features.reports.presentation.viewmodel.ReportsRange.Daily -> copy(dailyCursor = (dailyCursor - 1).coerceAtLeast(0))
        com.sortoapps.chorepilot.features.reports.presentation.viewmodel.ReportsRange.Weekly -> copy(weeklyCursor = (weeklyCursor - 1).coerceAtLeast(0))
        com.sortoapps.chorepilot.features.reports.presentation.viewmodel.ReportsRange.Monthly -> copy(monthlyCursor = (monthlyCursor - 1).coerceAtLeast(0))
        com.sortoapps.chorepilot.features.reports.presentation.viewmodel.ReportsRange.Yearly -> copy(yearlyCursor = (yearlyCursor - 1).coerceAtLeast(0))
    }
}

data class ReportsNavigationSnapshot(
    val title: String,
    val supportingText: String,
    val canGoForward: Boolean,
)

data class ReportsBarPoint(
    val label: String,
    val value: Int,
    val markerLabel: String,
    val supportingLabel: String = "",
)

data class ReportsLeaderboardEntry(
    val memberName: String,
    val avatarSeed: String,
    val completedCount: Int,
    val assignedCount: Int,
    val pendingCount: Int,
    val overdueCount: Int,
    val completionRate: Int,
    val rank: Int,
    val highlightLabel: String,
)

data class ReportsTaskHighlight(
    val memberName: String,
    val taskTitle: String,
    val statusLabel: String,
)

data class ReportsFirstFinisher(
    val memberName: String,
    val taskTitle: String,
    val completedAtLabel: String,
)

data class ReportsLegendItem(
    val title: String,
    val body: String,
)

data class ReportsPeriodSnapshot(
    val headline: String,
    val supportingText: String,
    val navigation: ReportsNavigationSnapshot,
    val rangeSummary: String,
    val chartTitle: String,
    val chartSubtitle: String,
    val metrics: List<DemoMetric>,
    val bars: List<ReportsBarPoint>,
    val firstFinisher: ReportsFirstFinisher? = null,
    val leaderboard: List<ReportsLeaderboardEntry> = emptyList(),
    val taskHighlights: List<ReportsTaskHighlight> = emptyList(),
    val legendItems: List<ReportsLegendItem>,
    val emptyTitle: String,
    val emptyBody: String,
    val emptyActionHint: String,
    val lockTitle: String,
    val lockBody: String,
    val lockBenefits: List<String>,
)

data class ReportsSnapshot(
    val daily: ReportsPeriodSnapshot,
    val weekly: ReportsPeriodSnapshot,
    val monthly: ReportsPeriodSnapshot,
    val yearly: ReportsPeriodSnapshot,
)

interface ReportsRepository {
    fun getReportsPeriodSnapshot(
        range: com.sortoapps.chorepilot.features.reports.presentation.viewmodel.ReportsRange,
        selection: ReportsPeriodSelection = ReportsPeriodSelection(),
    ): ReportsPeriodSnapshot

    fun getReportsSnapshot(selection: ReportsPeriodSelection = ReportsPeriodSelection()): ReportsSnapshot = ReportsSnapshot(
        daily = getReportsPeriodSnapshot(
            range = com.sortoapps.chorepilot.features.reports.presentation.viewmodel.ReportsRange.Daily,
            selection = selection,
        ),
        weekly = getReportsPeriodSnapshot(
            range = com.sortoapps.chorepilot.features.reports.presentation.viewmodel.ReportsRange.Weekly,
            selection = selection,
        ),
        monthly = getReportsPeriodSnapshot(
            range = com.sortoapps.chorepilot.features.reports.presentation.viewmodel.ReportsRange.Monthly,
            selection = selection,
        ),
        yearly = getReportsPeriodSnapshot(
            range = com.sortoapps.chorepilot.features.reports.presentation.viewmodel.ReportsRange.Yearly,
            selection = selection,
        ),
    )
}
