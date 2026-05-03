package com.sortoapps.chorepilot.features.home.presentation.viewmodel

import com.sortoapps.chorepilot.core.demo.DemoMetric
import com.sortoapps.chorepilot.features.auth.domain.HouseholdRole
import com.sortoapps.chorepilot.features.chores.domain.ChoreItem

enum class HomeSurfaceMode {
    Today,
    Celebration,
    NoChores,
    ProofQueue,
}

data class HomeDayUiModel(
    val id: String,
    val fullLabel: String,
    val label: String,
    val supportingText: String,
    val dayOffset: Int,
    val dateKey: String,
    val isSelected: Boolean,
    val isToday: Boolean,
)

enum class HomeCompletionPromptKind {
    CompleteForAnotherMember,
    RemoveSubmittedProof,
}

data class HomeCompletionPromptUiModel(
    val choreId: String,
    val choreTitle: String,
    val assigneeName: String = "",
    val kind: HomeCompletionPromptKind,
)

data class HomeReassignmentSuggestionUiModel(
    val id: String,
    val choreId: String,
    val title: String,
    val dateKey: String,
    val originalAssignee: String,
    val coverMember: String,
    val reasonText: String,
    val weekdayLabel: String,
    val scopeStartDate: String,
    val scopeEndDate: String,
    val outsideNormalRotation: Boolean,
)

data class HomeUiState(
    val householdName: String = "",
    val viewerName: String = "Erick",
    val viewerId: String = "",
    val viewerRole: HouseholdRole = HouseholdRole.Member,
    val headline: String = "",
    val supportingText: String = "",
    val selectedDayLabel: String = "",
    val days: List<HomeDayUiModel> = emptyList(),
    val metrics: List<DemoMetric> = emptyList(),
    val focusChores: List<ChoreItem> = emptyList(),
    val animatedRemovalChoreIds: Set<String> = emptySet(),
    val selectedSurface: HomeSurfaceMode = HomeSurfaceMode.Today,
    val completionPrompt: HomeCompletionPromptUiModel? = null,
    val autoAssignSuggestionsEnabled: Boolean = true,
    val isLoadingReassignmentSuggestions: Boolean = false,
    val reassignmentSuggestions: List<HomeReassignmentSuggestionUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)
