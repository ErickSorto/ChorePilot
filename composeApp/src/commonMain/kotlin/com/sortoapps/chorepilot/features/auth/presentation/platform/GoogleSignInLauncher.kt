package com.sortoapps.chorepilot.features.auth.presentation.platform

import androidx.compose.runtime.Composable

data class GoogleSignInToken(
    val idToken: String,
    val accessToken: String? = null,
    val displayName: String = "",
    val email: String = "",
)

data class GoogleSignInHandler(
    val launch: () -> Unit,
)

@Composable
expect fun rememberGoogleSignInHandler(
    onSuccess: (GoogleSignInToken) -> Unit,
    onFailure: (String) -> Unit,
): GoogleSignInHandler
