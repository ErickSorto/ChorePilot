package com.sortoapps.chorepilot.features.chores.data.model

import com.sortoapps.chorepilot.features.chores.domain.AvailabilitySuggestionDecision
import com.sortoapps.chorepilot.features.chores.domain.AvailabilitySuggestionDecisionStatus
import kotlinx.serialization.Serializable

@Serializable
data class ReassignmentDecisionDocument(
    val suggestionId: String = "",
    val choreId: String = "",
    val originalAssignee: String = "",
    val coverMember: String = "",
    val scopeStartDate: String = "",
    val scopeEndDate: String = "",
    val status: AvailabilitySuggestionDecisionStatus = AvailabilitySuggestionDecisionStatus.Dismissed,
    val createdAtEpochMs: Long = 0L,
)

fun ReassignmentDecisionDocument.toDecision(): AvailabilitySuggestionDecision = AvailabilitySuggestionDecision(
    suggestionId = suggestionId,
    choreId = choreId,
    originalAssignee = originalAssignee,
    coverMember = coverMember,
    scopeStartDate = scopeStartDate,
    scopeEndDate = scopeEndDate,
    status = status,
    createdAtEpochMs = createdAtEpochMs,
)

fun AvailabilitySuggestionDecision.toDocument(): ReassignmentDecisionDocument = ReassignmentDecisionDocument(
    suggestionId = suggestionId,
    choreId = choreId,
    originalAssignee = originalAssignee,
    coverMember = coverMember,
    scopeStartDate = scopeStartDate,
    scopeEndDate = scopeEndDate,
    status = status,
    createdAtEpochMs = createdAtEpochMs,
)
