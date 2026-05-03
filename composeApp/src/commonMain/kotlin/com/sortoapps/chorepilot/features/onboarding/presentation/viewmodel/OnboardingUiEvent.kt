package com.sortoapps.chorepilot.features.onboarding.presentation.viewmodel

sealed interface OnboardingUiEvent {
    data object BackClicked : OnboardingUiEvent

    data object ContinueClicked : OnboardingUiEvent

    data object TriggerVibration : OnboardingUiEvent

    data object TriggerVibrationSuccess : OnboardingUiEvent

    data class DisplayNameChanged(val value: String) : OnboardingUiEvent

    data class StepChoiceSelected(
        val stepId: String,
        val choiceId: String,
    ) : OnboardingUiEvent
}
