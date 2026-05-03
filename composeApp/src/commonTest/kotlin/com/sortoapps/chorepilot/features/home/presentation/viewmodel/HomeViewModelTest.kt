package com.sortoapps.chorepilot.features.home.presentation.viewmodel

import com.sortoapps.chorepilot.core.demo.DemoChore
import com.sortoapps.chorepilot.core.demo.DemoChoreStatus
import com.sortoapps.chorepilot.core.platform.VibratorUtil
import com.sortoapps.chorepilot.features.auth.domain.AuthFlowResult
import com.sortoapps.chorepilot.features.auth.domain.HouseholdRole
import com.sortoapps.chorepilot.features.auth.domain.InvitePreview
import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import com.sortoapps.chorepilot.features.auth.domain.SessionSnapshot
import com.sortoapps.chorepilot.features.chores.domain.ChoreDraft
import com.sortoapps.chorepilot.features.chores.domain.ChoreItem
import com.sortoapps.chorepilot.features.chores.domain.ChoreItemStatus
import com.sortoapps.chorepilot.features.chores.domain.ChoreRepository
import com.sortoapps.chorepilot.features.chores.domain.ChoreScheduleDay
import com.sortoapps.chorepilot.features.chores.domain.ProofUploadPayload
import com.sortoapps.chorepilot.features.chores.domain.ProofEvidence
import com.sortoapps.chorepilot.features.chores.domain.ProofUploadResult
import com.sortoapps.chorepilot.features.chores.domain.RoomDefinition
import com.sortoapps.chorepilot.features.home.domain.HomeDaySnapshot
import com.sortoapps.chorepilot.features.home.domain.HomeRepository
import com.sortoapps.chorepilot.features.home.domain.HomeRepositoryState
import com.sortoapps.chorepilot.features.home.domain.HomeSnapshot
import com.sortoapps.chorepilot.features.settings.domain.SettingsRepository
import com.sortoapps.chorepilot.features.settings.domain.SettingsSnapshot
import com.sortoapps.chorepilot.features.settings.domain.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun ownChoreCompletesImmediatelyWithoutPrompt() = runTest(dispatcher) {
        val choreRepository = FakeChoreRepository(
            listOf(
                choreItem(id = "c1", assignee = "Erick"),
            ),
        )
        val viewModel = HomeViewModel(
            homeRepository = FakeHomeRepository(choreRepository.getChores()),
            choreRepository = choreRepository,
            settingsRepository = FakeSettingsRepository(),
            vibratorUtil = FakeVibratorUtil(),
        )

        advanceUntilIdle()
        viewModel.onEvent(HomeUiEvent.ToggleChoreCompletion("c1"))
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.completionPrompt)
        assertEquals(false, choreRepository.lastToggle?.allowCompletingForSomeoneElse)
        assertEquals("Erick", choreRepository.lastToggle?.viewerName)
        assertEquals(ChoreItemStatus.Completed, choreRepository.getChore("c1")?.status)
        assertEquals("", choreRepository.getChore("c1")?.completedByName)
    }

    @Test
    fun otherMembersChoreShowsPromptBeforeCompleting() = runTest(dispatcher) {
        val choreRepository = FakeChoreRepository(
            listOf(
                choreItem(id = "c1", assignee = "Emma"),
            ),
        )
        val viewModel = HomeViewModel(
            homeRepository = FakeHomeRepository(choreRepository.getChores()),
            choreRepository = choreRepository,
            settingsRepository = FakeSettingsRepository(),
            vibratorUtil = FakeVibratorUtil(),
        )

        advanceUntilIdle()
        viewModel.onEvent(HomeUiEvent.ToggleChoreCompletion("c1"))
        advanceUntilIdle()

        val prompt = viewModel.uiState.value.completionPrompt
        assertNotNull(prompt)
        assertEquals("Emma", prompt.assigneeName)
        assertEquals(null, choreRepository.lastToggle)
    }

    @Test
    fun confirmingOtherMembersChoreCompletesAndTracksWhoDidIt() = runTest(dispatcher) {
        val choreRepository = FakeChoreRepository(
            listOf(
                choreItem(id = "c1", assignee = "Emma"),
            ),
        )
        val viewModel = HomeViewModel(
            homeRepository = FakeHomeRepository(choreRepository.getChores()),
            choreRepository = choreRepository,
            settingsRepository = FakeSettingsRepository(),
            vibratorUtil = FakeVibratorUtil(),
        )

        advanceUntilIdle()
        viewModel.onEvent(HomeUiEvent.ToggleChoreCompletion("c1"))
        viewModel.onEvent(HomeUiEvent.ConfirmCompletionForAnotherMember)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.completionPrompt)
        assertEquals(true, choreRepository.lastToggle?.allowCompletingForSomeoneElse)
        assertEquals(ChoreItemStatus.Completed, choreRepository.getChore("c1")?.status)
        assertEquals("Erick", choreRepository.getChore("c1")?.completedByName)
    }

    @Test
    fun nonAdminCannotUncheckAnotherMembersCompletedChore() = runTest(dispatcher) {
        val choreRepository = FakeChoreRepository(
            listOf(
                choreItem(id = "c1", assignee = "Emma", status = ChoreItemStatus.Completed),
            ),
        )
        val viewModel = HomeViewModel(
            homeRepository = FakeHomeRepository(
                choreRepository.getChores(),
                viewerRole = HouseholdRole.Member,
            ),
            choreRepository = choreRepository,
            settingsRepository = FakeSettingsRepository(),
            vibratorUtil = FakeVibratorUtil(),
        )

        advanceUntilIdle()
        viewModel.onEvent(HomeUiEvent.ToggleChoreCompletion("c1"))
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.completionPrompt)
        assertEquals(null, choreRepository.lastToggle)
        assertEquals(ChoreItemStatus.Completed, choreRepository.getChore("c1")?.status)
    }

    @Test
    fun assigneeCanUncheckOwnCompletedChoreWithoutPrompt() = runTest(dispatcher) {
        val choreRepository = FakeChoreRepository(
            listOf(
                choreItem(id = "c1", assignee = "Erick", status = ChoreItemStatus.Completed),
            ),
        )
        val viewModel = HomeViewModel(
            homeRepository = FakeHomeRepository(
                choreRepository.getChores(),
                viewerRole = HouseholdRole.Member,
            ),
            choreRepository = choreRepository,
            settingsRepository = FakeSettingsRepository(),
            vibratorUtil = FakeVibratorUtil(),
        )

        advanceUntilIdle()
        viewModel.onEvent(HomeUiEvent.ToggleChoreCompletion("c1"))
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.completionPrompt)
        assertEquals(false, choreRepository.lastToggle?.allowCompletingForSomeoneElse)
        assertEquals(ChoreItemStatus.DueSoon, choreRepository.getChore("c1")?.status)
    }

    @Test
    fun adminCanUncheckAnotherMembersCompletedChore() = runTest(dispatcher) {
        val choreRepository = FakeChoreRepository(
            listOf(
                choreItem(id = "c1", assignee = "Emma", status = ChoreItemStatus.Completed),
            ),
        )
        val viewModel = HomeViewModel(
            homeRepository = FakeHomeRepository(
                choreRepository.getChores(),
                viewerRole = HouseholdRole.Admin,
            ),
            choreRepository = choreRepository,
            settingsRepository = FakeSettingsRepository(),
            vibratorUtil = FakeVibratorUtil(),
        )

        advanceUntilIdle()
        viewModel.onEvent(HomeUiEvent.ToggleChoreCompletion("c1"))
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.completionPrompt)
        assertEquals(false, choreRepository.lastToggle?.allowCompletingForSomeoneElse)
        assertEquals(ChoreItemStatus.DueSoon, choreRepository.getChore("c1")?.status)
    }

    @Test
    fun loadsReassignmentSuggestionsForSelectedDay() = runTest(dispatcher) {
        val suggestion = com.sortoapps.chorepilot.features.chores.domain.AvailabilitySuggestion(
            id = "s1",
            choreId = "c1",
            choreTitle = "Trash",
            dateKey = "2026-04-08",
            weekdayLabel = "Tuesday",
            originalAssignee = "Erick",
            recommendedCoverMember = "Emma",
            reasonText = "Covering for Erick while they're away.",
            scopeStartDate = "2026-04-08",
            scopeEndDate = "2026-04-10",
            outsideNormalRotation = false,
        )
        val choreRepository = FakeChoreRepository(
            initial = listOf(choreItem(id = "c1", assignee = "Erick")),
            suggestions = listOf(suggestion),
        )

        val viewModel = HomeViewModel(
            homeRepository = FakeHomeRepository(choreRepository.getChores()),
            choreRepository = choreRepository,
            settingsRepository = FakeSettingsRepository(),
            vibratorUtil = FakeVibratorUtil(),
        )

        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.reassignmentSuggestions.size)
        assertEquals("Emma", viewModel.uiState.value.reassignmentSuggestions.first().coverMember)
    }

    @Test
    fun acceptingReassignmentSuggestionPersistsDecisionAndRemovesIt() = runTest(dispatcher) {
        val suggestion = com.sortoapps.chorepilot.features.chores.domain.AvailabilitySuggestion(
            id = "s1",
            choreId = "c1",
            choreTitle = "Trash",
            dateKey = "2026-04-08",
            weekdayLabel = "Tuesday",
            originalAssignee = "Erick",
            recommendedCoverMember = "Emma",
            reasonText = "Covering for Erick while they're away.",
            scopeStartDate = "2026-04-08",
            scopeEndDate = "2026-04-10",
            outsideNormalRotation = false,
        )
        val choreRepository = FakeChoreRepository(
            initial = listOf(choreItem(id = "c1", assignee = "Erick")),
            suggestions = listOf(suggestion),
        )

        val viewModel = HomeViewModel(
            homeRepository = FakeHomeRepository(choreRepository.getChores()),
            choreRepository = choreRepository,
            settingsRepository = FakeSettingsRepository(),
            vibratorUtil = FakeVibratorUtil(),
        )

        advanceUntilIdle()
        viewModel.onEvent(HomeUiEvent.ReassignmentAccepted("s1"))
        advanceUntilIdle()

        assertEquals("s1", choreRepository.acceptedSuggestionId)
        assertTrue(viewModel.uiState.value.reassignmentSuggestions.isEmpty())
    }

    private fun choreItem(
        id: String,
        assignee: String,
        status: ChoreItemStatus = ChoreItemStatus.DueSoon,
    ): ChoreItem = ChoreItem(
        id = id,
        title = "Take out trash",
        area = "Outside",
        cadence = "Daily",
        assignee = assignee,
        assigneeId = assignee.lowercase(),
        status = status,
        durationLabel = "10 min",
        dueLabel = "Today",
        description = "",
        reminder = "",
        tags = listOf("Outside"),
    )
}

private class FakeHomeRepository(
    chores: List<ChoreItem>,
    viewerRole: HouseholdRole = HouseholdRole.Owner,
) : HomeRepository {
    override val state: StateFlow<HomeRepositoryState> = MutableStateFlow(
        HomeRepositoryState(
            isLoading = false,
            snapshot = HomeSnapshot(
                householdName = "River House",
                viewerName = "Erick",
                viewerId = "erick",
                viewerRole = viewerRole,
                headline = "Today",
                supportingText = "Support",
                days = listOf(
                    HomeDaySnapshot(
                        id = "tue",
                        label = "Tuesday",
                        shortLabel = "Tue",
                        supportingText = "Today",
                        dayOffset = 0,
                        dateKey = "2026-04-08",
                        isToday = true,
                    ),
                ),
                metrics = emptyList(),
                focusChores = listOf(
                    DemoChore(
                        id = "demo",
                        title = "Demo",
                        area = "Outside",
                        cadence = "Daily",
                        assignee = "Erick",
                        assigneeId = "erick",
                        status = DemoChoreStatus.DueSoon,
                        durationLabel = "10 min",
                        dueLabel = "Today",
                        description = "",
                        reminder = "",
                        tags = listOf("Outside"),
                    ),
                ),
                choresByDay = mapOf("tue" to chores),
            ),
        ),
    )

    override suspend fun refresh() = Unit
}

private class FakeSessionRepository(
    role: HouseholdRole = HouseholdRole.Owner,
) : SessionRepository {
    override val session: StateFlow<SessionSnapshot> = MutableStateFlow(
        SessionSnapshot(
            signedIn = true,
            userId = "erick",
            displayName = "Erick",
            householdId = "house-1",
            householdName = "River House",
            role = role,
        ),
    )

    override suspend fun refreshSession() = Unit

    override suspend fun completeOnboarding(displayName: String) = Unit

    override suspend fun signIn(email: String, password: String, displayNameHint: String): AuthFlowResult = AuthFlowResult.Main

    override suspend fun signInWithGoogle(
        idToken: String,
        accessToken: String?,
        displayNameHint: String,
        emailHint: String,
    ): AuthFlowResult = AuthFlowResult.Main

    override suspend fun createHousehold(
        displayName: String,
        householdName: String,
        householdSizeLabel: String,
    ): AuthFlowResult = AuthFlowResult.Main

    override suspend fun joinHousehold(displayName: String, inviteCode: String): AuthFlowResult = AuthFlowResult.Main

    override suspend fun previewInvite(inviteCode: String): InvitePreview? = null

    override suspend fun dismissInitialPremiumFlow() = Unit

    override suspend fun signOut() = Unit

    override suspend fun resetDemo() = Unit
}

private class FakeVibratorUtil : VibratorUtil {
    override fun triggerVibration() = Unit

    override fun triggerVibrationSuccess() = Unit

    override fun cancelVibration() = Unit
}

private class FakeSettingsRepository(
    initial: SettingsSnapshot = SettingsSnapshot(),
) : SettingsRepository {
    override val settings: StateFlow<SettingsSnapshot> = MutableStateFlow(initial)

    override suspend fun updateThemeMode(themeMode: ThemeMode) = Unit

    override suspend fun updateEditorialWarmth(enabled: Boolean) = Unit

    override suspend fun updateNotifications(enabled: Boolean) = Unit

    override suspend fun updateWeekStartsOnMonday(enabled: Boolean) = Unit

    override suspend fun updateAutoAssignSuggestions(enabled: Boolean) = Unit

    override suspend fun updateLargerText(enabled: Boolean) = Unit

    override suspend fun updateAlwaysShowLabels(enabled: Boolean) = Unit
}

private class FakeChoreRepository(
    initial: List<ChoreItem>,
    suggestions: List<com.sortoapps.chorepilot.features.chores.domain.AvailabilitySuggestion> = emptyList(),
) : ChoreRepository {
    private val choreState = MutableStateFlow(initial)
    private val suggestionState = MutableStateFlow(suggestions)

    data class ToggleCall(
        val choreId: String,
        val viewerName: String,
        val allowCompletingForSomeoneElse: Boolean,
        val day: ChoreScheduleDay?,
    )

    var lastToggle: ToggleCall? = null
        private set

    var acceptedSuggestionId: String? = null
        private set

    override val chores: StateFlow<List<ChoreItem>> = choreState
    override val rooms: StateFlow<List<RoomDefinition>> = MutableStateFlow(emptyList())
    override val householdMembers: StateFlow<List<com.sortoapps.chorepilot.features.chores.domain.HouseholdSchedulingMember>> = MutableStateFlow(emptyList())

    override fun getChores(): List<ChoreItem> = choreState.value

    override fun getChore(id: String, day: ChoreScheduleDay?): ChoreItem? = choreState.value.firstOrNull { it.id == id }

    override fun getLatestProofForChore(
        choreId: String,
        day: ChoreScheduleDay?,
    ): ProofEvidence? = null

    override fun getDraft(id: String): ChoreDraft? = null

    override fun getScheduledChores(day: ChoreScheduleDay): List<ChoreItem> = choreState.value

    override suspend fun refresh() = Unit

    override suspend fun saveChore(draft: ChoreDraft): String = draft.id ?: "new-chore"

    override suspend fun deleteChore(choreId: String) = Unit

    override suspend fun toggleCompletion(
        choreId: String,
        viewerName: String,
        allowCompletingForSomeoneElse: Boolean,
        day: ChoreScheduleDay?,
    ) {
        lastToggle = ToggleCall(choreId, viewerName, allowCompletingForSomeoneElse, day)
        choreState.value = choreState.value.map { chore ->
            if (chore.id != choreId) {
                chore
            } else {
                chore.copy(
                    status = if (chore.status == ChoreItemStatus.Completed || chore.status == ChoreItemStatus.PendingProof) {
                        ChoreItemStatus.DueSoon
                    } else {
                        ChoreItemStatus.Completed
                    },
                    completedByName = if (chore.status == ChoreItemStatus.Completed || chore.status == ChoreItemStatus.PendingProof) {
                        ""
                    } else if (allowCompletingForSomeoneElse && !chore.assignee.equals(viewerName, ignoreCase = true)) {
                        viewerName
                    } else {
                        ""
                    },
                )
            }
        }
    }

    override suspend fun submitProof(
        choreId: String,
        payload: ProofUploadPayload,
        day: ChoreScheduleDay?,
    ): ProofUploadResult = ProofUploadResult(
        proofId = "test-proof",
        storagePath = "debug/test-proof.jpg",
        downloadUrl = "debug://test-proof",
    )

    override suspend fun approveProof(choreId: String, day: ChoreScheduleDay?) = Unit

    override suspend fun rejectProof(choreId: String, reason: String, day: ChoreScheduleDay?) = Unit

    override suspend fun getAvailabilitySuggestions(days: List<ChoreScheduleDay>) = suggestionState.value.filter { suggestion ->
        days.any { it.dateKey == suggestion.dateKey }
    }

    override suspend fun acceptAvailabilitySuggestion(suggestion: com.sortoapps.chorepilot.features.chores.domain.AvailabilitySuggestion) {
        acceptedSuggestionId = suggestion.id
        suggestionState.value = suggestionState.value.filterNot { it.id == suggestion.id }
    }

    override suspend fun dismissAvailabilitySuggestion(suggestion: com.sortoapps.chorepilot.features.chores.domain.AvailabilitySuggestion) {
        suggestionState.value = suggestionState.value.filterNot { it.id == suggestion.id }
    }
}
