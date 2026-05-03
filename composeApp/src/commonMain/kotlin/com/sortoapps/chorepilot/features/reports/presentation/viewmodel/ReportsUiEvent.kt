package com.sortoapps.chorepilot.features.reports.presentation.viewmodel

sealed interface ReportsUiEvent {
    data object TriggerVibration : ReportsUiEvent

    data class RangeSelected(val range: ReportsRange) : ReportsUiEvent
    data object PreviousPeriodRequested : ReportsUiEvent
    data object NextPeriodRequested : ReportsUiEvent
}
