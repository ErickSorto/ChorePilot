package com.sortoapps.chorepilot.features.chorecapture.data

import com.sortoapps.chorepilot.core.platform.localImagePathToBase64
import com.sortoapps.chorepilot.core.platform.localImagePathToFirebaseStorageData
import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import com.sortoapps.chorepilot.features.chorecapture.domain.DiscoveryAnalysisRepository
import com.sortoapps.chorepilot.features.chorecapture.domain.DiscoveryAnalysisRequest
import com.sortoapps.chorepilot.features.chorecapture.domain.DiscoveryAnalysisResult
import com.sortoapps.chorepilot.features.chorecapture.domain.DiscoveryDraftItem
import com.sortoapps.chorepilot.features.chorecapture.domain.DiscoverySourceImage
import com.sortoapps.chorepilot.features.chores.domain.ProofUploadPayload
import com.sortoapps.chorepilot.core.firebase.StoragePaths
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.functions.functions
import dev.gitlive.firebase.storage.storage
import dev.gitlive.firebase.storage.storageMetadata
import kotlin.text.Regex

class FirebaseDiscoveryAnalysisRepository(
    private val sessionRepository: SessionRepository,
) : DiscoveryAnalysisRepository {
    private val functions = Firebase.functions
    private val storage = Firebase.storage

    override suspend fun analyzeRoomPhoto(
        payload: ProofUploadPayload,
    ): DiscoveryAnalysisResult {
        val session = sessionRepository.session.value
        val householdId = session.householdId.takeIf(String::isNotBlank)
            ?: error("Create or join a household before analyzing a room photo.")
        val userId = session.userId.takeIf(String::isNotBlank)
            ?: error("Sign in before using discovery mode.")
        val request = DiscoveryAnalysisRequest(
            householdId = householdId,
            householdName = session.householdName,
            userId = userId,
            userName = session.displayName,
            mimeType = payload.mimeType,
            fileExtension = payload.fileExtension,
            imageBase64 = localImagePathToBase64(payload.localPath),
        )
        val response = functions
            .httpsCallable("analyzeDiscoveryRoom")
            .invoke(request)
            .data<DiscoveryAnalysisResult>()
        val cleanedDrafts = response.drafts
            .map { draft ->
                val normalizedTitle = normalizeDiscoveryTitle(draft.title)
                draft.copy(
                    title = normalizedTitle,
                    description = normalizeDiscoveryDescription(
                        title = normalizedTitle,
                        rawTitle = draft.title,
                        rawDescription = draft.description,
                    ),
                    roomName = draft.roomName.trim(),
                    selectedOneTimeDate = sanitizeOneTimeDate(draft.selectedOneTimeDate),
                )
            }
            .filter { it.title.isNotBlank() && it.roomName.isNotBlank() }
        if (cleanedDrafts.isEmpty()) {
            error("We couldn't turn that room photo into chores yet. Try a brighter, wider shot.")
        }
        return response.copy(drafts = cleanedDrafts)
    }

    override suspend fun persistAcceptedRoomPhoto(
        discoverySessionId: String,
        payload: ProofUploadPayload,
    ): DiscoverySourceImage {
        val session = sessionRepository.session.value
        val householdId = session.householdId.takeIf(String::isNotBlank)
            ?: error("Create or join a household before saving discovery chores.")
        val userId = session.userId.takeIf(String::isNotBlank)
            ?: error("Sign in before saving discovery chores.")
        val extension = payload.fileExtension.trimStart('.').ifBlank { "jpg" }
        val storagePath = StoragePaths.discoveryImage(
            householdId = householdId,
            userId = userId,
            discoverySessionId = discoverySessionId,
            fileExtension = extension,
        )
        val storageRef = storage.reference(storagePath)
        storageRef.putData(
            data = localImagePathToFirebaseStorageData(payload.localPath),
            metadata = storageMetadata {
                contentType = payload.mimeType
                setCustomMetadata("householdId", householdId)
                setCustomMetadata("userId", userId)
                setCustomMetadata("discoverySessionId", discoverySessionId)
            },
        )
        return DiscoverySourceImage(
            discoverySessionId = discoverySessionId,
            storagePath = storagePath,
            downloadUrl = storageRef.getDownloadUrl(),
        )
    }
}

private fun sanitizeOneTimeDate(
    raw: String,
): String {
    val normalized = raw.trim()
    if (normalized.isBlank()) return "Tomorrow"
    val supported = setOf("Today", "Tomorrow", "This Friday", "Next Monday")
    if (normalized in supported) return normalized
    if (Regex("""\b(19|20)\d{2}\b""").containsMatchIn(normalized)) return "Tomorrow"
    return when {
        normalized.contains("today", ignoreCase = true) -> "Today"
        normalized.contains("tomorrow", ignoreCase = true) -> "Tomorrow"
        normalized.contains("friday", ignoreCase = true) -> "This Friday"
        normalized.contains("monday", ignoreCase = true) -> "Next Monday"
        else -> "Tomorrow"
    }
}

private fun normalizeDiscoveryTitle(
    raw: String,
): String {
    val trimmed = raw.trim().removeSuffix(".")
    if (trimmed.isBlank()) return ""
    val compact = trimmed
        .replace(Regex("""\s+"""), " ")
        .split(Regex("""\b(?:because|since|so that|and then|then|while|where)\b""", RegexOption.IGNORE_CASE))
        .first()
        .trim()
        .trim(',', ';', ':', '-', ' ')
    val words = compact.split(" ").filter { it.isNotBlank() }
    val limited = words.take(5).joinToString(" ")
    return limited.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

private fun normalizeDiscoveryDescription(
    title: String,
    rawTitle: String,
    rawDescription: String,
): String {
    val description = rawDescription.trim()
    if (description.isNotBlank()) return description
    val extraFromTitle = rawTitle.trim()
        .removePrefix(title)
        .trim()
        .trim(',', ';', ':', '-', ' ')
    return extraFromTitle
}

class InMemoryDiscoveryAnalysisRepository : DiscoveryAnalysisRepository {
    override suspend fun analyzeRoomPhoto(
        payload: ProofUploadPayload,
    ): DiscoveryAnalysisResult = DiscoveryAnalysisResult(
        drafts = listOf(
            DiscoveryDraftItem(
                id = "draft-counter",
                title = "Clear the kitchen counters",
                description = "Wipe crumbs, clear small clutter, and leave the counters ready for the next meal.",
                roomName = "Kitchen",
                effort = com.sortoapps.chorepilot.features.chores.domain.ChoreEffortValue.Standard,
                proofNeededSuggested = true,
                repeatsEnabled = true,
                repeatFrequency = com.sortoapps.chorepilot.features.chores.domain.ChoreRepeatFrequencyValue.Weekly,
                selectedWeekdays = setOf("Sunday"),
            ),
            DiscoveryDraftItem(
                id = "draft-floor",
                title = "Sweep the floor",
                description = "Pick up scattered toys first, then sweep the visible debris around the living room.",
                roomName = "Living room",
                effort = com.sortoapps.chorepilot.features.chores.domain.ChoreEffortValue.Heavy,
                proofNeededSuggested = false,
                repeatsEnabled = false,
                selectedOneTimeDate = "Tomorrow",
            ),
        ),
    )

    override suspend fun persistAcceptedRoomPhoto(
        discoverySessionId: String,
        payload: ProofUploadPayload,
    ): DiscoverySourceImage = DiscoverySourceImage(
        discoverySessionId = discoverySessionId,
        storagePath = "debug/discovery/$discoverySessionId.${payload.fileExtension.trimStart('.')}",
        downloadUrl = "",
    )
}
