package com.sortoapps.chorepilot.features.chores.domain

import kotlinx.serialization.Serializable

@Serializable
enum class ChoreItemStatus {
    DueSoon,
    PendingProof,
    Completed,
    Overdue,
    Scheduled,
}

data class ChoreItem(
    val id: String,
    val title: String,
    val area: String,
    val cadence: String,
    val assignee: String,
    val assigneeId: String,
    val status: ChoreItemStatus,
    val durationLabel: String,
    val dueLabel: String,
    val description: String,
    val reminder: String,
    val tags: List<String>,
    val isActive: Boolean = true,
    val pendingApproval: Boolean = false,
    val assignmentReason: String = "",
    val assignmentOriginalAssignee: String = "",
    val completedByName: String = "",
    val completedAtEpochMs: Long = 0L,
    val proofRequired: Boolean = false,
    val proofPhotoCount: Int = 0,
    val submitterUserId: String = "",
    val submitterName: String = "",
    val reviewRejectedReason: String = "",
    val reviewedByName: String = "",
    val reviewedAtEpochMs: Long = 0L,
    val discoverySessionId: String = "",
    val discoveryImageStoragePath: String = "",
    val discoveryImageUrl: String = "",
    val discoveryGenerated: Boolean = false,
)
