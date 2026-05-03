package com.sortoapps.chorepilot.features.onboarding.presentation.viewmodel

import androidx.lifecycle.ViewModel
import chorepilot.composeapp.generated.resources.Res
import chorepilot.composeapp.generated.resources.onboarding_dream_outcome
import chorepilot.composeapp.generated.resources.onboarding_name_squirrel
import chorepilot.composeapp.generated.resources.onboarding_premium_bridge
import chorepilot.composeapp.generated.resources.onboarding_roadmap_support
import chorepilot.composeapp.generated.resources.onboarding_snapshot_cluster
import chorepilot.composeapp.generated.resources.onboarding_trust_support
import com.sortoapps.chorepilot.core.demo.DemoTone
import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import com.sortoapps.chorepilot.core.platform.VibratorUtil
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val sessionRepository: SessionRepository,
    private val vibratorUtil: VibratorUtil,
) : ViewModel() {
    private val showPremiumBridge = sessionRepository.session.value.householdId.isNotBlank()
    private val _uiState = MutableStateFlow(buildUiState(showPremiumBridge = showPremiumBridge))
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onEvent(event: OnboardingUiEvent) {
        when (event) {
            OnboardingUiEvent.TriggerVibration -> vibratorUtil.triggerVibration()
            OnboardingUiEvent.TriggerVibrationSuccess -> vibratorUtil.triggerVibrationSuccess()
            OnboardingUiEvent.BackClicked -> {
                _uiState.update { state ->
                    state.copy(currentStepIndex = (state.currentStepIndex - 1).coerceAtLeast(0))
                }
            }

            OnboardingUiEvent.ContinueClicked -> {
                val state = _uiState.value
                if (!state.canContinue) return

                if (state.isLastStep) {
                    viewModelScope.launch {
                        sessionRepository.completeOnboarding(state.displayName)
                        vibratorUtil.triggerVibrationSuccess()
                    }
                } else {
                    _uiState.update { current ->
                        current.copy(currentStepIndex = (current.currentStepIndex + 1).coerceAtMost(current.steps.lastIndex))
                    }
                }
            }

            is OnboardingUiEvent.DisplayNameChanged -> {
                _uiState.update { state ->
                    buildUiState(
                        displayName = event.value,
                        currentStepIndex = state.currentStepIndex,
                        selectedChoices = state.selectedChoices,
                        showPremiumBridge = showPremiumBridge,
                    )
                }
            }

            is OnboardingUiEvent.StepChoiceSelected -> {
                _uiState.update { state ->
                    buildUiState(
                        displayName = state.displayName,
                        currentStepIndex = state.currentStepIndex,
                        selectedChoices = state.selectedChoices + (event.stepId to event.choiceId),
                        showPremiumBridge = showPremiumBridge,
                    )
                }
            }
        }
    }

    private fun buildUiState(
        displayName: String = "Erick",
        currentStepIndex: Int = 0,
        selectedChoices: Map<String, String> = defaultPreviewSelections(),
        showPremiumBridge: Boolean = true,
    ): OnboardingUiState = previewState(
        displayName = displayName,
        currentStepIndex = currentStepIndex,
        selectedChoices = selectedChoices,
        showPremiumBridge = showPremiumBridge,
    )

    companion object {
        fun previewState(
            displayName: String = "Erick",
            currentStepIndex: Int = 0,
            selectedChoices: Map<String, String> = defaultPreviewSelections(),
            showPremiumBridge: Boolean = true,
        ): OnboardingUiState {
            val safeName = displayName.ifBlank { "Erick" }
            val normalizedChoices = normalizeSelections(selectedChoices)
            val steps = buildSteps(
                displayName = safeName,
                selectedChoices = normalizedChoices,
                showPremiumBridge = showPremiumBridge,
            )
            return OnboardingUiState(
                displayName = displayName,
                currentStepIndex = currentStepIndex.coerceIn(0, steps.lastIndex),
                selectedChoices = normalizedChoices,
                steps = steps,
            )
        }
        
        private fun buildSteps(
            displayName: String,
            selectedChoices: Map<String, String>,
            showPremiumBridge: Boolean,
        ): List<OnboardingStepUiModel> {
            val snapshotStats = buildSnapshotStats(selectedChoices)
            val householdInsight = buildHouseholdInsight(selectedChoices)
            val householdSpace = householdSpaceLabel(selectedChoices)
            val householdType = selectedChoices["household_type"]
            val nearOutcomeChoices = buildNearOutcomeChoices(householdType)
            val identityChoices = buildIdentityChoices(householdType)
            val roadmapItems = buildRoadmap(selectedChoices)
            val roadmapTitle = when (selectedChoices["near_outcome"]) {
                "fair" -> "A fairer week can show up quickly."
                "reminders" -> "Fewer reminders can show up quickly."
                "calm" -> "A calmer week can show up quickly."
                "independence" -> "More independence can show up quickly."
                "less_tension" -> "Less roommate tension can show up quickly."
                "shared_spaces" -> "Cleaner shared spaces can show up quickly."
                "smooth_close" -> "A smoother close-down can show up quickly."
                else -> "A meaningful win can show up quickly."
            }
            val roadmapBody = when (selectedChoices["feature_relief"]) {
                "auto_assignment" -> "Clear ownership can settle the next 7 days fast."
                "proof" -> "Visible follow-through can change the feel of the week."
                "smart_reminders" -> "Earlier nudges can smooth the week fast."
                "weekly_reports" -> "A clearer weekly view can rebalance things fast."
                else -> "A simple first week can build momentum fast."
            }

            return buildList {
                add(
                    OnboardingStepUiModel(
                        id = "dream",
                        screenLabel = "Screen 01",
                        eyebrow = "A calmer home starts here",
                        title = "Shared chores. Calmer evenings.",
                        body = "Clear owners, visible follow-through, and fewer reminders from day one.",
                        kind = OnboardingStepKind.Hero,
                        phaseLabels = listOf("Destination", "Personalize", "Reflect"),
                        activePhaseIndex = 0,
                        image = Res.drawable.onboarding_dream_outcome,
                        callouts = listOf(
                            OnboardingCalloutUiModel(
                                title = "Fairer handoffs",
                                description = "Dishes and resets stop drifting to one person.",
                                tone = DemoTone.Primary,
                            ),
                            OnboardingCalloutUiModel(
                                title = "Calmer evenings",
                                description = "Dinner cleanup and tomorrow prep feel coordinated.",
                                tone = DemoTone.Secondary,
                            ),
                        ),
                        primaryActionLabel = "Start my setup",
                        secondaryActionLabel = "Already have a group?",
                    ),
                )
                add(
                    OnboardingStepUiModel(
                        id = "household_type",
                        screenLabel = "Screen 02",
                        eyebrow = "First, set the scene",
                        title = "What kind of household are we setting up?",
                        body = "Choose the space so the setup fits from day one.",
                        kind = OnboardingStepKind.ChoiceList,
                        phaseLabels = listOf("Destination", "Household type", "Personalize"),
                        activePhaseIndex = 1,
                        choices = listOf(
                            OnboardingChoiceUiModel("family_home", "Family household", "Parents, kids, and shared routines at home.", "1"),
                            OnboardingChoiceUiModel("shared_home", "Shared apartment", "Roommates or partners sharing the weekly load.", "2"),
                            OnboardingChoiceUiModel("dorm_suite", "College dorm", "Shared spaces that need quick, visible ownership.", "3"),
                            OnboardingChoiceUiModel("office_team", "Office team", "A small team keeping common spaces running smoothly.", "4"),
                        ),
                        primaryActionLabel = "Use this setup style",
                        requiresChoice = true,
                    ),
                )
                add(
                    OnboardingStepUiModel(
                        id = "name",
                        screenLabel = "Screen 03",
                        eyebrow = "",
                        title = "What should we call you?",
                        body = "",
                        kind = OnboardingStepKind.NameCapture,
                        phaseLabels = listOf("Destination", "Personalize", "Reflect"),
                        activePhaseIndex = 1,
                        image = Res.drawable.onboarding_name_squirrel,
                        callouts = listOf(
                            OnboardingCalloutUiModel(
                                title = "Tailored copy",
                                description = "Your goals and pace shape the rest of the setup.",
                                tone = DemoTone.Secondary,
                            ),
                            OnboardingCalloutUiModel(
                                title = "Smarter defaults",
                                description = "We’ll tune the first plan around how your home actually works.",
                                tone = DemoTone.Primary,
                            ),
                        ),
                        primaryActionLabel = "Continue as $displayName",
                        requiresName = true,
                    ),
                )
                add(
                    OnboardingStepUiModel(
                        id = "near_outcome",
                        screenLabel = "Screen 04",
                        eyebrow = "$displayName, what would help first?",
                        title = "What would make your $householdSpace feel better this week?",
                        body = "Pick the first win you want.",
                        kind = OnboardingStepKind.ChoiceList,
                        phaseLabels = listOf("Destination", "Immediate goal", "Identity"),
                        activePhaseIndex = 1,
                        choices = nearOutcomeChoices,
                        primaryActionLabel = "Keep building the plan",
                        requiresChoice = true,
                    ),
                )
                add(
                    OnboardingStepUiModel(
                        id = "identity",
                        screenLabel = "Screen 05",
                        eyebrow = "Longer-term feeling",
                        title = "What kind of $householdSpace do you want this to become?",
                        body = "Choose the feeling you want this system to create.",
                        kind = OnboardingStepKind.ChoiceList,
                        phaseLabels = listOf("Immediate goal", "Identity", "Reflect"),
                        activePhaseIndex = 1,
                        choices = identityChoices,
                        primaryActionLabel = "That’s the direction",
                        requiresChoice = true,
                    ),
                )
                add(
                    OnboardingStepUiModel(
                        id = "mental_load",
                        screenLabel = "Screen 06",
                        eyebrow = "Intentional pause",
                        title = "How heavy does the household mental load feel right now?",
                        body = "This sets the tone of the first plan.",
                        kind = OnboardingStepKind.ScaleList,
                        phaseLabels = listOf("Reflect", "Mental load", "Breakdowns"),
                        activePhaseIndex = 1,
                        choices = listOf(
                            OnboardingChoiceUiModel("light", "Light friction", "Mostly under control"),
                            OnboardingChoiceUiModel("busy", "Busy", "Needs more structure"),
                            OnboardingChoiceUiModel("heavy", "Heavy", "One person is holding too much"),
                            OnboardingChoiceUiModel("chaotic", "Chaotic", "Things get forgotten constantly"),
                            OnboardingChoiceUiModel("burnout", "Near burnout", "It feels hard to keep up"),
                        ),
                        primaryActionLabel = "Continue with this pace",
                        requiresChoice = true,
                    ),
                )
                add(
                    OnboardingStepUiModel(
                        id = "breakdown",
                        screenLabel = "Screen 07",
                        eyebrow = "Household friction",
                        title = "Which breakdown stings the most right now?",
                        body = "Choose the biggest friction point.",
                        kind = OnboardingStepKind.ChoiceList,
                        phaseLabels = listOf("Mental load", "Breakdowns", "Most helpful feature"),
                        activePhaseIndex = 1,
                        choices = listOf(
                            OnboardingChoiceUiModel("forgotten", "Chores get forgotten", "Important tasks disappear until someone reminds everybody.", "!"),
                            OnboardingChoiceUiModel("uneven", "Work feels uneven", "One person absorbs too much planning, checking, and follow-up.", "="),
                            OnboardingChoiceUiModel("ownership", "No one knows whose task it is", "Ownership stays fuzzy until the last minute.", "?"),
                            OnboardingChoiceUiModel("accountability", "Accountability is weak", "There is no clean way to see follow-through or proof.", "✓"),
                        ),
                        primaryActionLabel = "Use this as our anchor",
                        requiresChoice = true,
                    ),
                )
                add(
                    OnboardingStepUiModel(
                        id = "feature_relief",
                        screenLabel = "Screen 08",
                        eyebrow = "What feels like relief?",
                        title = "Which feature would make the biggest difference first?",
                        body = "Pick the help you want first.",
                        kind = OnboardingStepKind.ChoiceList,
                        phaseLabels = listOf("Breakdowns", "Most helpful feature", "Attribution"),
                        activePhaseIndex = 1,
                        choices = listOf(
                            OnboardingChoiceUiModel("auto_assignment", "Auto assignment", "Build clearer ownership before the week turns messy.", "A"),
                            OnboardingChoiceUiModel("proof", "Proof required", "Know when something is truly done without nagging.", "B"),
                            OnboardingChoiceUiModel("smart_reminders", "Smart reminders", "Catch tasks early without turning the app into noise.", "C"),
                            OnboardingChoiceUiModel("weekly_reports", "Weekly reports", "See what is working and what still feels uneven.", "D"),
                        ),
                        primaryActionLabel = "Build around that first",
                        requiresChoice = true,
                    ),
                )
                add(
                    OnboardingStepUiModel(
                        id = "attribution",
                        screenLabel = "Screen 09",
                        eyebrow = "Quick one",
                        title = "Where did you first hear about ChorePilot?",
                        body = "One quick answer, then we show your first-week plan.",
                        kind = OnboardingStepKind.ChoiceList,
                        phaseLabels = listOf("Most helpful feature", "Where you heard about us", "Snapshot"),
                        activePhaseIndex = 1,
                        choices = listOf(
                            OnboardingChoiceUiModel("tiktok", "TikTok", "Short, practical family routine content.", "T"),
                            OnboardingChoiceUiModel("youtube", "YouTube", "Longer-form family systems or productivity content.", "Y"),
                            OnboardingChoiceUiModel("friend", "Friend or family", "Someone close mentioned it as a helpful routine tool.", "F"),
                            OnboardingChoiceUiModel("app_store", "App Store search", "You were looking for something more structured and calm.", "A"),
                        ),
                        primaryActionLabel = "Keep going",
                        requiresChoice = true,
                    ),
                )
                add(
                    OnboardingStepUiModel(
                        id = "snapshot",
                        screenLabel = "Screen 10",
                        eyebrow = "Your first-week plan",
                        title = householdInsight.snapshotHeadline,
                        body = householdInsight.snapshotBody,
                        kind = OnboardingStepKind.Snapshot,
                        phaseLabels = listOf("Plan", "Insights", "Starting point"),
                        activePhaseIndex = 1,
                        image = Res.drawable.onboarding_snapshot_cluster,
                        callouts = snapshotStats,
                        primaryActionLabel = "That feels accurate",
                    ),
                )
                add(
                    OnboardingStepUiModel(
                        id = "starting_point",
                        screenLabel = "Screen 11",
                        eyebrow = "Your starting point",
                        title = householdInsight.startingPointHeadline,
                        body = householdInsight.startingPointBody,
                        kind = OnboardingStepKind.InsightSummary,
                        phaseLabels = listOf("Insights", "Starting point", "Roadmap"),
                        activePhaseIndex = 1,
                        callouts = listOf(
                            OnboardingCalloutUiModel(
                                title = householdInsight.nowLabel,
                                description = householdInsight.meaningNow,
                                tone = DemoTone.Secondary,
                            ),
                            OnboardingCalloutUiModel(
                                title = householdInsight.startLabel,
                                description = householdInsight.startHere,
                                tone = DemoTone.Primary,
                            ),
                            OnboardingCalloutUiModel(
                                title = householdInsight.protectLabel,
                                description = householdInsight.protectNext,
                                tone = DemoTone.Tertiary,
                            ),
                        ),
                        primaryActionLabel = "Show the first 7 days",
                    ),
                )
                add(
                    OnboardingStepUiModel(
                        id = "roadmap",
                        screenLabel = "Screen 12",
                        eyebrow = "First 7 days",
                        title = roadmapTitle,
                        body = roadmapBody,
                        kind = OnboardingStepKind.Timeline,
                        phaseLabels = listOf("Starting point", "Day 1 to Day 7", "Trust"),
                        activePhaseIndex = 1,
                        image = Res.drawable.onboarding_roadmap_support,
                        timeline = roadmapItems,
                        primaryActionLabel = "That’s the kind of win we need",
                    ),
                )
                add(
                    OnboardingStepUiModel(
                        id = "trust",
                        screenLabel = "Screen 13",
                        eyebrow = "Why the next step feels safe",
                        title = "Built for real households, not productivity theater.",
                        body = "Private, flexible, and built for your $householdSpace.",
                        kind = OnboardingStepKind.Trust,
                        phaseLabels = listOf("Roadmap", "Trust", "Review"),
                        activePhaseIndex = 1,
                        image = Res.drawable.onboarding_trust_support,
                        callouts = listOf(
                            OnboardingCalloutUiModel(
                                title = "Private household space",
                                description = "No public feed, no social pressure, and no noisy growth hacks.",
                                tone = DemoTone.Primary,
                            ),
                            OnboardingCalloutUiModel(
                                title = "Parent-led control",
                                description = "Approvals, proof review, and availability rules stay where they belong.",
                                tone = DemoTone.Secondary,
                            ),
                            OnboardingCalloutUiModel(
                                title = "Flexible enough for real life",
                                description = "Vacation mode, day-off logic, and better reminder pacing keep the plan humane.",
                                tone = DemoTone.Tertiary,
                            ),
                        ),
                        chips = listOf(
                            "Family-safe" to DemoTone.Primary,
                            "Calm reminders" to DemoTone.Secondary,
                            "Clear ownership" to DemoTone.Tertiary,
                        ),
                        primaryActionLabel = "I trust this direction",
                    ),
                )
                add(
                    OnboardingStepUiModel(
                        id = "review",
                        screenLabel = "Optional",
                        eyebrow = "Optional",
                        title = "How does this first pass feel so far?",
                        body = "If ChorePilot already feels thoughtful, you can leave a quick rating now or keep moving.",
                        kind = OnboardingStepKind.Review,
                        phaseLabels = listOf("Trust", "Review", if (showPremiumBridge) "Premium" else "Setup"),
                        activePhaseIndex = 1,
                        primaryActionLabel = "Rate the experience",
                        secondaryActionLabel = "Maybe later",
                    ),
                )
                if (showPremiumBridge) {
                    add(
                        OnboardingStepUiModel(
                            id = "premium_bridge",
                            screenLabel = "Screen 14",
                            eyebrow = "",
                            title = "$displayName, ready to see the weekly rhythm before the chaos hits?",
                            body = "Plan the week before it piles up.",
                            kind = OnboardingStepKind.PremiumBridge,
                            phaseLabels = listOf("Trust", "Premium bridge", "Offer"),
                            activePhaseIndex = 1,
                            image = Res.drawable.onboarding_premium_bridge,
                            callouts = listOf(
                                OnboardingCalloutUiModel(
                                    title = "See the week clearly",
                                    description = "Spot overloaded days before the week starts.",
                                    tone = DemoTone.Primary,
                                ),
                                OnboardingCalloutUiModel(
                                    title = "Keep work balanced",
                                    description = "Make recurring chores feel visibly fair.",
                                    tone = DemoTone.Secondary,
                                ),
                                OnboardingCalloutUiModel(
                                    title = "Keep nudges calm",
                                    description = "Catch misses earlier without turning up the noise.",
                                    tone = DemoTone.Tertiary,
                                ),
                            ),
                            primaryActionLabel = "See the premium flow",
                            secondaryActionLabel = "Not yet",
                        ),
                    )
                }
            }
        }

    private fun defaultPreviewSelections(): Map<String, String> = mapOf(
        "household_type" to "family_home",
        "near_outcome" to "fair",
        "identity" to "teamwork",
        "mental_load" to "busy",
        "breakdown" to "ownership",
        "feature_relief" to "auto_assignment",
        "attribution" to "friend",
    )

    private fun householdSpaceLabel(selectedChoices: Map<String, String>): String = when (selectedChoices["household_type"]) {
        "family_home" -> "home"
        "shared_home" -> "apartment"
        "dorm_suite" -> "dorm"
        "office_team" -> "office"
        else -> "home"
    }

    private fun buildNearOutcomeChoices(householdType: String?): List<OnboardingChoiceUiModel> = when (householdType) {
        "shared_home" -> listOf(
            OnboardingChoiceUiModel("fair", "Fairer chore sharing", "Make the weekly load feel visible and less one-sided.", "1"),
            OnboardingChoiceUiModel("reminders", "Fewer reminders", "Reduce how often one person has to chase everyone else.", "2"),
            OnboardingChoiceUiModel("calm", "Calmer weeknights", "Make resets and shared-space handoffs feel smoother.", "3"),
            OnboardingChoiceUiModel("less_tension", "Less roommate tension", "Lower the friction that builds around shared responsibilities.", "4"),
        )

        "dorm_suite" -> listOf(
            OnboardingChoiceUiModel("fair", "Fairer shared work", "Make common-space tasks feel visible and evenly shared.", "1"),
            OnboardingChoiceUiModel("reminders", "Fewer reminders", "Reduce how often someone has to nudge the whole suite.", "2"),
            OnboardingChoiceUiModel("calm", "Calmer shared resets", "Make kitchen and bathroom cleanup feel less reactive.", "3"),
            OnboardingChoiceUiModel("shared_spaces", "Cleaner shared spaces", "Keep the room, kitchen, or bathroom from slipping so fast.", "4"),
        )

        "office_team" -> listOf(
            OnboardingChoiceUiModel("fair", "Fairer upkeep sharing", "Make common-space tasks feel clearly distributed across the team.", "1"),
            OnboardingChoiceUiModel("reminders", "Fewer reminders", "Reduce the repeated follow-up around closing and reset tasks.", "2"),
            OnboardingChoiceUiModel("calm", "Calmer team flow", "Make shared-space upkeep feel less interruptive during the day.", "3"),
            OnboardingChoiceUiModel("smooth_close", "Smoother close-down", "Keep end-of-day resets from turning into last-minute scramble.", "4"),
        )

        else -> listOf(
            OnboardingChoiceUiModel("fair", "Fairer chore sharing", "Make the workload feel visible and less one-sided.", "1"),
            OnboardingChoiceUiModel("reminders", "Fewer reminders", "Reduce how often one person carries the mental load.", "2"),
            OnboardingChoiceUiModel("calm", "Calmer evenings", "Create smoother dinner, homework, and reset transitions.", "3"),
            OnboardingChoiceUiModel("independence", "More kid independence", "Make tasks visible enough that children can step in sooner.", "4"),
        )
    }

    private fun buildIdentityChoices(householdType: String?): List<OnboardingChoiceUiModel> = when (householdType) {
        "shared_home" -> listOf(
            OnboardingChoiceUiModel("less_load", "Less coordination load", "One person should not have to silently steer the apartment.", "A"),
            OnboardingChoiceUiModel("teamwork", "More roommate teamwork", "Create shared ownership instead of chore negotiations.", "B"),
            OnboardingChoiceUiModel("arguments", "Fewer awkward reminders", "Make expectations clear before tension builds.", "C"),
            OnboardingChoiceUiModel("steady", "A steadier apartment rhythm", "Turn upkeep into a calmer weekly pattern.", "D"),
        )

        "dorm_suite" -> listOf(
            OnboardingChoiceUiModel("less_load", "Less coordinator load", "One person should not have to keep the suite on track alone.", "A"),
            OnboardingChoiceUiModel("teamwork", "More suite teamwork", "Make shared spaces feel like a group effort.", "B"),
            OnboardingChoiceUiModel("arguments", "Fewer roommate conflicts", "Make shared expectations clear before they cause friction.", "C"),
            OnboardingChoiceUiModel("steady", "A steadier dorm rhythm", "Keep shared spaces reset without constant reminders.", "D"),
        )

        "office_team" -> listOf(
            OnboardingChoiceUiModel("less_load", "Less manager follow-up", "One person should not have to chase every reset or close-down.", "A"),
            OnboardingChoiceUiModel("teamwork", "More team ownership", "Make common-space upkeep feel shared, not delegated.", "B"),
            OnboardingChoiceUiModel("arguments", "Fewer repeated nudges", "Make expectations clear before they become annoying.", "C"),
            OnboardingChoiceUiModel("steady", "A steadier team rhythm", "Turn shared upkeep into a reliable routine.", "D"),
        )

        else -> listOf(
            OnboardingChoiceUiModel("less_load", "Less parent mental load", "One person should not have to silently steer everything.", "A"),
            OnboardingChoiceUiModel("teamwork", "More family teamwork", "Create shared ownership instead of chore negotiations.", "B"),
            OnboardingChoiceUiModel("arguments", "Fewer arguments", "Make expectations clear before tension builds.", "C"),
            OnboardingChoiceUiModel("steady", "A steadier home rhythm", "Turn household care into a calmer weekly pattern.", "D"),
        )
    }

    private fun normalizeSelections(selectedChoices: Map<String, String>): Map<String, String> {
        val householdType = selectedChoices["household_type"]
        val validNearOutcomeIds = buildNearOutcomeChoices(householdType).map { it.id }.toSet()
        val validIdentityIds = buildIdentityChoices(householdType).map { it.id }.toSet()
        return selectedChoices.filter { (key, value) ->
            when (key) {
                "near_outcome" -> value in validNearOutcomeIds
                "identity" -> value in validIdentityIds
                else -> true
            }
        }
    }

    private fun buildSnapshotStats(selectedChoices: Map<String, String>): List<OnboardingCalloutUiModel> {
        val starterCount = when (selectedChoices["mental_load"]) {
            "burnout", "chaotic" -> "2"
            "heavy", "busy" -> "3"
            else -> "4"
        }
        val starterExamples = when (selectedChoices["household_type"]) {
            "dorm_suite" -> "shared kitchen checks, trash, and bathroom turns"
            "office_team" -> "close-down, dishes, and shared-supply checks"
            "shared_home" -> "kitchen tidy, trash, and laundry flow"
            else -> "dishes, evening tidy, and laundry flow"
        }
        val evidenceInsight = when (selectedChoices["household_type"]) {
            "office_team" -> OnboardingCalloutUiModel(
                title = "Clearer roles, less pressure",
                description = "Clearer role expectations at work are linked with less pressure and less psychological discomfort.",
                tone = DemoTone.Secondary,
            )

            "dorm_suite" -> OnboardingCalloutUiModel(
                title = "Steadier routines, lighter mood drag",
                description = "One college-student study found steadier daily routines were linked with lower depression.",
                tone = DemoTone.Secondary,
            )

            "shared_home" -> OnboardingCalloutUiModel(
                title = "Less chaos, less stress",
                description = "Home-environment research links more clutter and chaos with higher stress, which is why shared routines help fast.",
                tone = DemoTone.Secondary,
            )

            else -> OnboardingCalloutUiModel(
                title = "4.3 points lower parent anxiety",
                description = "One longitudinal family-routine study found parent anxiety scores 4.3 points lower when routines were kept more consistently.",
                tone = DemoTone.Secondary,
            )
        }
        return listOf(
            evidenceInsight,
            OnboardingCalloutUiModel(
                title = "$starterCount shared routines",
                description = when (selectedChoices["breakdown"]) {
                    "forgotten" -> "$starterCount shared routines around $starterExamples so repeat misses stop first."
                    "uneven" -> "$starterCount shared routines around $starterExamples so hidden work gets named sooner."
                    "ownership" -> "$starterCount shared routines around $starterExamples so every task has a clear owner."
                    "accountability" -> "$starterCount shared routines around $starterExamples so follow-through becomes visible fast."
                    else -> "$starterCount shared routines around $starterExamples so the next handoff feels obvious."
                },
                tone = DemoTone.Primary,
            ),
            OnboardingCalloutUiModel(
                title = when (selectedChoices["feature_relief"]) {
                    "auto_assignment" -> "Set visible owners from day one"
                    "proof" -> "Make follow-through easy to see"
                    "smart_reminders" -> "Nudge before things slip"
                    "weekly_reports" -> "Catch drift before it piles up"
                    else -> "Set visible owners from day one"
                },
                description = when (selectedChoices["feature_relief"]) {
                    "auto_assignment" -> "Give those weekly jobs visible owners from day one, so no one has to guess who is up next."
                    "proof" -> "Make follow-through visible without awkward chasing when something matters."
                    "smart_reminders" -> "Nudge the right person before the rush, not after something slips."
                    "weekly_reports" -> "Show where the week is drifting before it turns into one person’s burden."
                    else -> "Give those weekly jobs visible owners from day one."
                },
                tone = DemoTone.Tertiary,
            ),
        )
    }

    private fun buildRoadmap(selectedChoices: Map<String, String>): List<OnboardingTimelineItemUiModel> {
        val householdSpace = householdSpaceLabel(selectedChoices)
        val firstMove = when (selectedChoices["feature_relief"]) {
            "auto_assignment" -> "Place the evening tidy with a clear owner."
            "proof" -> "Turn one key chore into a visible done check."
            "smart_reminders" -> "Set one early reminder before the rush starts."
            "weekly_reports" -> "Surface the first imbalance before it grows."
            else -> "Give the next routine a visible owner."
        }
        val momentumMove = when (selectedChoices["breakdown"]) {
            "forgotten" -> "The chores that usually slip now stay visible in $householdSpace."
            "uneven" -> "The invisible work starts looking more evenly shared."
            "ownership" -> "Everyone can see who owns the next routine."
            "accountability" -> "Follow-through becomes visible without chasing."
            else -> "The next handoff stops drifting at the last minute."
        }
        val weekWin = when (selectedChoices["near_outcome"]) {
            "fair" -> "The week feels fairer before frustration builds."
            "reminders" -> "Fewer reminders are needed to keep things moving."
            "calm" -> "Evenings feel less reactive and more settled."
            "independence" -> "Kids can step in sooner with clearer expectations."
            "less_tension" -> "The apartment feels less tense around shared chores."
            "shared_spaces" -> "Shared spaces stay cleaner without constant nudging."
            "smooth_close" -> "Close-down feels smoother before the team leaves."
            else -> "The household rhythm feels calmer by the weekend."
        }

        return listOf(
            OnboardingTimelineItemUiModel("Day 1", "First clear owner", firstMove),
            OnboardingTimelineItemUiModel("Day 3", "Less friction", momentumMove),
            OnboardingTimelineItemUiModel("Day 7", "A calmer week", weekWin),
        )
    }

    private fun buildHouseholdInsight(selectedChoices: Map<String, String>): HouseholdInsight {
        val goalPhrase = when (selectedChoices["near_outcome"]) {
            "fair" -> "fairer handoffs"
            "reminders" -> "fewer reminders"
            "calm" -> "calmer evenings"
            "independence" -> "more kid independence"
            "less_tension" -> "less roommate tension"
            "shared_spaces" -> "cleaner shared spaces"
            "smooth_close" -> "smoother close-downs"
            else -> "clearer ownership"
        }
        val featurePhrase = when (selectedChoices["feature_relief"]) {
            "auto_assignment" -> "auto assignment"
            "proof" -> "light proof review"
            "smart_reminders" -> "smarter reminders"
            "weekly_reports" -> "weekly reports"
            else -> "auto assignment"
        }
        val shortFeaturePhrase = when (selectedChoices["feature_relief"]) {
            "auto_assignment" -> "clear owners"
            "proof" -> "simple proof"
            "smart_reminders" -> "earlier reminders"
            "weekly_reports" -> "weekly rebalance"
            else -> "clear owners"
        }
        val breakdownPhrase = when (selectedChoices["breakdown"]) {
            "forgotten" -> "tasks slipping until someone reminds everyone"
            "uneven" -> "one person carrying too much invisible work"
            "ownership" -> "ownership staying fuzzy too late"
            "accountability" -> "follow-through being hard to see"
            else -> "handoffs drifting until the last minute"
        }
        val householdSpace = householdSpaceLabel(selectedChoices)

        return HouseholdInsight(
            snapshotHeadline = "Here’s how we’d calm the week first.",
            snapshotBody = when (selectedChoices["feature_relief"]) {
                "auto_assignment" -> "Built around $goalPhrase and clearer ownership in your $householdSpace."
                "proof" -> "Built around $goalPhrase and clearer follow-through in your $householdSpace."
                "smart_reminders" -> "Built around $goalPhrase and earlier nudges in your $householdSpace."
                "weekly_reports" -> "Built around $goalPhrase and a lighter weekly load in your $householdSpace."
                else -> "Built around $goalPhrase and a lighter first week in your $householdSpace."
            },
            startingPointHeadline = "Start with $shortFeaturePhrase.",
            startingPointBody = when (selectedChoices["mental_load"]) {
                "burnout" -> "Keep the first setup tiny and reassuring."
                "chaotic" -> "Start with the handoff that causes the most stress."
                "heavy" -> "Make the next reset clear before adding more."
                "busy" -> "A light weekly structure can smooth things quickly."
                else -> "A small weekly rhythm can change the feel of home."
            },
            nowLabel = when (selectedChoices["breakdown"]) {
                "forgotten" -> "What slips now"
                "uneven" -> "What feels uneven"
                "ownership" -> "What feels fuzzy"
                "accountability" -> "What feels unseen"
                else -> "What drags now"
            },
            meaningNow = "Right now the biggest drag is $breakdownPhrase.",
            startLabel = when (selectedChoices["feature_relief"]) {
                "auto_assignment" -> "First move"
                "proof" -> "First check"
                "smart_reminders" -> "First nudge"
                "weekly_reports" -> "First report"
                else -> "First move"
            },
            startHere = "Lead with $featurePhrase.",
            protectLabel = when (selectedChoices["household_type"]) {
                "dorm_suite" -> "Protect shared spaces"
                "office_team" -> "Protect close-down"
                "shared_home" -> "Protect weeknights"
                else -> "Protect evenings"
            },
            protectNext = when (selectedChoices["household_type"]) {
                "dorm_suite" -> "Shared kitchen and bathroom resets."
                "office_team" -> "Closing tasks and common-space ownership."
                "shared_home" -> "Weeknight resets and shared space handoffs."
                else -> "Evenings and school-prep handoffs."
            },
        )
    }

    }

    private data class HouseholdInsight(
        val snapshotHeadline: String,
        val snapshotBody: String,
        val startingPointHeadline: String,
        val startingPointBody: String,
        val nowLabel: String,
        val meaningNow: String,
        val startLabel: String,
        val startHere: String,
        val protectLabel: String,
        val protectNext: String,
    )
}
