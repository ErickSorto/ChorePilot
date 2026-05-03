package com.sortoapps.chorepilot.features.notifications.data

import com.sortoapps.chorepilot.features.notifications.domain.PushNotificationRegistrationSynchronizer
import com.sortoapps.chorepilot.features.notifications.domain.PushTokenSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSUserDefaults

class IosPushTokenSource : PushTokenSource {
    override val platform: String = "ios"

    override val tokens: Flow<String?> = callbackFlow {
        val defaults = NSUserDefaults.standardUserDefaults
        trySend(defaults.stringForKey(PushNotificationRegistrationSynchronizer.TOKEN_KEY))
        val observer = NSNotificationCenter.defaultCenter.addObserverForName(
            name = TOKEN_CHANGED_NOTIFICATION,
            `object` = null,
            queue = null,
        ) { _ ->
            trySend(defaults.stringForKey(PushNotificationRegistrationSynchronizer.TOKEN_KEY))
        }
        awaitClose {
            NSNotificationCenter.defaultCenter.removeObserver(observer)
        }
    }

    companion object {
        const val TOKEN_CHANGED_NOTIFICATION = "ChorePilotPushTokenDidChange"
    }
}
