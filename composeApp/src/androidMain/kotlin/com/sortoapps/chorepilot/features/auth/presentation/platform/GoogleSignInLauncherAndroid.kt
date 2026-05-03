package com.sortoapps.chorepilot.features.auth.presentation.platform

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
actual fun rememberGoogleSignInHandler(
    onSuccess: (GoogleSignInToken) -> Unit,
    onFailure: (String) -> Unit,
): GoogleSignInHandler {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val serverClientId = remember(context) { context.findServerClientId() }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val signInTask = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        runCatching {
            val account = signInTask.getResult(ApiException::class.java)
            val idToken = account.idToken ?: error("Google did not return an ID token.")
            GoogleSignInToken(
                idToken = idToken,
                accessToken = null,
                displayName = account.displayName.orEmpty(),
                email = account.email.orEmpty(),
            )
        }.onSuccess(onSuccess)
            .onFailure { error ->
                onFailure(error.message ?: "We couldn't complete Google sign-in.")
            }
    }

    return remember(activity, serverClientId, launcher) {
        GoogleSignInHandler(
            launch = launch@{
                val resolvedActivity = activity
                    ?: run {
                        onFailure("Google sign-in needs an active screen.")
                        return@launch
                    }
                val resolvedClientId = serverClientId
                    ?: run {
                        onFailure("Google sign-in isn't configured yet. Add google-services.json and the Firebase web client.")
                        return@launch
                    }
                val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .requestIdToken(resolvedClientId)
                    .build()
                val client = GoogleSignIn.getClient(resolvedActivity, options)
                launcher.launch(client.signInIntent)
            },
        )
    }
}

private fun Context.findServerClientId(): String? {
    val identifier = resources.getIdentifier("default_web_client_id", "string", packageName)
    return if (identifier == 0) null else getString(identifier).takeIf(String::isNotBlank)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
