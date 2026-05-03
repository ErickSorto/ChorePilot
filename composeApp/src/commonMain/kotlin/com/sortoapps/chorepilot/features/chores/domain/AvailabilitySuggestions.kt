package com.sortoapps.chorepilot.features.chores.domain

import kotlinx.serialization.Serializable

data class AvailabilitySuggestion(
    val id: String,
    val choreId: String,
    val choreTitle: String,
    val dateKey: String,
    val weekdayLabel: String,
    val originalAssignee: String,
    val recommendedCoverMember: String,
    val reasonText: String,
    val scopeStartDate: String,
    val scopeEndDate: String,
    val outsideNormalRotation: Boolean,
)

@Serializable
enum class AvailabilitySuggestionDecisionStatus {
    Accepted,
    Dismissed,
}

@Serializable
data class AvailabilitySuggestionDecision(
    val suggestionId: String = "",
    val choreId: String = "",
    val originalAssignee: String = "",
    val coverMember: String = "",
    val scopeStartDate: String = "",
    val scopeEndDate: String = "",
    val status: AvailabilitySuggestionDecisionStatus = AvailabilitySuggestionDecisionStatus.Dismissed,
    val createdAtEpochMs: Long = 0L,
)

internal fun availabilitySuggestionId(
    choreId: String,
    originalAssignee: String,
    scopeStartDate: String,
    scopeEndDate: String,
): String = listOf(
    choreId.trim(),
    originalAssignee.trim().lowercase(),
    scopeStartDate.trim(),
    scopeEndDate.trim(),
).joinToString(separator = "|")

internal fun AvailabilitySuggestionDecision.appliesTo(
    choreId: String,
    originalAssignee: String,
    dateKey: String,
): Boolean = this.choreId == choreId &&
    this.originalAssignee.equals(originalAssignee, ignoreCase = true) &&
    scopeStartDate.isNotBlank() &&
    scopeEndDate.isNotBlank() &&
    dateKey >= scopeStartDate &&
    dateKey <= scopeEndDate
