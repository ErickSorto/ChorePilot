package com.sortoapps.chorepilot.features.auth.presentation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSBundle
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val GoogleSignInRequestNotification = "ChorePilotGoogleSignInRequested"
private const val GoogleSignInSuccessNotification = "ChorePilotGoogleSignInSucceeded"
private const val GoogleSignInFailureNotification = "ChorePilotGoogleSignInFailed"

@Composable
actual fun rememberGoogleSignInHandler(
    onSuccess: (GoogleSignInToken) -> Unit,
    onFailure: (String) -> Unit,
): GoogleSignInHandler {
    val scope = rememberCoroutineScope()
    val isConfigured = remember {
        NSBundle.mainBundle.pathForResource("GoogleService-Info", "plist") != null
    }

    return remember(scope, isConfigured) {
        GoogleSignInHandler(
            launch = {
                if (!isConfigured) {
                    onFailure("Google sign-in isn't configured on iOS yet. Add GoogleService-Info.plist to iosApp/iosApp.")
                    return@GoogleSignInHandler
                }
                scope.launch {
                    runCatching { awaitIosGoogleSignInResult() }
                        .onSuccess(onSuccess)
                        .onFailure { error ->
                            onFailure(error.message ?: "We couldn't complete Google sign-in.")
                        }
                }
            },
        )
    }
}

private suspend fun awaitIosGoogleSignInResult(): GoogleSignInToken =
    suspendCancellableCoroutine { continuation ->
        val notificationCenter = NSNotificationCenter.defaultCenter
        var successObserver: Any? = null
        var failureObserver: Any? = null

        fun cleanup() {
            successObserver?.let { notificationCenter.removeObserver(it) }
            failureObserver?.let { notificationCenter.removeObserver(it) }
            successObserver = null
            failureObserver = null
        }

        successObserver = notificationCenter.addObserverForName(
            name = GoogleSignInSuccessNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { notification: NSNotification? ->
            cleanup()
            val userInfo = notification?.userInfo.orEmpty()
            val idToken = userInfo["idToken"] as? String
                ?: return@addObserverForName continuation.resumeWithException(
                    IllegalStateException("Google sign-in did not return an ID token."),
                )
            continuation.resume(
                GoogleSignInToken(
                    idToken = idToken,
                    accessToken = userInfo["accessToken"] as? String,
                    displayName = userInfo["displayName"] as? String ?: "",
                    email = userInfo["email"] as? String ?: "",
                ),
            )
        }

        failureObserver = notificationCenter.addObserverForName(
            name = GoogleSignInFailureNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { notification: NSNotification? ->
            cleanup()
            val message = notification?.userInfo?.get("message") as? String
                ?: "We couldn't complete Google sign-in."
            continuation.resumeWithException(IllegalStateException(message))
        }

        continuation.invokeOnCancellation {
            cleanup()
        }

        notificationCenter.postNotificationName(
            aName = GoogleSignInRequestNotification,
            `object` = null,
            userInfo = null,
        )
    }
