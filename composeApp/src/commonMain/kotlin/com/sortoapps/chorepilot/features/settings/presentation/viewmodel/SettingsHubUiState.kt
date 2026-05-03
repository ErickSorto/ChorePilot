package com.sortoapps.chorepilot.features.settings.presentation.viewmodel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.ui.graphics.vector.ImageVector
import com.sortoapps.chorepilot.core.demo.DemoPreviewCard

data class SettingsHubUiState(
    val displayName: String = "Erick",
    val householdName: String = "River House",
    val activeHouseholdId: String = "",
    val availableHouseholds: List<SettingsHubHouseholdUiModel> = emptyList(),
    val isSwitchingHousehold: Boolean = false,
    val profileEyebrow: String = "Settings hub",
    val profileSummary: String = "Keep account, reminders, billing, rewards, and sign-in details aligned from one place.",
    val sections: List<SettingsHubSectionUiModel> = defaultSettingsHubSections(),
    val signOutAction: SettingsHubActionUiModel = defaultSignOutAction(),
    val showDebugTools: Boolean = false,
    val devSection: SettingsHubSectionUiModel? = defaultDevSection(),
    val devActions: List<SettingsHubDevActionUiModel> = defaultDevActions(),
    val previewCards: List<DemoPreviewCard> = emptyList(),
)

data class SettingsHubHouseholdUiModel(
    val id: String,
    val name: String,
    val roleLabel: String,
    val premiumLabel: String = "",
)

data class SettingsHubSectionUiModel(
    val title: String,
    val subtitle: String,
    val items: List<SettingsHubItemUiModel>,
)

data class SettingsHubItemUiModel(
    val title: String,
    val body: String,
    val actionLabel: String,
    val statusLabel: String,
    val icon: ImageVector,
    val destination: SettingsHubDestination,
)

data class SettingsHubActionUiModel(
    val title: String,
    val body: String,
    val actionLabel: String,
    val icon: ImageVector,
)

data class SettingsHubDevActionUiModel(
    val title: String,
    val icon: ImageVector,
    val action: SettingsHubDevAction,
)

enum class SettingsHubDestination {
    Account,
    Appearance,
    Notifications,
    Billing,
    Rewards,
}

enum class SettingsHubDevAction {
    StartOnboarding,
    OpenPremium,
    ResetDemo,
}

private fun defaultSettingsHubSections(): List<SettingsHubSectionUiModel> = listOf(
    SettingsHubSectionUiModel(
        title = "Account & household",
        subtitle = "The screens you reach for most when your profile, reminders, or display context need a quick adjustment.",
        items = listOf(
            SettingsHubItemUiModel(
                title = "Account",
                body = "Identity, household access, premium status, and session details.",
                actionLabel = "Manage account",
                statusLabel = "Profile",
                icon = Icons.Rounded.Person,
                destination = SettingsHubDestination.Account,
            ),
            SettingsHubItemUiModel(
                title = "Appearance",
                body = "Theme, readability, and navigation labels across the app.",
                actionLabel = "Review display options",
                statusLabel = "Display",
                icon = Icons.Rounded.Palette,
                destination = SettingsHubDestination.Appearance,
            ),
            SettingsHubItemUiModel(
                title = "Notifications",
                body = "Reminders, proof approvals, and household alerts that keep the day moving.",
                actionLabel = "Review reminder settings",
                statusLabel = "Alerts",
                icon = Icons.Rounded.Notifications,
                destination = SettingsHubDestination.Notifications,
            ),
        ),
    ),
    SettingsHubSectionUiModel(
        title = "Billing & rewards",
        subtitle = "Premium access and rewards settings for the whole household.",
        items = listOf(
            SettingsHubItemUiModel(
                title = "Manage subscription",
                body = "Plan access, purchases, and premium account details.",
                actionLabel = "Manage subscription",
                statusLabel = "Plan",
                icon = Icons.Rounded.CreditCard,
                destination = SettingsHubDestination.Billing,
            ),
            SettingsHubItemUiModel(
                title = "Rewards",
                body = "Points, streaks, rewards, and household motivation settings.",
                actionLabel = "Open rewards",
                statusLabel = "Points",
                icon = Icons.Rounded.CardGiftcard,
                destination = SettingsHubDestination.Rewards,
            ),
        ),
    ),
)

private fun defaultSignOutAction(): SettingsHubActionUiModel = SettingsHubActionUiModel(
    title = "Sign out",
    body = "Sign out on this device and head back to the welcome flow without changing your household data.",
    actionLabel = "Sign out on this device",
    icon = Icons.AutoMirrored.Rounded.Logout,
)

fun defaultDevSection(): SettingsHubSectionUiModel = SettingsHubSectionUiModel(
    title = "Developer",
    subtitle = "Quick testing shortcuts for debug builds only.",
    items = emptyList(),
)

fun defaultDevActions(): List<SettingsHubDevActionUiModel> = listOf(
    SettingsHubDevActionUiModel(
        title = "Start onboarding",
        icon = Icons.Rounded.PlayArrow,
        action = SettingsHubDevAction.StartOnboarding,
    ),
    SettingsHubDevActionUiModel(
        title = "Open premium",
        icon = Icons.Rounded.Bolt,
        action = SettingsHubDevAction.OpenPremium,
    ),
    SettingsHubDevActionUiModel(
        title = "Reset demo",
        icon = Icons.Rounded.Refresh,
        action = SettingsHubDevAction.ResetDemo,
    ),
)
