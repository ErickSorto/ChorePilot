package com.sortoapps.chorepilot.features.notifications.domain

import kotlinx.coroutines.flow.Flow

interface PushTokenSource {
    val platform: String
    val tokens: Flow<String?>
}
