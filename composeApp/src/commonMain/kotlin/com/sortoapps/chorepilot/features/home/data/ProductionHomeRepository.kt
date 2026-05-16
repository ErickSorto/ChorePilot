package com.sortoapps.chorepilot.features.home.data

import com.sortoapps.chorepilot.core.demo.DemoMetric
import com.sortoapps.chorepilot.core.demo.DemoTone
import com.sortoapps.chorepilot.core.firebase.FirebaseHouseholdStore
import com.sortoapps.chorepilot.features.auth.domain.HouseholdRole
import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import com.sortoapps.chorepilot.features.auth.domain.SessionSnapshot
import com.sortoapps.chorepilot.features.chores.domain.ChoreItem
import com.sortoapps.chorepilot.features.chores.domain.ChoreItemStatus
import com.sortoapps.chorepilot.features.chores.domain.ChoreRepository
import com.sortoapps.chorepilot.features.chores.domain.ChoreScheduleDay
import com.sortoapps.chorepilot.features.home.domain.HomeRepository
import com.sortoapps.chorepilot.features.home.domain.HomeDaySnapshot
import com.sortoapps.chorepilot.features.home.domain.HomeRepositoryState
import com.sortoapps.chorepilot.features.home.domain.HomeSnapshot
import dev.gitlive.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlinx.serialization.Serializable
import kotlin.time.Clock

class ProductionHomeRepository(
    private val choreRepository: ChoreRepository,
    private val sessionRepository: SessionRepository,
    private val householdStore: FirebaseHouseholdStore,
    appScope: CoroutineScope,
) : HomeRepository {
    private val _state = MutableStateFlow(HomeRepositoryState())
    override val state: StateFlow<HomeRepositoryState> = _state.asStateFlow()

    init {
        appScope.launch {
            combine(sessionRepository.session, choreRepository.chores) { session, _ -> session }
                .collectLatest { session ->
                    publishSnapshot(
                        session = session,
                        keepLoading = _state.value.isLoading,
                        errorMessage = _state.value.errorMessage,
                    )
                }
        }
    }

    override suspend fun refresh() {
        _state.value = _state.value.copy(isLoading = true, errorMessage = null)
        runCatching {
            choreRepository.refresh()
            publishSnapshot(
                session = sessionRepository.session.value,
                keepLoading = false,
                errorMessage = null,
            )
        }.onFailure { error ->
            _state.value = _state.value.copy(
                isLoading = false,
                errorMessage = error.message ?: "We couldn't load your dashboard right now.",
            )
        }
    }

    private suspend fun publishSnapshot(
        session: SessionSnapshot,
        keepLoading: Boolean,
        errorMessage: String?,
    ) {
        val snapshot = buildSnapshot(session)
        _state.value = HomeRepositoryState(
            isLoading = keepLoading,
            snapshot = snapshot,
            errorMessage = errorMessage,
        )
    }

    private suspend fun buildSnapshot(session: SessionSnapshot): HomeSnapshot {
        val household = loadHouseholdSafely(session.householdId)
        val members = loadMembersSafely(session.householdId)
        val viewer = members.firstOrNull { it.userId == session.userId }

        val householdName = household?.name.orEmpty()
            .ifBlank { session.householdName.ifBlank { "Your household" } }
        val viewerName = viewer?.displayName.orEmpty()
            .ifBlank { session.displayName.ifBlank { "there" } }
        val viewerId = session.userId
        val viewerRole = viewer?.role ?: session.role

        val days = buildScheduleWindowDays()
        val choresByDay = days.associate { day ->
            day.id to choreRepository.getScheduledChores(day.toScheduleDay())
        }
        val todayChores = days.firstOrNull { it.isToday }
            ?.let { choresByDay[it.id].orEmpty() }
            .orEmpty()
        val weeklyChores = choresByDay.values.flatten()

        return HomeSnapshot(
            householdName = householdName,
            viewerName = viewerName,
            viewerId = viewerId,
            viewerRole = viewerRole,
            headline = buildHeadline(
                viewerName = viewerName,
                householdName = householdName,
                todayChores = todayChores,
            ),
            supportingText = buildSupportingText(
                weeklyChores = weeklyChores,
                todayChores = todayChores,
                memberCount = household?.memberCount ?: members.size,
            ),
            days = days.map { day ->
                HomeDaySnapshot(
                    id = day.id,
                    label = day.label,
                    shortLabel = day.shortLabel,
                    supportingText = buildDaySupportingText(
                        chores = choresByDay[day.id].orEmpty(),
                        day = day,
                    ),
                    dayOffset = day.dayOffset,
                    dateKey = day.dateKey,
                    isToday = day.isToday,
                )
            },
            metrics = buildMetrics(
                todayChores = todayChores,
                weeklyChores = weeklyChores,
            ),
            focusChores = emptyList(),
            choresByDay = choresByDay,
        )
    }

    private suspend fun loadHouseholdSafely(householdId: String) = householdId
        .takeIf(String::isNotBlank)
        ?.let { runCatching { householdStore.loadHousehold(it) }.getOrNull() }

    private suspend fun loadMembersSafely(householdId: String): List<HomeMemberSource> {
        if (householdId.isBlank()) return emptyList()
        return runCatching {
            householdStore
                .householdRef(householdId)
                .collection("members")
                .get()
                .documents
                .mapNotNull { it.dataOrNull<HomeMemberSource>() }
                .filter { it.active }
        }.getOrDefault(emptyList())
    }
}

private data class HomeDayContext(
    val id: String,
    val label: String,
    val shortLabel: String,
    val dayOffset: Int,
    val dateKey: String,
    val isToday: Boolean,
)

private const val HomePastDayCount = 7
private const val HomeFutureDayCount = 14

private fun buildScheduleWindowDays(
    today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
): List<HomeDayContext> {
    return (-HomePastDayCount..HomeFutureDayCount).map { dayOffset ->
        val date = today.plus(DatePeriod(days = dayOffset))
        HomeDayContext(
            id = date.toString(),
            label = weekdayLabel(date.dayOfWeek),
            shortLabel = weekdayShortLabel(date.dayOfWeek),
            dayOffset = dayOffset,
            dateKey = date.toString(),
            isToday = dayOffset == 0,
        )
    }
}

private fun HomeDayContext.toScheduleDay(): ChoreScheduleDay = ChoreScheduleDay(
    id = id,
    weekdayName = label,
    dayOffset = dayOffset,
    isToday = isToday,
    dateKey = dateKey,
)

private fun buildHeadline(
    viewerName: String,
    householdName: String,
    todayChores: List<ChoreItem>,
): String {
    val viewerFirstName = viewerName.substringBefore(" ").ifBlank { "there" }
    val activeToday = todayChores.count { it.status != ChoreItemStatus.Completed }
    val overdueToday = todayChores.count { it.status == ChoreItemStatus.Overdue }
    val proofToday = todayChores.count { it.status == ChoreItemStatus.PendingProof }
    val completedToday = todayChores.count { it.status == ChoreItemStatus.Completed }

    return when {
        overdueToday > 0 -> "$overdueToday chore${pluralize(overdueToday)} need attention"
        proofToday > 0 && activeToday == proofToday -> "$proofToday proof review${pluralize(proofToday)} waiting"
        activeToday == 0 && completedToday > 0 -> "Nice work, $viewerFirstName"
        activeToday == 0 -> "$householdName is calm today"
        else -> "Hi $viewerFirstName, $activeToday chore${pluralize(activeToday)} left"
    }
}

private fun buildSupportingText(
    weeklyChores: List<ChoreItem>,
    todayChores: List<ChoreItem>,
    memberCount: Int,
): String {
    val pendingProof = todayChores.count { it.status == ChoreItemStatus.PendingProof }
    val weeklyCompleted = weeklyChores.count { it.status == ChoreItemStatus.Completed }
    val weeklyTotal = weeklyChores.size
    return when {
        pendingProof > 0 -> "$pendingProof proof submission${pluralize(pendingProof)} still need a quick review."
        weeklyTotal == 0 -> "Nothing is scheduled in this date window yet."
        memberCount > 1 -> "$memberCount members are keeping the rhythm steady."
        weeklyCompleted > 0 -> "$weeklyCompleted of $weeklyTotal chores are already finished in this window."
        else -> "Your upcoming schedule is lined up and ready."
    }
}

private fun buildDaySupportingText(
    chores: List<ChoreItem>,
    day: HomeDayContext,
): String {
    if (chores.isEmpty()) {
        return if (day.isToday) "No chores today" else "Open day"
    }

    val completed = chores.count { it.status == ChoreItemStatus.Completed }
    val pendingProof = chores.count { it.status == ChoreItemStatus.PendingProof }
    val active = chores.count { it.status != ChoreItemStatus.Completed }

    return when {
        pendingProof > 0 -> "$pendingProof waiting on proof"
        day.isToday && active > 0 -> "$active left today"
        day.isToday -> "All clear"
        day.dayOffset > 0 -> "${chores.size} planned"
        completed == chores.size -> "All complete"
        completed > 0 -> "$completed of ${chores.size} complete"
        else -> "${chores.size} scheduled"
    }
}

private fun buildMetrics(
    todayChores: List<ChoreItem>,
    weeklyChores: List<ChoreItem>,
): List<DemoMetric> {
    val dueToday = todayChores.count {
        it.status == ChoreItemStatus.DueSoon ||
            it.status == ChoreItemStatus.Overdue ||
            it.status == ChoreItemStatus.PendingProof
    }
    val completedToday = todayChores.count { it.status == ChoreItemStatus.Completed }
    val proofQueue = todayChores.count { it.status == ChoreItemStatus.PendingProof }
    val weeklyCompleted = weeklyChores.count { it.status == ChoreItemStatus.Completed }
    val weeklyTotal = weeklyChores.size
    val weeklyPercent = if (weeklyTotal == 0) 0 else (weeklyCompleted * 100) / weeklyTotal

    return listOf(
        DemoMetric(value = "$dueToday", label = "Due today", tone = DemoTone.Warning),
        DemoMetric(value = "$completedToday", label = "Completed", tone = DemoTone.Success),
        DemoMetric(value = "$proofQueue", label = "Proof", tone = DemoTone.Secondary),
        DemoMetric(
            value = "$weeklyCompleted of $weeklyTotal chores finished this week.",
            label = "$weeklyPercent%",
            tone = DemoTone.Primary,
        ),
    )
}

private fun weekdayLabel(dayOfWeek: DayOfWeek): String = when (dayOfWeek) {
    DayOfWeek.SUNDAY -> "Sunday"
    DayOfWeek.MONDAY -> "Monday"
    DayOfWeek.TUESDAY -> "Tuesday"
    DayOfWeek.WEDNESDAY -> "Wednesday"
    DayOfWeek.THURSDAY -> "Thursday"
    DayOfWeek.FRIDAY -> "Friday"
    DayOfWeek.SATURDAY -> "Saturday"
}

private fun weekdayShortLabel(dayOfWeek: DayOfWeek): String = weekdayLabel(dayOfWeek).take(3)

private fun pluralize(count: Int): String = if (count == 1) "" else "s"

@Serializable
private data class HomeMemberSource(
    val userId: String = "",
    val displayName: String = "",
    val role: HouseholdRole = HouseholdRole.Member,
    val active: Boolean = true,
)

private inline fun <reified T> DocumentSnapshot.dataOrNull(): T? = if (exists) data<T>() else null
