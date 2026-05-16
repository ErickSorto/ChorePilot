package com.sortoapps.chorepilot.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Notifications
import com.sortoapps.chorepilot.app.presentation.LocalAppSnackbarHostState
import com.sortoapps.chorepilot.core.designsystem.ChorePilotSnackbarHost
import com.sortoapps.chorepilot.core.designsystem.ChorePilotTopAppBar
import com.sortoapps.chorepilot.core.designsystem.ChorePilotTopBarBrandBadge
import com.sortoapps.chorepilot.core.designsystem.ChorePilotTopBarIconButton
import com.sortoapps.chorepilot.core.designsystem.ChorePilotWindowSizeClass
import com.sortoapps.chorepilot.core.platform.VibratorUtil
import com.sortoapps.chorepilot.features.chores.presentation.screen.ChoreEditorScreen
import com.sortoapps.chorepilot.features.chores.presentation.screen.ChoresScreen
import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.ChoreEditorUiEvent
import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.ChoreEditorViewModel
import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.ChoresViewModel
import com.sortoapps.chorepilot.features.family.presentation.screen.FamilyScreen
import com.sortoapps.chorepilot.features.family.presentation.viewmodel.FamilyViewModel
import com.sortoapps.chorepilot.features.home.presentation.screen.HomeScreen
import com.sortoapps.chorepilot.features.home.presentation.viewmodel.HomeDayUiModel
import com.sortoapps.chorepilot.features.home.presentation.viewmodel.HomeViewModel
import com.sortoapps.chorepilot.features.reports.presentation.screen.ReportsScreen
import com.sortoapps.chorepilot.features.reports.presentation.viewmodel.ReportsViewModel
import com.sortoapps.chorepilot.features.settings.domain.SettingsRepository
import com.sortoapps.chorepilot.features.settings.presentation.screen.SettingsHubScreen
import com.sortoapps.chorepilot.features.settings.presentation.viewmodel.SettingsHubViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    sizeClass: ChorePilotWindowSizeClass,
    onOpenChore: (String) -> Unit,
    onEditChore: (String) -> Unit,
    onCreateChore: () -> Unit,
    onOpenChoreCapture: (String, HomeDayUiModel?) -> Unit,
    onOpenMember: (String) -> Unit,
    onInviteMember: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenBilling: () -> Unit,
    onOpenRewards: () -> Unit,
    onStartOnboarding: () -> Unit,
    onOpenPremiumDebug: () -> Unit,
    onResetDemo: () -> Unit,
    onOpenPremium: (PremiumOrigin) -> Unit,
    onSignOut: () -> Unit,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val vibratorUtil = koinInject<VibratorUtil>()
    val settingsRepository = koinInject<SettingsRepository>()
    val settings by settingsRepository.settings.collectAsStateWithLifecycle()
    val snackbarHostState = LocalAppSnackbarHostState.current
    val density = LocalDensity.current
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0
    var homeScheduleHeaderExpanded by rememberSaveable { mutableStateOf(false) }
    val isCompactHome = sizeClass == ChorePilotWindowSizeClass.Compact &&
        currentDestination.isHomeDestination()
    val showHomeScheduleHeader = sizeClass != ChorePilotWindowSizeClass.Compact || homeScheduleHeaderExpanded
    val toggleHomeScheduleHeader = {
        vibratorUtil.triggerVibration()
        homeScheduleHeaderExpanded = !homeScheduleHeaderExpanded
    }

    val content: @Composable (Modifier) -> Unit = { modifier ->
        NavHost(
            navController = navController,
            startDestination = AppRoute.Home,
            modifier = modifier,
        ) {
            composable<AppRoute.Home> {
                val homeViewModel = koinViewModel<HomeViewModel>()
                val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
                HomeScreen(
                    state = homeUiState,
                    onEvent = homeViewModel::onEvent,
                    onOpenChoreCapture = { choreId, day ->
                        onOpenChoreCapture(choreId, day)
                    },
                    onOpenChore = onOpenChore,
                    showScheduleHeader = showHomeScheduleHeader,
                    reserveScheduleToggleSpace = isCompactHome,
                    onToggleScheduleHeader = if (isCompactHome) toggleHomeScheduleHeader else null,
                )
            }

            composable<AppRoute.Chores> {
                val choresViewModel = koinViewModel<ChoresViewModel>()
                val choresUiState by choresViewModel.uiState.collectAsStateWithLifecycle()
                ChoresScreen(
                    state = choresUiState,
                    onEvent = choresViewModel::onEvent,
                    onOpenChore = onOpenChore,
                    onCreateChore = onCreateChore,
                    onEditChore = onEditChore,
                    onOpenFoundSomething = { onOpenChoreCapture("", null) },
                )
            }

            composable<AppRoute.Family> {
                val familyViewModel = koinViewModel<FamilyViewModel>()
                val familyUiState by familyViewModel.uiState.collectAsStateWithLifecycle()
                FamilyScreen(
                    state = familyUiState,
                    onEvent = familyViewModel::onEvent,
                    onOpenMember = onOpenMember,
                    onInviteMember = onInviteMember,
                    onOpenPremium = { onOpenPremium(PremiumOrigin.InitialOnboarding) },
                )
            }

            composable<AppRoute.Reports> {
                val reportsViewModel = koinViewModel<ReportsViewModel>()
                val reportsUiState by reportsViewModel.uiState.collectAsStateWithLifecycle()
                ReportsScreen(
                    state = reportsUiState,
                    onEvent = reportsViewModel::onEvent,
                    onOpenPremium = {
                        onOpenPremium(
                            PremiumOrigin.Reports(reportsUiState.selectedRange),
                        )
                    },
                )
            }

            composable<AppRoute.More> {
                val settingsHubViewModel = koinViewModel<SettingsHubViewModel>()
                val settingsHubUiState by settingsHubViewModel.uiState.collectAsStateWithLifecycle()
                SettingsHubScreen(
                    state = settingsHubUiState,
                    onOpenAccount = onOpenAccount,
                    onOpenAppearance = onOpenAppearance,
                    onOpenNotifications = onOpenNotifications,
                    onOpenBilling = onOpenBilling,
                    onOpenRewards = onOpenRewards,
                    onStartOnboarding = onStartOnboarding,
                    onOpenPremium = onOpenPremiumDebug,
                    onResetDemo = onResetDemo,
                    onSignOut = onSignOut,
                    onSwitchHousehold = settingsHubViewModel::switchHousehold,
                    onHaptic = vibratorUtil::triggerVibration,
                )
            }
        }
    }

    when (sizeClass) {
        ChorePilotWindowSizeClass.Compact -> {
            Scaffold(
                modifier = Modifier.imePadding(),
                topBar = {
                    MainTopBar(
                        currentDestination = currentDestination,
                        onOpenNotifications = {
                            vibratorUtil.triggerVibration()
                            onOpenNotifications()
                        },
                    )
                },
                bottomBar = {
                    if (!isImeVisible) {
                        FloatingBottomBar(
                            currentDestination = currentDestination,
                            alwaysShowLabels = settings.alwaysShowLabels,
                            onNavigate = { route ->
                                vibratorUtil.triggerVibration()
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                },
                snackbarHost = {
                    MainSnackbarHost(hostState = snackbarHostState)
                },
            ) { paddingValues ->
                content(Modifier.padding(paddingValues))
            }
        }

        ChorePilotWindowSizeClass.Medium -> {
            Box(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxSize()) {
                    NavigationRail {
                        mainDestinations.forEach { destination ->
                            val selected = generateSequence(currentDestination) { it.parent }
                                .any { it.hasRoute(destination.route::class) }
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    vibratorUtil.triggerVibration()
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(destination.icon, contentDescription = destination.label) },
                                label = { Text(destination.label) },
                            )
                        }
                    }
                    content(Modifier.weight(1f))
                }
                MainSnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter),
                )
            }
        }

        ChorePilotWindowSizeClass.Expanded -> {
            Box(modifier = Modifier.fillMaxSize()) {
                PermanentNavigationDrawer(
                    drawerContent = {
                        PermanentDrawerSheet {
                            mainDestinations.forEach { destination ->
                                val selected = generateSequence(currentDestination) { it.parent }
                                    .any { it.hasRoute(destination.route::class) }
                                NavigationDrawerItem(
                                    label = { Text(destination.label) },
                                    selected = selected,
                                    onClick = {
                                        vibratorUtil.triggerVibration()
                                        navController.navigate(destination.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(destination.icon, contentDescription = destination.label) },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                )
                            }
                        }
                    },
                ) {
                    content(Modifier.fillMaxSize())
                }
                MainSnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter),
                )
            }
        }
    }
}

@Composable
private fun MainSnackbarHost(
    hostState: androidx.compose.material3.SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = Modifier
            .then(modifier)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        ChorePilotSnackbarHost(
            hostState = hostState,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopBar(
    currentDestination: androidx.navigation.NavDestination?,
    onOpenNotifications: () -> Unit,
) {
    val title = when {
        currentDestination?.hasRoute(AppRoute.Chores::class) == true -> "Chores"
        currentDestination?.hasRoute(AppRoute.Family::class) == true -> "Family"
        currentDestination?.hasRoute(AppRoute.Reports::class) == true -> "Reports"
        currentDestination?.hasRoute(AppRoute.More::class) == true -> "Settings"
        else -> "Home"
    }
    val isHome = currentDestination.isHomeDestination()
    Box {
        ChorePilotTopAppBar(
            sectionTitle = title,
            navigationIcon = { ChorePilotTopBarBrandBadge() },
            actionIcon = {
                if (isHome) {
                    ChorePilotTopBarIconButton(
                        icon = Icons.Rounded.Notifications,
                        contentDescription = "Notifications",
                        emphasized = true,
                        onClick = onOpenNotifications,
                    )
                }
            },
        )
    }
}

private fun androidx.navigation.NavDestination?.isHomeDestination(): Boolean =
    this?.hasRoute(AppRoute.Home::class) ?: true

@Composable
private fun FloatingBottomBar(
    currentDestination: androidx.navigation.NavDestination?,
    alwaysShowLabels: Boolean,
    onNavigate: (AppRoute) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
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
                    val selected = generateSequence(currentDestination) { it.parent }
                        .any { it.hasRoute(destination.route::class) }
                    FloatingBottomBarItem(
                        destination = destination,
                        selected = selected,
                        alwaysShowLabel = alwaysShowLabels,
                        onClick = { onNavigate(destination.route) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingBottomBarItem(
    destination: MainDestination,
    selected: Boolean,
    alwaysShowLabel: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val showLabel = alwaysShowLabel || selected

    Surface(
        modifier = modifier
            .clickable(onClick = onClick)
            .height(if (showLabel) 56.dp else 42.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
        } else {
            Color.Transparent
        },
        shape = RoundedCornerShape(18.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(
                        start = 4.dp,
                        end = 4.dp,
                        bottom = if (selected && showLabel) 5.dp else 0.dp,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = if (showLabel) {
                    Arrangement.spacedBy(3.dp, Alignment.CenterVertically)
                } else {
                    Arrangement.Center
                },
            ) {
                Icon(
                    imageVector = destination.icon,
                    contentDescription = destination.label,
                    modifier = Modifier.size(19.dp),
                    tint = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f)
                    },
                )

                if (showLabel) {
                    Text(
                        text = destination.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    )
                }
            }

            if (selected && showLabel) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 2.dp)
                        .width(24.dp)
                        .height(3.dp),
                    shape = RoundedCornerShape(99.dp),
                    color = MaterialTheme.colorScheme.primary,
                ) {}
            }
        }
    }
}
