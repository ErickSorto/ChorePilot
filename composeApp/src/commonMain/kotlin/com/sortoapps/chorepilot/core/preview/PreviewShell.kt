package com.sortoapps.chorepilot.core.preview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import com.sortoapps.chorepilot.core.designsystem.ChorePilotTopAppBar
import com.sortoapps.chorepilot.core.designsystem.ChorePilotTopBarBrandBadge
import com.sortoapps.chorepilot.core.designsystem.ChorePilotTopBarIconButton
import com.sortoapps.chorepilot.navigation.AppRoute
import com.sortoapps.chorepilot.navigation.mainDestinations

private val PreviewSafeAreaTop = 28.dp
private val PreviewSafeAreaBottom = 18.dp

@Composable
fun PreviewDeviceFrame(
    content: @Composable (Modifier) -> Unit,
) {
    content(
        Modifier
            .fillMaxSize()
            .padding(top = PreviewSafeAreaTop, bottom = PreviewSafeAreaBottom),
    )
}

@Composable
fun CompactPreviewShell(
    selectedRoute: AppRoute,
    alwaysShowLabels: Boolean = true,
    content: @Composable (Modifier) -> Unit,
) {
    PreviewDeviceFrame { frameModifier ->
        Scaffold(
            modifier = frameModifier.fillMaxSize(),
            topBar = {
                PreviewTopBar(selectedRoute = selectedRoute)
            },
            bottomBar = {
                PreviewBottomBar(
                    selectedRoute = selectedRoute,
                    alwaysShowLabels = alwaysShowLabels,
                )
            },
        ) { paddingValues ->
            content(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            )
        }
    }
}

@Composable
private fun PreviewTopBar(
    selectedRoute: AppRoute,
) {
    ChorePilotTopAppBar(
        sectionTitle = previewTitle(selectedRoute),
        navigationIcon = { ChorePilotTopBarBrandBadge() },
        actionIcon = {
            previewActionIcon(selectedRoute)?.let { icon ->
                ChorePilotTopBarIconButton(
                    icon = icon,
                    contentDescription = null,
                    enabled = false,
                    emphasized = selectedRoute == AppRoute.Home,
                )
            }
        },
    )
}

private fun previewTitle(selectedRoute: AppRoute): String = when (selectedRoute) {
    AppRoute.Home -> "Home"
    AppRoute.Chores -> "Chores"
    AppRoute.Family -> "Family"
    AppRoute.Reports -> "Reports"
    AppRoute.More -> "Settings"
    AppRoute.Account -> "Account"
    AppRoute.Settings -> "Appearance"
    else -> "ChorePilot"
}

private fun previewActionIcon(selectedRoute: AppRoute): ImageVector? = when (selectedRoute) {
    AppRoute.Home -> Icons.Rounded.Notifications
    else -> null
}

@Composable
private fun PreviewBottomBar(
    selectedRoute: AppRoute,
    alwaysShowLabels: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 4.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                mainDestinations.forEach { destination ->
                    PreviewBottomBarItem(
                        label = destination.label,
                        icon = destination.icon,
                        selected = selectedRoute::class == destination.route::class,
                        alwaysShowLabel = alwaysShowLabels,
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewBottomBarItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    alwaysShowLabel: Boolean,
) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .clickable(enabled = false, onClick = {})
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Surface(
            color = if (selected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f)
            },
            shape = CircleShape,
            shadowElevation = 0.dp,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .padding(10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (alwaysShowLabel || selected) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
