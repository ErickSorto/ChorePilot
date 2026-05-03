package com.sortoapps.chorepilot.features.settings.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sortoapps.chorepilot.core.designsystem.ChorePilotGradientBackground
import com.sortoapps.chorepilot.core.designsystem.ChorePilotScreen
import com.sortoapps.chorepilot.core.designsystem.ChorePilotTheme
import com.sortoapps.chorepilot.core.designsystem.PreviewCard
import com.sortoapps.chorepilot.core.preview.ChorePilotPreviewData
import com.sortoapps.chorepilot.core.preview.CompactPreviewShell
import com.sortoapps.chorepilot.features.settings.domain.ThemeMode
import com.sortoapps.chorepilot.features.settings.presentation.viewmodel.SettingsHubActionUiModel
import com.sortoapps.chorepilot.features.settings.presentation.viewmodel.SettingsHubDevAction
import com.sortoapps.chorepilot.features.settings.presentation.viewmodel.SettingsHubDevActionUiModel
import com.sortoapps.chorepilot.features.settings.presentation.viewmodel.SettingsHubDestination
import com.sortoapps.chorepilot.features.settings.presentation.viewmodel.SettingsHubHouseholdUiModel
import com.sortoapps.chorepilot.features.settings.presentation.viewmodel.SettingsHubItemUiModel
import com.sortoapps.chorepilot.features.settings.presentation.viewmodel.SettingsHubSectionUiModel
import com.sortoapps.chorepilot.features.settings.presentation.viewmodel.SettingsHubUiState

@Composable
fun SettingsHubScreen(
    state: SettingsHubUiState,
    onOpenAccount: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenBilling: () -> Unit,
    onOpenRewards: () -> Unit,
    onStartOnboarding: () -> Unit,
    onOpenPremium: () -> Unit,
    onResetDemo: () -> Unit,
    onSignOut: () -> Unit,
    onSwitchHousehold: (String) -> Unit,
    onHaptic: () -> Unit = {},
) {
    ChorePilotGradientBackground {
        ChorePilotScreen(
            title = "Settings",
            subtitle = "",
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 24.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    SettingsProfileCard(
                        displayName = state.displayName,
                        householdName = state.householdName,
                        activeHouseholdId = state.activeHouseholdId,
                        availableHouseholds = state.availableHouseholds,
                        isSwitchingHousehold = state.isSwitchingHousehold,
                        onSwitchHousehold = { householdId ->
                            onHaptic()
                            onSwitchHousehold(householdId)
                        },
                    )
                    state.sections.forEach { section ->
                        SettingsHubSection(
                            section = section,
                            onItemSelected = { destination ->
                                onHaptic()
                                when (destination) {
                                    SettingsHubDestination.Account -> onOpenAccount()
                                    SettingsHubDestination.Appearance -> onOpenAppearance()
                                    SettingsHubDestination.Notifications -> onOpenNotifications()
                                    SettingsHubDestination.Billing -> onOpenBilling()
                                    SettingsHubDestination.Rewards -> onOpenRewards()
                                }
                            },
                        )
                    }
                    if (state.showDebugTools) {
                        state.devSection?.let { section ->
                            DevToolsSection(
                                section = section,
                                actions = state.devActions,
                                onActionSelected = { action ->
                                    onHaptic()
                                    when (action) {
                                        SettingsHubDevAction.StartOnboarding -> onStartOnboarding()
                                        SettingsHubDevAction.OpenPremium -> onOpenPremium()
                                        SettingsHubDevAction.ResetDemo -> onResetDemo()
                                    }
                                },
                            )
                        }
                    }
                    SignOutSection(
                        action = state.signOutAction,
                        onClick = {
                            onHaptic()
                            onSignOut()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DevToolsSection(
    section: SettingsHubSectionUiModel,
    actions: List<SettingsHubDevActionUiModel>,
    onActionSelected: (SettingsHubDevAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = section.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Column {
                actions.forEachIndexed { index, action ->
                    DevToolRow(
                        action = action,
                        onClick = { onActionSelected(action.action) },
                    )
                    if (index != actions.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                            modifier = Modifier.padding(start = 56.dp, end = 14.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DevToolRow(
    action: SettingsHubDevActionUiModel,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.01f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(8.dp),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Text(
                text = action.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsProfileCard(
    displayName: String,
    householdName: String,
    activeHouseholdId: String,
    availableHouseholds: List<SettingsHubHouseholdUiModel>,
    isSwitchingHousehold: Boolean,
    onSwitchHousehold: (String) -> Unit,
) {
    var switcherExpanded by remember(activeHouseholdId, availableHouseholds) { mutableStateOf(false) }
    val canSwitchHouseholds = availableHouseholds.size > 1
    val activeHousehold = availableHouseholds.firstOrNull { it.id == activeHouseholdId }

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(8.dp),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = initialsFor(displayName),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = householdName.ifBlank { "Your household" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box {
                    Surface(
                        onClick = { if (canSwitchHouseholds && !isSwitchingHousehold) switcherExpanded = true },
                        enabled = canSwitchHouseholds && !isSwitchingHousehold,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(8.dp),
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(0.dp),
                            ) {
                                Text(
                                    text = activeHousehold?.name ?: householdName.ifBlank { "Your household" },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (activeHousehold?.roleLabel?.isNotBlank() == true) {
                                    Text(
                                        text = activeHousehold.roleLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                    )
                                }
                            }
                            if (isSwitchingHousehold) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            } else if (canSwitchHouseholds) {
                                Icon(
                                    imageVector = Icons.Rounded.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    DropdownMenu(
                        expanded = switcherExpanded && canSwitchHouseholds,
                        onDismissRequest = { switcherExpanded = false },
                    ) {
                        availableHouseholds.forEach { household ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(0.dp),
                                        ) {
                                            Text(
                                                text = household.name,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = buildString {
                                                    append(household.roleLabel)
                                                    if (household.premiumLabel.isNotBlank()) {
                                                        append(" • ")
                                                        append(household.premiumLabel)
                                                    }
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                        Spacer(modifier = Modifier.size(12.dp))
                                        if (household.id == activeHouseholdId) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    switcherExpanded = false
                                    if (household.id != activeHouseholdId) {
                                        onSwitchHousehold(household.id)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsHubSection(
    section: SettingsHubSectionUiModel,
    onItemSelected: (SettingsHubDestination) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = section.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Column {
                section.items.forEachIndexed { index, item ->
                    SettingsHubRow(
                        item = item,
                        onClick = { onItemSelected(item.destination) },
                    )
                    if (index != section.items.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                            modifier = Modifier.padding(start = 56.dp, end = 14.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsHubRow(
    item: SettingsHubItemUiModel,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.01f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(8.dp),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SignOutSection(
    action: SettingsHubActionUiModel,
    onClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Session",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            onClick = onClick,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(8.dp),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    Text(
                        text = action.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun initialsFor(name: String): String = name
    .trim()
    .split(" ")
    .filter { it.isNotBlank() }
    .take(2)
    .joinToString("") { it.first().uppercase() }
    .ifBlank { "CP" }

@Composable
fun PreviewGalleryScreen(
    state: SettingsHubUiState,
    onBack: () -> Unit,
) {
    ChorePilotGradientBackground {
        ChorePilotScreen(
            title = "Adaptive and resilience preview gallery",
            subtitle = "Milestone 4 stays available for reference without crowding the main product navigation.",
            actionLabel = "Back",
            onActionClick = onBack,
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                state.previewCards.forEach { preview ->
                    PreviewCard(
                        title = preview.title,
                        body = preview.description,
                        actionLabel = preview.routeLabel,
                        onClick = {},
                    )
                }
            }
        }
    }
}

@Preview(name = "Settings Hub", widthDp = 393, heightDp = 852)
@Composable
private fun SettingsHubScreenPreview() {
    ChorePilotTheme {
        CompactPreviewShell(selectedRoute = com.sortoapps.chorepilot.navigation.AppRoute.More) { modifier ->
            Column(modifier = modifier) {
                SettingsHubScreen(
                    state = ChorePilotPreviewData.settingsHubState(),
                    onOpenAccount = {},
                    onOpenAppearance = {},
                    onOpenNotifications = {},
                    onOpenBilling = {},
                    onOpenRewards = {},
                    onStartOnboarding = {},
                    onOpenPremium = {},
                    onResetDemo = {},
                    onSignOut = {},
                    onSwitchHousehold = {},
                )
            }
        }
    }
}

@Preview(name = "Settings Hub Dark", widthDp = 393, heightDp = 852)
@Composable
private fun SettingsHubScreenDarkPreview() {
    ChorePilotTheme(themeMode = ThemeMode.Dark) {
        CompactPreviewShell(selectedRoute = com.sortoapps.chorepilot.navigation.AppRoute.More) { modifier ->
            Column(modifier = modifier) {
                SettingsHubScreen(
                    state = ChorePilotPreviewData.settingsHubState(),
                    onOpenAccount = {},
                    onOpenAppearance = {},
                    onOpenNotifications = {},
                    onOpenBilling = {},
                    onOpenRewards = {},
                    onStartOnboarding = {},
                    onOpenPremium = {},
                    onResetDemo = {},
                    onSignOut = {},
                    onSwitchHousehold = {},
                )
            }
        }
    }
}

@Preview(name = "Settings Hub Wide", widthDp = 900, heightDp = 700)
@Composable
private fun SettingsHubScreenWidePreview() {
    ChorePilotTheme {
        CompactPreviewShell(selectedRoute = com.sortoapps.chorepilot.navigation.AppRoute.More) { modifier ->
            Column(modifier = modifier) {
                SettingsHubScreen(
                    state = ChorePilotPreviewData.settingsHubState(),
                    onOpenAccount = {},
                    onOpenAppearance = {},
                    onOpenNotifications = {},
                    onOpenBilling = {},
                    onOpenRewards = {},
                    onStartOnboarding = {},
                    onOpenPremium = {},
                    onResetDemo = {},
                    onSignOut = {},
                    onSwitchHousehold = {},
                )
            }
        }
    }
}

@Preview(name = "Settings Hub Long Names", widthDp = 393, heightDp = 852)
@Composable
private fun SettingsHubScreenLongNamesPreview() {
    ChorePilotTheme(largerTextEnabled = true) {
        CompactPreviewShell(selectedRoute = com.sortoapps.chorepilot.navigation.AppRoute.More) { modifier ->
            Column(modifier = modifier) {
                SettingsHubScreen(
                    state = ChorePilotPreviewData.settingsHubState(
                        displayName = "Erick Alejandro Sorto",
                        householdName = "River House and Garden Cooperative",
                    ),
                    onOpenAccount = {},
                    onOpenAppearance = {},
                    onOpenNotifications = {},
                    onOpenBilling = {},
                    onOpenRewards = {},
                    onStartOnboarding = {},
                    onOpenPremium = {},
                    onResetDemo = {},
                    onSignOut = {},
                    onSwitchHousehold = {},
                )
            }
        }
    }
}
