package com.sortoapps.chorepilot.features.feedbackpal.data

import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import com.sortoapps.chorepilot.features.feedbackpal.domain.FeedbackIntent
import com.sortoapps.chorepilot.features.feedbackpal.domain.FeedbackPalKind
import com.sortoapps.chorepilot.features.feedbackpal.domain.FeedbackPalRepository
import com.sortoapps.chorepilot.features.feedbackpal.domain.FeedbackPalSubmission
import com.sortoapps.chorepilot.features.feedbackpal.domain.FeedbackPalTriageResult
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.functions.functions

class FirebaseFeedbackPalRepository(
    private val sessionRepository: SessionRepository,
) : FeedbackPalRepository {
    private val functions = Firebase.functions

    override suspend fun submitFeedback(
        reportedIntent: FeedbackIntent,
        message: String,
        screenRoute: String,
        palKind: FeedbackPalKind,
    ): FeedbackPalTriageResult {
        val session = sessionRepository.session.value
        val householdId = session.householdId.takeIf(String::isNotBlank)
            ?: error("Join a household before sending feedback.")
        val userId = session.userId.takeIf(String::isNotBlank)
            ?: error("Sign in before sending feedback.")
        val trimmedMessage = message.trim()
        if (trimmedMessage.length < 8) {
            error("Add a little more detail so this can be triaged.")
        }

        return functions
            .httpsCallable("submitFeedbackPalItem")
            .invoke(
                FeedbackPalSubmission(
                    householdId = householdId,
                    householdName = session.householdName,
                    userId = userId,
                    userName = session.displayName,
                    palKind = palKind,
                    reportedIntent = reportedIntent,
                    message = trimmedMessage,
                    screenRoute = screenRoute,
                ),
            )
            .data()
    }
}
