package com.sortoapps.chorepilot.features.reports.presentation.viewmodel

import com.sortoapps.chorepilot.core.demo.DemoMetric
import kotlinx.serialization.Serializable

@Serializable
enum class ReportsRange {
    Daily,
    Weekly,
    Monthly,
    Yearly,
}

enum class ReportsSurfaceMode {
    Content,
    Locked,
    Empty,
    NoData,
}

data class ReportsLegendItemUiModel(
    val title: String,
    val body: String,
)

data class ReportsNavigationUiModel(
    val title: String,
    val supportingText: String,
    val canGoForward: Boolean,
)

data class ReportsChartBarUiModel(
    val label: String,
    val value: Int,
    val markerLabel: String,
    val supportingLabel: String = "",
)

data class ReportsFirstFinisherUiModel(
    val memberName: String,
    val taskTitle: String,
    val completedAtLabel: String,
)

data class ReportsLeaderboardEntryUiModel(
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

data class ReportsTaskHighlightUiModel(
    val memberName: String,
    val taskTitle: String,
    val statusLabel: String,
)

data class ReportsUiState(
    val selectedRange: ReportsRange = ReportsRange.Daily,
    val selectedSurface: ReportsSurfaceMode = ReportsSurfaceMode.Locked,
    val headline: String = "",
    val supportingText: String = "",
    val navigation: ReportsNavigationUiModel = ReportsNavigationUiModel(
        title = "",
        supportingText = "",
        canGoForward = false,
    ),
    val rangeSummary: String = "",
    val chartTitle: String = "",
    val chartSubtitle: String = "",
    val activeMetrics: List<DemoMetric> = emptyList(),
    val activeBars: List<ReportsChartBarUiModel> = emptyList(),
    val firstFinisher: ReportsFirstFinisherUiModel? = null,
    val leaderboard: List<ReportsLeaderboardEntryUiModel> = emptyList(),
    val taskHighlights: List<ReportsTaskHighlightUiModel> = emptyList(),
    val legendItems: List<ReportsLegendItemUiModel> = emptyList(),
    val emptyTitle: String = "",
    val emptyBody: String = "",
    val emptyActionHint: String = "",
    val lockTitle: String = "",
    val lockBody: String = "",
    val lockBenefits: List<String> = emptyList(),
    val hasChorePilotPro: Boolean = false,
)
