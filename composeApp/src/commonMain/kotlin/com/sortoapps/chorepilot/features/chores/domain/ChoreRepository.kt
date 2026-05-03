package com.sortoapps.chorepilot.features.chores.domain

import kotlinx.coroutines.flow.StateFlow

interface ChoreRepository {
    val chores: StateFlow<List<ChoreItem>>
    val rooms: StateFlow<List<RoomDefinition>>
    val householdMembers: StateFlow<List<HouseholdSchedulingMember>>

    fun getChores(): List<ChoreItem>

    fun getChore(id: String, day: ChoreScheduleDay? = null): ChoreItem?

    fun getLatestProofForChore(
        choreId: String,
        day: ChoreScheduleDay? = null,
    ): ProofEvidence?

    fun getDraft(id: String): ChoreDraft?

    fun getScheduledChores(day: ChoreScheduleDay): List<ChoreItem>

    suspend fun getAvailabilitySuggestions(days: List<ChoreScheduleDay>): List<AvailabilitySuggestion>

    suspend fun refresh()

    suspend fun saveChore(draft: ChoreDraft): String

    suspend fun approvePendingChore(choreId: String): Unit = error("Pending chore approval is not supported.")

    suspend fun rejectPendingChore(
        choreId: String,
        reason: String,
    ): Unit = error("Pending chore rejection is not supported.")

    suspend fun deleteChore(choreId: String)

    suspend fun toggleCompletion(
        choreId: String,
        viewerName: String,
        allowCompletingForSomeoneElse: Boolean = false,
        day: ChoreScheduleDay? = null,
    )

    suspend fun submitProof(
        choreId: String,
        payload: ProofUploadPayload,
        day: ChoreScheduleDay? = null,
    ): ProofUploadResult

    suspend fun approveProof(choreId: String, day: ChoreScheduleDay? = null)

    suspend fun rejectProof(
        choreId: String,
        reason: String,
        day: ChoreScheduleDay? = null,
    )

    suspend fun acceptAvailabilitySuggestion(suggestion: AvailabilitySuggestion)

    suspend fun dismissAvailabilitySuggestion(suggestion: AvailabilitySuggestion)
}
