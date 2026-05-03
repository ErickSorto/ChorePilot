package com.sortoapps.chorepilot.features.onboarding.presentation.viewmodel

import com.sortoapps.chorepilot.core.demo.DemoTone
import org.jetbrains.compose.resources.DrawableResource

enum class OnboardingStepKind {
    Hero,
    NameCapture,
    ChoiceList,
    ScaleList,
    Snapshot,
    InsightSummary,
    Timeline,
    Trust,
    Review,
    PremiumBridge,
}

data class OnboardingChoiceUiModel(
    val id: String,
    val title: String,
    val description: String,
    val leadingLabel: String? = null,
)

data class OnboardingTimelineItemUiModel(
    val dayLabel: String,
    val title: String,
    val body: String,
)

data class OnboardingCalloutUiModel(
    val title: String,
    val description: String,
    val tone: DemoTone = DemoTone.Neutral,
)

data class OnboardingStepUiModel(
    val id: String,
    val screenLabel: String,
    val eyebrow: String,
    val title: String,
    val body: String,
    val kind: OnboardingStepKind,
    val phaseLabels: List<String> = emptyList(),
    val activePhaseIndex: Int = 0,
    val image: DrawableResource? = null,
    val chips: List<Pair<String, DemoTone>> = emptyList(),
    val callouts: List<OnboardingCalloutUiModel> = emptyList(),
    val choices: List<OnboardingChoiceUiModel> = emptyList(),
    val timeline: List<OnboardingTimelineItemUiModel> = emptyList(),
    val primaryActionLabel: String,
    val secondaryActionLabel: String? = null,
    val requiresChoice: Boolean = false,
    val requiresName: Boolean = false,
)

data class OnboardingUiState(
    val displayName: String = "Erick",
    val currentStepIndex: Int = 0,
    val selectedChoices: Map<String, String> = emptyMap(),
    val steps: List<OnboardingStepUiModel> = emptyList(),
) {
    val currentStep: OnboardingStepUiModel
        get() = steps[currentStepIndex]

    val canGoBack: Boolean
        get() = currentStepIndex > 0

    val isLastStep: Boolean
        get() = currentStepIndex == steps.lastIndex

    val progressLabel: String
        get() = "${currentStepIndex + 1} / ${steps.size}"

    val progressFraction: Float
        get() = (currentStepIndex + 1).toFloat() / steps.size.coerceAtLeast(1)

    val canContinue: Boolean
        get() = when {
            currentStep.requiresName -> displayName.isNotBlank()
            currentStep.requiresChoice -> {
                val selectedChoice = selectedChoices[currentStep.id]
                selectedChoice != null && currentStep.choices.any { it.id == selectedChoice }
            }
            else -> true
        }
}
