package com.sortoapps.chorepilot.features.notifications.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                PROOF_LIFECYCLE_CHANNEL_ID,
                "Proof updates",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Proof submissions and approvals for your household chores."
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
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .build()
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }

        fun payloadFromIntent(intent: Intent?): NotificationTapPayload? {
            val source = intent ?: return null
            val notificationId = source.getStringExtra(EXTRA_NOTIFICATION_ID).orEmpty()
            if (notificationId.isBlank()) return null
            return NotificationTapPayload(
                notificationId = notificationId,
                choreId = source.getStringExtra(EXTRA_CHORE_ID).orEmpty(),
                type = notificationTypeOrDefault(source.getStringExtra(EXTRA_NOTIFICATION_TYPE).orEmpty()),
            )
        }
    }

    private fun GrantStatus.toAuthorizationStatus(): NotificationAuthorizationStatus = when (this) {
        GrantStatus.GRANTED,
        GrantStatus.PARTIAL_GRANTED -> NotificationAuthorizationStatus.Granted
        GrantStatus.DENIED -> NotificationAuthorizationStatus.Denied
        GrantStatus.DENIED_ALWAYS -> NotificationAuthorizationStatus.DeniedAlways
        GrantStatus.NOT_DETERMINED -> NotificationAuthorizationStatus.NotDetermined
    }
}
