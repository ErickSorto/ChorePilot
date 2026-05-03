package com.sortoapps.chorepilot

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.sortoapps.chorepilot.features.notifications.data.AndroidNotificationDeliveryGateway
import com.sortoapps.chorepilot.features.notifications.domain.NotificationTapBridge
import com.sortoapps.chorepilot.di.initKoin
import com.sortoapps.chorepilot.di.resolveAndroidFirebaseAvailability
import org.koin.android.ext.koin.androidContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT,
            ),
        )
        super.onCreate(savedInstanceState)
        resolveAndroidFirebaseAvailability(applicationContext)
        initKoin {
            androidContext(applicationContext)
        }
        AndroidNotificationDeliveryGateway.payloadFromIntent(intent)?.let(NotificationTapBridge::queuePayload)

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        AndroidNotificationDeliveryGateway.payloadFromIntent(intent)?.let(NotificationTapBridge::queuePayload)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
