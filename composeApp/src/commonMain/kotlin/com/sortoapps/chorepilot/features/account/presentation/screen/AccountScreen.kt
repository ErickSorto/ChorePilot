package com.sortoapps.chorepilot.features.account.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CreditScore
import androidx.compose.material.icons.rounded.DoorBack
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.PeopleAlt
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sortoapps.chorepilot.core.designsystem.ChorePilotGradientBackground
import com.sortoapps.chorepilot.core.designsystem.ChorePilotTheme
import com.sortoapps.chorepilot.core.designsystem.ChorePilotTopAppBar
import com.sortoapps.chorepilot.core.designsystem.ChorePilotTopBarIconButton
import com.sortoapps.chorepilot.core.designsystem.PrimaryButton
import com.sortoapps.chorepilot.core.designsystem.ToneChip
import com.sortoapps.chorepilot.core.preview.ChorePilotPreviewData
import com.sortoapps.chorepilot.core.preview.ChorePilotPreviewTheme
import com.sortoapps.chorepilot.core.preview.CompactPreviewShell
import com.sortoapps.chorepilot.features.account.presentation.viewmodel.AccountUiEvent
import com.sortoapps.chorepilot.features.account.presentation.viewmodel.AccountUiState
import com.sortoapps.chorepilot.features.settings.domain.ThemeMode
import com.sortoapps.chorepilot.navigation.AppRoute

@Composable
fun AccountScreen(
    state: AccountUiState,
    onEvent: (AccountUiEvent) -> Unit,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
) {
    ChorePilotGradientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                ChorePilotTopAppBar(
                    sectionTitle = "Account",
                    navigationIcon = {
                        ChorePilotTopBarIconButton(
                            icon = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            onClick = {
                                onEvent(AccountUiEvent.TriggerVibration)
                                onBack()
                            },
                        )
                    },
                )
            },
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (state.isLoading) {
                    item {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 720.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            AccountHeroCard(state = state)
                            AccountSectionCard(
                                title = "Profile",
                                rows = listOf(
                                    AccountInfoRowUiModel(
                                        icon = Icons.Rounded.PersonOutline,
                                        label = "Name",
                                        value = state.displayName,
                                    ),
                                    AccountInfoRowUiModel(
                                        icon = Icons.Rounded.MailOutline,
                                        label = "Email",
                                        value = state.email.ifBlank { "Google account email unavailable" },
                                    ),
                                    AccountInfoRowUiModel(
                                        icon = Icons.Rounded.Security,
                                        label = "Sign in",
                                        value = state.signedInMethodLabel,
                                    ),
                                ),
                            )
                            AccountSectionCard(
                                title = "Household",
                                rows = listOf(
                                    AccountInfoRowUiModel(
                                        icon = Icons.Rounded.PeopleAlt,
                                        label = "Household",
                                        value = state.householdName,
                                    ),
                                    AccountInfoRowUiModel(
                                        icon = Icons.Rounded.Badge,
                                        label = "Role",
                                        value = state.householdRoleLabel,
                                    ),
                                    AccountInfoRowUiModel(
                                        icon = Icons.Rounded.Shield,
                                        label = "Access",
                                        value = buildList {
                                            add(state.seatLabel)
                                            if (state.seatContextLabel.isNotBlank()) add(state.seatContextLabel)
                                        }.joinToString(" • "),
                                    ),
                                    AccountInfoRowUiModel(
                                        icon = Icons.Rounded.CreditScore,
                                        label = "Plan",
                                        value = state.premiumStatusLabel,
                                    ),
                                ),
                            )
                            AccountSectionCard(
                                title = "Session",
                                rows = listOf(
                                    AccountInfoRowUiModel(
                                        icon = Icons.Rounded.DoorBack,
                                        label = "This device",
                                        value = state.signOutSummary,
                                    ),
                                ),
                            )
                            SessionCard(
                                onSignOut = {
                                    onEvent(AccountUiEvent.TriggerVibration)
                                    onSignOut()
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
private fun AccountHeroCard(
    state: AccountUiState,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = initialsFor(state.displayName),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = state.displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = state.householdName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ToneChip(text = state.householdRoleLabel, tone = com.sortoapps.chorepilot.core.demo.DemoTone.Primary)
                ToneChip(
                    text = state.premiumStatusLabel,
                    tone = if (state.premiumStatusLabel.contains("Pro")) {
                        com.sortoapps.chorepilot.core.demo.DemoTone.Success
                    } else {
                        com.sortoapps.chorepilot.core.demo.DemoTone.Secondary
                    },
                )
            }
            Text(
                text = state.email.ifBlank { state.signedInMethodLabel },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AccountSectionCard(
    title: String,
    rows: List<AccountInfoRowUiModel>,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            rows.forEach { row -> AccountInfoRow(row = row) }
        }
    }
}

@Composable
private fun AccountInfoRow(
    row: AccountInfoRowUiModel,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(38.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = row.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = row.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = row.value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (row.supportingText.isNotBlank()) {
                Text(
                    text = row.supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private data class AccountInfoRowUiModel(
    val icon: ImageVector,
    val label: String,
    val value: String,
    val supportingText: String = "",
)

@Composable
private fun SessionCard(
    onSignOut: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PrimaryButton(
                text = "Sign out",
                onClick = onSignOut,
            )
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

@Preview(name = "Account Light", widthDp = 393, heightDp = 852)
@Composable
private fun AccountScreenPreview() {
    ChorePilotPreviewTheme {
        CompactPreviewShell(selectedRoute = AppRoute.More) { modifier ->
            Box(modifier = modifier) {
                AccountScreen(
                    state = ChorePilotPreviewData.accountState(),
                    onEvent = {},
                    onBack = {},
                    onSignOut = {},
                )
            }
        }
    }
}

@Preview(name = "Account Dark", widthDp = 393, heightDp = 852)
@Composable
private fun AccountScreenDarkPreview() {
    ChorePilotTheme(themeMode = ThemeMode.Dark) {
        CompactPreviewShell(selectedRoute = AppRoute.More) { modifier ->
            Box(modifier = modifier) {
                AccountScreen(
                    state = ChorePilotPreviewData.accountState(),
                    onEvent = {},
                    onBack = {},
                    onSignOut = {},
                )
            }
        }
    }
}

@Preview(name = "Account Wide", widthDp = 900, heightDp = 720)
@Composable
private fun AccountScreenWidePreview() {
    ChorePilotPreviewTheme {
        CompactPreviewShell(selectedRoute = AppRoute.More) { modifier ->
            Box(modifier = modifier) {
                AccountScreen(
                    state = ChorePilotPreviewData.accountState(),
                    onEvent = {},
                    onBack = {},
                    onSignOut = {},
                )
            }
        }
    }
}
