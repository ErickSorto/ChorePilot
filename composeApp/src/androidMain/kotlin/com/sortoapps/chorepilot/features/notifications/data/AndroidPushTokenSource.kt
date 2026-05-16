package com.sortoapps.chorepilot.features.notifications.data

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sortoapps.chorepilot.features.notifications.domain.NotificationTapPayload
import com.sortoapps.chorepilot.features.notifications.domain.PushTokenSource
import com.sortoapps.chorepilot.features.notifications.domain.PushNotificationRegistrationSynchronizer
import com.sortoapps.chorepilot.features.notifications.domain.notificationTypeOrDefault
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class AndroidPushTokenSource(
    private val context: Context,
    appScope: CoroutineScope,
) : PushTokenSource {
    override val platform: String = "android"

    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override val tokens: Flow<String?> = callbackFlow {
        trySend(preferences.getString(PushNotificationRegistrationSynchronizer.TOKEN_KEY, null))
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == PushNotificationRegistrationSynchronizer.TOKEN_KEY) {
                trySend(sharedPreferences.getString(key, null))
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    init {
        appScope.launch {
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    if (!token.isNullOrBlank()) {
                        persistToken(context, token)
                    }
                }
        }
    }

    companion object {
        private const val PREFERENCES_NAME = "chorepilot_demo_preferences"

        fun persistToken(
            context: Context,
            token: String,
        ) {
            context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(PushNotificationRegistrationSynchronizer.TOKEN_KEY, token)
                .apply()
        }
    }
}

class ChorePilotFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        AndroidPushTokenSource.persistToken(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"] ?: return
        val notificationDocumentId = message.data["notificationId"]
            ?: message.messageId
            ?: (title + body)
        AndroidNotificationDeliveryGateway.showNotification(
            context = applicationContext,
            notificationId = notificationDocumentId.hashCode(),
            title = title,
            body = body,
            payload = NotificationTapPayload(
                notificationId = notificationDocumentId,
                choreId = message.data["choreId"].orEmpty(),
                type = notificationTypeOrDefault(message.data["type"].orEmpty()),
            ),
        )
    }
}
