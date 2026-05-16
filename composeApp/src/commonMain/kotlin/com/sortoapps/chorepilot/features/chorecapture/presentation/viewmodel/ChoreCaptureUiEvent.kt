package com.sortoapps.chorepilot.features.chorecapture.presentation.viewmodel

import com.sortoapps.chorepilot.features.chores.domain.ChoreEffortValue
import com.sortoapps.chorepilot.features.chores.domain.ChoreDailyModeValue
import com.sortoapps.chorepilot.features.chores.domain.ChoreRepeatFrequencyValue
import com.sortoapps.chorepilot.features.chores.domain.ChoreScheduleDay
import com.sortoapps.chorepilot.features.chores.domain.ProofUploadPayload

sealed interface ChoreCaptureUiEvent {
    data object TriggerVibration : ChoreCaptureUiEvent

    data object TriggerVibrationSuccess : ChoreCaptureUiEvent

    data class LoadChore(
        val choreId: String,
        val day: ChoreScheduleDay? = null,
    ) : ChoreCaptureUiEvent

    data class ProofCaptured(val payload: ProofUploadPayload) : ChoreCaptureUiEvent

    data class CameraFailed(val message: String) : ChoreCaptureUiEvent

    data object UploadProof : ChoreCaptureUiEvent

    data object RetryDiscoveryAnalysis : ChoreCaptureUiEvent

    data object AddDiscoveryDraft : ChoreCaptureUiEvent

    data class DeleteDiscoveryDraft(val draftId: String) : ChoreCaptureUiEvent

    data class DiscoveryDraftTitleChanged(
        val draftId: String,
        val value: String,
    ) : ChoreCaptureUiEvent

    data class DiscoveryDraftRoomChanged(
        val draftId: String,
        val value: String,
    ) : ChoreCaptureUiEvent

    data class DiscoveryDraftEffortChanged(
        val draftId: String,
        val value: ChoreEffortValue,
    ) : ChoreCaptureUiEvent

    data class DiscoveryDraftProofRequiredChanged(
        val draftId: String,
        val enabled: Boolean,
    ) : ChoreCaptureUiEvent

    data class DiscoveryDraftRepeatsChanged(
        val draftId: String,
        val enabled: Boolean,
    ) : ChoreCaptureUiEvent

    data class DiscoveryDraftRepeatFrequencyChanged(
        val draftId: String,
        val value: ChoreRepeatFrequencyValue,
    ) : ChoreCaptureUiEvent

    data class DiscoveryDraftDailyModeChanged(
        val draftId: String,
        val value: ChoreDailyModeValue,
    ) : ChoreCaptureUiEvent

    data class DiscoveryDraftWeekdayToggled(
        val draftId: String,
        val value: String,
    ) : ChoreCaptureUiEvent

    data class DiscoveryDraftOneTimeDateChanged(
        val draftId: String,
        val value: String,
    ) : ChoreCaptureUiEvent

    data object SaveDiscoveryDrafts : ChoreCaptureUiEvent

    data object PremiumNavigationHandled : ChoreCaptureUiEvent

    data object DiscoveryReturnHandled : ChoreCaptureUiEvent

    data object ClearError : ChoreCaptureUiEvent
}
