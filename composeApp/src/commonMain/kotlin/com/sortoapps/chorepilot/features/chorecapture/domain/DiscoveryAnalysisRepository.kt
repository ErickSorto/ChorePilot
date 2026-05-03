package com.sortoapps.chorepilot.features.chorecapture.domain

import com.sortoapps.chorepilot.features.chores.domain.ChoreDailyModeValue
import com.sortoapps.chorepilot.features.chores.domain.ChoreEffortValue
import com.sortoapps.chorepilot.features.chores.domain.ChoreRepeatFrequencyValue
import com.sortoapps.chorepilot.features.chores.domain.ProofUploadPayload
import kotlinx.serialization.Serializable

@Serializable
data class DiscoveryAnalysisRequest(
    val householdId: String,
    val householdName: String,
    val userId: String,
    val userName: String,
    val mimeType: String,
    val fileExtension: String,
    val imageBase64: String,
)

@Serializable
data class DiscoveryDraftItem(
    val id: String,
    val title: String,
    val description: String = "",
    val roomName: String,
    val effort: ChoreEffortValue,
    val proofNeededSuggested: Boolean,
    val repeatsEnabled: Boolean = false,
    val repeatFrequency: ChoreRepeatFrequencyValue = ChoreRepeatFrequencyValue.Weekly,
    val dailyMode: ChoreDailyModeValue = ChoreDailyModeValue.Weekdays,
    val selectedWeekdays: Set<String> = emptySet(),
    val selectedOneTimeDate: String = "Tomorrow",
)

@Serializable
data class DiscoveryAnalysisResult(
    val drafts: List<DiscoveryDraftItem>,
)

data class DiscoverySourceImage(
    val discoverySessionId: String,
    val storagePath: String,
    val downloadUrl: String,
)

interface DiscoveryAnalysisRepository {
    suspend fun analyzeRoomPhoto(
        payload: ProofUploadPayload,
    ): DiscoveryAnalysisResult

    suspend fun persistAcceptedRoomPhoto(
        discoverySessionId: String,
        payload: ProofUploadPayload,
    ): DiscoverySourceImage
}
