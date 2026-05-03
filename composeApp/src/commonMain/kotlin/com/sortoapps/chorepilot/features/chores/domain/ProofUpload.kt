package com.sortoapps.chorepilot.features.chores.domain

import kotlinx.serialization.Serializable

data class ProofUploadPayload(
    val localPath: String,
    val previewUri: String,
    val mimeType: String = "image/jpeg",
    val fileExtension: String = "jpg",
)

data class ProofUploadResult(
    val proofId: String,
    val storagePath: String,
    val downloadUrl: String,
)

@Serializable
enum class ProofReviewStatus {
    Pending,
    Approved,
    Rejected,
}

data class ProofEvidence(
    val proofId: String,
    val choreId: String,
    val dateKey: String,
    val uploadedByUserId: String = "",
    val uploadedByName: String,
    val storagePath: String,
    val downloadUrl: String,
    val createdAtEpochMs: Long,
    val reviewStatus: ProofReviewStatus = ProofReviewStatus.Pending,
    val reviewedByName: String = "",
    val reviewedAtEpochMs: Long = 0L,
    val rejectionReason: String = "",
)
