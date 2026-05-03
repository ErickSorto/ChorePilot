package com.sortoapps.chorepilot.features.chorecapture.presentation.platform

import androidx.compose.runtime.Composable
import com.sortoapps.chorepilot.features.chores.domain.ProofUploadPayload

data class ProofCameraHandler(
    val launch: () -> Unit,
)

@Composable
expect fun rememberProofCameraHandler(
    onSuccess: (ProofUploadPayload) -> Unit,
    onFailure: (String) -> Unit,
): ProofCameraHandler
