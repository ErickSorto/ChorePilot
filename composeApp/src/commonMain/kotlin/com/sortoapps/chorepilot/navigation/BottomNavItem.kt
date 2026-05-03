package com.sortoapps.chorepilot.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

data class MainDestination(
    val label: String,
    val icon: ImageVector,
    val route: AppRoute,
)

val mainDestinations = listOf(
    MainDestination("Home", Icons.Rounded.Home, AppRoute.Home),
    MainDestination("Chores", Icons.Rounded.Checklist, AppRoute.Chores),
    MainDestination("Family", Icons.Rounded.Group, AppRoute.Family),
    MainDestination("Reports", Icons.Rounded.BarChart, AppRoute.Reports),
    MainDestination("Settings", Icons.Rounded.Settings, AppRoute.More),
)
