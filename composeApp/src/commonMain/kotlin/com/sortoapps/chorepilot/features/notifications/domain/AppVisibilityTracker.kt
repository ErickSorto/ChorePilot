package com.sortoapps.chorepilot.features.notifications.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppVisibilityTracker {
    private val _isForeground = MutableStateFlow(false)
    val isForeground: StateFlow<Boolean> = _isForeground.asStateFlow()

    fun setForeground(value: Boolean) {
        _isForeground.value = value
    }
}
