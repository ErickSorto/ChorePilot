package com.sortoapps.chorepilot.features.feedbackpal.domain

import kotlinx.serialization.Serializable

@Serializable
data class FeedbackPalSubmission(
    val householdId: String,
    val householdName: String,
    val userId: String,
    val userName: String,
    val palKind: FeedbackPalKind,
    val reportedIntent: FeedbackIntent,
    val message: String,
    val screenRoute: String,
)

@Serializable
data class FeedbackPalTriageResult(
    val id: String = "",
    val type: String = "Idea",
    val priority: String = "P2",
    val priorityScore: Int = 50,
    val effort: String = "Medium",
    val summary: String = "",
    val signals: List<String> = emptyList(),
)

interface FeedbackPalRepository {
    suspend fun submitFeedback(
        reportedIntent: FeedbackIntent,
        message: String,
        screenRoute: String,
        palKind: FeedbackPalKind,
    ): FeedbackPalTriageResult
}
