package com.sortoapps.chorepilot.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.sortoapps.chorepilot.app.presentation.LocalAppSnackbarHostState
import com.sortoapps.chorepilot.core.designsystem.ChorePilotGradientBackground
import com.sortoapps.chorepilot.core.designsystem.ChorePilotSnackbarHost
import com.sortoapps.chorepilot.core.designsystem.LocalWindowSizeClass
import com.sortoapps.chorepilot.core.designsystem.calculateChorePilotWindowSizeClass
import com.sortoapps.chorepilot.features.auth.domain.AuthFlowResult
import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import com.sortoapps.chorepilot.features.auth.domain.canAccessFamilyMember
import com.sortoapps.chorepilot.features.auth.domain.canManageFamilyInviteFlow
import com.sortoapps.chorepilot.features.auth.domain.resolveAccessibleFamilyMemberId
import com.sortoapps.chorepilot.features.auth.presentation.screen.AuthLandingScreen
import com.sortoapps.chorepilot.features.auth.presentation.screen.CreateHouseholdScreen
import com.sortoapps.chorepilot.features.auth.presentation.screen.JoinHouseholdScreen
import com.sortoapps.chorepilot.features.auth.presentation.screen.SignInScreen
import com.sortoapps.chorepilot.features.auth.presentation.platform.rememberGoogleSignInHandler
import com.sortoapps.chorepilot.features.auth.presentation.viewmodel.AuthViewModel
import com.sortoapps.chorepilot.features.account.presentation.screen.AccountScreen
import com.sortoapps.chorepilot.features.account.presentation.viewmodel.AccountViewModel
import com.sortoapps.chorepilot.features.billing.presentation.screen.BillingScreen
import com.sortoapps.chorepilot.features.billing.presentation.viewmodel.BillingViewModel
import com.sortoapps.chorepilot.features.chorecapture.presentation.screen.ChoreCaptureScreen
import com.sortoapps.chorepilot.features.chorecapture.presentation.viewmodel.ChoreCaptureUiEvent
import com.sortoapps.chorepilot.features.chorecapture.presentation.viewmodel.ChoreCaptureViewModel
import com.sortoapps.chorepilot.features.choredetail.presentation.screen.ChoreDetailScreen
import com.sortoapps.chorepilot.features.choredetail.presentation.viewmodel.ChoreDetailUiEvent
import com.sortoapps.chorepilot.features.choredetail.presentation.viewmodel.ChoreDetailViewModel
import com.sortoapps.chorepilot.features.chores.presentation.screen.ChoreEditorScreen
import com.sortoapps.chorepilot.features.chores.domain.ChoreScheduleDay
import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.ChoreEditorUiEvent
import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.ChoreEditorViewModel
import com.sortoapps.chorepilot.features.family.presentation.screen.DayOffScreen
import com.sortoapps.chorepilot.features.family.presentation.screen.GoneModeScreen
import com.sortoapps.chorepilot.features.family.presentation.screen.InviteMemberScreen
import com.sortoapps.chorepilot.features.family.presentation.screen.MemberProfileScreen
import com.sortoapps.chorepilot.features.family.presentation.viewmodel.FamilyUiEvent
import com.sortoapps.chorepilot.features.family.presentation.viewmodel.FamilyViewModel
import com.sortoapps.chorepilot.features.notifications.presentation.screen.NotificationsScreen
import com.sortoapps.chorepilot.features.notifications.domain.NotificationOpenRequest
import com.sortoapps.chorepilot.features.notifications.domain.NotificationOpenTarget
import com.sortoapps.chorepilot.features.notifications.domain.NotificationTapBridge
import com.sortoapps.chorepilot.features.notifications.domain.NotificationsRepository
import com.sortoapps.chorepilot.features.notifications.presentation.viewmodel.NotificationsViewModel
import com.sortoapps.chorepilot.features.onboarding.presentation.screen.OnboardingScreen
import com.sortoapps.chorepilot.features.onboarding.presentation.viewmodel.OnboardingUiEvent
import com.sortoapps.chorepilot.features.onboarding.presentation.viewmodel.OnboardingViewModel
import com.sortoapps.chorepilot.features.premium.presentation.screen.PremiumScreen
import com.sortoapps.chorepilot.features.premium.presentation.viewmodel.PremiumEntryMode
import com.sortoapps.chorepilot.features.premium.presentation.viewmodel.PremiumViewModel
import com.sortoapps.chorepilot.features.rewards.presentation.screen.RewardsScreen
import com.sortoapps.chorepilot.features.rewards.presentation.viewmodel.RewardsViewModel
import com.sortoapps.chorepilot.features.settings.presentation.screen.SettingsScreen
import com.sortoapps.chorepilot.features.settings.presentation.screen.PreviewGalleryScreen
import com.sortoapps.chorepilot.features.settings.presentation.viewmodel.SettingsHubViewModel
import com.sortoapps.chorepilot.features.settings.presentation.viewmodel.SettingsUiEvent
import com.sortoapps.chorepilot.features.settings.presentation.viewmodel.SettingsViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlinx.coroutines.launch

@Composable
fun ChorePilotNavHost() {
    BoxWithConstraints {
        val sizeClass = remember(maxWidth) {
            calculateChorePilotWindowSizeClass(maxWidth)
        }
        androidx.compose.runtime.CompositionLocalProvider(
            LocalWindowSizeClass provides sizeClass,
        ) {
            val navController = rememberNavController()
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = backStackEntry?.destination
            val sessionRepository = koinInject<SessionRepository>()
            val notificationsRepository = koinInject<NotificationsRepository>()
            val session by sessionRepository.session.collectAsStateWithLifecycle()
            val pendingNotificationTap by NotificationTapBridge.pendingOpenRequest.collectAsStateWithLifecycle()
            val coroutineScope = rememberCoroutineScope()
            val snackbarHostState = LocalAppSnackbarHostState.current

            LaunchedEffect(session.signedIn, backStackEntry?.destination) {
                val destination = backStackEntry?.destination ?: return@LaunchedEffect
                val canStaySignedOut = destination.hasRoute(AppRoute.Launch::class) ||
                    destination.hasRoute(AppRoute.SignIn::class) ||
                    destination.hasRoute(AppRoute.Premium::class)
                if (!session.signedIn && !canStaySignedOut) {
                    navController.navigate(AppRoute.SignIn) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }

            LaunchedEffect(session.signedIn, pendingNotificationTap, currentDestination) {
                if (!session.signedIn) return@LaunchedEffect
                val request = pendingNotificationTap ?: return@LaunchedEffect
                notificationsRepository.markRead(request.notificationId)
                navigateToNotificationTarget(
                    request = request,
                    currentDestination = currentDestination,
                    navigate = { route ->
                        navController.navigate(route) {
                            launchSingleTop = true
                        }
                    },
                )
                NotificationTapBridge.clearPendingRequest(request.notificationId)
            }

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                snackbarHost = {
                    if (currentDestination?.hasRoute(AppRoute.Main::class) != true) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            ChorePilotSnackbarHost(
                                hostState = snackbarHostState,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                },
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = AppRoute.Launch,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding)
                ) {
                    composable<AppRoute.Launch> {
                        LaunchedEffect(
                            session.signedIn,
                            session.needsHouseholdSetup,
                            session.needsOnboardingFlow,
                            session.needsInitialPremiumFlow,
                        ) {
                            val target = when {
                                session.signedIn && session.needsOnboardingFlow -> AppRoute.Onboarding
                                session.signedIn && session.needsHouseholdSetup -> AppRoute.HouseholdChoice
                                session.signedIn && session.needsInitialPremiumFlow -> AppRoute.Premium(PremiumOrigin.InitialOnboarding)
                                session.signedIn -> AppRoute.Main
                                else -> AppRoute.SignIn
                            }
                            navController.navigate(target) {
                                popUpTo(AppRoute.Launch) { inclusive = true }
                            }
                        }
                        Box(modifier = Modifier.fillMaxSize())
                    }

                    composable<AppRoute.Onboarding> {
                        val onboardingViewModel = koinViewModel<OnboardingViewModel>()
                        val onboardingUiState by onboardingViewModel.uiState.collectAsStateWithLifecycle()
                        OnboardingScreen(
                            state = onboardingUiState,
                            onEvent = onboardingViewModel::onEvent,
                            onOpenAuth = {
                                navController.navigate(AppRoute.JoinHousehold)
                            },
                            onOpenPremium = {
                                onboardingViewModel.onEvent(OnboardingUiEvent.ContinueClicked)
                                val target = if (session.householdId.isBlank()) {
                                    AppRoute.HouseholdChoice
                                } else {
                                    AppRoute.Premium(PremiumOrigin.InitialOnboarding)
                                }
                                navController.navigate(target) {
                                    popUpTo(AppRoute.Onboarding) { inclusive = true }
                                }
                            },
                            onFinish = {
                                val target = if (session.householdId.isBlank()) {
                                    AppRoute.HouseholdChoice
                                } else {
                                    AppRoute.Launch
                                }
                                navController.navigate(target) {
                                    popUpTo(AppRoute.Onboarding) { inclusive = true }
                                }
                            },
                        )
                    }

                    composable<AppRoute.AuthLanding> {
                        val authViewModel = koinViewModel<AuthViewModel>()
                        val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
                        AuthLandingScreen(
                            state = authUiState,
                            onEvent = authViewModel::onEvent,
                            onCreateHousehold = { navController.navigate(AppRoute.CreateHousehold) },
                            onJoinHousehold = { navController.navigate(AppRoute.JoinHousehold) },
                        )
                    }

                    composable<AppRoute.HouseholdChoice> {
                        val authViewModel = koinViewModel<AuthViewModel>()
                        val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
                        AuthLandingScreen(
                            state = authUiState,
                            onEvent = authViewModel::onEvent,
                            onCreateHousehold = { navController.navigate(AppRoute.CreateHousehold) },
                            onJoinHousehold = { navController.navigate(AppRoute.JoinHousehold) },
                        )
                    }

                    composable<AppRoute.SignIn> {
                        val authViewModel = koinViewModel<AuthViewModel>()
                        val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
                        val googleSignInHandler = rememberGoogleSignInHandler(
                            onSuccess = { token ->
                                authViewModel.signInWithGoogle(token) { result ->
                                    val target = result.toRoute()
                                    navController.navigate(target) {
                                        popUpTo(AppRoute.SignIn) { inclusive = true }
                                    }
                                }
                            },
                            onFailure = { message ->
                                authViewModel.onEvent(
                                    com.sortoapps.chorepilot.features.auth.presentation.viewmodel.AuthUiEvent.GoogleSignInFailed(message),
                                )
                            },
                        )
                        SignInScreen(
                            state = authUiState,
                            onEvent = authViewModel::onEvent,
                            onGoogleSignIn = googleSignInHandler.launch,
                        )
                    }

                    composable<AppRoute.CreateHousehold> {
                        val authViewModel = koinViewModel<AuthViewModel>()
                        val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
                        CreateHouseholdScreen(
                            state = authUiState,
                            onEvent = authViewModel::onEvent,
                            onCreate = {
                                authViewModel.createHousehold { result ->
                                    val target = result.toRoute()
                                    navController.navigate(target) {
                                        popUpTo(AppRoute.HouseholdChoice) { inclusive = true }
                                    }
                                }
                            },
                            onOpenPremium = { navController.navigate(AppRoute.Premium(PremiumOrigin.InitialOnboarding)) },
                        )
                    }

                    composable<AppRoute.JoinHousehold> {
                        val authViewModel = koinViewModel<AuthViewModel>()
                        val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
                        JoinHouseholdScreen(
                            state = authUiState,
                            onEvent = authViewModel::onEvent,
                            onJoin = {
                                authViewModel.joinHousehold { result ->
                                    val target = result.toRoute()
                                    navController.navigate(target) {
                                        popUpTo(AppRoute.JoinHousehold) { inclusive = true }
                                    }
                                }
                            },
                            onDontHavePlace = {
                                val target = if (session.onboardingCompleted) {
                                    AppRoute.HouseholdChoice
                                } else {
                                    AppRoute.Onboarding
                                }
                                navController.navigate(target) {
                                    popUpTo(AppRoute.JoinHousehold) { inclusive = true }
                                }
                            },
                        )
                    }

                    composable<AppRoute.Premium>(
                        enterTransition = {
                            slideInVertically(
                                initialOffsetY = { fullHeight -> fullHeight },
                                animationSpec = tween(durationMillis = 320),
                            )
                        },
                        exitTransition = {
                            slideOutVertically(
                                targetOffsetY = { fullHeight -> fullHeight },
                                animationSpec = tween(durationMillis = 320),
                            )
                        },
                        popExitTransition = {
                            slideOutVertically(
                                targetOffsetY = { fullHeight -> fullHeight },
                                animationSpec = tween(durationMillis = 320),
                            )
                        },
                    ) { entry ->
                        val route = entry.toRoute<AppRoute.Premium>()
                        val origin = route.toPremiumOrigin()
                        val premiumViewModel = koinViewModel<PremiumViewModel>(
                            key = route.originType.name,
                            parameters = {
                                parametersOf(
                                    when (origin) {
                                        PremiumOrigin.InitialOnboarding -> PremiumEntryMode.InitialOnboarding
                                        is PremiumOrigin.ChoreEditorProof,
                                        is PremiumOrigin.DiscoveryCapture,
                                        is PremiumOrigin.Reports,
                                        -> PremiumEntryMode.FeatureGate
                                    },
                                )
                            },
                        )
                        val premiumUiState by premiumViewModel.uiState.collectAsStateWithLifecycle()
                        PremiumScreen(
                            state = premiumUiState,
                            onEvent = premiumViewModel::onEvent,
                            onBack = { navController.popBackStack() },
                            onContinueToAuth = {
                                coroutineScope.launch {
                                    when (origin) {
                                        PremiumOrigin.InitialOnboarding -> {
                                            sessionRepository.dismissInitialPremiumFlow()
                                            navController.navigate(AppRoute.Main) {
                                                popUpTo(AppRoute.Onboarding) { inclusive = true }
                                            }
                                        }

                                        is PremiumOrigin.ChoreEditorProof,
                                        is PremiumOrigin.DiscoveryCapture,
                                        is PremiumOrigin.Reports,
                                        -> {
                                            navController.popBackStack()
                                        }
                                    }
                                }
                            },
                        )
                    }

                    composable<AppRoute.Main> {
                        MainScreen(
                            sizeClass = sizeClass,
                            onOpenChore = { navController.navigate(AppRoute.ChoreDetail(it)) },
                            onEditChore = { navController.navigate(AppRoute.EditChore(it)) },
                            onCreateChore = { navController.navigate(AppRoute.CreateChore) },
                            onOpenChoreCapture = { choreId, day ->
                                navController.navigate(
                                    AppRoute.ChoreCapture(
                                        choreId = choreId,
                                        weekdayName = day?.fullLabel.orEmpty(),
                                        dayOffset = day?.dayOffset ?: 0,
                                        isToday = day?.isToday ?: true,
                                        dateKey = day?.dateKey.orEmpty(),
                                        launchCameraImmediately = choreId.isNotBlank(),
                                    ),
                                )
                            },
                            onOpenMember = { navController.navigate(AppRoute.MemberProfile(it)) },
                            onInviteMember = { navController.navigate(AppRoute.InviteMember) },
                            onOpenAccount = { navController.navigate(AppRoute.Account) },
                            onOpenAppearance = { navController.navigate(AppRoute.Settings) },
                            onOpenNotifications = { navController.navigate(AppRoute.NotificationCenter) },
                            onOpenBilling = { navController.navigate(AppRoute.Billing) },
                            onOpenRewards = { navController.navigate(AppRoute.Rewards) },
                            onStartOnboarding = { navController.navigate(AppRoute.Onboarding) },
                            onOpenPremiumDebug = {
                                navController.navigate(AppRoute.Premium(PremiumOrigin.InitialOnboarding))
                            },
                            onResetDemo = {
                                coroutineScope.launch {
                                    sessionRepository.resetDemo()
                                    navController.navigate(AppRoute.SignIn) {
                                        popUpTo(navController.graph.id) { inclusive = true }
                                    }
                                }
                            },
                            onOpenPremium = { origin -> navController.navigate(AppRoute.Premium(origin)) },
                            onSignOut = {
                                coroutineScope.launch {
                                    sessionRepository.signOut()
                                    navController.navigate(AppRoute.SignIn) {
                                        popUpTo(navController.graph.id) { inclusive = true }
                                    }
                                }
                            },
                        )
                    }

                    composable<AppRoute.ChoreDetail> { entry ->
                        val route = entry.toRoute<AppRoute.ChoreDetail>()
                        val choreDetailViewModel = koinViewModel<ChoreDetailViewModel>()
                        val choreDetailUiState by choreDetailViewModel.uiState.collectAsStateWithLifecycle()
                        LaunchedEffect(route.choreId) {
                            choreDetailViewModel.onEvent(ChoreDetailUiEvent.LoadChore(route.choreId))
                        }
                        LaunchedEffect(choreDetailUiState.shouldCloseAfterDelete) {
                            if (choreDetailUiState.shouldCloseAfterDelete) {
                                choreDetailViewModel.onEvent(ChoreDetailUiEvent.DeleteNavigationHandled)
                                navController.popBackStack()
                            }
                        }
                        ChoreDetailScreen(
                            state = choreDetailUiState,
                            onEvent = choreDetailViewModel::onEvent,
                            onBack = { navController.popBackStack() },
                            onEdit = { navController.navigate(AppRoute.EditChore(route.choreId)) },
                            onCaptureProof = {
                                navController.navigate(
                                    AppRoute.ChoreCapture(
                                        choreId = route.choreId,
                                        launchCameraImmediately = true,
                                    ),
                                )
                            },
                        )
                    }

                    composable<AppRoute.CreateChore> {
                        val choreEditorViewModel = koinViewModel<ChoreEditorViewModel>()
                        val choreEditorUiState by choreEditorViewModel.uiState.collectAsStateWithLifecycle()
                        LaunchedEffect(Unit) {
                            choreEditorViewModel.onEvent(ChoreEditorUiEvent.LoadChore(null))
                        }
                        LaunchedEffect(choreEditorUiState.shouldCloseAfterDelete) {
                            if (choreEditorUiState.shouldCloseAfterDelete) {
                                choreEditorViewModel.onEvent(ChoreEditorUiEvent.DeleteNavigationHandled)
                                navController.popBackStack()
                            }
                        }
                        ChoreEditorScreen(
                            state = choreEditorUiState,
                            onEvent = choreEditorViewModel::onEvent,
                            onBack = { navController.popBackStack() },
                            onOpenPremium = {
                                navController.navigate(
                                    AppRoute.Premium(
                                        PremiumOrigin.ChoreEditorProof(choreId = null),
                                    ),
                                )
                            },
                            onSave = {
                                choreEditorViewModel.save {
                                    navController.popBackStack()
                                }
                            },
                        )
                    }

                    composable<AppRoute.ChoreCapture>(
                        enterTransition = {
                            slideInVertically(
                                initialOffsetY = { fullHeight -> fullHeight },
                                animationSpec = tween(durationMillis = 320),
                            )
                        },
                        exitTransition = {
                            slideOutVertically(
                                targetOffsetY = { fullHeight -> fullHeight },
                                animationSpec = tween(durationMillis = 320),
                            )
                        },
                        popExitTransition = {
                            slideOutVertically(
                                targetOffsetY = { fullHeight -> fullHeight },
                                animationSpec = tween(durationMillis = 320),
                            )
                        },
                    ) { entry ->
                        val route = entry.toRoute<AppRoute.ChoreCapture>()
                        val choreCaptureViewModel = koinViewModel<ChoreCaptureViewModel>()
                        val choreCaptureUiState by choreCaptureViewModel.uiState.collectAsStateWithLifecycle()
                        LaunchedEffect(route.choreId, route.dateKey, route.dayOffset, route.weekdayName) {
                            choreCaptureViewModel.onEvent(
                                ChoreCaptureUiEvent.LoadChore(
                                    choreId = route.choreId,
                                    day = route.toScheduleDayOrNull(),
                                ),
                            )
                        }
                        ChoreCaptureScreen(
                            state = choreCaptureUiState,
                            onEvent = choreCaptureViewModel::onEvent,
                            onBack = { navController.popBackStack() },
                            onOpenPremium = {
                                navController.navigate(
                                    AppRoute.Premium(
                                        PremiumOrigin.DiscoveryCapture(
                                            choreId = route.choreId,
                                            weekdayName = route.weekdayName,
                                            dayOffset = route.dayOffset,
                                            isToday = route.isToday,
                                            dateKey = route.dateKey,
                                        ),
                                    ),
                                )
                            },
                        )
                    }

                    composable<AppRoute.EditChore> { entry ->
                        val route = entry.toRoute<AppRoute.EditChore>()
                        val choreEditorViewModel = koinViewModel<ChoreEditorViewModel>()
                        val choreEditorUiState by choreEditorViewModel.uiState.collectAsStateWithLifecycle()
                        LaunchedEffect(route.choreId) {
                            choreEditorViewModel.onEvent(ChoreEditorUiEvent.LoadChore(route.choreId))
                        }
                        LaunchedEffect(choreEditorUiState.shouldCloseAfterDelete) {
                            if (choreEditorUiState.shouldCloseAfterDelete) {
                                choreEditorViewModel.onEvent(ChoreEditorUiEvent.DeleteNavigationHandled)
                                navController.popBackStack()
                            }
                        }
                        ChoreEditorScreen(
                            state = choreEditorUiState,
                            onEvent = choreEditorViewModel::onEvent,
                            onBack = { navController.popBackStack() },
                            onOpenPremium = {
                                navController.navigate(
                                    AppRoute.Premium(
                                        PremiumOrigin.ChoreEditorProof(choreId = route.choreId),
                                    ),
                                )
                            },
                            onSave = {
                                choreEditorViewModel.save {
                                    navController.popBackStack()
                                }
                            },
                        )
                    }

                    composable<AppRoute.MemberProfile> { entry ->
                        val route = entry.toRoute<AppRoute.MemberProfile>()
                        val resolvedMemberId = session.role.resolveAccessibleFamilyMemberId(
                            viewerId = session.userId,
                            requestedMemberId = route.memberId,
                        ) ?: route.memberId
                        val familyViewModel = koinViewModel<FamilyViewModel>()
                        val familyUiState by familyViewModel.uiState.collectAsStateWithLifecycle()
                        LaunchedEffect(resolvedMemberId) {
                            familyViewModel.onEvent(FamilyUiEvent.MemberSelected(resolvedMemberId))
                        }
                        MemberProfileScreen(
                            state = familyUiState,
                            onEvent = familyViewModel::onEvent,
                            onDayOff = { navController.navigate(AppRoute.DayOff(it)) },
                            onGoneMode = { navController.navigate(AppRoute.GoneMode(it)) },
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable<AppRoute.InviteMember> {
                        if (!session.role.canManageFamilyInviteFlow()) {
                            LaunchedEffect(session.role, session.userId) {
                                navController.popBackStack()
                            }
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Returning to family")
                            }
                        } else {
                            val familyViewModel = koinViewModel<FamilyViewModel>()
                            val familyUiState by familyViewModel.uiState.collectAsStateWithLifecycle()
                            LaunchedEffect(Unit) {
                                familyViewModel.onEvent(FamilyUiEvent.Refresh)
                            }
                            InviteMemberScreen(
                                state = familyUiState,
                                onEvent = familyViewModel::onEvent,
                                onBack = { navController.popBackStack() },
                            )
                        }
                    }

                    composable<AppRoute.DayOff> { entry ->
                        val route = entry.toRoute<AppRoute.DayOff>()
                        val resolvedMemberId = session.role.resolveAccessibleFamilyMemberId(
                            viewerId = session.userId,
                            requestedMemberId = route.memberId,
                        ) ?: route.memberId
                        val familyViewModel = koinViewModel<FamilyViewModel>()
                        val familyUiState by familyViewModel.uiState.collectAsStateWithLifecycle()
                        LaunchedEffect(resolvedMemberId) {
                            familyViewModel.onEvent(FamilyUiEvent.MemberSelected(resolvedMemberId))
                        }
                        DayOffScreen(
                            state = familyUiState,
                            onEvent = familyViewModel::onEvent,
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable<AppRoute.GoneMode> { entry ->
                        val route = entry.toRoute<AppRoute.GoneMode>()
                        val resolvedMemberId = session.role.resolveAccessibleFamilyMemberId(
                            viewerId = session.userId,
                            requestedMemberId = route.memberId,
                        ) ?: route.memberId
                        val familyViewModel = koinViewModel<FamilyViewModel>()
                        val familyUiState by familyViewModel.uiState.collectAsStateWithLifecycle()
                        LaunchedEffect(resolvedMemberId) {
                            familyViewModel.onEvent(FamilyUiEvent.MemberSelected(resolvedMemberId))
                        }
                        GoneModeScreen(
                            state = familyUiState,
                            onEvent = familyViewModel::onEvent,
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable<AppRoute.Account> {
                        val accountViewModel = koinViewModel<AccountViewModel>()
                        val accountUiState by accountViewModel.uiState.collectAsStateWithLifecycle()
                        AccountScreen(
                            state = accountUiState,
                            onEvent = accountViewModel::onEvent,
                            onBack = { navController.popBackStack() },
                            onSignOut = {
                                coroutineScope.launch {
                                    sessionRepository.signOut()
                                    navController.navigate(AppRoute.SignIn) {
                                        popUpTo(navController.graph.id) { inclusive = true }
                                    }
                                }
                            },
                        )
                    }

                    composable<AppRoute.Settings> {
                        val settingsViewModel = koinViewModel<SettingsViewModel>()
                        val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
                        SettingsScreen(
                            state = settingsUiState,
                            onEvent = settingsViewModel::onEvent,
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable<AppRoute.NotificationCenter> {
                        val notificationsViewModel = koinViewModel<NotificationsViewModel>()
                        val notificationsUiState by notificationsViewModel.uiState.collectAsStateWithLifecycle()
                        LaunchedEffect(notificationsUiState.pendingOpenRequest, currentDestination) {
                            val request = notificationsUiState.pendingOpenRequest ?: return@LaunchedEffect
                            navigateToNotificationTarget(
                                request = request,
                                currentDestination = currentDestination,
                                navigate = { route ->
                                    navController.navigate(route) {
                                        launchSingleTop = true
                                    }
                                },
                            )
                            notificationsViewModel.onEvent(
                                com.sortoapps.chorepilot.features.notifications.presentation.viewmodel.NotificationsUiEvent.PendingOpenHandled,
                            )
                        }
                        NotificationsScreen(
                            state = notificationsUiState,
                            onEvent = notificationsViewModel::onEvent,
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable<AppRoute.Billing> {
                        val billingViewModel = koinViewModel<BillingViewModel>()
                        val billingUiState by billingViewModel.uiState.collectAsStateWithLifecycle()
                        BillingScreen(
                            state = billingUiState,
                            onEvent = billingViewModel::onEvent,
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable<AppRoute.Rewards> {
                        val rewardsViewModel = koinViewModel<RewardsViewModel>()
                        val rewardsUiState by rewardsViewModel.uiState.collectAsStateWithLifecycle()
                        RewardsScreen(
                            state = rewardsUiState,
                            onEvent = rewardsViewModel::onEvent,
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable<AppRoute.PreviewGallery> {
                        val settingsHubViewModel = koinViewModel<SettingsHubViewModel>()
                        val settingsHubUiState by settingsHubViewModel.uiState.collectAsStateWithLifecycle()
                        PreviewGalleryScreen(
                            state = settingsHubUiState,
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}

private fun navigateToNotificationTarget(
    request: NotificationOpenRequest,
    currentDestination: androidx.navigation.NavDestination?,
    navigate: (AppRoute) -> Unit,
) {
    when (val target = request.target) {
        NotificationOpenTarget.Inbox -> {
            if (currentDestination?.hasRoute(AppRoute.NotificationCenter::class) != true) {
                navigate(AppRoute.NotificationCenter)
            }
        }

        is NotificationOpenTarget.Chore -> {
            if (target.choreId.isNotBlank()) {
                navigate(AppRoute.ChoreDetail(target.choreId))
            }
        }
    }
}

private fun AuthFlowResult.toRoute(): AppRoute = when (this) {
    AuthFlowResult.Onboarding -> AppRoute.Onboarding
    AuthFlowResult.HouseholdChoice -> AppRoute.HouseholdChoice
    AuthFlowResult.Main -> AppRoute.Main
    AuthFlowResult.CreateHousehold -> AppRoute.CreateHousehold
    AuthFlowResult.Premium -> AppRoute.Premium(PremiumOrigin.InitialOnboarding)
}

private fun AppRoute.ChoreCapture.toScheduleDayOrNull(): ChoreScheduleDay? {
    if (choreId.isBlank() || dateKey.isBlank()) return null
    return ChoreScheduleDay(
        id = dateKey,
        weekdayName = weekdayName.ifBlank { "Today" },
        dayOffset = dayOffset,
        isToday = isToday,
        dateKey = dateKey,
    )
}
