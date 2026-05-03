package com.sortoapps.chorepilot.features.settings.presentation.viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsHubUiStateTest {
    @Test
    fun defaultStateGroupsDestinationsIntoClearSections() {
        val state = SettingsHubUiState()

        assertEquals(
            listOf("Account & household", "Billing & rewards"),
            state.sections.map { it.title },
        )
        assertEquals(
            listOf(
                SettingsHubDestination.Account,
                SettingsHubDestination.Appearance,
                SettingsHubDestination.Notifications,
            ),
            state.sections.first().items.map { it.destination },
        )
        assertEquals(
            listOf(
                SettingsHubDestination.Billing,
                SettingsHubDestination.Rewards,
            ),
            state.sections.last().items.map { it.destination },
        )
        assertEquals("Sign out", state.signOutAction.title)
    }
}
