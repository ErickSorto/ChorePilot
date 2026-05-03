package com.sortoapps.chorepilot.features.onboarding.presentation.viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnboardingViewModelTest {
    @Test
    fun previewStateOmitsPremiumBridgeBeforeHouseholdEntry() {
        val state = OnboardingViewModel.previewState(showPremiumBridge = false)

        assertFalse(state.steps.any { it.id == "premium_bridge" })
        assertEquals("Setup", state.steps.last().phaseLabels.last())
    }

    @Test
    fun previewStateIncludesPremiumBridgeOnceHouseholdExists() {
        val state = OnboardingViewModel.previewState(showPremiumBridge = true)

        assertTrue(state.steps.any { it.id == "premium_bridge" })
        assertEquals("premium_bridge", state.steps.last().id)
    }
}
