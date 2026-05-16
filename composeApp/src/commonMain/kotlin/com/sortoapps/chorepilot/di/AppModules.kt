package com.sortoapps.chorepilot.di

import com.sortoapps.chorepilot.core.firebase.FirebaseHouseholdStore
import com.sortoapps.chorepilot.core.firebase.FirebaseAvailability
import com.sortoapps.chorepilot.core.firebase.FirebaseUnavailableChoreRepository
import com.sortoapps.chorepilot.core.firebase.FirebaseUnavailableDiscoveryAnalysisRepository
import com.sortoapps.chorepilot.core.firebase.FirebaseUnavailableFamilyRepository
import com.sortoapps.chorepilot.core.firebase.FirebaseUnavailableFeedbackPalRepository
import com.sortoapps.chorepilot.core.firebase.FirebaseUnavailableReportsRepository
import com.sortoapps.chorepilot.core.firebase.FirebaseUnavailableRewardsRepository
import com.sortoapps.chorepilot.core.firebase.FirebaseUnavailableSessionRepository
import com.sortoapps.chorepilot.core.demo.InMemoryDemoContentRepository
import com.sortoapps.chorepilot.core.revenuecat.RevenueCatSubscriptionRepository
import com.sortoapps.chorepilot.core.revenuecat.SubscriptionRepository
import com.sortoapps.chorepilot.features.account.presentation.viewmodel.AccountViewModel
import com.sortoapps.chorepilot.features.auth.data.FirebaseSessionRepository
import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import com.sortoapps.chorepilot.features.auth.presentation.viewmodel.AuthViewModel
import com.sortoapps.chorepilot.features.billing.presentation.viewmodel.BillingViewModel
import com.sortoapps.chorepilot.features.chorecapture.data.FirebaseDiscoveryAnalysisRepository
import com.sortoapps.chorepilot.features.chorecapture.domain.DiscoveryAnalysisRepository
import com.sortoapps.chorepilot.features.chorecapture.presentation.viewmodel.ChoreCaptureViewModel
import com.sortoapps.chorepilot.features.choredetail.presentation.viewmodel.ChoreDetailViewModel
import com.sortoapps.chorepilot.features.chores.data.FirebaseChoreRepository
import com.sortoapps.chorepilot.features.chores.domain.ApproveProofUseCase
import com.sortoapps.chorepilot.features.chores.domain.ChoreRepository
import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.ChoreEditorViewModel
import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.ChoresViewModel
import com.sortoapps.chorepilot.features.family.domain.FamilyRepository
import com.sortoapps.chorepilot.features.family.data.FirebaseFamilyRepository
import com.sortoapps.chorepilot.features.family.presentation.viewmodel.FamilyViewModel
import com.sortoapps.chorepilot.features.feedbackpal.data.FirebaseFeedbackPalRepository
import com.sortoapps.chorepilot.features.feedbackpal.domain.FeedbackPalRepository
import com.sortoapps.chorepilot.features.home.data.ProductionHomeRepository
import com.sortoapps.chorepilot.features.home.presentation.viewmodel.HomeViewModel
import com.sortoapps.chorepilot.features.home.domain.HomeRepository
import com.sortoapps.chorepilot.features.notifications.domain.HouseholdNotifications
import com.sortoapps.chorepilot.features.notifications.domain.AppVisibilityTracker
import com.sortoapps.chorepilot.features.notifications.domain.NotificationDeliveryGateway
import com.sortoapps.chorepilot.features.notifications.domain.NotificationPermissionSynchronizer
import com.sortoapps.chorepilot.features.notifications.domain.PushNotificationRegistrationSynchronizer
import com.sortoapps.chorepilot.features.notifications.domain.PushTokenSource
import com.sortoapps.chorepilot.features.notifications.domain.NotificationsRepository
import com.sortoapps.chorepilot.features.notifications.domain.NotificationSettingsController
import com.sortoapps.chorepilot.features.notifications.domain.RepositoryBackedHouseholdNotifications
import com.sortoapps.chorepilot.features.notifications.data.PushDeviceRegistrationRepository
import com.sortoapps.chorepilot.features.notifications.data.NotificationsStorePersistence
import com.sortoapps.chorepilot.features.notifications.data.PlatformPreferencesNotificationsStorePersistence
import com.sortoapps.chorepilot.features.notifications.data.StorageBackedNotificationsRepository
import com.sortoapps.chorepilot.features.notifications.data.FirebaseNotificationsRepository
import com.sortoapps.chorepilot.features.notifications.data.FirebasePushDeviceRegistrationRepository
import com.sortoapps.chorepilot.features.notifications.data.FirebaseSharedNotificationsStore
import com.sortoapps.chorepilot.features.notifications.data.NoOpPushDeviceRegistrationRepository
import com.sortoapps.chorepilot.features.notifications.presentation.viewmodel.NotificationsViewModel
import com.sortoapps.chorepilot.features.onboarding.presentation.viewmodel.OnboardingViewModel
import com.sortoapps.chorepilot.features.premium.presentation.viewmodel.PremiumViewModel
import com.sortoapps.chorepilot.features.premium.presentation.viewmodel.PremiumEntryMode
import com.sortoapps.chorepilot.features.reports.domain.ReportsRepository
import com.sortoapps.chorepilot.features.reports.data.ProductionReportsRepository
import com.sortoapps.chorepilot.features.reports.presentation.viewmodel.ReportsViewModel
import com.sortoapps.chorepilot.features.rewards.data.FirebaseRewardsRepository
import com.sortoapps.chorepilot.features.rewards.domain.RewardsRepository
import com.sortoapps.chorepilot.features.rewards.presentation.viewmodel.RewardsViewModel
import com.sortoapps.chorepilot.features.settings.data.PersistedSettingsRepository
import com.sortoapps.chorepilot.features.settings.domain.SettingsRepository
import com.sortoapps.chorepilot.features.settings.presentation.viewmodel.SettingsHubViewModel
import com.sortoapps.chorepilot.features.settings.presentation.viewmodel.SettingsViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.koin.dsl.bind
import org.koin.core.module.dsl.viewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

val appModule = module {
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    singleOf(::AppVisibilityTracker)
    single { FirebaseHouseholdStore() }
    single<SessionRepository> {
        if (get<FirebaseAvailability>().isConfigured) {
            FirebaseSessionRepository(get(), get(), get())
        } else {
            FirebaseUnavailableSessionRepository(get())
        }
    }
    singleOf(::PersistedSettingsRepository) bind SettingsRepository::class
    single<ChoreRepository> {
        if (get<FirebaseAvailability>().isConfigured) {
            FirebaseChoreRepository(get(), get(), get(), get())
        } else {
            FirebaseUnavailableChoreRepository(get())
        }
    }
    singleOf(::ApproveProofUseCase)
    single<DiscoveryAnalysisRepository> {
        if (get<FirebaseAvailability>().isConfigured) {
            FirebaseDiscoveryAnalysisRepository(get())
        } else {
            FirebaseUnavailableDiscoveryAnalysisRepository(get())
        }
    }
    singleOf(::InMemoryDemoContentRepository)
    single<FeedbackPalRepository> {
        if (get<FirebaseAvailability>().isConfigured) {
            FirebaseFeedbackPalRepository(get())
        } else {
            FirebaseUnavailableFeedbackPalRepository(get())
        }
    }
    singleOf(::RevenueCatSubscriptionRepository) bind SubscriptionRepository::class
    singleOf(::ProductionHomeRepository) bind HomeRepository::class
    single<FamilyRepository> {
        if (get<FirebaseAvailability>().isConfigured) {
            FirebaseFamilyRepository(get(), get(), get(), get())
        } else {
            FirebaseUnavailableFamilyRepository(get())
        }
    }
    single<ReportsRepository> {
        if (get<FirebaseAvailability>().isConfigured) {
            ProductionReportsRepository(get(), get())
        } else {
            FirebaseUnavailableReportsRepository(get())
        }
    }
    single<RewardsRepository> {
        if (get<FirebaseAvailability>().isConfigured) {
            FirebaseRewardsRepository(get(), get(), get(), get())
        } else {
            FirebaseUnavailableRewardsRepository(get())
        }
    }
    single<NotificationsStorePersistence> { PlatformPreferencesNotificationsStorePersistence(get()) }
    single<PushDeviceRegistrationRepository> {
        if (get<FirebaseAvailability>().isConfigured) {
            FirebasePushDeviceRegistrationRepository()
        } else {
            NoOpPushDeviceRegistrationRepository()
        }
    }
    single<NotificationsRepository> {
        if (get<FirebaseAvailability>().isConfigured) {
            FirebaseNotificationsRepository(
                sharedStore = FirebaseSharedNotificationsStore(get()),
                sessionRepository = get(),
                appScope = get(),
            )
        } else {
            StorageBackedNotificationsRepository(get(), get(), get())
        }
    }
    single { NotificationSettingsController(get(), get()) }
    single(createdAtStart = true) {
        NotificationPermissionSynchronizer(
            sessionRepository = get(),
            settingsRepository = get(),
            notificationSettingsController = get(),
            appScope = get(),
        )
    }
    single(createdAtStart = true) {
        PushNotificationRegistrationSynchronizer(
            registrationRepository = get(),
            sessionRepository = get(),
            settingsRepository = get(),
            pushTokenSource = get<PushTokenSource>(),
            platformPreferences = get(),
            appScope = get(),
        )
    }
    single<HouseholdNotifications> {
        RepositoryBackedHouseholdNotifications(
            notificationsRepository = get(),
            deliveryGateway = get(),
            appVisibilityTracker = get(),
            sessionRepository = get(),
            notificationsEnabled = { get<SettingsRepository>().settings.value.notificationsEnabled },
        )
    }
}

val viewModelModule = module {
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::AuthViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::ChoresViewModel)
    viewModelOf(::ChoreEditorViewModel)
    viewModelOf(::ChoreCaptureViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::AccountViewModel)
    viewModelOf(::ChoreDetailViewModel)
    viewModelOf(::FamilyViewModel)
    viewModelOf(::createReportsViewModel)
    viewModelOf(::RewardsViewModel)
    viewModelOf(::SettingsHubViewModel)
    viewModelOf(::NotificationsViewModel)
    viewModelOf(::BillingViewModel)
    viewModel { (entryMode: PremiumEntryMode) ->
        PremiumViewModel(
            sessionRepository = get(),
            subscriptionRepository = get(),
            vibratorUtil = get(),
            entryMode = entryMode,
        )
    }
}

private fun createReportsViewModel(
    reportsRepository: ReportsRepository,
    choreRepository: ChoreRepository,
    subscriptionRepository: SubscriptionRepository,
    vibratorUtil: com.sortoapps.chorepilot.core.platform.VibratorUtil,
): ReportsViewModel = ReportsViewModel(
    reportsRepository = reportsRepository,
    choreRepository = choreRepository,
    subscriptionRepository = subscriptionRepository,
    vibratorUtil = vibratorUtil,
)
