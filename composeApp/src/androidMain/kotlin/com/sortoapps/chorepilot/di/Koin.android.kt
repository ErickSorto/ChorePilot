package com.sortoapps.chorepilot.di

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import dev.brewkits.grant.GrantFactory
import dev.brewkits.grant.GrantManager
import com.sortoapps.chorepilot.core.platform.AndroidSystemTextActions
import com.sortoapps.chorepilot.core.firebase.FirebaseAvailability
import com.sortoapps.chorepilot.core.firebase.FirebaseAvailabilityStatus
import com.sortoapps.chorepilot.core.platform.AndroidPlatformPreferences
import com.sortoapps.chorepilot.core.platform.PlatformPreferences
import com.sortoapps.chorepilot.core.platform.SystemTextActions
import com.sortoapps.chorepilot.core.platform.VibratorUtil
import com.sortoapps.chorepilot.core.platform.VibratorUtilImpl
import com.sortoapps.chorepilot.features.notifications.data.AndroidNotificationDeliveryGateway
import com.sortoapps.chorepilot.features.notifications.data.AndroidPushTokenSource
import com.sortoapps.chorepilot.features.notifications.domain.NotificationDeliveryGateway
import com.sortoapps.chorepilot.features.notifications.domain.PushTokenSource
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

private const val FirebaseLogTag = "ChorePilotFirebase"

actual val platformModule: Module = module {
    single<GrantManager> { GrantFactory.create(androidContext()) }
    single { AndroidPlatformPreferences(androidContext()) } bind PlatformPreferences::class
    single<VibratorUtil> { VibratorUtilImpl(context = androidContext()) }
    single<SystemTextActions> { AndroidSystemTextActions(androidContext()) }
    single<NotificationDeliveryGateway> {
        AndroidNotificationDeliveryGateway(
            context = androidContext(),
            grantManager = get(),
        ).also { it.ensureChannel() }
    }
    single<PushTokenSource> { AndroidPushTokenSource(androidContext(), get()) }
    single { resolveAndroidFirebaseAvailability(androidContext()) }
}

internal fun resolveAndroidFirebaseAvailability(context: Context): FirebaseAvailability {
    val googleAppId = context.findStringResource("google_app_id")
    if (googleAppId.isNullOrBlank()) {
        return FirebaseAvailability(
            status = FirebaseAvailabilityStatus.MissingPlatformConfig,
            message = "Firebase isn't configured on Android. Add composeApp/google-services.json and rebuild the app.",
        ).also(::logFirebaseAvailability)
    }

    return runCatching {
        val initializedApp = FirebaseApp.getApps(context).firstOrNull() ?: FirebaseApp.initializeApp(context)
        if (initializedApp == null) {
            FirebaseAvailability(
                status = FirebaseAvailabilityStatus.InitializationFailed,
                message = "Firebase couldn't start on Android. Verify the values generated from composeApp/google-services.json.",
            )
        } else {
            FirebaseAvailability(
                status = FirebaseAvailabilityStatus.Configured,
            )
        }
    }.getOrElse { error ->
        FirebaseAvailability(
            status = FirebaseAvailabilityStatus.InitializationFailed,
            message = "Firebase failed to initialize on Android: ${error.message ?: "unknown error"}",
        )
    }.also(::logFirebaseAvailability)
}

private fun Context.findStringResource(name: String): String? {
    val identifier = resources.getIdentifier(name, "string", packageName)
    return if (identifier == 0) null else getString(identifier)
}

private fun logFirebaseAvailability(firebaseAvailability: FirebaseAvailability) {
    if (firebaseAvailability.isConfigured) return
    Log.e(FirebaseLogTag, firebaseAvailability.message ?: "Firebase is unavailable on Android.")
}
