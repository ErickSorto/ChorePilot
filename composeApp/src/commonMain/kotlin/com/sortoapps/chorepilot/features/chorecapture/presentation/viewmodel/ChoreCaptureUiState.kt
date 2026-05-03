package com.sortoapps.chorepilot.features.chorecapture.presentation.viewmodel

import com.sortoapps.chorepilot.features.chores.domain.ChoreDailyModeValue
import com.sortoapps.chorepilot.features.chores.domain.ChoreEffortValue
import com.sortoapps.chorepilot.features.chores.domain.ChoreRepeatFrequencyValue

enum class DiscoveryCapturePhase {
    Capture,
    Analyzing,
    ReviewDrafts,
    Saving,
    Saved,
    Error,
}

data class DiscoveryDraftUiModel(
    val id: String,
    val title: String = "",
    val description: String = "",
    val roomName: String = "",
    val effort: ChoreEffortValue = ChoreEffortValue.Standard,
    val proofNeededSuggested: Boolean = false,
    val repeatsEnabled: Boolean = false,
    val repeatFrequency: ChoreRepeatFrequencyValue = ChoreRepeatFrequencyValue.Weekly,
    val dailyMode: ChoreDailyModeValue = ChoreDailyModeValue.Weekdays,
    val selectedWeekdays: Set<String> = emptySet(),
    val selectedOneTimeDate: String = "Tomorrow",
) {
    val isValid: Boolean
        get() = title.isNotBlank() && roomName.isNotBlank()
}

data class ChoreCaptureUiState(
    val isLoading: Boolean = false,
    val unavailableMessage: String? = null,
    val isDiscoveryMode: Boolean = false,
    val isAdminFlow: Boolean = false,
    val choreId: String = "",
    val dayLabel: String = "",
    val choreTitle: String = "",
    val assigneeName: String = "",
    val roomName: String = "",
    val dueLabel: String = "",
    val title: String = "Upload proof",
    val subtitle: String = "Add one clear photo of the finished chore.",
    val helperTitle: String = "Show the finished result",
    val helperBody: String = "One clear photo is enough.",
    val previewUri: String? = null,
    val hasCapturedPhoto: Boolean = false,
    val captureLabel: String = "",
    val primaryActionLabel: String = "Take photo",
    val isSubmitting: Boolean = false,
    val uploadComplete: Boolean = false,
    val errorMessage: String? = null,
    val hasChorePilotPro: Boolean = false,
    val discoveryPhase: DiscoveryCapturePhase = DiscoveryCapturePhase.Capture,
    val discoveryDrafts: List<DiscoveryDraftUiModel> = emptyList(),
    val discoverySummary: String = "",
    val discoverySaveActionLabel: String = "Save drafts",
    val shouldOpenPremium: Boolean = false,
    val shouldReturnToList: Boolean = false,
    val successMessage: String? = null,
) {
    val canSaveDiscoveryDrafts: Boolean
        get() = discoveryDrafts.any { it.isValid }
}
