package com.sortoapps.chorepilot.features.reports.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sortoapps.chorepilot.core.platform.VibratorUtil
import com.sortoapps.chorepilot.core.revenuecat.SubscriptionRepository
import com.sortoapps.chorepilot.features.chores.domain.ChoreRepository
import com.sortoapps.chorepilot.features.reports.domain.ReportsBarPoint
import com.sortoapps.chorepilot.features.reports.domain.ReportsFirstFinisher
import com.sortoapps.chorepilot.features.reports.domain.ReportsLeaderboardEntry
import com.sortoapps.chorepilot.features.reports.domain.ReportsLegendItem
import com.sortoapps.chorepilot.features.reports.domain.ReportsPeriodSnapshot
import com.sortoapps.chorepilot.features.reports.domain.ReportsPeriodSelection
import com.sortoapps.chorepilot.features.reports.domain.ReportsRepository
import com.sortoapps.chorepilot.features.reports.domain.ReportsTaskHighlight
import com.sortoapps.chorepilot.features.reports.domain.ReportsNavigationSnapshot
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReportsViewModel(
    private val reportsRepository: ReportsRepository,
    choreRepository: ChoreRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val vibratorUtil: VibratorUtil,
    private val computationDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    constructor(
        reportsRepository: ReportsRepository,
        choreRepository: ChoreRepository,
        subscriptionRepository: SubscriptionRepository,
        vibratorUtil: VibratorUtil,
    ) : this(
        reportsRepository = reportsRepository,
        choreRepository = choreRepository,
        subscriptionRepository = subscriptionRepository,
        vibratorUtil = vibratorUtil,
        computationDispatcher = Dispatchers.Default,
    )

    private var periodSelection = ReportsPeriodSelection()

    private val _uiState = MutableStateFlow(
        buildUiState(
            selectedRange = ReportsRange.Daily,
            hasPro = subscriptionRepository.snapshot.value.hasChorePilotPro,
            presentation = reportsRepository.getReportsPeriodSnapshot(
                range = ReportsRange.Daily,
                selection = periodSelection,
            ),
        ),
    )
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            subscriptionRepository.snapshot.drop(1).collectLatest { subscription ->
                rebuildUiState(
                    selectedRange = uiState.value.selectedRange,
                    hasPro = subscription.hasChorePilotPro,
                )
            }
        }
        viewModelScope.launch {
            choreRepository.chores.drop(1).collectLatest {
                rebuildUiState(
                    selectedRange = uiState.value.selectedRange,
                    hasPro = subscriptionRepository.snapshot.value.hasChorePilotPro,
                )
            }
        }
    }

    fun onEvent(event: ReportsUiEvent) {
        when (event) {
            ReportsUiEvent.TriggerVibration -> vibratorUtil.triggerVibration()
            is ReportsUiEvent.RangeSelected -> viewModelScope.launch {
                rebuildUiState(
                    selectedRange = event.range,
                    hasPro = uiState.value.hasChorePilotPro,
                )
            }
            ReportsUiEvent.PreviousPeriodRequested -> viewModelScope.launch {
                val selectedRange = uiState.value.selectedRange
                periodSelection = periodSelection.moveBackward(selectedRange)
                rebuildUiState(
                    selectedRange = selectedRange,
                    hasPro = uiState.value.hasChorePilotPro,
                )
            }
            ReportsUiEvent.NextPeriodRequested -> viewModelScope.launch {
                val selectedRange = uiState.value.selectedRange
                periodSelection = periodSelection.moveForward(selectedRange)
                rebuildUiState(
                    selectedRange = selectedRange,
                    hasPro = uiState.value.hasChorePilotPro,
                )
            }
        }
    }

    private suspend fun rebuildUiState(
        selectedRange: ReportsRange,
        hasPro: Boolean,
    ) {
        val presentation = withContext(computationDispatcher) {
            reportsRepository.getReportsPeriodSnapshot(
                range = selectedRange,
                selection = periodSelection,
            )
        }
        _uiState.value = buildUiState(
            selectedRange = selectedRange,
            hasPro = hasPro,
            presentation = presentation,
        )
    }
}

private fun buildUiState(
    selectedRange: ReportsRange,
    hasPro: Boolean,
    presentation: ReportsPeriodSnapshot,
): ReportsUiState {
    val selectedSurface = selectedRange.resolveSurface(
        hasPro = hasPro,
        bars = presentation.bars,
        metricsCount = presentation.metrics.size,
    )
    return presentation.toUiState().copy(
        selectedRange = selectedRange,
        selectedSurface = selectedSurface,
        hasChorePilotPro = hasPro,
    )
}

private fun ReportsRange.resolveSurface(
    hasPro: Boolean,
    bars: List<ReportsBarPoint>,
    metricsCount: Int,
): ReportsSurfaceMode {
    if (this != ReportsRange.Daily && !hasPro) return ReportsSurfaceMode.Locked
    if (metricsCount == 0 && bars.isEmpty()) return ReportsSurfaceMode.Empty
    if (bars.isEmpty() || bars.none { it.value > 0 }) return ReportsSurfaceMode.NoData
    return ReportsSurfaceMode.Content
}

private fun ReportsPeriodSnapshot.toUiState(): ReportsUiState = ReportsUiState(
    headline = headline,
    supportingText = supportingText,
    navigation = navigation.toUiModel(),
    rangeSummary = rangeSummary,
    chartTitle = chartTitle,
    chartSubtitle = chartSubtitle,
    activeMetrics = metrics,
    activeBars = bars.map { it.toUiModel() },
    firstFinisher = firstFinisher?.toUiModel(),
    leaderboard = leaderboard.map { it.toUiModel() },
    taskHighlights = taskHighlights.map { it.toUiModel() },
    legendItems = legendItems.map { it.toUiModel() },
    emptyTitle = emptyTitle,
    emptyBody = emptyBody,
    emptyActionHint = emptyActionHint,
    lockTitle = lockTitle,
    lockBody = lockBody,
    lockBenefits = lockBenefits,
)

private fun ReportsNavigationSnapshot.toUiModel(): ReportsNavigationUiModel = ReportsNavigationUiModel(
    title = title,
    supportingText = supportingText,
    canGoForward = canGoForward,
)

private fun ReportsBarPoint.toUiModel(): ReportsChartBarUiModel = ReportsChartBarUiModel(
    label = label,
    value = value,
    markerLabel = markerLabel,
    supportingLabel = supportingLabel,
)

private fun ReportsLeaderboardEntry.toUiModel(): ReportsLeaderboardEntryUiModel = ReportsLeaderboardEntryUiModel(
    memberName = memberName,
    avatarSeed = avatarSeed,
    completedCount = completedCount,
    assignedCount = assignedCount,
    pendingCount = pendingCount,
    overdueCount = overdueCount,
    completionRate = completionRate,
    rank = rank,
    highlightLabel = highlightLabel,
)

private fun ReportsTaskHighlight.toUiModel(): ReportsTaskHighlightUiModel = ReportsTaskHighlightUiModel(
    memberName = memberName,
    taskTitle = taskTitle,
    statusLabel = statusLabel,
)

private fun ReportsFirstFinisher.toUiModel(): ReportsFirstFinisherUiModel = ReportsFirstFinisherUiModel(
    memberName = memberName,
    taskTitle = taskTitle,
    completedAtLabel = completedAtLabel,
)

private fun ReportsLegendItem.toUiModel(): ReportsLegendItemUiModel = ReportsLegendItemUiModel(
    title = title,
    body = body,
)
