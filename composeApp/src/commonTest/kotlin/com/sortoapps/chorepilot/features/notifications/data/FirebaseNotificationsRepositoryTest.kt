package com.sortoapps.chorepilot.features.notifications.data

import com.sortoapps.chorepilot.features.auth.domain.AuthFlowResult
import com.sortoapps.chorepilot.features.auth.domain.HouseholdRole
import com.sortoapps.chorepilot.features.auth.domain.InvitePreview
import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import com.sortoapps.chorepilot.features.auth.domain.SessionSnapshot
import com.sortoapps.chorepilot.features.notifications.domain.NotificationAuthorizationStatus
import com.sortoapps.chorepilot.features.notifications.domain.NotificationDeliveryGateway
import com.sortoapps.chorepilot.features.notifications.domain.NotificationEvent
import com.sortoapps.chorepilot.features.notifications.domain.NotificationItem
import com.sortoapps.chorepilot.features.notifications.domain.NotificationType
import com.sortoapps.chorepilot.features.notifications.domain.NotificationsRepository
import com.sortoapps.chorepilot.features.settings.domain.SettingsRepository
import com.sortoapps.chorepilot.features.settings.domain.SettingsSnapshot
import com.sortoapps.chorepilot.features.settings.domain.ThemeMode
import com.sortoapps.chorepilot.features.settings.domain.AppearancePreset
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseNotificationsRepositoryTest {
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
    fun proofSubmittedOnOneDeviceAppearsInAnotherMembersInbox() = runTest(dispatcher) {
        val sharedStore = FakeSharedNotificationsStore(
            members = listOf(
                fakeMember("owner-1", "Erick", HouseholdRole.Owner),
                fakeMember("member-1", "Ava", HouseholdRole.Member),
                fakeMember("member-2", "Mila", HouseholdRole.Member),
            ),
        )
        val ownerRepository = createRepository(
            sharedStore = sharedStore,
            sessionRepository = FakeBackendSessionRepository("owner-1"),
            appScope = backgroundScope,
        )
        val memberRepository = createRepository(
            sharedStore = sharedStore,
            sessionRepository = FakeBackendSessionRepository("member-2"),
            appScope = backgroundScope,
        )

        advanceUntilIdle()

        val recorded = ownerRepository.record(
            NotificationEvent.ProofSubmitted(
                householdId = "house-1",
                choreId = "chore-1",
                choreTitle = "Kitchen reset",
                dateKey = "2026-04-12",
                assigneeName = "Ava",
                submittingMemberName = "Erick",
                createdAtEpochMs = 10L,
            ),
        )
        memberRepository.refresh()
        advanceUntilIdle()

        assertNotNull(recorded)
        assertEquals("chore-1", recorded.choreId)
        assertEquals(emptyList(), memberRepository.state.value.items)
        assertEquals("chore-1", ownerRepository.state.value.items.firstOrNull()?.choreId)
        assertEquals(NotificationType.ProofSubmitted, ownerRepository.state.value.items.first().type)
    }

    @Test
    fun readStateStaysPerMemberAcrossSharedBackend() = runTest(dispatcher) {
        val sharedStore = FakeSharedNotificationsStore(
            members = listOf(
                fakeMember("owner-1", "Erick", HouseholdRole.Owner),
                fakeMember("member-2", "Ava", HouseholdRole.Member),
            ),
        )
        val ownerRepository = createRepository(
            sharedStore = sharedStore,
            sessionRepository = FakeBackendSessionRepository("owner-1"),
            appScope = backgroundScope,
        )
        val memberRepository = createRepository(
            sharedStore = sharedStore,
            sessionRepository = FakeBackendSessionRepository("member-2"),
            appScope = backgroundScope,
        )

        advanceUntilIdle()
        ownerRepository.record(
            NotificationEvent.ProofApproved(
                householdId = "house-1",
                choreId = "chore-2",
                choreTitle = "Trash night",
                dateKey = "2026-04-12",
                assigneeName = "Ava",
                submittingMemberName = "Ava",
                reviewerName = "Erick",
                completionOutcome = "approved_completed",
                recipientUserId = "member-2",
                createdAtEpochMs = 20L,
            ),
        )
        ownerRepository.refresh()
        memberRepository.refresh()
        advanceUntilIdle()

        val notificationId = memberRepository.state.value.items.first().id
        memberRepository.markRead(notificationId)
        ownerRepository.refresh()
        memberRepository.refresh()
        advanceUntilIdle()

        assertEquals(emptyList(), ownerRepository.state.value.items)
        assertNotNull(memberRepository.state.value.items.first { it.id == notificationId }.readAtEpochMs)
    }

    @Test
    fun proofRejectedOnlyAppearsForTheTargetMember() = runTest(dispatcher) {
        val sharedStore = FakeSharedNotificationsStore(
            members = listOf(
                fakeMember("owner-1", "Erick", HouseholdRole.Owner),
                fakeMember("member-2", "Ava", HouseholdRole.Member),
                fakeMember("member-3", "Mila", HouseholdRole.Member),
            ),
        )
        val ownerRepository = createRepository(
            sharedStore = sharedStore,
            sessionRepository = FakeBackendSessionRepository("owner-1"),
            appScope = backgroundScope,
        )
        val rejectedMemberRepository = createRepository(
            sharedStore = sharedStore,
            sessionRepository = FakeBackendSessionRepository("member-2"),
            appScope = backgroundScope,
        )
        val otherMemberRepository = createRepository(
            sharedStore = sharedStore,
            sessionRepository = FakeBackendSessionRepository("member-3"),
            appScope = backgroundScope,
        )

        advanceUntilIdle()
        ownerRepository.record(
            NotificationEvent.ProofRejected(
                householdId = "house-1",
                choreId = "chore-7",
                choreTitle = "Kitchen reset",
                dateKey = "2026-04-16",
                assigneeName = "Ava",
                submittingMemberName = "Ava",
                reviewerName = "Erick",
                rejectionReason = "Please show the full counter.",
                recipientUserId = "member-2",
                createdAtEpochMs = 33L,
            ),
        )
        rejectedMemberRepository.refresh()
        otherMemberRepository.refresh()
        advanceUntilIdle()

        assertEquals(NotificationType.ProofRejected, rejectedMemberRepository.state.value.items.single().type)
        assertEquals(emptyList(), otherMemberRepository.state.value.items)
    }

    @Test
    fun discoverySubmittedOnlyAppearsForManagers() = runTest(dispatcher) {
        val sharedStore = FakeSharedNotificationsStore(
            members = listOf(
                fakeMember("owner-1", "Erick", HouseholdRole.Owner),
                fakeMember("admin-1", "Ava", HouseholdRole.Admin),
                fakeMember("member-1", "Mila", HouseholdRole.Member),
            ),
        )
        val memberRepository = createRepository(
            sharedStore = sharedStore,
            sessionRepository = FakeBackendSessionRepository("member-1"),
            appScope = backgroundScope,
        )
        val adminRepository = createRepository(
            sharedStore = sharedStore,
            sessionRepository = FakeBackendSessionRepository("admin-1"),
            appScope = backgroundScope,
        )

        advanceUntilIdle()
        memberRepository.record(
            NotificationEvent.DiscoverySubmitted(
                householdId = "house-1",
                choreId = "discovery-1",
                choreTitle = "Straighten the shoe rack",
                submittingMemberName = "Mila",
                createdAtEpochMs = 120L,
            ),
        )
        adminRepository.refresh()
        memberRepository.refresh()
        advanceUntilIdle()

        assertEquals(NotificationType.DiscoverySubmitted, adminRepository.state.value.items.single().type)
        assertEquals(emptyList(), memberRepository.state.value.items)
    }

    @Test
    fun proofApprovedOnlyAppearsForTheApprovedMember() = runTest(dispatcher) {
        val sharedStore = FakeSharedNotificationsStore(
            members = listOf(
                fakeMember("owner-1", "Erick", HouseholdRole.Owner),
                fakeMember("member-2", "Ava", HouseholdRole.Member),
                fakeMember("member-3", "Mila", HouseholdRole.Member),
            ),
        )
        val ownerRepository = createRepository(
            sharedStore = sharedStore,
            sessionRepository = FakeBackendSessionRepository("owner-1"),
            appScope = backgroundScope,
        )
        val approvedMemberRepository = createRepository(
            sharedStore = sharedStore,
            sessionRepository = FakeBackendSessionRepository("member-2"),
            appScope = backgroundScope,
        )
        val otherMemberRepository = createRepository(
            sharedStore = sharedStore,
            sessionRepository = FakeBackendSessionRepository("member-3"),
            appScope = backgroundScope,
        )

        advanceUntilIdle()
        ownerRepository.record(
            NotificationEvent.ProofApproved(
                householdId = "house-1",
                choreId = "chore-9",
                choreTitle = "Kitchen reset",
                dateKey = "2026-04-16",
                assigneeName = "Ava",
                submittingMemberName = "Ava",
                reviewerName = "Erick",
                completionOutcome = "approved_completed",
                recipientUserId = "member-2",
                awardedPoints = 25,
                createdAtEpochMs = 84L,
            ),
        )
        approvedMemberRepository.refresh()
        otherMemberRepository.refresh()
        advanceUntilIdle()

        assertEquals(NotificationType.ProofApproved, approvedMemberRepository.state.value.items.single().type)
        assertEquals(emptyList(), otherMemberRepository.state.value.items)
    }

    @Test
    fun dueSoonAndAssignmentAlertsMatchRelevantMembersAndManagers() = runTest(dispatcher) {
        val sharedStore = FakeSharedNotificationsStore(
            members = listOf(
                fakeMember("owner-1", "Erick", HouseholdRole.Owner),
                fakeMember("admin-1", "Ava", HouseholdRole.Admin),
                fakeMember("member-1", "Emma", HouseholdRole.Member),
                fakeMember("member-2", "Mila", HouseholdRole.Member),
                fakeMember("member-3", "Noah", HouseholdRole.Member),
            ),
        )
        val ownerRepository = createRepository(
            sharedStore = sharedStore,
            sessionRepository = FakeBackendSessionRepository("owner-1"),
            appScope = backgroundScope,
        )
        val assigneeRepository = createRepository(
            sharedStore = sharedStore,
            sessionRepository = FakeBackendSessionRepository("member-1"),
            appScope = backgroundScope,
        )
        val coverRepository = createRepository(
            sharedStore = sharedStore,
            sessionRepository = FakeBackendSessionRepository("member-2"),
            appScope = backgroundScope,
        )
        val unrelatedRepository = createRepository(
            sharedStore = sharedStore,
            sessionRepository = FakeBackendSessionRepository("member-3"),
            appScope = backgroundScope,
        )

        advanceUntilIdle()
        ownerRepository.record(
            NotificationEvent.DueSoon(
                householdId = "house-1",
                choreId = "due-1",
                choreTitle = "Dog walk",
                dateKey = "2026-04-16",
                assigneeName = "Emma",
                dueLabel = "Due before dinner",
                createdAtEpochMs = 100L,
            ),
        )
        ownerRepository.record(
            NotificationEvent.AssignmentChanged(
                householdId = "house-1",
                choreId = "cover-1",
                choreTitle = "Bathroom reset",
                dateKey = "2026-04-16",
                originalAssigneeName = "Emma",
                newAssigneeName = "Mila",
                createdAtEpochMs = 110L,
            ),
        )
        assigneeRepository.refresh()
        coverRepository.refresh()
        unrelatedRepository.refresh()
        advanceUntilIdle()

        assertEquals(listOf("cover-1", "due-1"), ownerRepository.state.value.items.map { it.choreId })
        assertEquals(listOf("cover-1", "due-1"), assigneeRepository.state.value.items.map { it.choreId })
        assertEquals(listOf("cover-1"), coverRepository.state.value.items.map { it.choreId })
        assertEquals(emptyList(), unrelatedRepository.state.value.items)
    }

    private fun createRepository(
        sharedStore: FakeSharedNotificationsStore,
        sessionRepository: SessionRepository,
        appScope: CoroutineScope,
    ): NotificationsRepository = FirebaseNotificationsRepository(
        sharedStore = sharedStore,
        sessionRepository = sessionRepository,
        appScope = appScope,
    )
}

private class FakeSharedNotificationsStore(
    private val members: List<FakeSharedNotificationMember>,
) : SharedNotificationsStore {
    private val notifications = MutableStateFlow<Map<String, HouseholdNotificationDocument>>(emptyMap())

    override fun observeNotifications(
        householdId: String,
        recipientUserId: String,
    ): Flow<List<HouseholdNotificationDocument>> = notifications.map { current ->
        current.values
            .filter { document ->
                document.householdId == householdId && recipientUserId in document.recipientUserIds
            }
            .sortedByDescending(HouseholdNotificationDocument::createdAtEpochMs)
    }

    override suspend fun loadNotifications(
        householdId: String,
        recipientUserId: String,
    ): List<HouseholdNotificationDocument> = notifications.value.values
        .filter { document ->
            document.householdId == householdId && recipientUserId in document.recipientUserIds
        }
        .sortedByDescending(HouseholdNotificationDocument::createdAtEpochMs)

    override suspend fun loadNotification(
        householdId: String,
        notificationId: String,
    ): HouseholdNotificationDocument? = notifications.value[storageKey(householdId, notificationId)]

    override suspend fun upsertNotification(document: HouseholdNotificationDocument) {
        notifications.value = notifications.value + (storageKey(document.householdId, document.id) to document)
    }

    override suspend fun markRead(
        householdId: String,
        notificationId: String,
        userId: String,
        readAtEpochMs: Long,
    ) {
        val existing = loadNotification(householdId, notificationId) ?: return
        upsertNotification(
            existing.copy(
                readReceipts = existing.readReceipts
                    .filterNot { receipt -> receipt.userId == userId }
                    .plus(HouseholdNotificationReadReceiptDocument(userId = userId, readAtEpochMs = readAtEpochMs)),
            ),
        )
    }

    override suspend fun markAllRead(
        householdId: String,
        recipientUserId: String,
        readAtEpochMs: Long,
    ) {
        loadNotifications(householdId, recipientUserId).forEach { document ->
            markRead(
                householdId = householdId,
                notificationId = document.id,
                userId = recipientUserId,
                readAtEpochMs = readAtEpochMs,
            )
        }
    }

    override suspend fun activeRecipientUserIds(householdId: String): List<String> =
        members.map(FakeSharedNotificationMember::userId)

    override suspend fun activeRecipientUserIds(
        householdId: String,
        roles: Set<HouseholdRole>,
    ): List<String> = members
        .filter { roles.isEmpty() || it.role in roles }
        .map(FakeSharedNotificationMember::userId)

    override suspend fun activeRecipientUserIdsForMembers(
        householdId: String,
        memberKeys: Set<String>,
    ): List<String> {
        val normalizedKeys = memberKeys.map { it.trim().lowercase() }.filter(String::isNotBlank).toSet()
        return members
            .filter { member ->
                member.role == HouseholdRole.Owner ||
                    member.role == HouseholdRole.Admin ||
                    member.userId.lowercase() in normalizedKeys ||
                    member.displayName.lowercase() in normalizedKeys
            }
            .map(FakeSharedNotificationMember::userId)
    }

    private fun storageKey(
        householdId: String,
        notificationId: String,
    ): String = "$householdId|$notificationId"
}

private data class FakeSharedNotificationMember(
    val userId: String,
    val displayName: String,
    val role: HouseholdRole,
)

private fun fakeMember(
    userId: String,
    displayName: String,
    role: HouseholdRole,
): FakeSharedNotificationMember = FakeSharedNotificationMember(
    userId = userId,
    displayName = displayName,
    role = role,
)

private class FakeBackendSessionRepository(
    userId: String,
) : SessionRepository {
    override val session: StateFlow<SessionSnapshot> = MutableStateFlow(
        SessionSnapshot(
            signedIn = true,
            userId = userId,
            displayName = userId,
            householdId = "house-1",
            householdName = "River House",
            role = HouseholdRole.Member,
        ),
    )

    override suspend fun refreshSession() = Unit
    override suspend fun completeOnboarding(displayName: String) = Unit
    override suspend fun signIn(email: String, password: String, displayNameHint: String): AuthFlowResult = AuthFlowResult.Main
    override suspend fun signInWithGoogle(idToken: String, accessToken: String?, displayNameHint: String, emailHint: String): AuthFlowResult = AuthFlowResult.Main
    override suspend fun createHousehold(displayName: String, householdName: String, householdSizeLabel: String): AuthFlowResult = AuthFlowResult.Main
    override suspend fun joinHousehold(displayName: String, inviteCode: String): AuthFlowResult = AuthFlowResult.Main
    override suspend fun previewInvite(inviteCode: String): InvitePreview? = null
    override suspend fun dismissInitialPremiumFlow() = Unit
    override suspend fun switchActiveHousehold(householdId: String) = Unit
    override suspend fun signOut() = Unit
    override suspend fun resetDemo() = Unit
}

private class FakeBackendSettingsRepository : SettingsRepository {
    override val settings: StateFlow<SettingsSnapshot> = MutableStateFlow(
        SettingsSnapshot(notificationsEnabled = true),
    )

    override suspend fun updateThemeMode(themeMode: ThemeMode) = Unit
    override suspend fun updateAppearancePreset(appearancePreset: AppearancePreset) = Unit
    override suspend fun updateEditorialWarmth(enabled: Boolean) = Unit
    override suspend fun updateNotifications(enabled: Boolean) = Unit
    override suspend fun updateWeekStartsOnMonday(enabled: Boolean) = Unit
    override suspend fun updateAutoAssignSuggestions(enabled: Boolean) = Unit
    override suspend fun updateLargerText(enabled: Boolean) = Unit
    override suspend fun updateAlwaysShowLabels(enabled: Boolean) = Unit
}

private class FakeBackendNotificationGateway : NotificationDeliveryGateway {
    val delivered = mutableListOf<NotificationItem>()

    override suspend fun getAuthorizationStatus(): NotificationAuthorizationStatus =
        NotificationAuthorizationStatus.Granted

    override suspend fun requestAuthorization(): NotificationAuthorizationStatus =
        NotificationAuthorizationStatus.Granted

    override suspend fun deliver(item: NotificationItem) {
        delivered += item
    }
}
