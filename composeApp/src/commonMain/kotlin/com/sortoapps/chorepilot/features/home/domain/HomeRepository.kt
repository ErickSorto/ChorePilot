package com.sortoapps.chorepilot.features.home.domain

import com.sortoapps.chorepilot.core.demo.DemoChore
import com.sortoapps.chorepilot.core.demo.DemoMetric
import com.sortoapps.chorepilot.features.auth.domain.HouseholdRole
import com.sortoapps.chorepilot.features.chores.domain.ChoreItem
import kotlinx.coroutines.flow.StateFlow

data class HomeDaySnapshot(
    val id: String,
    val label: String,
    val shortLabel: String,
    val supportingText: String,
    val dayOffset: Int,
    val dateKey: String,
    val isToday: Boolean,
)

data class HomeSnapshot(
    val householdName: String,
    val viewerName: String,
    val viewerId: String,
    val viewerRole: HouseholdRole,
    val headline: String,
    val supportingText: String,
    val days: List<HomeDaySnapshot>,
    val metrics: List<DemoMetric>,
    val focusChores: List<DemoChore>,
    val choresByDay: Map<String, List<ChoreItem>> = emptyMap(),
)

data class HomeRepositoryState(
    val isLoading: Boolean = true,
    val snapshot: HomeSnapshot? = null,
    val errorMessage: String? = null,
)

interface HomeRepository {
    val state: StateFlow<HomeRepositoryState>

    suspend fun refresh()
}
