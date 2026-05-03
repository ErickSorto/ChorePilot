package com.sortoapps.chorepilot.features.chorecapture.presentation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.sortoapps.chorepilot.features.chores.domain.ProofUploadPayload
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val ProofCameraRequestNotification = "ChorePilotProofCameraRequested"
private const val ProofCameraSuccessNotification = "ChorePilotProofCameraSucceeded"
private const val ProofCameraFailureNotification = "ChorePilotProofCameraFailed"

@Composable
actual fun rememberProofCameraHandler(
    onSuccess: (ProofUploadPayload) -> Unit,
    onFailure: (String) -> Unit,
): ProofCameraHandler {
    val scope = rememberCoroutineScope()
    return remember(scope) {
        ProofCameraHandler(
            launch = {
                scope.launch {
                    runCatching { awaitProofCameraResult() }
                        .onSuccess(onSuccess)
                        .onFailure { error ->
                            onFailure(error.message ?: "We couldn't capture that photo.")
                        }
                }
            },
        )
    }
}

private suspend fun awaitProofCameraResult(): ProofUploadPayload =
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
            name = ProofCameraSuccessNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { notification: NSNotification? ->
            cleanup()
            val path = notification?.userInfo?.get("path") as? String
                ?: return@addObserverForName continuation.resumeWithException(
                    IllegalStateException("Camera capture did not return a file."),
                )
            continuation.resume(
                ProofUploadPayload(
                    localPath = path,
                    previewUri = "file://$path",
                    mimeType = "image/jpeg",
                    fileExtension = "jpg",
                ),
            )
        }

        failureObserver = notificationCenter.addObserverForName(
            name = ProofCameraFailureNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { notification: NSNotification? ->
            cleanup()
            val message = notification?.userInfo?.get("message") as? String
                ?: "We couldn't capture that photo."
            continuation.resumeWithException(IllegalStateException(message))
        }

        continuation.invokeOnCancellation { cleanup() }

        notificationCenter.postNotificationName(
            aName = ProofCameraRequestNotification,
            `object` = null,
            userInfo = null,
        )
    }
