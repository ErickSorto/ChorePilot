package com.sortoapps.chorepilot.features.notifications.data

import com.sortoapps.chorepilot.features.notifications.domain.NotificationAuthorizationStatus
import com.sortoapps.chorepilot.features.notifications.domain.NotificationDeliveryGateway
import com.sortoapps.chorepilot.features.notifications.domain.NotificationItem
import com.sortoapps.chorepilot.features.notifications.domain.toTapPayload
import dev.brewkits.grant.AppGrant
import dev.brewkits.grant.GrantManager
import dev.brewkits.grant.GrantStatus
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter

@OptIn(ExperimentalForeignApi::class)
class IosNotificationDeliveryGateway(
    private val grantManager: GrantManager,
) : NotificationDeliveryGateway {
    companion object {
        const val IOS_NOTIFICATION_ID_KEY = "notificationId"
        const val IOS_CHORE_ID_KEY = "choreId"
        const val IOS_NOTIFICATION_TYPE_KEY = "type"
    }

    override suspend fun getAuthorizationStatus(): NotificationAuthorizationStatus =
        grantManager.checkStatus(AppGrant.NOTIFICATION).toAuthorizationStatus()

    override suspend fun requestAuthorization(): NotificationAuthorizationStatus =
        grantManager.request(AppGrant.NOTIFICATION).toAuthorizationStatus()

    override suspend fun deliver(item: NotificationItem) {
        if (getAuthorizationStatus() != NotificationAuthorizationStatus.Granted) return
        val content = UNMutableNotificationContent().apply {
            val payload = item.toTapPayload()
            setTitle(item.title)
            setBody(item.body)
            setSound(UNNotificationSound.defaultSound)
            setUserInfo(
                mapOf(
                    IOS_NOTIFICATION_ID_KEY to payload.notificationId,
                    IOS_CHORE_ID_KEY to payload.choreId,
                    IOS_NOTIFICATION_TYPE_KEY to payload.type.name,
                ),
            )
        }
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
            timeInterval = 0.25,
            repeats = false,
        )
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = item.id,
            content = content,
            trigger = trigger,
        )
        UNUserNotificationCenter.currentNotificationCenter()
            .addNotificationRequest(request, withCompletionHandler = null)
    }

    private fun GrantStatus.toAuthorizationStatus(): NotificationAuthorizationStatus = when (this) {
        GrantStatus.GRANTED,
        GrantStatus.PARTIAL_GRANTED -> NotificationAuthorizationStatus.Granted
        GrantStatus.DENIED -> NotificationAuthorizationStatus.Denied
        GrantStatus.DENIED_ALWAYS -> NotificationAuthorizationStatus.DeniedAlways
        GrantStatus.NOT_DETERMINED,
        GrantStatus.BUSY -> NotificationAuthorizationStatus.NotDetermined
    }
}
