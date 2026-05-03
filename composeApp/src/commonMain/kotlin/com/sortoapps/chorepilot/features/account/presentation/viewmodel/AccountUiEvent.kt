package com.sortoapps.chorepilot.features.account.presentation.viewmodel

sealed interface AccountUiEvent {
    data object TriggerVibration : AccountUiEvent
}

