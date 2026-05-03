package com.sortoapps.chorepilot.features.settings.presentation.screen

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sortoapps.chorepilot.core.demo.DemoTone
import com.sortoapps.chorepilot.core.designsystem.ChorePilotGradientBackground
import com.sortoapps.chorepilot.core.designsystem.ChorePilotTheme
import com.sortoapps.chorepilot.core.designsystem.ChorePilotTopAppBar
import com.sortoapps.chorepilot.core.designsystem.ChorePilotTopBarIconButton
import com.sortoapps.chorepilot.core.designsystem.PremiumTabItem
import com.sortoapps.chorepilot.core.designsystem.PremiumTabSelector
import com.sortoapps.chorepilot.core.designsystem.ToneChip
import com.sortoapps.chorepilot.core.preview.ChorePilotPreviewTheme
import com.sortoapps.chorepilot.core.preview.CompactPreviewShell
import com.sortoapps.chorepilot.features.settings.domain.AppearancePreset
import com.sortoapps.chorepilot.features.settings.domain.ThemeMode
import com.sortoapps.chorepilot.features.settings.presentation.viewmodel.SettingsUiEvent
import com.sortoapps.chorepilot.features.settings.presentation.viewmodel.SettingsUiState
import com.sortoapps.chorepilot.navigation.AppRoute

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    onBack: () -> Unit,
) {
    ChorePilotGradientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                ChorePilotTopAppBar(
                    sectionTitle = "Appearance",
                    navigationIcon = {
                        ChorePilotTopBarIconButton(
                            icon = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            onClick = {
                                onEvent(SettingsUiEvent.TriggerVibration)
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
                            AppearanceHeroCard(
                                themeMode = state.themeMode,
                                largerText = state.largerText,
                                appearancePreset = state.appearancePreset,
                            )
                            state.saveFeedbackMessage?.let { message ->
                                AppearanceMessageCard(message = message)
                            }
                            AppearanceSection(title = "Theme") {
                                ThemeModeCard(
                                    selectedThemeMode = state.themeMode,
                                    onSelect = { themeMode ->
                                        onEvent(SettingsUiEvent.TriggerVibration)
                                        onEvent(SettingsUiEvent.ThemeModeSelected(themeMode))
                                    },
                                )
                            }
                            AppearanceSection(title = "Design system") {
                                AppearancePresetCard(
                                    selectedPreset = state.appearancePreset,
                                    onSelect = { preset ->
                                        onEvent(SettingsUiEvent.TriggerVibration)
                                        onEvent(SettingsUiEvent.AppearancePresetSelected(preset))
                                    },
                                )
                            }
                            AppearanceSection(title = "Readability") {
                                AppearanceSettingsCard(
                                    rows = listOf(
                                        AppearanceToggleRowUiModel(
                                            icon = Icons.Rounded.FormatSize,
                                            title = "Larger text",
                                            summary = "Scale typography up a bit for easier scanning without changing layout structure.",
                                            checked = state.largerText,
                                            onCheckedChange = { enabled ->
                                                onEvent(SettingsUiEvent.TriggerVibration)
                                                onEvent(SettingsUiEvent.ToggleLargerText(enabled))
                                            },
                                        ),
                                    ),
                                )
                            }
                            AppearanceSection(title = "Navigation") {
                                AppearanceSettingsCard(
                                    rows = listOf(
                                        AppearanceToggleRowUiModel(
                                            icon = Icons.AutoMirrored.Rounded.Label,
                                            title = "Always show labels",
                                            summary = "Keep bottom navigation labels visible instead of switching to the compact style.",
                                            checked = state.alwaysShowLabels,
                                            onCheckedChange = { enabled ->
                                                onEvent(SettingsUiEvent.TriggerVibration)
                                                onEvent(SettingsUiEvent.ToggleAlwaysShowLabels(enabled))
                                            },
                                        ),
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppearanceHeroCard(
    themeMode: ThemeMode,
    largerText: Boolean,
    appearancePreset: AppearancePreset,
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Tune how ChorePilot feels",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Theme, readability, and navigation choices update the whole app without changing your household data.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ToneChip(
                    text = if (themeMode == ThemeMode.Dark) "Dark" else "Light",
                    tone = DemoTone.Primary,
                )
                ToneChip(
                    text = appearancePresetChipLabel(appearancePreset),
                    tone = when (appearancePreset) {
                        AppearancePreset.Classic -> DemoTone.Neutral
                        AppearancePreset.EditorialWarm -> DemoTone.Secondary
                        AppearancePreset.SunDrenchedEditorial -> DemoTone.Warning
                    },
                )
                if (largerText) {
                    ToneChip(text = "Larger text", tone = DemoTone.Tertiary)
                }
            }
        }
    }
}

@Composable
private fun AppearanceMessageCard(
    message: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Visibility,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun AppearanceSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        content()
    }
}

@Composable
private fun ThemeModeCard(
    selectedThemeMode: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PremiumTabSelector(
                items = ThemeMode.entries.map { PremiumTabItem(label = it.name) },
                selectedIndex = ThemeMode.entries.indexOf(selectedThemeMode),
                onSelect = { onSelect(ThemeMode.entries[it]) },
                highlightColor = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                cornerRadius = 8.dp,
                indicatorCornerRadius = 8.dp,
                indicatorInset = 4.dp,
                flatStyle = true,
            )
            Text(
                text = if (selectedThemeMode == ThemeMode.Dark) {
                    "Use the darker palette for stronger contrast at night."
                } else {
                    "Keep the brighter palette for daytime scanning and softer surfaces."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AppearancePresetCard(
    selectedPreset: AppearancePreset,
    onSelect: (AppearancePreset) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PremiumTabSelector(
                items = AppearancePreset.entries.map { PremiumTabItem(label = appearancePresetTabLabel(it)) },
                selectedIndex = AppearancePreset.entries.indexOf(selectedPreset),
                onSelect = { onSelect(AppearancePreset.entries[it]) },
                highlightColor = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                cornerRadius = 8.dp,
                indicatorCornerRadius = 8.dp,
                indicatorInset = 4.dp,
                flatStyle = true,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when (selectedPreset) {
                                AppearancePreset.Classic -> Icons.Rounded.Palette
                                AppearancePreset.EditorialWarm -> Icons.Rounded.AutoAwesome
                                AppearancePreset.SunDrenchedEditorial -> Icons.Rounded.WbSunny
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = appearancePresetTitle(selectedPreset),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = appearancePresetDescription(selectedPreset),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun appearancePresetTabLabel(preset: AppearancePreset): String = when (preset) {
    AppearancePreset.Classic -> "Classic"
    AppearancePreset.EditorialWarm -> "Warm"
    AppearancePreset.SunDrenchedEditorial -> "Sun"
}

private fun appearancePresetTitle(preset: AppearancePreset): String = when (preset) {
    AppearancePreset.Classic -> "Classic ChorePilot"
    AppearancePreset.EditorialWarm -> "Warm editorial"
    AppearancePreset.SunDrenchedEditorial -> "Sun-drenched editorial"
}

private fun appearancePresetDescription(preset: AppearancePreset): String = when (preset) {
    AppearancePreset.Classic -> "Keep the current house style with cooler surfaces and the standard shape rhythm."
    AppearancePreset.EditorialWarm -> "Use the softer editorial palette and rounded structure already familiar in the app."
    AppearancePreset.SunDrenchedEditorial -> "Test the golden-hour palette with warmer paper-like surfaces, amber actions, and a richer editorial mood."
}

private fun appearancePresetChipLabel(preset: AppearancePreset): String = when (preset) {
    AppearancePreset.Classic -> "Classic"
    AppearancePreset.EditorialWarm -> "Warm"
    AppearancePreset.SunDrenchedEditorial -> "Sun-drenched"
}

private data class AppearanceToggleRowUiModel(
    val icon: ImageVector,
    val title: String,
    val summary: String,
    val checked: Boolean,
    val onCheckedChange: (Boolean) -> Unit,
)

@Composable
private fun AppearanceSettingsCard(
    rows: List<AppearanceToggleRowUiModel>,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column {
            rows.forEachIndexed { index, row ->
                AppearanceToggleRow(row = row)
                if (index != rows.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                        modifier = Modifier.padding(start = 56.dp, end = 14.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AppearanceToggleRow(
    row: AppearanceToggleRowUiModel,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
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
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = row.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = row.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = row.checked,
            onCheckedChange = row.onCheckedChange,
        )
    }
}

@Preview(name = "Appearance Light", widthDp = 393, heightDp = 852)
@Composable
private fun SettingsScreenPreview() {
    ChorePilotPreviewTheme {
        CompactPreviewShell(selectedRoute = AppRoute.More) { modifier ->
            Box(modifier = modifier) {
                SettingsScreen(
                    state = SettingsUiState(
                        themeMode = ThemeMode.Light,
                        appearancePreset = AppearancePreset.SunDrenchedEditorial,
                        largerText = false,
                        alwaysShowLabels = true,
                        saveFeedbackMessage = "Theme saved",
                    ),
                    onEvent = {},
                    onBack = {},
                )
            }
        }
    }
}

@Preview(name = "Appearance Dark", widthDp = 393, heightDp = 852)
@Composable
private fun SettingsScreenDarkPreview() {
    ChorePilotTheme(
        themeMode = ThemeMode.Dark,
        appearancePreset = AppearancePreset.EditorialWarm,
    ) {
        CompactPreviewShell(selectedRoute = AppRoute.More) { modifier ->
            Box(modifier = modifier) {
                SettingsScreen(
                    state = SettingsUiState(
                        themeMode = ThemeMode.Dark,
                        appearancePreset = AppearancePreset.EditorialWarm,
                        largerText = true,
                        alwaysShowLabels = false,
                    ),
                    onEvent = {},
                    onBack = {},
                )
            }
        }
    }
}

@Preview(name = "Appearance Wide", widthDp = 900, heightDp = 720)
@Composable
private fun SettingsScreenWidePreview() {
    ChorePilotPreviewTheme {
        CompactPreviewShell(selectedRoute = AppRoute.More) { modifier ->
            Box(modifier = modifier) {
                SettingsScreen(
                    state = SettingsUiState(
                        themeMode = ThemeMode.Light,
                        appearancePreset = AppearancePreset.Classic,
                        largerText = false,
                        alwaysShowLabels = true,
                    ),
                    onEvent = {},
                    onBack = {},
                )
            }
        }
    }
}
