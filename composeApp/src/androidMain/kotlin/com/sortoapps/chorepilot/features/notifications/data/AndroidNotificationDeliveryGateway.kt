package com.sortoapps.chorepilot.features.notifications.data

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sortoapps.chorepilot.R
import com.sortoapps.chorepilot.features.notifications.domain.NotificationAuthorizationStatus
import com.sortoapps.chorepilot.features.notifications.domain.NotificationDeliveryGateway
import com.sortoapps.chorepilot.features.notifications.domain.NotificationItem
import com.sortoapps.chorepilot.features.notifications.domain.NotificationTapPayload
import com.sortoapps.chorepilot.features.notifications.domain.notificationTypeOrDefault
import com.sortoapps.chorepilot.features.notifications.domain.toTapPayload
import dev.brewkits.grant.AppGrant
import dev.brewkits.grant.GrantManager
import dev.brewkits.grant.GrantStatus

class AndroidNotificationDeliveryGateway(
    private val context: Context,
    private val grantManager: GrantManager,
) : NotificationDeliveryGateway {
    override suspend fun getAuthorizationStatus(): NotificationAuthorizationStatus =
        grantManager.checkStatus(AppGrant.NOTIFICATION).toAuthorizationStatus()

    override suspend fun requestAuthorization(): NotificationAuthorizationStatus =
        grantManager.request(AppGrant.NOTIFICATION).toAuthorizationStatus()

    override suspend fun deliver(item: NotificationItem) {
        if (getAuthorizationStatus() != NotificationAuthorizationStatus.Granted) return
        showNotification(context, item.id.hashCode(), item.title, item.body, item.toTapPayload())
    }

    fun ensureChannel() {
        ensureChannel(context)
    }

    companion object {
        const val PROOF_LIFECYCLE_CHANNEL_ID = "proof_lifecycle_updates"
        const val EXTRA_NOTIFICATION_ID = "chorepilot.notification_id"
        const val EXTRA_CHORE_ID = "chorepilot.chore_id"
        const val EXTRA_NOTIFICATION_TYPE = "chorepilot.notification_type"
        private const val FCM_NOTIFICATION_ID = "notificationId"
        private const val FCM_CHORE_ID = "choreId"
        private const val FCM_NOTIFICATION_TYPE = "type"

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                PROOF_LIFECYCLE_CHANNEL_ID,
                "Household updates",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Due chores, proof reviews, assignments, and discovery approvals."
            }
            notificationManager.createNotificationChannel(channel)
        }

        fun showNotification(
            context: Context,
            notificationId: Int,
            title: String,
            body: String,
            payload: NotificationTapPayload? = null,
        ) {
            ensureChannel(context)
            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                ?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    if (payload != null) {
                        putExtra(EXTRA_NOTIFICATION_ID, payload.notificationId)
                        putExtra(EXTRA_CHORE_ID, payload.choreId)
                        putExtra(EXTRA_NOTIFICATION_TYPE, payload.type.name)
                    }
                }
            val contentIntent = launchIntent?.let { intent ->
                PendingIntent.getActivity(
                    context,
                    notificationId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            }
            val notification = NotificationCompat.Builder(context, PROOF_LIFECYCLE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_chorepilot_notification)
                .setColor(Color.rgb(85, 84, 177))
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .build()
            notifyIfAllowed(context, notificationId, notification)
        }

        @SuppressLint("MissingPermission")
        private fun notifyIfAllowed(
            context: Context,
            notificationId: Int,
            notification: android.app.Notification,
        ) {
            if (!canPostNotifications(context)) return
            runCatching {
                NotificationManagerCompat.from(context).notify(notificationId, notification)
            }
        }

        private fun canPostNotifications(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                return ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            }
            return NotificationManagerCompat.from(context).areNotificationsEnabled()
        }

        fun payloadFromIntent(intent: Intent?): NotificationTapPayload? {
            val source = intent ?: return null
            val notificationId = source.notificationExtra(EXTRA_NOTIFICATION_ID, FCM_NOTIFICATION_ID)
            if (notificationId.isBlank()) return null
            return NotificationTapPayload(
                notificationId = notificationId,
                choreId = source.notificationExtra(EXTRA_CHORE_ID, FCM_CHORE_ID),
                type = notificationTypeOrDefault(source.notificationExtra(EXTRA_NOTIFICATION_TYPE, FCM_NOTIFICATION_TYPE)),
            )
        }

        private fun Intent.notificationExtra(
            namespacedKey: String,
            fcmKey: String,
        ): String = getStringExtra(namespacedKey)
            ?: getStringExtra(fcmKey)
            ?: extras?.get(namespacedKey)?.toString()
            ?: extras?.get(fcmKey)?.toString()
            ?: ""
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
