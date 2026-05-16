package com.sortoapps.chorepilot.di

import dev.brewkits.grant.GrantFactory
import dev.brewkits.grant.GrantManager
import com.sortoapps.chorepilot.core.firebase.FirebaseAvailability
import com.sortoapps.chorepilot.core.firebase.FirebaseAvailabilityStatus
import com.sortoapps.chorepilot.core.platform.IosPlatformPreferences
import com.sortoapps.chorepilot.core.platform.IosSystemTextActions
import com.sortoapps.chorepilot.core.platform.PlatformPreferences
import com.sortoapps.chorepilot.core.platform.SystemTextActions
import com.sortoapps.chorepilot.core.platform.VibratorUtil
import com.sortoapps.chorepilot.core.platform.VibratorUtilImpl
import com.sortoapps.chorepilot.features.notifications.data.IosNotificationDeliveryGateway
import com.sortoapps.chorepilot.features.notifications.data.IosPushTokenSource
import com.sortoapps.chorepilot.features.notifications.domain.NotificationDeliveryGateway
import com.sortoapps.chorepilot.features.notifications.domain.PushTokenSource
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize
import platform.Foundation.NSBundle
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<GrantManager> { GrantFactory.create() }
    single { IosPlatformPreferences() } bind PlatformPreferences::class
    single<VibratorUtil> { VibratorUtilImpl() }
    single<SystemTextActions> { IosSystemTextActions() }
    single<NotificationDeliveryGateway> { IosNotificationDeliveryGateway(get()) }
    single<PushTokenSource> { IosPushTokenSource() }
    single { resolveIosFirebaseAvailability() }
}

internal fun resolveIosFirebaseAvailability(): FirebaseAvailability {
    val hasPlist = NSBundle.mainBundle.pathForResource("GoogleService-Info", "plist") != null
    if (!hasPlist) {
        return FirebaseAvailability(
            status = FirebaseAvailabilityStatus.MissingPlatformConfig,
            message = "Firebase isn't configured on iOS. Add GoogleService-Info.plist to iosApp/iosApp and rebuild the app.",
        ).also(::logFirebaseAvailability)
    }

    return runCatching {
        Firebase.initialize()
        FirebaseAvailability(status = FirebaseAvailabilityStatus.Configured)
    }.getOrElse { error ->
        FirebaseAvailability(
            status = FirebaseAvailabilityStatus.InitializationFailed,
            message = "Firebase failed to initialize on iOS: ${error.message ?: "unknown error"}",
        )
    }.also(::logFirebaseAvailability)
}

private fun logFirebaseAvailability(firebaseAvailability: FirebaseAvailability) {
    if (firebaseAvailability.isConfigured) return
    println("ChorePilotFirebase: ${firebaseAvailability.message ?: "Firebase is unavailable on iOS."}")
}
